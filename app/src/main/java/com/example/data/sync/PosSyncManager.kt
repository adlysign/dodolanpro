package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.entity.*
import com.example.data.repository.PosRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

class PosSyncManager(
    private val context: Context,
    private val repository: PosRepository
) {
    companion object {
        private const val TAG = "PosSyncManager"
        const val DEFAULT_PORT = 8888
        private const val DEVICE_TIMEOUT_MS = 15 * 60 * 1000L // 15 menit dianggap aktif
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private var clientHeartbeatJob: Job? = null

    // Track all registered connected devices (for Server)
    private val connectedDevicesMap = ConcurrentHashMap<String, DevicePeer>()

    private val prefs = context.getSharedPreferences("pos_sync_settings", Context.MODE_PRIVATE)

    private val _syncConfig = MutableStateFlow(loadConfig())
    val syncConfig: StateFlow<SyncConfig> = _syncConfig.asStateFlow()

    private val _syncStatus = MutableStateFlow(
        SyncStatusInfo(
            localIpAddress = getLocalIpAddress(),
            serverPort = DEFAULT_PORT
        )
    )
    val syncStatus: StateFlow<SyncStatusInfo> = _syncStatus.asStateFlow()

    init {
        // Auto-start based on saved role
        applyConfig(_syncConfig.value)
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val sortedInterfaces = interfaces.sortedByDescending { iface ->
                when {
                    iface.name.contains("wlan", ignoreCase = true) -> 3
                    iface.name.contains("ap", ignoreCase = true) -> 2
                    iface.name.contains("eth", ignoreCase = true) -> 1
                    else -> 0
                }
            }
            for (iface in sortedInterfaces) {
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving IP", e)
        }
        return "127.0.0.1"
    }

    private fun loadConfig(): SyncConfig {
        val roleStr = prefs.getString("sync_role", SyncRole.STANDALONE.name) ?: SyncRole.STANDALONE.name
        val role = try { SyncRole.valueOf(roleStr) } catch (_: Exception) { SyncRole.STANDALONE }
        val devId = prefs.getString("device_id", null) ?: "DEV-${java.util.UUID.randomUUID().toString().take(6).uppercase()}"
        prefs.edit().putString("device_id", devId).apply()
        val devName = prefs.getString("device_name", "Kasir 1") ?: "Kasir 1"
        val sHost = prefs.getString("server_host", "192.168.1.100") ?: "192.168.1.100"
        val sPort = prefs.getInt("server_port", DEFAULT_PORT)
        val interval = prefs.getInt("sync_interval", 5)
        val autoSync = prefs.getBoolean("auto_sync_enabled", true)
        return SyncConfig(
            role = role,
            deviceId = devId,
            deviceName = devName,
            serverHost = sHost,
            serverPort = sPort,
            autoSyncIntervalSec = interval,
            isAutoSyncEnabled = autoSync
        )
    }

    fun saveConfig(newConfig: SyncConfig) {
        prefs.edit()
            .putString("sync_role", newConfig.role.name)
            .putString("device_id", newConfig.deviceId)
            .putString("device_name", newConfig.deviceName)
            .putString("server_host", newConfig.serverHost)
            .putInt("server_port", newConfig.serverPort)
            .putInt("sync_interval", newConfig.autoSyncIntervalSec)
            .putBoolean("auto_sync_enabled", newConfig.isAutoSyncEnabled)
            .apply()
        _syncConfig.value = newConfig
        applyConfig(newConfig)
    }

    fun applyConfig(config: SyncConfig) {
        stopAllSyncServices()
        val ip = getLocalIpAddress()
        when (config.role) {
            SyncRole.SERVER -> {
                startServer(config.serverPort)
            }
            SyncRole.CLIENT -> {
                _syncStatus.value = _syncStatus.value.copy(
                    isServerRunning = false,
                    isConnectedToServer = false,
                    localIpAddress = ip,
                    serverPort = config.serverPort,
                    lastSyncStatus = "Mode Klien Aktif: Menghubungi Server ${config.serverHost}:${config.serverPort}..."
                )
            }
            SyncRole.STANDALONE -> {
                _syncStatus.value = _syncStatus.value.copy(
                    isServerRunning = false,
                    isConnectedToServer = false,
                    localIpAddress = ip,
                    serverPort = config.serverPort,
                    connectedDevices = emptyList(),
                    lastSyncStatus = "Mode Tunggal (Offline Stand-Alone)"
                )
            }
        }
    }

    private fun stopAllSyncServices() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        clientHeartbeatJob?.cancel()
        clientHeartbeatJob = null
    }

    // ==========================================
    // SERVER IMPLEMENTATION
    // ==========================================
    private fun startServer(port: Int) {
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket().apply {
                    reuseAddress = true
                    bind(java.net.InetSocketAddress(port))
                }
                val localIp = getLocalIpAddress()
                _syncStatus.value = _syncStatus.value.copy(
                    isServerRunning = true,
                    isConnectedToServer = true,
                    localIpAddress = localIp,
                    serverPort = port,
                    lastSyncStatus = "Server Aktif di $localIp:$port - Siap menerima koneksi kasir lain.",
                    isSuccess = true
                )
                Log.i(TAG, "Sync Server running on $localIp:$port")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch {
                            handleIncomingClient(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server start error", e)
                _syncStatus.value = _syncStatus.value.copy(
                    isServerRunning = false,
                    lastSyncStatus = "Gagal menjalankan Server: ${e.message}",
                    isSuccess = false
                )
            }
        }
    }

    private suspend fun handleIncomingClient(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = OutputStreamWriter(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext
            val method = parts[0]
            val path = parts[1]

            // Read headers to get content-length
            var contentLength = 0
            var headerLine: String?
            while (reader.readLine().also { headerLine = it } != null) {
                if (headerLine.isNullOrBlank()) break
                if (headerLine!!.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine!!.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body if POST
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = reader.read(buffer, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                bodyBuilder.append(buffer, 0, totalRead)
            }

            val body = bodyBuilder.toString()
            val clientIp = socket.inetAddress.hostAddress ?: "Unknown"

            val responseJson = when {
                method == "GET" && path == "/api/status" -> {
                    handleGetStatus()
                }
                method == "GET" && path == "/api/catalog" -> {
                    handleGetCatalog()
                }
                method == "POST" && path == "/api/heartbeat" -> {
                    handlePostHeartbeat(body, clientIp)
                }
                method == "POST" && path == "/api/sale" -> {
                    handlePostSale(body)
                }
                method == "GET" && path.startsWith("/api/eod_check") -> {
                    handleGetEodCheck()
                }
                else -> {
                    JSONObject().apply {
                        put("status", "error")
                        put("message", "Endpoint tidak ditemukan: $path")
                    }.toString()
                }
            }

            val responseBytes = responseJson.toByteArray(Charsets.UTF_8)
            writer.write("HTTP/1.1 200 OK\r\n")
            writer.write("Content-Type: application/json; charset=utf-8\r\n")
            writer.write("Content-Length: ${responseBytes.size}\r\n")
            writer.write("Access-Control-Allow-Origin: *\r\n")
            writer.write("Connection: close\r\n\r\n")
            writer.flush()
            socket.getOutputStream().write(responseBytes)
            socket.getOutputStream().flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client socket", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleGetStatus(): String {
        val root = JSONObject()
        root.put("status", "ok")
        root.put("serverTime", System.currentTimeMillis())
        val devicesArr = JSONArray()
        cleanOldDevices()
        for (peer in connectedDevicesMap.values) {
            val d = JSONObject()
            d.put("deviceId", peer.deviceId)
            d.put("deviceName", peer.deviceName)
            d.put("cashierNik", peer.cashierNik)
            d.put("cashierName", peer.cashierName)
            d.put("ipAddress", peer.ipAddress)
            d.put("pendingCount", peer.pendingCount)
            d.put("lastSeen", peer.lastSeenTimestamp)
            devicesArr.put(d)
        }
        root.put("devices", devicesArr)
        return root.toString()
    }

    private suspend fun handleGetCatalog(): String {
        return repository.exportDatabaseToJson()
    }

    private fun handlePostHeartbeat(body: String, clientIp: String): String {
        return try {
            val json = JSONObject(body)
            val devId = json.optString("deviceId", "UNKNOWN")
            val devName = json.optString("deviceName", "Kasir Klien")
            val nik = json.optString("cashierNik", "-")
            val name = json.optString("cashierName", "Kasir")
            val pending = json.optInt("pendingCount", 0)

            val peer = DevicePeer(
                deviceId = devId,
                deviceName = devName,
                role = "CLIENT",
                cashierNik = nik,
                cashierName = name,
                ipAddress = clientIp,
                lastSeenTimestamp = System.currentTimeMillis(),
                pendingCount = pending,
                isOnline = true
            )
            connectedDevicesMap[devId] = peer

            // Update UI StateFlow
            cleanOldDevices()
            _syncStatus.value = _syncStatus.value.copy(
                connectedDevices = connectedDevicesMap.values.toList()
            )

            val resp = JSONObject()
            resp.put("status", "ok")
            resp.put("serverTime", System.currentTimeMillis())
            resp.put("message", "Heartbeat diterima oleh Server")
            resp.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("status", "error")
                put("message", e.message)
            }.toString()
        }
    }

    private suspend fun handlePostSale(body: String): String {
        return try {
            val json = JSONObject(body)
            val bonNo = json.getString("bonNo")
            val cashierNik = json.optString("cashierNik", "-")
            val memberNo = json.optString("memberNo", "")
            val subtotal = json.getDouble("subtotal")
            val discount = json.getDouble("discount")
            val grandTotal = json.getDouble("grandTotal")
            val paymentMethod = json.optString("paymentMethod", "TUNAI")
            val nominalBayar = json.getDouble("nominalBayar")
            val nominalKembalian = json.getDouble("nominalKembalian")
            val timestamp = json.optLong("timestamp", System.currentTimeMillis())

            val itemsArr = json.getJSONArray("items")
            val txItems = mutableListOf<TransactionItemEntity>()
            for (i in 0 until itemsArr.length()) {
                val itObj = itemsArr.getJSONObject(i)
                val plu = itObj.getString("plu")
                val barcode = itObj.optString("barcode", plu)
                val nama = itObj.getString("nama")
                val hargaJual = itObj.getDouble("hargaJual")
                val qty = itObj.getInt("qty")
                val diskon = itObj.optDouble("diskonItem", 0.0)
                val subtotalItem = itObj.optDouble("subtotalItem", hargaJual * qty - diskon)
                val isRetur = itObj.optBoolean("isRetur", false)
                txItems.add(
                    TransactionItemEntity(
                        bonNo = bonNo,
                        plu = plu,
                        barcode = barcode,
                        nama = nama,
                        hargaJual = hargaJual,
                        qty = qty,
                        diskonItem = diskon,
                        subtotalItem = subtotalItem,
                        satuan = itObj.optString("satuan", "PCS"),
                        isPromo = false,
                        promoNote = if (isRetur) "Retur" else ""
                    )
                )
            }

            // Record transaction in central repository
            repository.checkoutTransaction(
                bonNo = bonNo,
                kasirNik = cashierNik,
                kasirNama = json.optString("cashierName", "Kasir"),
                memberNo = memberNo ?: "",
                subtotal = subtotal,
                diskonTotal = discount,
                donasi = 0.0,
                grandTotal = grandTotal,
                caraBayar = paymentMethod,
                bayarTunai = nominalBayar,
                bayarNonTunai = 0.0,
                kembalian = nominalKembalian,
                poinDigunakan = 0,
                nilaiPotonganPoin = 0.0,
                kodeVoucher = "",
                nilaiPotonganVoucher = 0.0,
                refPembayaran = "",
                items = txItems
            )

            JSONObject().apply {
                put("status", "ok")
                put("bonNo", bonNo)
                put("message", "Transaksi $bonNo berhasil disinkronkan ke Server master")
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting synced sale", e)
            JSONObject().apply {
                put("status", "error")
                put("message", "Gagal mencatat transaksi: ${e.message}")
            }.toString()
        }
    }

    private fun handleGetEodCheck(): String {
        cleanOldDevices()
        val pendingPeers = connectedDevicesMap.values.filter { it.pendingCount > 0 }
        val root = JSONObject()
        root.put("status", "ok")
        root.put("canProceed", pendingPeers.isEmpty())
        root.put("totalActiveDevices", connectedDevicesMap.size)
        val arr = JSONArray()
        for (peer in pendingPeers) {
            val d = JSONObject()
            d.put("deviceId", peer.deviceId)
            d.put("deviceName", peer.deviceName)
            d.put("cashierNik", peer.cashierNik)
            d.put("cashierName", peer.cashierName)
            d.put("pendingCount", peer.pendingCount)
            arr.put(d)
        }
        root.put("devicesWithPending", arr)
        return root.toString()
    }

    private fun cleanOldDevices() {
        val now = System.currentTimeMillis()
        val keysToRemove = mutableListOf<String>()
        for ((k, v) in connectedDevicesMap) {
            if (now - v.lastSeenTimestamp > DEVICE_TIMEOUT_MS) {
                keysToRemove.add(k)
            }
        }
        keysToRemove.forEach { connectedDevicesMap.remove(it) }
    }

    // ==========================================
    // CLIENT IMPLEMENTATION
    // ==========================================
    suspend fun sendClientHeartbeat(
        pendingCount: Int,
        cashierNik: String,
        cashierName: String,
        targetHost: String? = null,
        targetPort: Int? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cfg = _syncConfig.value
        val host = (targetHost ?: cfg.serverHost).trim()
        val port = targetPort ?: cfg.serverPort
        val urlStr = "http://$host:$port/api/heartbeat"
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3500
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val payload = JSONObject().apply {
                put("deviceId", cfg.deviceId)
                put("deviceName", cfg.deviceName)
                put("cashierNik", cashierNik)
                put("cashierName", cashierName)
                put("pendingCount", pendingCount)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            if (code == 200) {
                val resp = conn.inputStream.bufferedReader().use { it.readText() }
                if (host != cfg.serverHost || port != cfg.serverPort) {
                    saveConfig(cfg.copy(serverHost = host, serverPort = port))
                }
                _syncStatus.value = _syncStatus.value.copy(
                    isConnectedToServer = true,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    lastSyncStatus = "Terhubung dengan Server ($host:$port)",
                    isSuccess = true
                )
                Pair(true, "Berhasil terhubung ke Server Master ($host:$port)!")
            } else {
                _syncStatus.value = _syncStatus.value.copy(
                    isConnectedToServer = false,
                    lastSyncStatus = "Server merespons kode: $code",
                    isSuccess = false
                )
                Pair(false, "Server merespons HTTP $code")
            }
        } catch (e: Exception) {
            val err = e.localizedMessage ?: e.message ?: "Tidak dapat menjangkau server"
            _syncStatus.value = _syncStatus.value.copy(
                isConnectedToServer = false,
                lastSyncStatus = "Gagal terhubung ke $host:$port ($err)",
                isSuccess = false
            )
            Pair(false, "Koneksi ke $host:$port gagal ($err). Pastikan kedua perangkat terhubung ke Wi-Fi / Hotspot yang sama dan Server Master sudah aktif!")
        }
    }

    suspend fun syncCatalogFromServer(
        targetHost: String? = null,
        targetPort: Int? = null
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cfg = _syncConfig.value
        val host = (targetHost ?: cfg.serverHost).trim()
        val port = targetPort ?: cfg.serverPort
        val urlStr = "http://$host:$port/api/catalog"
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 15000
            }

            if (conn.responseCode == 200) {
                val json = conn.inputStream.bufferedReader().use { it.readText() }
                val restored = repository.restoreDatabaseFromJson(json)
                if (restored) {
                    if (host != cfg.serverHost || port != cfg.serverPort) {
                        saveConfig(cfg.copy(serverHost = host, serverPort = port))
                    }
                    _syncStatus.value = _syncStatus.value.copy(
                        lastSyncTimestamp = System.currentTimeMillis(),
                        lastSyncStatus = "Katalog & data master berhasil disinkronkan dari Server!",
                        isSuccess = true
                    )
                    Pair(true, "Sinkronisasi katalog dari Server ($host:$port) berhasil!")
                } else {
                    Pair(false, "Gagal memproses data katalog dari server")
                }
            } else {
                Pair(false, "Server merespons status: ${conn.responseCode}")
            }
        } catch (e: Exception) {
            Pair(false, "Koneksi gagal: ${e.message}. Pastikan IP server $host benar.")
        }
    }

    suspend fun scanLocalSubnetForServers(port: Int = DEFAULT_PORT): List<String> = withContext(Dispatchers.IO) {
        val localIp = getLocalIpAddress()
        if (localIp == "127.0.0.1" || !localIp.contains(".")) return@withContext emptyList()
        val subnet = localIp.substringBeforeLast(".") + "."

        val allIps = (1..254).map { "$subnet$it" }.filter { it != localIp }
        val jobs = allIps.map { ip ->
            async {
                try {
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress(ip, port), 250)
                    socket.close()
                    ip
                } catch (_: Exception) {
                    null
                }
            }
        }
        val results = jobs.awaitAll().filterNotNull()
        results
    }

    suspend fun pushSaleToServer(
        bonNo: String,
        cashierNik: String,
        memberNo: String?,
        subtotal: Double,
        discount: Double,
        grandTotal: Double,
        paymentMethod: String,
        nominalBayar: Double,
        nominalKembalian: Double,
        items: List<com.example.ui.viewmodel.CartItem>,
        timestamp: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val cfg = _syncConfig.value
        if (cfg.role != SyncRole.CLIENT) return@withContext true
        val urlStr = "http://${cfg.serverHost}:${cfg.serverPort}/api/sale"
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 5000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }

            val payload = JSONObject().apply {
                put("bonNo", bonNo)
                put("cashierNik", cashierNik)
                put("memberNo", memberNo)
                put("subtotal", subtotal)
                put("discount", discount)
                put("grandTotal", grandTotal)
                put("paymentMethod", paymentMethod)
                put("nominalBayar", nominalBayar)
                put("nominalKembalian", nominalKembalian)
                put("timestamp", timestamp)
                val itemsArr = JSONArray()
                for (it in items) {
                    val itObj = JSONObject().apply {
                        put("plu", it.product.plu)
                        put("barcode", it.product.barcode)
                        put("nama", it.product.nama)
                        put("hargaJual", it.unitPrice)
                        put("qty", it.qty)
                        put("diskonItem", it.customDiscount)
                        put("subtotalItem", it.subtotal)
                        put("satuan", it.selectedSatuan)
                        put("isRetur", it.isRetur)
                    }
                    itemsArr.put(itObj)
                }
                put("items", itemsArr)
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            conn.responseCode == 200
        } catch (e: Exception) {
            Log.e(TAG, "Failed pushing sale to server", e)
            false
        }
    }

    // ==========================================
    // EOD MULTI-DEVICE VALIDATION
    // ==========================================
    suspend fun checkEodPendingStatus(
        localPendingCount: Int,
        localCashierNik: String,
        localCashierName: String
    ): EodPendingCheckResult = withContext(Dispatchers.IO) {
        val cfg = _syncConfig.value

        // Check 1: Local device pending items
        if (localPendingCount > 0) {
            val localPeer = DevicePeer(
                deviceId = cfg.deviceId,
                deviceName = "${cfg.deviceName} (Perangkat Ini)",
                role = cfg.role.name,
                cashierNik = localCashierNik,
                cashierName = localCashierName,
                ipAddress = getLocalIpAddress(),
                lastSeenTimestamp = System.currentTimeMillis(),
                pendingCount = localPendingCount,
                isOnline = true
            )
            return@withContext EodPendingCheckResult(
                canProceed = false,
                localPendingCount = localPendingCount,
                totalPendingAcrossAllDevices = localPendingCount,
                devicesWithPending = listOf(localPeer),
                activeDevicesCount = 1,
                message = "Perangkat ini masih memiliki $localPendingCount keranjang pending yang belum diselesaikan!"
            )
        }

        // Check 2: If SERVER, inspect all active connected cashier devices
        if (cfg.role == SyncRole.SERVER) {
            cleanOldDevices()
            val pendingDevices = connectedDevicesMap.values.filter { it.pendingCount > 0 }
            val totalPending = pendingDevices.sumOf { it.pendingCount }
            return@withContext if (pendingDevices.isNotEmpty()) {
                EodPendingCheckResult(
                    canProceed = false,
                    localPendingCount = 0,
                    totalPendingAcrossAllDevices = totalPending,
                    devicesWithPending = pendingDevices,
                    activeDevicesCount = connectedDevicesMap.size + 1,
                    message = "Terdapat $totalPending keranjang pending yang belum diselesaikan di ${pendingDevices.size} perangkat kasir aktif!"
                )
            } else {
                EodPendingCheckResult(
                    canProceed = true,
                    localPendingCount = 0,
                    totalPendingAcrossAllDevices = 0,
                    devicesWithPending = emptyList(),
                    activeDevicesCount = connectedDevicesMap.size + 1,
                    message = "Semua kasir aktif telah menyelesaikan seluruh keranjang pending. Siap untuk EOD."
                )
            }
        }

        // Check 3: If CLIENT, query server eod_check endpoint
        if (cfg.role == SyncRole.CLIENT) {
            val urlStr = "http://${cfg.serverHost}:${cfg.serverPort}/api/eod_check"
            try {
                val url = URL(urlStr)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 4000
                }
                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(resp)
                    val canProceed = json.getBoolean("canProceed")
                    val totalActive = json.optInt("totalActiveDevices", 1)
                    val arr = json.getJSONArray("devicesWithPending")
                    val list = mutableListOf<DevicePeer>()
                    for (i in 0 until arr.length()) {
                        val d = arr.getJSONObject(i)
                        list.add(
                            DevicePeer(
                                deviceId = d.getString("deviceId"),
                                deviceName = d.getString("deviceName"),
                                role = "CLIENT",
                                cashierNik = d.getString("cashierNik"),
                                cashierName = d.getString("cashierName"),
                                ipAddress = "",
                                lastSeenTimestamp = System.currentTimeMillis(),
                                pendingCount = d.getInt("pendingCount")
                            )
                        )
                    }
                    val totalPending = list.sumOf { it.pendingCount }
                    return@withContext if (canProceed && list.isEmpty()) {
                        EodPendingCheckResult(
                            canProceed = true,
                            localPendingCount = 0,
                            totalPendingAcrossAllDevices = 0,
                            devicesWithPending = emptyList(),
                            activeDevicesCount = totalActive,
                            message = "Semua perangkat kasir telah menyelesaikan pekerjaan pending."
                        )
                    } else {
                        EodPendingCheckResult(
                            canProceed = false,
                            localPendingCount = 0,
                            totalPendingAcrossAllDevices = totalPending,
                            devicesWithPending = list,
                            activeDevicesCount = totalActive,
                            message = "Ada ${list.size} perangkat kasir yang masih memiliki $totalPending keranjang pending!"
                        )
                    }
                }
            } catch (e: Exception) {
                // If cannot connect to server, warn user
                return@withContext EodPendingCheckResult(
                    canProceed = false,
                    localPendingCount = 0,
                    totalPendingAcrossAllDevices = 0,
                    devicesWithPending = emptyList(),
                    activeDevicesCount = 1,
                    message = "Tidak dapat terhubung ke Server (${cfg.serverHost}) untuk verifikasi status kasir lain: ${e.message}"
                )
            }
        }

        // Default standalone
        EodPendingCheckResult(
            canProceed = true,
            localPendingCount = 0,
            totalPendingAcrossAllDevices = 0,
            devicesWithPending = emptyList(),
            activeDevicesCount = 1,
            message = "Tidak ada transaksi pending di perangkat ini."
        )
    }

    fun registerLocalDeviceAsServer(cashierNik: String, cashierName: String, localPending: Int) {
        val cfg = _syncConfig.value
        val localPeer = DevicePeer(
            deviceId = cfg.deviceId,
            deviceName = "${cfg.deviceName} (Server Master)",
            role = "SERVER",
            cashierNik = cashierNik,
            cashierName = cashierName,
            ipAddress = getLocalIpAddress(),
            lastSeenTimestamp = System.currentTimeMillis(),
            pendingCount = localPending,
            isOnline = true
        )
        connectedDevicesMap[cfg.deviceId] = localPeer
        _syncStatus.value = _syncStatus.value.copy(
            connectedDevices = connectedDevicesMap.values.toList()
        )
    }
}
