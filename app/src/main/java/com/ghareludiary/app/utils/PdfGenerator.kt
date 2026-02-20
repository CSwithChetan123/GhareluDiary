package com.ghareludiary.app.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.report.ReportData
import com.ghareludiary.app.report.ReportEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateReport(
        context: Context,
        reportData: ReportData,
        categoryName: String?,
        startDate: Long,
        endDate: Long
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points

        var pageNumber = 1
        var yPosition = 80f
        val lineHeight = 30f
        val margin = 40f

        // Get category type
        val categoryType = try {
            CategoryType.valueOf(categoryName?.uppercase()?.replace(" ", "_") ?: "")
        } catch (e: Exception) {
            null
        }

        // Create first page
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Title Paint
        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = Color.BLACK
        }

        // Header Paint
        val headerPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = Color.BLACK
        }

        // Normal Paint
        val normalPaint = Paint().apply {
            textSize = 12f
            color = Color.BLACK
        }

        // Date format
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        // Draw Title
        canvas.drawText("Gharelu Diary Report", margin, yPosition, titlePaint)
        yPosition += 40f

        // Draw Header Info
        canvas.drawText("Category: $categoryName", margin, yPosition, headerPaint)
        yPosition += 25f
        canvas.drawText(
            "Period: ${dateFormat.format(Date(startDate))} to ${dateFormat.format(Date(endDate))}",
            margin,
            yPosition,
            headerPaint
        )
        yPosition += 40f

        // Draw Summary
        canvas.drawText("Summary:", margin, yPosition, headerPaint)
        yPosition += 25f

        // FIXED: Better summary for service vs quantity categories
        if (categoryType?.hasQuantity() == true) {
            // Quantity categories (MILK, WATER)
            canvas.drawText("Total Entries: ${reportData.entryCount}", margin + 20, yPosition, normalPaint)
            yPosition += 20f
            canvas.drawText("Total Amount: ₹${"%.2f".format(reportData.totalAmount)}", margin + 20, yPosition, normalPaint)
            yPosition += 20f
            if (reportData.totalQuantity > 0) {
                canvas.drawText("Total Quantity: ${"%.1f".format(reportData.totalQuantity)}", margin + 20, yPosition, normalPaint)
                yPosition += 20f
            }
            canvas.drawText("No Entry Days: ${reportData.noEntryCount}", margin + 20, yPosition, normalPaint)
        } else {
            // Service categories (MAID, COOK, etc.)
            val yesCount = reportData.entryCount
            val noCount = reportData.entries.count { it.hasEntry && it.isNoEntry }
            val notRecordedCount = reportData.noEntryCount

            canvas.drawText("YES: $yesCount ${if (yesCount == 1) "day" else "days"} | NO: $noCount ${if (noCount == 1) "day" else "days"} | Not recorded: $notRecordedCount ${if (notRecordedCount == 1) "day" else "days"}", margin + 20, yPosition, normalPaint)
            yPosition += 20f
            if (reportData.totalAmount > 0) {
                canvas.drawText("Total Payment: ₹${"%.2f".format(reportData.totalAmount)}", margin + 20, yPosition, normalPaint)
                yPosition += 20f
            }
        }
        yPosition += 20f

        // Draw Table Header
        canvas.drawText("Date", margin, yPosition, headerPaint)
        canvas.drawText("Category", margin + 150, yPosition, headerPaint)
        canvas.drawText("Amount", margin + 300, yPosition, headerPaint)
        canvas.drawText("Quantity/Status", margin + 400, yPosition, headerPaint)
        yPosition += 5f

        // Draw line
        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, normalPaint)
        yPosition += 20f

        // Draw entries
        val entryDateFormat = SimpleDateFormat("dd-MMM-yyyy EEE", Locale.getDefault())

        for (entry in reportData.entries) {
            // Check if we need a new page
            if (yPosition > pageHeight - 80) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 80f

                // Redraw table header on new page
                canvas.drawText("Date", margin, yPosition, headerPaint)
                canvas.drawText("Category", margin + 150, yPosition, headerPaint)
                canvas.drawText("Amount", margin + 300, yPosition, headerPaint)
                canvas.drawText("Quantity/Status", margin + 400, yPosition, headerPaint)
                yPosition += 5f
                canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, normalPaint)
                yPosition += 20f
            }

            // Draw entry data
            canvas.drawText(entryDateFormat.format(Date(entry.date)), margin, yPosition, normalPaint)
            canvas.drawText(entry.categoryName, margin + 150, yPosition, normalPaint)

            // FIXED: Use proper display logic
            val displayValue = getDisplayValue(entry, categoryType)
            val displayPaint = getDisplayPaint(displayValue)

            // Amount column
            if (entry.hasEntry && !entry.isNoEntry && entry.amount > 0) {
                canvas.drawText("₹${"%.2f".format(entry.amount)}", margin + 300, yPosition, normalPaint)
            } else {
                canvas.drawText("-", margin + 300, yPosition, normalPaint)
            }

            // Quantity/Status column
            canvas.drawText(displayValue, margin + 400, yPosition, displayPaint)

            yPosition += lineHeight
        }

        // Finish the last page
        pdfDocument.finishPage(page)

        // Save PDF to file
        val fileName = "GhareluDiary_${categoryName?.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } finally {
            pdfDocument.close()
        }

        return file
    }

    private fun getDisplayValue(entry: ReportEntry, categoryType: CategoryType?): String {
        return when {
            // Case 1: No entry in database
            !entry.hasEntry -> "No Entry"

            // Case 2: User clicked NO
            entry.isNoEntry -> "NO"

            // Case 3: User clicked YES
            else -> {
                if (categoryType?.hasQuantity() == true) {
                    // Show QUANTITY for MILK/WATER
                    if (entry.quantity > 0) {
                        String.format("%.1f", entry.quantity)
                    } else {
                        "0.0"
                    }
                } else {
                    // Show "YES" for service categories
                    "YES"
                }
            }
        }
    }

    private fun getDisplayPaint(displayValue: String): Paint {
        return Paint().apply {
            textSize = 12f
            color = when (displayValue) {
                "NO" -> Color.RED
                "No Entry" -> Color.parseColor("#9E9E9E")
                "YES" -> Color.parseColor("#4CAF50")
                else -> Color.BLACK
            }
        }
    }
}