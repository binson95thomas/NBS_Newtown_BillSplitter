package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.model.Member

class ItemsAdapter(
    private var items: List<BillItem> = emptyList(),
    private var members: List<Member> = emptyList(),
    private val onItemClick: ((BillItem) -> Unit)? = null,
    private val onEditItem: ((BillItem) -> Unit)? = null,
    private val onMemberToggle: ((BillItem, String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<ItemsAdapter.ItemViewHolder>() {

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemName: TextView = itemView.findViewById(R.id.itemName)
        val itemPrice: TextView = itemView.findViewById(R.id.itemPrice)
        val editItemButton: ImageButton = itemView.findViewById(R.id.editItemButton)
        val memberCheckboxContainer: LinearLayout = itemView.findViewById(R.id.memberCheckboxContainer)
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
        
        // Setup member checkboxes
        setupMemberCheckboxes(holder.memberCheckboxContainer, item)
        
        // Setup edit button
        holder.editItemButton.setOnClickListener {
            onEditItem?.invoke(item)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    private fun setupMemberCheckboxes(container: LinearLayout, item: BillItem) {
        container.removeAllViews()
        
        members.forEach { member ->
            val checkbox = CheckBox(container.context).apply {
                text = member.name
                isChecked = item.assignedTo.contains(member.name)
                setOnCheckedChangeListener { _, isChecked ->
                    onMemberToggle?.invoke(item, member.name, isChecked)
                }
                setTextColor(container.context.getColor(R.color.blue_700))
                buttonTintList = android.content.res.ColorStateList.valueOf(
                    container.context.getColor(R.color.blue_600)
                )
            }
            container.addView(checkbox)
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<BillItem>) {
        items = newItems
        notifyDataSetChanged()
    }
    
    fun updateMembers(newMembers: List<Member>) {
        members = newMembers
        notifyDataSetChanged()
    }
} 