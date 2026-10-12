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
import com.example.data.local.entity.ReturnDocEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnPurchaseScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val returnDocs by viewModel.allReturns.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Form Retur Pembelian, 1: Fixed Retur, 2: Riwayat NRB
    var showNewReturnDialog by remember { mutableStateOf(false) }
    var selectedNrbForPrint by remember { mutableStateOf<ReturnDocEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RETUR PEMBELIAN (NRB)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("FORM RETUR") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("FIXED RETUR") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("ARSIP ODNER 03") })
            }

            when (selectedTab) {
                0 -> {
                    // Form Retur Pembelian (from PDF page 60-66)
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RetailAmberLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("PROSES PEMBUATAN FORM RETUR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF795548))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tahap 1: Membuat daftar barang yang akan diretur ke DC / Supplier (BKE, BR, BRP, BTA, BKC). Barang dalam Form belum memotong stok fisik sampai dilakukan proses Fixed.",
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showNewReturnDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("BUAT FORM RETUR PEMBELIAN BARU", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("DAFTAR FORM RETUR (PENDING FIXED):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        val draftReturns = returnDocs.filter { it.status == "FORM" }
                        if (draftReturns.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Tidak ada form retur yang pending.", color = GrayText)
                                }
                            }
                        } else {
                            items(draftReturns) { ret ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(ret.nrbNo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Surface(
                                                color = RetailAmber,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("STATUS: FORM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                        Text("Supplier: ${ret.supplierNama} (${ret.jenisRetur})", fontSize = 12.sp)
                                        Text("Alasan: ${ret.keteranganRetur}", fontSize = 11.sp, color = GrayText)
                                        Text("Nilai: ${formatRupiah(ret.totalNilai)}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Proses Fixed Retur (from PDF page 69-73)
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = RetailBlueLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("PROSES FIXED RETUR PEMBELIAN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tahap 2: Finalisasi retur toko. Pada proses ini, stok on-hand toko akan otomatis terpotong dan menghasilkan dokumen NRB Fixed (3-ply).",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("PILIH FORM UNTUK DI-FIXED:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        val draftReturns = returnDocs.filter { it.status == "FORM" }
                        if (draftReturns.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("Semua retur telah berstatus FIXED.", color = GrayText)
                                }
                            }
                        } else {
                            items(draftReturns) { ret ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(ret.nrbNo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Supplier: ${ret.supplierNama} | ${ret.keteranganRetur}", fontSize = 12.sp)
                                        Text("Total Nilai: ${formatRupiah(ret.totalNilai)}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = {
                                                viewModel.submitReturnDoc(
                                                    nrbNo = ret.nrbNo,
                                                    fakturAsal = ret.fakturAsal,
                                                    supplierNama = ret.supplierNama,
                                                    jenisRetur = ret.jenisRetur,
                                                    keterangan = ret.keteranganRetur,
                                                    status = "FIXED",
                                                    totalNilai = ret.totalNilai
                                                ) {
                                                    selectedNrbForPrint = ret.copy(status = "FIXED")
                                                    feedbackMessage = "Retur ${ret.nrbNo} berhasil FIXED!"
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PROSES FIXED & CETAK NRB (3-PLY)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Arsip Odner 03 (from PDF page 73 & 133)
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        items(returnDocs.filter { it.status == "FIXED" }) { ret ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("NRB: ${ret.nrbNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(ret.tanggal, fontSize = 11.sp, color = GrayText)
                                    }
                                    Text("Supplier: ${ret.supplierNama} (${ret.jenisRetur})", fontSize = 12.sp)
                                    Text("Alasan: ${ret.keteranganRetur} | Nilai: ${formatRupiah(ret.totalNilai)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        OutlinedButton(
                                            onClick = { selectedNrbForPrint = ret },
                                            modifier = Modifier.height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cetak NRB", fontSize = 11.sp)
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

    // Modal Create Form Retur
    if (showNewReturnDialog) {
        var returType by remember { mutableStateOf("DC") }
        var supplierName by remember { mutableStateOf("DC SAT CILEUNGSI 2") }
        var returReason by remember { mutableStateOf("BKE (Expired)") }
        var returNominal by remember { mutableStateOf("450000") }

        AlertDialog(
            onDismissRequest = { showNewReturnDialog = false },
            title = { Text("BUAT FORM RETUR PEMBELIAN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Jenis Retur:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DC", "BKL").forEach { r ->
                            FilterChip(selected = returType == r, onClick = { returType = r }, label = { Text("RETUR $r") })
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Supplier / DC") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Keterangan Retur:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val reasons = listOf("BKE (Expired)", "BR (Rusak)", "BRP (Pengiriman)", "BTA (Tidak Ada)", "BKC (Kurang)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        reasons.take(3).forEach { reas ->
                            FilterChip(selected = returReason == reas, onClick = { returReason = reas }, label = { Text(reas.take(5), fontSize = 10.sp) })
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = returNominal,
                        onValueChange = { returNominal = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Total Nilai Retur (Rp)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nom = returNominal.toDoubleOrNull() ?: 100000.0
                        val nrbCode = "EG85-R-D17-" + (1000..9999).random()
                        viewModel.submitReturnDoc(
                            nrbNo = nrbCode,
                            fakturAsal = "FAK-" + (1000..9999).random(),
                            supplierNama = supplierName,
                            jenisRetur = returType,
                            keterangan = returReason,
                            status = "FORM",
                            totalNilai = nom
                        ) {
                            showNewReturnDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Text("SIMPAN FORM", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewReturnDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal Print NRB (from PDF page 73)
    selectedNrbForPrint?.let { ret ->
        AlertDialog(
            onDismissRequest = { selectedNrbForPrint = null },
            title = { Text("CETAK NOTA RETUR BARANG (NRB)", fontWeight = FontWeight.Bold) },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                ) {
                    Text(
                        text = buildString {
                            appendLine("PT. SUMBER ALFARIA TRIJAYA TBK")
                            appendLine("NOTA RETUR BARANG (NRB - ${ret.status})")
                            appendLine("NOMOR NRB: ${ret.nrbNo}")
                            appendLine("SUPPLIER : ${ret.supplierNama}")
                            appendLine("TANGGAL  : ${ret.tanggal}")
                            appendLine("ALASAN   : ${ret.keteranganRetur}")
                            appendLine("TOTAL RP : ${formatRupiah(ret.totalNilai)}")
                            appendLine("----------------------------------------")
                            appendLine("Status Dokumen: FIXED (Stok Terpotong)")
                            appendLine("Arsip: Odner No. 03 (Nota Retur)")
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedNrbForPrint = null }, colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)) {
                    Text("TUTUP", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
