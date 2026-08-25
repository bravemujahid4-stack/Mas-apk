package com.masaccounts.app.ui.screens.customers

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
import com.masaccounts.app.data.local.entity.LedgerEntryEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: MasViewModel,
    onNavigateToStatement: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForDetails by remember { mutableStateOf<CustomerEntity?>(null) }

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.city.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Customers (Debtors)",
                company = company,
                user = currentUser,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_customer_top_button")) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
            // Search Bar & Summary
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search customer by name, code, phone, city...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_customer_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val totalReceivable = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
                    val totalAdvance = customers.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${customers.size} Customers",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                        Text(
                            text = "Net Receivables: ${formatCurrency(totalReceivable - totalAdvance, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (totalReceivable >= totalAdvance) GoldDark else AccountingGreen
                            )
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = if (searchQuery.isBlank()) "No Customers Added" else "No Matching Customers",
                        subtitle = if (searchQuery.isBlank()) "Add your first customer to start issuing sales invoices and tracking receivables." else "Try searching with a different name or phone number.",
                        icon = Icons.Default.People,
                        actionButtonText = if (canAdd && searchQuery.isBlank()) "+ Add Customer" else null,
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCustomers) { customer ->
                        CustomerCardItem(
                            customer = customer,
                            currency = currency,
                            onClick = { selectedCustomerForDetails = customer }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCustomerDialog(
            companyId = company?.id ?: 1L,
            onDismiss = { showAddDialog = false },
            onSave = { newCust ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createCustomer(newCust, user)
                    viewModel.showMessage("Customer '${newCust.name}' added successfully.")
                    showAddDialog = false
                }
            }
        )
    }

    selectedCustomerForDetails?.let { customer ->
        CustomerDetailsDialog(
            customer = customer,
            viewModel = viewModel,
            currency = currency,
            onDismiss = { selectedCustomerForDetails = null },
            onViewFullStatement = {
                selectedCustomerForDetails = null
                onNavigateToStatement(customer.id)
            }
        )
    }
}

@Composable
fun CustomerCardItem(
    customer: CustomerEntity,
    currency: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("customer_item_${customer.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(NavyPrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = customer.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyDark)
                        )
                        if (customer.code.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = customer.code,
                                    fontSize = 10.sp,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (customer.phone.isNotBlank() || customer.city.isNotBlank()) {
                        Text(
                            text = listOf(customer.phone, customer.city).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isReceivable = customer.currentBalance >= 0
                Text(
                    text = formatCurrency(kotlin.math.abs(customer.currentBalance), currency),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (customer.currentBalance == 0.0) Slate600 else if (isReceivable) GoldDark else AccountingGreen
                    )
                )
                Text(
                    text = if (customer.currentBalance == 0.0) "Settled" else if (isReceivable) "Receivable" else "Advance",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (customer.currentBalance == 0.0) Slate400 else if (isReceivable) GoldDark else AccountingGreen,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("CUST-${System.currentTimeMillis().toString().takeLast(4)}") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cnicNtn by remember { mutableStateOf("") }
    var openingBalStr by remember { mutableStateOf("") }
    var openingType by remember { mutableStateOf("DEBIT") } // DEBIT = Receivable, CREDIT = Advance
    var creditLimitStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Customer Name *") },
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
                        label = { Text("Code") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = cnicNtn,
                        onValueChange = { cnicNtn = it },
                        label = { Text("CNIC / NTN") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = openingBalStr,
                        onValueChange = { openingBalStr = it },
                        label = { Text("Opening Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Balance Type:", fontSize = 11.sp, color = Slate600)
                        Row {
                            FilterChip(
                                selected = openingType == "DEBIT",
                                onClick = { openingType = "DEBIT" },
                                label = { Text("Receivable", fontSize = 10.sp) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = openingType == "CREDIT",
                                onClick = { openingType = "CREDIT" },
                                label = { Text("Advance", fontSize = 10.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        error = "Customer Name is required."
                        return@Button
                    }
                    val opening = openingBalStr.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitStr.toDoubleOrNull() ?: 0.0

                    onSave(
                        CustomerEntity(
                            companyId = companyId,
                            code = code.trim(),
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            cnicNtn = cnicNtn.trim(),
                            openingBalance = opening,
                            openingBalanceType = openingType,
                            creditLimit = limit,
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save Customer")
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
fun CustomerDetailsDialog(
    customer: CustomerEntity,
    viewModel: MasViewModel,
    currency: String,
    onDismiss: () -> Unit,
    onViewFullStatement: () -> Unit
) {
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val customerLedger = remember(ledgerEntries, customer.id) {
        ledgerEntries.filter { it.partyType == "CUSTOMER" && it.partyId == customer.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = NavyPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Code: ${customer.code}", fontSize = 12.sp, color = Slate600)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current Balance:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(
                                text = formatCurrency(kotlin.math.abs(customer.currentBalance), currency) + if (customer.currentBalance >= 0) " (Receivable)" else " (Advance)",
                                fontWeight = FontWeight.Bold,
                                color = if (customer.currentBalance >= 0) GoldDark else AccountingGreen,
                                fontSize = 13.sp
                            )
                        }
                        if (customer.phone.isNotBlank()) {
                            Text("Phone: ${customer.phone}", fontSize = 12.sp, color = Slate800)
                        }
                        if (customer.city.isNotBlank() || customer.address.isNotBlank()) {
                            Text("Address: ${listOf(customer.address, customer.city).filter { it.isNotBlank() }.joinToString(", ")}", fontSize = 12.sp, color = Slate800)
                        }
                        if (customer.cnicNtn.isNotBlank()) {
                            Text("CNIC / NTN: ${customer.cnicNtn}", fontSize = 12.sp, color = Slate800)
                        }
                    }
                }

                Text("Recent Ledger Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)

                if (customerLedger.isEmpty()) {
                    Text("No transactions recorded yet for this customer.", fontSize = 12.sp, color = Slate500)
                } else {
                    customerLedger.takeLast(5).reversed().forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.description, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(formatDate(entry.date) + " • ${entry.voucherType} #${entry.voucherNumber}", fontSize = 10.sp, color = Slate500)
                            }
                            Text(
                                text = if (entry.debit > 0) "+ ${formatCurrency(entry.debit, currency)}" else "- ${formatCurrency(entry.credit, currency)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.debit > 0) GoldDark else AccountingGreen
                            )
                        }
                        Divider(color = Slate200)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onViewFullStatement, colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)) {
                Text("View Full Ledger Statement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
