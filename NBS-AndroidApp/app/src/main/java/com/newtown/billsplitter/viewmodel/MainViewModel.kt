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

    private lateinit var sharedPreferences: SharedPreferences
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
        val membersJson = sharedPreferences.getString("members", "[]")
        val type = object : TypeToken<List<Member>>() {}.type
        val membersList: List<Member> = gson.fromJson(membersJson, type)
        _members.value = membersList
    }

    private fun saveMembers() {
        val membersJson = gson.toJson(_members.value)
        sharedPreferences.edit().putString("members", membersJson).apply()
    }

    fun addMember(member: Member) {
        val currentMembers = _members.value.orEmpty().toMutableList()
        currentMembers.add(member)
        _members.value = currentMembers
        saveMembers()
    }

    fun removeMember(member: Member) {
        val currentMembers = _members.value.orEmpty().toMutableList()
        currentMembers.remove(member)
        _members.value = currentMembers
        saveMembers()
    }

    fun addBillItem(item: BillItem) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        val currentMembers = _members.value.orEmpty()
        
        // Auto-assign to all members for equal splitting by default
        val itemWithDefaultAssignment = if (currentMembers.isNotEmpty()) {
            item.copy(assignedTo = currentMembers.map { it.name })
        } else {
            item
        }
        
        currentItems.add(itemWithDefaultAssignment)
        _billItems.value = currentItems
        updateTotalAmount()
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
        currentItems.remove(item)
        _billItems.value = currentItems
        updateTotalAmount()
    }

    fun toggleItemMemberAssignment(item: BillItem, memberName: String, isAssigned: Boolean) {
        val currentItems = _billItems.value.orEmpty().toMutableList()
        val index = currentItems.indexOfFirst { it.id == item.id }
        if (index != -1) {
            val updatedItem = if (isAssigned) {
                item.copy(assignedTo = item.assignedTo + memberName)
            } else {
                item.copy(assignedTo = item.assignedTo - memberName)
            }
            currentItems[index] = updatedItem
            _billItems.value = currentItems
            updateTotalAmount()
        }
    }

    private fun updateTotalAmount() {
        val total = _billItems.value?.sumOf { it.price } ?: 0.0
        _totalAmount.value = total
    }

    fun setDiscountPercentage(percentage: Double) {
        _discountPercentage.value = percentage.coerceIn(0.0, 100.0)
    }

    fun getSubtotal(): Double {
        return _totalAmount.value ?: 0.0
    }

    fun getDiscountAmount(): Double {
        val subtotal = getSubtotal()
        val percentage = _discountPercentage.value ?: 0.0
        return subtotal * (percentage / 100.0)
    }

    fun getFinalTotal(): Double {
        return getSubtotal() - getDiscountAmount()
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
        
        if (members.isEmpty()) {
            return emptyList()
        }

        return members.map { member ->
            val memberItems = items.filter { item ->
                item.assignedTo.contains(member.name)
            }
            
            // Calculate member's share of each item
            val memberSubtotal = memberItems.sumOf { item ->
                if (item.assignedTo.isNotEmpty()) {
                    item.price / item.assignedTo.size
                } else {
                    0.0
                }
            }
            
            // Calculate discount share equally
            val memberDiscountShare = if (members.isNotEmpty()) {
                totalDiscount / members.size
            } else {
                0.0
            }
            
            val memberFinalAmount = memberSubtotal - memberDiscountShare

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
        _members.value = emptyList()
        _billItems.value = emptyList()
        _totalAmount.value = 0.0
        saveMembers()
    }

    fun processBillImage(imageUri: String) {
        _isProcessing.value = true
        
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