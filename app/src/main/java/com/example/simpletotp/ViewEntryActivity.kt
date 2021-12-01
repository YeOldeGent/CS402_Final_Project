package com.example.simpletotp

import android.R.attr
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletotp.totp.TOTPWrapper
import android.R.attr.label
import android.content.*

import com.example.simpletotp.totp.SafeTOTPEntry


class ViewEntryActivity(val wrapper: TOTPWrapper, val entry: SafeTOTPEntry) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewentry)

        val id = entry.id
        val name = entry.name

        val nameEdit = findViewById<EditText>(R.id.editTextName)
        val code = findViewById<TextView>(R.id.textViewCode)
        val timer = findViewById<TextView>(R.id.timer)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val copyButton = findViewById<Button>(R.id.copyButton)
        val doneButton = findViewById<Button>(R.id.doneButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        saveButton.setBackgroundColor(Color.DKGRAY)
        saveButton.isClickable = false

        nameEdit.setText(name)

        code.setText(wrapper.getTOTPcode(id, System.currentTimeMillis().toString().substring(0, 10).toLong()))

        val timeLeft = wrapper.timeLeftInCode()
        val timerText = "Time left: " + timeLeft
        timer.setText(timerText)

        saveButton.setOnClickListener {
            entry.name = nameEdit.text.toString()
            saveButton.setBackgroundColor(Color.DKGRAY)
            saveButton.isClickable = false
        }

        copyButton.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Code!", code.text)
            clipboard.setPrimaryClip(clip)
        }

        doneButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            //when clicked, a message will pop up, making them confirm their choice
            val skipDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            skipDialog.setTitle("Delete?")
            skipDialog.setMessage("Do you want delete this entry?")
            //if the press 'Yes', they will be brought to the add entry screen with two blank boxes
            skipDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, which ->
                wrapper.removeEntry(id)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            })
            //if the press 'Cancel', they can continue scanning as normal
            skipDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            skipDialog.show()
        }

        nameEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
                saveButton.setBackgroundColor(Color.BLUE)
                saveButton.isClickable = true
            }

            override fun afterTextChanged(p0: Editable?) {}
        })
    }

}