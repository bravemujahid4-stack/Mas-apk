package com.masaccounts.app.ui.screens.reports

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MasViewModel,
    onBackClick: () -> Unit
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val items by viewModel.items.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val ledgerEntries by viewModel.ledgerEntries.collectAsState()

    var selectedReportTab by remember { mutableIntStateOf(0) }
    val reportTabs = listOf("P&L", "Balance Sheet", "Trial Balance", "Aging")

    val currency = company?.currency ?: "PKR"

    // Computations
    // 1. P&L
    val totalSalesRevenue = accounts.filter { it.code == "4010" }.sumOf { it.currentBalance }
    val totalCostOfSales = accounts.filter { it.code == "5010" }.sumOf { it.currentBalance }
    val grossProfit = totalSalesRevenue - totalCostOfSales
    val totalOperatingExpenses = accounts.filter { it.type == "EXPENSE" && it.code != "5010" }.sumOf { it.currentBalance }
    val netProfit = grossProfit - totalOperatingExpenses

    // 2. Balance Sheet
    val cashAndBank = accounts.filter { it.code in listOf("1010", "1020", "1030") }.sumOf { it.currentBalance }
    val accountsReceivable = customers.sumOf { it.currentBalance }
    val inventoryValue = items.sumOf { it.currentStock * if (it.purchasePrice > 0) it.purchasePrice else it.openingCost }
    val otherAssets = accounts.filter { it.type == "ASSET" && it.code !in listOf("1010", "1020", "1030", "1040", "1050") }.sumOf { it.currentBalance }
    val totalAssets = cashAndBank + accountsReceivable + inventoryValue + otherAssets

    val accountsPayable = suppliers.sumOf { it.currentBalance }
    val otherLiabilities = accounts.filter { it.type == "LIABILITY" && it.code != "2010" }.sumOf { it.currentBalance }
    val totalLiabilities = accountsPayable + otherLiabilities

    val capitalEquity = accounts.filter { it.type == "EQUITY" }.sumOf { it.currentBalance }
    val totalEquity = capitalEquity + netProfit

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Financial Reports",
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
        ) {
            TabRow(
                selectedTabIndex = selectedReportTab,
                containerColor = Color.White,
                contentColor = NavyPrimary
            ) {
                reportTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedReportTab == index,
                        onClick = { selectedReportTab = index },
                        text = { Text(title, fontWeight = if (selectedReportTab == index) FontWeight.Bold else FontWeight.Medium) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedReportTab) {
                    0 -> {
                        // Profit & Loss
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Income Statement (Profit & Loss)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                                Text("For: ${company?.name ?: "Business"}", fontSize = 12.sp, color = Slate500)

                                Divider(color = Slate200)

                                ReportLineItem(label = "Sales Revenue (Gross Sales)", amount = totalSalesRevenue, currency = currency)
                                ReportLineItem(label = "Less: Cost of Goods Sold (FIFO COGS)", amount = -totalCostOfSales, currency = currency, isNegative = true)

                                Divider(color = Slate200)
                                ReportLineItem(
                                    label = "Gross Profit",
                                    amount = grossProfit,
                                    currency = currency,
                                    isBold = true,
                                    color = if (grossProfit >= 0) AccountingGreen else AccountingRed
                                )

                                Divider(color = Slate200)
                                Text("Operating Expenses:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyDark)

                                val expList = accounts.filter { it.type == "EXPENSE" && it.code != "5010" }
                                if (expList.isEmpty()) {
                                    Text("No operating expenses recorded.", fontSize = 12.sp, color = Slate500)
                                } else {
                                    expList.forEach { exp ->
                                        ReportLineItem(label = "${exp.code} - ${exp.name}", amount = exp.currentBalance, currency = currency)
                                    }
                                }

                                Divider(color = Slate200)
                                ReportLineItem(label = "Total Operating Expenses", amount = totalOperatingExpenses, currency = currency, isBold = true)

                                Divider(color = NavyDark, thickness = 2.dp)

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = if (netProfit >= 0) AccountingGreenLight else AccountingRedLight),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("NET PROFIT / (LOSS)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (netProfit >= 0) AccountingGreen else AccountingRed)
                                        Text(formatCurrency(netProfit, currency), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (netProfit >= 0) AccountingGreen else AccountingRed)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Balance Sheet
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Statement of Financial Position (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)
                                Text("As of today • Double-entry verified", fontSize = 12.sp, color = Slate500)

                                Divider(color = Slate200)
                                Text("ASSETS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NavyPrimary)
                                ReportLineItem(label = "Cash & Bank Balances", amount = cashAndBank, currency = currency)
                                ReportLineItem(label = "Accounts Receivable (Debtors)", amount = accountsReceivable, currency = currency)
                                ReportLineItem(label = "Inventory on Hand (Current Valuation)", amount = inventoryValue, currency = currency)
                                if (otherAssets > 0) {
                                    ReportLineItem(label = "Other Assets", amount = otherAssets, currency = currency)
                                }
                                Divider(color = Slate200)
                                ReportLineItem(label = "TOTAL ASSETS", amount = totalAssets, currency = currency, isBold = true, color = NavyPrimary)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("LIABILITIES", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccountingRed)
                                ReportLineItem(label = "Accounts Payable (Creditors)", amount = accountsPayable, currency = currency)
                                if (otherLiabilities > 0) {
                                    ReportLineItem(label = "Other Liabilities & Taxes", amount = otherLiabilities, currency = currency)
                                }
                                Divider(color = Slate200)
                                ReportLineItem(label = "TOTAL LIABILITIES", amount = totalLiabilities, currency = currency, isBold = true, color = AccountingRed)

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("EQUITY & CAPITAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PurplePrimary)
                                ReportLineItem(label = "Owner's Capital", amount = capitalEquity, currency = currency)
                                ReportLineItem(label = "Retained Earnings / Net Profit", amount = netProfit, currency = currency)
                                Divider(color = Slate200)
                                ReportLineItem(label = "TOTAL EQUITY", amount = totalEquity, currency = currency, isBold = true, color = PurplePrimary)

                                Divider(color = NavyDark, thickness = 2.dp)
                                ReportLineItem(
                                    label = "TOTAL LIABILITIES & EQUITY",
                                    amount = totalLiabilities + totalEquity,
                                    currency = currency,
                                    isBold = true,
                                    color = NavyDark
                                )
                            }
                        }
                    }
                    2 -> {
                        // Trial Balance
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Trial Balance", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyDark)

                                var tbTotalDr = 0.0
                                var tbTotalCr = 0.0

                                accounts.forEach { acc ->
                                    val isDr = acc.type in listOf("ASSET", "EXPENSE")
                                    val dr = if (isDr) acc.currentBalance else 0.0
                                    val cr = if (!isDr) acc.currentBalance else 0.0
                                    tbTotalDr += dr
                                    tbTotalCr += cr

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${acc.code} - ${acc.name}", fontSize = 12.sp, modifier = Modifier.weight(1f))
                                        Text(if (dr > 0) formatCurrency(dr, currency) else "-", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                        Text(if (cr > 0) formatCurrency(cr, currency) else "-", fontSize = 12.sp, modifier = Modifier.width(90.dp))
                                    }
                                    Divider(color = Slate100)
                                }

                                Divider(color = NavyDark, thickness = 2.dp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOTALS", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text(formatCurrency(tbTotalDr, currency), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccountingGreen, modifier = Modifier.width(90.dp))
                                    Text(formatCurrency(tbTotalCr, currency), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccountingBlue, modifier = Modifier.width(90.dp))
                                }
                            }
                        }
                    }
                    3 -> {
                        // Aging Summary
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Receivables Aging Summary (Debtors)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoldDark)
                                    customers.filter { it.currentBalance > 0 }.forEach { cust ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(cust.name, fontSize = 13.sp)
                                            Text(formatCurrency(cust.currentBalance, currency), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GoldDark)
                                        }
                                        Divider(color = Slate100)
                                    }
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Payables Aging Summary (Creditors)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccountingRed)
                                    suppliers.filter { it.currentBalance > 0 }.forEach { supp ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(supp.name, fontSize = 13.sp)
                                            Text(formatCurrency(supp.currentBalance, currency), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccountingRed)
                                        }
                                        Divider(color = Slate100)
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

@Composable
fun ReportLineItem(
    label: String,
    amount: Double,
    currency: String,
    isNegative: Boolean = false,
    isBold: Boolean = false,
    color: Color = Slate800
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 14.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) NavyDark else Slate700
        )
        Text(
            text = formatCurrency(kotlin.math.abs(amount), currency),
            fontSize = if (isBold) 14.sp else 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}
