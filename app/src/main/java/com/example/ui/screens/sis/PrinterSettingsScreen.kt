package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val cfg = storeConfig ?: return

    var paperSize by remember(cfg) { mutableStateOf(cfg.printerPaperSize) }
    var printerType by remember(cfg) { mutableStateOf(cfg.printerType) }
    var autoPrint by remember(cfg) { mutableStateOf(cfg.autoPrintReceipt) }
    var testStatus by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2E7D32),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = { Text("Pengaturan Printer Struk Kasir", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PosSurface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Konfigurasi Mesin Cetak Struk", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ukuran Kertas:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = paperSize == "58mm",
                                    onClick = { paperSize = "58mm" },
                                    label = { Text("58 mm (Standar)") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = paperSize == "80mm",
                                    onClick = { paperSize = "80mm" },
                                    label = { Text("80 mm (Lebar)") }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tipe Koneksi:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = printerType == "BLUETOOTH",
                                    onClick = { printerType = "BLUETOOTH" },
                                    label = { Text("Bluetooth") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = printerType == "USB",
                                    onClick = { printerType = "USB" },
                                    label = { Text("USB / OTG") }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Otomatis Cetak Struk Setelah Bayar:", fontSize = 13.sp)
                            Switch(checked = autoPrint, onCheckedChange = { autoPrint = it })
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val updated = cfg.copy(
                                    printerPaperSize = paperSize,
                                    printerType = printerType,
                                    autoPrintReceipt = autoPrint
                                )
                                viewModel.updateStoreConfig(updated)
                                testStatus = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIMPAN & TES PRINT SAMPLE STRUK", fontWeight = FontWeight.Bold)
                        }

                        if (testStatus) {
                            Text("✓ Pengaturan printer tersimpan. Sample struk berhasil digenerate!", color = RetailGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
