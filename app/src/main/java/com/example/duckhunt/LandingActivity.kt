package com.example.duckhunt

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class LandingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        val playButton: Button = findViewById(R.id.playButton)
        playButton.setOnClickListener {
            // Start the main game activity when Play button is pressed
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
