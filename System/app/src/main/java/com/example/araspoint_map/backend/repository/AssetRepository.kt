package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.Asset
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AssetRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val assetsCollection = firestore.collection("assets")

    suspend fun getAssets(): Result<List<Asset>> {
        return try {
            val snapshot = assetsCollection.get().await()
            val assetsList = snapshot.documents.mapNotNull { it.toObject(Asset::class.java) }
            Result.success(assetsList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findAsset(assetId: String): Result<Asset?> {
        return try {
            val document = assetsCollection.document(assetId).get().await()
            val asset = document.toObject(Asset::class.java)
            Result.success(asset)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addAsset(asset: Asset): Result<Unit> {
        return try {
            assetsCollection.document(asset.assetId).set(asset).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAsset(asset: Asset): Result<Unit> {
        return try {
            assetsCollection.document(asset.assetId).set(asset).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeAsset(assetId: String): Result<Unit> {
        return try {
            assetsCollection.document(assetId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
