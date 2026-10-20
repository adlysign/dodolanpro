package com.example.ui.screens.sis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.allProductsIncludingInactive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // Semua, Promo, Repack, Stok Tipis
    var selectedCategory by remember { mutableStateOf("Semua Kategori") }
    var showProductEditorDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    val categoriesList = remember(products) {
        listOf("Semua Kategori") + products.map { it.kategori.trim().ifEmpty { "Umum" } }.distinct().sorted()
    }

    val filteredProducts = remember(products, searchQuery, selectedFilter, selectedCategory) {
        products.filter { p ->
            val matchesFilter = when (selectedFilter) {
                "Promo" -> p.isPromo || p.hasDiscountOrPromo()
                "Repack" -> p.isRepack
                "Stok Tipis" -> p.stok <= p.minStok
                else -> true
            }
            val matchesCategory = selectedCategory == "Semua Kategori" || p.kategori.equals(selectedCategory, ignoreCase = true)
            matchesFilter && matchesCategory && (
                p.nama.contains(searchQuery, ignoreCase = true) ||
                p.plu.contains(searchQuery) ||
                p.barcode.contains(searchQuery) ||
                p.barcodeRepack.contains(searchQuery) ||
                p.barcodeRepack2.contains(searchQuery) ||
                p.rakPlanogram.contains(searchQuery, ignoreCase = true) ||
                p.satuanRepack.contains(searchQuery, ignoreCase = true)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PENGATURAN PRODUK & HARGA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Katalog, Promo Lengkap & Multi-Kemasan Repack", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showProductEditorDialog = true
                },
                containerColor = RetailRedPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("btn_tambah_produk")
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TAMBAH PRODUK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
                .padding(12.dp)
        ) {
            // Search Bar & Stats
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("product_search_input"),
                placeholder = { Text("Cari PLU / Barcode / Nama / Repack (Karton, Dus, Slop)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = retailTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Status (Kontras tinggi, anti-putih, teks terbaca jelas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Semua", "Promo", "Repack", "Stok Tipis").forEach { filter ->
                    val isSel = selectedFilter == filter
                    val labelText = when (filter) {
                        "Semua" -> "Semua (${products.size})"
                        "Promo" -> "Promo (${products.count { it.isPromo || it.hasDiscountOrPromo() }})"
                        "Repack" -> "Repack (${products.count { it.isRepack }})"
                        "Stok Tipis" -> "Stok Tipis (${products.count { it.stok <= it.minStok }})"
                        else -> filter
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) RetailNavy else Color(0xFFE2E8F0),
                        border = BorderStroke(1.5.dp, if (isSel) RetailNavy else Color(0xFF94A3B8)),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp).padding(end = 4.dp))
                            }
                            Text(
                                text = labelText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Chips Kategori (Tampilan jelas, warna kontras, tidak putih polos sebelum dipilih)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categoriesList) { cat ->
                    val isSel = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) RetailNavy else Color(0xFFE2E8F0),
                        border = BorderStroke(1.5.dp, if (isSel) RetailNavy else Color(0xFF94A3B8)),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp).padding(end = 4.dp))
                            } else {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(12.dp).padding(end = 4.dp))
                            }
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (feedbackMessage.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(feedbackMessage, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Products List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredProducts) { prod ->
                    ProductItemCard(
                        product = prod,
                        onClick = {
                            productToEdit = prod
                            showProductEditorDialog = true
                        }
                    )
                }
            }
        }
    }

    // Modal Editor: Add or Edit Product with Full Promo & Repack Controls
    if (showProductEditorDialog) {
        ProductEditorDialog(
            productToEdit = productToEdit,
            onDismiss = { showProductEditorDialog = false },
            onSave = { updatedProduct ->
                val isEdit = productToEdit != null
                viewModel.saveOrUpdateProduct(updatedProduct, isEdit = isEdit) {
                    feedbackMessage = "Produk ${updatedProduct.nama} berhasil disimpan!"
                    showProductEditorDialog = false
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
            .testTag("product_card_${product.plu}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title & Price Row (Dengan Harga Dasar Dicoret Bila Promo)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    Text("PLU: ${product.plu} | BC: ${product.barcode} | Rak: ${product.rakPlanogram}", fontSize = 11.sp, color = Color.DarkGray)
                }

                // Price display: normal or strikethrough with promo
                Column(horizontalAlignment = Alignment.End) {
                    if (product.hasDiscountOrPromo() || product.isPromo) {
                        Text(
                            text = formatRupiah(product.hargaJual),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                        )
                        Text(
                            text = formatRupiah(product.getEffectiveDiscountedPrice()),
                            fontWeight = FontWeight.Black,
                            color = RetailRedPrimary,
                            fontSize = 15.sp
                        )
                    } else {
                        Text(
                            text = formatRupiah(product.hargaJual),
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontSize = 15.sp
                        )
                    }
                    Text("Beli: ${formatRupiah(product.hargaBeli)}", fontSize = 10.sp, color = GrayText)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stock & Category Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (product.stok <= product.minStok) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Stok: ${product.stok} ${product.satuan}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (product.stok <= product.minStok) RetailRedPrimary else RetailGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kat: ${product.kategori}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }

                // Edit indicator
                Text("Tap untuk Edit >", fontSize = 10.sp, color = RetailNavy, fontWeight = FontWeight.Bold)
            }

            // Promo Badge Section
            if (product.isPromo || product.hasDiscountOrPromo()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "PROMO: ${product.getEffectivePromoLabel()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                    }
                }
            }

            // Repack Badges Section (Karton, Dus, Pak, Slop)
            if (product.isRepack && product.satuanRepack.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFE1F5FE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81D4FA)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = Color(0xFF0277BD), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "REPACK 1: ${product.satuanRepack} (Isi ${product.isiRepack} ${product.satuan}) = ${formatRupiah(product.hargaRepack)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF01579B)
                            )
                        }
                        if (product.barcodeRepack.isNotBlank()) {
                            Text("   Barcode Repack: ${product.barcodeRepack}", fontSize = 9.sp, color = Color(0xFF0277BD))
                        }
                        if (product.satuanRepack2.isNotBlank()) {
                            Text(
                                "   REPACK 2: ${product.satuanRepack2} (Isi ${product.isiRepack2} ${product.satuan}) = ${formatRupiah(product.hargaRepack2)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF01579B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductEditorDialog(
    productToEdit: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    val isEdit = productToEdit != null

    // Base Fields
    var plu by remember { mutableStateOf(productToEdit?.plu ?: Random.nextInt(1000, 9999).toString()) }
    var barcode by remember { mutableStateOf(productToEdit?.barcode ?: plu) }
    var nama by remember { mutableStateOf(productToEdit?.nama ?: "") }
    var kategori by remember { mutableStateOf(productToEdit?.kategori ?: "Umum") }
    var rak by remember { mutableStateOf(productToEdit?.rakPlanogram ?: "001-01") }
    var hargaBeliStr by remember { mutableStateOf(productToEdit?.hargaBeli?.toLong()?.toString() ?: "0") }
    var hargaJualStr by remember { mutableStateOf(productToEdit?.hargaJual?.toLong()?.toString() ?: "0") }
    var stokStr by remember { mutableStateOf(productToEdit?.stok?.toString() ?: "20") }
    var minStokStr by remember { mutableStateOf(productToEdit?.minStok?.toString() ?: "5") }
    var satuan by remember { mutableStateOf(productToEdit?.satuan ?: "PCS") }
    var varian by remember { mutableStateOf(productToEdit?.varian ?: "") }

    // Promo Fields
    var isPromo by remember { mutableStateOf(productToEdit?.isPromo ?: false) }
    var promoType by remember { mutableStateOf(productToEdit?.promoType ?: "DISKON_PERSEN") }
    var diskonPersenStr by remember { mutableStateOf(productToEdit?.diskonPersen?.toString() ?: "10.0") }
    var diskonNominalStr by remember { mutableStateOf(productToEdit?.diskonNominal?.toLong()?.toString() ?: "2000") }
    var promoMinBeliStr by remember { mutableStateOf(productToEdit?.promoMinBeli?.toString() ?: "2") }
    var promoGratisQtyStr by remember { mutableStateOf(productToEdit?.promoGratisQty?.toString() ?: "1") }
    var promoPotonganNominalStr by remember { mutableStateOf(productToEdit?.promoPotonganNominal?.toLong()?.toString() ?: "5000") }
    var grosirQtyStr by remember { mutableStateOf(productToEdit?.hargaGrosirQty?.toString() ?: "3") }
    var hargaGrosirStr by remember { mutableStateOf(productToEdit?.hargaGrosir?.toLong()?.toString() ?: "0") }
    var promoLabel by remember { mutableStateOf(productToEdit?.promoLabel ?: "") }
    var promoKeterangan by remember { mutableStateOf(productToEdit?.promoKeterangan ?: "") }

    // Repack Fields (Karton, Dus, Pak, Slop, dll.)
    var isRepack by remember { mutableStateOf(productToEdit?.isRepack ?: false) }
    var satuanRepack by remember { mutableStateOf(productToEdit?.satuanRepack ?: "KARTON") }
    var isiRepackStr by remember { mutableStateOf(productToEdit?.isiRepack?.toString() ?: "24") }
    var hargaRepackStr by remember { mutableStateOf(productToEdit?.hargaRepack?.toLong()?.toString() ?: "0") }
    var barcodeRepack by remember { mutableStateOf(productToEdit?.barcodeRepack ?: (if (barcode.isNotBlank()) "K$barcode" else "")) }

    var hasRepack2 by remember { mutableStateOf((productToEdit?.satuanRepack2 ?: "").isNotBlank()) }
    var satuanRepack2 by remember { mutableStateOf(productToEdit?.satuanRepack2 ?: "PAK") }
    var isiRepack2Str by remember { mutableStateOf(productToEdit?.isiRepack2?.toString() ?: "6") }
    var hargaRepack2Str by remember { mutableStateOf(productToEdit?.hargaRepack2?.toLong()?.toString() ?: "0") }
    var barcodeRepack2 by remember { mutableStateOf(productToEdit?.barcodeRepack2 ?: (if (barcode.isNotBlank()) "P$barcode" else "")) }
    var repackDeskripsi by remember { mutableStateOf(productToEdit?.repackDeskripsi ?: "") }

    // Editor tab: 0 = Info Pokok, 1 = Fitur Promo, 2 = Pengaturan Repack
    var activeTab by remember { mutableStateOf(0) }

    val hargaBeliNum = hargaBeliStr.toDoubleOrNull() ?: 0.0
    val hargaJualNum = hargaJualStr.toDoubleOrNull() ?: 0.0
    val marginPersen = if (hargaJualNum > 0) ((hargaJualNum - hargaBeliNum) / hargaJualNum * 100.0) else 0.0

    val tfColors = retailTextFieldColors()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .imePadding()
                .testTag("product_editor_modal"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = RetailNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isEdit) "EDIT PRODUK: ${productToEdit?.nama}" else "TAMBAH PRODUK BARU",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                "Lengkapi data barang, aturan promo & satuan repack",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                }

                // Tab Selector
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Info Pokok", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isPromo) RetailAmber else Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aturan Promo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isRepack) Color(0xFF0288D1) else Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Repack (Karton/Dus)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Tab Content Body
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // TAB 0: INFO POKOK
                    if (activeTab == 0) {
                        item {
                            Text("KODE & IDENTITAS BARANG", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = plu,
                                    onValueChange = { plu = it },
                                    label = { Text("Kode PLU") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = tfColors
                                )
                                OutlinedTextField(
                                    value = barcode,
                                    onValueChange = { barcode = it },
                                    label = { Text("Barcode Satuan") },
                                    modifier = Modifier.weight(1.4f),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            barcode = System.currentTimeMillis().toString().takeLast(12)
                                        }) {
                                            Icon(Icons.Default.Autorenew, contentDescription = "Gen Barcode", tint = RetailNavy)
                                        }
                                    },
                                    colors = tfColors
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = nama,
                                onValueChange = { nama = it },
                                label = { Text("Nama Barang Lengkap") },
                                modifier = Modifier.fillMaxWidth().testTag("input_product_name"),
                                singleLine = true,
                                colors = tfColors
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = kategori,
                                    onValueChange = { kategori = it },
                                    label = { Text("Kategori") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = tfColors
                                )
                                OutlinedTextField(
                                    value = rak,
                                    onValueChange = { rak = it },
                                    label = { Text("Nomor Rak/Planogram") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = tfColors
                                )
                            }

                            // Quick Category Selection Chips (Teks jelas, anti-putih sebelum dipilih)
                            Text("Pilihan Kategori Cepat:", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val commonCats = listOf("Umum", "Makanan", "Minuman", "Snack", "Sembako", "Rokok", "Personal Care", "Household", "Elektronik")
                                items(commonCats) { cat ->
                                    val isSel = kategori.equals(cat, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSel) RetailNavy else Color(0xFFE2E8F0),
                                        border = BorderStroke(1.5.dp, if (isSel) RetailNavy else Color(0xFF94A3B8)),
                                        modifier = Modifier.clickable { kategori = cat }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSel) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                            }
                                            Text(
                                                cat,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("SATUAN DASAR & STOK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text("Pilih Satuan Dasar:", fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val commonSatuans = listOf("PCS", "BKS", "BOTOL", "CUP", "SACHET", "KG", "LUSIN", "PACK")
                                items(commonSatuans) { s ->
                                    val isSel = satuan.equals(s, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSel) RetailNavy else Color(0xFFE2E8F0),
                                        border = BorderStroke(1.5.dp, if (isSel) RetailNavy else Color(0xFF94A3B8)),
                                        modifier = Modifier.clickable { satuan = s }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isSel) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                            }
                                            Text(
                                                s,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF0F172A)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = satuan,
                                    onValueChange = { satuan = it },
                                    label = { Text("Satuan Unit") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = tfColors
                                )
                                if (isEdit) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.weight(1.2f).height(56.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text("Stok (Terkunci)", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF64748B))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "${productToEdit?.stok ?: 0} $satuan",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = stokStr,
                                        onValueChange = { stokStr = it },
                                        label = { Text("Stok Awal ($satuan)") },
                                        modifier = Modifier.weight(1.2f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors
                                    )
                                }
                                OutlinedTextField(
                                    value = minStokStr,
                                    onValueChange = { minStokStr = it },
                                    label = { Text("Min. Stok Alert") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = tfColors
                                )
                            }

                            Surface(
                                color = if (isEdit) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, if (isEdit) Color(0xFFBFDBFE) else Color(0xFFBBF7D0)),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isEdit) Icons.Default.Lock else Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = if (isEdit) RetailNavy else RetailGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (isEdit)
                                            "Stok terkunci saat edit. Penambahan stok hanya dapat diproses melalui menu Pembelian."
                                        else
                                            "Stok awal dapat diisi atau dibiarkan 0. Penambahan stok selanjutnya melalui menu Pembelian.",
                                        fontSize = 10.sp,
                                        color = if (isEdit) RetailNavy else Color(0xFF166534),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("HARGA BELI & HARGA JUAL REGULER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = hargaBeliStr,
                                    onValueChange = { hargaBeliStr = it },
                                    label = { Text("Harga Beli HPP (Rp)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = tfColors
                                )
                                OutlinedTextField(
                                    value = hargaJualStr,
                                    onValueChange = { hargaJualStr = it },
                                    label = { Text("Harga Jual Normal (Rp)") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = tfColors
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Margin Display
                            Surface(
                                color = if (marginPersen >= 10.0) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Laba Kotor Satuan:", fontSize = 11.sp, color = Color.Black)
                                    Text(
                                        "${formatRupiah(maxOf(0.0, hargaJualNum - hargaBeliNum))} (${String.format(java.util.Locale.US, "%.1f", marginPersen)}%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (marginPersen >= 10.0) RetailGreen else Color(0xFFE65100)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = varian,
                                onValueChange = { varian = it },
                                label = { Text("Varian / Rasa (Opsional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = tfColors
                            )
                        }
                    }

                    // TAB 1: FITUR PROMO LENGKAP
                    if (activeTab == 1) {
                        item {
                            Surface(
                                color = if (isPromo) Color(0xFFFFF8E1) else PosSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isPromo) Color(0xFFFFB300) else PosCardBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isPromo = !isPromo }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Aktifkan Program Promo Produk", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                        Text("Produk akan otomatis menerapkan potongan diskon / bundling saat transaksi kasir.", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Switch(checked = isPromo, onCheckedChange = { isPromo = it })
                                }
                            }

                            if (isPromo) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("PILIH JENIS PROGRAM PROMO:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                Spacer(modifier = Modifier.height(6.dp))

                                val promoTypes = listOf(
                                    Triple("DISKON_PERSEN", "Diskon Persen (%)", "Contoh: Diskon 20%, 50%"),
                                    Triple("DISKON_NOMINAL", "Diskon Potongan Langsung (Rp)", "Contoh: Potongan Rp 2.500 per pcs"),
                                    Triple("BELI_X_GRATIS_Y", "Beli X Gratis Y (Buy X Get Y)", "Contoh: Beli 2 Gratis 1"),
                                    Triple("BELI_X_POTONGAN", "Beli X Potongan Nominal (Rp)", "Contoh: Beli 2 Potong Rp 5.000"),
                                    Triple("GROSIR", "Harga Khusus Bertingkat (Grosir)", "Contoh: Beli >= 3 harga jadi Rp 8.000")
                                )

                                promoTypes.forEach { (type, label, desc) ->
                                    Surface(
                                        color = if (promoType == type) Color(0xFFFFE082).copy(alpha = 0.35f) else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (promoType == type) RetailAmber else PosCardBorder),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .clickable { promoType = type }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = promoType == type, onClick = { promoType = type })
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                                Text(desc, fontSize = 10.sp, color = Color.DarkGray)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("PARAMETER PROMO:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                Spacer(modifier = Modifier.height(6.dp))

                                when (promoType) {
                                    "DISKON_PERSEN" -> {
                                        OutlinedTextField(
                                            value = diskonPersenStr,
                                            onValueChange = { diskonPersenStr = it },
                                            label = { Text("Besar Diskon Persen (%)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                        val p = diskonPersenStr.toDoubleOrNull() ?: 0.0
                                        val hargaPromo = hargaJualNum * (1.0 - (p / 100.0))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Simulasi: Harga Normal ${formatRupiah(hargaJualNum)} (dicoret) -> Harga Promo ${formatRupiah(hargaPromo)} (Hemat ${formatRupiah(hargaJualNum - hargaPromo)})",
                                            fontSize = 11.sp,
                                            color = RetailGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    "DISKON_NOMINAL" -> {
                                        OutlinedTextField(
                                            value = diskonNominalStr,
                                            onValueChange = { diskonNominalStr = it },
                                            label = { Text("Potongan Diskon Per Pcs (Rp)") },
                                            modifier = Modifier.fillMaxWidth(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                        val d = diskonNominalStr.toDoubleOrNull() ?: 0.0
                                        val hargaPromo = maxOf(0.0, hargaJualNum - d)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Simulasi: Harga Normal ${formatRupiah(hargaJualNum)} (dicoret) -> Harga Promo ${formatRupiah(hargaPromo)} (Hemat ${formatRupiah(d)})",
                                            fontSize = 11.sp,
                                            color = RetailGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    "BELI_X_GRATIS_Y" -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = promoMinBeliStr,
                                                onValueChange = { promoMinBeliStr = it },
                                                label = { Text("Beli Minimal (X)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                            OutlinedTextField(
                                                value = promoGratisQtyStr,
                                                onValueChange = { promoGratisQtyStr = it },
                                                label = { Text("Gratis Sebanyak (Y)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val x = promoMinBeliStr.toIntOrNull() ?: 2
                                        val y = promoGratisQtyStr.toIntOrNull() ?: 1
                                        Text(
                                            "Simulasi: Pembeli membeli $x $satuan, kasir otomatis memberikan $y $satuan GRATIS! (Hemat senilai ${formatRupiah(y * hargaJualNum)})",
                                            fontSize = 11.sp,
                                            color = RetailGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    "BELI_X_POTONGAN" -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = promoMinBeliStr,
                                                onValueChange = { promoMinBeliStr = it },
                                                label = { Text("Beli Minimal (X)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                            OutlinedTextField(
                                                value = promoPotonganNominalStr,
                                                onValueChange = { promoPotonganNominalStr = it },
                                                label = { Text("Total Potongan (Rp)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val x = promoMinBeliStr.toIntOrNull() ?: 2
                                        val pot = promoPotonganNominalStr.toDoubleOrNull() ?: 0.0
                                        Text(
                                            "Simulasi: Beli $x $satuan total normal ${formatRupiah(x * hargaJualNum)} mendapat potongan ${formatRupiah(pot)} jadi ${formatRupiah(maxOf(0.0, (x * hargaJualNum) - pot))}",
                                            fontSize = 11.sp,
                                            color = RetailGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    "GROSIR" -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(
                                                value = grosirQtyStr,
                                                onValueChange = { grosirQtyStr = it },
                                                label = { Text("Min. Pembelian (Qty)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                            OutlinedTextField(
                                                value = hargaGrosirStr,
                                                onValueChange = { hargaGrosirStr = it },
                                                label = { Text("Harga Khusus (Rp)") },
                                                modifier = Modifier.weight(1f),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                colors = tfColors
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = promoLabel,
                                    onValueChange = { promoLabel = it },
                                    label = { Text("Label / Tag Promo (Opsional: JSM, Super Hemat, dll.)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = tfColors
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = promoKeterangan,
                                    onValueChange = { promoKeterangan = it },
                                    label = { Text("Catatan / Syarat Ketentuan Promo") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = tfColors
                                )
                            }
                        }
                    }

                    // TAB 2: PENGATURAN REPACK MULTI-KEMASAN
                    if (activeTab == 2) {
                        item {
                            Surface(
                                color = if (isRepack) Color(0xFFE1F5FE) else PosSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRepack) Color(0xFF039BE5) else PosCardBorder),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isRepack = !isRepack }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Aktifkan Satuan Repack / Multi-Kemasan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                        Text("Memungkinkan penjualan dalam satuan Karton, Dus, Pak, Slop, Renceng, dll. dengan barcode dan harga tersendiri.", fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                    Switch(checked = isRepack, onCheckedChange = { isRepack = it })
                                }
                            }

                            if (isRepack) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("KEMASAN REPACK 1 (UTAMA)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0277BD))
                                Spacer(modifier = Modifier.height(6.dp))

                                Text("Pilih Satuan Kemasan 1:", fontSize = 11.sp, color = Color.DarkGray)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("KARTON", "DUS", "PAK", "SLOP", "RENCENG", "LUSIN", "BALL").forEach { s ->
                                        FilterChip(
                                            selected = satuanRepack.equals(s, ignoreCase = true),
                                            onClick = {
                                                satuanRepack = s
                                                if (barcodeRepack.isBlank()) barcodeRepack = "${s.take(1)}$barcode"
                                            },
                                            label = { Text(s, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = satuanRepack,
                                        onValueChange = { satuanRepack = it },
                                        label = { Text("Nama Kemasan") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = tfColors
                                    )
                                    OutlinedTextField(
                                        value = isiRepackStr,
                                        onValueChange = { isiRepackStr = it },
                                        label = { Text("Isi ($satuan Dasar)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = barcodeRepack,
                                        onValueChange = { barcodeRepack = it },
                                        label = { Text("Barcode Kemasan Repack 1") },
                                        modifier = Modifier.weight(1.3f),
                                        singleLine = true,
                                        colors = tfColors
                                    )
                                    OutlinedTextField(
                                        value = hargaRepackStr,
                                        onValueChange = { hargaRepackStr = it },
                                        label = { Text("Harga Jual (Rp)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = tfColors
                                    )
                                }

                                val isi1 = isiRepackStr.toIntOrNull() ?: 1
                                val hrg1 = hargaRepackStr.toDoubleOrNull() ?: 0.0
                                val equivalentPerPcs = if (isi1 > 0) hrg1 / isi1 else 0.0
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = Color(0xFFE0F7FA),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "1 $satuanRepack = $isi1 $satuan | Setara ${formatRupiah(equivalentPerPcs)} / $satuan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006064),
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { hasRepack2 = !hasRepack2 }
                                ) {
                                    Checkbox(checked = hasRepack2, onCheckedChange = { hasRepack2 = it })
                                    Text("Tambah Kemasan Bertingkat 2 (Contoh: Pak di dalam Karton, atau Slop)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                }

                                if (hasRepack2) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Pilih Satuan Kemasan 2:", fontSize = 11.sp, color = Color.DarkGray)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("PAK", "SLOP", "DUS", "RENCENG", "BKS", "KODI").forEach { s ->
                                            FilterChip(
                                                selected = satuanRepack2.equals(s, ignoreCase = true),
                                                onClick = {
                                                    satuanRepack2 = s
                                                    if (barcodeRepack2.isBlank()) barcodeRepack2 = "${s.take(1)}$barcode"
                                                },
                                                label = { Text(s, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = satuanRepack2,
                                            onValueChange = { satuanRepack2 = it },
                                            label = { Text("Nama Kemasan 2") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                        OutlinedTextField(
                                            value = isiRepack2Str,
                                            onValueChange = { isiRepack2Str = it },
                                            label = { Text("Isi ($satuan Dasar)") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = barcodeRepack2,
                                            onValueChange = { barcodeRepack2 = it },
                                            label = { Text("Barcode Kemasan Repack 2") },
                                            modifier = Modifier.weight(1.3f),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                        OutlinedTextField(
                                            value = hargaRepack2Str,
                                            onValueChange = { hargaRepack2Str = it },
                                            label = { Text("Harga Jual (Rp)") },
                                            modifier = Modifier.weight(1f),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            colors = tfColors
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = repackDeskripsi,
                                    onValueChange = { repackDeskripsi = it },
                                    label = { Text("Keterangan Kemasan (Contoh: 1 Karton isi 4 Pak @ 6 Pcs)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = tfColors
                                )
                            }
                        }
                    }
                }

                // Footer Buttons
                HorizontalDivider(color = PosCardBorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                        Text("BATAL", color = Color.Black)
                    }
                    Button(
                        onClick = {
                            val entity = ProductEntity(
                                plu = plu.trim().ifEmpty { Random.nextInt(1000, 9999).toString() },
                                barcode = barcode.trim().ifEmpty { plu.trim() },
                                nama = nama.trim().ifEmpty { "Produk Baru" },
                                kategori = kategori.trim().ifEmpty { "Umum" },
                                hargaBeli = hargaBeliStr.toDoubleOrNull() ?: 0.0,
                                hargaJual = hargaJualStr.toDoubleOrNull() ?: 0.0,
                                hargaMember = hargaJualStr.toDoubleOrNull() ?: 0.0,
                                stok = if (isEdit) (productToEdit?.stok ?: 0) else maxOf(0, stokStr.trim().toIntOrNull() ?: 0),
                                minStok = minStokStr.toIntOrNull() ?: 5,
                                satuan = satuan.trim().uppercase().ifEmpty { "PCS" },
                                rakPlanogram = rak.trim().ifEmpty { "001-01" },
                                varian = varian.trim(),

                                // Promo parameters
                                isPromo = isPromo,
                                promoType = promoType,
                                diskonPersen = diskonPersenStr.toDoubleOrNull() ?: 0.0,
                                diskonNominal = diskonNominalStr.toDoubleOrNull() ?: 0.0,
                                promoMinBeli = promoMinBeliStr.toIntOrNull() ?: 2,
                                promoGratisQty = promoGratisQtyStr.toIntOrNull() ?: 1,
                                promoPotonganNominal = promoPotonganNominalStr.toDoubleOrNull() ?: 0.0,
                                hargaGrosirQty = grosirQtyStr.toIntOrNull() ?: 2,
                                hargaGrosir = hargaGrosirStr.toDoubleOrNull() ?: 0.0,
                                promoLabel = promoLabel.trim(),
                                promoKeterangan = promoKeterangan.trim(),

                                // Repack parameters
                                isRepack = isRepack,
                                satuanRepack = if (isRepack) satuanRepack.trim().uppercase().ifEmpty { "KARTON" } else "",
                                isiRepack = if (isRepack) (isiRepackStr.toIntOrNull() ?: 24) else 1,
                                hargaRepack = if (isRepack) (hargaRepackStr.toDoubleOrNull() ?: 0.0) else 0.0,
                                barcodeRepack = if (isRepack) barcodeRepack.trim() else "",

                                satuanRepack2 = if (isRepack && hasRepack2) satuanRepack2.trim().uppercase() else "",
                                isiRepack2 = if (isRepack && hasRepack2) (isiRepack2Str.toIntOrNull() ?: 1) else 1,
                                hargaRepack2 = if (isRepack && hasRepack2) (hargaRepack2Str.toDoubleOrNull() ?: 0.0) else 0.0,
                                barcodeRepack2 = if (isRepack && hasRepack2) barcodeRepack2.trim() else "",
                                repackDeskripsi = if (isRepack) repackDeskripsi.trim() else ""
                            )
                            onSave(entity)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        modifier = Modifier.testTag("btn_save_product")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PRODUK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
