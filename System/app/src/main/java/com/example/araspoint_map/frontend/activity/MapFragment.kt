package com.example.araspoint_map.frontend.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.example.araspoint_map.R
import com.example.araspoint_map.databinding.FragmentMapBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val campusCenter = GeoPoint(14.066730, 120.626382)
    private var myLocationOverlay: MyLocationNewOverlay? = null

    companion object {
        val OSM_DE_TILES = XYTileSource(
            "OSM_DE",
            0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.de/")
        )
    }

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                enableUserLocationOnMap()
            } else {
                Toast.makeText(context, "Location permission denied. Map showing campus view.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOpenStreetMap()
        checkAndRequestLocationPermissions()
    }

    private fun checkAndRequestLocationPermissions() {
        val finePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            enableUserLocationOnMap()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableUserLocationOnMap() {
        if (_binding == null) return
        val mapView = binding.osmMapView

        if (myLocationOverlay == null) {
            myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView).apply {
                enableMyLocation()
            }
            mapView.overlays.add(myLocationOverlay)
        } else {
            myLocationOverlay?.enableMyLocation()
        }
        mapView.invalidate()
    }

    private fun setupOpenStreetMap() {
        val ctx = requireContext().applicationContext
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid_cache", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = "ArasPoint-BatStateU-CampusMap/1.0 (batstate-u.edu.ph; com.example.araspoint_map)"

        val mapView = binding.osmMapView
        mapView.setTileSource(OSM_DE_TILES)
        mapView.setMultiTouchControls(true)

        mapView.minZoomLevel = 17.0
        mapView.maxZoomLevel = 20.0

        val campusBoundingBox = BoundingBox(
            14.068500,
            120.628500,
            14.065000,
            120.624500
        )
        mapView.setScrollableAreaLimitDouble(campusBoundingBox)

        val controller = mapView.controller
        controller.setZoom(19.0)
        controller.setCenter(campusCenter)

        val campusPolygon = Polygon().apply {
            points = listOf(
                GeoPoint(14.067702, 120.625403),
                GeoPoint(14.067681, 120.627580),
                GeoPoint(14.065762, 120.627039),
                GeoPoint(14.065758, 120.625505),
                GeoPoint(14.067702, 120.625403)
            )
            fillColor = Color.argb(35, 124, 58, 237)
            strokeColor = Color.parseColor("#7C3AED")
            strokeWidth = 6.0f
            title = "BatStateU ARASOF Nasugbu Campus Boundary"
        }
        mapView.overlays.add(campusPolygon)

        addCampusBuildingMarker(
            geoPoint = GeoPoint(14.067400, 120.625700),
            title = "ADMIN HALL",
            description = "Executive, Registrar & Property Audit Office\nStatus: Operational",
            colorResId = R.color.primary
        )

        addCampusBuildingMarker(
            geoPoint = GeoPoint(14.067200, 120.627200),
            title = "CICS COMPLEX",
            description = "College of Informatics & Computing Sciences (Labs 1–6)\nStatus: Audit In Progress",
            colorResId = R.color.status_available
        )

        addCampusBuildingMarker(
            geoPoint = GeoPoint(14.067218, 120.626715),
            title = "HEB Building",
            description = "Higher Education Building",
            colorResId = R.color.secondary
        )

        addCampusBuildingMarker(
            geoPoint = GeoPoint(14.066000, 120.626800),
            title = "CSB / CET BUILDING",
            description = "College of Engineering & Technology Mechatronics Labs\nStatus: Operational",
            colorResId = R.color.status_maintenance
        )

        addCampusBuildingMarker(
            geoPoint = GeoPoint(14.07000, 120.625600),
            title = "UNIVERSITY GYMNASIUM",
            description = "Grandstand & Athletics Equipment Room\nStatus: Operational",
            colorResId = R.color.outline
        )

        binding.btnRecenterCampus.setOnClickListener {
            val userLocation = myLocationOverlay?.myLocation
            if (userLocation != null && campusBoundingBox.contains(userLocation)) {
                controller.animateTo(userLocation)
                controller.setZoom(19.0)
                Toast.makeText(context, "Centered on your live location", Toast.LENGTH_SHORT).show()
            } else {
                controller.animateTo(campusCenter)
                controller.setZoom(19.0)
                Toast.makeText(context, "Centered on ARASOF Campus", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnZoomIn.setOnClickListener {
            controller.zoomIn()
        }

        binding.btnZoomOut.setOnClickListener {
            controller.zoomOut()
        }
    }

    private fun addCampusBuildingMarker(geoPoint: GeoPoint, title: String, description: String, colorResId: Int) {
        val marker = Marker(binding.osmMapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            snippet = description

            val pinDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.map_pin)?.mutate()
            if (pinDrawable != null) {
                DrawableCompat.setTint(
                    pinDrawable,
                    ContextCompat.getColor(requireContext(), colorResId)
                )
                icon = pinDrawable
            }

            setOnMarkerClickListener { m, _ ->
                m.showInfoWindow()
                val sheet = BuildingAssetsBottomSheetDialogFragment(m.title ?: "Building", m.snippet ?: "Campus Facility")
                sheet.show(parentFragmentManager, "BuildingAssetsSheet")
                true
            }
        }
        binding.osmMapView.overlays.add(marker)
    }

    override fun onResume() {
        super.onResume()
        binding.osmMapView.onResume()
        myLocationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.osmMapView.onPause()
        myLocationOverlay?.disableMyLocation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
