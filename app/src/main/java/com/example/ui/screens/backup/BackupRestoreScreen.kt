package com.example.ui.screens.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var backupJsonOutput by remember { mutableStateOf("") }
    var savedBackupFile by remember { mutableStateOf<File?>(null) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var showConfirmResetDialog by remember { mutableStateOf(false) }

    // List files in DodolanPro/Backup
    val backupDir = remember { DocExportManager.getDodolanFolder(context, "Backup") }
    var backupFilesList by remember { mutableStateOf(backupDir.listFiles()?.toList() ?: emptyList()) }

    fun refreshFileList() {
        backupFilesList = backupDir.listFiles()?.toList() ?: emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BACKUP & RESTORE DATABASE", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                .padding(12.dp)
        ) {
            item {
                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RetailBlueLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailNavy.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("PENYIMPANAN LOCAL: DodolanPro/Backup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Text(
                                "Semua berkas backup akan otomatis tersimpan di memori perangkat dalam folder DodolanPro/Backup. Anda dapat mencadangkan dan memulihkan data kapan saja tanpa internet.",
                                fontSize = 11.sp,
                                color = DarkText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: BACKUP DATABASE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = RetailGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1. CADANGKAN DATABASE KE MEMORI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.exportBackupToFile { success, file, json ->
                                    isProcessing = false
                                    savedBackupFile = file
                                    backupJsonOutput = json
                                    refreshFileList()
                                    statusMessage = if (success && file != null) "Backup tersimpan di ${file.absolutePath}" else "Backup berhasil di-generate!"
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                            modifier = Modifier.fillMaxWidth().testTag("btn_export_backup")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("BACKUP SEKARANG (SIMPAN KE DodolanPro/Backup)")
                        }

                        if (savedBackupFile != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("File: ${savedBackupFile?.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                    Text("Lokasi: ${savedBackupFile?.absolutePath}", fontSize = 10.sp, color = GrayText)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("DodolanPro Backup", backupJsonOutput)
                                        clipboard.setPrimaryClip(clip)
                                        statusMessage = "Teks JSON backup disalin ke clipboard!"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SALIN TEKS")
                                }

                                Button(
                                    onClick = {
                                        savedBackupFile?.let { f ->
                                            DocExportManager.shareDocument(context, f, "DodolanPro Backup Database")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("BAGIKAN FILE")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 2: DAFTAR BERKAS DI DodolanPro/Backup & RESTORE DARI DEVICE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = RetailNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2. RESTORE DARI PENYIMPANAN DEVICE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Daftar backup yang ditemukan di folder DodolanPro/Backup:", fontSize = 11.sp, color = GrayText)

                        Spacer(modifier = Modifier.height(8.dp))

                        if (backupFilesList.isEmpty()) {
                            Text("Belum ada file backup di folder internal.", fontSize = 11.sp, color = GrayText)
                        } else {
                            backupFilesList.forEach { f ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(f.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                            Text("${f.length()} bytes", fontSize = 10.sp, color = GrayText)
                                        }

                                        Button(
                                            onClick = {
                                                isProcessing = true
                                                val content = f.readText()
                                                viewModel.restoreBackup(content) { ok ->
                                                    isProcessing = false
                                                    statusMessage = if (ok) "Database berhasil dipulihkan dari ${f.name}!" else "File backup corrupt / tidak valid!"
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailAmber),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pulihkan Ini", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = PosCardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Atau tempelkan isi teks JSON backup:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp).testTag("input_restore_json"),
                            placeholder = { Text("Tempel teks JSON di sini...") },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.restoreBackup(restoreJsonInput) { ok ->
                                    isProcessing = false
                                    statusMessage = if (ok) "Database berhasil dipulihkan dari teks JSON!" else "Format backup JSON tidak valid!"
                                    if (ok) restoreJsonInput = ""
                                }
                            },
                            enabled = !isProcessing && restoreJsonInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailAmber)
                        ) {
                            Text("PULIHKAN DARI TEKS JSON")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 3: RESET TO DEMO DATA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailRedPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = RetailRedPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("3. RESET KE DATA AWAL RETAIL DEMO", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Mengembalikan data ke katalog resmi produk minimarket Indonesia (Indomie, Aqua, Dji Sam Soe, Walls Cornetto, Bimoli, dsb.) beserta akun kasir dan PO contoh.",
                            fontSize = 11.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showConfirmResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("RESET KE DATA CONTOH DEFAULT", color = Color.Black)
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RetailGreenLight)
                    ) {
                        Text(
                            statusMessage,
                            modifier = Modifier.padding(12.dp),
                            color = RetailGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("KONFIRMASI RESET DATA", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mengatur ulang database ke data contoh default?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDemoData {
                            statusMessage = "Database berhasil direset ke katalog default!"
                            showConfirmResetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Text("YA, RESET DATA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmResetDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}
