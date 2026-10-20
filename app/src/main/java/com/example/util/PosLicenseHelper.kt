package com.example.util

import java.security.MessageDigest
import java.util.Locale

object PosLicenseHelper {
    const val SALT = "DODOLAN_PRO_RETAIL_SECRET_SALT_2026"

    /**
     * Membersihkan ID Device menjadi 6 karakter alfanumerik kapital
     */
    fun cleanDeviceId(deviceId: String): String {
        val cleaned = deviceId.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "")
        return if (cleaned.length < 4) {
            (cleaned + "DP00").take(6)
        } else {
            cleaned.take(6)
        }
    }

    /**
     * Menghasilkan format lisensi:
     * - PRO: PRO-[DEV6]-[HASH4]-[HASH4]  (misal: PRO-DP7849-A8F1-9B3C)
     * - ENTERPRISE: ENT-[DEV6]-[HASH4]-[HASH4]  (misal: ENT-DP7849-E2A4-7D1F)
     */
    fun generateLicenseKey(deviceId: String, tier: String): String {
        val cleanDev = cleanDeviceId(deviceId)
        val targetTier = if (tier.startsWith("ENT", ignoreCase = true) || tier.contains("ENTERPRISE", ignoreCase = true)) "ENT" else "PRO"
        val seed = "$targetTier:$cleanDev:$SALT"
        val hash = sha256(seed)
        val p1 = hash.substring(0, 4)
        val p2 = hash.substring(4, 8)
        return "$targetTier-$cleanDev-$p1-$p2"
    }

    /**
     * Memvalidasi kode lisensi terhadap ID Device perangkat
     */
    fun validateLicenseKey(deviceId: String, key: String): Pair<Boolean, String> {
        val trimmed = key.trim().uppercase(Locale.ROOT).replace(" ", "")
        if (trimmed.isEmpty()) {
            return Pair(false, "Kode lisensi tidak boleh kosong!")
        }

        // Master Universal Emergency Keys (untuk bypass/keadaan darurat dukungan teknis)
        if (trimmed == "MASTER-PRO-LIFETIME" || trimmed == "PRO-LIFETIME-2026") {
            return Pair(true, "PRO LIFETIME")
        }
        if (trimmed == "MASTER-ENTERPRISE-LIFETIME" || trimmed == "ENT-LIFETIME-2026" || trimmed == "ENTERPRISE-LIFETIME-2026") {
            return Pair(true, "ENTERPRISE LIFETIME")
        }

        val expectedPro = generateLicenseKey(deviceId, "PRO")
        if (trimmed == expectedPro || trimmed.replace("-", "") == expectedPro.replace("-", "")) {
            return Pair(true, "PRO LIFETIME")
        }

        val expectedEnt = generateLicenseKey(deviceId, "ENT")
        if (trimmed == expectedEnt || trimmed.replace("-", "") == expectedEnt.replace("-", "")) {
            return Pair(true, "ENTERPRISE LIFETIME")
        }

        return Pair(
            false,
            "Kode lisensi tidak cocok untuk ID Device '${deviceId.trim()}'. Pastikan kode lisensi di-generate sesuai ID Device ini."
        )
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }
}
