package com.masaccounts.app.ui.screens.accounts

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.data.local.entity.AccountEntity
import com.masaccounts.app.data.local.entity.JournalEntryEntity
import com.masaccounts.app.data.local.entity.JournalLineEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartOfAccountsScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onViewAccountLedger: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role == "ADMIN" || currentUser?.role == "ACCOUNTANT"

    val filteredAccounts = remember(accounts, selectedTypeFilter) {
        if (selectedTypeFilter == "ALL") accounts
        else accounts.filter { it.type == selectedTypeFilter }
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Chart of Accounts",
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_account_top_button")) {
                            Icon(Icons.Default.AddBox, contentDescription = "Add Account", tint = Color.White)
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
                    modifier = Modifier.testTag("add_account_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL", "ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE").forEach { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { selectedTypeFilter = type },
                            label = { Text(if (type == "ALL") "All (${accounts.size})" else type) }
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAccounts) { acc ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAccountLedger(acc.id) }
                            .testTag("account_item_${acc.code}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = Slate100) {
                                        Text(acc.code, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                                }
                                Text("${acc.type} • ${acc.subType.ifBlank { acc.type }}", fontSize = 11.sp, color = Slate500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(acc.currentBalance, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (acc.currentBalance >= 0) NavyPrimary else AccountingRed
                                )
                                Text("View Ledger →", fontSize = 10.sp, color = AccountingBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAccountDialog(
            companyId = company?.id ?: 1L,
            onDismiss = { showAddDialog = false },
            onSave = { newAcc ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    viewModel.repository.createAccount(newAcc, user)
                    viewModel.showMessage("Account '${newAcc.name}' created.")
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun AddAccountDialog(
    companyId: Long,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("ASSET") }
    var subType by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val types = listOf("ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Account", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text("Account Code (e.g. 1050)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("Account Title * (e.g. Petty Cash)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Account Classification / Type:", fontSize = 12.sp, color = Slate600)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subType,
                    onValueChange = { subType = it },
                    label = { Text("Sub-Type (e.g. Current Assets, Operating Expense)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || code.isBlank()) {
                        error = "Code and Account Title are required."
                        return@Button
                    }
                    onSave(
                        AccountEntity(
                            companyId = companyId,
                            code = code.trim(),
                            name = name.trim(),
                            type = type,
                            subType = subType.trim(),
                            isSystem = false
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashAndBankScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onViewAccountLedger: (Long) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val cashAccounts by viewModel.cashAndBankAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showTransferDialog by remember { mutableStateOf(false) }
    val currency = company?.currency ?: "PKR"

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Cash & Bank Accounts",
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showTransferDialog = true }, modifier = Modifier.testTag("fund_transfer_button")) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Funds", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showTransferDialog = true },
                containerColor = NavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fund_transfer_fab")
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer Funds")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalCash = cashAccounts.sumOf { it.currentBalance }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Total Cash & Liquid Liquidity", color = GoldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(formatCurrency(totalCash, currency), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            }

            Text("Accounts Breakdown", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(cashAccounts) { acc ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onViewAccountLedger(acc.id) }
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
                                        .size(46.dp)
                                        .background(
                                            if (acc.name.contains("Bank", ignoreCase = true)) AccountingBlue.copy(alpha = 0.12f)
                                            else AccountingGreenLight,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (acc.name.contains("Bank", ignoreCase = true)) Icons.Default.AccountBalance else Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = if (acc.name.contains("Bank", ignoreCase = true)) AccountingBlue else AccountingGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                                    Text("Account Code: ${acc.code}", fontSize = 11.sp, color = Slate500)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(acc.currentBalance, currency),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (acc.currentBalance >= 0) AccountingGreen else AccountingRed
                                )
                                Text("View Ledger →", fontSize = 11.sp, color = AccountingBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTransferDialog) {
        FundTransferDialog(
            companyId = company?.id ?: 1L,
            accounts = cashAccounts,
            currency = currency,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromAcc, toAcc, amount, notes ->
                coroutineScope.launch {
                    val user = currentUser ?: return@launch
                    val jv = JournalEntryEntity(
                        companyId = company?.id ?: 1L,
                        entryNumber = "TRF-${System.currentTimeMillis().toString().takeLast(6)}",
                        date = System.currentTimeMillis(),
                        reference = "Fund Transfer",
                        description = notes.ifBlank { "Transfer from ${fromAcc.name} to ${toAcc.name}" },
                        totalDebit = amount,
                        totalCredit = amount
                    )
                    val lines = listOf(
                        JournalLineEntity(journalId = 0L, companyId = jv.companyId, accountId = toAcc.id, accountCode = toAcc.code, accountName = toAcc.name, debit = amount, credit = 0.0, lineDescription = "Received from ${fromAcc.name}"),
                        JournalLineEntity(journalId = 0L, companyId = jv.companyId, accountId = fromAcc.id, accountCode = fromAcc.code, accountName = fromAcc.name, debit = 0.0, credit = amount, lineDescription = "Transferred to ${toAcc.name}")
                    )
                    viewModel.repository.createJournalEntry(jv, lines, user)
                    viewModel.showMessage("Transferred ${formatCurrency(amount, currency)} from ${fromAcc.name} to ${toAcc.name}.")
                    showTransferDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FundTransferDialog(
    companyId: Long,
    accounts: List<AccountEntity>,
    currency: String,
    onDismiss: () -> Unit,
    onTransfer: (AccountEntity, AccountEntity, Double, String) -> Unit
) {
    var fromAccountId by remember { mutableStateOf<Long?>(accounts.firstOrNull()?.id) }
    var toAccountId by remember { mutableStateOf<Long?>(accounts.getOrNull(1)?.id) }
    var amountStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val fromAcc = accounts.find { it.id == fromAccountId }
    val toAcc = accounts.find { it.id == toAccountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Funds", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (error != null) {
                    Text(error ?: "", color = AccountingRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text("From Account (Sender):", fontSize = 12.sp, color = Slate600)
                var expFrom by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expFrom, onExpandedChange = { expFrom = !expFrom }) {
                    OutlinedTextField(
                        value = fromAcc?.let { "${it.name} (${formatCurrency(it.currentBalance, currency)})" } ?: "Select From",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expFrom) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = expFrom, onDismissRequest = { expFrom = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text("${acc.name} (${formatCurrency(acc.currentBalance, currency)})") }, onClick = { fromAccountId = acc.id; expFrom = false })
                        }
                    }
                }

                Text("To Account (Receiver):", fontSize = 12.sp, color = Slate600)
                var expTo by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expTo, onExpandedChange = { expTo = !expTo }) {
                    OutlinedTextField(
                        value = toAcc?.let { "${it.name} (${formatCurrency(it.currentBalance, currency)})" } ?: "Select To",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expTo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(expanded = expTo, onDismissRequest = { expTo = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(text = { Text("${acc.name} (${formatCurrency(acc.currentBalance, currency)})") }, onClick = { toAccountId = acc.id; expTo = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Transfer Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt <= 0) {
                        error = "Enter a valid transfer amount."
                        return@Button
                    }
                    if (fromAccountId == toAccountId) {
                        error = "From and To accounts cannot be the same."
                        return@Button
                    }
                    val f = fromAcc ?: return@Button
                    val t = toAcc ?: return@Button
                    onTransfer(f, t, amt, notes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Transfer Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
