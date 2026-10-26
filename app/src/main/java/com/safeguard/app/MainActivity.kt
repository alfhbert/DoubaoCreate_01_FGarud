package com.safeguard.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.safeguard.app.ui.EventsFragment
import com.safeguard.app.ui.HomeFragment
import com.safeguard.app.ui.MapFragment
import com.safeguard.app.ui.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val home = HomeFragment()
    private val map = MapFragment()
    private val events = EventsFragment()
    private val settings = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setTitle(R.string.app_name)

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTo(home)
                R.id.nav_map -> switchTo(map)
                R.id.nav_events -> switchTo(events)
                R.id.nav_settings -> switchTo(settings)
            }
            true
        }
        if (savedInstanceState == null) {
            switchTo(home)
            nav.selectedItemId = R.id.nav_home
        }
    }

    private fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_pair -> {
                startActivity(android.content.Intent(this, PairActivity::class.java))
                true
            }
            R.id.action_antifraud -> {
                startActivity(android.content.Intent(this, AntiFraudActivity::class.java))
                true
            }
            R.id.action_start -> {
                SafeguardService.start(this)
                Toast.makeText(this, R.string.service_started, Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_stop -> {
                SafeguardService.stop(this)
                Toast.makeText(this, R.string.service_stopped, Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
