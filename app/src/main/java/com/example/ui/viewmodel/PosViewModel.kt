package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.PosRepository
import com.example.ui.screens.pos.formatRupiah
import com.example.util.DocExportManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class PosNavDestination {
    KASIR,
    EDIT_TOKO,
    CLEREK,
    PEMBELIAN,
    PENJUALAN,
    PRODUK,
    CETAK_LABEL_HARGA,
    STOK_OPNAME,
    RETUR_PEMBELIAN,
    EOD_TUTUP_TOKO,
    LAPORAN_TOKO,
    KARYAWAN,
    VOUCHER_POIN,
    PENGATURAN_MEMBER,
    PENGATURAN_SHIFT,
    PENGATURAN_PRINTER,
    BACKUP_RESTORE,
    SIS_DASHBOARD,
    SINKRONISASI_MULTI_DEVICE,
    PIUTANG
}

sealed class PosShortcutEvent {
    object F1_HELP : PosShortcutEvent()
    object F2_CARI : PosShortcutEvent()
    object F3_MEMBER : PosShortcutEvent()
    object F4_BATAL : PosShortcutEvent()
    object F5_PENDING : PosShortcutEvent()
    object F6_RECALL : PosShortcutEvent()
    object F7_RETUR : PosShortcutEvent()
    object F8_VOID : PosShortcutEvent()
    object F10_SIS : PosShortcutEvent()
    object F12_BAYAR : PosShortcutEvent()
    object ESCAPE : PosShortcutEvent()
    object DELETE_ITEM : PosShortcutEvent()
    object PLUS_PAY : PosShortcutEvent()
    object ARROW_UP : PosShortcutEvent()
    object ARROW_DOWN : PosShortcutEvent()
}

data class CartItem(
    val product: ProductEntity,
    val qty: Int = 1,
    val isGrosirApplied: Boolean = false,
    val isPromoApplied: Boolean = false,
    val customDiscount: Double = 0.0,
    val note: String = "",
    val isVoid: Boolean = false,
    val voidReason: String = "",
    val isRetur: Boolean = false,
    val selectedSatuan: String = product.satuan
) {
    val baseUnitPrice: Double
        get() {
            return when (selectedSatuan) {
                product.satuanRepack -> if (product.hargaRepack > 0) product.hargaRepack else product.hargaJual * maxOf(1, product.isiRepack)
                product.satuanRepack2 -> if (product.hargaRepack2 > 0) product.hargaRepack2 else product.hargaJual * maxOf(1, product.isiRepack2)
                else -> product.hargaJual
            }
        }

    val isPromoEligibleForUnit: Boolean
        get() = product.isPromoEligible(selectedSatuan)

    val promoDiscountAmount: Double
        get() {
            if (isVoid || isRetur) return 0.0

            val isBaseUnit = selectedSatuan.equals(product.satuan, ignoreCase = true)

            if (!product.isPromo && !product.hasDiscountOrPromo()) {
                if (isGrosirApplied && isBaseUnit && product.hargaGrosir > 0 && qty >= product.hargaGrosirQty) {
                    return qty * maxOf(0.0, baseUnitPrice - product.hargaGrosir)
                }
                return 0.0
            }

            if (!isPromoEligibleForUnit) {
                return 0.0
            }

            val kelipatan = product.promoBerlakuKelipatan

            return when (product.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (product.promoMinBeli > 0 && qty >= product.promoMinBeli) {
                        val freeUnits = if (kelipatan) {
                            (qty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
                        } else {
                            maxOf(1, product.promoGratisQty)
                        }
                        freeUnits * baseUnitPrice
                    } else 0.0
                }
                "BELI_X_POTONGAN" -> {
                    if (product.promoMinBeli > 0 && qty >= product.promoMinBeli && product.promoPotonganNominal > 0) {
                        val sets = if (kelipatan) {
                            qty / product.promoMinBeli
                        } else {
                            1
                        }
                        sets * product.promoPotonganNominal
                    } else 0.0
                }
                "DISKON_NOMINAL" -> {
                    if (product.diskonNominal > 0) {
                        if (kelipatan) {
                            qty * product.diskonNominal
                        } else {
                            product.diskonNominal
                        }
                    } else 0.0
                }
                "DISKON_PERSEN" -> {
                    if (product.diskonPersen > 0) {
                        if (kelipatan) {
                            qty * baseUnitPrice * (product.diskonPersen / 100.0)
                        } else {
                            baseUnitPrice * (product.diskonPersen / 100.0)
                        }
                    } else 0.0
                }
                "GROSIR" -> {
                    if (isBaseUnit && product.hargaGrosir > 0 && qty >= product.hargaGrosirQty) {
                        qty * maxOf(0.0, baseUnitPrice - product.hargaGrosir)
                    } else 0.0
                }
                else -> {
                    if (product.diskonPersen > 0) {
                        if (kelipatan) {
                            qty * baseUnitPrice * (product.diskonPersen / 100.0)
                        } else {
                            baseUnitPrice * (product.diskonPersen / 100.0)
                        }
                    } else if (product.diskonNominal > 0) {
                        if (kelipatan) {
                            qty * product.diskonNominal
                        } else {
                            product.diskonNominal
                        }
                    } else 0.0
                }
            }
        }

    val isPromoActive: Boolean
        get() = promoDiscountAmount > 0.0

    val unitPrice: Double
        get() {
            if (qty <= 0) return baseUnitPrice
            val effective = (baseUnitPrice * qty - promoDiscountAmount) / qty
            return maxOf(0.0, effective)
        }

    val subtotal: Double
        get() {
            if (isVoid) return 0.0
            val amount = maxOf(0.0, (baseUnitPrice * qty) - promoDiscountAmount - customDiscount)
            return if (isRetur) -amount else amount
        }

    val promoStatusText: String
        get() {
            if (isVoid || isRetur) return ""

            if (!product.isPromo && !product.hasDiscountOrPromo()) {
                if (product.hargaGrosir > 0 && product.hargaGrosirQty > 0 && selectedSatuan.equals(product.satuan, ignoreCase = true)) {
                    if (qty >= product.hargaGrosirQty) {
                        return "Grosir Beli >= ${product.hargaGrosirQty} (Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.hargaGrosirQty - qty
                        return "Beli $needed lagi untuk Harga Grosir ${formatRupiah(product.hargaGrosir)}"
                    }
                }
                return ""
            }

            if (!isPromoEligibleForUnit) {
                return "Promo '${product.getEffectivePromoLabel()}' khusus satuan ecer (${product.satuan})"
            }

            val kelipatanTag = if (!product.promoBerlakuKelipatan) " (Maks. 1x / struk)" else ""

            return when (product.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (qty >= product.promoMinBeli) {
                        val freeUnits = if (product.promoBerlakuKelipatan) {
                            (qty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
                        } else {
                            maxOf(1, product.promoGratisQty)
                        }
                        "Beli ${product.promoMinBeli} Gratis ${product.promoGratisQty}$kelipatanTag (Dapat $freeUnits Gratis, Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.promoMinBeli - qty
                        "Beli $needed lagi untuk dapatkan GRATIS ${product.promoGratisQty}!"
                    }
                }
                "BELI_X_POTONGAN" -> {
                    if (qty >= product.promoMinBeli) {
                        "Beli ${product.promoMinBeli} Potongan ${formatRupiah(product.promoPotonganNominal)}$kelipatanTag (Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.promoMinBeli - qty
                        "Beli $needed lagi untuk Potongan ${formatRupiah(product.promoPotonganNominal)}!"
                    }
                }
                "DISKON_NOMINAL" -> {
                    "Diskon ${formatRupiah(product.diskonNominal)}/item$kelipatanTag (Hemat ${formatRupiah(promoDiscountAmount)})"
                }
                "DISKON_PERSEN" -> {
                    "Diskon ${product.diskonPersen.toInt()}%$kelipatanTag (Hemat ${formatRupiah(promoDiscountAmount)})"
                }
                "GROSIR" -> {
                    if (qty >= product.hargaGrosirQty) {
                        "Grosir Beli >= ${product.hargaGrosirQty} (Harga ${formatRupiah(product.hargaGrosir)})"
                    } else {
                        val needed = product.hargaGrosirQty - qty
                        "Beli $needed lagi untuk Harga Grosir ${formatRupiah(product.hargaGrosir)}"
                    }
                }
                else -> {
                    if (isPromoActive) "Promo Hemat ${formatRupiah(promoDiscountAmount)}$kelipatanTag"
                    else product.getEffectivePromoLabel()
                }
            }
        }
}

data class PromoAlertInfo(
    val product: ProductEntity,
    val title: String,
    val description: String,
    val badge: String,
    val currentQty: Int,
    val neededQtyForPromo: Int? = null,
    val savingsText: String = ""
)

data class PendingCart(
    val id: String = UUID.randomUUID().toString(),
    val bonNo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val items: List<CartItem>,
    val member: MemberEntity? = null,
    val note: String = ""
) {
    val label: String get() = if (note.isNotBlank()) note else bonNo
    val totalAmount: Double get() = items.sumOf { it.subtotal }
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TRIAL_TRANSACTION_LIMIT = 50
    }

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = PosRepository(db.posDao())

    // Multi-Device Synchronization Manager
    val syncManager = com.example.data.sync.PosSyncManager(application, repository)
    val syncConfig = syncManager.syncConfig
    val syncStatus = syncManager.syncStatus

    fun isTrialLimitReached(): Boolean {
        return storeConfig.value.isTrial && allTransactions.value.size >= TRIAL_TRANSACTION_LIMIT
    }

    // Navigation
    private val _currentDestination = MutableStateFlow(PosNavDestination.KASIR)
    val currentDestination: StateFlow<PosNavDestination> = _currentDestination.asStateFlow()

    fun hasPermissionForDestination(dest: PosNavDestination): Boolean {
        if (dest == PosNavDestination.KASIR) return true
        val cashier = _currentCashier.value ?: return false
        if (cashier.isOwner()) return true
        if (dest == PosNavDestination.CLEREK) {
            return cashier.canClerekKasir
        }
        // Seluruh menu SIS membutuhkan izin dasar canAccessSis
        if (!cashier.canAccessSis) return false

        return when (dest) {
            PosNavDestination.KASIR -> true
            PosNavDestination.SIS_DASHBOARD -> cashier.canAccessSis
            PosNavDestination.PRODUK -> cashier.canManageProduk
            PosNavDestination.CETAK_LABEL_HARGA -> cashier.canCetakLabelRak
            PosNavDestination.PENJUALAN -> cashier.canViewLaporan || cashier.isAdmin()
            PosNavDestination.PEMBELIAN -> cashier.canManagePembelian
            PosNavDestination.RETUR_PEMBELIAN -> cashier.canReturPembelian
            PosNavDestination.STOK_OPNAME -> cashier.canStockOpname
            PosNavDestination.CLEREK -> cashier.canClerekKasir
            PosNavDestination.LAPORAN_TOKO -> cashier.canViewLaporan
            PosNavDestination.EOD_TUTUP_TOKO -> cashier.canEodTutupToko
            PosNavDestination.PENGATURAN_MEMBER -> cashier.canManageMember
            PosNavDestination.VOUCHER_POIN -> cashier.canManageVoucher
            PosNavDestination.KARYAWAN -> cashier.canManageKaryawan
            PosNavDestination.PENGATURAN_SHIFT -> cashier.canManageShift
            PosNavDestination.PENGATURAN_PRINTER -> cashier.canManageSettings
            PosNavDestination.EDIT_TOKO -> cashier.canManageSettings
            PosNavDestination.BACKUP_RESTORE -> cashier.canBackupRestore
            PosNavDestination.SINKRONISASI_MULTI_DEVICE -> cashier.canManageSettings
            PosNavDestination.PIUTANG -> cashier.canAccessSis || cashier.isAdmin() || cashier.canViewLaporan
        }
    }

    fun navigateTo(dest: PosNavDestination) {
        if (dest == PosNavDestination.KASIR) {
            _currentDestination.value = dest
            return
        }
        val cashier = _currentCashier.value
        if (cashier == null || (!cashier.isOwner() && !hasPermissionForDestination(dest))) {
            return
        }
        _currentDestination.value = dest
    }

    private val shiftPrefs = application.getSharedPreferences("dodolan_shift_session", android.content.Context.MODE_PRIVATE)

    // Cashier Session
    private val _isUserLoggedIn = MutableStateFlow(shiftPrefs.getBoolean("is_user_logged_in", false))
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _currentCashier = MutableStateFlow<CashierUserEntity?>(null)
    val currentCashier: StateFlow<CashierUserEntity?> = _currentCashier.asStateFlow()

    val allCashiers: StateFlow<List<CashierUserEntity>> = repository.allCashiers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Shifts (Tersimpan per kasir hingga proses clerek dilakukan)
    private val _isShiftOpen = MutableStateFlow(false)
    val isShiftOpen: StateFlow<Boolean> = _isShiftOpen.asStateFlow()

    private val _activeShift = MutableStateFlow(1)
    val activeShift: StateFlow<Int> = _activeShift.asStateFlow()

    private val _shiftModalAwal = MutableStateFlow(100000.0)
    val shiftModalAwal: StateFlow<Double> = _shiftModalAwal.asStateFlow()

    private val _shiftOpenedAt = MutableStateFlow("")
    val shiftOpenedAt: StateFlow<String> = _shiftOpenedAt.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

    fun loadShiftForCashier(nik: String) {
        val isOpen = shiftPrefs.getBoolean("is_shift_open_$nik", false)
        val shift = shiftPrefs.getInt("active_shift_$nik", 1)
        val modal = shiftPrefs.getFloat("shift_modal_awal_$nik", 100000f).toDouble()
        val openedAt = shiftPrefs.getString("shift_opened_at_$nik", "") ?: ""
        _isShiftOpen.value = isOpen
        _activeShift.value = shift
        _shiftModalAwal.value = modal
        _shiftOpenedAt.value = openedAt
    }

    init {
        viewModelScope.launch {
            val owner = repository.getCashierByNik("11111111")
            if (owner == null) {
                repository.saveCashier(
                    CashierUserEntity("11111111", "Owner", "1234", "OWNER")
                )
            }
            val lastNik = shiftPrefs.getString("last_logged_in_nik", null)
            val wasLoggedIn = shiftPrefs.getBoolean("is_user_logged_in", false)
            if (wasLoggedIn && !lastNik.isNullOrEmpty()) {
                val user = repository.getCashierByNik(lastNik)
                if (user != null) {
                    _currentCashier.value = user
                    _isUserLoggedIn.value = true
                    loadShiftForCashier(user.nik)
                } else {
                    _isShiftOpen.value = false
                }
            } else {
                _isShiftOpen.value = false
            }
        }

        // Real-Time Multi-Device Sync & Heartbeat Loop
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(4000L)
                try {
                    val cfg = syncManager.syncConfig.value
                    val localPending = _pendingCarts.value.size + (if (_cartItems.value.isNotEmpty()) 1 else 0)
                    val nik = _currentCashier.value?.nik ?: "001"
                    val name = _currentCashier.value?.nama ?: "Kasir"
                    if (cfg.role == com.example.data.sync.SyncRole.CLIENT) {
                        syncManager.sendClientHeartbeat(localPending, nik, name)
                    } else if (cfg.role == com.example.data.sync.SyncRole.SERVER) {
                        syncManager.registerLocalDeviceAsServer(nik, name, localPending)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    val allShiftConfigs: StateFlow<List<ShiftConfigEntity>> = repository.allShiftConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Store Config
    val storeConfig: StateFlow<StoreConfigEntity> = repository.storeConfig
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoreConfigEntity())

    // Products & Transactions
    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProductsIncludingInactive: StateFlow<List<ProductEntity>> = repository.allProductsAll
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMembers: StateFlow<List<MemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactionItems: StateFlow<List<TransactionItemEntity>> = repository.allTransactionItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allClerekRecords: StateFlow<List<ClerekEntity>> = repository.allClerekRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<PurchaseEntity>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchasePayments: StateFlow<List<PurchasePaymentEntity>> = repository.allPurchasePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockOpnames: StateFlow<List<StockOpnameEntity>> = repository.allStockOpnames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnDocEntity>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allVouchers: StateFlow<List<VoucherEntity>> = repository.getAllVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<SupplierEntity>> = repository.getAllSuppliers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStoreBranches: StateFlow<List<StoreBranchEntity>> = repository.getAllStoreBranches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceivables: StateFlow<List<ReceivableEntity>> = repository.allReceivables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceivablePayments: StateFlow<List<ReceivablePaymentEntity>> = repository.allReceivablePayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivables: StateFlow<List<ReceivableEntity>> get() = allReceivables
    val receivablePayments: StateFlow<List<ReceivablePaymentEntity>> get() = allReceivablePayments

    val allTokoGrosir: StateFlow<List<StoreBranchEntity>> get() = allStoreBranches

    val products: StateFlow<List<ProductEntity>> get() = allProducts
    val transactions: StateFlow<List<TransactionEntity>> get() = allTransactions
    val cashiers: StateFlow<List<CashierUserEntity>> get() = allCashiers
    val members: StateFlow<List<MemberEntity>> get() = allMembers
    val shiftConfigs: StateFlow<List<ShiftConfigEntity>> get() = allShiftConfigs
    val vouchers: StateFlow<List<VoucherEntity>> get() = allVouchers

    val activeMember: StateFlow<MemberEntity?> get() = currentMember

    val currentShift: StateFlow<ShiftConfigEntity?> = combine(
        allShiftConfigs, _activeShift
    ) { configs, shiftNum ->
        configs.find { it.shiftNumber == shiftNum } ?: ShiftConfigEntity(
            shiftNumber = shiftNum,
            shiftName = "Shift $shiftNum",
            jamMulai = "07:00",
            jamSelesai = "15:00"
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShiftConfigEntity(1, "Shift 1", "07:00", "15:00"))

    val lastTransaction: StateFlow<TransactionEntity?> = allTransactions.map {
        it.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _currentMember = MutableStateFlow<MemberEntity?>(null)
    val currentMember: StateFlow<MemberEntity?> = _currentMember.asStateFlow()

    private val _inputBarcodeInput = MutableStateFlow("")
    val inputBarcodeInput: StateFlow<String> = _inputBarcodeInput.asStateFlow()

    // Hardware Keyboard Shortcut Events
    private val _shortcutEvents = MutableSharedFlow<PosShortcutEvent>(extraBufferCapacity = 16)
    val shortcutEvents: SharedFlow<PosShortcutEvent> = _shortcutEvents.asSharedFlow()

    fun triggerShortcut(event: PosShortcutEvent) {
        _shortcutEvents.tryEmit(event)
    }

    private fun savePendingCartsToStorage(carts: List<PendingCart>) {
        try {
            val rootArray = JSONArray()
            for (cart in carts) {
                val cartObj = JSONObject().apply {
                    put("id", cart.id)
                    put("bonNo", cart.bonNo)
                    put("timestamp", cart.timestamp)
                    put("note", cart.note)
                    cart.member?.let { m ->
                        put("member", JSONObject().apply {
                            put("memberNo", m.memberNo)
                            put("nama", m.nama)
                            put("noHp", m.noHp)
                            put("alamat", m.alamat)
                            put("poin", m.poin)
                            put("totalBelanja", m.totalBelanja)
                        })
                    }
                    val itemsArray = JSONArray()
                    for (item in cart.items) {
                        val itemObj = JSONObject().apply {
                            put("qty", item.qty)
                            put("isGrosirApplied", item.isGrosirApplied)
                            put("isPromoApplied", item.isPromoApplied)
                            put("customDiscount", item.customDiscount)
                            put("note", item.note)
                            put("isVoid", item.isVoid)
                            put("voidReason", item.voidReason)
                            put("isRetur", item.isRetur)
                            val p = item.product
                            put("product", JSONObject().apply {
                                put("plu", p.plu)
                                put("barcode", p.barcode)
                                put("nama", p.nama)
                                put("kategori", p.kategori)
                                put("hargaBeli", p.hargaBeli)
                                put("hargaJual", p.hargaJual)
                                put("hargaMember", p.hargaMember)
                                put("stok", p.stok)
                                put("satuan", p.satuan)
                                put("rakPlanogram", p.rakPlanogram)
                                put("minStok", p.minStok)
                                put("isPromo", p.isPromo)
                                put("diskonPersen", p.diskonPersen)
                                put("statusAktif", p.statusAktif)
                                put("hargaGrosirQty", p.hargaGrosirQty)
                                put("hargaGrosir", p.hargaGrosir)
                            })
                        }
                        itemsArray.put(itemObj)
                    }
                    put("items", itemsArray)
                }
                rootArray.put(cartObj)
            }
            shiftPrefs.edit().putString("saved_pending_carts_json", rootArray.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadPendingCartsFromStorage(): List<PendingCart> {
        val json = shiftPrefs.getString("saved_pending_carts_json", null) ?: return emptyList()
        return try {
            val rootArray = JSONArray(json)
            val list = mutableListOf<PendingCart>()
            for (i in 0 until rootArray.length()) {
                val cartObj = rootArray.getJSONObject(i)
                val id = cartObj.optString("id", UUID.randomUUID().toString())
                val bonNo = cartObj.optString("bonNo", "")
                val timestamp = cartObj.optLong("timestamp", System.currentTimeMillis())
                val note = cartObj.optString("note", "")
                val member = cartObj.optJSONObject("member")?.let { mObj ->
                    MemberEntity(
                        memberNo = mObj.optString("memberNo", ""),
                        nama = mObj.optString("nama", ""),
                        noHp = mObj.optString("noHp", ""),
                        alamat = mObj.optString("alamat", ""),
                        poin = mObj.optInt("poin", 0),
                        totalBelanja = mObj.optDouble("totalBelanja", 0.0)
                    )
                }
                val itemsArray = cartObj.optJSONArray("items") ?: JSONArray()
                val items = mutableListOf<CartItem>()
                for (j in 0 until itemsArray.length()) {
                    val itObj = itemsArray.getJSONObject(j)
                    val pObj = itObj.getJSONObject("product")
                    val product = ProductEntity(
                        plu = pObj.optString("plu", ""),
                        barcode = pObj.optString("barcode", ""),
                        nama = pObj.optString("nama", ""),
                        kategori = pObj.optString("kategori", ""),
                        hargaBeli = pObj.optDouble("hargaBeli", 0.0),
                        hargaJual = pObj.optDouble("hargaJual", 0.0),
                        hargaMember = pObj.optDouble("hargaMember", pObj.optDouble("hargaJual", 0.0)),
                        stok = pObj.optInt("stok", 0),
                        satuan = pObj.optString("satuan", "PCS"),
                        rakPlanogram = pObj.optString("rakPlanogram", "001-01"),
                        minStok = pObj.optInt("minStok", 5),
                        isPromo = pObj.optBoolean("isPromo", false),
                        diskonPersen = pObj.optDouble("diskonPersen", 0.0),
                        statusAktif = pObj.optBoolean("statusAktif", true),
                        hargaGrosirQty = pObj.optInt("hargaGrosirQty", 2),
                        hargaGrosir = pObj.optDouble("hargaGrosir", 0.0)
                    )
                    items.add(
                        CartItem(
                            product = product,
                            qty = itObj.optInt("qty", 1),
                            isGrosirApplied = itObj.optBoolean("isGrosirApplied", false),
                            isPromoApplied = itObj.optBoolean("isPromoApplied", false),
                            customDiscount = itObj.optDouble("customDiscount", 0.0),
                            note = itObj.optString("note", ""),
                            isVoid = itObj.optBoolean("isVoid", false),
                            voidReason = itObj.optString("voidReason", ""),
                            isRetur = itObj.optBoolean("isRetur", false)
                        )
                    )
                }
                list.add(
                    PendingCart(
                        id = id,
                        bonNo = bonNo,
                        timestamp = timestamp,
                        items = items,
                        member = member,
                        note = note
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val _pendingCarts = MutableStateFlow<List<PendingCart>>(loadPendingCartsFromStorage())
    val pendingCarts: StateFlow<List<PendingCart>> = _pendingCarts.asStateFlow()

    private val _currentBonNo = MutableStateFlow(generateNewBonNo())
    val currentBonNo: StateFlow<String> = _currentBonNo.asStateFlow()

    private val _appliedPoin = MutableStateFlow(0)
    val appliedPoin: StateFlow<Int> = _appliedPoin.asStateFlow()

    private val _appliedVoucherCode = MutableStateFlow("")
    val appliedVoucherCode: StateFlow<String> = _appliedVoucherCode.asStateFlow()

    private val _appliedVoucherDiscount = MutableStateFlow(0.0)
    val appliedVoucherDiscount: StateFlow<Double> = _appliedVoucherDiscount.asStateFlow()

    // Promo Notification Alert
    private val _activePromoAlert = MutableStateFlow<PromoAlertInfo?>(null)
    val activePromoAlert: StateFlow<PromoAlertInfo?> = _activePromoAlert.asStateFlow()

    fun dismissPromoAlert() {
        _activePromoAlert.value = null
    }

    fun applyPromoAdditionalQty(product: ProductEntity, additionalQty: Int = 1) {
        addProductToCart(product, additionalQty)
        _activePromoAlert.value = null
    }

    // Totals
    val subtotal: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val grandTotal: StateFlow<Double> = combine(
        subtotal, _appliedPoin, _appliedVoucherDiscount, storeConfig
    ) { sub, poin, vDisc, cfg ->
        val poinNominal = if (cfg.poinAktif) poin * cfg.nilaiPerPoin else 0.0
        val base = maxOf(0.0, sub - poinNominal - vDisc)
        if (cfg.isTaxActive && !cfg.isTaxIncluded && cfg.ppnPercentage > 0.0) {
            val tax = base * (cfg.ppnPercentage / 100.0)
            base + tax
        } else {
            base
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val taxAmount: StateFlow<Double> = combine(
        subtotal, _appliedPoin, _appliedVoucherDiscount, storeConfig
    ) { sub, poin, vDisc, cfg ->
        if (cfg.isTaxActive && cfg.ppnPercentage > 0.0) {
            val poinNominal = if (cfg.poinAktif) poin * cfg.nilaiPerPoin else 0.0
            val base = maxOf(0.0, sub - poinNominal - vDisc)
            val rate = cfg.ppnPercentage / 100.0
            if (cfg.isTaxIncluded) {
                base * rate / (1.0 + rate)
            } else {
                base * rate
            }
        } else {
            0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val totalDiscount: StateFlow<Double> = combine(
        _cartItems, _appliedPoin, _appliedVoucherDiscount, storeConfig
    ) { items, poin, vDisc, cfg ->
        val itemsDiscount = items.sumOf { item ->
            val normalSubtotal = item.product.hargaJual * item.qty
            val actualSubtotal = item.subtotal
            if (normalSubtotal > actualSubtotal) normalSubtotal - actualSubtotal else 0.0
        }
        val poinNominal = if (cfg.poinAktif) poin * cfg.nilaiPerPoin else 0.0
        itemsDiscount + poinNominal + vDisc
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun onBarcodeInputChanged(value: String) {
        _inputBarcodeInput.value = value
    }

    private fun generateNewBonNo(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return "BON-${sdf.format(Date())}"
    }

    fun addProductToCart(product: ProductEntity, qty: Int = 1, selectedSatuan: String = product.satuan): Pair<Boolean, String> {
        val freshProduct = allProducts.value.find { it.plu == product.plu } ?: product
        if (freshProduct.stok <= 0) {
            return Pair(false, "Barang '${freshProduct.nama}' stok habis (0)! Tidak bisa lanjut transaksi.")
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.plu == freshProduct.plu && it.selectedSatuan == selectedSatuan && !it.isVoid && !it.isRetur }
        val newTotalQty: Int

        if (index >= 0) {
            val existing = currentList[index]
            val updatedQty = existing.qty + qty
            if (updatedQty > freshProduct.stok) {
                return Pair(false, "Stok '${freshProduct.nama}' tidak mencukupi (Tersisa: ${freshProduct.stok}, Diminta: $updatedQty)!")
            }
            val isGrosir = freshProduct.hargaGrosir > 0 && freshProduct.hargaGrosirQty > 0 && updatedQty >= freshProduct.hargaGrosirQty
            currentList[index] = existing.copy(
                product = freshProduct,
                qty = updatedQty,
                isGrosirApplied = isGrosir,
                isPromoApplied = freshProduct.isPromo || freshProduct.diskonPersen > 0
            )
            newTotalQty = updatedQty
        } else {
            if (qty > freshProduct.stok) {
                return Pair(false, "Stok '${freshProduct.nama}' tidak mencukupi (Tersisa: ${freshProduct.stok}, Diminta: $qty)!")
            }
            val isGrosir = freshProduct.hargaGrosir > 0 && freshProduct.hargaGrosirQty > 0 && qty >= freshProduct.hargaGrosirQty
            currentList.add(
                CartItem(
                    product = freshProduct,
                    qty = qty,
                    isGrosirApplied = isGrosir,
                    isPromoApplied = freshProduct.isPromo || freshProduct.diskonPersen > 0,
                    selectedSatuan = selectedSatuan
                )
            )
            newTotalQty = qty
        }
        _cartItems.value = currentList

        // Check for Promo Alerts
        if (freshProduct.isPromo && freshProduct.isPromoEligible(selectedSatuan)) {
            when (freshProduct.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (newTotalQty >= product.promoMinBeli) {
                        val freeUnits = if (freshProduct.promoBerlakuKelipatan) {
                            (newTotalQty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
                        } else {
                            maxOf(1, product.promoGratisQty)
                        }
                        val hemat = freeUnits * product.hargaJual
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "PROMO AKTIF: Beli ${product.promoMinBeli} Gratis ${product.promoGratisQty}!",
                            description = "Syarat pembelian minimal ${product.promoMinBeli} ${product.satuan} terpenuhi! Anda mendapatkan $freeUnits ${product.satuan} gratis (Hemat Rp ${hemat.toLong()}).",
                            badge = "PROMO AKTIF",
                            currentQty = newTotalQty,
                            savingsText = "Hemat Rp ${hemat.toLong()}"
                        )
                    } else {
                        val needed = product.promoMinBeli - newTotalQty
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "INFO PROMO: Beli ${product.promoMinBeli} Gratis ${product.promoGratisQty}!",
                            description = "Promo ${product.nama}: Cukup tambah $needed lagi untuk mendapatkan GRATIS ${product.promoGratisQty} ${product.satuan}!",
                            badge = "PROMO BELI ${product.promoMinBeli}",
                            currentQty = newTotalQty,
                            neededQtyForPromo = needed,
                            savingsText = "Gratis ${product.promoGratisQty} ${product.satuan}"
                        )
                    }
                    return Pair(true, "Berhasil menambahkan ${product.nama}")
                }
                "BELI_X_POTONGAN" -> {
                    if (newTotalQty >= product.promoMinBeli) {
                        val sets = if (freshProduct.promoBerlakuKelipatan) {
                            newTotalQty / product.promoMinBeli
                        } else {
                            1
                        }
                        val hemat = sets * product.promoPotonganNominal
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "PROMO AKTIF: Beli ${product.promoMinBeli} Potongan Rp ${product.promoPotonganNominal.toLong()}!",
                            description = "Potongan diskon Rp ${product.promoPotonganNominal.toLong()} per ${product.promoMinBeli} ${product.satuan} aktif otomatis (Total hemat Rp ${hemat.toLong()}).",
                            badge = "PROMO AKTIF",
                            currentQty = newTotalQty,
                            savingsText = "Hemat Rp ${hemat.toLong()}"
                        )
                    } else {
                        val needed = product.promoMinBeli - newTotalQty
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "INFO PROMO: Beli ${product.promoMinBeli} Potongan Rp ${product.promoPotonganNominal.toLong()}!",
                            description = "Tambah $needed lagi untuk mendapatkan potongan harga Rp ${product.promoPotonganNominal.toLong()}!",
                            badge = "POTONGAN RP ${product.promoPotonganNominal.toLong()}",
                            currentQty = newTotalQty,
                            neededQtyForPromo = needed,
                            savingsText = "Potongan Rp ${product.promoPotonganNominal.toLong()}"
                        )
                    }
                    return Pair(true, "Berhasil menambahkan ${product.nama}")
                }
                "DISKON_NOMINAL" -> {
                    val hemat = newTotalQty * product.diskonNominal
                    _activePromoAlert.value = PromoAlertInfo(
                        product = product,
                        title = "PROMO AKTIF: Potongan Rp ${product.diskonNominal.toLong()}/item!",
                        description = "Potongan langsung Rp ${product.diskonNominal.toLong()} diterapkan otomatis (Total hemat Rp ${hemat.toLong()}).",
                        badge = "PROMO AKTIF",
                        currentQty = newTotalQty,
                        savingsText = "Hemat Rp ${hemat.toLong()}"
                    )
                    return Pair(true, "Berhasil menambahkan ${product.nama}")
                }
                "DISKON_PERSEN" -> {
                    val hemat = newTotalQty * product.hargaJual * (product.diskonPersen / 100.0)
                    _activePromoAlert.value = PromoAlertInfo(
                        product = product,
                        title = "PROMO AKTIF: Diskon ${product.diskonPersen.toInt()}%!",
                        description = "Diskon ${product.diskonPersen.toInt()}% diterapkan otomatis untuk setiap pembelian ${product.nama}.",
                        badge = "PROMO AKTIF",
                        currentQty = newTotalQty,
                        savingsText = "Hemat Rp ${hemat.toLong()}"
                    )
                    return Pair(true, "Berhasil menambahkan ${product.nama}")
                }
                "GROSIR" -> {
                    if (newTotalQty >= product.hargaGrosirQty && product.hargaGrosir > 0) {
                        val hemat = (product.hargaJual - product.hargaGrosir) * newTotalQty
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "PROMO GROSIR BERHASIL AKTIF!",
                            description = "Pembelian minimal ${product.hargaGrosirQty} ${product.satuan} tercapai! Harga grosir Rp ${product.hargaGrosir.toLong()} diterapkan.",
                            badge = "HEMAT GROSIR",
                            currentQty = newTotalQty,
                            savingsText = "Hemat Rp ${hemat.toLong()}"
                        )
                    } else {
                        val needed = product.hargaGrosirQty - newTotalQty
                        _activePromoAlert.value = PromoAlertInfo(
                            product = product,
                            title = "PENAWARAN HARGA GROSIR!",
                            description = "Beli ${product.hargaGrosirQty} ${product.satuan} dapat harga khusus Rp ${product.hargaGrosir.toLong()}. Cukup tambah $needed lagi!",
                            badge = "PROMO BELI ${product.hargaGrosirQty}",
                            currentQty = newTotalQty,
                            neededQtyForPromo = needed,
                            savingsText = "Harga Rp ${product.hargaGrosir.toLong()}"
                        )
                    }
                    return Pair(true, "Berhasil menambahkan ${product.nama}")
                }
            }
        }

        if (product.hargaGrosir > 0 && product.hargaGrosirQty > 0) {
            if (newTotalQty >= product.hargaGrosirQty) {
                val hemat = (product.hargaJual - product.hargaGrosir) * newTotalQty
                _activePromoAlert.value = PromoAlertInfo(
                    product = product,
                    title = "PROMO GROSIR BERHASIL AKTIF!",
                    description = "Pembelian minimal ${product.hargaGrosirQty} ${product.satuan} tercapai! Harga grosir Rp ${product.hargaGrosir.toLong()} per ${product.satuan} diterapkan otomatis.",
                    badge = "HEMAT GROSIR",
                    currentQty = newTotalQty,
                    savingsText = "Hemat Rp ${hemat.toLong()}"
                )
            } else {
                val needed = product.hargaGrosirQty - newTotalQty
                _activePromoAlert.value = PromoAlertInfo(
                    product = product,
                    title = "PENAWARAN PROMO GROSIR!",
                    description = "Beli ${product.hargaGrosirQty} ${product.satuan} dapat harga potongan khusus Rp ${product.hargaGrosir.toLong()} (Harga normal Rp ${product.hargaJual.toLong()}). Cukup tambah $needed lagi!",
                    badge = "PROMO BELI ${product.hargaGrosirQty}",
                    currentQty = newTotalQty,
                    neededQtyForPromo = needed,
                    savingsText = "Potongan Rp ${(product.hargaJual - product.hargaGrosir).toLong()}/${product.satuan}"
                )
            }
        }
        return Pair(true, "Berhasil menambahkan ${product.nama}")
    }

    fun updateCartItemQty(index: Int, newQty: Int): Pair<Boolean, String> {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            if (newQty <= 0) {
                currentList.removeAt(index)
                _cartItems.value = currentList
                return Pair(true, "${item.product.nama} dihapus dari keranjang")
            } else {
                val fresh = allProducts.value.find { it.plu == item.product.plu } ?: item.product
                if (!item.isRetur && !item.isVoid) {
                    if (fresh.stok <= 0) {
                        currentList.removeAt(index)
                        _cartItems.value = currentList
                        return Pair(false, "Stok '${fresh.nama}' habis (0)! Item dihapus dari transaksi.")
                    }
                    if (newQty > fresh.stok) {
                        return Pair(false, "Stok '${fresh.nama}' tidak mencukupi (Tersisa: ${fresh.stok})!")
                    }
                }
                val isGrosir = fresh.hargaGrosir > 0 && fresh.hargaGrosirQty > 0 && newQty >= fresh.hargaGrosirQty
                currentList[index] = item.copy(
                    product = fresh,
                    qty = newQty,
                    isGrosirApplied = isGrosir
                )
                _cartItems.value = currentList
                return Pair(true, "Qty ${fresh.nama} diubah menjadi $newQty")
            }
        }
        return Pair(false, "Item tidak ditemukan")
    }

    fun updateCartItemQty(plu: String, newQty: Int): Pair<Boolean, String> {
        val currentList = _cartItems.value
        val index = currentList.indexOfFirst { it.product.plu == plu }
        return if (index >= 0) {
            updateCartItemQty(index, newQty)
        } else {
            Pair(false, "Item tidak ditemukan")
        }
    }

    fun updateCartItemSatuan(index: Int, newSatuan: String) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            currentList[index] = item.copy(selectedSatuan = newSatuan)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(index: Int) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(plu: String) {
        val currentList = _cartItems.value
        val index = currentList.indexOfFirst { it.product.plu == plu }
        if (index >= 0) {
            removeCartItem(index)
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _currentMember.value = null
        _appliedPoin.value = 0
        _appliedVoucherCode.value = ""
        _appliedVoucherDiscount.value = 0.0
        _activePromoAlert.value = null
        _currentBonNo.value = generateNewBonNo()
    }

    fun submitBarcodeOrPlu(
        query: String = _inputBarcodeInput.value,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            onResult(false, "Kode barcode atau PLU kosong")
            return
        }
        if (query == _inputBarcodeInput.value) {
            _inputBarcodeInput.value = ""
        }

        viewModelScope.launch {
            val product = repository.getProductByPluOrBarcode(trimmed)
            if (product != null) {
                if (product.stok <= 0) {
                    onResult(false, "Barang '${product.nama}' stok habis (0)! Tidak bisa lanjut transaksi.")
                    return@launch
                }
                val targetSatuan = when {
                    trimmed.equals(product.barcodeRepack, ignoreCase = true) && product.isRepack && product.satuanRepack.isNotBlank() -> product.satuanRepack
                    trimmed.equals(product.barcodeRepack2, ignoreCase = true) && product.isRepack && product.satuanRepack2.isNotBlank() -> product.satuanRepack2
                    else -> product.satuan
                }
                val res = addProductToCart(product, 1, targetSatuan)
                if (!res.first) {
                    onResult(false, res.second)
                    return@launch
                }
                val unitLabel = if (targetSatuan != product.satuan) " ($targetSatuan)" else ""
                onResult(true, "Berhasil menambahkan ${product.nama}$unitLabel")
            } else {
                val matches = repository.searchProducts(trimmed)
                if (matches.size == 1) {
                    val single = matches.first()
                    if (single.stok <= 0) {
                        onResult(false, "Barang '${single.nama}' stok habis (0)! Tidak bisa lanjut transaksi.")
                        return@launch
                    }
                    val targetSatuan = when {
                        trimmed.equals(single.barcodeRepack, ignoreCase = true) && single.isRepack && single.satuanRepack.isNotBlank() -> single.satuanRepack
                        trimmed.equals(single.barcodeRepack2, ignoreCase = true) && single.isRepack && single.satuanRepack2.isNotBlank() -> single.satuanRepack2
                        else -> single.satuan
                    }
                    val res = addProductToCart(single, 1, targetSatuan)
                    if (!res.first) {
                        onResult(false, res.second)
                        return@launch
                    }
                    val unitLabel = if (targetSatuan != single.satuan) " ($targetSatuan)" else ""
                    onResult(true, "Berhasil menambahkan ${single.nama}$unitLabel")
                } else if (matches.size > 1) {
                    onResult(false, "Ditemukan beberapa produk, pilih di katalog")
                } else {
                    onResult(false, "Produk dengan kode $trimmed tidak ditemukan!")
                }
            }
        }
    }

    fun submitBarcodeOrPlu(onNotFound: (String) -> Unit) {
        submitBarcodeOrPlu(_inputBarcodeInput.value) { success, msg ->
            if (!success) onNotFound(msg)
        }
    }

    fun submitBarcodeOrPlu(query: String, onNotFound: () -> Unit) {
        submitBarcodeOrPlu(query) { success, _ ->
            if (!success) onNotFound()
        }
    }

    fun addKantongPlastik() {
        viewModelScope.launch {
            val plastik = repository.getProductByPluOrBarcode("0016")
                ?: repository.searchProducts("Kantong Plastik").firstOrNull()
            if (plastik != null) {
                addProductToCart(plastik, 1)
            }
        }
    }

    fun setMember(member: MemberEntity?) {
        _currentMember.value = member
    }

    fun removeMember() {
        _currentMember.value = null
    }

    fun lookupMember(query: String, onResult: (MemberEntity?) -> Unit) {
        viewModelScope.launch {
            val member = repository.getMemberByKey(query)
            _currentMember.value = member
            onResult(member)
        }
    }

    fun applyVoucher(code: String, discountAmount: Double) {
        _appliedVoucherCode.value = code
        _appliedVoucherDiscount.value = discountAmount
    }

    fun applyVoucher(code: String): Pair<Boolean, String> {
        val trimmed = code.trim().uppercase()
        if (trimmed.isBlank()) {
            return Pair(false, "Masukkan kode voucher terlebih dahulu")
        }

        val curSubtotal = subtotal.value
        val voucher = allVouchers.value.find { it.kodeVoucher.equals(trimmed, ignoreCase = true) }

        if (voucher == null) {
            val config = storeConfig.value
            if (config.activeVoucherCode.isNotBlank() && config.activeVoucherCode.equals(trimmed, ignoreCase = true)) {
                val disc = if (config.voucherDiscountAmount > 0) config.voucherDiscountAmount else config.voucherDefaultNominal
                applyVoucher(trimmed, disc)
                return Pair(true, "Voucher $trimmed berhasil digunakan (potongan ${formatRupiah(disc)})")
            }
            return Pair(false, "Kode voucher '$trimmed' tidak terdaftar atau tidak ditemukan")
        }

        // 1. Cek apakah voucher sudah pernah digunakan
        if (voucher.isUsed) {
            val bonInfo = if (voucher.usedAtBonNo.isNotBlank()) " di struk #${voucher.usedAtBonNo}" else ""
            return Pair(false, "Voucher '$trimmed' SUDAH DIGUNAKAN$bonInfo dan tidak dapat dipakai lagi")
        }

        // 2. Cek apakah voucher sudah kedaluwarsa
        if (voucher.isExpired()) {
            return Pair(false, "Voucher '$trimmed' SUDAH TIDAK BERLAKU / KEDALUWARSA (berlaku s/d ${voucher.tanggalKedaluwarsa})")
        }

        // 3. Cek syarat minimal belanja
        if (curSubtotal < voucher.minBelanja) {
            return Pair(false, "Minimal belanja untuk voucher ini adalah ${formatRupiah(voucher.minBelanja)} (Subtotal belanja saat ini: ${formatRupiah(curSubtotal)})")
        }

        applyVoucher(voucher.kodeVoucher, voucher.nominalDiskon)
        return Pair(true, "Voucher '${voucher.kodeVoucher}' berhasil digunakan (potongan ${formatRupiah(voucher.nominalDiskon)})")
    }

    fun applyPoin(points: Int) {
        _appliedPoin.value = points
    }

    fun applyMemberPoints(points: Int) {
        applyPoin(points)
    }

    fun holdCurrentCart(note: String = ""): Boolean {
        if (_cartItems.value.isEmpty()) return false
        val pending = PendingCart(
            bonNo = _currentBonNo.value,
            items = _cartItems.value,
            member = _currentMember.value,
            note = note
        )
        val updated = _pendingCarts.value + pending
        _pendingCarts.value = updated
        savePendingCartsToStorage(updated)
        clearCart()
        return true
    }

    fun pendingCurrentCart(note: String = ""): Boolean = holdCurrentCart(note)

    fun resumePendingCart(pendingId: String): Boolean {
        val found = _pendingCarts.value.firstOrNull { it.id == pendingId } ?: return false
        val updated = _pendingCarts.value.filter { it.id != pendingId }
        _pendingCarts.value = updated
        savePendingCartsToStorage(updated)
        _cartItems.value = found.items
        _currentMember.value = found.member
        _currentBonNo.value = found.bonNo
        return true
    }

    fun deletePendingCart(pendingId: String) {
        val updated = _pendingCarts.value.filter { it.id != pendingId }
        _pendingCarts.value = updated
        savePendingCartsToStorage(updated)
    }

    fun deletePendingCartWithApproval(
        pendingId: String,
        otp: String,
        reason: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmed = otp.trim()
            if (trimmed.isEmpty()) {
                onComplete(false, "PIN / Password otorisasi tidak boleh kosong!")
                return@launch
            }

            val cashiers = try {
                repository.getAllCashiersList()
            } catch (e: Exception) {
                allCashiers.value
            }

            val authorizedStaff = cashiers.find { it.pin.trim() == trimmed && it.statusAktif }
            val isAuthorized: Pair<Boolean, String> = when {
                authorizedStaff == null -> {
                    if (trimmed == "1234" && (cashiers.isEmpty() || cashiers.any { it.isOwner() && it.pin.trim() == "1234" })) {
                        Pair(true, "Owner (Default)")
                    } else {
                        Pair(false, "PIN otorisasi salah! Anda tidak memiliki wewenang untuk tindakan ini.")
                    }
                }
                authorizedStaff.isOwner() || authorizedStaff.isAdmin() -> {
                    Pair(true, "${authorizedStaff.role} (${authorizedStaff.nama})")
                }
                authorizedStaff.isSupervisor() -> {
                    Pair(true, "Supervisor (${authorizedStaff.nama})")
                }
                else -> {
                    Pair(false, "Akses Ditolak: Karyawan '${authorizedStaff.nama}' (${authorizedStaff.role}). Kasir tidak boleh menghapus transaksi/item tertunda tanpa persetujuan Supervisor/Owner.")
                }
            }

            if (!isAuthorized.first) {
                onComplete(false, isAuthorized.second)
                return@launch
            }

            val target = _pendingCarts.value.firstOrNull { it.id == pendingId }
            val updated = _pendingCarts.value.filter { it.id != pendingId }
            _pendingCarts.value = updated
            savePendingCartsToStorage(updated)
            val labelStr = target?.label ?: pendingId
            onComplete(true, "Pending '$labelStr' berhasil dihapus dengan persetujuan ${isAuthorized.second}. Alasan: $reason")
        }
    }

    fun restorePendingCart(pendingId: String) {
        resumePendingCart(pendingId)
    }

    fun recallPendingCart(pendingId: String) = restorePendingCart(pendingId)

    fun isReturValidForCheckout(): Pair<Boolean, String> {
        val items = _cartItems.value
        val hasRetur = items.any { it.isRetur }
        val grand = grandTotal.value
        if (hasRetur && grand < 0) {
            return Pair(false, "Nilai total belanja setelah retur tidak boleh negatif (minimum Rp 0)!")
        }
        return Pair(true, "")
    }

    fun validateTransactionForCheckout(): Pair<Boolean, String> {
        if (!_isShiftOpen.value) {
            return Pair(false, "Shift belum dibuka! Harap buka shift kasir terlebih dahulu.")
        }
        val items = _cartItems.value
        if (items.isEmpty()) {
            return Pair(false, "Keranjang belanja masih kosong! Silakan scan barang terlebih dahulu.")
        }
        val nonVoidItems = items.filter { !it.isVoid }
        if (nonVoidItems.isEmpty()) {
            return Pair(false, "Semua item di keranjang telah di-void! Tidak ada transaksi yang dapat diproses.")
        }
        val grand = grandTotal.value
        if (grand <= 0.0) {
            return Pair(false, "Total transaksi Rp 0! Transaksi dengan nilai Rp 0 tidak dapat diproses.")
        }
        for (item in items) {
            if (!item.isVoid && !item.isRetur) {
                val liveProd = allProducts.value.find { it.plu == item.product.plu } ?: item.product
                if (liveProd.stok <= 0) {
                    return Pair(false, "Barang '${liveProd.nama}' stok habis (0)! Tidak bisa lanjut transaksi.")
                }
                if (item.qty > liveProd.stok) {
                    return Pair(false, "Jumlah '${liveProd.nama}' (${item.qty}) melebihi stok yang tersedia (${liveProd.stok})! Kurangi jumlah terlebih dahulu.")
                }
            }
        }
        val returCheck = isReturValidForCheckout()
        if (!returCheck.first) {
            return returCheck
        }
        return Pair(true, "")
    }

    suspend fun findTransactionForRetur(bonKey: String): Pair<TransactionEntity?, List<TransactionItemEntity>> {
        val res = repository.findTransactionForRetur(bonKey)
        return if (res != null) Pair(res.first, res.second) else Pair(null, emptyList())
    }

    fun verifyAuthorityPin(
        pinInput: String,
        requiredPermission: (CashierUserEntity) -> Boolean = { it.canVoidItem }
    ): Pair<Boolean, String> {
        val trimmed = pinInput.trim()
        if (trimmed.isEmpty()) {
            return Pair(false, "PIN / Password otorisasi tidak boleh kosong!")
        }
        val cashiers = allCashiers.value
        val authorizedUser = cashiers.find { it.pin.trim() == trimmed && it.statusAktif }
        if (authorizedUser != null) {
            if (authorizedUser.isOwner() || authorizedUser.isAdmin()) {
                return Pair(true, "Otorisasi ${authorizedUser.role} (${authorizedUser.nama}) disetujui")
            }
            if (authorizedUser.isSupervisor()) {
                return Pair(true, "Otorisasi Supervisor (${authorizedUser.nama}) disetujui")
            }
            if (requiredPermission(authorizedUser)) {
                return Pair(true, "Otorisasi ${authorizedUser.nama} disetujui")
            }
            return Pair(false, "Akses Ditolak: '${authorizedUser.nama}' tidak memiliki wewenang untuk tindakan ini.")
        }
        // Fallback hanya jika default owner 1234 dan ada akun owner dengan PIN 1234
        if (trimmed == "1234" && (cashiers.isEmpty() || cashiers.any { it.isOwner() && it.pin.trim() == "1234" })) {
            return Pair(true, "Otorisasi Owner disetujui")
        }
        return Pair(false, "PIN / Password otorisasi salah! Anda tidak memiliki wewenang untuk tindakan ini.")
    }

    fun addReturItem(
        bonNo: String,
        product: ProductEntity,
        qty: Int,
        reason: String,
        otp: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmed = otp.trim()
            if (trimmed.isEmpty()) {
                onComplete(false, "PIN otorisasi tidak boleh kosong!")
                return@launch
            }
            val cashiers = try {
                repository.getAllCashiersList()
            } catch (e: Exception) {
                allCashiers.value
            }
            val matchingCashier = cashiers.find { it.pin.trim() == trimmed && it.statusAktif }
            val isAuthorized = when {
                matchingCashier != null && (matchingCashier.isOwner() || matchingCashier.isAdmin() || matchingCashier.isSupervisor() || matchingCashier.canReturKonsumen) -> {
                    Pair(true, "Otorisasi ${matchingCashier.nama} disetujui")
                }
                trimmed == "1234" && (cashiers.isEmpty() || cashiers.any { it.isOwner() && it.pin.trim() == "1234" }) -> {
                    Pair(true, "Otorisasi Owner disetujui")
                }
                else -> {
                    Pair(false, "PIN otorisasi salah! Tidak memiliki hak retur konsumen.")
                }
            }
            if (!isAuthorized.first) {
                onComplete(false, isAuthorized.second)
                return@launch
            }
            val returCartItem = CartItem(
                product = product,
                qty = qty,
                isRetur = true,
                note = "Retur BON $bonNo ($reason)"
            )
            _cartItems.value = _cartItems.value + returCartItem
            onComplete(true, "Barang retur '${product.nama}' berhasil ditambahkan ke transaksi (${isAuthorized.second})")
        }
    }

    fun voidItem(
        index: Int,
        otp: String,
        reason: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val trimmed = otp.trim()
            if (trimmed.isEmpty()) {
                onComplete(false, "PIN / Password otorisasi tidak boleh kosong!")
                return@launch
            }

            // Validasi langsung dengan data kasir di database Room
            val cashiers = try {
                repository.getAllCashiersList()
            } catch (e: Exception) {
                allCashiers.value
            }

            // Cari kasir dengan PIN cocok dan berstatus aktif
            val authorizedStaff = cashiers.find { it.pin.trim() == trimmed && it.statusAktif }

            val authResult: Pair<Boolean, String> = when {
                authorizedStaff == null -> {
                    // Cek jika database belum memiliki akun lain dan owner default 1234
                    if (trimmed == "1234" && (cashiers.isEmpty() || cashiers.any { it.isOwner() && it.pin.trim() == "1234" })) {
                        Pair(true, "Otorisasi Owner disetujui")
                    } else {
                        Pair(false, "PIN / Password otorisasi salah! Anda tidak memiliki wewenang untuk tindakan ini.")
                    }
                }
                authorizedStaff.isOwner() || authorizedStaff.isAdmin() -> {
                    Pair(true, "Otorisasi ${authorizedStaff.role} (${authorizedStaff.nama}) disetujui")
                }
                authorizedStaff.isSupervisor() -> {
                    Pair(true, "Otorisasi Supervisor (${authorizedStaff.nama}) disetujui")
                }
                authorizedStaff.canVoidItem -> {
                    Pair(true, "Otorisasi Kasir (${authorizedStaff.nama}) disetujui")
                }
                else -> {
                    Pair(false, "Akses Ditolak: Karyawan '${authorizedStaff.nama}' (${authorizedStaff.role}) tidak memiliki izin VOID item.")
                }
            }

            if (!authResult.first) {
                onComplete(false, authResult.second)
                return@launch
            }

            val currentList = _cartItems.value.toMutableList()
            if (index in currentList.indices) {
                val item = currentList[index]
                currentList[index] = item.copy(
                    isVoid = true,
                    voidReason = reason
                )
                _cartItems.value = currentList
                onComplete(true, "Item '${item.product.nama}' berhasil di-void (${authResult.second})")
            } else {
                onComplete(false, "Item tidak ditemukan")
            }
        }
    }

    fun submitReturnDoc(doc: ReturnDocEntity, items: List<TransactionItemEntity> = emptyList(), onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.insertReturnDoc(doc)
                onDone(true)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun submitReturnDoc(
        nrbNo: String,
        fakturAsal: String,
        supplierNama: String,
        jenisRetur: String,
        keterangan: String,
        status: String,
        totalNilai: Double,
        onDone: () -> Unit = {}
    ) {
        val doc = ReturnDocEntity(
            nrbNo = nrbNo,
            tipeRetur = jenisRetur,
            refNo = fakturAsal,
            fakturAsal = fakturAsal,
            supplierNama = supplierNama,
            jenisRetur = jenisRetur,
            tanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
            totalNilai = totalNilai,
            alasan = keterangan,
            keteranganRetur = keterangan,
            status = status
        )
        submitReturnDoc(doc) { onDone() }
    }

    fun recordReturnPurchase(
        nrbNo: String,
        sumberTipe: String,
        sumberNama: String,
        fakturAsal: String,
        petugasNama: String,
        alasanUtama: String,
        items: List<Pair<ProductEntity, Pair<Int, Double>>>,
        itemReasons: Map<String, String> = emptyMap(),
        itemUnits: Map<String, Pair<String, Int>> = emptyMap(),
        isFixed: Boolean,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val jsonArr = org.json.JSONArray()
                var totalNilai = 0.0
                var totalQty = 0
                val itemsForStock = mutableListOf<Pair<String, Int>>()

                for ((prod, qtyAndPrice) in items) {
                    val (qty, price) = qtyAndPrice
                    val subtotal = price * qty
                    totalNilai += subtotal
                    totalQty += qty
                    val unitInfo = itemUnits[prod.plu] ?: Pair(prod.satuan, 1)
                    val konversi = maxOf(1, unitInfo.second)
                    itemsForStock.add(prod.plu to (qty * konversi))

                    val reason = itemReasons[prod.plu] ?: alasanUtama
                    val obj = org.json.JSONObject().apply {
                        put("plu", prod.plu)
                        put("barcode", prod.barcode)
                        put("nama", prod.nama)
                        put("satuan", unitInfo.first)
                        put("konversi", konversi)
                        put("qty", qty)
                        put("hargaBeli", price)
                        put("subtotal", subtotal)
                        put("alasan", reason)
                    }
                    jsonArr.put(obj)
                }

                val doc = ReturnDocEntity(
                    nrbNo = nrbNo,
                    tipeRetur = if (sumberTipe == "TOKO_GROSIR") "TOKO_GROSIR" else "SUPPLIER",
                    refNo = fakturAsal,
                    fakturAsal = fakturAsal,
                    supplierNama = sumberNama,
                    jenisRetur = if (sumberTipe == "TOKO_GROSIR") "RETUR TOKO GROSIR" else "RETUR SUPPLIER",
                    tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                    totalNilai = totalNilai,
                    alasan = alasanUtama,
                    keteranganRetur = alasanUtama,
                    status = if (isFixed) "FIXED" else "FORM",
                    timestamp = System.currentTimeMillis(),
                    petugasNama = petugasNama.ifBlank { _currentCashier.value?.nama ?: "Administrator" },
                    tokoTujuan = if (sumberTipe == "TOKO_GROSIR") "Toko Grosir" else "Supplier",
                    totalQty = totalQty,
                    itemsJson = jsonArr.toString()
                )

                repository.recordReturnPurchase(doc, itemsForStock, isFixed)
                onDone(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false)
            }
        }
    }

    fun recordCustomerReturn(
        nrkNo: String,
        bonNo: String,
        pelangganNama: String,
        petugasNama: String,
        alasanUtama: String,
        items: List<Pair<TransactionItemEntity, Pair<Int, String>>>, // item to (qty, reason)
        kembalikanKeStok: Boolean,
        metodePengembalian: String = "TUNAI",
        onDone: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                var totalNilai = 0.0
                var totalQty = 0
                val jsonArr = org.json.JSONArray()
                val itemsToRestock = mutableListOf<Pair<String, Int>>()

                for ((item, qtyAndReason) in items) {
                    val (qty, reason) = qtyAndReason
                    val subtotal = item.hargaSatuan * qty
                    totalNilai += subtotal
                    totalQty += qty
                    if (kembalikanKeStok) {
                        itemsToRestock.add(item.plu to qty)
                    }

                    val obj = org.json.JSONObject().apply {
                        put("plu", item.plu)
                        put("barcode", item.barcode)
                        put("nama", item.namaBarang)
                        put("satuan", "PCS")
                        put("qty", qty)
                        put("hargaSatuan", item.hargaSatuan)
                        put("subtotal", subtotal)
                        put("alasan", reason)
                    }
                    jsonArr.put(obj)
                }

                val doc = ReturnDocEntity(
                    nrbNo = nrkNo,
                    tipeRetur = "KONSUMEN",
                    refNo = bonNo,
                    fakturAsal = bonNo,
                    supplierNama = pelangganNama.ifBlank { "Konsumen Umum" },
                    jenisRetur = "RETUR PENJUALAN ($metodePengembalian)",
                    tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                    totalNilai = totalNilai,
                    alasan = alasanUtama,
                    keteranganRetur = "$alasanUtama (Metode: $metodePengembalian)",
                    status = "SELESAI",
                    timestamp = System.currentTimeMillis(),
                    petugasNama = petugasNama.ifBlank { _currentCashier.value?.nama ?: "Kasir" },
                    tokoTujuan = "Toko Utama",
                    totalQty = totalQty,
                    itemsJson = jsonArr.toString()
                )

                repository.recordCustomerReturn(doc, itemsToRestock)
                onDone(true, "Retur penjualan $nrkNo berhasil diproses! Total refund: Rp ${totalNilai.toLong()}")
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false, "Gagal memproses retur penjualan: ${e.message}")
            }
        }
    }

    fun finalizeReturnDoc(doc: ReturnDocEntity, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val itemsToDeduct = mutableListOf<Pair<String, Int>>()
                if (doc.itemsJson.isNotBlank()) {
                    try {
                        val arr = org.json.JSONArray(doc.itemsJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val plu = obj.optString("plu")
                            val qty = obj.optInt("qty", 0)
                            val konversi = maxOf(1, obj.optInt("konversi", 1))
                            val totalDeduct = qty * konversi
                            if (plu.isNotBlank() && totalDeduct > 0) {
                                itemsToDeduct.add(plu to totalDeduct)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                repository.finalizeReturnDoc(doc.nrbNo, itemsToDeduct)
                onDone(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onDone(false)
            }
        }
    }

    fun deleteReturnDoc(nrbNo: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteReturnDoc(nrbNo)
            onDone()
        }
    }

    fun processCheckout(
        caraBayar: String,
        bayarTunai: Double,
        bayarNonTunai: Double,
        kembalian: Double,
        donasi: Double,
        refPembayaran: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        val validation = validateTransactionForCheckout()
        if (!validation.first) {
            onComplete(false, validation.second)
            return
        }
        val config = storeConfig.value
        if (config.isTrial && allTransactions.value.size >= TRIAL_TRANSACTION_LIMIT) {
            onComplete(false, "Batas maksimal $TRIAL_TRANSACTION_LIMIT transaksi masa Trial telah tercapai. Silakan aktivasi lisensi Pro atau Enterprise untuk melanjutkan tanpa batasan.")
            return
        }
        val items = _cartItems.value
        val grand = grandTotal.value + donasi
        if (grand <= 0.0) {
            onComplete(false, "Total transaksi Rp 0! Transaksi dengan nilai Rp 0 tidak dapat diproses.")
            return
        }

        viewModelScope.launch {
            val bonNo = _currentBonNo.value
            val kasir = _currentCashier.value ?: CashierUserEntity("001", "Kasir Default", "1111")
            val sub = subtotal.value

            val txItems = items.map { item ->
                TransactionItemEntity(
                    bonNo = bonNo,
                    plu = item.product.plu,
                    barcode = item.product.barcode,
                    nama = item.product.nama,
                    hargaJual = item.unitPrice,
                    qty = if (item.isRetur) -item.qty else item.qty,
                    diskonItem = (item.product.hargaJual - item.unitPrice) * item.qty + item.customDiscount,
                    subtotalItem = item.subtotal,
                    satuan = item.product.satuan,
                    isPromo = item.isPromoApplied || item.isGrosirApplied,
                    promoNote = if (item.isRetur) "Retur" else if (item.isGrosirApplied) "Grosir" else item.product.promoLabel
                )
            }

            val success = repository.checkoutTransaction(
                bonNo = bonNo,
                kasirNik = kasir.nik,
                kasirNama = kasir.nama,
                memberNo = _currentMember.value?.memberNo ?: "",
                subtotal = sub,
                diskonTotal = (sub - grandTotal.value).coerceAtLeast(0.0),
                donasi = donasi,
                grandTotal = grand,
                caraBayar = caraBayar,
                bayarTunai = bayarTunai,
                bayarNonTunai = bayarNonTunai,
                kembalian = kembalian,
                poinDigunakan = _appliedPoin.value,
                nilaiPotonganPoin = _appliedPoin.value * 100.0,
                kodeVoucher = _appliedVoucherCode.value,
                nilaiPotonganVoucher = _appliedVoucherDiscount.value,
                refPembayaran = refPembayaran,
                items = txItems
            )

            if (success) {
                clearCart()
                _currentBonNo.value = generateNewBonNo()
                onComplete(true, bonNo)
            } else {
                onComplete(false, "Gagal memproses transaksi di database")
            }
        }
    }

    fun processCheckout(
        caraBayar: String,
        bayarTunai: Double,
        bayarNonTunai: Double,
        refPembayaran: String = "",
        piutangCustomerName: String = "",
        piutangCustomerPhone: String = "",
        piutangCustomerAddress: String = "",
        piutangDueDate: String = "",
        piutangNotes: String = "",
        onComplete: (Boolean, String, TransactionEntity?) -> Unit
    ) {
        val validation = validateTransactionForCheckout()
        if (!validation.first) {
            onComplete(false, validation.second, null)
            return
        }
        val config = storeConfig.value
        if (config.isTrial && allTransactions.value.size >= TRIAL_TRANSACTION_LIMIT) {
            onComplete(false, "Batas maksimal $TRIAL_TRANSACTION_LIMIT transaksi masa Trial telah tercapai. Silakan aktivasi lisensi Pro atau Enterprise untuk melanjutkan tanpa batasan.", null)
            return
        }
        val items = _cartItems.value
        val grand = grandTotal.value
        if (grand <= 0.0) {
            onComplete(false, "Total transaksi Rp 0! Transaksi dengan nilai Rp 0 tidak dapat diproses.", null)
            return
        }

        viewModelScope.launch {
            val bonNo = _currentBonNo.value
            val kasir = _currentCashier.value ?: CashierUserEntity("001", "Kasir Default", "1111")
            val sub = subtotal.value
            val kembalian = if (caraBayar == "TUNAI") {
                (bayarTunai - grand).coerceAtLeast(0.0)
            } else if (caraBayar == "CAMPURAN") {
                (bayarTunai + bayarNonTunai - grand).coerceAtLeast(0.0)
            } else {
                0.0
            }

            val txItems = items.map { item ->
                TransactionItemEntity(
                    bonNo = bonNo,
                    plu = item.product.plu,
                    barcode = item.product.barcode,
                    nama = item.product.nama,
                    hargaJual = item.unitPrice,
                    qty = if (item.isRetur) -item.qty else item.qty,
                    diskonItem = (item.product.hargaJual - item.unitPrice) * item.qty + item.customDiscount,
                    subtotalItem = item.subtotal,
                    satuan = item.product.satuan,
                    isPromo = item.isPromoApplied || item.isGrosirApplied,
                    promoNote = if (item.isRetur) "Retur" else if (item.isGrosirApplied) "Grosir" else item.product.promoLabel
                )
            }

            val config = storeConfig.value
            var voucherDiberikan = ""
            var voucherDiberikanNominal = 0.0
            var voucherDiberikanExpiry = ""

            // Otomatis terbitkan voucher acak unik untuk tiap struk jika diaktifkan dan memenuhi syarat
            if (config.voucherAutoGenerateOnReceipt && grand >= config.voucherMinBelanjaSyarat) {
                val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
                val prefix = if (config.voucherPrefix.isNotBlank()) config.voucherPrefix.trim().uppercase() else "VCR"
                val randomSuffix = (1..6).map { chars.random() }.joinToString("")
                voucherDiberikan = "$prefix-$randomSuffix"
                voucherDiberikanNominal = config.voucherDefaultNominal

                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, config.voucherMasaBerlakuHari.coerceAtLeast(1))
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                voucherDiberikanExpiry = sdf.format(cal.time)
                val todayStr = sdf.format(java.util.Date())

                val newVoucher = VoucherEntity(
                    kodeVoucher = voucherDiberikan,
                    nominalDiskon = voucherDiberikanNominal,
                    minBelanja = config.voucherMinBelanjaPakai,
                    tanggalMulai = todayStr,
                    tanggalKedaluwarsa = voucherDiberikanExpiry,
                    isUsed = false,
                    bonNoAsal = bonNo,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertVoucher(newVoucher)
            }

            val poinDiscountNominal = if (config.poinAktif) _appliedPoin.value * config.nilaiPerPoin else 0.0

            val success = repository.checkoutTransaction(
                bonNo = bonNo,
                kasirNik = kasir.nik,
                kasirNama = kasir.nama,
                memberNo = _currentMember.value?.memberNo ?: "",
                subtotal = sub,
                diskonTotal = (sub - grandTotal.value).coerceAtLeast(0.0),
                donasi = 0.0,
                grandTotal = grand,
                caraBayar = caraBayar,
                bayarTunai = bayarTunai,
                bayarNonTunai = bayarNonTunai,
                kembalian = kembalian,
                poinDigunakan = _appliedPoin.value,
                nilaiPotonganPoin = poinDiscountNominal,
                kodeVoucher = _appliedVoucherCode.value,
                nilaiPotonganVoucher = _appliedVoucherDiscount.value,
                refPembayaran = refPembayaran,
                items = txItems,
                voucherDiberikan = voucherDiberikan,
                voucherDiberikanNominal = voucherDiberikanNominal,
                voucherDiberikanExpiry = voucherDiberikanExpiry,
                piutangCustomerName = piutangCustomerName,
                piutangCustomerPhone = piutangCustomerPhone,
                piutangCustomerAddress = piutangCustomerAddress,
                piutangDueDate = piutangDueDate,
                piutangNotes = piutangNotes
            )

            if (success) {
                val tx = TransactionEntity(
                    bonNo = bonNo,
                    kasirNik = kasir.nik,
                    kasirNama = kasir.nama,
                    memberNo = _currentMember.value?.memberNo ?: "",
                    subtotal = sub,
                    diskonTotal = (sub - grandTotal.value).coerceAtLeast(0.0),
                    donasi = 0.0,
                    grandTotal = grand,
                    caraBayar = caraBayar,
                    bayarTunai = bayarTunai,
                    bayarNonTunai = bayarNonTunai,
                    kembalian = kembalian,
                    poinDigunakan = _appliedPoin.value,
                    nilaiPotonganPoin = poinDiscountNominal,
                    kodeVoucher = _appliedVoucherCode.value,
                    nilaiPotonganVoucher = _appliedVoucherDiscount.value,
                    refPembayaran = refPembayaran,
                    voucherDiberikan = voucherDiberikan,
                    voucherDiberikanNominal = voucherDiberikanNominal,
                    voucherDiberikanExpiry = voucherDiberikanExpiry,
                    timestamp = System.currentTimeMillis()
                )
                clearCart()
                _appliedVoucherCode.value = ""
                _appliedVoucherDiscount.value = 0.0
                _appliedPoin.value = 0
                _currentBonNo.value = generateNewBonNo()

                // Push completed sale to server if this is a Client device
                if (syncManager.syncConfig.value.role == com.example.data.sync.SyncRole.CLIENT) {
                    viewModelScope.launch {
                        syncManager.pushSaleToServer(
                            bonNo = bonNo,
                            cashierNik = kasir.nik,
                            memberNo = _currentMember.value?.memberNo,
                            subtotal = sub,
                            discount = (sub - grandTotal.value).coerceAtLeast(0.0),
                            grandTotal = grand,
                            paymentMethod = caraBayar,
                            nominalBayar = bayarTunai,
                            nominalKembalian = kembalian,
                            items = items,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }

                onComplete(true, "Transaksi berhasil diproses", tx)
            } else {
                onComplete(false, "Gagal memproses transaksi di database", null)
            }
        }
    }

    fun processCheckoutPiutang(
        customerName: String,
        customerPhone: String = "",
        customerAddress: String = "",
        dueDate: String = "",
        notes: String = "",
        dpAmount: Double = 0.0,
        onComplete: (Boolean, String, TransactionEntity?) -> Unit
    ) {
        val ref = "PIUTANG: $customerName${if (dueDate.isNotBlank()) " (Jatuh Tempo: $dueDate)" else ""}".trim()
        processCheckout(
            caraBayar = "PIUTANG",
            bayarTunai = dpAmount,
            bayarNonTunai = 0.0,
            refPembayaran = ref,
            piutangCustomerName = customerName,
            piutangCustomerPhone = customerPhone,
            piutangCustomerAddress = customerAddress,
            piutangDueDate = dueDate,
            piutangNotes = notes,
            onComplete = onComplete
        )
    }

    fun recordReceivablePayment(
        receivableId: Long,
        amount: Double,
        method: String = "TUNAI",
        notes: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        if (amount <= 0.0) {
            onComplete(false, "Nominal pembayaran harus lebih besar dari Rp 0")
            return
        }
        viewModelScope.launch {
            val cashierName = _currentCashier.value?.nama ?: "Kasir / Admin"
            val success = repository.payReceivable(
                receivableId = receivableId,
                amount = amount,
                method = method,
                notes = notes,
                kasirNama = cashierName
            )
            if (success) {
                onComplete(true, "Pembayaran piutang berhasil dicatat!")
            } else {
                onComplete(false, "Gagal mencatat pembayaran piutang")
            }
        }
    }

    fun addManualReceivable(
        customerName: String,
        customerPhone: String = "",
        customerAddress: String = "",
        memberNo: String = "",
        totalAmount: Double,
        dpAmount: Double = 0.0,
        dueDate: String = "",
        notes: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        if (customerName.isBlank()) {
            onComplete(false, "Nama pelanggan / debitur wajib diisi")
            return
        }
        if (totalAmount <= 0.0) {
            onComplete(false, "Total piutang harus lebih besar dari Rp 0")
            return
        }
        viewModelScope.launch {
            val cashier = _currentCashier.value ?: CashierUserEntity("001", "Administrator", "1111")
            val sisa = (totalAmount - dpAmount).coerceAtLeast(0.0)
            val status = if (sisa <= 0.0) "LUNAS" else if (dpAmount > 0.0) "SEBAGIAN" else "BELUM_LUNAS"
            val todayStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val bonNo = "PIUTANG-M-" + System.currentTimeMillis().toString().takeLast(6)

            val rec = ReceivableEntity(
                bonNo = bonNo,
                customerName = customerName.trim(),
                customerPhone = customerPhone.trim(),
                customerAddress = customerAddress.trim(),
                memberNo = memberNo.trim(),
                totalAmount = totalAmount,
                paidAmount = dpAmount,
                remainingAmount = sisa,
                status = status,
                tanggal = todayStr,
                dueDate = dueDate.ifBlank { todayStr },
                notes = notes.trim(),
                kasirNik = cashier.nik,
                kasirNama = cashier.nama
            )
            val recId = repository.addReceivable(rec)
            if (recId > 0 && dpAmount > 0.0) {
                repository.payReceivable(
                    receivableId = recId,
                    amount = dpAmount,
                    method = "TUNAI",
                    notes = "Uang Muka (DP) Awal",
                    kasirNama = cashier.nama
                )
            }
            onComplete(true, "Data piutang berhasil ditambahkan!")
        }
    }

    fun deleteReceivable(id: Long, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteReceivable(id)
                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }

    suspend fun getPaymentsForReceivable(receivableId: Long): List<ReceivablePaymentEntity> {
        return repository.getPaymentsForReceivable(receivableId)
    }


    // Multi-Device Synchronization Controls
    fun updateSyncConfig(newConfig: com.example.data.sync.SyncConfig) {
        syncManager.saveConfig(newConfig)
    }

    fun syncCatalogFromServer(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = syncManager.syncCatalogFromServer()
            onDone(result.first, result.second)
        }
    }

    fun testSyncConnection(onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val localPending = _pendingCarts.value.size + (if (_cartItems.value.isNotEmpty()) 1 else 0)
            val nik = _currentCashier.value?.nik ?: "001"
            val name = _currentCashier.value?.nama ?: "Kasir"
            val res = syncManager.sendClientHeartbeat(localPending, nik, name)
            onDone(res.first, res.second)
        }
    }

    fun checkMultiDeviceEodPending(onResult: (com.example.data.sync.EodPendingCheckResult) -> Unit) {
        viewModelScope.launch {
            val localPending = _pendingCarts.value.size + (if (_cartItems.value.isNotEmpty()) 1 else 0)
            val nik = _currentCashier.value?.nik ?: "001"
            val name = _currentCashier.value?.nama ?: "Kasir"
            val result = syncManager.checkEodPendingStatus(localPending, nik, name)
            onResult(result)
        }
    }

    // Voucher Management
    fun insertVoucher(voucher: VoucherEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.insertVoucher(voucher)
            onDone?.invoke()
        }
    }

    fun updateVoucher(voucher: VoucherEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateVoucher(voucher)
            onDone?.invoke()
        }
    }

    fun deleteVoucher(voucher: VoucherEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteVoucher(voucher)
            onDone?.invoke()
        }
    }

    fun generateManualRandomVoucher(
        nominal: Double,
        minBelanja: Double,
        masaBerlakuHari: Int,
        customCode: String = "",
        onDone: (VoucherEntity) -> Unit
    ) {
        viewModelScope.launch {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val code = if (customCode.isNotBlank()) customCode.trim().uppercase() else "VCR-" + (1..6).map { chars.random() }.joinToString("")
            val cal = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val todayStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, masaBerlakuHari.coerceAtLeast(1))
            val expStr = sdf.format(cal.time)

            val v = VoucherEntity(
                kodeVoucher = code,
                nominalDiskon = nominal,
                minBelanja = minBelanja,
                tanggalMulai = todayStr,
                tanggalKedaluwarsa = expStr,
                isUsed = false,
                bonNoAsal = "MANUAL",
                timestamp = System.currentTimeMillis()
            )
            repository.insertVoucher(v)
            onDone(v)
        }
    }

    fun batchGenerateVouchers(
        count: Int,
        nominal: Double,
        minBelanja: Double,
        masaBerlakuHari: Int,
        prefix: String = "VCR",
        onDone: (Int) -> Unit
    ) {
        viewModelScope.launch {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val pfx = if (prefix.isNotBlank()) prefix.trim().uppercase() else "VCR"
            val cal = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val todayStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, masaBerlakuHari.coerceAtLeast(1))
            val expStr = sdf.format(cal.time)

            val list = mutableListOf<VoucherEntity>()
            for (i in 1..count.coerceIn(1, 100)) {
                val randSuffix = (1..6).map { chars.random() }.joinToString("")
                val code = "$pfx-$randSuffix"
                list.add(
                    VoucherEntity(
                        kodeVoucher = code,
                        nominalDiskon = nominal,
                        minBelanja = minBelanja,
                        tanggalMulai = todayStr,
                        tanggalKedaluwarsa = expStr,
                        isUsed = false,
                        bonNoAsal = "BATCH",
                        timestamp = System.currentTimeMillis() + i
                    )
                )
            }
            repository.insertVouchers(list)
            onDone(list.size)
        }
    }

    fun toggleVoucherUsedStatus(voucher: VoucherEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            val updated = voucher.copy(
                isUsed = !voucher.isUsed,
                usedAtBonNo = if (!voucher.isUsed) "MANUAL-APPLIED" else "",
                usedAtTimestamp = if (!voucher.isUsed) System.currentTimeMillis() else 0L
            )
            repository.updateVoucher(updated)
            onDone?.invoke()
        }
    }

    fun adjustMemberPoints(memberNo: String, deltaPoints: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (deltaPoints >= 0) {
                repository.addMemberPoints(memberNo, deltaPoints)
            } else {
                repository.deductMemberPoints(memberNo, -deltaPoints)
            }
            onDone?.invoke()
        }
    }

    fun setMemberPoints(memberNo: String, newPoints: Int, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.setMemberPoints(memberNo, newPoints.coerceAtLeast(0))
            onDone?.invoke()
        }
    }

    // Clerek
    fun submitClerek(
        uangSetoranFisik: Double,
        onComplete: (ClerekEntity) -> Unit
    ) {
        viewModelScope.launch {
            val kasir = _currentCashier.value ?: CashierUserEntity("11111111", "Owner", "1234", "OWNER")
            val nik = kasir.nik
            val record = repository.calculateClerek(
                kasirNik = kasir.nik,
                modalKasir = _shiftModalAwal.value,
                uangSetoranFisik = uangSetoranFisik,
                shiftNum = _activeShift.value
            )
            // Shift aktif untuk kasir ini ditutup setelah proses clerek selesai
            _isShiftOpen.value = false
            shiftPrefs.edit()
                .putBoolean("is_shift_open_$nik", false)
                .putBoolean("is_shift_open", false)
                .apply()
            clearCart()
            onComplete(record)
        }
    }

    fun submitClerek(
        modalAwal: Double,
        uangSetoranFisik: Double,
        onComplete: (ClerekEntity) -> Unit
    ) {
        _shiftModalAwal.value = modalAwal
        submitClerek(uangSetoranFisik, onComplete)
    }

    fun submitClerek(
        entity: ClerekEntity,
        onComplete: (ClerekEntity) -> Unit = {}
    ) {
        submitClerek(entity.uangSetoranFisik, onComplete)
    }

    // Shift management
    fun openShift(shiftNum: Int, modalAwal: Double) {
        val nik = _currentCashier.value?.nik ?: "default"
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        _activeShift.value = shiftNum
        _shiftModalAwal.value = modalAwal
        _isShiftOpen.value = true
        _shiftOpenedAt.value = timeNow
        shiftPrefs.edit()
            .putBoolean("is_shift_open_$nik", true)
            .putInt("active_shift_$nik", shiftNum)
            .putFloat("shift_modal_awal_$nik", modalAwal.toFloat())
            .putString("shift_opened_at_$nik", timeNow)
            .putBoolean("is_shift_open", true)
            .putInt("active_shift", shiftNum)
            .putFloat("shift_modal_awal", modalAwal.toFloat())
            .putString("shift_opened_at", timeNow)
            .apply()
    }

    fun closeShift() {
        val nik = _currentCashier.value?.nik ?: "default"
        _isShiftOpen.value = false
        shiftPrefs.edit()
            .putBoolean("is_shift_open_$nik", false)
            .putBoolean("is_shift_open", false)
            .apply()
    }

    fun lockScreen() {
        _isScreenLocked.value = true
    }

    fun unlockScreen(pin: String): Boolean {
        val cashier = _currentCashier.value ?: return false
        return if (cashier.pin == pin) {
            _isScreenLocked.value = false
            true
        } else {
            false
        }
    }

    fun logoutCashier() {
        if (_cartItems.value.isNotEmpty()) {
            val kasirName = _currentCashier.value?.nama ?: "Kasir"
            val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            holdCurrentCart("Logout Kasir: $kasirName ($time)")
        }
        _isUserLoggedIn.value = false
        _currentCashier.value = null
        _isShiftOpen.value = false // Saat user logout, session kasir ditutup sehingga kasir berikutnya wajib buka shift
        shiftPrefs.edit()
            .putBoolean("is_user_logged_in", false)
            .remove("last_logged_in_nik")
            .apply()
        clearCart()
    }

    fun loginCashier(nik: String, pass: String, onResult: (Boolean, String) -> Unit) {
        val trimmedNik = nik.trim()
        val trimmedPass = pass.trim()
        viewModelScope.launch {
            val cashier = repository.getCashierByNik(trimmedNik)
            if (cashier != null && cashier.pin == trimmedPass) {
                _currentCashier.value = cashier
                _isUserLoggedIn.value = true
                loadShiftForCashier(cashier.nik)
                shiftPrefs.edit()
                    .putBoolean("is_user_logged_in", true)
                    .putString("last_logged_in_nik", cashier.nik)
                    .apply()
                onResult(true, "Selamat datang, ${cashier.nama}")
            } else if (trimmedNik == "11111111" && trimmedPass == "1234") {
                val owner = CashierUserEntity("11111111", "Owner", "1234", "OWNER")
                repository.saveCashier(owner)
                _currentCashier.value = owner
                _isUserLoggedIn.value = true
                loadShiftForCashier(owner.nik)
                shiftPrefs.edit()
                    .putBoolean("is_user_logged_in", true)
                    .putString("last_logged_in_nik", owner.nik)
                    .apply()
                onResult(true, "Selamat datang, ${owner.nama}")
            } else {
                onResult(false, "NIK atau PIN tidak cocok")
            }
        }
    }

    fun saveOrUpdateProduct(product: ProductEntity, isEdit: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            if (isEdit) {
                repository.updateProduct(product)
            } else {
                repository.insertProduct(product)
            }
            onDone()
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun saveMember(member: MemberEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveMember(member)
            onDone()
        }
    }

    fun updateMember(member: MemberEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateMember(member)
            onDone()
        }
    }

    fun deleteMember(member: MemberEntity) {
        viewModelScope.launch {
            repository.deleteMember(member)
        }
    }

    fun saveCashierUser(cashier: CashierUserEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveCashier(cashier)
            if (_currentCashier.value?.nik == cashier.nik) {
                _currentCashier.value = cashier
            }
            onDone()
        }
    }

    fun updateCashierUser(cashier: CashierUserEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCashier(cashier)
            if (_currentCashier.value?.nik == cashier.nik) {
                _currentCashier.value = cashier
            }
            onDone()
        }
    }

    fun deleteCashierUser(cashier: CashierUserEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCashier(cashier)
            onDone()
        }
    }

    fun saveShiftConfig(shift: ShiftConfigEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveShiftConfig(shift)
            onDone()
        }
    }

    fun updateShiftConfig(shift: ShiftConfigEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.updateShiftConfig(shift)
            onDone()
        }
    }

    fun updateStoreConfig(config: StoreConfigEntity) {
        viewModelScope.launch {
            repository.updateStoreConfig(config)
        }
    }

    fun exportBackupToFile(onDone: (Boolean, File?) -> Unit) {
        exportBackupToFile { success, file, _ -> onDone(success, file) }
    }

    fun exportBackupToFile(onDone: (Boolean, File?, String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDatabaseToJson()
            val (success, file) = DocExportManager.saveDocument(
                getApplication(),
                "Backup",
                "DodolanPro_AutoBackup_EOD",
                "json",
                json
            )
            onDone(success, file, json)
        }
    }

    fun executeEodBackup(
        onResult: (Boolean, File?, File?, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val json = repository.exportDatabaseToJson()
                val cfg = storeConfig.value
                val prefix = cfg.storeCode.ifBlank { "DODOLAN" }

                // 1. Simpan berkas cadangan database ke folder DodolanPro/Backup
                val (bSuccess, backupFile) = DocExportManager.saveDocument(
                    context,
                    "Backup",
                    "EOD_BACKUP_${prefix}",
                    "json",
                    json
                )

                // 2. Simpan juga salinan ke folder DodolanPro/EOD
                val (_, eodBackupCopy) = DocExportManager.saveDocument(
                    context,
                    "EOD",
                    "EOD_BACKUP_${prefix}",
                    "json",
                    json
                )

                // 3. Simpan berkas ringkasan EOD harian ke folder DodolanPro/EOD
                val todayStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                val txToday = allTransactions.value
                val clerekToday = allClerekRecords.value
                val totalOmset = txToday.sumOf { it.grandTotal }
                val totalTunai = txToday.sumOf { tx ->
                    when (tx.caraBayar) {
                        "TUNAI" -> {
                            val net = tx.bayarTunai - tx.kembalian
                            if (net > 0.0) net else tx.grandTotal
                        }
                        "CAMPURAN" -> (tx.bayarTunai - tx.kembalian).coerceAtLeast(0.0)
                        else -> 0.0
                    }
                }
                val totalNonTunai = txToday.sumOf { tx ->
                    when (tx.caraBayar) {
                        "TUNAI" -> 0.0
                        "CAMPURAN" -> if (tx.bayarNonTunai > 0.0) tx.bayarNonTunai else (tx.grandTotal - (tx.bayarTunai - tx.kembalian)).coerceAtLeast(0.0)
                        else -> tx.grandTotal
                    }
                }

                val summaryText = buildString {
                    appendLine("==========================================")
                    appendLine("       LAPORAN END OF DAY (EOD)")
                    appendLine("         ${cfg.storeName}")
                    appendLine("==========================================")
                    appendLine("Waktu EOD       : $todayStr")
                    appendLine("Kode Toko       : ${cfg.storeCode}")
                    appendLine("Petugas EOD     : ${_currentCashier.value?.nama ?: "Administrator"}")
                    appendLine("------------------------------------------")
                    appendLine("Total Transaksi : ${txToday.size} Transaksi")
                    appendLine("Total Omset     : ${formatRupiah(totalOmset)}")
                    appendLine("  • Tunai       : ${formatRupiah(totalTunai)}")
                    appendLine("  • Non-Tunai   : ${formatRupiah(totalNonTunai)}")
                    appendLine("Total Clerek    : ${clerekToday.size} Rekaman Shift")
                    appendLine("------------------------------------------")
                    appendLine("STATUS BACKUP DATA:")
                    appendLine("Berkas Backup   : ${backupFile.name}")
                    appendLine("Lokasi Android  : ${backupFile.parent}")
                    appendLine("Status Cadangan : BERHASIL DISIMPAN KE MEMORI")
                    appendLine("==========================================")
                }

                val (_, summaryFile) = DocExportManager.saveDocument(
                    context,
                    "EOD",
                    "EOD_RINGKASAN_${prefix}",
                    "txt",
                    summaryText
                )

                // Update status di terminal
                shiftPrefs.edit()
                    .putBoolean("is_shift_open", false)
                    .putBoolean("shift_opened_today", false)
                    .putString("last_eod_timestamp", todayStr)
                    .apply()

                onResult(bSuccess, backupFile, summaryFile, "Proses EOD dan pencadangan database berhasil disimpan ke folder Android.")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, null, null, "Gagal menjalankan backup EOD: ${e.message}")
            }
        }
    }

    val deviceId: String
        get() = syncConfig.value.deviceId

    fun activateOrUpgradeLicense(key: String, onComplete: (Boolean, String) -> Unit) {
        val cashier = _currentCashier.value
        if (cashier != null && !cashier.isOwner()) {
            onComplete(false, "Hanya Owner yang berhak mengaktifkan atau mengubah lisensi toko.")
            return
        }
        val devId = syncConfig.value.deviceId
        val (isValid, tierOrMsg) = com.example.util.PosLicenseHelper.validateLicenseKey(devId, key)
        if (isValid) {
            val currentCfg = storeConfig.value
            val updated = currentCfg.copy(
                isLicensed = true,
                licenseKey = key.trim().uppercase(),
                licenseType = tierOrMsg,
                licenseExpiryDate = "LIFETIME"
            )
            viewModelScope.launch {
                repository.updateStoreConfig(updated)
                onComplete(true, "Selamat! Lisensi $tierOrMsg berhasil diaktifkan.")
            }
        } else {
            onComplete(false, tierOrMsg)
        }
    }

    fun resetLicenseToTrial(onComplete: () -> Unit) {
        val cashier = _currentCashier.value
        if (cashier != null && !cashier.isOwner()) {
            return
        }
        val currentCfg = storeConfig.value
        val updated = currentCfg.copy(
            isLicensed = false,
            licenseKey = "",
            licenseType = "TRIAL",
            licenseExpiryDate = "TRIAL"
        )
        viewModelScope.launch {
            repository.updateStoreConfig(updated)
            onComplete()
        }
    }

    fun restoreBackup(jsonContent: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.restoreDatabaseFromJson(jsonContent)
                onDone(success)
            } catch (e: Exception) {
                onDone(false)
            }
        }
    }

    fun restoreBackup(file: File, onDone: (Boolean) -> Unit) {
        try {
            val json = file.readText(Charsets.UTF_8)
            restoreBackup(json, onDone)
        } catch (e: Exception) {
            onDone(false)
        }
    }

    fun restoreBackupFromUri(uri: android.net.Uri, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onDone(false, "Tidak dapat membuka berkas dari penyimpanan Android.")
                    }
                    return@launch
                }
                val json = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (json.isBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onDone(false, "Berkas backup kosong.")
                    }
                    return@launch
                }
                val success = repository.restoreDatabaseFromJson(json)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (success) {
                        onDone(true, "Database berhasil dipulihkan dari berkas storage!")
                    } else {
                        onDone(false, "Format berkas backup tidak valid / struktur data tidak sesuai.")
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onDone(false, "Gagal memulihkan backup: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }


    fun autoPendingCartOnEvent(reason: String = "") {
        if (_cartItems.value.isNotEmpty()) {
            val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
            val eventLabel = if (reason.isNotBlank()) {
                if (reason.contains(time)) reason else "$reason ($time)"
            } else {
                "Aplikasi Ditutup ($time)"
            }
            holdCurrentCart(eventLabel)
        }
    }

    fun resetToDemoData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.resetToDefaultDemoData()
            clearCart()
            onDone()
        }
    }

    fun saveOrUpdateProduct(product: ProductEntity) {
        saveOrUpdateProduct(product, false) {}
    }

    fun saveOrUpdateProduct(product: ProductEntity, onDone: () -> Unit) {
        saveOrUpdateProduct(product, false, onDone)
    }

    fun deleteProduct(plu: String) {
        viewModelScope.launch {
            val p = allProducts.value.find { it.plu == plu }
            if (p != null) repository.deleteProduct(p)
        }
    }

    fun saveMember(member: MemberEntity) {
        viewModelScope.launch { repository.saveMember(member) }
    }

    fun deleteMember(memberNo: String) {
        viewModelScope.launch {
            val m = allMembers.value.find { it.memberNo == memberNo }
            if (m != null) repository.deleteMember(m)
        }
    }

    fun deleteCashierUser(nik: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val c = allCashiers.value.find { it.nik == nik }
            if (c != null) {
                repository.deleteCashier(c)
            }
            onDone()
        }
    }

    fun recordStockOpname(so: StockOpnameEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recordStockOpname(so, emptyMap())
            onDone()
        }
    }

    fun recordStockOpname(
        soNo: String,
        rakFilter: String,
        adjustments: Map<String, Int>,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            var totalSelisihItem = 0
            var totalSelisihNilai = 0.0
            val allProds = allProducts.value
            val diffs = mutableMapOf<String, Int>()
            var totalFisikDihitung = 0
            var totalSistemDihitung = 0
            adjustments.forEach { (plu, count) ->
                totalFisikDihitung += count
                val prod = allProds.find { it.plu == plu }
                if (prod != null) {
                    totalSistemDihitung += prod.stok
                    val selisih = count - prod.stok
                    totalSelisihItem += selisih
                    totalSelisihNilai += selisih * prod.hargaBeli
                    diffs[plu] = selisih
                }
            }
            val so = StockOpnameEntity(
                soNo = soNo,
                tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                petugasNama = _currentCashier.value?.nama ?: "Petugas",
                totalItemDihitung = adjustments.size,
                totalSelisihFisik = totalFisikDihitung,
                rakFilter = rakFilter,
                totalSelisihItem = totalSelisihItem,
                totalSelisihNilai = totalSelisihNilai,
                keterangan = "Fisik: $totalFisikDihitung pcs | Sistem: $totalSistemDihitung pcs"
            )
            repository.recordStockOpname(so, diffs)
            onDone()
        }
    }

    fun addSupplier(
        nama: String,
        noHp: String = "",
        alamat: String = "",
        kontakPerson: String = "",
        keterangan: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val kode = "SUP-" + System.currentTimeMillis().toString().takeLast(4)
            val s = SupplierEntity(
                kode = kode,
                nama = nama.trim(),
                noHp = noHp.trim(),
                alamat = alamat.trim(),
                kontakPerson = kontakPerson.trim(),
                keterangan = keterangan.trim()
            )
            repository.insertSupplier(s)
            onDone()
        }
    }

    fun addStoreBranch(
        namaToko: String,
        alamat: String = "",
        telepon: String = "",
        keterangan: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val kode = "CAB-" + System.currentTimeMillis().toString().takeLast(4)
            val b = StoreBranchEntity(
                kodeToko = kode,
                namaToko = namaToko.trim(),
                alamat = alamat.trim(),
                telepon = telepon.trim(),
                keterangan = keterangan.trim()
            )
            repository.insertStoreBranch(b)
            onDone()
        }
    }

    suspend fun getPurchaseItems(faktur: String): List<PurchaseItemEntity> {
        return repository.getPurchaseItems(faktur)
    }

    suspend fun getItemsForBon(bonNo: String): List<TransactionItemEntity> {
        return repository.getItemsForBon(bonNo)
    }

    fun recordDirectPurchase(purchase: PurchaseEntity, items: List<PurchaseItemEntity>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.recordPurchase(purchase, items)
            onDone()
        }
    }

    fun recordDirectPurchase(
        faktur: String,
        supplier: String,
        tokoTujuan: String,
        petugasNama: String,
        keterangan: String,
        statusBayar: String = "LUNAS",
        uangMuka: Double = 0.0,
        sisaHutang: Double = 0.0,
        jatuhTempo: String = "",
        items: List<Pair<ProductEntity, Pair<Int, Double>>>,
        itemUnits: Map<String, Pair<String, Int>> = emptyMap(),
        costMethod: String = "AVERAGE",
        onDone: (PurchaseEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val purchaseItems = items.map { (prod, qtyAndPrice) ->
                val (qty, price) = qtyAndPrice
                val unitInfo = itemUnits[prod.plu] ?: Pair(prod.satuan, 1)
                PurchaseItemEntity(
                    noFakturBeli = faktur,
                    plu = prod.plu,
                    barcode = prod.barcode,
                    nama = prod.nama,
                    hargaBeli = price,
                    qty = qty,
                    subtotal = price * qty,
                    satuan = unitInfo.first,
                    konversi = maxOf(1, unitInfo.second)
                )
            }
            val totalNominal = purchaseItems.sumOf { it.subtotal }
            val totalItem = purchaseItems.sumOf { it.qty }
            val computedSisa = if (statusBayar == "LUNAS") 0.0 else maxOf(0.0, totalNominal - uangMuka)
            val finalStatus = if (statusBayar == "LUNAS" || computedSisa <= 0.0) "LUNAS" else if (uangMuka > 0) "SEBAGIAN" else "TEMPO"
            val purchase = PurchaseEntity(
                noFakturBeli = faktur,
                supplierNama = supplier,
                tanggal = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date()),
                totalBeli = totalNominal,
                statusBayar = finalStatus,
                keterangan = keterangan,
                totalItem = totalItem,
                totalNominal = totalNominal,
                kasirNik = _currentCashier.value?.nik ?: "ADMIN",
                petugasNama = petugasNama.ifBlank { _currentCashier.value?.nama ?: "Administrator" },
                tokoTujuan = tokoTujuan.ifBlank { "Toko Utama" },
                uangMuka = if (finalStatus == "LUNAS") totalNominal else uangMuka,
                sisaHutang = computedSisa,
                jatuhTempo = jatuhTempo
            )
            repository.recordPurchase(purchase, purchaseItems, updateCostMethod = costMethod)
            onDone(purchase)
        }
    }

    fun payPurchaseDebt(
        noFakturBeli: String,
        amount: Double,
        method: String = "TUNAI",
        notes: String = "",
        petugasNama: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordPurchasePayment(
                noFakturBeli = noFakturBeli,
                amount = amount,
                method = method,
                notes = notes,
                petugasNama = petugasNama.ifBlank { _currentCashier.value?.nama ?: "Administrator" }
            )
            onDone()
        }
    }

    fun updateProductUnit(plu: String, newSatuan: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            val prod = allProducts.value.find { it.plu == plu }
            if (prod != null && newSatuan.isNotBlank()) {
                repository.updateProduct(prod.copy(satuan = newSatuan.trim().uppercase()))
            }
            onDone()
        }
    }

    fun recordDirectPurchase(
        faktur: String,
        supplier: String,
        keterangan: String,
        items: List<Pair<ProductEntity, Pair<Int, Double>>>,
        onDone: () -> Unit = {}
    ) {
        recordDirectPurchase(
            faktur = faktur,
            supplier = supplier,
            tokoTujuan = "Toko Utama",
            petugasNama = _currentCashier.value?.nama ?: "Administrator",
            keterangan = keterangan,
            items = items,
            costMethod = "AVERAGE",
            onDone = { _ -> onDone() }
        )
    }

    fun seedRealisticTransactions(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.seedRealisticTransactions()
            onDone()
        }
    }
}

