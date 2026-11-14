package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.Asset
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AssetRepository : BaseRepository(), IAssetRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val assetsCollection = firestore.collection("assets")

    override suspend fun getAssets(): Result<List<Asset>> {
        return safeCall {
            val snapshot = assetsCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(Asset::class.java) }
        }
    }

    override suspend fun findAsset(assetId: String): Result<Asset?> {
        return safeCall {
            val document = assetsCollection.document(assetId).get().await()
            document.toObject(Asset::class.java)
        }
    }

    override suspend fun addAsset(asset: Asset): Result<Unit> {
        return safeCall {
            assetsCollection.document(asset.assetId).set(asset).await()
        }
    }

    override suspend fun updateAsset(asset: Asset): Result<Unit> {
        return safeCall {
            assetsCollection.document(asset.assetId).set(asset).await()
        }
    }

    override suspend fun removeAsset(assetId: String): Result<Unit> {
        return safeCall {
            assetsCollection.document(assetId).delete().await()
        }
    }
}
