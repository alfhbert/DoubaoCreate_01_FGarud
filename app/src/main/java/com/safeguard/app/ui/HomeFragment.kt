package com.safeguard.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.safeguard.app.R
import com.safeguard.app.SafeguardService
import com.safeguard.app.StatusHub
import com.safeguard.app.net.DeviceStatus
import com.safeguard.app.util.DeviceInfo
import com.safeguard.app.util.Prefs
import com.safeguard.app.util.LocationUtils

class HomeFragment : Fragment() {

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
            refresh()
        }

    private lateinit var tvRemoteName: TextView
    private lateinit var tvRemoteBattery: TextView
    private lateinit var tvRemoteLoc: TextView
    private lateinit var tvRemoteCall: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvConn: TextView
    private lateinit var tvSosHint: TextView
    private lateinit var btnSos: View
    private lateinit var tvMyName: TextView
    private lateinit var tvMyBattery: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        tvRemoteName = v.findViewById(R.id.tv_remote_name)
        tvRemoteBattery = v.findViewById(R.id.tv_remote_battery)
        tvRemoteLoc = v.findViewById(R.id.tv_remote_loc)
        tvRemoteCall = v.findViewById(R.id.tv_remote_call)
        tvDistance = v.findViewById(R.id.tv_distance)
        tvConn = v.findViewById(R.id.tv_conn)
        tvSosHint = v.findViewById(R.id.tv_sos_hint)
        btnSos = v.findViewById(R.id.btn_sos)
        tvMyName = v.findViewById(R.id.tv_my_name)
        tvMyBattery = v.findViewById(R.id.tv_my_battery)

        btnSos.setOnClickListener {
            if (Prefs.isPaired) {
                SafeguardService.sendSos(requireContext())
            } else {
                tvSosHint.text = getString(R.string.sos_unpaired_hint)
            }
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        StatusHub.addListener(refreshListener)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        StatusHub.removeListener(refreshListener)
    }

    private val refreshListener: () -> Unit = { refresh() }

    private fun refresh() {
        if (!::tvRemoteName.isInitialized) return
        val ctx = requireContext()

        tvMyName.text = getString(R.string.me_label, Prefs.deviceName)
        tvMyBattery.text = getString(
            R.string.battery_text,
            DeviceInfo.batteryLevel(ctx),
            if (DeviceInfo.isCharging(ctx)) getString(R.string.charging) else getString(R.string.not_charging)
        )

        tvConn.text = when {
            !Prefs.isPaired -> getString(R.string.conn_unpaired)
            StatusHub.connected -> getString(R.string.conn_online, StatusHub.broker)
            else -> getString(R.string.conn_connecting)
        }

        val remote = StatusHub.remoteStatus
        if (remote == null) {
            tvRemoteName.text = getString(R.string.remote_unpaired_or_offline)
            tvRemoteBattery.text = ""
            tvRemoteLoc.text = ""
            tvRemoteCall.text = ""
            tvDistance.text = ""
            btnSos.isEnabled = Prefs.isPaired
        } else {
            renderRemote(remote)
        }

        val missing = missingPermissions(ctx)
        if (missing.isNotEmpty()) {
            tvSosHint.text = getString(R.string.permission_hint, missing.joinToString("、"))
            tvSosHint.visibility = View.VISIBLE
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            tvSosHint.visibility = View.GONE
        }
    }

    private fun renderRemote(s: DeviceStatus) {
        tvRemoteName.text = s.deviceName
        tvRemoteBattery.text = if (s.battery in 0..100) {
            getString(
                R.string.battery_text,
                s.battery,
                if (s.charging) getString(R.string.charging) else getString(R.string.not_charging)
            )
        } else {
            getString(R.string.battery_unknown)
        }
        tvRemoteLoc.text = if (s.hasLocation) {
            getString(
                R.string.loc_text,
                String.format(java.util.Locale.CHINA, "%.4f, %.4f", s.lat, s.lng)
            )
        } else {
            getString(R.string.loc_none)
        }
        tvRemoteCall.text = callText(s)
        tvDistance.text = if (s.hasLocation && (s.lat != 0.0 || s.lng != 0.0)) {
            getString(R.string.distance_text, LocationUtils.formatDistance(LocationUtils.distanceMeters(0.0, 0.0, 0.0, 0.0)))
        } else {
            ""
        }
        btnSos.isEnabled = Prefs.isPaired
    }

    private fun callText(s: DeviceStatus): String {
        val base = when (s.callState) {
            "ringing" -> getString(R.string.call_ringing)
            "offhook" -> getString(R.string.call_offhook)
            else -> getString(R.string.call_idle)
        }
        val number = s.callNumber
        val contact = s.callContact
        return when {
            number.isNullOrBlank() && contact.isNullOrBlank() -> base
            contact != null -> "$base · $contact（$number）"
            else -> "$base · $number"
        }
    }

    private fun missingPermissions(ctx: android.content.Context): List<String> {
        val missing = mutableListOf<String>()
        if (!Prefs.locationGranted &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && !Prefs.notificationGranted &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!Prefs.phoneGranted &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            missing.add(Manifest.permission.READ_PHONE_STATE)
        }
        return missing
    }
}
