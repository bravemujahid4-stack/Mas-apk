package com.masaccounts.app.ui.screens.purchases

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
import com.masaccounts.app.data.local.entity.ItemEntity
import com.masaccounts.app.data.local.entity.PurchaseBillEntity
import com.masaccounts.app.data.local.entity.PurchaseItemEntity
import com.masaccounts.app.data.local.entity.SupplierEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    viewModel: MasViewModel,
    onCreatePurchaseClick: () -> Unit,
    onViewPurchaseDetails: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val purchases by viewModel.purchases.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    val filteredPurchases = remember(purchases, searchQuery, selectedFilter) {
        purchases.filter { bill ->
            val matchesFilter = when (selectedFilter) {
                "CASH" -> bill.paymentType == "CASH"
                "CREDIT" -> bill.paymentType == "CREDIT"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true
            else bill.billNumber.contains(searchQuery, ignoreCase = true) ||
                 bill.supplierName.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Purchase Bills",
                company = company,
                user = currentUser,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = onCreatePurchaseClick, modifier = Modifier.testTag("add_purchase_top_button")) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "New Purchase", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = onCreatePurchaseClick,
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_purchase_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Purchase")
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
            // Search & Filter Bar
            Surface(
                color = Color.White,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search bill #, supplier name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_purchases_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${purchases.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "CASH",
                            onClick = { selectedFilter = "CASH" },
                            label = { Text("Cash (${purchases.count { it.paymentType == "CASH" }})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "CREDIT",
                            onClick = { selectedFilter = "CREDIT" },
                            label = { Text("Credit (${purchases.count { it.paymentType == "CREDIT" }})") }
                        )
                    }
                }
            }

            if (filteredPurchases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "No Purchase Bills",
                        subtitle = "Record stock purchases from mills or suppliers to automatically generate FIFO lots.",
                        icon = Icons.Default.ShoppingBag,
                        actionButtonText = if (canAdd) "+ Record Purchase Bill" else null,
                        onActionClick = onCreatePurchaseClick
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPurchases) { bill ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewPurchaseDetails(bill.id) }
                                .testTag("purchase_bill_item_${bill.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(AccountingBlue.copy(alpha = 0.12f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalShipping,
                                            contentDescription = null,
                                            tint = AccountingBlue,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = bill.billNumber,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = bill.supplierName,
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                        Text(
                                            text = "${formatDate(bill.date)} • ${bill.paymentType}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(bill.totalAmount, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = AccountingBlue,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (bill.paymentType == "CREDIT" && bill.balanceDue > 0) {
                                        Text(
                                            text = "Payable: ${formatCurrency(bill.balanceDue, currency)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccountingRed, fontWeight = FontWeight.Bold)
                                        )
                                    } else {
                                        Text(
                                            text = "Paid in Full",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccountingGreen, fontWeight = FontWeight.Medium)
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

data class EditablePurchaseItem(
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val unit: String,
    var quantity: Double,
    var purchaseRate: Double,
    var discount: Double = 0.0
) {
    val totalPrice: Double get() = (quantity * purchaseRate) - discount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePurchaseBillScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onPurchaseCreated: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var billNumber by remember { mutableStateOf("BILL-${System.currentTimeMillis().toString().takeLast(6)}") }
    var paymentType by remember { mutableStateOf("CREDIT") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    LaunchedEffect(suppliers) {
        if (selectedSupplierId == null && suppliers.isNotEmpty()) {
            selectedSupplierId = suppliers.first().id
        }
    }

    var selectedCashAccountId by remember {
        mutableStateOf<Long?>(cashAccounts.firstOrNull()?.id)
    }

    var lineItems by remember { mutableStateOf(listOf<EditablePurchaseItem>()) }
    var discountStr by remember { mutableStateOf("") }
    var taxRateStr by remember { mutableStateOf("") }
    var amountPaidStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var showAddItemSheet by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val subtotal = lineItems.sumOf { it.totalPrice }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val taxRate = taxRateStr.toDoubleOrNull() ?: 0.0
    val taxAmount = ((subtotal - discount) * taxRate) / 100.0
    val totalAmount = (subtotal - discount + taxAmount).coerceAtLeast(0.0)
    val amountPaid = if (paymentType == "CASH") totalAmount else (amountPaidStr.toDoubleOrNull() ?: 0.0)
    val balanceDue = (totalAmount - amountPaid).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "New Purchase Bill",
                company = company,
                user = currentUser,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AccountingRedLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AccountingRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(errorMessage ?: "", color = AccountingRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Bill Information Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = billNumber,
                            onValueChange = { billNumber = it },
                            label = { Text("Bill / Invoice #") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = formatDate(System.currentTimeMillis()),
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Supplier Selection
                    Text("Select Supplier * (Required):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    var expandedSupplier by remember { mutableStateOf(false) }
                    val selectedSupp = suppliers.find { it.id == selectedSupplierId }

                    ExposedDropdownMenuBox(
                        expanded = expandedSupplier,
                        onExpandedChange = { expandedSupplier = !expandedSupplier }
                    ) {
                        OutlinedTextField(
                            value = selectedSupp?.name ?: "Select Supplier...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplier) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("supplier_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedSupplier,
                            onDismissRequest = { expandedSupplier = false }
                        ) {
                            suppliers.forEach { supp ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(supp.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                formatCurrency(supp.currentBalance, currency),
                                                fontSize = 11.sp,
                                                color = if (supp.currentBalance > 0) AccountingRed else AccountingGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedSupplierId = supp.id
                                        expandedSupplier = false
                                    }
                                )
                            }
                        }
                    }

                    // Payment Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { paymentType = "CASH" },
                            colors = if (paymentType == "CASH") ButtonDefaults.buttonColors(containerColor = AccountingGreen) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cash Purchase", color = if (paymentType == "CASH") Color.White else Slate800, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { paymentType = "CREDIT" },
                            colors = if (paymentType == "CREDIT") ButtonDefaults.buttonColors(containerColor = NavyPrimary) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Credit Purchase", color = if (paymentType == "CREDIT") Color.White else Slate800, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (paymentType == "CASH" || amountPaid > 0) {
                        var expandedCash by remember { mutableStateOf(false) }
                        val selectedCash = cashAccounts.find { it.id == selectedCashAccountId }

                        Text("Paid From Cash/Bank Account:", fontSize = 12.sp, color = Slate600)
                        ExposedDropdownMenuBox(
                            expanded = expandedCash,
                            onExpandedChange = { expandedCash = !expandedCash }
                        ) {
                            OutlinedTextField(
                                value = selectedCash?.name ?: "Select Payment Account",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCash) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCash,
                                onDismissRequest = { expandedCash = false }
                            ) {
                                cashAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${acc.code})") },
                                        onClick = {
                                            selectedCashAccountId = acc.id
                                            expandedCash = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Purchased Items Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Purchased Items (Creates FIFO Lots)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        Button(
                            onClick = { showAddItemSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccountingBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_item_to_purchase_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 12.sp)
                        }
                    }

                    if (lineItems.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No items added to bill yet. Click '+ Add Item' above.",
                            fontSize = 12.sp,
                            color = Slate500,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        lineItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        "${item.quantity} ${item.unit} @ ${formatCurrency(item.purchaseRate, currency)}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                                Text(
                                    text = formatCurrency(item.totalPrice, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NavyDark
                                )
                                IconButton(onClick = {
                                    lineItems = lineItems.filterIndexed { i, _ -> i != index }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AccountingRed, modifier = Modifier.size(18.dp))
                                }
                            }
                            Divider(color = Slate200)
                        }
                    }
                }
            }

            // Calculation & Totals Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", fontSize = 13.sp, color = Slate700)
                        Text(formatCurrency(subtotal, currency), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { discountStr = it },
                            label = { Text("Bill Discount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = taxRateStr,
                            onValueChange = { taxRateStr = it },
                            label = { Text("Tax %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (paymentType == "CREDIT") {
                        OutlinedTextField(
                            value = amountPaidStr,
                            onValueChange = { amountPaidStr = it },
                            label = { Text("Advance / Paid to Supplier") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Divider(color = Slate200)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                        Text(formatCurrency(totalAmount, currency), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingBlue)
                    }

                    if (paymentType == "CREDIT") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance Due (Creditor):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccountingRed)
                            Text(formatCurrency(balanceDue, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccountingRed)
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Remarks / Gate Pass / Vehicle No") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (lineItems.isEmpty()) {
                        errorMessage = "Please add at least one purchased item."
                        return@Button
                    }
                    if (selectedSupplierId == null) {
                        errorMessage = "Please select a supplier."
                        return@Button
                    }

                    val comp = company ?: return@Button
                    val user = currentUser ?: return@Button
                    val supplier = suppliers.find { it.id == selectedSupplierId } ?: return@Button

                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val bill = PurchaseBillEntity(
                                companyId = comp.id,
                                billNumber = billNumber.trim(),
                                date = System.currentTimeMillis(),
                                supplierId = supplier.id,
                                supplierName = supplier.name,
                                paymentType = paymentType,
                                cashAccountId = selectedCashAccountId,
                                cashAccountName = cashAccounts.find { it.id == selectedCashAccountId }?.name,
                                subtotal = subtotal,
                                discount = discount,
                                taxRate = taxRate,
                                taxAmount = taxAmount,
                                totalAmount = totalAmount,
                                amountPaid = amountPaid,
                                balanceDue = balanceDue,
                                notes = notes.trim()
                            )

                            val purchaseItems = lineItems.map {
                                PurchaseItemEntity(
                                    billId = 0L,
                                    companyId = comp.id,
                                    itemId = it.itemId,
                                    itemCode = it.itemCode,
                                    itemName = it.itemName,
                                    quantity = it.quantity,
                                    unit = it.unit,
                                    purchaseRate = it.purchaseRate,
                                    discount = it.discount,
                                    totalPrice = it.totalPrice
                                )
                            }

                            val result = viewModel.repository.createPurchaseBill(bill, purchaseItems, user)
                            if (result.isSuccess) {
                                viewModel.showMessage("Purchase bill #${bill.billNumber} created with FIFO lots.")
                                onPurchaseCreated(result.getOrThrow())
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create purchase bill."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save purchase."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_purchase_bill_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("SAVE & POST PURCHASE BILL", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddItemSheet) {
        AddItemToPurchaseDialog(
            items = items,
            onDismiss = { showAddItemSheet = false },
            onAdd = { newItem ->
                lineItems = lineItems + newItem
                showAddItemSheet = false
                errorMessage = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemToPurchaseDialog(
    items: List<ItemEntity>,
    onDismiss: () -> Unit,
    onAdd: (EditablePurchaseItem) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Long?>(items.firstOrNull()?.id) }
    var qtyStr by remember { mutableStateOf("1.0") }
    var priceStr by remember {
        mutableStateOf(items.firstOrNull()?.purchasePrice?.toString() ?: "0.0")
    }
    var discountStr by remember { mutableStateOf("0.0") }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedItem = items.find { it.id == selectedItemId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Item for Purchase", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.name ?: "Select Item",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        items.forEach { itm ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(itm.name, fontWeight = FontWeight.Medium)
                                        Text("Current Stock: ${itm.currentStock} ${itm.unit} • Last Cost: ${itm.purchasePrice}", fontSize = 11.sp, color = Slate600)
                                    }
                                },
                                onClick = {
                                    selectedItemId = itm.id
                                    priceStr = itm.purchasePrice.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Quantity (${selectedItem?.unit ?: "KG"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Purchase Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it },
                    label = { Text("Discount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val itm = selectedItem ?: return@Button
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val disc = discountStr.toDoubleOrNull() ?: 0.0

                    if (qty <= 0) {
                        error = "Quantity must be greater than 0."
                        return@Button
                    }
                    onAdd(
                        EditablePurchaseItem(
                            itemId = itm.id,
                            itemCode = itm.code,
                            itemName = itm.name,
                            unit = itm.unit,
                            quantity = qty,
                            purchaseRate = price,
                            discount = disc
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Add to Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
