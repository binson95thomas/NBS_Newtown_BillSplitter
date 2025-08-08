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
        val nameEditText = EditText(context).apply {
            hint = "Enter item name (e.g., Pizza, Coffee)"
            setPadding(20, 16, 20, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(android.graphics.Color.parseColor("#F5F5F5"))
                setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
            }
        }
        val priceEditText = EditText(context).apply {
            hint = "Enter price (e.g., 5.99)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(20, 16, 20, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(android.graphics.Color.parseColor("#F5F5F5"))
                setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
            }
        }

        val dialogLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            
            // Title for name
            val nameLabel = TextView(context).apply {
                text = "Item Name"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(0, 0, 0, 8)
            }
            addView(nameLabel)
            addView(nameEditText)
            
            // Spacing
            val spacing1 = View(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    24
                )
            }
            addView(spacing1)
            
            // Title for price
            val priceLabel = TextView(context).apply {
                text = "Price (£)"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(0, 0, 0, 8)
            }
            addView(priceLabel)
            addView(priceEditText)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("➕ Add New Item")
            .setView(dialogLayout)
            .setPositiveButton("Add Item") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val priceText = priceEditText.text.toString().trim()
                
                when {
                    name.isEmpty() -> {
                        Toast.makeText(context, "Please enter an item name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    priceText.isEmpty() -> {
                        Toast.makeText(context, "Please enter a price", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    else -> {
                        try {
                            val price = priceText.toDouble()
                            if (price <= 0) {
                                Toast.makeText(context, "Price must be greater than 0", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            val newItem = BillItem(
                                id = System.currentTimeMillis(),
                                name = name,
                                price = price
                            )
                            viewModel.addBillItem(newItem)
                            Toast.makeText(context, "✅ Item added successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(context, "Please enter a valid price (e.g., 5.99)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Focus on name field and show keyboard
        nameEditText.requestFocus()
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(nameEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    private fun showEditItemDialog(item: BillItem) {
        val nameEditText = EditText(context).apply {
            hint = "Enter item name (e.g., Pizza, Coffee)"
            setText(item.name)
            setPadding(20, 16, 20, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(android.graphics.Color.parseColor("#F5F5F5"))
                setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
            }
        }
        val priceEditText = EditText(context).apply {
            hint = "Enter price (e.g., 5.99)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.price.toString())
            setPadding(20, 16, 20, 16)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 12f
                setColor(android.graphics.Color.parseColor("#F5F5F5"))
                setStroke(2, android.graphics.Color.parseColor("#E0E0E0"))
            }
        }

        val dialogLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
            
            // Title for name
            val nameLabel = TextView(context).apply {
                text = "Item Name"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(0, 0, 0, 8)
            }
            addView(nameLabel)
            addView(nameEditText)
            
            // Spacing
            val spacing1 = View(context).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    24
                )
            }
            addView(spacing1)
            
            // Title for price
            val priceLabel = TextView(context).apply {
                text = "Price (£)"
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#666666"))
                setPadding(0, 0, 0, 8)
            }
            addView(priceLabel)
            addView(priceEditText)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("✏️ Edit Item")
            .setView(dialogLayout)
            .setPositiveButton("Update Item") { _, _ ->
                val name = nameEditText.text.toString().trim()
                val priceText = priceEditText.text.toString().trim()
                
                when {
                    name.isEmpty() -> {
                        Toast.makeText(context, "Please enter an item name", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    priceText.isEmpty() -> {
                        Toast.makeText(context, "Please enter a price", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    else -> {
                        try {
                            val price = priceText.toDouble()
                            if (price <= 0) {
                                Toast.makeText(context, "Price must be greater than 0", Toast.LENGTH_SHORT).show()
                                return@setPositiveButton
                            }
                            val updatedItem = item.copy(
                                name = name,
                                price = price
                            )
                            viewModel.updateBillItem(updatedItem)
                            Toast.makeText(context, "✅ Item updated successfully", Toast.LENGTH_SHORT).show()
                        } catch (e: NumberFormatException) {
                            Toast.makeText(context, "Please enter a valid price (e.g., 5.99)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
        
        // Focus on name field and show keyboard
        nameEditText.requestFocus()
        val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(nameEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 