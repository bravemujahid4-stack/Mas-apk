package com.masaccounts.app.ui.screens.suppliers

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
import com.masaccounts.app.data.local.entity.SupplierEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuppliersScreen(
    viewModel: MasViewModel,
    onNavigateToStatement: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedSupplierForDetails by remember { mutableStateOf<SupplierEntity?>(null) }

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else suppliers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery, ignoreCase = true) ||
            it.city.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Suppliers (Creditors)",
                company = company,
                user = currentUser,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_supplier_top_button")) {
                            Icon(Icons.Default.AddBusiness, contentDescription = "Add Supplier", tint = Color.White)
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
                    modifier = Modifier.testTag("add_supplier_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Supplier")
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
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search supplier by name, code, phone, city...") },
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
                            .testTag("search_supplier_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val totalPayable = suppliers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
                    val totalAdvance = suppliers.filter { it.currentBalance < 0 }.sumOf { -it.currentBalance }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${suppliers.size} Suppliers",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                        Text(
                            text = "Net Payables: ${formatCurrency(totalPayable - totalAdvance, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccountingRed
                            )
                        )
                    }
                }
            }

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = if (searchQuery.isBlank()) "No Suppliers Added" else "No Matching Suppliers",
                        subtitle = if (searchQuery.isBlank()) "Add your suppliers and mills to record purchase bills and track payables." else "Try searching with another keyword.",
                        icon = Icons.Default.LocalShipping,
                        actionButtonText = if (canAdd && searchQuery.isBlank()) "+ Add Supplier" else null,
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSuppliers) { supplier ->
                        SupplierCardItem(
                            supplier = supplier,
                            currency = currency,
                            onClick = { selectedSupplierForDetails = supplier }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSupplierDialog(
            companyId = company?.id ?: 1L,
            onDismiss = { showAddDialog = false },
            onSave = { newSupp ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createSupplier(newSupp, user)
                    viewModel.showMessage("Supplier '${newSupp.name}' added successfully.")
                    showAddDialog = false
                }
            }
        )
    }

    selectedSupplierForDetails?.let { supplier ->
        SupplierDetailsDialog(
            supplier = supplier,
            viewModel = viewModel,
            currency = currency,
            onDismiss = { selectedSupplierForDetails = null },
            onViewFullStatement = {
                selectedSupplierForDetails = null
                onNavigateToStatement(supplier.id)
            }
        )
    }
}

@Composable
fun SupplierCardItem(
    supplier: SupplierEntity,
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
            .testTag("supplier_item_${supplier.id}")
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
                        .background(AccountingBlue.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = AccountingBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = NavyDark)
                        )
                        if (supplier.code.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = supplier.code,
                                    fontSize = 10.sp,
                                    color = Slate600,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (supplier.phone.isNotBlank() || supplier.city.isNotBlank()) {
                        Text(
                            text = listOf(supplier.phone, supplier.city).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val isPayable = supplier.currentBalance >= 0
                Text(
                    text = formatCurrency(kotlin.math.abs(supplier.currentBalance), currency),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (supplier.currentBalance == 0.0) Slate600 else if (isPayable) AccountingRed else AccountingGreen
                    )
                )
                Text(
                    text = if (supplier.currentBalance == 0.0) "Settled" else if (isPayable) "Payable" else "Advance Paid",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (supplier.currentBalance == 0.0) Slate400 else if (isPayable) AccountingRed else AccountingGreen,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun AddSupplierDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onSave: (SupplierEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("SUPP-${System.currentTimeMillis().toString().takeLast(4)}") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var ntn by remember { mutableStateOf("") }
    var openingBalStr by remember { mutableStateOf("") }
    var openingType by remember { mutableStateOf("CREDIT") } // CREDIT = Payable, DEBIT = Advance
    var creditLimitStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Supplier", fontWeight = FontWeight.Bold) },
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
                    label = { Text("Supplier Name *") },
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
                        value = ntn,
                        onValueChange = { ntn = it },
                        label = { Text("NTN / STRN") },
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
                                selected = openingType == "CREDIT",
                                onClick = { openingType = "CREDIT" },
                                label = { Text("Payable", fontSize = 10.sp) },
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = openingType == "DEBIT",
                                onClick = { openingType = "DEBIT" },
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
                        error = "Supplier Name is required."
                        return@Button
                    }
                    val opening = openingBalStr.toDoubleOrNull() ?: 0.0
                    val limit = creditLimitStr.toDoubleOrNull() ?: 0.0

                    onSave(
                        SupplierEntity(
                            companyId = companyId,
                            code = code.trim(),
                            name = name.trim(),
                            phone = phone.trim(),
                            address = address.trim(),
                            city = city.trim(),
                            ntn = ntn.trim(),
                            openingBalance = opening,
                            openingBalanceType = openingType,
                            creditLimit = limit,
                            notes = notes.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save Supplier")
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
fun SupplierDetailsDialog(
    supplier: SupplierEntity,
    viewModel: MasViewModel,
    currency: String,
    onDismiss: () -> Unit,
    onViewFullStatement: () -> Unit
) {
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()
    val supplierLedger = remember(ledgerEntries, supplier.id) {
        ledgerEntries.filter { it.partyType == "SUPPLIER" && it.partyId == supplier.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = AccountingBlue)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(supplier.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Code: ${supplier.code}", fontSize = 12.sp, color = Slate600)
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
                                text = formatCurrency(kotlin.math.abs(supplier.currentBalance), currency) + if (supplier.currentBalance >= 0) " (Payable)" else " (Advance Paid)",
                                fontWeight = FontWeight.Bold,
                                color = if (supplier.currentBalance >= 0) AccountingRed else AccountingGreen,
                                fontSize = 13.sp
                            )
                        }
                        if (supplier.phone.isNotBlank()) {
                            Text("Phone: ${supplier.phone}", fontSize = 12.sp, color = Slate800)
                        }
                        if (supplier.city.isNotBlank() || supplier.address.isNotBlank()) {
                            Text("Address: ${listOf(supplier.address, supplier.city).filter { it.isNotBlank() }.joinToString(", ")}", fontSize = 12.sp, color = Slate800)
                        }
                    }
                }

                Text("Recent Ledger Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)

                if (supplierLedger.isEmpty()) {
                    Text("No transactions recorded yet for this supplier.", fontSize = 12.sp, color = Slate500)
                } else {
                    supplierLedger.takeLast(5).reversed().forEach { entry ->
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
                                text = if (entry.credit > 0) "+ ${formatCurrency(entry.credit, currency)}" else "- ${formatCurrency(entry.debit, currency)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (entry.credit > 0) AccountingRed else AccountingGreen
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
