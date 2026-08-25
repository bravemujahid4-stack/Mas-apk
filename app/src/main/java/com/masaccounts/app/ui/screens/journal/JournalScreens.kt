package com.masaccounts.app.ui.screens.journal

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
import com.masaccounts.app.data.local.entity.AccountEntity
import com.masaccounts.app.data.local.entity.JournalEntryEntity
import com.masaccounts.app.data.local.entity.JournalLineEntity
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalEntriesScreen(
    viewModel: MasViewModel,
    onCreateJournalClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()

    val currency = company?.currency ?: "PKR"
    val canAdd = currentUser?.role != "VIEWER"

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Journal Entries (JV)",
                company = company,
                user = currentUser,
                onBackClick = onBackClick,
                actions = {
                    if (canAdd) {
                        IconButton(onClick = onCreateJournalClick, modifier = Modifier.testTag("add_jv_top_button")) {
                            Icon(Icons.Default.AddBox, contentDescription = "New JV", tint = Color.White)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (canAdd) {
                FloatingActionButton(
                    onClick = onCreateJournalClick,
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_jv_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New JV")
                }
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
            if (journalEntries.isEmpty()) {
                EmptyStateCard(
                    title = "No Journal Entries",
                    subtitle = "Create standard double-entry journal vouchers (adjustments, depreciation, transfers).",
                    icon = Icons.Default.AccountBalance,
                    actionButtonText = if (canAdd) "+ New Journal Voucher" else null,
                    onActionClick = onCreateJournalClick
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(journalEntries) { jv ->
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
                                    Text(jv.entryNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(jv.description.ifBlank { "Journal Voucher" }, fontSize = 12.sp, color = Slate600)
                                    Text(formatDate(jv.date) + if (jv.reference.isNotBlank()) " • Ref: ${jv.reference}" else "", fontSize = 10.sp, color = Slate400)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(jv.totalDebit, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text("Balanced (Dr=Cr)", fontSize = 10.sp, color = AccountingGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class EditableJournalLine(
    var accountId: Long,
    var accountCode: String,
    var accountName: String,
    var debit: Double = 0.0,
    var credit: Double = 0.0,
    var lineDescription: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJournalEntryScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit,
    onJournalCreated: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var entryNumber by remember { mutableStateOf("JV-${System.currentTimeMillis().toString().takeLast(6)}") }
    var reference by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var lines by remember {
        mutableStateOf(
            listOf(
                EditableJournalLine(accountId = accounts.firstOrNull()?.id ?: 1L, accountCode = accounts.firstOrNull()?.code ?: "1010", accountName = accounts.firstOrNull()?.name ?: "Cash in Hand 1"),
                EditableJournalLine(accountId = accounts.getOrNull(1)?.id ?: 2L, accountCode = accounts.getOrNull(1)?.code ?: "1020", accountName = accounts.getOrNull(1)?.name ?: "Cash in Hand 2")
            )
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val currency = company?.currency ?: "PKR"
    val totalDebit = lines.sumOf { it.debit }
    val totalCredit = lines.sumOf { it.credit }
    val isBalanced = kotlin.math.abs(totalDebit - totalCredit) < 0.01 && totalDebit > 0

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "New Journal Voucher",
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

            // Header card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = entryNumber,
                            onValueChange = { entryNumber = it },
                            label = { Text("JV Number") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = formatDate(System.currentTimeMillis()),
                            onValueChange = {},
                            label = { Text("Date") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text("Reference / Document #") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("General Narration / Purpose *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Lines builder
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
                        Text("Journal Lines (Double Entry)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyDark)
                        Button(
                            onClick = {
                                val firstAcc = accounts.firstOrNull()
                                lines = lines + EditableJournalLine(
                                    accountId = firstAcc?.id ?: 1L,
                                    accountCode = firstAcc?.code ?: "1010",
                                    accountName = firstAcc?.name ?: "Cash in Hand 1"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+ Add Line", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    lines.forEachIndexed { index, line ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate50, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Line #${index + 1}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate600)
                                if (lines.size > 2) {
                                    IconButton(
                                        onClick = { lines = lines.filterIndexed { i, _ -> i != index } },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = AccountingRed)
                                    }
                                }
                            }

                            var expandedAcc by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedAcc,
                                onExpandedChange = { expandedAcc = !expandedAcc }
                            ) {
                                OutlinedTextField(
                                    value = "${line.accountCode} - ${line.accountName}",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedAcc,
                                    onDismissRequest = { expandedAcc = false }
                                ) {
                                    accounts.forEach { acc ->
                                        DropdownMenuItem(
                                            text = { Text("${acc.code} - ${acc.name} (${acc.type})") },
                                            onClick = {
                                                val updated = lines.toMutableList()
                                                updated[index] = line.copy(
                                                    accountId = acc.id,
                                                    accountCode = acc.code,
                                                    accountName = acc.name
                                                )
                                                lines = updated
                                                expandedAcc = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var debitText by remember { mutableStateOf(if (line.debit > 0) line.debit.toString() else "") }
                                var creditText by remember { mutableStateOf(if (line.credit > 0) line.credit.toString() else "") }

                                OutlinedTextField(
                                    value = debitText,
                                    onValueChange = {
                                        debitText = it
                                        val amt = it.toDoubleOrNull() ?: 0.0
                                        val updated = lines.toMutableList()
                                        updated[index] = line.copy(debit = amt, credit = 0.0)
                                        if (amt > 0) creditText = ""
                                        lines = updated
                                    },
                                    label = { Text("Debit ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                OutlinedTextField(
                                    value = creditText,
                                    onValueChange = {
                                        creditText = it
                                        val amt = it.toDoubleOrNull() ?: 0.0
                                        val updated = lines.toMutableList()
                                        updated[index] = line.copy(credit = amt, debit = 0.0)
                                        if (amt > 0) debitText = ""
                                        lines = updated
                                    },
                                    label = { Text("Credit ($currency)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Divider(color = Slate200)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Debit: ${formatCurrency(totalDebit, currency)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                        Text("Total Credit: ${formatCurrency(totalCredit, currency)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBalanced) AccountingGreenLight else AccountingRedLight,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isBalanced) "✓ Balanced! Total Debit matches Total Credit." else "⚠ Unbalanced! Debit (${formatCurrency(totalDebit, currency)}) ≠ Credit (${formatCurrency(totalCredit, currency)})",
                            color = if (isBalanced) AccountingGreen else AccountingRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (!isBalanced) {
                        errorMessage = "Cannot save unbalanced journal entry. Total Debit must equal Total Credit."
                        return@Button
                    }
                    val comp = company ?: return@Button
                    val user = currentUser ?: return@Button

                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val jv = JournalEntryEntity(
                                companyId = comp.id,
                                entryNumber = entryNumber.trim(),
                                date = System.currentTimeMillis(),
                                reference = reference.trim(),
                                description = description.trim(),
                                totalDebit = totalDebit,
                                totalCredit = totalCredit
                            )
                            val jvLines = lines.map {
                                JournalLineEntity(
                                    journalId = 0L,
                                    companyId = comp.id,
                                    accountId = it.accountId,
                                    accountCode = it.accountCode,
                                    accountName = it.accountName,
                                    debit = it.debit,
                                    credit = it.credit,
                                    lineDescription = it.lineDescription
                                )
                            }
                            val res = viewModel.repository.createJournalEntry(jv, jvLines, user)
                            if (res.isSuccess) {
                                viewModel.showMessage("Journal entry #${jv.entryNumber} posted.")
                                onJournalCreated()
                            } else {
                                errorMessage = res.exceptionOrNull()?.localizedMessage ?: "Failed to post JV."
                            }
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Failed to save journal."
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_jv_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp),
                enabled = isBalanced && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("POST JOURNAL VOUCHER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
