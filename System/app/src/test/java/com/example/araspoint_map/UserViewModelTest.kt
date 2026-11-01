package com.example.araspoint_map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.example.araspoint_map.backend.service.UserService
import com.example.araspoint_map.frontend.viewModel.UserViewModel
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
class UserViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val userService: UserService = mockk()
    private lateinit var viewModel: UserViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = UserViewModel(userService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllUsers_success_updatesUsersState() = runTest(testDispatcher) {
        val mockUsers = listOf(User(userId = "1", name = "John", role = UserRole.STUDENT))
        coEvery { userService.getAllUsers() } returns Result.success(mockUsers)

        viewModel.loadAllUsers()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(mockUsers, viewModel.users.value)
        assertEquals(false, viewModel.isLoading.value)
        assertEquals(null, viewModel.errorMessage.value)
    }

    @Test
    fun addUser_success_invokesCallbackAndReloads() = runTest(testDispatcher) {
        val user = User(userId = "1", name = "John", role = UserRole.STUDENT)
        coEvery { userService.addUser(user) } returns Result.success(Unit)
        coEvery { userService.getAllUsers() } returns Result.success(listOf(user))

        var successCalled = false
        var message: String? = null

        viewModel.addUser(user) { success, msg ->
            successCalled = success
            message = msg
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(true, successCalled)
        assertEquals("User added successfully", message)
    }
}
