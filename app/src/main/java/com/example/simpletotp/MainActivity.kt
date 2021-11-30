package com.example.simpletotp

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.simpletotp.totp.TOTPWrapper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun testTOTP(context: Context) {
        val totp = TOTPWrapper("1234", this)
        val safeTOTPEntries = totp.getSafeEntries()
        safeTOTPEntries.add(
            totp.createEntry(
                "Name",
                "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
                        + "3132333435363738393031323334353637383930" + "31323334",
                context
            )
        )
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
        println(TOTPWrapper.dbExists(this))
        val totp = TOTPWrapper("1234", this)
    }
}