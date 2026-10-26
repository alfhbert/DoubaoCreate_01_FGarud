package com.safeguard.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.safeguard.app.R
import com.safeguard.app.SafeguardService
import com.safeguard.app.util.Prefs

class SettingsFragment : Fragment() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (it[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true) {
                Prefs.backgroundLocationGranted = true
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_settings, container, false)

        val swService = v.findViewById<Switch>(R.id.sw_service)
        swService.isChecked = Prefs.serviceEnabled
        swService.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            Prefs.serviceEnabled = checked
            if (checked) {
                SafeguardService.start(requireContext())
            } else {
                SafeguardService.stop(requireContext())
            }
        }

        val etName = v.findViewById<EditText>(R.id.et_device_name)
        etName.setText(Prefs.deviceName)
        v.findViewById<View>(R.id.btn_save_name).setOnClickListener {
            val name = etName.text.toString().trim()
            if (name.isNotEmpty()) {
                Prefs.deviceName = name
                android.widget.Toast.makeText(requireContext(), R.string.saved, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        val swBigFont = v.findViewById<Switch>(R.id.sw_big_font)
        swBigFont.isChecked = Prefs.bigFont
        swBigFont.setOnCheckedChangeListener { _, checked ->
            Prefs.bigFont = checked
            android.widget.Toast.makeText(requireContext(), R.string.font_changed, android.widget.Toast.LENGTH_SHORT).show()
        }

        val tvThreshold = v.findViewById<TextView>(R.id.tv_threshold)
        tvThreshold.text = getString(R.string.threshold_text, Prefs.lowBatteryThreshold)
        v.findViewById<View>(R.id.btn_threshold).setOnClickListener {
            val presets = intArrayOf(10, 15, 20, 25, 30, 40, 50)
            val labels = presets.map { getString(R.string.threshold_value, it) }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.threshold_title)
                .setItems(labels) { _, which ->
                    Prefs.lowBatteryThreshold = presets[which]
                    tvThreshold.text = getString(R.string.threshold_text, Prefs.lowBatteryThreshold)
                }
                .show()
        }

        val tvZone = v.findViewById<TextView>(R.id.tv_zone)
        refreshZoneText(tvZone)
        v.findViewById<View>(R.id.btn_set_zone).setOnClickListener {
            val dialog = AlertDialog.Builder(requireContext())
            dialog.setTitle(R.string.zone_title)
            val latInput = EditText(requireContext()).apply { hint = getString(R.string.zone_lat_hint) }
            val lngInput = EditText(requireContext()).apply { hint = getString(R.string.zone_lng_hint) }
            val radiusInput = EditText(requireContext()).apply { hint = getString(R.string.zone_radius_hint) }
            val layout = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                addView(latInput)
                addView(lngInput)
                addView(radiusInput)
            }
            dialog.setView(layout)
            dialog.setPositiveButton(R.string.ok) { _, _ ->
                val lat = latInput.text.toString().toDoubleOrNull()
                val lng = lngInput.text.toString().toDoubleOrNull()
                val radius = radiusInput.text.toString().toIntOrNull()
                if (lat != null && lng != null && radius != null && radius > 0) {
                    Prefs.safeZoneLat = lat
                    Prefs.safeZoneLng = lng
                    Prefs.safeZoneRadius = radius
                    Prefs.safeZoneEnabled = true
                    refreshZoneText(tvZone)
                } else {
                    android.widget.Toast.makeText(requireContext(), R.string.zone_invalid, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            dialog.setNegativeButton(R.string.cancel, null)
            dialog.show()
        }
        v.findViewById<View>(R.id.btn_clear_zone).setOnClickListener {
            Prefs.safeZoneEnabled = false
            refreshZoneText(tvZone)
        }

        val tvBlack = v.findViewById<TextView>(R.id.tv_blacklist)
        refreshBlacklist(tvBlack)
        v.findViewById<View>(R.id.btn_blacklist).setOnClickListener {
            val input = EditText(requireContext()).apply { hint = getString(R.string.blacklist_hint) }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.blacklist_title)
                .setView(input)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val num = input.text.toString().trim()
                    if (num.isNotEmpty()) {
                        val list = Prefs.blacklistList().toMutableList()
                        if (!list.contains(num)) list.add(num)
                        Prefs.blacklistSave(list)
                        refreshBlacklist(tvBlack)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        v.findViewById<View>(R.id.btn_clear_blacklist).setOnClickListener {
            Prefs.blacklistSave(emptyList())
            refreshBlacklist(tvBlack)
        }

        v.findViewById<View>(R.id.btn_background_location).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 30 && !Prefs.backgroundLocationGranted) {
                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
            } else {
                android.widget.Toast.makeText(requireContext(), R.string.bg_location_done, android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        v.findViewById<View>(R.id.btn_events_clear).setOnClickListener {
            com.safeguard.app.util.EventsStore.clear(requireContext())
            android.widget.Toast.makeText(requireContext(), R.string.events_cleared, android.widget.Toast.LENGTH_SHORT).show()
        }

        return v
    }

    private fun refreshZoneText(tv: TextView) {
        tv.text = if (Prefs.safeZoneEnabled) {
            getString(
                R.string.zone_text,
                Prefs.safeZoneLat, Prefs.safeZoneLng, Prefs.safeZoneRadius
            )
        } else {
            getString(R.string.zone_disabled)
        }
    }

    private fun refreshBlacklist(tv: TextView) {
        val list = Prefs.blacklistList()
        tv.text = if (list.isEmpty()) {
            getString(R.string.blacklist_empty)
        } else {
            list.joinToString("、")
        }
    }
}
