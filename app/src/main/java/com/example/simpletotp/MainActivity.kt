package com.example.simpletotp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletotp.totp.TOTP
import com.example.simpletotp.totp.TOTPWrapper
import java.io.FileNotFoundException
import java.io.Serializable
import java.security.InvalidKeyException
import java.security.InvalidParameterException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun totpTesting(wrapper: TOTPWrapper) {
        val now = System.currentTimeMillis().toString().substring(0, 10).toLong()
        val x: Long = 30
        val t: Long = now / x
        var steps = t.toULong().toString(16).uppercase()
        while (steps.length < 16)
            steps = "0$steps"
        println(
            "TOTP CODE: ${
                TOTP.generateTOTP(
                    "4A4653473234324F4D5644475352324B4C46515843514C514F4E4C5853515251",
                    steps,
                    "6",
                    "HmacSHA256"
                )
            }"
        )
    }

    fun testTOTP(view: View) {
        //this is called from the activity_main.xml
        val submitButton = findViewById<Button>(R.id.submit_Button)
        var totp: TOTPWrapper?
        totp = null
        val pin = findViewById<EditText>(R.id.pinText).text.toString()
        println(pin)
        try {
            totp = TOTPWrapper(pin, this)
            Singleton.setWrapper(totp)
            Singleton.setEntries(totp.readEntries(this))
            totpTesting(totp)

            val intent = Intent(this, ListViewActivity::class.java)
            startActivity(intent)

        } catch (e: Exception) {
            when (e) {
                is InvalidKeyException -> {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setMessage("Wrong Pin")
                    dialogBuilder.setPositiveButton("Yes", { dialogInterface: DialogInterface, i: Int -> })
                    val alertDialog = dialogBuilder.create()
                    alertDialog.show()
                } is FileNotFoundException -> {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setMessage("Database error: If issue persists, please try reinstalling")
                    dialogBuilder.setPositiveButton("Yes", { dialogInterface: DialogInterface, i: Int -> })
                    val alertDialog = dialogBuilder.create()
                    alertDialog.show()
                }
            }
        }
        println("Made it")
    }

    private fun testDB() {
        val totp = TOTPWrapper("1234", this)
        val safeEntries = totp.readEntries(this)
        println("init from db: $safeEntries")
        safeEntries.add(
            totp.createEntry(
                "entry 1",
                "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
                        + "3132333435363738393031323334353637383930" + "31323334",
                this
            )
        )
        println("create: $safeEntries")
        safeEntries.last().name = "NEW ENTRY 1"
        totp.updateEntry(safeEntries.last(), this)
        println("update: $safeEntries")
        safeEntries.add(
            totp.createEntry(
                "entry 3",
                "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
                        + "3132333435363738393031323334353637383930" + "31323334",
                this
            )
        )
        println("create: $safeEntries")
        if (totp.deleteEntry(safeEntries.first(), this))
            safeEntries.remove(safeEntries.first())
        println("delete: $safeEntries")
    }
}