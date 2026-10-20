package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.activity.compose.BackHandler
import com.example.ui.screens.backup.BackupRestoreScreen
import com.example.ui.screens.clerek.ClerekScreen
import com.example.ui.screens.pos.PosScreen
import com.example.ui.screens.sis.*
import com.example.ui.theme.DodolanProTheme
import com.example.ui.viewmodel.PosNavDestination
import com.example.ui.viewmodel.PosShortcutEvent
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager

import com.example.ui.screens.splash.SplashScreen

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (viewModel.currentDestination.value == PosNavDestination.KASIR) {
            when (keyCode) {
                KeyEvent.KEYCODE_F1 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F1_HELP)
                    return true
                }
                KeyEvent.KEYCODE_F2 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F2_CARI)
                    return true
                }
                KeyEvent.KEYCODE_F3 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F3_MEMBER)
                    return true
                }
                KeyEvent.KEYCODE_F4 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F4_BATAL)
                    return true
                }
                KeyEvent.KEYCODE_F5 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F5_PENDING)
                    return true
                }
                KeyEvent.KEYCODE_F6 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F6_RECALL)
                    return true
                }
                KeyEvent.KEYCODE_F7 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F7_RETUR)
                    return true
                }
                KeyEvent.KEYCODE_F8 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F8_VOID)
                    return true
                }
                KeyEvent.KEYCODE_F10 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F10_SIS)
                    return true
                }
                KeyEvent.KEYCODE_F12 -> {
                    viewModel.triggerShortcut(PosShortcutEvent.F12_BAYAR)
                    return true
                }
                KeyEvent.KEYCODE_ESCAPE -> {
                    viewModel.triggerShortcut(PosShortcutEvent.ESCAPE)
                    return true
                }
                KeyEvent.KEYCODE_FORWARD_DEL -> {
                    viewModel.triggerShortcut(PosShortcutEvent.DELETE_ITEM)
                    return true
                }
                KeyEvent.KEYCODE_NUMPAD_ADD -> {
                    viewModel.triggerShortcut(PosShortcutEvent.PLUS_PAY)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    viewModel.triggerShortcut(PosShortcutEvent.ARROW_UP)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    viewModel.triggerShortcut(PosShortcutEvent.ARROW_DOWN)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DocExportManager.ensureAllDodolanDirectories(this)

        setContent {
            DodolanProTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onSplashFinished = { showSplash = false })
                } else {
                    val currentDest by viewModel.currentDestination.collectAsState()

                    BackHandler(enabled = currentDest != PosNavDestination.KASIR) {
                        if (currentDest == PosNavDestination.SIS_DASHBOARD) {
                            viewModel.navigateTo(PosNavDestination.KASIR)
                        } else {
                            viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD)
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                    when (currentDest) {
                        PosNavDestination.KASIR -> PosScreen(
                            viewModel = viewModel,
                            onNavigateTo = { viewModel.navigateTo(it) }
                        )
                        PosNavDestination.SIS_DASHBOARD -> SisDashboardScreen(
                            viewModel = viewModel,
                            onNavigateTo = { viewModel.navigateTo(it) }
                        )
                        PosNavDestination.CLEREK -> ClerekScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.KASIR) }
                        )
                        PosNavDestination.BACKUP_RESTORE -> BackupRestoreScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.SINKRONISASI_MULTI_DEVICE -> MultiDeviceSyncScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PRODUK -> ProductManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.EOD_TUTUP_TOKO -> EodScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.LAPORAN_TOKO -> ReportsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.EDIT_TOKO -> StoreConfigScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PEMBELIAN -> PurchaseScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PENJUALAN -> SalesHistoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) },
                            onNavigateToRetur = { viewModel.navigateTo(PosNavDestination.KASIR) }
                        )
                        PosNavDestination.CETAK_LABEL_HARGA -> PriceTagScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.STOK_OPNAME -> StockOpnameScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.RETUR_PEMBELIAN -> ReturnPurchaseScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.KARYAWAN -> EmployeeManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.VOUCHER_POIN -> VoucherPointsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PENGATURAN_MEMBER -> MemberManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PENGATURAN_SHIFT -> ShiftManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                        PosNavDestination.PENGATURAN_PRINTER -> PrinterSettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { viewModel.navigateTo(PosNavDestination.SIS_DASHBOARD) }
                        )
                    }
                }
            }
        }
    }
    }

    override fun onStop() {
        super.onStop()
        viewModel.autoPendingCartOnEvent("Aplikasi Ditutup")
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.autoPendingCartOnEvent("Aplikasi Ditutup")
    }
}
