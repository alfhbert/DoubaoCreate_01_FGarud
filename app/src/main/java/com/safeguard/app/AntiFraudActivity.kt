package com.safeguard.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safeguard.app.databinding.ActivityAntifraudBinding

class AntiFraudActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAntifraudBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAntifraudBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.af_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
