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
import com.example.data.local.entity.ReturnDocEntity
import com.example.ui.screens.pos.CameraBarcodeScannerDialog
import com.example.ui.screens.pos.LovProductDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReturnCartItem(
    val product: ProductEntity,
    var qty: Int,
    var hargaBeli: Double,
    var alasan: String = "BKE (Expired)",
    var satuan: String = product.satuan,
    var konversi: Int = 1,
    var updateMasterProduct: Boolean = false
) {
    val subtotal: Double get() = qty * hargaBeli
    val totalBaseQty: Int get() = qty * maxOf(1, konversi)
}

enum class SumberReturTipe {
    SUPPLIER,
    TOKO_GROSIR
}

val ALASAN_RETUR_OPTIONS = listOf(
    "BKE (Expired / Kadaluwarsa)",
    "BR (Barang Rusak / Fisik)",
    "BRP (Salah Kirim / Beda Barang)",
    "BTA (Barang Tidak Ada / Kurang)",
    "BKC (Barang Kurang / Klaim)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnPurchaseScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val allProducts by viewModel.allProducts.collectAsState()
    val allReturns by viewModel.allReturns.collectAsState()
    val allSuppliers by viewModel.allSuppliers.collectAsState()
    val allStoreBranches by viewModel.allStoreBranches.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Input Retur, 1: Riwayat & Arsip NRB

    // Sumber Retur: Supplier vs Toko Grosir
    var sumberTipe by remember { mutableStateOf(SumberReturTipe.SUPPLIER) }
    var selectedSupplierNama by remember { mutableStateOf("") }
    var selectedTokoGrosirNama by remember { mutableStateOf("") }

    // Dokumen Header
    var nrbInput by remember { mutableStateOf("NRB-" + (100000..999999).random()) }
    var fakturAsalInput by remember { mutableStateOf("") }
    var petugasNamaInput by remember { mutableStateOf("") }
    var alasanUtama by remember { mutableStateOf("BKE (Expired / Kadaluwarsa)") }
    var catatanTambahan by remember { mutableStateOf("") }

    // Init Defaults
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

    val returnCart = remember { mutableStateListOf<ReturnCartItem>() }
    var itemForUnitChange by remember { mutableStateOf<Pair<ReturnCartItem, Int>?>(null) }
    var barcodeInput by remember { mutableStateOf("") }

    // Dialog flags
    var showLovDialog by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAddTokoGrosirDialog by remember { mutableStateOf(false) }

    var feedbackMessage by remember { mutableStateOf("") }
    var selectedNrbForDoc by remember { mutableStateOf<ReturnDocEntity?>(null) }
    var docSaveMsg by remember { mutableStateOf("") }
    var riwayatSearchQuery by remember { mutableStateOf("") }
    var riwayatStatusFilter by remember { mutableStateOf("SEMUA") } // SEMUA, FORM, FIXED

    val totalNominal = returnCart.sumOf { it.subtotal }
    val totalQty = returnCart.sumOf { it.qty }

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

            val existing = returnCart.find { it.product.plu == found.plu && it.satuan == initSatuan }
            if (existing != null) {
                val idx = returnCart.indexOf(existing)
                returnCart[idx] = existing.copy(qty = existing.qty + 1)
            } else {
                returnCart.add(
                    ReturnCartItem(
                        product = found,
                        qty = 1,
                        hargaBeli = initHarga,
                        alasan = alasanUtama,
                        satuan = initSatuan,
                        konversi = initKonversi
                    )
                )
            }
            feedbackMessage = "Berhasil ditambahkan ke retur: ${found.nama} ($initSatuan)"
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
                        Text("RETUR PEMBELIAN (NRB)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Pengembalian Barang ke Supplier & Toko Grosir • Potong Stok Otomatis",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
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
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("INPUT RETUR PEMBELIAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                            Text("RIWAYAT & ARSIP NRB (${allReturns.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                    // CARD 1: PILIHAN SUMBER / TUJUAN RETUR
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, PosCardBorder),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "1. PILIH TUJUAN PENGEMBALIAN BARANG (RETUR):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RetailNavy
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Toggle Segmented: Retur ke Supplier vs Retur ke Toko Grosir
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Pilihan 1: Retur ke Supplier
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sumberTipe == SumberReturTipe.SUPPLIER) RetailNavy else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.5.dp, if (sumberTipe == SumberReturTipe.SUPPLIER) RetailNavy else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { sumberTipe = SumberReturTipe.SUPPLIER }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = if (sumberTipe == SumberReturTipe.SUPPLIER) Color.White else RetailNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    "RETUR KE SUPPLIER",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sumberTipe == SumberReturTipe.SUPPLIER) Color.White else Color(0xFF1E293B)
                                                )
                                                Text(
                                                    "Distributor / Pabrik / DC",
                                                    fontSize = 9.sp,
                                                    color = if (sumberTipe == SumberReturTipe.SUPPLIER) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }

                                    // Pilihan 2: Retur ke Toko Grosir
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (sumberTipe == SumberReturTipe.TOKO_GROSIR) RetailRedPrimary else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.5.dp, if (sumberTipe == SumberReturTipe.TOKO_GROSIR) RetailRedPrimary else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { sumberTipe = SumberReturTipe.TOKO_GROSIR }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = if (sumberTipe == SumberReturTipe.TOKO_GROSIR) Color.White else RetailRedPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    "RETUR KE TOKO GROSIR",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (sumberTipe == SumberReturTipe.TOKO_GROSIR) Color.White else Color(0xFF1E293B)
                                                )
                                                Text(
                                                    "Agen / Pasar Kulakan",
                                                    fontSize = 9.sp,
                                                    color = if (sumberTipe == SumberReturTipe.TOKO_GROSIR) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Dropdown & Tombol Tambah Sumber Retur
                                if (sumberTipe == SumberReturTipe.SUPPLIER) {
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
                                            Text("+ Tambah Supplier", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                                // No NRB & Petugas Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = nrbInput,
                                        onValueChange = { nrbInput = it },
                                        label = { Text("No. Dokumen NRB") },
                                        modifier = Modifier.weight(1.1f),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { nrbInput = "NRB-" + (100000..999999).random() }) {
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

                                Spacer(modifier = Modifier.height(8.dp))

                                // Faktur Beli Asal & Alasan Utama Retur
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    var expFaktur by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expFaktur,
                                        onExpandedChange = { expFaktur = !expFaktur },
                                        modifier = Modifier.weight(1.1f)
                                    ) {
                                        OutlinedTextField(
                                            value = fakturAsalInput,
                                            onValueChange = { fakturAsalInput = it },
                                            label = { Text("No. Faktur Beli Asal") },
                                            placeholder = { Text("Contoh: FB-123456") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFaktur) },
                                            singleLine = true
                                        )
                                        if (allPurchases.isNotEmpty()) {
                                            ExposedDropdownMenu(
                                                expanded = expFaktur,
                                                onDismissRequest = { expFaktur = false }
                                            ) {
                                                allPurchases.take(10).forEach { p ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column {
                                                                Text("${p.noFakturBeli} (${p.supplierNama})", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                                Text("${p.tanggal} - ${formatRupiah(p.totalBeli)}", fontSize = 10.sp, color = GrayText)
                                                            }
                                                        },
                                                        onClick = {
                                                            fakturAsalInput = p.noFakturBeli
                                                            if (sumberTipe == SumberReturTipe.SUPPLIER && p.supplierNama.isNotBlank()) {
                                                                selectedSupplierNama = p.supplierNama
                                                            }
                                                            expFaktur = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    var expAlasan by remember { mutableStateOf(false) }
                                    ExposedDropdownMenuBox(
                                        expanded = expAlasan,
                                        onExpandedChange = { expAlasan = !expAlasan },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = alasanUtama,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Alasan Retur Utama") },
                                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expAlasan) },
                                            singleLine = true
                                        )
                                        ExposedDropdownMenu(
                                            expanded = expAlasan,
                                            onDismissRequest = { expAlasan = false }
                                        ) {
                                            ALASAN_RETUR_OPTIONS.forEach { r ->
                                                DropdownMenuItem(
                                                    text = { Text(r, fontSize = 11.sp) },
                                                    onClick = {
                                                        alasanUtama = r
                                                        expAlasan = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // CARD 2: SCAN & INPUT PRODUK RETUR (PERSIS SEPERTI LAYAR KASIR & PEMBELIAN)
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
                                    OutlinedTextField(
                                        value = barcodeInput,
                                        onValueChange = { barcodeInput = it },
                                        placeholder = { Text("Scan Barcode / Ketik PLU...", fontSize = 12.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.QrCode, contentDescription = null, tint = RetailNavy)
                                        },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("return_barcode_input"),
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

                    // CARD 3: DAFTAR BARANG YANG AKAN DIRETUR
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "3. RINCIAN BARANG RETUR (${returnCart.size} Macam, $totalQty Pcs)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RetailNavy
                            )
                            if (returnCart.isNotEmpty()) {
                                TextButton(
                                    onClick = { returnCart.clear() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Kosongkan", color = RetailRedPrimary, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    if (returnCart.isEmpty()) {
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
                                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Belum ada barang di daftar retur.", fontWeight = FontWeight.Bold, color = GrayText)
                                        Text("Gunakan Scan Kamera atau Tombol LOV untuk memilih barang yang rusak/expired/kurang.", fontSize = 11.sp, color = GrayText)
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(returnCart) { index, item ->
                            val currentStock = item.product.stok
                            val isExceedingStock = item.totalBaseQty > currentStock

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, if (isExceedingStock) RetailAmber else PosCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Row 1: Nama Barang & Tombol Hapus
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
                                                Surface(
                                                    color = if (isExceedingStock) Color(0xFFFEF3C7) else Color(0xFFE2E8F0),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        "Stok: $currentStock ${item.product.satuan}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isExceedingStock) Color(0xFFB45309) else Color(0xFF334155),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(4.dp)) {
                                                    Text(
                                                        "Modal: ${formatRupiah(item.hargaBeli)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0369A1),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                // Tombol / Badge Ubah Satuan Retur
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
                                            onClick = { returnCart.removeAt(index) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RetailRedPrimary)
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))

                                    // Row 2: Input Harga Beli / Modal Pengembalian, Stepper Qty Retur, Subtotal
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Kolom Harga Beli (per Satuan yang dipilih)
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text("Harga / ${item.satuan} (Rp)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                            var priceStr by remember(item.hargaBeli) { mutableStateOf(item.hargaBeli.toLong().toString()) }
                                            OutlinedTextField(
                                                value = priceStr,
                                                onValueChange = {
                                                    priceStr = it
                                                    val parsed = it.toDoubleOrNull() ?: 0.0
                                                    returnCart[index] = item.copy(hargaBeli = parsed)
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

                                        // Kolom Qty dengan tombol [-] dan [+] (Tampilan Luas & Angka Terlihat Sempurna)
                                        Column(modifier = Modifier.weight(1.35f)) {
                                            Text("Qty Retur (${item.satuan})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.height(48.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(34.dp).clickable {
                                                        if (item.qty > 1) {
                                                            returnCart[index] = item.copy(qty = item.qty - 1)
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
                                                            returnCart[index] = item.copy(qty = maxOf(1, parsed))
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
                                                        returnCart[index] = item.copy(qty = item.qty + 1)
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
                                                color = RetailRedPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Pilihan Alasan Retur per Item
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Alasan:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText, modifier = Modifier.padding(end = 4.dp))
                                        val reasonCodes = listOf("BKE (Expired)", "BR (Rusak)", "BRP (Beda)", "BTA (Kurang)")
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            reasonCodes.forEach { code ->
                                                val isSelected = item.alasan.startsWith(code.take(3))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isSelected) RetailNavy else Color(0xFFF1F5F9),
                                                    modifier = Modifier.clickable {
                                                        returnCart[index] = item.copy(alasan = code)
                                                    }
                                                ) {
                                                    Text(
                                                        code.take(3),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isExceedingStock) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "⚠️ Perhatian: Qty retur (${item.qty} ${item.satuan} = ${item.totalBaseQty} ${item.product.satuan}) melebihi stok fisik toko ($currentStock ${item.product.satuan}).",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RetailRedPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // CARD 4: EDUKASI / PROSES BISNIS RETUR
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RetailAmberLight),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "INFORMASI ALUR RETUR PEMBELIAN (NRB - NOTA RETUR BARANG):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF78350F)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "• DRAFT FORM RETUR: Dokumen dicatat sebagai usulan retur. Stok fisik toko BELUM berkurang.\n" +
                                    "• FIXED RETUR: Dokumen difinalisasi, STOK FISIK OTOMATIS TERPOTONG dari inventaris, dan nota sah 3-ply diterbitkan.",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    // CARD 5: RINGKASAN & TOMBOL SIMPAN / FIXED
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
                                            "Tujuan: ${if (sumberTipe == SumberReturTipe.SUPPLIER) "Supplier ($selectedSupplierNama)" else "Toko Grosir ($selectedTokoGrosirNama)"}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RetailNavy
                                        )
                                        Text("Petugas: $petugasNamaInput", fontSize = 11.sp, color = GrayText)
                                        val totalBasePcs = returnCart.sumOf { it.totalBaseQty }
                                        val totalLabel = if (totalBasePcs != totalQty) "Total: $totalQty Unit ($totalBasePcs Pcs Dasar)" else "Total: $totalQty Pcs"
                                        Text("$totalLabel • ${returnCart.size} Macam Produk", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("TOTAL NILAI RETUR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                                        Text(
                                            formatRupiah(totalNominal),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = RetailRedPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Tombol Aksi: Simpan Form (Draft) vs Proses Fixed (Potong Stok)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Tombol 1: Simpan DRAFT (FORM)
                                    OutlinedButton(
                                        onClick = {
                                            if (returnCart.isEmpty()) {
                                                feedbackMessage = "Daftar barang retur masih kosong!"
                                                return@OutlinedButton
                                            }
                                            val sumberNama = if (sumberTipe == SumberReturTipe.SUPPLIER) {
                                                selectedSupplierNama.ifBlank { "Supplier Utama" }
                                            } else {
                                                selectedTokoGrosirNama.ifBlank { "Toko Grosir" }
                                            }

                                            val itemsToSave = returnCart.map {
                                                it.product to Pair(it.qty, it.hargaBeli)
                                            }
                                            val reasonsMap = returnCart.associate { it.product.plu to it.alasan }
                                            val itemUnitsMap = returnCart.associate { it.product.plu to Pair(it.satuan, it.konversi) }
                                            returnCart.forEach { item ->
                                                if (item.updateMasterProduct) {
                                                    viewModel.updateProductUnit(item.product.plu, item.satuan)
                                                }
                                            }

                                            viewModel.recordReturnPurchase(
                                                nrbNo = nrbInput,
                                                sumberTipe = if (sumberTipe == SumberReturTipe.SUPPLIER) "SUPPLIER" else "TOKO_GROSIR",
                                                sumberNama = sumberNama,
                                                fakturAsal = fakturAsalInput.ifBlank { "FAK-LANGSUNG" },
                                                petugasNama = petugasNamaInput,
                                                alasanUtama = alasanUtama,
                                                items = itemsToSave,
                                                itemReasons = reasonsMap,
                                                itemUnits = itemUnitsMap,
                                                isFixed = false
                                            ) { success ->
                                                if (success) {
                                                    feedbackMessage = "Form Retur $nrbInput berhasil disimpan sebagai DRAFT! (Stok belum dipotong)"
                                                    returnCart.clear()
                                                    nrbInput = "NRB-" + (100000..999999).random()
                                                } else {
                                                    feedbackMessage = "Gagal menyimpan Form Retur."
                                                }
                                            }
                                        },
                                        enabled = returnCart.isNotEmpty(),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("SIMPAN FORM (DRAFT)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    // Tombol 2: Proses FIXED (Potong Stok Langsung)
                                    Button(
                                        onClick = {
                                            if (returnCart.isEmpty()) {
                                                feedbackMessage = "Daftar barang retur masih kosong!"
                                                return@Button
                                            }
                                            val sumberNama = if (sumberTipe == SumberReturTipe.SUPPLIER) {
                                                selectedSupplierNama.ifBlank { "Supplier Utama" }
                                            } else {
                                                selectedTokoGrosirNama.ifBlank { "Toko Grosir" }
                                            }

                                            val currentNrb = nrbInput
                                            val itemsToSave = returnCart.map {
                                                it.product to Pair(it.qty, it.hargaBeli)
                                            }
                                            val reasonsMap = returnCart.associate { it.product.plu to it.alasan }
                                            val itemUnitsMap = returnCart.associate { it.product.plu to Pair(it.satuan, it.konversi) }
                                            returnCart.forEach { item ->
                                                if (item.updateMasterProduct) {
                                                    viewModel.updateProductUnit(item.product.plu, item.satuan)
                                                }
                                            }

                                            viewModel.recordReturnPurchase(
                                                nrbNo = currentNrb,
                                                sumberTipe = if (sumberTipe == SumberReturTipe.SUPPLIER) "SUPPLIER" else "TOKO_GROSIR",
                                                sumberNama = sumberNama,
                                                fakturAsal = fakturAsalInput.ifBlank { "FAK-LANGSUNG" },
                                                petugasNama = petugasNamaInput,
                                                alasanUtama = alasanUtama,
                                                items = itemsToSave,
                                                itemReasons = reasonsMap,
                                                itemUnits = itemUnitsMap,
                                                isFixed = true
                                            ) { success ->
                                                if (success) {
                                                    val docCreated = ReturnDocEntity(
                                                        nrbNo = currentNrb,
                                                        tipeRetur = if (sumberTipe == SumberReturTipe.SUPPLIER) "SUPPLIER" else "TOKO_GROSIR",
                                                        refNo = fakturAsalInput.ifBlank { "FAK-LANGSUNG" },
                                                        fakturAsal = fakturAsalInput.ifBlank { "FAK-LANGSUNG" },
                                                        supplierNama = sumberNama,
                                                        jenisRetur = if (sumberTipe == SumberReturTipe.SUPPLIER) "RETUR SUPPLIER" else "RETUR TOKO GROSIR",
                                                        tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                                                        totalNilai = totalNominal,
                                                        alasan = alasanUtama,
                                                        keteranganRetur = alasanUtama,
                                                        status = "FIXED",
                                                        petugasNama = petugasNamaInput,
                                                        tokoTujuan = if (sumberTipe == SumberReturTipe.SUPPLIER) "Supplier" else "Toko Grosir",
                                                        totalQty = totalQty
                                                    )
                                                    selectedNrbForDoc = docCreated
                                                    feedbackMessage = "Retur $currentNrb berhasil FIXED! Stok toko telah dipotong secara otomatis."
                                                    returnCart.clear()
                                                    nrbInput = "NRB-" + (100000..999999).random()
                                                } else {
                                                    feedbackMessage = "Gagal memproses Fixed Retur."
                                                }
                                            }
                                        },
                                        enabled = returnCart.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.3f).height(48.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PROSES FIXED (POTONG STOK)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // TAB 1: RIWAYAT & ARSIP DOKUMEN NRB
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Search Bar
                    OutlinedTextField(
                        value = riwayatSearchQuery,
                        onValueChange = { riwayatSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari No NRB, Supplier, Toko, Faktur Asal, Petugas...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        colors = retailTextFieldColors()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Status: SEMUA, FORM / DRAFT, FIXED
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SEMUA", "FORM", "FIXED").forEach { st ->
                            val isSel = riwayatStatusFilter == st
                            val count = when (st) {
                                "FORM" -> allReturns.count { it.status == "FORM" }
                                "FIXED" -> allReturns.count { it.status == "FIXED" }
                                else -> allReturns.size
                            }
                            FilterChip(
                                selected = isSel,
                                onClick = { riwayatStatusFilter = st },
                                label = { Text("$st ($count)", fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredReturns = allReturns.filter { ret ->
                        val matchSearch = riwayatSearchQuery.isBlank() ||
                                ret.nrbNo.contains(riwayatSearchQuery, ignoreCase = true) ||
                                ret.supplierNama.contains(riwayatSearchQuery, ignoreCase = true) ||
                                ret.fakturAsal.contains(riwayatSearchQuery, ignoreCase = true) ||
                                ret.petugasNama.contains(riwayatSearchQuery, ignoreCase = true) ||
                                ret.alasan.contains(riwayatSearchQuery, ignoreCase = true)

                        val matchStatus = when (riwayatStatusFilter) {
                            "FORM" -> ret.status == "FORM"
                            "FIXED" -> ret.status == "FIXED"
                            else -> true
                        }
                        matchSearch && matchStatus
                    }

                    if (filteredReturns.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada dokumen retur yang sesuai.", color = GrayText)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filteredReturns) { ret ->
                                val isFixed = ret.status == "FIXED"
                                val isGrosir = ret.tipeRetur.contains("GROSIR", ignoreCase = true) || ret.jenisRetur.contains("GROSIR", ignoreCase = true)

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, PosCardBorder),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header Row: No NRB & Status Badge
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
                                                Text(ret.nrbNo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }

                                            Surface(
                                                color = if (isFixed) RetailGreen else RetailAmber,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    if (isFixed) "FIXED (TERPOTONG)" else "DRAFT (FORM)",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            "Kepada: ${ret.supplierNama}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        if (ret.fakturAsal.isNotBlank()) {
                                            Text("Faktur Beli Asal: ${ret.fakturAsal}", fontSize = 11.sp, color = GrayText)
                                        }
                                        Text("Tanggal: ${ret.tanggal} | Petugas: ${ret.petugasNama}", fontSize = 10.sp, color = GrayText)
                                        Text("Alasan: ${ret.keteranganRetur}", fontSize = 11.sp, color = Color(0xFF334155))

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                formatRupiah(ret.totalNilai),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = RetailRedPrimary
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // Jika status FORM, bisa langsung Fixed
                                                if (!isFixed) {
                                                    Button(
                                                        onClick = {
                                                            viewModel.finalizeReturnDoc(ret) { success ->
                                                                if (success) {
                                                                    selectedNrbForDoc = ret.copy(status = "FIXED")
                                                                    feedbackMessage = "Retur ${ret.nrbNo} berhasil FIXED! Stok toko telah dipotong."
                                                                }
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Proses Fixed", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Tombol Cetak / Lihat Nota 3-Ply
                                                OutlinedButton(
                                                    onClick = { selectedNrbForDoc = ret },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Cetak NRB (3-Ply)", fontSize = 10.sp)
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
    }

    // Modal LOV Produk
    if (showLovDialog) {
        LovProductDialog(
            products = allProducts,
            onDismiss = { showLovDialog = false },
            onSelectProduct = { prod, qty ->
                val addQty = maxOf(1, qty)
                val existing = returnCart.find { it.product.plu == prod.plu && it.satuan == prod.satuan }
                if (existing != null) {
                    val idx = returnCart.indexOf(existing)
                    returnCart[idx] = existing.copy(qty = existing.qty + addQty)
                } else {
                    returnCart.add(
                        ReturnCartItem(
                            product = prod,
                            qty = addQty,
                            hargaBeli = prod.hargaBeli,
                            alasan = alasanUtama,
                            satuan = prod.satuan,
                            konversi = 1
                        )
                    )
                }
                feedbackMessage = "Berhasil ditambahkan ke retur: ${prod.nama} ($addQty ${prod.satuan})"
                showLovDialog = false
            }
        )
    }

    // Modal Scan Kamera
    if (showCameraScannerDialog) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScannerDialog = false },
            onBarcodeDetected = { scannedCode ->
                showCameraScannerDialog = false
                addProductByCode(scannedCode)
            }
        )
    }

    // Modal Tambah Supplier
    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onSave = { nama, noHp, alamat, kontak ->
                viewModel.addSupplier(nama, noHp, alamat, kontak) {
                    selectedSupplierNama = nama
                    showAddSupplierDialog = false
                    feedbackMessage = "Supplier '$nama' berhasil ditambahkan!"
                }
            }
        )
    }

    // Modal Tambah Toko Grosir
    if (showAddTokoGrosirDialog) {
        AddTokoGrosirDialog(
            onDismiss = { showAddTokoGrosirDialog = false },
            onSave = { namaToko, telepon, alamat ->
                viewModel.addStoreBranch(namaToko, alamat, telepon) {
                    selectedTokoGrosirNama = namaToko
                    showAddTokoGrosirDialog = false
                    feedbackMessage = "Toko Grosir '$namaToko' berhasil ditambahkan!"
                }
            }
        )
    }

    // Modal Ubah Satuan Retur (Multi-Satuan & Konversi)
    itemForUnitChange?.let { (item, index) ->
        ItemUnitDialog(
            product = item.product,
            currentSatuan = item.satuan,
            currentKonversi = item.konversi,
            currentHargaBeli = item.hargaBeli,
            dialogContext = "Retur",
            onDismiss = { itemForUnitChange = null },
            onConfirm = { cfg ->
                if (cfg.updateMasterProduct) {
                    viewModel.updateProductUnit(item.product.plu, cfg.satuan)
                }
                returnCart[index] = item.copy(
                    satuan = cfg.satuan,
                    konversi = cfg.konversi,
                    hargaBeli = cfg.hargaBeli,
                    updateMasterProduct = cfg.updateMasterProduct
                )
                itemForUnitChange = null
            }
        )
    }

    // Modal Cetak & Lihat Dokumen NRB 3-Ply (Odner 03 Format)
    selectedNrbForDoc?.let { doc ->
        val itemsList = remember(doc.itemsJson) {
            val list = mutableListOf<Map<String, Any>>()
            if (doc.itemsJson.isNotBlank()) {
                try {
                    val arr = JSONArray(doc.itemsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            mapOf(
                                "plu" to obj.optString("plu"),
                                "barcode" to obj.optString("barcode"),
                                "nama" to obj.optString("nama"),
                                "satuan" to obj.optString("satuan", "PCS"),
                                "konversi" to obj.optInt("konversi", 1),
                                "qty" to obj.optInt("qty", 1),
                                "hargaBeli" to obj.optDouble("hargaBeli", 0.0),
                                "subtotal" to obj.optDouble("subtotal", 0.0),
                                "alasan" to obj.optString("alasan", doc.alasan)
                            )
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            list
        }

        val docText = remember(doc, itemsList) {
            buildString {
                appendLine("========================================")
                appendLine("         DODOLAN PRO RETAIL POS")
                appendLine("   NOTA RETUR BARANG (NRB - ${doc.status})")
                appendLine("========================================")
                appendLine("NOMOR NRB       : ${doc.nrbNo}")
                appendLine("TANGGAL         : ${doc.tanggal}")
                appendLine("TUJUAN RETUR    : ${doc.supplierNama}")
                appendLine("JENIS TUJUAN    : ${doc.jenisRetur}")
                if (doc.fakturAsal.isNotBlank()) {
                    appendLine("FAKTUR ASAL     : ${doc.fakturAsal}")
                }
                appendLine("PETUGAS INPUT   : ${doc.petugasNama}")
                appendLine("ALASAN UTAMA    : ${doc.alasan}")
                appendLine("STATUS DOKUMEN  : ${if (doc.status == "FIXED") "FIXED (Stok Fisik Telah Terpotong)" else "FORM (Draft Pending)"}")
                appendLine("ARSIP           : ODNER NO. 03 (NOTA RETUR)")
                appendLine("----------------------------------------")
                appendLine("RINCIAN BARANG:")
                appendLine("PLU    NAMA BARANG          QTY      HARGA      SUBTOTAL")
                appendLine("----------------------------------------")
                if (itemsList.isNotEmpty()) {
                    itemsList.forEach { itm ->
                        val plu = (itm["plu"] as? String ?: "").take(6).padEnd(6)
                        val nama = (itm["nama"] as? String ?: "").take(18).padEnd(18)
                        val satuan = itm["satuan"] as? String ?: "PCS"
                        val konv = itm["konversi"] as? Int ?: 1
                        val qty = "${itm["qty"]} $satuan".take(8).padStart(8)
                        val harga = (itm["hargaBeli"] as? Double ?: 0.0).toLong().toString().padStart(8)
                        val sub = (itm["subtotal"] as? Double ?: 0.0).toLong().toString().padStart(9)
                        appendLine("$plu $nama $qty $harga $sub")
                        val itemAlasan = itm["alasan"] as? String ?: ""
                        val konvNote = if (konv > 1) " [Konversi: 1 $satuan = $konv Pcs]" else ""
                        if (itemAlasan.isNotBlank() || konvNote.isNotBlank()) {
                            appendLine("       [Keterangan: $itemAlasan$konvNote]")
                        }
                    }
                } else {
                    appendLine("Barang Retur Umum / Nilai Nominal: ${formatRupiah(doc.totalNilai)}")
                }
                appendLine("----------------------------------------")
                appendLine("TOTAL FISIK DIRETUR : ${doc.totalQty} Pcs")
                appendLine("TOTAL NILAI RETUR   : ${formatRupiah(doc.totalNilai)}")
                appendLine("========================================")
                appendLine("DISTRIBUSI LEMBAR NOTA (3-PLY):")
                appendLine("[1] Lembar 1 (Putih) : Diserahkan ke Supplier / DC")
                appendLine("[2] Lembar 2 (Merah) : Arsip Keuangan Toko (Odner 03)")
                appendLine("[3] Lembar 3 (Kuning): Arsip Logistik / Gudang")
                appendLine("----------------------------------------")
                appendLine("Tanda Tangan & Cap Sah:")
                appendLine("")
                appendLine("   Petugas Toko           Penerima Supplier")
                appendLine("  (_____________)        (_________________)")
                appendLine("========================================")
            }
        }

        AlertDialog(
            onDismissRequest = {
                selectedNrbForDoc = null
                docSaveMsg = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NOTA RETUR BARANG (3-PLY)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, PosCardBorder)
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    text = docText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = Color.Black
                                )
                            }
                        }
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
                                subfolder = "Retur",
                                prefix = "NRB_${doc.nrbNo}",
                                extension = "txt",
                                content = docText
                            )
                            docSaveMsg = "Tersimpan di DodolanPro/Retur/${saved.second.name}"
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN TXT", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val saved = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Retur",
                                prefix = "NRB_${doc.nrbNo}",
                                extension = "txt",
                                content = docText
                            )
                            DocExportManager.shareDocument(context, saved.second, "Nota Retur ${doc.nrbNo}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BAGIKAN", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            selectedNrbForDoc = null
                            docSaveMsg = ""
                        }
                    ) {
                        Text("TUTUP", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        )
    }
}
