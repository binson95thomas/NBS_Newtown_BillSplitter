package com.newtown.billsplitter.model

import java.util.Date

/**
 * Data class representing a saved bill scan
 */
data class SavedBill(
    val id: Long = 0,
    val billName: String, // Receipt name from the bill
    val scanDate: Date,
    val billItems: List<BillItem>,
    val subtotal: Double,
    val discountPercentage: Double,
    val discountAmount: Double,
    val finalTotal: Double,
    val memberNames: List<String> // List of members who were part of this bill
)
