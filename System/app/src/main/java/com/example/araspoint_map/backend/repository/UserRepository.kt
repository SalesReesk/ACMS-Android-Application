package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User

class UserRepository {

    private val users = mutableListOf<User>()

    suspend fun addUser(user: User): Result<Unit> {
        return try {
            users.add(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeUser(userId: String): Result<Boolean> {
        return try {
            val removed = users.removeIf { it.userId == userId }
            Result.success(removed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(updatedUser: User): Result<Boolean> {
        return try {
            val index = users.indexOfFirst { it.userId == updatedUser.userId }
            if (index == -1) {
                Result.success(false)
            } else {
                users[index] = updatedUser
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUser(userId: String): Result<User?> {
        return try {
            val user = users.find { it.userId == userId }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> {
        return try {
            val user = users.find { it.email == email }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            Result.success(users.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
