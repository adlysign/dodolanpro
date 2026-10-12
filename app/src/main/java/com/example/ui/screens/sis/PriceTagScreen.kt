package com.example.ui.screens.sis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.ui.screens.pos.formatRupiah
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTagScreen(
    viewModel: PosViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.allProducts.collectAsState()
    var selectedType by remember { mutableStateOf(0) } // 0: Harga Besar (Reguler/Promo), 1: Harga Kecil (Bumbu/Obat)
    var selectedFilter by remember { mutableStateOf("Semua") }
    var selectedRak by remember { mutableStateOf("038") }
    var searchQuery by remember { mutableStateOf("") }
    var showPrintSuccessDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, selectedFilter, searchQuery) {
        products.filter { p ->
            (selectedFilter == "Semua" ||
                    (selectedFilter == "Promo" && p.isPromo) ||
                    (selectedFilter == "Sewa Promosi" && p.isSewaPromosi)) &&
                    (p.nama.contains(searchQuery, ignoreCase = true) || p.plu.contains(searchQuery) || p.rakPlanogram.contains(searchQuery))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CETAK LABEL HARGA / PRICE TAG", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RetailNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosSurface)
                .padding(12.dp)
        ) {
            // Type Selector Tab (from PDF page 24: Harga Besar vs Harga Kecil)
            TabRow(selectedTabIndex = selectedType) {
                Tab(
                    selected = selectedType == 0,
                    onClick = { selectedType = 0 },
                    text = { Text("HARGA BESAR (REGULER/PROMO)") }
                )
                Tab(
                    selected = selectedType == 1,
                    onClick = { selectedType = 1 },
                    text = { Text("HARGA KECIL (BUMBU/OBAT)") }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Cari PLU / Nama / Rak...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Button(
                    onClick = { showPrintSuccessDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("CETAK SEMUA", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-filter chips (as in PDF page 26: PLU/Barcode, Kode Rak/Planogram, Sewa Promosi)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Semua", "Promo", "Sewa Promosi").forEach { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedType == 0) {
                // Harga Besar Layout: Grid of authentic Alfamart shelf label price tags (as in PDF page 14 & 32)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts) { prod ->
                        PriceTagLargeCard(product = prod)
                    }
                }
            } else {
                // Harga Kecil Layout: Compact 3-column / 3-row layout for spices/medicine (as in PDF page 34)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = RetailAmberLight)
                        ) {
                            Text(
                                "Format Label Kecil: Kertas stiker 3 kolom x 8 baris (Bumbu, Obat, Pulpen).",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                    items(filteredProducts) { prod ->
                        PriceTagSmallRow(product = prod)
                        HorizontalDivider(color = PosCardBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    if (showPrintSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showPrintSuccessDialog = false },
            title = { Text("SIMULASI CETAK LABEL HARGA", fontWeight = FontWeight.Bold) },
            text = {
                Text("Berhasil mencetak ${filteredProducts.size} label price tag sesuai format printer barcode! Pasang segera di rak/planogram masing-masing.")
            },
            confirmButton = {
                Button(onClick = { showPrintSuccessDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = RetailRedPrimary, contentColor = Color.White)) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Authentic Alfamart Large Shelf Price Tag Card (as shown in PDF page 14 & 32)
@Composable
fun PriceTagLargeCard(product: ProductEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black, RoundedCornerShape(4.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Price Display (Huge bold Rp.)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rp",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = formatRupiah(product.hargaJual).replace("Rp", "").trim(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Product Name
            Text(
                text = product.nama,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                lineHeight = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata: PLU, Barcode, Planogram
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PLU: ${product.plu}", fontSize = 9.sp, color = GrayText)
                Text("Rak: ${product.rakPlanogram.take(8)}", fontSize = 9.sp, color = GrayText)
            }
            Text("BC: ${product.barcode}", fontSize = 9.sp, color = GrayText)

            if (product.isPromo) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = RetailRedPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        "PROMO HEMAT",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// Small Shelf Price Tag Row for Spices/Medicine (as in PDF page 34)
@Composable
fun PriceTagSmallRow(product: ProductEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(product.nama, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("PLU: ${product.plu} | BC: ${product.barcode}", fontSize = 10.sp, color = GrayText)
        }
        Text(
            formatRupiah(product.hargaJual),
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = Color.Black,
            fontFamily = FontFamily.Monospace
        )
    }
}
