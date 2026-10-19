package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VoucherEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherPointsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val vouchers by viewModel.allVouchers.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<VoucherEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    val filteredVouchers = remember(vouchers, searchQuery) {
        if (searchQuery.isBlank()) vouchers
        else vouchers.filter { it.kodeVoucher.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VOUCHER & POIN TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Kelola Kupon Diskon & Reward Pelanggan", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = RetailRedPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("btn_tambah_voucher")
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BUAT VOUCHER", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF1F5F9))
                .padding(12.dp)
        ) {
            // Stats & Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Voucher", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.size}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailNavy)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Voucher Aktif", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.count { !it.isUsed && !it.isExpired() }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sudah Terpakai", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.count { it.isUsed }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF64748B))
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari Kode Voucher...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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

            if (filteredVouchers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Belum ada voucher yang dibuat.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredVouchers) { v ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(v.kodeVoucher, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = when (v.statusText) {
                                                "AKTIF" -> Color(0xFFE8F5E9)
                                                "KEDALUWARSA" -> Color(0xFFFFEBEE)
                                                else -> Color(0xFFECEFF1)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                v.statusText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (v.statusText) {
                                                    "AKTIF" -> RetailGreen
                                                    "KEDALUWARSA" -> RetailRedPrimary
                                                    else -> Color.Gray
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Diskon: ${formatRupiah(v.nominalDiskon)} | Min. Belanja: ${formatRupiah(v.minBelanja)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    if (v.tanggalKedaluwarsa.isNotBlank()) {
                                        Text("Berlaku s/d: ${v.tanggalKedaluwarsa}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    if (v.isUsed && v.usedAtBonNo.isNotBlank()) {
                                        Text("Dipakai pada Struk: ${v.usedAtBonNo}", fontSize = 10.sp, color = Color(0xFFE65100))
                                    }
                                }

                                IconButton(onClick = { voucherToDelete = v }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var kode by remember { mutableStateOf("") }
        var nominalStr by remember { mutableStateOf("10000") }
        var minBelanjaStr by remember { mutableStateOf("50000") }
        var masaBerlakuHari by remember { mutableStateOf("30") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Terbitkan Voucher Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = kode,
                        onValueChange = { kode = it.uppercase() },
                        label = { Text("Kode Voucher (Kosongkan utk Otomatis)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nominalStr,
                        onValueChange = { nominalStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Nominal Potongan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = minBelanjaStr,
                        onValueChange = { minBelanjaStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Syarat Minimal Belanja (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = masaBerlakuHari,
                        onValueChange = { masaBerlakuHari = it.filter { c -> c.isDigit() } },
                        label = { Text("Masa Berlaku (Hari)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nom = nominalStr.toDoubleOrNull() ?: 10000.0
                        val minB = minBelanjaStr.toDoubleOrNull() ?: 50000.0
                        val hari = masaBerlakuHari.toIntOrNull() ?: 30

                        if (kode.isBlank()) {
                            viewModel.generateManualRandomVoucher(nom, minB, hari) { v ->
                                feedbackMessage = "Voucher ${v.kodeVoucher} berhasil dibuat!"
                                showAddDialog = false
                            }
                        } else {
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, hari)
                            val expDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
                            val newV = VoucherEntity(
                                kodeVoucher = kode.trim(),
                                nominalDiskon = nom,
                                minBelanja = minB,
                                tanggalKedaluwarsa = expDate
                            )
                            viewModel.insertVoucher(newV) {
                                feedbackMessage = "Voucher ${newV.kodeVoucher} berhasil dibuat!"
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                ) {
                    Text("Simpan Voucher", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Batal", color = Color.Black)
                }
            }
        )
    }

    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("Hapus Voucher?", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = { Text("Apakah Anda yakin ingin menghapus voucher '${v.kodeVoucher}'?", color = Color.Black) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoucher(v) {
                            feedbackMessage = "Voucher ${v.kodeVoucher} dihapus"
                            voucherToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
                ) {
                    Text("Ya, Hapus", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { voucherToDelete = null }) {
                    Text("Batal", color = Color.Black)
                }
            }
        )
    }
}
