package com.example.data.local.entity

data class VoucherItem(
    val code: String,
    val nominal: Double,
    val minBelanja: Double = 0.0,
    val tanggalDibuat: Long = System.currentTimeMillis(),
    val expiredDate: Long,
    val expiredDateStr: String,
    val isUsed: Boolean = false,
    val usedAt: Long? = null,
    val usedAtBonNo: String = "",
    val sourceBonNo: String = ""
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiredDate
    val status: String get() = when {
        isUsed -> "SUDAH DIGUNAKAN"
        isExpired -> "KEDALUWARSA"
        else -> "AKTIF"
    }
}

data class VoucherSettings(
    val autoGenerateOnReceipt: Boolean = true,
    val minBelanjaDapatVoucher: Double = 50000.0,
    val nilaiVoucher: Double = 10000.0,
    val minBelanjaGunakanVoucher: Double = 30000.0,
    val masaBerlakuHari: Int = 14,
    val prefixKode: String = "VCR",
    val nilaiPerPoin: Double = 100.0,
    val minBelanjaDapatPoin: Double = 10000.0
)
