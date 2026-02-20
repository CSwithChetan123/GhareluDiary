package com.ghareludiary.app.category

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ghareludiary.app.adapter.EntryAdapter
import com.ghareludiary.app.databinding.ActivityCategoryBinding
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.Entry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryBinding
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var category: CategoryType
    private lateinit var entryAdapter: EntryAdapter
    private var isYesSelected = false
    private var selectedDate: Long? = null
    private var editingEntry: Entry? = null
    private var hasShownFormForToday = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryName = intent.getStringExtra("categoryType")
        val monthYear = intent.getStringExtra("monthYear")
        category = CategoryType.valueOf(categoryName?: "")
        viewModel.init(category, monthYear?: "")

        setUpUI()
        setUpObservers()
        setUpClickListener()
    }

    private fun setUpUI(){
        binding.tvTitle.text = category.displayName
        binding.tvQuestion.text = if(category.hasQuantity()){
            "Did you get ${category.displayName} today?"
        }
        else{
            "Did ${category.displayName} come today?"
        }

        if(category.hasQuantity()){
            binding.tvQuantityLabel.text = "How much did you get?"
            binding.tvPaymentLabel.text = "Worth of ${category.displayName}"
            binding.paymentSection.visibility = View.VISIBLE
        }
        else{
            binding.tvPaymentLabel.text = "Payment Amount (Optional)"
            binding.paymentSection.visibility = View.VISIBLE
        }

        entryAdapter = EntryAdapter(
            category = category,
            onEntryClick = {entry ->
                onEntryClick(entry)
            },
            onDeleteClick = {entry ->
                onDeleteClick(entry)
            }
        )

        binding.recyclerViewEntries.apply {
            layoutManager = LinearLayoutManager(this@CategoryActivity)
            adapter = entryAdapter
        }

        binding.formSection.visibility = View.GONE

    }

    private fun setUpObservers(){
        lifecycleScope.launch {
            viewModel.getEntriesWithBlanks().collect { entries ->
                displayEntries(entries)
                updateSummary(entries)

                if(!hasShownFormForToday){
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    today.set(Calendar.SECOND, 0)
                    today.set(Calendar.MILLISECOND, 0)

                    val todayEntries = entries.firstOrNull{ entry ->
                        isSameDay(entry.date, today.timeInMillis)
                    }

                    if(todayEntries != null && todayEntries.id == 0L) {
                        showFormForDate(today.timeInMillis)
                    }
                }
            }
        }
    }

    private fun setUpClickListener(){

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }


        binding.btnYes.setOnClickListener {
            selectYes()
        }

        binding.btnNo.setOnClickListener {
            selectNo()
        }

        binding.btnSave.setOnClickListener {
            saveEntry()
        }

    }

    private fun selectYes(){
        isYesSelected = true
        binding.btnYes.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
        binding.btnYes.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        binding.btnNo.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        binding.btnNo.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        binding.detailsSection.visibility = View.VISIBLE
        if(category.hasQuantity()){
            binding.quantitySection.visibility = View.VISIBLE
        }
        else{
            binding.quantitySection.visibility = View.GONE
        }

    }

    private fun selectNo(){
        isYesSelected = false
        binding.btnYes.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        binding.btnYes.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        binding.btnNo.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
        binding.btnNo.setTextColor(ContextCompat.getColor(this, android.R.color.white))

        binding.detailsSection.visibility = View.GONE
    }

    private fun saveEntry(){

        val quantity = if(category.hasQuantity()) {
            binding.etQuantity.text.toString().toDoubleOrNull() ?: 0.0
        }
        else{
            0.0
        }
        val amount = binding.etPayment.text.toString().toDoubleOrNull() ?: 0.0
        val remark = binding.etRemarks.text.toString()

        val finalAmount = if(!isYesSelected){
            -1.0
        }
        else{
            amount
        }

        if(category.hasQuantity() && isYesSelected){
            when{
                quantity <= 0 -> {
                    Toast.makeText(this, "Enter Quantity", Toast.LENGTH_SHORT).show()
                    return
                }
                amount <= 0 -> {
                    Toast.makeText(this, "Enter Amount", Toast.LENGTH_SHORT).show()
                    return
                }
            }
        }

        val dateToSave = selectedDate
        val entryToSave = editingEntry

        if(editingEntry != null){
            viewModel.updateEntry(editingEntry!!.copy(
                quantity = quantity,
                amount = finalAmount,
                remark = remark
            ))
        }
        else{
            viewModel.saveEntry(quantity, finalAmount, remark, dateToSave?: System.currentTimeMillis())
        }

        hideForm()
    }

    private fun hideForm(){
        binding.formSection.visibility = View.GONE
        binding.detailsSection.visibility = View.GONE
        binding.btnYes.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)
        binding.btnNo.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.darker_gray)

        binding.etQuantity.text?.clear()
        binding.etPayment.text?.clear()
        binding.etRemarks.text?.clear()
        isYesSelected = false
        editingEntry = null
        selectedDate = null
    }

    private fun displayEntries(entries: List<Entry>){
        if(entries.isEmpty()){
            binding.recyclerViewEntries.visibility = View.GONE
        }
        else{
            binding.recyclerViewEntries.visibility = View.VISIBLE
            entryAdapter.submitList(entries)

        }
    }

    private fun onEntryClick(entry: Entry){
        if(entry.id == 0L){
            showFormForDate(entry.date)
        }
        else{
            editEntry(entry)
        }
    }

    private fun onDeleteClick(entry: Entry){
        viewModel.deleteEntry(entry)
    }

    private fun showFormForDate(date: Long) {
        binding.formSection.visibility = View.VISIBLE
        binding.detailsSection.visibility = View.GONE

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(date))

        binding.tvQuestion.text = if(category.hasQuantity()){
            "Did you get ${category.displayName} on ${formattedDate}?"
        }
        else{
            "Did ${category.displayName} come on ${formattedDate}?"
        }

        binding.etQuantity.text?.clear()
        binding.etPayment.text?.clear()
        binding.etRemarks.text?.clear()

        selectedDate = date
        hasShownFormForToday = true
        binding.btnSave.isEnabled = true

    }

    private fun editEntry(entry: Entry){
        selectedDate = entry.date
        editingEntry = entry

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(entry.date))

        binding.tvQuestion.text = if(category.hasQuantity()){
            "Did you get ${category.displayName} on ${formattedDate}?"
        }
        else{
            "Did ${category.displayName} come on ${formattedDate}?"
        }
        binding.btnSave.isEnabled = true
        binding.formSection.visibility = View.VISIBLE

        if(category.hasQuantity() && entry.quantity > 0) {
            binding.etQuantity.setText(entry.quantity.toString())
        }
        if(entry.amount > 0){
            binding.etPayment.setText(entry.amount.toString())
        }
        if(!entry.remark.isNullOrBlank()){
            binding.etRemarks.setText(entry.remark)
        }
    }

    private fun isSameDay(timeStamp1: Long, timeStamp2: Long): Boolean{
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = timeStamp1
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val cal2 = Calendar.getInstance().apply {
            timeInMillis = timeStamp2
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }

    private fun updateSummary(entries: List<Entry>){
        val validEntries = entries.filter {it.amount != -1.0 && it.id != 0L}
        val totalAmount = validEntries.sumOf { it.amount}
        val totalQuantity = validEntries.sumOf { it.quantity }

        val summaryText = when{
            category.hasQuantity() ->{
                "Total: ₹${"%.2f".format(totalAmount)} | Quantity: ${"%.2f".format(totalQuantity)} ${category.getQuantity()} | Entries: ${validEntries.size}"
            }
            else ->{
                val yesCount = validEntries.size
                val noCount = entries.filter { it.amount == -1.0 }.size
                "Yes: $yesCount | No: $noCount | Entries: ${validEntries.filter { it.id != 0L }.size }"
            }
        }

        binding.tvSummary.text = summaryText
    }

}