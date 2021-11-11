package com.example.simpletotp

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.example.simpletotp.totp.TOTPWrapper
import java.time.Instant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun testTOTP() {
        val totp = TOTPWrapper("1234")
        val safeTOTPEntries = totp.getSafeEntries()
        safeTOTPEntries.add(
            totp.addEntry(
                "Name",
                "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
                        + "3132333435363738393031323334353637383930" + "31323334"
            )
        )
        val now = System.currentTimeMillis().toString().substring(0, 10).toLong()
        println("TOTP code: " + totp.getTOTPcode(safeTOTPEntries[0].id, now))
    }
}