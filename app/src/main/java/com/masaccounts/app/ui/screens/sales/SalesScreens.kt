package com.masaccounts.app.ui.screens.sales

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
import com.masaccounts.app.data.local.entity.CustomerEntity
import com.masaccounts.app.data.local.entity.ItemEntity
import com.masaccounts.app.data.local.entity.SaleInvoiceEntity
import com.masaccounts.app.data.local.entity.SaleItemEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    viewModel: MasViewModel,
    onCreateSaleClick: () -> Unit,
    onViewSaleDetails: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val sales by viewModel.sales.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, CASH, CREDIT

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    val filteredSales = remember(sales, searchQuery, selectedFilter) {
        sales.filter { sale ->
            val matchesFilter = when (selectedFilter) {
                "CASH" -> sale.paymentType == "CASH"
                "CREDIT" -> sale.paymentType == "CREDIT"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true
            else sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                 sale.customerName.contains(searchQuery, ignoreCase = true)

            matchesFilter && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Sales Invoices",
                company = company,
                user = currentUser,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = onCreateSaleClick, modifier = Modifier.testTag("add_sale_top_button")) {
                            Icon(Icons.Default.AddShoppingCart, contentDescription = "New Sale", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = onCreateSaleClick,
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_sale_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Sale")
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
                        placeholder = { Text("Search invoice #, customer name...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_sales_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("All (${sales.size})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "CASH",
                            onClick = { selectedFilter = "CASH" },
                            label = { Text("Cash (${sales.count { it.paymentType == "CASH" }})") }
                        )
                        FilterChip(
                            selected = selectedFilter == "CREDIT",
                            onClick = { selectedFilter = "CREDIT" },
                            label = { Text("Credit (${sales.count { it.paymentType == "CREDIT" }})") }
                        )
                    }
                }
            }

            if (filteredSales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "No Sales Invoices",
                        subtitle = "Generate your first sale invoice with cash or credit terms.",
                        icon = Icons.Default.PointOfSale,
                        actionButtonText = if (canAdd) "+ Create Sale Invoice" else null,
                        onActionClick = onCreateSaleClick
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales) { sale ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onViewSaleDetails(sale.id) }
                                .testTag("sale_invoice_item_${sale.id}")
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
                                            .background(
                                                if (sale.paymentType == "CASH") AccountingGreenLight else GoldContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (sale.paymentType == "CASH") Icons.Default.AttachMoney else Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = if (sale.paymentType == "CASH") AccountingGreen else GoldDark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = sale.invoiceNumber,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (sale.customerName.isNotBlank()) sale.customerName else "Cash Customer",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                                        )
                                        Text(
                                            text = "${formatDate(sale.date)} • ${sale.paymentType}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(sale.totalAmount, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = AccountingGreen,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (sale.paymentType == "CREDIT" && sale.balanceDue > 0) {
                                        Text(
                                            text = "Due: ${formatCurrency(sale.balanceDue, currency)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GoldDark, fontWeight = FontWeight.Bold)
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

data class EditableSaleItem(
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val unit: String,
    var quantity: Double,
    var unitPrice: Double,
    var discount: Double = 0.0
) {
    val totalPrice: Double get() = (quantity * unitPrice) - discount
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSaleInvoiceScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onSaleCreated: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val items by viewModel.items.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis().toString().takeLast(6)}") }
    var paymentType by remember { mutableStateOf("CREDIT") } // "CASH" or "CREDIT"
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var selectedCashAccountId by remember {
        mutableStateOf<Long?>(cashAccounts.firstOrNull()?.id)
    }
    LaunchedEffect(cashAccounts) {
        if (selectedCashAccountId == null && cashAccounts.isNotEmpty()) {
            selectedCashAccountId = cashAccounts.first().id
        }
    }

    var lineItems by remember { mutableStateOf(listOf<EditableSaleItem>()) }
    var discountStr by remember { mutableStateOf("") }
    var taxRateStr by remember { mutableStateOf("") }
    var amountPaidStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // Dialog state for adding item to line
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
                title = "New Sales Invoice",
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

            // Invoice details card
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
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it },
                            label = { Text("Invoice #") },
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

                    Text("Payment Terms & Customer:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    // Payment Type Toggle: CASH vs CREDIT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                paymentType = "CASH"
                                errorMessage = null
                            },
                            colors = if (paymentType == "CASH") ButtonDefaults.buttonColors(containerColor = AccountingGreen) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("payment_type_cash_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Cash Sale",
                                color = if (paymentType == "CASH") Color.White else Slate800,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                paymentType = "CREDIT"
                                errorMessage = null
                            },
                            colors = if (paymentType == "CREDIT") ButtonDefaults.buttonColors(containerColor = NavyPrimary) else ButtonDefaults.outlinedButtonColors(),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("payment_type_credit_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Credit Sale",
                                color = if (paymentType == "CREDIT") Color.White else Slate800,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Customer Selection
                    Text(
                        text = if (paymentType == "CREDIT") "Select Customer * (Required for Credit)" else "Select Customer (Optional for Cash)",
                        fontSize = 12.sp,
                        fontWeight = if (paymentType == "CREDIT") FontWeight.Bold else FontWeight.Normal,
                        color = if (paymentType == "CREDIT") NavyPrimary else Slate600
                    )

                    var expandedCustomer by remember { mutableStateOf(false) }
                    val selectedCust = customers.find { it.id == selectedCustomerId }

                    ExposedDropdownMenuBox(
                        expanded = expandedCustomer,
                        onExpandedChange = { expandedCustomer = !expandedCustomer }
                    ) {
                        OutlinedTextField(
                            value = selectedCust?.name ?: if (paymentType == "CASH") "Cash / Walk-in Customer" else "Select Customer...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomer) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("customer_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCustomer,
                            onDismissRequest = { expandedCustomer = false }
                        ) {
                            if (paymentType == "CASH") {
                                DropdownMenuItem(
                                    text = { Text("Cash / Walk-in Customer") },
                                    onClick = {
                                        selectedCustomerId = null
                                        expandedCustomer = false
                                    }
                                )
                            }
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cust.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                formatCurrency(cust.currentBalance, currency),
                                                fontSize = 11.sp,
                                                color = if (cust.currentBalance > 0) GoldDark else AccountingGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomerId = cust.id
                                        expandedCustomer = false
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    // Cash / Bank Account selector (if cash or receiving upfront payment)
                    if (paymentType == "CASH" || amountPaid > 0) {
                        var expandedCashAcc by remember { mutableStateOf(false) }
                        val selectedCash = cashAccounts.find { it.id == selectedCashAccountId }

                        Text("Receive Into Cash/Bank Account:", fontSize = 12.sp, color = Slate600)
                        ExposedDropdownMenuBox(
                            expanded = expandedCashAcc,
                            onExpandedChange = { expandedCashAcc = !expandedCashAcc }
                        ) {
                            OutlinedTextField(
                                value = selectedCash?.name ?: "Select Cash / Bank Account",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCashAcc) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCashAcc,
                                onDismissRequest = { expandedCashAcc = false }
                            ) {
                                cashAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text("${acc.name} (${acc.code})") },
                                        onClick = {
                                            selectedCashAccountId = acc.id
                                            expandedCashAcc = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Line Items Card
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
                        Text("Invoice Items", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        Button(
                            onClick = { showAddItemSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AccountingGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_item_to_sale_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item", fontSize = 12.sp)
                        }
                    }

                    if (lineItems.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No items added to invoice yet. Click '+ Add Item' above to select steel/iron products.",
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
                                        "${item.quantity} ${item.unit} @ ${formatCurrency(item.unitPrice, currency)}" + if (item.discount > 0) " (Disc: ${item.discount})" else "",
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
                            label = { Text("Invoice Discount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = taxRateStr,
                            onValueChange = { taxRateStr = it },
                            label = { Text("Sales Tax %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (paymentType == "CREDIT") {
                        OutlinedTextField(
                            value = amountPaidStr,
                            onValueChange = { amountPaidStr = it },
                            label = { Text("Advance / Amount Paid") },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Divider(color = Slate200)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                        Text(formatCurrency(totalAmount, currency), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AccountingGreen)
                    }

                    if (paymentType == "CREDIT") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Balance Due (Debtor):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldDark)
                            Text(formatCurrency(balanceDue, currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldDark)
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Remarks / Bilty No / Truck No") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Button(
                onClick = {
                    if (lineItems.isEmpty()) {
                        errorMessage = "Please add at least one item to the invoice."
                        return@Button
                    }
                    if (paymentType == "CREDIT" && selectedCustomerId == null) {
                        errorMessage = "Customer is required for credit sales."
                        return@Button
                    }
                    if (paymentType == "CASH" && selectedCashAccountId == null) {
                        errorMessage = "Please select a Cash or Bank account to receive payment."
                        return@Button
                    }

                    val comp = company ?: return@Button
                    val user = currentUser ?: return@Button
                    val customer = selectedCustomerId?.let { id -> customers.find { it.id == id } }

                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val saleInvoice = SaleInvoiceEntity(
                                companyId = comp.id,
                                invoiceNumber = invoiceNumber.trim(),
                                date = System.currentTimeMillis(),
                                customerId = customer?.id,
                                customerName = customer?.name ?: "Cash Customer",
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

                            val saleItems = lineItems.map {
                                SaleItemEntity(
                                    invoiceId = 0L,
                                    companyId = comp.id,
                                    itemId = it.itemId,
                                    itemCode = it.itemCode,
                                    itemName = it.itemName,
                                    quantity = it.quantity,
                                    unit = it.unit,
                                    unitPrice = it.unitPrice,
                                    discount = it.discount,
                                    totalPrice = it.totalPrice
                                )
                            }

                            val result = viewModel.repository.createSaleInvoice(saleInvoice, saleItems, user)
                            if (result.isSuccess) {
                                viewModel.showMessage("Sale invoice #${saleInvoice.invoiceNumber} created successfully.")
                                onSaleCreated(result.getOrThrow())
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create invoice."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save sale."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_sale_invoice_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("SAVE & POST INVOICE", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddItemSheet) {
        AddItemToSaleDialog(
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
fun AddItemToSaleDialog(
    items: List<ItemEntity>,
    onDismiss: () -> Unit,
    onAdd: (EditableSaleItem) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<Long?>(items.firstOrNull()?.id) }
    var qtyStr by remember { mutableStateOf("1.0") }
    var priceStr by remember {
        mutableStateOf(items.firstOrNull()?.salePrice?.toString() ?: "0.0")
    }
    var discountStr by remember { mutableStateOf("0.0") }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedItem = items.find { it.id == selectedItemId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Item for Sale", fontWeight = FontWeight.Bold) },
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
                                        Text("Stock: ${itm.currentStock} ${itm.unit} • Rate: ${itm.salePrice}", fontSize = 11.sp, color = Slate600)
                                    }
                                },
                                onClick = {
                                    selectedItemId = itm.id
                                    priceStr = itm.salePrice.toString()
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedItem != null) {
                    Text(
                        "Available Stock: ${selectedItem.currentStock} ${selectedItem.unit}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedItem.currentStock <= 0) AccountingRed else AccountingGreen
                    )
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
                        label = { Text("Unit Rate") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it },
                    label = { Text("Line Discount") },
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
                        EditableSaleItem(
                            itemId = itm.id,
                            itemCode = itm.code,
                            itemName = itm.name,
                            unit = itm.unit,
                            quantity = qty,
                            unitPrice = price,
                            discount = disc
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Add to Invoice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
