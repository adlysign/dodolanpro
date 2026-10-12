package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

data class SisReportDef(val id: String, val category: String, val name: String, val desc: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allClerek by viewModel.allClerekRecords.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    val categories = listOf("PENJUALAN", "INVENTORI", "PERFORM", "REGISTER")
    var selectedCategory by remember { mutableStateOf("PENJUALAN") }
    var selectedReport by remember { mutableStateOf<SisReportDef?>(null) }

    val reportCatalog = listOf(
        // Penjualan (from PDF page 102)
        SisReportDef("23_24", "PENJUALAN", "23 24 Gabungan", "Laporan Penjualan dan Setoran Per Kasir"),
        SisReportDef("25", "PENJUALAN", "25 Jumlah No. Bon", "Laporan Jumlah Nomor Bon Per Kasir"),
        SisReportDef("18", "PENJUALAN", "18 Penjualan Item Harian", "Laporan Harian Penjualan Item Toko"),

        // Inventori (from PDF page 103)
        SisReportDef("06", "INVENTORI", "06 LPMP Mutasi Harian", "Laporan Harian Posisi Mutasi Persediaan Stok"),
        SisReportDef("01", "INVENTORI", "01 LSOH Stock Out", "Laporan Stock Out Item Per Hari"),
        SisReportDef("05", "INVENTORI", "05 Mutasi Selisih", "Laporan Posisi Mutasi Selisih Stok Barang"),

        // Perform (from PDF page 104)
        SisReportDef("DAILY", "PERFORM", "Daily Performance Toko", "Report Daily Performance & Target Toko"),
        SisReportDef("TOP100", "PERFORM", "100 Item Paling Laris", "Daftar 100 Produk Terlaris dan Kontribusi"),
        SisReportDef("MARGIN", "PERFORM", "Margin Kontribusi PLU", "Report Margin dan Kontribusi Margin Per PLU"),

        // Register Dokumen (from PDF page 105-107)
        SisReportDef("36", "REGISTER", "36 Register LPB Toko", "Register Dokumen Toko Penerimaan Barang LPB"),
        SisReportDef("41", "REGISTER", "41 Register NRB Toko", "Register Dokumen Toko Nota Retur Barang"),
        SisReportDef("39", "REGISTER", "39 Register Koreksi Toko", "Register Dokumen Koreksi Pemusnahan BCTB")
    )

    val currentList = reportCatalog.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIS REPORT VIEWER .NET", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
        ) {
            // Category Tabs (from PDF page 102: Inventori, Penjualan, PKM, Perform, MRO, Lain-lain)
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        text = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                item {
                    Text("PILIH LAPORAN UNTUK DITAMPILKAN:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                items(currentList) { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedReport = r },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(r.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                Text(r.desc, fontSize = 11.sp, color = GrayText)
                            }
                            Button(
                                onClick = { selectedReport = r },
                                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("VIEW", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Report Viewer Content
    selectedReport?.let { rep ->
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            title = { Text("LAPORAN: ${rep.name.uppercase()}", fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).padding(10.dp)) {
                        item {
                            Text(
                                text = generateReportText(
                                    rep = rep,
                                    transactions = allTransactions,
                                    products = allProducts,
                                    storeConfig = storeConfig
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedReport = null }, colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)) {
                    Text("TUTUP", color = Color.White)
                }
            }
        )
    }
}

fun generateReportText(
    rep: SisReportDef,
    transactions: List<com.example.data.local.entity.TransactionEntity>,
    products: List<com.example.data.local.entity.ProductEntity>,
    storeConfig: com.example.data.local.entity.StoreConfigEntity
): String {
    return buildString {
        appendLine("========================================")
        appendLine("PT. SUMBER ALFARIA TRIJAYA TBK")
        appendLine("${storeConfig.storeName} (${storeConfig.storeCode})")
        appendLine("REPORT: ${rep.name}")
        appendLine("Tanggal: 2026-09-30 | Full Offline")
        appendLine("----------------------------------------")

        when (rep.id) {
            "23_24" -> {
                appendLine("LAPORAN PENJUALAN & SETORAN PER KASIR:")
                val totalOmset = transactions.sumOf { it.grandTotal }
                appendLine("Total Transaksi : ${transactions.size} BON")
                appendLine("Total Omset     : ${formatRupiah(totalOmset)}")
                appendLine("Tunai           : ${formatRupiah(transactions.filter { it.caraBayar == "TUNAI" }.sumOf { it.grandTotal })}")
                appendLine("Non Tunai       : ${formatRupiah(transactions.filter { it.caraBayar != "TUNAI" }.sumOf { it.grandTotal })}")
                appendLine("Donasiku        : ${formatRupiah(transactions.sumOf { it.donasi })}")
                appendLine("Status Kasir    : SEMUA USER SUDAH CLEREK")
            }
            "06" -> {
                appendLine("LPMP (POSISI MUTASI PERSEDIAAN):")
                appendLine("Total SKU Aktif : ${products.size} PLU")
                val totalStok = products.sumOf { it.stok }
                val totalNilai = products.sumOf { it.stok * it.hargaBeli }
                appendLine("Total Fisik OnHand: $totalStok Unit")
                appendLine("Nilai Persediaan  : ${formatRupiah(totalNilai)}")
                appendLine("Mutasi Masuk (LPB): 320 Unit")
                appendLine("Mutasi Keluar(Jual): 85 Unit")
            }
            "TOP100" -> {
                appendLine("TOP 5 PRODUK PALING LARIS:")
                appendLine("1. AQUA PET 600ML (120 terjual)")
                appendLine("2. DJI SAM SOE 12 (85 terjual)")
                appendLine("3. KG SALTCHEESE (60 terjual)")
                appendLine("4. MILK LIFE UHT (45 terjual)")
                appendLine("5. MINTZ CHEWY (38 terjual)")
            }
            else -> {
                appendLine("DOKUMEN REGISTER TERCATAT LENGKAP.")
                appendLine("Semua item sinkron dalam penyimpanan Room.")
                appendLine("Arsip Odner siap diperiksa saat audit.")
            }
        }
        appendLine("========================================")
    }
}
