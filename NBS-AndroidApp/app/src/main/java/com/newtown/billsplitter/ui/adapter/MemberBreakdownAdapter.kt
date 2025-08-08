package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.viewmodel.MainViewModel
import android.graphics.drawable.GradientDrawable

class MemberBreakdownAdapter(
    private var breakdowns: List<MainViewModel.MemberBreakdown>,
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
        
        // Set avatar color using a fallback color based on member name
        val memberColor = breakdown.memberName.hashCode().let { hash ->
            val colors = listOf(
                0xFF64B5F6.toInt(), // Blue
                0xFF81C784.toInt(), // Green
                0xFFFFB74D.toInt(), // Orange
                0xFFF06292.toInt(), // Pink
                0xFF9575CD.toInt(), // Purple
                0xFF4FC3F7.toInt(), // Light Blue
                0xFFFF8A65.toInt(), // Deep Orange
                0xFF4DB6AC.toInt()  // Teal
            )
            colors[kotlin.math.abs(hash) % colors.size]
        }
        
        val avatarDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(memberColor)
        }
        holder.itemView.findViewById<View>(R.id.memberAvatar)?.background = avatarDrawable
        
        // Show initials (no emoji support in MemberBreakdown)
        val avatarText = breakdown.memberName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        
        var avatarTextView = (holder.itemView.findViewWithTag("avatarText") as? TextView)
        if (avatarTextView == null) {
            avatarTextView = TextView(holder.itemView.context).apply {
                tag = "avatarText"
                textSize = 18f
                setTextColor(android.graphics.Color.WHITE)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                layoutParams = holder.itemView.findViewById<View>(R.id.memberAvatar)?.layoutParams
            }
            (holder.itemView.findViewById<View>(R.id.memberAvatar)?.parent as? ViewGroup)?.addView(avatarTextView)
        }
        avatarTextView.text = avatarText
        avatarTextView.bringToFront()
        
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

    fun updateData(newBreakdowns: List<MainViewModel.MemberBreakdown>) {
        (this as RecyclerView.Adapter<*>).apply {
            (this@MemberBreakdownAdapter).breakdowns = newBreakdowns
            notifyDataSetChanged()
        }
    }
} 