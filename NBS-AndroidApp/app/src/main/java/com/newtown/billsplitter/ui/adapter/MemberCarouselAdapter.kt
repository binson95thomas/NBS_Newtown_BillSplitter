package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.viewmodel.MainViewModel

class MemberCarouselAdapter(
    private var data: List<MainViewModel.MemberBreakdown>,
    private val onCopyClick: ((MainViewModel.MemberBreakdown) -> Unit)? = null
) : RecyclerView.Adapter<MemberCarouselAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val memberName: TextView = view.findViewById(R.id.memberName)
        val subtotalText: TextView = view.findViewById(R.id.subtotalText)
        val discountText: TextView = view.findViewById(R.id.discountText)
        val finalAmountText: TextView = view.findViewById(R.id.finalAmountText)
        val itemsText: TextView = view.findViewById(R.id.itemsText)
        val copyButton: ImageButton = view.findViewById(R.id.copyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_member_breakdown, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val breakdown = data[position]
        holder.memberName.text = breakdown.memberName
        holder.subtotalText.text = "Subtotal: £%.2f".format(breakdown.subtotal)
        holder.discountText.text = "Discount: -£%.2f".format(breakdown.discountShare)
        holder.finalAmountText.text = "Final: £%.2f".format(breakdown.finalAmount)
        val itemsList = breakdown.items.joinToString(", ") { "${it.name} (£%.2f)".format(it.price) }
        holder.itemsText.text = "Items: $itemsList"
        holder.copyButton.setOnClickListener { onCopyClick?.invoke(breakdown) }
    }

    fun update(newData: List<MainViewModel.MemberBreakdown>) {
        data = newData
        notifyDataSetChanged()
    }
}


