package com.isharaw.kmpproj

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform