package com.example

import android.os.Bundle
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
import com.example.ui.viewmodel.PosViewModel
import com.example.util.DocExportManager

class MainActivity : ComponentActivity() {
    private val viewModel: PosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DocExportManager.ensureAllDodolanDirectories(this)

        setContent {
            DodolanProTheme {
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

    override fun onStop() {
        super.onStop()
        viewModel.autoPendingCartOnEvent("Aplikasi Ditutup")
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.autoPendingCartOnEvent("Aplikasi Ditutup")
    }
}
