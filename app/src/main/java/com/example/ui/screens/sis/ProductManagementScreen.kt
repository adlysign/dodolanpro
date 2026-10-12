package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.allProductsIncludingInactive.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showProductEditorDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<ProductEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    val filteredProducts = remember(products, searchQuery) {
        products.filter {
            it.nama.contains(searchQuery, ignoreCase = true) ||
                    it.plu.contains(searchQuery) ||
                    it.barcode.contains(searchQuery) ||
                    it.rakPlanogram.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PENGATURAN PRODUK & HARGA", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Produk")
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari PLU / Barcode / Nama / Nomor Rak...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("TOTAL PRODUK: ${filteredProducts.size} ITEM", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GrayText)

            Spacer(modifier = Modifier.height(6.dp))

            if (feedbackMessage.isNotEmpty()) {
                Text(feedbackMessage, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredProducts) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                productToEdit = prod
                                showProductEditorDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Text(formatRupiah(prod.hargaJual), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text("PLU: ${prod.plu} | BC: ${prod.barcode} | Rak: ${prod.rakPlanogram}", fontSize = 11.sp, color = Color.Black)
                            Text("Beli: ${formatRupiah(prod.hargaBeli)} | Stok: ${prod.stok} ${prod.satuan}", fontSize = 11.sp, color = Color.Black)

                            if (prod.hargaGrosir > 0) {
                                Text("Harga Khusus: Beli >= ${prod.hargaGrosirQty} jadi ${formatRupiah(prod.hargaGrosir)}", fontSize = 10.sp, color = RetailAmber, fontWeight = FontWeight.SemiBold)
                            }

                            if (prod.isPromo) {
                                Text("Promo: Diskon ${prod.diskonPersen}% (${prod.promoLabel})", fontSize = 10.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                            }

                            if (prod.isRepack || prod.varian.isNotEmpty()) {
                                Text("Varian: ${prod.varian} | Repack: ${if (prod.isRepack) "Ya" else "Tidak"}", fontSize = 10.sp, color = GrayText)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Editor: Add or Edit Product
    if (showProductEditorDialog) {
        val isEdit = productToEdit != null
        var plu by remember { mutableStateOf(productToEdit?.plu ?: (1000..9999).random().toString()) }
        var barcode by remember { mutableStateOf(productToEdit?.barcode ?: plu) }
        var nama by remember { mutableStateOf(productToEdit?.nama ?: "") }
        var kategori by remember { mutableStateOf(productToEdit?.kategori ?: "Umum") }
        var rak by remember { mutableStateOf(productToEdit?.rakPlanogram ?: "RAK-01") }
        var hargaBeli by remember { mutableStateOf(productToEdit?.hargaBeli?.toLong()?.toString() ?: "0") }
        var hargaJual by remember { mutableStateOf(productToEdit?.hargaJual?.toLong()?.toString() ?: "0") }
        var stok by remember { mutableStateOf(productToEdit?.stok?.toString() ?: "10") }
        var satuan by remember { mutableStateOf(productToEdit?.satuan ?: "PCS") }
        var isPromo by remember { mutableStateOf(productToEdit?.isPromo ?: false) }
        var diskonPersen by remember { mutableStateOf(productToEdit?.diskonPersen?.toString() ?: "0.0") }
        var grosirQty by remember { mutableStateOf(productToEdit?.hargaGrosirQty?.toString() ?: "2") }
        var hargaGrosir by remember { mutableStateOf(productToEdit?.hargaGrosir?.toLong()?.toString() ?: "0") }
        var varian by remember { mutableStateOf(productToEdit?.varian ?: "") }
        var isRepack by remember { mutableStateOf(productToEdit?.isRepack ?: false) }

        AlertDialog(
            onDismissRequest = { showProductEditorDialog = false },
            title = { Text(if (isEdit) "EDIT PRODUK" else "TAMBAH PRODUK BARU", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    item {
                        val tfColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = PosCardBorder
                        )
                        OutlinedTextField(value = plu, onValueChange = { plu = it }, label = { Text("Kode PLU", color = Color.DarkGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = barcode, onValueChange = { barcode = it }, label = { Text("Barcode", color = Color.DarkGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Produk", color = Color.DarkGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = kategori, onValueChange = { kategori = it }, label = { Text("Kategori", color = Color.DarkGray) }, modifier = Modifier.weight(1f), singleLine = true, colors = tfColors)
                            OutlinedTextField(value = rak, onValueChange = { rak = it }, label = { Text("Nomor Rak", color = Color.DarkGray) }, modifier = Modifier.weight(1f), singleLine = true, colors = tfColors)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = hargaBeli, onValueChange = { hargaBeli = it }, label = { Text("Harga Beli (Rp)", color = Color.DarkGray) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = tfColors)
                            OutlinedTextField(value = hargaJual, onValueChange = { hargaJual = it }, label = { Text("Harga Jual (Rp)", color = Color.DarkGray) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = tfColors)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = stok, onValueChange = { stok = it }, label = { Text("Stok", color = Color.DarkGray) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = tfColors)
                            OutlinedTextField(value = satuan, onValueChange = { satuan = it }, label = { Text("Satuan (PCS/BKS)", color = Color.DarkGray) }, modifier = Modifier.weight(1f), singleLine = true, colors = tfColors)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Harga Khusus Bertingkat (Grosir):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = grosirQty, onValueChange = { grosirQty = it }, label = { Text("Beli Min (Qty)", color = Color.DarkGray) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = tfColors)
                            OutlinedTextField(value = hargaGrosir, onValueChange = { hargaGrosir = it }, label = { Text("Harga Khusus (Rp)", color = Color.DarkGray) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, colors = tfColors)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(value = varian, onValueChange = { varian = it }, label = { Text("Varian / Rasa (Opsional)", color = Color.DarkGray) }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = tfColors)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isPromo, onCheckedChange = { isPromo = it })
                            Text("Set Produk Promo", fontSize = 12.sp, color = Color.Black)
                            Spacer(modifier = Modifier.width(16.dp))
                            Checkbox(checked = isRepack, onCheckedChange = { isRepack = it })
                            Text("Repack", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val product = ProductEntity(
                            plu = plu.trim(),
                            barcode = barcode.trim(),
                            nama = nama.trim(),
                            kategori = kategori.trim(),
                            hargaBeli = hargaBeli.toDoubleOrNull() ?: 0.0,
                            hargaJual = hargaJual.toDoubleOrNull() ?: 0.0,
                            hargaMember = hargaJual.toDoubleOrNull() ?: 0.0,
                            stok = stok.toIntOrNull() ?: 0,
                            satuan = satuan.trim(),
                            rakPlanogram = rak.trim(),
                            isPromo = isPromo,
                            diskonPersen = diskonPersen.toDoubleOrNull() ?: 0.0,
                            hargaGrosirQty = grosirQty.toIntOrNull() ?: 2,
                            hargaGrosir = hargaGrosir.toDoubleOrNull() ?: 0.0,
                            varian = varian.trim(),
                            isRepack = isRepack
                        )
                        viewModel.saveOrUpdateProduct(product) {
                            feedbackMessage = "Produk ${product.nama} berhasil disimpan!"
                            showProductEditorDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                ) {
                    Text("SIMPAN PRODUK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showProductEditorDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}
