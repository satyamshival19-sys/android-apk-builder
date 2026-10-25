package com.example.sampleapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var listContainer: LinearLayout
    private val scriptsDir = File(filesDir, "scripts")
    private var activeSwitch: Switch? = null
    private val FILE_PICK_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // नोटिफिकेशन परमिशन
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        if (!scriptsDir.exists()) scriptsDir.mkdirs()

        listContainer = findViewById(R.id.scriptListContainer)
        
        // फाइल चुनने का पुराना और सुरक्षित तरीका
        findViewById<Button>(R.id.btnAddFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*" // सभी फाइलें दिखेंगी
            startActivityForResult(intent, FILE_PICK_REQUEST)
        }

        refreshList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                saveHtmlFile(uri)
            }
        }
    }

    private fun saveHtmlFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = "script_" + System.currentTimeMillis() + ".html"
            val file = File(scriptsDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Toast.makeText(this, "फाइल जुड़ गई!", Toast.LENGTH_SHORT).show()
            refreshList()
        } catch (e: Exception) {
            Toast.makeText(this, "फाइल लोड करने में एरर!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val files = scriptsDir.listFiles { file -> file.extension == "html" }
        
        if (files == null || files.isEmpty()) {
            val tv = TextView(this)
            tv.text = "कोई स्क्रिप्ट नहीं मिली। ऊपर से फाइल जोड़ें।"
            listContainer.addView(tv)
            return
        }

        for (file in files) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, 20, 0, 20)

            val tv = TextView(this)
            tv.text = file.name
            tv.textSize = 16f
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val toggle = Switch(this)
            toggle.text = "OFF"
            
            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    if (activeSwitch != null && activeSwitch != toggle) {
                        activeSwitch?.isChecked = false
                    }
                    activeSwitch = toggle
                    toggle.text = "ON"
                    startBackgroundService(file.name)
                } else {
                    if (activeSwitch == toggle) {
                        activeSwitch = null
                        toggle.text = "OFF"
                        stopService(Intent(this, HtmlService::class.java))
                    }
                }
            }

            row.addView(tv)
            row.addView(toggle)
            listContainer.addView(row)
        }
    }

    private fun startBackgroundService(fileName: String) {
        val intent = Intent(this, HtmlService::class.java)
        intent.putExtra("SCRIPT_NAME", fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
