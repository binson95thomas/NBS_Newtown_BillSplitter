package com.newtown.billsplitter.model

data class BillItem(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val assignedTo: List<String> = emptyList(),
    val isMultibuy: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayPrice(): String {
        val absPrice = kotlin.math.abs(price)
        return if (isMultibuy) "-£%.2f".format(absPrice) else "£%.2f".format(absPrice)
    }
    
    fun getCostPerPerson(): Double {
        return if (assignedTo.isNotEmpty()) {
            kotlin.math.abs(price) / assignedTo.size
        } else {
            0.0
        }
    }
    
    fun getCostPerPersonFormatted(): String {
        return "£%.2f".format(getCostPerPerson())
    }
} 