package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.repository.UserRepository

class UserService(
    private val userRepository: UserRepository
) {

    fun addUser(user: User) {
        userRepository.addUser(user)
    }

    fun removeUser(userId: String): Boolean {
        return userRepository.removeUser(userId)
    }

    fun updateUser(user: User): Boolean {
        return userRepository.updateUser(user)
    }

    fun findUser(userId: String): User? {
        return userRepository.findUser(userId)
    }

    fun findUserByEmail(email: String): User? {
        return userRepository.findUserByEmail(email)
    }

    fun getAllUsers(): List<User> {
        return userRepository.getAllUsers()
    }
}