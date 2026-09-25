package location

import kotlin.math.*

class DistanceCalculator {

    fun calculateDistance(
        userLatitude: Double,
        userLongitude: Double,
        assetLatitude: Double,
        assetLongitude: Double
    ): Double {

        val earthRadius = 6371.0 // Earth radius in km

        val lat1 = Math.toRadians(userLatitude)
        val lat2 = Math.toRadians(assetLatitude)

        val deltaLat = Math.toRadians(assetLatitude - userLatitude)
        val deltaLon = Math.toRadians(assetLongitude - userLongitude)

        val a = sin(deltaLat / 2).pow(2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }
}