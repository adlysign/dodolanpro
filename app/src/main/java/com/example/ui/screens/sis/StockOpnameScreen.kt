package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockOpnameEntity
import com.example.ui.screens.pos.CameraBarcodeScannerDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockOpnameScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allStockOpnames by viewModel.allStockOpnames.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Jalankan SO, 1: Riwayat SO
    var selectedRakFilter by remember { mutableStateOf("Semua") }
    var onlyShowDiff by remember { mutableStateOf(false) } // Filter: hanya tampilkan yang ada selisih
    var feedbackMsg by remember { mutableStateOf("") }
    var selectedSoForDoc by remember { mutableStateOf<StockOpnameEntity?>(null) }
    var docSavedMessage by remember { mutableStateOf("") }

    // Fitur Scan & Pencarian Cepat
    var showCameraScanner by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var lastScannedPlu by remember { mutableStateOf<String?>(null) }
    var fastScanMode by remember { mutableStateOf(false) } // Mode cepat: auto +1 setiap kali scan barcode
    var scanNotification by remember { mutableStateOf<String?>(null) }

    // Daftar PLU yang baru saja disentuh/di-scan/diedit agar selalu berada di urutan teratas
    val recentTouchedPlus = remember { mutableStateListOf<String>() }

    val racks = remember(allProducts) {
        listOf("Semua") + allProducts.map { it.rakPlanogram }.distinct().sorted()
    }

    // Physical count state map: PLU -> count
    val physicalCounts = remember { mutableStateMapOf<String, Int>() }

    // Inisialisasi hitungan fisik sesuai stok sistem saat pertama kali dimuat
    LaunchedEffect(allProducts) {
        allProducts.forEach { p ->
            if (!physicalCounts.containsKey(p.plu)) {
                physicalCounts[p.plu] = p.stok
            }
        }
    }

    // Fungsi pemroses scan barcode (baik dari kamera maupun input teks)
    fun processScannedBarcode(rawBarcode: String) {
        val clean = rawBarcode.trim()
        if (clean.isEmpty()) return

        val matched = allProducts.find { p ->
            p.barcode.equals(clean, ignoreCase = true) ||
            p.plu.equals(clean, ignoreCase = true) ||
            p.barcodeRepack.equals(clean, ignoreCase = true) ||
            p.barcodeRepack2.equals(clean, ignoreCase = true) ||
            p.nama.contains(clean, ignoreCase = true)
        }

        if (matched != null) {
            // Jika filter rak aktif dan produk ada di rak lain, alihkan ke Semua agar produk langsung terlihat
            if (selectedRakFilter != "Semua" && !matched.rakPlanogram.equals(selectedRakFilter, ignoreCase = true)) {
                selectedRakFilter = "Semua"
            }

            lastScannedPlu = matched.plu
            recentTouchedPlus.remove(matched.plu)
            recentTouchedPlus.add(0, matched.plu)

            if (fastScanMode) {
                val currentCount = physicalCounts[matched.plu] ?: matched.stok
                val newCount = currentCount + 1
                physicalCounts[matched.plu] = newCount
                val diff = newCount - matched.stok
                val diffText = if (diff > 0) "+$diff" else "$diff"
                scanNotification = "✓ Scan Berhasil: ${matched.nama} (+1). Fisik: $newCount pcs (Selisih: $diffText)"
            } else {
                val currentCount = physicalCounts[matched.plu] ?: matched.stok
                val diff = currentCount - matched.stok
                val diffText = if (diff == 0) "Pas" else if (diff > 0) "+$diff" else "$diff"
                scanNotification = "Produk ditemukan di depan mata: ${matched.nama} (Fisik: $currentCount, Selisih: $diffText)"
            }
            searchQuery = "" // reset search text agar tidak mempersempit daftar
            focusManager.clearFocus()
        } else {
            scanNotification = "Barcode / Kode '$clean' tidak ditemukan dalam database produk."
        }
    }

    val productsInFilter = remember(allProducts, selectedRakFilter) {
        allProducts.filter { selectedRakFilter == "Semua" || it.rakPlanogram.equals(selectedRakFilter, ignoreCase = true) }
    }

    // Filtering & Smart Sorting (Produk yang baru di-scan / disentuh selalu naik ke urutan teratas)
    val displayedProducts = remember(productsInFilter, onlyShowDiff, searchQuery, physicalCounts.toMap(), recentTouchedPlus.toList()) {
        val filtered = productsInFilter.filter { p ->
            val matchDiff = if (onlyShowDiff) {
                (physicalCounts[p.plu] ?: p.stok) != p.stok
            } else true

            val matchQuery = if (searchQuery.isBlank()) true else {
                p.nama.contains(searchQuery, ignoreCase = true) ||
                p.plu.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.barcodeRepack.contains(searchQuery, ignoreCase = true) ||
                p.barcodeRepack2.contains(searchQuery, ignoreCase = true)
            }
            matchDiff && matchQuery
        }

        // Urutkan: Produk yang baru di-scan / disentuh muncul di urutan paling atas!
        filtered.sortedWith(
            compareBy<ProductEntity> { p ->
                val idx = recentTouchedPlus.indexOf(p.plu)
                if (idx >= 0) idx else Int.MAX_VALUE
            }.thenBy { it.rakPlanogram }.thenBy { it.nama }
        )
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

    val lastScannedProduct = remember(allProducts, lastScannedPlu) {
        allProducts.find { it.plu == lastScannedPlu }
    }

    // Dialog Kamera Barcode Scanner
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScanner = false },
            onBarcodeDetected = { scannedCode ->
                showCameraScanner = false
                processScannedBarcode(scannedCode)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("STOK OPNAME (PENYESUAIAN FISIK)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Scan atau cari produk untuk hitung langsung", fontSize = 11.sp, color = Color.LightGray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Tombol cepat scan kamera di TopBar
                    FilledTonalButton(
                        onClick = { showCameraScanner = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFFFD54F),
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 8.dp).height(34.dp).testTag("btn_topbar_scan_so")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SCAN KAMERA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("PROSES STOK OPNAME", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("RIWAYAT SO", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {

                    // ==========================================
                    // 1. TOOLBAR SCAN BARCODE & PENCARIAN FLEKSIBEL
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text("Scan barcode / ketik nama / PLU...", fontSize = 12.sp, color = GrayText)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = RetailNavy)
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Hapus", tint = Color.Gray)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            processScannedBarcode(searchQuery)
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("input_search_so"),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // Tombol Scan Barcode Kamera Utama
                                Button(
                                    onClick = { showCameraScanner = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(50.dp).testTag("btn_scan_camera_so")
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SCAN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pilihan Mode Scan Cepat (+1 per scan) & info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { fastScanMode = !fastScanMode }
                                ) {
                                    Checkbox(
                                        checked = fastScanMode,
                                        onCheckedChange = { fastScanMode = it },
                                        modifier = Modifier.size(24.dp).testTag("switch_fast_scan_mode")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Mode Scan Cepat (Auto +1 tiap scan)",
                                        fontSize = 11.sp,
                                        fontWeight = if (fastScanMode) FontWeight.Bold else FontWeight.Normal,
                                        color = if (fastScanMode) RetailNavy else Color.DarkGray
                                    )
                                }

                                if (recentTouchedPlus.isNotEmpty()) {
                                    Text(
                                        "Item Terakhir Naik ke Atas",
                                        fontSize = 10.sp,
                                        color = RetailGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Banner Notifikasi Scan Terakhir
                    if (!scanNotification.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (scanNotification!!.contains("tidak ditemukan")) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (scanNotification!!.contains("tidak ditemukan")) RetailRedPrimary else RetailGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (scanNotification!!.contains("tidak ditemukan")) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (scanNotification!!.contains("tidak ditemukan")) RetailRedPrimary else RetailGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        scanNotification!!,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (scanNotification!!.contains("tidak ditemukan")) RetailRedDark else RetailGreen,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { scanNotification = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }

                    // ==========================================
                    // 2. KARTU SOROTAN PRODUK YANG SEDANG DI DEPAN MATA (HASIL SCAN TERAKHIR)
                    // ==========================================
                    if (lastScannedProduct != null) {
                        val prod = lastScannedProduct
                        val physical = physicalCounts[prod.plu] ?: prod.stok
                        val selisih = physical - prod.stok

                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("card_spotlight_scanned_product"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFBC02D)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = RetailNavy,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "BARANG DI DEPAN MATA",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Rak: ${prod.rakPlanogram}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GrayText
                                        )
                                    }
                                    IconButton(
                                        onClick = { lastScannedPlu = null },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Tutup Sorotan", tint = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    prod.nama,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "PLU: ${prod.plu} | Barcode: ${prod.barcode}",
                                    fontSize = 11.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Kontrol Stepper & Status Selisih
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Stok Sistem: ${prod.stok} pcs", fontSize = 11.sp, color = GrayText)
                                        Text(
                                            text = if (selisih == 0) "Status: PAS (0)" else if (selisih > 0) "Status: LEBIH (+$selisih pcs)" else "Status: KURANG ($selisih pcs)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selisih == 0) RetailGreen else if (selisih > 0) RetailNavy else RetailRedPrimary
                                        )
                                    }

                                    // Stepper Besar & Cepat
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Tombol Kurang -1
                                        FilledTonalIconButton(
                                            onClick = {
                                                val curr = physicalCounts[prod.plu] ?: prod.stok
                                                if (curr > 0) physicalCounts[prod.plu] = curr - 1
                                            },
                                            modifier = Modifier.size(36.dp).testTag("btn_spotlight_minus")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Kurang")
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Input Angka Langsung
                                        var textInput by remember(physical) { mutableStateOf(physical.toString()) }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D)),
                                            modifier = Modifier.width(60.dp).height(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                BasicTextField(
                                                    value = textInput,
                                                    onValueChange = { newStr ->
                                                        val clean = newStr.filter { it.isDigit() }
                                                        textInput = clean
                                                        val num = clean.toIntOrNull() ?: 0
                                                        physicalCounts[prod.plu] = num
                                                    },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    textStyle = TextStyle(
                                                        textAlign = TextAlign.Center,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.Black
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().testTag("input_spotlight_qty")
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // Tombol Tambah +1
                                        FilledTonalIconButton(
                                            onClick = {
                                                val curr = physicalCounts[prod.plu] ?: prod.stok
                                                physicalCounts[prod.plu] = curr + 1
                                            },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = RetailNavy,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.size(36.dp).testTag("btn_spotlight_plus")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Tambah")
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Tombol Lanjut Scan Berikutnya
                                        Button(
                                            onClick = { showCameraScanner = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(36.dp).testTag("btn_spotlight_scan_next")
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scan Lain", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ==========================================
                    // 3. CAKUPAN RAK & RINGKASAN STOK
                    // ==========================================
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cakupan Rak / Planogram:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                if (selectedRakFilter != "Semua") {
                                    Text(
                                        "Reset ke Semua",
                                        fontSize = 11.sp,
                                        color = RetailNavy,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable { selectedRakFilter = "Semua" }
                                    )
                                }
                            }
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

                    Spacer(modifier = Modifier.height(6.dp))

                    // Ringkasan KPI Jumlah Fisik & Sistem
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TOTAL SISTEM", fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                                Text("$totalSistemInFilter pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE0E0E0)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("JUMLAH FISIK", fontSize = 9.sp, color = RetailNavy, fontWeight = FontWeight.Bold)
                                Text("$totalFisikInFilter pcs", fontSize = 13.sp, fontWeight = FontWeight.Black, color = RetailNavy)
                            }
                            Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color(0xFFE0E0E0)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TOTAL SELISIH", fontSize = 9.sp, color = if (totalSelisihQty == 0) RetailGreen else RetailRedPrimary, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (totalSelisihQty > 0) "+$totalSelisihQty pcs" else "$totalSelisihQty pcs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalSelisihQty == 0) RetailGreen else RetailRedPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Buttons: "Cek Selisih" vs "Tampilkan Semua"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onlyShowDiff = !onlyShowDiff },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (onlyShowDiff) RetailRedDark else RetailNavy,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_toggle_only_diff")
                        ) {
                            Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (onlyShowDiff) "TAMPILKAN SEMUA" else "HANYA SELISIH ($totalSelisihCount)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = if (onlyShowDiff) "$totalSelisihCount item selisih" else "${displayedProducts.size} item ditampilkan",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ==========================================
                    // 4. DAFTAR PRODUK (DENGAN PENYORTIRAN PINTAR)
                    // ==========================================
                    Card(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PosSurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRODUK / RAK", modifier = Modifier.weight(2.6f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("SISTEM", modifier = Modifier.weight(1.0f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text("JUMLAH FISIK", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text("SELISIH", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = Color.Black)
                            }

                            if (displayedProducts.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = GrayText, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            if (searchQuery.isNotEmpty()) "Tidak ada produk yang cocok dengan '$searchQuery'"
                                            else if (onlyShowDiff) "Semua produk sudah pas! Tidak ada selisih stok."
                                            else "Tidak ada produk di rak ini.",
                                            color = GrayText,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    items(displayedProducts, key = { it.plu }) { prod ->
                                        val physical = physicalCounts[prod.plu] ?: prod.stok
                                        val selisih = physical - prod.stok
                                        val isRecentlyTouched = recentTouchedPlus.firstOrNull() == prod.plu
                                        val isSpotlighted = lastScannedPlu == prod.plu

                                        val rowBg = when {
                                            isSpotlighted -> Color(0xFFFFF9C4) // Kuning lembut jika baru saja di-scan
                                            isRecentlyTouched -> Color(0xFFE8F5E9) // Hijau sangat lembut
                                            selisih != 0 -> Color(0xFFFFF3E0) // Oranye lembut jika ada selisih
                                            else -> Color.Transparent
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(rowBg)
                                                .clickable {
                                                    lastScannedPlu = prod.plu
                                                    recentTouchedPlus.remove(prod.plu)
                                                    recentTouchedPlus.add(0, prod.plu)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.6f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isRecentlyTouched) {
                                                        Surface(
                                                            color = RetailNavy,
                                                            shape = RoundedCornerShape(3.dp),
                                                            modifier = Modifier.padding(end = 4.dp)
                                                        ) {
                                                            Text(
                                                                "TERBARU",
                                                                fontSize = 8.sp,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        prod.nama,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color.Black,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    "PLU: ${prod.plu} | ${prod.rakPlanogram} | ${prod.barcode}",
                                                    fontSize = 10.sp,
                                                    color = GrayText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
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
                                                    .height(36.dp)
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
                                                                if (curr > 0) {
                                                                    physicalCounts[prod.plu] = curr - 1
                                                                    recentTouchedPlus.remove(prod.plu)
                                                                    recentTouchedPlus.add(0, prod.plu)
                                                                }
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

                                                    // Input Angka Langsung
                                                    var textInput by remember(physical) { mutableStateOf(physical.toString()) }
                                                    BasicTextField(
                                                        value = textInput,
                                                        onValueChange = { newStr ->
                                                            val clean = newStr.filter { it.isDigit() }
                                                            textInput = clean
                                                            if (clean.isNotEmpty()) {
                                                                val num = clean.toIntOrNull() ?: 0
                                                                physicalCounts[prod.plu] = num
                                                                recentTouchedPlus.remove(prod.plu)
                                                                recentTouchedPlus.add(0, prod.plu)
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
                                                                recentTouchedPlus.remove(prod.plu)
                                                                recentTouchedPlus.add(0, prod.plu)
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

                    Spacer(modifier = Modifier.height(6.dp))

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
                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("btn_apply_stock_opname"),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENYESUAIAN STOK (APPLY)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
