package com.example.araspoint_map.frontend.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.araspoint_map.backend.model.MaintenanceRecord
import com.example.araspoint_map.backend.service.MaintenanceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaintenanceViewModel(
    private val maintenanceService: MaintenanceService = MaintenanceService()
) : ViewModel() {

    private val _reports = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val reports: StateFlow<List<MaintenanceRecord>> = _reports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationStatus = MutableStateFlow<Result<Unit>?>(null)
    val operationStatus: StateFlow<Result<Unit>?> = _operationStatus.asStateFlow()

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = maintenanceService.getAllReports()
            if (result.isSuccess) {
                _reports.value = result.getOrDefault(emptyList())
            }
            _isLoading.value = false
        }
    }

    fun addReport(record: MaintenanceRecord) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = maintenanceService.addReport(record)
            _operationStatus.value = result
            if (result.isSuccess) {
                loadReports()
            }
            _isLoading.value = false
        }
    }

    fun updateReport(record: MaintenanceRecord) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = maintenanceService.updateReport(record)
            _operationStatus.value = result
            if (result.isSuccess) {
                loadReports()
            }
            _isLoading.value = false
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = maintenanceService.deleteReport(reportId)
            _operationStatus.value = result
            if (result.isSuccess) {
                loadReports()
            }
            _isLoading.value = false
        }
    }
}
