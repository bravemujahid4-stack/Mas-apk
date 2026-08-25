package com.masaccounts.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val code: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val cnicNtn: String = "",
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "DEBIT", // "DEBIT" (Receivable) or "CREDIT" (Advance)
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val currentBalance: Double = 0.0, // Positive = Receivable
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val code: String,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val ntn: String = "",
    val openingBalance: Double = 0.0,
    val openingBalanceType: String = "CREDIT", // "CREDIT" (Payable) or "DEBIT" (Advance)
    val creditLimit: Double = 0.0,
    val notes: String = "",
    val currentBalance: Double = 0.0, // Positive = Payable
    val createdAt: Long = System.currentTimeMillis()
)
