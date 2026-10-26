package com.safeguard.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.safeguard.app.R
import com.safeguard.app.StatusHub
import com.safeguard.app.util.Prefs
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MapFragment : Fragment() {

    private var mapView: MapView? = null
    private var tvHint: TextView? = null
    private var myMarker: org.osmdroid.views.overlay.Marker? = null
    private var remoteMarker: org.osmdroid.views.overlay.Marker? = null
    private var lastMy: GeoPoint? = null
    private var lastRemote: GeoPoint? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = v.findViewById(R.id.map_view)
        tvHint = v.findViewById(R.id.tv_map_hint)
        return v
    }

    override fun onResume() {
        super.onResume()
        StatusHub.addListener(listener)
        setupMapIfNeeded()
        refresh()
    }

    override fun onPause() {
        super.onPause()
        StatusHub.removeListener(listener)
    }

    private val listener: () -> Unit = { refresh() }

    private fun setupMapIfNeeded() {
        val map = mapView ?: return
        if (map.tileSource != null) return
        Configuration.getInstance().load(requireContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()))
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(14.0)
        map.controller.setCenter(GeoPoint(45.8038, 126.5349))

        myMarker = org.osmdroid.views.overlay.Marker(map).apply {
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_me)
            setAnchor(0.5f, 0.5f)
            title = "我"
        }
        remoteMarker = org.osmdroid.views.overlay.Marker(map).apply {
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.marker_remote)
            setAnchor(0.5f, 0.5f)
            title = "对方"
        }
        map.overlays.add(myMarker)
        map.overlays.add(remoteMarker)
    }

    private fun refresh() {
        val map = mapView ?: return
        val me = StatusHub.myStatus
        val remote = StatusHub.remoteStatus

        val myPoint = me?.takeIf { it.hasLocation && (it.lat != 0.0 || it.lng != 0.0) }?.let {
            GeoPoint(it.lat, it.lng)
        }
        val remotePoint = remote?.takeIf { it.hasLocation && (it.lat != 0.0 || it.lng != 0.0) }?.let {
            GeoPoint(it.lat, it.lng)
        }

        myMarker?.apply {
            if (myPoint != null) {
                position = myPoint
                isEnabled = true
            } else {
                isEnabled = false
            }
        }
        remoteMarker?.apply {
            if (remotePoint != null) {
                position = remotePoint
                isEnabled = true
            } else {
                isEnabled = false
            }
        }

        when {
            !Prefs.isPaired -> {
                tvHint?.text = getString(R.string.map_hint_unpaired)
            }
            remotePoint == null && myPoint == null -> {
                tvHint?.text = getString(R.string.map_hint_waiting)
            }
            remotePoint != null && myPoint != null -> {
                tvHint?.text = getString(R.string.map_hint_both)
                if (lastRemote?.latitude != remotePoint.latitude || lastRemote?.longitude != remotePoint.longitude ||
                    lastMy?.latitude != myPoint.latitude || lastMy?.longitude != myPoint.longitude
                ) {
                    map.controller.animateTo(remotePoint)
                    lastRemote = remotePoint
                    lastMy = myPoint
                }
            }
            remotePoint != null -> {
                tvHint?.text = getString(R.string.map_hint_follow_remote)
                if (lastRemote?.latitude != remotePoint.latitude || lastRemote?.longitude != remotePoint.longitude) {
                    map.controller.animateTo(remotePoint)
                    lastRemote = remotePoint
                }
            }
            else -> {
                tvHint?.text = getString(R.string.map_hint_follow_me)
                if (lastMy?.latitude != myPoint?.latitude || lastMy?.longitude != myPoint?.longitude) {
                    myPoint?.let { map.controller.animateTo(it) }
                    lastMy = myPoint
                }
            }
        }
    }
}
