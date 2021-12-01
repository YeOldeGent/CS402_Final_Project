package com.example.simpletotp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletotp.totp.TOTPWrapper

class NewEntryActivity(val wrapper: TOTPWrapper) : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newentry)

        //get all the elements from the xml file
        val name = findViewById<EditText>(R.id.editTextName)
        val key = findViewById<EditText>(R.id.editTextKey)
        val error = findViewById<TextView>(R.id.errorText)
        val submit: Button = findViewById(R.id.submit)

        //get the key from the QR scanner, set the text box to display it automatically
        key.setText(intent.getStringExtra("KEY"))

        //save off the inputted information
        var nameString = name.text
        var keyString = key.text

        //when you push the button, make sure everything is the right format, if not, make the error text visible
        submit.setOnClickListener {
            error.text = ""
            if (name.text.length == 0) {
                error.text = "Please enter a name!!"
            } else if (key.text.length == 40 || key.text.length == 64 || key.text.length == 128) {
                wrapper.createEntry(nameString.toString(), keyString.toString(), this)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                key.setTextColor(Color.RED)
                error.text = "Invalid code length!!"
            }
        }

        //if a user starts to edit the key text box, hide the error and change the color back to black
        key.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                key.setTextColor(Color.BLACK)
                error.text = ""
                keyString = key.text
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        //if a user starts to edit the name, hide the error message
        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                error.text = ""
                nameString = name.text
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

    }

}
