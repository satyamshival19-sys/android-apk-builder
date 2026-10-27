package com.example.sampleapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.Manifest
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var scriptsDir: File
    private val FILE_PICK_REQUEST = 101
    private val activeSwitches = mutableListOf<String>()
    
    // 🔥 FIX 6: Infinite Loop se bachne ke liye flag
    private var isLimitExceededAction = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("बैकग्राउंड में चलने के लिए कृपया 'Display over other apps' की परमिशन दें।")
                .setPositiveButton("Allow") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }.setCancelable(false).show()
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

    override fun onResume() {
        super.onResume()
        if (::listContainer.isInitialized) refreshList()
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
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this, "फाइल जुड़ गई: $fileName", Toast.LENGTH_SHORT).show()
            refreshList()
        } catch (e: Exception) {
            Toast.makeText(this, "फाइल लोड करने में एरर!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList() {
        listContainer.removeAllViews()
        val files = scriptsDir.listFiles { file -> file.extension == "html" }
        
        activeSwitches.clear()
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            if (key.startsWith("run_") && value == true) activeSwitches.add(key.replace("run_", ""))
        }

        if (files == null || files.isEmpty()) {
            val tv = TextView(this).apply { text = "कोई स्क्रिप्ट नहीं मिली।" }
            listContainer.addView(tv)
            return
        }

        for (file in files) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 20, 0, 20)
            }

            val tv = TextView(this).apply {
                text = file.name
                textSize = 16f
                setTextColor(android.graphics.Color.RED)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, DisplayActivity::class.java).putExtra("SCRIPT_NAME", file.name))
                }
                setOnLongClickListener { showRenameDialog(file); true }
            }

            val btnDelete = TextView(this).apply {
                text = "🗑️"
                textSize = 20f
                setPadding(24, 0, 24, 0)
                setOnClickListener { showDeleteDialog(file) }
            }

            val toggle = Switch(this)
            val isRunning = activeSwitches.contains(file.name)
            toggle.isChecked = isRunning
            toggle.text = if (isRunning) "ON" else "OFF"

            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isLimitExceededAction) return@setOnCheckedChangeListener // Loop avoid

                if (isChecked) {
                    // 🔥 FIX 7: 5th toggle par Banner
                    if (activeSwitches.size >= 4) {
                        isLimitExceededAction = true
                        toggle.isChecked = false
                        isLimitExceededAction = false
                        
                        AlertDialog.Builder(this)
                            .setTitle("Limit Reached")
                            .setMessage("Aap ek samay par sirf 4 scripts chala sakte hain. Kripya pehle kisi ek script ka toggle OFF karein.")
                            .setPositiveButton("OK", null).show()
                        return@setOnCheckedChangeListener
                    }
                    
                    toggle.text = "ON"
                    activeSwitches.add(file.name)
                    prefs.edit().putBoolean("run_${file.name}", true).apply()
                    
                    startService(Intent(this, HtmlService::class.java))
                    startActivity(Intent(this, DisplayActivity::class.java).putExtra("SCRIPT_NAME", file.name))
                    
                } else {
                    toggle.text = "OFF"
                    activeSwitches.remove(file.name)
                    
                    // 🔥 FIX 8: Specific script ka data clear
                    WebViewManager.removeWebView(file.name)
                    prefs.edit().putBoolean("run_${file.name}", false).apply()
                    
                    if (activeSwitches.isEmpty()) stopService(Intent(this, HtmlService::class.java))
                }
            }

            row.addView(tv)
            row.addView(btnDelete)
            row.addView(toggle)
            listContainer.addView(row)
        }
    }

    private fun showRenameDialog(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("स्क्रिप्ट का नाम बदलें")
        val input = EditText(this).apply {
            setText(file.name)
            setSelection(file.name.lastIndexOf("."))
        }
        builder.setView(input)

        builder.setPositiveButton("बदलें") { _, _ ->
            var newName = input.text.toString().trim()
            if (newName.isEmpty()) return@setPositiveButton
            if (!newName.endsWith(".html", ignoreCase = true)) newName += ".html"
            if (newName == file.name) return@setPositiveButton

            val newFile = File(scriptsDir, newName)
            if (newFile.exists()) {
                Toast.makeText(this, "इस नाम की फाइल पहले से मौजूद है!", Toast.LENGTH_SHORT).show()
            } else if (file.renameTo(newFile)) {
                Toast.makeText(this, "नाम बदल गया!", Toast.LENGTH_SHORT).show()
                refreshList()
            } else {
                Toast.makeText(this, "नाम नहीं बदल पाया!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("रद्द करें") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("डिलीट करें?")
            .setMessage("क्या आप वाकई ${file.name} को डिलीट करना चाहते हैं?")
            .setPositiveButton("हाँ") { _, _ ->
                activeSwitches.remove(file.name)
                WebViewManager.removeWebView(file.name)
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putBoolean("run_${file.name}", false).apply()
                if (file.delete()) {
                    Toast.makeText(this, "फाइल डिलीट हो गई!", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            .setNegativeButton("नहीं", null)
            .show()
    }
}
