package com.example.simpletotp.totp

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.os.Build
import com.example.simpletotp.database.TOTPEntryHelper
import java.security.InvalidKeyException
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

/**
 * This is an all in one class to contain the TOTP keys in one place. Handles everything from generating codes to storing and modifying keys on disk.
 * @param pin: pin used to encrypt/decrypt TOTP keys from the disk
 */
class TOTPWrapper(private var pin: String) {
    private val entries = ArrayList<TOTPEntry>()
    private val secretKey: SecretKey
    private val iv: IvParameterSpec

    /**
     * On creation of a TOTP object, initialize cryptographic tools. Then, check for an existing database. If one exists, decrypt it
     * and store it in memory. If it does not, create a new database.
     */
    init {
        // check for errors
        if (pin.equals(null) || pin.length < 4)
            throw InvalidParameterException("Provided pin is either null or not long enough")
        // ========INITIALIZE CRYPTOGRAPHY========
        // initialize secret key hash
        val keygen = KeyGenerator.getInstance("AES")
        val secureRandom = SecureRandom.getInstance("SHA1PrNG")
        secureRandom.setSeed(pin.toByteArray())
        keygen.init(256, secureRandom)
        secretKey = keygen.generateKey()
        // initialize initial vector
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        iv = IvParameterSpec(cipher.iv)
        // delete pin from memory to be safe
        pin = ""
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
    fun addEntry(name: String, key: String, context: Context): SafeTOTPEntry {
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
        val newEntry = entries[entries.lastIndex]
        // TODO: on adding a new entry, write it to disk
//        addToDB(newEntry, context)
        // read from db
        readFromDB(newEntry, context)

        return SafeTOTPEntry(
            newEntry.id,
            name,
            false
        )
    }

    private fun readFromDB(entry: TOTPEntry, context: Context) {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.readableDatabase
        val projection = arrayOf("*")
        val selection = "${TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(entry.id)
        val sortOrder = "${TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE} DESC"
        val cursor = db.query(
            TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            sortOrder
        )
        println("Count: " + cursor.count)
        with(cursor) {
            while (moveToNext())
                println("id: " + getInt(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE)))
        }
    }

    private fun addToDB(entry: TOTPEntry, context: Context) {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID, entry.id)
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_KEY_TITLE, entry.key)
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE, entry.name)
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_CRYPTO_TITLE, entry.crypto)
            put(
                TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE,
                entry.favorite
            )
        }
        val newRowId =
            db?.insert(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME, null, values)
        println("New row id: $newRowId")
        db.close()
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

    /**
     * ========CRYPTOGRAPHY========
     */

    /**
     * Encrypt the given plaintext with AES CBC
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        val ciphertext: ByteArray = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Base64.getEncoder().encodeToString(ciphertext)
        } else {
            throw Error("Incorrect SDK version")
        }
    }

    /**
     * Decrypt the given ciphertext with AES CBC
     */
    fun decrypt(ciphertext: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val plaintext: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cipher.doFinal(Base64.getDecoder().decode(ciphertext))
        } else {
            throw Error("Incorrect SDK version")
        }
        return plaintext.toString(Charsets.UTF_8)
    }
}