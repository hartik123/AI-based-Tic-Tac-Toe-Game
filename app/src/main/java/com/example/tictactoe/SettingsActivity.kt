package com.example.tictactoe

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = layoutInflater
        val settingsLayout = inflater.inflate(R.layout.activity_settings, null)

        // Find the FrameLayout to add the game UI
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        contentFrame.addView(settingsLayout)
        // Cache file for game settings
        val file = File(cacheDir, "game_cache.json")
        val settingsContent = settingsLayout.findViewById<LinearLayout>(R.id.settings_content)

        // Initialize button colors based on cached data
        initializeButtonColors(settingsContent, file)

        // Set up button click listeners
        setupButtonListeners(settingsContent, file)
    }

    private fun initializeButtonColors(settingsContent: LinearLayout, file: File) {
        var currentGameLevel: String
        if (file.exists()) {
            val cachedData = file.readText()
            val jsonObject = JSONObject(cachedData)// Default to "medium"
            currentGameLevel = jsonObject.getString("gameLevel")
            Log.d("DEFINING", ""+jsonObject.getString("gameLevel"))
        } else {
            currentGameLevel = "easy" // Default game level if no file exists
        }

        for (i in 0 until settingsContent.childCount) {
            val btn = settingsContent.getChildAt(i) as Button
            btn.setBackgroundColor(
                if (btn.text.toString().lowercase() == currentGameLevel) {
                    Color.parseColor("#6375eb") // Selected level color
                } else {
                    Color.parseColor("#d98ff7") // Unselected level color
                }
            )
        }
    }

    private fun setupButtonListeners(settingsContent: LinearLayout, file: File) {
        for (i in 0 until settingsContent.childCount) {
            val btn = settingsContent.getChildAt(i) as Button
            btn.setOnClickListener {
                updateGameLevel(btn, file, settingsContent)
            }
        }
    }

    private fun updateGameLevel(selectedButton: Button, file: File, settingsContent: LinearLayout) {
        if (file.exists()) {
            // Step 1: Read the existing cached data
            val cachedData = file.readText()
            val jsonObject = JSONObject(cachedData)

            // Step 2: Update the game level in the JSON object
            jsonObject.put("gameLevel", selectedButton.text.toString().lowercase())
            Log.d("STATUS", ""+selectedButton.text.toString())
            // Step 3: Write the updated JSON back to the file
            file.writeText(jsonObject.toString())
        }

        // Update button colors after selection
        for (i in 0 until settingsContent.childCount) {
            val btn = settingsContent.getChildAt(i) as Button
            btn.setBackgroundColor(
                if (btn.text.toString().lowercase() == selectedButton.text.toString().lowercase()) {
                    Color.parseColor("#6375eb") // Selected button color
                } else {
                    Color.parseColor("#d98ff7") // Unselected button color
                }
            )
        }
    }
}
