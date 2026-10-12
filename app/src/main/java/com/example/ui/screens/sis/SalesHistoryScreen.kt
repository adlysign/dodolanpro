package com.example.ui.screens.sis

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRetur: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedTxItems by remember { mutableStateOf<List<TransactionItemEntity>>(emptyList()) }
    var saveDocMessage by remember { mutableStateOf("") }

    val filteredTransactions = remember(allTransactions, searchQuery) {
        allTransactions.filter {
            it.bonNo.contains(searchQuery, ignoreCase = true) ||
                    it.kasirNama.contains(searchQuery, ignoreCase = true) ||
                    it.memberNo.contains(searchQuery, ignoreCase = true) ||
                    it.tanggal.contains(searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PENJUALAN TOKO - RIWAYAT NOTA", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari Nomor Bon / Nama Kasir / Member / Tanggal...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("TOTAL RIWAYAT PENJUALAN: ${filteredTransactions.size} TRANSAKSI", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GrayText)

            Spacer(modifier = Modifier.height(6.dp))

            if (filteredTransactions.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data penjualan toko.", color = GrayText)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredTransactions) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BON: ${tx.bonNo}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                    Text("${tx.tanggal} ${tx.waktu}", fontSize = 11.sp, color = GrayText)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Kasir: ${tx.kasirNama}", fontSize = 12.sp, color = Color.Black)
                                    Text(formatRupiah(tx.grandTotal), fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                                }

                                Text("Metode: ${tx.caraBayar} ${if (tx.refPembayaran.isNotEmpty()) "(${tx.refPembayaran})" else ""}", fontSize = 11.sp, color = GrayText)

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedButton(
                                        onClick = { onNavigateToRetur() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp).padding(end = 6.dp)
                                    ) {
                                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Retur", fontSize = 10.sp)
                                    }

                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val items = viewModel.findTransactionForRetur(tx.bonNo).second
                                                selectedTxItems = items
                                                selectedTx = tx
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Lihat / Simpan File", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal View & Export Document to DodolanPro/Penjualan
    selectedTx?.let { tx ->
        val receiptDocText = buildString {
            appendLine("       ${storeConfig.storeName}       ")
            appendLine("       ${storeConfig.alamat}       ")
            appendLine("        TELP: ${storeConfig.telp}        ")
            appendLine("     NPWP: ${storeConfig.npwp}     ")
            appendLine("----------------------------------------")
            appendLine("BON   : ${tx.bonNo} KASIR: ${tx.kasirNama}")
            if (tx.memberNo.isNotEmpty()) {
                appendLine("MEMBER: ${tx.memberNo}")
            }
            appendLine("----------------------------------------")
            selectedTxItems.forEach { item ->
                val name = item.namaBarang.take(20).padEnd(20)
                val qtyStr = "${item.qty}".padStart(3)
                val priceStr = formatRupiah(item.hargaSatuan).padStart(12)
                appendLine("$name $qtyStr $priceStr")
            }
            appendLine("----------------------------------------")
            appendLine("TOTAL      : ${formatRupiah(tx.grandTotal)}")
            appendLine("Cara Bayar : ${tx.caraBayar}")
            if (tx.caraBayar == "CAMPURAN") {
                appendLine("Bayar Tunai: ${formatRupiah(tx.bayarTunai)}")
                appendLine("Non-Tunai  : ${formatRupiah(tx.bayarNonTunai)}")
            } else if (tx.caraBayar == "TUNAI") {
                appendLine("Tunai      : ${formatRupiah(tx.bayarTunai)}")
                appendLine("Kembalian  : ${formatRupiah(tx.kembalian)}")
            }
            appendLine("Tgl. ${tx.tanggal} ${tx.waktu}")
            appendLine("Status: ${tx.status}")
            appendLine("========================================")
        }

        AlertDialog(
            onDismissRequest = {
                selectedTx = null
                saveDocMessage = ""
            },
            title = { Text("NOTA PENJUALAN TOKO", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
                    ) {
                        Text(
                            text = receiptDocText,
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
                                subfolder = "Penjualan",
                                prefix = "Nota_${tx.bonNo}",
                                extension = "txt",
                                content = receiptDocText
                            )
                            saveDocMessage = "Tersimpan di DodolanPro/Penjualan/${saved.second.name}"
                        }
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN FILE", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            selectedTx = null
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
