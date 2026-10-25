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
        VoucherEntity::class,
        SupplierEntity::class,
        StoreBranchEntity::class,
        ReceivableEntity::class,
        ReceivablePaymentEntity::class,
        PurchasePaymentEntity::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE store_config ADD COLUMN belanjaPerPoin REAL NOT NULL DEFAULT 10000.0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinAktif INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN minPoinRedeem INTEGER NOT NULL DEFAULT 10")
                database.execSQL("ALTER TABLE store_config ADD COLUMN maxPoinRedeemPerTransaksi INTEGER NOT NULL DEFAULT 500")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinBonusMemberBaru INTEGER NOT NULL DEFAULT 20")
                database.execSQL("ALTER TABLE store_config ADD COLUMN voucherPrefix TEXT NOT NULL DEFAULT 'VCR'")
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptModel TEXT NOT NULL DEFAULT 'STANDARD_RETAIL'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptCustomGreeting TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowStoreName INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowCustomerName INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowMemberInfo INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowEarnedPoints INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowTotalItemQty INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowPpn INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowPaymentDetail INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptWifiInfo TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptShowBarcodeSimulation INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN receiptItemNameWrap INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN bluetoothPrinterName TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerIpAddress TEXT NOT NULL DEFAULT '192.168.1.100'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerPort INTEGER NOT NULL DEFAULT 9100")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerFeedLines INTEGER NOT NULL DEFAULT 3")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerAutoCut INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerOpenCashDrawer INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerPrintCopies INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printerFontSize TEXT NOT NULL DEFAULT 'NORMAL'")
            }
        }

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE store_config ADD COLUMN isTaxActive INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN taxName TEXT NOT NULL DEFAULT 'PPN'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN isTaxIncluded INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE store_config ADD COLUMN qrisData TEXT NOT NULL DEFAULT '00020101021226600016ID.CO.QRIS.WWW01189360050300000870940215000001234567890520454995802ID5910DODOLANPRO6007JAKARTA61051234062070703A016304'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN qrisMerchantName TEXT NOT NULL DEFAULT 'DodolanPro Supermarket'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN qrisNmid TEXT NOT NULL DEFAULT 'ID1020030040050'")
                database.execSQL("ALTER TABLE store_config ADD COLUMN printQrisOnReceipt INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE purchases ADD COLUMN petugasNama TEXT NOT NULL DEFAULT 'Admin / Petugas Gudang'")
                database.execSQL("ALTER TABLE purchases ADD COLUMN tokoTujuan TEXT NOT NULL DEFAULT 'Toko Utama'")
                database.execSQL("CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, kode TEXT NOT NULL DEFAULT '', nama TEXT NOT NULL, noHp TEXT NOT NULL DEFAULT '', alamat TEXT NOT NULL DEFAULT '', kontakPerson TEXT NOT NULL DEFAULT '', keterangan TEXT NOT NULL DEFAULT '')")
                database.execSQL("CREATE TABLE IF NOT EXISTS store_branches (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, kodeToko TEXT NOT NULL DEFAULT '', namaToko TEXT NOT NULL, alamat TEXT NOT NULL DEFAULT '', telepon TEXT NOT NULL DEFAULT '', keterangan TEXT NOT NULL DEFAULT '')")
            }
        }

        private val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE return_docs ADD COLUMN petugasNama TEXT NOT NULL DEFAULT 'Administrator'")
                database.execSQL("ALTER TABLE return_docs ADD COLUMN tokoTujuan TEXT NOT NULL DEFAULT 'Supplier'")
                database.execSQL("ALTER TABLE return_docs ADD COLUMN totalQty INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE return_docs ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE purchase_items ADD COLUMN satuan TEXT NOT NULL DEFAULT 'PCS'")
                database.execSQL("ALTER TABLE purchase_items ADD COLUMN konversi INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cashier_users ADD COLUMN canManageShift INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN promoBerlakuKelipatan INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN promoHanyaSatuanDasar INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN promoKhususSatuan TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinBisaGunakanSemua INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinMaksPersenBelanja REAL NOT NULL DEFAULT 100.0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN minBelanjaGunakanPoin REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinMaksPotonganNominal REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinDapatSaatRedeem INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE store_config ADD COLUMN poinModeInputKasir TEXT NOT NULL DEFAULT 'BEBAS'")
            }
        }

        private val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS receivables (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bonNo TEXT NOT NULL DEFAULT '',
                        customerName TEXT NOT NULL,
                        customerPhone TEXT NOT NULL DEFAULT '',
                        customerAddress TEXT NOT NULL DEFAULT '',
                        memberNo TEXT NOT NULL DEFAULT '',
                        totalAmount REAL NOT NULL DEFAULT 0.0,
                        paidAmount REAL NOT NULL DEFAULT 0.0,
                        remainingAmount REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'BELUM_LUNAS',
                        tanggal TEXT NOT NULL DEFAULT '',
                        dueDate TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        kasirNik TEXT NOT NULL DEFAULT '',
                        kasirNama TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS receivable_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        receivableId INTEGER NOT NULL,
                        bonNo TEXT NOT NULL DEFAULT '',
                        paymentAmount REAL NOT NULL DEFAULT 0.0,
                        paymentMethod TEXT NOT NULL DEFAULT 'TUNAI',
                        paymentDate TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        kasirNama TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE purchases ADD COLUMN uangMuka REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE purchases ADD COLUMN sisaHutang REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE purchases ADD COLUMN jatuhTempo TEXT NOT NULL DEFAULT ''")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS purchase_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        noFakturBeli TEXT NOT NULL DEFAULT '',
                        nominal REAL NOT NULL DEFAULT 0.0,
                        caraBayar TEXT NOT NULL DEFAULT 'TUNAI',
                        tanggal TEXT NOT NULL DEFAULT '',
                        catatan TEXT NOT NULL DEFAULT '',
                        petugasNama TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "dodolan_pro_db"
                )
                    .addMigrations(
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17
                    )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(appContext, scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return getDatabase(context, CoroutineScope(Dispatchers.IO))
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
                        if (dao.getAllSuppliersList().isEmpty()) {
                            dao.insertSuppliers(
                                listOf(
                                    SupplierEntity(kode = "SUP-001", nama = "DC SAT CILEUNGSI", noHp = "021-82490001", alamat = "Kawasan Industri Menara, Cileungsi", kontakPerson = "Bpk. Hendra"),
                                    SupplierEntity(kode = "SUP-002", nama = "PT INDOMARCO PRISMATAMA", noHp = "021-88991234", alamat = "Ancol Barat, Jakarta Utara", kontakPerson = "Ibu Maya"),
                                    SupplierEntity(kode = "SUP-003", nama = "PT WINGS SURYA", noHp = "021-4601111", alamat = "Pulo Gadung, Jakarta Timur", kontakPerson = "Bpk. Bambang"),
                                    SupplierEntity(kode = "SUP-004", nama = "DISTRIBUTOR SEMBAKO JAYA", noHp = "0812-9988-7766", alamat = "Pasar Induk Kramat Jati", kontakPerson = "H. Supardi")
                                )
                            )
                        }
                        if (dao.getAllStoreBranchesList().isEmpty()) {
                            dao.insertStoreBranches(
                                listOf(
                                    StoreBranchEntity(kodeToko = "TKG-001", namaToko = "Toko Grosir Berkah Jaya", alamat = "Pasar Induk Kramat Jati Blok A", telepon = "0812-1111-2222", keterangan = "Agen Sembako & Minyak"),
                                    StoreBranchEntity(kodeToko = "TKG-002", namaToko = "Agen Grosir Sembako Sumber Rejeki", alamat = "Jl. Raya Pasar Minggu No. 12", telepon = "0813-3333-4444", keterangan = "Grosir Snack & Minuman"),
                                    StoreBranchEntity(kodeToko = "TKG-003", namaToko = "Grosir Plastik & Bahan Kue Makmur", alamat = "Pasar Baru No. 45", telepon = "0815-5555-6666", keterangan = "Grosir Perlengkapan & Plastik"),
                                    StoreBranchEntity(kodeToko = "TKG-004", namaToko = "Agen Grosir Rokok & Kopi Sentosa", alamat = "Jl. Mangga Dua No. 8", telepon = "0817-7777-8888", keterangan = "Agen Grosir Rokok & Kopi")
                                )
                            )
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

            dao.insertSuppliers(
                listOf(
                    SupplierEntity(kode = "SUP-001", nama = "DC SAT CILEUNGSI", noHp = "021-82490001", alamat = "Kawasan Industri Menara, Cileungsi", kontakPerson = "Bpk. Hendra"),
                    SupplierEntity(kode = "SUP-002", nama = "PT INDOMARCO PRISMATAMA", noHp = "021-88991234", alamat = "Ancol Barat, Jakarta Utara", kontakPerson = "Ibu Maya"),
                    SupplierEntity(kode = "SUP-003", nama = "PT WINGS SURYA", noHp = "021-4601111", alamat = "Pulo Gadung, Jakarta Timur", kontakPerson = "Bpk. Bambang"),
                    SupplierEntity(kode = "SUP-004", nama = "DISTRIBUTOR SEMBAKO JAYA", noHp = "0812-9988-7766", alamat = "Pasar Induk Kramat Jati", kontakPerson = "H. Supardi")
                )
            )

            dao.insertStoreBranches(
                listOf(
                    StoreBranchEntity(kodeToko = "TKG-001", namaToko = "Toko Grosir Berkah Jaya", alamat = "Pasar Induk Kramat Jati Blok A", telepon = "0812-1111-2222", keterangan = "Agen Sembako & Minyak"),
                    StoreBranchEntity(kodeToko = "TKG-002", namaToko = "Agen Grosir Sembako Sumber Rejeki", alamat = "Jl. Raya Pasar Minggu No. 12", telepon = "0813-3333-4444", keterangan = "Grosir Snack & Minuman"),
                    StoreBranchEntity(kodeToko = "TKG-003", namaToko = "Grosir Plastik & Bahan Kue Makmur", alamat = "Pasar Baru No. 45", telepon = "0815-5555-6666", keterangan = "Grosir Perlengkapan & Plastik"),
                    StoreBranchEntity(kodeToko = "TKG-004", namaToko = "Agen Grosir Rokok & Kopi Sentosa", alamat = "Jl. Mangga Dua No. 8", telepon = "0817-7777-8888", keterangan = "Agen Grosir Rokok & Kopi")
                )
            )

            // Seed initial demo receivables
            dao.insertReceivables(
                listOf(
                    ReceivableEntity(
                        bonNo = "BON-20261020-001",
                        customerName = "Warung Bu Siti",
                        customerPhone = "0812-3344-5566",
                        customerAddress = "Jl. Melati No. 12 (Sebelah Masjid)",
                        memberNo = "MBR-0001",
                        totalAmount = 350000.0,
                        paidAmount = 100000.0,
                        remainingAmount = 250000.0,
                        status = "SEBAGIAN",
                        tanggal = todayStr,
                        dueDate = expFuture,
                        notes = "Kasbon sembako warung & minyak goreng",
                        kasirNik = "001",
                        kasirNama = "Administrator"
                    ),
                    ReceivableEntity(
                        bonNo = "BON-20261015-002",
                        customerName = "Bpk. Hendra Gunawan",
                        customerPhone = "0813-9876-5432",
                        customerAddress = "Komplek Perumahan Griya Indah Blok C3",
                        memberNo = "MBR-0002",
                        totalAmount = 145000.0,
                        paidAmount = 0.0,
                        remainingAmount = 145000.0,
                        status = "BELUM_LUNAS",
                        tanggal = "15/10/2026",
                        dueDate = expPast,
                        notes = "Belanja kebutuhan bulanan keluarga",
                        kasirNik = "002",
                        kasirNama = "Kasir 1 (Siti)"
                    ),
                    ReceivableEntity(
                        bonNo = "BON-20261010-003",
                        customerName = "Ibu Sri Wahyuni",
                        customerPhone = "0856-1122-3344",
                        customerAddress = "Jl. Mawar RT 02/04 No. 5",
                        memberNo = "MBR-0003",
                        totalAmount = 200000.0,
                        paidAmount = 200000.0,
                        remainingAmount = 0.0,
                        status = "LUNAS",
                        tanggal = "10/10/2026",
                        dueDate = "20/10/2026",
                        notes = "Lunas via transfer BCA",
                        kasirNik = "001",
                        kasirNama = "Administrator"
                    )
                )
            )

            // Seed sample purchase with tempo / piutang hutang
            dao.insertPurchase(
                PurchaseEntity(
                    noFakturBeli = "FB-20261022-001",
                    supplierNama = "DC SAT CILEUNGSI",
                    tanggal = todayStr,
                    totalBeli = 1500000.0,
                    statusBayar = "SEBAGIAN",
                    keterangan = "Kulakan minyak goreng, gula pasir, mie instan (Tempo 30 Hari)",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    totalItem = 25,
                    totalNominal = 1500000.0,
                    kasirNik = "001",
                    petugasNama = "Administrator",
                    tokoTujuan = "Supplier",
                    uangMuka = 500000.0,
                    sisaHutang = 1000000.0,
                    jatuhTempo = expFuture
                )
            )
            dao.insertPurchasePayment(
                PurchasePaymentEntity(
                    noFakturBeli = "FB-20261022-001",
                    nominal = 500000.0,
                    caraBayar = "DP (UANG MUKA)",
                    tanggal = todayStr,
                    catatan = "Uang muka dibayar saat pengiriman barang",
                    petugasNama = "Administrator",
                    timestamp = System.currentTimeMillis() - 86400000L
                )
            )
        }
    }
}

