package com.newtown.billsplitter.model

data class BillItem(
    val id: Long = 0,
    val name: String,
    val price: Double,
    val assignedTo: List<Long> = emptyList(), // Use member IDs
    val isMultibuy: Boolean = false,
    val itemType: String = "item", // "item", "deal", "discount", "colleague_discount"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getDisplayPrice(): String {
        val absPrice = kotlin.math.abs(price)
        return when {
            price < 0 -> "Deal: -£%.2f".format(absPrice)
            itemType == "deal" -> "Deal: -£%.2f".format(absPrice)
            itemType == "discount" -> "Discount: -£%.2f".format(absPrice)
            itemType == "colleague_discount" -> "Colleague Discount: -£%.2f".format(absPrice)
            else -> if (isMultibuy) "-£%.2f".format(absPrice) else "£%.2f".format(price)
        }
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
    
    fun isDealOrDiscount(): Boolean {
        return itemType == "deal" || itemType == "discount" || itemType == "colleague_discount" || price < 0
    }
    
    fun isColleagueDiscount(): Boolean {
        return itemType == "colleague_discount"
    }
} 