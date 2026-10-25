package com.example.sampleapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.Manifest
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var scriptsDir: File
    private var activeSwitch: Switch? = null
    private val FILE_PICK_REQUEST = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // नोटिफिकेशन की परमिशन मांगें
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        scriptsDir = File(filesDir, "scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()

        listContainer = findViewById(R.id.scriptListContainer)
        
        // फाइल जोड़ने का बटन
        findViewById<Button>(R.id.btnAddFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICK_REQUEST)
        }

        refreshList()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) saveHtmlFile(uri)
        }
    }

    private fun saveHtmlFile(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val files = scriptsDir.listFiles { file -> file.extension == "html" }
            if (files != null && files.size >= 5) {
                Toast.makeText(this, "आप केवल 5 फाइलें ही रख सकते हैं!", Toast.LENGTH_LONG).show()
                return
            }
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
            tv.setTextColor(android.graphics.Color.BLUE)
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // अगर आप सिर्फ देखना चाहें बिना ON किए, तो नाम पर टैप कर सकते हैं
            tv.setOnClickListener {
                val intent = Intent(this, DisplayActivity::class.java)
                intent.putExtra("SCRIPT_NAME", file.name)
                startActivity(intent)
            }

            // 👇 यह है आपका ON/OFF टॉगल 👇
            val toggle = Switch(this)
            toggle.text = "OFF"
            
            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // 1. अगर कोई और स्विच ऑन है, तो उसे बंद करें (एक बार में एक ही चलेगा)
                    if (activeSwitch != null && activeSwitch != toggle) {
                        activeSwitch?.isChecked = false
                    }
                    activeSwitch = toggle
                    toggle.text = "ON"
                    
                    // 2. बैकग्राउंड में चलाने के लिए सर्विस शुरू करें
                    startBackgroundService(file.name)
                    
                    // 3. स्क्रीन पर देखने के लिए डिस्प्ले एक्टिविटी खोलें
                    val intent = Intent(this, DisplayActivity::class.java)
                    intent.putExtra("SCRIPT_NAME", file.name)
                    startActivity(intent)
                    
                } else {
                    if (activeSwitch == toggle) {
                        activeSwitch = null
                        toggle.text = "OFF"
                        
                        // 1. बैकग्राउंड सर्विस बंद करें
                        stopService(Intent(this, HtmlService::class.java))
                        
                        // 2. खुली हुई स्क्रीन बंद करें
                        DisplayActivity.instance?.finish()
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
