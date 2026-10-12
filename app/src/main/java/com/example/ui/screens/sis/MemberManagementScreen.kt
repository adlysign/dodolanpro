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
import com.example.data.local.entity.MemberEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val members by viewModel.members.collectAsState()
    var memberNo by remember { mutableStateOf("MBR-" + System.currentTimeMillis().toString().takeLast(4)) }
    var nama by remember { mutableStateOf("") }
    var noHp by remember { mutableStateOf("") }

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
                title = { Text("Master Data Member Pelanggan", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
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
                        Text("Daftar Member Baru", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = memberNo, onValueChange = { memberNo = it }, label = { Text("No. Kartu") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = nama, onValueChange = { nama = it }, label = { Text("Nama Member") }, modifier = Modifier.weight(1.5f))
                        }
                        OutlinedTextField(value = noHp, onValueChange = { noHp = it }, label = { Text("No. WhatsApp / HP") }, modifier = Modifier.fillMaxWidth())

                        Button(
                            onClick = {
                                if (nama.isNotBlank() && memberNo.isNotBlank()) {
                                    viewModel.saveMember(MemberEntity(memberNo.trim(), nama.trim(), noHp.trim()))
                                    nama = ""
                                    noHp = ""
                                    memberNo = "MBR-" + System.currentTimeMillis().toString().takeLast(4)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMPAN MEMBER BARU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(members) { m ->
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
                            Text(m.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Kartu: ${m.memberNo} • HP: ${m.noHp}", fontSize = 11.sp, color = GrayText)
                            Text("Poin: ${m.poin} Poin • Belanja: ${formatRupiah(m.totalBelanja)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailGreen)
                        }
                        IconButton(onClick = { viewModel.deleteMember(m.memberNo) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = RetailRedPrimary)
                        }
                    }
                }
            }
        }
    }
}
