package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.ui.screens.pos.ReceiptDialog
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesHistoryScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRetur: () -> Unit = {}
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    val filteredList = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) transactions
        else transactions.filter {
            it.bonNo.contains(searchQuery, ignoreCase = true) ||
            it.kasirNama.contains(searchQuery, ignoreCase = true) ||
            it.caraBayar.contains(searchQuery, ignoreCase = true) ||
            it.tanggal.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("RIWAYAT PENJUALAN", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Total: ${transactions.size} transaksi", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRetur) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = "Retur", tint = Color.White)
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
                .background(Color(0xFFF1F5F9))
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().testTag("search_history_input"),
                placeholder = { Text("Cari Nomor Bon / Tanggal / Kasir...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = RetailNavy,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (feedbackMessage.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        feedbackMessage,
                        color = RetailGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data transaksi penjualan.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTransaction = tx },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(tx.bonNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = when (tx.caraBayar) {
                                                "TUNAI" -> Color(0xFFE8F5E9)
                                                "QRIS" -> Color(0xFFE3F2FD)
                                                else -> Color(0xFFFFF3E0)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                tx.caraBayar,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${tx.tanggal} ${tx.waktu} | Kasir: ${tx.kasirNama}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    if (tx.grandTotal < 0) {
                                        Text("RETUR PENJUALAN", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        formatRupiah(tx.grandTotal),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = if (tx.grandTotal < 0) Color(0xFFE65100) else Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Lihat Struk >", fontSize = 11.sp, color = RetailNavy, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        ReceiptDialog(
            transaction = tx,
            items = emptyList(),
            storeConfig = storeConfig,
            onDismiss = { selectedTransaction = null }
        )
    }
}
