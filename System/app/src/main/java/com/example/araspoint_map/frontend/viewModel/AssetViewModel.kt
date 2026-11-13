package com.example.araspoint_map.frontend.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.araspoint_map.backend.model.Asset
import com.example.araspoint_map.backend.service.AssetService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssetViewModel(
    private val assetService: AssetService = AssetService(),
    enableRealtime: Boolean = true
) : ViewModel() {

    private val _assets = MutableStateFlow<List<Asset>>(emptyList())
    val assets: StateFlow<List<Asset>> = _assets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationStatus = MutableStateFlow<Result<Unit>?>(null)
    val operationStatus: StateFlow<Result<Unit>?> = _operationStatus.asStateFlow()

    init {
        if (enableRealtime) {
            startRealtimeAssetsListener()
        }
    }

    private fun startRealtimeAssetsListener() {
        try {
            FirebaseFirestore.getInstance().collection("assets")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val list = snapshot.documents.mapNotNull { it.toObject(Asset::class.java) }
                        _assets.value = list
                    }
                }
        } catch (_: Exception) {}
    }

    fun loadAssets() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = assetService.readAllAssets()
            if (result.isSuccess) {
                _assets.value = result.getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    fun saveAsset(asset: Asset, isEditing: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isEditing) {
                assetService.updateAsset(asset)
            } else {
                assetService.createAsset(asset)
            }
            _operationStatus.value = result
            if (result.isSuccess) {
                loadAssets()
            }
            _isLoading.value = false
        }
    }

    fun deleteAsset(assetId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = assetService.deleteAsset(assetId)
            _operationStatus.value = result
            if (result.isSuccess) {
                loadAssets()
            }
            _isLoading.value = false
        }
    }
}
