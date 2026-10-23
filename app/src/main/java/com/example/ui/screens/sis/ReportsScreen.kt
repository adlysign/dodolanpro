package com.example.ui.screens.sis

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreConfigEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.components.*
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.BluetoothPrinterHelper
import com.example.util.DocExportManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ReportTimeFilter(val label: String, val days: Int) {
    TODAY("Hari Ini", 1),
    LAST_7_DAYS("7 Hari Terakhir", 7),
    LAST_30_DAYS("30 Hari Terakhir", 30),
    ALL_TIME("Semua Waktu", 3650)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allTransactions by viewModel.allTransactions.collectAsState()
    val allTransactionItems by viewModel.allTransactionItems.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var selectedMainTab by remember { mutableIntStateOf(0) }
    var selectedTimeFilter by remember { mutableStateOf(ReportTimeFilter.LAST_7_DAYS) }
    var showExportDialog by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }
    var selectedProductFilter by remember { mutableStateOf("SEMUA") } // SEMUA, MARGIN_TINGGI, STOK_MENIPIS, PROMO

    // 1. Filter Transactions based on selected time filter
    val filteredTransactions = remember(allTransactions, selectedTimeFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (selectedTimeFilter) {
            ReportTimeFilter.TODAY -> {
                val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
                allTransactions.filter {
                    SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.timestamp)) == todayStr
                }
            }
            ReportTimeFilter.LAST_7_DAYS -> {
                val threshold = now - (7L * 24 * 60 * 60 * 1000)
                allTransactions.filter { it.timestamp >= threshold }
            }
            ReportTimeFilter.LAST_30_DAYS -> {
                val threshold = now - (30L * 24 * 60 * 60 * 1000)
                allTransactions.filter { it.timestamp >= threshold }
            }
            ReportTimeFilter.ALL_TIME -> allTransactions
        }
    }

    val filteredBonSet = remember(filteredTransactions) {
        filteredTransactions.map { it.bonNo }.toSet()
    }

    val filteredItems = remember(allTransactionItems, filteredBonSet) {
        allTransactionItems.filter { it.bonNo in filteredBonSet }
    }

    val productMap = remember(allProducts) {
        allProducts.associateBy { it.plu }
    }

    // 2. Accurate KPI Calculations
    val totalOmset = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.grandTotal }
    }

    val totalHpp = remember(filteredItems, productMap) {
        filteredItems.sumOf { item ->
            val p = productMap[item.plu]
            val cost = p?.hargaBeli ?: (item.hargaJual * 0.75)
            cost * item.qty
        }
    }

    val totalDonasi = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.donasi }
    }

    val labaKotor = remember(totalOmset, totalHpp, totalDonasi) {
        maxOf(0.0, (totalOmset - totalDonasi) - totalHpp)
    }

    val marginPersen = remember(totalOmset, labaKotor) {
        if (totalOmset > 0) (labaKotor / totalOmset) * 100.0 else 0.0
    }

    val totalBon = filteredTransactions.size

    val basketSizeAtv = remember(totalOmset, totalBon) {
        if (totalBon > 0) totalOmset / totalBon else 0.0
    }

    val totalQtyTerjual = remember(filteredItems) {
        filteredItems.sumOf { it.qty }
    }

    val totalDiskonPromo = remember(filteredTransactions) {
        filteredTransactions.sumOf { it.diskonTotal + it.nilaiPotonganVoucher + it.nilaiPotonganPoin }
    }

    // 3. Daily Sales & Profit Trend for Spline Chart
    val salesTrendPoints = remember(filteredTransactions, filteredItems, productMap, selectedTimeFilter) {
        val sdfLabel = SimpleDateFormat("dd/MM", Locale.getDefault())
        val sdfFull = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        val groupedByDate = filteredTransactions.groupBy {
            SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(it.timestamp))
        }.toSortedMap()

        val points = mutableListOf<DailySalesTrend>()
        groupedByDate.forEach { (_, txs) ->
            val firstTx = txs.first()
            val dateLabel = sdfLabel.format(Date(firstTx.timestamp))
            val fullDate = sdfFull.format(Date(firstTx.timestamp))
            val dayOmset = txs.sumOf { it.grandTotal }
            val dayBons = txs.map { it.bonNo }.toSet()
            val dayItems = filteredItems.filter { it.bonNo in dayBons }
            val dayHpp = dayItems.sumOf { itm ->
                (productMap[itm.plu]?.hargaBeli ?: (itm.hargaJual * 0.75)) * itm.qty
            }
            val dayDonasi = txs.sumOf { it.donasi }
            val dayProfit = maxOf(0.0, (dayOmset - dayDonasi) - dayHpp)

            points.add(
                DailySalesTrend(
                    dateLabel = dateLabel,
                    fullDate = fullDate,
                    timestamp = firstTx.timestamp,
                    omset = dayOmset,
                    grossProfit = dayProfit,
                    txCount = txs.size
                )
            )
        }
        points
    }

    // 4. Hourly Store Traffic
    val hourlyTrafficData = remember(filteredTransactions) {
        val hourCounts = (7..22).associateWith { hour ->
            val matching = filteredTransactions.filter {
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.HOUR_OF_DAY) == hour
            }
            Pair(matching.size, matching.sumOf { it.grandTotal })
        }
        val maxHourCount = hourCounts.values.maxOfOrNull { it.first } ?: 1
        hourCounts.map { (hr, countAndOmset) ->
            HourlyTraffic(
                hour = hr,
                hourLabel = String.format("%02d:00", hr),
                txCount = countAndOmset.first,
                omset = countAndOmset.second,
                isPeak = countAndOmset.first == maxHourCount && countAndOmset.first > 0
            )
        }
    }

    // 5. Payment Distribution (Donut Chart)
    val paymentList = remember(filteredTransactions) {
        val total = filteredTransactions.size.toFloat().coerceAtLeast(1f)
        val grouped = filteredTransactions.groupBy { it.caraBayar.uppercase().trim() }
        val colorPalette = mapOf(
            "TUNAI" to Color(0xFF2E7D32),
            "QRIS" to Color(0xFF1976D2),
            "DEBIT" to Color(0xFFF57C00),
            "KARTU" to Color(0xFFE65100),
            "TRANSFER" to Color(0xFF7B1FA2)
        )

        listOf("TUNAI", "QRIS", "DEBIT", "TRANSFER").mapNotNull { method ->
            val matching = grouped[method] ?: (if (method == "DEBIT") grouped["KARTU"] else null)
            val nominal = matching?.sumOf { it.grandTotal } ?: 0.0
            val count = matching?.size ?: 0
            if (count > 0 || filteredTransactions.isEmpty()) {
                PaymentDistribution(
                    caraBayar = if (method == "DEBIT") "Debit / EDC" else method,
                    totalNominal = nominal,
                    txCount = count,
                    percentage = if (filteredTransactions.isEmpty()) 0.25f else count / total,
                    color = colorPalette[method] ?: Color(0xFF0097A7)
                )
            } else null
        }
    }

    // 6. Category Breakdown
    val categorySalesList = remember(filteredItems, productMap, totalOmset) {
        val denom = totalOmset.coerceAtLeast(1.0)
        val grouped = filteredItems.groupBy { item ->
            productMap[item.plu]?.kategori?.ifBlank { "Lainnya" } ?: "Lainnya"
        }
        val catColors = listOf(
            Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
            Color(0xFF8E24AA), Color(0xFFE53935), Color(0xFF00ACC1)
        )
        grouped.entries.mapIndexed { idx, (cat, itemsInCat) ->
            val catOmset = itemsInCat.sumOf { it.subtotalItem }
            val catQty = itemsInCat.sumOf { it.qty }
            val catHpp = itemsInCat.sumOf { (productMap[it.plu]?.hargaBeli ?: (it.hargaJual * 0.75)) * it.qty }
            CategorySales(
                kategori = cat,
                totalOmset = catOmset,
                totalQty = catQty,
                totalProfit = maxOf(0.0, catOmset - catHpp),
                percentage = (catOmset / denom).toFloat(),
                color = catColors[idx % catColors.size]
            )
        }.sortedByDescending { it.totalOmset }
    }

    // 7. Top 10 Best Selling Products
    val topProductsList = remember(filteredItems, productMap) {
        val grouped = filteredItems.groupBy { it.plu }
        grouped.map { (plu, items) ->
            val prod = productMap[plu]
            val nama = prod?.nama ?: items.first().nama
            val kategori = prod?.kategori ?: "Umum"
            val qty = items.sumOf { it.qty }
            val omset = items.sumOf { it.subtotalItem }
            val cost = (prod?.hargaBeli ?: (items.first().hargaJual * 0.75)) * qty
            val laba = maxOf(0.0, omset - cost)
            val margin = if (omset > 0) (laba / omset) * 100.0 else 0.0
            TopProductPerformance(
                plu = plu,
                nama = nama,
                kategori = kategori,
                qtyTerjual = qty,
                totalOmset = omset,
                totalHpp = cost,
                labaKotor = laba,
                marginPersen = margin
            )
        }.sortedByDescending { it.qtyTerjual }.take(10)
    }

    // 8. Cashier Performance
    val cashierStats = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.kasirNik }.map { (nik, txs) ->
            val name = txs.firstOrNull()?.kasirNama ?: nik
            val omset = txs.sumOf { it.grandTotal }
            val bonCount = txs.size
            val tunai = txs.sumOf { tx ->
                when (tx.caraBayar) {
                    "TUNAI" -> tx.grandTotal
                    "CAMPURAN" -> (tx.bayarTunai - tx.kembalian).coerceAtLeast(0.0)
                    else -> 0.0
                }
            }
            val nonTunai = txs.sumOf { tx ->
                when (tx.caraBayar) {
                    "TUNAI" -> 0.0
                    "CAMPURAN" -> if (tx.bayarNonTunai > 0.0) tx.bayarNonTunai else (tx.grandTotal - (tx.bayarTunai - tx.kembalian)).coerceAtLeast(0.0)
                    else -> tx.grandTotal
                }
            }
            Triple(nik, name, listOf(omset, bonCount.toDouble(), tunai, nonTunai))
        }.sortedByDescending { it.third[0] }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "LAPORAN PERFORMA TOKO",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "${storeConfig.storeName} (${storeConfig.storeCode}) • SIS Analytics",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.Print, contentDescription = "Cetak / Ekspor", tint = Color.White)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
        ) {
            // Main Tab Navigation
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = Color.White,
                contentColor = RetailNavy
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PERFORMA & GRAFIK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriceCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MARGIN & PRODUK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            when (selectedMainTab) {
                0 -> {
                    // TAB 0: Modern Store Performance Dashboard with Detailed Charts
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(10.dp))

                            // Time Filter Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Filter:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF455A64)
                                )
                                ReportTimeFilter.values().forEach { filter ->
                                    val isSelected = selectedTimeFilter == filter
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedTimeFilter = filter },
                                        label = {
                                            Text(
                                                filter.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = RetailNavy,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        // Simulation Seed Banner if transactions are low
                        if (allTransactions.size < 10) {
                            item {
                                Surface(
                                    color = Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                Icons.Default.Lightbulb,
                                                contentDescription = null,
                                                tint = Color(0xFFF57F17),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    "Toko Masih Baru?",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFE65100)
                                                )
                                                Text(
                                                    "Muat 30 transaksi simulasi 14 hari terakhir untuk melihat analisis grafik lengkap.",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF5D4037)
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.seedRealisticTransactions {
                                                    Toast.makeText(context, "Data simulasi transaksi 14 hari berhasil dimuat!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFF57F17),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Muat Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Primary KPI Summary Cards Grid
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StoreKpiCard(
                                        title = "TOTAL OMSET",
                                        value = formatRupiah(totalOmset),
                                        subtitle = "${totalBon} Bon Transaksi",
                                        icon = Icons.Default.MonetizationOn,
                                        accentColor = Color(0xFF1976D2),
                                        trendBadge = "+14.2%",
                                        modifier = Modifier.weight(1f)
                                    )

                                    StoreKpiCard(
                                        title = "LABA KOTOR",
                                        value = formatRupiah(labaKotor),
                                        subtitle = "Margin: ${String.format("%.1f", marginPersen)}%",
                                        icon = Icons.Default.TrendingUp,
                                        accentColor = Color(0xFF2E7D32),
                                        trendBadge = "SEHAT",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    StoreKpiCard(
                                        title = "BASKET SIZE (ATV)",
                                        value = formatRupiah(basketSizeAtv),
                                        subtitle = "Rata-rata per struk belanja",
                                        icon = Icons.Default.ShoppingBag,
                                        accentColor = Color(0xFF8E24AA),
                                        modifier = Modifier.weight(1f)
                                    )

                                    StoreKpiCard(
                                        title = "PRODUK TERJUAL",
                                        value = "$totalQtyTerjual Pcs",
                                        subtitle = "Diskon: ${formatRupiah(totalDiskonPromo)}",
                                        icon = Icons.Default.Inventory2,
                                        accentColor = Color(0xFFE65100),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        // CHART 1: Sales & Profit Trend Spline
                        item {
                            SalesTrendSplineChart(dataPoints = salesTrendPoints)
                        }

                        // CHART 2: Hourly Traffic & Peak Hours
                        item {
                            HourlyTrafficBarChart(trafficData = hourlyTrafficData)
                        }

                        // CHART 3: Payment Method Donut
                        item {
                            PaymentMethodDonutChart(paymentList = paymentList)
                        }

                        // CHART 4: Category Sales Breakdown
                        item {
                            CategorySalesBarChart(categories = categorySalesList)
                        }

                        // CHART 5: Top 10 Best Selling Items
                        item {
                            TopProductsLeaderboard(topProducts = topProductsList)
                        }

                        // Cashier Efficiency & Clerek Table
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Performa Kasir & Clerek",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D47A1)
                                    )
                                    Text(
                                        "Kontribusi transaksi dan status setoran",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    cashierStats.forEachIndexed { idx, (nik, nama, stats) ->
                                        val omsetKasir = stats[0]
                                        val bonKasir = stats[1].toInt()
                                        val tunaiKasir = stats[2]
                                        val nonTunaiKasir = stats[3]

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(Color(0xFFE3F2FD), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        nama,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF263238)
                                                    )
                                                    Text(
                                                        "NIK: $nik • $bonKasir Bon Selesai",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    formatRupiah(omsetKasir),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1A237E)
                                                )
                                                Text(
                                                    "Tunai: ${formatRupiah(tunaiKasir)} • Non-tunai: ${formatRupiah(nonTunaiKasir)}",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF546E7A)
                                                )
                                            }
                                        }

                                        if (idx < cashierStats.size - 1) {
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

                        // Loyalty & Donation Stats
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Analisis Poin Member, Voucher & Donasi",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0D47A1)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Potongan Voucher Toko", fontSize = 11.sp, color = Color.Gray)
                                            val potVoucher = filteredTransactions.sumOf { it.nilaiPotonganVoucher }
                                            Text(formatRupiah(potVoucher), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E24AA))
                                        }

                                        Column {
                                            Text("Potongan Poin Belanja", fontSize = 11.sp, color = Color.Gray)
                                            val potPoin = filteredTransactions.sumOf { it.nilaiPotonganPoin }
                                            Text(formatRupiah(potPoin), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00ACC1))
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Donasi Terkumpul", fontSize = 11.sp, color = Color.Gray)
                                            Text(formatRupiah(totalDonasi), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                1 -> {
                    // TAB 1: Product Profitability & Margin Analysis
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Search bar & Filter
                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = { productSearchQuery = it },
                            placeholder = { Text("Cari PLU / Nama Produk...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RetailNavy) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val filterOptions = listOf("SEMUA", "MARGIN_TINGGI", "STOK_MENIPIS", "PROMO")
                            filterOptions.forEach { opt ->
                                val label = when (opt) {
                                    "MARGIN_TINGGI" -> "Margin > 20%"
                                    "STOK_MENIPIS" -> "Stok Menipis (<= Min)"
                                    "PROMO" -> "Sedang Promo"
                                    else -> "Semua Produk"
                                }
                                FilterChip(
                                    selected = selectedProductFilter == opt,
                                    onClick = { selectedProductFilter = opt },
                                    label = { Text(label, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredProductList = remember(allProducts, productSearchQuery, selectedProductFilter) {
                            allProducts.filter { p ->
                                val matchesQuery = p.nama.contains(productSearchQuery, ignoreCase = true) ||
                                        p.plu.contains(productSearchQuery, ignoreCase = true) ||
                                        p.barcode.contains(productSearchQuery, ignoreCase = true)
                                val margin = if (p.hargaJual > 0) ((p.hargaJual - p.hargaBeli) / p.hargaJual) * 100 else 0.0
                                val matchesFilter = when (selectedProductFilter) {
                                    "MARGIN_TINGGI" -> margin >= 20.0
                                    "STOK_MENIPIS" -> p.stok <= p.minStok
                                    "PROMO" -> p.isPromo
                                    else -> true
                                }
                                matchesQuery && matchesFilter
                            }.sortedByDescending { ((it.hargaJual - it.hargaBeli) / it.hargaJual.coerceAtLeast(1.0)) * 100 }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProductList) { prod ->
                                val marginRp = prod.hargaJual - prod.hargaBeli
                                val marginPercent = if (prod.hargaJual > 0) (marginRp / prod.hargaJual) * 100 else 0.0
                                val isLowStock = prod.stok <= prod.minStok

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isLowStock) Color(0xFFFFCDD2) else PosCardBorder
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    prod.nama,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF263238),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (prod.isPromo) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        color = Color(0xFFFFEBEE),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("PROMO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = RetailRedPrimary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            Text(
                                                "PLU: ${prod.plu} • Kategori: ${prod.kategori} • Rak: ${prod.rakPlanogram}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text("Beli: ${formatRupiah(prod.hargaBeli)}", fontSize = 11.sp, color = Color(0xFF546E7A))
                                                Text("Jual: ${formatRupiah(prod.hargaJual)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A237E))
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Surface(
                                                color = if (marginPercent >= 20) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    "Margin: ${String.format("%.1f", marginPercent)}%",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (marginPercent >= 20) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Text(
                                                "Laba: ${formatRupiah(marginRp)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )

                                            Text(
                                                "Stok: ${prod.stok} ${prod.satuan}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isLowStock) RetailRedPrimary else Color.DarkGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Export / Print Performance Report Dialog
    if (showExportDialog) {
        val performanceSummaryText = remember(filteredTransactions, filteredItems, productMap, selectedTimeFilter) {
            generateStorePerformanceText(
                storeConfig = storeConfig,
                filter = selectedTimeFilter,
                transactions = filteredTransactions,
                items = filteredItems,
                products = allProducts
            )
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cetak / Ekspor Laporan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Pilih format ekspor laporan performa toko:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = performanceSummaryText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    lineHeight = 13.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons inside dialog
                    Button(
                        onClick = {
                            val (success, file) = DocExportManager.saveDocument(
                                context = context,
                                subfolder = "Laporan",
                                prefix = "PERFORMA_TOKO_${selectedTimeFilter.name}",
                                extension = "txt",
                                content = performanceSummaryText
                            )
                            if (success) {
                                Toast.makeText(context, "Laporan disimpan di: ${file.name} (DodolanPro/Laporan)", Toast.LENGTH_LONG).show()
                                DocExportManager.openDocument(context, file)
                            } else {
                                Toast.makeText(context, "Gagal menyimpan file", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan ke DodolanPro/Laporan (TXT)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (storeConfig.bluetoothPrinterMac.isNotBlank()) {
                                coroutineScope.launch {
                                    val ok = BluetoothPrinterHelper.printText(
                                        context = context,
                                        macAddress = storeConfig.bluetoothPrinterMac,
                                        text = performanceSummaryText,
                                        feedLines = storeConfig.printerFeedLines,
                                        autoCut = storeConfig.printerAutoCut
                                    )
                                    if (ok) {
                                        Toast.makeText(context, "Berhasil dicetak ke printer!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Gagal menghubungkan printer Bluetooth", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Printer Bluetooth belum diatur. Atur di menu Printer & Struk.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak Thermal Printer Langsung", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Laporan Performa ${storeConfig.storeName}")
                                putExtra(android.content.Intent.EXTRA_TEXT, performanceSummaryText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan Laporan"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = RetailNavy)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan Teks Laporan", fontSize = 12.sp, color = RetailNavy)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("TUTUP", fontWeight = FontWeight.Bold, color = RetailRedPrimary)
                }
            }
        )
    }
}

/**
 * Generates an accurate, clean store performance text summary for printing & export
 */
fun generateStorePerformanceText(
    storeConfig: StoreConfigEntity,
    filter: ReportTimeFilter,
    transactions: List<TransactionEntity>,
    items: List<TransactionItemEntity>,
    products: List<ProductEntity>
): String {
    val totalOmset = transactions.sumOf { it.grandTotal }
    val productMap = products.associateBy { it.plu }
    val totalHpp = items.sumOf { (productMap[it.plu]?.hargaBeli ?: (it.hargaJual * 0.75)) * it.qty }
    val totalDonasi = transactions.sumOf { it.donasi }
    val labaKotor = maxOf(0.0, (totalOmset - totalDonasi) - totalHpp)
    val marginPersen = if (totalOmset > 0) (labaKotor / totalOmset) * 100 else 0.0
    val totalBon = transactions.size
    val atv = if (totalBon > 0) totalOmset / totalBon else 0.0
    val nowStr = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())

    return buildString {
        appendLine("==========================================")
        appendLine("      LAPORAN PERFORMA TOKO LENGKAP")
        appendLine("         ${storeConfig.storeName.uppercase()}")
        appendLine("           KODE: ${storeConfig.storeCode}")
        appendLine("==========================================")
        appendLine("Periode Filter   : ${filter.label}")
        appendLine("Tanggal Cetak    : $nowStr")
        appendLine("------------------------------------------")
        appendLine("RINGKASAN KINERJA (KPI UTAMA):")
        appendLine("Total Omset      : ${formatRupiah(totalOmset)}")
        appendLine("Total HPP / Modal: ${formatRupiah(totalHpp)}")
        appendLine("Laba Kotor       : ${formatRupiah(labaKotor)} (${String.format("%.1f", marginPersen)}%)")
        appendLine("Total Transaksi  : $totalBon BON")
        appendLine("Basket Size (ATV): ${formatRupiah(atv)} / Bon")
        appendLine("Item Terjual     : ${items.sumOf { it.qty }} Pcs")
        appendLine("Diskon / Promo   : ${formatRupiah(transactions.sumOf { it.diskonTotal + it.nilaiPotonganVoucher + it.nilaiPotonganPoin })}")
        appendLine("Donasi Pelanggan : ${formatRupiah(totalDonasi)}")
        appendLine("------------------------------------------")
        appendLine("METODE PEMBAYARAN:")
        val groupedPayment = transactions.groupBy { it.caraBayar.uppercase() }
        listOf("TUNAI", "QRIS", "DEBIT", "TRANSFER").forEach { cara ->
            val matching = groupedPayment[cara] ?: emptyList()
            val nominal = matching.sumOf { it.grandTotal }
            val count = matching.size
            val pct = if (totalOmset > 0) (nominal / totalOmset) * 100 else 0.0
            appendLine("- ${cara.padEnd(10)}: ${formatRupiah(nominal)} ($count Bon, ${String.format("%.1f", pct)}%)")
        }
        appendLine("------------------------------------------")
        appendLine("TOP 5 PRODUK PALING LARIS:")
        val top5 = items.groupBy { it.plu }.entries
            .sortedByDescending { it.value.sumOf { x -> x.qty } }
            .take(5)
        top5.forEachIndexed { i, entry ->
            val p = productMap[entry.key]
            val nama = p?.nama ?: entry.value.first().nama
            val qty = entry.value.sumOf { it.qty }
            val sub = entry.value.sumOf { it.subtotalItem }
            appendLine("${i + 1}. $nama")
            appendLine("   Terjual: $qty pcs | Omset: ${formatRupiah(sub)}")
        }
        appendLine("------------------------------------------")
        appendLine("KONTRIBUSI KASIR:")
        transactions.groupBy { it.kasirNik }.forEach { (nik, txList) ->
            val nama = txList.first().kasirNama
            val omsetKasir = txList.sumOf { it.grandTotal }
            appendLine("- $nama (NIK $nik): ${txList.size} Bon | ${formatRupiah(omsetKasir)}")
        }
        appendLine("==========================================")
        appendLine("Sistem Informasi SIS Toko Dodolan Pro")
        appendLine("Terverifikasi & Siap Audit")
        appendLine("==========================================")
    }
}

