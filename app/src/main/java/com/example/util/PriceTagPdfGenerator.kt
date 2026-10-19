package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.sis.LabelCustomOptions
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PriceTagPdfGenerator {

    private fun formatNumber(amount: Double): String {
        return try {
            NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount.toLong())
        } catch (_: Exception) {
            amount.toLong().toString()
        }
    }

    private fun formatNumber(amount: Long): String {
        return try {
            NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount)
        } catch (_: Exception) {
            amount.toString()
        }
    }

    fun generatePriceTagPdf(
        context: Context,
        productsList: List<ProductEntity>,
        paperFormat: String = "A4",
        options: LabelCustomOptions = LabelCustomOptions(),
        storeName: String = "DodolanPro Mart",
        modeTitle: String = "Label Harga Rak"
    ): ByteArray {
        if (productsList.isEmpty()) return ByteArray(0)

        // Dimensi Kertas dalam points (72 pt per inch)
        // A4: 210 x 297 mm = 595 x 842 pt
        // F4/Folio: 215 x 330 mm = 612 x 936 pt
        val pageWidth = if (paperFormat.equals("F4", ignoreCase = true)) 612 else 595
        val pageHeight = if (paperFormat.equals("F4", ignoreCase = true)) 936 else 842

        val marginHorizontal = 18f
        val marginTop = 24f
        val marginBottom = 20f

        val printableWidth = pageWidth - (marginHorizontal * 2)
        val printableHeight = pageHeight - marginTop - marginBottom

        // Grid setting berdasarkan labelSize
        val (cols, rows) = when (options.labelSize) {
            "PROMO_BESAR" -> Pair(2, if (paperFormat == "F4") 6 else 5)
            "LABEL_KECIL" -> Pair(5, if (paperFormat == "F4") 14 else 12)
            else -> Pair(3, if (paperFormat == "F4") 9 else 8) // STANDAR_RAK
        }

        val cellWidth = printableWidth / cols
        val cellHeight = printableHeight / rows
        val labelsPerPage = cols * rows

        val pdfDoc = PdfDocument()
        val totalProducts = productsList.size
        val totalPages = (totalProducts + labelsPerPage - 1) / labelsPerPage

        val borderPaint = Paint().apply {
            color = android.graphics.Color.rgb(180, 190, 200)
            style = Paint.Style.STROKE
            strokeWidth = 0.75f
            isAntiAlias = true
        }

        val headerBgPaint = Paint().apply {
            color = android.graphics.Color.rgb(26, 54, 93) // Navy
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val promoBgPaint = Paint().apply {
            color = android.graphics.Color.rgb(211, 47, 47) // Red
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val regularTextPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            typeface = Typeface.DEFAULT
        }

        val todayDateStr = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val startItem = pageIndex * labelsPerPage
            val endItem = minOf(startItem + labelsPerPage, totalProducts)

            for (i in startItem until endItem) {
                val indexInPage = i - startItem
                val col = indexInPage % cols
                val row = indexInPage / cols

                val left = marginHorizontal + (col * cellWidth)
                val top = marginTop + (row * cellHeight)
                val right = left + cellWidth - 3f
                val bottom = top + cellHeight - 3f

                val product = productsList[i]
                val hasPromo = product.isPromo || product.hasDiscountOrPromo()

                // Border Box
                val cellRect = RectF(left, top, right, bottom)
                canvas.drawRoundRect(cellRect, 3f, 3f, borderPaint)

                // Top Banner (Store / Promo)
                val headerHeight = when (options.labelSize) {
                    "PROMO_BESAR" -> 22f
                    "LABEL_KECIL" -> 11f
                    else -> 15f
                }
                val headerRect = RectF(left, top, right, top + headerHeight)
                canvas.drawRect(headerRect, if (hasPromo && options.showPromoBadge) promoBgPaint else headerBgPaint)

                // Header Text
                textPaint.color = android.graphics.Color.WHITE
                textPaint.textSize = when (options.labelSize) {
                    "PROMO_BESAR" -> 11f
                    "LABEL_KECIL" -> 7f
                    else -> 8.5f
                }
                val headerTitle = if (hasPromo && options.showPromoBadge) {
                    "PROMO SPESIAL - $storeName"
                } else if (options.showStoreName) {
                    storeName
                } else {
                    "PRICE TAG"
                }
                canvas.drawText(headerTitle, left + 4f, top + headerHeight - 4f, textPaint)

                // Product Name
                textPaint.color = android.graphics.Color.BLACK
                textPaint.textSize = when (options.labelSize) {
                    "PROMO_BESAR" -> 13f
                    "LABEL_KECIL" -> 7.5f
                    else -> 9.5f
                }
                var pName = product.nama
                if (options.labelSize == "LABEL_KECIL" && pName.length > 18) {
                    pName = pName.substring(0, 16) + ".."
                } else if (pName.length > 28) {
                    pName = pName.substring(0, 26) + ".."
                }
                val nameY = top + headerHeight + (if (options.labelSize == "LABEL_KECIL") 11f else 16f)
                canvas.drawText(pName, left + 4f, nameY, textPaint)

                // PLU & Rak info
                regularTextPaint.textSize = when (options.labelSize) {
                    "PROMO_BESAR" -> 9.5f
                    "LABEL_KECIL" -> 6f
                    else -> 7.5f
                }
                regularTextPaint.color = android.graphics.Color.rgb(100, 110, 120)
                val infoY = nameY + (if (options.labelSize == "LABEL_KECIL") 8f else 12f)
                val rakText = if (options.showRak && product.rakPlanogram.isNotBlank()) " | RAK: ${product.rakPlanogram}" else ""
                val dateText = if (options.showDate) " | $todayDateStr" else ""
                canvas.drawText("PLU: ${product.plu}$rakText$dateText", left + 4f, infoY, regularTextPaint)

                // Price display
                val effectivePrice = product.getEffectiveDiscountedPrice()
                val normalPrice = product.hargaJual

                if (hasPromo && effectivePrice < normalPrice) {
                    // Coret Harga Normal
                    regularTextPaint.textSize = when (options.labelSize) {
                        "PROMO_BESAR" -> 11f
                        "LABEL_KECIL" -> 6.5f
                        else -> 8f
                    }
                    regularTextPaint.isStrikeThruText = true
                    regularTextPaint.color = android.graphics.Color.rgb(130, 140, 150)
                    val strikeY = infoY + (if (options.labelSize == "LABEL_KECIL") 10f else 14f)
                    canvas.drawText("Rp ${formatNumber(normalPrice)}", left + 4f, strikeY, regularTextPaint)
                    regularTextPaint.isStrikeThruText = false

                    // Harga Promo Besar
                    textPaint.textSize = when (options.labelSize) {
                        "PROMO_BESAR" -> 22f
                        "LABEL_KECIL" -> 10f
                        else -> 15f
                    }
                    textPaint.color = android.graphics.Color.rgb(211, 47, 47)
                    val priceY = strikeY + (if (options.labelSize == "LABEL_KECIL") 11f else 16f)
                    canvas.drawText("Rp ${formatNumber(effectivePrice)}", left + 4f, priceY, textPaint)
                } else {
                    // Harga Normal
                    textPaint.textSize = when (options.labelSize) {
                        "PROMO_BESAR" -> 20f
                        "LABEL_KECIL" -> 10f
                        else -> 15f
                    }
                    textPaint.color = android.graphics.Color.BLACK
                    val priceY = infoY + (if (options.labelSize == "LABEL_KECIL") 12f else 18f)
                    canvas.drawText("Rp ${formatNumber(normalPrice)}", left + 4f, priceY, textPaint)
                }

                // Satuan Kemasan
                regularTextPaint.textSize = when (options.labelSize) {
                    "PROMO_BESAR" -> 9.5f
                    "LABEL_KECIL" -> 6.5f
                    else -> 7.5f
                }
                regularTextPaint.color = android.graphics.Color.rgb(80, 80, 80)
                canvas.drawText("/ ${product.satuan}", right - 32f, bottom - 5f, regularTextPaint)
            }

            pdfDoc.finishPage(page)
        }

        val out = ByteArrayOutputStream()
        pdfDoc.writeTo(out)
        pdfDoc.close()
        return out.toByteArray()
    }
}
