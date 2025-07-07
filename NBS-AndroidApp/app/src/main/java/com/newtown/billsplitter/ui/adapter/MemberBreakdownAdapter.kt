package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.viewmodel.MainViewModel

class MemberBreakdownAdapter(
    private val breakdowns: List<MainViewModel.MemberBreakdown>,
    private val onCopyClick: ((MainViewModel.MemberBreakdown) -> Unit)? = null
) : RecyclerView.Adapter<MemberBreakdownAdapter.BreakdownViewHolder>() {

    class BreakdownViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val memberName: TextView = itemView.findViewById(R.id.memberName)
        val subtotalText: TextView = itemView.findViewById(R.id.subtotalText)
        val discountText: TextView = itemView.findViewById(R.id.discountText)
        val finalAmountText: TextView = itemView.findViewById(R.id.finalAmountText)
        val itemsText: TextView = itemView.findViewById(R.id.itemsText)
        val copyButton: ImageButton = itemView.findViewById(R.id.copyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BreakdownViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_breakdown, parent, false)
        return BreakdownViewHolder(view)
    }

    override fun onBindViewHolder(holder: BreakdownViewHolder, position: Int) {
        val breakdown = breakdowns[position]
        
        android.util.Log.d("MemberBreakdownAdapter", "Binding position $position: ${breakdown.memberName}")
        
        holder.memberName.text = breakdown.memberName
        holder.subtotalText.text = "Subtotal: £%.2f".format(breakdown.subtotal)
        holder.discountText.text = "Discount: -£%.2f".format(breakdown.discountShare)
        holder.finalAmountText.text = "Final: £%.2f".format(breakdown.finalAmount)
        
        // Show items breakdown
        val itemsList = breakdown.items.joinToString(", ") { "${it.name} (£%.2f)".format(it.price) }
        holder.itemsText.text = "Items: $itemsList"
        
        holder.copyButton.setOnClickListener {
            onCopyClick?.invoke(breakdown)
        }
    }

    override fun getItemCount(): Int {
        android.util.Log.d("MemberBreakdownAdapter", "getItemCount: ${breakdowns.size}")
        return breakdowns.size
    }
} 