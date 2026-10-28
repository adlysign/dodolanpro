package com.example.ui.screens.sis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.VoucherEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherPointsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val storeConfig by viewModel.storeConfig.collectAsState()
    val vouchers by viewModel.allVouchers.collectAsState()
    val members by viewModel.members.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Dialog states
    var showAddVoucherDialog by remember { mutableStateOf(false) }
    var showBatchVoucherDialog by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<VoucherEntity?>(null) }
    var memberToAdjustPoints by remember { mutableStateOf<MemberEntity?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PENGATURAN VOUCHER & POIN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            "Program Loyalitas Poin Pelanggan & Kupon Diskon Struk",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
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
                .background(Color(0xFFF1F5F9))
        ) {
            // Tab Selection Header
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = RetailNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("PENGATURAN POIN", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_pengaturan_poin")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("VOUCHER STRUK", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_voucher_struk")
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("DAFTAR VOUCHER (${vouchers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_daftar_voucher")
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("RIWAYAT POIN", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_riwayat_poin_voucher")
                )
            }

            // Global Notification / Feedback Banner
            if (feedbackMessage.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RetailGreen),
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                feedbackMessage,
                                color = RetailGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = { feedbackMessage = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = RetailGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Content based on tab
            when (selectedTabIndex) {
                0 -> PointsSettingsTab(
                    viewModel = viewModel,
                    storeConfig = storeConfig,
                    members = members,
                    onShowFeedback = { feedbackMessage = it },
                    onAdjustPoints = { memberToAdjustPoints = it },
                    onAddMember = { showAddMemberDialog = true }
                )
                1 -> ReceiptVoucherSettingsTab(
                    viewModel = viewModel,
                    storeConfig = storeConfig,
                    onShowFeedback = { feedbackMessage = it }
                )
                2 -> VoucherListTab(
                    vouchers = vouchers,
                    onAddVoucher = { showAddVoucherDialog = true },
                    onBatchVoucher = { showBatchVoucherDialog = true },
                    onDeleteVoucher = { voucherToDelete = it },
                    onToggleUsed = { v ->
                        viewModel.toggleVoucherUsedStatus(v) {
                            feedbackMessage = "Status voucher ${v.kodeVoucher} berhasil diperbarui!"
                        }
                    },
                    onCopyCode = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Kode voucher $code disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> MemberPointHistoryView(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // --- DIALOGS ---

    // 1. Dialog Tambah Voucher Tunggal
    if (showAddVoucherDialog) {
        SingleVoucherDialog(
            defaultPrefix = storeConfig.voucherPrefix,
            onDismiss = { showAddVoucherDialog = false },
            onSave = { kode, nominal, minBelanja, masaBerlaku ->
                if (kode.isBlank()) {
                    viewModel.generateManualRandomVoucher(nominal, minBelanja, masaBerlaku) { v ->
                        feedbackMessage = "Voucher ${v.kodeVoucher} senilai ${formatRupiah(v.nominalDiskon)} berhasil dibuat!"
                        showAddVoucherDialog = false
                    }
                } else {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, masaBerlaku)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val expDate = sdf.format(cal.time)
                    val todayStr = sdf.format(Date())
                    val newV = VoucherEntity(
                        kodeVoucher = kode.trim().uppercase(),
                        nominalDiskon = nominal,
                        minBelanja = minBelanja,
                        tanggalMulai = todayStr,
                        tanggalKedaluwarsa = expDate,
                        bonNoAsal = "MANUAL"
                    )
                    viewModel.insertVoucher(newV) {
                        feedbackMessage = "Voucher ${newV.kodeVoucher} berhasil disimpan!"
                        showAddVoucherDialog = false
                    }
                }
            }
        )
    }

    // 2. Dialog Buat Voucher Massal (Batch)
    if (showBatchVoucherDialog) {
        BatchVoucherDialog(
            defaultPrefix = storeConfig.voucherPrefix,
            onDismiss = { showBatchVoucherDialog = false },
            onGenerate = { count, nominal, minBelanja, masaBerlaku, prefix ->
                viewModel.batchGenerateVouchers(count, nominal, minBelanja, masaBerlaku, prefix) { generatedCount ->
                    feedbackMessage = "Berhasil membuat $generatedCount voucher baru dengan prefix $prefix!"
                    showBatchVoucherDialog = false
                }
            }
        )
    }

    // 3. Dialog Konfirmasi Hapus Voucher
    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RetailRedPrimary) },
            title = { Text("Hapus Voucher?", fontWeight = FontWeight.Bold, color = Color.Black) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus voucher '${v.kodeVoucher}' senilai ${formatRupiah(v.nominalDiskon)}?\nTindakan ini tidak dapat dibatalkan.",
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoucher(v) {
                            feedbackMessage = "Voucher ${v.kodeVoucher} berhasil dihapus"
                            voucherToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
                ) {
                    Text("Ya, Hapus Voucher", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { voucherToDelete = null }) {
                    Text("Batal", color = Color.Black)
                }
            }
        )
    }

    // 4. Dialog Penyesuaian Poin Member (Points Adjustment)
    memberToAdjustPoints?.let { member ->
        AdjustMemberPointsDialog(
            member = member,
            nilaiPerPoin = storeConfig.nilaiPerPoin,
            onDismiss = { memberToAdjustPoints = null },
            onConfirmAdjust = { delta, isDirectSet ->
                if (isDirectSet) {
                    viewModel.setMemberPoints(member.memberNo, delta) {
                        feedbackMessage = "Saldo poin ${member.nama} diubah menjadi $delta Poin"
                        memberToAdjustPoints = null
                    }
                } else {
                    viewModel.adjustMemberPoints(member.memberNo, delta) {
                        val ket = if (delta >= 0) "ditambah $delta" else "dikurang ${-delta}"
                        feedbackMessage = "Poin member ${member.nama} berhasil $ket Poin!"
                        memberToAdjustPoints = null
                    }
                }
            }
        )
    }

    // 5. Dialog Tambah Member Cepat
    if (showAddMemberDialog) {
        QuickAddMemberDialog(
            initialBonusPoints = if (storeConfig.poinAktif) storeConfig.poinBonusMemberBaru else 0,
            onDismiss = { showAddMemberDialog = false },
            onSave = { noKartu, nama, hp, bonus ->
                val newMember = MemberEntity(
                    memberNo = noKartu,
                    nama = nama,
                    noHp = hp,
                    poin = bonus
                )
                viewModel.saveMember(newMember)
                feedbackMessage = "Member $nama berhasil didaftarkan dengan $bonus Poin bonus!"
                showAddMemberDialog = false
            }
        )
    }
}

// ==============================================================================
// TAB 1: PENGATURAN POIN LOYALITAS
// ==============================================================================
@Composable
private fun PointsSettingsTab(
    viewModel: PosViewModel,
    storeConfig: com.example.data.local.entity.StoreConfigEntity,
    members: List<MemberEntity>,
    onShowFeedback: (String) -> Unit,
    onAdjustPoints: (MemberEntity) -> Unit,
    onAddMember: () -> Unit
) {
    var poinAktif by remember(storeConfig) { mutableStateOf(storeConfig.poinAktif) }
    var belanjaPerPoinStr by remember(storeConfig) { mutableStateOf(storeConfig.belanjaPerPoin.toLong().toString()) }
    var nilaiPerPoinStr by remember(storeConfig) { mutableStateOf(storeConfig.nilaiPerPoin.toLong().toString()) }
    var minPoinRedeemStr by remember(storeConfig) { mutableStateOf(storeConfig.minPoinRedeem.toString()) }
    var maxPoinRedeemStr by remember(storeConfig) { mutableStateOf(storeConfig.maxPoinRedeemPerTransaksi.toString()) }
    var poinBonusBaruStr by remember(storeConfig) { mutableStateOf(storeConfig.poinBonusMemberBaru.toString()) }
    var poinBisaGunakanSemua by remember(storeConfig) { mutableStateOf(storeConfig.poinBisaGunakanSemua) }
    var poinMaksPersenBelanjaStr by remember(storeConfig) { mutableStateOf(storeConfig.poinMaksPersenBelanja.toInt().toString()) }
    var minBelanjaGunakanPoinStr by remember(storeConfig) { mutableStateOf(storeConfig.minBelanjaGunakanPoin.toLong().toString()) }
    var poinMaksPotonganNominalStr by remember(storeConfig) { mutableStateOf(storeConfig.poinMaksPotonganNominal.toLong().toString()) }
    var poinDapatSaatRedeem by remember(storeConfig) { mutableStateOf(storeConfig.poinDapatSaatRedeem) }

    // State kalkulator simulasi
    var testBelanjaStr by remember { mutableStateOf("100000") }
    var testSaldoPoinStr by remember { mutableStateOf("500") }
    var memberSearchQuery by remember { mutableStateOf("") }

    val filteredMembers = remember(members, memberSearchQuery) {
        if (memberSearchQuery.isBlank()) members
        else members.filter {
            it.nama.contains(memberSearchQuery, ignoreCase = true) ||
            it.noHp.contains(memberSearchQuery) ||
            it.memberNo.contains(memberSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Status Program Loyalitas Poin
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("STATUS PROGRAM POIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                Text(
                                    if (poinAktif) "Program loyalitas member AKTIF" else "Program loyalitas member NONAKTIF",
                                    fontSize = 11.sp,
                                    color = if (poinAktif) RetailGreen else Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = poinAktif,
                            onCheckedChange = { poinAktif = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RetailGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rasio Belanja", fontSize = 10.sp, color = Color.Gray)
                            Text("Rp ${belanjaPerPoinStr} = 1 Poin", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Nilai Tukar Poin", fontSize = 10.sp, color = Color.Gray)
                            Text("1 Poin = Rp ${nilaiPerPoinStr}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Mode Potongan", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                if (poinBisaGunakanSemua) "Bisa Semua (100%)" else "Maks $poinMaksPersenBelanjaStr%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }

        // Form Konfigurasi Aturan Poin
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "1. ATURAN PEROLEHAN & NILAI POIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RetailNavy
                    )

                    OutlinedTextField(
                        value = belanjaPerPoinStr,
                        onValueChange = { belanjaPerPoinStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Kelipatan Belanja untuk Mendapat 1 Poin (Rp)") },
                        supportingText = { Text("Member mendapat 1 Poin setiap belanja kelipatan nominal ini (cth: 10.000)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nilaiPerPoinStr,
                        onValueChange = { nilaiPerPoinStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Nilai Tukar 1 Poin ke Rupiah (Rp)") },
                        supportingText = { Text("Besar potongan harga per 1 Poin saat ditukar di kasir (cth: 100)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = poinBonusBaruStr,
                        onValueChange = { poinBonusBaruStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Bonus Poin Member Baru") },
                        suffix = { Text("Poin") },
                        supportingText = { Text("Hadiah poin selamat datang otomatis saat registrasi member") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tetap Dapat Poin Baru Saat Tukar Poin", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = RetailNavy)
                            Text("Member tetap memperoleh poin dari sisa pembayaran transaksi saat menggunakan poin", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = poinDapatSaatRedeem,
                            onCheckedChange = { poinDapatSaatRedeem = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        "2. ATURAN PEMOTONGAN BELANJA DARI POIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RetailNavy
                    )

                    // Pilihan Mode: Bisa Semua (100%) vs Dibatasi Persentase
                    Surface(
                        color = if (poinBisaGunakanSemua) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (poinBisaGunakanSemua) Color(0xFF3B82F6) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { poinBisaGunakanSemua = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = poinBisaGunakanSemua,
                                onClick = { poinBisaGunakanSemua = true }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Dapat Digunakan Semua (100% dari Total Belanja)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                Text("Poin dapat memotong total belanja secara penuh sampai Rp 0 selama saldo poin member mencukupi.", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    Surface(
                        color = if (!poinBisaGunakanSemua) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (!poinBisaGunakanSemua) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { poinBisaGunakanSemua = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !poinBisaGunakanSemua,
                                onClick = { poinBisaGunakanSemua = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Dibatasi Maksimal Persentase Belanja (%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                Text("Poin hanya boleh memotong maksimal sekian persen dari total belanja (contoh: maks 50% belanja).", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Field Persentase (muncul jika tidak bisa digunakan semua)
                    if (!poinBisaGunakanSemua) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Tentukan Persentase Maksimal Potongan Poin:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF92400E))
                            OutlinedTextField(
                                value = poinMaksPersenBelanjaStr,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    val num = digits.toIntOrNull() ?: 0
                                    poinMaksPersenBelanjaStr = num.coerceIn(1, 100).toString()
                                },
                                label = { Text("Maksimal Persen Potongan Belanja") },
                                suffix = { Text("%") },
                                supportingText = { Text("Contoh: 50% berarti belanja Rp 100.000 maksimal dipotong Rp 50.000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )

                            // Quick preset chips
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(20, 30, 50, 70, 80).forEach { preset ->
                                    FilterChip(
                                        selected = poinMaksPersenBelanjaStr == preset.toString(),
                                        onClick = { poinMaksPersenBelanjaStr = preset.toString() },
                                        label = { Text("$preset%") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFF59E0B),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "3. SYARAT MINIMAL & BATASAN LAINNYA",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RetailNavy
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minBelanjaGunakanPoinStr,
                            onValueChange = { minBelanjaGunakanPoinStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Min. Belanja Pakai Poin") },
                            prefix = { Text("Rp ") },
                            supportingText = { Text("0 = Tanpa min. belanja") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = poinMaksPotonganNominalStr,
                            onValueChange = { poinMaksPotonganNominalStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Maks. Potongan Nominal") },
                            prefix = { Text("Rp ") },
                            supportingText = { Text("0 = Tanpa batas Rp") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minPoinRedeemStr,
                            onValueChange = { minPoinRedeemStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Min. Saldo Poin Member") },
                            suffix = { Text("Poin") },
                            supportingText = { Text("Syarat min. saldo member") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPoinRedeemStr,
                            onValueChange = { maxPoinRedeemStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Maks. Poin / Transaksi") },
                            suffix = { Text("Poin") },
                            supportingText = { Text("0 = Tanpa batas poin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val belanjaRule = belanjaPerPoinStr.toDoubleOrNull() ?: 10000.0
                            val nilaiRule = nilaiPerPoinStr.toDoubleOrNull() ?: 100.0
                            val minRedeem = minPoinRedeemStr.toIntOrNull() ?: 10
                            val maxRedeem = maxPoinRedeemStr.toIntOrNull() ?: 500
                            val bonusBaru = poinBonusBaruStr.toIntOrNull() ?: 20
                            val persenRule = (poinMaksPersenBelanjaStr.toDoubleOrNull() ?: 100.0).coerceIn(1.0, 100.0)
                            val minBelanjaPakai = minBelanjaGunakanPoinStr.toDoubleOrNull() ?: 0.0
                            val maksNominalCap = poinMaksPotonganNominalStr.toDoubleOrNull() ?: 0.0

                            val updated = storeConfig.copy(
                                poinAktif = poinAktif,
                                belanjaPerPoin = belanjaRule,
                                nilaiPerPoin = nilaiRule,
                                minPoinRedeem = minRedeem,
                                maxPoinRedeemPerTransaksi = maxRedeem,
                                poinBonusMemberBaru = bonusBaru,
                                poinBisaGunakanSemua = poinBisaGunakanSemua,
                                poinMaksPersenBelanja = if (poinBisaGunakanSemua) 100.0 else persenRule,
                                minBelanjaGunakanPoin = minBelanjaPakai,
                                poinMaksPotonganNominal = maksNominalCap,
                                poinDapatSaatRedeem = poinDapatSaatRedeem
                            )
                            viewModel.updateStoreConfig(updated)
                            onShowFeedback("Pengaturan Program Poin berhasil disimpan!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_simpan_pengaturan_poin")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENGATURAN POIN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Kalkulator & Simulasi Perolehan Poin
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMULASI & KALKULATOR ATURAN POIN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                    }

                    Text(
                        "Coba masukkan nominal belanja dan saldo poin untuk menguji aturan yang Anda buat:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = testBelanjaStr,
                            onValueChange = { testBelanjaStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Total Belanja (Rp)") },
                            prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = testSaldoPoinStr,
                            onValueChange = { testSaldoPoinStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Saldo Poin Member") },
                            suffix = { Text("Poin") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val testBelanja = testBelanjaStr.toDoubleOrNull() ?: 0.0
                    val testSaldoPoin = testSaldoPoinStr.toIntOrNull() ?: 0
                    val belanjaRule = belanjaPerPoinStr.toDoubleOrNull() ?: 10000.0
                    val nilaiRule = nilaiPerPoinStr.toDoubleOrNull() ?: 100.0
                    val minRedeem = minPoinRedeemStr.toIntOrNull() ?: 10
                    val maxRedeem = maxPoinRedeemStr.toIntOrNull() ?: 500
                    val persenLimit = if (poinBisaGunakanSemua) 100.0 else (poinMaksPersenBelanjaStr.toDoubleOrNull() ?: 100.0).coerceIn(1.0, 100.0)
                    val minBelanjaPakai = minBelanjaGunakanPoinStr.toDoubleOrNull() ?: 0.0
                    val maksNominalCap = poinMaksPotonganNominalStr.toDoubleOrNull() ?: 0.0

                    // Hitung poin didapat
                    val estimatedPoints = if (belanjaRule > 0) (testBelanja / belanjaRule).toInt() else 0

                    // Hitung batas penukaran poin
                    val canUsePoints = testBelanja >= minBelanjaPakai && testSaldoPoin >= minRedeem && testSaldoPoin > 0

                    val maxPotonganByPersen = testBelanja * (persenLimit / 100.0)
                    var maxPotonganAllowed = maxPotonganByPersen
                    if (maksNominalCap > 0.0) {
                        maxPotonganAllowed = minOf(maxPotonganAllowed, maksNominalCap)
                    }

                    val maxPointsAllowed = if (nilaiRule > 0) (maxPotonganAllowed / nilaiRule).toInt() else 0
                    var usablePoints = testSaldoPoin.coerceAtMost(maxPointsAllowed)
                    if (maxRedeem > 0) {
                        usablePoints = usablePoints.coerceAtMost(maxRedeem)
                    }
                    if (!canUsePoints) {
                        usablePoints = 0
                    }

                    val actualDiscount = usablePoints * nilaiRule
                    val sisaBayar = maxOf(0.0, testBelanja - actualDiscount)

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Poin Diperoleh dari Belanja:", fontSize = 12.sp, color = Color.Gray)
                                Text("+$estimatedPoints Poin", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailNavy)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Maksimal Poin Boleh Dipakai:", fontSize = 12.sp, color = Color.Gray)
                                Text("$usablePoints Poin (dari $testSaldoPoin Poin)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE65100))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Potongan Belanja:", fontSize = 12.sp, color = Color.Gray)
                                Text("-${formatRupiah(actualDiscount)} (${if (testBelanja > 0) String.format(Locale.US, "%.1f%%", (actualDiscount / testBelanja) * 100) else "0%"})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailGreen)
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sisa yang Harus Dibayar:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                                Text(formatRupiah(sisaBayar), fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailNavy)
                            }

                            if (!canUsePoints && testSaldoPoin > 0) {
                                Text(
                                    text = if (testBelanja < minBelanjaPakai) "ℹ Belanja belum mencapai syarat minimal ${formatRupiah(minBelanjaPakai)}" else "ℹ Saldo poin belum mencapai syarat minimal $minRedeem Poin",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Member Points Management Card & Leaderboard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = RetailNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SALDO POIN MEMBER TOKO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                        }
                        TextButton(onClick = onAddMember) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Member Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = memberSearchQuery,
                        onValueChange = { memberSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari Member (Nama / HP / No Kartu)...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (filteredMembers.isEmpty()) {
                        Text(
                            "Belum ada data member yang sesuai.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            filteredMembers.take(8).forEach { member ->
                                Surface(
                                    color = Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(member.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                            Text("${member.memberNo} • ${member.noHp}", fontSize = 11.sp, color = Color.Gray)
                                            val potVal = member.poin * storeConfig.nilaiPerPoin
                                            Text(
                                                "Saldo: ${member.poin} Poin (Senilai ${formatRupiah(potVal)})",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = RetailGreen
                                            )
                                        }

                                        Button(
                                            onClick = { onAdjustPoints(member) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Atur Poin", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                            if (filteredMembers.size > 8) {
                                Text(
                                    "Menampilkan 8 dari ${filteredMembers.size} member.",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 2: PENGATURAN VOUCHER OTOMATIS STRUK KASIR
// ==============================================================================
@Composable
private fun ReceiptVoucherSettingsTab(
    viewModel: PosViewModel,
    storeConfig: com.example.data.local.entity.StoreConfigEntity,
    onShowFeedback: (String) -> Unit
) {
    var autoGen by remember(storeConfig) { mutableStateOf(storeConfig.voucherAutoGenerateOnReceipt) }
    var minSyaratStr by remember(storeConfig) { mutableStateOf(storeConfig.voucherMinBelanjaSyarat.toLong().toString()) }
    var defaultNominalStr by remember(storeConfig) { mutableStateOf(storeConfig.voucherDefaultNominal.toLong().toString()) }
    var minPakaiStr by remember(storeConfig) { mutableStateOf(storeConfig.voucherMinBelanjaPakai.toLong().toString()) }
    var masaBerlakuStr by remember(storeConfig) { mutableStateOf(storeConfig.voucherMasaBerlakuHari.toString()) }
    var prefixCode by remember(storeConfig) { mutableStateOf(storeConfig.voucherPrefix) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("CETAK VOUCHER DI STRUK", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                Text(
                                    if (autoGen) "Otomatis terbitkan voucher jika belanja memenuhi syarat" else "Penerbitan voucher di struk dinonaktifkan",
                                    fontSize = 11.sp,
                                    color = if (autoGen) RetailGreen else Color.Gray
                                )
                            }
                        }
                        Switch(
                            checked = autoGen,
                            onCheckedChange = { autoGen = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RetailGreen
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("KONFIGURASI KUPOIN STRUK KASIR", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)

                    OutlinedTextField(
                        value = minSyaratStr,
                        onValueChange = { minSyaratStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Syarat Minimal Belanja Struk (Rp)") },
                        supportingText = { Text("Hanya transaksi dengan grand total >= nominal ini yang dapat voucher di struk") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = defaultNominalStr,
                        onValueChange = { defaultNominalStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Nominal Potongan Kupon Hadiah (Rp)") },
                        supportingText = { Text("Besar diskon potongan kupon voucher yang diberikan (cth: 5.000)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = minPakaiStr,
                        onValueChange = { minPakaiStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Syarat Min. Belanja Saat Memakai Kupon (Rp)") },
                        supportingText = { Text("Pelanggan harus belanja min. sebesar ini pada transaksi berikutnya (cth: 30.000)") },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = masaBerlakuStr,
                            onValueChange = { masaBerlakuStr = it.filter { c -> c.isDigit() } },
                            label = { Text("Masa Berlaku") },
                            suffix = { Text("Hari") },
                            supportingText = { Text("Kedaluwarsa sejak cetak") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = prefixCode,
                            onValueChange = { prefixCode = it.uppercase() },
                            label = { Text("Awalan (Prefix)") },
                            supportingText = { Text("Contoh: VCR / KUPON") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val minSyarat = minSyaratStr.toDoubleOrNull() ?: 50000.0
                            val defNominal = defaultNominalStr.toDoubleOrNull() ?: 5000.0
                            val minPakai = minPakaiStr.toDoubleOrNull() ?: 30000.0
                            val masaHari = masaBerlakuStr.toIntOrNull() ?: 7
                            val pfx = if (prefixCode.isNotBlank()) prefixCode.trim().uppercase() else "VCR"

                            val updated = storeConfig.copy(
                                voucherAutoGenerateOnReceipt = autoGen,
                                voucherMinBelanjaSyarat = minSyarat,
                                voucherDefaultNominal = defNominal,
                                voucherMinBelanjaPakai = minPakai,
                                voucherMasaBerlakuHari = masaHari,
                                voucherPrefix = pfx
                            )
                            viewModel.updateStoreConfig(updated)
                            onShowFeedback("Pengaturan Kupon Struk Kasir berhasil disimpan!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_simpan_voucher_struk")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMPAN PENGATURAN STRUK", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Preview Thermal Receipt Struk Kasir
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SIMULASI CETAK VOUCHER DI STRUK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Kotak Bergaya Kertas Struk Kasir
                    Surface(
                        color = Color(0xFFFAF9F6),
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF94A3B8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "----------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                "KODE VOUCHER BELANJA ANDA",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val pfx = if (prefixCode.isNotBlank()) prefixCode.trim().uppercase() else "VCR"
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
                            ) {
                                Text(
                                    "  $pfx-8K9Y2Z  ",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val defNominal = defaultNominalStr.toDoubleOrNull() ?: 5000.0
                            val minPakai = minPakaiStr.toDoubleOrNull() ?: 30000.0
                            val masaHari = masaBerlakuStr.toIntOrNull() ?: 7
                            Text("POTONGAN    : ${formatRupiah(defNominal)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            Text("MIN. BELANJA: ${formatRupiah(minPakai)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Black)
                            Text("BERLAKU S/D : $masaHari HARI DARI SEKARANG", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.DarkGray)
                            Text("*Hanya berlaku 1x transaksi", fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = Color.Gray)
                            Text(
                                "----------------------------------------",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// TAB 3: DAFTAR & KELOLA VOUCHER (Manager & Batch Generator)
// ==============================================================================
@Composable
private fun VoucherListTab(
    vouchers: List<VoucherEntity>,
    onAddVoucher: () -> Unit,
    onBatchVoucher: () -> Unit,
    onDeleteVoucher: (VoucherEntity) -> Unit,
    onToggleUsed: (VoucherEntity) -> Unit,
    onCopyCode: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("SEMUA") } // SEMUA, AKTIF, TERPAKAI, KEDALUWARSA

    val filtered = remember(vouchers, searchQuery, selectedFilter) {
        vouchers.filter { v ->
            val matchQuery = searchQuery.isBlank() || v.kodeVoucher.contains(searchQuery, ignoreCase = true)
            val matchFilter = when (selectedFilter) {
                "AKTIF" -> !v.isUsed && !v.isExpired()
                "TERPAKAI" -> v.isUsed
                "KEDALUWARSA" -> !v.isUsed && v.isExpired()
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary Metrics Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.size}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailNavy)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aktif", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.count { !it.isUsed && !it.isExpired() }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailGreen)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Terpakai", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.count { it.isUsed }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF64748B))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Kedaluwarsa", fontSize = 11.sp, color = Color.Gray)
                        Text("${vouchers.count { !it.isUsed && it.isExpired() }}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = RetailRedPrimary)
                    }
                }
            }
        }

        // Action Buttons Row: + Buat Voucher & ⚡ Batch Generate
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddVoucher,
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_tambah_voucher_single")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BUAT VOUCHER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Button(
                    onClick = onBatchVoucher,
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_batch_voucher")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TERBITKAN MASSAL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cari Kode Voucher...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                colors = retailTextFieldColors()
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("SEMUA", "AKTIF", "TERPAKAI", "KEDALUWARSA").forEach { tab ->
                    val isSel = selectedFilter == tab
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedFilter = tab },
                        label = { Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RetailNavy,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFE2E8F0),
                            labelColor = Color(0xFF0F172A)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSel,
                            borderColor = if (isSel) RetailNavy else Color(0xFF94A3B8),
                            borderWidth = 1.5.dp
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada kupon voucher ditemukan.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(filtered, key = { it.kodeVoucher }) { v ->
                VoucherCardItem(
                    voucher = v,
                    onCopy = { onCopyCode(v.kodeVoucher) },
                    onDelete = { onDeleteVoucher(v) },
                    onToggleUsed = { onToggleUsed(v) }
                )
            }
        }
    }
}

// ==============================================================================
// VOUCHER CARD COMPONENT
// ==============================================================================
@Composable
private fun VoucherCardItem(
    voucher: VoucherEntity,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleUsed: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        voucher.kodeVoucher,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = RetailNavy
                    )
                    IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Kode", tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }

                Surface(
                    color = when (voucher.statusText) {
                        "AKTIF" -> Color(0xFFE8F5E9)
                        "KEDALUWARSA" -> Color(0xFFFFEBEE)
                        else -> Color(0xFFECEFF1)
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        voucher.statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (voucher.statusText) {
                            "AKTIF" -> RetailGreen
                            "KEDALUWARSA" -> RetailRedPrimary
                            else -> Color.Gray
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Diskon: ${formatRupiah(voucher.nominalDiskon)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RetailGreen
                    )
                    Text(
                        "Min. Belanja: ${formatRupiah(voucher.minBelanja)}",
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (voucher.tanggalKedaluwarsa.isNotBlank()) {
                        Text("Berlaku s/d: ${voucher.tanggalKedaluwarsa}", fontSize = 11.sp, color = Color.Gray)
                    }
                    if (voucher.isUsed && voucher.usedAtBonNo.isNotBlank()) {
                        Text("Dipakai: ${voucher.usedAtBonNo}", fontSize = 10.sp, color = Color(0xFFE65100))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleUsed,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        if (voucher.isUsed) Icons.Default.Replay else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (voucher.isUsed) "Aktifkan Kembali" else "Tandai Terpakai", fontSize = 11.sp)
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = RetailRedPrimary)
                }
            }
        }
    }
}

// ==============================================================================
// MODAL DIALOGS
// ==============================================================================

@Composable
private fun SingleVoucherDialog(
    defaultPrefix: String,
    onDismiss: () -> Unit,
    onSave: (kode: String, nominal: Double, minBelanja: Double, masaBerlakuHari: Int) -> Unit
) {
    var kode by remember { mutableStateOf("") }
    var nominalStr by remember { mutableStateOf("10000") }
    var minBelanjaStr by remember { mutableStateOf("50000") }
    var masaBerlakuHariStr by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buat Voucher Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it.uppercase() },
                    label = { Text("Kode Voucher (Kosongkan utk Acak)") },
                    placeholder = { Text("cth: PROMO-10K") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Nominal Diskon Potongan (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minBelanjaStr,
                    onValueChange = { minBelanjaStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Syarat Minimal Belanja (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = masaBerlakuHariStr,
                    onValueChange = { masaBerlakuHariStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Masa Berlaku") },
                    suffix = { Text("Hari") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nom = nominalStr.toDoubleOrNull() ?: 10000.0
                    val minB = minBelanjaStr.toDoubleOrNull() ?: 50000.0
                    val hari = masaBerlakuHariStr.toIntOrNull() ?: 30
                    onSave(kode, nom, minB, hari)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
            ) {
                Text("Simpan Voucher", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", color = Color.Black)
            }
        }
    )
}

@Composable
private fun BatchVoucherDialog(
    defaultPrefix: String,
    onDismiss: () -> Unit,
    onGenerate: (count: Int, nominal: Double, minBelanja: Double, masaBerlakuHari: Int, prefix: String) -> Unit
) {
    var countStr by remember { mutableStateOf("10") }
    var prefix by remember { mutableStateOf(if (defaultPrefix.isNotBlank()) defaultPrefix else "VCR") }
    var nominalStr by remember { mutableStateOf("5000") }
    var minBelanjaStr by remember { mutableStateOf("30000") }
    var masaBerlakuHariStr by remember { mutableStateOf("14") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = RetailNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Terbitkan Voucher Massal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Buat kupon unik dalam jumlah banyak untuk dibagikan pada event promosi toko.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = countStr,
                        onValueChange = { countStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah Kupon") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = prefix,
                        onValueChange = { prefix = it.uppercase() },
                        label = { Text("Prefix Kode") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Diskon Tiap Voucher (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minBelanjaStr,
                    onValueChange = { minBelanjaStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Syarat Minimal Belanja (Rp)") },
                    prefix = { Text("Rp ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = masaBerlakuHariStr,
                    onValueChange = { masaBerlakuHariStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Masa Berlaku") },
                    suffix = { Text("Hari") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val count = countStr.toIntOrNull() ?: 10
                    val nom = nominalStr.toDoubleOrNull() ?: 5000.0
                    val minB = minBelanjaStr.toDoubleOrNull() ?: 30000.0
                    val hari = masaBerlakuHariStr.toIntOrNull() ?: 14
                    onGenerate(count, nom, minB, hari, prefix)
                },
                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
            ) {
                Text("Generate Sekarang", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", color = Color.Black)
            }
        }
    )
}

@Composable
private fun AdjustMemberPointsDialog(
    member: MemberEntity,
    nilaiPerPoin: Double,
    onDismiss: () -> Unit,
    onConfirmAdjust: (points: Int, isDirectSet: Boolean) -> Unit
) {
    var mode by remember { mutableStateOf("TAMBAH") } // TAMBAH, KURANG, SET
    var pointInputStr by remember { mutableStateOf("50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Penyesuaian Poin Member", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(member.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RetailNavy)
                        Text("Kartu: ${member.memberNo} • HP: ${member.noHp}", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "Saldo Saat Ini: ${member.poin} Poin (Senilai ${formatRupiah(member.poin * nilaiPerPoin)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = RetailGreen
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("TAMBAH", "KURANG", "SET").forEach { m ->
                        val isSel = mode == m
                        Button(
                            onClick = { mode = m },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) RetailNavy else Color(0xFFE2E8F0),
                                contentColor = if (isSel) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                when (m) {
                                    "TAMBAH" -> "+ Tambah"
                                    "KURANG" -> "- Kurang"
                                    else -> "Set Saldo"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = pointInputStr,
                    onValueChange = { pointInputStr = it.filter { c -> c.isDigit() } },
                    label = {
                        Text(
                            when (mode) {
                                "TAMBAH" -> "Jumlah Poin Ditambahkan"
                                "KURANG" -> "Jumlah Poin Dikurangkan"
                                else -> "Saldo Poin Baru Langsung"
                            }
                        )
                    },
                    suffix = { Text("Poin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val points = pointInputStr.toIntOrNull() ?: 0
                val afterPoints = when (mode) {
                    "TAMBAH" -> member.poin + points
                    "KURANG" -> (member.poin - points).coerceAtLeast(0)
                    else -> points
                }
                Text(
                    "Estimasi Saldo Akhir: $afterPoints Poin (Senilai ${formatRupiah(afterPoints * nilaiPerPoin)})",
                    fontSize = 11.sp,
                    color = RetailGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pts = pointInputStr.toIntOrNull() ?: 0
                    when (mode) {
                        "TAMBAH" -> onConfirmAdjust(pts, false)
                        "KURANG" -> onConfirmAdjust(-pts, false)
                        "SET" -> onConfirmAdjust(pts, true)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
            ) {
                Text("Simpan Perubahan", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", color = Color.Black)
            }
        }
    )
}

@Composable
private fun QuickAddMemberDialog(
    initialBonusPoints: Int,
    onDismiss: () -> Unit,
    onSave: (noKartu: String, nama: String, hp: String, bonusPoints: Int) -> Unit
) {
    var noKartu by remember { mutableStateOf("MBR-" + System.currentTimeMillis().toString().takeLast(4)) }
    var nama by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }
    var bonusPointsStr by remember { mutableStateOf(initialBonusPoints.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RetailNavy)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Daftar Member Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = noKartu,
                    onValueChange = { noKartu = it },
                    label = { Text("No. Kartu Member") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Lengkap Pelanggan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it },
                    label = { Text("Nomor WhatsApp / HP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bonusPointsStr,
                    onValueChange = { bonusPointsStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Bonus Poin Registrasi Awal") },
                    suffix = { Text("Poin") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nama.isNotBlank() && noKartu.isNotBlank()) {
                        val bonus = bonusPointsStr.toIntOrNull() ?: 0
                        onSave(noKartu.trim(), nama.trim(), hp.trim(), bonus)
                    }
                },
                enabled = nama.isNotBlank() && noKartu.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
            ) {
                Text("Simpan Member", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal", color = Color.Black)
            }
        }
    )
}
