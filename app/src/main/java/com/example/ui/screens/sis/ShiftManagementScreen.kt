package com.example.ui.screens.sis

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ShiftConfigEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val shifts by viewModel.allShiftConfigs.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val isShiftOpen by viewModel.isShiftOpen.collectAsState()
    val shiftModalAwal by viewModel.shiftModalAwal.collectAsState()
    val shiftOpenedAt by viewModel.shiftOpenedAt.collectAsState()

    var showEditorDialog by remember { mutableStateOf(false) }
    var selectedShiftToEdit by remember { mutableStateOf<ShiftConfigEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PENGATURAN SHIFT KERJA", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                onClick = {
                    selectedShiftToEdit = null
                    showEditorDialog = true
                },
                containerColor = RetailNavy,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Shift")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
                .padding(12.dp)
        ) {
            // Active Shift Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = if (isShiftOpen) RetailGreenLight else Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isShiftOpen) RetailGreen else PosCardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isShiftOpen) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (isShiftOpen) RetailGreen else RetailRedPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isShiftOpen) "SHIFT AKTIF BERJALAN" else "STATUS: SHIFT BELUM DIBUKA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isShiftOpen) RetailGreen else Color.Black
                            )
                        }
                        if (isShiftOpen) {
                            Text("Shift #$activeShift • Dibuka: $shiftOpenedAt", fontSize = 11.sp, color = Color.Black)
                            Text("Modal Awal Kasir: ${formatRupiah(shiftModalAwal)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        } else {
                            Text("Wajib buka shift di layar kasir sebelum transaksi", fontSize = 11.sp, color = GrayText)
                        }
                    }

                    if (isShiftOpen) {
                        OutlinedButton(
                            onClick = {
                                viewModel.closeShift()
                                feedbackMessage = "Shift kasir ditutup!"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                        ) {
                            Text("TUTUP SHIFT", fontSize = 11.sp, color = Color.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("DAFTAR MASTER SHIFT KASIR:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)

            Spacer(modifier = Modifier.height(6.dp))

            if (feedbackMessage.isNotEmpty()) {
                Text(feedbackMessage, color = RetailGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(shifts) { shift ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedShiftToEdit = shift
                                showEditorDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(shift.shiftName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (shift.shiftNumber == activeShift && isShiftOpen) {
                                        Surface(color = RetailGreen, shape = RoundedCornerShape(4.dp)) {
                                            Text("SEDANG AKTIF", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                                Text("Jam Kerja: ${shift.jamMulai} - ${shift.jamSelesai}", fontSize = 12.sp, color = Color.DarkGray)
                                Text("Modal Standar: ${formatRupiah(shift.modalAwalStandar)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailNavy)
                            }

                            IconButton(onClick = {
                                selectedShiftToEdit = shift
                                showEditorDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RetailNavy)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditorDialog) {
        val isEdit = selectedShiftToEdit != null
        var shiftNum by remember { mutableStateOf(selectedShiftToEdit?.shiftNumber?.toString() ?: ((shifts.maxOfOrNull { it.shiftNumber } ?: 0) + 1).toString()) }
        var name by remember { mutableStateOf(selectedShiftToEdit?.shiftName ?: "Shift $shiftNum") }
        var jamMulai by remember { mutableStateOf(selectedShiftToEdit?.jamMulai ?: "07:00") }
        var jamSelesai by remember { mutableStateOf(selectedShiftToEdit?.jamSelesai ?: "15:00") }
        var modal by remember { mutableStateOf(selectedShiftToEdit?.modalAwalStandar?.toLong()?.toString() ?: "100000") }

        AlertDialog(
            onDismissRequest = { showEditorDialog = false },
            title = { Text(if (isEdit) "EDIT SHIFT" else "TAMBAH SHIFT BARU", fontWeight = FontWeight.Bold, color = RetailNavy) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = RetailNavy,
                        unfocusedBorderColor = PosCardBorder
                    )
                    OutlinedTextField(
                        value = shiftNum,
                        onValueChange = { shiftNum = it },
                        label = { Text("Nomor Shift", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        enabled = !isEdit,
                        singleLine = true,
                        colors = tfColors
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Shift (misal: Shift 1 - Pagi)", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = tfColors
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = jamMulai,
                            onValueChange = { jamMulai = it },
                            label = { Text("Jam Mulai", color = Color.DarkGray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = tfColors
                        )
                        OutlinedTextField(
                            value = jamSelesai,
                            onValueChange = { jamSelesai = it },
                            label = { Text("Jam Selesai", color = Color.DarkGray) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = tfColors
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = modal,
                        onValueChange = { modal = it },
                        label = { Text("Modal Awal Standar (Rp)", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = tfColors
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = shiftNum.toIntOrNull() ?: 1
                        val modalVal = modal.toDoubleOrNull() ?: 100000.0
                        val entity = ShiftConfigEntity(
                            shiftNumber = num,
                            shiftName = name.trim(),
                            jamMulai = jamMulai.trim(),
                            jamSelesai = jamSelesai.trim(),
                            modalAwalStandar = modalVal,
                            isActive = true
                        )
                        if (isEdit) {
                            viewModel.updateShiftConfig(entity) {
                                feedbackMessage = "Shift ${entity.shiftName} berhasil diperbarui!"
                                showEditorDialog = false
                            }
                        } else {
                            viewModel.saveShiftConfig(entity) {
                                feedbackMessage = "Shift ${entity.shiftName} berhasil ditambahkan!"
                                showEditorDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditorDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}
