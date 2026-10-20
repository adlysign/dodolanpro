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
}
