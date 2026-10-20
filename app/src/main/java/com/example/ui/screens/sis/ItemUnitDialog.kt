package com.example.ui.screens.sis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*

data class SelectedUnitConfig(
    val satuan: String,
    val konversi: Int,
    val hargaBeli: Double,
    val updateMasterProduct: Boolean
)

val COMMON_RETAIL_UNITS = listOf(
    "PCS", "DUS", "KARTON", "PACK", "RENCENG", "BAL", 
    "LUSIN", "SLOP", "BTL", "KG", "GRAM", "BOX", 
    "SAK", "KRAT", "IKAT", "ROLL", "KALENG", "BUNGKUS", "ZAK"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemUnitDialog(
    product: ProductEntity,
    currentSatuan: String,
    currentKonversi: Int,
    currentHargaBeli: Double,
    dialogContext: String = "Pembelian", // "Pembelian" atau "Retur"
    onDismiss: () -> Unit,
    onConfirm: (SelectedUnitConfig) -> Unit
) {
    var selectedSatuan by remember { mutableStateOf(currentSatuan.ifBlank { product.satuan }) }
    var konversiStr by remember { mutableStateOf(maxOf(1, currentKonversi).toString()) }
    var hargaBeliStr by remember { mutableStateOf(currentHargaBeli.toLong().toString()) }
    var updateMasterProduct by remember { mutableStateOf(false) }

    val konversiNum = konversiStr.toIntOrNull() ?: 1
    val hargaBeliNum = hargaBeliStr.toDoubleOrNull() ?: 0.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RetailNavy.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Ubah Satuan $dialogContext",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = RetailNavy
                            )
                            Text(
                                "Multi-satuan, konversi grosir & repack",
                                fontSize = 11.sp,
                                color = GrayText
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = GrayText)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Produk Ringkas
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                        Text("PLU: ${product.plu} | Barcode: ${product.barcode}", fontSize = 10.sp, color = GrayText)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "Satuan Dasar: ${product.satuan}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "Stok Toko: ${product.stok} ${product.satuan}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Opsi 1: Satuan Terdaftar Produk (Repack / Dasar)
                if (product.isRepack && product.satuanRepack.isNotBlank()) {
                    Text(
                        "PILIHAN SATUAN DARI REPACK PRODUK:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RetailNavy
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Satuan Dasar
                        val isSelBase = selectedSatuan.equals(product.satuan, ignoreCase = true) && konversiNum == 1
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelBase) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isSelBase) RetailNavy else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSatuan = product.satuan
                                    konversiStr = "1"
                                    hargaBeliStr = product.hargaBeli.toLong().toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelBase,
                                        onClick = {
                                            selectedSatuan = product.satuan
                                            konversiStr = "1"
                                            hargaBeliStr = product.hargaBeli.toLong().toString()
                                        }
                                    )
                                    Column {
                                        Text(
                                            "Satuan Dasar (${product.satuan})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Text("1 ${product.satuan} = 1 ${product.satuan} dasar", fontSize = 10.sp, color = GrayText)
                                    }
                                }
                                Text(
                                    formatRupiah(product.hargaBeli),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RetailNavy
                                )
                            }
                        }

                        // Repack 1
                        val isSelRepack1 = selectedSatuan.equals(product.satuanRepack, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelRepack1) Color(0xFFEFF6FF) else Color.White,
                            border = BorderStroke(1.dp, if (isSelRepack1) RetailNavy else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedSatuan = product.satuanRepack
                                    konversiStr = product.isiRepack.toString()
                                    val suggestedPrice = if (product.hargaRepack > 0) {
                                        product.hargaRepack
                                    } else {
                                        product.hargaBeli * product.isiRepack
                                    }
                                    hargaBeliStr = suggestedPrice.toLong().toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelRepack1,
                                        onClick = {
                                            selectedSatuan = product.satuanRepack
                                            konversiStr = product.isiRepack.toString()
                                            val suggestedPrice = if (product.hargaRepack > 0) {
                                                product.hargaRepack
                                            } else {
                                                product.hargaBeli * product.isiRepack
                                            }
                                            hargaBeliStr = suggestedPrice.toLong().toString()
                                        }
                                    )
                                    Column {
                                        Text(
                                            "Repack 1 (${product.satuanRepack})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            "1 ${product.satuanRepack} = ${product.isiRepack} ${product.satuan} dasar",
                                            fontSize = 10.sp,
                                            color = GrayText
                                        )
                                    }
                                }
                                Text(
                                    formatRupiah(if (product.hargaRepack > 0) product.hargaRepack else product.hargaBeli * product.isiRepack),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = RetailGreen
                                )
                            }
                        }

                        // Repack 2 (jika ada)
                        if (product.satuanRepack2.isNotBlank()) {
                            val isSelRepack2 = selectedSatuan.equals(product.satuanRepack2, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelRepack2) Color(0xFFEFF6FF) else Color.White,
                                border = BorderStroke(1.dp, if (isSelRepack2) RetailNavy else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSatuan = product.satuanRepack2
                                        konversiStr = product.isiRepack2.toString()
                                        val suggestedPrice = if (product.hargaRepack2 > 0) {
                                            product.hargaRepack2
                                        } else {
                                            product.hargaBeli * product.isiRepack2
                                        }
                                        hargaBeliStr = suggestedPrice.toLong().toString()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelRepack2,
                                            onClick = {
                                                selectedSatuan = product.satuanRepack2
                                                konversiStr = product.isiRepack2.toString()
                                                val suggestedPrice = if (product.hargaRepack2 > 0) {
                                                    product.hargaRepack2
                                                } else {
                                                    product.hargaBeli * product.isiRepack2
                                                }
                                                hargaBeliStr = suggestedPrice.toLong().toString()
                                            }
                                        )
                                        Column {
                                            Text(
                                                "Repack 2 (${product.satuanRepack2})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                "1 ${product.satuanRepack2} = ${product.isiRepack2} ${product.satuan} dasar",
                                                fontSize = 10.sp,
                                                color = GrayText
                                            )
                                        }
                                    }
                                    Text(
                                        formatRupiah(if (product.hargaRepack2 > 0) product.hargaRepack2 else product.hargaBeli * product.isiRepack2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = RetailGreen
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Opsi 2: Pilihan Cepat Satuan Retail Populer
                Text(
                    "PILIHAN CEPAT SATUAN RETAIL:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetailNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    COMMON_RETAIL_UNITS.forEach { unit ->
                        val isSel = selectedSatuan.equals(unit, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) RetailNavy else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) RetailNavy else Color(0xFFCBD5E1)),
                            modifier = Modifier.clickable {
                                selectedSatuan = unit
                                if (unit.equals(product.satuan, ignoreCase = true)) {
                                    konversiStr = "1"
                                }
                            }
                        ) {
                            Text(
                                text = unit,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF1E293B),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Satuan & Konversi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nama Satuan
                    Column(modifier = Modifier.weight(1.2f)) {
                        Text("Nama Satuan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                        OutlinedTextField(
                            value = selectedSatuan,
                            onValueChange = { selectedSatuan = it.uppercase() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("Contoh: DUS") }
                        )
                    }

                    // Isi Konversi ke Satuan Dasar
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Isi (Konversi)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                        OutlinedTextField(
                            value = konversiStr,
                            onValueChange = { konversiStr = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("1") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Catatan Konversi
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ 1 $selectedSatuan = $konversiNum ${product.satuan} dasar. Saat $dialogContext, stok fisik toko akan disesuaikan otomatis sebanyak ${konversiNum}x per unit.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF166534),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Harga Beli per Satuan
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Harga Beli / Retur per $selectedSatuan (Rp)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RetailNavy
                    )
                    OutlinedTextField(
                        value = hargaBeliStr,
                        onValueChange = { hargaBeliStr = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0") }
                    )
                    if (konversiNum > 1 && hargaBeliNum > 0) {
                        Text(
                            "Setara ${formatRupiah(hargaBeliNum / konversiNum)} / ${product.satuan} dasar",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Checkbox: Jadikan Satuan Utama di Master Produk
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { updateMasterProduct = !updateMasterProduct }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = updateMasterProduct,
                            onCheckedChange = { updateMasterProduct = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                "Perbarui Satuan Utama di Database Barang",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                "Ubah satuan dasar produk '${product.nama}' di master katalog menjadi '$selectedSatuan'",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tombol Aksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }
                    Button(
                        onClick = {
                            val finalSatuan = selectedSatuan.trim().ifBlank { product.satuan }
                            val finalKonversi = maxOf(1, konversiNum)
                            val finalHarga = if (hargaBeliNum > 0) hargaBeliNum else product.hargaBeli
                            onConfirm(
                                SelectedUnitConfig(
                                    satuan = finalSatuan,
                                    konversi = finalKonversi,
                                    hargaBeli = finalHarga,
                                    updateMasterProduct = updateMasterProduct
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Terapkan Satuan")
                    }
                }
            }
        }
    }
}
