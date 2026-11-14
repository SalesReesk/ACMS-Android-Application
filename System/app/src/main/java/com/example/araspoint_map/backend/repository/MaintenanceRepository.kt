package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MaintenanceRepository : BaseRepository(), IMaintenanceRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val recordsCollection = firestore.collection("maintenance_reports")

    override suspend fun getReports(): Result<List<MaintenanceRecord>> {
        return safeCall {
            val snapshot = recordsCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(MaintenanceRecord::class.java) }
        }
    }

    override suspend fun addReport(record: MaintenanceRecord): Result<Unit> {
        return safeCall {
            recordsCollection.document(record.reportId).set(record).await()
        }
    }

    override suspend fun updateReport(record: MaintenanceRecord): Result<Unit> {
        return safeCall {
            recordsCollection.document(record.reportId).set(record).await()
        }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return safeCall {
            recordsCollection.document(reportId).delete().await()
        }
    }
}
