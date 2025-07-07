package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.R
import com.newtown.billsplitter.model.Member

class MemberCheckboxAdapter(
    private var members: List<Member> = emptyList(),
    private var selectedMembers: List<String> = emptyList(),
    private val onMemberToggle: ((String, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<MemberCheckboxAdapter.MemberCheckboxViewHolder>() {

    class MemberCheckboxViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.memberCheckbox)
        val memberName: TextView = itemView.findViewById(R.id.memberName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberCheckboxViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_checkbox, parent, false)
        return MemberCheckboxViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberCheckboxViewHolder, position: Int) {
        val member = members[position]
        holder.memberName.text = member.name
        holder.checkbox.isChecked = selectedMembers.contains(member.name)
        
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onMemberToggle?.invoke(member.name, isChecked)
        }
    }

    override fun getItemCount(): Int = members.size

    fun updateMembers(newMembers: List<Member>, newSelectedMembers: List<String>) {
        members = newMembers
        selectedMembers = newSelectedMembers
        notifyDataSetChanged()
    }
} 