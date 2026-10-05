package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User

class UserRepository {

    private val users = mutableListOf<User>()

    fun addUser(user: User) {
        users.add(user)
    }

    fun removeUser(userId: String): Boolean {
        return users.removeIf {
            it.userId == userId
        }
    }

    fun updateUser(updatedUser: User): Boolean {
        val index = users.indexOfFirst {
            it.userId == updatedUser.userId
        }

        if (index == -1) {
            return false
        }

        users[index] = updatedUser
        return true
    }

    fun findUser(userId: String): User? {
        return users.find {
            it.userId == userId
        }
    }

    fun findUserByEmail(email: String): User? {
        return users.find {
            it.email == email
        }
    }

    fun getAllUsers(): List<User> {
        return users.toList()
    }

    fun clearUsers() {
        users.clear()
    }
}