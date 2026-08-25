package com.masaccounts.app.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.masaccounts.app.R
import com.masaccounts.app.ui.DateFilterPeriod
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.*
import com.masaccounts.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MasViewModel,
    onNavigateToRoute: (String) -> Unit,
    onQuickAction: (String) -> Unit = onNavigateToRoute
) {
    val company by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    val metrics by viewModel.dashboardMetrics.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    val currency = company?.currency ?: "PKR"

    Scaffold(
        topBar = {
            MasTopAppBar(
                title = "Executive Dashboard",
                company = company,
                user = currentUser,
                actions = {
                    IconButton(onClick = { onNavigateToRoute("reports") }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Reports", tint = Color.White)
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
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Executive Summary Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(NavyDark, NavyPrimary)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET PROFIT (${selectedPeriod.displayName.uppercase()})",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = formatCurrency(metrics.netProfit, currency),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (metrics.netProfit >= 0) Color.White else AccountingRedLight
                                )
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = if (metrics.netProfit >= 0) AccountingGreen.copy(alpha = 0.25f) else AccountingRed.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, if (metrics.netProfit >= 0) AccountingGreen else AccountingRed)
                        ) {
                            Icon(
                                imageVector = if (metrics.netProfit >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (metrics.netProfit >= 0) Color(0xFF4ADE80) else AccountingRedLight,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Date Period Filter Horizontal Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DateFilterPeriod.values()) { period ->
                            val isSelected = selectedPeriod == period
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) GoldPrimary else NavyDark.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, if (isSelected) GoldLight else Slate600),
                                modifier = Modifier
                                    .clickable { viewModel.setPeriod(period) }
                                    .testTag("period_chip_${period.name}")
                            ) {
                                Text(
                                    text = period.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) NavyDark else Color.White
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Hub
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    ),
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        QuickActionItem(
                            icon = Icons.Default.PointOfSale,
                            label = "+ Sale",
                            color = AccountingGreen,
                            onClick = { onQuickAction("new_sale") }
                        )
                    }
                    item {
                        QuickActionItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "+ Purchase",
                            color = AccountingBlue,
                            onClick = { onQuickAction("new_purchase") }
                        )
                    }
                    item {
                        QuickActionItem(
                            icon = Icons.Default.CallReceived,
                            label = "Receipt",
                            color = GoldDark,
                            onClick = { onQuickAction("new_receipt") }
                        )
                    }
                    item {
                        QuickActionItem(
                            icon = Icons.Default.CallMade,
                            label = "Payment",
                            color = AccountingRed,
                            onClick = { onQuickAction("new_payment") }
                        )
                    }
                    item {
                        QuickActionItem(
                            icon = Icons.Default.ReceiptLong,
                            label = "Expense",
                            color = PurplePrimary,
                            onClick = { onQuickAction("new_expense") }
                        )
                    }
                    item {
                        QuickActionItem(
                            icon = Icons.Default.AccountBalance,
                            label = "Journal",
                            color = NavyPrimary,
                            onClick = { onQuickAction("new_journal") }
                        )
                    }
                }
            }

            // Core Financial Position (Liquidity & Working Capital)
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cash & Bank Balances",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Cash in Hand 1",
                        amount = metrics.cashInHand1,
                        currency = currency,
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = AccountingGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("cash_bank") }
                    )
                    MetricCard(
                        title = "Cash in Hand 2",
                        amount = metrics.cashInHand2,
                        currency = currency,
                        icon = Icons.Default.Payments,
                        accentColor = AccountingGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("cash_bank") }
                    )
                }

                MetricCard(
                    title = "Total Bank Balance",
                    amount = metrics.bankBalance,
                    currency = currency,
                    icon = Icons.Default.AccountBalance,
                    accentColor = AccountingBlue,
                    onClick = { onNavigateToRoute("cash_bank") }
                )

                Text(
                    text = "Receivables, Payables & Inventory",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Customer Receivables",
                        amount = metrics.totalReceivables,
                        currency = currency,
                        icon = Icons.Default.People,
                        accentColor = GoldDark,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("customers") }
                    )
                    MetricCard(
                        title = "Supplier Payables",
                        amount = metrics.totalPayables,
                        currency = currency,
                        icon = Icons.Default.LocalShipping,
                        accentColor = AccountingRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("purchases") }
                    )
                }

                MetricCard(
                    title = "Stock Inventory Value",
                    amount = metrics.inventoryValue,
                    currency = currency,
                    icon = Icons.Default.Inventory2,
                    accentColor = PurplePrimary,
                    onClick = { onNavigateToRoute("inventory") }
                )

                Text(
                    text = "Trading Performance (${selectedPeriod.displayName})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Sales",
                        amount = metrics.totalSales,
                        currency = currency,
                        icon = Icons.Default.PointOfSale,
                        accentColor = AccountingGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("sales") }
                    )
                    MetricCard(
                        title = "Total Purchases",
                        amount = metrics.totalPurchases,
                        currency = currency,
                        icon = Icons.Default.ShoppingBag,
                        accentColor = AccountingBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("purchases") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Cost of Goods Sold (COGS)",
                        amount = metrics.cogs,
                        currency = currency,
                        icon = Icons.Default.Receipt,
                        accentColor = Slate600,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Operating Expenses",
                        amount = metrics.totalExpenses,
                        currency = currency,
                        icon = Icons.Default.ReceiptLong,
                        accentColor = AccountingRed,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToRoute("expenses") }
                    )
                }
            }

            // Recent Transactions
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Sales Invoices",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                    )
                    TextButton(onClick = { onNavigateToRoute("sales") }) {
                        Text("View All", color = NavyPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (sales.isEmpty()) {
                    EmptyStateCard(
                        title = "No Sales Yet",
                        subtitle = "Create your first sales invoice to begin recording business revenue.",
                        icon = Icons.Default.PointOfSale,
                        actionButtonText = "+ New Sale",
                        onActionClick = { onQuickAction("new_sale") }
                    )
                } else {
                    sales.take(4).forEach { sale ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onNavigateToRoute("sales") }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
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
                                            modifier = Modifier.size(20.dp)
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
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatCurrency(sale.totalAmount, currency),
                                        fontWeight = FontWeight.Bold,
                                        color = AccountingGreen,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatDate(sale.date),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .width(88.dp)
            .clickable { onClick() }
            .testTag("quick_action_$label")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = NavyDark,
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}
