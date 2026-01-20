package com.newtown.billsplitter.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.newtown.billsplitter.model.BillItem
import com.newtown.billsplitter.model.SavedBill
import java.util.Date

/**
 * Simple manager for saved bills using SharedPreferences
 */
class SavedBillManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("SavedBills", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val maxBills = 3
    
    fun getLastThreeBills(): List<SavedBill> {
        val billsJson = sharedPreferences.getString("saved_bills", "[]")
        val type = object : TypeToken<List<SavedBill>>() {}.type
        val bills: List<SavedBill> = gson.fromJson(billsJson, type) ?: emptyList()
        return bills.sortedByDescending { it.scanDate }.take(maxBills)
    }
    
    fun saveBill(
        billName: String,
        billItems: List<BillItem>,
        subtotal: Double,
        discountPercentage: Double,
        discountAmount: Double,
        finalTotal: Double,
        memberNames: List<String>
    ) {
        val savedBill = SavedBill(
            id = System.currentTimeMillis(),
            billName = billName,
            scanDate = Date(),
            billItems = billItems,
            subtotal = subtotal,
            discountPercentage = discountPercentage,
            discountAmount = discountAmount,
            finalTotal = finalTotal,
            memberNames = memberNames
        )
        
        val currentBills = getLastThreeBills().toMutableList()
        currentBills.add(savedBill)
        
        // Keep only the last 3 bills
        val sortedBills = currentBills.sortedByDescending { it.scanDate }.take(maxBills)
        
        val billsJson = gson.toJson(sortedBills)
        sharedPreferences.edit().putString("saved_bills", billsJson).apply()
    }
    
    fun deleteBill(billId: Long) {
        val currentBills = getLastThreeBills().toMutableList()
        currentBills.removeAll { it.id == billId }
        
        val billsJson = gson.toJson(currentBills)
        sharedPreferences.edit().putString("saved_bills", billsJson).apply()
    }
    
    fun clearAllBills() {
        sharedPreferences.edit().remove("saved_bills").apply()
    }
}
