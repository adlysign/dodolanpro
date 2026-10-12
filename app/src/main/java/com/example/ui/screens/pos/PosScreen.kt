package com.example.ui.screens.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
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

    // Shift and Lock States
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val isShiftOpen by viewModel.isShiftOpen.collectAsState()
    val activeShift by viewModel.activeShift.collectAsState()
    val shiftModalAwal by viewModel.shiftModalAwal.collectAsState()
    val isScreenLocked by viewModel.isScreenLocked.collectAsState()
    val allShiftConfigs by viewModel.allShiftConfigs.collectAsState()

    // Dialog states
    var showLovDialog by remember { mutableStateOf(false) }
    var showPendingLabelDialog by remember { mutableStateOf(false) }
    var showRecallDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showVoidDialog by remember { mutableStateOf(false) }
    var showReturDialog by remember { mutableStateOf(false) }
    var showMemberDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showBukaShiftDialog by remember { mutableStateOf(false) }
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedCartIndex by remember { mutableStateOf<Int?>(null) }
    var feedbackMessage by remember { mutableStateOf("") }

    val promoItemsInCart = remember(cartItems) {
        cartItems.filter { (!it.isVoid && !it.isRetur) && (it.product.isPromo || it.isPromoApplied || it.product.diskonPersen > 0) }
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
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                "DodolanPro",
                                fontWeight = FontWeight.Black,
                                color = Color.Black, // Warna teks hitam jika background putih
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
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

                            // Tombol Menu SIS (HANYA ICON)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.clickable { onNavigateTo(PosNavDestination.SIS_DASHBOARD) }
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
                            onShiftClick = { showBukaShiftDialog = true }
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
                                                onQtyDecrease = { viewModel.updateCartItemQty(index, item.qty - 1) }
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
                            onF2Cari = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else {
                                    showLovDialog = true
                                }
                            },
                            onF3Member = { showMemberDialog = true },
                            onF4Batal = { viewModel.clearCart() },
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
                            onF10Menu = { onNavigateTo(PosNavDestination.SIS_DASHBOARD) }
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
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else if (cartItems.isEmpty()) {
                                    feedbackMessage = "Keranjang belanja masih kosong!"
                                } else {
                                    val validity = viewModel.isReturValidForCheckout()
                                    if (!validity.first) {
                                        feedbackMessage = validity.second
                                    } else {
                                        showPaymentDialog = true
                                    }
                                }
                            },
                            onEnter = {
                                if (!isShiftOpen) {
                                    feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                    showBukaShiftDialog = true
                                } else if (barcodeText.isNotEmpty()) {
                                    viewModel.submitBarcodeOrPlu(barcodeText) {
                                        feedbackMessage = "Barang tidak ditemukan!"
                                    }
                                    barcodeText = ""
                                } else if (cartItems.isNotEmpty()) {
                                    showPaymentDialog = true
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
                        onShiftClick = { showBukaShiftDialog = true }
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
                                            onQtyDecrease = { viewModel.updateCartItemQty(index, item.qty - 1) }
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
                        onF2Cari = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Buka shift kasir terlebih dahulu!"
                                showBukaShiftDialog = true
                            } else showLovDialog = true
                        },
                        onF3Member = { showMemberDialog = true },
                        onF4Batal = { viewModel.clearCart() },
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
                        onF10Menu = { onNavigateTo(PosNavDestination.SIS_DASHBOARD) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            if (!isShiftOpen) {
                                feedbackMessage = "Shift belum dibuka! Harap buka shift kasir terlebih dahulu."
                                showBukaShiftDialog = true
                            } else if (cartItems.isEmpty()) {
                                feedbackMessage = "Keranjang belanja kosong!"
                            } else {
                                val validity = viewModel.isReturValidForCheckout()
                                if (!validity.first) feedbackMessage = validity.second
                                else showPaymentDialog = true
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
                } else {
                    viewModel.addProductToCart(prod, qty)
                    feedbackMessage = "${prod.nama} ($qty pcs) ditambahkan!"
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
            onDeleteCart = { id ->
                viewModel.deletePendingCart(id)
                feedbackMessage = "Transaksi pending dihapus!"
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
            onApplyMemberPoints = { viewModel.applyMemberPoints(it) },
            onApplyVoucher = { viewModel.applyVoucher(it) },
            onDismiss = { showPaymentDialog = false },
            onProcessPayment = { caraBayar, tunai, nonTunai, ref ->
                viewModel.processCheckout(caraBayar, tunai, nonTunai, ref) { ok, msg, tx ->
                    if (ok && tx != null) {
                        showPaymentDialog = false
                        showReceiptDialog = tx
                    } else {
                        feedbackMessage = msg
                    }
                }
            }
        )
    }

    if (showVoidDialog && selectedCartIndex != null && selectedCartIndex in cartItems.indices) {
        VoidItemDialog(
            item = cartItems[selectedCartIndex!!],
            onDismiss = { showVoidDialog = false },
            onConfirmVoid = { otp, reason ->
                viewModel.voidItem(selectedCartIndex!!, otp, reason) { ok, msg ->
                    feedbackMessage = msg
                    if (ok) showVoidDialog = false
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
                viewModel.submitBarcodeOrPlu(scannedCode) {
                    feedbackMessage = "Barang tidak ditemukan untuk barcode '$scannedCode'"
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
                        if (!isShiftOpen) {
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
            items = cartItems,
            storeConfig = storeConfig,
            onDismiss = { showReceiptDialog = null }
        )
    }
}

// --- High-Contrast Black Text Table Row as requested ---
@Composable
fun ItemTableRow(
    index: Int,
    item: CartItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onQtyIncrease: () -> Unit,
    onQtyDecrease: () -> Unit
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
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${index + 1}", modifier = Modifier.weight(0.5f), fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        Text(item.product.plu, modifier = Modifier.weight(1.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Column(modifier = Modifier.weight(3.5f)) {
            Text(
                item.product.nama,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = Color.Black // Wajib teks hitam pekat
            )
            if (item.isVoid) {
                Text("Item Void (${item.voidReason})", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else if (item.isRetur) {
                Text("RETUR KONSUMEN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else if (item.product.isPromo) {
                Text("PROMO", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Qty controls
        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!item.isVoid && !item.isRetur) {
                IconButton(onClick = onQtyDecrease, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(12.dp))
                }
                Text("${item.qty}", fontWeight = FontWeight.Black, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 2.dp), color = Color.Black)
                IconButton(onClick = onQtyIncrease, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            } else {
                Text(if (item.isRetur) "-${item.qty}" else "${item.qty}", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.Black)
            }
        }

        Text(
            formatRupiah(item.unitPrice),
            modifier = Modifier.weight(1.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Text(
            if (item.isVoid) "Rp 0" else formatRupiah(item.subtotal),
            modifier = Modifier.weight(1.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.End,
            color = Color.Black
        )
    }
}

@Composable
fun FunctionKeysBar(
    pendingCount: Int,
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
            Column(
                modifier = Modifier.clickable { onShiftClick() }
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
                        modifier = Modifier.clickable { onShiftClick() }
                    ) {
                        Text(
                            if (isShiftOpen) "SHIFT $shiftNumber" else "SHIFT TERTUTUP (BUKA)",
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
                color = if (member != null) RetailGreen else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.clickable { onMemberClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CardMembership,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (member != null) "${member.nama} (${member.poin} Poin)" else "F3 + Member",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
            Text("NO", modifier = Modifier.weight(0.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("PLU", modifier = Modifier.weight(1.3f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("DESKRIPSI PRODUK", modifier = Modifier.weight(3.5f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("QTY", modifier = Modifier.weight(1.4f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.Black)
            Text("HARGA", modifier = Modifier.weight(1.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color.Black)
            Text("TOTAL", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color.Black)
        }
    }
}

@Composable
fun PromoBannerSection(
    promoItems: List<CartItem> = emptyList()
) {
    if (promoItems.isEmpty()) return

    Surface(
        color = Color(0xFFFFFDE7),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFE082)),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().testTag("active_promo_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = RetailAmber, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            val promoTitles = promoItems.map { item ->
                val diskon = if (item.product.diskonPersen > 0) " (Diskon ${item.product.diskonPersen.toInt()}%)" else " (Promo)"
                "${item.product.nama}$diskon"
            }.distinct().joinToString(" • ")
            Column {
                Text("Promo Retail Aktif Keranjang:", fontSize = 10.sp, color = RetailAmber, fontWeight = FontWeight.Bold)
                Text(promoTitles, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.SemiBold, maxLines = 2)
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
            unfocusedBorderColor = PosCardBorder,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
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
