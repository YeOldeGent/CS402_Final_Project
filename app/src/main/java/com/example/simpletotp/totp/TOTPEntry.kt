package com.example.simpletotp.totp

data class TOTPEntry(
    val key: String,
    val id: String,
    val crypto: String,
    var name: String,
    var favorite: Boolean
)