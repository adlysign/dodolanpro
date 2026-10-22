package com.example.ui.screens.sis

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ReturnDocEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.BluetoothPrinterHelper
import com.example.util.DocExportManager
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SelectedReturItem(
    val item: TransactionItemEntity,
    var returQty: Int = 1,
    var alasan: String = "BR (Barang Rusak / Cacat)"
) {
    val subtotalRefund: Double get() = item.hargaSatuan * returQty
}

val ALASAN_RETUR_KONSUMEN = listOf(
    "BR (Barang Rusak / Cacat)",
    "BKE (Expired / Kadaluwarsa)",
    "BRP (Kemasan Pecah / Bocor)",
    "BKS (Konsumen Salah Beli)",
    "BTA (Barang Tidak Sesuai)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturPenjualanView(
    viewModel: PosViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allReturns by viewModel.allReturns.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var subTab by remember { mutableStateOf(0) } // 0: Input Retur Penjualan, 1: Riwayat Retur Penjualan

    // Input States
    var nrkInput by remember { mutableStateOf("NRK-" + (100000..999999).random()) }
    var bonQueryInput by remember { mutableStateOf("") }
    var isSearchingBon by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }

    var loadedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var loadedTxItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }

    // Selected items for return: item.id to SelectedReturItem
    val selectedItemsMap = remember { mutableStateMapOf<Long, SelectedReturItem>() }

    var kembalikanKeStok by remember { mutableStateOf(true) }
    var metodePengembalian by remember { mutableStateOf("TUNAI") } // TUNAI, TUKAR_BARANG, SALDO_TOKO
    var supervisorPinInput by remember { mutableStateOf("") }
    var petugasNama by remember { mutableStateOf("") }

    // Dialogs
    var showRecentTxDialog by remember { mutableStateOf(false) }
    var selectedNrkForDialog by remember { mutableStateOf<ReturnDocEntity?>(null) }
    var docSaveMsg by remember { mutableStateOf("") }

    // Search & Filter in Riwayat
    var riwayatSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(currentCashier) {
        if (petugasNama.isBlank()) {
            petugasNama = currentCashier?.nama ?: "Kasir"
        }
    }

    val consumerReturns = remember(allReturns) {
        allReturns.filter { it.tipeRetur.equals("KONSUMEN", ignoreCase = true) || it.jenisRetur.contains("KONSUMEN", ignoreCase = true) }
    }

    val totalRefundNominal = selectedItemsMap.values.sumOf { it.subtotalRefund }
    val totalQtyRetur = selectedItemsMap.values.sumOf { it.returQty }

    fun lookupBon(bonKey: String) {
        val trimmed = bonKey.trim()
        if (trimmed.isEmpty()) {
            searchError = "Masukkan nomor BON terlebih dahulu"
            return
        }
        isSearchingBon = true
        searchError = ""
        coroutineScope.launch {
            val res = viewModel.findTransactionForRetur(trimmed)
            isSearchingBon = false
            if (res.first != null && res.second.isNotEmpty()) {
                loadedTransaction = res.first
                loadedTxItems = res.second
                selectedItemsMap.clear()
                searchError = ""
                feedbackMessage = "Dokumen BON ${res.first?.bonNo} ditemukan (${res.second.size} macam barang)."
            } else {
                searchError = "Transaksi dengan nomor BON '$trimmed' tidak ditemukan di database."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PosSurface)
    ) {
        // Sub-Tab Switcher
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    label = { Text("INPUT RETUR PENJUALAN", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RetailNavy,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    label = { Text("RIWAYAT RETUR (${consumerReturns.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RetailNavy,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Feedback Banner
        if (feedbackMessage.isNotBlank()) {
            Surface(
                color = Color(0xFFEFF6FF),
                modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, Color(0xFFBFDBFE)))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(feedbackMessage, fontSize = 12.sp, color = RetailNavy, fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { feedbackMessage = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = RetailNavy, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        if (subTab == 0) {
            // ==========================================
            // TAB 0: INPUT RETUR PENJUALAN (KONSUMEN)
            // ==========================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // CARD 1: Cari BON Transaksi Asal
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PosCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = RetailRedPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1. Masukkan No. BON / Struk Asal:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                }
                                TextButton(
                                    onClick = { showRecentTxDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = RetailNavy)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pilih Transaksi Terakhir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = bonQueryInput,
                                    onValueChange = {
                                        bonQueryInput = it
                                        searchError = ""
                                    },
                                    placeholder = { Text("Ketik No BON lengkap atau 4 digit akhir") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { lookupBon(bonQueryInput) })
                                )
                                Button(
                                    onClick = { lookupBon(bonQueryInput) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(52.dp)
                                ) {
                                    if (isSearchingBon) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CARI BON")
                                    }
                                }
                            }

                            if (searchError.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(searchError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Info Transaksi jika sudah ditemukan
                            loadedTransaction?.let { tx ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("NO. BON: ${tx.bonNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                            Text(tx.tanggal, fontSize = 11.sp, color = GrayText)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Kasir: ${tx.kasirNama}", fontSize = 11.sp, color = Color(0xFF334155))
                                            Text("Member: ${tx.memberNo.ifBlank { "Non-Member" }}", fontSize = 11.sp, color = Color(0xFF334155))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Metode: ${tx.caraBayar}", fontSize = 11.sp, color = GrayText)
                                            Text("Total Belanja: ${formatRupiah(tx.grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailRedPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CARD 2: Pilih Barang yang Dikembalikan
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PosCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = RetailRedPrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("2. Pilih Barang yang Diretur:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                }
                                if (loadedTxItems.isNotEmpty()) {
                                    Text("${selectedItemsMap.size}/${loadedTxItems.size} Dipilih", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (loadedTxItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Belum ada transaksi BON yang dimuat", fontWeight = FontWeight.Bold, color = GrayText, fontSize = 12.sp)
                                        Text("Ketik nomor BON di atas atau klik 'Pilih Transaksi Terakhir'", fontSize = 11.sp, color = GrayText)
                                    }
                                }
                            } else {
                                loadedTxItems.forEachIndexed { idx, item ->
                                    val isSelected = selectedItemsMap.containsKey(item.id)
                                    val selectedInfo = selectedItemsMap[item.id]

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) RetailAmberLight else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(1.dp, if (isSelected) RetailAmber else Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            if (checked) {
                                                                selectedItemsMap[item.id] = SelectedReturItem(item = item, returQty = 1)
                                                            } else {
                                                                selectedItemsMap.remove(item.id)
                                                            }
                                                        }
                                                    )
                                                    Column {
                                                        Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                                        Text("PLU: ${item.plu} | Dibeli: ${item.qty} pcs @ ${formatRupiah(item.hargaSatuan)}", fontSize = 11.sp, color = GrayText)
                                                    }
                                                }
                                                Text(formatRupiah(item.subtotal), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                            }

                                            // Detail jika dipilih
                                            if (isSelected && selectedInfo != null) {
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFFDE68A))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Jumlah Retur:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFE2E8F0),
                                                            modifier = Modifier.size(28.dp).clickable {
                                                                if (selectedInfo.returQty > 1) {
                                                                    selectedItemsMap[item.id] = selectedInfo.copy(returQty = selectedInfo.returQty - 1)
                                                                }
                                                            }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            }
                                                        }

                                                        Text(
                                                            "${selectedInfo.returQty} pcs",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(horizontal = 10.dp)
                                                        )

                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = RetailNavy,
                                                            modifier = Modifier.size(28.dp).clickable {
                                                                if (selectedInfo.returQty < item.qty) {
                                                                    selectedItemsMap[item.id] = selectedInfo.copy(returQty = selectedInfo.returQty + 1)
                                                                }
                                                            }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        "Refund: ${formatRupiah(selectedInfo.subtotalRefund)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = RetailRedPrimary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                // Pilihan Alasan
                                                Text("Alasan Pengembalian:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    ALASAN_RETUR_KONSUMEN.take(3).forEach { r ->
                                                        val isCodeSel = selectedInfo.alasan.startsWith(r.take(3))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isCodeSel) RetailNavy else Color.White,
                                                            border = BorderStroke(1.dp, if (isCodeSel) RetailNavy else PosCardBorder),
                                                            modifier = Modifier.clickable {
                                                                selectedItemsMap[item.id] = selectedInfo.copy(alasan = r)
                                                            }
                                                        ) {
                                                            Text(
                                                                r.take(3),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCodeSel) Color.White else Color(0xFF475569),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
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

                // CARD 3: Konfigurasi, Opsi Restock & Otorisasi PIN
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PosCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = RetailRedPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("3. Opsi Retur & Otorisasi:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Switch Restock Fisik
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Kembalikan Barang ke Stok Fisik Toko", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    Text(
                                        if (kembalikanKeStok) "Stok bertambah otomatis (+Qty)" else "Barang rusak/dibuang, stok tidak dikembalikan",
                                        fontSize = 10.sp,
                                        color = if (kembalikanKeStok) RetailGreen else RetailRedPrimary
                                    )
                                }
                                Switch(
                                    checked = kembalikanKeStok,
                                    onCheckedChange = { kembalikanKeStok = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Metode Pengembalian Dana
                            Text("Metode Pengembalian Dana ke Konsumen:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "TUNAI" to "Uang Tunai",
                                    "TUKAR_BARANG" to "Tukar Barang",
                                    "SALDO_TOKO" to "Nota Kredit"
                                ).forEach { (code, label) ->
                                    FilterChip(
                                        selected = metodePengembalian == code,
                                        onClick = { metodePengembalian = code },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Petugas & No Dokumen NRK
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = nrkInput,
                                    onValueChange = { nrkInput = it },
                                    label = { Text("No. Dokumen NRK") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = petugasNama,
                                    onValueChange = { petugasNama = it },
                                    label = { Text("Petugas Kasir") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // PIN Otorisasi Supervisor
                            OutlinedTextField(
                                value = supervisorPinInput,
                                onValueChange = { supervisorPinInput = it },
                                label = { Text("PIN Otorisasi Supervisor / Owner *") },
                                placeholder = { Text("Ketik PIN untuk persetujuan retur") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = RetailRedPrimary) }
                            )
                        }
                    }
                }

                // CARD 4: Ringkasan Nilai & Tombol Proses Retur
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RetailAmberLight),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("RINGKASAN RETUR KONSUMEN", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF78350F))
                                    Text("Total Fisik Diretur: $totalQtyRetur Pcs (${selectedItemsMap.size} Macam)", fontSize = 11.sp, color = Color(0xFF92400E))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("TOTAL DANA REFUND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                    Text(
                                        formatRupiah(totalRefundNominal),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = RetailRedPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    if (loadedTransaction == null) {
                                        feedbackMessage = "Harap muat dokumen BON transaksi terlebih dahulu!"
                                        return@Button
                                    }
                                    if (selectedItemsMap.isEmpty()) {
                                        feedbackMessage = "Pilih minimal 1 barang yang akan diretur!"
                                        return@Button
                                    }
                                    if (supervisorPinInput.trim().isEmpty()) {
                                        feedbackMessage = "PIN otorisasi supervisor / owner wajib diisi!"
                                        return@Button
                                    }

                                    // Verifikasi wewenang PIN
                                    val auth = viewModel.verifyAuthorityPin(supervisorPinInput) { it.canReturKonsumen }
                                    if (!auth.first) {
                                        feedbackMessage = auth.second
                                        return@Button
                                    }

                                    val currentNrk = nrkInput
                                    val bonNo = loadedTransaction?.bonNo ?: ""
                                    val itemsToProcess = selectedItemsMap.values.map { sel ->
                                        sel.item to Pair(sel.returQty, sel.alasan)
                                    }

                                    viewModel.recordCustomerReturn(
                                        nrkNo = currentNrk,
                                        bonNo = bonNo,
                                        pelangganNama = loadedTransaction?.memberNo?.ifBlank { "Konsumen Umum" } ?: "Konsumen Umum",
                                        petugasNama = petugasNama,
                                        alasanUtama = selectedItemsMap.values.firstOrNull()?.alasan ?: "Retur Transaksi",
                                        items = itemsToProcess,
                                        kembalikanKeStok = kembalikanKeStok,
                                        metodePengembalian = metodePengembalian
                                    ) { success, msg ->
                                        if (success) {
                                            feedbackMessage = msg
                                            // Tampilkan dialog cetak nota retur
                                            val createdDoc = ReturnDocEntity(
                                                nrbNo = currentNrk,
                                                tipeRetur = "KONSUMEN",
                                                refNo = bonNo,
                                                fakturAsal = bonNo,
                                                supplierNama = loadedTransaction?.memberNo?.ifBlank { "Konsumen Umum" } ?: "Konsumen Umum",
                                                jenisRetur = "RETUR PENJUALAN ($metodePengembalian)",
                                                tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                                                totalNilai = totalRefundNominal,
                                                alasan = selectedItemsMap.values.firstOrNull()?.alasan ?: "Retur",
                                                keteranganRetur = "Restock: ${if (kembalikanKeStok) "YA" else "TIDAK"} | Metode: $metodePengembalian",
                                                status = "SELESAI",
                                                petugasNama = petugasNama,
                                                tokoTujuan = "Toko Utama",
                                                totalQty = totalQtyRetur,
                                                itemsJson = org.json.JSONArray().apply {
                                                    selectedItemsMap.values.forEach { sel ->
                                                        put(org.json.JSONObject().apply {
                                                            put("plu", sel.item.plu)
                                                            put("barcode", sel.item.barcode)
                                                            put("nama", sel.item.namaBarang)
                                                            put("satuan", "PCS")
                                                            put("qty", sel.returQty)
                                                            put("hargaSatuan", sel.item.hargaSatuan)
                                                            put("subtotal", sel.subtotalRefund)
                                                            put("alasan", sel.alasan)
                                                        })
                                                    }
                                                }.toString()
                                            )
                                            selectedNrkForDialog = createdDoc
                                            // Reset input
                                            selectedItemsMap.clear()
                                            loadedTransaction = null
                                            loadedTxItems = emptyList()
                                            bonQueryInput = ""
                                            supervisorPinInput = ""
                                            nrkInput = "NRK-" + (100000..999999).random()
                                        } else {
                                            feedbackMessage = msg
                                        }
                                    }
                                },
                                enabled = selectedItemsMap.isNotEmpty() && supervisorPinInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PROSES RETUR PENJUALAN (NRK)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 1: RIWAYAT RETUR PENJUALAN
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = riwayatSearchQuery,
                    onValueChange = { riwayatSearchQuery = it },
                    placeholder = { Text("Cari No NRK, No BON, atau Nama Konsumen...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (riwayatSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { riwayatSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filteredReturns = remember(consumerReturns, riwayatSearchQuery) {
                    if (riwayatSearchQuery.isBlank()) consumerReturns
                    else consumerReturns.filter {
                        it.nrbNo.contains(riwayatSearchQuery, ignoreCase = true) ||
                        it.refNo.contains(riwayatSearchQuery, ignoreCase = true) ||
                        it.supplierNama.contains(riwayatSearchQuery, ignoreCase = true) ||
                        it.petugasNama.contains(riwayatSearchQuery, ignoreCase = true)
                    }
                }

                // Summary Stats
                val totalRefundSemua = consumerReturns.sumOf { it.totalNilai }
                val totalItemFisikSemua = consumerReturns.sumOf { it.totalQty }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, PosCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL RETUR KONSUMEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                            Text("${consumerReturns.size} Dokumen ($totalItemFisikSemua Pcs)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("TOTAL DANA REFUND", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrayText)
                            Text(formatRupiah(totalRefundSemua), fontWeight = FontWeight.Black, fontSize = 15.sp, color = RetailRedPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredReturns.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Belum ada riwayat retur penjualan", fontWeight = FontWeight.Bold, color = GrayText)
                            Text("Retur barang dari konsumen yang diproses akan tersimpan di sini", fontSize = 11.sp, color = GrayText)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredReturns) { ret ->
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
                                        Text(ret.nrbNo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "SELESAI",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RetailGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("BON Asal: ${ret.refNo} | Tanggal: ${ret.tanggal}", fontSize = 11.sp, color = GrayText)
                                    Text("Konsumen: ${ret.supplierNama} • Petugas: ${ret.petugasNama}", fontSize = 11.sp, color = Color(0xFF334155))
                                    Text("Alasan: ${ret.alasan}", fontSize = 11.sp, color = Color(0xFF64748B))

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Dana Pengembalian:", fontSize = 10.sp, color = GrayText)
                                            Text(formatRupiah(ret.totalNilai), fontWeight = FontWeight.Black, fontSize = 14.sp, color = RetailRedPrimary)
                                        }
                                        OutlinedButton(
                                            onClick = { selectedNrkForDialog = ret },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Lihat / Cetak NRK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // Modal Pilih dari Transaksi Terakhir
    if (showRecentTxDialog) {
        val recentTxList = remember(allTransactions) { allTransactions.take(15) }
        AlertDialog(
            onDismissRequest = { showRecentTxDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PILIH TRANSAKSI BON TERAKHIR", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            text = {
                if (recentTxList.isEmpty()) {
                    Text("Belum ada riwayat transaksi penjualan di sistem.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                        items(recentTxList) { tx ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        bonQueryInput = tx.bonNo
                                        lookupBon(tx.bonNo)
                                        showRecentTxDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(tx.bonNo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                        Text(tx.tanggal, fontSize = 10.sp, color = GrayText)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kasir: ${tx.kasirNama}", fontSize = 11.sp, color = GrayText)
                                        Text(formatRupiah(tx.grandTotal), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailRedPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecentTxDialog = false }) {
                    Text("TUTUP")
                }
            }
        )
    }

    // Modal Cetak & Lihat Dokumen NRK (Nota Retur Konsumen)
    selectedNrkForDialog?.let { doc ->
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
                                "qty" to obj.optInt("qty", 1),
                                "hargaSatuan" to obj.optDouble("hargaSatuan", 0.0),
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

        val slipText = remember(doc, itemsList, storeConfig) {
            buildString {
                appendLine("========================================")
                appendLine("       ${storeConfig.storeName.ifBlank { "DODOLAN PRO POS" }}")
                appendLine("      ${storeConfig.storeAddress.ifBlank { "SUPERMARKET & TOKO RETAIL" }}")
                appendLine("========================================")
                appendLine("       NOTA RETUR PENJUALAN (NRK)")
                appendLine("========================================")
                appendLine("NOMOR NRK       : ${doc.nrbNo}")
                appendLine("NOMOR BON ASAL  : ${doc.refNo}")
                appendLine("TANGGAL RETUR   : ${doc.tanggal}")
                appendLine("KONSUMEN        : ${doc.supplierNama}")
                appendLine("PETUGAS KASIR   : ${doc.petugasNama}")
                appendLine("JENIS PROSES    : ${doc.jenisRetur}")
                appendLine("STATUS DOKUMEN  : SELESAI")
                appendLine("----------------------------------------")
                appendLine("BARANG YANG DIKEMBALIKAN:")
                appendLine("PLU    NAMA BARANG          QTY      HARGA      REFUND")
                appendLine("----------------------------------------")
                if (itemsList.isNotEmpty()) {
                    itemsList.forEach { itm ->
                        val plu = (itm["plu"] as? String ?: "").take(6).padEnd(6)
                        val nama = (itm["nama"] as? String ?: "").take(18).padEnd(18)
                        val qty = "${itm["qty"]} pcs".take(8).padStart(8)
                        val harga = (itm["hargaSatuan"] as? Double ?: 0.0).toLong().toString().padStart(8)
                        val sub = (itm["subtotal"] as? Double ?: 0.0).toLong().toString().padStart(9)
                        appendLine("$plu $nama $qty $harga $sub")
                        val itemAlasan = itm["alasan"] as? String ?: ""
                        if (itemAlasan.isNotBlank()) {
                            appendLine("       [Alasan: $itemAlasan]")
                        }
                    }
                } else {
                    appendLine("Barang Retur Konsumen: ${formatRupiah(doc.totalNilai)}")
                }
                appendLine("----------------------------------------")
                appendLine("TOTAL FISIK DIRETUR : ${doc.totalQty} Pcs")
                appendLine("TOTAL PENGEMBALIAN  : ${formatRupiah(doc.totalNilai)}")
                appendLine("KETERANGAN         : ${doc.keteranganRetur}")
                appendLine("========================================")
                appendLine("LEMBAR BUKTI RETUR KONSUMEN:")
                appendLine("[1] Lembar Konsumen : Bukti Pengembalian Dana/Barang")
                appendLine("[2] Lembar Kasir    : Rekap Clerek & Audit Toko")
                appendLine("----------------------------------------")
                appendLine("   Konsumen / Pelanggan       Petugas Kasir")
                appendLine("")
                appendLine("  (____________________)     (${doc.petugasNama})")
                appendLine("========================================")
            }
        }

        AlertDialog(
            onDismissRequest = {
                selectedNrkForDialog = null
                docSaveMsg = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SLIP NOTA RETUR KONSUMEN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                                    text = slipText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    if (docSaveMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(docSaveMsg, fontSize = 11.sp, color = RetailNavy, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Tombol Cetak Printer Thermal
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (storeConfig.bluetoothPrinterMac.isNotBlank()) {
                                    val escPos = com.example.util.ReceiptFormatter.generateEscPos(slipText, storeConfig)
                                    val res = BluetoothPrinterHelper.printOverBluetooth(storeConfig.bluetoothPrinterMac, escPos)
                                    docSaveMsg = if (res.first) "✓ Selesai mencetak ke printer" else res.second
                                } else {
                                    val res = BluetoothPrinterHelper.shareReceiptText(context, slipText, "Nota Retur ${doc.nrbNo}")
                                    docSaveMsg = res.second
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CETAK STRUK", fontSize = 11.sp)
                    }

                    // Tombol Simpan Dokumen
                    OutlinedButton(
                        onClick = {
                            val res = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Retur",
                                prefix = doc.nrbNo,
                                extension = "txt",
                                content = slipText
                            )
                            docSaveMsg = if (res.first) "Berkas tersimpan di: ${res.second.absolutePath}" else "Gagal menyimpan berkas."
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN FILE", fontSize = 11.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedNrkForDialog = null; docSaveMsg = "" }) {
                    Text("TUTUP")
                }
            }
        )
    }
}
