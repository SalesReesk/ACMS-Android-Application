package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.repository.IMaintenanceRepository
import com.example.araspoint_map.backend.repository.MaintenanceRepository

class MaintenanceService(
    private val maintenanceRepository: IMaintenanceRepository = MaintenanceRepository()
) : IMaintenanceService {

    override suspend fun addReport(record: MaintenanceRecord) = maintenanceRepository.addReport(record)

    override suspend fun updateReport(record: MaintenanceRecord) = maintenanceRepository.updateReport(record)

    override suspend fun deleteReport(reportId: String) = maintenanceRepository.deleteReport(reportId)

    override suspend fun getAllReports() = maintenanceRepository.getReports()
}
