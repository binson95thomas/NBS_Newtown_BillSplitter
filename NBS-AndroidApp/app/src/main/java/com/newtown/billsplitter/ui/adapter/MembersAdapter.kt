package com.newtown.billsplitter.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.newtown.billsplitter.databinding.ItemMemberBinding
import com.newtown.billsplitter.model.Member

class MembersAdapter(
    private val members: MutableList<Member> = mutableListOf(),
    private val onMemberClick: (Member) -> Unit = {},
    private val onMemberDelete: (Member) -> Unit = {}
) : RecyclerView.Adapter<MembersAdapter.MemberViewHolder>() {

    class MemberViewHolder(
        private val binding: ItemMemberBinding,
        private val onMemberClick: (Member) -> Unit,
        private val onMemberDelete: (Member) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Member) {
            binding.memberName.text = member.name
            binding.memberEmail.text = "Member #${member.id}"
            
            binding.root.setOnClickListener {
                onMemberClick(member)
            }
            
            binding.deleteButton.setOnClickListener {
                onMemberDelete(member)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding, onMemberClick, onMemberDelete)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size

    fun updateMembers(newMembers: List<Member>) {
        members.clear()
        members.addAll(newMembers)
        notifyDataSetChanged()
    }

    fun addMember(member: Member) {
        members.add(member)
        notifyItemInserted(members.size - 1)
    }

    fun removeMember(member: Member) {
        val position = members.indexOf(member)
        if (position != -1) {
            members.removeAt(position)
            notifyItemRemoved(position)
        }
    }
} 