package com.ghareludiary.app.model

data class MonthlySummary(
    val monthYear: String,
    val categoryStates: Map<CategoryType, CategoryStats>
)

data class CategoryStats(
    val categoryType: CategoryType,
    val totalAmount: Double,
    val totalQuantity: Double,
    val entryCount: Int,
    val lastEntryDate: Long? = null
)