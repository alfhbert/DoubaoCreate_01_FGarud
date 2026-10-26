package com.safeguard.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.safeguard.app.AntiFraudActivity
import com.safeguard.app.MainActivity
import com.safeguard.app.PairActivity
import com.safeguard.app.R
import com.safeguard.app.SafeguardService
import com.safeguard.app.StatusHub
import com.safeguard.app.databinding.FragmentHomeBinding
import com.safeguard.app.util.FontScaler
import com.safeguard.app.util.LocationUtils
import com.safeguard.app.util.Prefs

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val refreshListener: () -> Unit = { refresh() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnPair.setOnClickListener {
            startActivity(Intent(requireContext(), PairActivity::class.java))
        }
        binding.btnUnpair.setOnClickListener {
            Prefs.pairCode = ""
            StatusHub.remoteStatus = null
            StatusHub.connected = false
            refresh()
        }
        binding.btnSos.setOnClickListener {
            SafeguardService.sendSos(requireContext())
        }
        binding.btnMap.setOnClickListener { openTab(R.id.nav_map) }
        binding.btnEvents.setOnClickListener { openTab(R.id.nav_events) }
        binding.btnAntiFraud.setOnClickListener {
            startActivity(Intent(requireContext(), AntiFraudActivity::class.java))
        }
        binding.switchService.setOnCheckedChangeListener { _, checked ->
            Prefs.serviceEnabled = checked
            if (checked) SafeguardService.start(requireContext())
            else SafeguardService.stop(requireContext())
        }

        FontScaler.apply(binding.root, if (Prefs.largeFont) 1f else 0.82f)
    }

    override fun onStart() {
        super.onStart()
        StatusHub.addListener(refreshListener)
        refresh()
    }

    override fun onStop() {
        StatusHub.removeListener(refreshListener)
        super.onStop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun openTab(id: Int) {
        val act = activity as? MainActivity ?: return
        act.findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = id
    }

    private fun refresh() {
        binding.switchService.isChecked = Prefs.serviceEnabled

        binding.textServiceState.text = when {
            !Prefs.isPaired -> getString(R.string.service_state_unpaired)
            !Prefs.serviceEnabled -> getString(R.string.service_state_stopped)
            StatusHub.connected -> getString(R.string.service_state)
            else -> getString(R.string.service_state_connecting)
        }

        if (Prefs.isPaired) {
            binding.textPairState.text = getString(R.string.pair_state_ok, Prefs.pairCode)
            binding.btnPair.setText(R.string.btn_pair_manage)
            binding.btnUnpair.visibility = View.VISIBLE
        } else {
            binding.textPairState.text = getString(R.string.pair_state_none)
            binding.btnPair.setText(R.string.btn_pair_go)
            binding.btnUnpair.visibility = View.GONE
        }

        // 我的设备
        val my = StatusHub.myStatus
        if (my != null) {
            binding.progressBattery.progress = my.battery.coerceIn(0, 100)
            binding.textBattery.text = if (my.battery >= 0) {
                getString(R.string.battery_value, my.battery)
            } else {
                getString(R.string.battery_unknown)
            }
            binding.textMyCharge.text = when {
                my.battery < 0 -> getString(R.string.charging_unknown)
                my.charging -> getString(R.string.charging_yes)
                else -> getString(R.string.charging_no)
            }
        } else {
            binding.progressBattery.progress = 0
            binding.textBattery.text = getString(R.string.battery_unknown)
            binding.textMyCharge.text = getString(R.string.charging_unknown)
        }
        binding.textMyCall.text = callText(my?.callState)
        binding.textMyLocation.text = if (my?.hasLocation == true) {
            getString(R.string.loc_value, my.lat, my.lng, my.accuracy.toInt())
        } else {
            getString(R.string.loc_none)
        }

        // 对方设备
        val remote = StatusHub.remoteStatus
        if (remote != null) {
            binding.textRemoteName.text = remote.deviceName
            binding.textRemoteBattery.text = if (remote.battery >= 0) {
                getString(R.string.battery_value, remote.battery)
            } else {
                getString(R.string.battery_unknown)
            }
            binding.textRemoteCall.text = callText(remote.callState)
            binding.textRemoteLocation.text = if (remote.hasLocation) {
                getString(R.string.loc_value, remote.lat, remote.lng, remote.accuracy.toInt())
            } else {
                getString(R.string.loc_none)
            }

            if (remote.hasLocation && my?.hasLocation == true) {
                val d = LocationUtils.distanceMeters(my.lat, my.lng, remote.lat, remote.lng)
                binding.textRemoteDistance.text = getString(
                    R.string.remote_distance, LocationUtils.formatDistance(d)
                )
                binding.textRemoteDistance.visibility = View.VISIBLE
            } else {
                binding.textRemoteDistance.visibility = View.GONE
            }

            val stale = System.currentTimeMillis() - remote.ts > 3 * 60 * 1000L
            if (stale) {
                binding.textRemoteOffline.text = getString(
                    R.string.remote_offline, LocationUtils.formatTime(remote.ts)
                )
                binding.textRemoteOffline.visibility = View.VISIBLE
            } else {
                binding.textRemoteOffline.visibility = View.GONE
            }

            if (remote.scamRisk > 0) {
                binding.badgeScam.text = getString(R.string.scam_badge, remote.scamReason)
                binding.badgeScam.visibility = View.VISIBLE
            } else {
                binding.badgeScam.visibility = View.GONE
            }
        } else {
            binding.textRemoteName.text = getString(R.string.card_remote)
            binding.textRemoteBattery.text = getString(R.string.battery_unknown)
            binding.textRemoteCall.text = getString(R.string.call_unknown)
            binding.textRemoteLocation.text = getString(R.string.loc_none)
            binding.textRemoteDistance.visibility = View.GONE
            binding.textRemoteOffline.text = getString(R.string.remote_unknown)
            binding.textRemoteOffline.visibility = View.VISIBLE
            binding.badgeScam.visibility = View.GONE
        }
    }

    private fun callText(state: String?): String = when (state) {
        "ringing" -> getString(R.string.call_ringing)
        "offhook" -> getString(R.string.call_offhook)
        "idle" -> getString(R.string.call_idle)
        else -> getString(R.string.call_unknown)
    }
}
