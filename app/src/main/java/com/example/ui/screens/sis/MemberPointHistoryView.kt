package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.MemberPointLogEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPointHistoryView(
    viewModel: PosViewModel,
    initialMemberFilter: String? = null,
    onClearInitialFilter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pointLogs by viewModel.memberPointLogs.collectAsState()
    val members by viewModel.members.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberNo by remember(initialMemberFilter) { mutableStateOf(initialMemberFilter) }
    var selectedTypeFilter by remember { mutableStateOf("SEMUA") }
    var selectedPeriodFilter by remember { mutableStateOf("SEMUA") }

    // Dialog state for manual point adjustment
    var showAdjustDialog by remember { mutableStateOf(false) }
    var memberToAdjust by remember { mutableStateOf<MemberEntity?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }

    // Feedback message
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Filtered logs computation
    val filteredLogs = remember(pointLogs, selectedMemberNo, selectedTypeFilter, selectedPeriodFilter, searchQuery) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgo = cal.timeInMillis

        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startOfMonth = cal.timeInMillis

        pointLogs.filter { log ->
            // Member Filter
            val matchesMember = if (selectedMemberNo.isNullOrBlank()) {
                true
            } else {
                log.memberNo.equals(selectedMemberNo, ignoreCase = true)
            }

            // Type Filter
            val matchesType = when (selectedTypeFilter) {
                "REDEEM" -> log.type.equals("REDEEM", ignoreCase = true)
                "EARN" -> log.type.equals("EARN", ignoreCase = true)
                "BONUS" -> log.type.equals("BONUS", ignoreCase = true)
                "ADJUSTMENT" -> log.type.equals("ADJUSTMENT", ignoreCase = true)
                else -> true
            }

            // Period Filter
            val matchesPeriod = when (selectedPeriodFilter) {
                "HARI_INI" -> log.timestamp >= startOfToday
                "7_HARI" -> log.timestamp >= sevenDaysAgo
                "BULAN_INI" -> log.timestamp >= startOfMonth
                else -> true
            }

            // Search query
            val q = searchQuery.trim().lowercase()
            val matchesQuery = if (q.isBlank()) {
                true
            } else {
                log.memberNama.lowercase().contains(q) ||
                        log.memberNo.lowercase().contains(q) ||
                        log.referenceNo.lowercase().contains(q) ||
                        log.note.lowercase().contains(q)
            }

            matchesMember && matchesType && matchesPeriod && matchesQuery
        }
    }

    // Summary calculation for filtered logs
    val totalPoinKeluar = remember(filteredLogs) {
        filteredLogs.filter { it.pointsDelta < 0 }.sumOf { -it.pointsDelta }
    }
    val totalPoinMasuk = remember(filteredLogs) {
        filteredLogs.filter { it.pointsDelta > 0 }.sumOf { it.pointsDelta }
    }

    val selectedMemberObj = remember(members, selectedMemberNo) {
        members.find { it.memberNo == selectedMemberNo }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Feedback Banner
            if (feedbackMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RetailGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
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
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feedbackMessage.orEmpty(),
                                    color = RetailNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { feedbackMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = GrayText)
                            }
                        }
                    }
                }
            }

            // Summary Stats Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Poin Digunakan", fontSize = 11.sp, color = GrayText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("-$totalPoinKeluar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text("Tukar Diskon", fontSize = 10.sp, color = GrayText)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Poin Diperoleh", fontSize = 11.sp, color = GrayText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("+$totalPoinMasuk", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RetailGreen)
                            Text("Belanja & Bonus", fontSize = 10.sp, color = GrayText)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Total Riwayat", fontSize = 11.sp, color = GrayText)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${filteredLogs.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                            Text("Aktivitas Mutasi", fontSize = 10.sp, color = GrayText)
                        }
                    }
                }
            }

            // Member Filter Indicator / Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Filter Member:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RetailNavy)
                            }

                            FilledTonalButton(
                                onClick = { showMemberPicker = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("choose_member_filter_button")
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (selectedMemberNo.isNullOrBlank()) "Pilih Member Spesifik" else "Ganti Member",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Active Filter Banner or "Semua Member"
                        if (selectedMemberNo != null) {
                            Surface(
                                color = RetailNavy.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RetailNavy.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedMemberObj?.nama ?: "Member: $selectedMemberNo",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = RetailNavy
                                        )
                                        Text(
                                            text = "Kartu: ${selectedMemberObj?.memberNo ?: selectedMemberNo} • Saldo Sekarang: ${selectedMemberObj?.poin ?: 0} Poin",
                                            fontSize = 11.sp,
                                            color = GrayText
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            selectedMemberNo = null
                                            onClearInitialFilter?.invoke()
                                        },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("clear_member_filter_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Hapus Filter Member", tint = RetailRedPrimary)
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SuggestionChip(
                                    onClick = { selectedMemberNo = null },
                                    label = { Text("Semua Member (${members.size})", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = RetailNavy.copy(alpha = 0.1f),
                                        labelColor = RetailNavy
                                    )
                                )
                                Text(
                                    "Menampilkan seluruh mutasi koin pelanggan",
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_point_log_input"),
                    placeholder = { Text("Cari nama member, no kartu, nomor bon, atau catatan...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // Filter Chips: Jenis Mutasi & Periode
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Type Filters
                    Text("Tipe Aktivitas Poin:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailNavy)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val types = listOf(
                            "SEMUA" to "Semua Tipe",
                            "REDEEM" to "Penggunaan (-)",
                            "EARN" to "Perolehan (+)",
                            "BONUS" to "Bonus Awal",
                            "ADJUSTMENT" to "Koreksi Manual"
                        )
                        items(types) { (key, label) ->
                            FilterChip(
                                selected = selectedTypeFilter == key,
                                onClick = { selectedTypeFilter = key },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (selectedTypeFilter == key) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RetailNavy,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_type_$key")
                            )
                        }
                    }

                    // Period Filters
                    Text("Rentang Waktu:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RetailNavy)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val periods = listOf(
                            "SEMUA" to "Semua Waktu",
                            "HARI_INI" to "Hari Ini",
                            "7_HARI" to "7 Hari Terakhir",
                            "BULAN_INI" to "Bulan Ini"
                        )
                        items(periods) { (key, label) ->
                            FilterChip(
                                selected = selectedPeriodFilter == key,
                                onClick = { selectedPeriodFilter = key },
                                label = { Text(label, fontSize = 11.sp) },
                                leadingIcon = if (selectedPeriodFilter == key) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_period_$key")
                            )
                        }
                    }
                }
            }

            // Section Header with Reset Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Penggunaan (${filteredLogs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = RetailNavy
                    )

                    val isFiltered = selectedMemberNo != null || selectedTypeFilter != "SEMUA" ||
                            selectedPeriodFilter != "SEMUA" || searchQuery.isNotBlank()

                    if (isFiltered) {
                        TextButton(
                            onClick = {
                                selectedMemberNo = null
                                selectedTypeFilter = "SEMUA"
                                selectedPeriodFilter = "SEMUA"
                                searchQuery = ""
                                onClearInitialFilter?.invoke()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("reset_all_point_filters_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Filter", fontSize = 12.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // List of Point Logs
            if (filteredLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.HistoryEdu,
                                contentDescription = null,
                                tint = GrayText,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Tidak ada riwayat mutasi poin yang sesuai",
                                fontWeight = FontWeight.Medium,
                                color = GrayText,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Coba ubah kriteria pencarian atau pilih filter lain",
                                fontSize = 12.sp,
                                color = GrayText.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    PointLogItemCard(
                        log = log,
                        onMemberClick = {
                            selectedMemberNo = log.memberNo
                        }
                    )
                }
            }
        }
    }

    // --- MEMBER PICKER DIALOG ---
    if (showMemberPicker) {
        Dialog(
            onDismissRequest = { showMemberPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.8f)
                    .testTag("member_picker_dialog"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pilih Member untuk Filter", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                        IconButton(onClick = { showMemberPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    var pickerSearch by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = pickerSearch,
                        onValueChange = { pickerSearch = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari nama atau nomor member...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    val filteredMembersInPicker = remember(members, pickerSearch) {
                        if (pickerSearch.isBlank()) members else {
                            val q = pickerSearch.trim().lowercase()
                            members.filter { it.nama.lowercase().contains(q) || it.memberNo.lowercase().contains(q) }
                        }
                    }

                    // Option for "Semua Member"
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedMemberNo = null
                                onClearInitialFilter?.invoke()
                                showMemberPicker = false
                            },
                        color = if (selectedMemberNo == null) RetailNavy.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tampilkan Semua Member", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                            if (selectedMemberNo == null) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = RetailNavy)
                            }
                        }
                    }

                    HorizontalDivider()

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMembersInPicker, key = { it.memberNo }) { m ->
                            val isSelected = m.memberNo == selectedMemberNo
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedMemberNo = m.memberNo
                                        showMemberPicker = false
                                    },
                                color = if (isSelected) RetailNavy.copy(alpha = 0.12f) else Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) RetailNavy else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(m.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                        Text("No: ${m.memberNo} • Poin: ${m.poin}", fontSize = 11.sp, color = GrayText)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailNavy)
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

@Composable
fun PointLogItemCard(
    log: MemberPointLogEntity,
    onMemberClick: () -> Unit
) {
    val isDeduction = log.pointsDelta < 0
    val deltaColor = if (isDeduction) Color(0xFFDC2626) else RetailGreen
    val deltaPrefix = if (log.pointsDelta > 0) "+" else ""

    val (badgeText, badgeBg, badgeIcon) = when (log.type.uppercase()) {
        "REDEEM" -> Triple("PENGGUNAAN POIN", Color(0xFFFEE2E2), Icons.Default.ShoppingBag)
        "EARN" -> Triple("PEROLEHAN BELANJA", Color(0xFFDCFCE7), Icons.Default.TrendingUp)
        "BONUS" -> Triple("BONUS PENDAFTARAN", Color(0xFFEDE9FE), Icons.Default.Stars)
        "ADJUSTMENT" -> Triple("KOREKSI MANUAL", Color(0xFFE0F2FE), Icons.Default.Tune)
        else -> Triple(log.type, Color(0xFFF1F5F9), Icons.Default.Info)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("point_log_card_${log.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Type Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = if (isDeduction) Color(0xFFB91C1C) else RetailNavy,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDeduction) Color(0xFFB91C1C) else RetailNavy
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = GrayText, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = log.tanggal.ifBlank { "Waktu tidak tercatat" },
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Content Row: Member details & Point Delta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onMemberClick() }
                    ) {
                        Text(
                            text = log.memberNama,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RetailNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.memberNo,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GrayText,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (log.referenceNo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ref/Bon: ${log.referenceNo}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2563EB)
                        )
                    }

                    if (log.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = log.note,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Points Delta & Balance After Badge
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Surface(
                        color = if (isDeduction) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$deltaPrefix${log.pointsDelta} Poin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = deltaColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Saldo: ${log.balanceAfter} Poin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrayText
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberPointHistoryDialog(
    member: MemberEntity,
    viewModel: PosViewModel,
    onDismiss: () -> Unit,
    onViewFullHistory: () -> Unit
) {
    val pointLogs by viewModel.memberPointLogs.collectAsState()
    val memberLogs = remember(pointLogs, member.memberNo) {
        pointLogs.filter { it.memberNo.equals(member.memberNo, ignoreCase = true) }
    }

    var selectedTypeFilter by remember { mutableStateOf("SEMUA") }

    val filteredMemberLogs = remember(memberLogs, selectedTypeFilter) {
        if (selectedTypeFilter == "SEMUA") memberLogs else {
            memberLogs.filter { it.type.equals(selectedTypeFilter, ignoreCase = true) }
        }
    }

    val totalRedeemed = remember(memberLogs) {
        memberLogs.filter { it.pointsDelta < 0 }.sumOf { -it.pointsDelta }
    }
    val totalEarned = remember(memberLogs) {
        memberLogs.filter { it.pointsDelta > 0 }.sumOf { it.pointsDelta }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("member_point_history_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFFFEF3C7)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(member.nama, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                            Text("Kartu: ${member.memberNo} • ${member.poin} Poin Aktif", fontSize = 12.sp, color = GrayText)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                // Mini summary bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Saldo Poin Saat Ini", fontSize = 10.sp, color = Color(0xFF92400E))
                            Text("${member.poin} Poin", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
                        }
                    }
                    Surface(
                        color = Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Total Poin Masuk", fontSize = 10.sp, color = RetailGreen)
                            Text("+$totalEarned Poin", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailGreen)
                        }
                    }
                    Surface(
                        color = Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Total Ditukarkan", fontSize = 10.sp, color = Color(0xFFB91C1C))
                            Text("-$totalRedeemed Poin", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB91C1C))
                        }
                    }
                }

                // Type filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val types = listOf(
                        "SEMUA" to "Semua (${memberLogs.size})",
                        "REDEEM" to "Digunakan",
                        "EARN" to "Didapat",
                        "BONUS" to "Bonus",
                        "ADJUSTMENT" to "Koreksi"
                    )
                    items(types) { (key, label) ->
                        FilterChip(
                            selected = selectedTypeFilter == key,
                            onClick = { selectedTypeFilter = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = RetailNavy,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider()

                // List of logs
                if (filteredMemberLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada catatan mutasi poin untuk kategori ini",
                            fontSize = 13.sp,
                            color = GrayText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredMemberLogs, key = { it.id }) { log ->
                            PointLogItemCard(
                                log = log,
                                onMemberClick = {}
                            )
                        }
                    }
                }

                // Footer action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup")
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onViewFullHistory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tab Riwayat Lengkap", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
