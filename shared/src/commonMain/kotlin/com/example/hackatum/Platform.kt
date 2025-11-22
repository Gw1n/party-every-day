package com.example.hackatum

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform