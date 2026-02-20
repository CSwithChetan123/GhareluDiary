package com.ghareludiary.app.report

data class ReportData(
    val entries: List<ReportEntry>,
    val entryCount: Int,
    val noEntryCount: Int,
    val totalAmount: Double,
    val totalQuantity: Double
)

data class ReportEntry(
    val date: Long,
    val categoryName: String,
    val quantity: Double,
    val amount: Double,
    val remark: String,
    val isNoEntry: Boolean,
    val hasEntry: Boolean = true
)

enum class FilterStatus{
    ALL, ENTRIES_ONLY, NO_ENTRIES_ONLY
}
