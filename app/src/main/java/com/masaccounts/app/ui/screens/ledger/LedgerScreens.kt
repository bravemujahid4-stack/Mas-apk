package com.masaccounts.app.ui.screens.ledger

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.data.local.entity.LedgerEntryEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*

data class LedgerRowWithBalance(
    val entry: LedgerEntryEntity,
    val runningBalance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: MasViewModel,
    initialAccountId: Long? = null,
    initialPartyType: String? = null,
    initialPartyId: Long? = null,
    onBackClick: () -> Unit,
    onPrintStatement: (List<LedgerRowWithBalance>, String) -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val allLedgerEntries by viewModel.ledgerEntries.collectAsState()

    var selectedFilterType by remember {
        mutableStateOf(
            if (initialPartyType == "CUSTOMER") "CUSTOMER"
            else if (initialPartyType == "SUPPLIER") "SUPPLIER"
            else if (initialAccountId != null) "ACCOUNT"
            else "ALL"
        )
    }

    var selectedAccountId by remember { mutableStateOf(initialAccountId) }
    var selectedPartyId by remember { mutableStateOf(initialPartyId) }
    var searchQuery by remember { mutableStateOf("") }

    val currency = company?.currency ?: "PKR"

    // Compute ledger rows with running balances
    val ledgerRows = remember(allLedgerEntries, selectedFilterType, selectedAccountId, selectedPartyId, searchQuery) {
        val filtered = allLedgerEntries.filter { entry ->
            val matchesType = when (selectedFilterType) {
                "ACCOUNT" -> selectedAccountId == null || entry.accountId == selectedAccountId
                "CUSTOMER" -> entry.partyType == "CUSTOMER" && (selectedPartyId == null || entry.partyId == selectedPartyId)
                "SUPPLIER" -> entry.partyType == "SUPPLIER" && (selectedPartyId == null || entry.partyId == selectedPartyId)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true
            else entry.description.contains(searchQuery, ignoreCase = true) ||
                 entry.voucherNumber.contains(searchQuery, ignoreCase = true) ||
                 entry.accountName.contains(searchQuery, ignoreCase = true) ||
                 (entry.partyName?.contains(searchQuery, ignoreCase = true) == true)

            matchesType && matchesSearch
        }.sortedWith(compareBy({ it.date }, { it.id }))

        var running = 0.0
        filtered.map { entry ->
            running += (entry.debit - entry.credit)
            LedgerRowWithBalance(entry, running)
        }
    }

    val totalDebit = ledgerRows.sumOf { it.entry.debit }
    val totalCredit = ledgerRows.sumOf { it.entry.credit }

    val title = when (selectedFilterType) {
        "CUSTOMER" -> selectedPartyId?.let { id -> customers.find { it.id == id }?.let { "${it.name} Statement" } } ?: "Customer Ledgers"
        "SUPPLIER" -> selectedPartyId?.let { id -> suppliers.find { it.id == id }?.let { "${it.name} Statement" } } ?: "Supplier Ledgers"
        "ACCOUNT" -> selectedAccountId?.let { id -> accounts.find { it.id == id }?.let { "${it.name} Ledger" } } ?: "Account Ledger"
        else -> "General Ledger"
    }

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = title,
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { onPrintStatement(ledgerRows, title) }) {
                        Icon(Icons.Default.Print, contentDescription = "Print / Export Statement", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Slate50)
        ) {
            // Filter Controls
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilterType == "ALL",
                            onClick = { selectedFilterType = "ALL" },
                            label = { Text("General Ledger", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilterType == "ACCOUNT",
                            onClick = { selectedFilterType = "ACCOUNT" },
                            label = { Text("By Account", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilterType == "CUSTOMER",
                            onClick = { selectedFilterType = "CUSTOMER" },
                            label = { Text("Customer", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilterType == "SUPPLIER",
                            onClick = { selectedFilterType = "SUPPLIER" },
                            label = { Text("Supplier", fontSize = 11.sp) }
                        )
                    }

                    if (selectedFilterType == "ACCOUNT") {
                        var expAcc by remember { mutableStateOf(false) }
                        val acc = accounts.find { it.id == selectedAccountId }
                        ExposedDropdownMenuBox(expanded = expAcc, onExpandedChange = { expAcc = !expAcc }) {
                            OutlinedTextField(
                                value = acc?.let { "${it.code} - ${it.name}" } ?: "All Accounts",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expAcc) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expAcc, onDismissRequest = { expAcc = false }) {
                                DropdownMenuItem(text = { Text("All Accounts") }, onClick = { selectedAccountId = null; expAcc = false })
                                accounts.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text("${a.code} - ${a.name}") },
                                        onClick = { selectedAccountId = a.id; expAcc = false }
                                    )
                                }
                            }
                        }
                    } else if (selectedFilterType == "CUSTOMER") {
                        var expCust by remember { mutableStateOf(false) }
                        val cust = customers.find { it.id == selectedPartyId }
                        ExposedDropdownMenuBox(expanded = expCust, onExpandedChange = { expCust = !expCust }) {
                            OutlinedTextField(
                                value = cust?.name ?: "All Customers",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCust) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expCust, onDismissRequest = { expCust = false }) {
                                DropdownMenuItem(text = { Text("All Customers") }, onClick = { selectedPartyId = null; expCust = false })
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = { selectedPartyId = c.id; expCust = false }
                                    )
                                }
                            }
                        }
                    } else if (selectedFilterType == "SUPPLIER") {
                        var expSupp by remember { mutableStateOf(false) }
                        val supp = suppliers.find { it.id == selectedPartyId }
                        ExposedDropdownMenuBox(expanded = expSupp, onExpandedChange = { expSupp = !expSupp }) {
                            OutlinedTextField(
                                value = supp?.name ?: "All Suppliers",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expSupp) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            ExposedDropdownMenu(expanded = expSupp, onDismissRequest = { expSupp = false }) {
                                DropdownMenuItem(text = { Text("All Suppliers") }, onClick = { selectedPartyId = null; expSupp = false })
                                suppliers.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s.name) },
                                        onClick = { selectedPartyId = s.id; expSupp = false }
                                    )
                                }
                            }
                        }
                    }

                    // Totals summary bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Dr: ${formatCurrency(totalDebit, currency)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccountingGreen)
                        Text("Total Cr: ${formatCurrency(totalCredit, currency)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccountingBlue)
                        Text("Net: ${formatCurrency(kotlin.math.abs(totalDebit - totalCredit), currency)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    }
                }
            }

            if (ledgerRows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateCard(
                        title = "No Ledger Entries",
                        subtitle = "Ledger entries are automatically created with every sale, purchase, receipt, payment, and journal voucher.",
                        icon = Icons.Default.MenuBook
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ledgerRows) { row ->
                        LedgerEntryCard(row = row, currency = currency)
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerEntryCard(
    row: LedgerRowWithBalance,
    currency: String
) {
    val entry = row.entry

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.description,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${formatDate(entry.date)} • ${entry.accountCode} - ${entry.accountName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate600)
                    )
                    if (entry.partyName != null) {
                        Text(
                            text = "Party: ${entry.partyName} (${entry.voucherType} #${entry.voucherNumber})",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate500)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (entry.debit > 0) {
                        Text(
                            text = "Dr: ${formatCurrency(entry.debit, currency)}",
                            fontWeight = FontWeight.Bold,
                            color = AccountingGreen,
                            fontSize = 13.sp
                        )
                    }
                    if (entry.credit > 0) {
                        Text(
                            text = "Cr: ${formatCurrency(entry.credit, currency)}",
                            fontWeight = FontWeight.Bold,
                            color = AccountingBlue,
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = "Bal: ${formatCurrency(kotlin.math.abs(row.runningBalance), currency)}" + if (row.runningBalance >= 0) " Dr" else " Cr",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                    )
                }
            }
        }
    }
}
