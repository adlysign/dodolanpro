package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.CashierUserEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val cashiers by viewModel.cashiers.collectAsState()
    val currentCashier by viewModel.currentCashier.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("SEMUA") }

    var editingCashier by remember { mutableStateOf<CashierUserEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var cashierToDelete by remember { mutableStateOf<CashierUserEntity?>(null) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    // Filter cashiers
    val filteredCashiers = remember(cashiers, searchQuery, selectedRoleFilter) {
        cashiers.filter { c ->
            val matchesSearch = c.nama.contains(searchQuery, ignoreCase = true) ||
                    c.nik.contains(searchQuery, ignoreCase = true)
            val matchesRole = when (selectedRoleFilter) {
                "SEMUA" -> true
                "AKTIF" -> c.statusAktif
                "NONAKTIF" -> !c.statusAktif
                else -> c.role.equals(selectedRoleFilter, ignoreCase = true)
            }
            matchesSearch && matchesRole
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("btn_back_employee")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Column {
                        Text(
                            "Kelola Kasir & Hak Akses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            "Wewenang POS Kasir, Shift, & Akses Back Office",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { isCreatingNew = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RetailRedPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_add_cashier")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH KASIR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = {
            feedbackMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { feedbackMessage = null }) {
                            Text("TUTUP", color = Color.White)
                        }
                    },
                    containerColor = Color(0xFF323232),
                    contentColor = Color.White
                ) {
                    Text(msg, fontSize = 13.sp)
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF6F8FA))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stats Overview Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatItem(
                            label = "Total Pengguna",
                            value = "${cashiers.size}",
                            color = RetailNavy
                        )
                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE0E0E0))
                        StatItem(
                            label = "Kasir Aktif",
                            value = "${cashiers.count { it.statusAktif }}",
                            color = RetailGreen
                        )
                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE0E0E0))
                        StatItem(
                            label = "Admin / SPV",
                            value = "${cashiers.count { it.isAdmin() || it.isSupervisor() }}",
                            color = Color(0xFF5E35B1)
                        )
                        VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE0E0E0))
                        StatItem(
                            label = "Kasir POS",
                            value = "${cashiers.count { it.role.equals("KASIR", ignoreCase = true) }}",
                            color = RetailRedPrimary
                        )
                    }
                }
            }

            // Search and Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_cashier"),
                        placeholder = { Text("Cari NIK atau Nama Petugas...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RetailNavy) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = RetailNavy,
                            unfocusedBorderColor = Color(0xFFBDBDBD)
                        )
                    )

                    // Role filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filterOptions = listOf("SEMUA", "KASIR", "SUPERVISOR", "ADMIN", "OWNER", "AKTIF", "NONAKTIF")
                        items(filterOptions) { filter ->
                            val isSelected = selectedRoleFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedRoleFilter = filter },
                                label = {
                                    Text(
                                        filter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = RetailNavy,
                                    selectedLabelColor = Color.White,
                                    containerColor = Color.White,
                                    labelColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }
            }

            // Cashiers list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DAFTAR PENGGUNA & HAK AKSES (${filteredCashiers.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        "Klik 'Edit Hak Akses' untuk mengubah izin",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Cashier Items
            if (filteredCashiers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Tidak ada petugas yang cocok", color = Color.DarkGray, fontWeight = FontWeight.Medium)
                            Text("Coba ubah kata kunci pencarian atau filter role.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(filteredCashiers, key = { it.nik }) { cashier ->
                    CashierListItemCard(
                        cashier = cashier,
                        isCurrentLoggedIn = currentCashier?.nik == cashier.nik,
                        onEdit = { editingCashier = cashier },
                        onToggleStatus = {
                            val updated = cashier.copy(statusAktif = !cashier.statusAktif)
                            viewModel.updateCashierUser(updated) {
                                feedbackMessage = "Status ${cashier.nama} diubah menjadi ${if (updated.statusAktif) "AKTIF" else "NONAKTIF"}"
                            }
                        },
                        onDelete = { cashierToDelete = cashier }
                    )
                }
            }
        }
    }

    // Modal: Edit or Create Cashier & Permissions
    if (isCreatingNew) {
        CashierEditAccessDialog(
            cashier = null,
            onDismiss = { isCreatingNew = false },
            onSave = { newCashier ->
                viewModel.saveCashierUser(newCashier) {
                    feedbackMessage = "Akun kasir ${newCashier.nama} berhasil ditambahkan!"
                }
                isCreatingNew = false
            }
        )
    }

    editingCashier?.let { cashier ->
        CashierEditAccessDialog(
            cashier = cashier,
            onDismiss = { editingCashier = null },
            onSave = { updatedCashier ->
                viewModel.updateCashierUser(updatedCashier) {
                    feedbackMessage = "Hak akses & profil ${updatedCashier.nama} berhasil diperbarui!"
                }
                editingCashier = null
            }
        )
    }

    // Modal: Delete Confirmation
    cashierToDelete?.let { cashier ->
        AlertDialog(
            onDismissRequest = { cashierToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = RetailRedPrimary) },
            title = { Text("Hapus Pengguna Kasir?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus akun ${cashier.nama} (NIK: ${cashier.nik})? Data profil dan hak akses akan dihapus dari sistem."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCashierUser(cashier.nik) {
                            feedbackMessage = "Akun ${cashier.nama} berhasil dihapus."
                        }
                        cashierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary)
                ) {
                    Text("HAPUS SEKARANG", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cashierToDelete = null }) {
                    Text("BATAL")
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
    }
}

@Composable
private fun CashierListItemCard(
    cashier: CashierUserEntity,
    isCurrentLoggedIn: Boolean,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    val roleColor = when (cashier.role.uppercase()) {
        "OWNER" -> Color(0xFFF57F17) // Amber/Gold
        "ADMIN" -> RetailNavy
        "SUPERVISOR" -> Color(0xFF6A1B9A) // Purple
        else -> Color(0xFF00695C) // Kasir Teal
    }

    val activeCount = cashier.activePermissionsCount()
    val totalCount = 23
    val progress = activeCount.toFloat() / totalCount.toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_cashier_${cashier.nik}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Name, NIK, Status & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(roleColor.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cashier.nama.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = roleColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            cashier.nama,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.Black
                        )
                        if (isCurrentLoggedIn) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = RetailGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "SEDANG LOGIN",
                                    color = RetailGreen,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text("NIK: ${cashier.nik}", fontSize = 11.sp, color = Color.Gray)
                        Text("•", fontSize = 11.sp, color = Color.Gray)
                        Text("PIN: ••••", fontSize = 11.sp, color = Color.Gray)

                        Surface(
                            color = roleColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                cashier.role.uppercase(),
                                color = roleColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = if (cashier.statusAktif) RetailGreen.copy(alpha = 0.12f) else Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (cashier.statusAktif) "AKTIF" else "NONAKTIF",
                                color = if (cashier.statusAktif) RetailGreen else RetailRedPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Switch status toggle
                if (cashier.nik != "11111111") {
                    Switch(
                        checked = cashier.statusAktif,
                        onCheckedChange = { onToggleStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RetailGreen,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("switch_status_${cashier.nik}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Hak Akses Progress & Capability Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Hak Akses: $activeCount dari $totalCount Izin Aktif",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (activeCount == totalCount) RetailGreen else Color.DarkGray
                )
                Text(
                    if (cashier.isOwner()) "Akses Penuh (Owner)" else "${(progress * 100).toInt()}% Wewenang",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = when {
                    activeCount >= 20 -> RetailGreen
                    activeCount >= 10 -> RetailNavy
                    else -> RetailAmber
                },
                trackColor = Color(0xFFECEFF1)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Permission badges preview
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cashier.canTransaksiPos) item { PermissionTag("POS Kasir", RetailRedPrimary) }
                if (cashier.canVoidItem) item { PermissionTag("Void Item", RetailRedDark) }
                if (cashier.canCancelTransaksi) item { PermissionTag("Cancel Bon", Color(0xFFC2185B)) }
                if (cashier.canReturKonsumen) item { PermissionTag("Retur Konsumen", Color(0xFF7B1FA2)) }
                if (cashier.canManualDiscount) item { PermissionTag("Diskon Manual", RetailAmber) }
                if (cashier.canBukaLaciUang) item { PermissionTag("Buka Drawer", Color(0xFF5D4037)) }
                if (cashier.canClerekKasir) item { PermissionTag("Clerek Setoran", RetailNavy) }
                if (cashier.canAccessSis) item { PermissionTag("Akses SIS", Color(0xFF00796B)) }
                if (cashier.canManageProduk) item { PermissionTag("Katalog Produk", Color(0xFF0288D1)) }
                if (cashier.canEodTutupToko) item { PermissionTag("EOD & Backup", Color(0xFF37474F)) }
                if (cashier.canViewLaporan) item { PermissionTag("Laporan Toko", RetailGreen) }
                if (cashier.canManageKaryawan) item { PermissionTag("Kelola Kasir", Color(0xFF3949AB)) }
                if (cashier.canManageVoucher) item { PermissionTag("Voucher & Poin", RetailAmber) }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delete button
                if (cashier.nik != "11111111") {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RetailRedPrimary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_delete_cashier_${cashier.nik}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hapus", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "Akun Utama (Permanen)",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Edit Button (Primary)
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RetailNavy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_edit_cashier_${cashier.nik}")
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("EDIT HAK AKSES & PROFIL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PermissionTag(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

/**
 * Dialog Komprehensif untuk Menambah / Mengedit Kasir Beserta Seluruh Hak Akses (23 Hak Akses)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierEditAccessDialog(
    cashier: CashierUserEntity?,
    onDismiss: () -> Unit,
    onSave: (CashierUserEntity) -> Unit
) {
    val isEdit = cashier != null

    var nik by remember { mutableStateOf(cashier?.nik ?: "") }
    var nama by remember { mutableStateOf(cashier?.nama ?: "") }
    var pin by remember { mutableStateOf(cashier?.pin ?: "1234") }
    var isPinVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf(cashier?.role ?: "KASIR") }
    var statusAktif by remember { mutableStateOf(cashier?.statusAktif ?: true) }

    // --- 1. HAK AKSES KASIR (POS) ---
    var canTransaksiPos by remember { mutableStateOf(cashier?.canTransaksiPos ?: true) }
    var canVoidItem by remember { mutableStateOf(cashier?.canVoidItem ?: false) }
    var canCancelTransaksi by remember { mutableStateOf(cashier?.canCancelTransaksi ?: true) }
    var canReturKonsumen by remember { mutableStateOf(cashier?.canReturKonsumen ?: false) }
    var canPendingRecall by remember { mutableStateOf(cashier?.canPendingRecall ?: true) }
    var canManualDiscount by remember { mutableStateOf(cashier?.canManualDiscount ?: false) }
    var canBukaLaciUang by remember { mutableStateOf(cashier?.canBukaLaciUang ?: true) }

    // --- 2. HAK AKSES SHIFT & CLEREK ---
    var canBukaTutupShift by remember { mutableStateOf(cashier?.canBukaTutupShift ?: true) }
    var canClerekKasir by remember { mutableStateOf(cashier?.canClerekKasir ?: true) }
    var canViewOmsetShift by remember { mutableStateOf(cashier?.canViewOmsetShift ?: false) }

    // --- 3. HAK AKSES SIS & MANAJEMEN ---
    var canAccessSis by remember { mutableStateOf(cashier?.canAccessSis ?: false) }
    var canManageProduk by remember { mutableStateOf(cashier?.canManageProduk ?: false) }
    var canManagePembelian by remember { mutableStateOf(cashier?.canManagePembelian ?: false) }
    var canCetakLabelRak by remember { mutableStateOf(cashier?.canCetakLabelRak ?: false) }
    var canStockOpname by remember { mutableStateOf(cashier?.canStockOpname ?: false) }
    var canReturPembelian by remember { mutableStateOf(cashier?.canReturPembelian ?: false) }
    var canEodTutupToko by remember { mutableStateOf(cashier?.canEodTutupToko ?: false) }
    var canViewLaporan by remember { mutableStateOf(cashier?.canViewLaporan ?: false) }
    var canManageVoucher by remember { mutableStateOf(cashier?.canManageVoucher ?: false) }
    var canManageKaryawan by remember { mutableStateOf(cashier?.canManageKaryawan ?: false) }
    var canManageMember by remember { mutableStateOf(cashier?.canManageMember ?: false) }
    var canManageSettings by remember { mutableStateOf(cashier?.canManageSettings ?: false) }
    var canBackupRestore by remember { mutableStateOf(cashier?.canBackupRestore ?: false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fungsi menerapkan template role
    fun applyRoleTemplate(selectedRole: String) {
        val template = CashierUserEntity.createWithRole(
            nik = nik.ifBlank { "TMP" },
            nama = nama.ifBlank { "TMP" },
            pin = pin.ifBlank { "1234" },
            role = selectedRole,
            statusAktif = statusAktif
        )
        canTransaksiPos = template.canTransaksiPos
        canVoidItem = template.canVoidItem
        canCancelTransaksi = template.canCancelTransaksi
        canReturKonsumen = template.canReturKonsumen
        canPendingRecall = template.canPendingRecall
        canManualDiscount = template.canManualDiscount
        canBukaLaciUang = template.canBukaLaciUang

        canBukaTutupShift = template.canBukaTutupShift
        canClerekKasir = template.canClerekKasir
        canViewOmsetShift = template.canViewOmsetShift

        canAccessSis = template.canAccessSis
        canManageProduk = template.canManageProduk
        canManagePembelian = template.canManagePembelian
        canCetakLabelRak = template.canCetakLabelRak
        canStockOpname = template.canStockOpname
        canReturPembelian = template.canReturPembelian
        canEodTutupToko = template.canEodTutupToko
        canViewLaporan = template.canViewLaporan
        canManageVoucher = template.canManageVoucher
        canManageKaryawan = template.canManageKaryawan
        canManageMember = template.canManageMember
        canManageSettings = template.canManageSettings
        canBackupRestore = template.canBackupRestore
    }

    // Fungsi pilih semua atau kosongkan semua
    fun toggleAll(enable: Boolean) {
        canTransaksiPos = enable
        canVoidItem = enable
        canCancelTransaksi = enable
        canReturKonsumen = enable
        canPendingRecall = enable
        canManualDiscount = enable
        canBukaLaciUang = enable

        canBukaTutupShift = enable
        canClerekKasir = enable
        canViewOmsetShift = enable

        canAccessSis = enable
        canManageProduk = enable
        canManagePembelian = enable
        canCetakLabelRak = enable
        canStockOpname = enable
        canReturPembelian = enable
        canEodTutupToko = enable
        canViewLaporan = enable
        canManageVoucher = enable
        canManageKaryawan = enable
        canManageMember = enable
        canManageSettings = enable
        canBackupRestore = enable
    }

    val activeCount = listOf(
        canTransaksiPos, canVoidItem, canCancelTransaksi, canReturKonsumen, canPendingRecall,
        canManualDiscount, canBukaLaciUang, canBukaTutupShift, canClerekKasir, canViewOmsetShift,
        canAccessSis, canManageProduk, canManagePembelian, canCetakLabelRak, canStockOpname,
        canReturPembelian, canEodTutupToko, canViewLaporan, canManageVoucher, canManageKaryawan,
        canManageMember, canManageSettings, canBackupRestore
    ).count { it }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Header
                Surface(
                    color = RetailNavy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                if (isEdit) "Edit Kasir & Hak Akses: $nama" else "Tambah Kasir & Hak Akses Baru",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                "Total Hak Akses Aktif: $activeCount dari 23 Izin",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    errorMessage?.let { err ->
                        Surface(
                            color = Color(0xFFFFEBEE),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RetailRedPrimary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = RetailRedPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, color = RetailRedPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // --- SECTION 1: PROFIL & AKUN ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1. Data Profil & Akun Pengguna", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = nik,
                                    onValueChange = { nik = it },
                                    label = { Text("NIK Kasir", color = Color.DarkGray) },
                                    enabled = !isEdit, // NIK cannot be changed if editing existing
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        disabledTextColor = Color.DarkGray,
                                        focusedBorderColor = RetailNavy,
                                        unfocusedBorderColor = Color(0xFFBDBDBD)
                                    )
                                )

                                OutlinedTextField(
                                    value = nama,
                                    onValueChange = { nama = it },
                                    label = { Text("Nama Lengkap", color = Color.DarkGray) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.5f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = RetailNavy,
                                        unfocusedBorderColor = Color(0xFFBDBDBD)
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pin = it },
                                    label = { Text("PIN Akses (4-6 Digit)", color = Color.DarkGray) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    visualTransformation = if (isPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPinVisible = !isPinVisible }) {
                                            Icon(
                                                if (isPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle PIN",
                                                tint = Color.Gray
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedBorderColor = RetailNavy,
                                        unfocusedBorderColor = Color(0xFFBDBDBD)
                                    )
                                )

                                // Status toggle
                                Surface(
                                    color = Color(0xFFF5F7FA),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Status Akun", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                if (statusAktif) "AKTIF (Bisa Login)" else "NONAKTIF",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (statusAktif) RetailGreen else RetailRedPrimary
                                            )
                                        }
                                        Switch(
                                            checked = statusAktif,
                                            onCheckedChange = { statusAktif = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = RetailGreen
                                            )
                                        )
                                    }
                                }
                            }

                            // Role selection chips
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Pilih Role Utama:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val roles = listOf("KASIR", "SUPERVISOR", "ADMIN", "OWNER")
                                    roles.forEach { r ->
                                        val isSelected = role.equals(r, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                role = r
                                                applyRoleTemplate(r)
                                            },
                                            label = { Text(r, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = RetailNavy,
                                                selectedLabelColor = Color.White,
                                                containerColor = Color(0xFFF1F3F5),
                                                labelColor = Color.DarkGray
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- SECTION 2: HAK AKSES LENGKAP ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header & Quick Preset Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = RetailNavy, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("2. Detail Hak Akses Pengguna ($activeCount/23)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = RetailNavy)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { toggleAll(true) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("PILIH SEMUA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { toggleAll(false) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("KOSONGKAN", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { applyRoleTemplate(role) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8EAF6), contentColor = RetailNavy),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("TEMPLATE ROLE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // --- GRUP 1: TRANSAKSI POS TERMINAL (7 Izin) ---
                            PermissionGroupCard(
                                title = "A. Transaksi Kasir (POS Terminal)",
                                subtitle = "Kewenangan petugas saat berada di layar kasir",
                                icon = Icons.Default.PointOfSale,
                                headerColor = RetailRedPrimary
                            ) {
                                PermissionToggleRow(
                                    title = "Transaksi Kasir (POS)",
                                    description = "Dapat membuka scanner, memasukkan keranjang & memproses checkout",
                                    checked = canTransaksiPos,
                                    onCheckedChange = { canTransaksiPos = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Void Item Belanja",
                                    description = "Dapat menghapus item produk yang sudah discan di keranjang",
                                    checked = canVoidItem,
                                    onCheckedChange = { canVoidItem = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Batalkan / Clear Transaksi",
                                    description = "Dapat membatalkan dan mengosongkan seluruh keranjang transaksi",
                                    checked = canCancelTransaksi,
                                    onCheckedChange = { canCancelTransaksi = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Retur Barang Konsumen",
                                    description = "Dapat melayani pengembalian / nota retur belanja pembeli",
                                    checked = canReturKonsumen,
                                    onCheckedChange = { canReturKonsumen = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Pending & Recall Struk",
                                    description = "Dapat menahan antrian belanja dan memanggil kembali transaksi pending",
                                    checked = canPendingRecall,
                                    onCheckedChange = { canPendingRecall = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Diskon Manual / Potongan",
                                    description = "Dapat memberikan potongan harga manual pada transaksi belanja",
                                    checked = canManualDiscount,
                                    onCheckedChange = { canManualDiscount = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Buka Laci Uang (Drawer)",
                                    description = "Dapat membuka laci uang kasir secara manual tanpa transaksi",
                                    checked = canBukaLaciUang,
                                    onCheckedChange = { canBukaLaciUang = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // --- GRUP 2: SHIFT & SETORAN (3 Izin) ---
                            PermissionGroupCard(
                                title = "B. Shift & Rekonsiliasi Kasir",
                                subtitle = "Kewenangan buka modal awal dan tutup shift kerja",
                                icon = Icons.Default.Schedule,
                                headerColor = Color(0xFF6D4C41)
                            ) {
                                PermissionToggleRow(
                                    title = "Buka / Tutup Shift",
                                    description = "Dapat menginput modal awal kasir dan memulai sesi shift",
                                    checked = canBukaTutupShift,
                                    onCheckedChange = { canBukaTutupShift = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Clerek Kasir (Setoran)",
                                    description = "Dapat melakukan rekap hitung uang fisik dan mencetak struk clerek",
                                    checked = canClerekKasir,
                                    onCheckedChange = { canClerekKasir = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Lihat Omset Saat Shift",
                                    description = "Dapat melihat total omset uang penjualan saat shift sedang berjalan",
                                    checked = canViewOmsetShift,
                                    onCheckedChange = { canViewOmsetShift = it }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // --- GRUP 3: SISTEM INFORMASI SWALAYAN (SIS) & BACK OFFICE (13 Izin) ---
                            PermissionGroupCard(
                                title = "C. SIS & Manajemen Toko (Back Office)",
                                subtitle = "Kewenangan mengakses menu pengelolaan dan database toko",
                                icon = Icons.Default.Storefront,
                                headerColor = RetailNavy
                            ) {
                                PermissionToggleRow(
                                    title = "Akses Menu Back Office (SIS)",
                                    description = "Dapat masuk ke layar dashboard menu SIS dan pengaturan",
                                    checked = canAccessSis,
                                    onCheckedChange = { canAccessSis = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Kelola Katalog & Harga Produk",
                                    description = "Tambah master barang, ubah harga jual, barcode, & minimal stok",
                                    checked = canManageProduk,
                                    onCheckedChange = { canManageProduk = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Faktur Pembelian & Supplier",
                                    description = "Input penerimaan barang supplier (Goods Receipt) & purchase order",
                                    checked = canManagePembelian,
                                    onCheckedChange = { canManagePembelian = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Cetak Label Rak & Barcode",
                                    description = "Mencetak price tag planogram rak minimarket",
                                    checked = canCetakLabelRak,
                                    onCheckedChange = { canCetakLabelRak = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Stok Opname Toko",
                                    description = "Menginput hasil perhitungan fisik stok dan penyesuaian selisih",
                                    checked = canStockOpname,
                                    onCheckedChange = { canStockOpname = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Retur Pembelian ke Supplier",
                                    description = "Membuat nota retur barang (NRB) rusak/kadaluarsa ke distributor",
                                    checked = canReturPembelian,
                                    onCheckedChange = { canReturPembelian = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Proses EOD & Backup Toko",
                                    description = "Menjalankan End of Day harian dan backup otomatis ke folder Android",
                                    checked = canEodTutupToko,
                                    onCheckedChange = { canEodTutupToko = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Laporan Penjualan & Margin",
                                    description = "Melihat histori transaksi, omset harian, laba kotor, dan rekap bon",
                                    checked = canViewLaporan,
                                    onCheckedChange = { canViewLaporan = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Pengaturan Voucher & Poin",
                                    description = "Membuat promo voucher belanja unik acak dan rasio poin loyalitas",
                                    checked = canManageVoucher,
                                    onCheckedChange = { canManageVoucher = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Kelola Kasir & Hak Akses",
                                    description = "Menambah akun petugas dan mengatur kewenangan akses pengguna",
                                    checked = canManageKaryawan,
                                    onCheckedChange = { canManageKaryawan = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Kelola Data Member Pelanggan",
                                    description = "Mendaftarkan kartu member, nomor HP, dan poin loyalty pembeli",
                                    checked = canManageMember,
                                    onCheckedChange = { canManageMember = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Pengaturan Toko & Printer",
                                    description = "Mengubah nama toko, footer struk, dan koneksi printer bluetooth",
                                    checked = canManageSettings,
                                    onCheckedChange = { canManageSettings = it }
                                )
                                HorizontalDivider(color = Color(0xFFF0F0F0))
                                PermissionToggleRow(
                                    title = "Cadangkan & Pulihkan Database",
                                    description = "Ekspor dan impor cadangan file database offline",
                                    checked = canBackupRestore,
                                    onCheckedChange = { canBackupRestore = it }
                                )
                            }
                        }
                    }
                }

                // Dialog Footer with Actions
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("BATAL", color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                if (nik.isBlank()) {
                                    errorMessage = "NIK Kasir wajib diisi!"
                                    return@Button
                                }
                                if (nama.isBlank()) {
                                    errorMessage = "Nama Kasir wajib diisi!"
                                    return@Button
                                }
                                if (pin.isBlank() || pin.length < 4) {
                                    errorMessage = "PIN Kasir minimal 4 angka!"
                                    return@Button
                                }

                                val finalUser = CashierUserEntity(
                                    nik = nik.trim(),
                                    nama = nama.trim(),
                                    pin = pin.trim(),
                                    role = role.trim().uppercase(),
                                    statusAktif = statusAktif,
                                    createdAt = cashier?.createdAt ?: System.currentTimeMillis(),

                                    canTransaksiPos = canTransaksiPos,
                                    canVoidItem = canVoidItem,
                                    canCancelTransaksi = canCancelTransaksi,
                                    canReturKonsumen = canReturKonsumen,
                                    canPendingRecall = canPendingRecall,
                                    canManualDiscount = canManualDiscount,
                                    canBukaLaciUang = canBukaLaciUang,

                                    canBukaTutupShift = canBukaTutupShift,
                                    canClerekKasir = canClerekKasir,
                                    canViewOmsetShift = canViewOmsetShift,

                                    canAccessSis = canAccessSis,
                                    canManageProduk = canManageProduk,
                                    canManagePembelian = canManagePembelian,
                                    canCetakLabelRak = canCetakLabelRak,
                                    canStockOpname = canStockOpname,
                                    canReturPembelian = canReturPembelian,
                                    canEodTutupToko = canEodTutupToko,
                                    canViewLaporan = canViewLaporan,
                                    canManageVoucher = canManageVoucher,
                                    canManageKaryawan = canManageKaryawan,
                                    canManageMember = canManageMember,
                                    canManageSettings = canManageSettings,
                                    canBackupRestore = canBackupRestore
                                )

                                onSave(finalUser)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_save_cashier_access")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isEdit) "SIMPAN PERUBAHAN HAK AKSES" else "TAMBAH PENGGUNA",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionGroupCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    headerColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = Color(0xFFFAFAFA),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Surface(
                    color = headerColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = headerColor, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                    Text(subtitle, fontSize = 10.sp, color = Color.Gray)
                }
            }

            HorizontalDivider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(bottom = 6.dp))

            content()
        }
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = if (checked) Color.Black else Color(0xFF616161)
            )
            Text(
                description,
                fontSize = 10.sp,
                color = Color.Gray,
                lineHeight = 13.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = RetailGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}
