package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.model.SavedBill
import java.text.SimpleDateFormat
import java.util.*

class SavedBillsAdapter(
    private var savedBills: List<SavedBill>,
    private val onBillSelected: (SavedBill) -> Unit
) : RecyclerView.Adapter<SavedBillsAdapter.SavedBillViewHolder>() {

    class SavedBillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val billNameText: TextView = itemView.findViewById(R.id.billNameText)
        val scanDateText: TextView = itemView.findViewById(R.id.scanDateText)
        val itemCountText: TextView = itemView.findViewById(R.id.itemCountText)
        val totalAmountText: TextView = itemView.findViewById(R.id.totalAmountText)
        val membersText: TextView = itemView.findViewById(R.id.membersText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedBillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_saved_bill, parent, false)
        return SavedBillViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedBillViewHolder, position: Int) {
        val savedBill = savedBills[position]
        
        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(savedBill.scanDate)
        
        // Set data
        holder.billNameText.text = savedBill.billName
        holder.scanDateText.text = formattedDate
        holder.itemCountText.text = "${savedBill.billItems.size} items"
        holder.totalAmountText.text = "£%.2f".format(savedBill.finalTotal)
        holder.membersText.text = savedBill.memberNames.joinToString(", ")
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onBillSelected(savedBill)
        }
    }

    override fun getItemCount(): Int = savedBills.size

    fun updateSavedBills(newSavedBills: List<SavedBill>) {
        savedBills = newSavedBills
        notifyDataSetChanged()
    }
}
