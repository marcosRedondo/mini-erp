package com.mrm.minierp.models

data class Client(
    val id: Int = 0,
    val name: String = "",
    val taxId: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val notes: String = "",
    val isVip: Boolean = false
)
