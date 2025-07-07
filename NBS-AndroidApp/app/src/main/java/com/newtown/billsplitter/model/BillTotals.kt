package com.newtown.billsplitter.model

data class BillTotals(
    val subtotal: Double = 0.0,
    val discountPercent: Double = 0.0,
    val discountAmount: Double = 0.0,
    val finalTotal: Double = 0.0,
    val memberTotals: Map<String, MemberTotal> = emptyMap()
) {
    fun getSubtotalFormatted(): String = "£%.2f".format(subtotal)
    fun getDiscountAmountFormatted(): String = "-£%.2f".format(discountAmount)
    fun getFinalTotalFormatted(): String = "£%.2f".format(finalTotal)
}

data class MemberTotal(
    val memberName: String,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val finalTotal: Double = 0.0,
    val items: List<MemberItem> = emptyList()
) {
    fun getSubtotalFormatted(): String = "£%.2f".format(subtotal)
    fun getDiscountFormatted(): String = "-£%.2f".format(discount)
    fun getFinalTotalFormatted(): String = "£%.2f".format(finalTotal)
}

data class MemberItem(
    val name: String,
    val price: Double,
    val sharedWith: Int
) {
    fun getPriceFormatted(): String = "£%.2f".format(price)
} 