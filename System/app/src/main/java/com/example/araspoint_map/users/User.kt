package com.example.araspoint_map.users

import com.example.araspoint_map.asset.Asset
import com.example.araspoint_map.location.DistanceCalculator

open class User {

    companion object {
        private var nextId = 1
    }
    var userId: String = "SR-${nextId++.toString().padStart(4, '0')}"
    var name: String = ""
    var email: String = ""
    var password: String = ""

    fun login(): Boolean {
        println("$name logged in.")
        return true
    }

    fun logout() {
        println("$name logged out.")
    }

    fun viewProfile() {
        println("User Id: $userId")
        println("Name: $name")
        println("Email: $email")
    }

    fun viewMap() {
        println("$name viewing map.")
    }

    fun searchAsset() {
        println("$name searching assets.")
    }

    fun calculateDistanceToAsset(
        asset: Asset,
        currentLatitude: Double,
        currentLongitude: Double
    ): Double {
        val distance = DistanceCalculator().calculateDistance(
            currentLatitude,
            currentLongitude,
            asset.latitude,
            asset.longitude
        )

        println("$name is ${"%.2f".format(distance)} km away from ${asset.assetName}.")
        return distance
    }
}
