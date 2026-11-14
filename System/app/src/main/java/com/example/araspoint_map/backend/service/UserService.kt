package com.example.araspoint_map.backend.service

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.repository.IUserRepository
import com.example.araspoint_map.backend.repository.UserRepository

class UserService(
    private val userRepository: IUserRepository = UserRepository()
) : IUserService {

    override suspend fun addUser(user: User) = userRepository.addUser(user)

    override suspend fun getAllUsers() = userRepository.getAllUsers()
}
