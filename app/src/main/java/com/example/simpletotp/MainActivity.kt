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
import java.io.FileNotFoundException
import java.security.InvalidKeyException


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun initPin(view: View) {
        //this is called from the activity_main.xml
        val submitButton = findViewById<Button>(R.id.submit_Button)
        var totp: TOTPWrapper?
        totp = null
        val pin = findViewById<EditText>(R.id.pinText).text.toString()
        try {
            totp = TOTPWrapper(pin, this)
            Singleton.setWrapper(totp)
            Singleton.setEntries(totp.readEntries(this))

            val intent = Intent(this, ListViewActivity::class.java)
            startActivity(intent)

        } catch (e: Exception) {
            when (e) {
                is InvalidKeyException -> {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setMessage("If you have forgotten your pin, you must clear app data to reset pin.")
                    dialogBuilder.setTitle("Wrong pin")
                    dialogBuilder.setPositiveButton(
                        "Ok",
                        { dialogInterface: DialogInterface, i: Int -> })
                    val alertDialog = dialogBuilder.create()
                    alertDialog.show()
                }
                is FileNotFoundException -> {
                    val dialogBuilder = AlertDialog.Builder(this)
                    dialogBuilder.setTitle("Database error")
                    dialogBuilder.setMessage("If issue persists, please try clearing app data")
                    dialogBuilder.setPositiveButton(
                        "Ok",
                        { dialogInterface: DialogInterface, i: Int -> })
                    val alertDialog = dialogBuilder.create()
                    alertDialog.show()
                }
            }
        }
    }
}