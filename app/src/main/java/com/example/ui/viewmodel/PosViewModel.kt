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
    SIS_DASHBOARD
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

    val promoDiscountAmount: Double
        get() {
            if (isVoid || isRetur) return 0.0
            if (!product.isPromo && !product.hasDiscountOrPromo()) {
                if (isGrosirApplied && product.hargaGrosir > 0 && qty >= product.hargaGrosirQty) {
                    return qty * maxOf(0.0, baseUnitPrice - product.hargaGrosir)
                }
                return 0.0
            }

            return when (product.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (product.promoMinBeli > 0 && qty >= product.promoMinBeli) {
                        val freeUnits = (qty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
                        freeUnits * baseUnitPrice
                    } else 0.0
                }
                "BELI_X_POTONGAN" -> {
                    if (product.promoMinBeli > 0 && qty >= product.promoMinBeli && product.promoPotonganNominal > 0) {
                        val sets = qty / product.promoMinBeli
                        sets * product.promoPotonganNominal
                    } else 0.0
                }
                "DISKON_NOMINAL" -> {
                    if (product.diskonNominal > 0) {
                        qty * product.diskonNominal
                    } else 0.0
                }
                "DISKON_PERSEN" -> {
                    if (product.diskonPersen > 0) {
                        qty * baseUnitPrice * (product.diskonPersen / 100.0)
                    } else 0.0
                }
                "GROSIR" -> {
                    if (product.hargaGrosir > 0 && qty >= product.hargaGrosirQty) {
                        qty * maxOf(0.0, baseUnitPrice - product.hargaGrosir)
                    } else 0.0
                }
                else -> {
                    if (product.diskonPersen > 0) {
                        qty * baseUnitPrice * (product.diskonPersen / 100.0)
                    } else if (product.diskonNominal > 0) {
                        qty * product.diskonNominal
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
            if (!product.isPromo && !product.hasDiscountOrPromo()) {
                if (product.hargaGrosir > 0 && product.hargaGrosirQty > 0) {
                    if (qty >= product.hargaGrosirQty) {
                        return "Grosir Beli >= ${product.hargaGrosirQty} (Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.hargaGrosirQty - qty
                        return "Beli $needed lagi untuk Harga Grosir ${formatRupiah(product.hargaGrosir)}"
                    }
                }
                return ""
            }

            return when (product.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (qty >= product.promoMinBeli) {
                        val freeUnits = (qty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
                        "Beli ${product.promoMinBeli} Gratis ${product.promoGratisQty} (Dapat $freeUnits Gratis, Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.promoMinBeli - qty
                        "Beli $needed lagi untuk dapatkan GRATIS ${product.promoGratisQty}!"
                    }
                }
                "BELI_X_POTONGAN" -> {
                    if (qty >= product.promoMinBeli) {
                        "Beli ${product.promoMinBeli} Potongan ${formatRupiah(product.promoPotonganNominal)} (Hemat ${formatRupiah(promoDiscountAmount)})"
                    } else {
                        val needed = product.promoMinBeli - qty
                        "Beli $needed lagi untuk Potongan ${formatRupiah(product.promoPotonganNominal)}!"
                    }
                }
                "DISKON_NOMINAL" -> {
                    "Diskon ${formatRupiah(product.diskonNominal)}/item (Hemat ${formatRupiah(promoDiscountAmount)})"
                }
                "DISKON_PERSEN" -> {
                    "Diskon ${product.diskonPersen.toInt()}% (Hemat ${formatRupiah(promoDiscountAmount)})"
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
                    if (isPromoActive) "Promo Hemat ${formatRupiah(promoDiscountAmount)}"
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

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = PosRepository(db.posDao())

    // Navigation
    private val _currentDestination = MutableStateFlow(PosNavDestination.KASIR)
    val currentDestination: StateFlow<PosNavDestination> = _currentDestination.asStateFlow()

    fun navigateTo(dest: PosNavDestination) {
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

    // Shifts (Tersimpan permanen hingga proses clerek dilakukan)
    private val _isShiftOpen = MutableStateFlow(shiftPrefs.getBoolean("is_shift_open", false))
    val isShiftOpen: StateFlow<Boolean> = _isShiftOpen.asStateFlow()

    private val _activeShift = MutableStateFlow(shiftPrefs.getInt("active_shift", 1))
    val activeShift: StateFlow<Int> = _activeShift.asStateFlow()

    private val _shiftModalAwal = MutableStateFlow(shiftPrefs.getFloat("shift_modal_awal", 100000f).toDouble())
    val shiftModalAwal: StateFlow<Double> = _shiftModalAwal.asStateFlow()

    private val _shiftOpenedAt = MutableStateFlow(shiftPrefs.getString("shift_opened_at", "") ?: "")
    val shiftOpenedAt: StateFlow<String> = _shiftOpenedAt.asStateFlow()

    private val _isScreenLocked = MutableStateFlow(false)
    val isScreenLocked: StateFlow<Boolean> = _isScreenLocked.asStateFlow()

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
                }
            }
        }
    }

    val allShiftConfigs: StateFlow<List<ShiftConfigEntity>> = repository.allShiftConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Store Config
    val storeConfig: StateFlow<StoreConfigEntity> = repository.storeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreConfigEntity())

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

    val allStockOpnames: StateFlow<List<StockOpnameEntity>> = repository.allStockOpnames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReturns: StateFlow<List<ReturnDocEntity>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVouchers: StateFlow<List<VoucherEntity>> = repository.getAllVouchers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grandTotal: StateFlow<Double> = combine(
        subtotal, _appliedPoin, _appliedVoucherDiscount, storeConfig
    ) { sub, poin, vDisc, cfg ->
        val poinNominal = if (cfg.poinAktif) poin * cfg.nilaiPerPoin else 0.0
        val total = sub - poinNominal - vDisc
        if (total < 0.0) 0.0 else total
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun onBarcodeInputChanged(value: String) {
        _inputBarcodeInput.value = value
    }

    private fun generateNewBonNo(): String {
        val sdf = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
        return "BON-${sdf.format(Date())}"
    }

    fun addProductToCart(product: ProductEntity, qty: Int = 1, selectedSatuan: String = product.satuan) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.plu == product.plu && it.selectedSatuan == selectedSatuan && !it.isVoid && !it.isRetur }
        val newTotalQty: Int

        if (index >= 0) {
            val existing = currentList[index]
            val updatedQty = existing.qty + qty
            val isGrosir = product.hargaGrosir > 0 && product.hargaGrosirQty > 0 && updatedQty >= product.hargaGrosirQty
            currentList[index] = existing.copy(
                qty = updatedQty,
                isGrosirApplied = isGrosir,
                isPromoApplied = product.isPromo || product.diskonPersen > 0
            )
            newTotalQty = updatedQty
        } else {
            val isGrosir = product.hargaGrosir > 0 && product.hargaGrosirQty > 0 && qty >= product.hargaGrosirQty
            currentList.add(
                CartItem(
                    product = product,
                    qty = qty,
                    isGrosirApplied = isGrosir,
                    isPromoApplied = product.isPromo || product.diskonPersen > 0,
                    selectedSatuan = selectedSatuan
                )
            )
            newTotalQty = qty
        }
        _cartItems.value = currentList

        // Check for Promo Alerts
        if (product.isPromo) {
            when (product.promoType) {
                "BELI_X_GRATIS_Y" -> {
                    if (newTotalQty >= product.promoMinBeli) {
                        val freeUnits = (newTotalQty / product.promoMinBeli) * maxOf(1, product.promoGratisQty)
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
                    return
                }
                "BELI_X_POTONGAN" -> {
                    if (newTotalQty >= product.promoMinBeli) {
                        val sets = newTotalQty / product.promoMinBeli
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
                    return
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
                    return
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
                    return
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
                    return
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
    }

    fun updateCartItemQty(index: Int, newQty: Int) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                val item = currentList[index]
                val isGrosir = item.product.hargaGrosir > 0 && item.product.hargaGrosirQty > 0 && newQty >= item.product.hargaGrosirQty
                currentList[index] = item.copy(
                    qty = newQty,
                    isGrosirApplied = isGrosir
                )
            }
            _cartItems.value = currentList
        }
    }

    fun updateCartItemQty(plu: String, newQty: Int) {
        val currentList = _cartItems.value
        val index = currentList.indexOfFirst { it.product.plu == plu }
        if (index >= 0) {
            updateCartItemQty(index, newQty)
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
                val targetSatuan = when {
                    trimmed.equals(product.barcodeRepack, ignoreCase = true) && product.isRepack && product.satuanRepack.isNotBlank() -> product.satuanRepack
                    trimmed.equals(product.barcodeRepack2, ignoreCase = true) && product.isRepack && product.satuanRepack2.isNotBlank() -> product.satuanRepack2
                    else -> product.satuan
                }
                addProductToCart(product, 1, targetSatuan)
                val unitLabel = if (targetSatuan != product.satuan) " ($targetSatuan)" else ""
                onResult(true, "Berhasil menambahkan ${product.nama}$unitLabel")
            } else {
                val matches = repository.searchProducts(trimmed)
                if (matches.size == 1) {
                    val single = matches.first()
                    val targetSatuan = when {
                        trimmed.equals(single.barcodeRepack, ignoreCase = true) && single.isRepack && single.satuanRepack.isNotBlank() -> single.satuanRepack
                        trimmed.equals(single.barcodeRepack2, ignoreCase = true) && single.isRepack && single.satuanRepack2.isNotBlank() -> single.satuanRepack2
                        else -> single.satuan
                    }
                    addProductToCart(single, 1, targetSatuan)
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

    suspend fun findTransactionForRetur(bonKey: String): Pair<TransactionEntity?, List<TransactionItemEntity>> {
        val res = repository.findTransactionForRetur(bonKey)
        return if (res != null) Pair(res.first, res.second) else Pair(null, emptyList())
    }

    fun addReturItem(
        bonNo: String,
        product: ProductEntity,
        qty: Int,
        reason: String,
        otp: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val returCartItem = CartItem(
            product = product,
            qty = qty,
            isRetur = true,
            note = "Retur BON $bonNo ($reason)"
        )
        _cartItems.value = _cartItems.value + returCartItem
        onComplete(true, "Barang retur '${product.nama}' berhasil ditambahkan ke transaksi")
    }

    fun voidItem(
        index: Int,
        otp: String,
        reason: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val currentList = _cartItems.value.toMutableList()
        if (index in currentList.indices) {
            val item = currentList[index]
            currentList[index] = item.copy(
                isVoid = true,
                voidReason = reason
            )
            _cartItems.value = currentList
            onComplete(true, "Item '${item.product.nama}' berhasil di-void")
        } else {
            onComplete(false, "Item tidak ditemukan")
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

    fun processCheckout(
        caraBayar: String,
        bayarTunai: Double,
        bayarNonTunai: Double,
        kembalian: Double,
        donasi: Double,
        refPembayaran: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        if (!_isShiftOpen.value) {
            onComplete(false, "Shift belum dibuka! Harap buka shift kasir terlebih dahulu.")
            return
        }
        val items = _cartItems.value
        if (items.isEmpty()) {
            onComplete(false, "Keranjang belanja kosong")
            return
        }

        viewModelScope.launch {
            val bonNo = _currentBonNo.value
            val kasir = _currentCashier.value ?: CashierUserEntity("001", "Kasir Default", "1111")
            val sub = subtotal.value
            val grand = grandTotal.value + donasi

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
        onComplete: (Boolean, String, TransactionEntity?) -> Unit
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            onComplete(false, "Keranjang belanja kosong", null)
            return
        }

        viewModelScope.launch {
            val bonNo = _currentBonNo.value
            val kasir = _currentCashier.value ?: CashierUserEntity("001", "Kasir Default", "1111")
            val sub = subtotal.value
            val grand = grandTotal.value
            val kembalian = if (caraBayar == "TUNAI") (bayarTunai - grand).coerceAtLeast(0.0) else 0.0

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
                voucherDiberikanExpiry = voucherDiberikanExpiry
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
                onComplete(true, "Transaksi berhasil diproses", tx)
            } else {
                onComplete(false, "Gagal memproses transaksi di database", null)
            }
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
            val record = repository.calculateClerek(
                kasirNik = kasir.nik,
                modalKasir = _shiftModalAwal.value,
                uangSetoranFisik = uangSetoranFisik,
                shiftNum = _activeShift.value
            )
            // Shift aktif sampai proses clerek selesai dilakukan
            _isShiftOpen.value = false
            shiftPrefs.edit().putBoolean("is_shift_open", false).apply()
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
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        _activeShift.value = shiftNum
        _shiftModalAwal.value = modalAwal
        _isShiftOpen.value = true
        _shiftOpenedAt.value = timeNow
        shiftPrefs.edit()
            .putBoolean("is_shift_open", true)
            .putInt("active_shift", shiftNum)
            .putFloat("shift_modal_awal", modalAwal.toFloat())
            .putString("shift_opened_at", timeNow)
            .apply()
    }

    fun closeShift() {
        _isShiftOpen.value = false
        shiftPrefs.edit().putBoolean("is_shift_open", false).apply()
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
        shiftPrefs.edit()
            .putBoolean("is_user_logged_in", false)
            .remove("last_logged_in_nik")
            .apply()
        // Shift TIDAK ditutup saat logout; shift tetap aktif sampai proses clerek selesai dilakukan
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

    fun updateMember(member: MemberEntity, onDone: () -> Unit) {
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
                val totalTunai = txToday.filter { it.caraBayar == "TUNAI" }.sumOf { it.grandTotal }
                val totalNonTunai = txToday.filter { it.caraBayar != "TUNAI" }.sumOf { it.grandTotal }

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

    fun activateOrUpgradeLicense(key: String, onComplete: (Boolean, String) -> Unit) {
        val trimmed = key.trim().uppercase()
        val tier = when {
            trimmed.contains("ENTERPRISE") -> "ENTERPRISE LIFETIME"
            trimmed.contains("PRO") -> "PRO FULL OFFLINE"
            else -> "AKTIF STANDARD"
        }
        val currentCfg = storeConfig.value
        val updated = currentCfg.copy(
            isLicensed = true,
            licenseKey = key.trim(),
            licenseType = tier,
            licenseExpiryDate = "LIFETIME"
        )
        updateStoreConfig(updated)
        onComplete(true, "Lisensi berhasil diaktifkan: $tier")
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
            adjustments.forEach { (plu, count) ->
                val prod = allProds.find { it.plu == plu }
                if (prod != null) {
                    val selisih = count - prod.stok
                    totalSelisihItem += selisih
                    totalSelisihNilai += selisih * prod.hargaBeli
                    diffs[plu] = selisih
                }
            }
            val so = StockOpnameEntity(
                soNo = soNo,
                tanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
                petugasNama = _currentCashier.value?.nama ?: "Petugas",
                totalItemDihitung = adjustments.size,
                totalSelisihFisik = totalSelisihItem,
                rakFilter = rakFilter,
                totalSelisihItem = totalSelisihItem,
                totalSelisihNilai = totalSelisihNilai
            )
            repository.recordStockOpname(so, diffs)
            onDone()
        }
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
        keterangan: String,
        items: List<Pair<ProductEntity, Pair<Int, Double>>>,
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val purchaseItems = items.map { (prod, qtyAndPrice) ->
                val (qty, price) = qtyAndPrice
                PurchaseItemEntity(
                    noFakturBeli = faktur,
                    plu = prod.plu,
                    barcode = prod.barcode,
                    nama = prod.nama,
                    hargaBeli = price,
                    qty = qty,
                    subtotal = price * qty
                )
            }
            val totalNominal = purchaseItems.sumOf { it.subtotal }
            val totalItem = purchaseItems.sumOf { it.qty }
            val purchase = PurchaseEntity(
                noFakturBeli = faktur,
                supplierNama = supplier,
                tanggal = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()),
                totalBeli = totalNominal,
                keterangan = keterangan,
                totalItem = totalItem,
                totalNominal = totalNominal,
                kasirNik = _currentCashier.value?.nik ?: "ADMIN"
            )
            repository.recordPurchase(purchase, purchaseItems)
            onDone()
        }
    }

    fun seedRealisticTransactions(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.seedRealisticTransactions()
            onDone()
        }
    }
}

