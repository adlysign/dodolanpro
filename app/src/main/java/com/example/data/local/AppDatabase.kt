package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.PosDao
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        StoreConfigEntity::class,
        CashierUserEntity::class,
        ShiftConfigEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ClerekEntity::class,
        StockOpnameEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        ReturnDocEntity::class,
        MemberEntity::class,
        EmployeeScheduleEntity::class,
        BapEntity::class,
        GoodsReceiptEntity::class,
        PurchaseOrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dodolan_pro_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO store_config (id, storeName, storeAddress, storePhone, taxNumber, receiptFooter, isLicensed, licenseKey, licenseType, licenseExpiryDate, donasiDefault, ppnPercentage, printerPaperSize, printerType, bluetoothPrinterMac, autoPrintReceipt) " +
                    "VALUES (1, 'Dodolan Pro Supermarket', 'Jl. Merdeka No. 45, Jakarta', '0812-3456-7890', '01.234.567.8-901.000', 'Terima Kasih Atas Kunjungan Anda\nBarang Yang Sudah Dibeli Tidak Dapat Ditukar', 1, 'PRO-OFFLINE-FULL-2026', 'PRO_FULL', 'LIFETIME', 0.0, 0.0, '58mm', 'BLUETOOTH', '', 1);"
                )
                scope.launch(Dispatchers.IO) {
                    INSTANCE?.let { database ->
                        populateInitialData(database.posDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL(
                    "INSERT OR IGNORE INTO store_config (id, storeName, storeAddress, storePhone, taxNumber, receiptFooter, isLicensed, licenseKey, licenseType, licenseExpiryDate, donasiDefault, ppnPercentage, printerPaperSize, printerType, bluetoothPrinterMac, autoPrintReceipt) " +
                    "VALUES (1, 'Dodolan Pro Supermarket', 'Jl. Merdeka No. 45, Jakarta', '0812-3456-7890', '01.234.567.8-901.000', 'Terima Kasih Atas Kunjungan Anda\nBarang Yang Sudah Dibeli Tidak Dapat Ditukar', 1, 'PRO-OFFLINE-FULL-2026', 'PRO_FULL', 'LIFETIME', 0.0, 0.0, '58mm', 'BLUETOOTH', '', 1);"
                )
                db.execSQL(
                    "INSERT OR REPLACE INTO cashier_users (nik, nama, pin, role, statusAktif, createdAt) " +
                    "VALUES ('11111111', 'Owner', '1234', 'OWNER', 1, 1700000000000);"
                )
            }
        }

        suspend fun populateInitialData(dao: PosDao) {
            dao.insertOrUpdateConfig(StoreConfigEntity())

            dao.insertCashiers(
                listOf(
                    CashierUserEntity("11111111", "Owner", "1234", "OWNER"),
                    CashierUserEntity("001", "Administrator", "1234", "ADMIN"),
                    CashierUserEntity("002", "Kasir 1 (Siti)", "1111", "KASIR"),
                    CashierUserEntity("003", "Kasir 2 (Budi)", "2222", "KASIR"),
                    CashierUserEntity("004", "Supervisor (Rudi)", "3333", "SUPERVISOR")
                )
            )

            dao.insertShiftConfigs(
                listOf(
                    ShiftConfigEntity(1, "Shift Pagi", "07:00", "15:00", 100000.0, 10000.0),
                    ShiftConfigEntity(2, "Shift Siang", "15:00", "23:00", 100000.0, 10000.0),
                    ShiftConfigEntity(3, "Shift Malam", "23:00", "07:00", 100000.0, 10000.0)
                )
            )

            dao.insertMembers(
                listOf(
                    MemberEntity("MBR-0001", "Budi Santoso", "081234567890", "Jl. Mawar No. 10", 150, 450000.0),
                    MemberEntity("MBR-0002", "Siti Aminah", "082345678901", "Jl. Melati No. 5", 85, 230000.0),
                    MemberEntity("MBR-0003", "Ahmad Fauzi", "083456789012", "Jl. Anggrek No. 12", 210, 680000.0)
                )
            )

            dao.insertProducts(
                listOf(
                    ProductEntity("0001", "8992388111014", "Indomie Goreng Spesial 85g", "Makanan", 2800.0, 3500.0, 3300.0, 120, "PCS", "A01-01", 10, isPromo = true, promoLabel = "Beli 5 Lebih Hemat", hargaGrosirQty = 5, hargaGrosir = 3200.0),
                    ProductEntity("0002", "8992388111021", "Indomie Kuah Ayam Bawang 69g", "Makanan", 2700.0, 3300.0, 3100.0, 85, "PCS", "A01-02", 10),
                    ProductEntity("0003", "8998009010223", "Aqua Air Mineral 600ml", "Minuman", 3000.0, 4000.0, 3800.0, 96, "BTL", "B02-01", 12, isPromo = true, promoLabel = "Beli 3 Rp 10.000", hargaGrosirQty = 3, hargaGrosir = 3333.0),
                    ProductEntity("0004", "8998009010330", "Aqua Air Mineral 1500ml", "Minuman", 5500.0, 7000.0, 6700.0, 48, "BTL", "B02-02", 10),
                    ProductEntity("0005", "8991389221001", "Minyak Goreng Bimoli 2 Liter", "Sembako", 32000.0, 37500.0, 36500.0, 40, "BTL", "C01-01", 5),
                    ProductEntity("0006", "8999999195022", "Beras Ramos Wangi 5 Kg", "Sembako", 62000.0, 71000.0, 69500.0, 25, "KRG", "C02-01", 5),
                    ProductEntity("0007", "8996001301018", "Teh Pucuk Harum 350ml", "Minuman", 2800.0, 3800.0, 3500.0, 75, "BTL", "B01-01", 12, isPromo = true, promoLabel = "Promo Segar", diskonPersen = 10.0),
                    ProductEntity("0008", "8999999001012", "Gula Pasir Gulaku 1 Kg", "Sembako", 15000.0, 18000.0, 17500.0, 50, "KRG", "C01-02", 10),
                    ProductEntity("0009", "8991002101211", "Rokok Surya 12 Batang", "Rokok", 24000.0, 27000.0, 27000.0, 60, "PACK", "D01-01", 10),
                    ProductEntity("0010", "8991002101212", "Rokok Surya 12 (1 Slop)", "Rokok", 240000.0, 265000.0, 265000.0, 10, "SLOP", "D01-02", 2, isRepack = true, satuanRepack = "SLOP", isiRepack = 10, hargaRepack = 265000.0),
                    ProductEntity("0011", "8991001100552", "Susu Ultra Milk Coklat 200ml", "Minuman", 4500.0, 5800.0, 5500.0, 80, "PCS", "B03-01", 10),
                    ProductEntity("0012", "8992753112233", "Sabun Lifebuoy Total 10 110g", "Toiletries", 4000.0, 5200.0, 5000.0, 45, "PCS", "E01-01", 8),
                    ProductEntity("0013", "8999999551122", "Kopi Kapal Api Spesial Mix 10s", "Minuman", 11000.0, 14000.0, 13500.0, 65, "PACK", "A02-01", 10),
                    ProductEntity("0014", "8999999551123", "Kopi Kapal Api Spesial Renceng", "Minuman", 110000.0, 135000.0, 132000.0, 15, "RENCENG", "A02-02", 3, isRepack = true, satuanRepack = "RENCENG", isiRepack = 10, hargaRepack = 135000.0),
                    ProductEntity("0015", "8992753221199", "Shampoo Pantene 160ml", "Toiletries", 21000.0, 26500.0, 25500.0, 30, "BTL", "E02-01", 5),
                    ProductEntity("0016", "8992753000011", "Kantong Plastik Sedang", "Perlengkapan", 150.0, 300.0, 300.0, 500, "PCS", "KASIR", 50)
                )
            )
        }
    }
}
