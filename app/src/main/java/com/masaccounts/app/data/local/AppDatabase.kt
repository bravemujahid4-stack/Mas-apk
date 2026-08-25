package com.masaccounts.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.masaccounts.app.data.local.dao.*
import com.masaccounts.app.data.local.entity.*

@Database(
    entities = [
        CompanyEntity::class,
        UserEntity::class,
        AccountEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        CategoryEntity::class,
        UnitEntity::class,
        ItemEntity::class,
        InventoryLotEntity::class,
        StockMovementEntity::class,
        SaleInvoiceEntity::class,
        SaleItemEntity::class,
        SalesReturnEntity::class,
        SalesReturnItemEntity::class,
        PurchaseBillEntity::class,
        PurchaseItemEntity::class,
        PurchaseReturnEntity::class,
        PurchaseReturnItemEntity::class,
        PaymentReceiptEntity::class,
        ExpenseEntity::class,
        JournalEntryEntity::class,
        JournalLineEntity::class,
        LedgerEntryEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun partyDao(): PartyDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mas_accounts_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
