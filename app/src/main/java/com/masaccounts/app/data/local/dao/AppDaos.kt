package com.masaccounts.app.data.local.dao

import androidx.room.*
import com.masaccounts.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY id DESC")
    fun getAllCompanies(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE id = :id LIMIT 1")
    suspend fun getCompanyById(id: Long): CompanyEntity?

    @Query("SELECT * FROM companies ORDER BY id ASC LIMIT 1")
    fun getFirstCompany(): Flow<CompanyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: CompanyEntity): Long

    @Update
    suspend fun updateCompany(company: CompanyEntity)

    @Delete
    suspend fun deleteCompany(company: CompanyEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE companyId = :companyId ORDER BY name ASC")
    fun getUsersForCompany(companyId: Long): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE email = :email AND companyId = :companyId LIMIT 1")
    suspend fun getUserByEmail(email: String, companyId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmailAnyCompany(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE companyId = :companyId ORDER BY code ASC")
    fun getAccountsForCompany(companyId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE companyId = :companyId AND subType IN ('CASH', 'BANK') ORDER BY code ASC")
    fun getCashAndBankAccounts(companyId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE companyId = :companyId AND type = 'EXPENSE' ORDER BY code ASC")
    fun getExpenseAccounts(companyId: Long): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE companyId = :companyId AND code = :code LIMIT 1")
    suspend fun getAccountByCode(companyId: Long, code: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)
}

@Dao
interface PartyDao {
    @Query("SELECT * FROM customers WHERE companyId = :companyId ORDER BY name ASC")
    fun getCustomers(companyId: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM suppliers WHERE companyId = :companyId ORDER BY name ASC")
    fun getSuppliers(companyId: Long): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Delete
    suspend fun deleteSupplier(supplier: SupplierEntity)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM categories WHERE companyId = :companyId ORDER BY name ASC")
    fun getCategories(companyId: Long): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM units WHERE companyId = :companyId ORDER BY name ASC")
    fun getUnits(companyId: Long): Flow<List<UnitEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnit(unit: UnitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnits(units: List<UnitEntity>)

    @Query("SELECT * FROM items WHERE companyId = :companyId ORDER BY name ASC")
    fun getItems(companyId: Long): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    // FIFO Lots
    @Query("SELECT * FROM inventory_lots WHERE companyId = :companyId AND itemId = :itemId AND remainingQuantity > 0 ORDER BY purchaseDate ASC, id ASC")
    suspend fun getAvailableLotsForItemFifo(companyId: Long, itemId: Long): List<InventoryLotEntity>

    @Query("SELECT * FROM inventory_lots WHERE companyId = :companyId ORDER BY purchaseDate DESC")
    fun getAllLots(companyId: Long): Flow<List<InventoryLotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLot(lot: InventoryLotEntity): Long

    @Update
    suspend fun updateLot(lot: InventoryLotEntity)

    @Query("SELECT * FROM stock_movements WHERE companyId = :companyId ORDER BY date DESC")
    fun getStockMovements(companyId: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity): Long
}

@Dao
interface TransactionDao {
    // Sales
    @Query("SELECT * FROM sale_invoices WHERE companyId = :companyId ORDER BY date DESC, id DESC")
    fun getSales(companyId: Long): Flow<List<SaleInvoiceEntity>>

    @Query("SELECT * FROM sale_invoices WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleInvoiceEntity?

    @Query("SELECT * FROM sale_items WHERE invoiceId = :invoiceId")
    suspend fun getSaleItems(invoiceId: Long): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleInvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    // Sales Returns
    @Query("SELECT * FROM sales_returns WHERE companyId = :companyId ORDER BY date DESC")
    fun getSalesReturns(companyId: Long): Flow<List<SalesReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesReturn(salesReturn: SalesReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalesReturnItems(items: List<SalesReturnItemEntity>)

    // Purchases
    @Query("SELECT * FROM purchase_bills WHERE companyId = :companyId ORDER BY date DESC, id DESC")
    fun getPurchases(companyId: Long): Flow<List<PurchaseBillEntity>>

    @Query("SELECT * FROM purchase_bills WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: Long): PurchaseBillEntity?

    @Query("SELECT * FROM purchase_items WHERE billId = :billId")
    suspend fun getPurchaseItems(billId: Long): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(bill: PurchaseBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    // Purchase Returns
    @Query("SELECT * FROM purchase_returns WHERE companyId = :companyId ORDER BY date DESC")
    fun getPurchaseReturns(companyId: Long): Flow<List<PurchaseReturnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseReturn(purchaseReturn: PurchaseReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseReturnItems(items: List<PurchaseReturnItemEntity>)

    // Payment Receipts
    @Query("SELECT * FROM payment_receipts WHERE companyId = :companyId ORDER BY date DESC, id DESC")
    fun getPaymentReceipts(companyId: Long): Flow<List<PaymentReceiptEntity>>

    @Query("SELECT * FROM payment_receipts WHERE companyId = :companyId AND type = :type ORDER BY date DESC")
    fun getPaymentReceiptsByType(companyId: Long, type: String): Flow<List<PaymentReceiptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentReceipt(voucher: PaymentReceiptEntity): Long

    // Expenses
    @Query("SELECT * FROM expenses WHERE companyId = :companyId ORDER BY date DESC, id DESC")
    fun getExpenses(companyId: Long): Flow<List<ExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    // Journal Entries
    @Query("SELECT * FROM journal_entries WHERE companyId = :companyId ORDER BY date DESC, id DESC")
    fun getJournalEntries(companyId: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_lines WHERE journalId = :journalId")
    suspend fun getJournalLines(journalId: Long): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    // Ledgers
    @Query("SELECT * FROM ledger_entries WHERE companyId = :companyId ORDER BY date ASC, id ASC")
    fun getAllLedgerEntries(companyId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE companyId = :companyId AND accountId = :accountId ORDER BY date ASC, id ASC")
    fun getLedgerEntriesForAccount(companyId: Long, accountId: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE companyId = :companyId AND partyType = :partyType AND partyId = :partyId ORDER BY date ASC, id ASC")
    fun getLedgerEntriesForParty(companyId: Long, partyType: String, partyId: Long): Flow<List<LedgerEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: LedgerEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<LedgerEntryEntity>)

    // Audit Logs
    @Query("SELECT * FROM audit_logs WHERE companyId = :companyId ORDER BY timestamp DESC")
    fun getAuditLogs(companyId: Long): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}
