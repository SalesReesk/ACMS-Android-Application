package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.navigation.GeoNode

data class Building(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val centerLat: Double = 0.0,
    val centerLng: Double = 0.0,
    val description: String = "",
    val department: String = "",
    val boundaryPoints: List<Pair<Double, Double>> = emptyList()
) {
    fun toGeoNode(): GeoNode {
        return GeoNode(
            id = id,
            name = name,
            lat = centerLat,
            lng = centerLng
        )
    }
}
