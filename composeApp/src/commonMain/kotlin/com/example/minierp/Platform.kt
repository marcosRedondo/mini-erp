package com.example.minierp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform