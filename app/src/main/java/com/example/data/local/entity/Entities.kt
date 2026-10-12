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
    val activeVoucherCode: String = "",
    val voucherDiscountAmount: Double = 0.0,
    val isLicensed: Boolean = true,
    val licenseKey: String = "PRO-OFFLINE-FULL-2026",
    val licenseType: String = "PRO_FULL",
    val licenseExpiryDate: String = "LIFETIME",
    val donasiDefault: Double = 0.0,
    val ppnPercentage: Double = 0.0,
    val printerPaperSize: String = "58mm",
    val printerType: String = "BLUETOOTH",
    val bluetoothPrinterMac: String = "",
    val autoPrintReceipt: Boolean = true
) {
    val licenseStatus: String get() = if (isLicensed) "AKTIF" else "TIDAK AKTIF"
    val licenseTier: String get() = licenseType
}

@Entity(tableName = "cashier_users")
data class CashierUserEntity(
    @PrimaryKey val nik: String,
    val nama: String,
    val pin: String,
    val role: String = "KASIR", // ADMIN, KASIR, SUPERVISOR
    val statusAktif: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

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
    val barcodeRepack: String = ""
)

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
    val kasirNik: String = "ADMIN"
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
    val subtotal: Double
)

@Entity(tableName = "return_docs")
data class ReturnDocEntity(
    @PrimaryKey val nrbNo: String,
    val tipeRetur: String = "SUPPLIER", // KONSUMEN, SUPPLIER
    val refNo: String = "", // No Bon atau No Faktur Beli
    val fakturAsal: String = refNo,
    val supplierNama: String = "Supplier Utama",
    val jenisRetur: String = tipeRetur,
    val tanggal: String = "",
    val totalNilai: Double = 0.0,
    val alasan: String = "",
    val keteranganRetur: String = alasan,
    val status: String = "FORM",
    val timestamp: Long = System.currentTimeMillis()
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
