package com.example.simpletotp.totp

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.os.Build
import com.example.simpletotp.database.TOTPEntryHelper
import java.io.FileNotFoundException
import java.security.InvalidKeyException
import java.security.InvalidParameterException
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

/**
 * This is an all in one class to contain the TOTP keys in one place. Handles everything from generating codes to storing and modifying keys on disk.
 * @param pin: pin used to encrypt/decrypt TOTP keys from the disk
 */
class TOTPWrapper(private var pin: String, context: Context) {
    private val entries = ArrayList<TOTPEntry>()
    private val secretKey: SecretKey
    private val iv: IvParameterSpec

    /**
     * On creation of a TOTP object, initialize cryptographic tools. Then, check for an existing database. If one exists, decrypt it
     * and store it in memory. If it does not, create a new database.
     */
    init {
        // check for a valid pin
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
        // delete plaintext pin from memory to be safe
        pin = ""

        // ========DB INITIALIZATION========

        // ========PIN VALIDATION========
        // if pin is incorrect, throw an error
        if (false)
            throw InvalidKeyException("Incorrect pin")
        dbReadAll(context)
    }

    companion object {
        /**
         * Checks if the database exists already. Can be used to check for first-time setup.
         */
        @JvmStatic
        fun dbExists(context: Context): Boolean {
            val dbFile = context.getDatabasePath(TOTPEntryHelper.DATABASE_NAME)
            return dbFile.exists()
        }
    }

    /**
     * ========ENTRY MANIPULATION========
     */

    // }----Create----{

    /**
     * Creates a new TOTP entry to the list and returns the new entry in safe form.
     */
    fun createEntry(name: String, key: String, context: Context): SafeTOTPEntry? {
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
        // add new entry to db
        if (dbCreate(newEntry, context) == -1)
            return null

        return SafeTOTPEntry(
            newEntry.id,
            name,
            false
        )
    }

    /**
     * Creates a new entry in the database
     */
    private fun dbCreate(entry: TOTPEntry, context: Context): Int {
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
        val id = db.insert(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME, null, values)
        db.close()
        return if (id == -1L) -1 else 0
    }

    // }----Read----{

    /**
     * Returns an ArrayList of TOTP entries in safe form.
     */
    fun readEntries(context: Context): ArrayList<SafeTOTPEntry> {
        dbReadAll(context)
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
     * Reads all entries from the db and adds them to the entry list.
     */
    private fun dbReadAll(context: Context) {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM " + TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME,
            null
        ) ?: throw FileNotFoundException("Database does not exist or was not found")
        // if successful, clear entries list
        entries.clear()
        with(cursor) {
            while (moveToNext())
                entries.add(
                    TOTPEntry(
                        getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_KEY_TITLE)),
                        getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID)),
                        getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE)),
                        getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_CRYPTO_TITLE)),
                        getInt(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE)) == 1
                    )
                )
        }
        db.close()
    }

    // }----Update----{

    /**
     * Updates the entry with the matching id with the safe version's name and favorite status.
     * Returns false if the entry doesn't exist or does not need to be updated.
     */
    fun updateEntry(safeTOTPEntry: SafeTOTPEntry, context: Context): Boolean {
        val entry = entries.find { it.id == safeTOTPEntry.id } ?: return false
        if (entry.name == safeTOTPEntry.name && entry.favorite == safeTOTPEntry.favorite)
            return false
        entry.name = safeTOTPEntry.name
        entry.favorite = safeTOTPEntry.favorite
        // update value on disk
        if (dbUpdate(entry, context) < 1)
            return false
        return true
    }

    /**
     * Updates the database entry reflection of the given TOTPEntry
     */
    private fun dbUpdate(entry: TOTPEntry, context: Context): Int {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE, entry.name)
            put(
                TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE,
                entry.favorite
            )
        }
        val selection = "${TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(entry.id)
        val count = db.update(
            TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        )
        db.close()
        return count
    }

    // }----Delete----{

    /**
     * Removes the entry with the given id
     */
    fun deleteEntry(entry: SafeTOTPEntry, context: Context): Boolean {
        for (i in 0 until entries.size)
            if (entries[i].id == entry.id) {
                if (dbDelete(entries.removeAt(i), context) < 1)
                    return false
                return true
            }
        return false
    }

    /**
     * Deletes the given entry from the db
     */
    private fun dbDelete(entry: TOTPEntry, context: Context): Int {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.writableDatabase
        val selection = "${TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(entry.id)
        val count = db.delete(
            TOTPEntryHelper.TOTPEntryContract.TOTPEntry.TABLE_NAME,
            selection,
            selectionArgs
        )
        db.close()
        return count
    }

    /**
     * ========TOTP ALGORITHM========
     */

    /**
     * Generates a TOTP code for the entry with the given id, or null if said entry does not exist.
     * @param id: id of entry to be used
     * @param now: current time in seconds I think? Use `System.currentTimeMillis().toString().substring(0, 10).toLong()`
     *
     * Note: to get time left on code, use the following:
     * ```
     * var sec = now % 60
     * var secondsLeft = sec % 30
     * ```
     * When `sec % 30 == 0`, it is time to generate a new TOTP code. Could use a thread which
     * checks the time every 0.5 seconds or so to see if the current code has expired?
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