package com.example.ui.screens.sis

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.MemberEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler { onNavigateBack() }

    val members by viewModel.members.collectAsState()
    val storeConfig by viewModel.storeConfig.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddCard by remember { mutableStateOf(false) }

    // State for Add Member
    var newMemberNo by remember { mutableStateOf("MBR-" + System.currentTimeMillis().toString().takeLast(5)) }
    var newNama by remember { mutableStateOf("") }
    var newNoHp by remember { mutableStateOf("") }
    var newAlamat by remember { mutableStateOf("") }

    // State for Edit Member Dialog
    var editingMember by remember { mutableStateOf<MemberEntity?>(null) }

    // State for Delete Member Dialog
    var memberToDelete by remember { mutableStateOf<MemberEntity?>(null) }

    // Feedback message
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) {
            members
        } else {
            val q = searchQuery.trim().lowercase()
            members.filter { m ->
                m.nama.lowercase().contains(q) ||
                        m.memberNo.lowercase().contains(q) ||
                        m.noHp.lowercase().contains(q) ||
                        m.alamat.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Column {
                        Text("Master Data Member Pelanggan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${members.size} member terdaftar", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showAddCard = !showAddCard
                            if (showAddCard) {
                                newMemberNo = "MBR-" + System.currentTimeMillis().toString().takeLast(5)
                            }
                        },
                        modifier = Modifier.testTag("toggle_add_member_button")
                    ) {
                        Icon(
                            if (showAddCard) Icons.Default.Close else Icons.Default.PersonAdd,
                            contentDescription = "Tambah Member",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(PosSurface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Feedback Banner
                if (feedbackMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RetailGreen.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RetailGreen)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feedbackMessage.orEmpty(),
                                        color = RetailNavy,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = { feedbackMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", tint = GrayText)
                                }
                            }
                        }
                    }
                }

                // Summary Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Member", fontSize = 11.sp, color = GrayText)
                                Text("${members.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RetailNavy)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Member Aktif", fontSize = 11.sp, color = GrayText)
                                Text("${members.count { it.statusAktif }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RetailGreen)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Poin", fontSize = 11.sp, color = GrayText)
                                Text("${members.sumOf { it.poin }}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_member_input"),
                        placeholder = { Text("Cari berdasarkan nama, kartu, nomor HP, atau alamat...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Cari") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus Pencarian")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                }

                // Add Member Form (Visible when toggled)
                if (showAddCard) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_member_card"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = RetailNavy)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Pendaftaran Member Baru", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RetailNavy)
                                    }
                                    IconButton(onClick = { showAddCard = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = newMemberNo,
                                        onValueChange = { newMemberNo = it },
                                        label = { Text("No. Kartu / Barcode") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("input_new_member_no"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = newNama,
                                        onValueChange = { newNama = it },
                                        label = { Text("Nama Member *") },
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .testTag("input_new_member_nama"),
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = newNoHp,
                                    onValueChange = { newNoHp = it },
                                    label = { Text("Nomor WhatsApp / HP") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_member_phone"),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = newAlamat,
                                    onValueChange = { newAlamat = it },
                                    label = { Text("Alamat Tempat Tinggal") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_new_member_address"),
                                    maxLines = 2
                                )

                                val bonusPoin = if (storeConfig.poinAktif) storeConfig.poinBonusMemberBaru else 0
                                if (bonusPoin > 0) {
                                    Surface(
                                        color = Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "★ Member baru otomatis mendapatkan bonus $bonusPoin poin!",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (newNama.isNotBlank() && newMemberNo.isNotBlank()) {
                                            val memberToSave = MemberEntity(
                                                memberNo = newMemberNo.trim(),
                                                nama = newNama.trim(),
                                                noHp = newNoHp.trim(),
                                                alamat = newAlamat.trim(),
                                                poin = bonusPoin,
                                                statusAktif = true
                                            )
                                            viewModel.saveMember(memberToSave)
                                            feedbackMessage = "Member '${newNama.trim()}' berhasil didaftarkan!"
                                            newNama = ""
                                            newNoHp = ""
                                            newAlamat = ""
                                            newMemberNo = "MBR-" + System.currentTimeMillis().toString().takeLast(5)
                                            showAddCard = false
                                        }
                                    },
                                    enabled = newNama.isNotBlank() && newMemberNo.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_add_member_button")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SIMPAN MEMBER BARU", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Member List Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Daftar Member (${filteredMembers.size})" else "Hasil Pencarian (${filteredMembers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = RetailNavy
                        )
                        if (!showAddCard) {
                            FilledTonalButton(
                                onClick = {
                                    showAddCard = true
                                    newMemberNo = "MBR-" + System.currentTimeMillis().toString().takeLast(5)
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_member_quick_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tambah Member", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (filteredMembers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.PeopleOutline,
                                    contentDescription = null,
                                    tint = GrayText,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    if (searchQuery.isBlank()) "Belum ada data member" else "Tidak ada member yang cocok dengan '$searchQuery'",
                                    fontWeight = FontWeight.Medium,
                                    color = GrayText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMembers, key = { it.memberNo }) { m ->
                        MemberItemCard(
                            member = m,
                            onEditClick = { editingMember = m },
                            onDeleteClick = { memberToDelete = m }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG EDIT MEMBER ---
    if (editingMember != null) {
        EditMemberDialog(
            member = editingMember!!,
            onDismiss = { editingMember = null },
            onSave = { updatedMember ->
                viewModel.updateMember(updatedMember) {
                    feedbackMessage = "Data member '${updatedMember.nama}' berhasil diperbarui!"
                }
                editingMember = null
            }
        )
    }

    // --- DIALOG KONFIRMASI HAPUS ---
    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Hapus Member?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Anda yakin ingin menghapus data member '${memberToDelete?.nama}' (No: ${memberToDelete?.memberNo})? Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = memberToDelete
                        if (m != null) {
                            viewModel.deleteMember(m.memberNo)
                            feedbackMessage = "Member '${m.nama}' telah dihapus."
                        }
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary),
                    modifier = Modifier.testTag("confirm_delete_member_button")
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun MemberItemCard(
    member: MemberEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.memberNo}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (member.statusAktif) RetailNavy.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = member.nama.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (member.statusAktif) RetailNavy else Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.nama,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = RetailNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!member.statusAktif) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Non-Aktif",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Kartu: ${member.memberNo}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GrayText
                        )
                    }
                }

                // Edit and Delete Actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_member_button_${member.memberNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Member",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_member_button_${member.memberNo}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Member",
                            tint = RetailRedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Details info: Phone & Address
            if (member.noHp.isNotBlank() || member.alamat.isNotBlank()) {
                if (member.noHp.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = GrayText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(member.noHp, fontSize = 12.sp, color = Color(0xFF334155))
                    }
                }
                if (member.alamat.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GrayText,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            member.alamat,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Badges: Poin & Total Belanja
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${member.poin} Poin",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Text(
                    text = "Total Belanja: ${formatRupiah(member.totalBelanja)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RetailGreen
                )
            }
        }
    }
}

@Composable
fun EditMemberDialog(
    member: MemberEntity,
    onDismiss: () -> Unit,
    onSave: (MemberEntity) -> Unit
) {
    var editNama by remember { mutableStateOf(member.nama) }
    var editNoHp by remember { mutableStateOf(member.noHp) }
    var editAlamat by remember { mutableStateOf(member.alamat) }
    var editPoinStr by remember { mutableStateOf(member.poin.toString()) }
    var editStatusAktif by remember { mutableStateOf(member.statusAktif) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("edit_member_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = Color(0xFF2563EB).copy(alpha = 0.1f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Edit Data Member", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = RetailNavy)
                            Text("No. Kartu: ${member.memberNo}", fontSize = 12.sp, color = GrayText)
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Nama Field
                OutlinedTextField(
                    value = editNama,
                    onValueChange = { editNama = it },
                    label = { Text("Nama Lengkap Member *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_nama_input"),
                    singleLine = true
                )

                // No HP Field
                OutlinedTextField(
                    value = editNoHp,
                    onValueChange = { editNoHp = it },
                    label = { Text("Nomor WhatsApp / HP") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                // Alamat Field
                OutlinedTextField(
                    value = editAlamat,
                    onValueChange = { editAlamat = it },
                    label = { Text("Alamat") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_address_input"),
                    maxLines = 2
                )

                // Poin Field
                OutlinedTextField(
                    value = editPoinStr,
                    onValueChange = { editPoinStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Jumlah Poin") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_member_points_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("Poin dapat disesuaikan manual oleh manajemen", fontSize = 11.sp) }
                )

                // Status Aktif Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status Member Aktif", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            if (editStatusAktif) "Member dapat bertransaksi & kumpul poin" else "Member dinonaktifkan sementara",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }
                    Switch(
                        checked = editStatusAktif,
                        onCheckedChange = { editStatusAktif = it },
                        modifier = Modifier.testTag("edit_member_status_switch")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_edit_member_button")
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (editNama.isNotBlank()) {
                                val newPoin = editPoinStr.toIntOrNull() ?: member.poin
                                val updated = member.copy(
                                    nama = editNama.trim(),
                                    noHp = editNoHp.trim(),
                                    alamat = editAlamat.trim(),
                                    poin = newPoin,
                                    statusAktif = editStatusAktif
                                )
                                onSave(updated)
                            }
                        },
                        enabled = editNama.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = RetailNavy),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("save_edit_member_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
