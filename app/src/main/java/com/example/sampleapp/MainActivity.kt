package com.example.sampleapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.*
import android.Manifest
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var scriptsDir: File
    private val FILE_PICK_REQUEST = 101
    
    // Active switches ka track rakhne ke liye
    private val activeSwitches = mutableListOf<Pair<String, Switch>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        scriptsDir = File(filesDir, "scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()

        listContainer = findViewById(R.id.scriptListContainer)
        
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
            val files = scriptsDir.listFiles { file -> file.extension == "html" }
            // 12 files ka limit
            if (files != null && files.size >= 12) {
                Toast.makeText(this, "आप केवल 12 फाइलें ही रख सकते हैं!", Toast.LENGTH_LONG).show()
                return
            }

            var fileName = "unknown.html"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val originalName = cursor.getString(nameIndex)
                    fileName = if (originalName.endsWith(".html", true)) originalName else "$originalName.html"
                }
            }

            val file = File(scriptsDir, fileName)
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            Toast.makeText(this, "फाइल जुड़ गई: $fileName", Toast.LENGTH_SHORT).show()
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
            tv.text = "कोई स्क्रिप्ट नहीं मिली।"
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
            tv.setTextColor(android.graphics.Color.RED)
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // Open DisplayActivity
            tv.setOnClickListener {
                val intent = Intent(this, DisplayActivity::class.java)
                intent.putExtra("SCRIPT_NAME", file.name)
                startActivity(intent)
            }

            // Delete Button
            val btnDelete = TextView(this)
            btnDelete.text = "🗑️"
            btnDelete.textSize = 20f
            btnDelete.setPadding(24, 0, 24, 0)
            btnDelete.setOnClickListener {
                showDeleteDialog(file)
            }

            val toggle = Switch(this)
            
            // Check if this file is currently active (WebView exists in Manager)
            val isRunning = WebViewManager.activeWebViews.containsKey(file.name)
            toggle.isChecked = isRunning
            toggle.text = if (isRunning) "ON" else "OFF"
            
            if (isRunning) {
                activeSwitches.add(Pair(file.name, toggle))
            }

            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Limit: Maximum 4 scripts at a time
                    if (activeSwitches.size >= 4) {
                        toggle.isChecked = false
                        Toast.makeText(this, "एक समय पर केवल 4 स्क्रिप्ट ही चल सकती हैं!", Toast.LENGTH_SHORT).show()
                        return@setOnCheckedChangeListener
                    }
                    toggle.text = "ON"
                    activeSwitches.add(Pair(file.name, toggle))
                    startBackgroundService(file.name)
                } else {
                    toggle.text = "OFF"
                    activeSwitches.removeAll { it.first == file.name }
                    stopBackgroundService(file.name)
                }
            }

            row.addView(tv)
            row.addView(btnDelete)
            row.addView(toggle)
            listContainer.addView(row)
        }
    }

    private fun showDeleteDialog(file: File) {
        android.app.AlertDialog.Builder(this)
            .setTitle("डिलीट करें?")
            .setMessage("क्या आप वाकई ${file.name} को डिलीट करना चाहते हैं?")
            .setPositiveButton("हाँ") { _, _ ->
                stopBackgroundService(file.name)
                WebViewManager.removeWebView(file.name)
                if (file.delete()) {
                    Toast.makeText(this, "फाइल डिलीट हो गई!", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton("नहीं", null)
            .show()
    }

    private fun startBackgroundService(fileName: String) {
        val intent = Intent(this, HtmlService::class.java)
        intent.action = "START"
        intent.putExtra("SCRIPT_NAME", fileName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopBackgroundService(fileName: String) {
        val intent = Intent(this, HtmlService::class.java)
        intent.action = "STOP"
        intent.putExtra("SCRIPT_NAME", fileName)
        startService(intent)
    }
}
