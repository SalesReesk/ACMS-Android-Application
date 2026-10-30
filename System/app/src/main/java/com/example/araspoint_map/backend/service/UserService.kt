package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.repository.UserRepository

class UserService(
    private val userRepository: UserRepository = UserRepository()
) {

    suspend fun addUser(user: User) = userRepository.addUser(user)

    suspend fun removeUser(userId: String) = userRepository.removeUser(userId)

    suspend fun updateUser(user: User) = userRepository.updateUser(user)

    suspend fun findUser(userId: String) = userRepository.findUser(userId)

    suspend fun findUserByEmail(email: String) = userRepository.findUserByEmail(email)

    suspend fun getAllUsers() = userRepository.getAllUsers()
}
