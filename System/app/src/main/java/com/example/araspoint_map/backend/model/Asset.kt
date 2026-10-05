package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus

data class Asset(
    var assetId: String = generateAssetId(),
    var assetName: String = "",
    var assetType: String = "",

    var latitude: Double = 0.0,
    var longitude: Double = 0.0,

    var location: String = "",
    var status: AssetStatus = AssetStatus.AVAILABLE,
    var condition: AssetCondition = AssetCondition.GOOD,
    var photo: String = "",
    var remarks: String = ""
) {
    companion object {
        private var nextId = 1

        private fun generateAssetId(): String {
            return "AAMS-${nextId++.toString().padStart(4, '0')}"
        }
    }
}