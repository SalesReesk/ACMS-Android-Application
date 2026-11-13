package com.example.araspoint_map.frontend.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.araspoint_map.backend.model.Building
import com.example.araspoint_map.databinding.ActivityMapBinding

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var locationManager: LocationManager? = null

    private val campusBuildings = listOf(
        Building("HEB", "HEB Building", "HEB", 14.067322, 120.626777, "Higher Education Building", "General Academics"),
        Building("CTEB", "CTEB Building", "CTEB", 14.066589, 120.626364, "College of Teacher Education Building", "CTE"),
        Building("GYM", "JOSON GYMNASIUM", "GYM", 14.067103, 120.626169, "University Athletics & Event Gym", "Sports"),
        Building("TAAL", "TAAL Building", "TAAL", 14.067332, 120.625627, "Academic Building Taal", "General"),
        Building("LEMERY", "LEMERY Building", "LEMERY", 14.065859, 120.626151, "Academic Building Lemery", "General"),
        Building("IBAAN", "IBAAN Building", "IBAAN", 14.067655, 120.625926, "Academic Building Ibaan", "General"),
        Building("CAS", "CAS BUILDING", "CAS", 14.066022, 120.626146, "College of Arts and Sciences", "CAS"),
        Building("HOSTEL", "NASUGBU HOSTEL", "HOSTEL", 14.066962, 120.627023, "Campus Guest Hostel & Lodging", "Services"),
        Building("IMT", "IMT Building", "IMT", 14.066387, 120.625581, "Industrial Materials & Technology", "CIT"),
        Building("LIAN", "LIAN Cafeteria", "LIAN", 14.066721, 120.626725, "Campus Student Cafeteria", "Services"),
        Building("ROSARIO", "ROSARIO Canteen", "ROSARIO", 14.067319, 120.625961, "Student Refreshment Canteen", "Services"),
        Building("INFIRMARY", "INFIRMARY", "INFIRMARY", 14.067080, 120.625864, "Campus Health Services & Clinic", "CHS"),
        Building("APACIBLE", "APACIBLE MUSEUM", "APACIBLE", 14.067366, 120.626187, "Historical Museum & Exhibits", "Culture"),
        Building("ERMITA", "ERMITA BUILDING", "ERMITA", 14.067032, 120.625615, "Ermita Academic Building", "General")
    )

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission denied. Map showing campus view.", Toast.LENGTH_SHORT).show()
            }
        }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onBuildingClicked(buildingId: String) {
            runOnUiThread {
                val b = campusBuildings.find { it.id == buildingId }
                val title = b?.name ?: buildingId
                val subtitle = if (b != null) "${b.description} • ${b.department}" else "Campus Facility"
                val sheet = BuildingAssetsBottomSheetDialogFragment(
                    buildingTitle = title,
                    buildingSubtitle = subtitle
                )
                sheet.show(supportFragmentManager, "BuildingAssetsSheet")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnBackMap.setOnClickListener {
            finish()
        }

        setupLeafletWebView()
        checkAndRequestLocationPermissions()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupLeafletWebView() {
        binding.mapWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        binding.mapWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        binding.mapWebView.addJavascriptInterface(WebAppInterface(), "AndroidBridge")
        binding.mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                for (b in campusBuildings) {
                    val safeName = b.name.replace("'", "\\'")
                    binding.mapWebView.evaluateJavascript(
                        "addBuildingMarker('${b.id}', '$safeName', ${b.centerLat}, ${b.centerLng})",
                        null
                    )
                }
            }
        }
        binding.mapWebView.loadUrl("file:///android_asset/campus_map.html")

        binding.btnRecenterCampus.setOnClickListener {
            binding.mapWebView.evaluateJavascript("recenterCampus()", null)
            Toast.makeText(this, "Centered on campus", Toast.LENGTH_SHORT).show()
        }

        binding.btnZoomIn.setOnClickListener {
            binding.mapWebView.evaluateJavascript("zoomIn()", null)
        }

        binding.btnZoomOut.setOnClickListener {
            binding.mapWebView.evaluateJavascript("zoomOut()", null)
        }
    }

    private fun checkAndRequestLocationPermissions() {
        val finePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePermission == PackageManager.PERMISSION_GRANTED || coarsePermission == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    binding.mapWebView.evaluateJavascript(
                        "updateUserMarker(${location.latitude}, ${location.longitude})",
                        null
                    )
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, locationListener)
            }
            if (locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true) {
                locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, locationListener)
            }

            val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnown != null) {
                binding.mapWebView.evaluateJavascript(
                    "updateUserMarker(${lastKnown.latitude}, ${lastKnown.longitude})",
                    null
                )
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager = null
    }
}
