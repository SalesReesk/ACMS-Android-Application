package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MaintenanceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val recordsCollection = firestore.collection("maintenance_reports")

    suspend fun getReports(): Result<List<MaintenanceRecord>> {
        return try {
            val snapshot = recordsCollection.get().await()
            val list = snapshot.documents.mapNotNull { it.toObject(MaintenanceRecord::class.java) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReport(record: MaintenanceRecord): Result<Unit> {
        return try {
            recordsCollection.document(record.reportId).set(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReport(record: MaintenanceRecord): Result<Unit> {
        return try {
            recordsCollection.document(record.reportId).set(record).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            recordsCollection.document(reportId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
