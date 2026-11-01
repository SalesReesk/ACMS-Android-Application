package com.example.araspoint_map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.service.MaintenanceService
import com.example.araspoint_map.frontend.viewModel.MaintenanceViewModel
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
class MaintenanceViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val maintenanceService: MaintenanceService = mockk()
    private lateinit var viewModel: MaintenanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MaintenanceViewModel(maintenanceService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadReports_success_updatesReportsState() = runTest(testDispatcher) {
        val mockReports = listOf(MaintenanceRecord(reportId = "1", description = "Fix AC"))
        coEvery { maintenanceService.getAllReports() } returns Result.success(mockReports)

        viewModel.loadReports()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(mockReports, viewModel.reports.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun addReport_success_reloadsReports() = runTest(testDispatcher) {
        val report = MaintenanceRecord(reportId = "1", description = "Fix AC")
        val mockReports = listOf(report)
        coEvery { maintenanceService.addReport(report) } returns Result.success(Unit)
        coEvery { maintenanceService.getAllReports() } returns Result.success(mockReports)

        viewModel.addReport(report)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Result.success(Unit), viewModel.operationStatus.value)
        assertEquals(mockReports, viewModel.reports.value)
    }
}
