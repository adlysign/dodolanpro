package com.example.ui.screens.sis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.StoreBranchEntity
import com.example.data.local.entity.SupplierEntity
import com.example.ui.screens.pos.CameraBarcodeScannerDialog
import com.example.ui.screens.pos.LovProductDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager

data class PurchaseCartItem(
    val product: ProductEntity,
    var qty: Int,
    var hargaBeli: Double,
    var satuan: String = product.satuan,
    var konversi: Int = 1,
    var updateMasterProduct: Boolean = false
) {
    val subtotal: Double get() = qty * hargaBeli
    val totalBaseQty: Int get() = qty * maxOf(1, konversi)
}

enum class SumberBelanjaTipe {
    SUPPLIER,
    TOKO_GROSIR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    val allStoreBranches by viewModel.allStoreBranches.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Input Pembelian, 1: Riwayat Pembelian

    // Sumber Belanja: Supplier vs Toko Grosir
    var sumberTipe by remember { mutableStateOf(SumberBelanjaTipe.SUPPLIER) }
    var selectedSupplierNama by remember { mutableStateOf("") }
    var selectedTokoGrosirNama by remember { mutableStateOf("") }

    var fakturInput by remember { mutableStateOf("FB-" + (100000..999999).random()) }
    var petugasNamaInput by remember { mutableStateOf("") }
    var keteranganInput by remember { mutableStateOf("") }

    // Inisialisasi default
    LaunchedEffect(allSuppliers) {
        if (selectedSupplierNama.isBlank() && allSuppliers.isNotEmpty()) {
            selectedSupplierNama = allSuppliers.first().nama
        }
    }
    LaunchedEffect(allStoreBranches) {
        if (selectedTokoGrosirNama.isBlank() && allStoreBranches.isNotEmpty()) {
            selectedTokoGrosirNama = allStoreBranches.first().namaToko
        }
    }
    LaunchedEffect(currentCashier) {
        if (petugasNamaInput.isBlank()) {
            petugasNamaInput = currentCashier?.nama ?: "Administrator"
        }
    }

    val purchaseCart = remember { mutableStateListOf<PurchaseCartItem>() }
    var itemForUnitChange by remember { mutableStateOf<Pair<PurchaseCartItem, Int>?>(null) }
    var barcodeInput by remember { mutableStateOf("") }
    var costMethod by remember { mutableStateOf("AVERAGE") } // "AVERAGE" = Moving Average Cost, "LAST" = Last Purchase Price

    var showLovDialog by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAddTokoGrosirDialog by remember { mutableStateOf(false) }

    var feedbackMessage by remember { mutableStateOf("") }
    var selectedPurchaseForDoc by remember { mutableStateOf<PurchaseEntity?>(null) }
    var docSaveMsg by remember { mutableStateOf("") }
    var riwayatSearchQuery by remember { mutableStateOf("") }

    val totalNominal = purchaseCart.sumOf { it.subtotal }
    val totalQty = purchaseCart.sumOf { it.qty }

    fun addProductByCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        val found = allProducts.find {
            it.plu.equals(trimmed, ignoreCase = true) ||
            it.barcode.equals(trimmed, ignoreCase = true) ||
            it.barcodeRepack.equals(trimmed, ignoreCase = true) ||
            it.barcodeRepack2.equals(trimmed, ignoreCase = true) ||
            it.nama.contains(trimmed, ignoreCase = true)
        }
        if (found != null) {
            val isRepack1 = found.isRepack && trimmed.equals(found.barcodeRepack, ignoreCase = true)
            val isRepack2 = found.isRepack && trimmed.equals(found.barcodeRepack2, ignoreCase = true)
            val initSatuan = when {
                isRepack1 && found.satuanRepack.isNotBlank() -> found.satuanRepack
                isRepack2 && found.satuanRepack2.isNotBlank() -> found.satuanRepack2
                else -> found.satuan
            }
            val initKonversi = when {
                isRepack1 -> found.isiRepack
                isRepack2 -> found.isiRepack2
                else -> 1
            }
            val initHarga = when {
                isRepack1 && found.hargaRepack > 0 -> found.hargaRepack
                isRepack2 && found.hargaRepack2 > 0 -> found.hargaRepack2
                else -> found.hargaBeli * initKonversi
            }

            val existing = purchaseCart.find { it.product.plu == found.plu && it.satuan == initSatuan }
            if (existing != null) {
                val idx = purchaseCart.indexOf(existing)
                purchaseCart[idx] = existing.copy(qty = existing.qty + 1)
            } else {
                purchaseCart.add(
                    PurchaseCartItem(
                        product = found,
                        qty = 1,
                        hargaBeli = initHarga,
                        satuan = initSatuan,
                        konversi = initKonversi
                    )
                )
            }
            feedbackMessage = "Berhasil ditambahkan: ${found.nama} ($initSatuan)"
            barcodeInput = ""
        } else {
            feedbackMessage = "Produk tidak ditemukan untuk kode '$trimmed'"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PEMBELIAN KULAKAN BARANG", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Belanja Supplier & Toko Grosir • Stok & HPP Otomatis", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
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
            // Tab Navigation
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INPUT PEMBELIAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RIWAYAT NOTA (${allPurchases.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // CARD 1: PILIHAN SUMBER BELANJA (SUPPLIER VS TOKO GROSIR)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, PosCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "1. PILIH SUMBER KULAKAN / BELANJA:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RetailNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Toggle Button Segmented: Supplier vs Toko Grosir
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Pilihan 1: Belanja dari Supplier
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) RetailNavy else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.5.dp, if (sumberTipe == SumberBelanjaTipe.SUPPLIER) RetailNavy else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { sumberTipe = SumberBelanjaTipe.SUPPLIER }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) Color.White else RetailNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    "BELANJA DARI SUPPLIER",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) Color.White else Color(0xFF1E293B)
                                                )
                                                Text(
                                                    "Distributor / Pabrik",
                                                    fontSize = 9.sp,
                                                    color = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }

                                    // Pilihan 2: Belanja dari Toko Grosir
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sumberTipe == SumberBelanjaTipe.TOKO_GROSIR) RetailRedPrimary else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.5.dp, if (sumberTipe == SumberBelanjaTipe.TOKO_GROSIR) RetailRedPrimary else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { sumberTipe = SumberBelanjaTipe.TOKO_GROSIR }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = if (sumberTipe == SumberBelanjaTipe.TOKO_GROSIR) Color.White else RetailRedPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    "BELANJA DARI TOKO GROSIR",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sumberTipe == SumberBelanjaTipe.TOKO_GROSIR) Color.White else Color(0xFF1E293B)
                                                )
                                                Text(
                                                    "Agen / Pasar Kulakan",
                                                    fontSize = 9.sp,
                                                    color = if (sumberTipe == SumberBelanjaTipe.TOKO_GROSIR) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Dropdown & Tombol Tambah Data Sumber
                                if (sumberTipe == SumberBelanjaTipe.SUPPLIER) {
                                    // Pilihan Supplier
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Pilih Data Supplier:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Button(
                                            onClick = { showAddSupplierDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Tambah Supplier Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    var expSupplier by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expSupplier,
                                        onExpandedChange = { expSupplier = !expSupplier }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedSupplierNama.ifBlank { "Pilih Supplier / Distributor" },
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSupplier) },
                                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = RetailNavy) },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expSupplier,
                                            onDismissRequest = { expSupplier = false }
                                        ) {
                                            if (allSuppliers.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Belum ada data supplier. Klik + Tambah Supplier") },
                                                    onClick = { expSupplier = false; showAddSupplierDialog = true }
                                                )
                                            } else {
                                                allSuppliers.forEach { sup ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(sup.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                if (sup.noHp.isNotBlank()) Text("Telp: ${sup.noHp}", fontSize = 10.sp, color = GrayText)
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedSupplierNama = sup.nama
                                                            expSupplier = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Pilihan Toko Grosir
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Pilih Data Toko Grosir / Agen:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                        Button(
                                            onClick = { showAddTokoGrosirDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Tambah Toko Grosir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    var expToko by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expToko,
                                        onExpandedChange = { expToko = !expToko }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedTokoGrosirNama.ifBlank { "Pilih Toko Grosir / Agen Kulakan" },
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expToko) },
                                            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = RetailRedPrimary) },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expToko,
                                            onDismissRequest = { expToko = false }
                                        ) {
                                            if (allStoreBranches.isEmpty()) {
                                                DropdownMenuItem(
                                                    text = { Text("Belum ada data toko grosir. Klik + Tambah Toko Grosir") },
                                                    onClick = { expToko = false; showAddTokoGrosirDialog = true }
                                                )
                                            } else {
                                                allStoreBranches.forEach { br ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text(br.namaToko, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                if (br.alamat.isNotBlank()) Text(br.alamat, fontSize = 10.sp, color = GrayText)
                                                            }
                                                        },
                                                        onClick = {
                                                            selectedTokoGrosirNama = br.namaToko
                                                            expToko = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Row No Faktur & Petugas Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = fakturInput,
                                        onValueChange = { fakturInput = it },
                                        label = { Text("No. Faktur / Nota Beli") },
                                        modifier = Modifier.weight(1.1f),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { fakturInput = "FB-" + (100000..999999).random() }) {
                                                Icon(Icons.Default.Autorenew, contentDescription = "Acak", tint = RetailNavy)
                                            }
                                        }
                                    )

                                    OutlinedTextField(
                                        value = petugasNamaInput,
                                        onValueChange = { petugasNamaInput = it },
                                        label = { Text("Petugas Input *") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = RetailNavy) }
                                    )
                                }
                            }
                        }
                    }

                    // CARD 2: SCAN & CARI PRODUK (SEPERTI LAYAR KASIR)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.5.dp, RetailNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "2. SCAN / INPUT PRODUK SEPERTI LAYAR KASIR:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RetailNavy
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Text Field Scan / Input PLU Barcode
                                    OutlinedTextField(
                                        value = barcodeInput,
                                        onValueChange = { barcodeInput = it },
                                        placeholder = { Text("Scan Barcode / Ketik PLU...", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.QrCode, contentDescription = null, tint = RetailNavy)
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("purchase_barcode_input"),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { addProductByCode(barcodeInput) })
                                    )

                                    // Tombol Scan Kamera
                                    FilledTonalButton(
                                        onClick = { showCameraScannerDialog = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = "Scan Kamera", tint = RetailRedPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailRedPrimary)
                                    }

                                    // Tombol LOV
                                    Button(
                                        onClick = { showLovDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Icon(Icons.Default.ListAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LOV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (feedbackMessage.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = if (feedbackMessage.contains("tidak", ignoreCase = true)) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            feedbackMessage,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (feedbackMessage.contains("tidak", ignoreCase = true)) RetailRedPrimary else RetailGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 3: DAFTAR BARANG YANG DIBELI (RESPONSIF, LUAS, TIDAK TERGENCET)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3. RINCIAN BARANG BELANJA (${purchaseCart.size} Macam, $totalQty Pcs)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RetailNavy
                            )
                            if (purchaseCart.isNotEmpty()) {
                                TextButton(
                                    onClick = { purchaseCart.clear() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Kosongkan", color = RetailRedPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (purchaseCart.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Belum ada barang di daftar belanja.", fontWeight = FontWeight.Bold, color = GrayText)
                                        Text("Gunakan Scan Kamera atau Tombol LOV untuk memilih barang kulakan.", fontSize = 11.sp, color = GrayText)
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(purchaseCart) { index, item ->
                            val currentStock = maxOf(0, item.product.stok)
                            val currentCost = item.product.hargaBeli
                            val konversi = maxOf(1, item.konversi)
                            val buyBaseQty = item.qty * konversi
                            val buyCostPerBase = if (konversi > 1) item.hargaBeli / konversi else item.hargaBeli
                            val projectedHpp = if ((currentStock + buyBaseQty) > 0) {
                                ((currentStock * currentCost) + (buyBaseQty * buyCostPerBase)) / (currentStock + buyBaseQty)
                            } else buyCostPerBase

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, PosCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Row 1: Nama Barang, Tag Satuan & Tombol Hapus
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                            Text(item.product.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                            Text("PLU: ${item.product.plu} | Barcode: ${item.product.barcode}", fontSize = 10.sp, color = GrayText)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                Surface(color = Color(0xFFE2E8F0), shape = RoundedCornerShape(4.dp)) {
                                                    Text(
                                                        "Stok: $currentStock ${item.product.satuan}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF334155),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(4.dp)) {
                                                    Text(
                                                        "Modal: ${formatRupiah(currentCost)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0369A1),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                // Tombol / Badge Ubah Satuan
                                                Surface(
                                                    color = Color(0xFFEFF6FF),
                                                    shape = RoundedCornerShape(4.dp),
                                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                                    modifier = Modifier.clickable { itemForUnitChange = item to index }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(11.dp), tint = RetailNavy)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            "Satuan: ${item.satuan}" + (if (item.konversi > 1) " (x${item.konversi})" else ""),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = RetailNavy
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Ubah", fontSize = 9.sp, color = RetailRedPrimary, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { purchaseCart.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RetailRedPrimary)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                                    // Row 2: Input Harga Beli Baru, Qty Stepper, dan Subtotal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Kolom Harga Beli Baru (per Satuan yang dipilih)
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text("Harga Beli / ${item.satuan} (Rp)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                            var priceStr by remember(item.hargaBeli) { mutableStateOf(item.hargaBeli.toLong().toString()) }
                                            OutlinedTextField(
                                                value = priceStr,
                                                onValueChange = {
                                                    priceStr = it
                                                    val parsed = it.toDoubleOrNull() ?: 0.0
                                                    purchaseCart[index] = item.copy(hargaBeli = parsed)
                                                },
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                singleLine = true
                                            )
                                            if (item.konversi > 1) {
                                                Text(
                                                    "Setara ${formatRupiah(item.hargaBeli / item.konversi)}/${item.product.satuan}",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF0369A1),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                            }
                                        }

                                        // Kolom Qty dengan tombol [-] dan [+] (Tampilan Luas, Angka Terlihat Sempurna)
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text("Qty Beli (${item.satuan})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(34.dp).clickable {
                                                        if (item.qty > 1) {
                                                            purchaseCart[index] = item.copy(qty = item.qty - 1)
                                                        }
                                                    }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp), tint = Color.Black)
                                                    }
                                                }

                                                var qtyStr by remember(item.qty) { mutableStateOf(item.qty.toString()) }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(38.dp)
                                                        .padding(horizontal = 4.dp)
                                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                                        .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(6.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = qtyStr,
                                                        onValueChange = {
                                                            val clean = it.filter { ch -> ch.isDigit() }
                                                            qtyStr = clean
                                                            val parsed = clean.toIntOrNull() ?: 1
                                                            purchaseCart[index] = item.copy(qty = maxOf(1, parsed))
                                                        },
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                                        textStyle = androidx.compose.ui.text.TextStyle(
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Black,
                                                            textAlign = TextAlign.Center,
                                                            color = Color.Black
                                                        ),
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = RetailNavy,
                                                    modifier = Modifier.size(34.dp).clickable {
                                                        purchaseCart[index] = item.copy(qty = item.qty + 1)
                                                    }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            if (item.konversi > 1) {
                                                Text(
                                                    "1 ${item.satuan} = ${item.konversi} ${item.product.satuan} (Total: ${item.totalBaseQty} ${item.product.satuan})",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF0369A1),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 1.dp)
                                                )
                                            }
                                        }

                                        // Subtotal
                                        Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                            Text("Subtotal", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                            Text(
                                                formatRupiah(item.subtotal),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Edukasi & Simulasi HPP Rata-Rata Bergerak
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Calculate, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Kalkulasi Modal Rata-Rata (Weighted Average):",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF166534)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "Stok Lama: $currentStock @ ${formatRupiah(currentCost)} + Beli: ${item.qty} ${item.satuan} (${buyBaseQty} ${item.product.satuan}) @ ${formatRupiah(item.hargaBeli)} ➔ HPP Baru: ${formatRupiah(projectedHpp)} / ${item.product.satuan}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                            Text(
                                                "Modal barang lama tidak rusak/hilang, dan dihitung proporsional sehingga laba penjualan tetap adil.",
                                                fontSize = 9.sp,
                                                color = Color(0xFF166534)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // CARD 4: PILIHAN METODE HPP & TOTAL
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Aturan Pembaruan Harga Modal Produk di Database:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = costMethod == "AVERAGE",
                                        onClick = { costMethod = "AVERAGE" }
                                    )
                                    Text("HPP Rata-Rata Bergerak (Standar Retail)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    RadioButton(
                                        selected = costMethod == "LAST",
                                        onClick = { costMethod = "LAST" }
                                    )
                                    Text("Harga Baru Saja", fontSize = 11.sp)
                                }
                                Text(
                                    if (costMethod == "AVERAGE")
                                        "✓ Direkomendasikan: Menggabungkan nilai modal stok lama & pembelian baru secara adil."
                                    else
                                        "✓ Modal di katalog produk langsung disamakan dengan harga beli nota ini.",
                                    fontSize = 10.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // CARD 5: RINGKASAN & TOMBOL SIMPAN BESAR
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, PosCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Sumber: ${if (sumberTipe == SumberBelanjaTipe.SUPPLIER) "Supplier ($selectedSupplierNama)" else "Toko Grosir ($selectedTokoGrosirNama)"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RetailNavy
                                        )
                                        Text("Petugas Input: $petugasNamaInput", fontSize = 11.sp, color = GrayText)
                                        val totalBasePcs = purchaseCart.sumOf { it.totalBaseQty }
                                        val totalLabel = if (totalBasePcs != totalQty) "Total: $totalQty Unit ($totalBasePcs Pcs Dasar)" else "Total: $totalQty Pcs"
                                        Text("$totalLabel • ${purchaseCart.size} Macam Produk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("TOTAL PEMBELIAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                        Text(
                                            formatRupiah(totalNominal),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = RetailRedPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (purchaseCart.isEmpty()) {
                                            feedbackMessage = "Daftar barang pembelian masih kosong!"
                                            return@Button
                                        }
                                        val sumberNama = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) {
                                            selectedSupplierNama.ifBlank { "Supplier Umum" }
                                        } else {
                                            selectedTokoGrosirNama.ifBlank { "Toko Grosir Berkah" }
                                        }
                                        val sumberLabel = if (sumberTipe == SumberBelanjaTipe.SUPPLIER) "Supplier" else "Toko Grosir"

                                        val itemsToSave = purchaseCart.map {
                                            it.product to Pair(it.qty, it.hargaBeli)
                                        }
                                        val itemUnitsMap = purchaseCart.associate {
                                            it.product.plu to Pair(it.satuan, it.konversi)
                                        }
                                        purchaseCart.forEach { item ->
                                            if (item.updateMasterProduct) {
                                                viewModel.updateProductUnit(item.product.plu, item.satuan)
                                            }
                                        }
                                        viewModel.recordDirectPurchase(
                                            faktur = fakturInput,
                                            supplier = sumberNama,
                                            tokoTujuan = sumberLabel,
                                            petugasNama = petugasNamaInput.ifBlank { "Administrator" },
                                            keterangan = keteranganInput.ifBlank { "Belanja $sumberLabel" },
                                            items = itemsToSave,
                                            itemUnits = itemUnitsMap,
                                            costMethod = costMethod
                                        ) {
                                            feedbackMessage = "Pembelian $fakturInput dari $sumberNama berhasil disimpan oleh $petugasNamaInput! Stok dan HPP telah diperbarui."
                                            purchaseCart.clear()
                                            fakturInput = "FB-" + (100000..999999).random()
                                        }
                                    },
                                    enabled = purchaseCart.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SIMPAN PEMBELIAN (UPDATE STOK & MODAL)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // TAB 1: RIWAYAT PEMBELIAN DENGAN MENAMPILKAN SUMBER & PETUGAS
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    OutlinedTextField(
                        value = riwayatSearchQuery,
                        onValueChange = { riwayatSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari Faktur, Supplier, Toko Grosir, atau Petugas...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = retailTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredPurchases = allPurchases.filter { p ->
                        riwayatSearchQuery.isBlank() ||
                        p.noFakturBeli.contains(riwayatSearchQuery, ignoreCase = true) ||
                        p.supplierNama.contains(riwayatSearchQuery, ignoreCase = true) ||
                        p.tokoTujuan.contains(riwayatSearchQuery, ignoreCase = true) ||
                        p.petugasNama.contains(riwayatSearchQuery, ignoreCase = true) ||
                        p.kasirNik.contains(riwayatSearchQuery, ignoreCase = true)
                    }

                    if (filteredPurchases.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Belum ada riwayat nota pembelian.", color = GrayText)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredPurchases) { p ->
                                val isGrosir = p.tokoTujuan.contains("Grosir", ignoreCase = true)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, PosCardBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = if (isGrosir) Color(0xFFFFEBEE) else Color(0xFFE0F2FE),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Icon(
                                                            if (isGrosir) Icons.Default.Storefront else Icons.Default.LocalShipping,
                                                            contentDescription = null,
                                                            tint = if (isGrosir) RetailRedPrimary else RetailNavy,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            if (isGrosir) "TOKO GROSIR" else "SUPPLIER",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isGrosir) RetailRedPrimary else RetailNavy
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(p.noFakturBeli, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                            }

                                            Text(p.tanggal, fontSize = 11.sp, color = GrayText)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Sumber Nama
                                        Text(
                                            "Sumber: ${p.supplierNama}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Petugas Penginput (Sesuai Permintaan User)
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Badge, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Petugas Input: ${p.petugasNama} (NIK: ${p.kasirNik})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = RetailNavy
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Total: ${p.totalItem} pcs • ${formatRupiah(p.totalNominal)}",
                                                fontWeight = FontWeight.Black,
                                                color = RetailGreen,
                                                fontSize = 13.sp
                                            )

                                            Button(
                                                onClick = { selectedPurchaseForDoc = p },
                                                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Cetak / Simpan", fontSize = 11.sp)
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
    }

    // Modal LOV Daftar Produk
    if (showLovDialog) {
        LovProductDialog(
            products = allProducts,
            onDismiss = { showLovDialog = false },
            onSelectProduct = { prod, qty ->
                val existing = purchaseCart.find { it.product.plu == prod.plu && it.satuan == prod.satuan }
                if (existing != null) {
                    val idx = purchaseCart.indexOf(existing)
                    purchaseCart[idx] = existing.copy(qty = existing.qty + qty)
                } else {
                    purchaseCart.add(
                        PurchaseCartItem(
                            product = prod,
                            qty = qty,
                            hargaBeli = prod.hargaBeli,
                            satuan = prod.satuan,
                            konversi = 1
                        )
                    )
                }
                showLovDialog = false
            }
        )
    }

    // Modal Camera Barcode Scanner
    if (showCameraScannerDialog) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScannerDialog = false },
            onBarcodeDetected = { scannedCode ->
                showCameraScannerDialog = false
                addProductByCode(scannedCode)
            }
        )
    }

    // Dialog Tambah Data Supplier Baru
    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onSave = { nama, noHp, alamat, kontak ->
                viewModel.addSupplier(nama = nama, noHp = noHp, alamat = alamat, kontakPerson = kontak) {
                    selectedSupplierNama = nama
                    showAddSupplierDialog = false
                    feedbackMessage = "Supplier '$nama' berhasil ditambahkan!"
                }
            }
        )
    }

    // Dialog Tambah Data Toko Grosir Baru
    if (showAddTokoGrosirDialog) {
        AddTokoGrosirDialog(
            onDismiss = { showAddTokoGrosirDialog = false },
            onSave = { namaToko, telepon, alamat ->
                viewModel.addStoreBranch(namaToko = namaToko, telepon = telepon, alamat = alamat) {
                    selectedTokoGrosirNama = namaToko
                    showAddTokoGrosirDialog = false
                    feedbackMessage = "Toko Grosir '$namaToko' berhasil ditambahkan!"
                }
            }
        )
    }

    // Modal Ubah Satuan Pembelian (Multi-Satuan & Konversi)
    itemForUnitChange?.let { (item, index) ->
        ItemUnitDialog(
            product = item.product,
            currentSatuan = item.satuan,
            currentKonversi = item.konversi,
            currentHargaBeli = item.hargaBeli,
            dialogContext = "Pembelian",
            onDismiss = { itemForUnitChange = null },
            onConfirm = { cfg ->
                if (cfg.updateMasterProduct) {
                    viewModel.updateProductUnit(item.product.plu, cfg.satuan)
                }
                purchaseCart[index] = item.copy(
                    satuan = cfg.satuan,
                    konversi = cfg.konversi,
                    hargaBeli = cfg.hargaBeli,
                    updateMasterProduct = cfg.updateMasterProduct
                )
                itemForUnitChange = null
            }
        )
    }

    // Modal Document Preview for Purchase (saved in DodolanPro/Pembelian)
    selectedPurchaseForDoc?.let { pur ->
        val docText = buildString {
            appendLine("========================================")
            appendLine("       NOTA PEMBELIAN BARANG MASUK      ")
            appendLine("----------------------------------------")
            appendLine("Faktur   : ${pur.noFakturBeli}")
            appendLine("Tanggal  : ${pur.tanggal}")
            appendLine("Sumber   : ${pur.supplierNama} (${pur.tokoTujuan})")
            appendLine("Petugas  : ${pur.petugasNama} (NIK: ${pur.kasirNik})")
            appendLine("Total Pcs: ${pur.totalItem}")
            appendLine("Total Rp : ${formatRupiah(pur.totalNominal)}")
            appendLine("Ket      : ${pur.keterangan}")
            appendLine("----------------------------------------")
            appendLine("Status: STOK DITAMBAHKAN & HPP DIPERBARUI")
            appendLine("Arsip : DodolanPro/Pembelian")
            appendLine("========================================")
        }

        AlertDialog(
            onDismissRequest = {
                selectedPurchaseForDoc = null
                docSaveMsg = ""
            },
            title = { Text("DOKUMEN NOTA PEMBELIAN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                    ) {
                        Text(
                            text = docText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp),
                            color = Color.Black
                        )
                    }
                    if (docSaveMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(docSaveMsg, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val saved = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Pembelian",
                                prefix = "Beli_${pur.noFakturBeli}",
                                extension = "txt",
                                content = docText
                            )
                            docSaveMsg = "Tersimpan di DodolanPro/Pembelian/${saved.second.name}"
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN FILE", fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            selectedPurchaseForDoc = null
                            docSaveMsg = ""
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

@Composable
fun AddSupplierDialog(
    onDismiss: () -> Unit,
    onSave: (nama: String, noHp: String, alamat: String, kontak: String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var noHp by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var kontak by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = RetailNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text("TAMBAH DATA SUPPLIER", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan identitas distributor / pabrik pemasok:", fontSize = 11.sp, color = GrayText)
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it; errorMsg = "" },
                    label = { Text("Nama Supplier / Distributor *") },
                    placeholder = { Text("Contoh: PT Wings Surya, DC Cileungsi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noHp,
                    onValueChange = { noHp = it },
                    label = { Text("No. Telepon / WhatsApp") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = kontak,
                    onValueChange = { kontak = it },
                    label = { Text("Kontak Person / Sales (PIC)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat Gudang / Kantor Supplier") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = RetailRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isBlank()) {
                        errorMsg = "Nama supplier wajib diisi!"
                        return@Button
                    }
                    onSave(nama.trim(), noHp.trim(), alamat.trim(), kontak.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
            ) {
                Text("SIMPAN SUPPLIER", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}

@Composable
fun AddTokoGrosirDialog(
    onDismiss: () -> Unit,
    onSave: (namaToko: String, telepon: String, alamat: String) -> Unit
) {
    var namaToko by remember { mutableStateOf("") }
    var telepon by remember { mutableStateOf("") }
    var alamat by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = RetailRedPrimary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("TAMBAH DATA TOKO GROSIR", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Masukkan toko grosir / agen tempat Anda kulakan:", fontSize = 11.sp, color = GrayText)
                OutlinedTextField(
                    value = namaToko,
                    onValueChange = { namaToko = it; errorMsg = "" },
                    label = { Text("Nama Toko Grosir / Agen Kulakan *") },
                    placeholder = { Text("Contoh: Toko Grosir Berkah, Agen Sembako Makmur") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = telepon,
                    onValueChange = { telepon = it },
                    label = { Text("Nomor Telepon Toko Grosir") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                OutlinedTextField(
                    value = alamat,
                    onValueChange = { alamat = it },
                    label = { Text("Alamat / Pasar Tempat Grosir") },
                    placeholder = { Text("Contoh: Pasar Induk Kramat Jati Blok A") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = RetailRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (namaToko.isBlank()) {
                        errorMsg = "Nama toko grosir wajib diisi!"
                        return@Button
                    }
                    onSave(namaToko.trim(), telepon.trim(), alamat.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
            ) {
                Text("SIMPAN TOKO GROSIR", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("BATAL")
            }
        }
    )
}
