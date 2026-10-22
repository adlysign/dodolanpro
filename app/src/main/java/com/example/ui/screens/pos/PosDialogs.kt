package com.example.ui.screens.pos

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import com.example.ui.components.QrisBarcodeView
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.data.local.entity.*
import com.example.ui.components.QrisPaymentCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PendingCart
import com.example.util.DocExportManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    format.maximumFractionDigits = 0
    return format.format(amount).replace("Rp", "Rp ")
}

// --- 1. LOV (Product Lookup) Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LovProductDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSelectProduct: (ProductEntity, Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedQty by remember { mutableStateOf(1) }

    val categories = remember(products) {
        listOf("Semua") + products.map { it.kategori }.distinct().sorted()
    }

    val filtered = remember(searchQuery, selectedCategory, products) {
        products.filter { p ->
            (selectedCategory == "Semua" || p.kategori.equals(selectedCategory, ignoreCase = true)) &&
                    (p.nama.contains(searchQuery, ignoreCase = true) ||
                            p.plu.contains(searchQuery, ignoreCase = true) ||
                            p.barcode.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .imePadding()
                .testTag("lov_product_dialog"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header (Red retail banner style)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "LOV - CARI PRODUK / PLU",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().testTag("lov_search_input"),
                    placeholder = { Text("Ketik Nama Barang / PLU / Barcode...") },
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

                // Category Filter Chips (Teks jelas, background kontras, anti-putih sebelum dipilih)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        Surface(
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("lov_cat_${cat}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) RetailNavy else Color(0xFFE2E8F0),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) RetailNavy else Color(0xFF94A3B8)
                            ),
                            shadowElevation = if (isSelected) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(
                                        Icons.Default.Sell,
                                        contentDescription = null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailNavy.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("PLU", modifier = Modifier.weight(1.2f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    Text("Nama Barang", modifier = Modifier.weight(3f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    Text("Stok", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                    Text("Harga", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, color = Color.Black)
                }

                // Table Items
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { prod ->
                        val isSelected = selectedProduct?.plu == prod.plu
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) RetailAmberLight else Color.Transparent)
                                .clickable { selectedProduct = prod }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prod.plu, modifier = Modifier.weight(1.2f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Column(modifier = Modifier.weight(3f)) {
                                Text(prod.nama, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = Color.Black)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    // High contrast category badge (Background terang, teks gelap jelas)
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(3.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            prod.kategori.ifEmpty { "Umum" },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Rak: ${prod.rakPlanogram.ifEmpty { "-" }}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (prod.stok <= 0) "0 (HABIS)" else "${prod.stok}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prod.stok <= 0) RetailRedPrimary else Color.Black
                                )
                                if (prod.stok <= 0) {
                                    Surface(
                                        color = RetailRedPrimary.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Text(
                                            "STOK 0",
                                            color = RetailRedPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1.5f), horizontalAlignment = Alignment.End) {
                                if (prod.hasDiscountOrPromo() || prod.isPromo) {
                                    Text(
                                        formatRupiah(prod.hargaJual),
                                        fontSize = 10.sp,
                                        color = Color.Gray,
                                        style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough)
                                    )
                                    Text(
                                        formatRupiah(prod.getEffectiveDiscountedPrice()),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = RetailRedPrimary
                                    )
                                } else {
                                    Text(
                                        formatRupiah(prod.hargaJual),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.End,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Qty: ", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        IconButton(
                            onClick = { if (selectedQty > 1) selectedQty-- },
                            enabled = selectedProduct != null
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Kurang")
                        }
                        Text("$selectedQty", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp), color = Color.Black)
                        IconButton(
                            onClick = { selectedQty++ },
                            enabled = selectedProduct != null
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Tambah")
                        }
                    }

                    Row {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                            Text("TUTUP")
                        }
                        val isProductInStock = selectedProduct != null && (selectedProduct?.stok ?: 0) > 0
                        Button(
                            onClick = {
                                selectedProduct?.let {
                                    if (it.stok > 0) {
                                        onSelectProduct(it, selectedQty)
                                        onDismiss()
                                    }
                                }
                            },
                            enabled = isProductInStock,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isProductInStock) RetailRedPrimary else Color.LightGray,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.testTag("lov_ambil_button")
                        ) {
                            Text(
                                if (selectedProduct != null && (selectedProduct?.stok ?: 0) <= 0) "STOK HABIS (0)" else "AMBIL (ENTER)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- 2. Mandatory Pending Label Dialog ---
@Composable
fun PendingLabelDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String) -> Unit
) {
    var labelInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailAmber, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text("PENDING TRANSAKSI (F5)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Wajib memberi nama/label antrian pending agar mudah dicari saat recall:",
                    fontSize = 12.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = {
                        labelInput = it
                        if (it.isNotBlank()) errorText = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("pending_label_input"),
                    placeholder = { Text("Contoh: Ibu Ani / Meja 3 / Baju Biru") },
                    singleLine = true,
                    isError = errorText.isNotEmpty()
                )

                if (errorText.isNotEmpty()) {
                    Text(errorText, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("BATAL")
                    }
                    Button(
                        onClick = {
                            if (labelInput.trim().isEmpty()) {
                                errorText = "Label pending wajib diisi!"
                            } else {
                                onConfirm(labelInput.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailAmber),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_pending")
                    ) {
                        Text("SIMPAN PENDING", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 3. Recall Pending List Dialog with Mandatory Supervisor Approval for Delete ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePendingApprovalDialog(
    cart: PendingCart,
    onDismiss: () -> Unit,
    onConfirmDelete: (pin: String, reason: String, onError: (String) -> Unit) -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Konsumen Batal / Tidak Kembali") }
    var errorMessage by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Konsumen Batal / Tidak Kembali",
        "Antrian Dobel / Salah Input",
        "Barang Batal / Rusak",
        "Lainnya"
    )

    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(cart.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .imePadding()
                .testTag("dialog_delete_pending_approval"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "OTORISASI PERSETUJUAN: HAPUS PENDING",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Security notice banner (Kasir dilarang hapus tanpa persetujuan)
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Kasir TIDAK DAPAT menghapus transaksi pending tanpa persetujuan Supervisor / Owner / Admin.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBF360C),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Info card for pending transaction
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Antrian: ${cart.label}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("Waktu: $timeStr", fontSize = 11.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Total: ${cart.items.size} item | Nilai: ${formatRupiah(cart.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = RetailNavy
                        )
                        if (cart.member != null) {
                            Text("Member: ${cart.member.nama}", fontSize = 11.sp, color = RetailGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val itemsSummary = cart.items.take(3).joinToString(", ") { "${it.product.nama} (${it.qty})" } +
                                if (cart.items.size > 3) " +${cart.items.size - 3} lainnya" else ""
                        Text("Barang: $itemsSummary", fontSize = 10.sp, color = Color.DarkGray, maxLines = 2)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Alasan Pembatalan / Hapus:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.take(2).forEach { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.drop(2).forEach { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("PIN Otorisasi (Supervisor / Owner / Admin):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        otpCode = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth().testTag("pin_delete_pending_input"),
                    placeholder = { Text("Masukkan PIN Supervisor / Owner...") },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan",
                                tint = Color.Gray
                            )
                        }
                    },
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(errorMessage, color = RetailRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("BATAL")
                    }
                    Button(
                        onClick = {
                            onConfirmDelete(otpCode, reason) { err ->
                                errorMessage = err
                            }
                        },
                        enabled = otpCode.isNotBlank(),
                        modifier = Modifier.weight(1.2f).testTag("btn_confirm_delete_pending"),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                    ) {
                        Text("SETUJUI & HAPUS", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecallPendingDialog(
    pendingCarts: List<PendingCart>,
    onResumeCart: (id: String) -> Unit,
    onDeleteCart: (cart: PendingCart, pin: String, reason: String, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingToDelete by remember { mutableStateOf<PendingCart?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
                .imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailNavy, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DAFTAR TRANSAKSI PENDING (RECALL F6)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                    }
                }

                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ketentuan SOP: Kasir tidak boleh menghapus transaksi / item tertunda tanpa persetujuan Supervisor/Owner.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBF360C)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var expandedCartId by remember { mutableStateOf<String?>(null) }

                if (pendingCarts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Tidak ada transaksi dalam antrian pending.", color = GrayText, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(pendingCarts) { cart ->
                            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(cart.timestamp))
                            val isExpanded = expandedCartId == cart.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "🏷️ ${cart.label}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.Black
                                        )
                                        Text("Pukul: $timeStr", fontSize = 11.sp, color = GrayText)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "Jumlah Item: ${cart.items.size} barang | Total: ${formatRupiah(cart.totalAmount)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )

                                    if (cart.member != null) {
                                        Text("Member: ${cart.member.nama}", fontSize = 11.sp, color = RetailGreen)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    TextButton(
                                        onClick = { expandedCartId = if (isExpanded) null else cart.id },
                                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(
                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = RetailNavy
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isExpanded) "Sembunyikan Rincian Item" else "Lihat Rincian Item Tertunda (${cart.items.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RetailNavy
                                        )
                                    }

                                    if (isExpanded) {
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                cart.items.forEachIndexed { idx, itm ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("${idx + 1}. ${itm.product.nama}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                                        Text("${itm.qty} ${itm.selectedSatuan} • ${formatRupiah(itm.subtotal)}", fontSize = 11.sp, color = Color.DarkGray)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        OutlinedButton(
                                            onClick = { pendingToDelete = cart },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RetailRedPrimary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp).padding(end = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Hapus (Persetujuan SPV)", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                onResumeCart(cart.id)
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lanjutkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("TUTUP")
                }
            }
        }
    }

    if (pendingToDelete != null) {
        DeletePendingApprovalDialog(
            cart = pendingToDelete!!,
            onDismiss = { pendingToDelete = null },
            onConfirmDelete = { pin, reason, onError ->
                onDeleteCart(pendingToDelete!!, pin, reason) { err ->
                    if (err.isEmpty()) {
                        pendingToDelete = null
                    } else {
                        onError(err)
                    }
                }
            }
        )
    }
}

// --- 4. Payment Dialog with Split Payment & Member Points (Donasi removed) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(
    grandTotal: Double,
    currentMember: MemberEntity?,
    storeConfig: StoreConfigEntity,
    appliedPoin: Int,
    onApplyMemberPoints: (Int) -> Unit,
    onApplyVoucher: (String) -> Pair<Boolean, String>,
    onDismiss: () -> Unit,
    onProcessPayment: (caraBayar: String, bayarTunai: Double, bayarNonTunai: Double, ref: String) -> Unit
) {
    var selectedMethod by remember { mutableStateOf("TUNAI") } // TUNAI, QRIS, CAMPURAN, WALLET, CARD
    var cashInput by remember { mutableStateOf("") }
    var nonCashInput by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf("QRIS") }
    var walletPhone by remember { mutableStateOf("") }
    var selectedCardBank by remember { mutableStateOf("BCA") }
    var cardApprovalCode by remember { mutableStateOf("") }
    var voucherCodeInput by remember { mutableStateOf("") }
    var voucherMessage by remember { mutableStateOf("") }
    var isVoucherSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Otomatis arahkan kursor ke kolom nominal saat masuk pembayaran
    LaunchedEffect(Unit) {
        delay(60)
        for (i in 1..4) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
            delay(100)
        }
    }

    LaunchedEffect(selectedMethod) {
        if (selectedMethod == "TUNAI" || selectedMethod == "CAMPURAN") {
            delay(50)
            for (i in 1..3) {
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) {}
                delay(90)
            }
        }
    }

    val numericCash = cashInput.toDoubleOrNull() ?: 0.0
    val numericNonCash = nonCashInput.toDoubleOrNull() ?: 0.0
    val totalPaid = if (selectedMethod == "CAMPURAN") (numericCash + numericNonCash) else numericCash
    val kembalian = (totalPaid - grandTotal).coerceAtLeast(0.0)

    val quickCashButtons = listOf(1000.0, 5000.0, 10000.0, 20000.0, 50000.0, 100000.0)
    val walletOptions = listOf("QRIS", "DANA", "GOPAY", "OVO", "SHOPEEPAY", "LINKAJA", "KREDIVO")
    val bankOptions = listOf("BCA", "MANDIRI", "BRI", "BNI")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .imePadding()
                .testTag("payment_dialog"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Total Tagihan Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("TOTAL TAGIHAN", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (grandTotal <= 0.0) "Rp 0 (TRANSAKSI KOSONG/TIDAK VALID)" else formatRupiah(grandTotal),
                            color = Color.White,
                            fontSize = if (grandTotal <= 0.0) 16.sp else 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Batal", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Member Points Deduction Section
                if (currentMember != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (storeConfig.poinAktif) RetailGreenLight else Color(0xFFF1F5F9))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Member: ${currentMember.nama} (${currentMember.poin} Poin)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (storeConfig.poinAktif) RetailGreen else Color.Gray)
                                if (!storeConfig.poinAktif) {
                                    Text("Program poin loyalty sedang dinonaktifkan di pengaturan", fontSize = 10.sp, color = Color.Gray)
                                } else {
                                    val minNote = if (storeConfig.minPoinRedeem > 0) " (Min. ${storeConfig.minPoinRedeem} Poin)" else ""
                                    Text("Tukar poin potongan harga (1 Poin = ${formatRupiah(storeConfig.nilaiPerPoin)})$minNote", fontSize = 10.sp, color = Color.Black)
                                }
                            }

                            if (storeConfig.poinAktif) {
                                if (appliedPoin > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("-${formatRupiah(appliedPoin * storeConfig.nilaiPerPoin)}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        TextButton(onClick = { onApplyMemberPoints(0) }) {
                                            Text("Batal", fontSize = 10.sp)
                                        }
                                    }
                                } else {
                                    val minPoin = storeConfig.minPoinRedeem.coerceAtLeast(0)
                                    val canRedeem = currentMember.poin >= minPoin && currentMember.poin > 0
                                    Button(
                                        onClick = {
                                            val maxPointsByTotal = if (storeConfig.nilaiPerPoin > 0) (grandTotal / storeConfig.nilaiPerPoin).toInt() else 0
                                            var usable = currentMember.poin.coerceAtMost(maxPointsByTotal)
                                            if (storeConfig.maxPoinRedeemPerTransaksi > 0) {
                                                usable = usable.coerceAtMost(storeConfig.maxPoinRedeemPerTransaksi)
                                            }
                                            onApplyMemberPoints(usable)
                                        },
                                        enabled = canRedeem,
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Gunakan Poin", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Payment Method Selector Tabs: TUNAI, QRIS, CAMPURAN, WALLET, CARD
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("TUNAI", "QRIS", "CAMPURAN", "WALLET", "CARD").forEach { method ->
                        val isSelected = selectedMethod == method
                        Button(
                            onClick = { selectedMethod = method },
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) RetailNavy else PosSurfaceVariant,
                                contentColor = if (isSelected) Color.White else Color.Black
                            ),
                            contentPadding = PaddingValues(2.dp)
                        ) {
                            Text(method, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Method Specific Fields
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedMethod) {
                        "TUNAI" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Uang Tunai Konsumen:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = cashInput,
                                    onValueChange = { cashInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .testTag("cash_input_field"),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            if (numericCash >= grandTotal) {
                                                onProcessPayment("TUNAI", numericCash, 0.0, "TUNAI")
                                            }
                                        }
                                    ),
                                    placeholder = { Text("Ketik nominal pembayaran (Rp)...", color = Color.Gray) },
                                    textStyle = LocalTextStyle.current.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                    colors = retailTextFieldColors(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Quick Cash Buttons
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    quickCashButtons.take(3).forEach { amt ->
                                        OutlinedButton(
                                            onClick = { cashInput = amt.toLong().toString() },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text(NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amt), fontSize = 11.sp, color = Color.Black)
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    quickCashButtons.drop(3).forEach { amt ->
                                        OutlinedButton(
                                            onClick = { cashInput = amt.toLong().toString() },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text(NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amt), fontSize = 11.sp, color = Color.Black)
                                        }
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    Button(
                                        onClick = { cashInput = grandTotal.toLong().toString() },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailAmber)
                                    ) {
                                        Text("UANG PAS (${formatRupiah(grandTotal)})", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RetailGreenLight)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("KEMBALIAN:", fontWeight = FontWeight.Bold, color = RetailGreen, fontSize = 13.sp)
                                        Text(formatRupiah(kembalian), fontWeight = FontWeight.Bold, color = RetailGreen, fontSize = 18.sp)
                                    }
                                }
                            }
                        }

                        "CAMPURAN" -> {
                            // Split Payment: Tunai + Non-Tunai
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = RetailAmberLight)
                                ) {
                                    Text(
                                        "PEMBAYARAN CAMPURAN: Bayar sebagian dengan Tunai dan sisanya Non-Tunai (EDC/QRIS).",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6D4C41),
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("1. Bayar Tunai (Rp):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = cashInput,
                                    onValueChange = {
                                        cashInput = it
                                        val cVal = it.toDoubleOrNull() ?: 0.0
                                        val sisa = (grandTotal - cVal).coerceAtLeast(0.0)
                                        nonCashInput = sisa.toLong().toString()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    placeholder = { Text("Ketik nominal uang tunai...", color = Color.Gray) },
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("2. Sisa Bayar Non-Tunai (EDC / QRIS):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                OutlinedTextField(
                                    value = nonCashInput,
                                    onValueChange = { nonCashInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("BCA EDC", "MANDIRI", "QRIS", "DANA").forEach { b ->
                                        FilterChip(
                                            selected = selectedCardBank == b,
                                            onClick = { selectedCardBank = b },
                                            label = { Text(b, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                if (selectedCardBank == "QRIS") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "QRIS PEMBAYARAN CAMPURAN (Sisa: ${formatRupiah(numericNonCash)})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF1E3A8A)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            QrisBarcodeView(
                                                data = storeConfig.qrisData.ifBlank { "00020101021226600016ID.CO.QRIS.WWW01189360050300000870940215000001234567890520454995802ID5910DODOLANPRO6007JAKARTA61051234062070703A016304" },
                                                sizeDp = 135.dp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "NMID: ${storeConfig.qrisNmid.ifBlank { "ID1020030040050" }}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "QRIS" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QrisPaymentCard(
                                    merchantName = storeConfig.qrisMerchantName.ifBlank { storeConfig.storeName },
                                    nmid = storeConfig.qrisNmid.ifBlank { "ID1020030040050" },
                                    qrisData = storeConfig.qrisData.ifBlank { "00020101021226600016ID.CO.QRIS.WWW01189360050300000870940215000001234567890520454995802ID5910DODOLANPRO6007JAKARTA61051234062070703A016304" },
                                    amount = grandTotal
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = walletPhone,
                                    onValueChange = { walletPhone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("No. Referensi / RRN (Opsional)") },
                                    placeholder = { Text("Contoh: RRN-1029384756") },
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )
                            }
                        }

                        "WALLET" -> {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Pilih E-Wallet / QRIS:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    walletOptions.take(4).forEach { w ->
                                        FilterChip(selected = selectedWallet == w, onClick = { selectedWallet = w }, label = { Text(w, fontSize = 9.sp) })
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    walletOptions.drop(4).forEach { w ->
                                        FilterChip(selected = selectedWallet == w, onClick = { selectedWallet = w }, label = { Text(w, fontSize = 9.sp) })
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (selectedWallet == "QRIS") {
                                    QrisPaymentCard(
                                        merchantName = storeConfig.qrisMerchantName.ifBlank { storeConfig.storeName },
                                        nmid = storeConfig.qrisNmid.ifBlank { "ID1020030040050" },
                                        qrisData = storeConfig.qrisData.ifBlank { "00020101021226600016ID.CO.QRIS.WWW01189360050300000870940215000001234567890520454995802ID5910DODOLANPRO6007JAKARTA61051234062070703A016304" },
                                        amount = grandTotal
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                OutlinedTextField(
                                    value = walletPhone,
                                    onValueChange = { walletPhone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("No. HP / Kode QRIS / Ref") },
                                    placeholder = { Text("08xxxxxxxxxx atau Ref") },
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )
                            }
                        }

                        "CARD" -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text("Pilih EDC Bank:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    bankOptions.forEach { b ->
                                        FilterChip(selected = selectedCardBank == b, onClick = { selectedCardBank = b }, label = { Text(b) })
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = cardApprovalCode,
                                    onValueChange = { cardApprovalCode = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("No. Kartu / Trace / Approval Code") },
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )
                            }
                        }
                    }
                }

                // Voucher Input Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = voucherCodeInput,
                        onValueChange = { voucherCodeInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ketik Kode Voucher (Opsional)") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1A1A1A),
                            unfocusedTextColor = Color(0xFF1A1A1A),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = Color(0xFFB0BEC5)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            val res = onApplyVoucher(voucherCodeInput)
                            isVoucherSuccess = res.first
                            voucherMessage = res.second
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                    ) {
                        Text("Klaim", color = Color.White)
                    }
                }
                if (voucherMessage.isNotEmpty()) {
                    Text(
                        voucherMessage,
                        fontSize = 11.sp,
                        color = if (isVoucherSuccess) RetailGreen else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Process Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("BATAL")
                    }
                    Button(
                        onClick = {
                            if (grandTotal <= 0.0) {
                                errorMessage = "Total transaksi Rp 0! Transaksi dengan nilai Rp 0 tidak dapat diproses."
                                return@Button
                            }
                            when (selectedMethod) {
                                "TUNAI" -> {
                                    if (numericCash < grandTotal) {
                                        errorMessage = "Uang tunai kurang dari tagihan!"
                                    } else {
                                        onProcessPayment("TUNAI", numericCash, 0.0, "TUNAI")
                                    }
                                }
                                "QRIS" -> {
                                    onProcessPayment(
                                        "QRIS",
                                        0.0,
                                        grandTotal,
                                        "QRIS NMID:${storeConfig.qrisNmid.ifBlank { "ID1020030040050" }} ${if (walletPhone.isNotBlank()) "Ref:$walletPhone" else ""}".trim()
                                    )
                                }
                                "CAMPURAN" -> {
                                    if ((numericCash + numericNonCash) < grandTotal) {
                                        errorMessage = "Total pembayaran campuran kurang dari tagihan!"
                                    } else {
                                        onProcessPayment("CAMPURAN", numericCash, numericNonCash, "CAMPURAN: Tunai + $selectedCardBank")
                                    }
                                }
                                "WALLET" -> {
                                    onProcessPayment("WALLET", 0.0, grandTotal, "$selectedWallet - $walletPhone")
                                }
                                "CARD" -> {
                                    onProcessPayment("CARD", 0.0, grandTotal, "EDC $selectedCardBank #$cardApprovalCode")
                                }
                            }
                        },
                        enabled = grandTotal > 0.0,
                        modifier = Modifier.weight(1.5f).testTag("payment_proses_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (grandTotal > 0.0) RetailRedPrimary else Color.LightGray,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (grandTotal <= 0.0) "TIDAK VALID" else "PROSES (ENTER)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// --- 5. Thermal Receipt Dialog with Customizable Receipt Models & Direct Bluetooth Printing ---
@Composable
fun ReceiptDialog(
    transaction: TransactionEntity,
    items: List<CartItem>,
    storeConfig: StoreConfigEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var saveStatusMsg by remember { mutableStateOf("") }
    var isPrinting by remember { mutableStateOf(false) }

    val receiptText = remember(transaction, items, storeConfig) {
        com.example.util.ReceiptFormatter.formatReceipt(transaction, items, storeConfig)
    }

    // Auto-print on dialog open if enabled
    LaunchedEffect(Unit) {
        if (storeConfig.autoPrintReceipt && storeConfig.printerType == "BLUETOOTH" && storeConfig.bluetoothPrinterMac.isNotBlank()) {
            val escPos = com.example.util.ReceiptFormatter.generateEscPos(receiptText, storeConfig)
            com.example.util.BluetoothPrinterHelper.printOverBluetooth(storeConfig.bluetoothPrinterMac, escPos)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = RetailRedPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("STRUK TRANSAKSI KASIR", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Model: ${storeConfig.receiptModel} (${storeConfig.printerPaperSize})", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray, RoundedCornerShape(4.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                        item {
                            Text(
                                text = receiptText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = if (storeConfig.printerPaperSize == "80mm") 10.sp else 11.sp,
                                lineHeight = 15.sp,
                                color = Color.Black
                            )
                        }
                    }
                }

                if (saveStatusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(saveStatusMsg, fontSize = 11.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons: Cetak, Simpan, Bagikan, Selesai
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isPrinting = true
                                if (storeConfig.printerType == "BLUETOOTH" && storeConfig.bluetoothPrinterMac.isNotBlank()) {
                                    val escPos = com.example.util.ReceiptFormatter.generateEscPos(receiptText, storeConfig)
                                    val res = com.example.util.BluetoothPrinterHelper.printOverBluetooth(storeConfig.bluetoothPrinterMac, escPos)
                                    saveStatusMsg = if (res.first) "✓ Selesai mencetak ke printer" else res.second
                                } else {
                                    val res = com.example.util.BluetoothPrinterHelper.shareReceiptText(context, receiptText, "Struk ${transaction.bonNo}")
                                    saveStatusMsg = res.second
                                }
                                isPrinting = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White),
                        enabled = !isPrinting
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CETAK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val saved = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Penjualan",
                                prefix = "Struk_${transaction.bonNo}",
                                extension = "txt",
                                content = receiptText
                            )
                            saveStatusMsg = "Tersimpan di DodolanPro/Penjualan/${saved.second.name}"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Struk ${transaction.bonNo}")
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan Struk"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BAGIKAN", fontSize = 10.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                    ) {
                        Text("SELESAI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- 6. VOID Item Dialog ---
@Composable
fun VoidItemDialog(
    item: CartItem,
    onDismiss: () -> Unit,
    onConfirmVoid: (otp: String, reason: String, onError: (String) -> Unit) -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Konsumen Batal") }
    val reasons = listOf("Konsumen Batal", "Salah Input Qty", "Dobel Barcode Scan", "Barang Rusak")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text("OTORISASI VOID ITEM KERANJANG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Batalkan item berikut:", fontSize = 12.sp, color = Color.Black)
                Text(item.product.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                Text("Qty: ${item.qty} ${item.product.satuan} | Subtotal: ${formatRupiah(item.subtotal)}", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(10.dp))

                Text("Alasan Void:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.take(2).forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r, fontSize = 10.sp) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    reasons.drop(2).forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r, fontSize = 10.sp) })
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("PIN / Password Otorisasi (Supervisor / Owner / Admin):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                OutlinedTextField(
                    value = otpCode,
                    onValueChange = {
                        otpCode = it
                        if (errorMessage.isNotEmpty()) errorMessage = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Masukkan PIN otorisasi...") },
                    visualTransformation = if (isPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Sembunyikan" else "Tampilkan",
                                tint = Color.Gray
                            )
                        }
                    },
                    singleLine = true
                )

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(errorMessage, color = RetailRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("BATAL")
                    }
                    Button(
                        onClick = {
                            onConfirmVoid(otpCode, reason) { err ->
                                errorMessage = err
                            }
                        },
                        enabled = otpCode.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                    ) {
                        Text("PROSES VOID", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 7. RETUR Konsumen Dialog ---
@Composable
fun ReturKonsumenDialog(
    onDismiss: () -> Unit,
    onLookupBon: suspend (String) -> Pair<TransactionEntity?, List<TransactionItemEntity>>,
    onConfirmRetur: (bonNo: String, product: ProductEntity, qty: Int, reason: String, otp: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var bonInput by remember { mutableStateOf("") }
    var loadedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var loadedItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var selectedItem by remember { mutableStateOf<TransactionItemEntity?>(null) }
    var returQty by remember { mutableStateOf(1) }
    var selectedReason by remember { mutableStateOf("BKE (Expired)") }
    var otpInput by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var searchError by remember { mutableStateOf("") }

    val reasons = listOf("BKE (Expired)", "BR (Barang Rusak)", "BTA (Barang Cacat)", "BRP (Kemasan Pecah)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RETUR TRANSAKSI KONSUMEN (F7)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (step) {
                    1 -> {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. Masukkan Nomor BON Transaksi Asal:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("Bisa ketik 4 digit terakhir atau nomor bon lengkap", fontSize = 11.sp, color = GrayText)

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = bonInput,
                                onValueChange = { bonInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Contoh: M5X7 atau K427-0408-1234") },
                                singleLine = true
                            )

                            if (searchError.isNotEmpty()) {
                                Text(searchError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (bonInput.trim().isEmpty()) {
                                        searchError = "Nomor BON tidak boleh kosong!"
                                        return@Button
                                    }
                                    coroutineScope.launch {
                                        val res = onLookupBon(bonInput)
                                        if (res.first != null && res.second.isNotEmpty()) {
                                            loadedTx = res.first
                                            loadedItems = res.second
                                            step = 2
                                            searchError = ""
                                        } else {
                                            searchError = "Transaksi dengan nomor BON tersebut tidak ditemukan!"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                            ) {
                                Text("CARI DOKUMEN BON")
                            }
                        }
                    }

                    2 -> {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nomor BON: ${loadedTx?.bonNo} (${loadedTx?.tanggal})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("Pilih barang yang akan diretur:", fontSize = 12.sp, color = GrayText)

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(modifier = Modifier.weight(1f)) {
                                items(loadedItems) { item ->
                                    val isSelected = selectedItem?.id == item.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                selectedItem = item
                                                returQty = 1
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) RetailAmberLight else PosSurfaceVariant
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                                Text("PLU: ${item.plu} | Beli: ${item.qty} pcs", fontSize = 11.sp, color = GrayText)
                                            }
                                            Text(formatRupiah(item.hargaSatuan), fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }

                            if (selectedItem != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Jumlah Retur: ", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Black)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { if (returQty > 1) returQty-- }) {
                                            Icon(Icons.Default.Remove, contentDescription = null)
                                        }
                                        Text("$returQty", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                        IconButton(onClick = { if (returQty < (selectedItem?.qty ?: 1)) returQty++ }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                        }
                                    }
                                }

                                Text("Alasan Retur:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Black)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    reasons.take(2).forEach { r ->
                                        FilterChip(selected = selectedReason == r, onClick = { selectedReason = r }, label = { Text(r, fontSize = 9.sp) })
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) {
                                    Text("KEMBALI")
                                }
                                Button(
                                    onClick = { step = 3 },
                                    enabled = selectedItem != null,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                                ) {
                                    Text("LANJUT OTP", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    3 -> {
                        Column(modifier = Modifier.weight(1f)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RetailAmberLight)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("RINGKASAN RETUR:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    Text("Barang: ${selectedItem?.namaBarang}", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                    Text("Qty: -$returQty | Nilai: ${formatRupiah((selectedItem?.hargaSatuan ?: 0.0) * returQty)}", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Scan / Masukkan OTP Retur Transaksi:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            OutlinedTextField(
                                value = otpInput,
                                onValueChange = { otpInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Kode OTP Retur") },
                                trailingIcon = {
                                    TextButton(onClick = { otpInput = (100000..999999).random().toString() }) {
                                        Text("Bypass OTP", fontSize = 11.sp, color = RetailNavy)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) {
                                    Text("KEMBALI")
                                }
                                Button(
                                    onClick = {
                                        selectedItem?.let { itm ->
                                            val dummyProd = ProductEntity(
                                                plu = itm.plu,
                                                barcode = itm.plu,
                                                nama = itm.namaBarang,
                                                kategori = "Retur",
                                                hargaBeli = itm.hargaSatuan,
                                                hargaJual = itm.hargaSatuan,
                                                hargaMember = itm.hargaSatuan,
                                                stok = 100
                                            )
                                            onConfirmRetur(loadedTx?.bonNo ?: "", dummyProd, returQty, selectedReason, otpInput)
                                            onDismiss()
                                        }
                                    },
                                    enabled = otpInput.length >= 4,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                                ) {
                                    Text("PROSES RETUR", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 8. Member Dialog ---
@Composable
fun MemberDialog(
    currentMember: MemberEntity?,
    onDismiss: () -> Unit,
    onLookup: (String) -> Unit,
    onRemoveMember: () -> Unit
) {
    var phoneInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text("IDENTIFIKASI MEMBER KONSUMEN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (currentMember != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RetailGreenLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CardMembership, contentDescription = null, tint = RetailGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MEMBER TERHUBUNG", fontWeight = FontWeight.Bold, color = RetailGreen, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Nama   : ${currentMember.nama}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("No. HP : ${currentMember.noHp}", fontSize = 12.sp, color = Color.Black)
                            Text("Poin   : ${currentMember.poin} Poin", fontWeight = FontWeight.Bold, color = RetailAmber, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onRemoveMember()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("HAPUS MEMBER")
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                        ) {
                            Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Text("Apakah Anda ingin scan kartu member atau input nomor HP konsumen?", fontSize = 13.sp, color = Color.Black)

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nomor HP / Nomor Member") },
                        placeholder = { Text("08xxxxxxxxxx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("TIDAK (F4)")
                        }
                        Button(
                            onClick = {
                                onLookup(phoneInput)
                                onDismiss()
                            },
                            enabled = phoneInput.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                        ) {
                            Text("YA / CARI", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- 9. Login Kasir Dialog ---
@Composable
fun CashierLoginDialog(
    cashiers: List<CashierUserEntity>,
    canDismiss: Boolean = true,
    onDismiss: () -> Unit,
    onLogin: (nik: String, pass: String) -> Unit
) {
    var nikInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var detectedName by remember { mutableStateOf("") }

    LaunchedEffect(nikInput, cashiers) {
        val trimmed = nikInput.trim()
        val found = cashiers.find { it.nik == trimmed }
        detectedName = when {
            trimmed.isEmpty() -> ""
            trimmed == "11111111" -> "Owner"
            found != null -> found.nama
            else -> ""
        }
    }

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).imePadding(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetailRedPrimary, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        if (canDismiss) "GANTI KASIR / PENGGUNA" else "LOGIN APLIKASI",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nikInput,
                    onValueChange = { nikInput = it },
                    modifier = Modifier.fillMaxWidth().testTag("input_login_nik"),
                    label = { Text("NIK Karyawan", color = Color.DarkGray) },
                    placeholder = { Text("Masukkan NIK...", color = GrayText) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = RetailNavy)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = RetailRedPrimary,
                        unfocusedBorderColor = PosCardBorder
                    )
                )

                if (detectedName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp),
                        color = PosSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Pengguna: $detectedName",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier.fillMaxWidth().testTag("input_login_password"),
                    label = { Text("Password / PIN", color = Color.DarkGray) },
                    placeholder = { Text("Masukkan password atau PIN...", color = GrayText) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = RetailNavy)
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val desc = if (passwordVisible) "Sembunyikan password" else "Tampilkan password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = desc, tint = Color.Gray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = RetailRedPrimary,
                        unfocusedBorderColor = PosCardBorder
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canDismiss) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("BATAL", color = Color.Black)
                        }
                    }
                    Button(
                        onClick = {
                            if (nikInput.isNotBlank() && passwordInput.isNotBlank()) {
                                onLogin(nikInput.trim(), passwordInput.trim())
                            }
                        },
                        enabled = nikInput.isNotBlank() && passwordInput.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("btn_login_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                    ) {
                        Text("MASUK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 11. Buka Shift Kasir Dialog (Wajib Sebelum Transaksi) ---
fun checkShiftTimeValid(jamMulai: String, jamSelesai: String, calendar: Calendar = Calendar.getInstance()): Boolean {
    return try {
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val startParts = jamMulai.split(":").map { it.trim().toInt() }
        val startTotalMinutes = startParts[0] * 60 + (if (startParts.size > 1) startParts[1] else 0)

        val endParts = jamSelesai.split(":").map { it.trim().toInt() }
        val endTotalMinutes = endParts[0] * 60 + (if (endParts.size > 1) endParts[1] else 0)

        if (startTotalMinutes <= endTotalMinutes) {
            currentTotalMinutes in startTotalMinutes until endTotalMinutes
        } else {
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        }
    } catch (e: Exception) {
        true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BukaShiftDialog(
    currentCashier: CashierUserEntity? = null,
    shifts: List<ShiftConfigEntity>,
    onOpenShift: (shiftNum: Int, modal: Double) -> Unit,
    onSwitchCashier: () -> Unit,
    onDismiss: () -> Unit
) {
    val defaultFallbackShifts = remember {
        listOf(
            ShiftConfigEntity(shiftNumber = 1, shiftName = "Shift 1 (Pagi)", jamMulai = "07:00", jamSelesai = "15:00", modalAwalStandar = 100000.0),
            ShiftConfigEntity(shiftNumber = 2, shiftName = "Shift 2 (Siang)", jamMulai = "15:00", jamSelesai = "23:00", modalAwalStandar = 100000.0),
            ShiftConfigEntity(shiftNumber = 3, shiftName = "Shift 3 (Malam)", jamMulai = "23:00", jamSelesai = "07:00", modalAwalStandar = 100000.0)
        )
    }
    val activeShifts = remember(shifts) {
        if (shifts.isNotEmpty()) shifts else defaultFallbackShifts
    }

    val now = Calendar.getInstance()
    val currentTimeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
    val autoMatchedShift = remember(activeShifts) {
        activeShifts.find { checkShiftTimeValid(it.jamMulai, it.jamSelesai, now) } ?: activeShifts.firstOrNull()
    }

    var selectedShiftNumber by remember(activeShifts) {
        mutableStateOf(autoMatchedShift?.shiftNumber ?: 1)
    }
    val currentSelectedShift = activeShifts.firstOrNull { it.shiftNumber == selectedShiftNumber }
    var modalInput by remember(currentSelectedShift) {
        mutableStateOf(currentSelectedShift?.modalAwalStandar?.toLong()?.toString() ?: "100000")
    }
    var errorMessage by remember { mutableStateOf("") }

    val quickModals = listOf(50000L, 100000L, 200000L, 500000L)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(2.dp, RetailNavy),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header dengan tombol Tutup (Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RetailNavy,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LockClock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("BUKA SHIFT KASIR", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.Black)
                            Text("Jam Sekarang: $currentTimeStr WIB • Sesi Kerja Kasir", fontSize = 11.sp, color = Color.Black)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Black)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = PosCardBorder)

                // Info Petugas Aktif
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PETUGAS KASIR BERTUGAS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                            Text("${currentCashier?.nama ?: "Kasir"} (${currentCashier?.nik ?: "-"})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("Jabatan: ${currentCashier?.role ?: "-"}", fontSize = 11.sp, color = Color.Black)
                        }
                        OutlinedButton(
                            onClick = onSwitchCashier,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RetailNavy)
                        ) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ganti User", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pilih Shift
                Text("1. Pilih Jadwal Shift (Sesuai Jam Kerja):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeShifts.forEach { shift ->
                        val isSelected = shift.shiftNumber == selectedShiftNumber
                        val isTimeValid = checkShiftTimeValid(shift.jamMulai, shift.jamSelesai, now)

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedShiftNumber = shift.shiftNumber
                                    modalInput = shift.modalAwalStandar.toLong().toString()
                                    errorMessage = if (isTimeValid) "" else "Shift ${shift.shiftName} (${shift.jamMulai}-${shift.jamSelesai}) di luar jadwal standar ($currentTimeStr), tetap dapat dibuka."
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isSelected -> RetailNavy
                                isTimeValid -> Color(0xFFF5F5F5)
                                else -> Color(0xFFEEEEEE)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> RetailNavy
                                    isTimeValid -> PosCardBorder
                                    else -> Color(0xFFE0E0E0)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    shift.shiftName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = when {
                                        isSelected -> Color.White
                                        isTimeValid -> Color.Black
                                        else -> Color.Gray
                                    }
                                )
                                Text(
                                    "${shift.jamMulai}-${shift.jamSelesai}",
                                    fontSize = 9.sp,
                                    color = when {
                                        isSelected -> Color.White.copy(0.8f)
                                        isTimeValid -> Color.DarkGray
                                        else -> Color.Gray
                                    }
                                )
                                Text(
                                    if (isTimeValid) "● Sesuai Jam" else "✕ Di Luar Jam",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isSelected -> if (isTimeValid) Color(0xFF81C784) else Color(0xFFFF8A80)
                                        isTimeValid -> RetailGreen
                                        else -> RetailRedPrimary
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Modal Awal Kas
                Text("2. Masukkan Modal Kas Awal (Uang Kembalian):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = modalInput,
                    onValueChange = { modalInput = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Modal Awal (Rp)", color = Color.DarkGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = RetailNavy,
                        unfocusedBorderColor = PosCardBorder
                    ),
                    leadingIcon = {
                        Text("Rp", fontWeight = FontWeight.Bold, color = RetailNavy, modifier = Modifier.padding(start = 8.dp))
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tombol Cepat Pilihan Modal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickModals.forEach { amt ->
                        OutlinedButton(
                            onClick = { modalInput = amt.toString() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text(NumberFormat.getIntegerInstance(Locale("id", "ID")).format(amt), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.35f)
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TUTUP", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val parsedModal = modalInput.toDoubleOrNull()
                            if (parsedModal == null || parsedModal < 0) {
                                errorMessage = "Masukkan nilai modal kas awal yang valid!"
                            } else {
                                onOpenShift(selectedShiftNumber, parsedModal)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(0.65f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BUKA SHIFT", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// --- 12. Lock Screen Dialog (Kunci Terminal Kasir) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenDialog(
    cashier: CashierUserEntity? = null,
    onUnlock: (pinOrPassword: String) -> Boolean,
    onLogout: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { /* Modal: strictly locked */ },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(8.dp)
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(2.dp, RetailRedPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = RetailRedDark,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("TERMINAL KASIR TERKUNCI", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Black)
                Text("Petugas: ${cashier?.nama ?: "Kasir"} (${cashier?.nik ?: "-"})", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                Text("Masukkan PIN atau Password untuk membuka kembali", fontSize = 11.sp, color = Color.Black)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorText = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN / Password Kasir", color = Color.DarkGray) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = RetailRedPrimary,
                        unfocusedBorderColor = PosCardBorder
                    )
                )

                // Keypad PIN Cepat
                Spacer(modifier = Modifier.height(10.dp))
                val pinKeys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                Column(modifier = Modifier.widthIn(max = 240.dp)) {
                    pinKeys.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { k ->
                                Button(
                                    onClick = {
                                        when (k) {
                                            "C" -> pinInput = ""
                                            "⌫" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                            else -> pinInput += k
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (k == "C" || k == "⌫") Color(0xFFECEFF1) else Color(0xFFF5F5F5),
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(k, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                                }
                            }
                        }
                    }
                }

                if (errorText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorText, color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("LOGOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Button(
                        onClick = {
                            if (pinInput.isEmpty()) {
                                errorText = "Masukkan PIN atau Password!"
                            } else {
                                val ok = onUnlock(pinInput)
                                if (!ok) {
                                    errorText = "PIN atau Password salah!"
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BUKA KUNCI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

