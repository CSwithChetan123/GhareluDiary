package com.ghareludiary.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ghareludiary.app.databinding.ItemEntryBinding
import com.ghareludiary.app.model.CategoryType
import com.ghareludiary.app.model.Entry
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EntryAdapter(
    private val category: CategoryType,
    private val onEntryClick: (Entry) -> Unit,
    private val onDeleteClick: (Entry) -> Unit
): ListAdapter<Entry, EntryAdapter.EntryViewHolder>(EntryDiffCallback()){


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EntryAdapter.EntryViewHolder {
        val binding = ItemEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding, category, onEntryClick, onDeleteClick)

    }

    override fun onBindViewHolder(holder: EntryAdapter.EntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class EntryViewHolder(
        private val binding: ItemEntryBinding,
        private val category: CategoryType,
        private val onEntryClick: (Entry) -> Unit,
        private val onDeleteClick: (Entry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: Entry){
            binding.tvDate.text = entry.getFormattedDate()

            val isBlankEntry = entry.id == 0L
            val isNotEntry = entry.amount == - 1.0

            when{
                isBlankEntry -> setUpBlankEntry()
                isNotEntry -> setUpNotEntry(entry)
                else -> setUpRegularEntry(entry)
            }

            binding.root.setOnClickListener { onEntryClick(entry) }
            binding.root.setOnLongClickListener {
                showDeleteDialog(entry)
                true
            }
            binding.btnDelete.setOnClickListener {
                showDeleteDialog(entry)
            }
        }

        private fun showDeleteDialog(entry: Entry){
            MaterialAlertDialogBuilder(binding.root.context)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("YES") { _, _ ->
                    onDeleteClick(entry)
                }
                .setNegativeButton("NO", null)
                .show()
        }

        private fun setUpBlankEntry(){
            binding.tvQuantity.visibility = View.GONE
            binding.tvAmount.visibility = View.GONE
            binding.tvRemarks.visibility = View.GONE

            binding.tvBlankEntry.visibility = View.VISIBLE
            binding.tvBlankEntry.text = "Tap to add Entry"
            binding.root.alpha = 0.5f
        }

        private fun setUpNotEntry(entry: Entry){
            binding.tvRemarks.visibility = View.GONE
            binding.tvQuantity.visibility = View.GONE

            binding.tvAmount.visibility = View.VISIBLE
            binding.tvAmount.text = "NO"
        }

        private fun setUpRegularEntry(entry: Entry){
            binding.tvBlankEntry.visibility = View.GONE

            if(category.hasQuantity() && entry.quantity > 0){
                binding.tvQuantity.visibility = View.VISIBLE
                binding.tvQuantity.text = "${entry.quantity} ${category.getQuantity()}"
            }
            else{
                binding.tvQuantity.visibility = View.GONE
            }

            setUpAmount(entry)

            if(!entry.remark.isNullOrBlank()){
                binding.tvRemarks.visibility = View.VISIBLE
                binding.tvRemarks.text = entry.remark
            }
            else{
                binding.tvRemarks.visibility = View.GONE
            }
        }

        private fun setUpAmount(entry: Entry){
            when{
                category.hasQuantity() && entry.amount > 0 ->{
                    binding.tvAmount.visibility = View.VISIBLE
                    binding.tvAmount.text = "₹${entry.amount}"
                }

                !category.hasQuantity() && entry.amount>0 ->{
                    binding.tvAmount.visibility = View.VISIBLE
                    binding.tvAmount.text = "YES | ${entry.amount}"
                }

                !category.hasQuantity() ->{
                    binding.tvAmount.visibility = View.VISIBLE
                    binding.tvAmount.text = "YES"
                }

                else ->{
                    binding.tvAmount.visibility = View.GONE
                }
            }
        }


    }

    class EntryDiffCallback: DiffUtil.ItemCallback<Entry>() {
        override fun areItemsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem.id == newItem.id && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Entry, newItem: Entry): Boolean {
            return oldItem == newItem
        }

    }

}

