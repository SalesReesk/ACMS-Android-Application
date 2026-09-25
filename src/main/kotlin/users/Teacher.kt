package users

import asset.Asset
import maintenance.Report

class Teacher : User() {

    fun viewAsset(asset: Asset) {
        println("Asset ID: ${asset.assetId}")
        println("Asset Name: ${asset.assetName}")
        println("Asset Type: ${asset.assetType}")
        println("Location: ${asset.location}")
        println("Latitude: ${asset.latitude}")
        println("Longitude: ${asset.longitude}")
        println("Status: ${asset.status}")
        println("Condition: ${asset.condition}")
        println("Photo: ${asset.photo}")
        println("Remarks: ${asset.remarks}")
    }

    fun reportAsset(
        asset: Asset,
        description: String
    ): Report {
        val report = Report()

        report.asset = asset
        report.reporter = this
        report.description = description

        println("$name reported ${asset.assetName}.")

        return report
    }

    fun checkReportStatus(report: Report) {
        println("Report Status: ${report.status}")
    }
}
