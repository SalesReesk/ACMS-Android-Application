package com.example.araspoint_map

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.service.AssetService
import com.example.araspoint_map.frontend.viewModel.AssetViewModel
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
class AssetViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val assetService: AssetService = mockk()
    private lateinit var viewModel: AssetViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AssetViewModel(assetService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAssets_success_updatesAssetsState() = runTest(testDispatcher) {
        val mockAssets = listOf(Asset(assetId = "1", assetName = "Test Asset"))
        coEvery { assetService.readAllAssets() } returns Result.success(mockAssets)

        viewModel.loadAssets()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(mockAssets, viewModel.assets.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun deleteAsset_success_reloadsAssets() = runTest(testDispatcher) {
        val mockAssets = emptyList<Asset>()
        coEvery { assetService.deleteAsset("1") } returns Result.success(Unit)
        coEvery { assetService.readAllAssets() } returns Result.success(mockAssets)

        viewModel.deleteAsset("1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(Result.success(Unit), viewModel.operationStatus.value)
        assertEquals(mockAssets, viewModel.assets.value)
    }
}
