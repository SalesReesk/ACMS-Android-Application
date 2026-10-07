package com.example.araspoint_map.frontend.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {

            val result = authRepository.register(
                name = name,
                email = email,
                password = password,
                role = role
            )

            if (result.isSuccess) {

                val user = result.getOrNull()

                onResult(
                    true,
                    "Account created successfully"
                )

            } else {

                onResult(
                    false,
                    result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {

            val result = authRepository.login(
                email,
                password
            )

            if (result.isSuccess) {

                val user = result.getOrNull()

                onResult(
                    true,
                    "Welcome, ${user?.name}"
                )

            } else {

                onResult(
                    false,
                    result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}