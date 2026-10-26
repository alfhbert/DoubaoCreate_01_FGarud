package com.safeguard.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.safeguard.app.databinding.ActivityMainBinding
import com.safeguard.app.ui.EventsFragment
import com.safeguard.app.ui.HomeFragment
import com.safeguard.app.ui.MapFragment
import com.safeguard.app.ui.SettingsFragment
import com.safeguard.app.util.Prefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQ_PERMS = 100
    private val REQ_BG = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        if (savedInstanceState == null) {
            switchTab(R.id.nav_home)
        }
        binding.bottomNav.setOnItemSelectedListener { item ->
            switchTab(item.itemId)
            true
        }
        requestPermissionsIfNeeded()
    }

    private fun switchTab(id: Int) {
        val f: Fragment = when (id) {
            R.id.nav_map -> MapFragment()
            R.id.nav_events -> EventsFragment()
            R.id.nav_settings -> SettingsFragment()
            else -> HomeFragment()
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_pair -> {
                startActivity(Intent(this, PairActivity::class.java))
                true
            }
            R.id.action_antifraud -> {
                startActivity(Intent(this, AntiFraudActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestPermissionsIfNeeded() {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.POST_NOTIFICATIONS
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.READ_PHONE_STATE
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.READ_CONTACTS
        }
        if (need.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, need.toTypedArray(), REQ_PERMS)
        } else {
            maybeRequestBgLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMS) {
            maybeRequestBgLocation()
        }
    }

    private fun maybeRequestBgLocation() {
        if (Build.VERSION.SDK_INT >= 29 && !Prefs.bgLocationRequested) {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            val bg = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            if ((fine || coarse) && !bg) {
                Prefs.bgLocationRequested = true
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQ_BG
                )
            }
        }
    }
}
