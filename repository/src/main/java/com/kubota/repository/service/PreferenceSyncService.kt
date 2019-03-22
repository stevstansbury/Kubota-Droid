package com.kubota.repository.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.text.format.DateUtils
import android.util.Log
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.kubota.network.model.Parser
import com.kubota.network.service.UserPreferencesService
import com.kubota.repository.data.*
import com.kubota.repository.ext.getPublicClientApplication
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.ModelPreferencesRepo
import com.kubota.repository.user.PCASetting
import com.kubota.repository.user.UserRepo
import com.kubota.repository.utils.Utils
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.AuthenticationResult
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.exception.MsalException

class PreferenceSyncService: Service() {

    companion object {
        private const val LOG_TAG = "PreferenceSyncService"
        private const val EXTRA_ACTION = "ACTION"
    }

    private val userPreferencesService = Utils.getRetrofit().create(UserPreferencesService::class.java)

    private lateinit var accountDao: AccountDao
    private lateinit var dealerDao: DealerDao
    private lateinit var modelDao: ModelDao

    private var serviceLooper: Looper? = null
    private var serviceHandler: ServiceHandler? = null

    // Handler that receives messages from the thread
    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            try {
                accountDao.getAccount()?.let {account ->
                    if (account.isGuest() || account.flags == Account.FLAGS_TOKEN_EXPIRED) return@let

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
                                        authenticationResult?.let {authResult ->
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

        private fun syncAllPreferences(account: Account) {
            val response = userPreferencesService.getPreferences("Bearer ${account.accessToken}").execute()
            if (response.isSuccessful) {

                response.body()?.let { body ->
                    if (!isTokenExpired(account, body.toString())) {
                        account.flags = Account.FLAGS_NORMAL

                        val userPrefs = Parser.getUserPreferencesParser(body as LinkedTreeMap<String, *>).parse()

                        val modelList = modelDao.getModels() ?: emptyList()
                        for (model in modelList) {
                            modelDao.delete(model)
                        }

                        for (model in userPrefs.models) {
                            modelDao.insert(model.toRepositoryModel(account.id))
                        }
                    } else {
                        Log.d(LOG_TAG, "PreferenceSync: Token was expired")
                    }
                }

            } else {
                Log.d(LOG_TAG, "PreferenceSync was unsuccessful")
            }
        }

        private fun addModel(account: Account, model: Model) {
            val response = userPreferencesService.addModel("Bearer ${account.accessToken}", model.toNetworkModel()).execute()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (!isTokenExpired(account, body.toString())) {
                        //Should we do something with the response?
                    } else {
                        Log.d(LOG_TAG, "AddModel: Token was expired")
                    }
                }

            } else {
                Log.d(LOG_TAG, "AddModel was not successful")
            }
        }

        private fun deleteModel(account: Account, model: Model) {
            val response = userPreferencesService.deleteModel("Bearer ${account.accessToken}", model.toNetworkModel()).execute()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (!isTokenExpired(account, body.toString())) {
                        //Should we do something with the response?
                    } else {
                        Log.d(LOG_TAG, "DeleteModel: Token was expired")
                    }
                }
            } else {
                Log.d(LOG_TAG, "Token was expired")
            }
        }

        private fun updateModel(account: Account, model: Model) {
            val response = userPreferencesService.editModel("Bearer ${account.accessToken}", model.toNetworkModel()).execute()
            if (response.isSuccessful) {
                response.body()?.let { body ->
                    if (!isTokenExpired(account, body.toString())) {
                        //Should we do something with the response?
                    } else {
                        Log.d(LOG_TAG, "UpdateModel: Token was expired")
                    }
                }
            } else {
                Log.d(LOG_TAG, "updateModel was not successful")
            }
        }

        private fun addDealer(account: Account, dealer: Dealer) {

        }

        private fun deleteDealer(account: Account, dealer: Dealer) {

        }

        private fun isTokenExpired(account: Account, responseBody: String): Boolean {
            if (responseBody.contains("AuthenticationFailed")) {
                account.flags = Account.FLAGS_TOKEN_EXPIRED
                return true
            }

            return false
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

private fun Model.toNetworkModel(): com.kubota.network.model.Model {
    val newModel  =  com.kubota.network.model.Model()
    newModel.id = id
    newModel.manualName = manualName
    newModel.model = model
    newModel.serialNumber = serialNumber ?: ""
    newModel.modelCategory = if (category.isEmpty()) null else category

    return newModel
}

private fun com.kubota.network.model.Model.toRepositoryModel(userId: Int): Model {
    return Model(id, userId, manualName, model, serialNumber, modelCategory ?: "")
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

