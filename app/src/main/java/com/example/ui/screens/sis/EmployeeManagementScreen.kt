package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CashierUserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val cashiers by viewModel.cashiers.collectAsState()
    var nik by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("1234") }
    var role by remember { mutableStateOf("KASIR") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = { Text("Manajemen Karyawan & Kasir", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PosSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tambah Kasir / Petugas Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = nik, onValueChange = { nik = it }, label = { Text("NIK Kasir") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.weight(1.5f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("PIN Akses") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role (KASIR/ADMIN/OWNER)") }, modifier = Modifier.weight(1f))
                        }
                        Button(
                            onClick = {
                                if (nik.isNotBlank() && nama.isNotBlank()) {
                                    viewModel.saveCashierUser(CashierUserEntity(nik.trim(), nama.trim(), pin.trim(), role.trim()))
                                    nik = ""
                                    nama = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TAMBAH PENGGUNA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(cashiers) { c ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(c.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("NIK: ${c.nik} • Role: ${c.role} • PIN: ****", fontSize = 11.sp, color = GrayText)
                        }
                        if (c.nik != "11111111") {
                            IconButton(onClick = { viewModel.deleteCashierUser(c.nik) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RetailRedPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
