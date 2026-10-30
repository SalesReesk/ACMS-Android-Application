package com.example.araspoint_map

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.cloudinary.android.MediaManager
import org.osmdroid.config.Configuration
import java.io.File

class ArasPointApp : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        try {
            val config = HashMap<String, String>()
            config["cloud_name"] = "djyggmhmk"
            MediaManager.init(this, config)
        } catch (_: Exception) {}

        try {
            val ctx = applicationContext
            val osmConfig = Configuration.getInstance()
            osmConfig.userAgentValue = "ArasPoint-BatStateU-CampusMap/1.0 (batstate-u.edu.ph; com.example.araspoint_map)"
            val basePath = File(cacheDir, "osmdroid")
            val tileCache = File(basePath, "tiles")
            osmConfig.osmdroidBasePath = basePath
            osmConfig.osmdroidTileCache = tileCache
            osmConfig.tileFileSystemCacheMaxBytes = 100L * 1024 * 1024
            osmConfig.tileFileSystemCacheTrimBytes = 80L * 1024 * 1024
            osmConfig.load(ctx, ctx.getSharedPreferences("osmdroid_official_prefs", MODE_PRIVATE))
        } catch (_: Exception) {}
    }
}
