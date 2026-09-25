package users

import asset.Asset
import asset.Inventory
import maintenance.Report
import maintenance.MaintenanceTask

class Admin : User() {

    fun addAsset(
        inventory: Inventory,
        asset: Asset
    ) {
        inventory.addAsset(asset)
        println("${asset.assetName} added.")
    }

    fun updateAsset(
        inventory: Inventory,
        asset: Asset
    ) {
        inventory.updateAsset(asset)
        println("${asset.assetName} updated.")
    }

    fun removeAsset(
        inventory: Inventory,
        asset: Asset
    ) {
        inventory.removeAsset(asset)
        println("${asset.assetName} removed.")
    }

    fun viewAllAsset(inventory: Inventory) {
        for (asset in inventory.getAllAsset()) {
            inventory.viewDetails(asset)
            println()
        }
    }

    fun viewReport(report: Report) {
        report.viewReport()
    }

    fun assignMaintenanceTask(
        report: Report,
        personnel: MaintenancePersonnel
    ): MaintenanceTask {

        val task = MaintenanceTask()

        task.taskId = 1
        task.report = report
        task.asset = report.asset
        task.assignedPersonnel = personnel

        println("Maintenance task assigned to ${personnel.name}.")

        return task
    }
}