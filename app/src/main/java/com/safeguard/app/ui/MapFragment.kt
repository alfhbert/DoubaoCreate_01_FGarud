package com.safeguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.safeguard.app.R
import com.safeguard.app.StatusHub
import com.safeguard.app.databinding.FragmentMapBinding
import com.safeguard.app.util.LocationUtils
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var myMarker: Marker? = null
    private var remoteMarker: Marker? = null
    private var follow = true

    private val refreshListener: () -> Unit = { refreshMarkers() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(12.0)
        binding.btnFollow.setOnClickListener {
            follow = !follow
            binding.btnFollow.setText(if (follow) R.string.map_follow else R.string.map_unfollow)
            if (follow) centerOnRemote()
        }
        refreshMarkers()
    }

    override fun onStart() {
        super.onStart()
        binding.map.onResume()
        StatusHub.addListener(refreshListener)
        refreshMarkers()
    }

    override fun onStop() {
        StatusHub.removeListener(refreshListener)
        binding.map.onPause()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.map.onDetach()
        _binding = null
        super.onDestroyView()
    }

    private fun refreshMarkers() {
        val map = binding.map
        val my = StatusHub.myStatus
        val remote = StatusHub.remoteStatus
        val context = requireContext()

        if (my?.hasLocation == true) {
            if (myMarker == null) {
                myMarker = Marker(map).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(context, R.drawable.marker_me)
                    title = getString(R.string.map_me)
                }
                map.overlays.add(myMarker)
            }
            myMarker?.position = GeoPoint(my.lat, my.lng)
            myMarker?.infoWindow = null
        } else {
            myMarker?.let { map.overlays.remove(it) }
            myMarker = null
        }

        if (remote?.hasLocation == true) {
            if (remoteMarker == null) {
                remoteMarker = Marker(map).apply {
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(context, R.drawable.marker_remote)
                }
                map.overlays.add(remoteMarker)
            }
            remoteMarker?.position = GeoPoint(remote.lat, remote.lng)
            remoteMarker?.title = remote.deviceName
            remoteMarker?.infoWindow = null
            binding.textMapStatus.text =
                "${remote.deviceName} · ${LocationUtils.formatTime(remote.ts)}"
            if (follow) centerOnRemote()
        } else {
            remoteMarker?.let { map.overlays.remove(it) }
            remoteMarker = null
            binding.textMapStatus.text = getString(R.string.remote_unknown)
            if (my?.hasLocation == true) {
                map.controller.animateTo(GeoPoint(my.lat, my.lng), 15.0, 500L)
            }
        }
        map.invalidate()
    }

    private fun centerOnRemote() {
        val remote = StatusHub.remoteStatus
        if (remote?.hasLocation == true) {
            binding.map.controller.animateTo(GeoPoint(remote.lat, remote.lng), 15.0, 500L)
        }
    }
}
