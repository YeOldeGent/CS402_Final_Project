package com.example.simpletotp.totp

import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.reflect.UndeclaredThrowableException
import java.math.BigInteger
import java.security.GeneralSecurityException
import java.security.InvalidParameterException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and


/**
 * This is an all in one class to contain the TOTP keys in one place. Handles everything from generating codes to storing and modifying keys on disk.
 * @param pin: pin used to encrypt/decrypt TOTP keys from the disk
 *
 * Credit for TOTP protocol:
 * https://tools.ietf.org/html/rfc6238#page-9
 * @author u011279
 */
class TOTP(private val pin: String) {
    private val entries = ArrayList<TOTPEntry>()

    /**
     * On creation of a TOTP object, check for a file and decrypt it. Else, init a new file
     */
    init {
        // check for errors
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
    fun addEntry(name: String, key: String, crypto: String = "HmacSHA1"): SafeTOTPEntry {
        entries.add(
            TOTPEntry(
                key,
                System.currentTimeMillis().toString(),
                name,
                crypto
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
     * ========TOTP PROTOCOL CODE========
     */

    /**
     * This method uses the JCE to provide the crypto algorithm. HMAC computes a
     * Hashed Message Authentication Code with the crypto hash algorithm as a
     * parameter.
     *
     * @param crypto: the crypto algorithm (HmacSHA1, HmacSHA256, HmacSHA512)
     * @param keyBytes: the bytes to use for the HMAC key
     * @param text: the message or text to be authenticated
     */
    private fun hmacSha(
        crypto: String, keyBytes: ByteArray,
        text: ByteArray
    ): ByteArray {
        return try {
            val hmac: Mac = Mac.getInstance(crypto)
            val macKey = SecretKeySpec(keyBytes, "RAW")
            hmac.init(macKey)
            hmac.doFinal(text)
        } catch (gse: GeneralSecurityException) {
            throw UndeclaredThrowableException(gse)
        }
    }

    /**
     * This method converts a HEX string to Byte[]
     *
     * @param hex: the HEX string
     *
     * @return: a byte array
     */
    private fun hexStr2Bytes(hex: String): ByteArray {
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        val bArray = BigInteger("10$hex", 16).toByteArray()

        // Copy all the REAL bytes, not the "first"
        val ret = ByteArray(bArray.size - 1)
        for (i in ret.indices) {
            ret[i] = bArray[i + 1]
        }
        return ret
    }

    /**
     * This method generates a TOTP value for the given set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return: a numeric String in base 10 that includes
     * [truncationDigits] digits
     */
    fun generateTOTP256(
        key: String,
        time: String,
        returnDigits: String
    ): String {
        return generateTOTP(key, time, returnDigits, "HmacSHA256")
    }

    /**
     * This method generates a TOTP value for the given set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param time: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return: a numeric String in base 10 that includes
     * [truncationDigits] digits
     */
    fun generateTOTP512(
        key: String,
        time: String,
        returnDigits: String
    ): String {
        return generateTOTP(key, time, returnDigits, "HmacSHA512")
    }

    /**
     * This method generates a TOTP value for the given set of parameters.
     *
     * @param key: the shared secret, HEX encoded
     * @param timeIn: a value that reflects a time
     * @param returnDigits: number of digits to return
     *
     * @return: a numeric String in base 10 that includes
     * [truncationDigits] digits
     */
    @JvmOverloads
    fun generateTOTP(
        key: String,
        timeIn: String,
        returnDigits: String,
        crypto: String = "HmacSHA1"
    ): String {
        var time = timeIn
        val codeDigits = Integer.decode(returnDigits).toInt()
        var result: String? = null
        val digitsPower // 0   1    2     3      4       5        6         7          8
                = intArrayOf(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000)

        // Using the counter
        // First 8 bytes are for the movingFactor
        // Compliant with base RFC 4226 (HOTP)
        while (time.length < 16) {
            time = "0$timeIn"
        }

        // Get the HEX in a Byte[]
        val msg = hexStr2Bytes(time)
        val k = hexStr2Bytes(key)
        val hash = hmacSha(crypto, k, msg)

        // put selected bytes into result int
        val offset: Byte = hash[hash.size - 1] and 0xf
        val binary: Int = (((hash[offset.toInt()] and 0x7f).toInt() shl 24)
                or ((hash[offset + 1] and 0xff.toByte()).toInt() shl 16)
                or ((hash[offset + 2] and 0xff.toByte()).toInt() shl 8)
                or (hash[offset + 3] and 0xff.toByte()).toInt())
        val otp = binary % digitsPower[codeDigits]
        result = otp.toString()
        while (result!!.length < codeDigits) {
            result = "0$result"
        }
        return result
    }
}