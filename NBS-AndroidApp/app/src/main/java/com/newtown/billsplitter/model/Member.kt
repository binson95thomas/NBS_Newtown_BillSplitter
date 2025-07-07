package com.newtown.billsplitter.model

data class Member(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) 