package com.newtown.billsplitter.model

// Example color palette (can be expanded)
val MEMBER_COLORS = listOf(
    0xFFE57373.toInt(), // Red
    0xFF64B5F6.toInt(), // Blue
    0xFF81C784.toInt(), // Green
    0xFFFFB74D.toInt(), // Orange
    0xFFBA68C8.toInt(), // Purple
    0xFFFF8A65.toInt(), // Deep Orange
    0xFFA1887F.toInt(), // Brown
    0xFF90A4AE.toInt()  // Blue Grey
)

data class Member(
    val id: Long = 0,
    val name: String,
    val color: Int = MEMBER_COLORS.random(),
    val emoji: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) 