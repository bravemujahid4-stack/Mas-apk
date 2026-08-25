package com.masaccounts.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val address: String = "",
    val city: String = "",
    val phone: String = "",
    val email: String = "",
    val ntn: String = "",
    val strn: String = "",
    val currency: String = "PKR",
    val fiscalYear: String = "2026-2027",
    val logoUri: String = "",
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String = "ADMIN", // "ADMIN", "ACCOUNTANT", "VIEWER"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val code: String,
    val name: String,
    val type: String, // "ASSET", "LIABILITY", "EQUITY", "REVENUE", "EXPENSE"
    val subType: String, // "CASH", "BANK", "RECEIVABLE", "PAYABLE", "INVENTORY", "CAPITAL", "RETAINED_EARNINGS", "SALES", "COGS", "OPERATING_EXPENSE", "OTHER"
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "DEBIT", // "DEBIT", "CREDIT"
    val currentBalance: Double = 0.0,
    val isSystem: Boolean = false,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val branch: String? = null
)
