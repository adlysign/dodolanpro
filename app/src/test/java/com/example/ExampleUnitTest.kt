package com.example

import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.viewmodel.CartItem
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testRupiahFormatting() {
        val formatted = formatRupiah(19500.0)
        assertTrue(formatted.contains("19.500") || formatted.contains("19,500"))
    }

    @Test
    fun testCartItemCalculation() {
        val product = ProductEntity(
            plu = "234",
            barcode = "899990902823",
            nama = "DJI SAM SOE 12",
            kategori = "Rokok",
            hargaBeli = 17500.0,
            hargaJual = 19700.0,
            hargaMember = 19500.0,
            stok = 100
        )

        val regularItem = CartItem(product = product, qty = 2)
        assertEquals(39400.0, regularItem.subtotal, 0.01)

        val voidItem = regularItem.copy(isVoid = true)
        assertEquals(0.0, voidItem.subtotal, 0.01)

        val returItem = CartItem(product = product, qty = 1, isRetur = true)
        assertEquals(-19700.0, returItem.subtotal, 0.01)
    }

    @Test
    fun testReturAndReplacementCart() {
        val prodRetur = ProductEntity(
            plu = "7132",
            barcode = "899421411012",
            nama = "KG SALTCHEESE 200G",
            kategori = "Biskuit",
            hargaBeli = 10000.0,
            hargaJual = 11700.0,
            hargaMember = 11500.0,
            stok = 50
        )
        val prodReplacement = ProductEntity(
            plu = "234",
            barcode = "899990902823",
            nama = "DJI SAM SOE 12",
            kategori = "Rokok",
            hargaBeli = 17500.0,
            hargaJual = 19700.0,
            hargaMember = 19500.0,
            stok = 100
        )

        val items = listOf(
            CartItem(product = prodRetur, qty = 1, isRetur = true),
            CartItem(product = prodReplacement, qty = 1)
        )

        val total = items.sumOf { it.subtotal }
        assertEquals(8000.0, total, 0.01) // 19700 - 11700 = 8000
    }

    @Test
    fun testOwnerEntityProperties() {
        val owner = com.example.data.local.entity.CashierUserEntity(
            nik = "11111111",
            nama = "Owner",
            pin = "1234",
            role = "OWNER"
        )
        assertEquals("11111111", owner.nik)
        assertEquals("Owner", owner.nama)
        assertEquals("1234", owner.pin)
        assertEquals("OWNER", owner.role)
        assertTrue(owner.statusAktif)
    }
}
