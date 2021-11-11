package com.example.simpletotp.totp

/**
 * TOTP entry containing key. Only for use in TOTP.kt
 *
 * key must be 40, 64, or 128 character in length
 */
data class TOTPEntry(
    val key: String,
    val id: String,
    var name: String,
    val crypto: String,
    var favorite: Boolean = false
)