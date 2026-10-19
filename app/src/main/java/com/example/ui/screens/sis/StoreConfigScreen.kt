package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ui.screens.pos.formatRupiah
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

    // State Pengaturan Pajak
    var isTaxActive by remember(storeConfig) { mutableStateOf(storeConfig.isTaxActive || storeConfig.ppnPercentage > 0) }
    var taxName by remember(storeConfig) { mutableStateOf(storeConfig.taxName.ifBlank { "PPN" }) }
    var ppnPercentage by remember(storeConfig) {
        mutableStateOf(
            if (storeConfig.ppnPercentage > 0) {
                if (storeConfig.ppnPercentage % 1.0 == 0.0) storeConfig.ppnPercentage.toInt().toString() else storeConfig.ppnPercentage.toString()
            } else "11"
        )
    }
    var isTaxIncluded by remember(storeConfig) { mutableStateOf(storeConfig.isTaxIncluded) }
    var receiptShowPpn by remember(storeConfig) { mutableStateOf(storeConfig.receiptShowPpn) }

    var feedbackMsg by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PENGATURAN TOKO & PAJAK", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. IDENTITAS TOKO
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = RetailNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("IDENTITAS TOKO", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                        }

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
                            label = { Text("NPWP Toko (Nomor Pokok Wajib Pajak)") },
                            singleLine = true
                        )
                    }
                }
            }

            // 2. PENGATURAN PAJAK TOKO (PPN / PB1 / PAJAK PENJUALAN)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTaxActive) RetailNavy.copy(alpha = 0.12f) else Color(0xFFF1F5F9),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = if (isTaxActive) RetailNavy else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "PENGATURAN PAJAK TOKO",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = RetailNavy
                                )
                                Text(
                                    "Atur tarif pajak, nama pajak, dan metode inklusif/eksklusif",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Status Pemungutan Pajak
                        Surface(
                            color = if (isTaxActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isTaxActive) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Status Pemungutan Pajak",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isTaxActive) RetailNavy else Color(0xFF475569)
                                    )
                                    Text(
                                        if (isTaxActive) "Pajak aktif ($taxName $ppnPercentage%) diterapkan pada transaksi"
                                        else "Non-aktif (Toko bebas pajak / tidak memungut PPN)",
                                        fontSize = 11.sp,
                                        color = if (isTaxActive) Color(0xFF2563EB) else Color.Gray
                                    )
                                }
                                Switch(
                                    checked = isTaxActive,
                                    onCheckedChange = { isTaxActive = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = RetailNavy
                                    )
                                )
                            }
                        }

                        if (isTaxActive) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // Nama / Jenis Pajak
                            Text("Nama / Jenis Pajak", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = taxName,
                                onValueChange = { taxName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Contoh: PPN, PB1, Pajak Restoran") },
                                singleLine = true
                            )

                            // Quick Suggestions Nama Pajak
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("PPN", "PB1 (Resto)", "Pajak Daerah", "Pajak Toko").forEach { preset ->
                                    val cleanName = preset.substringBefore(" ")
                                    val isSel = taxName.equals(cleanName, ignoreCase = true)
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { taxName = cleanName },
                                        label = { Text(preset, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Tarif Persentase Pajak
                            Text("Tarif Pajak (%)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = ppnPercentage,
                                onValueChange = { ppnPercentage = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Persentase Pajak") },
                                trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            // Quick Presets Tarif Pajak
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "11" to "11% (PPN Ritel)",
                                    "12" to "12% (PPN 2025)",
                                    "10" to "10% (PB1 Resto)",
                                    "5" to "5% (Khusus)",
                                    "0" to "0% (Bebas)"
                                ).forEach { (rateVal, label) ->
                                    val isSel = ppnPercentage == rateVal
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { ppnPercentage = rateVal },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Metode Pengenaan Pajak (Inklusif vs Eksklusif)
                            Text("Metode Pengenaan Pajak", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RetailNavy)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Pilihan 1: Inklusif
                            Surface(
                                color = if (isTaxIncluded) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isTaxIncluded) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isTaxIncluded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isTaxIncluded,
                                        onClick = { isTaxIncluded = true },
                                        colors = RadioButtonDefaults.colors(selectedColor = RetailGreen)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Pajak Inklusif (Harga Sudah Termasuk Pajak)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isTaxIncluded) Color(0xFF166534) else Color(0xFF334155)
                                        )
                                        Text(
                                            "Harga di rak/katalog sudah mencakup pajak. Nilai pajak dirinci otomatis pada struk (Standar Supermarket/Ritel).",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Pilihan 2: Eksklusif
                            Surface(
                                color = if (!isTaxIncluded) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (!isTaxIncluded) Color(0xFFFDE68A) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isTaxIncluded = false }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = !isTaxIncluded,
                                        onClick = { isTaxIncluded = false },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD97706))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            "Pajak Eksklusif (Pajak Ditambahkan ke Tagihan)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (!isTaxIncluded) Color(0xFF92400E) else Color(0xFF334155)
                                        )
                                        Text(
                                            "Pajak dihitung dan ditambahkan di atas subtotal saat kasir memproses total tagihan (Standar Resto/Kafe/Jasa).",
                                            fontSize = 10.sp,
                                            color = Color.Gray,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Cetak Rincian di Struk Kasir
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cetak Rincian Pajak di Struk", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RetailNavy)
                                    Text("Mencetak baris keterangan pajak pada kertas struk kasir", fontSize = 10.sp, color = Color.Gray)
                                }
                                Switch(
                                    checked = receiptShowPpn,
                                    onCheckedChange = { receiptShowPpn = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = RetailNavy
                                    )
                                )
                            }

                            // Simulasi Perhitungan
                            val pctVal = ppnPercentage.toDoubleOrNull() ?: 0.0
                            if (pctVal > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "💡 SIMULASI TRANSAKSI Rp 100.000:",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = RetailNavy
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        if (isTaxIncluded) {
                                            val taxAmt = 100000.0 * (pctVal / 100.0) / (1.0 + (pctVal / 100.0))
                                            val dpp = 100000.0 - taxAmt
                                            Text("• Total Bayar Konsumen : Rp 100.000", fontSize = 11.sp, color = Color(0xFF1E293B))
                                            Text("• DPP (Dasar Pajak)    : Rp ${formatRupiah(dpp)}", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text("• $taxName $pctVal% (Termasuk) : Rp ${formatRupiah(taxAmt)}", fontSize = 11.sp, color = RetailGreen, fontWeight = FontWeight.Bold)
                                        } else {
                                            val taxAmt = 100000.0 * (pctVal / 100.0)
                                            val grand = 100000.0 + taxAmt
                                            Text("• Subtotal Belanja     : Rp 100.000", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Text("• $taxName $pctVal% (Ditambahkan) : +Rp ${formatRupiah(taxAmt)}", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                            Text("• Total Bayar Konsumen : Rp ${formatRupiah(grand)}", fontSize = 11.sp, color = RetailNavy, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. PENGATURAN STRUK KASIR
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = RetailNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PENGATURAN STRUK KASIR", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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
                    }
                }
            }

            // 4. NAVIGASI VOUCHER & PRINTER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Color(0xFFF3E8FF),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8B4FE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF7E22CE))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PROGRAM VOUCHER & POIN TOKO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF581C87))
                                Text("Pengaturan nilai tukar poin member, syarat belanja, serta voucher struk kasir dapat dikelola pada menu 'Voucher & Poin'.", fontSize = 11.sp, color = Color(0xFF6B21A8))
                            }
                        }
                    }

                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.navigateTo(com.example.ui.viewmodel.PosNavDestination.PENGATURAN_PRINTER)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PENGATURAN MODEL STRUK & PRINTER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                                Text("Pilih model struk (Ritel, Minimalis, Cafe, Elegan), ukuran kertas 58mm/80mm, serta koneksi Bluetooth printer thermal.", fontSize = 11.sp, color = Color(0xFF2E7D32))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF2E7D32))
                        }
                    }

                    if (feedbackMsg.isNotEmpty()) {
                        Text(feedbackMsg, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val pPrice = plastikPrice.toDoubleOrNull() ?: 200.0
                            val pRate = ppnPercentage.toDoubleOrNull() ?: 0.0
                            val updated = storeConfig.copy(
                                storeCode = storeCode,
                                storeName = storeName,
                                alamat = alamat,
                                telp = telp,
                                npwp = npwp,
                                headerMsg = headerMsg,
                                footerMsg = footerMsg,
                                plastikPrice = pPrice,
                                isTaxActive = isTaxActive,
                                taxName = taxName.ifBlank { "PPN" },
                                ppnPercentage = if (isTaxActive) pRate else 0.0,
                                isTaxIncluded = isTaxIncluded,
                                receiptShowPpn = receiptShowPpn
                            )
                            viewModel.updateStoreConfig(updated)
                            feedbackMsg = "Pengaturan toko & pajak berhasil disimpan!"
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENGATURAN TOKO & PAJAK", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
