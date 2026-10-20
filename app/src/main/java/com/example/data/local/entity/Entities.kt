package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "store_config")
data class StoreConfigEntity(
    @PrimaryKey val id: Int = 1,
    val storeCode: String = "M001",
    val storeName: String = "Dodolan Pro Supermarket",
    val storeAddress: String = "Jl. Merdeka No. 45, Jakarta",
    val alamat: String = "Jl. Merdeka No. 45, Jakarta",
    val storePhone: String = "0812-3456-7890",
    val telp: String = "0812-3456-7890",
    val taxNumber: String = "01.234.567.8-901.000",
    val npwp: String = "01.234.567.8-901.000",
    val headerMsg: String = "Dodolan Pro Supermarket",
    val receiptFooter: String = "Terima Kasih Atas Kunjungan Anda\nBarang Yang Sudah Dibeli Tidak Dapat Ditukar",
    val footerMsg: String = "Terima Kasih Atas Kunjungan Anda\nBarang Yang Sudah Dibeli Tidak Dapat Ditukar",
    val plastikPrice: Double = 200.0,
    val nilaiPerPoin: Double = 100.0,
    val belanjaPerPoin: Double = 10000.0,
    val poinAktif: Boolean = true,
    val minPoinRedeem: Int = 10,
    val maxPoinRedeemPerTransaksi: Int = 500,
    val poinBonusMemberBaru: Int = 20,
    val receiptShowAlamat: Boolean = true,
    val receiptShowTelp: Boolean = true,
    val receiptShowNpwp: Boolean = true,
    val receiptShowBonNo: Boolean = true,
    val receiptShowKasir: Boolean = true,
    val receiptShowPlu: Boolean = true,
    val receiptShowDiskon: Boolean = true,
    val receiptShowHemat: Boolean = true,
    val receiptShowVoucher: Boolean = true,
    val receiptShowDateTime: Boolean = true,
    val receiptPolicyMsg: String = "Barang yang sudah dibeli tidak dapat ditukar",
    val receiptModel: String = "STANDARD_RETAIL", // STANDARD_RETAIL, COMPACT_MINIMAL, CAFE_RESTO, ELEGANT_BORDER
    val receiptCustomGreeting: String = "",
    val receiptShowStoreName: Boolean = true,
    val receiptShowCustomerName: Boolean = true,
    val receiptShowMemberInfo: Boolean = true,
    val receiptShowEarnedPoints: Boolean = true,
    val receiptShowTotalItemQty: Boolean = true,
    val receiptShowPpn: Boolean = true,
    val receiptShowPaymentDetail: Boolean = true,
    val receiptWifiInfo: String = "",
    val receiptShowBarcodeSimulation: Boolean = true,
    val receiptItemNameWrap: Boolean = false,
    val activeVoucherCode: String = "",
    val voucherDiscountAmount: Double = 0.0,
    val voucherAutoGenerateOnReceipt: Boolean = true,
    val voucherDefaultNominal: Double = 5000.0,
    val voucherMinBelanjaSyarat: Double = 50000.0,
    val voucherMinBelanjaPakai: Double = 30000.0,
    val voucherMasaBerlakuHari: Int = 7,
    val voucherPrefix: String = "VCR",
    val isLicensed: Boolean = false,
    val licenseKey: String = "",
    val licenseType: String = "TRIAL",
    val licenseExpiryDate: String = "TRIAL",
    val donasiDefault: Double = 0.0,
    val isTaxActive: Boolean = false,
    val taxName: String = "PPN",
    val isTaxIncluded: Boolean = true,
    val ppnPercentage: Double = 0.0,
    val qrisData: String = "00020101021226600016ID.CO.QRIS.WWW01189360050300000870940215000001234567890520454995802ID5910DODOLANPRO6007JAKARTA61051234062070703A016304",
    val qrisMerchantName: String = "DodolanPro Supermarket",
    val qrisNmid: String = "ID1020030040050",
    val printQrisOnReceipt: Boolean = false,
    val printerPaperSize: String = "58mm", // 58mm, 80mm
    val printerType: String = "BLUETOOTH", // BLUETOOTH, USB, WIFI_LAN, SYSTEM_PRINT
    val bluetoothPrinterName: String = "",
    val bluetoothPrinterMac: String = "",
    val printerIpAddress: String = "192.168.1.100",
    val printerPort: Int = 9100,
    val printerFeedLines: Int = 3,
    val printerAutoCut: Boolean = true,
    val printerOpenCashDrawer: Boolean = false,
    val printerPrintCopies: Int = 1,
    val printerFontSize: String = "NORMAL", // NORMAL, CONDENSED
    val autoPrintReceipt: Boolean = true
) {
    val licenseStatus: String get() = if (isLicensed) licenseType else "TRIAL"
    val licenseTier: String get() = licenseType
    val isTrial: Boolean get() = !isLicensed || licenseType.equals("TRIAL", ignoreCase = true)
    val isPro: Boolean get() = isLicensed && licenseType.contains("PRO", ignoreCase = true)
    val isEnterprise: Boolean get() = isLicensed && licenseType.contains("ENTERPRISE", ignoreCase = true)
}

@Entity(tableName = "cashier_users")
data class CashierUserEntity(
    @PrimaryKey val nik: String,
    val nama: String,
    val pin: String,
    val role: String = "KASIR", // OWNER, ADMIN, SUPERVISOR, KASIR
    val statusAktif: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),

    // --- HAK AKSES KASIR (POS) ---
    val canTransaksiPos: Boolean = true,
    val canVoidItem: Boolean = false,
    val canCancelTransaksi: Boolean = true,
    val canReturKonsumen: Boolean = false,
    val canPendingRecall: Boolean = true,
    val canManualDiscount: Boolean = false,
    val canBukaLaciUang: Boolean = true,

    // --- HAK AKSES SHIFT & CLEREK ---
    val canBukaTutupShift: Boolean = true,
    val canClerekKasir: Boolean = true,
    val canViewOmsetShift: Boolean = false,

    // --- HAK AKSES SIS & MANAJEMEN ---
    val canAccessSis: Boolean = false,
    val canManageProduk: Boolean = false,
    val canManagePembelian: Boolean = false,
    val canCetakLabelRak: Boolean = false,
    val canStockOpname: Boolean = false,
    val canReturPembelian: Boolean = false,
    val canEodTutupToko: Boolean = false,
    val canViewLaporan: Boolean = false,
    val canManageVoucher: Boolean = false,
    val canManageKaryawan: Boolean = false,
    val canManageMember: Boolean = false,
    val canManageSettings: Boolean = false,
    val canBackupRestore: Boolean = false
) {
    fun isOwner(): Boolean = role.equals("OWNER", ignoreCase = true) || nik == "11111111"
    fun isAdmin(): Boolean = isOwner() || role.equals("ADMIN", ignoreCase = true)
    fun isSupervisor(): Boolean = isAdmin() || role.equals("SUPERVISOR", ignoreCase = true)

    fun activePermissionsCount(): Int {
        var count = 0
        if (canTransaksiPos) count++
        if (canVoidItem) count++
        if (canCancelTransaksi) count++
        if (canReturKonsumen) count++
        if (canPendingRecall) count++
        if (canManualDiscount) count++
        if (canBukaLaciUang) count++
        if (canBukaTutupShift) count++
        if (canClerekKasir) count++
        if (canViewOmsetShift) count++
        if (canAccessSis) count++
        if (canManageProduk) count++
        if (canManagePembelian) count++
        if (canCetakLabelRak) count++
        if (canStockOpname) count++
        if (canReturPembelian) count++
        if (canEodTutupToko) count++
        if (canViewLaporan) count++
        if (canManageVoucher) count++
        if (canManageKaryawan) count++
        if (canManageMember) count++
        if (canManageSettings) count++
        if (canBackupRestore) count++
        return count
    }

    companion object {
        fun createWithRole(
            nik: String,
            nama: String,
            pin: String,
            role: String,
            statusAktif: Boolean = true
        ): CashierUserEntity {
            val upperRole = role.trim().uppercase()
            return when (upperRole) {
                "OWNER", "ADMIN" -> CashierUserEntity(
                    nik = nik,
                    nama = nama,
                    pin = pin,
                    role = upperRole,
                    statusAktif = statusAktif,
                    canTransaksiPos = true,
                    canVoidItem = true,
                    canCancelTransaksi = true,
                    canReturKonsumen = true,
                    canPendingRecall = true,
                    canManualDiscount = true,
                    canBukaLaciUang = true,
                    canBukaTutupShift = true,
                    canClerekKasir = true,
                    canViewOmsetShift = true,
                    canAccessSis = true,
                    canManageProduk = true,
                    canManagePembelian = true,
                    canCetakLabelRak = true,
                    canStockOpname = true,
                    canReturPembelian = true,
                    canEodTutupToko = true,
                    canViewLaporan = true,
                    canManageVoucher = true,
                    canManageKaryawan = true,
                    canManageMember = true,
                    canManageSettings = true,
                    canBackupRestore = true
                )
                "SUPERVISOR" -> CashierUserEntity(
                    nik = nik,
                    nama = nama,
                    pin = pin,
                    role = upperRole,
                    statusAktif = statusAktif,
                    canTransaksiPos = true,
                    canVoidItem = true,
                    canCancelTransaksi = true,
                    canReturKonsumen = true,
                    canPendingRecall = true,
                    canManualDiscount = true,
                    canBukaLaciUang = true,
                    canBukaTutupShift = true,
                    canClerekKasir = true,
                    canViewOmsetShift = true,
                    canAccessSis = true,
                    canManageProduk = true,
                    canManagePembelian = true,
                    canCetakLabelRak = true,
                    canStockOpname = true,
                    canReturPembelian = true,
                    canEodTutupToko = false,
                    canViewLaporan = true,
                    canManageVoucher = true,
                    canManageKaryawan = false,
                    canManageMember = true,
                    canManageSettings = false,
                    canBackupRestore = false
                )
                else -> CashierUserEntity(
                    nik = nik,
                    nama = nama,
                    pin = pin,
                    role = "KASIR",
                    statusAktif = statusAktif,
                    canTransaksiPos = true,
                    canVoidItem = false,
                    canCancelTransaksi = true,
                    canReturKonsumen = false,
                    canPendingRecall = true,
                    canManualDiscount = false,
                    canBukaLaciUang = true,
                    canBukaTutupShift = true,
                    canClerekKasir = true,
                    canViewOmsetShift = false,
                    canAccessSis = false,
                    canManageProduk = false,
                    canManagePembelian = false,
                    canCetakLabelRak = false,
                    canStockOpname = false,
                    canReturPembelian = false,
                    canEodTutupToko = false,
                    canViewLaporan = false,
                    canManageVoucher = false,
                    canManageKaryawan = false,
                    canManageMember = false,
                    canManageSettings = false,
                    canBackupRestore = false
                )
            }
        }
    }
}

@Entity(tableName = "shift_configs")
data class ShiftConfigEntity(
    @PrimaryKey val shiftNumber: Int,
    val shiftName: String,
    val jamMulai: String,
    val jamSelesai: String,
    val modalDefault: Double = 100000.0,
    val modalAwalStandar: Double = modalDefault,
    val toleransiSelisih: Double = 10000.0,
    val isAktif: Boolean = true,
    val isActive: Boolean = isAktif
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val plu: String,
    val barcode: String,
    val nama: String,
    val kategori: String,
    val hargaBeli: Double,
    val hargaJual: Double,
    val hargaMember: Double = hargaJual,
    val stok: Int,
    val satuan: String = "PCS",
    val rakPlanogram: String = "001-01",
    val minStok: Int = 5,
    val isPromo: Boolean = false,
    val diskonPersen: Double = 0.0,
    val isSewaPromosi: Boolean = false,
    val promoLabel: String = "",
    val statusAktif: Boolean = true,
    val hargaGrosirQty: Int = 2,
    val hargaGrosir: Double = 0.0,
    val varian: String = "",
    val isRepack: Boolean = false,
    val satuanRepack: String = "",
    val isiRepack: Int = 1,
    val hargaRepack: Double = 0.0,
    val barcodeRepack: String = "",
    val satuanRepack2: String = "",
    val isiRepack2: Int = 1,
    val hargaRepack2: Double = 0.0,
    val barcodeRepack2: String = "",
    val repackDeskripsi: String = "",

    // Promo detail
    val promoType: String = "DISKON_PERSEN", // "DISKON_PERSEN", "DISKON_NOMINAL", "BELI_X_GRATIS_Y", "BELI_X_POTONGAN", "GROSIR"
    val diskonNominal: Double = 0.0,
    val promoMinBeli: Int = 2,
    val promoGratisQty: Int = 1,
    val promoPotonganNominal: Double = 0.0,
    val promoBerlakuDari: String = "",
    val promoBerlakuSampai: String = "",
    val promoKeterangan: String = ""
) {
    fun hasDiscountOrPromo(): Boolean {
        if (!isPromo) return false
        return diskonPersen > 0 || diskonNominal > 0 || promoPotonganNominal > 0 ||
                promoType == "BELI_X_GRATIS_Y" || (hargaGrosir > 0)
    }

    fun getEffectivePromoLabel(): String {
        if (!isPromo) return ""
        if (promoKeterangan.isNotBlank()) return promoKeterangan
        return when (promoType) {
            "BELI_X_GRATIS_Y" -> "Beli $promoMinBeli Gratis $promoGratisQty"
            "BELI_X_POTONGAN" -> "Beli $promoMinBeli Potongan Rp ${promoPotonganNominal.toLong()}"
            "DISKON_NOMINAL" -> "Diskon Rp ${diskonNominal.toLong()}"
            "DISKON_PERSEN" -> if (diskonPersen > 0) "Diskon ${diskonPersen.toInt()}%" else "Promo"
            "GROSIR" -> "Beli >= $hargaGrosirQty Jadi Rp ${hargaGrosir.toLong()}"
            else -> if (promoLabel.isNotBlank()) promoLabel else "PROMO"
        }
    }

    fun getEffectiveDiscountedPrice(): Double {
        if (!isPromo) return hargaJual
        return when (promoType) {
            "DISKON_PERSEN" -> if (diskonPersen > 0) hargaJual * (1.0 - (diskonPersen / 100.0)) else hargaJual
            "DISKON_NOMINAL" -> if (diskonNominal > 0) maxOf(0.0, hargaJual - diskonNominal) else hargaJual
            "BELI_X_POTONGAN" -> if (promoMinBeli > 0 && promoPotonganNominal > 0) maxOf(0.0, hargaJual - (promoPotonganNominal / promoMinBeli)) else hargaJual
            "BELI_X_GRATIS_Y" -> if (promoMinBeli + promoGratisQty > 0) (hargaJual * promoMinBeli) / (promoMinBeli + promoGratisQty) else hargaJual
            "GROSIR" -> if (hargaGrosir > 0) hargaGrosir else hargaJual
            else -> if (diskonPersen > 0) hargaJual * (1.0 - (diskonPersen / 100.0)) else hargaJual
        }
    }
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val bonNo: String,
    val kasirNik: String,
    val kasirNama: String,
    val memberNo: String = "",
    val subtotal: Double,
    val diskonTotal: Double,
    val donasi: Double = 0.0,
    val grandTotal: Double,
    val caraBayar: String, // TUNAI, QRIS, TRANSFER, DEBIT, KARTU
    val bayarTunai: Double,
    val bayarNonTunai: Double = 0.0,
    val kembalian: Double,
    val poinDigunakan: Int = 0,
    val nilaiPotonganPoin: Double = 0.0,
    val kodeVoucher: String = "",
    val nilaiPotonganVoucher: Double = 0.0,
    val refPembayaran: String = "",
    val voucherDiberikan: String = "",
    val voucherDiberikanNominal: Double = 0.0,
    val voucherDiberikanExpiry: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isClerek: Boolean = false
) {
    val status: String get() = "SELESAI"
    val tanggal: String get() = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
    val waktu: String get() = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bonNo: String,
    val plu: String,
    val barcode: String,
    val nama: String,
    val hargaJual: Double,
    val qty: Int,
    val diskonItem: Double = 0.0,
    val subtotalItem: Double,
    val satuan: String = "PCS",
    val isPromo: Boolean = false,
    val promoNote: String = ""
) {
    val namaBarang: String get() = nama
    val hargaSatuan: Double get() = hargaJual
}

@Entity(tableName = "clerek_records")
data class ClerekEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kasirNik: String,
    val kasirNama: String = "",
    val shiftNumber: Int,
    val modalAwal: Double,
    val totalPenjualanTunai: Double,
    val totalPenjualanNonTunai: Double,
    val totalPenjualan: Double,
    val totalDonasi: Double,
    val uangSetoranFisik: Double,
    val selisih: Double,
    val statusSelisih: String, // PAS, LEBIH, KURANG
    val jumlahTransaksi: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val shift: String get() = "Shift $shiftNumber"
    val modalKasir: Double get() = modalAwal
    val totalJualUmum: Double get() = totalPenjualan
    val totalTunai: Double get() = totalPenjualanTunai
    val totalCard: Double get() = totalPenjualanNonTunai
    val totalWallet: Double get() = 0.0
    val totalVoucher: Double get() = 0.0
    val status: String get() = statusSelisih
    val tanggal: String get() = java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

@Entity(tableName = "stock_opname_records")
data class StockOpnameEntity(
    @PrimaryKey val soNo: String,
    val tanggal: String,
    val petugasNama: String,
    val totalItemDihitung: Int,
    val totalSelisihFisik: Int,
    val rakFilter: String = "SEMUA RAK",
    val totalSelisihItem: Int = totalSelisihFisik,
    val totalSelisihNilai: Double = 0.0,
    val keterangan: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kode: String = "",
    val nama: String,
    val noHp: String = "",
    val alamat: String = "",
    val kontakPerson: String = "",
    val keterangan: String = ""
)

@Entity(tableName = "store_branches")
data class StoreBranchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kodeToko: String = "",
    val namaToko: String,
    val alamat: String = "",
    val telepon: String = "",
    val keterangan: String = ""
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val noFakturBeli: String,
    val supplierNama: String,
    val tanggal: String,
    val totalBeli: Double,
    val statusBayar: String = "LUNAS",
    val keterangan: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val totalItem: Int = 1,
    val totalNominal: Double = totalBeli,
    val kasirNik: String = "ADMIN",
    val petugasNama: String = "Admin / Petugas Gudang",
    val tokoTujuan: String = "Toko Utama"
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noFakturBeli: String,
    val plu: String,
    val barcode: String,
    val nama: String,
    val hargaBeli: Double,
    val qty: Int,
    val subtotal: Double,
    val satuan: String = "PCS",
    val konversi: Int = 1
)

@Entity(tableName = "return_docs")
data class ReturnDocEntity(
    @PrimaryKey val nrbNo: String,
    val tipeRetur: String = "SUPPLIER", // KONSUMEN, SUPPLIER, TOKO_GROSIR
    val refNo: String = "", // No Bon atau No Faktur Beli
    val fakturAsal: String = refNo,
    val supplierNama: String = "Supplier Utama",
    val jenisRetur: String = tipeRetur,
    val tanggal: String = "",
    val totalNilai: Double = 0.0,
    val alasan: String = "",
    val keteranganRetur: String = alasan,
    val status: String = "FORM",
    val timestamp: Long = System.currentTimeMillis(),
    val petugasNama: String = "Administrator",
    val tokoTujuan: String = "Supplier",
    val totalQty: Int = 0,
    val itemsJson: String = ""
)

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val memberNo: String,
    val nama: String,
    val noHp: String,
    val alamat: String = "",
    val poin: Int = 0,
    val totalBelanja: Double = 0.0,
    val statusAktif: Boolean = true,
    val tanggalDaftar: Long = System.currentTimeMillis()
)

@Entity(tableName = "employee_schedules")
data class EmployeeScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val karyawanNik: String,
    val karyawanNama: String,
    val tanggal: String,
    val shiftNumber: Int,
    val shiftName: String,
    val statusHadir: String = "JADWAL"
)

@Entity(tableName = "bap_records")
data class BapEntity(
    @PrimaryKey val bapNo: String,
    val tanggal: String,
    val jenisMasalah: String,
    val deskripsi: String,
    val penyelesaian: String,
    val petugasNama: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "goods_receipts")
data class GoodsReceiptEntity(
    @PrimaryKey val lpbNo: String,
    val poNo: String,
    val supplierNama: String,
    val tanggal: String,
    val totalItem: Int,
    val diterimaOleh: String,
    val status: String = "SELESAI",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey val poNo: String,
    val supplierNama: String,
    val tanggal: String,
    val estimasiTotal: Double,
    val status: String = "TERKIRIM",
    val keterangan: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vouchers")
data class VoucherEntity(
    @PrimaryKey val kodeVoucher: String,
    val nominalDiskon: Double,
    val minBelanja: Double = 0.0,
    val tanggalMulai: String = "",
    val tanggalKedaluwarsa: String = "", // Format: "dd/MM/yyyy"
    val isUsed: Boolean = false,
    val usedAtBonNo: String = "",
    val usedAtTimestamp: Long = 0L,
    val bonNoAsal: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val statusText: String
        get() {
            if (isUsed) return "SUDAH DIGUNAKAN"
            return if (isExpired()) "KEDALUWARSA" else "AKTIF"
        }

    fun isExpired(): Boolean {
        if (tanggalKedaluwarsa.isBlank()) return false
        return try {
            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val expDate = sdf.parse(tanggalKedaluwarsa)
            val todayStr = sdf.format(java.util.Date())
            val today = sdf.parse(todayStr)
            expDate != null && today != null && today.after(expDate)
        } catch (_: Exception) {
            false
        }
    }
}

