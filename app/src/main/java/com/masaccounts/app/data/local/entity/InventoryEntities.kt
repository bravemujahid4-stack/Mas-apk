package com.masaccounts.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val name: String
)

@Entity(tableName = "units")
data class UnitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val code: String, // e.g. "KG", "Ton", "Piece", "Meter", "Foot", "Bag"
    val name: String
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val code: String,
    val name: String,
    val category: String = "Iron",
    val unit: String = "KG",
    val openingQuantity: Double = 0.0,
    val openingCost: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val minStock: Double = 0.0,
    val currentStock: Double = 0.0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_lots")
data class InventoryLotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val lotNumber: String,
    val itemId: Long,
    val itemName: String,
    val purchaseDate: Long,
    val initialQuantity: Double,
    val remainingQuantity: Double,
    val unitCost: Double,
    val supplierId: Long? = null,
    val supplierName: String? = null,
    val referenceType: String = "PURCHASE", // "PURCHASE", "OPENING_STOCK", "SALES_RETURN"
    val referenceId: Long? = null
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val companyId: Long,
    val itemId: Long,
    val itemName: String,
    val date: Long,
    val movementType: String, // "IN", "OUT", "RETURN_IN", "RETURN_OUT", "OPENING"
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double,
    val referenceType: String, // "SALE", "PURCHASE", "SALES_RETURN", "PURCHASE_RETURN", "OPENING"
    val referenceNumber: String,
    val notes: String = ""
)
