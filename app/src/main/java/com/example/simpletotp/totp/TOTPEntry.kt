package com.example.simpletotp.totp

/**
 * TOTP entry containing key. ONly for use in TOTP.kt
 */
data class TOTPEntry(
    val key: String,
    val id: String,
    var name: String,
    val crypto: String = "HmacSHA1",
    var favorite: Boolean = false
)