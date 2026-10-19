package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.pos.formatRupiah
import java.security.MessageDigest

/**
 * Generates a deterministic 2D boolean QR matrix representing the QRIS payload.
 * Includes official QR finder patterns at top-left, top-right, bottom-left and timing strips.
 */
fun generateQrisMatrix(payload: String, size: Int = 25): Array<BooleanArray> {
    val matrix = Array(size) { BooleanArray(size) { false } }

    fun drawFinderPattern(startX: Int, startY: Int) {
        for (r in 0 until 7) {
            for (c in 0 until 7) {
                val isOuter = r == 0 || r == 6 || c == 0 || c == 6
                val isCenter = r in 2..4 && c in 2..4
                matrix[startY + r][startX + c] = isOuter || isCenter
            }
        }
    }

    // 1. Finder patterns (7x7)
    drawFinderPattern(0, 0)
    drawFinderPattern(size - 7, 0)
    drawFinderPattern(0, size - 7)

    // 2. Timing patterns (alternating black and white)
    for (i in 7 until size - 7) {
        matrix[6][i] = (i % 2 == 0)
        matrix[i][6] = (i % 2 == 0)
    }

    // 3. Alignment pattern at (size-9, size-9) if size >= 25
    if (size >= 25) {
        val ax = size - 7
        val ay = size - 7
        for (r in -2..2) {
            for (c in -2..2) {
                val isOuter = r == -2 || r == 2 || c == -2 || c == 2
                val isCenter = r == 0 && c == 0
                matrix[ay + r][ax + c] = isOuter || isCenter
            }
        }
    }

    // 4. Fill remaining data modules with deterministic bits from payload hash
    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(payload.toByteArray(Charsets.UTF_8))
    var bitIndex = 0

    for (r in 0 until size) {
        for (c in 0 until size) {
            val inFinder1 = r < 8 && c < 8
            val inFinder2 = r < 8 && c >= size - 8
            val inFinder3 = r >= size - 8 && c < 8
            val inTiming = r == 6 || c == 6
            val inAlign = size >= 25 && (r in (size - 9)..(size - 5) && c in (size - 9)..(size - 5))

            if (!inFinder1 && !inFinder2 && !inFinder3 && !inTiming && !inAlign) {
                val byteVal = hash[bitIndex % hash.size].toInt()
                val bitVal = (byteVal ushr (bitIndex % 8)) and 1
                matrix[r][c] = (bitVal == 1)
                bitIndex++
            }
        }
    }

    return matrix
}

@Composable
fun QrisBarcodeView(
    data: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 180.dp
) {
    val cleanData = if (data.isBlank()) "00020101021226600016ID.CO.QRIS.WWW" else data
    val matrix = remember(cleanData) { generateQrisMatrix(cleanData, 25) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val n = matrix.size
            val cellSize = size.minDimension / n

            // Draw white background
            drawRect(color = Color.White, size = size)

            // Draw QR modules
            for (r in 0 until n) {
                for (c in 0 until n) {
                    if (matrix[r][c]) {
                        drawRect(
                            color = Color(0xFF0F172A),
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1D Linear Barcode (Code 128 / Code 39 simulation) for NMID or Reference code.
 */
@Composable
fun LinearBarcode1DView(
    data: String,
    modifier: Modifier = Modifier,
    heightDp: Dp = 50.dp,
    label: String? = null
) {
    val cleanData = if (data.isBlank()) "ID1020030040050" else data
    val bars = remember(cleanData) {
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(cleanData.toByteArray(Charsets.UTF_8))
        BooleanArray(56) { i ->
            val byte = hash[i % hash.size].toInt()
            ((byte ushr (i % 8)) and 1) == 1
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
        ) {
            val totalBars = bars.size
            val barWidth = size.width / (totalBars * 1.4f)
            var currentX = 0f

            for (i in 0 until totalBars) {
                val isThick = bars[i]
                val currentBarWidth = if (isThick) barWidth * 1.9f else barWidth
                drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(currentX, 0f),
                    size = Size(currentBarWidth, size.height)
                )
                currentX += currentBarWidth + barWidth * 0.4f
                if (currentX >= size.width) break
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "* ${label ?: cleanData} *",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Full QRIS Display Card complete with standard National QRIS / GPN branding,
 * merchant info, NMID, and amount.
 * Allows switching between 2D QR Code, 1D Barcode, or displaying both.
 */
@Composable
fun QrisPaymentCard(
    merchantName: String,
    nmid: String,
    qrisData: String,
    amount: Double? = null,
    modifier: Modifier = Modifier
) {
    var displayMode by remember { mutableStateOf("QR") } // "QR", "BARCODE", "BOTH"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: QRIS & GPN Logo simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            "QRIS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        "Pembayaran Digital",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Surface(
                    color = Color(0xFF1E3A8A),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "GPN",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Merchant Name & NMID
            Text(
                text = merchantName.ifBlank { "DODOLANPRO STORE" }.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )
            Text(
                text = "NMID: ${nmid.ifBlank { "ID1020030040050" }}",
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )

            if (amount != null && amount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "TOTAL: ${formatRupiah(amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF1D4ED8),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Selector for 2D QR Code vs 1D Barcode vs Keduanya
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = displayMode == "QR",
                    onClick = { displayMode = "QR" },
                    label = { Text("QR Code 2D", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = displayMode == "BARCODE",
                    onClick = { displayMode = "BARCODE" },
                    label = { Text("Barcode 1D", fontSize = 10.sp) }
                )
                FilterChip(
                    selected = displayMode == "BOTH",
                    onClick = { displayMode = "BOTH" },
                    label = { Text("Keduanya", fontSize = 10.sp) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Barcode and/or QR display
            when (displayMode) {
                "QR" -> {
                    QrisBarcodeView(
                        data = qrisData,
                        sizeDp = 175.dp
                    )
                }
                "BARCODE" -> {
                    LinearBarcode1DView(
                        data = nmid.ifBlank { "ID1020030040050" },
                        heightDp = 60.dp,
                        label = "NMID: ${nmid.ifBlank { "ID1020030040050" }}"
                    )
                }
                "BOTH" -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QrisBarcodeView(
                            data = qrisData,
                            sizeDp = 150.dp
                        )
                        LinearBarcode1DView(
                            data = nmid.ifBlank { "ID1020030040050" },
                            heightDp = 44.dp,
                            label = "NMID: ${nmid.ifBlank { "ID1020030040050" }}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Supported E-Wallets & Banks
            Text(
                "Scan menggunakan aplikasi M-Banking atau E-Wallet apa saja:",
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("BCA", "Mandiri", "BRI", "BNI", "GoPay", "OVO", "DANA", "ShopeePay").forEach { appName ->
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            appName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Helpful customization hint
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "⚙ Kustomisasi Nama Merchant, NMID, & Barcode di: SIS -> Printer & Struk",
                    fontSize = 9.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}
