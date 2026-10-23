package com.example.util

import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StoreConfigEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.viewmodel.CartItem
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale

object ReceiptFormatter {

    const val MODEL_STANDARD_RETAIL = "STANDARD_RETAIL"
    const val MODEL_COMPACT_MINIMAL = "COMPACT_MINIMAL"
    const val MODEL_CAFE_RESTO = "CAFE_RESTO"
    const val MODEL_ELEGANT_BORDER = "ELEGANT_BORDER"

    val AVAILABLE_MODELS = listOf(
        ReceiptModelInfo(
            id = MODEL_STANDARD_RETAIL,
            title = "Standar Ritel / Supermarket",
            subtitle = "Format lengkap Indomaret/Alfamart: rincian diskon, PPN, poin reward, & voucher",
            iconName = "Storefront"
        ),
        ReceiptModelInfo(
            id = MODEL_COMPACT_MINIMAL,
            title = "Ringkas / Hemat Kertas",
            subtitle = "Spasi padat 1 baris per produk. Sangat hemat kertas struk thermal 58mm",
            iconName = "ReceiptLong"
        ),
        ReceiptModelInfo(
            id = MODEL_CAFE_RESTO,
            title = "Cafe, Resto & F&B",
            subtitle = "Format pesanan resto: No. Meja/Antrean, PB1, jam saji, & info Wi-Fi toko",
            iconName = "LocalCafe"
        ),
        ReceiptModelInfo(
            id = MODEL_ELEGANT_BORDER,
            title = "Elegan / Berbingkai",
            subtitle = "Desain premium berbingkai ganda estetis untuk butik, bakery & toko modern",
            iconName = "AutoAwesome"
        )
    )

    data class ReceiptModelInfo(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconName: String
    )

    fun getLineWidth(paperSize: String): Int {
        return if (paperSize.equals("80mm", ignoreCase = true)) 48 else 32
    }

    private fun formatRupiah(amount: Double): String {
        return try {
            NumberFormat.getNumberInstance(Locale("in", "ID")).format(amount.toLong())
        } catch (_: Exception) {
            amount.toLong().toString()
        }
    }

    private fun center(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val leftPad = (width - text.length) / 2
        return " ".repeat(leftPad) + text
    }

    private fun justify(left: String, right: String, width: Int): String {
        val available = width - right.length
        return if (left.length > available) {
            val truncatedLeft = left.take(maxOf(0, available - 1))
            val spaces = maxOf(1, width - truncatedLeft.length - right.length)
            truncatedLeft + " ".repeat(spaces) + right
        } else {
            val spaces = maxOf(1, width - left.length - right.length)
            left + " ".repeat(spaces) + right
        }
    }

    private fun line(char: Char, width: Int): String {
        return char.toString().repeat(width)
    }

    /**
     * Format a real POS transaction into receipt text according to store configuration.
     */
    fun formatReceipt(
        transaction: TransactionEntity,
        items: List<CartItem>,
        config: StoreConfigEntity
    ): String {
        val width = getLineWidth(config.printerPaperSize)
        val model = config.receiptModel

        return when (model) {
            MODEL_COMPACT_MINIMAL -> formatCompact(transaction, items, config, width)
            MODEL_CAFE_RESTO -> formatCafe(transaction, items, config, width)
            MODEL_ELEGANT_BORDER -> formatElegant(transaction, items, config, width)
            else -> formatStandard(transaction, items, config, width)
        }
    }

    /**
     * Generate a full representative sample receipt to test & live preview changes.
     */
    fun formatSampleReceipt(config: StoreConfigEntity): String {
        val sampleItems = listOf(
            CartItem(
                product = ProductEntity(
                    plu = "1001",
                    barcode = "8992753123456",
                    nama = "Beras Premium 5kg",
                    satuan = "ZAK",
                    kategori = "Sembako",
                    hargaBeli = 65000.0,
                    hargaJual = 74500.0,
                    stok = 45,
                    diskonPersen = 0.0
                ),
                qty = 1
            ),
            CartItem(
                product = ProductEntity(
                    plu = "1002",
                    barcode = "8998866123001",
                    nama = "Minyak Goreng 2L",
                    satuan = "PCH",
                    kategori = "Sembako",
                    hargaBeli = 31000.0,
                    hargaJual = 35000.0,
                    stok = 120,
                    isPromo = true,
                    diskonPersen = 10.0
                ),
                qty = 2
            ),
            CartItem(
                product = ProductEntity(
                    plu = "2005",
                    barcode = "8991102234005",
                    nama = "Teh Celup Kotak 25s",
                    satuan = "BOX",
                    kategori = "Minuman",
                    hargaBeli = 5500.0,
                    hargaJual = 7500.0,
                    stok = 80,
                    diskonPersen = 0.0
                ),
                qty = 1
            )
        )

        val subtotal = 74500.0 + (35000.0 * 2) + 7500.0 // 152000
        val diskonItem = (35000.0 * 2 * 0.10) // 7000
        val potonganPoin = 5000.0
        val potonganVoucher = 10000.0
        val totalDiskon = diskonItem + potonganPoin + potonganVoucher
        val grandTotal = subtotal - totalDiskon // 130000

        val sampleTx = TransactionEntity(
            bonNo = "BON-20261019-0012",
            kasirNik = "K001",
            kasirNama = "Siti Rahma",
            memberNo = "MBR-88019",
            subtotal = subtotal,
            diskonTotal = totalDiskon,
            donasi = 0.0,
            grandTotal = grandTotal,
            caraBayar = "TUNAI",
            bayarTunai = 150000.0,
            bayarNonTunai = 0.0,
            kembalian = 20000.0,
            poinDigunakan = 50,
            nilaiPotonganPoin = potonganPoin,
            kodeVoucher = "VCR-HEMAT10",
            nilaiPotonganVoucher = potonganVoucher,
            refPembayaran = "",
            voucherDiberikan = if (config.voucherAutoGenerateOnReceipt) "VCR-89241" else "",
            voucherDiberikanNominal = config.voucherDefaultNominal,
            voucherDiberikanExpiry = "26/10/2026",
            timestamp = System.currentTimeMillis()
        )

        return formatReceipt(sampleTx, sampleItems, config)
    }

    // -------------------------------------------------------------
    // MODEL 1: STANDAR RITEL / SUPERMARKET (Indomaret / Alfamart)
    // -------------------------------------------------------------
    private fun formatStandard(
        tx: TransactionEntity,
        items: List<CartItem>,
        cfg: StoreConfigEntity,
        w: Int
    ): String = buildString {
        val sep = line('-', w)
        val dblSep = line('=', w)

        if (cfg.receiptShowStoreName && cfg.storeName.isNotBlank()) {
            appendLine(center(cfg.storeName.uppercase(), w))
        }
        if (cfg.receiptShowAlamat && cfg.alamat.isNotBlank()) {
            appendLine(center(cfg.alamat, w))
        }
        if (cfg.receiptShowTelp && cfg.telp.isNotBlank()) {
            appendLine(center("TELP/WA: ${cfg.telp}", w))
        }
        if (cfg.receiptShowNpwp && cfg.npwp.isNotBlank()) {
            appendLine(center("NPWP: ${cfg.npwp}", w))
        }
        if (cfg.receiptCustomGreeting.isNotBlank()) {
            appendLine(center(cfg.receiptCustomGreeting, w))
        }

        appendLine(sep)

        // Metadata Header
        if (cfg.receiptShowBonNo && cfg.receiptShowKasir) {
            appendLine(justify("NO: ${tx.bonNo}", "KASIR: ${tx.kasirNama}", w))
        } else if (cfg.receiptShowBonNo) {
            appendLine("NO BON : ${tx.bonNo}")
        } else if (cfg.receiptShowKasir) {
            appendLine("KASIR  : ${tx.kasirNama}")
        }

        if (cfg.receiptShowDateTime) {
            appendLine("WAKTU  : ${tx.tanggal} ${tx.waktu}")
        }

        if (cfg.receiptShowMemberInfo && tx.memberNo.isNotBlank()) {
            appendLine("MEMBER : ${tx.memberNo}")
        }

        appendLine(sep)

        // Product Items
        if (items.isEmpty()) {
            appendLine(center("(Tidak ada rincian item)", w))
        } else {
            items.forEach { item ->
                val p = item.product
                val pluTag = if (cfg.receiptShowPlu && p.plu.isNotBlank()) "[${p.plu}] " else ""
                val unitTag = if (item.selectedSatuan.isNotBlank()) " (${item.selectedSatuan})" else ""
                val fullName = "$pluTag${p.nama}$unitTag"

                val unitPrice = item.unitPrice
                val lineTotal = item.subtotal
                val lineTotalStr = formatRupiah(kotlin.math.abs(lineTotal))

                if (w >= 48) {
                    // Wide format 80mm
                    val itemDetail = "${item.qty} ${item.selectedSatuan} x ${formatRupiah(unitPrice)}"
                    val namePart = fullName.take(24).padEnd(24)
                    val detailPart = itemDetail.padStart(12)
                    val totPart = lineTotalStr.padStart(10)
                    appendLine("$namePart$detailPart  $totPart")
                } else {
                    // 58mm 2-line format
                    appendLine(fullName.take(w))
                    val qtyPrice = "  ${item.qty} ${item.selectedSatuan} x ${formatRupiah(unitPrice)}"
                    appendLine(justify(qtyPrice, lineTotalStr, w))
                }

                if (cfg.receiptShowDiskon && item.promoDiscountAmount > 0) {
                    appendLine("  * Promo (-Rp ${formatRupiah(item.promoDiscountAmount)})")
                } else if (cfg.receiptShowDiskon && p.isPromo && p.diskonPersen > 0) {
                    val diskonVal = (item.qty * unitPrice * p.diskonPersen / 100.0)
                    appendLine("  * Diskon ${p.diskonPersen}% (-Rp ${formatRupiah(diskonVal)})")
                }
                if (item.customDiscount > 0) {
                    appendLine("  * Potongan Manual (-Rp ${formatRupiah(item.customDiscount)})")
                }
                if (item.isVoid) {
                    appendLine("  ** DIBATALKAN / VOID (${item.voidReason}) **")
                } else if (item.isRetur) {
                    appendLine("  ** RETUR BARANG KONSUMEN **")
                }
            }
        }

        appendLine(sep)

        // Totals & Calculations
        if (cfg.receiptShowTotalItemQty) {
            val totalQty = items.filter { !it.isVoid }.sumOf { if (it.isRetur) -it.qty else it.qty }
            appendLine(justify("Total Item / Qty", "${items.size} / $totalQty pcs", w))
        }

        appendLine(justify("Subtotal", "Rp ${formatRupiah(tx.subtotal)}", w))

        if (tx.nilaiPotonganPoin > 0) {
            appendLine(justify("Poin Member (${tx.poinDigunakan} pt)", "-Rp ${formatRupiah(tx.nilaiPotonganPoin)}", w))
        }

        if (tx.nilaiPotonganVoucher > 0) {
            appendLine(justify("Voucher (${tx.kodeVoucher})", "-Rp ${formatRupiah(tx.nilaiPotonganVoucher)}", w))
        }

        appendLine(dblSep)
        appendLine(justify("TOTAL BELANJA", "Rp ${formatRupiah(tx.grandTotal)}", w))
        appendLine(dblSep)

        // Payment Info
        if (cfg.receiptShowPaymentDetail) {
            appendLine(justify("Cara Bayar", tx.caraBayar, w))
            if (tx.caraBayar == "CAMPURAN") {
                appendLine(justify("  - Tunai", "Rp ${formatRupiah(tx.bayarTunai)}", w))
                appendLine(justify("  - Non-Tunai", "Rp ${formatRupiah(tx.bayarNonTunai)}", w))
            } else if (tx.caraBayar == "TUNAI") {
                appendLine(justify("Bayar Tunai", "Rp ${formatRupiah(tx.bayarTunai)}", w))
            } else {
                if (tx.refPembayaran.isNotBlank()) {
                    appendLine(justify("Ref ID", tx.refPembayaran, w))
                }
            }
            appendLine(justify("Kembalian", "Rp ${formatRupiah(tx.kembalian)}", w))
        }

        if (cfg.receiptShowPpn && cfg.isTaxActive && cfg.ppnPercentage > 0.0) {
            val rate = cfg.ppnPercentage / 100.0
            val taxAmount = if (cfg.isTaxIncluded) {
                tx.grandTotal * rate / (1.0 + rate)
            } else {
                tx.subtotal * rate
            }
            val rateStr = if (cfg.ppnPercentage % 1.0 == 0.0) "${cfg.ppnPercentage.toInt()}%" else "${cfg.ppnPercentage}%"
            val label = if (cfg.isTaxIncluded) {
                "Termasuk ${cfg.taxName} $rateStr"
            } else {
                "${cfg.taxName} $rateStr"
            }
            appendLine(justify(label, "Rp ${formatRupiah(taxAmount)}", w))
        }

        if (cfg.receiptShowHemat && tx.diskonTotal > 0) {
            appendLine(sep)
            appendLine(center("ANDA HEMAT: Rp ${formatRupiah(tx.diskonTotal)}", w))
        }

        // Earned points info
        if (cfg.receiptShowEarnedPoints && cfg.poinAktif && tx.memberNo.isNotBlank()) {
            val earnedPoints = (tx.grandTotal / maxOf(1.0, cfg.belanjaPerPoin)).toInt()
            appendLine(sep)
            appendLine(center("LOYALTY REWARDS", w))
            appendLine(justify("Poin Diperoleh", "+$earnedPoints poin", w))
        }

        // Issued Voucher
        if (cfg.receiptShowVoucher && tx.voucherDiberikan.isNotBlank()) {
            appendLine(sep)
            appendLine(center("KUPON VOUCHER BELANJA", w))
            appendLine(center("KODE: ${tx.voucherDiberikan}", w))
            appendLine(center("NILAI: Rp ${formatRupiah(tx.voucherDiberikanNominal)}", w))
            if (cfg.voucherMinBelanjaPakai > 0) {
                appendLine(center("Min Belanja: Rp ${formatRupiah(cfg.voucherMinBelanjaPakai)}", w))
            }
            appendLine(center("Berlaku s/d: ${tx.voucherDiberikanExpiry}", w))
            appendLine(center("*Gunakan kode ini pd belanja berikutnya", w))
        }

        if (cfg.receiptWifiInfo.isNotBlank()) {
            appendLine(sep)
            appendLine(center(cfg.receiptWifiInfo, w))
        }

        if (cfg.printQrisOnReceipt || tx.caraBayar == "QRIS") {
            appendLine(sep)
            appendLine(center("QRIS PEMBAYARAN DIGITAL", w))
            if (cfg.qrisMerchantName.isNotBlank()) {
                appendLine(center(cfg.qrisMerchantName.uppercase(), w))
            }
            if (cfg.qrisNmid.isNotBlank()) {
                appendLine(center("NMID: ${cfg.qrisNmid}", w))
            }
            appendLine(center("|||| | ||||| || |||||| | |||", w))
            appendLine(center("SCAN DENGAN M-BANKING / E-WALLET", w))
        }

        if (cfg.receiptShowBarcodeSimulation) {
            appendLine(sep)
            appendLine(center("|||| | ||||| || |||||| | |||", w))
            appendLine(center("* ${tx.bonNo} *", w))
        }

        appendLine(sep)
        if (cfg.footerMsg.isNotBlank()) {
            cfg.footerMsg.lines().forEach { line ->
                appendLine(center(line.trim(), w))
            }
        }
        if (cfg.receiptPolicyMsg.isNotBlank()) {
            cfg.receiptPolicyMsg.lines().forEach { line ->
                appendLine(center(line.trim(), w))
            }
        }
    }

    // -------------------------------------------------------------
    // MODEL 2: RINGKAS / HEMAT KERTAS (Compact & Eco-friendly)
    // -------------------------------------------------------------
    private fun formatCompact(
        tx: TransactionEntity,
        items: List<CartItem>,
        cfg: StoreConfigEntity,
        w: Int
    ): String = buildString {
        val sep = line('-', w)

        if (cfg.receiptShowStoreName && cfg.storeName.isNotBlank()) {
            appendLine(center(cfg.storeName.uppercase(), w))
        }
        if (cfg.receiptShowTelp && cfg.telp.isNotBlank()) {
            appendLine(center("Telp: ${cfg.telp}", w))
        }

        appendLine(justify("${tx.bonNo}", "${tx.tanggal}", w))
        appendLine(justify("Kasir: ${tx.kasirNama}", "${tx.waktu}", w))
        if (tx.memberNo.isNotBlank()) {
            appendLine("Member: ${tx.memberNo}")
        }
        appendLine(sep)

        if (items.isEmpty()) {
            appendLine(center("(Tidak ada rincian item)", w))
        } else {
            items.forEach { item ->
                val p = item.product
                val unitPrice = item.unitPrice
                val lineTotal = item.subtotal
                val nameShort = p.nama.take(15).padEnd(15)
                val qtyStr = "${item.qty}x".padStart(3)
                val totalStr = formatRupiah(kotlin.math.abs(lineTotal)).padStart(9)
                appendLine("$nameShort $qtyStr $totalStr")
            }
        }

        appendLine(sep)
        appendLine(justify("TOTAL", "Rp ${formatRupiah(tx.grandTotal)}", w))
        appendLine(justify("Bayar (${tx.caraBayar})", "Rp ${formatRupiah(tx.bayarTunai + tx.bayarNonTunai)}", w))
        if (tx.kembalian > 0) {
            appendLine(justify("Kembali", "Rp ${formatRupiah(tx.kembalian)}", w))
        }

        if (tx.diskonTotal > 0) {
            appendLine(justify("Hemat", "Rp ${formatRupiah(tx.diskonTotal)}", w))
        }

        appendLine(sep)
        appendLine(center("Terima Kasih - Simpan Struk Ini", w))
    }

    // -------------------------------------------------------------
    // MODEL 3: CAFE, RESTO & F&B (Order / Dine-In / PB1 / Wi-Fi)
    // -------------------------------------------------------------
    private fun formatCafe(
        tx: TransactionEntity,
        items: List<CartItem>,
        cfg: StoreConfigEntity,
        w: Int
    ): String = buildString {
        val sep = line('-', w)
        val waveSep = line('~', w)

        if (cfg.receiptShowStoreName && cfg.storeName.isNotBlank()) {
            appendLine(center("☕ ${cfg.storeName.uppercase()} ☕", w))
        }
        if (cfg.receiptShowAlamat && cfg.alamat.isNotBlank()) {
            appendLine(center(cfg.alamat, w))
        }
        appendLine(center("BILL / ORDER RECEIPT", w))
        appendLine(waveSep)

        appendLine(justify("ORDER: #${tx.bonNo.takeLast(4)}", "MEJA: 05 [DINE-IN]", w))
        appendLine(justify("SERVER: ${tx.kasirNama}", "${tx.tanggal} ${tx.waktu}", w))

        if (tx.memberNo.isNotBlank()) {
            appendLine("TAMU / MEMBER: ${tx.memberNo}")
        }

        appendLine(sep)
        appendLine(justify("MENU PESANAN", "TOTAL", w))
        appendLine(sep)

        if (items.isEmpty()) {
            appendLine(center("(Tidak ada menu pesanan)", w))
        } else {
            items.forEach { item ->
                val p = item.product
                val unitPrice = item.unitPrice
                val lineTotal = item.subtotal
                val unitTag = if (item.selectedSatuan.isNotBlank()) " (${item.selectedSatuan})" else ""
                appendLine("${item.qty}x ${p.nama}$unitTag")
                val subItem = "@${formatRupiah(unitPrice)}"
                val totItem = "Rp ${formatRupiah(kotlin.math.abs(lineTotal))}"
                appendLine(justify("   $subItem", totItem, w))
            }
        }

        appendLine(sep)
        appendLine(justify("Subtotal", "Rp ${formatRupiah(tx.subtotal)}", w))

        if (tx.diskonTotal > 0) {
            appendLine(justify("Diskon Promo", "-Rp ${formatRupiah(tx.diskonTotal)}", w))
        }

        if (cfg.receiptShowPpn && cfg.isTaxActive && cfg.ppnPercentage > 0.0) {
            val rate = cfg.ppnPercentage / 100.0
            val taxAmount = if (cfg.isTaxIncluded) {
                tx.grandTotal * rate / (1.0 + rate)
            } else {
                tx.subtotal * rate
            }
            val rateStr = if (cfg.ppnPercentage % 1.0 == 0.0) "${cfg.ppnPercentage.toInt()}%" else "${cfg.ppnPercentage}%"
            val label = if (cfg.isTaxIncluded) {
                "Termasuk ${cfg.taxName} $rateStr"
            } else {
                "${cfg.taxName} $rateStr"
            }
            appendLine(justify(label, "Rp ${formatRupiah(taxAmount)}", w))
        }

        appendLine(line('=', w))
        appendLine(justify("TOTAL TAGIHAN", "Rp ${formatRupiah(tx.grandTotal)}", w))
        appendLine(line('=', w))

        appendLine(justify("Metode Bayar", tx.caraBayar, w))
        appendLine(justify("Status", "LUNAS", w))

        if (cfg.receiptWifiInfo.isNotBlank()) {
            appendLine(sep)
            appendLine(center("📶 FREE WI-FI & SOSMED", w))
            cfg.receiptWifiInfo.lines().forEach {
                appendLine(center(it.trim(), w))
            }
        }

        appendLine(waveSep)
        appendLine(center("Terima Kasih & Selamat Menikmati!", w))
        appendLine(center("Tag IG Kami: @${cfg.storeCode.lowercase()}", w))
    }

    // -------------------------------------------------------------
    // MODEL 4: ELEGAN / BERBINGKAI (Boutique & Premium)
    // -------------------------------------------------------------
    private fun formatElegant(
        tx: TransactionEntity,
        items: List<CartItem>,
        cfg: StoreConfigEntity,
        w: Int
    ): String = buildString {
        val dblSep = line('=', w)
        val dotSep = line('*', w)
        val sep = line('-', w)

        appendLine(dblSep)
        if (cfg.receiptShowStoreName && cfg.storeName.isNotBlank()) {
            appendLine(center("✨ ${cfg.storeName.uppercase()} ✨", w))
        }
        if (cfg.receiptShowAlamat && cfg.alamat.isNotBlank()) {
            appendLine(center(cfg.alamat, w))
        }
        if (cfg.receiptShowTelp && cfg.telp.isNotBlank()) {
            appendLine(center("WhatsApp: ${cfg.telp}", w))
        }
        appendLine(dotSep)

        appendLine(justify("Faktur : ${tx.bonNo}", "Kasir : ${tx.kasirNama}", w))
        appendLine(justify("Tanggal: ${tx.tanggal}", "Jam   : ${tx.waktu}", w))
        if (tx.memberNo.isNotBlank()) {
            appendLine(center("VIP MEMBER: ${tx.memberNo}", w))
        }

        appendLine(dblSep)

        if (items.isEmpty()) {
            appendLine(center("(Tidak ada rincian item)", w))
        } else {
            items.forEach { item ->
                val p = item.product
                val unitPrice = item.unitPrice
                val lineTotal = item.subtotal
                val unitTag = if (item.selectedSatuan.isNotBlank()) " (${item.selectedSatuan})" else ""
                appendLine("${p.nama.uppercase()}$unitTag")
                val detail = "${item.qty} ${item.selectedSatuan} @ Rp ${formatRupiah(unitPrice)}"
                val tot = "Rp ${formatRupiah(kotlin.math.abs(lineTotal))}"
                appendLine(justify("  $detail", tot, w))
            }
        }

        appendLine(sep)
        appendLine(justify("SUBTOTAL", "Rp ${formatRupiah(tx.subtotal)}", w))
        if (tx.diskonTotal > 0) {
            appendLine(justify("PRIVILEGE DISCOUNT", "-Rp ${formatRupiah(tx.diskonTotal)}", w))
        }
        appendLine(dblSep)
        appendLine(justify("TOTAL AKHIR", "Rp ${formatRupiah(tx.grandTotal)}", w))
        appendLine(dblSep)

        appendLine(justify("PEMBAYARAN", tx.caraBayar, w))
        appendLine(justify("DITERIMA", "Rp ${formatRupiah(tx.bayarTunai + tx.bayarNonTunai)}", w))
        if (tx.kembalian > 0) {
            appendLine(justify("KEMBALI", "Rp ${formatRupiah(tx.kembalian)}", w))
        }

        if (cfg.receiptShowBarcodeSimulation) {
            appendLine(sep)
            appendLine(center("|||||| |||| | |||||| |||", w))
            appendLine(center(tx.bonNo, w))
        }

        appendLine(dotSep)
        appendLine(center("Kesenangan Melayani Anda!", w))
        appendLine(center("DodolanPro Exclusive Retail", w))
        appendLine(dblSep)
    }

    /**
     * Convert text receipt into ESC/POS bytes for thermal printers.
     */
    fun generateEscPos(
        receiptText: String,
        config: StoreConfigEntity,
        openDrawer: Boolean = false,
        cutPaper: Boolean = false
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // 1. ESC @ (Initialize printer)
        stream.write(byteArrayOf(0x1B, 0x40))

        // 2. Open Cash Drawer if requested
        if (openDrawer || config.printerOpenCashDrawer) {
            stream.write(byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte()))
        }

        // 3. Set character font (Condensed or Normal)
        if (config.printerFontSize.equals("CONDENSED", ignoreCase = true)) {
            stream.write(byteArrayOf(0x1B, 0x4D, 0x01)) // Font B (smaller)
        } else {
            stream.write(byteArrayOf(0x1B, 0x4D, 0x00)) // Font A (standard)
        }

        // 4. Print body text
        val textBytes = receiptText.toByteArray(Charsets.ISO_8859_1)
        stream.write(textBytes)

        // 5. Feed lines
        val feeds = maxOf(1, config.printerFeedLines)
        stream.write(byteArrayOf(0x1B, 0x64, feeds.toByte()))

        // 6. Paper Cut command if requested
        if (cutPaper || config.printerAutoCut) {
            stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        }

        return stream.toByteArray()
    }
}
