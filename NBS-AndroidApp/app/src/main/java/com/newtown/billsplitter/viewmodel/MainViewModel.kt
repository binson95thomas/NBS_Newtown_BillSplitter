package com.newtown.billsplitter.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.model.Member
import com.newtown.billsplitter.model.MEMBER_COLORS
import com.newtown.billsplitter.service.GeminiService
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val _members = MutableLiveData<List<Member>>()
    val members: LiveData<List<Member>> = _members
    
    private val _billItems = MutableLiveData<List<BillItem>>()
    val billItems: LiveData<List<BillItem>> = _billItems
    
    private val _totalAmount = MutableLiveData<Double>()
    val totalAmount: LiveData<Double> = _totalAmount

    private val _discountPercentage = MutableLiveData<Double>()
    val discountPercentage: LiveData<Double> = _discountPercentage

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private var sharedPreferences: SharedPreferences? = null
    private val gson = Gson()
    private var geminiService: GeminiService? = null

    init {
        _members.value = emptyList()
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        _discountPercentage.value = 0.0
        _isProcessing.value = false
    }

    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences("BillSplitterPrefs", Context.MODE_PRIVATE)
        geminiService = GeminiService(context)
        loadMembers()
    }

    private fun loadMembers() {
        val prefs = sharedPreferences ?: return
        val membersJson = prefs.getString("members", "[]")
        val type = object : TypeToken<List<Member>>() {}.type
        val membersList: List<Member> = gson.fromJson(membersJson, type)
        _members.value = membersList
    }

    private fun saveMembers() {
        val prefs = sharedPreferences ?: return
        val membersJson = gson.toJson(_members.value)
        prefs.edit().putString("members", membersJson).apply()
    }

    fun addMember(member: Member) {
        val currentMembers = _members.value.orEmpty().toMutableList()
        // Assign color in round-robin fashion
        val color = MEMBER_COLORS[currentMembers.size % MEMBER_COLORS.size]
        val memberWithColor = member.copy(color = color)
        currentMembers.add(memberWithColor)
        _members.value = currentMembers
        saveMembers()
    }

    fun removeMember(member: Member) {
        val currentMembers = _members.value.orEmpty().toMutableList()
        currentMembers.removeAll { it.id == member.id }
        _members.value = currentMembers
        saveMembers()
    }

    fun addBillItem(item: BillItem) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        val currentMembers = _members.value.orEmpty()
        // Auto-assign to all members for equal splitting by default
        val itemWithDefaultAssignment = if (currentMembers.isNotEmpty()) {
            item.copy(assignedTo = currentMembers.map { it.id })
        } else {
            item
        }
        currentItems.add(itemWithDefaultAssignment)
        _billItems.value = currentItems
        updateTotalAmount()
        android.util.Log.d("MainViewModel", "Added item: ${item.name}, price: ${item.price}, type: ${item.itemType}, assignedTo: ${itemWithDefaultAssignment.assignedTo}")
    }

    fun updateBillItem(updatedItem: BillItem) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        val index = currentItems.indexOfFirst { it.id == updatedItem.id }
        if (index != -1) {
            currentItems[index] = updatedItem
            _billItems.value = currentItems
            updateTotalAmount()
        }
    }

    fun removeBillItem(item: BillItem) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        currentItems.removeAll { it.id == item.id }
        _billItems.value = currentItems
        updateTotalAmount()
    }

    fun toggleItemMemberAssignment(item: BillItem, memberId: Long, isAssigned: Boolean) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val currentItem = currentItems[index]
            val updatedItem = if (isAssigned) {
                currentItem.copy(assignedTo = currentItem.assignedTo + memberId)
            } else {
                currentItem.copy(assignedTo = currentItem.assignedTo.filter { it != memberId })
            }
            currentItems[index] = updatedItem
            _billItems.value = currentItems
            updateTotalAmount()
        }
    }

    private fun updateTotalAmount() {
        try {
            val items = _billItems.value ?: emptyList()
            // Include all items (positive and negative) except colleague discounts
            // Negative items are item-specific deals that should be included in splitting
            val total = items.filter { 
                it.itemType != "colleague_discount"
            }.sumOf { it.price }
            _totalAmount.value = total
            android.util.Log.d("MainViewModel", "Updated total: $total, items count: ${items.size}")
        } catch (e: Exception) {
            android.util.Log.e("MainViewModel", "Error updating total amount", e)
            _totalAmount.value = 0.0
        }
    }

    fun setDiscountPercentage(percentage: Double) {
        _discountPercentage.value = percentage.coerceIn(0.0, 100.0)
    }

    fun getSubtotal(): Double {
        val items = _billItems.value ?: emptyList()
        // Include all items (positive and negative) except colleague discounts
        // Negative items are item-specific deals that should be included in splitting
        val filteredItems = items.filter { 
            it.itemType != "colleague_discount"
        }
        val subtotal = filteredItems.sumOf { it.price }
        
        android.util.Log.d("MainViewModel", "getSubtotal: ${items.size} total items, ${filteredItems.size} filtered items")
        android.util.Log.d("MainViewModel", "getSubtotal: subtotal = $subtotal")
        filteredItems.forEach { item ->
            android.util.Log.d("MainViewModel", "getSubtotal: item=${item.name}, price=${item.price}, type=${item.itemType}")
        }
        
        return subtotal
    }

    fun getDiscountAmount(): Double {
        val subtotal = getSubtotal()
        val discountPercentage = _discountPercentage.value ?: 0.0
        return (subtotal * discountPercentage) / 100.0
    }

    fun getFinalTotal(): Double {
        val subtotal = getSubtotal()
        val discountAmount = getDiscountAmount()
        return subtotal - discountAmount
    }

    fun getTotalDeals(): Double {
        val items = _billItems.value ?: emptyList()
        return items.filter { it.itemType == "deal" }.sumOf { kotlin.math.abs(it.price) }
    }

    fun getTotalDiscounts(): Double {
        val items = _billItems.value ?: emptyList()
        return items.filter { it.itemType == "discount" }.sumOf { kotlin.math.abs(it.price) }
    }

    fun getPerPersonAmount(): Double {
        val memberCount = _members.value?.size ?: 1
        return getFinalTotal().div(memberCount)
    }

    data class MemberBreakdown(
        val memberName: String,
        val items: List<BillItem>,
        val subtotal: Double,
        val discountShare: Double,
        val finalAmount: Double
    )

    fun getMemberBreakdowns(): List<MemberBreakdown> {
        val members = _members.value ?: return emptyList()
        val items = _billItems.value ?: return emptyList()
        val totalDiscount = getDiscountAmount()
        val subtotal = getSubtotal()
        
        android.util.Log.d("MainViewModel", "getMemberBreakdowns: ${members.size} members, ${items.size} items, subtotal: $subtotal, discount: $totalDiscount")
        
        if (members.isEmpty()) {
            android.util.Log.d("MainViewModel", "No members found")
            return emptyList()
        }
        
        return members.map { member ->
            val memberItems = items.filter { item ->
                item.assignedTo.contains(member.id) && item.itemType != "colleague_discount"
            }
            android.util.Log.d("MainViewModel", "Member ${member.name}: ${memberItems.size} items assigned")
            
            // Calculate member's share of each item (include both positive and negative items)
            val memberSubtotal = memberItems.sumOf { item ->
                if (item.assignedTo.isNotEmpty()) {
                    item.price / item.assignedTo.size
                } else {
                    0.0
                }
            }
            // Calculate discount share proportionally based on member's subtotal
            val memberDiscountShare = if (subtotal > 0) {
                (memberSubtotal / subtotal) * totalDiscount
            } else {
                0.0
            }
            val memberFinalAmount = memberSubtotal - memberDiscountShare
            
            android.util.Log.d("MainViewModel", "Member ${member.name}: subtotal=$memberSubtotal, discount=$memberDiscountShare, final=$memberFinalAmount")
            
            MemberBreakdown(
                memberName = member.name,
                items = memberItems,
                subtotal = memberSubtotal,
                discountShare = memberDiscountShare,
                finalAmount = memberFinalAmount
            )
        }
    }

    fun clearAll() {
        // Only clear items and totals, NOT members
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        _discountPercentage.value = 0.0
        // Don't clear members - they should persist
    }

    fun clearAllData() {
        // This method clears everything including members (for fresh start)
        _members.value = emptyList()
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        _discountPercentage.value = 0.0
        saveMembers()
    }

    fun clearItemsOnly() {
        // Clear only items and totals, keep members
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        _discountPercentage.value = 0.0
    }

    fun processBillImage(imageUri: String) {
        _isProcessing.value = true
        
        // Clear all data except members when processing a new bill
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        _discountPercentage.value = 0.0
        
        viewModelScope.launch {
            try {
                val geminiService = geminiService
                if (geminiService != null) {
                    val extractedItems = geminiService.processBillImage(imageUri)
                    
                    // Add extracted items to the list with auto-assignment
                    extractedItems.forEach { item ->
                        addBillItem(item) // This will auto-assign to all members
                    }
                } else {
                    // Fallback: add a placeholder item
                    val placeholderItem = BillItem(
                        id = System.currentTimeMillis(),
                        name = "Bill Item (Gemini)",
                        price = 0.0
                    )
                    addBillItem(placeholderItem)
                }
            } catch (e: Exception) {
                // Handle Gemini error
                val errorItem = BillItem(
                    id = System.currentTimeMillis(),
                    name = "Gemini Error: ${e.message}",
                    price = 0.0
                )
                addBillItem(errorItem)
            } finally {
                _isProcessing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Gemini service doesn't need explicit cleanup
    }
} 