package com.newtown.billsplitter.ui.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.newtown.billsplitter.databinding.FragmentTotalsBinding
import com.newtown.billsplitter.ui.adapter.MemberBreakdownAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel

class TotalsFragment : Fragment() {
    private var _binding: FragmentTotalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTotalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        setupDiscountInput()
        observeViewModel()
    }

    private fun setupDiscountInput() {
        binding.applyDiscountButton.setOnClickListener {
            applyDiscount()
        }
        
        binding.discountEditText.setOnEditorActionListener { _, _, _ ->
            applyDiscount()
            true
        }
        
        binding.discountEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                applyDiscount()
            }
        }
        
        // Add text change listener for real-time updates
        binding.discountEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString().isNotEmpty()) {
                    applyDiscount()
                }
            }
        })
    }

    private fun observeViewModel() {
        viewModel.totalAmount.observe(viewLifecycleOwner) { 
            updateBillSummary() 
        }
        viewModel.discountPercentage.observe(viewLifecycleOwner) { 
            updateBillSummary() 
        }
        viewModel.members.observe(viewLifecycleOwner) { 
            updateMemberBreakdowns() 
        }
        viewModel.billItems.observe(viewLifecycleOwner) { 
            updateMemberBreakdowns() 
        }
    }

    private fun updateBillSummary() {
        val subtotal = viewModel.getSubtotal()
        val discountAmount = viewModel.getDiscountAmount()
        val finalTotal = viewModel.getFinalTotal()
        val discountPercentage = viewModel.discountPercentage.value ?: 0.0
        
        binding.subtotalText.text = "$%.2f".format(subtotal)
        binding.discountText.text = "- $%.2f (%.1f%%)".format(discountAmount, discountPercentage)
        binding.totalText.text = "$%.2f".format(finalTotal)
    }

    private fun updateMemberBreakdowns() {
        val breakdowns = viewModel.getMemberBreakdowns()
        
        binding.memberBreakdownRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = MemberBreakdownAdapter(breakdowns) { breakdown ->
                copyToClipboard(breakdown)
            }
        }
    }

    private fun copyToClipboard(breakdown: MainViewModel.MemberBreakdown) {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copyText = "${breakdown.memberName}: $%.2f".format(breakdown.finalAmount)
        val clip = ClipData.newPlainText("Member Amount", copyText)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun applyDiscount() {
        val discountText = binding.discountEditText.text.toString()
        if (discountText.isNotEmpty()) {
            try {
                val discount = discountText.toDouble()
                viewModel.setDiscountPercentage(discount)
                // Force update the UI
                updateBillSummary()
            } catch (e: NumberFormatException) {
                // Invalid input, ignore
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 