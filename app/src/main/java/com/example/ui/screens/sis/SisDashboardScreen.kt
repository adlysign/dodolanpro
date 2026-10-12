package com.example.ui.screens.sis

import androidx.compose.foundation.background
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
    val destination: PosNavDestination
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SisDashboardScreen(
    viewModel: PosViewModel,
    onNavigateTo: (PosNavDestination) -> Unit
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()

    var showLicenseDialog by remember { mutableStateOf(false) }
    var licenseInput by remember { mutableStateOf("") }
    var licenseMessage by remember { mutableStateOf("") }

    // Exactly ordered as requested by user
    val menuItems = listOf(
        SisMenuItem(
            title = "1. Edit Toko",
            subtitle = "Pengaturan informasi toko",
            icon = Icons.Default.Storefront,
            color = Color(0xFF1565C0),
            destination = PosNavDestination.EDIT_TOKO
        ),
        SisMenuItem(
            title = "2. Kasir Pos",
            subtitle = "Layar Transaksi & Penjualan",
            icon = Icons.Default.PointOfSale,
            color = RetailRedPrimary,
            destination = PosNavDestination.KASIR
        ),
        SisMenuItem(
            title = "3. Clerek Setoran",
            subtitle = "Penghitungan Uang & Tutup Kasir",
            icon = Icons.Default.Calculate,
            color = Color(0xFF00897B),
            destination = PosNavDestination.CLEREK
        ),
        SisMenuItem(
            title = "4. Pembelian",
            subtitle = "Pembelian Supplier & Toko",
            icon = Icons.Default.AddShoppingCart,
            color = Color(0xFF43A047),
            destination = PosNavDestination.PEMBELIAN
        ),
        SisMenuItem(
            title = "5. Penjualan",
            subtitle = "Penjualan Toko",
            icon = Icons.Default.ReceiptLong,
            color = Color(0xFF6A1B9A),
            destination = PosNavDestination.PENJUALAN
        ),
        SisMenuItem(
            title = "6. Produk",
            subtitle = "Pengaturan Produk",
            icon = Icons.Default.Category,
            color = Color(0xFFE65100),
            destination = PosNavDestination.PRODUK
        ),
        SisMenuItem(
            title = "7. Cetak Label Harga",
            subtitle = "Price Tag Rak & Barcode",
            icon = Icons.Default.Style,
            color = Color(0xFFF57C00),
            destination = PosNavDestination.CETAK_LABEL_HARGA
        ),
        SisMenuItem(
            title = "8. Stok Opname",
            subtitle = "Penyesuaian Stok",
            icon = Icons.Default.FactCheck,
            color = Color(0xFF00838F),
            destination = PosNavDestination.STOK_OPNAME
        ),
        SisMenuItem(
            title = "9. Retur Pembelian",
            subtitle = "Retur Pembelian Supplier & Toko",
            icon = Icons.Default.AssignmentReturn,
            color = Color(0xFFC62828),
            destination = PosNavDestination.RETUR_PEMBELIAN
        ),
        SisMenuItem(
            title = "10. Tutup Toko (EOD)",
            subtitle = "End Of Day & Backup Data",
            icon = Icons.Default.Nightlife,
            color = Color(0xFF263238),
            destination = PosNavDestination.EOD_TUTUP_TOKO
        ),
        SisMenuItem(
            title = "11. Laporan Toko",
            subtitle = "Laporan performa toko",
            icon = Icons.Default.BarChart,
            color = Color(0xFF2E7D32),
            destination = PosNavDestination.LAPORAN_TOKO
        ),
        SisMenuItem(
            title = "12. Karyawan",
            subtitle = "Data karyawan & Hak Akses",
            icon = Icons.Default.Badge,
            color = Color(0xFF4527A0),
            destination = PosNavDestination.KARYAWAN
        ),
        SisMenuItem(
            title = "13. Voucher & Poin",
            subtitle = "Pengaturan Voucher & Poin",
            icon = Icons.Default.Loyalty,
            color = Color(0xFFD81B60),
            destination = PosNavDestination.VOUCHER_POIN
        ),
        SisMenuItem(
            title = "14. Pengaturan Member",
            subtitle = "Data Member & Saldo Poin",
            icon = Icons.Default.CardMembership,
            color = Color(0xFF00796B),
            destination = PosNavDestination.PENGATURAN_MEMBER
        ),
        SisMenuItem(
            title = "15. Pengaturan Shift",
            subtitle = "Jam Kerja & Modal Awal Kasir",
            icon = Icons.Default.Schedule,
            color = Color(0xFF5D4037),
            destination = PosNavDestination.PENGATURAN_SHIFT
        ),
        SisMenuItem(
            title = "16. Pengaturan Printer",
            subtitle = "Konfigurasi Printer & Desain Struk Custom",
            icon = Icons.Default.Print,
            color = Color(0xFF37474F),
            destination = PosNavDestination.PENGATURAN_PRINTER
        ),
        SisMenuItem(
            title = "17. Backup & Restore",
            subtitle = "Pencadangan & Pemulihan Database",
            icon = Icons.Default.CloudSync,
            color = Color(0xFF0097A7),
            destination = PosNavDestination.BACKUP_RESTORE
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("DODOLANPRO - SISTEM INFORMASI TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${storeConfig.storeName} (${storeConfig.storeCode})", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateTo(PosNavDestination.KASIR) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali ke POS")
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
                .background(PosSurface)
                .padding(12.dp)
        ) {
            // Header: Status Login User & Status Lisensi (Tombol Buka Kasir diganti jadi Status Lisensi yang bisa diklik)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Login User
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("USER BERTUGAS:", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        }
                        Text("${currentCashier?.nama ?: "Administrator"} (${currentCashier?.role ?: "ADMIN"})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        Text("NIK: ${currentCashier?.nik ?: "001"} | Status: Aktif", fontSize = 11.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.lockScreen()
                                    onNavigateTo(PosNavDestination.KASIR)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RetailNavy)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kunci", fontSize = 10.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.logoutCashier()
                                    onNavigateTo(PosNavDestination.KASIR)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Logout", fontSize = 10.sp, color = Color.Black)
                            }
                        }
                    }

                    // Status Lisensi (Clickable to enter license / upgrade license)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (storeConfig.licenseStatus == "AKTIF") RetailGreenLight else RetailAmberLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (storeConfig.licenseStatus == "AKTIF") RetailGreen else RetailAmber),
                        modifier = Modifier.clickable {
                            licenseInput = storeConfig.licenseKey
                            showLicenseDialog = true
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "LISENSI: ${storeConfig.licenseStatus}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (storeConfig.licenseStatus == "AKTIF") RetailGreen else RetailAmber
                                )
                            }
                            Text(storeConfig.licenseTier, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text("Ketuk utk Upgrade 🔑", fontSize = 9.sp, color = RetailNavy)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("MENU OPERASIONAL & KELOLA TOKO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)

            Spacer(modifier = Modifier.height(6.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                            .clickable { onNavigateTo(item.destination) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = item.color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                            }

                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, maxLines = 1)
                                Text(item.subtitle, fontSize = 10.sp, color = Color.DarkGray, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Input Lisensi & Upgrade Lisensi
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = {
                showLicenseDialog = false
                licenseMessage = ""
            },
            title = { Text("AKTIVASI & UPGRADE LISENSI", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Status Lisensi Saat Ini: ${storeConfig.licenseTier} (${storeConfig.licenseStatus})",
                        fontWeight = FontWeight.Bold,
                        color = RetailGreen,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Kunci Aktif: ${storeConfig.licenseKey}", fontSize = 11.sp, color = GrayText)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Masukkan Serial Key / Kunci Lisensi Baru:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    OutlinedTextField(
                        value = licenseInput,
                        onValueChange = { licenseInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: DODOLAN-PRO-XXXX-XXXX", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = PosCardBorder
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { licenseInput = "DODOLAN-PRO-PRO-TIER" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("Key Pro", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = { licenseInput = "DODOLAN-PRO-ENTERPRISE-LIFETIME" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text("Key Enterprise", fontSize = 10.sp)
                        }
                    }

                    if (licenseMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(licenseMessage, fontSize = 12.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.activateOrUpgradeLicense(licenseInput) { ok, msg ->
                            licenseMessage = msg
                            if (ok) {
                                showLicenseDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Text("AKTIFKAN LISENSI", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showLicenseDialog = false
                    licenseMessage = ""
                }) {
                    Text("TUTUP")
                }
            }
        )
    }
}
