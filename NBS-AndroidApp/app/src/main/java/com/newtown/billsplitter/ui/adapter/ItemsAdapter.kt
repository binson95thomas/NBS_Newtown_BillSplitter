package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.newtown.billsplitter.R
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.model.Member
import android.graphics.drawable.GradientDrawable
import android.widget.Button
import android.widget.LinearLayout

class ItemsAdapter(
    private var items: List<BillItem> = emptyList(),
    private var members: List<Member> = emptyList(),
    private val onItemClick: ((BillItem) -> Unit)? = null,
    private val onEditItem: ((BillItem) -> Unit)? = null,
    private val onDeleteItem: ((BillItem) -> Unit)? = null,
    private val onMemberToggle: ((BillItem, Long, Boolean) -> Unit)? = null,
    private val onSplitEvenly: ((BillItem) -> Unit)? = null
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val editItemButton: ImageButton = itemView.findViewById(R.id.editItemButton)
        val deleteItemButton: ImageButton = itemView.findViewById(R.id.deleteItemButton)
        val memberCheckboxContainer: FlexboxLayout = itemView.findViewById(R.id.memberCheckboxContainer)
        val splitEvenlyButton: Button = itemView.findViewById(R.id.splitEvenlyButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bill, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemName.text = item.name
        holder.itemPrice.text = item.getDisplayPrice()
        
        // Style deals and discounts differently
        if (item.price < 0) {
            // Negative items are deals/discounts - show in orange/red
            holder.itemPrice.setTextColor(android.graphics.Color.parseColor("#F59E0B")) // Orange for deals
            holder.itemName.setTextColor(android.graphics.Color.parseColor("#D97706")) // Darker orange
        } else if (item.isDealOrDiscount()) {
            holder.itemPrice.setTextColor(android.graphics.Color.parseColor("#10B981")) // Green for other deals
            holder.itemName.setTextColor(android.graphics.Color.parseColor("#059669")) // Darker green
        } else {
            holder.itemPrice.setTextColor(android.graphics.Color.parseColor("#4FACFE")) // Blue for regular items
            holder.itemName.setTextColor(android.graphics.Color.parseColor("#1E293B")) // Dark for regular items
        }
        
        // Setup member checkboxes and avatars
        setupMemberCheckboxes(holder.memberCheckboxContainer, item, false)
        // Setup Split Evenly button
        setupSplitEvenlyButton(holder, item)
        // Setup edit button
        holder.editItemButton.setOnClickListener {
            onEditItem?.invoke(item)
        }
        holder.deleteItemButton.setOnClickListener {
            onDeleteItem?.invoke(item)
        }
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    private fun setupSplitEvenlyButton(holder: ItemViewHolder, item: BillItem) {
        val allAssigned = members.all { item.assignedTo.contains(it.id) }
        holder.splitEvenlyButton.isSelected = allAssigned
        holder.splitEvenlyButton.alpha = 1.0f
        holder.splitEvenlyButton.setOnClickListener {
            if (!allAssigned) {
                // Check all
                onSplitEvenly?.invoke(item.copy(assignedTo = members.map { it.id }))
                setupMemberCheckboxes(holder.memberCheckboxContainer, item.copy(assignedTo = members.map { it.id }), true)
            } else {
                // Uncheck all
                onSplitEvenly?.invoke(item.copy(assignedTo = emptyList()))
                setupMemberCheckboxes(holder.memberCheckboxContainer, item.copy(assignedTo = emptyList()), true)
            }
        }
    }

    private fun setupMemberCheckboxes(container: FlexboxLayout, item: BillItem, animate: Boolean) {
        container.removeAllViews()
        members.forEach { member ->
            val checkbox = CheckBox(container.context).apply {
                text = member.emoji?.let { "${it} ${member.name}" } ?: member.name
                isChecked = item.assignedTo.contains(member.id)
                setOnCheckedChangeListener { _, isChecked ->
                    onMemberToggle?.invoke(item, member.id, isChecked)
                }
                setTextColor(member.color)
                buttonTintList = android.content.res.ColorStateList.valueOf(member.color)
                // Make checkbox and text bigger
                textSize = 16f
                minHeight = 48
                // Add padding for better touch area
                setPadding(8, 8, 8, 8)
            }
            container.addView(checkbox)
            if (animate && item.assignedTo.contains(member.id)) {
                checkbox.scaleX = 0.7f
                checkbox.scaleY = 0.7f
                checkbox.animate().scaleX(1.1f).scaleY(1.1f).setDuration(120).withEndAction {
                    checkbox.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }.start()/**/
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<BillItem>) {
        val oldItems = items.toList()
        items = newItems
        
        // Use notifyItemChanged for better performance and state preservation
        if (oldItems.size == newItems.size) {
            // Same size, update each item individually
            for (i in newItems.indices) {
                if (i < oldItems.size && oldItems[i] != newItems[i]) {
                    notifyItemChanged(i)
                }
            }
        } else {
            // Different size, use notifyDataSetChanged
            notifyDataSetChanged()
        }
    }
    
    fun updateMembers(newMembers: List<Member>) {
        members = newMembers
        notifyDataSetChanged()
    }
} 