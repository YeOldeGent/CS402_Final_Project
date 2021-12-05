package com.example.simpletotp.totp

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.simpletotp.database.TOTPEntryHelper
import java.io.FileNotFoundException
import java.io.Serializable
import java.security.InvalidKeyException
import java.security.InvalidParameterException
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.absoluteValue

/**
 * This is an all in one class to contain the TOTP keys in one place. Handles everything from generating codes to storing and modifying keys on disk.
 * @param pin: pin used to encrypt/decrypt TOTP keys from the disk
 */
class TOTPWrapper(private var pin: String, context: Context) : Serializable {
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            throw Error("Incorrect SDK version")
        else {
            // ========PIN VALIDATION========

            /**
             * ENCRYPTION NOTES:
             * - Using PBE (Password Base Encryption)
             *  - Password and salt combined to create AES key
             * - IV used similar to salt, but in combination with AES key
             * - IV and salt safe to store in DB in plaintext
             */
            if (!dbExists(context)) {
                // db does not exist - create new secret key, salt, and iv
                // ---- create secret key and iv strings ----
                val salt = hashString("SHA-256", System.currentTimeMillis().toString())
                val pinHash = hashString("SHA-256", pin)
                // ---- INIT CRYPTO START ----
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                val spec = PBEKeySpec(
                    pinHash.toCharArray(),
                    Base64.getDecoder().decode(salt),
                    1000,
                    256
                )
                val tmp = factory.generateSecret(spec)
                secretKey = SecretKeySpec(tmp.encoded, "AES")
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                iv = IvParameterSpec(cipher.iv)
                // ---- INIT CRYPTO END ----
                val ivString = Base64.getEncoder().encodeToString(cipher.iv)
                // save salt, iv, and hashed secretKeyString in db
                if (!storeKeys(context, secretKey.encoded.decodeToString(), ivString, salt))
                    throw FileNotFoundException("Issues connecting with database")
            } else {
                // db exists, get iv and salt to validate pin
                // get keys
                val map = getKeys(context)
                val salt = map["salt"]
                val ivString = map["iv"]
                val secretKeyHash = map["secret key"]
                if (salt == null || ivString == null || secretKeyHash == null)
                    throw FileNotFoundException("Either the database could not be connected to or the keys were not saved")
                val pinHash = hashString("SHA-256", pin)
                // ---- INIT CRYTPO START ----
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                val spec = PBEKeySpec(
                    pinHash.toCharArray(),
                    Base64.getDecoder().decode(salt),
                    1000,
                    256
                )
                val tmp = factory.generateSecret(spec)
                secretKey = SecretKeySpec(tmp.encoded, "AES")
                val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
                iv = IvParameterSpec(Base64.getDecoder().decode(ivString))
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
                // ---- INIT CRYPTO END ----
                if (hashString("SHA-256", secretKey.encoded.decodeToString()) != secretKeyHash)
                    throw InvalidKeyException("Incorrect pin")
                println("CORRECT PIN")
            }
            // ========READ ALL DB ENTRIES========
            dbReadAll(context)
        }
    }

    /**
     * ========STARTUP HELPERS========
     */

    /**
     * Gets keys from Keys table in database
     */
    @SuppressLint("Recycle")
    private fun getKeys(context: Context): HashMap<String, String> {
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM Keys",
            null
        ) ?: throw FileNotFoundException("Database does not exist or was not found")
        val map = HashMap<String, String>()
        with(cursor) {
            while (moveToNext()) {
                map[getString(getColumnIndexOrThrow("label"))] =
                    getString(getColumnIndexOrThrow("value"))
            }
        }
        db.close()
        return map
    }

    /**
     * Store keys in Keys table. Stores hash of secret key
     */
    private fun storeKeys(
        context: Context,
        secretKeyString: String,
        ivString: String,
        salt: String
    ): Boolean {
        println("SAVING KEYS")
        val dbHelper = TOTPEntryHelper(context)
        val db = dbHelper.writableDatabase
        db.execSQL(TOTPEntryHelper.KeysContract.SQL_DELETE_KEYS)
        db.execSQL(TOTPEntryHelper.KeysContract.SQL_CREATE_KEYS)
        val arr = arrayOf("secret key", "iv", "salt")
        arr.forEach {
            val values = ContentValues().apply {
                put("label", it)
                put(
                    "value", when (it) {
                        "secret key" -> hashString("SHA-256", secretKeyString)
                        "iv" -> ivString
                        else -> salt
                    }
                )
            }
            val id = db.insert("Keys", null, values)
            if (id == -1L) {
                db.close()
                return false
            }
        }
        db.close()
        return true
    }

    /**
     * ========Static functions========
     */
    companion object {
        /**
         * Checks if the database exists already. Can be used to check for first-time setup.
         */
        @JvmStatic
        fun dbExists(context: Context): Boolean {
            val dbFile = context.getDatabasePath(TOTPEntryHelper.DATABASE_NAME)
            return dbFile.exists()
        }

        /**
         * Returns the amount of time (in seconds) left in the current code. When the value is 0,
         * then it is time to generate a new code.
         */
        @JvmStatic
        fun timeLeftInCode(): Int {
            val sec = System.currentTimeMillis().toString().substring(0, 10).toLong() % 60
            return if ((sec % 30).toInt() == 0) 0 else ((sec % 30).toInt() - 30).absoluteValue
        }
    }

    /**
     * ========ENTRY MANIPULATION========
     * CRUD functions
     */

    // }----Create----{

    /**
     * Creates a new TOTP entry to the list and returns the new entry in safe form.
     */
    fun createEntry(name: String, key: String, context: Context): SafeTOTPEntry {
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
            throw FileNotFoundException("Error communicating with database")

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
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID, encrypt(entry.id))
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_KEY_TITLE, encrypt(entry.key))
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE, encrypt(entry.name))
            put(
                TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_CRYPTO_TITLE,
                encrypt(entry.crypto)
            )
            // leave favorite unencrypted for now bc it's a boolean
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
                        decrypt(getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_KEY_TITLE))),
                        decrypt(getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID))),
                        decrypt(getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE))),
                        decrypt(getString(getColumnIndexOrThrow(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_CRYPTO_TITLE))),
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
        if (dbUpdate(entry, context) != 0)
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
            put(TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_NAME_TITLE, encrypt(entry.name))
            put(
                TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_FAVORITE_TITLE,
                entry.favorite
            )
        }
        val selection = "${TOTPEntryHelper.TOTPEntryContract.TOTPEntry.COLUMN_ID} = ?"
        val selectionArgs = arrayOf(encrypt(entry.id))
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
        val selectionArgs = arrayOf(encrypt(entry.id))
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
     *
     * Note: to get time left on code, use the following:
     * ```
     * var sec = now % 60
     * var secondsLeft = sec % 30
     * ```
     * When `sec % 30 == 0`, it is time to generate a new TOTP code. Could use a thread which
     * checks the time every 0.5 seconds or so to see if the current code has expired?
     */
    fun getTOTPcode(id: String): String? {
        val now = System.currentTimeMillis().toString().substring(0, 10).toLong()
        val entry = entries.find { it.id == id } ?: return null
        val x: Long = 30
        val t: Long = now / x
        var steps = t.toULong().toString(16).uppercase()
        while (steps.length < 16)
            steps = "0$steps"
        return TOTP.generateTOTP(entry.key, steps, "6", entry.crypto)
    }

    /**
     * ========CRYPTOGRAPHY========
     */

    /**
     * Encrypt the given plaintext with AES CBC
     */
    private fun encrypt(plaintext: String): String {
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
    private fun decrypt(ciphertext: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
        val plaintext: ByteArray = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cipher.doFinal(Base64.getDecoder().decode(ciphertext))
        } else {
            throw Error("Incorrect SDK version")
        }
        return plaintext.toString(Charsets.UTF_8)
    }

    /**
     * Hashes string. Can use any type of hash (i.e. MD5, SHA-1, SHA-256, etc.)
     */
    private fun hashString(type: String, input: String): String {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input.toByteArray())
        return printHexBinary(bytes)
    }

    private fun printHexBinary(data: ByteArray): String {
        val hexChars = "0123456789ABCDEF".toCharArray()
        val r = StringBuilder(data.size * 2)
        data.forEach { b ->
            val i = b.toInt()
            r.append(hexChars[i shr 4 and 0xF])
            r.append(hexChars[i and 0xF])
        }
        return r.toString()
    }
}