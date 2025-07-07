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
            onMemberToggle = { item, memberName, isAssigned ->
                viewModel.toggleItemMemberAssignment(item, memberName, isAssigned)
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
            binding.totalAmountText.text = "Total: $%.2f".format(items.sumOf { it.price })
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
    }

    private fun showAddItemDialog() {
        val nameEditText = EditText(context).apply {
            hint = "Item name"
        }
        val priceEditText = EditText(context).apply {
            hint = "Price (e.g., 5.99)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val dialogLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(nameEditText)
            addView(priceEditText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add New Item")
            .setView(dialogLayout)
            .setPositiveButton("Add") { _, _ ->
                val name = nameEditText.text.toString()
                val priceText = priceEditText.text.toString()
                
                if (name.isNotEmpty() && priceText.isNotEmpty()) {
                    try {
                        val price = priceText.toDouble()
                        val newItem = BillItem(
                            id = System.currentTimeMillis(),
                            name = name,
                            price = price
                        )
                        viewModel.addBillItem(newItem)
                        Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog(item: BillItem) {
        val nameEditText = EditText(context).apply {
            hint = "Item name"
            setText(item.name)
        }
        val priceEditText = EditText(context).apply {
            hint = "Price (e.g., 5.99)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(item.price.toString())
        }

        val dialogLayout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            addView(nameEditText)
            addView(priceEditText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Item")
            .setView(dialogLayout)
            .setPositiveButton("Update") { _, _ ->
                val name = nameEditText.text.toString()
                val priceText = priceEditText.text.toString()
                
                if (name.isNotEmpty() && priceText.isNotEmpty()) {
                    try {
                        val price = priceText.toDouble()
                        val updatedItem = item.copy(
                            name = name,
                            price = price
                        )
                        viewModel.updateBillItem(updatedItem)
                        Toast.makeText(context, "Item updated successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: NumberFormatException) {
                        Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 