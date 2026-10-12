package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherPointsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    var savedSuccess by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voucher & Loyalitas Poin", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6A1B9A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PosSurface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF6A1B9A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sistem Poin Member", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                    }

                    Text(
                        "Konfigurasi perhitungan perolehan dan penukaran poin belanja member aktif.",
                        fontSize = 12.sp,
                        color = GrayText
                    )

                    OutlinedTextField(
                        value = "100",
                        onValueChange = {},
                        label = { Text("Nilai 1 Poin (Rupiah)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        supportingText = { Text("1 Poin = Potongan Rp 100") }
                    )

                    OutlinedTextField(
                        value = "10.000",
                        onValueChange = {},
                        label = { Text("Minimal Belanja Dapatkan 1 Poin (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        supportingText = { Text("Setiap kelipatan Rp 10.000 mendapatkan 1 poin") }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFF6A1B9A))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voucher Promo Aktif", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                    }

                    val sampleVouchers = listOf(
                        "HEMAT10" to "Potongan Rp 10.000 min belanja Rp 100.000",
                        "BERKAH25" to "Potongan Rp 25.000 min belanja Rp 250.000",
                        "PROMO5" to "Potongan Rp 5.000 tanpa minimal belanja"
                    )

                    sampleVouchers.forEach { (code, desc) ->
                        Surface(
                            color = PosSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(code, fontWeight = FontWeight.Bold, color = RetailNavy)
                                    Text(desc, fontSize = 11.sp, color = GrayText)
                                }
                                Surface(
                                    color = Color(0xFFE1BEE7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "AKTIF",
                                        color = Color(0xFF4A148C),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (savedSuccess) {
                Surface(
                    color = RetailGreenLight,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Aturan voucher & poin aktif disimpan!", color = RetailGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = { savedSuccess = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SIMPAN PENGATURAN", fontWeight = FontWeight.Bold)
            }
        }
    }
}
