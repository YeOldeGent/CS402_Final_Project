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
import com.example.simpletotp.totp.SafeTOTPEntry
import com.example.simpletotp.totp.TOTPWrapper
import java.security.InvalidParameterException

class NewEntryActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newentry)

        //get all the elements from the xml file
        val name = findViewById<EditText>(R.id.textViewName)
        val key = findViewById<EditText>(R.id.editTextKey)
        val error = findViewById<TextView>(R.id.errorText)
        val submit: Button = findViewById(R.id.submit)

        //get the key from the QR scanner, set the text box to display it automatically
        key.setText(intent.getStringExtra("KEY"))
        if (intent.hasExtra("NAME"))
            name.setText(intent.getStringExtra("NAME"))

        //save off the inputted information
        var nameString = name.text
        var keyString = key.text

        //when you push the button, make sure everything is the right format, if not, make the error text visible
        submit.setOnClickListener {
            error.text = ""
            if (name.text.isEmpty()) {
                error.text = "Please enter a name!!"
            } else {
                val entries = Singleton.globalEntries() as ArrayList<SafeTOTPEntry>
                val wrapper = Singleton.globalWrapper() as TOTPWrapper
                while (true) {
                    try {
                        entries.add(
                            wrapper.createEntry(
                                nameString.toString(),
                                keyString.toString(),
                                this
                            )
                        )
                        break
                    } catch (e: InvalidParameterException) {
                        key.setTextColor(Color.RED)
                        error.text = "Invalid code length!"
                    }
                }
                Singleton.setEntries(entries)
                Singleton.setWrapper(wrapper)
                val intent = Intent(this, ListViewActivity::class.java)
                startActivity(intent)
            }

            //if a user starts to edit the key text box, hide the error and change the color back to black
            key.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    key.setTextColor(Color.BLACK)
                    error.text = ""
                    keyString = key.text
                }

                override fun afterTextChanged(p0: Editable?) {}
            })

            //if a user starts to edit the name, hide the error message
            name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun onTextChanged(
                    s: CharSequence, start: Int,
                    before: Int, count: Int
                ) {
                    error.text = ""
                    nameString = name.text
                }

                override fun afterTextChanged(p0: Editable?) {}
            })

        }
    }
}
