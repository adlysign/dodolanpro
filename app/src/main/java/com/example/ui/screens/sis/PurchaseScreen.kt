package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.data.local.entity.PurchaseEntity
import com.example.ui.screens.pos.LovProductDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import java.text.SimpleDateFormat
import java.util.*

data class PurchaseCartItem(
    val product: ProductEntity,
    var qty: Int,
    var hargaBeli: Double
) {
    val subtotal: Double get() = qty * hargaBeli
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

    var selectedTab by remember { mutableStateOf(0) } // 0: Input Pembelian, 1: Riwayat Pembelian
    var fakturInput by remember { mutableStateOf("FB-" + (100000..999999).random()) }
    var supplierInput by remember { mutableStateOf("DC SAT CILEUNGSI") }
    var keteranganInput by remember { mutableStateOf("Pembelian Stok Rutin") }

    val purchaseCart = remember { mutableStateListOf<PurchaseCartItem>() }
    var showLovDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var selectedPurchaseForDoc by remember { mutableStateOf<PurchaseEntity?>(null) }
    var docSaveMsg by remember { mutableStateOf("") }

    val totalNominal = purchaseCart.sumOf { it.subtotal }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PEMBELIAN SUPPLIER & TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("INPUT PEMBELIAN") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("RIWAYAT PEMBELIAN") })
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    // Header Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = fakturInput,
                                    onValueChange = { fakturInput = it },
                                    label = { Text("No. Faktur Beli") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = supplierInput,
                                    onValueChange = { supplierInput = it },
                                    label = { Text("Supplier / Asal Toko") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action: Add Item button (like POS screen)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showLovDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TAMBAH PRODUK (LOV)")
                        }

                        Text(
                            "Total: ${formatRupiah(totalNominal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = RetailRedPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Table items in purchase cart
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
                                    .padding(8.dp)
                            ) {
                                Text("PRODUK", modifier = Modifier.weight(2.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("HARGA BELI", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                Text("QTY", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color.Black)
                                Text("SUBTOTAL", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = Color.Black)
                                Text("", modifier = Modifier.weight(0.5f))
                            }

                            if (purchaseCart.isEmpty()) {
                                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Belum ada produk yang ditambahkan.", color = GrayText)
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    itemsIndexed(purchaseCart) { index, item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.5f)) {
                                                Text(item.product.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                                Text("PLU: ${item.product.plu} | Stok Saat Ini: ${item.product.stok}", fontSize = 10.sp, color = GrayText)
                                            }

                                            // Edit Harga Beli
                                            var priceStr by remember(item.hargaBeli) { mutableStateOf(item.hargaBeli.toLong().toString()) }
                                            OutlinedTextField(
                                                value = priceStr,
                                                onValueChange = {
                                                    priceStr = it
                                                    val parsed = it.toDoubleOrNull() ?: 0.0
                                                    purchaseCart[index] = item.copy(hargaBeli = parsed)
                                                },
                                                modifier = Modifier.weight(1.8f).height(48.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            )

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Edit Qty
                                            var qtyStr by remember(item.qty) { mutableStateOf(item.qty.toString()) }
                                            OutlinedTextField(
                                                value = qtyStr,
                                                onValueChange = {
                                                    qtyStr = it
                                                    val parsed = it.toIntOrNull() ?: 1
                                                    purchaseCart[index] = item.copy(qty = parsed)
                                                },
                                                modifier = Modifier.weight(1.2f).height(48.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                            )

                                            Text(
                                                formatRupiah(item.subtotal),
                                                modifier = Modifier.weight(1.8f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.End,
                                                color = Color.Black
                                            )

                                            IconButton(
                                                onClick = { purchaseCart.removeAt(index) },
                                                modifier = Modifier.weight(0.5f).size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Hapus", tint = RetailRedPrimary)
                                            }
                                        }
                                        HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }

                    if (feedbackMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(feedbackMessage, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Simpan: otomatis tambah stok produk dan simpan riwayat pembelian
                    Button(
                        onClick = {
                            if (purchaseCart.isEmpty()) {
                                feedbackMessage = "Daftar barang pembelian masih kosong!"
                                return@Button
                            }
                            val itemsToSave = purchaseCart.map {
                                it.product to Pair(it.qty, it.hargaBeli)
                            }
                            viewModel.recordDirectPurchase(
                                faktur = fakturInput,
                                supplier = supplierInput,
                                keterangan = keteranganInput,
                                items = itemsToSave
                            ) {
                                feedbackMessage = "Pembelian $fakturInput berhasil disimpan! Stok produk otomatis bertambah."
                                purchaseCart.clear()
                                fakturInput = "FB-" + (100000..999999).random()
                            }
                        },
                        enabled = purchaseCart.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PEMBELIAN (UPDATE STOK OTOMATIS)", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Tab 1: Riwayat Pembelian (Dapat dicetak dan disimpan dokumen)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    item {
                        Text("DAFTAR RIWAYAT NOTA PEMBELIAN:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (allPurchases.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada riwayat pembelian.", color = GrayText)
                            }
                        }
                    } else {
                        items(allPurchases) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(p.noFakturBeli, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                        Text(p.tanggal, fontSize = 11.sp, color = GrayText)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Supplier: ${p.supplierNama}", fontSize = 12.sp, color = Color.Black)
                                    Text("Total ${p.totalItem} pcs • ${formatRupiah(p.totalNominal)}", fontWeight = FontWeight.Bold, color = RetailGreen, fontSize = 13.sp)

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = { selectedPurchaseForDoc = p },
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

    if (showLovDialog) {
        LovProductDialog(
            products = allProducts,
            onDismiss = { showLovDialog = false },
            onSelectProduct = { prod, qty ->
                val existing = purchaseCart.find { it.product.plu == prod.plu }
                if (existing != null) {
                    existing.qty += qty
                } else {
                    purchaseCart.add(PurchaseCartItem(product = prod, qty = qty, hargaBeli = prod.hargaBeli))
                }
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
            appendLine("Supplier : ${pur.supplierNama}")
            appendLine("Petugas  : ${pur.kasirNik}")
            appendLine("Total Pcs: ${pur.totalItem}")
            appendLine("Total Rp : ${formatRupiah(pur.totalNominal)}")
            appendLine("Ket      : ${pur.keterangan}")
            appendLine("----------------------------------------")
            appendLine("Status: STOK DITAMBAHKAN KE GUDANG/TOKO")
            appendLine("Arsip: DodolanPro/Pembelian")
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
