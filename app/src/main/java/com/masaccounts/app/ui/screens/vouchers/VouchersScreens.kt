package com.masaccounts.app.ui.screens.vouchers

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
import com.masaccounts.app.data.local.entity.ExpenseEntity
import com.masaccounts.app.data.local.entity.PaymentReceiptEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerReceiptScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var voucherNumber by remember { mutableStateOf("RCP-${System.currentTimeMillis().toString().takeLast(6)}") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(customers.firstOrNull()?.id) }
    var selectedAccountId by remember { mutableStateOf<Long?>(cashAccounts.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedAccount = cashAccounts.find { it.id == selectedAccountId }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Customer Receipt (CRV)",
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
                Card(colors = CardDefaults.cardColors(containerColor = AccountingRedLight)) {
                    Text(
                        text = errorMessage ?: "",
                        color = AccountingRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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
                            value = voucherNumber,
                            onValueChange = { voucherNumber = it },
                            label = { Text("Receipt #") },
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

                    Text("Customer (Debtor):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    var expandedCustomer by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCustomer,
                        onExpandedChange = { expandedCustomer = !expandedCustomer }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "Select Customer...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCustomer) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCustomer,
                            onDismissRequest = { expandedCustomer = false }
                        ) {
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cust.name, fontWeight = FontWeight.Medium)
                                            Text(
                                                "Due: " + formatCurrency(cust.currentBalance, currency),
                                                fontSize = 11.sp,
                                                color = if (cust.currentBalance > 0) GoldDark else AccountingGreen
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCustomerId = cust.id
                                        expandedCustomer = false
                                    }
                                )
                            }
                        }
                    }

                    if (selectedCustomer != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Receivable Due:", fontSize = 12.sp, color = Slate600)
                                Text(
                                    formatCurrency(selectedCustomer.currentBalance, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedCustomer.currentBalance > 0) GoldDark else AccountingGreen,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text("Deposit Into Account:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    var expandedAccount by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedAccount,
                        onExpandedChange = { expandedAccount = !expandedAccount }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.name} (${it.code})" } ?: "Select Account...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccount) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAccount,
                            onDismissRequest = { expandedAccount = false }
                        ) {
                            cashAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.code})") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        expandedAccount = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it; errorMessage = null },
                        label = { Text("Received Amount * ($currency)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("receipt_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Cheque # / Online Transaction Ref / Slip #") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Narration") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        errorMessage = "Please enter a valid received amount."
                        return@Button
                    }
                    if (selectedCustomerId == null) {
                        errorMessage = "Please select a customer."
                        return@Button
                    }
                    if (selectedAccountId == null) {
                        errorMessage = "Please select a receiving cash or bank account."
                        return@Button
                    }

                    val comp = company ?: return@Button
                    val user = currentUser ?: return@Button
                    val customer = selectedCustomer ?: return@Button
                    val account = selectedAccount ?: return@Button

                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val voucher = PaymentReceiptEntity(
                                companyId = comp.id,
                                voucherNumber = voucherNumber.trim(),
                                type = "RECEIPT",
                                partyType = "CUSTOMER",
                                partyId = customer.id,
                                partyName = customer.name,
                                accountId = account.id,
                                accountName = account.name,
                                amount = amount,
                                date = System.currentTimeMillis(),
                                reference = reference.trim(),
                                notes = notes.trim()
                            )
                            viewModel.repository.createCustomerReceipt(voucher, user)
                            viewModel.showMessage("Receipt of ${formatCurrency(amount, currency)} from ${customer.name} saved.")
                            onBackClick()
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save receipt."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_receipt_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("SAVE RECEIPT VOUCHER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierPaymentScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var voucherNumber by remember { mutableStateOf("PAY-${System.currentTimeMillis().toString().takeLast(6)}") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    var selectedAccountId by remember { mutableStateOf<Long?>(cashAccounts.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val selectedSupplier = suppliers.find { it.id == selectedSupplierId }
    val selectedAccount = cashAccounts.find { it.id == selectedAccountId }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Supplier Payment (CPV)",
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
                Card(colors = CardDefaults.cardColors(containerColor = AccountingRedLight)) {
                    Text(
                        text = errorMessage ?: "",
                        color = AccountingRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

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
                            value = voucherNumber,
                            onValueChange = { voucherNumber = it },
                            label = { Text("Payment #") },
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

                    Text("Supplier (Creditor):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    var expandedSupplier by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedSupplier,
                        onExpandedChange = { expandedSupplier = !expandedSupplier }
                    ) {
                        OutlinedTextField(
                            value = selectedSupplier?.name ?: "Select Supplier...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupplier) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
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
                                                "Payable: " + formatCurrency(supp.currentBalance, currency),
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

                    if (selectedSupplier != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate100,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Balance Payable:", fontSize = 12.sp, color = Slate600)
                                Text(
                                    formatCurrency(selectedSupplier.currentBalance, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedSupplier.currentBalance > 0) AccountingRed else AccountingGreen,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Text("Pay From Account:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                    var expandedAccount by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedAccount,
                        onExpandedChange = { expandedAccount = !expandedAccount }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.let { "${it.name} (${it.code})" } ?: "Select Account...",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAccount) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAccount,
                            onDismissRequest = { expandedAccount = false }
                        ) {
                            cashAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.name} (${acc.code})") },
                                    onClick = {
                                        selectedAccountId = acc.id
                                        expandedAccount = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it; errorMessage = null },
                        label = { Text("Paid Amount * ($currency)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Cheque # / Online Bank Transfer Ref #") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Narration") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        errorMessage = "Please enter a valid amount paid."
                        return@Button
                    }
                    if (selectedSupplierId == null) {
                        errorMessage = "Please select a supplier."
                        return@Button
                    }
                    if (selectedAccountId == null) {
                        errorMessage = "Please select a paying cash or bank account."
                        return@Button
                    }

                    val comp = company ?: return@Button
                    val user = currentUser ?: return@Button
                    val supplier = selectedSupplier ?: return@Button
                    val account = selectedAccount ?: return@Button

                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val voucher = PaymentReceiptEntity(
                                companyId = comp.id,
                                voucherNumber = voucherNumber.trim(),
                                type = "PAYMENT",
                                partyType = "SUPPLIER",
                                partyId = supplier.id,
                                partyName = supplier.name,
                                accountId = account.id,
                                accountName = account.name,
                                amount = amount,
                                date = System.currentTimeMillis(),
                                reference = reference.trim(),
                                notes = notes.trim()
                            )
                            viewModel.repository.createSupplierPayment(voucher, user)
                            viewModel.showMessage("Payment of ${formatCurrency(amount, currency)} to ${supplier.name} saved.")
                            onBackClick()
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save payment."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("SAVE PAYMENT VOUCHER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val expenseAccounts by viewModel.expenseAccounts.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    val currency = company?.currency ?: "PKR"

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Operating Expenses",
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_expense_top_button")) {
                        Icon(Icons.Default.AddCard, contentDescription = "Add Expense", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .padding(16.dp)
        ) {
            val totalExpense = expenses.sumOf { it.amount }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Operating Expenses", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(formatCurrency(totalExpense, currency), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = GoldLight, modifier = Modifier.size(32.dp))
                }
            }

            if (expenses.isEmpty()) {
                EmptyStateCard(
                    title = "No Expenses Recorded",
                    subtitle = "Track rent, electricity, salaries, tea/entertainment, freight, and general shop expenses.",
                    icon = Icons.Default.ReceiptLong,
                    actionButtonText = "+ Record Expense",
                    onActionClick = { showAddDialog = true }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(expenses) { exp ->
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
                                    Text(exp.expenseAccountName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(exp.description.ifBlank { "Voucher #${exp.voucherNumber}" }, fontSize = 12.sp, color = Slate600)
                                    Text("Paid via: ${exp.paymentAccountName} • ${formatDate(exp.date)}", fontSize = 10.sp, color = Slate400)
                                }
                                Text(
                                    text = formatCurrency(exp.amount, currency),
                                    fontWeight = FontWeight.Bold,
                                    color = AccountingRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            companyId = company?.id ?: 1L,
            expenseAccounts = expenseAccounts,
            paymentAccounts = cashAccounts,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { newExp ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createExpense(newExp, user)
                    viewModel.showMessage("Expense of ${formatCurrency(newExp.amount, currency)} recorded.")
                    showAddDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    companyId: Long,
    expenseAccounts: List<com.masaccounts.app.data.local.entity.AccountEntity>,
    paymentAccounts: List<com.masaccounts.app.data.local.entity.AccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntity) -> Unit
) {
    var selectedExpAccountId by remember { mutableStateOf<Long?>(expenseAccounts.firstOrNull()?.id) }
    var selectedPayAccountId by remember { mutableStateOf<Long?>(paymentAccounts.firstOrNull()?.id) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reference by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val expAcc = expenseAccounts.find { it.id == selectedExpAccountId }
    val payAcc = paymentAccounts.find { it.id == selectedPayAccountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Expense", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text("Expense Category / Head:", fontSize = 12.sp, color = Slate600)
                var expandedExp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedExp,
                    onExpandedChange = { expandedExp = !expandedExp }
                ) {
                    OutlinedTextField(
                        value = expAcc?.name ?: "Select Expense Account",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExp) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedExp,
                        onDismissRequest = { expandedExp = false }
                    ) {
                        expenseAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.code})") },
                                onClick = {
                                    selectedExpAccountId = acc.id
                                    expandedExp = false
                                }
                            )
                        }
                    }
                }

                Text("Paid From Account:", fontSize = 12.sp, color = Slate600)
                var expandedPay by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedPay,
                    onExpandedChange = { expandedPay = !expandedPay }
                ) {
                    OutlinedTextField(
                        value = payAcc?.name ?: "Select Payment Account",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPay) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPay,
                        onDismissRequest = { expandedPay = false }
                    ) {
                        paymentAccounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.code})") },
                                onClick = {
                                    selectedPayAccountId = acc.id
                                    expandedPay = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Bill # / Voucher Reference") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) {
                        error = "Please enter an amount greater than 0."
                        return@Button
                    }
                    val eAcc = expAcc ?: return@Button
                    val pAcc = payAcc ?: return@Button

                    onSave(
                        ExpenseEntity(
                            companyId = companyId,
                            voucherNumber = "EXP-${System.currentTimeMillis().toString().takeLast(6)}",
                            date = System.currentTimeMillis(),
                            expenseAccountId = eAcc.id,
                            expenseAccountName = eAcc.name,
                            paymentAccountId = pAcc.id,
                            paymentAccountName = pAcc.name,
                            amount = amount,
                            description = description.trim(),
                            reference = reference.trim()
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
