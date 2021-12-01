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
import android.os.CountDownTimer
import java.text.SimpleDateFormat
import java.util.*


class ViewEntryActivity(val wrapper: TOTPWrapper, val entry: SafeTOTPEntry) : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewentry)

        //save off the id and name for easy use
        val id = entry.id
        val name = entry.name

        //get all the layout elements
        val nameEdit = findViewById<EditText>(R.id.editTextName)
        val code = findViewById<TextView>(R.id.textViewCode)
        val timer = findViewById<TextView>(R.id.timer)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val copyButton = findViewById<Button>(R.id.copyButton)
        val doneButton = findViewById<Button>(R.id.doneButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        //make it so they can click the save button right away
        saveButton.setBackgroundColor(Color.DKGRAY)
        saveButton.isClickable = false

        //set the name text box
        nameEdit.setText(name)

        //get the code and set the text view
        code.setText(wrapper.getTOTPcode(id, System.currentTimeMillis().toString().substring(0, 10).toLong()))

        //start the timer, pass in the timer, code, and entry id
        newTimer(timer, code, id)

        //when you click save, save off the new name and disable the button again
        saveButton.setOnClickListener {
            entry.name = nameEdit.text.toString()
            saveButton.setBackgroundColor(Color.DKGRAY)
            saveButton.isClickable = false
        }

        //when clicking the copy button, save the code to the users clipboard
        copyButton.setOnClickListener {
            val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Code!", code.text)
            clipboard.setPrimaryClip(clip)
        }

        //switch back to main
        doneButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        //remove the current entry
        deleteButton.setOnClickListener {
            //when clicked, a message will pop up, making them confirm their choice
            val delDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            delDialog.setTitle("Delete?")
            delDialog.setMessage("Do you want delete this entry?")
            //if the press 'Yes', the entry will be removed and they will be brought back to the list
            delDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, which ->
                wrapper.deleteEntry(entry, this)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            })
            //if the press 'Cancel', they can continue editing/viewing the entry
            delDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            delDialog.show()
        }

        //when you edit the name, make the save button clickable
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

    //create a timer
    fun newTimer(timer: TextView, code: TextView, id: String) {
        val timeLeft = wrapper.timeLeftInCode() * 1000
        var timerText = "Time left: "
        object : CountDownTimer(timeLeft.toLong(), 1000) {
            //on every tick, update the timer text
            override fun onTick(millisUntilFinished: Long) {
                timerText = "Time left: " + SimpleDateFormat("mm:ss").format(Date(millisUntilFinished))
                timer.setText(timerText)
            }

            //when the timer finished, create a new code, then a new timer
            override fun onFinish() {
                code.setText(wrapper.getTOTPcode(id, System.currentTimeMillis().toString().substring(0, 10).toLong()))
                newTimer(timer, code, id)
            }
        }.start()
    }

}