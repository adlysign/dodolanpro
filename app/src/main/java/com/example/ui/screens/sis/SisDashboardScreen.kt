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
import androidx.compose.ui.platform.testTag
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
    val storeConfig by viewModel.storeConfig.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val menuItems = listOf(
        SisMenuItem("Kasir POS", "Buka Mesin Kasir & Transaksi", Icons.Default.PointOfSale, RetailRedPrimary, PosNavDestination.KASIR, "menu_kasir"),
        SisMenuItem("Produk & Harga", "Master Barang, Repack & Promo", Icons.Default.Inventory2, RetailNavy, PosNavDestination.PRODUK, "menu_produk"),
        SisMenuItem("Cetak Label Rak", "Label Harga A4/F4 & Rak", Icons.Default.QrCode2, Color(0xFF00897B), PosNavDestination.CETAK_LABEL_HARGA, "menu_cetak_label"),
        SisMenuItem("Riwayat Penjualan", "${allTransactions.size} Transaksi Tersimpan", Icons.Default.ReceiptLong, Color(0xFF1E88E5), PosNavDestination.PENJUALAN, "menu_penjualan"),
        SisMenuItem("Pembelian Barang", "Input Pembelian dari Supplier", Icons.Default.ShoppingBag, Color(0xFFFB8C00), PosNavDestination.PEMBELIAN, "menu_pembelian"),
        SisMenuItem("Retur Pembelian", "Pengembalian Barang ke Supplier", Icons.Default.AssignmentReturn, Color(0xFFE53935), PosNavDestination.RETUR_PEMBELIAN, "menu_retur_pembelian"),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SIS DASHBOARD TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${storeConfig.storeName} (${storeConfig.storeCode})", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                actions = {
                    Button(
                        onClick = { onNavigateTo(PosNavDestination.KASIR) },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("btn_back_to_pos")
                    ) {
                        Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("KASIR (F10)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateTo(item.destination) }
                            .testTag(item.testTag),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = item.color.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(item.subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
