package com.newtown.billsplitter.ui.fragments

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
import com.newtown.billsplitter.databinding.FragmentItemsBinding
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.ui.adapter.ItemsAdapter
import com.newtown.billsplitter.viewmodel.MainViewModel
import android.widget.TextView
import android.view.inputmethod.InputMethodManager
import com.newtown.billsplitter.utils.HapticUtils
import com.newtown.billsplitter.utils.AnimationUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class ItemsFragment : Fragment() {
    private var _binding: FragmentItemsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MainViewModel
    private lateinit var itemsAdapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        
        setupItemsList()
        setupAddItemButton()
        observeViewModel()
    }

    private fun setupItemsList() {
        itemsAdapter = ItemsAdapter(
            onItemClick = { item -> },
            onEditItem = { item -> showEditItemDialog(item) },
            onDeleteItem = { item ->
                viewModel.removeBillItem(item)
            },
            onMemberToggle = { item, memberId, isAssigned ->
                viewModel.toggleItemMemberAssignment(item, memberId, isAssigned)
            },
            onSplitEvenly = { updatedItem ->
                viewModel.updateBillItem(updatedItem)
            }
        )
        binding.itemsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = itemsAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.billItems.observe(viewLifecycleOwner) { items ->
            itemsAdapter.updateItems(items)
            updateEmptyState(items.isEmpty())
            
            // Calculate and display totals
            val subtotal = items.filter { !it.isDealOrDiscount() }.sumOf { it.price }
            val deals = items.filter { it.itemType == "deal" }.sumOf { kotlin.math.abs(it.price) }
            val discounts = items.filter { it.itemType == "discount" }.sumOf { kotlin.math.abs(it.price) }
            
            binding.totalAmountText.text = "Total: £%.2f".format(subtotal)
            
            // Show deals and discounts if they exist
            if (deals > 0 || discounts > 0) {
                binding.dealsInfoText.visibility = View.VISIBLE
                val dealsText = if (deals > 0) "Deals: £%.2f".format(deals) else ""
                val discountsText = if (discounts > 0) "Discounts: £%.2f".format(discounts) else ""
                binding.dealsInfoText.text = listOf(dealsText, discountsText).filter { it.isNotEmpty() }.joinToString(" | ")
            } else {
                binding.dealsInfoText.visibility = View.GONE
            }
        }
        
        viewModel.members.observe(viewLifecycleOwner) { members ->
            itemsAdapter.updateMembers(members)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.itemsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun setupAddItemButton() {
        binding.addItemButton.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            showAddItemDialog()
        }
        binding.clearAllButton.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Clear All Items")
                .setMessage("Are you sure you want to remove all items? Members will be preserved.")
                .setPositiveButton("Clear All") { _, _ ->
                    viewModel.clearItemsOnly()
                    itemsAdapter.updateItems(emptyList())
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showAddItemDialog() {
        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.newtown.billsplitter.R.layout.dialog_add_item, null
        )
        
        // Get references to views
        val nameEditText = dialogView.findViewById<TextInputEditText>(com.newtown.billsplitter.R.id.itemNameEditText)
        val priceEditText = dialogView.findViewById<TextInputEditText>(com.newtown.billsplitter.R.id.itemPriceEditText)
        val quickPrice1 = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.quickPrice1)
        val quickPrice2 = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.quickPrice2)
        val quickPrice3 = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.quickPrice3)
        val cancelButton = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.cancelButton)
        val addButton = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.addButton)
        
        // Create dialog with no title (we have custom header)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Setup quick price buttons with haptic feedback
        quickPrice1.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            priceEditText.setText("2.50")
        }
        
        quickPrice2.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            priceEditText.setText("5.00")
        }
        
        quickPrice3.setOnClickListener {
            HapticUtils.lightTap(it)
            AnimationUtils.bounceButton(it)
            priceEditText.setText("10.00")
        }
        
        // Setup cancel button
        cancelButton.setOnClickListener {
            HapticUtils.lightTap(it)
            dialog.dismiss()
        }
        
        // Setup add button
        addButton.setOnClickListener {
            HapticUtils.mediumTap(it)
            AnimationUtils.bounceButton(it)
            
            val name = nameEditText.text.toString().trim()
            val priceText = priceEditText.text.toString().trim()
            
            when {
                name.isEmpty() -> {
                    nameEditText.error = "Item name is required"
                    HapticUtils.errorPattern(requireContext())
                    return@setOnClickListener
                }
                priceText.isEmpty() -> {
                    priceEditText.error = "Price is required"
                    HapticUtils.errorPattern(requireContext())
                    return@setOnClickListener
                }
                else -> {
                    try {
                        val price = priceText.toDouble()
                        if (price <= 0) {
                            priceEditText.error = "Price must be greater than 0"
                            HapticUtils.errorPattern(requireContext())
                            return@setOnClickListener
                        }
                        
                        // Success - add the item
                        val newItem = BillItem(
                            id = System.currentTimeMillis(),
                            name = name,
                            price = price
                        )
                        viewModel.addBillItem(newItem)
                        
                        // Success feedback
                        HapticUtils.successPattern(requireContext())
                        Toast.makeText(context, "🌊 Item added successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        
                    } catch (e: NumberFormatException) {
                        priceEditText.error = "Please enter a valid number (e.g., 5.99)"
                        HapticUtils.errorPattern(requireContext())
                    }
                }
            }
        }
        
        // Show dialog
        dialog.show()
        
        // Add entrance animation
        AnimationUtils.slideInFromBottom(dialogView)
        
        // Focus on name field and show keyboard
        nameEditText.requestFocus()
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showEditItemDialog(item: BillItem) {
        // Create dialog with custom layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            com.newtown.billsplitter.R.layout.dialog_edit_item, null
        )
        
        // Get references to views
        val nameEditText = dialogView.findViewById<TextInputEditText>(com.newtown.billsplitter.R.id.itemNameEditText)
        val priceEditText = dialogView.findViewById<TextInputEditText>(com.newtown.billsplitter.R.id.itemPriceEditText)
        val cancelButton = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.cancelButton)
        val updateButton = dialogView.findViewById<MaterialButton>(com.newtown.billsplitter.R.id.updateButton)
        
        // Pre-fill with current values
        nameEditText.setText(item.name)
        priceEditText.setText(item.price.toString())
        
        // Create dialog with no title (we have custom header)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Setup cancel button
        cancelButton.setOnClickListener {
            HapticUtils.lightTap(it)
            dialog.dismiss()
        }
        
        // Setup update button
        updateButton.setOnClickListener {
            HapticUtils.mediumTap(it)
            AnimationUtils.bounceButton(it)
            
            val name = nameEditText.text.toString().trim()
            val priceText = priceEditText.text.toString().trim()
            
            when {
                name.isEmpty() -> {
                    nameEditText.error = "Item name is required"
                    HapticUtils.errorPattern(requireContext())
                    return@setOnClickListener
                }
                priceText.isEmpty() -> {
                    priceEditText.error = "Price is required"
                    HapticUtils.errorPattern(requireContext())
                    return@setOnClickListener
                }
                else -> {
                    try {
                        val price = priceText.toDouble()
                        if (price <= 0) {
                            priceEditText.error = "Price must be greater than 0"
                            HapticUtils.errorPattern(requireContext())
                            return@setOnClickListener
                        }
                        
                        // Success - update the item
                        val updatedItem = item.copy(
                            name = name,
                            price = price
                        )
                        viewModel.updateBillItem(updatedItem)
                        
                        // Success feedback
                        HapticUtils.successPattern(requireContext())
                        Toast.makeText(context, "🌊 Item updated successfully!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        
                    } catch (e: NumberFormatException) {
                        priceEditText.error = "Please enter a valid number (e.g., 5.99)"
                        HapticUtils.errorPattern(requireContext())
                    }
                }
            }
        }
        
        // Show dialog
        dialog.show()
        
        // Add entrance animation
        AnimationUtils.slideInFromBottom(dialogView)
        
        // Focus on name field and show keyboard
        nameEditText.requestFocus()
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(nameEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 