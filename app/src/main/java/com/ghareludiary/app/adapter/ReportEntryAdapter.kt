package com.ghareludiary.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ghareludiary.app.databinding.ItemReportEntryBinding
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.report.ReportEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ReportEntryAdapter: ListAdapter<ReportEntry, ReportEntryAdapter.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportEntryAdapter.ViewHolder {
        val binding = ItemReportEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)

    }

    override fun onBindViewHolder(holder: ReportEntryAdapter.ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemReportEntryBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(entry: ReportEntry) {
            val dateFormat = SimpleDateFormat("dd-MMM-yyyy EEE", Locale.getDefault())
            binding.tvDate.text = dateFormat.format(Date(entry.date))

            val categoryType = try {
                CategoryType.valueOf(entry.categoryName.uppercase())
            } catch (e: Exception){
                null

            }

            val displayValue = getDisplayValue(entry, categoryType)
            binding.tvValue.text = displayValue

        }

        private fun getDisplayValue(entry: ReportEntry, categoryType: CategoryType?): String{
            return when{
                !entry.hasEntry -> "No Entry"
                entry.isNoEntry -> "No"
                else ->{
                    if(categoryType?.hasQuantity() ?: false){

                        if(entry.quantity > 0){
                            "${entry.quantity} ${categoryType?.getQuantity()}"
                        } else {
                            "0.0"
                        }
                    }
                    else{
                        "YES"
                    }
                }
            }
        }
    }

    class DiffCallback: DiffUtil.ItemCallback<ReportEntry>(){
        override fun areItemsTheSame(
            oldItem: ReportEntry,
            newItem: ReportEntry
        ): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(
            oldItem: ReportEntry,
            newItem: ReportEntry
        ): Boolean {
            return oldItem == newItem
        }

    }


}