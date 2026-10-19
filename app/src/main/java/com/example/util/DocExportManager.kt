package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object DocExportManager {
    const val ROOT_DIR_NAME = "DodolanPro"
    private const val TAG = "DocExportManager"

    val STANDARD_SUBFOLDERS = listOf(
        "Clerek", "Penjualan", "Struk", "Pembelian", "Stok", "Laporan", "Backup", "Retur", "EOD", "LabelRak"
    )

    fun ensureAllDodolanDirectories(context: Context) {
        for (sub in STANDARD_SUBFOLDERS) {
            try {
                File(Environment.getExternalStorageDirectory(), "$ROOT_DIR_NAME/$sub").mkdirs()
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$ROOT_DIR_NAME/$sub").mkdirs()
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$ROOT_DIR_NAME/$sub").mkdirs()
                File(context.getExternalFilesDir(null), "$ROOT_DIR_NAME/$sub").mkdirs()
                File(context.filesDir, "$ROOT_DIR_NAME/$sub").mkdirs()
            } catch (e: Exception) {
                Log.e(TAG, "Error ensuring dir: $sub", e)
            }
        }
    }

    fun getDodolanFolder(context: Context, subfolder: String): File {
        val publicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$ROOT_DIR_NAME/$subfolder")
        if (publicDir.exists() || publicDir.mkdirs()) {
            return publicDir
        }
        val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$ROOT_DIR_NAME/$subfolder")
        if (docsDir.exists() || docsDir.mkdirs()) {
            return docsDir
        }
        val extAppDir = File(context.getExternalFilesDir(null), "$ROOT_DIR_NAME/$subfolder")
        if (extAppDir.exists() || extAppDir.mkdirs()) {
            return extAppDir
        }
        val intDir = File(context.filesDir, "$ROOT_DIR_NAME/$subfolder")
        intDir.mkdirs()
        return intDir
    }

    fun saveDocument(
        context: Context,
        subfolder: String,
        prefix: String,
        extension: String,
        content: String
    ): Pair<Boolean, File> {
        ensureAllDodolanDirectories(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanExt = extension.removePrefix(".")
        val fileName = "${prefix}_${timestamp}.$cleanExt"

        val targetDir = getDodolanFolder(context, subfolder)
        val targetFile = File(targetDir, fileName)

        var success = false
        try {
            targetFile.writeText(content, Charsets.UTF_8)
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file directly", e)
            try {
                val fallbackFile = File(context.filesDir, fileName)
                fallbackFile.writeText(content, Charsets.UTF_8)
                return Pair(true, fallbackFile)
            } catch (err: Exception) {
                Log.e(TAG, "Fallback save failed", err)
            }
        }

        // On Android 10+ also write to MediaStore if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (cleanExt == "txt" || cleanExt == "json" || cleanExt == "csv")) {
            try {
                val mime = when (cleanExt) {
                    "json" -> "application/json"
                    "csv" -> "text/csv"
                    else -> "text/plain"
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_DIR_NAME/$subfolder")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore insert error", e)
            }
        }

        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
        return Pair(success, targetFile)
    }

    fun saveDocument(
        context: Context,
        subfolder: String,
        prefix: String,
        extension: String,
        bytes: ByteArray
    ): Pair<Boolean, File> {
        ensureAllDodolanDirectories(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val cleanExt = extension.removePrefix(".")
        val fileName = "${prefix}_${timestamp}.$cleanExt"

        val targetDir = getDodolanFolder(context, subfolder)
        val targetFile = File(targetDir, fileName)

        var success = false
        try {
            targetFile.writeBytes(bytes)
            success = true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving binary file directly", e)
            try {
                val fallbackFile = File(context.filesDir, fileName)
                fallbackFile.writeBytes(bytes)
                return Pair(true, fallbackFile)
            } catch (err: Exception) {
                Log.e(TAG, "Fallback save failed", err)
            }
        }

        // On Android 10+ also write to MediaStore if possible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cleanExt == "pdf") {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_DIR_NAME/$subfolder")
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(bytes)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore insert error", e)
            }
        }

        MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), null, null)
        return Pair(success, targetFile)
    }

    fun getAllFilesInSubfolder(context: Context, subfolder: String): List<File> {
        val results = mutableListOf<File>()
        val dirs = listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "$ROOT_DIR_NAME/$subfolder"),
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "$ROOT_DIR_NAME/$subfolder"),
            File(context.getExternalFilesDir(null), "$ROOT_DIR_NAME/$subfolder"),
            File(context.filesDir, "$ROOT_DIR_NAME/$subfolder")
        )
        for (dir in dirs) {
            if (dir.exists()) {
                dir.listFiles()?.filter { it.isFile }?.let { results.addAll(it) }
            }
        }
        return results.distinctBy { it.name }.sortedByDescending { it.lastModified() }
    }

    fun openDocument(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when (file.extension.lowercase(Locale.ROOT)) {
                "pdf" -> "application/pdf"
                "json" -> "application/json"
                "csv" -> "text/csv"
                else -> context.contentResolver.getType(uri) ?: "text/plain"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Dokumen"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka dokumen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareDocument(context: Context, file: File, title: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when (file.extension.lowercase(Locale.ROOT)) {
                "pdf" -> "application/pdf"
                "json" -> "application/json"
                "csv" -> "text/csv"
                else -> context.contentResolver.getType(uri) ?: "text/plain"
            }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, "Dokumen dari Dodolan Pro: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan dokumen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdfDocument(context: Context, file: File, jobName: String = "Cetak Dokumen DodolanPro") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
            if (printManager == null) {
                openDocument(context, file)
                return
            }
            val printAdapter = object : android.print.PrintDocumentAdapter() {
                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    val pdi = android.print.PrintDocumentInfo.Builder(file.name)
                        .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build()
                    callback?.onLayoutFinished(pdi, true)
                }

                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        java.io.FileInputStream(file).use { input ->
                            java.io.FileOutputStream(destination?.fileDescriptor).use { output ->
                                input.copyTo(output)
                            }
                        }
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }
            }
            val printAttributes = android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS)
                .build()
            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching print manager, falling back to openDocument", e)
            openDocument(context, file)
        }
    }

    fun getReadablePath(file: File, subfolder: String): String {
        return "/storage/emulated/0/Download/DodolanPro/$subfolder/${file.name}"
    }
}
