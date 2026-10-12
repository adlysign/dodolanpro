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
        items: List<TransactionItemEntity>
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
                timestamp = System.currentTimeMillis(),
                isClerek = false
            )
            dao.insertTransaction(tx)
            dao.insertTransactionItems(items)

            // Deduct product stocks
            for (item in items) {
                dao.deductStock(item.plu, item.qty)
            }

            // Member points reward & deduction
            if (memberNo.isNotBlank()) {
                if (poinDigunakan > 0) {
                    dao.deductMemberPoints(memberNo, poinDigunakan)
                }
                // Earn 1 point per 10.000 spent
                val earnedPoints = (grandTotal / 10000).toInt()
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
        items: List<PurchaseItemEntity>
    ) = withContext(Dispatchers.IO) {
        dao.insertPurchase(purchase)
        dao.insertPurchaseItems(items)
        for (item in items) {
            dao.addStock(item.plu, item.qty)
        }
    }

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

    suspend fun updateStoreConfig(config: StoreConfigEntity) = withContext(Dispatchers.IO) {
        dao.insertOrUpdateConfig(config)
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
}
