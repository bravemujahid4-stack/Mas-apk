package com.masaccounts.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.masaccounts.app.data.auth.FirebaseAuthManager
import com.masaccounts.app.data.local.AppDatabase
import com.masaccounts.app.data.local.entity.*
import com.masaccounts.app.data.repository.MasRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

enum class DateFilterPeriod(val displayName: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Fiscal Year"),
    ALL_TIME("All Time")
}

data class DashboardMetrics(
    val cashInHand1: Double = 0.0,
    val cashInHand2: Double = 0.0,
    val bankBalance: Double = 0.0,
    val totalReceivables: Double = 0.0,
    val totalPayables: Double = 0.0,
    val inventoryValue: Double = 0.0,
    val totalSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val cogs: Double = 0.0,
    val netProfit: Double = 0.0
)

class MasViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val repository = MasRepository(db)

    // All companies
    val companies: StateFlow<List<CompanyEntity>> = repository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Current active company
    val activeCompany: StateFlow<CompanyEntity?> = repository.getFirstCompany()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Current authenticated user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Filter date period for dashboard & reports
    private val _selectedPeriod = MutableStateFlow(DateFilterPeriod.ALL_TIME)
    val selectedPeriod: StateFlow<DateFilterPeriod> = _selectedPeriod.asStateFlow()

    // Status message / toast / snackbar
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearMessage() { _userMessage.value = null }
    fun clearUserMessage() { _userMessage.value = null }
    fun showMessage(msg: String) { _userMessage.value = msg }

    // Date Filter helpers
    fun setPeriod(period: DateFilterPeriod) {
        _selectedPeriod.value = period
    }

    private fun getPeriodStartTime(period: DateFilterPeriod): Long {
        val cal = Calendar.getInstance()
        return when (period) {
            DateFilterPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            DateFilterPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            DateFilterPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            DateFilterPeriod.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            DateFilterPeriod.ALL_TIME -> 0L
        }
    }

    // Auto-login or set default user on company selection
    init {
        viewModelScope.launch {
            activeCompany.collect { company ->
                if (company != null && _currentUser.value == null) {
                    val user = repository.getUserByEmail("admin@masaccounts.com", company.id)
                    if (user != null) {
                        _currentUser.value = user
                    } else {
                        val firstUser = repository.getUserById(1)
                        if (firstUser != null && firstUser.companyId == company.id) {
                            _currentUser.value = firstUser
                        }
                    }
                }
            }
        }
    }

    fun loginUser(user: UserEntity) {
        _currentUser.value = user
        viewModelScope.launch {
            repository.logAction(user.companyId, user, "LOGIN", "User '${user.name}' logged in.")
        }
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.logAction(user.companyId, user, "LOGOUT", "User '${user.name}' logged out.")
            }
        }
        FirebaseAuthManager.signOut()
        _currentUser.value = null
    }

    // Reactive Data Collections for Active Company
    val accounts: StateFlow<List<AccountEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getAccountsForCompany(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashAndBankAccounts: StateFlow<List<AccountEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getCashAndBankAccounts(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseAccounts: StateFlow<List<AccountEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getExpenseAccounts(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getCustomers(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getSuppliers(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getItems(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getCategories(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val units: StateFlow<List<UnitEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getUnits(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<SaleInvoiceEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getSales(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<PurchaseBillEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getPurchases(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salesReturns: StateFlow<List<SalesReturnEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getSalesReturns(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseReturns: StateFlow<List<PurchaseReturnEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getPurchaseReturns(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentReceipts: StateFlow<List<PaymentReceiptEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getPaymentReceipts(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getExpenses(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntryEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getJournalEntries(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getAllLedgerEntries(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryLots: StateFlow<List<InventoryLotEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getAllLots(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockMovements: StateFlow<List<StockMovementEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getStockMovements(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val users: StateFlow<List<UserEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getUsersForCompany(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = activeCompany.flatMapLatest { comp ->
        if (comp == null) flowOf(emptyList()) else repository.getAuditLogs(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Dashboard Metrics using combine
    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        accounts,
        customers,
        suppliers,
        items,
        sales,
        purchases,
        expenses,
        selectedPeriod
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        val accList = args[0] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val custList = args[1] as List<CustomerEntity>
        @Suppress("UNCHECKED_CAST")
        val suppList = args[2] as List<SupplierEntity>
        @Suppress("UNCHECKED_CAST")
        val itemList = args[3] as List<ItemEntity>
        @Suppress("UNCHECKED_CAST")
        val saleList = args[4] as List<SaleInvoiceEntity>
        @Suppress("UNCHECKED_CAST")
        val purchList = args[5] as List<PurchaseBillEntity>
        @Suppress("UNCHECKED_CAST")
        val expList = args[6] as List<ExpenseEntity>
        val period = args[7] as DateFilterPeriod

        val startTime = getPeriodStartTime(period)

        val cash1 = accList.find { it.code == "1010" }?.currentBalance ?: 0.0
        val cash2 = accList.find { it.code == "1020" }?.currentBalance ?: 0.0
        val bankTotal = accList.filter { it.subType == "BANK" }.sumOf { it.currentBalance }

        val receivables = custList.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
        val payables = suppList.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }

        val invVal = itemList.sumOf { it.currentStock * if (it.purchasePrice > 0) it.purchasePrice else it.openingCost }

        val filteredSales = saleList.filter { it.date >= startTime }
        val totalSales = filteredSales.sumOf { it.totalAmount }
        val totalCogs = filteredSales.sumOf { it.cogsTotal }

        val filteredPurchases = purchList.filter { it.date >= startTime }
        val totalPurchases = filteredPurchases.sumOf { it.totalAmount }

        val filteredExpenses = expList.filter { it.date >= startTime }
        val totalExpenses = filteredExpenses.sumOf { it.amount }

        val netProfit = totalSales - totalCogs - totalExpenses

        DashboardMetrics(
            cashInHand1 = cash1,
            cashInHand2 = cash2,
            bankBalance = bankTotal,
            totalReceivables = receivables,
            totalPayables = payables,
            inventoryValue = invVal,
            totalSales = totalSales,
            totalPurchases = totalPurchases,
            totalExpenses = totalExpenses,
            cogs = totalCogs,
            netProfit = netProfit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())
}
