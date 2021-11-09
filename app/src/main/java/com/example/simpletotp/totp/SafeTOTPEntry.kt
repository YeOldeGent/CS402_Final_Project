package com.example.simpletotp.totp

/**
 * Safe data class not containing TOTP key for use in rest of program
 */
data class SafeTOTPEntry(
    val id: String,
    var name: String,
    var favorite: Boolean = false
)
