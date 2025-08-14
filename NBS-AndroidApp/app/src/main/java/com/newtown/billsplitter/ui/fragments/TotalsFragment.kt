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
import androidx.recyclerview.widget.PagerSnapHelper
import com.newtown.billsplitter.ui.adapter.MemberCarouselAdapter
import com.newtown.billsplitter.databinding.FragmentTotalsBinding
import com.newtown.billsplitter.ui.adapter.MemberBreakdownAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel

class TotalsFragment : Fragment() {
    private var _binding: FragmentTotalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private lateinit var breakdownAdapter: MemberBreakdownAdapter
    private var carouselAdapter: MemberCarouselAdapter? = null

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

        // Setup carousel RecyclerView with snap helper
        carouselAdapter = MemberCarouselAdapter(emptyList()) { breakdown ->
            copyToClipboard(breakdown)
        }
        binding.memberCarouselRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }

        // Setup recalculate button
        binding.recalculateButton.setOnClickListener {
            recalculateTotal()
        }
    }

    private fun setupDiscountInput() {
        // Only recalculate on editor action (Enter key) or manual button click
        binding.discountEditText.setOnEditorActionListener { _, _, _ ->
            applyDiscount()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.discountPercentage.observe(viewLifecycleOwner) { percentage -> 
            // Update the input field to show the current discount percentage
            val safePercentage = percentage ?: 0.0
            if (binding.discountEditText.text.toString() != safePercentage.toString()) {
                binding.discountEditText.setText(safePercentage.toString())
            }
            updateBillSummary() 
        }
        viewModel.members.observe(viewLifecycleOwner) { 
            updateMemberBreakdowns() 
        }
        viewModel.billItems.observe(viewLifecycleOwner) { 
            updateBillSummary() // Add this to recalculate when items change
            updateMemberBreakdowns() 
        }
    }

    private fun updateBillSummary() {
        val subtotal = viewModel.getSubtotal()
        val discountAmount = viewModel.getDiscountAmount()
        val finalTotal = viewModel.getFinalTotal()
        val discountPercentage = viewModel.getDiscountPercentageSafe()
        
        // Calculate deals and discounts from items
        val items = viewModel.billItems.value ?: emptyList()
        val deals = items.filter { it.itemType == "deal" }
        val discounts = items.filter { it.itemType == "discount" }
        val totalDeals = deals.sumOf { kotlin.math.abs(it.price) }
        val totalDiscounts = discounts.sumOf { kotlin.math.abs(it.price) }
        
        android.util.Log.d("TotalsFragment", "updateBillSummary: subtotal=$subtotal, discountAmount=$discountAmount, finalTotal=$finalTotal")
        android.util.Log.d("TotalsFragment", "updateBillSummary: items count=${items.size}, deals count=${deals.size}, discounts count=${discounts.size}")
        
        binding.subtotalText.text = "£%.2f".format(subtotal)
        binding.discountText.text = "- £%.2f (%.1f%%)".format(discountAmount, discountPercentage)
        binding.totalText.text = "£%.2f".format(finalTotal)
        
        // Show deals and discounts if they exist
        if (deals.isNotEmpty() || discounts.isNotEmpty()) {
            binding.dealsDiscountsCard.visibility = View.VISIBLE
            val dealsText = if (deals.isNotEmpty()) {
                "Deals Applied: £%.2f".format(totalDeals)
            } else ""
            val discountsText = if (discounts.isNotEmpty()) {
                "Discounts: £%.2f".format(totalDiscounts)
            } else ""
            binding.dealsDiscountsText.text = listOf(dealsText, discountsText).filter { it.isNotEmpty() }.joinToString("\n")
        } else {
            binding.dealsDiscountsCard.visibility = View.GONE
        }
    }

    private fun updateMemberBreakdowns() {
        val breakdowns = viewModel.getMemberBreakdowns()
        carouselAdapter?.update(breakdowns)
        if (breakdowns.isNotEmpty()) {
            binding.memberPagerTitle.text = breakdowns.first().memberName
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
                updateMemberBreakdowns()
            } catch (e: NumberFormatException) {
                // Invalid input, treat as 0%
                viewModel.setDiscountPercentage(0.0)
                updateBillSummary()
                updateMemberBreakdowns()
            }
        } else {
            // Empty text means 0%
            viewModel.setDiscountPercentage(0.0)
            updateBillSummary()
            updateMemberBreakdowns()
        }
    }

    private fun recalculateTotal() {
        android.util.Log.d("TotalsFragment", "Recalculating total from items...")
        
        // First apply the current discount percentage from the input field
        applyDiscount()
        
        // Then force recalculation from items using existing ViewModel methods
        // This will use the current formula: Final Total = Subtotal - Discount Amount
        updateBillSummary()
        updateMemberBreakdowns()
        
        // Show feedback to user
        Toast.makeText(context, "Total recalculated using current formula", Toast.LENGTH_SHORT).show()
        
        android.util.Log.d("TotalsFragment", "Recalculation complete")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 