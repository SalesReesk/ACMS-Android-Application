package com.example.araspoint_map.asset

class Inventory {
    private val assetList = mutableListOf<Asset>()

    fun addAsset(asset: Asset) {
        assetList.add(asset)
    }

    fun removeAsset(asset: Asset) {
        assetList.remove(asset)
    }

    fun updateAsset(asset: Asset) {
        val index = assetList.indexOfFirst {
            it.assetId == asset.assetId
        }

        if (index != -1) {
            assetList[index] = asset
        }
    }

    fun findAsset(id: String): Asset? {
        return assetList.find {
            it.assetId == id }
    }

    fun getAllAsset(): List<Asset> {
        return assetList
    }

    fun viewDetails(asset: Asset) {
        println("Asset ID: ${asset.assetId}")
        println("Asset Name: ${asset.assetName}")
        println("Asset Type: ${asset.assetType}")
        println("Location: ${asset.location}")
        println("Latitude: ${asset.latitude}")
        println("Longitude: ${asset.longitude}")
        println("Status: ${asset.status}")
        println("Condition: ${asset.condition}")
        println("Photo: ${asset.photo}")
        println("Remarks: ${asset.remarks}")
    }
}