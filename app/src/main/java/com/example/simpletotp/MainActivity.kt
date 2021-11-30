package com.example.simpletotp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.simpletotp.totp.TOTPWrapper

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

    private fun testEncryption() {
        val wrapper = TOTPWrapper("1234", this)
        val encrypted = wrapper.encrypt("Hello world!")
        println(encrypted)
        println(wrapper.decrypt(encrypted))
    }

    private fun testDB() {
        val totp = TOTPWrapper("1234", this)
//        val safeEntries = totp.getSafeEntries()
//        safeEntries[0].name = "New Name"
//        totp.updateEntry(safeEntries[0], this)
    }
}