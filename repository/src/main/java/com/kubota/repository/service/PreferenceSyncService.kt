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
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.exception.MsalException

private const val MANUAL_BASE_URL = "https://mykubota.azurewebsites.net"
private const val MANUAL_PDF_URL = "http://drive.google.com/viewerng/viewer?embedded=true&url=$MANUAL_BASE_URL/PDFs/"
private const val MANUAL_HTML_URL = "$MANUAL_BASE_URL/HTML/"
private const val EXTRA_ACTION = "ACTION"

class PreferenceSyncService: Service() {

    private lateinit var accountDao: AccountDao
    private lateinit var dealerDao: DealerDao
    private lateinit var modelDao: ModelDao

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
                            val iAccount = pca.accounts.getUserByPolicy(PCASetting.SignIn().policy)

                            if (iAccount == null) {
                                return@let
                            } else {

                                pca.acquireTokenSilentAsync(UserRepo.SCOPES, iAccount, null, true,
                                    object : AuthenticationCallback {
                                        private val bundle = msg.data

                                        override fun onSuccess(authenticationResult: AuthenticationResult?) {
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
            val response = manualsApi.getManualMapping(modelName)
            return when(response) {
                is NetworkResponse.Success ->  {
                    if (response.value.location.contains("html", true)) "$MANUAL_HTML_URL${response.value.location}"
                    else if (response.value.location.contains("pdf", true)) "$MANUAL_PDF_URL${response.value.location}"
                    else null
                }
                is NetworkResponse.ServerError -> null
                is NetworkResponse.IOException -> null
            }
        }

        private fun syncAllPreferences(account: Account) {
            if (account.isGuest()) {
                account.flags = Account.FLAGS_NORMAL
            } else {
                val results = api.getPreferences(accessToken = account.accessToken)
                when (results) {
                    is NetworkResponse.Success -> {
                        val userPrefs = results.value

                        compareLocalAndServerDealers(
                            accountId = account.id,
                            serverDealerList = userPrefs.dealers ?: emptyList(),
                            localDealerList = dealerDao.getDealers() ?: emptyList()
                        )
                        compareLocalAndServerModels(
                            accountId = account.id,
                            serverModelsList = userPrefs.models ?: emptyList(),
                            localModelList = modelDao.getModels() ?: emptyList()
                        )
                        account.flags = Account.FLAGS_NORMAL
                    }

                    is NetworkResponse.ServerError -> {
                        if (results.code == 401) {
                            account.flags = Account.FLAGS_TOKEN_EXPIRED
                        }
                    }

                    is NetworkResponse.IOException -> {

                    }
                }
            }
        }

        private fun addModel(account: Account, model: Model) {
            val manualLocation = syncMaintenanceManuals(modelName = model.model)
            val guideList = GuidesRepo(model.model).getGuideList()
            val hasGuides = guideList.isNotEmpty()
            val newModel = model.copy(hasGuide = hasGuides, manualLocation = manualLocation)

            if (account.isGuest()) {
                modelDao.insert(newModel)
                account.flags = Account.FLAGS_NORMAL
            } else {
                val results = api.addModel(accessToken = account.accessToken, model = newModel.toNetworkModel())
                when (results) {
                    is NetworkResponse.Success -> {
                        modelDao.insert(newModel)
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
                val results = api.deleteModel(accessToken = account.accessToken, model = model.toNetworkModel())
                when (results) {
                    is NetworkResponse.Success -> {
                        modelDao.delete(model)
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
                val results = api.updateModel(accessToken = account.accessToken, model = model.toNetworkModel())
                when (results) {
                    is NetworkResponse.Success -> {
                        modelDao.update(model)
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
                val results = api.addDealer(accessToken = account.accessToken, dealer = dealer.toNetworkDealer())
                when (results) {
                    is NetworkResponse.Success -> {
                        dealerDao.insert(dealer)
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
                val results = api.deleteDealer(accessToken = account.accessToken, dealer = dealer.toNetworkDealer())
                when (results) {
                    is NetworkResponse.Success -> {
                        dealerDao.delete(dealer)
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

            if (serverModelsMap.isEmpty() && localModelsMap.isNotEmpty()) {
                for (model in localModelList) {
                    modelDao.delete(model)
                }
            } else {
                for (key in serverModelsMap.keys) {
                    val model1 = serverModelsMap[key]
                    val model2 = localModelsMap[key]
                    if (model1 == null && model2 != null) {
                        modelDao.delete(model2)
                    } else if ((model1 != null && model2 != null) || (model2 == null && model1 != null)) {
                        val manualLocation = syncMaintenanceManuals(modelName = model1.model)

                        val guideList = GuidesRepo(model1.model).getGuideList()
                        val hasGuides = guideList.isNotEmpty()
                        val tempModel = model1.toRepositoryModel(model2?.id ?: 0, accountId, manualLocation, hasGuides)

                        if (model2 == null) {
                            modelDao.insert(tempModel)
                        } else if (tempModel != model2) {
                            modelDao.update(tempModel)
                        }

                    }
                }
            }
        }

        private fun compareLocalAndServerDealers(accountId: Int, serverDealerList: List<NetworkDealer>, localDealerList: List<Dealer>) {
            val localDealersMap = hashMapOf<String, Dealer>()
            for (model in localDealerList) {
                localDealersMap[model.serverId] = model
            }

            val serverDealersMap = hashMapOf<String, NetworkDealer>()
            for (model in serverDealerList) {
                serverDealersMap[model.id] = model
            }

            if (serverDealersMap.isEmpty() && localDealersMap.isNotEmpty()) {
                for (dealer in localDealerList) {
                    dealerDao.delete(dealer)
                }
            } else {
                for (key in serverDealersMap.keys) {
                    val dealer1 = serverDealersMap[key]
                    val dealer2 = localDealersMap[key]

                    if (dealer1 == null && dealer2 != null) {
                        dealerDao.delete(dealer2)
                    } else if ((dealer1 != null && dealer2 != null) || (dealer2 == null && dealer1 != null)) {

                        val tempDealer = dealer1.toRepositoryModel(dealer2?.id ?: 0, accountId)
                        if (dealer2 == null) {
                            dealerDao.insert(tempDealer)
                        } else if (tempDealer != dealer2) {
                            dealerDao.update(tempDealer)
                        }

                    }
                }
            }

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

private fun List<IAccount>.getUserByPolicy(policy: String): IAccount? {
    for (user in this) {
        val userIdentifier = user.homeAccountIdentifier.identifier
        if (userIdentifier.contains(policy.toLowerCase())) {
            return user
        }
    }

    return null
}

