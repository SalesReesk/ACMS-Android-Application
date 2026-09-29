package com.example.araspoint_map.maintenance

import com.example.araspoint_map.asset.Asset
import com.example.araspoint_map.users.MaintenancePersonnel

class MaintenanceTask {
    var taskId: Int = 0
    var status: ReportStatus = ReportStatus.PENDING
    var remarks: String = ""

    var report: Report? = null
    var asset: Asset? = null
    var assignedPersonnel: MaintenancePersonnel? = null

    fun assignPersonnel(
        personnel: MaintenancePersonnel
    ) {
        assignedPersonnel = personnel
    }

    fun startTask() {
        status = ReportStatus.IN_PROGRESS
    }

    fun updateStatus(status: ReportStatus) {
        this.status = status
    }

    fun completeTask() {
        status = ReportStatus.COMPLETED
    }

    fun cancelTask() {
        status = ReportStatus.CANCELLED
    }
}