package com.limanphotos.limandoc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform