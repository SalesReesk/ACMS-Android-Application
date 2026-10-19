package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.repository.AssetRepository

class AssetService(
    private val assetRepository: AssetRepository = AssetRepository()
) {

    suspend fun createAsset(asset: Asset) = assetRepository.addAsset(asset)

    suspend fun readAllAssets() = assetRepository.getAssets()

    suspend fun readAssetById(assetId: String) = assetRepository.findAsset(assetId)

    suspend fun updateAsset(asset: Asset) = assetRepository.updateAsset(asset)

    suspend fun deleteAsset(assetId: String) = assetRepository.removeAsset(assetId)
}
