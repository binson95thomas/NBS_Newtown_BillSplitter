package com.newtown.billsplitter.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.newtown.billsplitter.R
import com.newtown.billsplitter.databinding.FragmentMembersBinding
import com.newtown.billsplitter.model.Member
import com.newtown.billsplitter.ui.adapter.MembersAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel

class MembersFragment : Fragment() {
    
    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: MainViewModel
    private lateinit var membersAdapter: MembersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setupRecyclerView()
        setupAddMemberButton()
        observeData()
    }
    
    private fun setupRecyclerView() {
        membersAdapter = MembersAdapter(
            onMemberDelete = { member -> viewModel.removeMember(member) }
        )
        binding.membersRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = membersAdapter
        }
    }

    private fun setupAddMemberButton() {
        binding.addMemberButton.setOnClickListener {
            // Show add member dialog from MainActivity or here
            (activity as? com.newtown.billsplitter.MainActivity)?.let {
                it.showAddMemberDialog()
            }
        }
    }
    
    private fun observeData() {
        viewModel.members.observe(viewLifecycleOwner) { members ->
            membersAdapter.updateMembers(members)
            binding.memberCountText.text = "${members.size} members"
            updateEmptyState(members.isEmpty())
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.membersRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 