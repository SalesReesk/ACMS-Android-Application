package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.MaintenanceRecord

class MaintenanceRepository {

    private val maintenanceRecords = mutableListOf<MaintenanceRecord>()

    fun addMaintenanceRecord(record: MaintenanceRecord) {
        maintenanceRecords.add(record)
    }

    fun removeMaintenanceRecord(reportId: Int): Boolean {
        return maintenanceRecords.removeIf {
            it.reportId == reportId
        }
    }

    fun updateMaintenanceRecord(
        updatedRecord: MaintenanceRecord
    ): Boolean {

        val index = maintenanceRecords.indexOfFirst {
            it.reportId == updatedRecord.reportId
        }

        if (index == -1) {
            return false
        }

        maintenanceRecords[index] = updatedRecord
        return true
    }

    fun findMaintenanceRecord(reportId: Int): MaintenanceRecord? {
        return maintenanceRecords.find {
            it.reportId == reportId
        }
    }

    fun getMaintenanceRecordsByAsset(
        assetId: String
    ): List<MaintenanceRecord> {

        return maintenanceRecords.filter {
            it.asset?.assetId == assetId
        }
    }

    fun getAllMaintenanceRecords(): List<MaintenanceRecord> {
        return maintenanceRecords.toList()
    }

    fun clearMaintenanceRecords() {
        maintenanceRecords.clear()
    }
}