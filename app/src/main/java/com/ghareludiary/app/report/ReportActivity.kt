package com.ghareludiary.app.report

import android.R
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ghareludiary.app.adapter.ReportEntryAdapter
import com.ghareludiary.app.databinding.ActivityReportBinding
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportBinding
    private val viewModel: ReportViewModel by viewModels()
    private val adapter = ReportEntryAdapter()

    private var startDate: Long = 0L
    private var endDate: Long = 0L
    private var selectedCategory: CategoryType? = null
    private var selectedFilter: FilterStatus = FilterStatus.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpToolbar()
        setUpRecyclerView()
        setUpSpinner()
        setUpDateRange()
        setUpButton()
        setUpObservers()
        loadReport()
    }

    private fun setUpToolbar(){
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setUpRecyclerView(){
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setUpSpinner(){
        val categories = listOf("All Categories") + CategoryType.values().map { it.displayName }
        val categoryAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categoryAdapter

        binding.spinnerCategory.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedCategory = if(position == 0){
                    null
                }
                else {
                    CategoryType.values()[position - 1]
                }
                loadReport()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        val filters = listOf("All", "Entries Only", "No Entries Only")
        val filterAdapter = ArrayAdapter(this, R.layout.simple_spinner_item, filters)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = filterAdapter

        binding.spinnerFilter.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedFilter = when(position){
                    0 -> FilterStatus.ALL
                    1 -> FilterStatus.ENTRIES_ONLY
                    2 -> FilterStatus.NO_ENTRIES_ONLY
                    else -> FilterStatus.ALL
                }
                loadReport()

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
    }

    private fun setUpDateRange(){
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        endDate = calendar.timeInMillis

        updateDisplay()

        binding.btnDateRange.setOnClickListener {
            showDateRangePicker()
        }

        binding.tvStartDate.setOnClickListener {
            showStartDatePicker()
        }

        binding.tvEndDate.setOnClickListener {
            showEndDatePicker()
        }
    }

    private fun updateDisplay(){
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.tvStartDate.text = dateFormat.format(Date(startDate)).uppercase()
        binding.tvEndDate.text = dateFormat.format(Date(endDate)).uppercase()
    }

    private fun showDateRangePicker(){
        showStartDatePicker()
    }

    private fun showStartDatePicker(){
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startDate = cal.timeInMillis
                updateDisplay()
                showEndDatePicker()

            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showEndDatePicker(){
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endDate

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 23, 59, 59)
                cal.set(Calendar.MILLISECOND, 999)
                endDate = cal.timeInMillis
                updateDisplay()
                showEndDatePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setUpButton(){
        binding.btnDownloadPdf.setOnClickListener {
            generatePdf()
        }

        binding.btnWhatsappShare.setOnClickListener {
            shareViaWhatsapp()
        }
    }

    private fun setUpObservers(){
        lifecycleScope.launch{
            viewModel.reportData.collect{ reportData ->
                adapter.submitList(reportData.entries)
                updateSummary(reportData)
            }
        }
    }

    private fun loadReport(){
        viewModel.loadReport(startDate, endDate, selectedCategory, selectedFilter)
    }

    private fun updateSummary(reportData: ReportData){
        val categoryName = selectedCategory?.displayName ?: "All Categories"
        val summaryText = if(selectedCategory?.hasQuantity() ?: false){
            when(selectedFilter){
                FilterStatus.ALL, FilterStatus.ENTRIES_ONLY ->{
                    buildString {
                        append("Total ${reportData.entryCount} $categoryName")

                        if(reportData.totalAmount > 0){
                            append(" worth ₹${"%.2f".format(reportData.totalAmount)}.")
                        }

                        if(reportData.totalQuantity > 0){
                            append("\nTotal Quantity: ${"%.1f".format(reportData.totalQuantity)} ${selectedCategory!!.getCountLable()}")
                        }
                    }
                }
                FilterStatus.NO_ENTRIES_ONLY ->{
                    "Total ${reportData.noEntryCount} ${if(reportData.noEntryCount == 1) "day" else "days"} with no entry."
                }
            }

        }
        else{
            when(selectedFilter){
                FilterStatus.ALL -> {
                    val yesCount = reportData.entryCount
                    val noCount = reportData.entries.count { it.hasEntry && it.isNoEntry}
                    val notRecordedCount = reportData.noEntryCount

                    buildString {
                        append("YES: $yesCount | NO: $noCount | Not recorded: $notRecordedCount")

                        if(reportData.totalAmount > 0) {
                            append("\n Total Payment: ₹${"%.2f".format(reportData.totalAmount)}")
                        }
                    }
                }
                FilterStatus.ENTRIES_ONLY ->{
                    "Total ${reportData.entryCount} ${if(reportData.entryCount == 1) "day" else "days"} $categoryName"
                }
                FilterStatus.NO_ENTRIES_ONLY ->{
                    "Total ${reportData.noEntryCount} ${if(reportData.noEntryCount == 1) "day" else "days"} with no entry}."
                }
            }
        }
        binding.tvSummary.text = summaryText
    }

    private fun generatePdf(){
        lifecycleScope.launch {
            try {
                val reportData = viewModel.reportData.value
                val categoryName = selectedCategory?.displayName ?: "All Categories"
                val pdfFile = PdfGenerator.generateReport(this@ReportActivity, reportData, categoryName, startDate, endDate)
                openPdf(pdfFile)
            }
            catch (e: Exception){

            }
        }
    }

    private fun openPdf(file: File){
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            startActivity(intent)
        }
        catch (e: Exception){
            Toast.makeText(this, "PDF viewer not found", Toast.LENGTH_SHORT).show()

        }
    }

    private fun shareViaWhatsapp(){
        lifecycleScope.launch {
            try {
                val reportData = viewModel.reportData.value
                val categoryName = selectedCategory?.displayName
                val pdfFile = PdfGenerator.generateReport(this@ReportActivity, reportData, categoryName, startDate, endDate)

                val uri = FileProvider.getUriForFile(
                    this@ReportActivity,
                    "${packageName}.provider",
                    pdfFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "Sharing...")
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                try {
                    startActivity(intent)
                }
                catch (e: Exception){
                    val generalIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_TEXT, "Sharing...")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    startActivity(Intent.createChooser(generalIntent, "Share via"))
                }
            }
            catch (e: Exception){

            }
        }

    }
}