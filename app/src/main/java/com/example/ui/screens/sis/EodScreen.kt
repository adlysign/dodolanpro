package com.example.ui.screens.sis

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class EodStepItem(val title: String, var isDone: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EodScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val allClerek by viewModel.allClerekRecords.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var isRunningEod by remember { mutableStateOf(false) }
    var eodProgress by remember { mutableStateOf(0f) }
    var eodFinished by remember { mutableStateOf(false) }
    var showSendDcDialog by remember { mutableStateOf(false) }
    var dataSentSuccess by remember { mutableStateOf(false) }

    val eodSteps = remember {
        mutableStateListOf(
            EodStepItem("Update Transaksi Penjualan"),
            EodStepItem("Update Tanggal Pakai Voucher"),
            EodStepItem("Transaksi Kantong Plastik"),
            EodStepItem("Transaksi Non Commerce & Donasi"),
            EodStepItem("Rekap Data LPB Warehouse & BKL"),
            EodStepItem("Rekap Data Retur NRB"),
            EodStepItem("Rekap Data Koreksi Stok"),
            EodStepItem("Reset Stok Awal Hari"),
            EodStepItem("Reset Transaksi Backmas & KSF"),
            EodStepItem("Generate Berkas LZH Kirim Data DC")
        )
    }

    val unclerekCount = remember(allTransactions) { allTransactions.count { !it.isClerek } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TUTUP TOKO (EOD - END OF DAY)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                .padding(12.dp)
        ) {
            // Syarat Toko bisa EOD (from PDF page 89)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RetailBlueLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, RetailNavy.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("SYARAT TUTUP TOKO (EOD):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. ${if (unclerekCount == 0) "✅" else "⚠️"} Toko sudah clerek setoran semua user POS (${unclerekCount} transaksi pending clerek)",
                        fontSize = 11.sp,
                        fontWeight = if (unclerekCount > 0) FontWeight.Bold else FontWeight.Normal,
                        color = Color.Black
                    )
                    Text("2. ✅ SO perhari untuk rak yang ditentukan sudah dilakukan (Adjust)", fontSize = 11.sp)
                    Text("3. ✅ Barang receipt LPB sudah selesai diproses", fontSize = 11.sp)
                    Text("4. ✅ Semua EDC dan E-Trans sudah tersettlement", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isRunningEod || eodFinished) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (eodFinished) "Proses EOD Berhasil Diselesaikan!" else "Proses EOD sedang berjalan...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (eodFinished) RetailGreen else RetailNavy
                            )
                            Text("${(eodProgress * 100).toInt()}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { eodProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = if (eodFinished) RetailGreen else RetailRedPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Steps Checklist (from PDF page 93)
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    Text("KETERANGAN PROSES EOD (BATCH):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(eodSteps) { step ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = step.isDone,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = RetailGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(step.title, fontSize = 12.sp, fontWeight = if (step.isDone) FontWeight.SemiBold else FontWeight.Normal)
                            }
                            HorizontalDivider(color = PosCardBorder.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            if (eodFinished) {
                Button(
                    onClick = { showSendDcDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("KIRIM DATA KE WAREHOUSE DC (.LZH)")
                }
            } else {
                Button(
                    onClick = {
                        isRunningEod = true
                        eodProgress = 0f
                        coroutineScope.launch {
                            for (i in eodSteps.indices) {
                                delay(160)
                                eodSteps[i] = eodSteps[i].copy(isDone = true)
                                eodProgress = (i + 1).toFloat() / eodSteps.size
                            }
                            isRunningEod = false
                            eodFinished = true
                        }
                    },
                    enabled = !isRunningEod,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("JALANKAN PROSES EOD TUTUP TOKO", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    // Modal Kirim Data DC (from PDF page 94-100)
    if (showSendDcDialog) {
        val fileName = "${storeConfig.storeCode.lowercase()}${SimpleDateFormat("ddMM", Locale.getDefault()).format(Date())}.LZH"
        AlertDialog(
            onDismissRequest = { showSendDcDialog = false },
            title = { Text("KIRIM DATA DC (EOD EXPORT)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("File arsip EOD harian siap dikirimkan:")
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Nama Berkas : $fileName", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Tujuan      : satdc_balaraja@blj.sat.co.id", fontSize = 11.sp, color = RetailNavy)
                            Text("Status Data : Tersedia Offline & Terkompresi", fontSize = 11.sp, color = RetailGreen)
                        }
                    }
                    if (dataSentSuccess) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✅ Berkas berhasil diexport dan disimulasikan terkirim ke DC!", color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        dataSentSuccess = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Text("KIRIM DATA SEKARANG", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSendDcDialog = false }) {
                    Text("TUTUP")
                }
            }
        )
    }
}
