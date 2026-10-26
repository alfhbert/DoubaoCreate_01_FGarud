package com.safeguard.app.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.safeguard.app.R
import com.safeguard.app.SafeguardService
import com.safeguard.app.StatusHub
import com.safeguard.app.databinding.FragmentSettingsBinding
import com.safeguard.app.util.FontScaler
import com.safeguard.app.util.Prefs
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FontScaler.apply(binding.root, if (Prefs.largeFont) 1f else 0.82f)

        binding.btnSaveName.setOnClickListener {
            val name = binding.editDeviceName.text?.toString()?.trim()
            if (!name.isNullOrBlank()) {
                Prefs.deviceName = name
                Toast.makeText(requireContext(), R.string.toast_saved, Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchService.setOnCheckedChangeListener { _, checked ->
            Prefs.serviceEnabled = checked
            if (checked) SafeguardService.start(requireContext())
            else SafeguardService.stop(requireContext())
        }

        binding.seekBattery.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                Prefs.lowBatteryThreshold = progress
                binding.textBatteryValue.text = getString(
                    R.string.set_battery_threshold_value, progress
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.switchSafeZone.setOnCheckedChangeListener { _, checked ->
            Prefs.safeZoneEnabled = checked
            updateSafeZoneState()
        }

        binding.btnSetHere.setOnClickListener {
            val my = StatusHub.myStatus
            if (my?.hasLocation == true) {
                Prefs.safeZoneLat = my.lat
                Prefs.safeZoneLng = my.lng
                updateSafeZoneState()
                Toast.makeText(requireContext(), R.string.toast_zone_set, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), R.string.toast_no_loc, Toast.LENGTH_LONG).show()
            }
        }

        binding.seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val r = 200 + progress * 100
                Prefs.safeZoneRadius = r
                binding.textRadiusValue.text = getString(R.string.set_safe_zone_radius_value, r)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnAddBlacklist.setOnClickListener {
            val n = binding.editBlacklist.text?.toString()?.trim()
            if (!n.isNullOrBlank()) {
                Prefs.addBlacklist(n)
                binding.editBlacklist.setText("")
                renderBlacklist()
            }
        }

        binding.switchLargeFont.setOnCheckedChangeListener { _, checked ->
            Prefs.largeFont = checked
            activity?.recreate()
        }

        loadValues()
    }

    override fun onResume() {
        super.onResume()
        loadValues()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun loadValues() {
        binding.editDeviceName.setText(Prefs.deviceName)
        binding.switchService.isChecked = Prefs.serviceEnabled
        binding.seekBattery.progress = Prefs.lowBatteryThreshold.coerceIn(10, 50)
        binding.textBatteryValue.text = getString(
            R.string.set_battery_threshold_value, Prefs.lowBatteryThreshold
        )
        binding.switchSafeZone.isChecked = Prefs.safeZoneEnabled
        val r = Prefs.safeZoneRadius.coerceIn(200, 5000)
        binding.seekRadius.progress = (r - 200) / 100
        binding.textRadiusValue.text = getString(R.string.set_safe_zone_radius_value, r)
        binding.switchLargeFont.isChecked = Prefs.largeFont
        updateSafeZoneState()
        renderBlacklist()
    }

    private fun updateSafeZoneState() {
        binding.textSafeZoneState.text = when {
            !Prefs.safeZoneEnabled -> getString(R.string.set_safe_zone_off)
            Prefs.safeZoneLat == 0.0 && Prefs.safeZoneLng == 0.0 ->
                getString(R.string.set_safe_zone_none)
            else -> String.format(
                Locale.CHINA,
                "安全中心：%.4f, %.4f · 半径 %d 米",
                Prefs.safeZoneLat,
                Prefs.safeZoneLng,
                Prefs.safeZoneRadius
            )
        }
    }

    private fun renderBlacklist() {
        binding.containerBlacklist.removeAllViews()
        val items = Prefs.blacklistList()
        if (items.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = getString(R.string.set_blacklist_empty)
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                setPadding(4, 8, 4, 8)
            }
            binding.containerBlacklist.addView(tv)
            return
        }
        for (n in items) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val tv = TextView(requireContext()).apply {
                text = n
                textSize = 16f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            val btn = MaterialButton(requireContext()).apply {
                text = getString(R.string.btn_remove)
                textSize = 14f
                isAllCaps = false
            }
            btn.setOnClickListener {
                Prefs.removeBlacklist(n)
                renderBlacklist()
            }
            row.addView(tv)
            row.addView(btn)
            binding.containerBlacklist.addView(row)
        }
    }
}
