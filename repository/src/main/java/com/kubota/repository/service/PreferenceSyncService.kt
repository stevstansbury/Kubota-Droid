package com.kubota.repository.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import com.google.gson.Gson
import com.kubota.network.model.Address
import com.kubota.network.service.AuthAPI
import com.kubota.network.service.ManualAPI
import com.kubota.network.model.Dealer as NetworkDealer
import com.kubota.network.model.Equipment as NetworkEquipment
import com.kubota.network.service.NetworkResponse
import com.kubota.network.service.UserPreferencesAPI
import com.kubota.repository.data.*
import com.kubota.repository.prefs.DealerPreferencesRepo
import com.kubota.repository.prefs.GuidesRepo
import com.kubota.repository.prefs.EquipmentPreferencesRepo
import java.util.concurrent.atomic.AtomicBoolean

private const val MANUAL_BASE_URL = "https://mykubota.azurewebsites.net"
private const val MANUAL_PDF_URL = "http://drive.google.com/viewerng/viewer?embedded=true&url=$MANUAL_BASE_URL/PDFs/"
private const val MANUAL_HTML_URL = "$MANUAL_BASE_URL/HTML/"
private const val EXTRA_ACTION = "ACTION"

class PreferenceSyncService: Service() {

    private lateinit var accountDao: AccountDao
    private lateinit var dealerDao: DealerDao
    private lateinit var equipmentDao: EquipmentDao
    private lateinit var faultCodeDao: FaultCodeDao

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

                        val expirationTime = account.expireDate - (60 * 3)
                        val refreshToken = account.refreshToken
                        if ((System.currentTimeMillis() / 1000) >= expirationTime) {
                            if (refreshToken != null) {
                                // Refresh the token
                                val api = AuthAPI()

                                val response = api.refreshToken(refreshToken)
                                when (response) {
                                    is NetworkResponse.Success -> {
                                        account.accessToken = response.value.accessToken
                                        account.refreshToken = response.value.refreshToken
                                        account.expireDate = response.value.expirationDate
                                        accountDao.update(account)
                                        handleMessage(account, msg.data)
                                    }
                                    is NetworkResponse.ServerError -> {
                                        account.flags = Account.FLAGS_TOKEN_EXPIRED
                                        accountDao.update(account)
                                    }
                                }
                            } else {
                                account.flags = Account.FLAGS_TOKEN_EXPIRED
                                accountDao.update(account)
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
                val serializedEquipment = data?.getString(EquipmentPreferencesRepo.EXTRA_EQUIPMENT)
                val serializedDealer = data?.getString(DealerPreferencesRepo.EXTRA_DEALER)

                when(action) {
                    Intent.ACTION_INSERT -> {

                        if (serializedEquipment != null) {
                            val equipment = Gson().fromJson(serializedEquipment, Equipment::class.java)
                            addEquipment(account, equipment)
                        } else if (serializedDealer != null) {
                            val dealer = Gson().fromJson(serializedDealer, Dealer::class.java)
                            addDealer(account, dealer)
                        }

                    }

                    Intent.ACTION_EDIT -> {

                        if (serializedEquipment != null) {
                            val equipment = Gson().fromJson(serializedEquipment, Equipment::class.java)
                            updateEquipment(account, equipment)
                        }

                    }

                    Intent.ACTION_DELETE -> {

                        if (serializedEquipment != null) {
                            val equipment = Gson().fromJson(serializedEquipment, Equipment::class.java)
                            deleteEquipment(account, equipment)
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

                        compareLocalAndServerEquipments(
                            accountId = account.id,
                            serverEquipmentsList = userPrefs.equipments ?: emptyList(),
                            localEquipmentList = equipmentDao.getEquipments() ?: emptyList()
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

        private fun addEquipment(account: Account, equipment: Equipment) {
            // TODO: This has a possibility to return a false positive, should mark as an incomplete sync
            val manualLocation = syncMaintenanceManuals(modelName = equipment.model)

            // TODO: Mark this equipment as an incomplete sync, in order to re-sync later
            val hasGuides = when (val guideList = GuidesRepo(equipment.model).getGuideList()) {
                is GuidesRepo.Response.Success -> guideList.data.isNotEmpty()
                is GuidesRepo.Response.Failure -> false
            }

            val newEquipment = equipment.copy(hasGuide = hasGuides, manualLocation = manualLocation)

            if (account.isGuest()) {
                equipmentDao.insert(newEquipment)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.addEquipment(accessToken = account.accessToken, equipment = newEquipment.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) equipmentDao.insert(newEquipment)
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

        private fun deleteEquipment(account: Account, equipment: Equipment) {
            if (account.isGuest()) {
                equipmentDao.delete(equipment)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.deleteEquipment(accessToken = account.accessToken, equipment = equipment.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) equipmentDao.delete(equipment)
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

        private fun updateEquipment(account: Account, equipment: Equipment) {
            if (account.isGuest()) {
                equipmentDao.update(equipment)
                account.flags = Account.FLAGS_NORMAL
            } else {
                when (val results = api.updateEquipment(accessToken = account.accessToken, equipment = equipment.toNetworkModel())) {
                    is NetworkResponse.Success -> {
                        if (cancelled.get().not()) equipmentDao.update(equipment)
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

        private fun compareLocalAndServerEquipments(accountId: Int, serverEquipmentsList: List<NetworkEquipment>, localEquipmentList: List<Equipment>) {
            val localEquipmentsMap = hashMapOf<String, Equipment>()
            for (equipment in localEquipmentList) {
                localEquipmentsMap[equipment.serverId] = equipment
            }

            val serverEquipmentsMap = hashMapOf<String, NetworkEquipment>()
            for (equipment in serverEquipmentsList) {
                serverEquipmentsMap[equipment.id] = equipment
            }

            if (cancelled.get()) return

            // Step 1: Remove equipments not on the server.
            val serverIdSet = serverEquipmentsMap.keys
            val removeList = localEquipmentsMap.keys.filter { !serverIdSet.contains(it) }
            for (id in removeList) {
                val tempEquipment = localEquipmentsMap[id]
                tempEquipment?.let {
                    equipmentDao.delete(it)
                    localEquipmentsMap.remove(id)
                }
            }

            // Step 2: Add Equipment in the server and not saved locally.
            val localIdSet = localEquipmentsMap.keys
            val addList = serverEquipmentsMap.keys.filter { !localIdSet.contains(it) }
            for (id in addList) {
                serverEquipmentsMap[id]?.let {
                    val hasGuides = when (val guideList = GuidesRepo(it.model).getGuideList()) {
                        is GuidesRepo.Response.Success -> guideList.data.isNotEmpty()
                        is GuidesRepo.Response.Failure -> false
                    }
                    val manualLocation = when {
                        it.manualName.contains("html", true) -> "$MANUAL_HTML_URL${it.manualName}"
                        it.manualName.contains("pdf", true) -> "$MANUAL_PDF_URL${it.manualName}"
                        else -> it.manualName
                    }
                    equipmentDao.insert(it.toRepositoryModel(0, accountId, manualLocation, hasGuides))
                    //TODO(JC): We are not handling here if the equipment has fault codes.
                    serverEquipmentsMap.remove(id)
                }
            }

            //Step 3: Compare and determine which equipments require an update.
            if (serverEquipmentsMap.isEmpty() && localEquipmentsMap.isNotEmpty()) {
                for (equipment in localEquipmentList) {
                    if (cancelled.get()) return

                    equipmentDao.delete(equipment)
                }
            } else {
                for (key in serverEquipmentsMap.keys) {
                    if (cancelled.get()) return

                    val serverEquipment = serverEquipmentsMap[key]
                    val localEquipment = localEquipmentsMap[key]
                    if ((serverEquipment != null && localEquipment != null) || (localEquipment == null && serverEquipment != null)) {
                        val manualLocation = syncMaintenanceManuals(modelName = serverEquipment.model)

                        // TODO: Mark this equipment as an incomplete sync, in order to re-sync later
                        val hasGuides = when (val guideList = GuidesRepo(serverEquipment.model).getGuideList()) {
                            is GuidesRepo.Response.Success -> guideList.data.isNotEmpty()
                            is GuidesRepo.Response.Failure -> false
                        }
                        val tempEquipment = serverEquipment.toRepositoryModel(localEquipment?.id ?: 0, accountId, manualLocation, hasGuides)

                        if (localEquipment == null) {
                            if (cancelled.get()) return

                            equipmentDao.insert(tempEquipment)
                        } else if (tempEquipment != localEquipment) {
                            if (cancelled.get()) return

                            equipmentDao.update(tempEquipment)
                        }
                        localEquipmentsMap.remove(key)
                    }
                }
                // Delete the localEquipments that were not found on the server
                localEquipmentsMap.forEach {
                    if (cancelled.get()) return
                    equipmentDao.delete(it.value)
                }
            }
        }

        private fun compareLocalAndServerDealers(accountId: Int, serverDealerList: List<NetworkDealer>, localDealerList: List<Dealer>) {
            val localDealersMap = hashMapOf<String, Dealer>()
            for (dealer in localDealerList) {
                localDealersMap[dealer.serverId] = dealer
            }

            if (cancelled.get()) return

            val serverDealersMap = hashMapOf<String, NetworkDealer>()
            for (dealer in serverDealerList) {
                serverDealersMap[dealer.id] = dealer
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
        equipmentDao = AppDatabase.getInstance(context = applicationContext).equipmentDao()
        faultCodeDao = AppDatabase.getInstance(context = applicationContext).faultCodeDao()
    }
}

private fun Equipment.toNetworkModel(): NetworkEquipment {
    return NetworkEquipment(
        id = serverId,
        manualName = manualName,
        model = model,
        pinOrSerial = serialNumber,
        category = category,
        nickname = nickname,
        engineHours = engineHours.toDouble(),
        location = null,
        fuelLevelPercent = fuelLevel,
        defLevelPercent = defLevel,
        faultCodes = emptyList(),
        batteryVolt = battery,
        isEngineRunning = engineState,
        isVerified = isVerified
    )
}

private fun NetworkEquipment.toRepositoryModel(
    id: Int,
    userId: Int,
    manualLocation: String?,
    hasGuides: Boolean
): Equipment {
    return Equipment(
        id = id,
        serverId = this.id,
        userId = userId,
        model = model,
        serialNumber = pinOrSerial,
        category = category ?: "",
        manualName = manualName,
        manualLocation = manualLocation,
        hasGuide = hasGuides,
        nickname = this.nickname,
        engineHours = engineHours?.toInt() ?: 0,
        coolantTemperature = null,
        battery = batteryVolt,
        fuelLevel = fuelLevelPercent,
        defLevel = defLevelPercent,
        engineState = isEngineRunning,
        latitude = location?.latitude,
        longitude = location?.longitude,
        isVerified = isVerified
    )
}

private fun Dealer.toNetworkDealer(): NetworkDealer {
    val address = Address(
        street = streetAddress,
        city = city,
        zip = postalCode,
        stateCode = stateCode,
        countryCode = countryCode
    )
    return NetworkDealer(
        id = serverId,
        urlName = webAddress,
        address = address,
        phone = phone,
        dealerName = name,
        dealerNumber = number
    )
}

private fun NetworkDealer.toRepositoryModel(id: Int, userId: Int): Dealer {
    return Dealer(
        id = id,
        serverId = this.id,
        userId = userId,
        name = dealerName,
        streetAddress = address.street,
        city = address.city,
        stateCode = address.stateCode,
        postalCode = address.zip,
        countryCode = address.countryCode,
        phone = phone,
        webAddress = urlName,
        number = dealerNumber
    )
}

