package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.Asset

class AssetRepository {

    private val assets = mutableListOf<Asset>()

    fun addAsset(asset: Asset) {
        assets.add(asset)
    }

    fun removeAsset(assetId: String): Boolean {
        return assets.removeIf { it.assetId == assetId }
    }

    fun updateAsset(updatedAsset: Asset): Boolean {
        val index = assets.indexOfFirst {
            it.assetId == updatedAsset.assetId
        }

        if (index == -1) {
            return false
        }

        assets[index] = updatedAsset
        return true
    }

    fun findAsset(assetId: String): Asset? {
        return assets.find {
            it.assetId == assetId
        }
    }

    fun getAllAssets(): List<Asset> {
        return assets.toList()
    }

    fun clearAssets() {
        assets.clear()
    }
}