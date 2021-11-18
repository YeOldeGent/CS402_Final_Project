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


    fun testTOTP(view: View) {

        val submitButton = findViewById<Button>(R.id.submit_Button)
        var totp: TOTPWrapper?
        totp = null
        val pin = findViewById<EditText>(R.id.pinText).text.toString()
        println(pin)
        try {
            totp = TOTPWrapper(pin)
        }catch (e: InvalidParameterException){
            val dialogBuilder = AlertDialog.Builder(this)
            dialogBuilder.setMessage("Wrong Pin")
            dialogBuilder.setPositiveButton("Yes", { dialogInterface: DialogInterface, i: Int -> })
            val alertDialog = dialogBuilder.create()
            alertDialog.show()
        }
        println("Made it")

        //TODO: Make the wrapper do something now


        //val totp = TOTPWrapper("1234")
//        val safeTOTPEntries = totp.getSafeEntries()
//        safeTOTPEntries.add(
//            totp.addEntry(
//                "Name",
//                "3132333435363738393031323334353637383930" + "3132333435363738393031323334353637383930"
//                        + "3132333435363738393031323334353637383930" + "31323334"
//            )
//        )
//        val now = System.currentTimeMillis().toString().substring(0, 10).toLong()
//        println("TOTP code: " + totp.getTOTPcode(safeTOTPEntries[0].id, now))

    }

    private fun testEncryption() {
        val wrapper = TOTPWrapper("1234")
        val encrypted = wrapper.encrypt("Hello world!")
        println(encrypted)
        println(wrapper.decrypt(encrypted))
    }
}