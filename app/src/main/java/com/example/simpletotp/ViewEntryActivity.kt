package com.example.simpletotp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.simpletotp.totp.TOTPWrapper
import android.content.*

import com.example.simpletotp.totp.SafeTOTPEntry
import android.os.CountDownTimer
import android.text.InputType
import android.widget.CheckBox
import com.example.simpletotp.totp.TOTPWrapper.Companion.timeLeftInCode
import java.text.SimpleDateFormat
import java.util.*


class ViewEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewentry)
        var position = intent.getIntExtra("POS", -1)

        var entries = Singleton.globalEntries() as ArrayList<SafeTOTPEntry>
        var entry = entries.get(position)

        //save off the id and name for easy use
        val id = entry.id
        val name = entry.name

        //get all the layout elements
        val nameEdit = findViewById<EditText>(R.id.textViewName)
        val code = findViewById<TextView>(R.id.textViewCode)
        val timer = findViewById<TextView>(R.id.timer)
        val changeButton = findViewById<Button>(R.id.changeButton)
        val copyButton = findViewById<Button>(R.id.copyButton)
        val doneButton = findViewById<Button>(R.id.doneButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)
        val favoriteCheck = findViewById<CheckBox>(R.id.favoriteCheck)

        favoriteCheck.isChecked = entry.favorite

        //set the name text box
        nameEdit.setText(name)

        //get the code and set the text view
        var wrapper = Singleton.globalWrapper() as TOTPWrapper
        code.setText(wrapper.getTOTPcode(id))

        //start the timer, pass in the timer, code, and entry id
        newTimer(timer, code, id)

        //when you click save, save off the new name and disable the button again
        changeButton.setOnClickListener {
            val editDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            editDialog.setTitle("Edit Name")
            val input = EditText(this)
            input.setHint("Enter Name")
            input.inputType = InputType.TYPE_CLASS_TEXT
            editDialog.setView(input)
            editDialog.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                entries = Singleton.globalEntries() as ArrayList<SafeTOTPEntry>
                wrapper = Singleton.globalWrapper() as TOTPWrapper
                entry.name = input.text.toString()
                wrapper.updateEntry(entry, this)
                Singleton.setEntries(entries)
                Singleton.setWrapper(wrapper)
            })
            editDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            editDialog.show()
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
                entries = Singleton.globalEntries() as ArrayList<SafeTOTPEntry>
                wrapper = Singleton.globalWrapper() as TOTPWrapper
                wrapper.deleteEntry(entry, this)
                Singleton.setEntries(wrapper.readEntries(this))
                Singleton.setWrapper(wrapper)
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            })
            //if the press 'Cancel', they can continue editing/viewing the entry
            delDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            delDialog.show()
        }

        favoriteCheck.setOnCheckedChangeListener { buttonView, isChecked ->
            wrapper = Singleton.globalWrapper() as TOTPWrapper
            if (isChecked) {
                entry.favorite = true
                wrapper.updateEntry(entry, this)
            } else {
                entry.favorite = false
                wrapper.updateEntry(entry, this)
            }
            Singleton.setEntries(wrapper.readEntries(this))
            Singleton.setWrapper(wrapper)
        }

    }

    //create a timer
    fun newTimer(timer: TextView, code: TextView, id: String) {
        val timeLeft = timeLeftInCode() * 1000
        var timerText = "Time left: "
        object : CountDownTimer(timeLeft.toLong(), 1000) {
            //on every tick, update the timer text
            override fun onTick(millisUntilFinished: Long) {
                timerText = "Time left: " + SimpleDateFormat("mm:ss").format(Date(millisUntilFinished))
                timer.setText(timerText)
            }

            //when the timer finished, create a new code, then a new timer
            override fun onFinish() {
                var wrapper = Singleton.globalWrapper() as TOTPWrapper
                code.setText(wrapper.getTOTPcode(id))
                newTimer(timer, code, id)
            }
        }.start()
    }

}