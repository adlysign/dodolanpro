package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.CameraBarcodeScannerDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import com.example.util.PriceTagPdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class LabelCustomOptions(
    val labelSize: String = "STANDAR_RAK", // STANDAR_RAK (60x30), PROMO_BESAR (70x40), LABEL_KECIL (33x15)
    val showStoreName: Boolean = true,
    val showBarcode: Boolean = true,
    val showRak: Boolean = true,
    val showDate: Boolean = true,
    val showPromoBadge: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTagScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    // 4 Distinct Main Modes: 0 = SEMUA LABEL, 1 = PROMO, 2 = PER RAK, 3 = CUSTOM
    var activeTab by remember { mutableStateOf(0) }

    // Dynamic list of unique racks from products
    val availableRaks = remember(products) {
        val list = products.map { it.rakPlanogram.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        if (list.isEmpty()) listOf("RAK-01", "RAK-02", "001-01") else list
    }
    var selectedRak by remember { mutableStateOf(availableRaks.firstOrNull() ?: "001-01") }

    // Custom selective print list: PLU -> copy count (default 1)
    val customSelectedMap = remember { mutableStateMapOf<String, Int>() }

    var searchQuery by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showLabelOptionsDialog by remember { mutableStateOf(false) }
    var showPrintSimulationDialog by remember { mutableStateOf(false) }
    var showSavedDocumentsDialog by remember { mutableStateOf(false) }
    var selectedPaperFormat by remember { mutableStateOf("A4") }
    var lastGeneratedPdfFile by remember { mutableStateOf<File?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }
    var isFeedbackSuccess by remember { mutableStateOf(true) }

    var labelOptions by remember { mutableStateOf(LabelCustomOptions()) }

    val currentDateStr = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    // Products to display according to activeTab
    val displayProducts = remember(products, activeTab, selectedRak, searchQuery, customSelectedMap.toMap()) {
        val baseList = when (activeTab) {
            0 -> products // SEMUA LABEL
            1 -> products.filter { it.isPromo || it.hasDiscountOrPromo() } // PROMO
            2 -> products.filter { it.rakPlanogram.equals(selectedRak, ignoreCase = true) } // PER RAK
            3 -> products.filter { customSelectedMap.containsKey(it.plu) } // CUSTOM
            else -> products
        }

        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { p ->
                p.nama.contains(searchQuery, ignoreCase = true) ||
                p.plu.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.barcodeRepack.contains(searchQuery, ignoreCase = true) ||
                p.rakPlanogram.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Total label copies to print
    val totalCopiesToPrint = remember(activeTab, customSelectedMap.toMap(), displayProducts) {
        if (activeTab == 3) {
            customSelectedMap.values.sum()
        } else {
            displayProducts.size
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "CETAK LABEL HARGA / PRICE TAG",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            "Cetak Semua • Promo • Per Rak • Custom",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSavedDocumentsDialog = true }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Dokumen Tersimpan", tint = Color.White)
                    }
                    IconButton(onClick = { showLabelOptionsDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Pengaturan Tampilan Label", tint = Color.White)
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
                .background(Color(0xFFF1F3F6))
        ) {
            // BANNER DOKUMEN PDF SIAP CETAK (JIKA BARU SAJA DISIMPAN)
            if (lastGeneratedPdfFile != null) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Dokumen Siap: ${lastGeneratedPdfFile?.name}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { lastGeneratedPdfFile?.let { DocExportManager.openDocument(context, it) } },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Black, color = RetailNavy)
                            }
                            TextButton(
                                onClick = { lastGeneratedPdfFile?.let { DocExportManager.printPdfDocument(context, it, "Cetak Label") } },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Cetak", fontSize = 11.sp, fontWeight = FontWeight.Black, color = RetailGreen)
                            }
                            TextButton(
                                onClick = { lastGeneratedPdfFile?.let { DocExportManager.shareDocument(context, it, "Dokumen Label") } },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Bagikan", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                            }
                            IconButton(onClick = { lastGeneratedPdfFile = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
            // PROFESSIONAL SEGMENTED TABS (Semua Label, Promo, Per Rak, Custom)
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color.White,
                    contentColor = RetailNavy,
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "SEMUA (${products.size})",
                                fontWeight = if (activeTab == 0) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeTab == 0) RetailNavy else Color(0xFF616161)
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            val promoCount = products.count { it.isPromo || it.hasDiscountOrPromo() }
                            Text(
                                "PROMO ($promoCount)",
                                fontWeight = if (activeTab == 1) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeTab == 1) RetailRedPrimary else Color(0xFF616161)
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Text(
                                "PER RAK",
                                fontWeight = if (activeTab == 2) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeTab == 2) RetailNavy else Color(0xFF616161)
                            )
                        }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = {
                            Text(
                                "CUSTOM (${customSelectedMap.size})",
                                fontWeight = if (activeTab == 3) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (activeTab == 3) Color(0xFF00695C) else Color(0xFF616161)
                            )
                        }
                    )
                }
            }

            // CLEAN ACTION TOOLBAR (Camera Icon Button, Search Bar, Format & Print Button)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TOMBOL SCAN HANYA SIMBOL KAMERA SAJA (Sesuai Permintaan User)
                    IconButton(
                        onClick = { showCameraScanner = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(RetailNavy, RoundedCornerShape(8.dp))
                            .testTag("btn_camera_scan_only")
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Scan Barcode Produk",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("price_tag_search"),
                        placeholder = { Text("Cari PLU / Barcode / Nama...", fontSize = 12.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF111111),
                            unfocusedTextColor = Color(0xFF111111),
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = Color(0xFFCCCCCC),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Format Settings Icon Button
                    IconButton(
                        onClick = { showLabelOptionsDialog = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Format Tampilan",
                            tint = RetailNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Main Print Button
                    Button(
                        onClick = {
                            if (totalCopiesToPrint == 0) {
                                feedbackMessage = "Tidak ada produk yang siap dicetak!"
                                isFeedbackSuccess = false
                            } else {
                                showPrintSimulationDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_cetak_label")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "CETAK ($totalCopiesToPrint)",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }

                // SUB-TOOLBAR: PER RAK SELECTOR OR BATCH ADD BUTTONS
                if (activeTab == 2) {
                    // PER RAK: Clean, horizontal rack selector chips
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PILIH NOMOR RAK:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF111111)
                                )
                                Text(
                                    "${displayProducts.size} Produk di Rak $selectedRak",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RetailNavy
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(availableRaks) { rak ->
                                    val isSelected = selectedRak.equals(rak, ignoreCase = true)
                                    Surface(
                                        color = if (isSelected) RetailNavy else Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) RetailNavy else Color(0xFFE0E0E0)
                                        ),
                                        modifier = Modifier.clickable { selectedRak = rak }
                                    ) {
                                        Text(
                                            text = "Rak $rak",
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else Color(0xFF333333),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    displayProducts.forEach { p ->
                                        customSelectedMap[p.plu] = (customSelectedMap[p.plu] ?: 0) + 1
                                    }
                                    feedbackMessage = "Semua produk di Rak $selectedRak dimasukkan ke daftar Custom!"
                                    isFeedbackSuccess = true
                                    activeTab = 3 // Move to custom tab
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+ Masukkan Semua Produk Rak Ini ke Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (activeTab == 1 && displayProducts.isNotEmpty()) {
                    // PROMO TAB: Quick batch button
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            displayProducts.forEach { p ->
                                customSelectedMap[p.plu] = (customSelectedMap[p.plu] ?: 0) + 1
                            }
                            feedbackMessage = "Semua produk promo dimasukkan ke daftar Custom!"
                            isFeedbackSuccess = true
                            activeTab = 3
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+ Masukkan Semua Produk Promo ke Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (activeTab == 3 && customSelectedMap.isNotEmpty()) {
                    // CUSTOM TAB: Summary & Clear All Button
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total: ${customSelectedMap.size} item (${customSelectedMap.values.sum()} lembar label)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF004D40)
                            )
                            Text(
                                "Kosongkan Semua",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = RetailRedPrimary,
                                modifier = Modifier.clickable { customSelectedMap.clear() }
                            )
                        }
                    }
                }

                // FEEDBACK MESSAGE
                if (feedbackMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = if (isFeedbackSuccess) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFeedbackSuccess) RetailGreen else RetailRedPrimary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (isFeedbackSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isFeedbackSuccess) RetailGreen else RetailRedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    feedbackMessage,
                                    fontSize = 11.sp,
                                    color = if (isFeedbackSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { feedbackMessage = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Black)
                            }
                        }
                    }
                }
            }

            // MAIN CONTENT: PRODUCT LABEL CARDS (SUPER CRISP & CLEAR)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (activeTab == 3 && displayProducts.isEmpty()) {
                    // Empty Custom List State
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                color = Color(0xFFE8EAF6),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = RetailNavy,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "Daftar Cetak Custom Masih Kosong",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF111111)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Pindai barcode produk menggunakan tombol kamera di atas,\natau pilih produk dari tab 'SEMUA', 'PROMO', atau 'PER RAK'.",
                                fontSize = 12.sp,
                                color = Color(0xFF616161),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showCameraScanner = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Buka Kamera Scan", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { activeTab = 0 }) {
                                    Text("Pilih dari Semua", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (labelOptions.labelSize == "LABEL_KECIL") {
                    // Small compact label row format
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(displayProducts) { prod ->
                            val isCustomSelected = customSelectedMap.containsKey(prod.plu)
                            val copies = customSelectedMap[prod.plu] ?: 1

                            PriceTagSmallRowItem(
                                product = prod,
                                isCustomMode = activeTab == 3,
                                isCustomSelected = isCustomSelected,
                                copies = copies,
                                onCopiesChange = { newCount ->
                                    if (newCount <= 0) customSelectedMap.remove(prod.plu)
                                    else customSelectedMap[prod.plu] = newCount
                                },
                                onToggleSelect = {
                                    if (isCustomSelected) customSelectedMap.remove(prod.plu)
                                    else customSelectedMap[prod.plu] = 1
                                }
                            )
                            HorizontalDivider(color = Color(0xFFE0E0E0))
                        }
                    }
                } else {
                    // Standard / Large Shelf Card Grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(displayProducts) { prod ->
                            val isCustomSelected = customSelectedMap.containsKey(prod.plu)
                            val copies = customSelectedMap[prod.plu] ?: 1

                            PriceTagGridCardItem(
                                product = prod,
                                options = labelOptions,
                                storeName = storeConfig.storeName,
                                dateStr = currentDateStr,
                                isCustomMode = activeTab == 3,
                                isCustomSelected = isCustomSelected,
                                copies = copies,
                                onCopiesChange = { newCount ->
                                    if (newCount <= 0) customSelectedMap.remove(prod.plu)
                                    else customSelectedMap[prod.plu] = newCount
                                },
                                onToggleSelect = {
                                    if (isCustomSelected) customSelectedMap.remove(prod.plu)
                                    else customSelectedMap[prod.plu] = 1
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Camera Barcode Scanner Dialog
    if (showCameraScanner) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScanner = false },
            onBarcodeDetected = { scannedCode ->
                val trimmed = scannedCode.trim()
                val foundProduct = products.find {
                    it.barcode.equals(trimmed, ignoreCase = true) ||
                    it.plu.equals(trimmed, ignoreCase = true) ||
                    it.barcodeRepack.equals(trimmed, ignoreCase = true) ||
                    it.barcodeRepack2.equals(trimmed, ignoreCase = true)
                }

                if (foundProduct != null) {
                    val currentCount = customSelectedMap[foundProduct.plu] ?: 0
                    customSelectedMap[foundProduct.plu] = currentCount + 1
                    feedbackMessage = "Scan berhasil: '${foundProduct.nama}' (Total: ${currentCount + 1} lembar)!"
                    isFeedbackSuccess = true
                    showCameraScanner = false
                    activeTab = 3 // Switch to Custom tab to view it
                } else {
                    feedbackMessage = "Barcode '$trimmed' tidak ditemukan di database produk!"
                    isFeedbackSuccess = false
                }
            }
        )
    }

    // LABEL OPTIONS DIALOG WITH 100% VISIBLE TEXT (Fixed ContainerColor and Text Colors)
    if (showLabelOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showLabelOptionsDialog = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111111),
            textContentColor = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PENGATURAN TAMPILAN LABEL",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Text(
                        "UKURAN LABEL KERTAS / STIKER:",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = RetailNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val sizes = listOf(
                        Pair("STANDAR_RAK", "Standar Rak Supermarket (60 x 30 mm)"),
                        Pair("PROMO_BESAR", "Promo Besar Menjol (70 x 40 mm)"),
                        Pair("LABEL_KECIL", "Label Barcode Kecil (33 x 15 mm / 3 Kolom)")
                    )

                    sizes.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { labelOptions = labelOptions.copy(labelSize = key) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = labelOptions.labelSize == key,
                                onClick = { labelOptions = labelOptions.copy(labelSize = key) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RetailNavy,
                                    unselectedColor = Color(0xFF424242)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF111111)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "ELEMEN YANG DITAMPILKAN DI LABEL:",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = RetailNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { labelOptions = labelOptions.copy(showStoreName = !labelOptions.showStoreName) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = labelOptions.showStoreName,
                            onCheckedChange = { labelOptions = labelOptions.copy(showStoreName = it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RetailNavy,
                                uncheckedColor = Color(0xFF424242),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nama Toko (${storeConfig.storeName})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { labelOptions = labelOptions.copy(showBarcode = !labelOptions.showBarcode) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = labelOptions.showBarcode,
                            onCheckedChange = { labelOptions = labelOptions.copy(showBarcode = it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RetailNavy,
                                uncheckedColor = Color(0xFF424242),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Barcode Satuan Produk", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { labelOptions = labelOptions.copy(showRak = !labelOptions.showRak) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = labelOptions.showRak,
                            onCheckedChange = { labelOptions = labelOptions.copy(showRak = it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RetailNavy,
                                uncheckedColor = Color(0xFF424242),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nomor Rak / Planogram", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { labelOptions = labelOptions.copy(showDate = !labelOptions.showDate) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = labelOptions.showDate,
                            onCheckedChange = { labelOptions = labelOptions.copy(showDate = it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = RetailNavy,
                                uncheckedColor = Color(0xFF424242),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tanggal Cetak ($currentDateStr)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLabelOptionsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("TERAPKAN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // CETAK & SIMPAN DOKUMEN LABEL (A4 / F4) DIALOG
    if (showPrintSimulationDialog) {
        val printItemsList = displayProducts
        val modeTitle = when (activeTab) {
            0 -> "Semua Label Toko"
            1 -> "Semua Produk Promo"
            2 -> "Produk Rak $selectedRak"
            3 -> "Pilihan Custom"
            else -> "Label Rak"
        }
        val modeSuffix = when (activeTab) {
            0 -> "Semua"
            1 -> "Promo"
            2 -> "Rak_${selectedRak.replace(" ", "_")}"
            3 -> "Custom"
            else -> "Rak"
        }

        // Pairs of (Product, copies)
        val productsWithCopies: List<Pair<ProductEntity, Int>> = printItemsList.map { prod ->
            val copies = if (activeTab == 3) (customSelectedMap[prod.plu] ?: 1) else 1
            Pair(prod, copies)
        }
        val flattenedProductsList: List<ProductEntity> = productsWithCopies.flatMap { (prod, count) -> List(count) { prod } }

        val labelsPerPage = when (labelOptions.labelSize) {
            "PROMO_BESAR" -> if (selectedPaperFormat == "F4") 18 else 15
            "LABEL_KECIL" -> if (selectedPaperFormat == "F4") 65 else 55
            else -> if (selectedPaperFormat == "F4") 32 else 28
        }
        val estimatedPages = (totalCopiesToPrint + labelsPerPage - 1) / labelsPerPage

        AlertDialog(
            onDismissRequest = { showPrintSimulationDialog = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111111),
            textContentColor = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "CETAK & SIMPAN DOKUMEN LABEL",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF111111)
                        )
                        Text(
                            "Format Kertas A4 & F4 (Folio)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                    Text(
                        "PILIH UKURAN KERTAS PRINTER:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = RetailNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Segmented Radio Buttons for Paper Format (A4 vs F4)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaperFormat = "A4" }
                                .testTag("paper_format_a4"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPaperFormat == "A4") RetailNavy else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selectedPaperFormat == "A4") RetailNavy else Color(0xFFCBD5E1)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "KERTAS A4",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (selectedPaperFormat == "A4") Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    "210 x 297 mm",
                                    fontSize = 10.sp,
                                    color = if (selectedPaperFormat == "A4") Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPaperFormat = "F4" }
                                .testTag("paper_format_f4"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPaperFormat == "F4") RetailNavy else Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selectedPaperFormat == "F4") RetailNavy else Color(0xFFCBD5E1)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "KERTAS F4 (FOLIO)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = if (selectedPaperFormat == "F4") Color.White else Color(0xFF1E293B)
                                )
                                Text(
                                    "215 x 330 mm",
                                    fontSize = 10.sp,
                                    color = if (selectedPaperFormat == "F4") Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Details
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                "• Mode: $modeTitle ($totalCopiesToPrint total label)",
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "• Layout: ${labelOptions.labelSize.replace("_", " ")} (~$labelsPerPage label/lembar)",
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "• Estimasi: $estimatedPages Lembar Dokumen $selectedPaperFormat",
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "• Lokasi Simpan: /DodolanPro/LabelRak/",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Ringkasan Item Cetak:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RetailNavy)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp)) {
                        items(printItemsList) { item ->
                            val copies = if (activeTab == 3) (customSelectedMap[item.plu] ?: 1) else 1
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    item.nama,
                                    fontSize = 11.sp,
                                    color = Color(0xFF111111),
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("$copies Lembar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Tombol 1: Simpan Dokumen PDF
                    Button(
                        onClick = {
                            val pdfBytes = PriceTagPdfGenerator.generatePriceTagPdf(
                                context = context,
                                productsList = flattenedProductsList,
                                paperFormat = selectedPaperFormat,
                                options = labelOptions,
                                storeName = storeConfig.storeName,
                                modeTitle = modeTitle
                            )
                            if (pdfBytes.isNotEmpty()) {
                                val (saved, file) = DocExportManager.saveDocument(
                                    context = context,
                                    subfolder = "LabelRak",
                                    prefix = "LABEL_${selectedPaperFormat}_${modeSuffix}",
                                    extension = "pdf",
                                    bytes = pdfBytes
                                )
                                if (saved) {
                                    lastGeneratedPdfFile = file
                                    showPrintSimulationDialog = false
                                    feedbackMessage = "Berhasil simpan dokumen ${file.name} ($selectedPaperFormat)!"
                                    isFeedbackSuccess = true
                                    DocExportManager.openDocument(context, file)
                                } else {
                                    feedbackMessage = "Gagal menyimpan dokumen PDF!"
                                    isFeedbackSuccess = false
                                }
                            } else {
                                feedbackMessage = "Gagal membuat PDF!"
                                isFeedbackSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_save_pdf")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN & BUKA PDF ($selectedPaperFormat)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Tombol 2: Cetak Langsung ke Printer
                    Button(
                        onClick = {
                            val pdfBytes = PriceTagPdfGenerator.generatePriceTagPdf(
                                context = context,
                                productsList = flattenedProductsList,
                                paperFormat = selectedPaperFormat,
                                options = labelOptions,
                                storeName = storeConfig.storeName,
                                modeTitle = modeTitle
                            )
                            if (pdfBytes.isNotEmpty()) {
                                val (saved, file) = DocExportManager.saveDocument(
                                    context = context,
                                    subfolder = "LabelRak",
                                    prefix = "LABEL_${selectedPaperFormat}_${modeSuffix}",
                                    extension = "pdf",
                                    bytes = pdfBytes
                                )
                                if (saved) {
                                    lastGeneratedPdfFile = file
                                    showPrintSimulationDialog = false
                                    feedbackMessage = "Membuka printer dialog untuk ${file.name}..."
                                    isFeedbackSuccess = true
                                    DocExportManager.printPdfDocument(context, file, "Label Rak $selectedPaperFormat")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_direct_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CETAK KE PRINTER ($selectedPaperFormat)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Tombol 3: Batal
                    OutlinedButton(
                        onClick = { showPrintSimulationDialog = false },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TUTUP", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // SAVED DOCUMENTS MANAGER DIALOG
    if (showSavedDocumentsDialog) {
        val savedFiles = remember(showSavedDocumentsDialog) {
            DocExportManager.getAllFilesInSubfolder(context, "LabelRak")
        }

        AlertDialog(
            onDismissRequest = { showSavedDocumentsDialog = false },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111111),
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "DOKUMEN LABEL TERSIMPAN",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Daftar dokumen PDF label harga siap cetak di /DodolanPro/LabelRak/:",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (savedFiles.isEmpty()) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Belum ada dokumen label tersimpan.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Klik tombol CETAK untuk membuat dokumen A4 atau F4.",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                            items(savedFiles) { f ->
                                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(f.lastModified()))
                                val sizeKb = f.length() / 1024
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    f.name,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E293B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Text("$dateStr • $sizeKb KB", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { DocExportManager.openDocument(context, f) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TextButton(
                                                onClick = { DocExportManager.printPdfDocument(context, f, "Cetak Label") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Cetak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailGreen)
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            TextButton(
                                                onClick = { DocExportManager.shareDocument(context, f, "Dokumen Label Harga") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Bagikan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSavedDocumentsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * CRISP, PROFESSIONAL SUPERMARKET SHELF LABEL (Alfamart / Supermarket Standard)
 * High-contrast, clean lines, bold black typography.
 */
@Composable
fun PriceTagGridCardItem(
    product: ProductEntity,
    options: LabelCustomOptions,
    storeName: String,
    dateStr: String,
    isCustomMode: Boolean,
    isCustomSelected: Boolean,
    copies: Int,
    onCopiesChange: (Int) -> Unit,
    onToggleSelect: () -> Unit
) {
    val isPromo = product.isPromo || product.hasDiscountOrPromo()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPromo) 2.dp else 1.dp,
                color = if (isPromo) Color(0xFFD32F2F) else Color(0xFFBDBDBD),
                shape = RoundedCornerShape(6.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Store Name or Promo Banner Ribbon
            if (isPromo) {
                Surface(
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.getEffectivePromoLabel().ifBlank { "PROMO HEMAT" }.uppercase(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        if (options.showDate) {
                            Text(dateStr, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (options.showStoreName) {
                Surface(
                    color = Color(0xFF263238),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = storeName.uppercase(),
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (options.showDate) {
                            Text(dateStr, color = Color.White.copy(alpha = 0.85f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Body
            Column(modifier = Modifier.padding(8.dp)) {
                // Product Title (Crisp, High Contrast Black)
                Text(
                    text = product.nama,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    lineHeight = 16.sp,
                    color = Color(0xFF111111),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Location / PLU Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (options.showRak) {
                        Surface(
                            color = Color(0xFFEDE7F6),
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                "RAK: ${product.rakPlanogram.take(10)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4A148C),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        "PLU: ${product.plu}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // PRICE BOX
                if (isPromo) {
                    // Promo Price with Strikethrough
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF8E1), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HARGA NORMAL:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5D4037))
                            Text(
                                text = formatRupiah(product.hargaJual),
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Black,
                                style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("Rp", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFB71C1C))
                            Text(
                                text = formatRupiah(product.getEffectiveDiscountedPrice()).replace("Rp", "").trim(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFB71C1C)
                            )
                        }
                    }
                } else {
                    // Regular Price Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(4.dp))
                            .padding(6.dp)
                    ) {
                        Text("HARGA JUAL:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF616161))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("Rp", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF111111))
                            Text(
                                text = formatRupiah(product.hargaJual).replace("Rp", "").trim(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF111111)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Barcode simulation
                if (options.showBarcode) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(10.dp)
                                .padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val pattern = (product.barcode + "10110").hashCode().toString()
                            pattern.forEach { ch ->
                                val w = if (ch in '0'..'4') 1.5.dp else 3.dp
                                Box(modifier = Modifier.width(w).fillMaxHeight().background(Color.Black))
                            }
                        }
                        Text(
                            text = product.barcode,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF212121)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 6.dp))

                // Action Footer
                if (isCustomMode) {
                    // Stepper control in Custom Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCopiesChange(copies - 1) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang", tint = Color.Black)
                            }
                            Text(
                                "$copies Lbr",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = Color.Black
                            )
                            IconButton(
                                onClick = { onCopiesChange(copies + 1) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah", tint = RetailNavy)
                            }
                        }

                        IconButton(
                            onClick = { onCopiesChange(0) },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RetailRedPrimary)
                        }
                    }
                } else {
                    // Button to toggle into Custom List
                    Button(
                        onClick = onToggleSelect,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCustomSelected) RetailGreen else Color(0xFFECEFF1),
                            contentColor = if (isCustomSelected) Color.White else Color(0xFF263238)
                        )
                    ) {
                        Icon(
                            if (isCustomSelected) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (isCustomSelected) "TERPILIH ($copies)" else "+ PILIH KE CUSTOM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact Row Item for Small Sticker Format (33x15mm style)
 */
@Composable
fun PriceTagSmallRowItem(
    product: ProductEntity,
    isCustomMode: Boolean,
    isCustomSelected: Boolean,
    copies: Int,
    onCopiesChange: (Int) -> Unit,
    onToggleSelect: () -> Unit
) {
    val isPromo = product.isPromo || product.hasDiscountOrPromo()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCustomSelected) Color(0xFFF1F8E9) else Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                product.nama,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                color = Color(0xFF111111)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PLU: ${product.plu}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(modifier = Modifier.width(6.dp))
                Text("BC: ${product.barcode}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rak: ${product.rakPlanogram}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF512DA8))
            }
            if (isPromo) {
                Text(
                    product.getEffectivePromoLabel(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = RetailRedPrimary
                )
            }
        }

        // Price with Strikethrough
        Column(modifier = Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
            if (isPromo) {
                Text(
                    formatRupiah(product.hargaJual),
                    fontSize = 10.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                )
                Text(
                    formatRupiah(product.getEffectiveDiscountedPrice()),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFFB71C1C)
                )
            } else {
                Text(
                    formatRupiah(product.hargaJual),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = Color(0xFF111111)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action
        if (isCustomMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onCopiesChange(copies - 1) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
                Text("$copies", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color.Black)
                IconButton(onClick = { onCopiesChange(copies + 1) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = RetailNavy, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onCopiesChange(0) }, modifier = Modifier.size(26.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                }
            }
        } else {
            IconButton(onClick = onToggleSelect, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isCustomSelected) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    contentDescription = "Pilih",
                    tint = if (isCustomSelected) RetailGreen else RetailNavy,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
