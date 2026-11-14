package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.User

interface IAssetService {
    suspend fun createAsset(asset: Asset): Result<Unit>
    suspend fun readAllAssets(): Result<List<Asset>>
    suspend fun readAssetById(assetId: String): Result<Asset?>
    suspend fun updateAsset(asset: Asset): Result<Unit>
    suspend fun deleteAsset(assetId: String): Result<Unit>
}

interface IMaintenanceService {
    suspend fun addReport(record: MaintenanceRecord): Result<Unit>
    suspend fun updateReport(record: MaintenanceRecord): Result<Unit>
    suspend fun deleteReport(reportId: String): Result<Unit>
    suspend fun getAllReports(): Result<List<MaintenanceRecord>>
}

interface IUserService {
    suspend fun addUser(user: User): Result<Unit>
    suspend fun getAllUsers(): Result<List<User>>
}
