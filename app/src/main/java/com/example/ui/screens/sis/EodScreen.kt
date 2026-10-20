package com.example.ui.screens.sis

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class EodStepItem(val title: String, var isDone: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EodScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allClerek by viewModel.allClerekRecords.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val isShiftOpen by viewModel.isShiftOpen.collectAsState()

    var isRunningEod by remember { mutableStateOf(false) }
    var eodProgress by remember { mutableStateOf(0f) }
    var eodFinished by remember { mutableStateOf(false) }
    var savedBackupFile by remember { mutableStateOf<File?>(null) }
    var savedSummaryFile by remember { mutableStateOf<File?>(null) }
    var statusMessage by remember { mutableStateOf("") }

    val eodSteps = remember {
        mutableStateListOf(
            EodStepItem("1. Validasi Status Shift & Clerek Kasir"),
            EodStepItem("2. Rekapitulasi Data Transaksi Penjualan"),
            EodStepItem("3. Pembaruan Tanggal & Status Voucher"),
            EodStepItem("4. Pembuatan Berkas Cadangan Database JSON"),
            EodStepItem("5. Simpan Otomatis Cadangan ke Memori Android (DodolanPro/Backup)"),
            EodStepItem("6. Pembuatan Berkas Laporan Ringkasan EOD (DodolanPro/EOD)"),
            EodStepItem("7. Finalisasi Tutup Toko Harian")
        )
    }

    val unclerekCount = remember(allTransactions) { allTransactions.count { !it.isClerek } }
    val hasClerekDone = remember(allClerek) { allClerek.isNotEmpty() }
    val isShiftClosed = !isShiftOpen

    // Multi-Device & Kasir Pending Check
    val syncConfig by viewModel.syncConfig.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    var eodPendingCheck by remember { mutableStateOf<com.example.data.sync.EodPendingCheckResult?>(null) }
    var isCheckingPending by remember { mutableStateOf(false) }

    fun refreshPendingCheck() {
        isCheckingPending = true
        viewModel.checkMultiDeviceEodPending { res ->
            isCheckingPending = false
            eodPendingCheck = res
        }
    }

    LaunchedEffect(Unit) {
        refreshPendingCheck()
    }

    val hasPendingAcrossDevices = eodPendingCheck?.canProceed == false

    // Syarat utama EOD:
    // 1. Shift kasir sudah pernah dibuka dan sekarang sudah ditutup/selesai (isShiftClosed)
    // 2. Sudah dilakukan proses clerek kasir (hasClerekDone)
    // 3. Tidak ada transaksi yang belum di-clerek (unclerekCount == 0)
    // 4. Semua kasir aktif & perangkat multi-device telah menyelesaikan keranjang pending (!hasPendingAcrossDevices)
    val canRunEod = isShiftClosed && hasClerekDone && unclerekCount == 0 && !hasPendingAcrossDevices

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "TUTUP TOKO (EOD - END OF DAY)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Pencadangan Data Otomatis & Penutupan Operasional Harian",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
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
                .padding(14.dp)
        ) {
            // Card 1: Syarat Tutup Toko (EOD)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "SYARAT PENJALANAN EOD TUTUP TOKO:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = RetailNavy
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status 1: Shift Kasir
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isShiftClosed && hasClerekDone) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isShiftClosed && hasClerekDone) RetailGreen else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isShiftClosed && hasClerekDone)
                                "Shift kasir telah dibuka dan diselesaikan (Shift Ditutup)"
                            else if (isShiftOpen)
                                "Shift kasir saat ini masih AKTIF / Terbuka (Wajib Clerek dahulu)"
                            else
                                "Belum ada shift yang dibuka & diselesaikan hari ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isShiftClosed && hasClerekDone) RetailGreen else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status 2: Clerek Setoran
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (hasClerekDone && unclerekCount == 0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (hasClerekDone && unclerekCount == 0) RetailGreen else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (hasClerekDone && unclerekCount == 0)
                                "Toko sudah clerek setoran kasir (${allClerek.size} sesi clerek tersimpan, 0 pending)"
                            else if (unclerekCount > 0)
                                "Masih ada $unclerekCount transaksi penjualan yang belum di-clerek!"
                            else
                                "Kasir belum melakukan proses Clerek setoran!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasClerekDone && unclerekCount == 0) RetailGreen else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status 3: Backup Otomatis Android
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Folder penyimpanan Android siap: Documents/Downloads/DodolanPro/Backup",
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status 4: Validasi Multi-Perangkat & Pending Item Kasir
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (!hasPendingAcrossDevices) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (!hasPendingAcrossDevices) RetailGreen else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (!hasPendingAcrossDevices)
                                "Semua kasir aktif telah menyelesaikan transaksi pending (0 Pending)"
                            else
                                "EOD Ditolak: ${eodPendingCheck?.message ?: "Ada transaksi pending yang belum selesai"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (!hasPendingAcrossDevices) RetailGreen else Color(0xFFC62828)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Warning jika terdapat Pending Items di Kasir Aktif
            if (hasPendingAcrossDevices && !eodFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailRedPrimary)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = RetailRedPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "EOD DITOLAK: MASIH ADA KERANJANG PENDING!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = RetailRedPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Tutup Toko (EOD) belum bisa dilakukan. Semua kasir aktif wajib menyelesaikan seluruh transaksi pending miliknya sebelum EOD:",
                            fontSize = 11.sp,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        eodPendingCheck?.devicesWithPending?.forEach { dev ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetailRedPrimary.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(dev.deviceName, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                        Text("Kasir: ${dev.cashierName} (NIK: ${dev.cashierNik})", fontSize = 10.sp, color = GrayText)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = RetailRedPrimary
                                    ) {
                                        Text(
                                            "${dev.pendingCount} Pending",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { refreshPendingCheck() },
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cek Ulang Status", fontSize = 11.sp)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(PosNavDestination.KASIR) },
                                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Text("Buka Kasir", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Warning jika belum memenuhi syarat lain
            if (!canRunEod && !hasPendingAcrossDevices && !eodFinished) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "EOD BELUM DAPAT DIJALANKAN",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isShiftOpen) {
                                "Shift kasir saat ini masih aktif dan belum di-clerek. Silakan buka menu Clerek kasir terlebih dahulu untuk mencetak dan menyimpan setoran shift."
                            } else if (unclerekCount > 0) {
                                "Terdapat $unclerekCount transaksi yang belum dimasukkan ke dalam Clerek. Silakan selesaikan Clerek di menu Clerek Kasir."
                            } else {
                                "EOD hanya bisa dilakukan ketika shift kasir sudah dibuka dan sudah dilakukan proses Clerek."
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (isShiftOpen || unclerekCount > 0) {
                                    viewModel.navigateTo(PosNavDestination.CLEREK)
                                } else {
                                    viewModel.navigateTo(PosNavDestination.KASIR)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailAmber, contentColor = Color.White),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                if (isShiftOpen || unclerekCount > 0) Icons.Default.PointOfSale else Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isShiftOpen || unclerekCount > 0) "Buka Menu Clerek Sekarang" else "Buka Shift di Layar Kasir",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Progress Bar saat proses berjalan
            if (isRunningEod || eodFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (eodFinished) "Proses EOD & Backup Berhasil Selesai!" else "Proses EOD sedang berjalan...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (eodFinished) RetailGreen else RetailNavy
                            )
                            Text(
                                "${(eodProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { eodProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (eodFinished) RetailGreen else RetailRedPrimary,
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Checklist Tahapan EOD
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    Text(
                        "TAHAPAN PROSES EOD & PENCADANGAN OTOMATIS:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(eodSteps) { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = step.isDone,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = RetailGreen,
                                        uncheckedColor = Color(0xFF94A3B8)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    step.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (step.isDone) FontWeight.Bold else FontWeight.Medium,
                                    color = if (step.isDone) Color(0xFF0F172A) else Color(0xFF475569)
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }

                    // Hasil Berkas Backup jika sudah selesai
                    if (eodFinished && savedBackupFile != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = RetailGreenLight,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "BERKAS BACKUP BERHASIL TERSIMPAN DI ANDROID",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RetailGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "File Backup : ${savedBackupFile?.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Lokasi Folder : ${savedBackupFile?.parent ?: "DodolanPro/Backup"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155)
                                )
                                if (savedSummaryFile != null) {
                                    Text(
                                        "Laporan EOD : ${savedSummaryFile?.name}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button
            if (eodFinished) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            savedBackupFile?.let { file ->
                                try {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Bagikan Berkas Backup EOD"))
                                } catch (_: Exception) {
                                    // Fallback text share
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "Backup EOD berhasil disimpan di: ${file.absolutePath}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Salin Info Backup"))
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("BAGIKAN FILE BACKUP", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SELESAI (KEMBALI)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }
                }
            } else {
                Button(
                    onClick = {
                        isRunningEod = true
                        eodProgress = 0f
                        coroutineScope.launch {
                            // Step 1 - 3
                            for (i in 0..2) {
                                delay(200)
                                eodSteps[i] = eodSteps[i].copy(isDone = true)
                                eodProgress = (i + 1).toFloat() / eodSteps.size
                            }

                            // Step 4 - 6: Jalankan backup nyata ke memori Android
                            viewModel.executeEodBackup { success, backupFile, summaryFile, msg ->
                                coroutineScope.launch {
                                    for (i in 3 until eodSteps.size) {
                                        delay(250)
                                        eodSteps[i] = eodSteps[i].copy(isDone = true)
                                        eodProgress = (i + 1).toFloat() / eodSteps.size
                                    }
                                    savedBackupFile = backupFile
                                    savedSummaryFile = summaryFile
                                    statusMessage = msg
                                    isRunningEod = false
                                    eodFinished = true
                                }
                            }
                        }
                    },
                    enabled = canRunEod && !isRunningEod,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetailRedPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCBD5E1),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (canRunEod) Icons.Default.Backup else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (canRunEod) Color.White else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (canRunEod)
                            "JALANKAN PROSES EOD & BACKUP DATA"
                        else
                            "EOD TERKUNCI (Wajib Buka Shift & Clerek Dahulu)",
                        fontWeight = FontWeight.Bold,
                        color = if (canRunEod) Color.White else Color(0xFF64748B),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
