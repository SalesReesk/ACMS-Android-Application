package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.enums.AssetCondition
import com.example.araspoint_map.backend.model.enums.AssetStatus
import com.example.araspoint_map.backend.repository.AssetRepository

class AssetService(
    private val assetRepository: AssetRepository
) {

    fun addAsset(asset: Asset) {
        assetRepository.addAsset(asset)
    }

    fun removeAsset(assetId: String): Boolean {
        return assetRepository.removeAsset(assetId)
    }

    fun updateAsset(asset: Asset): Boolean {
        return assetRepository.updateAsset(asset)
    }

    fun findAsset(assetId: String): Asset? {
        return assetRepository.findAsset(assetId)
    }

    fun getAllAssets(): List<Asset> {
        return assetRepository.getAllAssets()
    }

    fun getAssetsByStatus(status: AssetStatus): List<Asset> {
        return assetRepository.getAllAssets().filter {
            it.status == status
        }
    }

    fun getAssetsByCondition(condition: AssetCondition): List<Asset> {
        return assetRepository.getAllAssets().filter {
            it.condition == condition
        }
    }
}