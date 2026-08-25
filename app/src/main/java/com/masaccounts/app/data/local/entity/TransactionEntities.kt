package com.masaccounts.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_invoices")
data class SaleInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val invoiceNumber: String,
    val date: Long,
    val customerId: Long? = null,
    val customerName: String = "",
    val paymentType: String, // "CASH", "CREDIT"
    val cashAccountId: Long? = null,
    val cashAccountName: String? = null,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val cogsTotal: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val notes: String = "",
    val createdByName: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val companyId: Long,
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalPrice: Double,
    val cogs: Double = 0.0
)

@Entity(tableName = "sales_returns")
data class SalesReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val returnNumber: String,
    val originalInvoiceNumber: String = "",
    val date: Long,
    val customerId: Long? = null,
    val customerName: String = "",
    val paymentType: String = "CREDIT", // "CASH", "CREDIT"
    val cashAccountId: Long? = null,
    val cashAccountName: String? = null,
    val totalAmount: Double = 0.0,
    val totalCogsReversed: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales_return_items")
data class SalesReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val companyId: Long,
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val totalPrice: Double,
    val unitCost: Double = 0.0
)

@Entity(tableName = "purchase_bills")
data class PurchaseBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val billNumber: String,
    val date: Long,
    val supplierId: Long,
    val supplierName: String,
    val paymentType: String = "CREDIT", // "CASH", "CREDIT"
    val cashAccountId: Long? = null,
    val cashAccountName: String? = null,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val notes: String = "",
    val createdByName: String = "Admin",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val companyId: Long,
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val purchaseRate: Double,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val totalPrice: Double
)

@Entity(tableName = "purchase_returns")
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val returnNumber: String,
    val originalBillNumber: String = "",
    val date: Long,
    val supplierId: Long,
    val supplierName: String,
    val paymentType: String = "CREDIT",
    val cashAccountId: Long? = null,
    val cashAccountName: String? = null,
    val totalAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_return_items")
data class PurchaseReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val companyId: Long,
    val itemId: Long,
    val itemCode: String,
    val itemName: String,
    val quantity: Double,
    val unit: String,
    val purchaseRate: Double,
    val totalPrice: Double
)

@Entity(tableName = "payment_receipts")
data class PaymentReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val voucherNumber: String,
    val type: String, // "RECEIPT" (Customer receipt) or "PAYMENT" (Supplier payment)
    val partyType: String, // "CUSTOMER", "SUPPLIER"
    val partyId: Long,
    val partyName: String,
    val accountId: Long, // Cash or Bank Account ID
    val accountName: String,
    val amount: Double,
    val date: Long,
    val reference: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val voucherNumber: String,
    val date: Long,
    val expenseAccountId: Long,
    val expenseAccountName: String,
    val paymentAccountId: Long,
    val paymentAccountName: String,
    val amount: Double,
    val description: String = "",
    val reference: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val entryNumber: String,
    val date: Long,
    val reference: String = "",
    val description: String = "",
    val totalDebit: Double,
    val totalCredit: Double,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_lines")
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalId: Long,
    val companyId: Long,
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val lineDescription: String = ""
)

@Entity(tableName = "ledger_entries")
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val date: Long,
    val accountId: Long,
    val accountCode: String,
    val accountName: String,
    val partyType: String? = null, // "CUSTOMER", "SUPPLIER", null
    val partyId: Long? = null,
    val partyName: String? = null,
    val voucherType: String, // "SALE", "PURCHASE", "RECEIPT", "PAYMENT", "EXPENSE", "JOURNAL", "SALES_RETURN", "PURCHASE_RETURN", "OPENING_BALANCE", "TRANSFER"
    val voucherNumber: String,
    val description: String,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String,
    val userName: String,
    val action: String,
    val details: String
)
