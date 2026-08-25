package com.masaccounts.app.ui.screens.print

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.masaccounts.app.data.local.entity.CompanyEntity
import com.masaccounts.app.ui.components.MasAppLogo
import com.masaccounts.app.ui.components.formatCurrency
import com.masaccounts.app.ui.components.formatDate
import com.masaccounts.app.ui.screens.ledger.LedgerRowWithBalance
import com.masaccounts.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintStatementDialog(
    company: CompanyEntity?,
    title: String,
    rows: List<LedgerRowWithBalance>,
    currency: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Print / Export Preview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Print action simulation / snackbar */ }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Slate100)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Printable A4/Receipt sheet representation
                Card(
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Slate300),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(company?.name ?: "Business Name", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NavyDark)
                                if (company?.address?.isNotBlank() == true) {
                                    Text(company.address, fontSize = 11.sp, color = Slate600)
                                }
                                if (company?.phone?.isNotBlank() == true) {
                                    Text("Phone: ${company.phone}", fontSize = 11.sp, color = Slate600)
                                }
                            }
                            MasAppLogo(size = 48.dp)
                        }

                        Divider(color = NavyDark, thickness = 2.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                            Text("Printed: ${formatDate(System.currentTimeMillis())}", fontSize = 11.sp, color = Slate500)
                        }

                        // Table header
                        Surface(color = Slate100, shape = RoundedCornerShape(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Date", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(65.dp))
                                Text("Particulars / Ref", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("Debit", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Text("Credit", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                                Text("Balance", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                            }
                        }

                        rows.forEach { row ->
                            val e = row.entry
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(formatDate(e.date), fontSize = 10.sp, modifier = Modifier.width(65.dp))
                                Text(e.description, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text(if (e.debit > 0) formatCurrency(e.debit, "") else "-", fontSize = 10.sp, modifier = Modifier.width(60.dp))
                                Text(if (e.credit > 0) formatCurrency(e.credit, "") else "-", fontSize = 10.sp, modifier = Modifier.width(60.dp))
                                Text(formatCurrency(kotlin.math.abs(row.runningBalance), "") + if (row.runningBalance >= 0) " Dr" else " Cr", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                            }
                            Divider(color = Slate100)
                        }

                        Divider(color = NavyDark)

                        val totDr = rows.sumOf { it.entry.debit }
                        val totCr = rows.sumOf { it.entry.credit }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Totals ($currency):", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text(formatCurrency(totDr, ""), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                            Text(formatCurrency(totCr, ""), fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                            Text(formatCurrency(kotlin.math.abs(totDr - totCr), "") + if (totDr >= totCr) " Dr" else " Cr", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(70.dp))
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // Signatures
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Divider(modifier = Modifier.width(100.dp), color = Slate400)
                                Text("Prepared By", fontSize = 10.sp, color = Slate600)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Divider(modifier = Modifier.width(100.dp), color = Slate400)
                                Text("Authorized Sign", fontSize = 10.sp, color = Slate600)
                            }
                        }
                    }
                }
            }
        }
    }
}
