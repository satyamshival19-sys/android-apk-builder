package com.example.sampleapp

import android.app.Activity
import android.app.AlertDialog
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
    private var activeSwitch: Switch? = null
    private val FILE_PICK_REQUEST = 101

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
            if (files != null && files.size >= 5) {
                Toast.makeText(this, "आप केवल 5 फाइलें ही रख सकते हैं!", Toast.LENGTH_LONG).show()
                return
            }

            var fileName = "unknown.html"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val originalName = cursor.getString(nameIndex)
                    if (originalName.endsWith(".html", true)) {
                        fileName = originalName
                    } else {
                        fileName = "$originalName.html"
                    }
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

            // 1. स्क्रिप्ट का नाम
            val tv = TextView(this)
            tv.text = file.name
            tv.textSize = 16f
            tv.setTextColor(android.graphics.Color.RED)
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // 👇 नाम बदलने का लॉजिक (लॉन्ग प्रेस करने पर) 👇
            tv.setOnLongClickListener {
                showRenameDialog(file, tv)
                true
            }

            // क्लिक करने पर HTML स्क्रीन खुलेगी
            tv.setOnClickListener {
                val intent = Intent(this, DisplayActivity::class.java)
                intent.putExtra("SCRIPT_NAME", file.name)
                startActivity(intent)
            }

            // 2. डिलीट बटन (🗑️)
            val btnDelete = TextView(this)
            btnDelete.text = "🗑️" // कचरा पेटी आइकन
            btnDelete.textSize = 20f
            btnDelete.setPadding(24, 0, 24, 0)
            btnDelete.setOnClickListener {
                showDeleteDialog(file)
            }

            // 3. टॉगल स्विच
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
                    
                    val intent = Intent(this, DisplayActivity::class.java)
                    intent.putExtra("SCRIPT_NAME", file.name)
                    startActivity(intent)
                    
                } else {
                    if (activeSwitch == toggle) {
                        activeSwitch = null
                        toggle.text = "OFF"
                        stopService(Intent(this, HtmlService::class.java))
                        DisplayActivity.instance?.finish()
                    }
                }
            }

            row.addView(tv)
            row.addView(btnDelete)
            row.addView(toggle)
            listContainer.addView(row)
        }
    }

    // 👇 नाम बदलने वाला डायलॉग 👇
    private fun showRenameDialog(file: File, tv: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("स्क्रिप्ट का नाम बदलें")

        val input = EditText(this)
        input.setText(file.name)
        // कर्सर को .html से पहले रखें
        input.setSelection(file.name.lastIndexOf("."))
        builder.setView(input)

        builder.setPositiveButton("बदलें") { _, _ ->
            var newName = input.text.toString().trim()
            if (newName.isEmpty()) return@setPositiveButton
            
            if (!newName.endsWith(".html", ignoreCase = true)) {
                newName += ".html"
            }

            if (newName == file.name) return@setPositiveButton

            val newFile = File(scriptsDir, newName)
            if (newFile.exists()) {
                Toast.makeText(this, "इस नाम की फाइल पहले से मौजूद है!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            // अगर यही फाइल चल रही है, तो सर्विस बंद करें
            if (activeSwitch != null && tv.text == file.name) {
                stopService(Intent(this, HtmlService::class.java))
                DisplayActivity.instance?.finish()
                activeSwitch = null
            }

            if (file.renameTo(newFile)) {
                Toast.makeText(this, "नाम बदल गया!", Toast.LENGTH_SHORT).show()
                refreshList()
            } else {
                Toast.makeText(this, "नाम नहीं बदल पाया!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("रद्द करें") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    // 👇 फाइल डिलीट करने वाला डायलॉग 👇
    private fun showDeleteDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle("डिलीट करें?")
            .setMessage("क्या आप वाकई ${file.name} को डिलीट करना चाहते हैं?")
            .setPositiveButton("हाँ") { _, _ ->
                // अगर यही फाइल चल रही है, तो सर्विस बंद करें
                if (activeSwitch != null) {
                    stopService(Intent(this, HtmlService::class.java))
                    DisplayActivity.instance?.finish()
                    activeSwitch = null
                }
                
                if (file.delete()) {
                    Toast.makeText(this, "फाइल डिलीट हो गई!", Toast.LENGTH_SHORT).show()
                    refreshList()
                } else {
                    Toast.makeText(this, "डिलीट नहीं हो पाई!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("नहीं", null)
            .show()
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
