package com.example.ui.screens.sis

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.StoreConfigEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.BluetoothPrinterHelper
import com.example.util.ReceiptFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val storeConfig by viewModel.storeConfig.collectAsState()
    val cfg = storeConfig ?: return

    var selectedTab by remember { mutableIntStateOf(0) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    var saveStatusMsg by remember { mutableStateOf("") }
    var hardwareActionMsg by remember { mutableStateOf("") }
    var isHardwareTesting by remember { mutableStateOf(false) }

    // --- State variables synced with StoreConfigEntity ---
    var receiptModel by remember(cfg) { mutableStateOf(cfg.receiptModel) }
    var paperSize by remember(cfg) { mutableStateOf(cfg.printerPaperSize) }
    var printerType by remember(cfg) { mutableStateOf(cfg.printerType) }
    var btName by remember(cfg) { mutableStateOf(cfg.bluetoothPrinterName) }
    var btMac by remember(cfg) { mutableStateOf(cfg.bluetoothPrinterMac) }
    var ipAddress by remember(cfg) { mutableStateOf(cfg.printerIpAddress) }
    var port by remember(cfg) { mutableStateOf(cfg.printerPort.toString()) }
    var autoPrint by remember(cfg) { mutableStateOf(cfg.autoPrintReceipt) }
    var autoCut by remember(cfg) { mutableStateOf(cfg.printerAutoCut) }
    var openCashDrawer by remember(cfg) { mutableStateOf(cfg.printerOpenCashDrawer) }
    var printCopies by remember(cfg) { mutableIntStateOf(cfg.printerPrintCopies) }
    var feedLines by remember(cfg) { mutableIntStateOf(cfg.printerFeedLines) }
    var fontSize by remember(cfg) { mutableStateOf(cfg.printerFontSize) }

    // Customization Toggles
    var showStoreName by remember(cfg) { mutableStateOf(cfg.receiptShowStoreName) }
    var showAlamat by remember(cfg) { mutableStateOf(cfg.receiptShowAlamat) }
    var showTelp by remember(cfg) { mutableStateOf(cfg.receiptShowTelp) }
    var showNpwp by remember(cfg) { mutableStateOf(cfg.receiptShowNpwp) }
    var customGreeting by remember(cfg) { mutableStateOf(cfg.receiptCustomGreeting) }
    var showBonNo by remember(cfg) { mutableStateOf(cfg.receiptShowBonNo) }
    var showKasir by remember(cfg) { mutableStateOf(cfg.receiptShowKasir) }
    var showDateTime by remember(cfg) { mutableStateOf(cfg.receiptShowDateTime) }
    var showMemberInfo by remember(cfg) { mutableStateOf(cfg.receiptShowMemberInfo) }
    var showEarnedPoints by remember(cfg) { mutableStateOf(cfg.receiptShowEarnedPoints) }
    var showPlu by remember(cfg) { mutableStateOf(cfg.receiptShowPlu) }
    var showDiskon by remember(cfg) { mutableStateOf(cfg.receiptShowDiskon) }
    var showTotalItemQty by remember(cfg) { mutableStateOf(cfg.receiptShowTotalItemQty) }
    var showPpn by remember(cfg) { mutableStateOf(cfg.receiptShowPpn) }
    var showPaymentDetail by remember(cfg) { mutableStateOf(cfg.receiptShowPaymentDetail) }
    var showHemat by remember(cfg) { mutableStateOf(cfg.receiptShowHemat) }
    var showVoucher by remember(cfg) { mutableStateOf(cfg.receiptShowVoucher) }
    var showBarcode by remember(cfg) { mutableStateOf(cfg.receiptShowBarcodeSimulation) }
    var footerMsg by remember(cfg) { mutableStateOf(cfg.footerMsg) }
    var policyMsg by remember(cfg) { mutableStateOf(cfg.receiptPolicyMsg) }
    var wifiInfo by remember(cfg) { mutableStateOf(cfg.receiptWifiInfo) }

    // Temporary active configuration used for real-time live preview
    val activeConfig = remember(
        cfg, receiptModel, paperSize, printerType, btName, btMac, ipAddress, port,
        autoPrint, autoCut, openCashDrawer, printCopies, feedLines, fontSize,
        showStoreName, showAlamat, showTelp, showNpwp, customGreeting, showBonNo, showKasir,
        showDateTime, showMemberInfo, showEarnedPoints, showPlu, showDiskon, showTotalItemQty,
        showPpn, showPaymentDetail, showHemat, showVoucher, showBarcode, footerMsg, policyMsg, wifiInfo
    ) {
        cfg.copy(
            receiptModel = receiptModel,
            printerPaperSize = paperSize,
            printerType = printerType,
            bluetoothPrinterName = btName,
            bluetoothPrinterMac = btMac,
            printerIpAddress = ipAddress,
            printerPort = port.toIntOrNull() ?: 9100,
            autoPrintReceipt = autoPrint,
            printerAutoCut = autoCut,
            printerOpenCashDrawer = openCashDrawer,
            printerPrintCopies = printCopies,
            printerFeedLines = feedLines,
            printerFontSize = fontSize,
            receiptShowStoreName = showStoreName,
            receiptShowAlamat = showAlamat,
            receiptShowTelp = showTelp,
            receiptShowNpwp = showNpwp,
            receiptCustomGreeting = customGreeting,
            receiptShowBonNo = showBonNo,
            receiptShowKasir = showKasir,
            receiptShowDateTime = showDateTime,
            receiptShowMemberInfo = showMemberInfo,
            receiptShowEarnedPoints = showEarnedPoints,
            receiptShowPlu = showPlu,
            receiptShowDiskon = showDiskon,
            receiptShowTotalItemQty = showTotalItemQty,
            receiptShowPpn = showPpn,
            receiptShowPaymentDetail = showPaymentDetail,
            receiptShowHemat = showHemat,
            receiptShowVoucher = showVoucher,
            receiptShowBarcodeSimulation = showBarcode,
            footerMsg = footerMsg,
            receiptPolicyMsg = policyMsg,
            receiptWifiInfo = wifiInfo
        )
    }

    // Generated receipt text preview
    val sampleReceiptText = remember(activeConfig) {
        ReceiptFormatter.formatSampleReceipt(activeConfig)
    }

    fun saveConfig() {
        viewModel.updateStoreConfig(activeConfig)
        saveStatusMsg = "✓ Pengaturan printer & model struk berhasil disimpan!"
        Toast.makeText(context, "Pengaturan Printer & Struk Berhasil Disimpan", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Column {
                        Text("Pengaturan Printer & Struk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Kustomisasi Model Struk & Hardware Thermal", fontSize = 11.sp, color = Color(0xFFA5D6A7))
                    }
                },
                actions = {
                    Button(
                        onClick = { saveConfig() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color(0xFF1B5E20)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SIMPAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PosSurface)
        ) {
            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("1. Model Struk", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("2. Kustomisasi Isi", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("3. Hardware Printer", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("4. Live Pratinjau", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            if (saveStatusMsg.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF81C784)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(saveStatusMsg, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            when (selectedTab) {
                0 -> ModelStrukTab(
                    currentModel = receiptModel,
                    onModelSelected = { receiptModel = it },
                    paperSize = paperSize,
                    onPaperSizeChanged = { paperSize = it },
                    fontSize = fontSize,
                    onFontSizeChanged = { fontSize = it },
                    sampleReceiptText = sampleReceiptText,
                    onSwitchToPreview = { selectedTab = 3 }
                )
                1 -> KustomisasiIsiTab(
                    showStoreName = showStoreName, onShowStoreNameChange = { showStoreName = it },
                    showAlamat = showAlamat, onShowAlamatChange = { showAlamat = it },
                    showTelp = showTelp, onShowTelpChange = { showTelp = it },
                    showNpwp = showNpwp, onShowNpwpChange = { showNpwp = it },
                    customGreeting = customGreeting, onCustomGreetingChange = { customGreeting = it },
                    showBonNo = showBonNo, onShowBonNoChange = { showBonNo = it },
                    showKasir = showKasir, onShowKasirChange = { showKasir = it },
                    showDateTime = showDateTime, onShowDateTimeChange = { showDateTime = it },
                    showMemberInfo = showMemberInfo, onShowMemberInfoChange = { showMemberInfo = it },
                    showEarnedPoints = showEarnedPoints, onShowEarnedPointsChange = { showEarnedPoints = it },
                    showPlu = showPlu, onShowPluChange = { showPlu = it },
                    showDiskon = showDiskon, onShowDiskonChange = { showDiskon = it },
                    showTotalItemQty = showTotalItemQty, onShowTotalItemQtyChange = { showTotalItemQty = it },
                    showPpn = showPpn, onShowPpnChange = { showPpn = it },
                    showPaymentDetail = showPaymentDetail, onShowPaymentDetailChange = { showPaymentDetail = it },
                    showHemat = showHemat, onShowHematChange = { showHemat = it },
                    showVoucher = showVoucher, onShowVoucherChange = { showVoucher = it },
                    showBarcode = showBarcode, onShowBarcodeChange = { showBarcode = it },
                    footerMsg = footerMsg, onFooterMsgChange = { footerMsg = it },
                    policyMsg = policyMsg, onPolicyMsgChange = { policyMsg = it },
                    wifiInfo = wifiInfo, onWifiInfoChange = { wifiInfo = it }
                )
                2 -> HardwarePrinterTab(
                    printerType = printerType, onPrinterTypeChange = { printerType = it },
                    paperSize = paperSize, onPaperSizeChange = { paperSize = it },
                    btName = btName, onBtNameChange = { btName = it },
                    btMac = btMac, onBtMacChange = { btMac = it },
                    ipAddress = ipAddress, onIpAddressChange = { ipAddress = it },
                    port = port, onPortChange = { port = it },
                    autoPrint = autoPrint, onAutoPrintChange = { autoPrint = it },
                    autoCut = autoCut, onAutoCutChange = { autoCut = it },
                    openCashDrawer = openCashDrawer, onOpenCashDrawerChange = { openCashDrawer = it },
                    printCopies = printCopies, onPrintCopiesChange = { printCopies = it },
                    feedLines = feedLines, onFeedLinesChange = { feedLines = it },
                    onOpenBluetoothPicker = { showBluetoothPicker = true },
                    onTestCashDrawer = {
                        coroutineScope.launch {
                            isHardwareTesting = true
                            hardwareActionMsg = "Mengirim sinyal buka laci kasir (ESC p)..."
                            val res = BluetoothPrinterHelper.kickCashDrawer(btMac)
                            hardwareActionMsg = if (res.first) "✓ Sinyal laci kasir terkirim!" else "Pemberitahuan: ${res.second}"
                            isHardwareTesting = false
                        }
                    },
                    onTestPaperCut = {
                        coroutineScope.launch {
                            isHardwareTesting = true
                            hardwareActionMsg = "Mengirim sinyal potong kertas thermal..."
                            val res = BluetoothPrinterHelper.cutPaper(btMac)
                            hardwareActionMsg = if (res.first) "✓ Sinyal potong kertas terkirim!" else "Pemberitahuan: ${res.second}"
                            isHardwareTesting = false
                        }
                    },
                    isHardwareTesting = isHardwareTesting,
                    hardwareActionMsg = hardwareActionMsg
                )
                3 -> LivePratinjauTab(
                    activeConfig = activeConfig,
                    sampleReceiptText = sampleReceiptText,
                    onSave = { saveConfig() },
                    onPrintSample = {
                        coroutineScope.launch {
                            if (activeConfig.printerType == "BLUETOOTH" && activeConfig.bluetoothPrinterMac.isNotBlank()) {
                                Toast.makeText(context, "Mencetak ke printer Bluetooth...", Toast.LENGTH_SHORT).show()
                                val escPosBytes = ReceiptFormatter.generateEscPos(sampleReceiptText, activeConfig)
                                val result = BluetoothPrinterHelper.printOverBluetooth(activeConfig.bluetoothPrinterMac, escPosBytes)
                                if (result.first) {
                                    Toast.makeText(context, "✓ Berhasil mencetak ke printer!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Fallback to share/save document if hardware socket fails
                                    Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                                    BluetoothPrinterHelper.shareReceiptText(context, sampleReceiptText, "Sample Struk DodolanPro")
                                }
                            } else {
                                val res = BluetoothPrinterHelper.shareReceiptText(context, sampleReceiptText, "Sample Struk DodolanPro")
                                Toast.makeText(context, res.second, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onShareSample = {
                        BluetoothPrinterHelper.shareReceiptText(context, sampleReceiptText, "Sample Struk DodolanPro")
                    },
                    onCopyText = {
                        clipboardManager.setText(AnnotatedString(sampleReceiptText))
                        Toast.makeText(context, "Teks struk berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Modal dialog to discover & choose paired Bluetooth printers
    if (showBluetoothPicker) {
        BluetoothDevicePickerDialog(
            onDismiss = { showBluetoothPicker = false },
            onDeviceSelected = { name, mac ->
                btName = name
                btMac = mac
                showBluetoothPicker = false
                Toast.makeText(context, "Printer $name ($mac) dipilih", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =====================================================================
// TAB 1: PILIHAN MODEL STRUK
// =====================================================================
@Composable
private fun ModelStrukTab(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    paperSize: String,
    onPaperSizeChanged: (String) -> Unit,
    fontSize: String,
    onFontSizeChanged: (String) -> Unit,
    sampleReceiptText: String,
    onSwitchToPreview: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF1B5E20))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PILIHAN MODEL TAMPILAN STRUK", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                    }
                    Text(
                        "Pilih model tata letak struk yang sesuai dengan jenis bisnis Anda. Setiap model dirancang profesional dan dapat dikustomisasi lebih lanjut.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    ReceiptFormatter.AVAILABLE_MODELS.forEach { modelInfo ->
                        val isSelected = currentModel == modelInfo.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onModelSelected(modelInfo.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFE8F5E9) else Color(0xFFF8FAFC)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE2E8F0)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onModelSelected(modelInfo.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2E7D32))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = modelInfo.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF1B5E20) else Color(0xFF1E293B)
                                        )
                                        if (modelInfo.id == ReceiptFormatter.MODEL_STANDARD_RETAIL) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0xFFFFE082),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    "Rekomendasi",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFE65100),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = modelInfo.subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Ukuran Kertas & Karakter
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("FORMAT KERTAS & DENSITAS CETAK", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ukuran Kertas Thermal:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("58mm (32 kolom) atau 80mm (48 kolom)", fontSize = 10.sp, color = Color.Gray)
                        }
                        Row {
                            FilterChip(
                                selected = paperSize == "58mm",
                                onClick = { onPaperSizeChanged("58mm") },
                                label = { Text("58 mm (Mini/Portable)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC8E6C9),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = paperSize == "80mm",
                                onClick = { onPaperSizeChanged("80mm") },
                                label = { Text("80 mm (Lebar)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC8E6C9),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                )
                            )
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Ukuran Karakter / Font:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Font A (Normal) atau Font B (Rapat/Kecil)", fontSize = 10.sp, color = Color.Gray)
                        }
                        Row {
                            FilterChip(
                                selected = fontSize == "NORMAL",
                                onClick = { onFontSizeChanged("NORMAL") },
                                label = { Text("Normal (Font A)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC8E6C9),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = fontSize == "CONDENSED",
                                onClick = { onFontSizeChanged("CONDENSED") },
                                label = { Text("Condensed (Font B)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC8E6C9),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Mini preview widget
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cuplikan Model Struk Terpilih", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF334155))
                        }
                        TextButton(onClick = onSwitchToPreview) {
                            Text("Lihat Layar Penuh →", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    text = sampleReceiptText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// TAB 2: KUSTOMISASI ISI & ELEMEN STRUK
// =====================================================================
@Composable
private fun KustomisasiIsiTab(
    showStoreName: Boolean, onShowStoreNameChange: (Boolean) -> Unit,
    showAlamat: Boolean, onShowAlamatChange: (Boolean) -> Unit,
    showTelp: Boolean, onShowTelpChange: (Boolean) -> Unit,
    showNpwp: Boolean, onShowNpwpChange: (Boolean) -> Unit,
    customGreeting: String, onCustomGreetingChange: (String) -> Unit,
    showBonNo: Boolean, onShowBonNoChange: (Boolean) -> Unit,
    showKasir: Boolean, onShowKasirChange: (Boolean) -> Unit,
    showDateTime: Boolean, onShowDateTimeChange: (Boolean) -> Unit,
    showMemberInfo: Boolean, onShowMemberInfoChange: (Boolean) -> Unit,
    showEarnedPoints: Boolean, onShowEarnedPointsChange: (Boolean) -> Unit,
    showPlu: Boolean, onShowPluChange: (Boolean) -> Unit,
    showDiskon: Boolean, onShowDiskonChange: (Boolean) -> Unit,
    showTotalItemQty: Boolean, onShowTotalItemQtyChange: (Boolean) -> Unit,
    showPpn: Boolean, onShowPpnChange: (Boolean) -> Unit,
    showPaymentDetail: Boolean, onShowPaymentDetailChange: (Boolean) -> Unit,
    showHemat: Boolean, onShowHematChange: (Boolean) -> Unit,
    showVoucher: Boolean, onShowVoucherChange: (Boolean) -> Unit,
    showBarcode: Boolean, onShowBarcodeChange: (Boolean) -> Unit,
    footerMsg: String, onFooterMsgChange: (String) -> Unit,
    policyMsg: String, onPolicyMsgChange: (String) -> Unit,
    wifiInfo: String, onWifiInfoChange: (String) -> Unit
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedLabelColor = Color(0xFF1B5E20),
        unfocusedLabelColor = Color(0xFF475569),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedBorderColor = Color(0xFF2E7D32),
        unfocusedBorderColor = Color(0xFFCBD5E1)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Toko
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("BAGIAN HEADER & IDENTITAS TOKO", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    ReceiptToggleRow("Tampilkan Nama Toko", showStoreName, onShowStoreNameChange)
                    ReceiptToggleRow("Tampilkan Alamat Toko", showAlamat, onShowAlamatChange)
                    ReceiptToggleRow("Tampilkan Nomor Telepon / WA Toko", showTelp, onShowTelpChange)
                    ReceiptToggleRow("Tampilkan NPWP / NIB Toko", showNpwp, onShowNpwpChange)

                    OutlinedTextField(
                        value = customGreeting,
                        onValueChange = onCustomGreetingChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pesan Slogan / Sambutan Header (Opsional)") },
                        placeholder = { Text("Contoh: Selamat Datang di DodolanPro!") },
                        singleLine = true,
                        colors = textFieldColors
                    )
                }
            }
        }

        // 2. Data Transaksi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATA TRANSAKSI & PELANGGAN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    ReceiptToggleRow("Tampilkan Nomor Bon Transaksi", showBonNo, onShowBonNoChange)
                    ReceiptToggleRow("Tampilkan Nama Kasir Bertugas", showKasir, onShowKasirChange)
                    ReceiptToggleRow("Tampilkan Tanggal & Waktu Cetak", showDateTime, onShowDateTimeChange)
                    ReceiptToggleRow("Tampilkan Nomor & Nama Member", showMemberInfo, onShowMemberInfoChange)
                    ReceiptToggleRow("Tampilkan Poin Reward Didapat Transaksi Ini", showEarnedPoints, onShowEarnedPointsChange)
                }
            }
        }

        // 3. Detail Item & Pembayaran
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DETAIL BARANG & PERHITUNGAN", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    ReceiptToggleRow("Tampilkan Kode PLU / Barcode Produk", showPlu, onShowPluChange)
                    ReceiptToggleRow("Tampilkan Rincian Diskon Promo per Barang", showDiskon, onShowDiskonChange)
                    ReceiptToggleRow("Tampilkan Total Item & Total Kuantitas (Qty)", showTotalItemQty, onShowTotalItemQtyChange)
                    ReceiptToggleRow("Tampilkan Rincian Pajak (PPN 11% / PB1)", showPpn, onShowPpnChange)
                    ReceiptToggleRow("Tampilkan Detail Pembayaran (Tunai, Non-Tunai, Kembali)", showPaymentDetail, onShowPaymentDetailChange)
                    ReceiptToggleRow("Tampilkan Penghematan Belanja ('ANDA HEMAT')", showHemat, onShowHematChange)
                }
            }
        }

        // 4. Promo, Footer & Media Sosial
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("PROMO, FOOTER & SOSIAL MEDIA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    ReceiptToggleRow("Tampilkan Kupon Voucher Belanja di Struk", showVoucher, onShowVoucherChange)
                    ReceiptToggleRow("Tampilkan Barcode Simulasi No Bon di Bawah", showBarcode, onShowBarcodeChange)

                    OutlinedTextField(
                        value = footerMsg,
                        onValueChange = onFooterMsgChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ucapan Terima Kasih (Footer Struk)") },
                        maxLines = 2,
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = policyMsg,
                        onValueChange = onPolicyMsgChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kebijakan Retur / Penukaran Barang") },
                        maxLines = 2,
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = wifiInfo,
                        onValueChange = onWifiInfoChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Info Free Wi-Fi / Akun Sosmed Toko") },
                        placeholder = { Text("Contoh: Wi-Fi: Toko_Guest | IG: @dodolanpro") },
                        maxLines = 2,
                        colors = textFieldColors
                    )
                }
            }
        }
    }
}

// =====================================================================
// TAB 3: HARDWARE PRINTER & KONEKSI
// =====================================================================
@Composable
private fun HardwarePrinterTab(
    printerType: String, onPrinterTypeChange: (String) -> Unit,
    paperSize: String, onPaperSizeChange: (String) -> Unit,
    btName: String, onBtNameChange: (String) -> Unit,
    btMac: String, onBtMacChange: (String) -> Unit,
    ipAddress: String, onIpAddressChange: (String) -> Unit,
    port: String, onPortChange: (String) -> Unit,
    autoPrint: Boolean, onAutoPrintChange: (Boolean) -> Unit,
    autoCut: Boolean, onAutoCutChange: (Boolean) -> Unit,
    openCashDrawer: Boolean, onOpenCashDrawerChange: (Boolean) -> Unit,
    printCopies: Int, onPrintCopiesChange: (Int) -> Unit,
    feedLines: Int, onFeedLinesChange: (Int) -> Unit,
    onOpenBluetoothPicker: () -> Unit,
    onTestCashDrawer: () -> Unit,
    onTestPaperCut: () -> Unit,
    isHardwareTesting: Boolean,
    hardwareActionMsg: String
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        focusedLabelColor = Color(0xFF1B5E20),
        unfocusedLabelColor = Color(0xFF475569),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color(0xFFF8FAFC),
        focusedBorderColor = Color(0xFF2E7D32),
        unfocusedBorderColor = Color(0xFFCBD5E1)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tipe Koneksi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TIPE KONEKSI PRINTER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = printerType == "BLUETOOTH",
                            onClick = { onPrinterTypeChange("BLUETOOTH") },
                            label = { Text("Bluetooth Thermal") },
                            leadingIcon = { Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                        )
                        FilterChip(
                            selected = printerType == "USB",
                            onClick = { onPrinterTypeChange("USB") },
                            label = { Text("USB / OTG Direct") },
                            leadingIcon = { Icon(Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                        )
                        FilterChip(
                            selected = printerType == "WIFI_LAN",
                            onClick = { onPrinterTypeChange("WIFI_LAN") },
                            label = { Text("Wi-Fi / LAN Network") },
                            leadingIcon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                        )
                        FilterChip(
                            selected = printerType == "SYSTEM_PRINT",
                            onClick = { onPrinterTypeChange("SYSTEM_PRINT") },
                            label = { Text("System Print / PDF") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                        )
                    }

                    if (printerType == "BLUETOOTH") {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Perangkat Bluetooth Terpilih:", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                        Text(
                                            if (btName.isNotBlank()) "$btName ($btMac)" else "Belum ada printer yang dipilih",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (btName.isNotBlank()) Color(0xFF1B5E20) else Color(0xFFD32F2F)
                                        )
                                    }
                                    Button(
                                        onClick = onOpenBluetoothPicker,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PILIH PRINTER", fontSize = 11.sp)
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = btName,
                                        onValueChange = onBtNameChange,
                                        label = { Text("Nama Printer") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = textFieldColors
                                    )
                                    OutlinedTextField(
                                        value = btMac,
                                        onValueChange = onBtMacChange,
                                        label = { Text("MAC Address") },
                                        placeholder = { Text("00:11:22:33:44:55") },
                                        modifier = Modifier.weight(1.2f),
                                        singleLine = true,
                                        colors = textFieldColors
                                    )
                                }
                            }
                        }
                    } else if (printerType == "WIFI_LAN") {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = ipAddress,
                                    onValueChange = onIpAddressChange,
                                    label = { Text("IP Address Printer") },
                                    modifier = Modifier.weight(2f),
                                    singleLine = true,
                                    colors = textFieldColors
                                )
                                OutlinedTextField(
                                    value = port,
                                    onValueChange = onPortChange,
                                    label = { Text("Port (ESC/POS)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = textFieldColors
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fitur Hardware & Otomatisasi
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, PosCardBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("OTOMATISASI & KONTROL HARDWARE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))

                    ReceiptToggleRow("Cetak Otomatis Setelah Pembayaran Kasir", autoPrint, onAutoPrintChange)
                    ReceiptToggleRow("Buka Laci Uang Kasir (Cash Drawer RJ11 via ESC/POS)", openCashDrawer, onOpenCashDrawerChange)
                    ReceiptToggleRow("Perintah Potong Kertas Otomatis (Auto-Cut ESC/POS)", autoCut, onAutoCutChange)

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Rangkap Cetak (Copies):", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = printCopies == 1,
                                onClick = { onPrintCopiesChange(1) },
                                label = { Text("1 Rangkap") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                            )
                            FilterChip(
                                selected = printCopies == 2,
                                onClick = { onPrintCopiesChange(2) },
                                label = { Text("2 Rangkap (Kasir+Toko)") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Jumlah Baris Feed Kertas Kosong:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 2, 3, 4, 5).forEach { lines ->
                                FilterChip(
                                    selected = feedLines == lines,
                                    onClick = { onFeedLinesChange(lines) },
                                    label = { Text("$lines") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9), selectedLabelColor = Color(0xFF1B5E20))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Tombol Uji Perangkat Keras
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Handyman, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PENGUJIAN SINYAL HARDWARE PRINTER", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB45309))
                    }
                    Text(
                        "Gunakan tombol di bawah untuk menguji respon fisik mesin printer kasir:",
                        fontSize = 11.sp,
                        color = Color(0xFF78350F)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTestCashDrawer,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isHardwareTesting
                        ) {
                            Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buka Laci Kasir", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onTestPaperCut,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isHardwareTesting
                        ) {
                            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Potong Kertas", fontSize = 11.sp)
                        }
                    }

                    if (hardwareActionMsg.isNotEmpty()) {
                        Text(hardwareActionMsg, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB45309))
                    }
                }
            }
        }
    }
}

// =====================================================================
// TAB 4: LIVE PRATINJAU STRUK REALISTIS (THERMAL PAPER SIMULATOR)
// =====================================================================
@Composable
private fun LivePratinjauTab(
    activeConfig: StoreConfigEntity,
    sampleReceiptText: String,
    onSave: () -> Unit,
    onPrintSample: () -> Unit,
    onShareSample: () -> Unit,
    onCopyText: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Status bar & Model Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color(0xFF2E7D32),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "MODEL: ${activeConfig.receiptModel}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "KERTAS: ${activeConfig.printerPaperSize}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = onCopyText, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin Teks", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onShareSample, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Thermal Receipt Paper Sheet Preview
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                item {
                    Text(
                        text = sampleReceiptText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = if (activeConfig.printerPaperSize == "80mm") 10.sp else 11.sp,
                        lineHeight = 15.sp,
                        color = Color(0xFF111827)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onPrintSample,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1B5E20))
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TES PRINT SAMPLE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20), contentColor = Color.White)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TERAPKAN & SIMPAN", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

// Reusable row for toggling receipt elements
@Composable
private fun ReceiptToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2E7D32)
            )
        )
    }
}

// Modal dialog for scanning and selecting paired Bluetooth thermal printers
@Composable
private fun BluetoothDevicePickerDialog(
    onDismiss: () -> Unit,
    onDeviceSelected: (name: String, mac: String) -> Unit
) {
    val context = LocalContext.current
    var pairedDevices by remember { mutableStateOf(BluetoothPrinterHelper.getPairedDevices(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFF1B5E20))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Printer Bluetooth", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Text(
                    "Daftar printer Bluetooth yang telah dipasangkan (Bonded/Paired) di perangkat ini. Pilih printer kasir Anda:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (pairedDevices.isEmpty()) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Belum ada printer Bluetooth yang terpasang di Android.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                "1. Hidupkan printer thermal Anda\n2. Buka Pengaturan Bluetooth HP/Tablet Anda\n3. Lakukan 'Pairing' dengan printer (PIN standar: 0000 atau 1234)\n4. Tekan 'Muat Ulang' di bawah.",
                                fontSize = 10.sp,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(pairedDevices.size) { index ->
                            val device = pairedDevices[index]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onDeviceSelected(device.first, device.second) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(device.first, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("MAC: ${device.second}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                                }
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = {
                        pairedDevices = BluetoothPrinterHelper.getPairedDevices(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Muat Ulang", fontSize = 11.sp)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Tutup", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
