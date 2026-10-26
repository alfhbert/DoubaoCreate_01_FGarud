package com.safeguard.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.safeguard.app.util.Prefs
import java.util.concurrent.ThreadLocalRandom
import kotlin.text.Regex

class PairActivity : AppCompatActivity() {

    private val codeRegex = Regex("^[0-9]{6}$")

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                it[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                Prefs.locationGranted = true
            }
            if (it[Manifest.permission.POST_NOTIFICATIONS] == true) {
                Prefs.notificationGranted = true
            }
            if (it[Manifest.permission.READ_PHONE_STATE] == true) {
                Prefs.phoneGranted = true
            }
            if (it[Manifest.permission.READ_CONTACTS] == true) {
                Prefs.contactsGranted = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pair)
        supportActionBar?.setTitle(R.string.pair_title)

        findViewById<android.widget.Button>(R.id.btn_generate).setOnClickListener {
            val code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000))
            Prefs.pairCode = code
            Prefs.paired = true
            ensurePermissions()
            Toast.makeText(this, R.string.pair_code_generated, Toast.LENGTH_LONG).show()
            SafeguardService.start(this)
            finish()
        }

        findViewById<android.widget.Button>(R.id.btn_join).setOnClickListener {
            val code = findViewById<android.widget.EditText>(R.id.edit_code).text.toString().trim()
            if (!codeRegex.matches(code)) {
                Toast.makeText(this, R.string.pair_code_invalid, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            Prefs.pairCode = code
            Prefs.paired = true
            ensurePermissions()
            SafeguardService.start(this)
            finish()
        }

        findViewById<android.widget.Button>(R.id.btn_unpair).setOnClickListener {
            Prefs.paired = false
            Prefs.pairCode = ""
            Toast.makeText(this, R.string.pair_unpaired, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun ensurePermissions() {
        val needed = mutableListOf<String>()
        if (!Prefs.locationGranted) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && !Prefs.notificationGranted) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!Prefs.phoneGranted) {
            needed.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (!Prefs.contactsGranted) {
            needed.add(Manifest.permission.READ_CONTACTS)
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
