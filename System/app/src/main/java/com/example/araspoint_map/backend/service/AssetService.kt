package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.repository.AssetRepository
import com.example.araspoint_map.backend.repository.IAssetRepository

class AssetService(
    private val assetRepository: IAssetRepository = AssetRepository()
) : IAssetService {

    override suspend fun createAsset(asset: Asset) = assetRepository.addAsset(asset)

    override suspend fun readAllAssets() = assetRepository.getAssets()

    override suspend fun readAssetById(assetId: String) = assetRepository.findAsset(assetId)

    override suspend fun updateAsset(asset: Asset) = assetRepository.updateAsset(asset)

    override suspend fun deleteAsset(assetId: String) = assetRepository.removeAsset(assetId)
}
