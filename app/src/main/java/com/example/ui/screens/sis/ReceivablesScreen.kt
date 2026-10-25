package com.example.ui.screens.sis

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.ReceivableEntity
import com.example.data.local.entity.ReceivablePaymentEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.retailTextFieldColors
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivablesScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val receivables by viewModel.allReceivables.collectAsState()
    val allPayments by viewModel.allReceivablePayments.collectAsState()
    val members by viewModel.allMembers.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("SEMUA") } // SEMUA, BELUM_LUNAS, JATUH_TEMPO, LUNAS
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddManualDialog by remember { mutableStateOf(false) }
    var payingReceivable by remember { mutableStateOf<ReceivableEntity?>(null) }
    var viewingHistoryReceivable by remember { mutableStateOf<ReceivableEntity?>(null) }
    var deletingReceivable by remember { mutableStateOf<ReceivableEntity?>(null) }
    var sharingReminderReceivable by remember { mutableStateOf<ReceivableEntity?>(null) }

    // Metrics calculations
    val totalPiutangBelumLunas = remember(receivables) {
        receivables.filter { !it.status.equals("LUNAS", ignoreCase = true) }
            .sumOf { it.remainingAmount }
    }
    val totalPiutangLunas = remember(receivables) {
        receivables.filter { it.status.equals("LUNAS", ignoreCase = true) }
            .sumOf { it.totalAmount }
    }
    val countJatuhTempo = remember(receivables) {
        receivables.count { it.isOverdue() }
    }
    val countDebiturAktif = remember(receivables) {
        receivables.filter { !it.status.equals("LUNAS", ignoreCase = true) }
            .map { it.customerName.trim().lowercase() }
            .distinct()
            .size
    }

    // Filtered list
    val filteredReceivables = remember(receivables, searchQuery, selectedFilter) {
        receivables.filter { r ->
            val matchesSearch = searchQuery.isBlank() ||
                    r.customerName.contains(searchQuery, ignoreCase = true) ||
                    r.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    r.bonNo.contains(searchQuery, ignoreCase = true) ||
                    r.notes.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "BELUM_LUNAS" -> !r.status.equals("LUNAS", ignoreCase = true)
                "JATUH_TEMPO" -> r.isOverdue()
                "LUNAS" -> r.status.equals("LUNAS", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("receivables_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Column {
                        Text("Piutang Pelanggan (Kasbon)", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "${receivables.size} Catatan Piutang • ${formatRupiah(totalPiutangBelumLunas)} Tertunggak",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddManualDialog = true },
                        modifier = Modifier.testTag("btn_add_manual_receivable")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Tambah Piutang Manual", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF1F5F9))
                .padding(12.dp)
        ) {
            // --- 1. KPI Summary Cards ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Total Piutang Belum Lunas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MoneyOff, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Belum Lunas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatRupiah(totalPiutangBelumLunas), fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFB71C1C))
                        Text("$countDebiturAktif debitur aktif", fontSize = 9.sp, color = Color.Gray)
                    }
                }

                // Card 2: Lewat Jatuh Tempo
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = if (countJatuhTempo > 0) Color(0xFFFFF3E0) else Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (countJatuhTempo > 0) Color(0xFFFFB74D) else Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Jatuh Tempo", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$countJatuhTempo Tagihan", fontWeight = FontWeight.Black, fontSize = 13.sp, color = if (countJatuhTempo > 0) Color(0xFFE65100) else Color.Black)
                        Text(if (countJatuhTempo > 0) "Perlu ditagih" else "Semua tepat waktu", fontSize = 9.sp, color = Color.Gray)
                    }
                }

                // Card 3: Total Piutang Lunas
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Total Lunas", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RetailGreen)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(formatRupiah(totalPiutangLunas), fontWeight = FontWeight.Black, fontSize = 13.sp, color = RetailGreen)
                        Text("Riwayat pelunasan", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 2. Search Field ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("receivables_search_input"),
                placeholder = { Text("Cari Debitur / No. Bon / No. HP / Catatan...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RetailNavy) },
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

            // --- 3. Filter Chips ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    Pair("SEMUA", "Semua (${receivables.size})"),
                    Pair("BELUM_LUNAS", "Belum Lunas (${receivables.count { !it.status.equals("LUNAS", ignoreCase = true) }})"),
                    Pair("JATUH_TEMPO", "Jatuh Tempo ($countJatuhTempo)"),
                    Pair("LUNAS", "Lunas (${receivables.count { it.status.equals("LUNAS", ignoreCase = true) }})")
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color.Black
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (key == "JATUH_TEMPO") Color(0xFFE65100) else RetailNavy,
                            containerColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Color.Transparent else Color(0xFFCBD5E1)
                        )
                    )
                }
            }

            if (feedbackMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(feedbackMessage ?: "", fontSize = 11.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { feedbackMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = RetailGreen)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- 4. Receivables List ---
            if (filteredReceivables.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada data piutang yang sesuai", color = Color.Gray, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddManualDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Catat Piutang Baru", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredReceivables, key = { it.id }) { item ->
                        ReceivableCardItem(
                            receivable = item,
                            onPay = { payingReceivable = item },
                            onViewHistory = { viewingHistoryReceivable = item },
                            onShareReminder = { sharingReminderReceivable = item },
                            onDelete = { deletingReceivable = item }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG 1: Catat Pembayaran / Cicilan Piutang ---
    if (payingReceivable != null) {
        val target = payingReceivable!!
        var payAmountInput by remember { mutableStateOf(target.remainingAmount.toLong().toString()) }
        var selectedPayMethod by remember { mutableStateOf("TUNAI") } // TUNAI, TRANSFER, QRIS, DEBIT
        var payNotes by remember { mutableStateOf("") }
        var payErrorMessage by remember { mutableStateOf("") }

        val numericAmount = payAmountInput.toDoubleOrNull() ?: 0.0

        Dialog(
            onDismissRequest = { payingReceivable = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .imePadding()
                    .testTag("dialog_pay_receivable"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = RetailGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Catat Pembayaran Piutang", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                        }
                        IconButton(onClick = { payingReceivable = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Info Debitur Card
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Debitur: ${target.customerName}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            if (target.customerPhone.isNotBlank()) {
                                Text("No. HP: ${target.customerPhone}", fontSize = 11.sp, color = Color.Gray)
                            }
                            if (target.bonNo.isNotBlank()) {
                                Text("No. Bon: ${target.bonNo}", fontSize = 11.sp, color = RetailNavy)
                            }
                            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Sisa Piutang Saat Ini:", fontSize = 11.sp, color = Color.Black)
                                Text(formatRupiah(target.remainingAmount), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Nominal Pembayaran (Rp):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = payAmountInput,
                        onValueChange = { payAmountInput = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_pay_receivable_amount"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("Ketik nominal pembayaran...") },
                        singleLine = true,
                        colors = retailTextFieldColors()
                    )

                    // Quick buttons: Lunas Penuh atau 50%
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { payAmountInput = target.remainingAmount.toLong().toString() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                        ) {
                            Text("Lunas Penuh (${formatRupiah(target.remainingAmount)})", fontSize = 10.sp)
                        }
                        if (target.remainingAmount >= 20000.0) {
                            OutlinedButton(
                                onClick = { payAmountInput = (target.remainingAmount / 2).toLong().toString() },
                                modifier = Modifier.weight(0.6f)
                            ) {
                                Text("Cicil 50%", fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Metode Pembayaran:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("TUNAI", "TRANSFER", "QRIS", "DEBIT").forEach { method ->
                            FilterChip(
                                selected = selectedPayMethod == method,
                                onClick = { selectedPayMethod = method },
                                label = { Text(method, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = payNotes,
                        onValueChange = { payNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Catatan Pembayaran (Opsional)") },
                        placeholder = { Text("Contoh: Cicilan ke-1 / Titip kasir") },
                        singleLine = true,
                        colors = retailTextFieldColors()
                    )

                    if (payErrorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(payErrorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { payingReceivable = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (numericAmount <= 0.0) {
                                    payErrorMessage = "Nominal pembayaran harus lebih besar dari Rp 0"
                                    return@Button
                                }
                                if (numericAmount > target.remainingAmount) {
                                    payErrorMessage = "Nominal pembayaran melebihi sisa tagihan (${formatRupiah(target.remainingAmount)})"
                                    return@Button
                                }
                                viewModel.recordReceivablePayment(
                                    receivableId = target.id,
                                    amount = numericAmount,
                                    method = selectedPayMethod,
                                    notes = payNotes.ifBlank { "Pembayaran $selectedPayMethod" }
                                ) { ok, msg ->
                                    if (ok) {
                                        feedbackMessage = "Pembayaran sebesar ${formatRupiah(numericAmount)} untuk ${target.customerName} berhasil disimpan!"
                                        payingReceivable = null
                                    } else {
                                        payErrorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("btn_confirm_pay_receivable"),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                        ) {
                            Text("Simpan Pembayaran", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG 2: Tambah Piutang Manual ---
    if (showAddManualDialog) {
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custAddress by remember { mutableStateOf("") }
        var memberNo by remember { mutableStateOf("") }
        var totalAmountInput by remember { mutableStateOf("") }
        var dpAmountInput by remember { mutableStateOf("0") }
        var dueDateInput by remember {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 14)
            mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time))
        }
        var manualNotes by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showAddManualDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.9f)
                    .imePadding()
                    .testTag("dialog_add_manual_receivable"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Catat Piutang Manual", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                        IconButton(onClick = { showAddManualDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Nama Debitur / Pelanggan *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            OutlinedTextField(
                                value = custName,
                                onValueChange = { custName = it },
                                modifier = Modifier.fillMaxWidth().testTag("manual_cust_name"),
                                placeholder = { Text("Contoh: Bpk. Bambang / Warung Barokah") },
                                singleLine = true,
                                colors = retailTextFieldColors()
                            )
                        }

                        if (members.isNotEmpty()) {
                            item {
                                Text("Pilih dari Member:", fontSize = 10.sp, color = Color.Gray)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(members.take(6)) { m ->
                                        FilterChip(
                                            selected = custName == m.nama,
                                            onClick = {
                                                custName = m.nama
                                                custPhone = m.noHp
                                                custAddress = m.alamat
                                                memberNo = m.memberNo
                                            },
                                            label = { Text(m.nama, fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = custPhone,
                                    onValueChange = { custPhone = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("No. HP / WA (Opsional)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = custAddress,
                                    onValueChange = { custAddress = it },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Alamat / Lokasi (Opsional)") },
                                    singleLine = true,
                                    colors = retailTextFieldColors()
                                )
                            }
                        }

                        item {
                            Text("Total Piutang (Rp) *", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            OutlinedTextField(
                                value = totalAmountInput,
                                onValueChange = { totalAmountInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("manual_total_amount"),
                                placeholder = { Text("Contoh: 150000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = retailTextFieldColors()
                            )
                        }

                        item {
                            Text("Uang Muka / DP Awal (Rp)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            OutlinedTextField(
                                value = dpAmountInput,
                                onValueChange = { dpAmountInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("0 jika tanpa uang muka") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = retailTextFieldColors()
                            )
                        }

                        item {
                            Text("Jatuh Tempo Pembayaran (dd/MM/yyyy)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf(7, 14, 30).forEach { days ->
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.DAY_OF_YEAR, days)
                                    val dStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
                                    FilterChip(
                                        selected = dueDateInput == dStr,
                                        onClick = { dueDateInput = dStr },
                                        label = { Text("+$days Hari", fontSize = 10.sp) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = dueDateInput,
                                onValueChange = { dueDateInput = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                placeholder = { Text("dd/MM/yyyy") },
                                singleLine = true,
                                colors = retailTextFieldColors()
                            )
                        }

                        item {
                            Text("Catatan / Keterangan", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                            OutlinedTextField(
                                value = manualNotes,
                                onValueChange = { manualNotes = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Contoh: Kasbon belanja pesanan katering") },
                                singleLine = true,
                                colors = retailTextFieldColors()
                            )
                        }

                        if (inputError.isNotBlank()) {
                            item {
                                Text(inputError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showAddManualDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                val tot = totalAmountInput.toDoubleOrNull() ?: 0.0
                                val dp = dpAmountInput.toDoubleOrNull() ?: 0.0
                                if (custName.isBlank()) {
                                    inputError = "Nama pelanggan / debitur wajib diisi!"
                                    return@Button
                                }
                                if (tot <= 0.0) {
                                    inputError = "Total piutang harus lebih besar dari Rp 0"
                                    return@Button
                                }
                                if (dp >= tot) {
                                    inputError = "Uang muka tidak boleh sama atau melebihi total piutang!"
                                    return@Button
                                }
                                viewModel.addManualReceivable(
                                    customerName = custName,
                                    customerPhone = custPhone,
                                    customerAddress = custAddress,
                                    memberNo = memberNo,
                                    totalAmount = tot,
                                    dpAmount = dp,
                                    dueDate = dueDateInput,
                                    notes = manualNotes
                                ) { ok, msg ->
                                    if (ok) {
                                        feedbackMessage = "Piutang untuk $custName berhasil dicatat!"
                                        showAddManualDialog = false
                                    } else {
                                        inputError = msg
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.5f).testTag("btn_save_manual_receivable"),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                        ) {
                            Text("Simpan Piutang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOG 3: Detail & Riwayat Pembayaran ---
    if (viewingHistoryReceivable != null) {
        val target = viewingHistoryReceivable!!
        var paymentsForThis by remember { mutableStateOf<List<ReceivablePaymentEntity>>(emptyList()) }
        var transactionItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
        var isLoadingDetails by remember { mutableStateOf(true) }

        LaunchedEffect(target.id) {
            isLoadingDetails = true
            paymentsForThis = viewModel.getPaymentsForReceivable(target.id)
            if (target.bonNo.isNotBlank()) {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                transactionItems = db.posDao().getItemsForBon(target.bonNo)
            }
            isLoadingDetails = false
        }

        Dialog(
            onDismissRequest = { viewingHistoryReceivable = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Detail & Riwayat Piutang", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                            Text(target.customerName, fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { viewingHistoryReceivable = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFE2E8F0))

                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            // Ringkasan Keuangan
                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Status Tagihan:", fontSize = 11.sp, color = Color.Gray)
                                        Text(
                                            target.status.replace("_", " "),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = when (target.status) {
                                                "LUNAS" -> RetailGreen
                                                "SEBAGIAN" -> Color(0xFFE65100)
                                                else -> Color(0xFFC62828)
                                            }
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Piutang Awal:", fontSize = 11.sp, color = Color.Gray)
                                        Text(formatRupiah(target.totalAmount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Terbayar:", fontSize = 11.sp, color = Color.Gray)
                                        Text(formatRupiah(target.paidAmount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailGreen)
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFE2E8F0))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sisa Hutang:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                        Text(formatRupiah(target.remainingAmount), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                                    }
                                    if (target.dueDate.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Jatuh Tempo: ${target.dueDate}" + if (target.isOverdue()) " (LEWAT TEMPO)" else "",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (target.isOverdue()) Color(0xFFC62828) else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        // Riwayat Pembayaran / Cicilan
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Riwayat Cicilan & Pembayaran (${paymentsForThis.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                        }

                        if (paymentsForThis.isEmpty()) {
                            item {
                                Surface(
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Belum ada riwayat pembayaran / cicilan yang dicatat.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(8.dp))
                                }
                            }
                        } else {
                            items(paymentsForThis) { p ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(formatRupiah(p.paymentAmount), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailGreen)
                                            Text("${p.paymentDate} • Metode: ${p.paymentMethod}", fontSize = 10.sp, color = Color.Gray)
                                            if (p.notes.isNotBlank()) {
                                                Text(p.notes, fontSize = 10.sp, color = Color.Black)
                                            }
                                        }
                                        Text("Petugas: ${p.kasirNama}", fontSize = 9.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Detail Barang dari Transaksi Kasir (jika ada bonNo)
                        if (transactionItems.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Item Belanja Bon #${target.bonNo} (${transactionItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            }

                            items(transactionItems) { item ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.nama, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                            Text("${item.qty} ${item.satuan} @ ${formatRupiah(item.hargaJual)}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(formatRupiah(item.subtotalItem), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewingHistoryReceivable = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                    ) {
                        Text("Tutup")
                    }
                }
            }
        }
    }

    // --- DIALOG 4: Kirim Pengingat / Salin WA ---
    if (sharingReminderReceivable != null) {
        val target = sharingReminderReceivable!!
        val storeName = storeConfig.storeName.ifBlank { "Toko" }
        val templateText = "Halo Bpk/Ibu ${target.customerName},\n" +
                "Kami dari *$storeName* menginformasikan catatan tagihan belanja Anda:\n\n" +
                "• No Bon: ${target.bonNo.ifBlank { "-" }}\n" +
                "• Tanggal: ${target.tanggal}\n" +
                "• Total Belanja: ${formatRupiah(target.totalAmount)}\n" +
                "• Sudah Dibayar: ${formatRupiah(target.paidAmount)}\n" +
                "• *SISA HUTANG: ${formatRupiah(target.remainingAmount)}*\n" +
                "• Jatuh Tempo: ${target.dueDate}\n\n" +
                "Pembayaran dapat dilakukan secara tunai di kasir toko atau transfer. Terima kasih banyak atas kepercayaan Anda berbelanja di *$storeName*."

        AlertDialog(
            onDismissRequest = { sharingReminderReceivable = null },
            icon = { Icon(Icons.Default.Share, contentDescription = null, tint = RetailGreen) },
            title = { Text("Kirim Pengingat Tagihan", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pesan pengingat untuk ${target.customerName}:", fontSize = 12.sp, color = Color.Gray)
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            templateText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPhone = target.customerPhone.filter { it.isDigit() }
                        val formattedPhone = when {
                            cleanPhone.startsWith("0") -> "62" + cleanPhone.drop(1)
                            cleanPhone.startsWith("62") -> cleanPhone
                            else -> cleanPhone
                        }
                        if (formattedPhone.isNotBlank()) {
                            try {
                                val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=" + Uri.encode(templateText)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, templateText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Kirim Pengingat Piutang"))
                            }
                        } else {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, templateText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Kirim Pengingat Piutang"))
                        }
                        sharingReminderReceivable = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kirim WhatsApp")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(templateText))
                        Toast.makeText(context, "Teks pengingat berhasil disalin!", Toast.LENGTH_SHORT).show()
                        sharingReminderReceivable = null
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Teks")
                }
            }
        )
    }

    // --- DIALOG 5: Konfirmasi Hapus ---
    if (deletingReceivable != null) {
        val target = deletingReceivable!!
        AlertDialog(
            onDismissRequest = { deletingReceivable = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Hapus Catatan Piutang?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus data piutang '${target.customerName}' dengan sisa tagihan ${formatRupiah(target.remainingAmount)}? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReceivable(target.id) { ok ->
                            if (ok) {
                                feedbackMessage = "Data piutang berhasil dihapus."
                            }
                            deletingReceivable = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deletingReceivable = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun ReceivableCardItem(
    receivable: ReceivableEntity,
    onPay: () -> Unit,
    onViewHistory: () -> Unit,
    onShareReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val isLunas = receivable.status.equals("LUNAS", ignoreCase = true)
    val isOverdue = receivable.isOverdue()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("receivable_card_${receivable.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isLunas -> Color(0xFFA5D6A7)
                isOverdue -> Color(0xFFFFAB91)
                else -> Color(0xFFE2E8F0)
            }
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Customer Name & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isLunas) RetailGreenLight else Color(0xFFFFEBEE),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isLunas) Icons.Default.CheckCircle else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isLunas) RetailGreen else Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            receivable.customerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RetailNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (receivable.bonNo.isNotBlank()) {
                            Text("Bon: ${receivable.bonNo}", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdue) {
                        Surface(
                            color = Color(0xFFFFEBEE),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "LEWAT TEMPO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC62828),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = when (receivable.status) {
                            "LUNAS" -> Color(0xFFE8F5E9)
                            "SEBAGIAN" -> Color(0xFFFFF3E0)
                            else -> Color(0xFFFFEBEE)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            receivable.status.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (receivable.status) {
                                "LUNAS" -> RetailGreen
                                "SEBAGIAN" -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contact & Notes Info
            if (receivable.customerPhone.isNotBlank() || receivable.customerAddress.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (receivable.customerPhone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(receivable.customerPhone, fontSize = 11.sp, color = Color(0xFF334155))
                        }
                    }
                    if (receivable.customerAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(receivable.customerAddress, fontSize = 11.sp, color = Color(0xFF334155), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (receivable.notes.isNotBlank()) {
                Text("Ket: ${receivable.notes}", fontSize = 10.sp, color = Color(0xFF64748B), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Financial Summary Block
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Tagihan: ${formatRupiah(receivable.totalAmount)}", fontSize = 10.sp, color = Color.Gray)
                        Text("Terbayar: ${formatRupiah(receivable.paidAmount)}", fontSize = 10.sp, color = RetailGreen)
                        if (receivable.dueDate.isNotBlank()) {
                            Text(
                                "Jatuh Tempo: ${receivable.dueDate}",
                                fontSize = 10.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOverdue) Color(0xFFC62828) else Color(0xFF475569)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("SISA HUTANG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isLunas) RetailGreen else Color(0xFFC62828))
                        Text(
                            formatRupiah(receivable.remainingAmount),
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isLunas) RetailGreen else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isLunas) {
                        Button(
                            onClick = onPay,
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp).testTag("btn_pay_${receivable.id}")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Bayar / Cicil", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onViewHistory,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_history_${receivable.id}")
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Riwayat", fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (!isLunas) {
                        IconButton(onClick = onShareReminder, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, contentDescription = "Ingatkan WA", tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
