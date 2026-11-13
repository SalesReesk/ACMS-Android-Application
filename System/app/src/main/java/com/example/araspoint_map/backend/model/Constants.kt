package com.example.araspoint_map.backend.model

object Constants {
    val BUILDINGS = arrayOf(
        "HEB", "CTEB", "GYM", "TAAL", "LEMERY", "IBAAN",
        "CAS", "HOSTEL", "IMT", "LIAN", "ROSARIO", "INFIRMARY", "APACIBLE", "ERMITA"
    )
    val FILTER_BUILDINGS = arrayOf("All Buildings") + BUILDINGS

    val ASSET_CATEGORIES = arrayOf(
        "IT Equipment",
        "Electronics & AV",
        "Laboratory & Scientific",
        "Furniture",
        "Appliances",
        "Facility & Infrastructure",
        "Safety & Security",
        "Sports & Recreation"
    )

    val BUILDING_FLOORS_MAP = mapOf(
        "HEB" to listOf("1", "2", "3"),
        "CTEB" to listOf("1", "2", "3", "4"),
        "LEMERY" to listOf("1", "2"),
        "CAS" to listOf("1", "2"),
        "TAAL" to listOf("1", "2", "3", "4"),
        "APACIBLE" to listOf("1", "2"),
        "HOSTEL" to listOf("1", "2", "3", "4"),
        "IMT" to listOf("1", "2"),
        "ERMITA" to listOf("1"),
        "INFIRMARY" to listOf("1"),
        "IBAAN" to listOf("1", "2"),
        "LIAN" to listOf("1"),
        "ROSARIO" to listOf("1"),
        "GYM" to listOf("1")
    )
}
