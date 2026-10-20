package com.example.ui.screens.backup

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.text.SimpleDateFormat
import java.util.*

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
    var isSuccessStatus by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // Dialog states
    var showConfirmResetDialog by remember { mutableStateOf(false) }
    var fileToRestore by remember { mutableStateOf<File?>(null) }
    var uriToRestore by remember { mutableStateOf<Uri?>(null) }
    var uriFileNameToRestore by remember { mutableStateOf("") }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    // Backup files in Android storage directories
    var backupFilesList by remember { mutableStateOf(DocExportManager.getAllFilesInSubfolder(context, "Backup")) }

    fun refreshFileList() {
        backupFilesList = DocExportManager.getAllFilesInSubfolder(context, "Backup")
    }

    // Storage Access Framework Document Picker: Allows selecting backup .json from ANY folder in Android
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            uriToRestore = uri
            uriFileNameToRestore = fileName
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BACKUP & RESTORE DATABASE", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_backup")) {
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
                            Text("PENYIMPANAN LOCAL ANDROID", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            Text(
                                "Berkas backup tersimpan di folder 'DodolanPro/Backup' di memori Android (Download/Dokumen). Anda dapat membuat cadangan atau memulihkan (restore) langsung dari folder mana saja di Android menggunakan Pengelola Berkas.",
                                fontSize = 11.sp,
                                color = DarkText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 1: CADANGKAN DATABASE KE MEMORI ANDROID
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = RetailGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("1. CADANGKAN DATA KE STORAGE ANDROID", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Cadangkan seluruh produk, member, kasir, transaksi, dan pengaturan toko ke format JSON offline yang aman.",
                            fontSize = 11.sp,
                            color = GrayText
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.exportBackupToFile { success, file, json ->
                                    isProcessing = false
                                    savedBackupFile = file
                                    backupJsonOutput = json
                                    refreshFileList()
                                    isSuccessStatus = success
                                    statusMessage = if (success && file != null) {
                                        "Backup berhasil dibuat & tersimpan di folder: ${file.name}"
                                    } else {
                                        "Backup selesai di-generate!"
                                    }
                                }
                            },
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(containerColor = RetailGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_export_backup")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("BUAT BACKUP SEKARANG (SIMPAN KE DodolanPro/Backup)", fontWeight = FontWeight.Bold)
                        }

                        if (savedBackupFile != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("File Terbuat: ${savedBackupFile?.name}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
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
                                        isSuccessStatus = true
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

                // SECTION 2: RESTORE DARI FOLDER & STORAGE ANDROID
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RestorePage, contentDescription = null, tint = RetailNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("2. RESTORE / PULIHKAN DARI FOLDER ANDROID", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SUB-SECTION A: BUKA FILE PICKER DARI STORAGE ANDROID
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetailNavy.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pilih File Bebas Dari Folder Android", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Buka File Manager Android untuk memilih berkas backup (.json) yang tersimpan di folder mana saja (Download, Dokumen, WhatsApp, SD Card, dll).",
                                    fontSize = 11.sp,
                                    color = DarkText
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        // Open system document picker for any file / JSON
                                        openDocumentLauncher.launch(arrayOf("*/*", "application/json", "text/plain"))
                                    },
                                    enabled = !isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailAmber),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("btn_pick_file_from_android")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("PILIH FILE DARI FOLDER ANDROID", fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SUB-SECTION B: DAFTAR BERKAS DI FOLDER DodolanPro/Backup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Berkas di Folder DodolanPro/Backup:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                                Text(
                                    "Ditemukan ${backupFilesList.size} file backup di penyimpanan",
                                    fontSize = 10.sp,
                                    color = GrayText
                                )
                            }

                            IconButton(
                                onClick = { refreshFileList() },
                                modifier = Modifier.testTag("btn_refresh_backup_files")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Segarkan Berkas", tint = RetailNavy)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (backupFilesList.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, tint = GrayText, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Belum ada file backup di folder 'DodolanPro/Backup'.", fontSize = 11.sp, color = GrayText)
                                    Text("Gunakan tombol 'PILIH FILE DARI FOLDER ANDROID' di atas untuk merestore dari folder lain.", fontSize = 10.sp, color = GrayText)
                                }
                            }
                        } else {
                            backupFilesList.forEach { f ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(f.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Text(formatFileSize(f.length()), fontSize = 10.sp, color = GrayText)
                                                        Text("•", fontSize = 10.sp, color = GrayText)
                                                        Text(formatFileDate(f.lastModified()), fontSize = 10.sp, color = GrayText)
                                                    }
                                                }
                                            }

                                            // Action Buttons for this file
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        DocExportManager.shareDocument(context, f, "DodolanPro Backup File")
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = "Bagikan", tint = RetailNavy, modifier = Modifier.size(16.dp))
                                                }

                                                IconButton(
                                                    onClick = { fileToDelete = f },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = RetailRedPrimary, modifier = Modifier.size(16.dp))
                                                }

                                                Button(
                                                    onClick = { fileToRestore = f },
                                                    enabled = !isProcessing,
                                                    colors = ButtonDefaults.buttonColors(containerColor = RetailAmber),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pulihkan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            "Lokasi: ${f.parentFile?.absolutePath ?: "Internal Storage"}",
                                            fontSize = 9.sp,
                                            color = GrayText
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = PosCardBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        // SUB-SECTION C: TEMPEL TEKS JSON MANUAL
                        Text("Atau tempelkan isi teks JSON backup:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = restoreJsonInput,
                            onValueChange = { restoreJsonInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(95.dp)
                                .testTag("input_restore_json"),
                            placeholder = { Text("Tempel teks JSON di sini...") },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.restoreBackup(restoreJsonInput) { ok ->
                                    isProcessing = false
                                    isSuccessStatus = ok
                                    statusMessage = if (ok) "Database berhasil dipulihkan dari teks JSON!" else "Format backup JSON tidak valid!"
                                    if (ok) restoreJsonInput = ""
                                }
                            },
                            enabled = !isProcessing && restoreJsonInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
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
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Text("RESET KE DATA CONTOH DEFAULT", color = Color.Black)
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccessStatus) RetailGreenLight else Color(0xFFFFEBEE)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSuccessStatus) RetailGreen else RetailRedPrimary
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (isSuccessStatus) RetailGreen else RetailRedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                statusMessage,
                                color = if (isSuccessStatus) RetailGreen else RetailRedPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: KONFIRMASI RESTORE DARI FILE PICKER ANDROID
    if (uriToRestore != null) {
        AlertDialog(
            onDismissRequest = {
                uriToRestore = null
                uriFileNameToRestore = ""
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RetailAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KONFIRMASI RESTORE", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Apakah Anda yakin ingin memulihkan database dari berkas terpilih?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Berkas: $uriFileNameToRestore", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                            Text("Data produk, member, kasir, transaksi akan diperbarui sesuai backup ini.", fontSize = 11.sp, color = GrayText)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentUri = uriToRestore
                        uriToRestore = null
                        uriFileNameToRestore = ""
                        if (currentUri != null) {
                            isProcessing = true
                            viewModel.restoreBackupFromUri(currentUri) { success, msg ->
                                isProcessing = false
                                isSuccessStatus = success
                                statusMessage = msg
                                refreshFileList()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailAmber, contentColor = Color.Black)
                ) {
                    Text("YA, RESTORE SEKARANG", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    uriToRestore = null
                    uriFileNameToRestore = ""
                }) {
                    Text("BATAL")
                }
            }
        )
    }

    // DIALOG 2: KONFIRMASI RESTORE DARI FILE INTERNAL / DODOLAN FOLDER
    if (fileToRestore != null) {
        val targetFile = fileToRestore!!
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RetailAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KONFIRMASI RESTORE", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Pulihkan database menggunakan berkas berikut?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PosSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(targetFile.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                            Text("Ukuran: ${formatFileSize(targetFile.length())}", fontSize = 10.sp, color = GrayText)
                            Text("Tanggal: ${formatFileDate(targetFile.lastModified())}", fontSize = 10.sp, color = GrayText)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        fileToRestore = null
                        isProcessing = true
                        val content = try { targetFile.readText() } catch (_: Exception) { "" }
                        if (content.isNotBlank()) {
                            viewModel.restoreBackup(content) { ok ->
                                isProcessing = false
                                isSuccessStatus = ok
                                statusMessage = if (ok) "Database berhasil dipulihkan dari ${targetFile.name}!" else "File backup corrupt / format tidak valid!"
                            }
                        } else {
                            isProcessing = false
                            isSuccessStatus = false
                            statusMessage = "Gagal membaca isi berkas ${targetFile.name}."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailAmber, contentColor = Color.Black)
                ) {
                    Text("YA, PULIHKAN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToRestore = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    // DIALOG 3: KONFIRMASI HAPUS BERKAS BACKUP
    if (fileToDelete != null) {
        val targetDelete = fileToDelete!!
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("HAPUS BERKAS BACKUP", fontWeight = FontWeight.Bold) },
            text = { Text("Hapus berkas '${targetDelete.name}' secara permanen dari perangkat?") },
            confirmButton = {
                Button(
                    onClick = {
                        val ok = try { targetDelete.delete() } catch (_: Exception) { false }
                        fileToDelete = null
                        refreshFileList()
                        isSuccessStatus = ok
                        statusMessage = if (ok) "Berkas '${targetDelete.name}' berhasil dihapus." else "Gagal menghapus berkas."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Text("HAPUS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { fileToDelete = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    // DIALOG 4: KONFIRMASI RESET DATA AWAL
    if (showConfirmResetDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmResetDialog = false },
            title = { Text("KONFIRMASI RESET DATA", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin mengatur ulang database ke data contoh default?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDemoData {
                            isSuccessStatus = true
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

// Helpers
private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "backup.json"
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val str = cursor.getString(nameIndex)
                    if (!str.isNullOrBlank()) name = str
                }
            }
        } catch (_: Exception) {}
    } else if (uri.scheme == "file") {
        uri.path?.let { p ->
            val f = File(p)
            if (f.name.isNotBlank()) name = f.name
        }
    }
    return name
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private fun formatFileDate(millis: Long): String {
    if (millis <= 0) return "-"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))
}
