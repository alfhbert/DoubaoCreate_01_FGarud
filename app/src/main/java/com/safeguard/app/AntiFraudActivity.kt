package com.safeguard.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AntiFraudActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_antifraud)
        supportActionBar?.setTitle(R.string.antifraud_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
