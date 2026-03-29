package com.mrm.minierp.models

data class Company(
    val name: String,
    val logoBase64: String? = null,
    val nif: String = "",
    val phone: String = "",
    val address: String = ""
)
