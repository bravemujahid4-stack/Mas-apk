package com.masaccounts.app.ui.screens.returns

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.data.local.entity.PurchaseReturnEntity
import com.masaccounts.app.data.local.entity.PurchaseReturnItemEntity
import com.masaccounts.app.data.local.entity.SalesReturnEntity
import com.masaccounts.app.data.local.entity.SalesReturnItemEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnsScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val salesReturns by viewModel.salesReturns.collectAsState()
    val purchaseReturns by viewModel.purchaseReturns.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sales Returns", "Purchase Returns")
    val currency = company?.currency ?: "PKR"

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Sales & Purchase Returns",
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_return_top_button")) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = "Add Return", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_return_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Return")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    if (salesReturns.isEmpty()) {
                        EmptyStateCard(
                            title = "No Sales Returns Recorded",
                            subtitle = "Record returned iron/steel goods from customers with automatic FIFO lot restocking and customer balance credit.",
                            icon = Icons.Default.AssignmentReturn,
                            actionButtonText = "+ Record Sales Return",
                            onActionClick = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(salesReturns) { ret ->
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
                                            Text(
                                                text = "Sales Return #${ret.returnNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text("Customer: ${ret.customerName} • ${formatDate(ret.date)}", fontSize = 12.sp, color = Slate600)
                                            if (ret.notes.isNotBlank()) {
                                                Text("Notes: ${ret.notes}", fontSize = 11.sp, color = Slate400)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatCurrency(ret.totalAmount, currency),
                                                fontWeight = FontWeight.Bold,
                                                color = AccountingRed,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (ret.paymentType == "CASH") "Cash Refund" else "Credit Note",
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (purchaseReturns.isEmpty()) {
                        EmptyStateCard(
                            title = "No Purchase Returns Recorded",
                            subtitle = "Record goods returned back to suppliers with automatic inventory stock reduction and supplier balance debit.",
                            icon = Icons.Default.AssignmentReturn,
                            actionButtonText = "+ Record Purchase Return",
                            onActionClick = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(purchaseReturns) { ret ->
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
                                            Text(
                                                text = "Purchase Return #${ret.returnNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text("Supplier: ${ret.supplierName} • ${formatDate(ret.date)}", fontSize = 12.sp, color = Slate600)
                                            if (ret.notes.isNotBlank()) {
                                                Text("Notes: ${ret.notes}", fontSize = 11.sp, color = Slate400)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatCurrency(ret.totalAmount, currency),
                                                fontWeight = FontWeight.Bold,
                                                color = AccountingGreen,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = if (ret.paymentType == "CASH") "Cash Refund" else "Debit Note",
                                                fontSize = 10.sp,
                                                color = Slate500
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

    if (showAddDialog) {
        AddReturnDialog(
            companyId = company?.id ?: 1L,
            customers = customers,
            suppliers = suppliers,
            items = items,
            cashAccounts = cashAccounts,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSaveSalesReturn = { ret, retItems ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createSalesReturn(ret, retItems, user)
                    viewModel.showMessage("Sales Return #${ret.returnNumber} processed with FIFO restocking & ledger updates.")
                    showAddDialog = false
                }
            },
            onSavePurchaseReturn = { ret, retItems ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createPurchaseReturn(ret, retItems, user)
                    viewModel.showMessage("Purchase Return #${ret.returnNumber} processed with inventory deduction & ledger updates.")
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReturnDialog(
    companyId: Long,
    customers: List<com.masaccounts.app.data.local.entity.CustomerEntity>,
    suppliers: List<com.masaccounts.app.data.local.entity.SupplierEntity>,
    items: List<com.masaccounts.app.data.local.entity.ItemEntity>,
    cashAccounts: List<com.masaccounts.app.data.local.entity.AccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSaveSalesReturn: (SalesReturnEntity, List<SalesReturnItemEntity>) -> Unit,
    onSavePurchaseReturn: (PurchaseReturnEntity, List<PurchaseReturnItemEntity>) -> Unit
) {
    var returnType by remember { mutableStateOf("SALES_RETURN") } // SALES_RETURN, PURCHASE_RETURN
    var returnNumber by remember { mutableStateOf("RET-${System.currentTimeMillis().toString().takeLast(6)}") }
    var selectedPartyId by remember { mutableStateOf<Long?>(null) }
    var selectedItemId by remember { mutableStateOf<Long?>(items.firstOrNull()?.id) }
    var qtyStr by remember { mutableStateOf("1.0") }
    var rateStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CREDIT") } // CREDIT or CASH
    var selectedCashAccountId by remember { mutableStateOf<Long?>(cashAccounts.firstOrNull()?.id) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedItem = items.find { it.id == selectedItemId }

    LaunchedEffect(selectedItemId, returnType) {
        if (selectedItem != null) {
            rateStr = if (returnType == "SALES_RETURN") selectedItem.salePrice.toString() else selectedItem.purchasePrice.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Return Voucher", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = returnType == "SALES_RETURN",
                        onClick = { returnType = "SALES_RETURN"; selectedPartyId = null },
                        label = { Text("Sales Return (Customer)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = returnType == "PURCHASE_RETURN",
                        onClick = { returnType = "PURCHASE_RETURN"; selectedPartyId = null },
                        label = { Text("Purchase Return (Supplier)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Party dropdown
                if (returnType == "SALES_RETURN") {
                    Text("Customer:", fontSize = 12.sp, color = Slate600)
                    var expCust by remember { mutableStateOf(false) }
                    val cust = customers.find { it.id == selectedPartyId }
                    ExposedDropdownMenuBox(expanded = expCust, onExpandedChange = { expCust = !expCust }) {
                        OutlinedTextField(
                            value = cust?.name ?: "Select Customer",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCust) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(expanded = expCust, onDismissRequest = { expCust = false }) {
                            customers.forEach { c ->
                                DropdownMenuItem(text = { Text(c.name) }, onClick = { selectedPartyId = c.id; expCust = false })
                            }
                        }
                    }
                } else {
                    Text("Supplier:", fontSize = 12.sp, color = Slate600)
                    var expSupp by remember { mutableStateOf(false) }
                    val supp = suppliers.find { it.id == selectedPartyId }
                    ExposedDropdownMenuBox(expanded = expSupp, onExpandedChange = { expSupp = !expSupp }) {
                        OutlinedTextField(
                            value = supp?.name ?: "Select Supplier",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSupp) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(8.dp)
                        )
                        ExposedDropdownMenu(expanded = expSupp, onDismissRequest = { expSupp = false }) {
                            suppliers.forEach { s ->
                                DropdownMenuItem(text = { Text(s.name) }, onClick = { selectedPartyId = s.id; expSupp = false })
                            }
                        }
                    }
                }

                // Item dropdown
                Text("Item to Return:", fontSize = 12.sp, color = Slate600)
                var expItem by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expItem, onExpandedChange = { expItem = !expItem }) {
                    OutlinedTextField(
                        value = selectedItem?.name ?: "Select Item",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expItem) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = expItem, onDismissRequest = { expItem = false }) {
                        items.forEach { itm ->
                            DropdownMenuItem(text = { Text("${itm.name} (${itm.currentStock} ${itm.unit})") }, onClick = { selectedItemId = itm.id; expItem = false })
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("Qty (${selectedItem?.unit ?: "KG"})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it },
                        label = { Text("Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Return (e.g. Damaged, Wrong Size)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val itm = selectedItem ?: return@Button
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val rate = rateStr.toDoubleOrNull() ?: 0.0
                    if (qty <= 0 || rate <= 0) {
                        error = "Quantity and Rate must be greater than 0."
                        return@Button
                    }
                    if (selectedPartyId == null) {
                        error = "Please select a party."
                        return@Button
                    }

                    val totalAmount = qty * rate

                    if (returnType == "SALES_RETURN") {
                        val cust = customers.find { it.id == selectedPartyId }
                        val ret = SalesReturnEntity(
                            companyId = companyId,
                            returnNumber = returnNumber.trim(),
                            date = System.currentTimeMillis(),
                            customerId = selectedPartyId,
                            customerName = cust?.name ?: "Customer",
                            paymentType = paymentMethod,
                            cashAccountId = selectedCashAccountId,
                            totalAmount = totalAmount,
                            notes = reason.trim()
                        )
                        val returnItems = listOf(
                            SalesReturnItemEntity(
                                returnId = 0L,
                                companyId = companyId,
                                itemId = itm.id,
                                itemCode = itm.code,
                                itemName = itm.name,
                                quantity = qty,
                                unit = itm.unit,
                                unitPrice = rate,
                                totalPrice = totalAmount,
                                unitCost = if (itm.purchasePrice > 0) itm.purchasePrice else itm.openingCost
                            )
                        )
                        onSaveSalesReturn(ret, returnItems)
                    } else {
                        val supp = suppliers.find { it.id == selectedPartyId }
                        val ret = PurchaseReturnEntity(
                            companyId = companyId,
                            returnNumber = returnNumber.trim(),
                            date = System.currentTimeMillis(),
                            supplierId = selectedPartyId ?: 0L,
                            supplierName = supp?.name ?: "Supplier",
                            paymentType = paymentMethod,
                            cashAccountId = selectedCashAccountId,
                            totalAmount = totalAmount,
                            notes = reason.trim()
                        )
                        val returnItems = listOf(
                            PurchaseReturnItemEntity(
                                returnId = 0L,
                                companyId = companyId,
                                itemId = itm.id,
                                itemCode = itm.code,
                                itemName = itm.name,
                                quantity = qty,
                                unit = itm.unit,
                                purchaseRate = rate,
                                totalPrice = totalAmount
                            )
                        )
                        onSavePurchaseReturn(ret, returnItems)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Process Return")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
