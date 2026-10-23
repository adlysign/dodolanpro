package com.example.ui.screens.clerek

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ClerekEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClerekScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentCashier by viewModel.currentCashier.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val clerekHistory by viewModel.allClerekRecords.collectAsState()
    val pendingCarts by viewModel.pendingCarts.collectAsState()
    val isShiftOpen by viewModel.isShiftOpen.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val shiftModalAwal by viewModel.shiftModalAwal.collectAsState()

    var uangSetoranFisikInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var lastSettledRecord by remember { mutableStateOf<ClerekEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }
    var saveDocMessage by remember { mutableStateOf("") }

    val unclerekTx = remember(allTransactions, currentCashier) {
        allTransactions.filter { it.kasirNik == (currentCashier?.nik ?: "") && !it.isClerek }
    }

    val liveTotalJual = remember(unclerekTx) { unclerekTx.sumOf { it.grandTotal } }
    val liveTunai = remember(unclerekTx) {
        unclerekTx.sumOf { tx ->
            when (tx.caraBayar) {
                "TUNAI" -> {
                    val net = tx.bayarTunai - tx.kembalian
                    if (net > 0.0) net else tx.grandTotal
                }
                "CAMPURAN" -> (tx.bayarTunai - tx.kembalian).coerceAtLeast(0.0)
                else -> 0.0
            }
        }
    }
    val liveNonTunai = remember(unclerekTx) {
        unclerekTx.sumOf { tx ->
            when (tx.caraBayar) {
                "TUNAI" -> 0.0
                "CAMPURAN" -> if (tx.bayarNonTunai > 0.0) tx.bayarNonTunai else (tx.grandTotal - (tx.bayarTunai - tx.kembalian)).coerceAtLeast(0.0)
                else -> tx.grandTotal
            }
        }
    }
    val liveCard = remember(unclerekTx) {
        unclerekTx.filter { it.caraBayar == "CARD" || it.caraBayar == "CAMPURAN" }.sumOf {
            if (it.caraBayar == "CAMPURAN") it.bayarNonTunai else it.grandTotal
        }
    }
    val liveWallet = remember(unclerekTx) {
        unclerekTx.filter { it.caraBayar == "WALLET" }.sumOf { it.grandTotal }
    }
    val liveVoucher = remember(unclerekTx) {
        unclerekTx.sumOf { it.nilaiPotonganVoucher }
    }

    val numericSetoranFisik = uangSetoranFisikInput.toDoubleOrNull() ?: 0.0
    val liveSelisih = numericSetoranFisik - (shiftModalAwal + liveTunai)

    val hasPending = pendingCarts.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CLEREK SETORAN KASIR", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailRedPrimary,
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("PROSES CLEREK") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("CETAK ULANG SETORAN") })
            }

            if (selectedTab == 0) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    item {
                        // User Header
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RetailNavy)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        if (isShiftOpen) "KASIR BERTUGAS (SHIFT $activeShift)" else "KASIR BERTUGAS (SHIFT TERTUTUP)",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(currentCashier?.nama ?: "Kasir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("NIK: ${currentCashier?.nik ?: "-"} | Role: ${currentCashier?.role ?: "-"}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (isShiftOpen) RetailGreen else Color.DarkGray,
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Text(
                                            if (isShiftOpen) "SHIFT AKTIF" else "SUDAH CLEREK",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (lastSettledRecord != null) {
                            // Tampilan HASIL REKONSILIASI CLEREK setelah proses selesai
                            val record = lastSettledRecord!!
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    when (record.statusSelisih) {
                                        "LEBIH" -> RetailNavy
                                        "KURANG" -> RetailRedPrimary
                                        else -> RetailGreen
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "HASIL REKONSILIASI CLEREK",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = RetailNavy
                                            )
                                            Text(
                                                "${record.kasirNama.ifBlank { currentCashier?.nama ?: "Kasir" }} (${record.kasirNik}) • ${record.shift}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                        Surface(
                                            color = when (record.statusSelisih) {
                                                "LEBIH" -> RetailNavy
                                                "KURANG" -> RetailRedPrimary
                                                else -> RetailGreen
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "STATUS: ${record.statusSelisih}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    ClerekRow("Modal Kasir Awal (Otomatis Buka Shift)", formatRupiah(record.modalAwal), isBold = true)
                                    ClerekRow("Total Uang Fisik Diinput", formatRupiah(record.uangSetoranFisik), isBold = true, color = RetailNavy)
                                    ClerekRow("Penjualan Tunai Sistem", formatRupiah(record.totalPenjualanTunai), color = RetailGreen)
                                    ClerekRow("Penjualan Non-Tunai Sistem", formatRupiah(record.totalPenjualanNonTunai))
                                    ClerekRow("Total Penjualan Shift", formatRupiah(record.totalPenjualan), isBold = true)
                                    ClerekRow("Total Donasi", formatRupiah(record.totalDonasi))
                                    ClerekRow("Jumlah Transaksi", "${record.jumlahTransaksi} transaksi")

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                    val uangSeharusnya = record.modalAwal + record.totalPenjualanTunai + record.totalDonasi
                                    ClerekRow("Uang Fisik Seharusnya di Laci (Modal + Tunai)", formatRupiah(uangSeharusnya), isBold = true)
                                    ClerekRow(
                                        label = when {
                                            record.selisih > 0 -> "Selisih Lebih (+)"
                                            record.selisih < 0 -> "Selisih Kurang (-)"
                                            else -> "Selisih (Pas)"
                                        },
                                        value = formatRupiah(record.selisih),
                                        isBold = true,
                                        color = when {
                                            record.selisih > 0 -> RetailNavy
                                            record.selisih < 0 -> RetailRedPrimary
                                            else -> RetailGreen
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val slipText = buildString {
                                                    appendLine("========================================")
                                                    appendLine("      BUKTI CLEREK SETORAN KASIR        ")
                                                    appendLine("----------------------------------------")
                                                    appendLine("Tanggal     : ${record.tanggal}")
                                                    appendLine("Kasir       : ${record.kasirNama.ifBlank { currentCashier?.nama ?: "Kasir" }} (${record.kasirNik})")
                                                    appendLine("Shift       : ${record.shift}")
                                                    appendLine("Modal Awal  : ${formatRupiah(record.modalAwal)}")
                                                    appendLine("----------------------------------------")
                                                    appendLine("Total Jual  : ${formatRupiah(record.totalPenjualan)}")
                                                    appendLine("Tunai Sistem: ${formatRupiah(record.totalPenjualanTunai)}")
                                                    appendLine("Non-Tunai   : ${formatRupiah(record.totalPenjualanNonTunai)}")
                                                    appendLine("Total Donasi: ${formatRupiah(record.totalDonasi)}")
                                                    appendLine("----------------------------------------")
                                                    appendLine("Uang Fisik  : ${formatRupiah(record.uangSetoranFisik)}")
                                                    appendLine("Seharusnya  : ${formatRupiah(uangSeharusnya)}")
                                                    appendLine("Selisih     : ${formatRupiah(record.selisih)}")
                                                    appendLine("Status      : ${record.statusSelisih}")
                                                    appendLine("========================================")
                                                }
                                                val saved = DocExportManager.saveDocument(
                                                    context = context,
                                                    subfolder = "Clerek",
                                                    prefix = "Clerek_${record.kasirNik}_${record.tanggal.replace("-", "").replace(" ", "_").replace(":", "")}",
                                                    extension = "txt",
                                                    content = slipText
                                                )
                                                saveDocMessage = "Tersimpan di DodolanPro/Clerek/${saved.second.name}"
                                            },
                                            modifier = Modifier.weight(1f).height(42.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SIMPAN FILE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = onNavigateBack,
                                            modifier = Modifier.weight(1f).height(42.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("KEMBALI KE KASIR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (saveDocMessage.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(saveDocMessage, fontSize = 11.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (!isShiftOpen) {
                            // Tampilan jika shift kasir sudah ditutup / sudah di-clerek
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, RetailGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(28.dp),
                                        color = RetailGreenLight,
                                        modifier = Modifier.size(56.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = RetailGreen,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "STATUS SHIFT: SUDAH DITUTUP (CLEREK SELESAI)",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = RetailNavy,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Shift untuk petugas ${currentCashier?.nama ?: "Kasir"} (${currentCashier?.nik ?: "-"}) saat ini sudah ditutup (sudah selesai di-clerek). Proses clerek hanya dapat dijalankan satu kali per sesi shift saat shift masih aktif.",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { selectedTab = 1 },
                                            modifier = Modifier.weight(1f).height(44.dp)
                                        ) {
                                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("RIWAYAT SETORAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = onNavigateBack,
                                            modifier = Modifier.weight(1f).height(44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                                        ) {
                                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("KEMBALI KE KASIR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Guard against Pending Transaksi
                            if (hasPending) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, RetailRedPrimary)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = RetailRedPrimary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PERINGATAN: TIDAK BISA CLEREK!", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Masih ada ${pendingCarts.size} transaksi dalam status PENDING di Kasir. Ketentuan sistem: Selesaikan atau hapus semua transaksi pending terlebih dahulu sebelum melakukan clerek setoran!",
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = onNavigateBack,
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("KEMBALI KE KASIR UNTUK RECALL", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Anti-Fraud Blind Balancing Info
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = RetailNavy,
                                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "CLEREK TERTUTUP (ANTI-FRAUD / BLIND BALANCING)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = RetailNavy
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Hitung seluruh uang fisik tunai yang ada di laci kasir secara mandiri tanpa melihat angka sistem. Modal awal kasir (${formatRupiah(shiftModalAwal)}) sudah otomatis tercatat saat Buka Shift. Kasir HANYA memasukkan jumlah uang fisik di laci saja tanpa memasukkan modal, karena modal sudah otomatis tercatat oleh sistem. Hasil rekonsiliasi dan selisih akan otomatis dihitung dan ditampilkan setelah proses disimpan.",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Input Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Input Jumlah Uang Fisik Kasir (Hasil Hitung Laci):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Info Box: Modal Awal Kasir Otomatis dari Buka Shift
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = RetailNavy,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        "Modal Awal Kasir (Shift #$activeShift)",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        "Tercatat otomatis saat Buka Shift (tidak perlu diinput lagi)",
                                                        fontSize = 10.sp,
                                                        color = RetailGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                formatRupiah(shiftModalAwal),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RetailNavy
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = uangSetoranFisikInput,
                                        onValueChange = { uangSetoranFisikInput = it.filter { ch -> ch.isDigit() } },
                                        modifier = Modifier.fillMaxWidth().testTag("input_setoran_fisik"),
                                        label = { Text("Jumlah Total Uang Fisik di Laci Kasir") },
                                        placeholder = { Text("Ketik total uang tunai yang dihitung di laci...") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "*Kasir hanya memasukkan total uang fisik di laci saja tanpa memasukkan modal, karena modal sudah otomatis tercatat oleh sistem.",
                                        fontSize = 10.sp,
                                        color = GrayText
                                    )
                                }
                            }

                            if (feedbackMessage.isNotEmpty()) {
                                Text(
                                    feedbackMessage,
                                    color = if (feedbackMessage.contains("berhasil", ignoreCase = true)) RetailGreen else RetailRedPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Submit Clerek button
                            Button(
                                onClick = {
                                    if (hasPending) {
                                        feedbackMessage = "Tidak bisa clerek! Masih ada transaksi pending."
                                        return@Button
                                    }
                                    if (uangSetoranFisikInput.isBlank()) {
                                        feedbackMessage = "Harap masukkan jumlah uang fisik yang ada di laci kasir!"
                                        return@Button
                                    }
                                    val numericFisik = uangSetoranFisikInput.toDoubleOrNull() ?: 0.0
                                    viewModel.submitClerek(numericFisik) { record ->
                                        lastSettledRecord = record
                                        feedbackMessage = "Clerek berhasil diproses! Hasil rekonsiliasi ditampilkan."
                                        uangSetoranFisikInput = ""
                                    }
                                },
                                enabled = !hasPending,
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_proses_clerek"),
                                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PROSES & LIHAT HASIL CLEREK", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Riwayat & Cetak Ulang Setoran (Save as document in DodolanPro/Clerek)
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    item {
                        Text(
                            "RIWAYAT CLEREK & CETAK ULANG",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = Color.Black
                        )
                    }

                    if (clerekHistory.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Belum ada riwayat clerek setoran tersimpan.", color = GrayText)
                            }
                        }
                    } else {
                        items(clerekHistory) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${item.kasirNama} (${item.kasirNik})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                        Text(item.tanggal, fontSize = 11.sp, color = GrayText)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Jual: ${formatRupiah(item.totalJualUmum)}", fontSize = 12.sp, color = Color.Black)
                                        Text("Setor: ${formatRupiah(item.uangSetoranFisik)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text(
                                            "Selisih: ${formatRupiah(item.selisih)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.selisih >= 0) RetailGreen else Color.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        Button(
                                            onClick = { lastSettledRecord = item },
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

    // Modal preview for Clerek print slip with Document Export to DodolanPro/Clerek
    lastSettledRecord?.let { record ->
        val clerekText = buildString {
            appendLine("========================================")
            appendLine("      BUKTI CLEREK SETORAN KASIR        ")
            appendLine("----------------------------------------")
            appendLine("Tanggal     : ${record.tanggal}")
            appendLine("Kasir       : ${record.kasirNama} (${record.kasirNik})")
            appendLine("Shift       : ${record.shift}")
            appendLine("Modal Awal  : ${formatRupiah(record.modalKasir)}")
            appendLine("----------------------------------------")
            appendLine("Total Jual  : ${formatRupiah(record.totalJualUmum)}")
            appendLine("Tunai Sistem: ${formatRupiah(record.totalTunai)}")
            appendLine("Card / EDC  : ${formatRupiah(record.totalCard)}")
            appendLine("E-Wallet    : ${formatRupiah(record.totalWallet)}")
            appendLine("Voucher     : ${formatRupiah(record.totalVoucher)}")
            appendLine("----------------------------------------")
            appendLine("Setoran Fisik : ${formatRupiah(record.uangSetoranFisik)}")
            appendLine("Selisih       : ${formatRupiah(record.selisih)}")
            appendLine("Status        : ${record.status}")
            appendLine("========================================")
        }

        AlertDialog(
            onDismissRequest = { lastSettledRecord = null },
            title = { Text("BUKTI KLEREK SETORAN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                    ) {
                        Text(
                            text = clerekText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp),
                            color = Color.Black
                        )
                    }
                    if (saveDocMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(saveDocMessage, fontSize = 11.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            val saved = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Clerek",
                                prefix = "Clerek_${record.kasirNik}_${record.tanggal.replace("-", "")}",
                                extension = "txt",
                                content = clerekText
                            )
                            saveDocMessage = "Tersimpan di DodolanPro/Clerek/${saved.second.name}"
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN FILE", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            lastSettledRecord = null
                            saveDocMessage = ""
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
fun ClerekRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    color: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (color != Color.Unspecified) color else Color.Black
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (color != Color.Unspecified) color else Color.Black
        )
    }
}

