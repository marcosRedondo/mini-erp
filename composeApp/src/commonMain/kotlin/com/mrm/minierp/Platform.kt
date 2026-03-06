package com.mrm.minierp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun openUrl(url: String)