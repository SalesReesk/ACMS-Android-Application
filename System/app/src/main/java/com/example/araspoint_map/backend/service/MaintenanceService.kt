package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import com.example.araspoint_map.backend.repository.MaintenanceRepository

class MaintenanceService(
    private val maintenanceRepository: MaintenanceRepository = MaintenanceRepository()
) {

    suspend fun addReport(record: MaintenanceRecord) = maintenanceRepository.addReport(record)

    suspend fun updateReport(record: MaintenanceRecord) = maintenanceRepository.updateReport(record)

    suspend fun deleteReport(reportId: String) = maintenanceRepository.deleteReport(reportId)

    suspend fun getAllReports() = maintenanceRepository.getReports()
}
