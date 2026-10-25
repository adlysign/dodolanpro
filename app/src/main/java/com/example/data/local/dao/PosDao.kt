package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    @Query("SELECT * FROM products WHERE statusAktif = 1 ORDER BY nama ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY nama ASC")
    fun getAllProductsIncludingInactive(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE statusAktif = 1 ORDER BY nama ASC")
    suspend fun getAllProductsList(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE plu = :code OR barcode = :code OR barcodeRepack = :code OR barcodeRepack2 = :code LIMIT 1")
    suspend fun getProductByPluOrBarcode(code: String): ProductEntity?

    @Query("SELECT * FROM products WHERE nama LIKE '%' || :query || '%' OR plu LIKE '%' || :query || '%' OR barcode LIKE '%' || :query || '%' OR barcodeRepack LIKE '%' || :query || '%' OR barcodeRepack2 LIKE '%' || :query || '%'")
    suspend fun searchProducts(query: String): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("UPDATE products SET stok = stok - :qty WHERE plu = :plu")
    suspend fun deductStock(plu: String, qty: Int)

    @Query("UPDATE products SET stok = stok + :qty WHERE plu = :plu")
    suspend fun addStock(plu: String, qty: Int)

    @Query("UPDATE products SET stok = :newStock WHERE plu = :plu")
    suspend fun setStock(plu: String, newStock: Int)

    // Cashiers
    @Query("SELECT * FROM cashier_users ORDER BY nama ASC")
    fun getAllCashiers(): Flow<List<CashierUserEntity>>

    @Query("SELECT * FROM cashier_users")
    suspend fun getAllCashiersList(): List<CashierUserEntity>

    @Query("SELECT * FROM cashier_users WHERE nik = :nik LIMIT 1")
    suspend fun getCashierByNik(nik: String): CashierUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashier(cashier: CashierUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashiers(cashiers: List<CashierUserEntity>)

    @Update
    suspend fun updateCashier(cashier: CashierUserEntity)

    @Delete
    suspend fun deleteCashier(cashier: CashierUserEntity)

    // Members
    @Query("SELECT * FROM members ORDER BY nama ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members")
    suspend fun getAllMembersList(): List<MemberEntity>

    @Query("SELECT * FROM members WHERE memberNo = :key OR noHp = :key LIMIT 1")
    suspend fun getMemberByKey(key: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Delete
    suspend fun deleteMember(member: MemberEntity)

    @Query("UPDATE members SET poin = poin + :points WHERE memberNo = :memberNo")
    suspend fun addMemberPoints(memberNo: String, points: Int)

    @Query("UPDATE members SET poin = CASE WHEN poin >= :points THEN poin - :points ELSE 0 END WHERE memberNo = :memberNo")
    suspend fun deductMemberPoints(memberNo: String, points: Int)

    @Query("UPDATE members SET poin = :points WHERE memberNo = :memberNo")
    suspend fun setMemberPoints(memberNo: String, points: Int)

    // Shift Configs
    @Query("SELECT * FROM shift_configs ORDER BY shiftNumber ASC")
    fun getAllShiftConfigs(): Flow<List<ShiftConfigEntity>>

    @Query("SELECT * FROM shift_configs ORDER BY shiftNumber ASC")
    suspend fun getAllShiftConfigsList(): List<ShiftConfigEntity>

    @Query("SELECT * FROM shift_configs WHERE shiftNumber = :shiftNum LIMIT 1")
    suspend fun getShiftConfigByNumber(shiftNum: Int): ShiftConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftConfig(shift: ShiftConfigEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShiftConfigs(shifts: List<ShiftConfigEntity>)

    @Update
    suspend fun updateShiftConfig(shift: ShiftConfigEntity)

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE bonNo = :bonNo LIMIT 1")
    suspend fun getTransactionByBon(bonNo: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE bonNo LIKE '%' || :suffix LIMIT 1")
    suspend fun getTransactionByBonSuffix(suffix: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE kasirNik = :nik AND isClerek = 0")
    suspend fun getUnclerekTransactionsForKasir(nik: String): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isClerek = 1 WHERE kasirNik = :nik AND isClerek = 0")
    suspend fun markTransactionsClerek(nik: String)

    // Transaction Items
    @Query("SELECT * FROM transaction_items WHERE bonNo = :bonNo")
    suspend fun getItemsForBon(bonNo: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items")
    fun getAllTransactionItemsFlow(): Flow<List<TransactionItemEntity>>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllTransactionItemsList(): List<TransactionItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    // Clerek
    @Query("SELECT * FROM clerek_records ORDER BY timestamp DESC")
    fun getAllClerekRecords(): Flow<List<ClerekEntity>>

    @Query("SELECT * FROM clerek_records ORDER BY timestamp DESC")
    suspend fun getAllClerekList(): List<ClerekEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClerek(clerek: ClerekEntity)

    // Purchases
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    suspend fun getAllPurchasesList(): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE noFakturBeli = :faktur LIMIT 1")
    suspend fun getPurchaseByFaktur(faktur: String): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchase_items WHERE noFakturBeli = :faktur")
    suspend fun getItemsForPurchase(faktur: String): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    // Purchase Debt Payments (Cicilan / Pelunasan Hutang Pembelian)
    @Query("SELECT * FROM purchase_payments ORDER BY timestamp DESC")
    fun getAllPurchasePayments(): Flow<List<PurchasePaymentEntity>>

    @Query("SELECT * FROM purchase_payments WHERE noFakturBeli = :faktur ORDER BY timestamp ASC")
    fun getPaymentsForPurchase(faktur: String): Flow<List<PurchasePaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasePayment(payment: PurchasePaymentEntity)

    // Stock Opname
    @Query("SELECT * FROM stock_opname_records ORDER BY timestamp DESC")
    fun getAllStockOpnames(): Flow<List<StockOpnameEntity>>

    @Query("SELECT * FROM stock_opname_records ORDER BY timestamp DESC")
    suspend fun getAllStockOpnamesList(): List<StockOpnameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockOpname(so: StockOpnameEntity)

    // Returns
    @Query("SELECT * FROM return_docs ORDER BY nrbNo DESC")
    fun getAllReturns(): Flow<List<ReturnDocEntity>>

    @Query("SELECT * FROM return_docs")
    suspend fun getAllReturnsList(): List<ReturnDocEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnDoc(doc: ReturnDocEntity)

    @Update
    suspend fun updateReturnDoc(doc: ReturnDocEntity)

    @Query("SELECT * FROM return_docs WHERE nrbNo = :nrbNo LIMIT 1")
    suspend fun getReturnDoc(nrbNo: String): ReturnDocEntity?

    @Query("DELETE FROM return_docs WHERE nrbNo = :nrbNo")
    suspend fun deleteReturnDoc(nrbNo: String)

    // Orders & Receipts
    @Query("SELECT * FROM purchase_orders ORDER BY poNo DESC")
    fun getAllPurchaseOrders(): Flow<List<PurchaseOrderEntity>>

    @Query("SELECT * FROM purchase_orders")
    suspend fun getAllPurchaseOrdersList(): List<PurchaseOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrder(po: PurchaseOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseOrders(pos: List<PurchaseOrderEntity>)

    @Query("SELECT * FROM goods_receipts ORDER BY lpbNo DESC")
    fun getAllGoodsReceipts(): Flow<List<GoodsReceiptEntity>>

    @Query("SELECT * FROM goods_receipts")
    suspend fun getAllGoodsReceiptsList(): List<GoodsReceiptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoodsReceipt(gr: GoodsReceiptEntity)

    // BAP
    @Query("SELECT * FROM bap_records ORDER BY bapNo DESC")
    fun getAllBapRecords(): Flow<List<BapEntity>>

    @Query("SELECT * FROM bap_records")
    suspend fun getAllBapList(): List<BapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBap(bap: BapEntity)

    // Schedules
    @Query("SELECT * FROM employee_schedules ORDER BY karyawanNama ASC")
    fun getAllSchedules(): Flow<List<EmployeeScheduleEntity>>

    @Query("SELECT * FROM employee_schedules")
    suspend fun getAllSchedulesList(): List<EmployeeScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: EmployeeScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<EmployeeScheduleEntity>)

    // Store Config
    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    fun getStoreConfig(): Flow<StoreConfigEntity?>

    @Query("SELECT * FROM store_config WHERE id = 1 LIMIT 1")
    suspend fun getStoreConfigOnce(): StoreConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateConfig(config: StoreConfigEntity)

    // Clear queries for data reset / restore
    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM cashier_users")
    suspend fun clearCashiers()

    @Query("DELETE FROM members")
    suspend fun clearMembers()

    @Query("DELETE FROM shift_configs")
    suspend fun clearShiftConfigs()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM transaction_items")
    suspend fun clearTransactionItems()

    @Query("DELETE FROM clerek_records")
    suspend fun clearClerek()

    @Query("DELETE FROM purchases")
    suspend fun clearPurchases()

    @Query("DELETE FROM purchase_items")
    suspend fun clearPurchaseItems()

    @Query("DELETE FROM purchase_orders")
    suspend fun clearPurchaseOrders()

    @Query("DELETE FROM goods_receipts")
    suspend fun clearGoodsReceipts()

    @Query("DELETE FROM stock_opname_records")
    suspend fun clearStockOpnames()

    @Query("DELETE FROM return_docs")
    suspend fun clearReturns()

    @Query("DELETE FROM bap_records")
    suspend fun clearBap()

    @Query("DELETE FROM employee_schedules")
    suspend fun clearSchedules()

    // Suppliers
    @Query("SELECT * FROM suppliers ORDER BY nama ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY nama ASC")
    suspend fun getAllSuppliersList(): List<SupplierEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers")
    suspend fun clearSuppliers()

    // Store Branches
    @Query("SELECT * FROM store_branches ORDER BY namaToko ASC")
    fun getAllStoreBranches(): Flow<List<StoreBranchEntity>>

    @Query("SELECT * FROM store_branches ORDER BY namaToko ASC")
    suspend fun getAllStoreBranchesList(): List<StoreBranchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreBranch(branch: StoreBranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoreBranches(branches: List<StoreBranchEntity>)

    @Update
    suspend fun updateStoreBranch(branch: StoreBranchEntity)

    @Delete
    suspend fun deleteStoreBranch(branch: StoreBranchEntity)

    @Query("DELETE FROM store_branches")
    suspend fun clearStoreBranches()

    // Vouchers
    @Query("SELECT * FROM vouchers ORDER BY timestamp DESC")
    fun getAllVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers ORDER BY timestamp DESC")
    suspend fun getAllVouchersList(): List<VoucherEntity>

    @Query("SELECT * FROM vouchers WHERE UPPER(kodeVoucher) = UPPER(:code) LIMIT 1")
    suspend fun getVoucherByCode(code: String): VoucherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVouchers(vouchers: List<VoucherEntity>)

    @Update
    suspend fun updateVoucher(voucher: VoucherEntity)

    @Delete
    suspend fun deleteVoucher(voucher: VoucherEntity)

    @Query("UPDATE vouchers SET isUsed = 1, usedAtBonNo = :bonNo, usedAtTimestamp = :timestamp WHERE UPPER(kodeVoucher) = UPPER(:code)")
    suspend fun markVoucherAsUsed(code: String, bonNo: String, timestamp: Long)

    @Query("UPDATE vouchers SET isUsed = :isUsed, usedAtBonNo = CASE WHEN :isUsed = 0 THEN '' ELSE usedAtBonNo END WHERE UPPER(kodeVoucher) = UPPER(:code)")
    suspend fun markVoucherStatus(code: String, isUsed: Boolean)

    @Query("DELETE FROM vouchers")
    suspend fun clearVouchers()

    // Receivables (Piutang Pelanggan)
    @Query("SELECT * FROM receivables ORDER BY createdAt DESC")
    fun getAllReceivables(): Flow<List<ReceivableEntity>>

    @Query("SELECT * FROM receivables ORDER BY createdAt DESC")
    suspend fun getAllReceivablesList(): List<ReceivableEntity>

    @Query("SELECT * FROM receivables WHERE id = :id LIMIT 1")
    suspend fun getReceivableById(id: Long): ReceivableEntity?

    @Query("SELECT * FROM receivables WHERE bonNo = :bonNo LIMIT 1")
    suspend fun getReceivableByBonNo(bonNo: String): ReceivableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: ReceivableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivables(receivables: List<ReceivableEntity>)

    @Update
    suspend fun updateReceivable(receivable: ReceivableEntity)

    @Delete
    suspend fun deleteReceivable(receivable: ReceivableEntity)

    @Query("DELETE FROM receivables WHERE id = :id")
    suspend fun deleteReceivableById(id: Long)

    @Query("DELETE FROM receivables")
    suspend fun clearReceivables()

    // Receivable Payments (Cicilan & Pelunasan Piutang)
    @Query("SELECT * FROM receivable_payments WHERE receivableId = :receivableId ORDER BY timestamp DESC")
    fun getPaymentsForReceivable(receivableId: Long): Flow<List<ReceivablePaymentEntity>>

    @Query("SELECT * FROM receivable_payments WHERE receivableId = :receivableId ORDER BY timestamp DESC")
    suspend fun getPaymentsForReceivableList(receivableId: Long): List<ReceivablePaymentEntity>

    @Query("SELECT * FROM receivable_payments ORDER BY timestamp DESC")
    fun getAllReceivablePayments(): Flow<List<ReceivablePaymentEntity>>

    @Query("SELECT * FROM receivable_payments ORDER BY timestamp DESC")
    suspend fun getAllReceivablePaymentsList(): List<ReceivablePaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivablePayment(payment: ReceivablePaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivablePayments(payments: List<ReceivablePaymentEntity>)

    @Delete
    suspend fun deleteReceivablePayment(payment: ReceivablePaymentEntity)

    @Query("DELETE FROM receivable_payments WHERE receivableId = :receivableId")
    suspend fun deletePaymentsByReceivableId(receivableId: Long)

    @Query("DELETE FROM receivable_payments")
    suspend fun clearReceivablePayments()
}

