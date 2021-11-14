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

class NewEntryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_newentry)

        val name = findViewById<EditText>(R.id.editTextName)
        val key = findViewById<EditText>(R.id.editTextKey)
        val error = findViewById<TextView>(R.id.errorText)
        val submit: Button = findViewById(R.id.submit)

        key.setText(intent.getStringExtra("KEY"))

        val nameString = name.text
        val keyString = key.text

        submit.setOnClickListener {
            error.text = ""
            if (name.text.length == 0) {
                error.text = "Please enter a name!!"
            } else if (key.text.length == 40 || key.text.length == 64 || key.text.length == 128) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                key.setTextColor(Color.RED)
                error.text = "Invalid code length!!"
            }
        }

        key.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                key.setTextColor(Color.BLACK)
                error.text = ""
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

        name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                error.text = ""
            }

            override fun afterTextChanged(p0: Editable?) {}
        })

    }

}
