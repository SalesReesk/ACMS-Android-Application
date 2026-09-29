package com.example.araspoint_map.users

import com.example.araspoint_map.asset.AssetCondition
import com.example.araspoint_map.maintenance.MaintenanceTask
import com.example.araspoint_map.maintenance.ReportStatus

class MaintenancePersonnel : User() {

    fun viewAssignedTask(task: MaintenanceTask) {
        println("Task ID: ${task.taskId}")
        println("Equipment: ${task.asset?.assetName}")
        println("Status: ${task.status}")
    }

    fun acceptTask(task: MaintenanceTask) {
        task.status = ReportStatus.IN_PROGRESS
        println("$name accepted Task ${task.taskId}.")
    }

    fun updateMaintenanceStatus(
        task: MaintenanceTask,
        status: ReportStatus,
    ) {
        task.status = status
        println("Task ${task.taskId} status updated to $status.")
    }

    fun updateEquipmentCondition(
        task: MaintenanceTask,
        condition: AssetCondition
    ) {
        task.asset?.condition = condition

        println(
            "${task.asset?.assetName} condition updated to $condition."
        )
    }

    fun addMaintenanceRemarks(
        task: MaintenanceTask,
        remarks: String
    ) {
        task.remarks = remarks
    }

    fun completeTask(task: MaintenanceTask) {
        task.status = ReportStatus.COMPLETED
        println("Task ${task.taskId} completed.")
    }
}