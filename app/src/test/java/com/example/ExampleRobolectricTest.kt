package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DodolanPro", appName)
  }

  @Test
  fun `test database creation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    val db = com.example.data.local.AppDatabase.getDatabase(context, scope)
    com.example.data.local.AppDatabase.populateInitialData(db.posDao())
    val config = db.posDao().getStoreConfigOnce()
    assert(config != null)
  }

  @Test
  fun `test main activity launch`() {
    androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assert(activity != null)
      }
    }
  }

  @Test
  fun `test license generator and validator`() {
    val devId = "DEV-KASIR-1234"
    val proKey = com.example.util.PosLicenseHelper.generateLicenseKey(devId, "PRO")
    assert(proKey.startsWith("PRO-"))

    val (proOk, proTier) = com.example.util.PosLicenseHelper.validateLicenseKey(devId, proKey)
    assert(proOk)
    assertEquals("PRO LIFETIME", proTier)

    val entKey = com.example.util.PosLicenseHelper.generateLicenseKey(devId, "ENTERPRISE")
    assert(entKey.startsWith("ENT-"))

    val (entOk, entTier) = com.example.util.PosLicenseHelper.validateLicenseKey(devId, entKey)
    assert(entOk)
    assertEquals("ENTERPRISE LIFETIME", entTier)

    // Invalid device test
    val (wrongDevOk, _) = com.example.util.PosLicenseHelper.validateLicenseKey("DEV-OTHER-9999", proKey)
    assert(!wrongDevOk)

    // Master emergency key test
    val (masterOk, masterTier) = com.example.util.PosLicenseHelper.validateLicenseKey(devId, "MASTER-PRO-LIFETIME")
    assert(masterOk)
    assertEquals("PRO LIFETIME", masterTier)
  }

  @Test
  fun `test initial install default to trial mode and 50 transaction limit`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    val db = com.example.data.local.AppDatabase.getDatabase(context, scope)
    com.example.data.local.AppDatabase.populateInitialData(db.posDao())
    val config = db.posDao().getStoreConfigOnce()
    assert(config != null)
    assert(config!!.isTrial)
    assertEquals("TRIAL", config.licenseType)
    assertEquals(false, config.isLicensed)
    assertEquals(50, com.example.ui.viewmodel.PosViewModel.TRIAL_TRANSACTION_LIMIT)
  }

  @Test
  fun `test shift isolation when user A logs out and user B logs in`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PosViewModel(app)

    // Save test cashier User B
    val userB = com.example.data.local.entity.CashierUserEntity("22222222", "User B", "4321", "KASIR")
    vm.repository.saveCashier(userB)

    // 1. User A (Owner) logs in and opens shift
    var loginAOk = false
    vm.loginCashier("11111111", "1234") { ok, _ -> loginAOk = ok }
    for (i in 1..30) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (loginAOk) break
      Thread.sleep(50)
    }
    assert(loginAOk)
    assert(!vm.isShiftOpen.value) // Before open shift, shift is closed

    vm.openShift(1, 150000.0)
    assert(vm.isShiftOpen.value) // User A shift is now open

    // 2. User A logs out
    vm.logoutCashier()
    assert(!vm.isUserLoggedIn.value)
    assert(!vm.isShiftOpen.value) // Shift is closed on logout so next cashier cannot transact

    // 3. User B logs in
    var loginBOk = false
    vm.loginCashier("22222222", "4321") { ok, _ -> loginBOk = ok }
    for (i in 1..30) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (loginBOk) break
      Thread.sleep(50)
    }
    assert(loginBOk)
    // User B MUST have isShiftOpen == false because User B hasn't opened their shift!
    assertEquals("User B must open shift first and cannot inherit User A's shift", false, vm.isShiftOpen.value)

    // Validation should block transaction for User B because shift is not open
    val check = vm.validateTransactionForCheckout()
    assert(!check.first)
    assert(check.second.contains("Shift belum dibuka"))
  }

  @Test
  fun `test zero transaction amount is rejected`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PosViewModel(app)
    vm.openShift(1, 100000.0)

    // Empty cart -> rejected
    val checkEmpty = vm.validateTransactionForCheckout()
    assert(!checkEmpty.first)
    assert(checkEmpty.second.contains("kosong"))

    // Product with price 0
    val zeroProduct = com.example.data.local.entity.ProductEntity(
      plu = "FREE01",
      barcode = "999000",
      nama = "Sample Gratis",
      kategori = "Umum",
      hargaBeli = 0.0,
      hargaJual = 0.0,
      stok = 10
    )
    vm.repository.insertProduct(zeroProduct)
    vm.addProductToCart(zeroProduct, 1)

    // Subtotal 0 / GrandTotal 0 -> rejected
    val checkZero = vm.validateTransactionForCheckout()
    assert(!checkZero.first)
    assert(checkZero.second.contains("Rp 0"))
  }

  @Test
  fun `test out of stock product cannot proceed with transaction`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PosViewModel(app)
    vm.openShift(1, 100000.0)

    val outOfStockProduct = com.example.data.local.entity.ProductEntity(
      plu = "OUT01",
      barcode = "999111",
      nama = "Barang Habis",
      kategori = "Snack",
      hargaBeli = 5000.0,
      hargaJual = 8000.0,
      stok = 0
    )
    vm.repository.insertProduct(outOfStockProduct)

    // Adding 0 stock product to cart should fail
    val addResult = vm.addProductToCart(outOfStockProduct, 1)
    assert(!addResult.first)
    assert(addResult.second.contains("stok habis") || addResult.second.contains("(0)"))
  }

  @Test
  fun `test cashier permission restrictions in SIS menus`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PosViewModel(app)

    // Create restricted cashier: can access SIS, but cannot manage products, employees, or settings
    val restrictedCashier = com.example.data.local.entity.CashierUserEntity(
      nik = "KASIR01",
      nama = "Budi Kasir",
      pin = "1234",
      role = "KASIR",
      statusAktif = true,
      canAccessSis = true,
      canManageProduk = false,
      canManageKaryawan = false,
      canManageSettings = false,
      canBackupRestore = false,
      canEodTutupToko = false,
      canClerekKasir = true
    )
    vm.repository.saveCashier(restrictedCashier)

    // Login as restricted cashier
    var loginDone = false
    vm.loginCashier("KASIR01", "1234") { ok, _ -> loginDone = ok }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (loginDone) break
      Thread.sleep(50)
    }
    assert(loginDone)

    // Verify permission checks for restricted destinations
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.KASIR))
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.SIS_DASHBOARD))
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.CLEREK))
    assert(!vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.PRODUK))
    assert(!vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.KARYAWAN))
    assert(!vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.EDIT_TOKO))
    assert(!vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.BACKUP_RESTORE))
    assert(!vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.EOD_TUTUP_TOKO))

    // Navigation to restricted destination should be blocked
    vm.navigateTo(com.example.ui.viewmodel.PosNavDestination.KARYAWAN)
    assert(vm.currentDestination.value != com.example.ui.viewmodel.PosNavDestination.KARYAWAN)

    // Now login as Owner
    var ownerLoginDone = false
    vm.loginCashier("11111111", "1234") { ok, _ -> ownerLoginDone = ok }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (ownerLoginDone) break
      Thread.sleep(50)
    }
    assert(ownerLoginDone)

    // Owner has full permission to all destinations
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.PRODUK))
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.KARYAWAN))
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.EDIT_TOKO))
    assert(vm.hasPermissionForDestination(com.example.ui.viewmodel.PosNavDestination.BACKUP_RESTORE))

    // Owner can navigate freely
    vm.navigateTo(com.example.ui.viewmodel.PosNavDestination.KARYAWAN)
    assertEquals(com.example.ui.viewmodel.PosNavDestination.KARYAWAN, vm.currentDestination.value)
  }

  @Test
  fun `test only owner can activate or reset license, non-owners are rejected`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.viewmodel.PosViewModel(app)

    // 1. Login as standard Kasir (non-owner)
    val cashier = com.example.data.local.entity.CashierUserEntity(
      nik = "KASIR99",
      nama = "Siti Kasir",
      pin = "9999",
      role = "KASIR",
      statusAktif = true
    )
    vm.repository.saveCashier(cashier)

    var kasirLoginDone = false
    vm.loginCashier("KASIR99", "9999") { ok, _ -> kasirLoginDone = ok }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (kasirLoginDone) break
      Thread.sleep(50)
    }
    assert(kasirLoginDone)
    assert(vm.currentCashier.value?.isOwner() == false)

    // Non-owner attempts to activate license with valid key -> MUST BE REJECTED
    val devId = vm.deviceId
    val proKey = com.example.util.PosLicenseHelper.generateLicenseKey(devId, "PRO")
    var kasirActivationOk = false
    var kasirMsg = ""
    vm.activateOrUpgradeLicense(proKey) { ok, msg ->
      kasirActivationOk = ok
      kasirMsg = msg
    }
    assert(!kasirActivationOk)
    assert(kasirMsg.contains("Hanya Owner"))
    assert(!vm.storeConfig.value.isLicensed)

    // 2. Now login as Owner
    var ownerLoginDone = false
    vm.loginCashier("11111111", "1234") { ok, _ -> ownerLoginDone = ok }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (ownerLoginDone) break
      Thread.sleep(50)
    }
    assert(ownerLoginDone)
    assert(vm.currentCashier.value?.isOwner() == true)

    // Owner activates license -> SUCCEEDS
    var ownerActivationOk = false
    vm.activateOrUpgradeLicense(proKey) { ok, _ -> ownerActivationOk = ok }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (ownerActivationOk) break
      Thread.sleep(50)
    }
    assert(ownerActivationOk)
    assert(vm.storeConfig.value.isLicensed)
    assertEquals("PRO LIFETIME", vm.storeConfig.value.licenseType)

    // Owner resets license -> SUCCEEDS
    var resetDone = false
    vm.resetLicenseToTrial { resetDone = true }
    for (i in 1..20) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      if (resetDone) break
      Thread.sleep(50)
    }
    assert(resetDone)
    assert(!vm.storeConfig.value.isLicensed)
    assertEquals("TRIAL", vm.storeConfig.value.licenseType)
  }
}

