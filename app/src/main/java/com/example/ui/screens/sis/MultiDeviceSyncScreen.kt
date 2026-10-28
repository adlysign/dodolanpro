package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.SyncConfig
import com.example.data.sync.SyncRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiDeviceSyncScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val syncConfig by viewModel.syncConfig.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()
    val pendingCarts by viewModel.pendingCarts.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    var selectedRole by remember(syncConfig) { mutableStateOf(syncConfig.role) }
    var deviceNameInput by remember(syncConfig) { mutableStateOf(syncConfig.deviceName) }
    var serverHostInput by remember(syncConfig) { mutableStateOf(syncConfig.serverHost) }
    var serverPortInput by remember(syncConfig) { mutableStateOf(syncConfig.serverPort.toString()) }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    var isScanningLan by remember { mutableStateOf(false) }
    var discoveredServers by remember { mutableStateOf<List<String>>(emptyList()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var isSyncingCatalog by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf("") }
    var isSuccessAction by remember { mutableStateOf(true) }

    val localPendingCount = pendingCarts.size + (if (cartItems.isNotEmpty()) 1 else 0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SINKRONISASI MULTI-PERANGKAT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Konfigurasi Real-Time Server Master & Klien Kasir",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_sync")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
                .padding(14.dp)
        ) {
            item {
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RetailBlueLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailNavy.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SINKRONISASI JARINGAN WI-FI / LAN LOKAL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Text(
                                "Menghubungkan beberapa perangkat kasir (POS) dalam satu toko secara offline melalui Wi-Fi lokal. Semua data katalog, harga, member, dan riwayat penjualan akan sinkron, sementara transaksi pending tetap dikerjakan masing-masing kasir.",
                                fontSize = 11.sp,
                                color = DarkText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: PILIH PERAN PERANGKAT (ROLE SELECTOR)
                Text("1. PERAN PERANGKAT INI (DEVICE ROLE)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1: Standalone
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = SyncRole.STANDALONE },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == SyncRole.STANDALONE) RetailNavy else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedRole == SyncRole.STANDALONE) RetailNavy else PosCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = if (selectedRole == SyncRole.STANDALONE) Color.White else RetailNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "STANDALONE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selectedRole == SyncRole.STANDALONE) Color.White else Color.Black
                            )
                            Text(
                                "Tunggal / Offline",
                                fontSize = 9.sp,
                                color = if (selectedRole == SyncRole.STANDALONE) Color.White.copy(alpha = 0.8f) else GrayText
                            )
                        }
                    }

                    // Option 2: Server
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = SyncRole.SERVER },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == SyncRole.SERVER) RetailGreen else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedRole == SyncRole.SERVER) RetailGreen else PosCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Dns,
                                contentDescription = null,
                                tint = if (selectedRole == SyncRole.SERVER) Color.White else RetailGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "SERVER MASTER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (selectedRole == SyncRole.SERVER) Color.White else Color.Black
                            )
                            Text(
                                "Pusat Database Toko",
                                fontSize = 9.sp,
                                color = if (selectedRole == SyncRole.SERVER) Color.White.copy(alpha = 0.8f) else GrayText
                            )
                        }
                    }

                    // Option 3: Client
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedRole = SyncRole.CLIENT },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedRole == SyncRole.CLIENT) RetailAmber else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedRole == SyncRole.CLIENT) RetailAmber else PosCardBorder
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.PointOfSale,
                                contentDescription = null,
                                tint = if (selectedRole == SyncRole.CLIENT) Color.Black else RetailAmber
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "KASIR KLIEN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.Black
                            )
                            Text(
                                "Terminal Kasir 2, 3...",
                                fontSize = 9.sp,
                                color = if (selectedRole == SyncRole.CLIENT) Color.Black.copy(alpha = 0.8f) else GrayText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 2: DETAIL KONFIGURASI PERAN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        when (selectedRole) {
                            SyncRole.STANDALONE -> {
                                Text(
                                    "Mode Standalone (Tunggal)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = RetailNavy
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Aplikasi berjalan di perangkat ini secara mandiri tanpa terhubung ke perangkat kasir lain. Database dan transaksi tersimpan lokal di memori perangkat.",
                                    fontSize = 11.sp,
                                    color = DarkText
                                )
                            }
                            SyncRole.SERVER -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Dns, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Konfigurasi Server Master (Device 1)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RetailGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = RetailGreenLight,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("ALAMAT SERVER UNTUK KASIR KLIEN:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RetailNavy)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "IP: ${syncStatus.localIpAddress}  |  Port: ${syncStatus.serverPort}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            OutlinedButton(
                                                onClick = {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(syncStatus.localIpAddress))
                                                    actionMessage = "IP Server (${syncStatus.localIpAddress}) disalin ke clipboard!"
                                                    isSuccessAction = true
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("SALIN IP", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Status Layanan: ${if (syncStatus.isServerRunning) "AKTIF (Menerima Koneksi)" else "Belum Dijalankan"}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (syncStatus.isServerRunning) RetailGreen else RetailRedPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = deviceNameInput,
                                    onValueChange = { deviceNameInput = it },
                                    label = { Text("Nama Perangkat Server") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_sync_dev_name"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = serverPortInput,
                                    onValueChange = { serverPortInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Port Server (Default: 8888)") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_sync_port"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                            SyncRole.CLIENT -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PointOfSale, contentDescription = null, tint = RetailAmber, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Konfigurasi Klien Kasir (Device Tambahan)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = deviceNameInput,
                                    onValueChange = { deviceNameInput = it },
                                    label = { Text("Nama Perangkat Kasir (misal: Kasir 2)") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_client_dev_name"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = serverHostInput,
                                    onValueChange = { serverHostInput = it.trim() },
                                    label = { Text("IP Server Master (misal: 192.168.1.100)") },
                                    placeholder = { Text("192.168.1.100") },
                                    modifier = Modifier.fillMaxWidth().testTag("input_client_server_host"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Tombol Scan LAN Otomatis
                                OutlinedButton(
                                    onClick = {
                                        isScanningLan = true
                                        actionMessage = "Sedang memindai jaringan Wi-Fi untuk menemukan Server Master..."
                                        viewModel.scanLocalNetworkForServers { list ->
                                            isScanningLan = false
                                            discoveredServers = list
                                            if (list.isNotEmpty()) {
                                                serverHostInput = list.first()
                                                actionMessage = "Ditemukan Server Master di ${list.first()}! IP otomatis diterapkan."
                                                isSuccessAction = true
                                            } else {
                                                actionMessage = "Tidak ditemukan Server otomatis. Pastikan Server Master aktif dan kedua perangkat terhubung ke Wi-Fi / Hotspot yang sama."
                                                isSuccessAction = false
                                            }
                                        }
                                    },
                                    enabled = !isScanningLan,
                                    modifier = Modifier.fillMaxWidth().testTag("btn_scan_lan_server")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isScanningLan) "Memindai Jaringan Wi-Fi..." else "CARI SERVER OTOMATIS (SCAN LAN)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = serverPortInput,
                                    onValueChange = { serverPortInput = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("Port Server (Default: 8888)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            isTestingConnection = true
                                            val port = serverPortInput.toIntOrNull() ?: 8888
                                            viewModel.testSyncConnection(serverHostInput.trim(), port) { ok, msg ->
                                                isTestingConnection = false
                                                isSuccessAction = ok
                                                actionMessage = msg
                                            }
                                        },
                                        enabled = !isTestingConnection && serverHostInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailAmber, contentColor = Color.Black),
                                        modifier = Modifier.weight(1f).testTag("btn_test_sync_conn")
                                    ) {
                                        Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isTestingConnection) "MENGUJI..." else "UJI KONEKSI", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = {
                                            isSyncingCatalog = true
                                            val port = serverPortInput.toIntOrNull() ?: 8888
                                            viewModel.syncCatalogFromServer(serverHostInput.trim(), port) { ok, msg ->
                                                isSyncingCatalog = false
                                                isSuccessAction = ok
                                                actionMessage = msg
                                            }
                                        },
                                        enabled = !isSyncingCatalog && serverHostInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                        modifier = Modifier.weight(1f).testTag("btn_sync_catalog_now")
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isSyncingCatalog) "SINKRON..." else "SINKRON KATALOG", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val port = serverPortInput.toIntOrNull() ?: 8888
                                val updated = syncConfig.copy(
                                    role = selectedRole,
                                    deviceName = deviceNameInput.ifBlank { "Kasir 1" },
                                    serverHost = serverHostInput.ifBlank { "192.168.1.100" },
                                    serverPort = port
                                )
                                viewModel.updateSyncConfig(updated)
                                isSuccessAction = true
                                actionMessage = "Pengaturan peran sinkronisasi berhasil disimpan!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_save_sync_config")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMPAN PENGATURAN PERAN SINKRONISASI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 3: ATURAN PENTING TRANSAKSI PENDING KASIR
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PauseCircle, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ISOLASI PENDING ITEM & TANGGUNG JAWAB KASIR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "• Data katalog, harga, stok, dan member selalu sinkron antar semua perangkat.\n" +
                            "• Namun keranjang PENDING (Hold Order) TIDAK DIBAGIKAN ke perangkat lain karena merupakan tanggung jawab masing-masing kasir.\n" +
                            "• Saat Tutup Toko (EOD), sistem membaca seluruh kasir/perangkat aktif. Jika MASIH ADA KASIR DENGAN KERANJANG PENDING, EOD AKAN DITOLAK secara otomatis hingga semua pending diselesaikan.",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Status Pending di Perangkat Ini:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
                                    Text(
                                        if (localPendingCount == 0) "0 Keranjang Pending (Bersih & Siap EOD)" else "$localPendingCount Keranjang Pending Aktif!",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (localPendingCount == 0) RetailGreen else RetailRedPrimary
                                    )
                                }

                                if (localPendingCount > 0) {
                                    Button(
                                        onClick = { viewModel.navigateTo(com.example.ui.viewmodel.PosNavDestination.KASIR) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Selesaikan di Kasir", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: DAFTAR KASIR & PERANGKAT TERHUBUNG (KHUSUS SERVER)
                if (selectedRole == SyncRole.SERVER || syncConfig.role == SyncRole.SERVER) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Devices, contentDescription = null, tint = RetailNavy)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "DAFTAR PERANGKAT KASIR AKTIF REAL-TIME",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = RetailNavy
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = RetailBlueLight,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        "${syncStatus.connectedDevices.size} Perangkat",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RetailNavy
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (syncStatus.connectedDevices.isEmpty()) {
                                Text(
                                    "Belum ada perangkat kasir klien yang terhubung saat ini. Kasir lain dapat menghubungkan terminalnya ke IP ${syncStatus.localIpAddress}:${syncStatus.serverPort}.",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            } else {
                                syncStatus.connectedDevices.forEach { peer ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (peer.pendingCount > 0) RetailRedPrimary.copy(alpha = 0.5f) else PosCardBorder
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        peer.deviceName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.Black
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (peer.role == "SERVER") RetailGreenLight else RetailBlueLight
                                                    ) {
                                                        Text(
                                                            peer.role,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (peer.role == "SERVER") RetailGreen else RetailNavy,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Kasir: ${peer.cashierName} (NIK: ${peer.cashierNik}) • IP: ${peer.ipAddress}",
                                                    fontSize = 10.sp,
                                                    color = DarkText
                                                )
                                                Text(
                                                    "Terakhir aktif: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(peer.lastSeenTimestamp))}",
                                                    fontSize = 9.sp,
                                                    color = GrayText
                                                )
                                            }

                                            // Pending status indicator
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (peer.pendingCount == 0) RetailGreenLight else Color(0xFFFFEBEE),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (peer.pendingCount == 0) RetailGreen else RetailRedPrimary
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        if (peer.pendingCount == 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = if (peer.pendingCount == 0) RetailGreen else RetailRedPrimary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        if (peer.pendingCount == 0) "0 Pending (SIAP EOD)" else "${peer.pendingCount} PENDING!",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (peer.pendingCount == 0) RetailGreen else RetailRedPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Action / Status Banner
                if (actionMessage.isNotEmpty() || syncStatus.lastSyncStatus.isNotEmpty()) {
                    val msg = actionMessage.ifEmpty { syncStatus.lastSyncStatus }
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccessAction) RetailGreenLight else Color(0xFFFFEBEE)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSuccessAction) RetailGreen else RetailRedPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSuccessAction) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccessAction) RetailGreen else RetailRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                msg,
                                color = if (isSuccessAction) RetailGreen else RetailRedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
