package com.example.araspoint_map.frontend.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.service.UserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(
    private val userService: UserService = UserService()
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadAllUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userService.getAllUsers()
            if (result.isSuccess) {
                _users.value = result.getOrDefault(emptyList())
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load users"
            }
            _isLoading.value = false
        }
    }

    fun addUser(user: User, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = userService.addUser(user)
            if (result.isSuccess) {
                onResult(true, "User added successfully")
                loadAllUsers()
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to add user")
            }
        }
    }
}
