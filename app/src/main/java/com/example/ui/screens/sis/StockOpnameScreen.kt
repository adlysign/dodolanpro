package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockOpnameEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allStockOpnames by viewModel.allStockOpnames.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Jalankan SO, 1: Riwayat SO
    var selectedRakFilter by remember { mutableStateOf("Semua") }
    var onlyShowDiff by remember { mutableStateOf(false) } // Filter: produk yang sudah pas tidak ditampilkan lagi
    var feedbackMsg by remember { mutableStateOf("") }
    var selectedSoForDoc by remember { mutableStateOf<StockOpnameEntity?>(null) }
    var docSavedMessage by remember { mutableStateOf("") }

    val racks = remember(allProducts) {
        listOf("Semua") + allProducts.map { it.rakPlanogram }.distinct().sorted()
    }

    // Physical count state map: PLU -> count
    val physicalCounts = remember { mutableStateMapOf<String, Int>() }

    // Initialize physical counts with current system stock if empty
    LaunchedEffect(allProducts) {
        allProducts.forEach { p ->
            if (!physicalCounts.containsKey(p.plu)) {
                physicalCounts[p.plu] = p.stok
            }
        }
    }

    val productsInFilter = remember(allProducts, selectedRakFilter) {
        allProducts.filter { selectedRakFilter == "Semua" || it.rakPlanogram.equals(selectedRakFilter, ignoreCase = true) }
    }

    // Filter: if onlyShowDiff is true, show ONLY items where physical != system
    val displayedProducts = remember(productsInFilter, onlyShowDiff, physicalCounts.toMap()) {
        if (onlyShowDiff) {
            productsInFilter.filter { (physicalCounts[it.plu] ?: it.stok) != it.stok }
        } else {
            productsInFilter
        }
    }

    val totalSelisihCount = remember(productsInFilter, physicalCounts.toMap()) {
        productsInFilter.count { (physicalCounts[it.plu] ?: it.stok) != it.stok }
    }

    val totalFisikInFilter = remember(productsInFilter, physicalCounts.toMap()) {
        productsInFilter.sumOf { physicalCounts[it.plu] ?: it.stok }
    }

    val totalSistemInFilter = remember(productsInFilter) {
        productsInFilter.sumOf { it.stok }
    }

    val totalSelisihQty = totalFisikInFilter - totalSistemInFilter

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("STOK OPNAME (PENYESUAIAN STOK)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("PROSES STOK OPNAME") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("RIWAYAT SO") })
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Filter bar: Pilih Semua atau Per Rak
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("1. Cakupan Rak / Planogram:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(4.dp))
                            ScrollableTabRow(
                                selectedTabIndex = racks.indexOf(selectedRakFilter).coerceAtLeast(0),
                                edgePadding = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                racks.forEach { rk ->
                                    Tab(
                                        selected = selectedRakFilter == rk,
                                        onClick = { selectedRakFilter = rk },
                                        text = { Text(rk, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ringkasan KPI Jumlah Fisik & Sistem
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TOTAL SISTEM", fontSize = 9.5.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                                Text("$totalSistemInFilter pcs", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE0E0E0)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("JUMLAH FISIK", fontSize = 9.5.sp, color = RetailNavy, fontWeight = FontWeight.Bold)
                                Text("$totalFisikInFilter pcs", fontSize = 14.sp, fontWeight = FontWeight.Black, color = RetailNavy)
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFFE0E0E0)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("SELISIH FISIK", fontSize = 9.5.sp, color = if (totalSelisihQty == 0) RetailGreen else RetailRedPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (totalSelisihQty > 0) "+$totalSelisihQty pcs" else "$totalSelisihQty pcs",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalSelisihQty == 0) RetailGreen else RetailRedPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons: "Cek Selisih" vs "Tampilkan Semua"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onlyShowDiff = !onlyShowDiff },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (onlyShowDiff) RetailRedDark else RetailNavy,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (onlyShowDiff) "TAMPILKAN SEMUA" else "CEK SELISIH ($totalSelisihCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Text(
                            if (onlyShowDiff) "Menampilkan $totalSelisihCount item selisih" else "${productsInFilter.size} item dalam cakupan",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Table
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(PosSurfaceVariant).padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRODUK / RAK", modifier = Modifier.weight(2.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("SISTEM", modifier = Modifier.weight(1.0f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text("JUMLAH FISIK", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text("SELISIH", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = Color.Black)
                            }

                            if (displayedProducts.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        if (onlyShowDiff) "Semua produk sudah pas! Tidak ada selisih stok." else "Tidak ada produk.",
                                        color = RetailGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(displayedProducts) { prod ->
                                        val physical = physicalCounts[prod.plu] ?: prod.stok
                                        val selisih = physical - prod.stok

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (selisih != 0) Color(0xFFFFF3E0) else Color.Transparent)
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.6f)) {
                                                Text(prod.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                                Text("PLU: ${prod.plu} | ${prod.rakPlanogram}", fontSize = 10.sp, color = GrayText)
                                            }

                                            Text(
                                                "${prod.stok}",
                                                modifier = Modifier.weight(1.0f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                color = Color.Black
                                            )

                                            // Count Input & Stepper (Tampil Sempurna, Jelas, & Tidak Terpotong)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFF8FAFC),
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (selisih != 0) Color(0xFFFF9800) else Color(0xFFCFD8DC)
                                                ),
                                                modifier = Modifier
                                                    .weight(2.2f)
                                                    .height(38.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Tombol Minus [-]
                                                    Surface(
                                                        shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp),
                                                        color = Color(0xFFECEFF1),
                                                        modifier = Modifier
                                                            .width(26.dp)
                                                            .fillMaxHeight()
                                                            .clickable {
                                                                val curr = physicalCounts[prod.plu] ?: prod.stok
                                                                if (curr > 0) physicalCounts[prod.plu] = curr - 1
                                                            }
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.Remove,
                                                                contentDescription = "Kurang Fisik",
                                                                modifier = Modifier.size(13.dp),
                                                                tint = Color.Black
                                                            )
                                                        }
                                                    }

                                                    // Input Angka Langsung (BasicTextField, tidak terpotong)
                                                    var textInput by remember(physical) { mutableStateOf(physical.toString()) }
                                                    BasicTextField(
                                                        value = textInput,
                                                        onValueChange = { newStr ->
                                                            val clean = newStr.filter { it.isDigit() }
                                                            textInput = clean
                                                            if (clean.isNotEmpty()) {
                                                                val num = clean.toIntOrNull() ?: 0
                                                                physicalCounts[prod.plu] = num
                                                            } else {
                                                                physicalCounts[prod.plu] = 0
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 2.dp),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        textStyle = TextStyle(
                                                            textAlign = TextAlign.Center,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = if (selisih != 0) Color(0xFFE65100) else Color.Black
                                                        )
                                                    )

                                                    // Tombol Plus [+]
                                                    Surface(
                                                        shape = RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp),
                                                        color = RetailNavy,
                                                        modifier = Modifier
                                                            .width(26.dp)
                                                            .fillMaxHeight()
                                                            .clickable {
                                                                val curr = physicalCounts[prod.plu] ?: prod.stok
                                                                physicalCounts[prod.plu] = curr + 1
                                                            }
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = "Tambah Fisik",
                                                                modifier = Modifier.size(13.dp),
                                                                tint = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Text(
                                                if (selisih > 0) "+$selisih" else "$selisih",
                                                modifier = Modifier.weight(1.2f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End,
                                                color = if (selisih == 0) RetailGreen else if (selisih > 0) RetailNavy else Color.Black
                                            )
                                        }
                                        HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    if (feedbackMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(feedbackMsg, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button: Simpan & Terapkan Penyesuaian Stok
                    Button(
                        onClick = {
                            val soCode = "SO-" + SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) + "-" + (100..999).random()
                            val adjustments = physicalCounts.toMap()
                            viewModel.recordStockOpname(
                                soNo = soCode,
                                rakFilter = selectedRakFilter,
                                adjustments = adjustments
                            ) {
                                feedbackMsg = "Stok Opname $soCode berhasil disimpan! Stok sistem disesuaikan."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENYESUAIAN STOK (APPLY)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Tab 1: Riwayat Stok Opname (Dapat dicetak & disimpan sebagai dokumen)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    item {
                        Text("RIWAYAT DOKUMEN STOK OPNAME:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (allStockOpnames.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada riwayat Stok Opname.", color = GrayText)
                            }
                        }
                    } else {
                        items(allStockOpnames) { so ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(so.soNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                        Text(so.tanggal, fontSize = 11.sp, color = GrayText)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Cakupan: ${so.rakFilter} | Petugas: ${so.petugasNama}", fontSize = 12.sp, color = Color.Black)
                                    if (so.keterangan.isNotEmpty()) {
                                        Text(so.keterangan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailNavy)
                                    } else if (so.totalSelisihFisik > 0) {
                                        Text("Jumlah Fisik: ${so.totalSelisihFisik} pcs", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailNavy)
                                    }
                                    Text(
                                        "SKU: ${so.totalItemDihitung} | Selisih: ${so.totalSelisihItem} item (${formatRupiah(so.totalSelisihNilai)})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (so.totalSelisihItem == 0) RetailGreen else Color.Black
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = { selectedSoForDoc = so },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cetak / Simpan Dokumen", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Export Document to DodolanPro/StokOpname
    selectedSoForDoc?.let { so ->
        val soDocText = buildString {
            appendLine("========================================")
            appendLine("       BERITA ACARA STOK OPNAME         ")
            appendLine("----------------------------------------")
            appendLine("Nomor SO : ${so.soNo}")
            appendLine("Tanggal  : ${so.tanggal}")
            appendLine("Cakupan  : ${so.rakFilter}")
            appendLine("Petugas  : ${so.petugasNama}")
            appendLine("----------------------------------------")
            appendLine("Total SKU Dihitung : ${so.totalItemDihitung} SKU")
            if (so.totalSelisihFisik > 0) {
                appendLine("Total Jumlah Fisik : ${so.totalSelisihFisik} pcs")
            }
            if (so.keterangan.isNotEmpty()) {
                appendLine("Detail Fisik/Sistem: ${so.keterangan}")
            }
            appendLine("Total Selisih Item : ${so.totalSelisihItem} pcs")
            appendLine("Total Nilai Selisih: ${formatRupiah(so.totalSelisihNilai)}")
            appendLine("----------------------------------------")
            appendLine("Status: STOK FISIK TELAH DISINKRONKAN")
            appendLine("Arsip: DodolanPro/StokOpname")
            appendLine("========================================")
        }

        AlertDialog(
            onDismissRequest = {
                selectedSoForDoc = null
                docSavedMessage = ""
            },
            title = { Text("DOKUMEN STOK OPNAME", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                    ) {
                        Text(
                            text = soDocText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp),
                            color = Color.Black
                        )
                    }
                    if (docSavedMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(docSavedMessage, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val saved = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "StokOpname",
                                prefix = "SO_${so.soNo}",
                                extension = "txt",
                                content = soDocText
                            )
                            docSavedMessage = "Tersimpan di DodolanPro/StokOpname/${saved.second.name}"
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN FILE", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            selectedSoForDoc = null
                            docSavedMessage = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                    ) {
                        Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}
