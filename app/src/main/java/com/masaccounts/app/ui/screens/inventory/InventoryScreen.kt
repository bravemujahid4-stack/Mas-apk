package com.masaccounts.app.ui.screens.inventory

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.data.local.entity.CategoryEntity
import com.masaccounts.app.data.local.entity.ItemEntity
import com.masaccounts.app.data.local.entity.UnitEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MasViewModel
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val items by viewModel.items.collectAsState()
    val inventoryLots by viewModel.inventoryLots.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val units by viewModel.units.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Items", "FIFO Lots", "Movements")

    var searchQuery by remember { mutableStateOf("") }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showCategoryUnitDialog by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    val filteredItems = remember(items, searchQuery) {
        if (searchQuery.isBlank()) items
        else items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Inventory & Stock",
                company = company,
                user = currentUser,
                actions = {
                    IconButton(onClick = { showCategoryUnitDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Units & Categories", tint = Color.White)
                    }
                    if (canAdd) {
                        IconButton(onClick = { showAddItemDialog = true }, modifier = Modifier.testTag("add_item_top_button")) {
                            Icon(Icons.Default.AddBox, contentDescription = "Add Item", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd && selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddItemDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Items List Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            color = Color.White,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Search iron/steel items, category...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                val totalValuation = items.sumOf { it.currentStock * if (it.purchasePrice > 0) it.purchasePrice else it.openingCost }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${items.size} Items", style = MaterialTheme.typography.bodySmall.copy(color = Slate600))
                                    Text(
                                        "Total Stock Value: ${formatCurrency(totalValuation, currency)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PurplePrimary)
                                    )
                                }
                            }
                        }

                        if (filteredItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateCard(
                                    title = if (searchQuery.isBlank()) "No Items Added" else "No Matching Items",
                                    subtitle = if (searchQuery.isBlank()) "Add your items (e.g. Deformed Bar, G.I Sheet, Angle Iron) to begin inventory tracking." else "Try searching with a different keyword.",
                                    icon = Icons.Default.Inventory2,
                                    actionButtonText = if (canAdd && searchQuery.isBlank()) "+ Add Item" else null,
                                    onActionClick = { showAddItemDialog = true }
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(filteredItems) { item ->
                                    ItemCardView(item = item, currency = currency)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // FIFO Lots Tab
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "FIFO Inventory Lots (Remaining Batches)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyDark),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        if (inventoryLots.filter { it.remainingQuantity > 0 }.isEmpty()) {
                            EmptyStateCard(
                                title = "No Active Lots",
                                subtitle = "FIFO batches are automatically created when purchases or opening stock are recorded.",
                                icon = Icons.Default.Layers
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(inventoryLots.filter { it.remainingQuantity > 0 }) { lot ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(lot.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Lot: ${lot.lotNumber} • ${lot.referenceType}", fontSize = 11.sp, color = Slate600)
                                                Text("Purchased: ${formatDate(lot.purchaseDate)}", fontSize = 10.sp, color = Slate400)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    "${lot.remainingQuantity} / ${lot.initialQuantity} Qty",
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccountingGreen,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "Cost: ${formatCurrency(lot.unitCost, currency)}",
                                                    fontSize = 11.sp,
                                                    color = Slate600
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Movements Tab
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = "Stock Movement History",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyDark),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        if (stockMovements.isEmpty()) {
                            EmptyStateCard(
                                title = "No Movements",
                                subtitle = "Stock movements will be tracked automatically as sales and purchases occur.",
                                icon = Icons.Default.History
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stockMovements) { mov ->
                                    val isIncoming = mov.movementType == "IN" || mov.movementType == "RETURN_IN" || mov.movementType == "OPENING"
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = BorderStroke(1.dp, Slate200),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(mov.itemName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("${mov.movementType} • ${mov.referenceNumber}", fontSize = 11.sp, color = Slate600)
                                                Text(formatDate(mov.date), fontSize = 10.sp, color = Slate400)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = (if (isIncoming) "+" else "-") + "${mov.quantity}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isIncoming) AccountingGreen else AccountingRed,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = formatCurrency(mov.totalCost, currency),
                                                    fontSize = 11.sp,
                                                    color = Slate600
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            companyId = company?.id ?: 1L,
            categories = categories,
            units = units,
            onDismiss = { showAddItemDialog = false },
            onSave = { newItem ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createItem(newItem, user)
                    viewModel.showMessage("Item '${newItem.name}' added with FIFO lot tracking.")
                    showAddItemDialog = false
                }
            }
        )
    }

    if (showCategoryUnitDialog) {
        CategoryUnitDialog(
            companyId = company?.id ?: 1L,
            categories = categories,
            units = units,
            onDismiss = { showCategoryUnitDialog = false },
            onAddCategory = { name ->
                coroutineScope.launch {
                    viewModel.repository.addCategory(CategoryEntity(companyId = company?.id ?: 1L, name = name))
                }
            },
            onAddUnit = { code, name ->
                coroutineScope.launch {
                    viewModel.repository.addUnit(UnitEntity(companyId = company?.id ?: 1L, code = code, name = name))
                }
            }
        )
    }
}

@Composable
fun ItemCardView(
    item: ItemEntity,
    currency: String
) {
    val isLowStock = item.minStock > 0 && item.currentStock <= item.minStock

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (isLowStock) AccountingRedLight else Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyDark)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate100
                    ) {
                        Text(
                            text = item.category,
                            fontSize = 10.sp,
                            color = Slate600,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "${item.currentStock} ${item.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (item.currentStock <= 0) AccountingRed else NavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Code: ${item.code.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )
                Text(
                    text = "Purchase: ${formatCurrency(item.purchasePrice, currency)} | Sale: ${formatCurrency(item.salePrice, currency)}",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                )
            }

            if (isLowStock) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccountingRedLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccountingRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Low Stock Alert (Min: ${item.minStock} ${item.unit})", fontSize = 11.sp, color = AccountingRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddItemDialog(
    companyId: Long,
    categories: List<CategoryEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit
) {
    var code by remember { mutableStateOf("ITEM-${System.currentTimeMillis().toString().takeLast(4)}") }
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(if (categories.isNotEmpty()) categories.first().name else "Iron") }
    var selectedUnit by remember { mutableStateOf(if (units.isNotEmpty()) units.first().code else "KG") }
    var openingQtyStr by remember { mutableStateOf("") }
    var openingCostStr by remember { mutableStateOf("") }
    var purchasePriceStr by remember { mutableStateOf("") }
    var salePriceStr by remember { mutableStateOf("") }
    var minStockStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item / Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Item Name * (e.g. Deformed Bar 60 Grade)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Item Code") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = { selectedCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = { selectedUnit = it },
                        label = { Text("Unit (e.g. KG, Ton)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Min Stock Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("Purchase Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { salePriceStr = it },
                        label = { Text("Sale Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Divider(color = Slate200)
                Text("Opening Inventory (FIFO Batch):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openingQtyStr,
                        onValueChange = { openingQtyStr = it },
                        label = { Text("Opening Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = openingCostStr,
                        onValueChange = { openingCostStr = it },
                        label = { Text("Cost Per Unit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Specs") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Item Name is required."
                        return@Button
                    }
                    val openingQty = openingQtyStr.toDoubleOrNull() ?: 0.0
                    val openingCost = openingCostStr.toDoubleOrNull() ?: 0.0
                    val purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val salePrice = salePriceStr.toDoubleOrNull() ?: 0.0
                    val minStock = minStockStr.toDoubleOrNull() ?: 0.0

                    onSave(
                        ItemEntity(
                            companyId = companyId,
                            code = code.trim(),
                            name = name.trim(),
                            category = selectedCategory.trim().ifBlank { "Iron" },
                            unit = selectedUnit.trim().ifBlank { "KG" },
                            openingQuantity = openingQty,
                            openingCost = openingCost,
                            purchasePrice = purchasePrice,
                            salePrice = salePrice,
                            minStock = minStock,
                            description = description.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CategoryUnitDialog(
    companyId: Long,
    categories: List<CategoryEntity>,
    units: List<UnitEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onAddUnit: (String, String) -> Unit
) {
    var newCat by remember { mutableStateOf("") }
    var newUnitCode by remember { mutableStateOf("") }
    var newUnitName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categories & Units", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Categories:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCat,
                        onValueChange = { newCat = it },
                        placeholder = { Text("New category name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newCat.isNotBlank()) {
                                onAddCategory(newCat.trim())
                                newCat = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                Text("Existing: " + categories.joinToString(", ") { it.name }, fontSize = 11.sp, color = Slate600)

                Divider(color = Slate200)

                Text("Units of Measure:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newUnitCode,
                        onValueChange = { newUnitCode = it },
                        placeholder = { Text("Code (e.g. KG)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = newUnitName,
                        onValueChange = { newUnitName = it },
                        placeholder = { Text("Name (e.g. Kilogram)") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    Button(
                        onClick = {
                            if (newUnitCode.isNotBlank() && newUnitName.isNotBlank()) {
                                onAddUnit(newUnitCode.trim(), newUnitName.trim())
                                newUnitCode = ""
                                newUnitName = ""
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }
                Text("Existing: " + units.joinToString(", ") { "${it.code} (${it.name})" }, fontSize = 11.sp, color = Slate600)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
