package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreConfigScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val storeConfig by viewModel.storeConfig.collectAsState()

    var storeCode by remember(storeConfig) { mutableStateOf(storeConfig.storeCode) }
    var storeName by remember(storeConfig) { mutableStateOf(storeConfig.storeName) }
    var alamat by remember(storeConfig) { mutableStateOf(storeConfig.alamat) }
    var telp by remember(storeConfig) { mutableStateOf(storeConfig.telp) }
    var npwp by remember(storeConfig) { mutableStateOf(storeConfig.npwp) }
    var headerMsg by remember(storeConfig) { mutableStateOf(storeConfig.headerMsg) }
    var footerMsg by remember(storeConfig) { mutableStateOf(storeConfig.footerMsg) }
    var plastikPrice by remember(storeConfig) { mutableStateOf(storeConfig.plastikPrice.toLong().toString()) }
    var feedbackMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EDIT TOKO - INFORMASI TOKO", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
                .padding(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("IDENTITAS TOKO", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = storeCode,
                            onValueChange = { storeCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Kode Toko (e.g. K427)") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nama Toko") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = alamat,
                            onValueChange = { alamat = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Alamat Lengkap Toko") },
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = telp,
                            onValueChange = { telp = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nomor Telepon / WhatsApp") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = npwp,
                            onValueChange = { npwp = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("NPWP Toko") },
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("PENGATURAN STRUK KASIR", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = headerMsg,
                            onValueChange = { headerMsg = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Header Pesan Struk") },
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = footerMsg,
                            onValueChange = { footerMsg = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Footer Ucapan Struk") },
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = plastikPrice,
                            onValueChange = { plastikPrice = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Harga Kantong Plastik Standar (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )

                        if (feedbackMsg.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(feedbackMsg, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val pPrice = plastikPrice.toDoubleOrNull() ?: 200.0
                                val updated = storeConfig.copy(
                                    storeCode = storeCode,
                                    storeName = storeName,
                                    alamat = alamat,
                                    telp = telp,
                                    npwp = npwp,
                                    headerMsg = headerMsg,
                                    footerMsg = footerMsg,
                                    plastikPrice = pPrice
                                )
                                viewModel.updateStoreConfig(updated)
                                feedbackMsg = "Informasi toko berhasil disimpan!"
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMPAN INFORMASI TOKO", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
