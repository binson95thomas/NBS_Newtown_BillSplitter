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
import com.newtown.billsplitter.utils.HapticUtils
import com.newtown.billsplitter.utils.AnimationUtils
import com.newtown.billsplitter.utils.UniversalShareUtils
import com.newtown.billsplitter.model.BillItem

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

        // Setup carousel RecyclerView with snap helper and haptic feedback
        carouselAdapter = MemberCarouselAdapter(emptyList()) { breakdown ->
            // Add haptic feedback for copy action
            HapticUtils.mediumTap(binding.root)
            copyToClipboard(breakdown)
        }
        binding.memberCarouselRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = carouselAdapter
            PagerSnapHelper().attachToRecyclerView(this)
            
            // Add smooth scrolling
            addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                        updateMemberPills()
                    }
                }
            })
        }

        // Setup recalculate button
        binding.recalculateButton.setOnClickListener {
            recalculateTotal()
        }
        
        // Setup universal share button
        binding.whatsappShareButton.setOnClickListener {
            HapticUtils.mediumTap(it)
            AnimationUtils.bounceButton(it)
            shareToAnyApp()
        }
        
        // Long press for message preview
        binding.whatsappShareButton.setOnLongClickListener {
            HapticUtils.strongTap(requireContext())
            AnimationUtils.pulseView(it, 2)
            showMessagePreview()
            true
        }
    }

    private fun setupDiscountInput() {
        // Save discount on every text change for better persistence
        binding.discountEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s?.isNotEmpty() == true) {
                    try {
                        val discount = s.toString().toDouble()
                        viewModel.setDiscountPercentage(discount)
                    } catch (e: NumberFormatException) {
                        // Invalid input, ignore
                    }
                }
            }
        })
        
        // Also handle Enter key for immediate calculation
        binding.discountEditText.setOnEditorActionListener { _, _, _ ->
            applyDiscount()
            true
        }
    }

    private fun observeViewModel() {
        viewModel.discountPercentage.observe(viewLifecycleOwner) { percentage -> 
            // Only update the input field if it's empty or different from the loaded value
            val safePercentage = percentage ?: 0.0
            val currentText = binding.discountEditText.text.toString()
            if (currentText.isEmpty() || currentText.toDoubleOrNull() != safePercentage) {
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
        setupMemberPills(breakdowns)
        
        // Show/hide empty state
        if (breakdowns.isEmpty()) {
            binding.memberCarouselRecyclerView.visibility = View.GONE
            binding.emptyStateContainer.visibility = View.VISIBLE
        } else {
            binding.memberCarouselRecyclerView.visibility = View.VISIBLE
            binding.emptyStateContainer.visibility = View.GONE
        }
    }
    
    private fun setupMemberPills(breakdowns: List<MainViewModel.MemberBreakdown>) {
        // Hide the separate pills container since pills are now integrated into cards
        binding.memberPillsContainer.removeAllViews()
        binding.memberPillsContainer.parent?.let { parent ->
            if (parent is ViewGroup) {
                (parent as ViewGroup).visibility = View.GONE
            }
        }
    }
    
             private fun createMemberPill(breakdown: MainViewModel.MemberBreakdown, isActive: Boolean): View {
        val pillButton = android.widget.Button(requireContext()).apply {
            text = breakdown.memberName
            textSize = 14f
            setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.WHITE)
            setPadding(16, 8, 16, 8)
                         setBackgroundResource(if (isActive) com.newtown.billsplitter.R.drawable.modern_member_pill_active_background else com.newtown.billsplitter.R.drawable.modern_member_pill_background)
            
            setOnClickListener {
                // Scroll to this member in the carousel
                val breakdowns = viewModel.getMemberBreakdowns()
                binding.memberCarouselRecyclerView.smoothScrollToPosition(breakdowns.indexOf(breakdown))
            }
        }
        
        val layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginEnd = 8
        }
        pillButton.layoutParams = layoutParams
        
        return pillButton
    }
    
    private fun updateMemberPills() {
        val layoutManager = binding.memberCarouselRecyclerView.layoutManager as? LinearLayoutManager
        val visiblePosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
        
                 // Update pill states
         for (i in 0 until binding.memberPillsContainer.childCount) {
             val pill = binding.memberPillsContainer.getChildAt(i) as? android.widget.Button
                           pill?.setBackgroundResource(if (i == visiblePosition) com.newtown.billsplitter.R.drawable.modern_member_pill_active_background else com.newtown.billsplitter.R.drawable.modern_member_pill_background)
         }
    }

    private fun copyToClipboard(breakdown: MainViewModel.MemberBreakdown) {
        val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val copyText = "${breakdown.memberName}: £${"%.2f".format(breakdown.finalAmount)}"
        val clip = ClipData.newPlainText("Member Amount", copyText)
        clipboardManager.setPrimaryClip(clip)
        
        // Success haptic and animation for copy action
        HapticUtils.successPattern(requireContext())
        
        // Show success toast with animation
        Toast.makeText(context, "✨ Copied to clipboard!", Toast.LENGTH_SHORT).show()
        
        // Add celebration animation to the current card
        val currentPosition = (binding.memberCarouselRecyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition() ?: 0
        val currentView = binding.memberCarouselRecyclerView.findViewHolderForAdapterPosition(currentPosition)?.itemView
        currentView?.let { view ->
            AnimationUtils.celebrationBurst(view, requireContext())
            AnimationUtils.pulseView(view, 2)
        }
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
    
    /**
     * Show message preview with option to share or copy
     */
    private fun showMessagePreview() {
        try {
            val billItems = viewModel.billItems.value ?: emptyList()
            val memberBreakdowns = viewModel.getMemberBreakdowns()
            val discountPercentage = viewModel.discountPercentage.value ?: 0.0
            
            // Calculate bill totals
            val subtotal = viewModel.getSubtotal()
            val discountAmount = viewModel.getDiscountAmount()
            val finalTotal = viewModel.getFinalTotal()
            
            // Validate that we have data to preview
            if (billItems.isEmpty()) {
                Toast.makeText(context, "No bill items to preview. Please add items first.", Toast.LENGTH_LONG).show()
                HapticUtils.errorPattern(requireContext())
                return
            }
            
            if (memberBreakdowns.isEmpty()) {
                Toast.makeText(context, "No member breakdowns available. Please add members first.", Toast.LENGTH_LONG).show()
                HapticUtils.errorPattern(requireContext())
                return
            }
            
            // Show preview dialog
            UniversalShareUtils.showMessagePreview(
                context = requireContext(),
                billItems = billItems,
                memberBreakdowns = memberBreakdowns,
                discountPercentage = discountPercentage,
                subtotal = subtotal,
                discountAmount = discountAmount,
                finalTotal = finalTotal
            ) {
                // Callback when user confirms to share
                shareToAnyAppDirect(billItems, memberBreakdowns, discountPercentage, subtotal, discountAmount, finalTotal)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("TotalsFragment", "Error showing message preview", e)
            Toast.makeText(context, "Failed to show preview: ${e.message}", Toast.LENGTH_LONG).show()
            HapticUtils.errorPattern(requireContext())
        }
    }
    
    /**
     * Share directly to any app (used by preview callback)
     */
    private fun shareToAnyAppDirect(
        billItems: List<BillItem>,
        memberBreakdowns: List<MainViewModel.MemberBreakdown>,
        discountPercentage: Double,
        subtotal: Double,
        discountAmount: Double,
        finalTotal: Double
    ) {
        UniversalShareUtils.shareBillToAnyApp(
            context = requireContext(),
            billItems = billItems,
            memberBreakdowns = memberBreakdowns,
            discountPercentage = discountPercentage,
            subtotal = subtotal,
            discountAmount = discountAmount,
            finalTotal = finalTotal
        )
        
        // Success feedback
        Toast.makeText(context, "🌊 Bill shared to any app!", Toast.LENGTH_SHORT).show()
        AnimationUtils.celebrationBurst(binding.whatsappShareButton, requireContext())
    }
    
    /**
     * Share bill breakdown to any app with beautiful formatting
     */
    private fun shareToAnyApp() {
        try {
            val billItems = viewModel.billItems.value ?: emptyList()
            val memberBreakdowns = viewModel.getMemberBreakdowns()
            val discountPercentage = viewModel.discountPercentage.value ?: 0.0
            
            // Calculate bill totals
            val subtotal = viewModel.getSubtotal()
            val discountAmount = viewModel.getDiscountAmount()
            val finalTotal = viewModel.getFinalTotal()
            
            // Validate that we have data to share
            if (billItems.isEmpty()) {
                Toast.makeText(context, "No bill items to share. Please add items first.", Toast.LENGTH_LONG).show()
                HapticUtils.errorPattern(requireContext())
                return
            }
            
            if (memberBreakdowns.isEmpty()) {
                Toast.makeText(context, "No member breakdowns available. Please add members first.", Toast.LENGTH_LONG).show()
                HapticUtils.errorPattern(requireContext())
                return
            }
            
            // Show loading animation during message preparation
            AnimationUtils.pulseView(binding.whatsappShareButton, 1)
            
            // Share to any app with beautiful formatting
            UniversalShareUtils.shareBillToAnyApp(
                context = requireContext(),
                billItems = billItems,
                memberBreakdowns = memberBreakdowns,
                discountPercentage = discountPercentage,
                subtotal = subtotal,
                discountAmount = discountAmount,
                finalTotal = finalTotal
            )
            
            // Success feedback
            Toast.makeText(context, "🌊 Bill shared to any app!", Toast.LENGTH_SHORT).show()
            
            // Celebration animation for successful share
            AnimationUtils.celebrationBurst(binding.whatsappShareButton, requireContext())
            
        } catch (e: Exception) {
            android.util.Log.e("TotalsFragment", "Error sharing to any app", e)
            Toast.makeText(context, "Failed to share bill: ${e.message}", Toast.LENGTH_LONG).show()
            HapticUtils.errorPattern(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 