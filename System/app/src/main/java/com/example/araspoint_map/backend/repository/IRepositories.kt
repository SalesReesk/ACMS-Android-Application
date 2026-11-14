package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole

interface IAssetRepository {
    suspend fun getAssets(): Result<List<Asset>>
    suspend fun findAsset(assetId: String): Result<Asset?>
    suspend fun addAsset(asset: Asset): Result<Unit>
    suspend fun updateAsset(asset: Asset): Result<Unit>
    suspend fun removeAsset(assetId: String): Result<Unit>
}

interface IUserRepository {
    suspend fun addUser(user: User): Result<Unit>
    suspend fun removeUser(userId: String): Result<Boolean>
    suspend fun updateUser(updatedUser: User): Result<Boolean>
    suspend fun findUser(userId: String): Result<User?>
    suspend fun getAllUsers(): Result<List<User>>
}

interface IMaintenanceRepository {
    suspend fun getReports(): Result<List<MaintenanceRecord>>
    suspend fun addReport(record: MaintenanceRecord): Result<Unit>
    suspend fun updateReport(record: MaintenanceRecord): Result<Unit>
    suspend fun deleteReport(reportId: String): Result<Unit>
}

interface IAuthRepository {
    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        idNumber: String = ""
    ): Result<User>
    suspend fun login(email: String, password: String): Result<User>
    fun logout()
    suspend fun getCurrentUser(): User?
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUser(): Boolean
    suspend fun getUserRole(): UserRole?
    suspend fun getFullUser(): User?
    suspend fun updateUserProfile(name: String): Result<Unit>
    suspend fun getMaintenanceUsers(): List<User>
    suspend fun getAllUsers(): List<User>
    suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
}
