package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

data class DailySalesTrend(
    val dateLabel: String,
    val fullDate: String,
    val timestamp: Long,
    val omset: Double,
    val grossProfit: Double,
    val txCount: Int
)

data class HourlyTraffic(
    val hour: Int,
    val hourLabel: String,
    val txCount: Int,
    val omset: Double,
    val isPeak: Boolean = false
)

data class PaymentDistribution(
    val caraBayar: String,
    val totalNominal: Double,
    val txCount: Int,
    val percentage: Float,
    val color: Color
)

data class CategorySales(
    val kategori: String,
    val totalOmset: Double,
    val totalQty: Int,
    val totalProfit: Double,
    val percentage: Float,
    val color: Color
)

data class TopProductPerformance(
    val plu: String,
    val nama: String,
    val kategori: String,
    val qtyTerjual: Int,
    val totalOmset: Double,
    val totalHpp: Double,
    val labaKotor: Double,
    val marginPersen: Double
)

/**
 * 1. KPI Metric Card with Trend, Accent Line & Modern Typography
 */
@Composable
fun StoreKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    bgColor: Color = Color.White,
    trendBadge: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF37474F)
                    )
                }

                if (trendBadge != null) {
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = trendBadge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A237E),
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF616161),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 2. Modern Interactive Line Chart with Gradient Fill & Smooth Curves
 */
@Composable
fun SalesTrendSplineChart(
    dataPoints: List<DailySalesTrend>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Tren Penjualan & Laba Kotor",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )
                    Text(
                        "Visualisasi omset dan margin laba",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF1976D2), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Omset", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF2E7D32), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Laba", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada data transaksi untuk grafik.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                val maxVal = remember(dataPoints) {
                    val m = dataPoints.maxOfOrNull { maxOf(it.omset, it.grossProfit) } ?: 1.0
                    if (m <= 0.0) 100000.0 else m * 1.15
                }

                // Tooltip info if selected
                AnimatedVisibility(visible = selectedIndex != null) {
                    selectedIndex?.let { idx ->
                        if (idx in dataPoints.indices) {
                            val pt = dataPoints[idx]
                            Surface(
                                color = Color(0xFFE8EAF6),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(pt.fullDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A237E))
                                        Text("${pt.txCount} Transaksi Selesai", fontSize = 10.sp, color = Color(0xFF455A64))
                                    }
                                    Row {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Omset: ${formatRupiah(pt.omset)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                                            val margin = if (pt.omset > 0) (pt.grossProfit / pt.omset) * 100 else 0.0
                                            Text("Laba: ${formatRupiah(pt.grossProfit)} (${String.format("%.1f", margin)}%)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Canvas Chart
                Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dataPoints) {
                                detectTapGestures { offset ->
                                    val count = dataPoints.size
                                    if (count > 0) {
                                        val spacing = size.width / (count - 1).coerceAtLeast(1)
                                        val nearestIdx = (offset.x / spacing).toInt().coerceIn(0, count - 1)
                                        selectedIndex = nearestIdx
                                    }
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height - 24.dp.toPx()
                        val count = dataPoints.size
                        val stepX = width / (count - 1).coerceAtLeast(1)

                        // 1. Grid Lines (3 horizontal guide lines)
                        for (i in 0..3) {
                            val y = height * (i / 3f)
                            drawLine(
                                color = Color(0xFFECEFF1),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 2. Build Paths for Omset and Gross Profit
                        val omsetPath = Path()
                        val profitPath = Path()
                        val omsetFillPath = Path()
                        val profitFillPath = Path()

                        val omsetPoints = mutableListOf<Offset>()
                        val profitPoints = mutableListOf<Offset>()

                        dataPoints.forEachIndexed { i, dp ->
                            val x = i * stepX
                            val yOmset = height - ((dp.omset / maxVal).toFloat() * height).coerceIn(0f, height)
                            val yProfit = height - ((dp.grossProfit / maxVal).toFloat() * height).coerceIn(0f, height)

                            omsetPoints.add(Offset(x, yOmset))
                            profitPoints.add(Offset(x, yProfit))
                        }

                        fun buildBezierPath(pts: List<Offset>, path: Path, fillPath: Path) {
                            if (pts.isEmpty()) return
                            path.moveTo(pts[0].x, pts[0].y)
                            fillPath.moveTo(pts[0].x, height)
                            fillPath.lineTo(pts[0].x, pts[0].y)

                            for (i in 0 until pts.size - 1) {
                                val p0 = pts[i]
                                val p1 = pts[i + 1]
                                val cx = (p0.x + p1.x) / 2f
                                path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                            }
                            fillPath.lineTo(pts.last().x, height)
                            fillPath.close()
                        }

                        buildBezierPath(omsetPoints, omsetPath, omsetFillPath)
                        buildBezierPath(profitPoints, profitPath, profitFillPath)

                        // Draw Gradient Fills
                        drawPath(
                            path = omsetFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2196F3).copy(alpha = 0.28f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        drawPath(
                            path = profitFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF4CAF50).copy(alpha = 0.20f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw Strokes
                        drawPath(
                            path = omsetPath,
                            color = Color(0xFF1976D2),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        drawPath(
                            path = profitPath,
                            color = Color(0xFF2E7D32),
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw Point Circles
                        omsetPoints.forEachIndexed { idx, pt ->
                            val isSelected = selectedIndex == idx
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color(0xFF1976D2),
                                radius = if (isSelected) 4.5.dp.toPx() else 2.5.dp.toPx(),
                                center = pt
                            )
                        }

                        profitPoints.forEachIndexed { idx, pt ->
                            val isSelected = selectedIndex == idx
                            drawCircle(
                                color = Color.White,
                                radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                                center = pt
                            )
                            drawCircle(
                                color = Color(0xFF2E7D32),
                                radius = if (isSelected) 3.5.dp.toPx() else 2.dp.toPx(),
                                center = pt
                            )
                        }
                    }
                }

                // X-Axis Labels Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val count = dataPoints.size
                    val step = (count / 6).coerceAtLeast(1)
                    dataPoints.forEachIndexed { idx, item ->
                        if (idx % step == 0 || idx == count - 1) {
                            Text(
                                text = item.dateLabel,
                                fontSize = 9.sp,
                                color = if (selectedIndex == idx) Color(0xFF1976D2) else Color(0xFF78909C),
                                fontWeight = if (selectedIndex == idx) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Hourly Store Traffic & Peak Hours Bar Chart
 */
@Composable
fun HourlyTrafficBarChart(
    trafficData: List<HourlyTraffic>,
    modifier: Modifier = Modifier
) {
    val peakHour = trafficData.maxByOrNull { it.txCount }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Distribusi Jam Ramai Toko",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )
                    Text(
                        "Frekuensi transaksi per jam operasional",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                if (peakHour != null && peakHour.txCount > 0) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Peak: ${peakHour.hourLabel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val maxTx = remember(trafficData) {
                (trafficData.maxOfOrNull { it.txCount } ?: 1).coerceAtLeast(1)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                trafficData.forEach { h ->
                    val fraction = (h.txCount.toFloat() / maxTx).coerceIn(0.05f, 1f)
                    val isPeak = h.txCount == maxTx && h.txCount > 0

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        if (h.txCount > 0) {
                            Text(
                                text = "${h.txCount}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPeak) Color(0xFFE65100) else Color(0xFF455A64)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .fillMaxHeight(0.75f * fraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (isPeak) Brush.verticalGradient(
                                        listOf(Color(0xFFFF9800), Color(0xFFE65100))
                                    ) else if (h.txCount > 0) Brush.verticalGradient(
                                        listOf(Color(0xFF64B5F6), Color(0xFF1976D2))
                                    ) else Brush.verticalGradient(
                                        listOf(Color(0xFFCFD8DC), Color(0xFFB0BEC5))
                                    )
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = h.hourLabel.substringBefore(":"),
                            fontSize = 9.sp,
                            color = if (isPeak) Color(0xFFE65100) else Color(0xFF78909C),
                            fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. Payment Method Modern Donut Chart with Breakdown Cards
 */
@Composable
fun PaymentMethodDonutChart(
    paymentList: List<PaymentDistribution>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val totalTransactions = paymentList.sumOf { it.txCount }
    val totalOmset = paymentList.sumOf { it.totalNominal }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Pembagian Metode Pembayaran",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )
            Text(
                "Proporsi transaksi tunai vs digital",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Donut Canvas
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(paymentList) {
                                detectTapGestures { offset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = offset.x - center.x
                                    val dy = offset.y - center.y
                                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    if (angle < 0) angle += 360f

                                    var currentAngle = -90f
                                    if (currentAngle < 0) currentAngle += 360f

                                    var cumulative = 0f
                                    paymentList.forEachIndexed { i, p ->
                                        val sweep = p.percentage * 360f
                                        val start = (cumulative - 90f + 360f) % 360f
                                        val end = (start + sweep) % 360f
                                        if (start < end) {
                                            if (angle in start..end) selectedIndex = i
                                        } else {
                                            if (angle >= start || angle <= end) selectedIndex = i
                                        }
                                        cumulative += sweep
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 24.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f

                        if (paymentList.isEmpty()) {
                            drawArc(
                                color = Color(0xFFECEFF1),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        } else {
                            paymentList.forEachIndexed { idx, item ->
                                val sweepAngle = item.percentage * 360f
                                val isSelected = selectedIndex == idx
                                drawArc(
                                    color = item.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle - 2f, // subtle gap
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(
                                        width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }

                    // Center text in Donut
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val currentSel = selectedIndex?.let { paymentList.getOrNull(it) }
                        if (currentSel != null) {
                            Text(
                                text = currentSel.caraBayar,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = currentSel.color
                            )
                            Text(
                                text = "${String.format("%.1f", currentSel.percentage * 100)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF263238)
                            )
                        } else {
                            Text(
                                text = "$totalTransactions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A237E)
                            )
                            Text(
                                text = "Total Bon",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Legend Column
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentList.forEachIndexed { idx, p ->
                        val isSelected = selectedIndex == idx
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) p.color.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedIndex = if (isSelected) null else idx }
                                .padding(vertical = 4.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(p.color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        p.caraBayar,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF263238)
                                    )
                                    Text(
                                        "${p.txCount} Bon (${String.format("%.0f", p.percentage * 100)}%)",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Text(
                                formatRupiah(p.totalNominal),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Category Breakdown with Horizontal Progress Bars
 */
@Composable
fun CategorySalesBarChart(
    categories: List<CategorySales>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Penjualan Berdasarkan Kategori",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0D47A1)
            )
            Text(
                "Kontribusi omset per divisi produk",
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(14.dp))

            categories.forEach { cat ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(cat.color, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.kategori,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF37474F)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${cat.totalQty} terjual)",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        Text(
                            text = "${formatRupiah(cat.totalOmset)} (${String.format("%.1f", cat.percentage * 100)}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A237E)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFECEFF1))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(cat.percentage.coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(cat.color)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. Top Products Leaderboard Table with Profit Margin
 */
@Composable
fun TopProductsLeaderboard(
    topProducts: List<TopProductPerformance>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Top 10 Produk Paling Laris",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D47A1)
                    )
                    Text(
                        "Peringkat penjualan dan kontribusi profit",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Text(
                        "FAST MOVING",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada data produk terjual.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                topProducts.forEachIndexed { index, prod ->
                    val rankColor = when (index) {
                        0 -> Color(0xFFFFB300) // Gold
                        1 -> Color(0xFF9E9E9E) // Silver
                        2 -> Color(0xFFCD7F32) // Bronze
                        else -> Color(0xFF78909C)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank Badge
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(rankColor.copy(alpha = 0.18f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = rankColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Product Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prod.nama,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF263238),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PLU: ${prod.plu} • ${prod.kategori}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFFF1F8E9),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${prod.qtyTerjual} pcs",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Financial Contribution
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatRupiah(prod.totalOmset),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A237E)
                            )
                            Text(
                                text = "Laba: ${formatRupiah(prod.labaKotor)} (${String.format("%.1f", prod.marginPersen)}%)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    if (index < topProducts.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }
        }
    }
}
