package com.example.ui.screens.sis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosViewModel

data class SisMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val destination: PosNavDestination,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SisDashboardScreen(
    viewModel: PosViewModel,
    onNavigateTo: (PosNavDestination) -> Unit
) {
    val context = LocalContext.current
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allReceivables by viewModel.allReceivables.collectAsState()
    val syncConfig by viewModel.syncConfig.collectAsState()

    val currentCashier by viewModel.currentCashier.collectAsState()
    val isOwner = currentCashier?.isOwner() == true

    var showLicenseDialog by remember { mutableStateOf(false) }
    var inputLicenseKey by remember { mutableStateOf("") }
    var licenseDialogMessage by remember { mutableStateOf("") }
    var isLicenseSuccess by remember { mutableStateOf(false) }

    var showAccessDeniedDialog by remember { mutableStateOf(false) }
    var deniedMenuTitle by remember { mutableStateOf("") }

    val deviceId = viewModel.deviceId

    // Kembali ke kasir jika tombol back sistem ditekan
    BackHandler {
        onNavigateTo(PosNavDestination.KASIR)
    }

    // Menu list: Jika paket PRO aktif, menu sinkronisasi multi-device TIDAK dimunculkan
    val menuItems = remember(storeConfig.licenseType, storeConfig.isLicensed, allTransactions.size, allReceivables.size) {
        val unpaidCount = allReceivables.count { !it.status.equals("LUNAS", ignoreCase = true) }
        val list = mutableListOf(
            SisMenuItem("Kasir POS", "Buka Mesin Kasir & Transaksi", Icons.Default.PointOfSale, RetailRedPrimary, PosNavDestination.KASIR, "menu_kasir"),
            SisMenuItem("Produk & Harga", "Master Barang, Repack & Promo", Icons.Default.Inventory2, RetailNavy, PosNavDestination.PRODUK, "menu_produk"),
            SisMenuItem("Cetak Label Rak", "Label Harga A4/F4 & Rak", Icons.Default.QrCode2, Color(0xFF00897B), PosNavDestination.CETAK_LABEL_HARGA, "menu_cetak_label"),
            SisMenuItem("Riwayat Penjualan", "${allTransactions.size} Transaksi Tersimpan", Icons.Default.ReceiptLong, Color(0xFF1E88E5), PosNavDestination.PENJUALAN, "menu_penjualan"),
            SisMenuItem(
                "Piutang Pelanggan",
                if (unpaidCount > 0) "$unpaidCount Tagihan Belum Lunas" else "Kasbon, Cicilan & Jatuh Tempo",
                Icons.Default.CreditScore,
                Color(0xFFE65100),
                PosNavDestination.PIUTANG,
                "menu_piutang"
            ),
            SisMenuItem("Pembelian Barang", "Input Pembelian dari Supplier", Icons.Default.ShoppingBag, Color(0xFFFB8C00), PosNavDestination.PEMBELIAN, "menu_pembelian"),
            SisMenuItem("Retur", "Retur Penjualan & Retur Pembelian", Icons.Default.AssignmentReturn, Color(0xFFE53935), PosNavDestination.RETUR_PEMBELIAN, "menu_retur"),
            SisMenuItem("Stok Opname", "Penyesuaian & Koreksi Fisik", Icons.Default.FactCheck, Color(0xFF6D4C41), PosNavDestination.STOK_OPNAME, "menu_stok_opname"),
            SisMenuItem("Clerek Kasir", "Cek Setoran Kasir & Kas", Icons.Default.AccountBalanceWallet, Color(0xFF43A047), PosNavDestination.CLEREK, "menu_clerek"),
            SisMenuItem("Laporan Lengkap", "Laporan Kasir, Harian & SIS", Icons.Default.BarChart, Color(0xFF3949AB), PosNavDestination.LAPORAN_TOKO, "menu_laporan"),
            SisMenuItem("Tutup Toko (EOD)", "End of Day & Rekap Harian", Icons.Default.LockClock, Color(0xFFD81B60), PosNavDestination.EOD_TUTUP_TOKO, "menu_eod"),
            SisMenuItem("Member & Pelanggan", "Data Pelanggan & Reward", Icons.Default.CardMembership, Color(0xFF00ACC1), PosNavDestination.PENGATURAN_MEMBER, "menu_member"),
            SisMenuItem("Voucher & Poin", "Kupon Diskon & Poin Belanja", Icons.Default.ConfirmationNumber, Color(0xFF8E24AA), PosNavDestination.VOUCHER_POIN, "menu_voucher"),
            SisMenuItem("Karyawan & Kasir", "Hak Akses & Petugas Toko", Icons.Default.Badge, Color(0xFF546E7A), PosNavDestination.KARYAWAN, "menu_karyawan"),
            SisMenuItem("Pengaturan Shift", "Jam Kerja & Shift Kasir", Icons.Default.Schedule, Color(0xFF5E35B1), PosNavDestination.PENGATURAN_SHIFT, "menu_shift"),
            SisMenuItem("Printer & Struk", "Format Struk & Koneksi Printer", Icons.Default.Print, Color(0xFF7CB342), PosNavDestination.PENGATURAN_PRINTER, "menu_printer"),
            SisMenuItem("Profil Toko", "Nama, Alamat & Kode Toko", Icons.Default.Storefront, Color(0xFF039BE5), PosNavDestination.EDIT_TOKO, "menu_profil_toko"),
            SisMenuItem("Backup & Restore", "Cadangkan & Pulihkan Database", Icons.Default.Backup, Color(0xFF455A64), PosNavDestination.BACKUP_RESTORE, "menu_backup")
        )

        // SINKRONISASI MULTI-DEVICE:
        // Muncul pada mode TRIAL atau ENTERPRISE.
        // Jika paket PRO diaktifkan, menu ini TIDAK AKAN MUNCUL karena paket PRO khusus single-device mode.
        if (!storeConfig.isPro) {
            list.add(
                SisMenuItem("Sinkron Multi-Device", "Server Master & Kasir Klien Real-Time", Icons.Default.SyncAlt, Color(0xFF00897B), PosNavDestination.SINKRONISASI_MULTI_DEVICE, "menu_sinkronisasi")
            )
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SIS DASHBOARD TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "${storeConfig.storeName} (${storeConfig.storeCode}) • ${currentCashier?.nama ?: "Kasir"} [${currentCashier?.role ?: "KASIR"}]",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    // Badge ID Device & Status Lisensi (digeser ke kanan rapi & profesional)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            storeConfig.isEnterprise -> Color(0xFF0F5132) // Emerald Green Elegan Enterprise
                            storeConfig.isPro -> Color(0xFF0D47A1)        // Royal Blue Modern Pro
                            else -> Color(0xFF881337)                     // Deep Burgundy Crimson Trial
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when {
                                storeConfig.isEnterprise -> Color(0xFFFFD54F)
                                storeConfig.isPro -> Color(0xFF90CAF9)
                                else -> Color(0xFFFCA5A5)
                            }
                        ),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                inputLicenseKey = storeConfig.licenseKey
                                licenseDialogMessage = ""
                                showLicenseDialog = true
                            }
                            .testTag("btn_device_license_status")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when {
                                    storeConfig.isEnterprise -> Icons.Default.WorkspacePremium
                                    storeConfig.isPro -> Icons.Default.Verified
                                    else -> Icons.Default.HourglassTop
                                },
                                contentDescription = "Status Lisensi",
                                tint = when {
                                    storeConfig.isEnterprise -> Color(0xFFFFD54F)
                                    storeConfig.isPro -> Color(0xFFBBDEFB)
                                    else -> Color(0xFFFFEBEE)
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "ID: $deviceId",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (isOwner) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Ubah Lisensi (Owner)",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = "Lihat Status Lisensi",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                                Text(
                                    when {
                                        storeConfig.isEnterprise -> "ENTERPRISE LIFETIME"
                                        storeConfig.isPro -> "PRO LIFETIME"
                                        else -> "TRIAL (${allTransactions.size}/50 Tx)"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = when {
                                        storeConfig.isEnterprise -> Color(0xFFFFF59D)
                                        storeConfig.isPro -> Color(0xFFBBDEFB)
                                        else -> Color(0xFFFFCDD2)
                                    }
                                )
                            }
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
                .padding(12.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    val hasAccess = isOwner || viewModel.hasPermissionForDestination(item.destination)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (hasAccess) {
                                    onNavigateTo(item.destination)
                                } else {
                                    deniedMenuTitle = item.title
                                    showAccessDeniedDialog = true
                                }
                            }
                            .testTag(item.testTag),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasAccess) Color.White else Color(0xFFF8FAFC)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (hasAccess) 2.dp else 0.5.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = if (!hasAccess) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)) else null
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (hasAccess) item.color.copy(alpha = 0.12f) else Color(0xFFE2E8F0),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            item.icon,
                                            contentDescription = null,
                                            tint = if (hasAccess) item.color else Color(0xFF94A3B8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (hasAccess) Color.Black else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (hasAccess) item.subtitle else "Akses Dibatasi - Butuh Wewenang",
                                    fontSize = 10.sp,
                                    color = if (hasAccess) Color.Gray else RetailRedPrimary,
                                    maxLines = 1
                                )
                            }

                            // Lock badge overlay untuk menu yang tidak memiliki izin akses
                            if (!hasAccess) {
                                Surface(
                                    shape = RoundedCornerShape(topEnd = 10.dp, bottomStart = 8.dp),
                                    color = Color(0xFFFFEBEE),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFCDD2)),
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Terkunci",
                                            tint = RetailRedPrimary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            "Terkunci",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RetailRedPrimary
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

    // DIALOG AKSES MENU DITOLAK
    if (showAccessDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showAccessDeniedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = RetailRedPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Akses Menu Ditolak",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Akun Anda (${currentCashier?.nama ?: "Kasir"} [${currentCashier?.role ?: "KASIR"}]) tidak memiliki izin hak akses untuk membuka menu '${deniedMenuTitle}'.",
                        fontSize = 13.sp,
                        color = DarkText
                    )
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Wewenang kasir dapat dikonfigurasi oleh Owner atau Administrator pada menu 'Karyawan & Kasir'.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAccessDeniedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                ) {
                    Text("MENGERTI")
                }
            }
        )
    }

    // DIALOG PENGATURAN & AKTIVASI LISENSI
    // HANYA OWNER YANG DAPAT MENGUBAH / MENGAKTIFKAN LISENSI.
    // PENGGUNA SELAIN OWNER HANYA BISA MELIHAT STATUS SAJA (READ-ONLY).
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isOwner) Icons.Default.Key else Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = RetailNavy
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isOwner) "AKTIVASI & LISENSI APLIKASI (OWNER)" else "STATUS LISENSI APLIKASI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    // Box Informasi ID Device Perangkat Ini (Dapat disalin oleh siapa pun untuk diserahkan ke Owner)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ID DEVICE PERANGKAT INI:", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    deviceId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = RetailNavy
                                )
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", deviceId))
                                        Toast.makeText(context, "ID Device disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin ID", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Lisensi Saat Ini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Lisensi:", fontSize = 11.sp, color = DarkText)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                storeConfig.isEnterprise -> Color(0xFFE8F5E9)
                                storeConfig.isPro -> Color(0xFFE3F2FD)
                                else -> Color(0xFFFFEBEE)
                            }
                        ) {
                            Text(
                                when {
                                    storeConfig.isEnterprise -> "ENTERPRISE LIFETIME"
                                    storeConfig.isPro -> "PRO LIFETIME"
                                    else -> "TRIAL (${allTransactions.size}/50 Transaksi)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = when {
                                    storeConfig.isEnterprise -> Color(0xFF2E7D32)
                                    storeConfig.isPro -> Color(0xFF1565C0)
                                    else -> Color(0xFFC62828)
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Informasi Paket
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = RetailBlueLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("PILIHAN PAKET LISENSI:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = RetailNavy)
                            Text("• TRIAL: Kuota maksimal 50 transaksi pertama untuk uji coba fitur.", fontSize = 10.sp, color = DarkText)
                            Text("• PRO LIFETIME: Transaksi tanpa batas untuk kasir single-device offline.", fontSize = 10.sp, color = DarkText)
                            Text("• ENTERPRISE: Transaksi tanpa batas & Sinkronisasi real-time multi-device.", fontSize = 10.sp, color = DarkText)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isOwner) {
                        // OWNER VIEW: Input field dan panduan aktivasi
                        Text(
                            "Akun Anda terverifikasi sebagai OWNER. Anda berwenang memasukkan kode lisensi atau mereset status lisensi aplikasi.",
                            fontSize = 11.sp,
                            color = RetailNavy,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = inputLicenseKey,
                            onValueChange = { inputLicenseKey = it.uppercase() },
                            label = { Text("Masukkan Kode Lisensi") },
                            placeholder = { Text("PRO-... atau ENT-...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_license_key")
                        )

                        if (licenseDialogMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                licenseDialogMessage,
                                color = if (isLicenseSuccess) RetailGreen else RetailRedPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // NON-OWNER VIEW: Read-only pemberitahuan wewenang terbatas
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFF57F17),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "HAK AKSES LISENSI KHUSUS OWNER",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Hanya akun OWNER yang dapat mengubah, mengaktifkan, atau mereset lisensi toko. Akun Anda (${currentCashier?.nama ?: "Kasir"} [${currentCashier?.role ?: "KASIR"}]) hanya diizinkan melihat status lisensi.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF5D4037)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isOwner) {
                    Button(
                        onClick = {
                            if (inputLicenseKey.isBlank()) {
                                licenseDialogMessage = "Masukkan kode lisensi terlebih dahulu."
                                isLicenseSuccess = false
                                return@Button
                            }
                            viewModel.activateOrUpgradeLicense(inputLicenseKey) { ok, msg ->
                                isLicenseSuccess = ok
                                licenseDialogMessage = msg
                                if (ok) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                    ) {
                        Text("AKTIFKAN LISENSI")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Tombol Reset ke Trial hanya muncul untuk OWNER saat lisensi aktif
                    if (isOwner && storeConfig.isLicensed) {
                        TextButton(
                            onClick = {
                                viewModel.resetLicenseToTrial {
                                    inputLicenseKey = ""
                                    isLicenseSuccess = true
                                    licenseDialogMessage = "Status lisensi direset kembali ke TRIAL."
                                }
                            }
                        ) {
                            Text("RESET TRIAL", color = RetailRedPrimary, fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(onClick = { showLicenseDialog = false }) {
                        Text("TUTUP")
                    }
                }
            }
        )
    }
}
