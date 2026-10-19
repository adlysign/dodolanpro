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
        PurchaseOrderEntity::class,
        VoucherEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "dodolan_pro_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(appContext, scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context,
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = getDatabase(context, scope)
                        populateInitialData(database.posDao())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                scope.launch(Dispatchers.IO) {
                    try {
                        val database = getDatabase(context, scope)
                        val dao = database.posDao()
                        val cfg = dao.getStoreConfigOnce()
                        if (cfg == null) {
                            dao.insertOrUpdateConfig(StoreConfigEntity())
                        }
                        val owner = dao.getCashierByNik("11111111")
                        if (owner == null) {
                            dao.insertCashier(CashierUserEntity.createWithRole("11111111", "Owner", "1234", "OWNER"))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: PosDao) {
            dao.insertOrUpdateConfig(StoreConfigEntity())

            dao.insertCashiers(
                listOf(
                    CashierUserEntity.createWithRole("11111111", "Owner", "1234", "OWNER"),
                    CashierUserEntity.createWithRole("001", "Administrator", "1234", "ADMIN"),
                    CashierUserEntity.createWithRole("002", "Kasir 1 (Siti)", "1111", "KASIR"),
                    CashierUserEntity.createWithRole("003", "Kasir 2 (Budi)", "2222", "KASIR"),
                    CashierUserEntity.createWithRole("004", "Supervisor (Rudi)", "3333", "SUPERVISOR")
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
                    ProductEntity("0001", "8992388111014", "Indomie Goreng Spesial 85g", "Makanan", 2800.0, 3500.0, 3300.0, 120, "PCS", "A01-01", 10, isPromo = true, promoType = "BELI_X_GRATIS_Y", promoMinBeli = 5, promoGratisQty = 1, promoLabel = "Beli 5 Gratis 1", promoKeterangan = "Beli 5 Gratis 1 Spesial", isRepack = true, satuanRepack = "DUS", isiRepack = 40, hargaRepack = 136000.0, repackDeskripsi = "1 Dus = 40 Pcs"),
                    ProductEntity("0002", "8992388111021", "Indomie Kuah Ayam Bawang 69g", "Makanan", 2700.0, 3300.0, 3100.0, 85, "PCS", "A01-02", 10, isPromo = true, promoType = "BELI_X_POTONGAN", promoMinBeli = 2, promoPotonganNominal = 1000.0, promoLabel = "Beli 2 Potongan Rp 1.000", promoKeterangan = "Beli 2 Potongan Rp 1.000", isRepack = true, satuanRepack = "DUS", isiRepack = 40, hargaRepack = 128000.0),
                    ProductEntity("0003", "8998009010223", "Aqua Air Mineral 600ml", "Minuman", 3000.0, 4000.0, 3800.0, 96, "BTL", "B02-01", 12, isPromo = true, promoType = "BELI_X_POTONGAN", promoMinBeli = 3, promoPotonganNominal = 2000.0, promoLabel = "Beli 3 Potongan Rp 2.000", promoKeterangan = "Beli 3 Potongan Rp 2.000 (Rp 10.000/3 Btl)", isRepack = true, satuanRepack = "DUS", isiRepack = 24, hargaRepack = 88000.0, repackDeskripsi = "1 Dus = 24 Btl"),
                    ProductEntity("0004", "8998009010330", "Aqua Air Mineral 1500ml", "Minuman", 5500.0, 7000.0, 6700.0, 48, "BTL", "B02-02", 10, isRepack = true, satuanRepack = "DUS", isiRepack = 12, hargaRepack = 78000.0),
                    ProductEntity("0005", "8991389221001", "Minyak Goreng Bimoli 2 Liter", "Sembako", 32000.0, 37500.0, 36500.0, 40, "BTL", "C01-01", 5, isPromo = true, promoType = "DISKON_NOMINAL", diskonNominal = 2500.0, promoLabel = "Hemat Rp 2.500", promoKeterangan = "Potongan Langsung Rp 2.500/Btl", isRepack = true, satuanRepack = "DUS", isiRepack = 6, hargaRepack = 215000.0),
                    ProductEntity("0006", "8999999195022", "Beras Ramos Wangi 5 Kg", "Sembako", 62000.0, 71000.0, 69500.0, 25, "KRG", "C02-01", 5),
                    ProductEntity("0007", "8996001301018", "Teh Pucuk Harum 350ml", "Minuman", 2800.0, 3800.0, 3500.0, 75, "BTL", "B01-01", 12, isPromo = true, promoType = "BELI_X_GRATIS_Y", promoMinBeli = 2, promoGratisQty = 1, promoLabel = "Beli 2 Gratis 1", promoKeterangan = "Promo Segar Beli 2 Gratis 1", isRepack = true, satuanRepack = "DUS", isiRepack = 24, hargaRepack = 84000.0),
                    ProductEntity("0008", "8999999001012", "Gula Pasir Gulaku 1 Kg", "Sembako", 15000.0, 18000.0, 17500.0, 50, "KRG", "C01-02", 10),
                    ProductEntity("0009", "8991002101211", "Rokok Surya 12 Batang", "Rokok", 24000.0, 27000.0, 27000.0, 60, "PACK", "D01-01", 10, isRepack = true, satuanRepack = "SLOP", isiRepack = 10, hargaRepack = 265000.0, satuanRepack2 = "BAL", isiRepack2 = 200, hargaRepack2 = 5250000.0, repackDeskripsi = "1 Slop = 10 Pack, 1 Bal = 20 Slop"),
                    ProductEntity("0010", "8991002101212", "Rokok Surya 12 (1 Slop)", "Rokok", 240000.0, 265000.0, 265000.0, 10, "SLOP", "D01-02", 2, isRepack = true, satuanRepack = "BAL", isiRepack = 20, hargaRepack = 5250000.0),
                    ProductEntity("0011", "8991001100552", "Susu Ultra Milk Coklat 200ml", "Minuman", 4500.0, 5800.0, 5500.0, 80, "PCS", "B03-01", 10, isPromo = true, promoType = "DISKON_PERSEN", diskonPersen = 15.0, promoLabel = "Diskon 15%", promoKeterangan = "Diskon 15% Susu Sehat", isRepack = true, satuanRepack = "DUS", isiRepack = 24, hargaRepack = 132000.0),
                    ProductEntity("0012", "8992753112233", "Sabun Lifebuoy Total 10 110g", "Toiletries", 4000.0, 5200.0, 5000.0, 45, "PCS", "E01-01", 8, isPromo = true, promoType = "BELI_X_POTONGAN", promoMinBeli = 2, promoPotonganNominal = 2000.0, promoLabel = "Beli 2 Potongan Rp 2.000", promoKeterangan = "Beli 2 Potongan Rp 2.000"),
                    ProductEntity("0013", "8999999551122", "Kopi Kapal Api Spesial Mix 10s", "Minuman", 11000.0, 14000.0, 13500.0, 65, "PACK", "A02-01", 10, isRepack = true, satuanRepack = "RENCENG", isiRepack = 10, hargaRepack = 135000.0, satuanRepack2 = "DUS", isiRepack2 = 120, hargaRepack2 = 1580000.0),
                    ProductEntity("0014", "8999999551123", "Kopi Kapal Api Spesial Renceng", "Minuman", 110000.0, 135000.0, 132000.0, 15, "RENCENG", "A02-02", 3, isRepack = true, satuanRepack = "DUS", isiRepack = 12, hargaRepack = 1580000.0),
                    ProductEntity("0015", "8992753221199", "Shampoo Pantene 160ml", "Toiletries", 21000.0, 26500.0, 25500.0, 30, "BTL", "E02-01", 5, isPromo = true, promoType = "DISKON_NOMINAL", diskonNominal = 3000.0, promoLabel = "Diskon Rp 3.000", promoKeterangan = "Diskon Spesial Perawatan"),
                    ProductEntity("0016", "8992753000011", "Kantong Plastik Sedang", "Perlengkapan", 150.0, 300.0, 300.0, 500, "PCS", "KASIR", 50)
                )
            )

            val cal = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val todayStr = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, 7)
            val expFuture = sdf.format(cal.time)
            cal.add(java.util.Calendar.DAY_OF_YEAR, -15)
            val expPast = sdf.format(cal.time)

            dao.insertVouchers(
                listOf(
                    VoucherEntity("VCR-PROMO5K", 5000.0, 30000.0, todayStr, expFuture, isUsed = false, bonNoAsal = "INIT-01"),
                    VoucherEntity("VCR-HEMAT10K", 10000.0, 50000.0, todayStr, expFuture, isUsed = false, bonNoAsal = "INIT-02"),
                    VoucherEntity("VCR-EXPIRED", 5000.0, 20000.0, expPast, expPast, isUsed = false, bonNoAsal = "INIT-03"),
                    VoucherEntity("VCR-TERPAKAI", 5000.0, 20000.0, todayStr, expFuture, isUsed = true, usedAtBonNo = "BON-DEMO-001", bonNoAsal = "INIT-04")
                )
            )
        }
    }
}
