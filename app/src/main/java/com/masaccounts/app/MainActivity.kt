package com.masaccounts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.masaccounts.app.ui.MasViewModel
import com.masaccounts.app.ui.components.MasBottomNavigation
import com.masaccounts.app.ui.screens.accounts.CashAndBankScreen
import com.masaccounts.app.ui.screens.accounts.ChartOfAccountsScreen
import com.masaccounts.app.ui.screens.auth.LoginScreen
import com.masaccounts.app.ui.screens.company.CompanySetupScreen
import com.masaccounts.app.ui.screens.company.CreateCompanyScreen
import com.masaccounts.app.ui.screens.customers.CustomersScreen
import com.masaccounts.app.ui.screens.dashboard.DashboardScreen
import com.masaccounts.app.ui.screens.inventory.InventoryScreen
import com.masaccounts.app.ui.screens.journal.CreateJournalEntryScreen
import com.masaccounts.app.ui.screens.journal.JournalEntriesScreen
import com.masaccounts.app.ui.screens.ledger.LedgerRowWithBalance
import com.masaccounts.app.ui.screens.ledger.LedgerScreen
import com.masaccounts.app.ui.screens.print.PrintStatementDialog
import com.masaccounts.app.ui.screens.purchases.CreatePurchaseBillScreen
import com.masaccounts.app.ui.screens.purchases.PurchasesScreen
import com.masaccounts.app.ui.screens.reports.ReportsScreen
import com.masaccounts.app.ui.screens.returns.ReturnsScreen
import com.masaccounts.app.ui.screens.sales.CreateSaleInvoiceScreen
import com.masaccounts.app.ui.screens.sales.SalesScreen
import com.masaccounts.app.ui.screens.settings.SettingsScreen
import com.masaccounts.app.ui.screens.suppliers.SuppliersScreen
import com.masaccounts.app.ui.screens.vouchers.CustomerReceiptScreen
import com.masaccounts.app.ui.screens.vouchers.ExpenseScreen
import com.masaccounts.app.ui.screens.vouchers.SupplierPaymentScreen
import com.masaccounts.app.ui.theme.MasAccountsTheme

object Routes {
    const val CREATE_COMPANY = "create_company"
    const val COMPANY_SETUP = "company_setup"
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CUSTOMERS = "customers"
    const val SUPPLIERS = "suppliers"
    const val INVENTORY = "inventory"
    const val SALES = "sales"
    const val CREATE_SALE = "create_sale"
    const val PURCHASES = "purchases"
    const val CREATE_PURCHASE = "create_purchase"
    const val RECEIPT_VOUCHER = "receipt_voucher"
    const val PAYMENT_VOUCHER = "payment_voucher"
    const val EXPENSES = "expenses"
    const val JOURNAL_ENTRIES = "journal_entries"
    const val CREATE_JOURNAL = "create_journal"
    const val LEDGER = "ledger"
    const val CHART_OF_ACCOUNTS = "chart_of_accounts"
    const val CASH_BANK = "cash_bank"
    const val REPORTS = "reports"
    const val RETURNS = "returns"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    private val viewModel: MasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MasAccountsTheme {
                MasAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MasAppContent(viewModel: MasViewModel) {
    val navController = rememberNavController()
    val companies by viewModel.companies.collectAsState()
    val activeCompany by viewModel.activeCompany.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val message by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var printDialogData by remember {
        mutableStateOf<Pair<List<LedgerRowWithBalance>, String>?>(null)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Determine initial destination
    val startDestination = remember(companies, activeCompany, currentUser) {
        when {
            companies.isEmpty() -> Routes.CREATE_COMPANY
            currentUser == null -> Routes.LOGIN
            else -> Routes.DASHBOARD
        }
    }

    // Keep navigation in sync if company count or login status changes
    LaunchedEffect(companies, currentUser) {
        if (companies.isEmpty()) {
            navController.navigate(Routes.CREATE_COMPANY) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser == null && navController.currentDestination?.route != Routes.CREATE_COMPANY) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD,
        Routes.SALES,
        Routes.PURCHASES,
        Routes.CUSTOMERS,
        Routes.SUPPLIERS
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar && currentUser != null) {
                MasBottomNavigation(
                    currentRoute = currentRoute ?: Routes.DASHBOARD,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(Routes.DASHBOARD) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.CREATE_COMPANY) {
                CreateCompanyScreen(
                    viewModel = viewModel,
                    onCompanyCreated = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.CREATE_COMPANY) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.COMPANY_SETUP) {
                CompanySetupScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onCreateNewCompanyClick = {
                        navController.navigate(Routes.CREATE_COMPANY)
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRoute = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.CUSTOMERS) {
                CustomersScreen(
                    viewModel = viewModel,
                    onNavigateToStatement = { customerId ->
                        navController.navigate("${Routes.LEDGER}?partyType=CUSTOMER&partyId=$customerId")
                    }
                )
            }

            composable(Routes.SUPPLIERS) {
                SuppliersScreen(
                    viewModel = viewModel,
                    onNavigateToStatement = { supplierId ->
                        navController.navigate("${Routes.LEDGER}?partyType=SUPPLIER&partyId=$supplierId")
                    }
                )
            }

            composable(Routes.INVENTORY) {
                InventoryScreen(
                    viewModel = viewModel
                )
            }

            composable(Routes.SALES) {
                SalesScreen(
                    viewModel = viewModel,
                    onCreateSaleClick = { navController.navigate(Routes.CREATE_SALE) },
                    onViewSaleDetails = { saleId ->
                        // View sale details
                    }
                )
            }

            composable(Routes.CREATE_SALE) {
                CreateSaleInvoiceScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaleCreated = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.PURCHASES) {
                PurchasesScreen(
                    viewModel = viewModel,
                    onCreatePurchaseClick = { navController.navigate(Routes.CREATE_PURCHASE) },
                    onViewPurchaseDetails = { purchaseId ->
                        // View purchase details
                    }
                )
            }

            composable(Routes.CREATE_PURCHASE) {
                CreatePurchaseBillScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onPurchaseCreated = { _ ->
                        navController.popBackStack()
                    }
                )
            }

            composable(Routes.RECEIPT_VOUCHER) {
                CustomerReceiptScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.PAYMENT_VOUCHER) {
                SupplierPaymentScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.EXPENSES) {
                ExpenseScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.JOURNAL_ENTRIES) {
                JournalEntriesScreen(
                    viewModel = viewModel,
                    onCreateJournalClick = { navController.navigate(Routes.CREATE_JOURNAL) },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.CREATE_JOURNAL) {
                CreateJournalEntryScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onJournalCreated = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Routes.LEDGER}?accountId={accountId}&partyType={partyType}&partyId={partyId}",
                arguments = listOf(
                    navArgument("accountId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("partyType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("partyId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val accId = backStackEntry.arguments?.getString("accountId")?.toLongOrNull()
                val partyType = backStackEntry.arguments?.getString("partyType")
                val partyId = backStackEntry.arguments?.getString("partyId")?.toLongOrNull()

                LedgerScreen(
                    viewModel = viewModel,
                    initialAccountId = accId,
                    initialPartyType = partyType,
                    initialPartyId = partyId,
                    onBackClick = { navController.popBackStack() },
                    onPrintStatement = { rows, title ->
                        printDialogData = Pair(rows, title)
                    }
                )
            }

            composable(Routes.CHART_OF_ACCOUNTS) {
                ChartOfAccountsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onViewAccountLedger = { accountId ->
                        navController.navigate("${Routes.LEDGER}?accountId=$accountId")
                    }
                )
            }

            composable(Routes.CASH_BANK) {
                CashAndBankScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onViewAccountLedger = { accountId ->
                        navController.navigate("${Routes.LEDGER}?accountId=$accountId")
                    }
                )
            }

            composable(Routes.REPORTS) {
                ReportsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.RETURNS) {
                ReturnsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToCompanySetup = { navController.navigate(Routes.COMPANY_SETUP) },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }

    printDialogData?.let { (rows, title) ->
        PrintStatementDialog(
            company = activeCompany,
            title = title,
            rows = rows,
            currency = activeCompany?.currency ?: "PKR",
            onDismiss = { printDialogData = null }
        )
    }
}
