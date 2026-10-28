package com.example.ui.screens.pos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CashierUserEntity
import com.example.data.local.entity.MemberEntity
import com.example.data.local.entity.ShiftConfigEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosShortcutEvent
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    onNavigateTo: (PosNavDestination) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.screenWidthDp >= 600

    val cartItems by viewModel.cartItems.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val bonNo by viewModel.currentBonNo.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val allCashiers by viewModel.allCashiers.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val pendingCarts by viewModel.pendingCarts.collectAsState()
    val appliedPoin by viewModel.appliedPoin.collectAsState()
    val allMembers by viewModel.allMembers.collectAsState()
    val allReceivables by viewModel.allReceivables.collectAsState()
    val syncConfig by viewModel.syncConfig.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    val memberRemainingPiutang = remember(currentMember, allReceivables) {
        if (currentMember == null) 0.0
        else {
            val m = currentMember!!
            allReceivables.filter { r ->
                !r.status.equals("LUNAS", ignoreCase = true) && (
                    (r.memberNo.isNotBlank() && r.memberNo == m.memberNo) ||
                    (r.customerPhone.isNotBlank() && m.noHp.isNotBlank() && r.customerPhone == m.noHp) ||
                    (m.nama.isNotBlank() && r.customerName.trim().equals(m.nama.trim(), ignoreCase = true))
                )
            }.sumOf { it.remainingAmount }
        }
    }
    var lastMemberRemainingPiutang by remember { mutableDoubleStateOf(0.0) }

    // Shift and Lock States
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val isShiftOpen by viewModel.isShiftOpen.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val shiftModalAwal by viewModel.shiftModalAwal.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val allShiftConfigs by viewModel.allShiftConfigs.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()

    val context = LocalContext.current
    val isTrialLimited = storeConfig.isTrial && allTransactions.size >= 50

    // Dialog states
    var showLovDialog by remember { mutableStateOf(false) }
    var showPendingLabelDialog by remember { mutableStateOf(false) }
    var showRecallDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showTrialLimitDialog by remember { mutableStateOf(false) }
    var trialLicenseInput by remember { mutableStateOf("") }
    var trialLicenseError by remember { mutableStateOf("") }
    var showVoidDialog by remember { mutableStateOf(false) }
    var showReturDialog by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showBukaShiftDialog by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var lastCompletedCartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var selectedCartIndex by remember { mutableStateOf<Int?>(null) }
    var itemToEditQty by remember { mutableStateOf<Pair<Int, CartItem>?>(null) }
    var itemToChangeSatuan by remember { mutableStateOf<Pair<Int, CartItem>?>(null) }
    var itemToDelete by remember { mutableStateOf<Pair<Int, CartItem>?>(null) }
    var showKeyboardShortcutsHelp by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCancelApprovalDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    val rootFocusRequester = remember { FocusRequester() }

    val handleOpenSis = {
        if (currentCashier?.isOwner() == true || currentCashier?.canAccessSis == true) {
            onNavigateTo(PosNavDestination.SIS_DASHBOARD)
        } else {
            feedbackMessage = "Akses Ditolak: Akun kasir (${currentCashier?.nama ?: ""}) tidak memiliki izin akses SIS Back Office!"
        }
    }

    val handleCancelTransaction = {
        val canCancel = currentCashier?.isOwner() == true || currentCashier?.canCancelTransaksi == true
        if (!canCancel) {
            feedbackMessage = "Akses Ditolak: Anda tidak memiliki wewenang untuk membatalkan transaksi (F4). Hubungi Supervisor/Owner!"
        } else if (cartItems.isEmpty()) {
            feedbackMessage = "Keranjang belanja masih kosong."
        } else {
            showCancelDialog = true
        }
    }

    LaunchedEffect(Unit) {
        try {
            rootFocusRequester.requestFocus()
        } catch (_: Exception) {}
        viewModel.shortcutEvents.collect { event ->
            when (event) {
                PosShortcutEvent.F1_HELP -> showKeyboardShortcutsHelp = true
                PosShortcutEvent.F2_CARI -> {
                    if (!isShiftOpen) {
                        feedbackMessage = "Buka shift kasir terlebih dahulu!"
                        showBukaShiftDialog = true
                    } else showLovDialog = true
                }
                PosShortcutEvent.F3_MEMBER -> showMemberDialog = true
                PosShortcutEvent.F4_BATAL -> handleCancelTransaction()
                PosShortcutEvent.F5_PENDING -> {
                    if (cartItems.isNotEmpty()) showPendingLabelDialog = true
                }
                PosShortcutEvent.F6_RECALL -> showRecallDialog = true
                PosShortcutEvent.F7_RETUR -> showReturDialog = true
                PosShortcutEvent.F8_VOID -> {
                    if (selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
                        showVoidDialog = true
                    } else {
                        feedbackMessage = "Pilih item di tabel terlebih dahulu untuk VOID!"
                    }
                }
                PosShortcutEvent.F10_SIS -> handleOpenSis()
                PosShortcutEvent.F12_BAYAR, PosShortcutEvent.PLUS_PAY -> {
                    val check = viewModel.validateTransactionForCheckout()
                    if (!check.first) {
                        feedbackMessage = check.second
                        if (!isShiftOpen) showBukaShiftDialog = true
                    } else if (isTrialLimited) {
                        showTrialLimitDialog = true
                    } else {
                        showPaymentDialog = true
                    }
                }
                PosShortcutEvent.ESCAPE -> {
                    showLovDialog = false
                    showMemberDialog = false
                    showRecallDialog = false
                    showPendingLabelDialog = false
                    showPaymentDialog = false
                    showTrialLimitDialog = false
                    showVoidDialog = false
                    showReturDialog = false
                    showKeyboardShortcutsHelp = false
                    showCancelDialog = false
                    itemToEditQty = null
                    itemToChangeSatuan = null
                    itemToDelete = null
                }
                PosShortcutEvent.DELETE_ITEM -> {
                    if (selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
                        showVoidDialog = true
                    } else if (cartItems.isNotEmpty()) {
                        selectedCartIndex = cartItems.size - 1
                        showVoidDialog = true
                    }
                }
                PosShortcutEvent.ARROW_UP -> {
                    if (cartItems.isNotEmpty()) {
                        val cur = selectedCartIndex ?: 0
                        selectedCartIndex = (cur - 1).coerceAtLeast(0)
                    }
                }
                PosShortcutEvent.ARROW_DOWN -> {
                    if (cartItems.isNotEmpty()) {
                        val cur = selectedCartIndex ?: -1
                        selectedCartIndex = (cur + 1).coerceAtMost(cartItems.size - 1)
                    }
                }
            }
        }
    }

    val promoItemsInCart = remember(cartItems) {
        cartItems.filter { (!it.isVoid && !it.isRetur) && (it.product.isPromo || it.product.hasDiscountOrPromo() || it.isPromoActive || it.product.hargaGrosir > 0) }
    }

    var barcodeText by remember { mutableStateOf("") }

    // Jam Digital Minimalis & Tanggal Real-time
    var currentClockMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentClockMillis = System.currentTimeMillis()
            delay(1000)
        }
    }
    val calendarInstance = remember(currentClockMillis) {
        Calendar.getInstance().apply { timeInMillis = currentClockMillis }
    }
    val digitalClockTime = remember(currentClockMillis) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(calendarInstance.time)
    }
    val fullDateIndo = remember(currentClockMillis) {
        val dayName = when (calendarInstance.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
        val dateNum = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(calendarInstance.time)
        "$dayName,  $dateNum"
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(feedbackMessage)
            feedbackMessage = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = RetailRedDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // KIRI: Branding DodolanPro, Nama & Kode Toko di bawahnya, dan Alamat Toko teks kecil di bawah nama toko
                    Column(
                        modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White
                            ) {
                                Text(
                                    "DodolanPro",
                                    fontWeight = FontWeight.Black,
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (storeConfig.isTrial) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (allTransactions.size >= 50) Color(0xFFB71C1C) else Color(0xFFD97706),
                                    border = BorderStroke(1.dp, if (allTransactions.size >= 50) Color(0xFFFFCDD2) else Color(0xFFFFE082)),
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .clickable {
                                            trialLicenseInput = storeConfig.licenseKey
                                            trialLicenseError = ""
                                            showTrialLimitDialog = true
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (allTransactions.size >= 50) Icons.Default.Lock else Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            if (allTransactions.size >= 50) "TRIAL HABIS (50/50)" else "TRIAL (${allTransactions.size}/50)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                        Text(
                            "${storeConfig.storeName} (${storeConfig.storeCode})",
                            color = Color.White, // Warna teks putih jika background merah/hitam
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                        Text(
                            storeConfig.alamat,
                            color = Color.White.copy(alpha = 0.9f), // Tulisan kecil alamat toko
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }

                    // KANAN: Icon kunci dan logout (hanya icon saja), di bawahnya jam digital minimalis & hari tanggal angka
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Tombol Kunci Layar (HANYA ICON)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable { viewModel.lockScreen() }
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = "Kunci",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Tombol Logout Kasir (HANYA ICON)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clickable {
                                        viewModel.logoutCashier()
                                        showLoginDialog = true
                                        feedbackMessage = "Kasir berhasil logout"
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = "Logout",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Tombol Sinkronisasi Multi-Device (Dapat diakses Kasir & Owner tanpa terkunci)
                            val isSyncOnline = (syncConfig.role == com.example.data.sync.SyncRole.SERVER && syncStatus.isServerRunning) ||
                                    (syncConfig.role == com.example.data.sync.SyncRole.CLIENT && syncStatus.isConnectedToServer)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSyncOnline) RetailGreen.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("btn_sync_multi_device")
                                    .clickable { onNavigateTo(PosNavDestination.SINKRONISASI_MULTI_DEVICE) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(28.dp)
                                        .padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.SyncAlt,
                                        contentDescription = "Sinkronisasi Multi-Device",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        when (syncConfig.role) {
                                            com.example.data.sync.SyncRole.SERVER -> "SERVER"
                                            com.example.data.sync.SyncRole.CLIENT -> if (syncStatus.isConnectedToServer) "ONLINE" else "KLIEN"
                                            else -> "SYNC"
                                        },
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Tombol Menu SIS (HANYA ICON)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier
                                    .testTag("btn_menu_sis")
                                    .clickable { handleOpenSis() }
                            ) {
                                Box(
                                    modifier = Modifier.size(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Dashboard,
                                        contentDescription = "Menu SIS",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Jam digital minimalis dan hari tanggal bulan tahun angka (contoh: Kamis,  01/10/26)
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = digitalClockTime,
                                color = Color.White, // teks putih pada background merah/hitam
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = fullDateIndo, // contoh: Kamis,  01/10/26
                                color = Color.White.copy(alpha = 0.95f), // teks putih pada background merah/hitam
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .background(PosSurface)
                .focusRequester(rootFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.F1 -> {
                                showKeyboardShortcutsHelp = true
                                true
                            }
                            Key.F2 -> {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Buka shift kasir terlebih dahulu!"
                                    showBukaShiftDialog = true
                                } else showLovDialog = true
                                true
                            }
                            Key.F3 -> {
                                showMemberDialog = true
                                true
                            }
                            Key.F4 -> {
                                handleCancelTransaction()
                                true
                            }
                            Key.F5 -> {
                                if (cartItems.isNotEmpty()) showPendingLabelDialog = true
                                true
                            }
                            Key.F6 -> {
                                showRecallDialog = true
                                true
                            }
                            Key.F7 -> {
                                showReturDialog = true
                                true
                            }
                            Key.F8 -> {
                                if (cartItems.isNotEmpty()) showVoidDialog = true
                                true
                            }
                            Key.F10 -> {
                                handleOpenSis()
                                true
                            }
                            Key.Plus, Key.NumPadAdd -> {
                                if (barcodeText.isEmpty() && cartItems.isNotEmpty()) {
                                    val check = viewModel.validateTransactionForCheckout()
                                    if (!check.first) {
                                        feedbackMessage = check.second
                                        if (!isShiftOpen) showBukaShiftDialog = true
                                    } else if (isTrialLimited) {
                                        showTrialLimitDialog = true
                                    } else {
                                        showPaymentDialog = true
                                    }
                                    true
                                } else false
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                if (barcodeText.isNotBlank()) {
                                    if (!isShiftOpen) {
                                        feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                        showBukaShiftDialog = true
                                    } else {
                                        viewModel.submitBarcodeOrPlu(barcodeText) { ok, msg ->
                                            feedbackMessage = msg
                                        }
                                        barcodeText = ""
                                    }
                                    true
                                } else false
                            }
                            Key.Delete -> {
                                if (selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
                                    showVoidDialog = true
                                    true
                                } else false
                            }
                            Key.Escape -> {
                                if (showLovDialog || showMemberDialog || showRecallDialog || showPendingLabelDialog || showPaymentDialog || showVoidDialog || showReturDialog || showKeyboardShortcutsHelp || itemToEditQty != null || itemToChangeSatuan != null || itemToDelete != null) {
                                    showLovDialog = false
                                    showMemberDialog = false
                                    showRecallDialog = false
                                    showPendingLabelDialog = false
                                    showPaymentDialog = false
                                    showVoidDialog = false
                                    showReturDialog = false
                                    showKeyboardShortcutsHelp = false
                                    itemToEditQty = null
                                    itemToChangeSatuan = null
                                    itemToDelete = null
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                }
        ) {
            if (isLandscape || isTablet) {
                // Dual Pane Layout
                Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
                        TerminalHeaderBar(
                            cashier = currentCashier,
                            bonNo = bonNo,
                            member = currentMember,
                            onMemberClick = { showMemberDialog = true },
                            isShiftOpen = isShiftOpen,
                            shiftNumber = activeShift,
                            shiftModal = shiftModalAwal,
                            memberRemainingPiutang = memberRemainingPiutang,
                            onShiftClick = { if (!isShiftOpen) showBukaShiftDialog = true }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .border(1.dp, PosCardBorder, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                ItemsTableHeader()

                                if (cartItems.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ShoppingCart,
                                                contentDescription = null,
                                                tint = Color(0xFFBDBDBD),
                                                modifier = Modifier.size(44.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Keranjang Belanja Masih Kosong",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Color.Black
                                            )
                                            Text(
                                                "Scan barcode, ketik PLU, atau tekan F2 (Cari Barang)",
                                                fontSize = 11.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        itemsIndexed(cartItems) { index, item ->
                                            ItemTableRow(
                                                index = index,
                                                item = item,
                                                isSelected = selectedCartIndex == index,
                                                onClick = { selectedCartIndex = index },
                                                onQtyIncrease = { viewModel.updateCartItemQty(index, item.qty + 1) },
                                                onQtyDecrease = {
                                                    if (item.qty > 1) {
                                                        viewModel.updateCartItemQty(index, item.qty - 1)
                                                    } else {
                                                        selectedCartIndex = index
                                                        showVoidDialog = true
                                                    }
                                                },
                                                onEditQtyDirect = { itemToEditQty = Pair(index, item) },
                                                onChangeSatuan = { itemToChangeSatuan = Pair(index, item) },
                                                onDeleteItem = {
                                                    selectedCartIndex = index
                                                    showVoidDialog = true
                                                }
                                            )
                                            HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                                        }
                                    }
                                }

                                if (promoItemsInCart.isNotEmpty()) {
                                    PromoBannerSection(promoItems = promoItemsInCart)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        FunctionKeysBar(
                            pendingCount = pendingCarts.size,
                            onF1Help = { showKeyboardShortcutsHelp = true },
                            onF2Cari = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else {
                                    showLovDialog = true
                                }
                            },
                            onF3Member = { showMemberDialog = true },
                            onF4Batal = { handleCancelTransaction() },
                            onF5Pending = {
                                if (cartItems.isEmpty()) {
                                    feedbackMessage = "Keranjang masih kosong, tidak bisa dipending!"
                                } else {
                                    showPendingLabelDialog = true
                                }
                            },
                            onF6Recall = { showRecallDialog = true },
                            onF7Retur = { showReturDialog = true },
                            onF8Void = {
                                if (selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
                                    showVoidDialog = true
                                } else {
                                    feedbackMessage = "Pilih item di tabel terlebih dahulu untuk VOID!"
                                }
                            },
                            onF10Menu = { handleOpenSis() }
                        )
                    }

                    Column(modifier = Modifier.weight(0.9f).fillMaxHeight()) {
                        TotalBannerDisplay(grandTotal = grandTotal)

                        Spacer(modifier = Modifier.height(8.dp))

                        BarcodeSearchBar(
                            value = barcodeText,
                            onValueChange = { barcodeText = it },
                            onSubmit = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else {
                                    viewModel.submitBarcodeOrPlu(barcodeText) {
                                        feedbackMessage = "Barang tidak ditemukan untuk kode '$barcodeText'"
                                    }
                                    barcodeText = ""
                                }
                            },
                            onOpenScanner = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Buka shift kasir terlebih dahulu!"
                                    showBukaShiftDialog = true
                                } else showLovDialog = true
                            },
                            onCameraScan = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else {
                                    showCameraScannerDialog = true
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        NumericKeypadCard(
                            onDigit = { digit -> barcodeText += digit },
                            onBackspace = { if (barcodeText.isNotEmpty()) barcodeText = barcodeText.dropLast(1) },
                            onClear = { barcodeText = "" },
                            onPlusCashend = {
                                val check = viewModel.validateTransactionForCheckout()
                                if (!check.first) {
                                    feedbackMessage = check.second
                                    if (!isShiftOpen) showBukaShiftDialog = true
                                } else if (isTrialLimited) {
                                    showTrialLimitDialog = true
                                } else {
                                    showPaymentDialog = true
                                }
                            },
                            onEnter = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else if (barcodeText.isNotEmpty()) {
                                    viewModel.submitBarcodeOrPlu(barcodeText) { ok, msg ->
                                        feedbackMessage = msg
                                    }
                                    barcodeText = ""
                                } else if (cartItems.isNotEmpty()) {
                                    val check = viewModel.validateTransactionForCheckout()
                                    if (!check.first) {
                                        feedbackMessage = check.second
                                    } else if (isTrialLimited) {
                                        showTrialLimitDialog = true
                                    } else {
                                        showPaymentDialog = true
                                    }
                                }
                            }
                        )
                    }
                }
            } else {
                // Mobile Portrait Layout
                Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    TotalBannerDisplay(grandTotal = grandTotal, isCompact = true)

                    Spacer(modifier = Modifier.height(6.dp))

                    TerminalHeaderBar(
                        cashier = currentCashier,
                        bonNo = bonNo,
                        member = currentMember,
                        onMemberClick = { showMemberDialog = true },
                        isShiftOpen = isShiftOpen,
                        shiftNumber = activeShift,
                        shiftModal = shiftModalAwal,
                        isCompact = true,
                        memberRemainingPiutang = memberRemainingPiutang,
                        onShiftClick = { if (!isShiftOpen) showBukaShiftDialog = true }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    BarcodeSearchBar(
                        value = barcodeText,
                        onValueChange = { barcodeText = it },
                        onSubmit = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                showBukaShiftDialog = true
                            } else {
                                viewModel.submitBarcodeOrPlu(barcodeText) {
                                    feedbackMessage = "Barang tidak ditemukan!"
                                }
                                barcodeText = ""
                            }
                        },
                        onOpenScanner = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Buka shift kasir terlebih dahulu!"
                                showBukaShiftDialog = true
                            } else showLovDialog = true
                        },
                        onCameraScan = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                showBukaShiftDialog = true
                            } else {
                                showCameraScannerDialog = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .border(1.dp, PosCardBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            ItemsTableHeader()
                            if (cartItems.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = Color(0xFFBDBDBD),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "Keranjang Belanja Masih Kosong",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Black
                                        )
                                        Text(
                                            "Scan barcode atau tekan F2 (Cari Barang)",
                                            fontSize = 11.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(modifier = Modifier.weight(1f)) {
                                    itemsIndexed(cartItems) { index, item ->
                                        ItemTableRow(
                                            index = index,
                                            item = item,
                                            isSelected = selectedCartIndex == index,
                                            onClick = { selectedCartIndex = index },
                                            onQtyIncrease = { viewModel.updateCartItemQty(index, item.qty + 1) },
                                            onQtyDecrease = {
                                                if (item.qty > 1) {
                                                    viewModel.updateCartItemQty(index, item.qty - 1)
                                                } else {
                                                    selectedCartIndex = index
                                                    showVoidDialog = true
                                                }
                                            },
                                            onEditQtyDirect = { itemToEditQty = Pair(index, item) },
                                            onChangeSatuan = { itemToChangeSatuan = Pair(index, item) },
                                            onDeleteItem = {
                                                selectedCartIndex = index
                                                showVoidDialog = true
                                            }
                                        )
                                        HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                            if (promoItemsInCart.isNotEmpty()) {
                                PromoBannerSection(promoItems = promoItemsInCart)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    FunctionKeysBar(
                        pendingCount = pendingCarts.size,
                        onF1Help = { showKeyboardShortcutsHelp = true },
                        onF2Cari = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Buka shift kasir terlebih dahulu!"
                                showBukaShiftDialog = true
                            } else showLovDialog = true
                        },
                        onF3Member = { showMemberDialog = true },
                        onF4Batal = { handleCancelTransaction() },
                        onF5Pending = {
                            if (cartItems.isEmpty()) {
                                feedbackMessage = "Keranjang kosong!"
                            } else {
                                showPendingLabelDialog = true
                            }
                        },
                        onF6Recall = { showRecallDialog = true },
                        onF7Retur = { showReturDialog = true },
                        onF8Void = {
                            if (selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
                                showVoidDialog = true
                            } else {
                                feedbackMessage = "Pilih item di tabel terlebih dahulu!"
                            }
                        },
                        onF10Menu = { handleOpenSis() }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            val check = viewModel.validateTransactionForCheckout()
                            if (!check.first) {
                                feedbackMessage = check.second
                                if (!isShiftOpen) showBukaShiftDialog = true
                            } else if (isTrialLimited) {
                                showTrialLimitDialog = true
                            } else {
                                showPaymentDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("cashend_button_mobile"),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BAYAR / CASHEND ( + )", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    // --- Dialog Handlers ---
    if (showLovDialog) {
        LovProductDialog(
            products = allProducts,
            onDismiss = { showLovDialog = false },
            onSelectProduct = { prod, qty ->
                if (!isShiftOpen) {
                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                    showBukaShiftDialog = true
                } else if (prod.stok <= 0) {
                    feedbackMessage = "Barang '${prod.nama}' stok habis (0)! Tidak bisa lanjut transaksi."
                } else {
                    val res = viewModel.addProductToCart(prod, qty)
                    if (res.first) {
                        feedbackMessage = "${prod.nama} ($qty pcs) ditambahkan!"
                    } else {
                        feedbackMessage = res.second
                    }
                }
            }
        )
    }

    if (showPendingLabelDialog) {
        PendingLabelDialog(
            onDismiss = { showPendingLabelDialog = false },
            onConfirm = { label ->
                if (viewModel.pendingCurrentCart(label)) {
                    feedbackMessage = "Transaksi dipending dengan label: '$label'"
                }
                showPendingLabelDialog = false
            }
        )
    }

    if (showRecallDialog) {
        RecallPendingDialog(
            pendingCarts = pendingCarts,
            onResumeCart = { id ->
                if (viewModel.resumePendingCart(id)) {
                    feedbackMessage = "Transaksi pending berhasil dipulihkan ke keranjang!"
                }
            },
            onDeleteCart = { cart, pin, reason, onError ->
                viewModel.deletePendingCartWithApproval(cart.id, pin, reason) { ok, msg ->
                    feedbackMessage = msg
                    if (ok) {
                        onError("")
                    } else {
                        onError(msg)
                    }
                }
            },
            onDismiss = { showRecallDialog = false }
        )
    }

    if (showPaymentDialog) {
        PaymentDialog(
            grandTotal = grandTotal,
            currentMember = currentMember,
            storeConfig = storeConfig,
            appliedPoin = appliedPoin,
            members = allMembers,
            memberRemainingPiutang = memberRemainingPiutang,
            onApplyMemberPoints = { viewModel.applyMemberPoints(it) },
            onApplyVoucher = { viewModel.applyVoucher(it) },
            onDismiss = {
                viewModel.applyMemberPoints(0)
                showPaymentDialog = false
            },
            onProcessPayment = { caraBayar, tunai, nonTunai, ref, pCustName, pCustPhone, pCustAddress, pDueDate, pNotes ->
                val currentCartSnapshot = cartItems.toList()
                val currentDebt = memberRemainingPiutang
                viewModel.processCheckout(
                    caraBayar = caraBayar,
                    bayarTunai = tunai,
                    bayarNonTunai = nonTunai,
                    refPembayaran = ref,
                    piutangCustomerName = pCustName,
                    piutangCustomerPhone = pCustPhone,
                    piutangCustomerAddress = pCustAddress,
                    piutangDueDate = pDueDate,
                    piutangNotes = pNotes
                ) { ok, msg, tx ->
                    if (ok && tx != null) {
                        lastCompletedCartItems = currentCartSnapshot
                        lastMemberRemainingPiutang = currentDebt
                        showPaymentDialog = false
                        showReceiptDialog = tx
                    } else {
                        feedbackMessage = msg
                    }
                }
            },
            onProcessPaymentPiutang = { custName, custPhone, custAddress, dueDate, notes, dpAmount ->
                val currentCartSnapshot = cartItems.toList()
                val currentDebt = memberRemainingPiutang
                viewModel.processCheckoutPiutang(
                    customerName = custName,
                    customerPhone = custPhone,
                    customerAddress = custAddress,
                    dueDate = dueDate,
                    notes = notes,
                    dpAmount = dpAmount
                ) { ok, msg, tx ->
                    if (ok && tx != null) {
                        lastCompletedCartItems = currentCartSnapshot
                        lastMemberRemainingPiutang = currentDebt
                        showPaymentDialog = false
                        showReceiptDialog = tx
                    } else {
                        feedbackMessage = msg
                    }
                }
            }
        )
    }

    if (showTrialLimitDialog) {
        AlertDialog(
            onDismissRequest = { showTrialLimitDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Batas 50 Transaksi Trial", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFB71C1C))
                        Text("Masa Percobaan Selesai", fontSize = 11.sp, color = GrayText)
                    }
                }
            },
            text = {
                Column {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, PosCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("ID DEVICE PERANGKAT INI:", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    viewModel.deviceId,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = RetailNavy
                                )
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Device ID", viewModel.deviceId))
                                        Toast.makeText(context, "ID Device disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Salin ID", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        "Aplikasi DodolanPro Anda berada dalam mode TRIAL dengan batas 50 transaksi. Transaksi saat ini telah mencapai kuota: ${allTransactions.size} / 50 transaksi.",
                        fontSize = 12.sp,
                        color = DarkText,
                        lineHeight = 16.sp
                    )
                    val isOwner = currentCashier?.isOwner() == true
                    if (isOwner) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Untuk melanjutkan transaksi pelanggan tanpa batas, silakan masukkan lisensi PRO LIFETIME atau ENTERPRISE:",
                            fontSize = 11.sp,
                            color = GrayText
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = trialLicenseInput,
                            onValueChange = { trialLicenseInput = it.uppercase() },
                            label = { Text("Kode Lisensi") },
                            placeholder = { Text("PRO-... atau ENT-...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_trial_license_key")
                        )

                        if (trialLicenseError.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                trialLicenseError,
                                color = RetailRedPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Batas uji coba 50 transaksi tercapai. Hanya akun Pemilik Toko (Owner) yang berwenang memasukkan kode lisensi. Silakan hubungi Owner toko.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF5D4037)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (currentCashier?.isOwner() == true) {
                    Button(
                        onClick = {
                            if (trialLicenseInput.isBlank()) {
                                trialLicenseError = "Masukkan kode lisensi terlebih dahulu."
                                return@Button
                            }
                            viewModel.activateOrUpgradeLicense(trialLicenseInput) { ok, msg ->
                                if (ok) {
                                    showTrialLimitDialog = false
                                    trialLicenseError = ""
                                    trialLicenseInput = ""
                                    feedbackMessage = msg
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                } else {
                                    trialLicenseError = msg
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RetailGreen)
                    ) {
                        Text("AKTIFKAN LISENSI")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showTrialLimitDialog = false
                        handleOpenSis()
                    }) {
                        Text("MENU SIS", color = RetailNavy, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { showTrialLimitDialog = false }) {
                        Text("TUTUP", fontSize = 12.sp)
                    }
                }
            }
        )
    }

    if (showVoidDialog && selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
        VoidItemDialog(
            item = cartItems[selectedCartIndex!!],
            onDismiss = { showVoidDialog = false },
            onConfirmVoid = { otp, reason, onError ->
                viewModel.voidItem(selectedCartIndex!!, otp, reason) { ok, msg ->
                    feedbackMessage = msg
                    if (ok) {
                        showVoidDialog = false
                    } else {
                        onError(msg)
                    }
                }
            }
        )
    }

    if (showReturDialog) {
        ReturKonsumenDialog(
            onDismiss = { showReturDialog = false },
            onLookupBon = { bonKey -> viewModel.findTransactionForRetur(bonKey) },
            onConfirmRetur = { bon, prod, qty, reason, otp ->
                viewModel.addReturItem(bon, prod, qty, reason, otp) { ok, msg ->
                    feedbackMessage = msg
                }
            }
        )
    }

    if (showMemberDialog) {
        MemberDialog(
            currentMember = currentMember,
            onDismiss = { showMemberDialog = false },
            onLookup = { key ->
                viewModel.lookupMember(key) { m ->
                    feedbackMessage = if (m != null) "Member ditemukan: ${m.nama}" else "Member tidak ditemukan!"
                }
            },
            onRemoveMember = {
                viewModel.setMember(null)
                feedbackMessage = "Member dilepas"
            }
        )
    }

    if (showCameraScannerDialog) {
        CameraBarcodeScannerDialog(
            onDismiss = { showCameraScannerDialog = false },
            onBarcodeDetected = { scannedCode ->
                showCameraScannerDialog = false
                barcodeText = scannedCode
                viewModel.submitBarcodeOrPlu(scannedCode) { ok, msg ->
                    feedbackMessage = msg
                }
            }
        )
    }

    // --- Alur: 1. Login Petugas Kasir Dahulu -> 2. Buka Shift (Boleh Ditutup) -> 3. Transaksi Wajib Buka Shift ---
    if (!isUserLoggedIn || showLoginDialog) {
        CashierLoginDialog(
            cashiers = allCashiers,
            canDismiss = isUserLoggedIn,
            onDismiss = {
                if (isUserLoggedIn) showLoginDialog = false
            },
            onLogin = { nik, pass ->
                viewModel.loginCashier(nik, pass) { ok, msg ->
                    feedbackMessage = msg
                    if (ok) {
                        showLoginDialog = false
                        if (!viewModel.isShiftOpen.value) {
                            showBukaShiftDialog = true
                        }
                    }
                }
            }
        )
    }

    if (showBukaShiftDialog) {
        BukaShiftDialog(
            currentCashier = currentCashier,
            shifts = allShiftConfigs,
            onOpenShift = { shiftNum, modal ->
                viewModel.openShift(shiftNum, modal)
                feedbackMessage = "Shift #$shiftNum dibuka! Modal: ${formatRupiah(modal)}"
                showBukaShiftDialog = false
            },
            onSwitchCashier = {
                showBukaShiftDialog = false
                showLoginDialog = true
            },
            onDismiss = {
                showBukaShiftDialog = false
            }
        )
    }

    if (isScreenLocked) {
        LockScreenDialog(
            cashier = currentCashier,
            onUnlock = { pin ->
                val ok = viewModel.unlockScreen(pin)
                if (!ok) {
                    feedbackMessage = "PIN atau Password salah!"
                }
                ok
            },
            onLogout = {
                viewModel.logoutCashier()
            }
        )
    }

    showReceiptDialog?.let { tx ->
        ReceiptDialog(
            transaction = tx,
            items = if (lastCompletedCartItems.isNotEmpty()) lastCompletedCartItems else cartItems,
            storeConfig = storeConfig,
            memberRemainingPiutang = lastMemberRemainingPiutang,
            onDismiss = {
                showReceiptDialog = null
                lastCompletedCartItems = emptyList()
                lastMemberRemainingPiutang = 0.0
            }
        )
    }

    // 1. DIALOG UBAH QTY LANGSUNG
    if (itemToEditQty != null) {
        val (index, item) = itemToEditQty!!
        var qtyInput by remember { mutableStateOf(item.qty.toString()) }
        AlertDialog(
            onDismissRequest = { itemToEditQty = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ubah Qty Produk", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black)
                }
            },
            text = {
                Column {
                    Text(item.product.nama, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("PLU: ${item.product.plu} | Satuan: ${item.selectedSatuan}", fontSize = 12.sp, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = qtyInput,
                        onValueChange = { qtyInput = it.filter { char -> char.isDigit() } },
                        label = { Text("Jumlah (Qty)", color = RetailNavy, fontWeight = FontWeight.Bold) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("input_direct_qty"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = Color(0xFF64748B),
                            focusedLabelColor = RetailNavy,
                            unfocusedLabelColor = Color(0xFF334155)
                        ),
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Pintasan Cepat Qty:", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 5, 10, 12, 24).forEach { q ->
                            val isSelected = qtyInput == q.toString()
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) RetailNavy else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) RetailNavy else Color(0xFF94A3B8)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { qtyInput = q.toString() }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Text(
                                        "$q",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQty = qtyInput.toIntOrNull() ?: item.qty
                        if (newQty > 0) {
                            val res = viewModel.updateCartItemQty(index, newQty)
                            feedbackMessage = res.second
                            itemToEditQty = null
                        } else {
                            itemToEditQty = null
                            selectedCartIndex = index
                            showVoidDialog = true
                            feedbackMessage = "Menghapus item memerlukan otorisasi persetujuan Supervisor/Owner!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Simpan Qty", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { itemToEditQty = null },
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Batal", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. DIALOG UBAH SATUAN / KEMASAN PRODUK
    if (itemToChangeSatuan != null) {
        val (index, item) = itemToChangeSatuan!!
        val prod = item.product
        val availableSatuans = remember(prod) {
            val list = mutableListOf<String>()
            list.add(prod.satuan.ifEmpty { "PCS" })
            if (prod.satuanRepack.isNotBlank() && !list.contains(prod.satuanRepack)) list.add(prod.satuanRepack)
            if (prod.satuanRepack2.isNotBlank() && !list.contains(prod.satuanRepack2)) list.add(prod.satuanRepack2)
            listOf("DUS", "PACK", "SLOP", "RENCENG", "KARTON", "BAL").forEach { u ->
                if (!list.contains(u)) list.add(u)
            }
            list
        }

        AlertDialog(
            onDismissRequest = { itemToChangeSatuan = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Satuan / Kemasan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            },
            text = {
                Column {
                    Text(prod.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                    Text("Pilih satuan kemasan untuk item ini:", fontSize = 11.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(availableSatuans) { sat ->
                            val isCurrent = item.selectedSatuan.equals(sat, ignoreCase = true)
                            val satPrice = when (sat) {
                                prod.satuanRepack -> if (prod.hargaRepack > 0) prod.hargaRepack else prod.hargaJual * maxOf(1, prod.isiRepack)
                                prod.satuanRepack2 -> if (prod.hargaRepack2 > 0) prod.hargaRepack2 else prod.hargaJual * maxOf(1, prod.isiRepack2)
                                prod.satuan -> prod.hargaJual
                                else -> prod.hargaJual
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCurrent) RetailNavy.copy(alpha = 0.1f) else Color.White,
                                border = BorderStroke(1.dp, if (isCurrent) RetailNavy else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.updateCartItemSatuan(index, sat)
                                        feedbackMessage = "Satuan diubah menjadi $sat"
                                        itemToChangeSatuan = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sat, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isCurrent) RetailNavy else Color.Black)
                                            if (isCurrent) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = RetailNavy,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text("Aktif", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                }
                                            }
                                        }
                                        if (sat == prod.satuanRepack && prod.isiRepack > 1) {
                                            Text("Isi: ${prod.isiRepack} ${prod.satuan}", fontSize = 10.sp, color = Color(0xFF475569))
                                        } else if (sat == prod.satuanRepack2 && prod.isiRepack2 > 1) {
                                            Text("Isi: ${prod.isiRepack2} ${prod.satuan}", fontSize = 10.sp, color = Color(0xFF475569))
                                        }
                                    }
                                    Text(formatRupiah(satPrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = RetailRedPrimary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { itemToChangeSatuan = null }) {
                    Text("Tutup", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. DIALOG HAPUS PRODUK DARI KERANJANG
    if (itemToDelete != null) {
        val (index, item) = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RetailRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hapus Produk dari Keranjang?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            },
            text = {
                Column {
                    Text("Apakah Anda yakin ingin menghapus item ini dari keranjang belanja?", fontSize = 13.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(item.product.nama, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                            Text("PLU: ${item.product.plu} | Qty: ${item.qty} ${item.selectedSatuan} | Total: ${formatRupiah(item.subtotal)}", fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isSupervisorOrOwner = currentCashier?.isSupervisor() == true || currentCashier?.isOwner() == true || currentCashier?.canVoidItem == true
                        if (isSupervisorOrOwner) {
                            viewModel.removeCartItem(index)
                            feedbackMessage = "${item.product.nama} dihapus dari keranjang"
                            itemToDelete = null
                            if (selectedCartIndex == index) selectedCartIndex = null
                        } else {
                            itemToDelete = null
                            selectedCartIndex = index
                            showVoidDialog = true
                            feedbackMessage = "Menghapus item memerlukan otorisasi persetujuan Supervisor/Owner!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                    modifier = Modifier.testTag("btn_confirm_delete_item")
                ) {
                    Text("Ya, Hapus", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Batal", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. DIALOG BATAL TRANSAKSI (F4)
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = RetailRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Batalkan Transaksi (F4)?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            },
            text = {
                Text(
                    "Apakah Anda yakin ingin mengosongkan seluruh keranjang belanja dan membatalkan transaksi ini?",
                    fontSize = 13.sp,
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val isSupervisorOrOwner = currentCashier?.isSupervisor() == true || currentCashier?.isOwner() == true
                        if (cartItems.isEmpty() || isSupervisorOrOwner) {
                            viewModel.clearCart()
                            selectedCartIndex = null
                            showCancelDialog = false
                            feedbackMessage = "Seluruh transaksi dibatalkan / keranjang dikosongkan"
                        } else {
                            // Kasir memerlukan persetujuan Supervisor untuk membatalkan transaksi berisi barang
                            showCancelDialog = false
                            showCancelApprovalDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
                ) {
                    Text("Ya, Batalkan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) {
                    Text("Kembali", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCancelApprovalDialog) {
        var supervisorPin by remember { mutableStateOf("") }
        var cancelReason by remember { mutableStateOf("Konsumen Batal") }
        var cancelError by remember { mutableStateOf("") }
        val cancelReasons = listOf("Konsumen Batal", "Salah Input", "Ganti Metode Bayar", "Lainnya")

        AlertDialog(
            onDismissRequest = { showCancelApprovalDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = RetailRedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Persetujuan Batal Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            },
            text = {
                Column {
                    Text(
                        "SOP Kasir: Menghapus seluruh item atau membatalkan transaksi memerlukan persetujuan Supervisor / Owner.",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Alasan Pembatalan:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cancelReasons.take(2).forEach { r ->
                            FilterChip(selected = cancelReason == r, onClick = { cancelReason = r }, label = { Text(r, fontSize = 10.sp) })
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        cancelReasons.drop(2).forEach { r ->
                            FilterChip(selected = cancelReason == r, onClick = { cancelReason = r }, label = { Text(r, fontSize = 10.sp) })
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = supervisorPin,
                        onValueChange = { supervisorPin = it; cancelError = "" },
                        label = { Text("PIN Supervisor / Owner") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (cancelError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(cancelError, color = RetailRedPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val auth = viewModel.verifyAuthorityPin(supervisorPin) { it.isSupervisor() || it.isOwner() || it.isAdmin() }
                        if (auth.first) {
                            viewModel.clearCart()
                            selectedCartIndex = null
                            showCancelApprovalDialog = false
                            feedbackMessage = "Transaksi dibatalkan dengan persetujuan ${auth.second}. Alasan: $cancelReason"
                        } else {
                            cancelError = auth.second
                        }
                    },
                    enabled = supervisorPin.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
                ) {
                    Text("Setujui & Batalkan", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelApprovalDialog = false }) {
                    Text("Batal", color = Color.Black)
                }
            }
        )
    }

    // 5. DIALOG PANDUAN SHORTCUT KEYBOARD (F1)
    if (showKeyboardShortcutsHelp) {
        AlertDialog(
            onDismissRequest = { showKeyboardShortcutsHelp = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            shape = RoundedCornerShape(12.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = RetailNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panduan Shortcut Keyboard Kasir", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                }
            },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    val shortcuts = listOf(
                        Triple("F1", "Bantuan Shortcut", "Buka jendela panduan tombol keyboard"),
                        Triple("F2", "Cari Barang (LOV)", "Pencarian produk berdasarkan nama/kategori"),
                        Triple("F3", "Member / Pelanggan", "Pilih atau cari nomor member toko"),
                        Triple("F4", "Batal Transaksi", "Kosongkan seluruh keranjang kasir"),
                        Triple("F5", "Pending Transaksi", "Simpan transaksi sementara"),
                        Triple("F6", "Recall Transaksi", "Panggil transaksi yang disimpan"),
                        Triple("F7", "Retur Penjualan", "Input pengembalian barang konsumen"),
                        Triple("F8", "Void Item", "Batalkan produk di baris tabel"),
                        Triple("F10", "Menu SIS Toko", "Buka dashboard manajemen toko"),
                        Triple("F12 / [+]", "Bayar / Checkout", "Lanjut ke pembayaran & cetak struk"),
                        Triple("Enter", "Input Barcode / PLU", "Tambah barang ke keranjang"),
                        Triple("Delete", "Hapus Item", "Hapus item produk yang dipilih"),
                        Triple("[-] / [+]", "Ubah Qty", "Kurangi atau tambah jumlah item"),
                        Triple("Klik Qty", "Ketik Qty Langsung", "Input angka jumlah secara cepat"),
                        Triple("Klik Satuan", "Ubah Kemasan", "Ganti ke PCS, DUS, SLOP, dll"),
                        Triple("Esc", "Tutup Jendela", "Batalkan / tutup jendela dialog aktif")
                    )
                    items(shortcuts) { (key, title, desc) ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = RetailNavy,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        key,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                                    Text(desc, fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showKeyboardShortcutsHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy)
                ) {
                    Text("Tutup (Esc)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- High-Contrast Black Text Table Row with +/- Qty, Direct Qty Edit, Unit Change & Delete ---
@Composable
fun ItemTableRow(
    index: Int,
    item: CartItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onQtyIncrease: () -> Unit,
    onQtyDecrease: () -> Unit,
    onEditQtyDirect: () -> Unit,
    onChangeSatuan: () -> Unit,
    onDeleteItem: () -> Unit
) {
    val bgColor = when {
        item.isVoid -> Color(0xFFFFEBEE)
        item.isRetur -> Color(0xFFFFF3E0)
        isSelected -> RetailBlueLight
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // NO
        Text("${index + 1}", modifier = Modifier.weight(0.35f), fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)

        // PLU
        Text(item.product.plu, modifier = Modifier.weight(0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        // DESKRIPSI & SATUAN (Satuan dapat diklik untuk merubah satuan)
        Column(modifier = Modifier.weight(2.3f)) {
            Text(
                item.product.nama,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Clickable Satuan Badge untuk merubah satuan produk
                Surface(
                    color = if (item.selectedSatuan != item.product.satuan) RetailNavy else Color(0xFFECEFF1),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, if (item.selectedSatuan != item.product.satuan) RetailNavy else Color(0xFFCFD8DC)),
                    modifier = Modifier.clickable { onChangeSatuan() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            item.selectedSatuan,
                            color = if (item.selectedSatuan != item.product.satuan) Color.White else Color.Black,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Ubah Satuan",
                            modifier = Modifier.size(11.dp),
                            tint = if (item.selectedSatuan != item.product.satuan) Color.White else Color.Black
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = if (item.product.stok <= 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(3.dp),
                    border = BorderStroke(0.5.dp, if (item.product.stok <= 0) Color(0xFFFFCDD2) else Color(0xFFC8E6C9))
                ) {
                    Text(
                        "Stok: ${item.product.stok}",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.product.stok <= 0) RetailRedPrimary else RetailGreen,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                if (item.isVoid) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("VOID", color = RetailRedPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                } else if (item.isRetur) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RETUR", color = Color(0xFFE65100), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // QTY CONTROLS (- dan +, serta Klik Qty Langsung - Lega & Jelas)
        Row(
            modifier = Modifier.weight(2.4f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.isVoid && !item.isRetur) {
                // Tombol [-] Kurangi Qty
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFECEFF1),
                    border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onQtyDecrease() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Remove, contentDescription = "Kurang Qty", modifier = Modifier.size(12.dp), tint = Color.Black)
                    }
                }

                // Klik Qty untuk input langsung - Lega & Tidak Terpotong
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .defaultMinSize(minWidth = 30.dp)
                        .height(24.dp)
                        .clickable { onEditQtyDirect() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 3.dp)) {
                        Text(
                            text = "${item.qty}",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }
                }

                // Tombol [+] Tambah Qty
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = RetailNavy,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onQtyIncrease() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Qty", modifier = Modifier.size(12.dp), tint = Color.White)
                    }
                }
            } else {
                Text(
                    text = if (item.isRetur) "-${item.qty}" else "${item.qty}",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }

        // HARGA SATUAN
        Column(
            modifier = Modifier.weight(1.3f),
            horizontalAlignment = Alignment.End
        ) {
            if (item.promoDiscountAmount > 0 || item.unitPrice < item.baseUnitPrice) {
                Text(
                    text = formatRupiah(item.baseUnitPrice),
                    fontSize = 8.sp,
                    color = Color.Gray,
                    style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                )
                Text(
                    text = formatRupiah(item.unitPrice),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RetailRedPrimary
                )
            } else {
                Text(
                    text = formatRupiah(item.unitPrice),
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // TOTAL
        Column(
            modifier = Modifier.weight(1.5f),
            horizontalAlignment = Alignment.End
        ) {
            if (item.isVoid) {
                Text("Rp 0", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.Gray)
            } else if (item.promoDiscountAmount > 0) {
                Text(
                    text = formatRupiah(item.baseUnitPrice * item.qty),
                    fontSize = 8.sp,
                    color = Color.Gray,
                    style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                )
                Text(
                    text = formatRupiah(item.subtotal),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = RetailGreen
                )
            } else {
                Text(
                    text = formatRupiah(item.subtotal),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        // AKSI (HAPUS PRODUK)
        Box(
            modifier = Modifier.weight(0.5f),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onDeleteItem,
                modifier = Modifier.size(26.dp).testTag("btn_delete_cart_item_$index")
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Hapus Produk",
                    tint = RetailRedPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun FunctionKeysBar(
    pendingCount: Int,
    onF1Help: () -> Unit,
    onF2Cari: () -> Unit,
    onF3Member: () -> Unit,
    onF4Batal: () -> Unit,
    onF5Pending: () -> Unit,
    onF6Recall: () -> Unit,
    onF7Retur: () -> Unit,
    onF8Void: () -> Unit,
    onF10Menu: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FKeyButton("F1 Bantuan", Color(0xFF455A64), onF1Help)
        FKeyButton("F2 Cari", RetailNavy, onF2Cari)
        FKeyButton("F3 Member", RetailGreen, onF3Member)
        FKeyButton("F4 Batal", Color(0xFF78909C), onF4Batal)
        FKeyButton("F5 Pending", RetailAmber, onF5Pending)
        FKeyButton(if (pendingCount > 0) "F6 Recall ($pendingCount)" else "F6 Recall", if (pendingCount > 0) RetailRedPrimary else Color(0xFF00ACC1), onF6Recall)
        FKeyButton("F7 Retur", Color(0xFFD84315), onF7Retur)
        FKeyButton("F8 Void", RetailRedDark, onF8Void)
        FKeyButton("F10 SIS", Color(0xFF37474F), onF10Menu)
    }
}

@Composable
fun FKeyButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun TerminalHeaderBar(
    cashier: CashierUserEntity? = null,
    bonNo: String,
    member: MemberEntity?,
    onMemberClick: () -> Unit,
    isShiftOpen: Boolean = true,
    shiftNumber: Int = 1,
    shiftModal: Double = 100000.0,
    isCompact: Boolean = false,
    memberRemainingPiutang: Double = 0.0,
    onShiftClick: () -> Unit = {}
) {
    Surface(
        color = RetailNavy,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = if (isCompact) 4.dp else 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val shiftClickModifier = if (!isShiftOpen) Modifier.clickable { onShiftClick() } else Modifier
            Column(
                modifier = shiftClickModifier
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "BON: $bonNo",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 11.sp else 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (isShiftOpen) RetailGreen else Color(0xFFD32F2F),
                        shape = RoundedCornerShape(3.dp),
                        modifier = shiftClickModifier
                    ) {
                        Text(
                            if (isShiftOpen) "SHIFT $shiftNumber (AKTIF)" else "SHIFT TERTUTUP (BUKA)",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    if (isShiftOpen) "Kasir: ${cashier?.nik ?: "-"} - ${cashier?.nama ?: "-"} • Modal: ${formatRupiah(shiftModal)}"
                    else "Kasir: ${cashier?.nik ?: "-"} - ${cashier?.nama ?: "-"} • [Shift Belum Dibuka]",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = if (isCompact) 10.sp else 11.sp
                )
            }
            Surface(
                color = if (member != null) {
                    if (memberRemainingPiutang > 0.0) Color(0xFFDC2626) else RetailGreen
                } else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { onMemberClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (memberRemainingPiutang > 0.0) Icons.Default.Warning else Icons.Default.CardMembership,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            if (member != null) "${member.nama} (${member.poin} Poin)" else "F3 + Member",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (member != null && memberRemainingPiutang > 0.0) {
                            Text(
                                "Piutang: ${formatRupiah(memberRemainingPiutang)}",
                                color = Color.Yellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemsTableHeader() {
    Surface(
        color = Color(0xFFECEFF1),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NO", modifier = Modifier.weight(0.35f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("PLU", modifier = Modifier.weight(0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("DESKRIPSI", modifier = Modifier.weight(2.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("QTY", modifier = Modifier.weight(2.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
            Text("HARGA", modifier = Modifier.weight(1.3f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color.Black)
            Text("TOTAL", modifier = Modifier.weight(1.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color.Black)
            Text("AKSI", modifier = Modifier.weight(0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
        }
    }
}

@Composable
fun PromoBannerSection(
    promoItems: List<CartItem> = emptyList()
) {
    if (promoItems.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_promo_banner"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            // Header: Promo Aktif
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocalOffer,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Promo Aktif",
                        fontSize = 12.sp,
                        color = Color(0xFFBF360C),
                        fontWeight = FontWeight.Black
                    )
                }
                Surface(
                    color = Color(0xFFFFE082),
                    shape = RoundedCornerShape(3.dp)
                ) {
                    Text(
                        "${promoItems.size} Item Promo",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Auto-generated promo details per item
            promoItems.forEach { item ->
                val prod = item.product
                val promoDesc = when {
                    !item.isPromoEligibleForUnit -> {
                        "Promo '${prod.getEffectivePromoLabel()}' khusus ecer (${prod.satuan}) • Satuan: ${item.selectedSatuan} (Tidak Terpotong)"
                    }
                    prod.promoType == "BELI_X_GRATIS_Y" -> {
                        if (item.qty >= prod.promoMinBeli) {
                            val freeUnits = (item.qty / prod.promoMinBeli) * maxOf(1, prod.promoGratisQty)
                            "Beli ${prod.promoMinBeli} Gratis ${prod.promoGratisQty} -> Dapat $freeUnits GRATIS! (Hemat ${formatRupiah(item.promoDiscountAmount)})"
                        } else {
                            val needed = prod.promoMinBeli - item.qty
                            "Beli ${prod.promoMinBeli} Gratis ${prod.promoGratisQty} -> Beli $needed lagi untuk dapat GRATIS ${prod.promoGratisQty}!"
                        }
                    }
                    prod.promoType == "BELI_X_POTONGAN" -> {
                        if (item.qty >= prod.promoMinBeli) {
                            "Beli ${prod.promoMinBeli} Potongan ${formatRupiah(prod.promoPotonganNominal)} -> Hemat ${formatRupiah(item.promoDiscountAmount)}"
                        } else {
                            val needed = prod.promoMinBeli - item.qty
                            "Beli ${prod.promoMinBeli} Potongan ${formatRupiah(prod.promoPotonganNominal)} -> Beli $needed lagi untuk potongan harga!"
                        }
                    }
                    prod.promoType == "DISKON_PERSEN" -> {
                        val diskon = if (prod.diskonPersen > 0) prod.diskonPersen.toInt() else 0
                        "Diskon $diskon% -> Harga promo ${formatRupiah(item.unitPrice)} (Hemat ${formatRupiah(item.promoDiscountAmount)})"
                    }
                    prod.promoType == "DISKON_NOMINAL" -> {
                        "Diskon ${formatRupiah(prod.diskonNominal)}/item -> Harga promo ${formatRupiah(item.unitPrice)} (Hemat ${formatRupiah(item.promoDiscountAmount)})"
                    }
                    prod.promoType == "GROSIR" || (prod.hargaGrosir > 0 && prod.hargaGrosirQty > 0) -> {
                        if (item.qty >= prod.hargaGrosirQty) {
                            "Harga Grosir Beli >= ${prod.hargaGrosirQty} -> ${formatRupiah(prod.hargaGrosir)}/item (Hemat ${formatRupiah(item.promoDiscountAmount)})"
                        } else {
                            val needed = prod.hargaGrosirQty - item.qty
                            "Harga Grosir Beli >= ${prod.hargaGrosirQty} -> Beli $needed lagi untuk dapat Harga ${formatRupiah(prod.hargaGrosir)}"
                        }
                    }
                    item.promoStatusText.isNotBlank() -> item.promoStatusText
                    prod.promoKeterangan.isNotBlank() -> prod.promoKeterangan
                    else -> if (prod.promoLabel.isNotBlank()) prod.promoLabel else "Promo Diskon Aktif"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "• ${prod.nama}: ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        promoDesc,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isPromoActive) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }
        }
    }
}

@Composable
fun TotalBannerDisplay(
    grandTotal: Double,
    isCompact: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RetailNavy),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isCompact) 8.dp else 12.dp)
        ) {
            Text(
                "TOTAL BELANJA:",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                formatRupiah(grandTotal),
                color = Color.White,
                fontSize = if (isCompact) 22.sp else 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BarcodeSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenScanner: () -> Unit,
    onCameraScan: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().testTag("barcode_search_bar"),
        placeholder = { Text("Ketik PLU / Barcode / Tekan + Bayar", fontSize = 12.sp, color = Color.Black) },
        leadingIcon = {
            IconButton(onClick = onOpenScanner) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Daftar Produk (LOV)", tint = RetailRedPrimary)
            }
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCameraScan,
                    modifier = Modifier.testTag("btn_camera_barcode_scanner")
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Scan Barcode Kamera",
                        tint = RetailRedPrimary
                    )
                }
                IconButton(
                    onClick = onSubmit,
                    modifier = Modifier.testTag("btn_barcode_enter")
                ) {
                    Icon(
                        Icons.Default.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = RetailNavy
                    )
                }
            }
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RetailRedPrimary,
            unfocusedBorderColor = Color(0xFF94A3B8),
            focusedTextColor = Color(0xFF0F172A),
            unfocusedTextColor = Color(0xFF0F172A),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor = RetailRedPrimary,
            focusedPlaceholderColor = Color(0xFF475569),
            unfocusedPlaceholderColor = Color(0xFF64748B)
        ),
        shape = RoundedCornerShape(6.dp)
    )
}

@Composable
fun NumericKeypadCard(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPlusCashend: () -> Unit,
    onEnter: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            val rows = listOf(
                listOf("7", "8", "9", "C"),
                listOf("4", "5", "6", "⌫"),
                listOf("1", "2", "3", "00"),
                listOf("0", "000", "+", "BAYAR")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isBayar = key == "BAYAR" || key == "+"
                        val isSpecial = key == "C" || key == "⌫"
                        Button(
                            onClick = {
                                when (key) {
                                    "C" -> onClear()
                                    "⌫" -> onBackspace()
                                    "+", "BAYAR" -> {
                                        if (onEnter != null) onEnter() else onPlusCashend()
                                    }
                                    else -> onDigit(key)
                                }
                            },
                            modifier = Modifier
                                .weight(if (key == "BAYAR") 1.2f else 1f)
                                .height(46.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isBayar -> RetailRedPrimary
                                    isSpecial -> Color(0xFF90A4AE)
                                    else -> Color(0xFFF5F5F5)
                                },
                                contentColor = if (isBayar || isSpecial) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                key,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (key.length > 2) 11.sp else 16.sp,
                                color = if (isBayar || isSpecial) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}
