package com.example.simpletotp

import android.content.DialogInterface

import android.content.Context

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.simpletotp.totp.TOTPWrapper
import java.security.InvalidParameterException
import java.time.Instant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun testTOTP() {
        val totp = TOTPWrapper("1234", this)
        val safeTOTPEntries = totp.readEntries(this)
        val entry = totp.createEntry(
            "Name",
            "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
                    + "3132333435363738393031323334353637383930" + "31323334",
            this
        ) ?: return
        safeTOTPEntries.add(entry)
        val now = System.currentTimeMillis().toString().substring(0, 10).toLong()
        println("TOTP code: " + totp.getTOTPcode(safeTOTPEntries[0].id, now))
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