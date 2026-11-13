package com.example.araspoint_map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.repository.AuthRepository
import com.example.araspoint_map.frontend.viewModel.AuthViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val authRepository: AuthRepository = mockk()
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun login_success_invokesCallbackWithSuccess() = runTest(testDispatcher) {
        val mockUser = User(userId = "1", name = "Admin", email = "admin@test.com", role = UserRole.ADMIN)
        coEvery { authRepository.login("admin@test.com", "password") } returns Result.success(mockUser)

        var successCalled = false
        var message: String? = null

        viewModel.login("admin@test.com", "password") { success, msg ->
            successCalled = success
            message = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, successCalled)
        assertEquals("Welcome, Admin", message)
    }

    @Test
    fun register_success_invokesCallbackWithSuccess() = runTest(testDispatcher) {
        val mockUser = User(userId = "1", name = "User", email = "user@test.com", role = UserRole.STUDENT)
        coEvery { authRepository.register("User", "user@test.com", "password", UserRole.STUDENT, "") } returns Result.success(mockUser)

        var successCalled = false
        var message: String? = null

        viewModel.register("User", "user@test.com", "password", UserRole.STUDENT) { success, msg ->
            successCalled = success
            message = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, successCalled)
        assertEquals("Account created successfully", message)
    }
}
