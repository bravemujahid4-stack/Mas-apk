package com.masaccounts.app.data.repository

import com.masaccounts.app.data.local.AppDatabase
import com.masaccounts.app.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MasRepository(private val db: AppDatabase) {

    // --- Company & Setup ---
    fun getAllCompanies(): Flow<List<CompanyEntity>> = db.companyDao().getAllCompanies()
    fun getFirstCompany(): Flow<CompanyEntity?> = db.companyDao().getFirstCompany()
    suspend fun getCompanyById(id: Long): CompanyEntity? = db.companyDao().getCompanyById(id)

    suspend fun createCompany(
        company: CompanyEntity,
        adminName: String = "Admin",
        adminEmail: String = "admin@masaccounts.com",
        adminPassword: String = "admin123"
    ): Long = withContext(Dispatchers.IO) {
        val companyId = db.companyDao().insertCompany(company)

        // Initialize standard Pakistani double-entry Chart of Accounts for this company
        val defaultAccounts = listOf(
            AccountEntity(companyId = companyId, code = "1010", name = "Cash in Hand 1", type = "ASSET", subType = "CASH", isSystem = true),
            AccountEntity(companyId = companyId, code = "1020", name = "Cash in Hand 2", type = "ASSET", subType = "CASH", isSystem = true),
            AccountEntity(companyId = companyId, code = "1030", name = "Bank Account", type = "ASSET", subType = "BANK", isSystem = true, bankName = "Main Bank", accountNumber = "0000-000000-000"),
            AccountEntity(companyId = companyId, code = "1040", name = "Accounts Receivable (Debtors)", type = "ASSET", subType = "RECEIVABLE", isSystem = true),
            AccountEntity(companyId = companyId, code = "1050", name = "Inventory Asset (Stock)", type = "ASSET", subType = "INVENTORY", isSystem = true),
            AccountEntity(companyId = companyId, code = "2010", name = "Accounts Payable (Creditors)", type = "LIABILITY", subType = "PAYABLE", isSystem = true),
            AccountEntity(companyId = companyId, code = "2020", name = "Sales Tax Payable", type = "LIABILITY", subType = "OTHER", isSystem = true),
            AccountEntity(companyId = companyId, code = "3010", name = "Owner's Capital", type = "EQUITY", subType = "CAPITAL", isSystem = true),
            AccountEntity(companyId = companyId, code = "3020", name = "Retained Earnings", type = "EQUITY", subType = "RETAINED_EARNINGS", isSystem = true),
            AccountEntity(companyId = companyId, code = "4010", name = "Sales Revenue", type = "REVENUE", subType = "SALES", isSystem = true),
            AccountEntity(companyId = companyId, code = "4020", name = "Sales Return & Allowances", type = "REVENUE", subType = "SALES", isSystem = true),
            AccountEntity(companyId = companyId, code = "5010", name = "Cost of Goods Sold (COGS)", type = "EXPENSE", subType = "COGS", isSystem = true),
            AccountEntity(companyId = companyId, code = "5020", name = "Operating Expenses", type = "EXPENSE", subType = "OPERATING_EXPENSE", isSystem = true),
            AccountEntity(companyId = companyId, code = "5030", name = "Purchase Return & Allowances", type = "EXPENSE", subType = "OTHER", isSystem = true),
            AccountEntity(companyId = companyId, code = "5040", name = "Salaries & Wages", type = "EXPENSE", subType = "OPERATING_EXPENSE", isSystem = false),
            AccountEntity(companyId = companyId, code = "5050", name = "Shop / Factory Rent", type = "EXPENSE", subType = "OPERATING_EXPENSE", isSystem = false),
            AccountEntity(companyId = companyId, code = "5060", name = "Electricity & Utilities", type = "EXPENSE", subType = "OPERATING_EXPENSE", isSystem = false),
            AccountEntity(companyId = companyId, code = "5070", name = "Freight & Carriage", type = "EXPENSE", subType = "OPERATING_EXPENSE", isSystem = false)
        )
        db.accountDao().insertAccounts(defaultAccounts)

        // Initialize default Category
        db.inventoryDao().insertCategory(CategoryEntity(companyId = companyId, name = "Iron"))

        // Initialize default Units of Measure (KG primary for iron)
        val defaultUnits = listOf(
            UnitEntity(companyId = companyId, code = "KG", name = "Kilogram"),
            UnitEntity(companyId = companyId, code = "Ton", name = "Metric Ton"),
            UnitEntity(companyId = companyId, code = "Piece", name = "Piece"),
            UnitEntity(companyId = companyId, code = "Meter", name = "Meter"),
            UnitEntity(companyId = companyId, code = "Foot", name = "Foot"),
            UnitEntity(companyId = companyId, code = "Bag", name = "Bag")
        )
        db.inventoryDao().insertUnits(defaultUnits)

        // Create Admin user
        db.userDao().insertUser(
            UserEntity(
                companyId = companyId,
                name = adminName,
                email = adminEmail.trim().lowercase(),
                passwordHash = adminPassword,
                role = "ADMIN"
            )
        )

        // Audit log
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = companyId,
                userEmail = adminEmail,
                userName = adminName,
                action = "CREATE_COMPANY",
                details = "Company '${company.name}' created with standard chart of accounts."
            )
        )

        companyId
    }

    suspend fun updateCompany(company: CompanyEntity, user: UserEntity) = withContext(Dispatchers.IO) {
        db.companyDao().updateCompany(company)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = company.id,
                userEmail = user.email,
                userName = user.name,
                action = "UPDATE_COMPANY",
                details = "Company settings updated. Locked status: ${company.isLocked}"
            )
        )
    }

    // --- Users & Authentication ---
    fun getUsersForCompany(companyId: Long): Flow<List<UserEntity>> = db.userDao().getUsersForCompany(companyId)
    suspend fun getUserByEmail(email: String, companyId: Long): UserEntity? = db.userDao().getUserByEmail(email.trim().lowercase(), companyId)
    suspend fun getUserByEmailAnyCompany(email: String): UserEntity? = db.userDao().getUserByEmailAnyCompany(email.trim().lowercase())
    suspend fun getUserById(id: Long): UserEntity? = db.userDao().getUserById(id)

    suspend fun createUser(user: UserEntity, admin: UserEntity): Long = withContext(Dispatchers.IO) {
        val id = db.userDao().insertUser(user.copy(email = user.email.trim().lowercase()))
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = user.companyId,
                userEmail = admin.email,
                userName = admin.name,
                action = "CREATE_USER",
                details = "Created user '${user.name}' (${user.email}) with role ${user.role}."
            )
        )
        id
    }

    suspend fun updateUser(user: UserEntity, admin: UserEntity) = withContext(Dispatchers.IO) {
        db.userDao().updateUser(user)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = user.companyId,
                userEmail = admin.email,
                userName = admin.name,
                action = "UPDATE_USER",
                details = "Updated user '${user.name}' (${user.email}) role/status."
            )
        )
    }

    suspend fun logAction(companyId: Long, user: UserEntity, action: String, details: String) = withContext(Dispatchers.IO) {
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = companyId,
                userEmail = user.email,
                userName = user.name,
                action = action,
                details = details
            )
        )
    }

    // --- Chart of Accounts ---
    fun getAccountsForCompany(companyId: Long): Flow<List<AccountEntity>> = db.accountDao().getAccountsForCompany(companyId)
    fun getCashAndBankAccounts(companyId: Long): Flow<List<AccountEntity>> = db.accountDao().getCashAndBankAccounts(companyId)
    fun getExpenseAccounts(companyId: Long): Flow<List<AccountEntity>> = db.accountDao().getExpenseAccounts(companyId)
    suspend fun getAccountById(id: Long): AccountEntity? = db.accountDao().getAccountById(id)
    suspend fun getAccountByCode(companyId: Long, code: String): AccountEntity? = db.accountDao().getAccountByCode(companyId, code)

    suspend fun createAccount(account: AccountEntity, user: UserEntity): Long = withContext(Dispatchers.IO) {
        val id = db.accountDao().insertAccount(account.copy(currentBalance = account.openingBalance))
        if (account.openingBalance != 0.0) {
            val isDebit = account.openingBalanceType == "DEBIT"
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = account.companyId,
                    date = System.currentTimeMillis(),
                    accountId = id,
                    accountCode = account.code,
                    accountName = account.name,
                    voucherType = "OPENING_BALANCE",
                    voucherNumber = "OP-${account.code}",
                    description = "Opening balance for ${account.name}",
                    debit = if (isDebit) account.openingBalance else 0.0,
                    credit = if (!isDebit) account.openingBalance else 0.0
                )
            )
        }
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = account.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_ACCOUNT",
                details = "Created account '${account.name}' (${account.code}) Type: ${account.type}"
            )
        )
        id
    }

    suspend fun updateAccount(account: AccountEntity, user: UserEntity) = withContext(Dispatchers.IO) {
        db.accountDao().updateAccount(account)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = account.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "UPDATE_ACCOUNT",
                details = "Updated account '${account.name}' (${account.code})"
            )
        )
    }

    // --- Customers ---
    fun getCustomers(companyId: Long): Flow<List<CustomerEntity>> = db.partyDao().getCustomers(companyId)
    suspend fun getCustomerById(id: Long): CustomerEntity? = db.partyDao().getCustomerById(id)

    suspend fun createCustomer(customer: CustomerEntity, user: UserEntity): Long = withContext(Dispatchers.IO) {
        val initialBalance = if (customer.openingBalanceType == "DEBIT") customer.openingBalance else -customer.openingBalance
        val customerToSave = customer.copy(currentBalance = initialBalance)
        val id = db.partyDao().insertCustomer(customerToSave)

        if (customer.openingBalance != 0.0) {
            val arAccount = db.accountDao().getAccountByCode(customer.companyId, "1040")
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = customer.companyId,
                    date = System.currentTimeMillis(),
                    accountId = arAccount?.id ?: 0L,
                    accountCode = arAccount?.code ?: "1040",
                    accountName = arAccount?.name ?: "Accounts Receivable",
                    partyType = "CUSTOMER",
                    partyId = id,
                    partyName = customer.name,
                    voucherType = "OPENING_BALANCE",
                    voucherNumber = "OP-CUST-$id",
                    description = "Opening balance for ${customer.name}",
                    debit = if (customer.openingBalanceType == "DEBIT") customer.openingBalance else 0.0,
                    credit = if (customer.openingBalanceType == "CREDIT") customer.openingBalance else 0.0
                )
            )
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = customer.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_CUSTOMER",
                details = "Created customer '${customer.name}' with code '${customer.code}'"
            )
        )
        id
    }

    suspend fun updateCustomer(customer: CustomerEntity, user: UserEntity) = withContext(Dispatchers.IO) {
        db.partyDao().updateCustomer(customer)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = customer.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "UPDATE_CUSTOMER",
                details = "Updated customer '${customer.name}' (${customer.code})"
            )
        )
    }

    // --- Suppliers ---
    fun getSuppliers(companyId: Long): Flow<List<SupplierEntity>> = db.partyDao().getSuppliers(companyId)
    suspend fun getSupplierById(id: Long): SupplierEntity? = db.partyDao().getSupplierById(id)

    suspend fun createSupplier(supplier: SupplierEntity, user: UserEntity): Long = withContext(Dispatchers.IO) {
        val initialBalance = if (supplier.openingBalanceType == "CREDIT") supplier.openingBalance else -supplier.openingBalance
        val supplierToSave = supplier.copy(currentBalance = initialBalance)
        val id = db.partyDao().insertSupplier(supplierToSave)

        if (supplier.openingBalance != 0.0) {
            val apAccount = db.accountDao().getAccountByCode(supplier.companyId, "2010")
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = supplier.companyId,
                    date = System.currentTimeMillis(),
                    accountId = apAccount?.id ?: 0L,
                    accountCode = apAccount?.code ?: "2010",
                    accountName = apAccount?.name ?: "Accounts Payable",
                    partyType = "SUPPLIER",
                    partyId = id,
                    partyName = supplier.name,
                    voucherType = "OPENING_BALANCE",
                    voucherNumber = "OP-SUP-$id",
                    description = "Opening balance for ${supplier.name}",
                    debit = if (supplier.openingBalanceType == "DEBIT") supplier.openingBalance else 0.0,
                    credit = if (supplier.openingBalanceType == "CREDIT") supplier.openingBalance else 0.0
                )
            )
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = supplier.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_SUPPLIER",
                details = "Created supplier '${supplier.name}' with code '${supplier.code}'"
            )
        )
        id
    }

    suspend fun updateSupplier(supplier: SupplierEntity, user: UserEntity) = withContext(Dispatchers.IO) {
        db.partyDao().updateSupplier(supplier)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = supplier.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "UPDATE_SUPPLIER",
                details = "Updated supplier '${supplier.name}' (${supplier.code})"
            )
        )
    }

    // --- Inventory, Categories & Units ---
    fun getCategories(companyId: Long): Flow<List<CategoryEntity>> = db.inventoryDao().getCategories(companyId)
    suspend fun addCategory(category: CategoryEntity) = db.inventoryDao().insertCategory(category)

    fun getUnits(companyId: Long): Flow<List<UnitEntity>> = db.inventoryDao().getUnits(companyId)
    suspend fun addUnit(unit: UnitEntity) = db.inventoryDao().insertUnit(unit)

    fun getItems(companyId: Long): Flow<List<ItemEntity>> = db.inventoryDao().getItems(companyId)
    suspend fun getItemById(id: Long): ItemEntity? = db.inventoryDao().getItemById(id)

    suspend fun createItem(item: ItemEntity, user: UserEntity): Long = withContext(Dispatchers.IO) {
        val initialStock = item.openingQuantity
        val itemToSave = item.copy(currentStock = initialStock)
        val itemId = db.inventoryDao().insertItem(itemToSave)

        if (item.openingQuantity > 0) {
            // Create initial FIFO inventory lot
            db.inventoryDao().insertLot(
                InventoryLotEntity(
                    companyId = item.companyId,
                    lotNumber = "LOT-OP-$itemId",
                    itemId = itemId,
                    itemName = item.name,
                    purchaseDate = System.currentTimeMillis(),
                    initialQuantity = item.openingQuantity,
                    remainingQuantity = item.openingQuantity,
                    unitCost = item.openingCost,
                    referenceType = "OPENING_STOCK"
                )
            )

            // Stock movement
            db.inventoryDao().insertStockMovement(
                StockMovementEntity(
                    companyId = item.companyId,
                    itemId = itemId,
                    itemName = item.name,
                    date = System.currentTimeMillis(),
                    movementType = "OPENING",
                    quantity = item.openingQuantity,
                    unitCost = item.openingCost,
                    totalCost = item.openingQuantity * item.openingCost,
                    referenceType = "OPENING",
                    referenceNumber = "OP-STOCK-$itemId",
                    notes = "Opening stock for ${item.name}"
                )
            )

            // Ledger entry
            val invAsset = db.accountDao().getAccountByCode(item.companyId, "1050")
            val equity = db.accountDao().getAccountByCode(item.companyId, "3010")
            val totalValue = item.openingQuantity * item.openingCost
            if (totalValue > 0) {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = item.companyId,
                        date = System.currentTimeMillis(),
                        accountId = invAsset?.id ?: 0L,
                        accountCode = invAsset?.code ?: "1050",
                        accountName = invAsset?.name ?: "Inventory Asset",
                        voucherType = "OPENING_BALANCE",
                        voucherNumber = "OP-INV-$itemId",
                        description = "Opening inventory for ${item.name} (${item.openingQuantity} ${item.unit} @ ${item.openingCost})",
                        debit = totalValue,
                        credit = 0.0
                    )
                )
                if (equity != null) {
                    db.accountDao().updateAccount(equity.copy(currentBalance = equity.currentBalance + totalValue))
                }
                if (invAsset != null) {
                    db.accountDao().updateAccount(invAsset.copy(currentBalance = invAsset.currentBalance + totalValue))
                }
            }
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = item.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_ITEM",
                details = "Created item '${item.name}' (${item.code}) with opening qty ${item.openingQuantity} ${item.unit}"
            )
        )
        itemId
    }

    suspend fun updateItem(item: ItemEntity, user: UserEntity) = withContext(Dispatchers.IO) {
        db.inventoryDao().updateItem(item)
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = item.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "UPDATE_ITEM",
                details = "Updated item '${item.name}' (${item.code})"
            )
        )
    }

    fun getAllLots(companyId: Long): Flow<List<InventoryLotEntity>> = db.inventoryDao().getAllLots(companyId)
    fun getStockMovements(companyId: Long): Flow<List<StockMovementEntity>> = db.inventoryDao().getStockMovements(companyId)

    // --- Sales with FIFO COGS and Credit/Cash Rules ---
    fun getSales(companyId: Long): Flow<List<SaleInvoiceEntity>> = db.transactionDao().getSales(companyId)
    suspend fun getSaleById(id: Long): SaleInvoiceEntity? = db.transactionDao().getSaleById(id)
    suspend fun getSaleItems(invoiceId: Long): List<SaleItemEntity> = db.transactionDao().getSaleItems(invoiceId)

    suspend fun createSaleInvoice(
        sale: SaleInvoiceEntity,
        items: List<SaleItemEntity>,
        user: UserEntity
    ): Result<Long> = withContext(Dispatchers.IO) {
        // Validation Rule:
        // CASH SALE: Customer selection is OPTIONAL. Cash account is required.
        // CREDIT SALE: Customer selection is REQUIRED.
        if (sale.paymentType == "CREDIT") {
            if (sale.customerId == null || sale.customerId <= 0L || sale.customerName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Customer is required for credit sales."))
            }
        } else if (sale.paymentType == "CASH") {
            if (sale.cashAccountId == null || sale.cashAccountId <= 0L) {
                return@withContext Result.failure(IllegalArgumentException("Please select a Cash/Bank account for cash sale."))
            }
        }

        // Calculate FIFO COGS for each item & consume lots
        var totalInvoiceCogs = 0.0
        val processedItems = mutableListOf<SaleItemEntity>()

        for (item in items) {
            var qtyRemainingToConsume = item.quantity
            var itemTotalCogs = 0.0

            val availableLots = db.inventoryDao().getAvailableLotsForItemFifo(sale.companyId, item.itemId)
            for (lot in availableLots) {
                if (qtyRemainingToConsume <= 0) break

                val takeFromLot = minOf(qtyRemainingToConsume, lot.remainingQuantity)
                val lotCogs = takeFromLot * lot.unitCost
                itemTotalCogs += lotCogs
                qtyRemainingToConsume -= takeFromLot

                val updatedLot = lot.copy(remainingQuantity = lot.remainingQuantity - takeFromLot)
                db.inventoryDao().updateLot(updatedLot)
            }

            // If stock in lots was less than requested quantity, use item's purchase price or opening cost for remaining
            if (qtyRemainingToConsume > 0) {
                val dbItem = db.inventoryDao().getItemById(item.itemId)
                val fallbackCost = if (dbItem != null && dbItem.purchasePrice > 0) dbItem.purchasePrice else dbItem?.openingCost ?: 0.0
                itemTotalCogs += qtyRemainingToConsume * fallbackCost
            }

            totalInvoiceCogs += itemTotalCogs
            processedItems.add(item.copy(cogs = itemTotalCogs))

            // Update item current stock
            val dbItem = db.inventoryDao().getItemById(item.itemId)
            if (dbItem != null) {
                db.inventoryDao().updateItem(dbItem.copy(currentStock = dbItem.currentStock - item.quantity))
            }

            // Record Stock Movement
            db.inventoryDao().insertStockMovement(
                StockMovementEntity(
                    companyId = sale.companyId,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    date = sale.date,
                    movementType = "OUT",
                    quantity = item.quantity,
                    unitCost = if (item.quantity > 0) itemTotalCogs / item.quantity else 0.0,
                    totalCost = itemTotalCogs,
                    referenceType = "SALE",
                    referenceNumber = sale.invoiceNumber,
                    notes = "Sold in ${sale.invoiceNumber} to ${if (sale.customerName.isNotBlank()) sale.customerName else "Cash Customer"}"
                )
            )
        }

        // Save Sale Invoice
        val saleToSave = sale.copy(
            cogsTotal = totalInvoiceCogs,
            balanceDue = if (sale.paymentType == "CASH") 0.0 else (sale.totalAmount - sale.amountPaid),
            createdByName = user.name
        )
        val invoiceId = db.transactionDao().insertSale(saleToSave)
        val itemsWithInvoiceId = processedItems.map { it.copy(invoiceId = invoiceId, companyId = sale.companyId) }
        db.transactionDao().insertSaleItems(itemsWithInvoiceId)

        // --- Double Entry Postings ---
        val salesRevenueAcc = db.accountDao().getAccountByCode(sale.companyId, "4010")
        val arAcc = db.accountDao().getAccountByCode(sale.companyId, "1040")
        val invAcc = db.accountDao().getAccountByCode(sale.companyId, "1050")
        val cogsAcc = db.accountDao().getAccountByCode(sale.companyId, "5010")

        // 1. Credit Sales Revenue
        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = sale.companyId,
                date = sale.date,
                accountId = salesRevenueAcc?.id ?: 0L,
                accountCode = salesRevenueAcc?.code ?: "4010",
                accountName = salesRevenueAcc?.name ?: "Sales Revenue",
                partyType = if (sale.customerId != null) "CUSTOMER" else null,
                partyId = sale.customerId,
                partyName = sale.customerName.ifBlank { "Cash Customer" },
                voucherType = "SALE",
                voucherNumber = sale.invoiceNumber,
                description = "Sales Invoice #${sale.invoiceNumber} (${sale.paymentType})",
                debit = 0.0,
                credit = sale.totalAmount
            )
        )
        salesRevenueAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + sale.totalAmount)) }

        // 2. Cash / Bank receipt or Accounts Receivable debit
        if (sale.paymentType == "CASH" || sale.amountPaid > 0) {
            val cashPaid = if (sale.paymentType == "CASH") sale.totalAmount else sale.amountPaid
            val cashAcc = sale.cashAccountId?.let { db.accountDao().getAccountById(it) }
            if (cashAcc != null) {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = sale.companyId,
                        date = sale.date,
                        accountId = cashAcc.id,
                        accountCode = cashAcc.code,
                        accountName = cashAcc.name,
                        partyType = if (sale.customerId != null) "CUSTOMER" else null,
                        partyId = sale.customerId,
                        partyName = sale.customerName.ifBlank { "Cash Customer" },
                        voucherType = "SALE",
                        voucherNumber = sale.invoiceNumber,
                        description = "Cash received for Sale #${sale.invoiceNumber}",
                        debit = cashPaid,
                        credit = 0.0
                    )
                )
                db.accountDao().updateAccount(cashAcc.copy(currentBalance = cashAcc.currentBalance + cashPaid))
            }
        }

        if (sale.paymentType == "CREDIT" && sale.balanceDue > 0) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = sale.companyId,
                    date = sale.date,
                    accountId = arAcc?.id ?: 0L,
                    accountCode = arAcc?.code ?: "1040",
                    accountName = arAcc?.name ?: "Accounts Receivable",
                    partyType = "CUSTOMER",
                    partyId = sale.customerId,
                    partyName = sale.customerName,
                    voucherType = "SALE",
                    voucherNumber = sale.invoiceNumber,
                    description = "Credit Sale #${sale.invoiceNumber} to ${sale.customerName}",
                    debit = sale.balanceDue,
                    credit = 0.0
                )
            )
            arAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + sale.balanceDue)) }

            // Update Customer Balance
            if (sale.customerId != null) {
                val cust = db.partyDao().getCustomerById(sale.customerId)
                if (cust != null) {
                    db.partyDao().updateCustomer(cust.copy(currentBalance = cust.currentBalance + sale.balanceDue))
                }
            }
        }

        // 3. FIFO COGS and Inventory Asset
        if (totalInvoiceCogs > 0) {
            // Debit COGS
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = sale.companyId,
                    date = sale.date,
                    accountId = cogsAcc?.id ?: 0L,
                    accountCode = cogsAcc?.code ?: "5010",
                    accountName = cogsAcc?.name ?: "Cost of Goods Sold",
                    voucherType = "SALE",
                    voucherNumber = sale.invoiceNumber,
                    description = "COGS for Sale #${sale.invoiceNumber}",
                    debit = totalInvoiceCogs,
                    credit = 0.0
                )
            )
            cogsAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + totalInvoiceCogs)) }

            // Credit Inventory Asset
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = sale.companyId,
                    date = sale.date,
                    accountId = invAcc?.id ?: 0L,
                    accountCode = invAcc?.code ?: "1050",
                    accountName = invAcc?.name ?: "Inventory Asset",
                    voucherType = "SALE",
                    voucherNumber = sale.invoiceNumber,
                    description = "Stock reduction for Sale #${sale.invoiceNumber}",
                    debit = 0.0,
                    credit = totalInvoiceCogs
                )
            )
            invAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - totalInvoiceCogs)) }
        }

        // Audit Log
        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = sale.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_SALE",
                details = "Created sale #${sale.invoiceNumber} (${sale.paymentType}) Total: ${sale.totalAmount} for ${sale.customerName.ifBlank { "Cash Customer" }}"
            )
        )

        Result.success(invoiceId)
    }

    // --- Purchases with FIFO Lot Creation ---
    fun getPurchases(companyId: Long): Flow<List<PurchaseBillEntity>> = db.transactionDao().getPurchases(companyId)
    suspend fun getPurchaseById(id: Long): PurchaseBillEntity? = db.transactionDao().getPurchaseById(id)
    suspend fun getPurchaseItems(billId: Long): List<PurchaseItemEntity> = db.transactionDao().getPurchaseItems(billId)

    suspend fun createPurchaseBill(
        bill: PurchaseBillEntity,
        items: List<PurchaseItemEntity>,
        user: UserEntity
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (bill.supplierId <= 0L || bill.supplierName.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Supplier is required for purchase bill."))
        }

        val billToSave = bill.copy(
            balanceDue = if (bill.paymentType == "CASH") 0.0 else (bill.totalAmount - bill.amountPaid),
            createdByName = user.name
        )
        val billId = db.transactionDao().insertPurchase(billToSave)
        val itemsWithBillId = items.map { it.copy(billId = billId, companyId = bill.companyId) }
        db.transactionDao().insertPurchaseItems(itemsWithBillId)

        // Create FIFO Lots & Update Stock
        for (item in items) {
            val lotNumber = "LOT-PB${billId}-${item.itemId}"
            db.inventoryDao().insertLot(
                InventoryLotEntity(
                    companyId = bill.companyId,
                    lotNumber = lotNumber,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    purchaseDate = bill.date,
                    initialQuantity = item.quantity,
                    remainingQuantity = item.quantity,
                    unitCost = item.purchaseRate,
                    supplierId = bill.supplierId,
                    supplierName = bill.supplierName,
                    referenceType = "PURCHASE",
                    referenceId = billId
                )
            )

            val dbItem = db.inventoryDao().getItemById(item.itemId)
            if (dbItem != null) {
                db.inventoryDao().updateItem(
                    dbItem.copy(
                        currentStock = dbItem.currentStock + item.quantity,
                        purchasePrice = item.purchaseRate
                    )
                )
            }

            db.inventoryDao().insertStockMovement(
                StockMovementEntity(
                    companyId = bill.companyId,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    date = bill.date,
                    movementType = "IN",
                    quantity = item.quantity,
                    unitCost = item.purchaseRate,
                    totalCost = item.totalPrice,
                    referenceType = "PURCHASE",
                    referenceNumber = bill.billNumber,
                    notes = "Purchased from ${bill.supplierName} in Bill #${bill.billNumber}"
                )
            )
        }

        // --- Double Entry Postings ---
        val invAcc = db.accountDao().getAccountByCode(bill.companyId, "1050")
        val apAcc = db.accountDao().getAccountByCode(bill.companyId, "2010")

        // 1. Debit Inventory Asset
        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = bill.companyId,
                date = bill.date,
                accountId = invAcc?.id ?: 0L,
                accountCode = invAcc?.code ?: "1050",
                accountName = invAcc?.name ?: "Inventory Asset",
                partyType = "SUPPLIER",
                partyId = bill.supplierId,
                partyName = bill.supplierName,
                voucherType = "PURCHASE",
                voucherNumber = bill.billNumber,
                description = "Purchase Bill #${bill.billNumber} from ${bill.supplierName}",
                debit = bill.totalAmount,
                credit = 0.0
            )
        )
        invAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + bill.totalAmount)) }

        // 2. Credit Accounts Payable or Cash
        if (bill.paymentType == "CASH" || bill.amountPaid > 0) {
            val paid = if (bill.paymentType == "CASH") bill.totalAmount else bill.amountPaid
            val cashAcc = bill.cashAccountId?.let { db.accountDao().getAccountById(it) }
            if (cashAcc != null) {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = bill.companyId,
                        date = bill.date,
                        accountId = cashAcc.id,
                        accountCode = cashAcc.code,
                        accountName = cashAcc.name,
                        partyType = "SUPPLIER",
                        partyId = bill.supplierId,
                        partyName = bill.supplierName,
                        voucherType = "PURCHASE",
                        voucherNumber = bill.billNumber,
                        description = "Cash paid for Purchase #${bill.billNumber}",
                        debit = 0.0,
                        credit = paid
                    )
                )
                db.accountDao().updateAccount(cashAcc.copy(currentBalance = cashAcc.currentBalance - paid))
            }
        }

        if (bill.paymentType == "CREDIT" && bill.balanceDue > 0) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = bill.companyId,
                    date = bill.date,
                    accountId = apAcc?.id ?: 0L,
                    accountCode = apAcc?.code ?: "2010",
                    accountName = apAcc?.name ?: "Accounts Payable",
                    partyType = "SUPPLIER",
                    partyId = bill.supplierId,
                    partyName = bill.supplierName,
                    voucherType = "PURCHASE",
                    voucherNumber = bill.billNumber,
                    description = "Credit Purchase #${bill.billNumber} from ${bill.supplierName}",
                    debit = 0.0,
                    credit = bill.balanceDue
                )
            )
            apAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + bill.balanceDue)) }

            val supplier = db.partyDao().getSupplierById(bill.supplierId)
            if (supplier != null) {
                db.partyDao().updateSupplier(supplier.copy(currentBalance = supplier.currentBalance + bill.balanceDue))
            }
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = bill.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_PURCHASE",
                details = "Created purchase bill #${bill.billNumber} Total: ${bill.totalAmount} from ${bill.supplierName}"
            )
        )

        Result.success(billId)
    }

    // --- Sales Returns ---
    fun getSalesReturns(companyId: Long): Flow<List<SalesReturnEntity>> = db.transactionDao().getSalesReturns(companyId)

    suspend fun createSalesReturn(
        salesReturn: SalesReturnEntity,
        items: List<SalesReturnItemEntity>,
        user: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val returnId = db.transactionDao().insertSalesReturn(salesReturn)
        val itemsWithReturnId = items.map { it.copy(returnId = returnId, companyId = salesReturn.companyId) }
        db.transactionDao().insertSalesReturnItems(itemsWithReturnId)

        var totalCogsReversed = 0.0

        for (item in items) {
            val cost = item.unitCost.coerceAtLeast(0.0)
            val itemCogs = cost * item.quantity
            totalCogsReversed += itemCogs

            // Restock to FIFO lots
            db.inventoryDao().insertLot(
                InventoryLotEntity(
                    companyId = salesReturn.companyId,
                    lotNumber = "LOT-SR${returnId}-${item.itemId}",
                    itemId = item.itemId,
                    itemName = item.itemName,
                    purchaseDate = salesReturn.date,
                    initialQuantity = item.quantity,
                    remainingQuantity = item.quantity,
                    unitCost = cost,
                    referenceType = "SALES_RETURN",
                    referenceId = returnId
                )
            )

            val dbItem = db.inventoryDao().getItemById(item.itemId)
            if (dbItem != null) {
                db.inventoryDao().updateItem(dbItem.copy(currentStock = dbItem.currentStock + item.quantity))
            }

            db.inventoryDao().insertStockMovement(
                StockMovementEntity(
                    companyId = salesReturn.companyId,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    date = salesReturn.date,
                    movementType = "RETURN_IN",
                    quantity = item.quantity,
                    unitCost = cost,
                    totalCost = itemCogs,
                    referenceType = "SALES_RETURN",
                    referenceNumber = salesReturn.returnNumber,
                    notes = "Sales return from ${salesReturn.customerName}"
                )
            )
        }

        // Accounting: Debit Sales Returns (4020), Credit AR / Cash
        val srAcc = db.accountDao().getAccountByCode(salesReturn.companyId, "4020")
        val arAcc = db.accountDao().getAccountByCode(salesReturn.companyId, "1040")
        val invAcc = db.accountDao().getAccountByCode(salesReturn.companyId, "1050")
        val cogsAcc = db.accountDao().getAccountByCode(salesReturn.companyId, "5010")

        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = salesReturn.companyId,
                date = salesReturn.date,
                accountId = srAcc?.id ?: 0L,
                accountCode = srAcc?.code ?: "4020",
                accountName = srAcc?.name ?: "Sales Return & Allowances",
                partyType = if (salesReturn.customerId != null) "CUSTOMER" else null,
                partyId = salesReturn.customerId,
                partyName = salesReturn.customerName,
                voucherType = "SALES_RETURN",
                voucherNumber = salesReturn.returnNumber,
                description = "Sales Return #${salesReturn.returnNumber} from ${salesReturn.customerName}",
                debit = salesReturn.totalAmount,
                credit = 0.0
            )
        )

        if (salesReturn.customerId != null) {
            val cust = db.partyDao().getCustomerById(salesReturn.customerId)
            if (cust != null) {
                db.partyDao().updateCustomer(cust.copy(currentBalance = cust.currentBalance - salesReturn.totalAmount))
            }
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = salesReturn.companyId,
                    date = salesReturn.date,
                    accountId = arAcc?.id ?: 0L,
                    accountCode = arAcc?.code ?: "1040",
                    accountName = arAcc?.name ?: "Accounts Receivable",
                    partyType = "CUSTOMER",
                    partyId = salesReturn.customerId,
                    partyName = salesReturn.customerName,
                    voucherType = "SALES_RETURN",
                    voucherNumber = salesReturn.returnNumber,
                    description = "Credit note for Return #${salesReturn.returnNumber}",
                    debit = 0.0,
                    credit = salesReturn.totalAmount
                )
            )
        } else if (salesReturn.cashAccountId != null) {
            val cashAcc = db.accountDao().getAccountById(salesReturn.cashAccountId)
            if (cashAcc != null) {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = salesReturn.companyId,
                        date = salesReturn.date,
                        accountId = cashAcc.id,
                        accountCode = cashAcc.code,
                        accountName = cashAcc.name,
                        voucherType = "SALES_RETURN",
                        voucherNumber = salesReturn.returnNumber,
                        description = "Cash refund for Sales Return #${salesReturn.returnNumber}",
                        debit = 0.0,
                        credit = salesReturn.totalAmount
                    )
                )
                db.accountDao().updateAccount(cashAcc.copy(currentBalance = cashAcc.currentBalance - salesReturn.totalAmount))
            }
        }

        // Reversal of Inventory & COGS
        if (totalCogsReversed > 0) {
            invAcc?.let {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = salesReturn.companyId,
                        date = salesReturn.date,
                        accountId = it.id,
                        accountCode = it.code,
                        accountName = it.name,
                        voucherType = "SALES_RETURN",
                        voucherNumber = salesReturn.returnNumber,
                        description = "Stock restoration for Return #${salesReturn.returnNumber}",
                        debit = totalCogsReversed,
                        credit = 0.0
                    )
                )
                db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance + totalCogsReversed))
            }
            cogsAcc?.let {
                db.transactionDao().insertLedgerEntry(
                    LedgerEntryEntity(
                        companyId = salesReturn.companyId,
                        date = salesReturn.date,
                        accountId = it.id,
                        accountCode = it.code,
                        accountName = it.name,
                        voucherType = "SALES_RETURN",
                        voucherNumber = salesReturn.returnNumber,
                        description = "COGS reversal for Return #${salesReturn.returnNumber}",
                        debit = 0.0,
                        credit = totalCogsReversed
                    )
                )
                db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - totalCogsReversed))
            }
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = salesReturn.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_SALES_RETURN",
                details = "Created sales return #${salesReturn.returnNumber} Amount: ${salesReturn.totalAmount}"
            )
        )

        returnId
    }

    // --- Purchase Returns ---
    fun getPurchaseReturns(companyId: Long): Flow<List<PurchaseReturnEntity>> = db.transactionDao().getPurchaseReturns(companyId)

    suspend fun createPurchaseReturn(
        purchaseReturn: PurchaseReturnEntity,
        items: List<PurchaseReturnItemEntity>,
        user: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val returnId = db.transactionDao().insertPurchaseReturn(purchaseReturn)
        val itemsWithReturnId = items.map { it.copy(returnId = returnId, companyId = purchaseReturn.companyId) }
        db.transactionDao().insertPurchaseReturnItems(itemsWithReturnId)

        for (item in items) {
            val dbItem = db.inventoryDao().getItemById(item.itemId)
            if (dbItem != null) {
                db.inventoryDao().updateItem(dbItem.copy(currentStock = dbItem.currentStock - item.quantity))
            }

            db.inventoryDao().insertStockMovement(
                StockMovementEntity(
                    companyId = purchaseReturn.companyId,
                    itemId = item.itemId,
                    itemName = item.itemName,
                    date = purchaseReturn.date,
                    movementType = "RETURN_OUT",
                    quantity = item.quantity,
                    unitCost = item.purchaseRate,
                    totalCost = item.totalPrice,
                    referenceType = "PURCHASE_RETURN",
                    referenceNumber = purchaseReturn.returnNumber,
                    notes = "Purchase return to ${purchaseReturn.supplierName}"
                )
            )
        }

        val apAcc = db.accountDao().getAccountByCode(purchaseReturn.companyId, "2010")
        val invAcc = db.accountDao().getAccountByCode(purchaseReturn.companyId, "1050")

        // Debit Accounts Payable (reduces supplier balance)
        val supplier = db.partyDao().getSupplierById(purchaseReturn.supplierId)
        if (supplier != null) {
            db.partyDao().updateSupplier(supplier.copy(currentBalance = supplier.currentBalance - purchaseReturn.totalAmount))
        }

        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = purchaseReturn.companyId,
                date = purchaseReturn.date,
                accountId = apAcc?.id ?: 0L,
                accountCode = apAcc?.code ?: "2010",
                accountName = apAcc?.name ?: "Accounts Payable",
                partyType = "SUPPLIER",
                partyId = purchaseReturn.supplierId,
                partyName = purchaseReturn.supplierName,
                voucherType = "PURCHASE_RETURN",
                voucherNumber = purchaseReturn.returnNumber,
                description = "Debit note for Purchase Return #${purchaseReturn.returnNumber} to ${purchaseReturn.supplierName}",
                debit = purchaseReturn.totalAmount,
                credit = 0.0
            )
        )
        apAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - purchaseReturn.totalAmount)) }

        // Credit Inventory Asset
        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = purchaseReturn.companyId,
                date = purchaseReturn.date,
                accountId = invAcc?.id ?: 0L,
                accountCode = invAcc?.code ?: "1050",
                accountName = invAcc?.name ?: "Inventory Asset",
                partyType = "SUPPLIER",
                partyId = purchaseReturn.supplierId,
                partyName = purchaseReturn.supplierName,
                voucherType = "PURCHASE_RETURN",
                voucherNumber = purchaseReturn.returnNumber,
                description = "Stock reduction for Purchase Return #${purchaseReturn.returnNumber}",
                debit = 0.0,
                credit = purchaseReturn.totalAmount
            )
        )
        invAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - purchaseReturn.totalAmount)) }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = purchaseReturn.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_PURCHASE_RETURN",
                details = "Created purchase return #${purchaseReturn.returnNumber} Amount: ${purchaseReturn.totalAmount} to ${purchaseReturn.supplierName}"
            )
        )

        returnId
    }

    // --- Receipts & Payments ---
    fun getPaymentReceipts(companyId: Long): Flow<List<PaymentReceiptEntity>> = db.transactionDao().getPaymentReceipts(companyId)
    fun getPaymentReceiptsByType(companyId: Long, type: String): Flow<List<PaymentReceiptEntity>> = db.transactionDao().getPaymentReceiptsByType(companyId, type)

    suspend fun createCustomerReceipt(
        voucher: PaymentReceiptEntity,
        user: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val voucherId = db.transactionDao().insertPaymentReceipt(voucher.copy(type = "RECEIPT", partyType = "CUSTOMER"))

        // Update Customer Balance
        val customer = db.partyDao().getCustomerById(voucher.partyId)
        if (customer != null) {
            db.partyDao().updateCustomer(customer.copy(currentBalance = customer.currentBalance - voucher.amount))
        }

        // Double Entry: Debit Cash/Bank, Credit Accounts Receivable
        val cashAcc = db.accountDao().getAccountById(voucher.accountId)
        val arAcc = db.accountDao().getAccountByCode(voucher.companyId, "1040")

        if (cashAcc != null) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = voucher.companyId,
                    date = voucher.date,
                    accountId = cashAcc.id,
                    accountCode = cashAcc.code,
                    accountName = cashAcc.name,
                    partyType = "CUSTOMER",
                    partyId = voucher.partyId,
                    partyName = voucher.partyName,
                    voucherType = "RECEIPT",
                    voucherNumber = voucher.voucherNumber,
                    description = "Customer Receipt from ${voucher.partyName} (Ref: ${voucher.reference})",
                    debit = voucher.amount,
                    credit = 0.0
                )
            )
            db.accountDao().updateAccount(cashAcc.copy(currentBalance = cashAcc.currentBalance + voucher.amount))
        }

        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = voucher.companyId,
                date = voucher.date,
                accountId = arAcc?.id ?: 0L,
                accountCode = arAcc?.code ?: "1040",
                accountName = arAcc?.name ?: "Accounts Receivable",
                partyType = "CUSTOMER",
                partyId = voucher.partyId,
                partyName = voucher.partyName,
                voucherType = "RECEIPT",
                voucherNumber = voucher.voucherNumber,
                description = "Payment received from ${voucher.partyName}",
                debit = 0.0,
                credit = voucher.amount
            )
        )
        arAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - voucher.amount)) }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = voucher.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_RECEIPT",
                details = "Received ${voucher.amount} from customer '${voucher.partyName}' via ${voucher.accountName}"
            )
        )

        voucherId
    }

    suspend fun createSupplierPayment(
        voucher: PaymentReceiptEntity,
        user: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val voucherId = db.transactionDao().insertPaymentReceipt(voucher.copy(type = "PAYMENT", partyType = "SUPPLIER"))

        // Update Supplier Balance
        val supplier = db.partyDao().getSupplierById(voucher.partyId)
        if (supplier != null) {
            db.partyDao().updateSupplier(supplier.copy(currentBalance = supplier.currentBalance - voucher.amount))
        }

        // Double Entry: Debit Accounts Payable, Credit Cash/Bank
        val cashAcc = db.accountDao().getAccountById(voucher.accountId)
        val apAcc = db.accountDao().getAccountByCode(voucher.companyId, "2010")

        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = voucher.companyId,
                date = voucher.date,
                accountId = apAcc?.id ?: 0L,
                accountCode = apAcc?.code ?: "2010",
                accountName = apAcc?.name ?: "Accounts Payable",
                partyType = "SUPPLIER",
                partyId = voucher.partyId,
                partyName = voucher.partyName,
                voucherType = "PAYMENT",
                voucherNumber = voucher.voucherNumber,
                description = "Payment to Supplier ${voucher.partyName} (Ref: ${voucher.reference})",
                debit = voucher.amount,
                credit = 0.0
            )
        )
        apAcc?.let { db.accountDao().updateAccount(it.copy(currentBalance = it.currentBalance - voucher.amount)) }

        if (cashAcc != null) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = voucher.companyId,
                    date = voucher.date,
                    accountId = cashAcc.id,
                    accountCode = cashAcc.code,
                    accountName = cashAcc.name,
                    partyType = "SUPPLIER",
                    partyId = voucher.partyId,
                    partyName = voucher.partyName,
                    voucherType = "PAYMENT",
                    voucherNumber = voucher.voucherNumber,
                    description = "Cash/Bank payment to ${voucher.partyName}",
                    debit = 0.0,
                    credit = voucher.amount
                )
            )
            db.accountDao().updateAccount(cashAcc.copy(currentBalance = cashAcc.currentBalance - voucher.amount))
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = voucher.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_PAYMENT",
                details = "Paid ${voucher.amount} to supplier '${voucher.partyName}' via ${voucher.accountName}"
            )
        )

        voucherId
    }

    // --- Expenses ---
    fun getExpenses(companyId: Long): Flow<List<ExpenseEntity>> = db.transactionDao().getExpenses(companyId)

    suspend fun createExpense(
        expense: ExpenseEntity,
        user: UserEntity
    ): Long = withContext(Dispatchers.IO) {
        val expenseId = db.transactionDao().insertExpense(expense)

        val expAcc = db.accountDao().getAccountById(expense.expenseAccountId)
        val payAcc = db.accountDao().getAccountById(expense.paymentAccountId)

        // Debit Expense Account
        if (expAcc != null) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = expense.companyId,
                    date = expense.date,
                    accountId = expAcc.id,
                    accountCode = expAcc.code,
                    accountName = expAcc.name,
                    voucherType = "EXPENSE",
                    voucherNumber = expense.voucherNumber,
                    description = "Expense: ${expense.description} (Ref: ${expense.reference})",
                    debit = expense.amount,
                    credit = 0.0
                )
            )
            db.accountDao().updateAccount(expAcc.copy(currentBalance = expAcc.currentBalance + expense.amount))
        }

        // Credit Payment Account (Cash/Bank)
        if (payAcc != null) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = expense.companyId,
                    date = expense.date,
                    accountId = payAcc.id,
                    accountCode = payAcc.code,
                    accountName = payAcc.name,
                    voucherType = "EXPENSE",
                    voucherNumber = expense.voucherNumber,
                    description = "Paid for ${expense.expenseAccountName}: ${expense.description}",
                    debit = 0.0,
                    credit = expense.amount
                )
            )
            db.accountDao().updateAccount(payAcc.copy(currentBalance = payAcc.currentBalance - expense.amount))
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = expense.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_EXPENSE",
                details = "Expense of ${expense.amount} under '${expense.expenseAccountName}' via ${expense.paymentAccountName}"
            )
        )

        expenseId
    }

    // --- Journal Entries (Strict Double Entry: Debit == Credit) ---
    fun getJournalEntries(companyId: Long): Flow<List<JournalEntryEntity>> = db.transactionDao().getJournalEntries(companyId)
    suspend fun getJournalLines(journalId: Long): List<JournalLineEntity> = db.transactionDao().getJournalLines(journalId)

    suspend fun createJournalEntry(
        entry: JournalEntryEntity,
        lines: List<JournalLineEntity>,
        user: UserEntity
    ): Result<Long> = withContext(Dispatchers.IO) {
        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }

        // Strict accounting check: Total Debit must equal Total Credit within 0.01 tolerance
        if (kotlin.math.abs(totalDebit - totalCredit) > 0.01) {
            return@withContext Result.failure(
                IllegalArgumentException("Unbalanced Journal Entry! Total Debit ($totalDebit) must equal Total Credit ($totalCredit).")
            )
        }
        if (totalDebit <= 0.0) {
            return@withContext Result.failure(
                IllegalArgumentException("Journal Entry must have an amount greater than zero.")
            )
        }

        val entryToSave = entry.copy(totalDebit = totalDebit, totalCredit = totalCredit)
        val journalId = db.transactionDao().insertJournalEntry(entryToSave)
        val linesWithId = lines.map { it.copy(journalId = journalId, companyId = entry.companyId) }
        db.transactionDao().insertJournalLines(linesWithId)

        // Post each line to General Ledger & Account balances
        for (line in lines) {
            db.transactionDao().insertLedgerEntry(
                LedgerEntryEntity(
                    companyId = entry.companyId,
                    date = entry.date,
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    voucherType = "JOURNAL",
                    voucherNumber = entry.entryNumber,
                    description = line.lineDescription.ifBlank { entry.description },
                    debit = line.debit,
                    credit = line.credit
                )
            )

            val acc = db.accountDao().getAccountById(line.accountId)
            if (acc != null) {
                val netChange = if (acc.type == "ASSET" || acc.type == "EXPENSE") {
                    line.debit - line.credit
                } else {
                    line.credit - line.debit
                }
                db.accountDao().updateAccount(acc.copy(currentBalance = acc.currentBalance + netChange))
            }
        }

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = entry.companyId,
                userEmail = user.email,
                userName = user.name,
                action = "CREATE_JOURNAL",
                details = "Created Journal Entry #${entry.entryNumber} Debit: $totalDebit, Credit: $totalCredit"
            )
        )

        Result.success(journalId)
    }

    // --- Fund Transfer between Cash/Bank Accounts ---
    suspend fun transferFunds(
        companyId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        date: Long,
        reference: String,
        notes: String,
        user: UserEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (amount <= 0) return@withContext Result.failure(IllegalArgumentException("Transfer amount must be greater than zero."))
        if (fromAccountId == toAccountId) return@withContext Result.failure(IllegalArgumentException("Source and Destination accounts must be different."))

        val fromAcc = db.accountDao().getAccountById(fromAccountId) ?: return@withContext Result.failure(IllegalArgumentException("Source account not found."))
        val toAcc = db.accountDao().getAccountById(toAccountId) ?: return@withContext Result.failure(IllegalArgumentException("Destination account not found."))

        val voucherNumber = "TRF-${System.currentTimeMillis().toString().takeLast(6)}"

        // Credit Source Account
        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = companyId,
                date = date,
                accountId = fromAcc.id,
                accountCode = fromAcc.code,
                accountName = fromAcc.name,
                voucherType = "TRANSFER",
                voucherNumber = voucherNumber,
                description = "Fund transfer to ${toAcc.name} (Ref: $reference)",
                debit = 0.0,
                credit = amount
            )
        )
        db.accountDao().updateAccount(fromAcc.copy(currentBalance = fromAcc.currentBalance - amount))

        // Debit Destination Account
        db.transactionDao().insertLedgerEntry(
            LedgerEntryEntity(
                companyId = companyId,
                date = date,
                accountId = toAcc.id,
                accountCode = toAcc.code,
                accountName = toAcc.name,
                voucherType = "TRANSFER",
                voucherNumber = voucherNumber,
                description = "Fund transfer from ${fromAcc.name} (Ref: $reference)",
                debit = amount,
                credit = 0.0
            )
        )
        db.accountDao().updateAccount(toAcc.copy(currentBalance = toAcc.currentBalance + amount))

        db.transactionDao().insertAuditLog(
            AuditLogEntity(
                companyId = companyId,
                userEmail = user.email,
                userName = user.name,
                action = "TRANSFER",
                details = "Transferred $amount from '${fromAcc.name}' to '${toAcc.name}'"
            )
        )

        Result.success(Unit)
    }

    // --- Ledgers & Audit Logs ---
    fun getAllLedgerEntries(companyId: Long): Flow<List<LedgerEntryEntity>> = db.transactionDao().getAllLedgerEntries(companyId)
    fun getLedgerEntriesForAccount(companyId: Long, accountId: Long): Flow<List<LedgerEntryEntity>> = db.transactionDao().getLedgerEntriesForAccount(companyId, accountId)
    fun getLedgerEntriesForParty(companyId: Long, partyType: String, partyId: Long): Flow<List<LedgerEntryEntity>> = db.transactionDao().getLedgerEntriesForParty(companyId, partyType, partyId)
    fun getAuditLogs(companyId: Long): Flow<List<AuditLogEntity>> = db.transactionDao().getAuditLogs(companyId)
}
