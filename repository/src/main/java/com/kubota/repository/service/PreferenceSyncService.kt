package com.kubota.repository.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.text.format.DateUtils
import com.google.gson.Gson
import com.kubota.network.model.Address
import com.kubota.network.service.ManualAPI
import com.kubota.network.model.Dealer as NetworkDealer
import com.kubota.network.model.Model as NetworkModel
import com.kubota.network.service.NetworkResponse
import com.kubota.network.service.UserPreferencesAPI
import com.kubota.repository.data.*
import com.kubota.repository.ext.getPublicClientApplication
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.GuidesRepo
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.PCASetting
import com.kubota.repository.user.UserRepo
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import java.util.concurrent.atomic.AtomicBoolean

private const val MANUAL_BASE_URL = "https://mykubota.azurewebsites.net"
private const val MANUAL_PDF_URL = "http://drive.google.com/viewerng/viewer?embedded=true&url=$MANUAL_BASE_URL/PDFs/"
private const val MANUAL_HTML_URL = "$MANUAL_BASE_URL/HTML/"
private const val EXTRA_ACTION = "ACTION"

class PreferenceSyncService: Service() {

    private lateinit var accountDao: AccountDao
    private lateinit var dealerDao: DealerDao
    private lateinit var modelDao: ModelDao
    private val cancelled = AtomicBoolean(false)
    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        private val api = UserPreferencesAPI()
        private val manualsApi = ManualAPI()

        override fun handleMessage(msg: Message) {
            try {
                accountDao.getAccount()?.let {account ->
                    if (!account.isGuest()) {
                        if (account.flags == Account.FLAGS_TOKEN_EXPIRED) return@let

                        val expirationTime = account.expireDate - (DateUtils.MINUTE_IN_MILLIS * 5)
                        if (System.currentTimeMillis() >= expirationTime) {
                            // Refresh the token
                            val pca = applicationContext.getPublicClientApplication()
                            val iAccount = pca.getAccount(account.homeAccountIdentifier ?: "", PCASetting.SignIn().authority)

                            if (iAccount == null) {
                                account.flags = Account.FLAGS_TOKEN_EXPIRED
                                accountDao.update(account)
                            } else {

                                pca.acquireTokenSilentAsync(UserRepo.SCOPES, iAccount, null, true,
                                    object : AuthenticationCallback {
                                        private val bundle = msg.data

                                        override fun onSuccess(authenticationResult: IAuthenticationResult?) {
                                            authenticationResult?.let { authResult ->
                                                Thread(Runnable {
                                                    account.expireDate = authResult.expiresOn.time
                                                    account.accessToken = authResult.accessToken

                                                    handleMessage(account, bundle)
                                                }).start()
                                            }
                                        }

                                        override fun onCancel() {

                                        }

                                        override fun onError(exception: MsalException?) {
                                            Thread(Runnable {
                                                account.flags = Account.FLAGS_TOKEN_EXPIRED
                                                accountDao.update(account)
                                            }).start()
                                        }

                                    })
                            }

                            return@let
                        }
                    }

                    handleMessage(account, msg.data)
                }

            } catch (e: InterruptedException) {
                // Restore interrupt status.
                Thread.currentThread().interrupt()
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1)
        }

        private fun handleMessage(account: Account, data: Bundle?) {
            account.flags = Account.FLAGS_SYNCING
            accountDao.update(account)

            data?.getString(EXTRA_ACTION).let {action ->
                val serializedModel = data?.getString(ModelPreferencesRepo.EXTRA_MODEL)
                val serializedDealer = data?.getString(DealerPreferencesRepo.EXTRA_DEALER)

                when(action) {
                    Intent.ACTION_INSERT -> {

                        if (serializedModel != null) {
                            val model = Gson().fromJson(serializedModel, Model::class.java)
                            addModel(account, model)
                        } else if (serializedDealer != null) {
                            val dealer = Gson().fromJson(serializedDealer, Dealer::class.java)
                            addDealer(account, dealer)
                        }

                    }

                    Intent.ACTION_EDIT -> {

                        if (serializedModel != null) {
                            val model = Gson().fromJson(serializedModel, Model::class.java)
                            updateModel(account, model)
                        }

                    }

                    Intent.ACTION_DELETE -> {

                        if (serializedModel != null) {
                            val model = Gson().fromJson(serializedModel, Model::class.java)
                            deleteModel(account, model)
                        } else if (serializedDealer != null) {
                            val dealer = Gson().fromJson(serializedDealer, Dealer::class.java)
                            deleteDealer(account, dealer)
                        }

                    }

                    Intent.ACTION_SYNC -> {
                        syncAllPreferences(account)
                    }
                }
            }

            accountDao.update(account)
        }

        private fun syncMaintenanceManuals(modelName: String): String? {
            return when(val response = manualsApi.getManualMapping(modelName)) {
                is NetworkResponse.Success ->  {
                    when {
                        response.value.location.contains("html", true) -> "$MANUAL_HTML_URL${response.value.location}"
                        response.value.location.contains("pdf", true) -> "$MANUAL_PDF_URL${response.value.location}"
                        else -> null
                    }
                }
                is NetworkResponse.ServerError -> null
                is NetworkResponse.IOException -> null
            }
        }

        private fun syncAllPreferences(account: Account) {
            if (account.isGuest()) {
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.getPreferences(accessToken = account.accessToken)) {
                    is NetworkResponse.Success -> {
                        val userPrefs = results.value

                        if (cancelled.get()) cancelOperation(account)

                        compareLocalAndServerDealers(
                            accountId = account.id,
                            serverDealerList = userPrefs.dealers ?: emptyList(),
                            localDealerList = dealerDao.getDealers() ?: emptyList()
                        )

                        if (cancelled.get()) cancelOperation(account)

                        compareLocalAndServerModels(
                            accountId = account.id,
                            serverModelsList = userPrefs.models ?: emptyList(),
                            localModelList = modelDao.getModels() ?: emptyList()
                        )
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun addModel(account: Account, model: Model) {
            // TODO: This has a possibility to return a false positive, should mark as an incomplete sync
            val manualLocation = syncMaintenanceManuals(modelName = model.model)

            // TODO: Mark this model as an incomplete sync, in order to re-sync later
            val hasGuides = when (val guideList = GuidesRepo(model.model).getGuideList()) {
                is GuidesRepo.Response.Success -> guideList.data.isNotEmpty()
                is GuidesRepo.Response.Failure -> false
            }

            val newModel = model.copy(hasGuide = hasGuides, manualLocation = manualLocation)

            if (account.isGuest()) {
                modelDao.insert(newModel)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.addModel(accessToken = account.accessToken, model = newModel.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) modelDao.insert(newModel)
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun deleteModel(account: Account, model: Model) {
            if (account.isGuest()) {
                modelDao.delete(model)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.deleteModel(accessToken = account.accessToken, model = model.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) modelDao.delete(model)
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun updateModel(account: Account, model: Model) {
            if (account.isGuest()) {
                modelDao.update(model)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.updateModel(accessToken = account.accessToken, model = model.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) modelDao.update(model)
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun addDealer(account: Account, dealer: Dealer) {
            if (account.isGuest()) {
                dealerDao.insert(dealer)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.addDealer(accessToken = account.accessToken, dealer = dealer.toNetworkDealer())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) dealerDao.insert(dealer)
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun deleteDealer(account: Account, dealer: Dealer) {
            if (account.isGuest()) {
                dealerDao.delete(dealer)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.deleteDealer(accessToken = account.accessToken, dealer = dealer.toNetworkDealer())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) dealerDao.delete(dealer)
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        account.flags = if (results.code == 401) {
                            Account.FLAGS_TOKEN_EXPIRED
                        } else {
                            Account.FLAGS_NORMAL
                        }
                    }

                    is NetworkResponse.IOException -> {
                        account.flags = Account.FLAGS_NORMAL
                    }
                }
            }
        }

        private fun compareLocalAndServerModels(accountId: Int, serverModelsList: List<NetworkModel>, localModelList: List<Model>) {
            val localModelsMap = hashMapOf<String, Model>()
            for (model in localModelList) {
                localModelsMap[model.serverId] = model
            }

            val serverModelsMap = hashMapOf<String, NetworkModel>()
            for (model in serverModelsList) {
                serverModelsMap[model.id] = model
            }

            if (cancelled.get()) return

            if (serverModelsMap.isEmpty() && localModelsMap.isNotEmpty()) {
                for (model in localModelList) {
                    if (cancelled.get()) return

                    modelDao.delete(model)
                }
            } else {
                for (key in serverModelsMap.keys) {
                    if (cancelled.get()) return

                    val serverModel = serverModelsMap[key]
                    val localModel = localModelsMap[key]
                    if ((serverModel != null && localModel != null) || (localModel == null && serverModel != null)) {
                        val manualLocation = syncMaintenanceManuals(modelName = serverModel.model)

                        // TODO: Mark this model as an incomplete sync, in order to re-sync later
                        val hasGuides = when (val guideList = GuidesRepo(serverModel.model).getGuideList()) {
                            is GuidesRepo.Response.Success -> guideList.data.isNotEmpty()
                            is GuidesRepo.Response.Failure -> false
                        }
                        val tempModel = serverModel.toRepositoryModel(localModel?.id ?: 0, accountId, manualLocation, hasGuides)

                        if (localModel == null) {
                            if (cancelled.get()) return

                            modelDao.insert(tempModel)
                        } else if (tempModel != localModel) {
                            if (cancelled.get()) return

                            modelDao.update(tempModel)
                        }
                        localModelsMap.remove(key)
                    }
                }
                // Delete the localModels that were not found on the server
                localModelsMap.forEach {
                    if (cancelled.get()) return
                    modelDao.delete(it.value)
                }
            }
        }

        private fun compareLocalAndServerDealers(accountId: Int, serverDealerList: List<NetworkDealer>, localDealerList: List<Dealer>) {
            val localDealersMap = hashMapOf<String, Dealer>()
            for (model in localDealerList) {
                localDealersMap[model.serverId] = model
            }

            if (cancelled.get()) return

            val serverDealersMap = hashMapOf<String, NetworkDealer>()
            for (model in serverDealerList) {
                serverDealersMap[model.id] = model
            }

            if (cancelled.get()) return

            if (serverDealersMap.isEmpty() && localDealersMap.isNotEmpty()) {
                for (dealer in localDealerList) {
                    if (cancelled.get()) return

                    dealerDao.delete(dealer)
                }
            } else {
                for (key in serverDealersMap.keys) {
                    if (cancelled.get()) return

                    val dealer1 = serverDealersMap[key]
                    val dealer2 = localDealersMap[key]

                    if ((dealer1 != null && dealer2 != null) || (dealer2 == null && dealer1 != null)) {

                        val tempDealer = dealer1.toRepositoryModel(dealer2?.id ?: 0, accountId)
                        if (dealer2 == null) {
                            dealerDao.insert(tempDealer)
                        } else if (tempDealer != dealer2) {
                            dealerDao.update(tempDealer)
                        }
                        localDealersMap.remove(key)
                    }
                }
                // Delete the localDealers that were not found on the server
                localDealersMap.forEach { dealerDao.delete(it.value) }
            }

        }

        private fun cancelOperation(account: Account) {
            account.flags = Account.FLAGS_NORMAL
        }
    }

    override fun onCreate() {
        HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND).apply {
            start()

            // Get the HandlerThread's Looper and use it for our Handler
            serviceLooper = looper
            serviceHandler = ServiceHandler(looper)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        HandlerThread("Service", Process.THREAD_PRIORITY_BACKGROUND).apply {
            serviceHandler?.obtainMessage()?.also { msg ->
                msg.arg1 = startId
                val bundle = intent?.extras ?: Bundle(1)
                intent?.let {
                    bundle.putString(EXTRA_ACTION, intent.action)
                }
                msg.data = bundle

                serviceHandler?.sendMessage(msg)
            }
        }

        return START_NOT_STICKY

    }

    override fun onDestroy() {
        super.onDestroy()

        cancelled.set(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        accountDao = AppDatabase.getInstance(context = applicationContext).accountDao()
        dealerDao = AppDatabase.getInstance(context = applicationContext).dealerDao()
        modelDao = AppDatabase.getInstance(context = applicationContext).modelDao()
    }

}

private fun Model.toNetworkModel(): NetworkModel {
    return NetworkModel(serverId, manualName, model, serialNumber ?: "", category)
}

private fun NetworkModel.toRepositoryModel(id: Int, userId: Int, manualLocation: String?, hasGuides: Boolean): Model {
    return Model(id = id, serverId = this.id ,userId = userId, model = model, serialNumber = serialNumber, category = category ?: "",
        manualName = manualName, manualLocation = manualLocation, hasGuide = hasGuides)
}

private fun Dealer.toNetworkDealer(): NetworkDealer {
    val address = Address(street = streetAddress, city = city, zip = postalCode, stateCode = stateCode, countryCode = countryCode)
    return NetworkDealer(id = serverId, urlName = webAddress, address = address, phone = phone, dealerName = name, dealerNumber = number)
}

private fun NetworkDealer.toRepositoryModel(id: Int, userId: Int): Dealer {
    return Dealer(id = id, serverId = this.id, userId = userId, name = dealerName, streetAddress = address.street,
        city = address.city, stateCode = address.stateCode, postalCode = address.zip, countryCode = address.countryCode,
        phone = phone, webAddress = urlName, number = dealerNumber)
}

