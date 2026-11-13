package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.repository.UserRepository

class UserService(
    private val userRepository: UserRepository = UserRepository()
) {

    suspend fun addUser(user: User) = userRepository.addUser(user)

    suspend fun getAllUsers() = userRepository.getAllUsers()
}
