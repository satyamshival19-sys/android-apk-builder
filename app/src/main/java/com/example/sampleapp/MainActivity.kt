package com.example.sampleapp

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var scriptsDir: File
    private lateinit var searchBar: EditText
    private val FILE_PICK_REQUEST = 101
    private val activeSwitches = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Please allow 'Display over other apps' for background execution.")
                .setPositiveButton("Allow") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }.setCancelable(false).show()
        }

        scriptsDir = File(filesDir, "scripts")
        if (!scriptsDir.exists()) scriptsDir.mkdirs()

        listContainer = findViewById(R.id.scriptListContainer)
        searchBar = findViewById(R.id.searchBar)

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        findViewById<Button>(R.id.btnAddFile).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, FILE_PICK_REQUEST)
        }
        refreshList()
    }

    override fun onResume() {
        super.onResume()
        if (::listContainer.isInitialized) refreshList(searchBar.text.toString())
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
                Toast.makeText(this, "You can only keep 12 files!", Toast.LENGTH_LONG).show()
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
            refreshList(searchBar.text.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading file!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshList(query: String = "") {
        listContainer.removeAllViews()
        val files = scriptsDir.listFiles { file -> 
            file.extension == "html" && file.name.contains(query, ignoreCase = true) 
        }
        
        activeSwitches.clear()
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        for ((key, value) in prefs.all) {
            if (key.startsWith("run_") && value == true) activeSwitches.add(key.replace("run_", ""))
        }

        if (files == null || files.isEmpty()) {
            val tv = TextView(this)
            tv.text = "No scripts found."
            tv.setTextColor(Color.WHITE)
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
            tv.setTextColor(Color.RED)
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            tv.setOnClickListener {
                val intent = Intent(this, DisplayActivity::class.java)
                intent.putExtra("SCRIPT_NAME", file.name)
                startActivity(intent)
            }
            tv.setOnLongClickListener { showRenameDialog(file); true }

            // Manual Reset Button (Clears only this script's data)
            val btnReset = TextView(this)
            btnReset.text = "🔄"
            btnReset.textSize = 18f
            btnReset.setPadding(16, 0, 16, 0)
            btnReset.setOnClickListener {
                WebViewManager.removeWebView(file.name)
                if (activeSwitches.contains(file.name)) {
                    WebViewManager.getOrCreateWebView(this, file.name)
                }
                Toast.makeText(this, "Data Cleared!", Toast.LENGTH_SHORT).show()
            }

            val btnDelete = TextView(this)
            btnDelete.text = "🗑️"
            btnDelete.textSize = 20f
            btnDelete.setPadding(16, 0, 16, 0)
            btnDelete.setOnClickListener { showDeleteDialog(file) }

            val toggle = Switch(this)
            val isRunning = activeSwitches.contains(file.name)
            toggle.isChecked = isRunning
            toggle.text = if (isRunning) "ON" else "OFF"

            toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Point 3: 5th toggle par banner
                    if (activeSwitches.size >= 4) {
                        toggle.isChecked = false
                        AlertDialog.Builder(this)
                            .setTitle("Limit Reached")
                            .setMessage("Aap ek samay par sirf 4 scripts chala sakte hain. Kripya pehle kisi ek script ka toggle OFF karein.")
                            .setPositiveButton("OK", null)
                            .show()
                        return@setOnCheckedChangeListener
                    }
                    toggle.text = "ON"
                    if (!activeSwitches.contains(file.name)) activeSwitches.add(file.name)
                    prefs.edit().putBoolean("run_${file.name}", true).apply()
                    
                    startService(Intent(this, HtmlService::class.java))
                    val intent = Intent(this, DisplayActivity::class.java)
                    intent.putExtra("SCRIPT_NAME", file.name)
                    startActivity(intent)
                    refreshList(searchBar.text.toString()) 
                    
                } else {
                    toggle.text = "OFF"
                    activeSwitches.remove(file.name)
                    
                    // Point 2: Reset only this specific script's data
                    WebViewManager.removeWebView(file.name)
                    prefs.edit().putBoolean("run_${file.name}", false).apply()
                    
                    if (activeSwitches.isEmpty()) stopService(Intent(this, HtmlService::class.java))
                    refreshList(searchBar.text.toString())
                }
            }

            row.addView(tv)
            row.addView(btnReset)
            row.addView(btnDelete)
            row.addView(toggle)
            listContainer.addView(row)
        }
    }

    private fun showRenameDialog(file: File) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Rename Script")
        val input = EditText(this)
        input.setText(file.name)
        builder.setView(input)
        builder.setPositiveButton("Rename") { _, _ ->
            var newName = input.text.toString().trim()
            if (newName.isEmpty()) return@setPositiveButton
            if (!newName.endsWith(".html", true)) newName += ".html"
            val newFile = File(scriptsDir, newName)
            if (newFile.exists()) {
                Toast.makeText(this, "File already exists!", Toast.LENGTH_SHORT).show()
            } else if (file.renameTo(newFile)) {
                refreshList(searchBar.text.toString())
            }
        }
        builder.setNegativeButton("Cancel", null).show()
    }

    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("Delete?")
            .setPositiveButton("Yes") { _, _ ->
                activeSwitches.remove(file.name)
                WebViewManager.removeWebView(file.name)
                getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit().putBoolean("run_${file.name}", false).apply()
                if (file.delete()) refreshList(searchBar.text.toString())
            }.setNegativeButton("No", null).show()
    }
}
