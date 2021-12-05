package com.example.simpletotp

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletotp.totp.TOTPWrapper
import java.io.Serializable
import java.security.InvalidParameterException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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

            val intent = Intent(this, ListViewActivity::class.java)
            startActivity(intent)

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