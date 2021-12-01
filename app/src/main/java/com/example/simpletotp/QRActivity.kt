package com.example.simpletotp

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.example.simpletotp.totp.TOTPWrapper
import java.util.jar.Manifest

class QRActivity(val wrapper: TOTPWrapper) : AppCompatActivity() {

    private lateinit var codescanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr)

        //get permission to use camera
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 123)
        } else {
            startScanning()
        }
    }

    private fun startScanning() {
        //set up all the options for the camera/scanner
        val scannerView: CodeScannerView = findViewById(R.id.scanner_view)
        codescanner = CodeScanner(this, scannerView)
        codescanner.camera = CodeScanner.CAMERA_BACK
        codescanner.formats = CodeScanner.ALL_FORMATS

        codescanner.autoFocusMode = AutoFocusMode.SAFE
        codescanner.scanMode = ScanMode.SINGLE
        codescanner.isAutoFocusEnabled = true
        codescanner.isFlashEnabled = false

        //adds the skip button to the bottom right
        val skip = findViewById<View>(R.id.skipButton)
        skip.setOnClickListener {
            //when clicked, a message will pop up, making them confirm their choice
            val skipDialog: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
            skipDialog.setTitle("Skip?")
            skipDialog.setMessage("Do you want to skip the scanner and manually input a key?")
            //if the press 'Yes', they will be brought to the add entry screen with two blank boxes
            skipDialog.setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, which ->
                val intent = Intent(this, NewEntryActivity::class.java)
                intent.putExtra("KEY","")
                startActivity(intent)
            })
            //if the press 'Cancel', they can continue scanning as normal
            skipDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })
            skipDialog.show()
        }

        //if it successfully scans a code, start the NewEntryActivity and send it the key
        codescanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                Toast.makeText(this, "Scan Result: ${it.text}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, NewEntryActivity(wrapper)::class.java)
                intent.putExtra("KEY",it.text)
                startActivity(intent)
            }
        }

        //shows message if the camera has an error
        codescanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        //auto-focus when you tap the screen
        scannerView.setOnClickListener {
            codescanner.startPreview()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //checks that the camera permission request was successful
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                startScanning()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::codescanner.isInitialized) {
            codescanner?.startPreview()
        }
    }

    override fun onPause() {
        if (::codescanner.isInitialized) {
            codescanner?.releaseResources()
        }
        super.onPause()
    }

}
