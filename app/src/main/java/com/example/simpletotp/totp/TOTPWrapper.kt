package com.example.simpletotp.totp

import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.InvalidParameterException
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList
import kotlin.experimental.and
import kotlin.math.absoluteValue

/**
 * This is an all in one class to contain the TOTP keys in one place. Handles everything from generating codes to storing and modifying keys on disk.
 * @param pin: pin used to encrypt/decrypt TOTP keys from the disk
 *
 * Credit for TOTP protocol:
 * https://tools.ietf.org/html/rfc6238#page-9
 * @author u011279
 */
class TOTPWrapper(private val pin: String) {
    private val entries = ArrayList<TOTPEntry>()

    /**
     * On creation of a TOTP object, check for a file and decrypt it. Else, init a new file
     */
    init {
        // check for errors, we are also handling pin checking here
        //if(pin.equals(null) || pin.length < 4 || !pin.equals(storedPin))
        if (pin.equals(null) || pin.length < 4)
            throw InvalidParameterException("Provided pin is either null or not long enough")
        // TODO: implement file reading and decryption
    }

    /**
     * ========ENTRY MANIPULATION========
     */

    /**
     * Returns an ArrayList of TOTP entries in safe form.
     */
    fun getSafeEntries(): ArrayList<SafeTOTPEntry> {
        val safeEntries = ArrayList<SafeTOTPEntry>()
        entries.forEach {
            safeEntries.add(
                SafeTOTPEntry(
                    it.id,
                    it.name,
                    it.favorite
                )
            )
        }
        return safeEntries
    }

    /**
     * Adds a new TOTP entry to the list and returns the new entry in safe form.
     */
    fun addEntry(name: String, key: String): SafeTOTPEntry {
        entries.add(
            TOTPEntry(
                key,
                System.currentTimeMillis().toString(),
                name,
                when (key.length) {
                    40 -> "HmacSHA1"
                    64 -> "HmacSHA256"
                    128 -> "HmacSHA512"
                    else -> throw InvalidParameterException("Invalid TOTP key")
                }
            )
        )
        // TODO: on adding a new entry, write it to disk
        return SafeTOTPEntry(
            entries[entries.lastIndex].id,
            name,
            false
        )
    }

    /**
     * Removes the entry with the given id
     */
    fun removeEntry(id: String): Boolean {
        for (i in 0 until entries.size)
            if (entries[i].id == id) {
                entries.removeAt(i)
                // TODO: remove entry from disk
                return true
            }
        return false
    }

    /**
     * Updates the entry with the matching id with the safe version's name and favorite status.
     * Returns false if the entry doesn't exist or does not need to be updated.
     */
    fun updateEntry(safeTOTPEntry: SafeTOTPEntry): Boolean {
        val entry = entries.find { it.id == safeTOTPEntry.id } ?: return false
        if (entry.name == safeTOTPEntry.name && entry.favorite == safeTOTPEntry.favorite)
            return false
        entry.name = safeTOTPEntry.name
        entry.favorite = safeTOTPEntry.favorite
        // TODO: update value on disk
        return true
    }

    /**
     * Generates a TOTP code for the entry with the given id, or null if said entry does not exist.
     * @param id: id of entry to be used
     * @param now: current time in seconds I think? Use `System.currentTimeMillis().toString().substring(0, 10).toLong()`
     */
    fun getTOTPcode(id: String, now: Long): String? {
        val entry = entries.find { it.id == id } ?: return null
        val x: Long = 30
        val t: Long = now / x
        var steps = t.toULong().toString(16).uppercase()
        while (steps.length < 16)
            steps = "0$steps"
        return TOTP.generateTOTP(entry.key, steps, "6", entry.crypto)
    }

    /**
     * Returns the amount of time (in seconds) left in the current code. When the value is 0,
     * then it is time to generate a new code.
     */
    fun timeLeftInCode(): Int {
        val sec = System.currentTimeMillis().toString().substring(0, 10).toLong() % 60
        return if ((sec % 30).toInt() == 0) 0 else ((sec % 30).toInt() - 30).absoluteValue
    }
}