package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User

class UserRepository : BaseRepository(), IUserRepository {

    private val users = mutableListOf<User>()

    override suspend fun addUser(user: User): Result<Unit> {
        return safeCall {
            users.add(user)
        }
    }

    override suspend fun removeUser(userId: String): Result<Boolean> {
        return safeCall {
            users.removeIf { it.userId == userId }
        }
    }

    override suspend fun updateUser(updatedUser: User): Result<Boolean> {
        return safeCall {
            val index = users.indexOfFirst { it.userId == updatedUser.userId }
            if (index == -1) {
                false
            } else {
                users[index] = updatedUser
                true
            }
        }
    }

    override suspend fun findUser(userId: String): Result<User?> {
        return safeCall {
            users.find { it.userId == userId }
        }
    }

    suspend fun findUserByEmail(email: String): Result<User?> {
        return safeCall {
            users.find { it.email == email }
        }
    }

    override suspend fun getAllUsers(): Result<List<User>> {
        return safeCall {
            users.toList()
        }
    }
}
