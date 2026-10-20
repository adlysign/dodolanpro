package com.example.data.repository

import com.example.data.local.dao.PosDao
import com.example.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class PosRepository(private val dao: PosDao) {

    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val allProductsAll: Flow<List<ProductEntity>> = dao.getAllProductsIncludingInactive()
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()
    val allTransactionItems: Flow<List<TransactionItemEntity>> = dao.getAllTransactionItemsFlow()
    val allClerekRecords: Flow<List<ClerekEntity>> = dao.getAllClerekRecords()
    val allPurchases: Flow<List<PurchaseEntity>> = dao.getAllPurchases()
    val allStockOpnames: Flow<List<StockOpnameEntity>> = dao.getAllStockOpnames()
    val allReturns: Flow<List<ReturnDocEntity>> = dao.getAllReturns()
    val allBapRecords: Flow<List<BapEntity>> = dao.getAllBapRecords()
    val allSchedules: Flow<List<EmployeeScheduleEntity>> = dao.getAllSchedules()
    val storeConfig: Flow<StoreConfigEntity> = dao.getStoreConfig().map { it ?: StoreConfigEntity() }
    val allMembers: Flow<List<MemberEntity>> = dao.getAllMembers()
    val allCashiers: Flow<List<CashierUserEntity>> = dao.getAllCashiers()
    val allShiftConfigs: Flow<List<ShiftConfigEntity>> = dao.getAllShiftConfigs()

    suspend fun getProductByPluOrBarcode(code: String): ProductEntity? = withContext(Dispatchers.IO) {
        dao.getProductByPluOrBarcode(code.trim())
    }

    suspend fun searchProducts(query: String): List<ProductEntity> = withContext(Dispatchers.IO) {
        dao.searchProducts(query.trim())
    }

    suspend fun insertProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.deleteProduct(product)
    }

    suspend fun getCashierByNik(nik: String): CashierUserEntity? = withContext(Dispatchers.IO) {
        dao.getCashierByNik(nik)
    }

    suspend fun saveCashier(cashier: CashierUserEntity) = withContext(Dispatchers.IO) {
        dao.insertCashier(cashier)
    }

    suspend fun updateCashier(cashier: CashierUserEntity) = withContext(Dispatchers.IO) {
        dao.updateCashier(cashier)
    }

    suspend fun deleteCashier(cashier: CashierUserEntity) = withContext(Dispatchers.IO) {
        dao.deleteCashier(cashier)
    }

    suspend fun getMemberByKey(key: String): MemberEntity? = withContext(Dispatchers.IO) {
        dao.getMemberByKey(key.trim())
    }

    suspend fun saveMember(member: MemberEntity) = withContext(Dispatchers.IO) {
        dao.insertMember(member)
    }

    suspend fun updateMember(member: MemberEntity) = withContext(Dispatchers.IO) {
        dao.updateMember(member)
    }

    suspend fun deleteMember(member: MemberEntity) = withContext(Dispatchers.IO) {
        dao.deleteMember(member)
    }

    suspend fun addMemberPoints(memberNo: String, points: Int) = withContext(Dispatchers.IO) {
        dao.addMemberPoints(memberNo, points)
    }

    suspend fun deductMemberPoints(memberNo: String, points: Int) = withContext(Dispatchers.IO) {
        dao.deductMemberPoints(memberNo, points)
    }

    suspend fun setMemberPoints(memberNo: String, points: Int) = withContext(Dispatchers.IO) {
        dao.setMemberPoints(memberNo, points)
    }

    suspend fun saveShiftConfig(shift: ShiftConfigEntity) = withContext(Dispatchers.IO) {
        dao.insertShiftConfig(shift)
    }

    suspend fun updateShiftConfig(shift: ShiftConfigEntity) = withContext(Dispatchers.IO) {
        dao.updateShiftConfig(shift)
    }

    suspend fun ensureDefaultShifts() = withContext(Dispatchers.IO) {
        val existing = dao.getAllShiftConfigsList()
        if (existing.isEmpty()) {
            dao.insertShiftConfigs(
                listOf(
                    ShiftConfigEntity(1, "Shift Pagi", "07:00", "15:00", 100000.0, 10000.0),
                    ShiftConfigEntity(2, "Shift Siang", "15:00", "23:00", 100000.0, 10000.0),
                    ShiftConfigEntity(3, "Shift Malam", "23:00", "07:00", 100000.0, 10000.0)
                )
            )
        }
    }

    suspend fun getItemsForBon(bonNo: String): List<TransactionItemEntity> = withContext(Dispatchers.IO) {
        dao.getItemsForBon(bonNo)
    }

    suspend fun findTransactionForRetur(bonKey: String): Pair<TransactionEntity, List<TransactionItemEntity>>? = withContext(Dispatchers.IO) {
        val tx = dao.getTransactionByBon(bonKey.trim()) ?: dao.getTransactionByBonSuffix(bonKey.trim())
        if (tx != null) {
            val items = dao.getItemsForBon(tx.bonNo)
            Pair(tx, items)
        } else {
            null
        }
    }

    suspend fun checkoutTransaction(
        bonNo: String,
        kasirNik: String,
        kasirNama: String,
        memberNo: String,
        subtotal: Double,
        diskonTotal: Double,
        donasi: Double,
        grandTotal: Double,
        caraBayar: String,
        bayarTunai: Double,
        bayarNonTunai: Double,
        kembalian: Double,
        poinDigunakan: Int,
        nilaiPotonganPoin: Double,
        kodeVoucher: String,
        nilaiPotonganVoucher: Double,
        refPembayaran: String,
        items: List<TransactionItemEntity>,
        voucherDiberikan: String = "",
        voucherDiberikanNominal: Double = 0.0,
        voucherDiberikanExpiry: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val tx = TransactionEntity(
                bonNo = bonNo,
                kasirNik = kasirNik,
                kasirNama = kasirNama,
                memberNo = memberNo,
                subtotal = subtotal,
                diskonTotal = diskonTotal,
                donasi = donasi,
                grandTotal = grandTotal,
                caraBayar = caraBayar,
                bayarTunai = bayarTunai,
                bayarNonTunai = bayarNonTunai,
                kembalian = kembalian,
                poinDigunakan = poinDigunakan,
                nilaiPotonganPoin = nilaiPotonganPoin,
                kodeVoucher = kodeVoucher,
                nilaiPotonganVoucher = nilaiPotonganVoucher,
                refPembayaran = refPembayaran,
                voucherDiberikan = voucherDiberikan,
                voucherDiberikanNominal = voucherDiberikanNominal,
                voucherDiberikanExpiry = voucherDiberikanExpiry,
                timestamp = System.currentTimeMillis(),
                isClerek = false
            )
            dao.insertTransaction(tx)
            dao.insertTransactionItems(items)

            // Tandai voucher yang digunakan sebagai 'SUDAH DIGUNAKAN'
            if (kodeVoucher.isNotBlank()) {
                dao.markVoucherAsUsed(kodeVoucher, bonNo, System.currentTimeMillis())
            }

            // Deduct product stocks
            for (item in items) {
                dao.deductStock(item.plu, item.qty)
            }

            // Member points reward & deduction
            val storeConfig = dao.getStoreConfigOnce() ?: StoreConfigEntity()
            if (memberNo.isNotBlank() && storeConfig.poinAktif) {
                if (poinDigunakan > 0) {
                    dao.deductMemberPoints(memberNo, poinDigunakan)
                }
                // Earn points based on store config belanjaPerPoin rule
                val belanjaRule = if (storeConfig.belanjaPerPoin > 0.0) storeConfig.belanjaPerPoin else 10000.0
                val earnedPoints = (grandTotal / belanjaRule).toInt()
                if (earnedPoints > 0) {
                    dao.addMemberPoints(memberNo, earnedPoints)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun calculateClerek(
        kasirNik: String,
        modalKasir: Double,
        uangSetoranFisik: Double,
        shiftNum: Int
    ): ClerekEntity = withContext(Dispatchers.IO) {
        val txs = dao.getUnclerekTransactionsForKasir(kasirNik)
        var totalTunai = 0.0
        var totalNonTunai = 0.0
        var totalDonasi = 0.0

        for (tx in txs) {
            if (tx.caraBayar == "TUNAI") {
                totalTunai += (tx.grandTotal - tx.donasi)
            } else {
                totalNonTunai += tx.grandTotal
            }
            totalDonasi += tx.donasi
        }

        val totalPenjualan = totalTunai + totalNonTunai
        val uangSeharusnya = modalKasir + totalTunai + totalDonasi
        val selisih = uangSetoranFisik - uangSeharusnya
        val status = when {
            selisih > 0 -> "LEBIH"
            selisih < 0 -> "KURANG"
            else -> "PAS"
        }

        val record = ClerekEntity(
            kasirNik = kasirNik,
            shiftNumber = shiftNum,
            modalAwal = modalKasir,
            totalPenjualanTunai = totalTunai,
            totalPenjualanNonTunai = totalNonTunai,
            totalPenjualan = totalPenjualan,
            totalDonasi = totalDonasi,
            uangSetoranFisik = uangSetoranFisik,
            selisih = selisih,
            statusSelisih = status,
            jumlahTransaksi = txs.size,
            timestamp = System.currentTimeMillis()
        )
        dao.insertClerek(record)
        dao.markTransactionsClerek(kasirNik)
        record
    }

    suspend fun recordPurchase(
        purchase: PurchaseEntity,
        items: List<PurchaseItemEntity>,
        updateCostMethod: String = "AVERAGE"
    ) = withContext(Dispatchers.IO) {
        dao.insertPurchase(purchase)
        dao.insertPurchaseItems(items)
        for (item in items) {
            val existing = dao.getProductByPluOrBarcode(item.plu)
            if (existing != null) {
                val currentStock = maxOf(0, existing.stok)
                val currentCost = existing.hargaBeli
                val konversi = maxOf(1, item.konversi)
                val buyQty = item.qty * konversi
                val buyCostPerBase = if (konversi > 1) item.hargaBeli / konversi else item.hargaBeli

                // Moving Weighted Average Cost (Harga Pokok Rata-Rata Bergerak)
                // Jika metode AVERAGE dan total stok > 0:
                // HPP baru = ((Stok Lama * HPP Lama) + (Qty Beli * Harga Beli Baru per Satuan Dasar)) / (Stok Lama + Qty Beli)
                val newHpp = if (updateCostMethod == "AVERAGE" && (currentStock + buyQty) > 0) {
                    ((currentStock * currentCost) + (buyQty * buyCostPerBase)) / (currentStock + buyQty)
                } else {
                    buyCostPerBase
                }

                val updatedProduct = existing.copy(
                    stok = existing.stok + buyQty,
                    hargaBeli = newHpp
                )
                dao.updateProduct(updatedProduct)
            } else {
                val konversi = maxOf(1, item.konversi)
                dao.addStock(item.plu, item.qty * konversi)
            }
        }
    }

    // Suppliers
    fun getAllSuppliers(): Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    suspend fun getAllSuppliersList(): List<SupplierEntity> = withContext(Dispatchers.IO) { dao.getAllSuppliersList() }
    suspend fun insertSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) { dao.insertSupplier(supplier) }
    suspend fun updateSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) { dao.updateSupplier(supplier) }
    suspend fun deleteSupplier(supplier: SupplierEntity) = withContext(Dispatchers.IO) { dao.deleteSupplier(supplier) }

    // Store Branches
    fun getAllStoreBranches(): Flow<List<StoreBranchEntity>> = dao.getAllStoreBranches()
    suspend fun getAllStoreBranchesList(): List<StoreBranchEntity> = withContext(Dispatchers.IO) { dao.getAllStoreBranchesList() }
    suspend fun insertStoreBranch(branch: StoreBranchEntity) = withContext(Dispatchers.IO) { dao.insertStoreBranch(branch) }
    suspend fun updateStoreBranch(branch: StoreBranchEntity) = withContext(Dispatchers.IO) { dao.updateStoreBranch(branch) }
    suspend fun deleteStoreBranch(branch: StoreBranchEntity) = withContext(Dispatchers.IO) { dao.deleteStoreBranch(branch) }

    suspend fun getPurchaseItems(faktur: String): List<PurchaseItemEntity> = withContext(Dispatchers.IO) {
        dao.getItemsForPurchase(faktur)
    }

    suspend fun recordStockOpname(
        so: StockOpnameEntity,
        itemDifferences: Map<String, Int>
    ) = withContext(Dispatchers.IO) {
        dao.insertStockOpname(so)
        for ((plu, diff) in itemDifferences) {
            dao.addStock(plu, diff)
        }
    }

    suspend fun insertReturnDoc(retur: ReturnDocEntity) = withContext(Dispatchers.IO) {
        dao.insertReturnDoc(retur)
    }

    suspend fun recordReturnPurchase(
        doc: ReturnDocEntity,
        itemsToDeduct: List<Pair<String, Int>>,
        isFixed: Boolean
    ) = withContext(Dispatchers.IO) {
        val finalDoc = doc.copy(status = if (isFixed) "FIXED" else "FORM")
        dao.insertReturnDoc(finalDoc)
        if (isFixed) {
            for ((plu, qty) in itemsToDeduct) {
                dao.addStock(plu, -qty)
            }
        }
    }

    suspend fun finalizeReturnDoc(
        nrbNo: String,
        itemsToDeduct: List<Pair<String, Int>>
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getReturnDoc(nrbNo)
        if (existing != null) {
            dao.updateReturnDoc(existing.copy(status = "FIXED"))
            for ((plu, qty) in itemsToDeduct) {
                dao.addStock(plu, -qty)
            }
        }
    }

    suspend fun deleteReturnDoc(nrbNo: String) = withContext(Dispatchers.IO) {
        dao.deleteReturnDoc(nrbNo)
    }

    suspend fun updateStoreConfig(config: StoreConfigEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateConfig(config)
    }

    // Vouchers
    fun getAllVouchers(): Flow<List<VoucherEntity>> = dao.getAllVouchers()
    suspend fun getAllVouchersList(): List<VoucherEntity> = withContext(Dispatchers.IO) { dao.getAllVouchersList() }
    suspend fun getVoucherByCode(code: String): VoucherEntity? = withContext(Dispatchers.IO) { dao.getVoucherByCode(code) }
    suspend fun insertVoucher(voucher: VoucherEntity) = withContext(Dispatchers.IO) { dao.insertVoucher(voucher) }
    suspend fun insertVouchers(vouchers: List<VoucherEntity>) = withContext(Dispatchers.IO) { dao.insertVouchers(vouchers) }
    suspend fun updateVoucher(voucher: VoucherEntity) = withContext(Dispatchers.IO) { dao.updateVoucher(voucher) }
    suspend fun deleteVoucher(voucher: VoucherEntity) = withContext(Dispatchers.IO) { dao.deleteVoucher(voucher) }
    suspend fun markVoucherAsUsed(code: String, bonNo: String, timestamp: Long = System.currentTimeMillis()) = withContext(Dispatchers.IO) {
        dao.markVoucherAsUsed(code, bonNo, timestamp)
    }
    suspend fun markVoucherStatus(code: String, isUsed: Boolean) = withContext(Dispatchers.IO) {
        dao.markVoucherStatus(code, isUsed)
    }

    suspend fun getStoreConfigOnce(): StoreConfigEntity = withContext(Dispatchers.IO) {
        dao.getStoreConfigOnce() ?: StoreConfigEntity()
    }

    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportTime", System.currentTimeMillis())

        val products = JSONArray()
        dao.getAllProductsList().forEach { p ->
            val obj = JSONObject()
            obj.put("plu", p.plu)
            obj.put("barcode", p.barcode)
            obj.put("nama", p.nama)
            obj.put("kategori", p.kategori)
            obj.put("hargaBeli", p.hargaBeli)
            obj.put("hargaJual", p.hargaJual)
            obj.put("hargaMember", p.hargaMember)
            obj.put("stok", p.stok)
            obj.put("satuan", p.satuan)
            obj.put("rakPlanogram", p.rakPlanogram)
            obj.put("minStok", p.minStok)
            obj.put("isPromo", p.isPromo)
            obj.put("diskonPersen", p.diskonPersen)
            obj.put("isSewaPromosi", p.isSewaPromosi)
            obj.put("promoLabel", p.promoLabel)
            obj.put("statusAktif", p.statusAktif)
            obj.put("hargaGrosirQty", p.hargaGrosirQty)
            obj.put("hargaGrosir", p.hargaGrosir)
            obj.put("varian", p.varian)
            obj.put("isRepack", p.isRepack)
            products.put(obj)
        }
        root.put("products", products)

        val txs = JSONArray()
        dao.getAllTransactionsList().forEach { t ->
            val obj = JSONObject()
            obj.put("bonNo", t.bonNo)
            obj.put("kasirNik", t.kasirNik)
            obj.put("kasirNama", t.kasirNama)
            obj.put("memberNo", t.memberNo)
            obj.put("subtotal", t.subtotal)
            obj.put("diskonTotal", t.diskonTotal)
            obj.put("donasi", t.donasi)
            obj.put("grandTotal", t.grandTotal)
            obj.put("caraBayar", t.caraBayar)
            obj.put("bayarTunai", t.bayarTunai)
            obj.put("bayarNonTunai", t.bayarNonTunai)
            obj.put("kembalian", t.kembalian)
            obj.put("timestamp", t.timestamp)
            txs.put(obj)
        }
        root.put("transactions", txs)

        val members = JSONArray()
        dao.getAllMembersList().forEach { m ->
            val obj = JSONObject()
            obj.put("memberNo", m.memberNo)
            obj.put("nama", m.nama)
            obj.put("noHp", m.noHp)
            obj.put("alamat", m.alamat)
            obj.put("poin", m.poin)
            obj.put("totalBelanja", m.totalBelanja)
            members.put(obj)
        }
        root.put("members", members)

        val cashiers = JSONArray()
        dao.getAllCashiersList().forEach { c ->
            val obj = JSONObject()
            obj.put("nik", c.nik)
            obj.put("nama", c.nama)
            obj.put("pin", c.pin)
            obj.put("role", c.role)
            cashiers.put(obj)
        }
        root.put("cashiers", cashiers)

        val vouchers = JSONArray()
        dao.getAllVouchersList().forEach { v ->
            val obj = JSONObject()
            obj.put("kodeVoucher", v.kodeVoucher)
            obj.put("nominalDiskon", v.nominalDiskon)
            obj.put("minBelanja", v.minBelanja)
            obj.put("tanggalMulai", v.tanggalMulai)
            obj.put("tanggalKedaluwarsa", v.tanggalKedaluwarsa)
            obj.put("isUsed", v.isUsed)
            obj.put("usedAtBonNo", v.usedAtBonNo)
            obj.put("bonNoAsal", v.bonNoAsal)
            vouchers.put(obj)
        }
        root.put("vouchers", vouchers)

        root.toString(2)
    }

    suspend fun restoreDatabaseFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (root.has("products")) {
                val array = root.getJSONArray("products")
                val list = mutableListOf<ProductEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ProductEntity(
                            plu = obj.getString("plu"),
                            barcode = obj.getString("barcode"),
                            nama = obj.getString("nama"),
                            kategori = obj.getString("kategori"),
                            hargaBeli = obj.getDouble("hargaBeli"),
                            hargaJual = obj.getDouble("hargaJual"),
                            hargaMember = obj.optDouble("hargaMember", obj.getDouble("hargaJual")),
                            stok = obj.getInt("stok"),
                            satuan = obj.optString("satuan", "PCS"),
                            rakPlanogram = obj.optString("rakPlanogram", "001-01"),
                            minStok = obj.optInt("minStok", 5),
                            isPromo = obj.optBoolean("isPromo", false),
                            diskonPersen = obj.optDouble("diskonPersen", 0.0),
                            isSewaPromosi = obj.optBoolean("isSewaPromosi", false),
                            promoLabel = obj.optString("promoLabel", ""),
                            statusAktif = obj.optBoolean("statusAktif", true),
                            hargaGrosirQty = obj.optInt("hargaGrosirQty", 2),
                            hargaGrosir = obj.optDouble("hargaGrosir", 0.0),
                            varian = obj.optString("varian", ""),
                            isRepack = obj.optBoolean("isRepack", false)
                        )
                    )
                }
                dao.clearProducts()
                dao.insertProducts(list)
            }

            if (root.has("members")) {
                val array = root.getJSONArray("members")
                val list = mutableListOf<MemberEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        MemberEntity(
                            memberNo = obj.getString("memberNo"),
                            nama = obj.getString("nama"),
                            noHp = obj.getString("noHp"),
                            alamat = obj.optString("alamat", ""),
                            poin = obj.optInt("poin", 0),
                            totalBelanja = obj.optDouble("totalBelanja", 0.0)
                        )
                    )
                }
                dao.clearMembers()
                dao.insertMembers(list)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun resetToDefaultDemoData() = withContext(Dispatchers.IO) {
        dao.clearProducts()
        dao.clearTransactions()
        dao.clearTransactionItems()
        dao.clearClerek()
        dao.clearPurchases()
        dao.clearPurchaseItems()
        dao.clearStockOpnames()
        dao.clearReturns()
        dao.clearMembers()
        dao.clearCashiers()
        dao.clearShiftConfigs()
        com.example.data.local.AppDatabase.populateInitialData(dao)
    }

    suspend fun seedRealisticTransactions() = withContext(Dispatchers.IO) {
        val products = dao.getAllProductsList()
        if (products.isEmpty()) return@withContext

        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60 * 60 * 1000
        val paymentMethods = listOf("TUNAI", "QRIS", "TUNAI", "DEBIT", "QRIS", "TRANSFER")
        val cashiers = listOf(
            Pair("002", "Kasir 1 (Siti)"),
            Pair("003", "Kasir 2 (Budi)"),
            Pair("001", "Administrator")
        )
        val memberNumbers = listOf("MBR-0001", "MBR-0002", "MBR-0003", "", "", "")

        val generatedTxs = mutableListOf<TransactionEntity>()
        val generatedItems = mutableListOf<TransactionItemEntity>()

        var bonCounter = 100
        val daysBack = 13
        val hoursDistribution = listOf(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21)

        val cal = java.util.Calendar.getInstance()
        for (dayOffset in daysBack downTo 0) {
            val txCountForDay = if (dayOffset == 0) 6 else (3 + (dayOffset % 5))
            for (i in 0 until txCountForDay) {
                bonCounter++
                val bonNo = "BON-2026${String.format("%04d", bonCounter)}"
                cal.timeInMillis = now - (dayOffset * dayMillis)
                val hour = hoursDistribution[(i + dayOffset) % hoursDistribution.size]
                val minute = (i * 17) % 60
                cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                cal.set(java.util.Calendar.MINUTE, minute)
                cal.set(java.util.Calendar.SECOND, 20)
                val txTimestamp = cal.timeInMillis

                val cashier = cashiers[(i + dayOffset) % cashiers.size]
                val member = memberNumbers[(i * 3 + dayOffset) % memberNumbers.size]
                val payment = paymentMethods[(i + dayOffset * 2) % paymentMethods.size]

                val numItems = 1 + ((i + dayOffset) % 4)
                var subtotal = 0.0
                var diskonTotal = 0.0
                val currentTxItems = mutableListOf<TransactionItemEntity>()

                for (itemIdx in 0 until numItems) {
                    val prod = products[(itemIdx * 3 + i + dayOffset) % products.size]
                    val qty = 1 + ((itemIdx + i) % 3)
                    val diskon = if (prod.isPromo) (prod.hargaJual * 0.1 * qty) else 0.0
                    val sub = (prod.hargaJual * qty) - diskon
                    subtotal += (prod.hargaJual * qty)
                    diskonTotal += diskon

                    currentTxItems.add(
                        TransactionItemEntity(
                            bonNo = bonNo,
                            plu = prod.plu,
                            barcode = prod.barcode,
                            nama = prod.nama,
                            hargaJual = prod.hargaJual,
                            qty = qty,
                            diskonItem = diskon,
                            subtotalItem = sub,
                            satuan = prod.satuan,
                            isPromo = prod.isPromo,
                            promoNote = if (prod.isPromo) prod.promoLabel else ""
                        )
                    )
                }

                val donasi = if (payment == "TUNAI" && (i % 2 == 0)) 500.0 else 0.0
                val grandTotal = (subtotal - diskonTotal) + donasi
                val bayarTunai = if (payment == "TUNAI") ((grandTotal / 10000).toInt() + 1) * 10000.0 else 0.0
                val bayarNonTunai = if (payment != "TUNAI") grandTotal else 0.0
                val kembalian = if (payment == "TUNAI") maxOf(0.0, bayarTunai - grandTotal) else 0.0

                val tx = TransactionEntity(
                    bonNo = bonNo,
                    kasirNik = cashier.first,
                    kasirNama = cashier.second,
                    memberNo = member,
                    subtotal = subtotal,
                    diskonTotal = diskonTotal,
                    donasi = donasi,
                    grandTotal = grandTotal,
                    caraBayar = payment,
                    bayarTunai = bayarTunai,
                    bayarNonTunai = bayarNonTunai,
                    kembalian = kembalian,
                    poinDigunakan = if (member.isNotBlank() && i % 3 == 0) 10 else 0,
                    nilaiPotonganPoin = if (member.isNotBlank() && i % 3 == 0) 1000.0 else 0.0,
                    kodeVoucher = if (i % 5 == 0) "VCR-HEMAT10K" else "",
                    nilaiPotonganVoucher = if (i % 5 == 0) 10000.0 else 0.0,
                    refPembayaran = if (payment != "TUNAI") "REF-${System.currentTimeMillis() % 1000000}" else "",
                    timestamp = txTimestamp,
                    isClerek = true
                )
                generatedTxs.add(tx)
                generatedItems.addAll(currentTxItems)
            }
        }

        dao.insertTransactions(generatedTxs)
        dao.insertTransactionItems(generatedItems)
    }
}
