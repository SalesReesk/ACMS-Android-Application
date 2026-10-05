package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import com.example.araspoint_map.backend.repository.MaintenanceRepository

class MaintenanceService(
    private val maintenanceRepository: MaintenanceRepository
) {

    fun addMaintenanceRecord(record: MaintenanceRecord) {
        maintenanceRepository.addMaintenanceRecord(record)
    }

    fun removeMaintenanceRecord(reportId: Int): Boolean {
        return maintenanceRepository.removeMaintenanceRecord(reportId)
    }

    fun updateMaintenanceRecord(record: MaintenanceRecord): Boolean {
        return maintenanceRepository.updateMaintenanceRecord(record)
    }

    fun findMaintenanceRecord(reportId: Int): MaintenanceRecord? {
        return maintenanceRepository.findMaintenanceRecord(reportId)
    }

    fun getAllMaintenanceRecords(): List<MaintenanceRecord> {
        return maintenanceRepository.getAllMaintenanceRecords()
    }

    fun getMaintenanceRecordsByAsset(
        assetId: String
    ): List<MaintenanceRecord> {
        return maintenanceRepository.getMaintenanceRecordsByAsset(assetId)
    }

    fun getRecordsByStatus(
        status: ReportStatus
    ): List<MaintenanceRecord> {
        return maintenanceRepository.getAllMaintenanceRecords().filter {
            it.status == status
        }
    }

    fun getRecordsByPriority(
        priority: PriorityStatus
    ): List<MaintenanceRecord> {
        return maintenanceRepository.getAllMaintenanceRecords().filter {
            it.priority == priority
        }
    }
}