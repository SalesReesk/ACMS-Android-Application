package maintenance

import asset.Asset
import users.User
import kotlin.jvm.JvmName

class Report {
    var reportId: Int = 0
    var description: String = ""
    var dateReported: String = ""
    var status: ReportStatus = ReportStatus.PENDING
    var priority: PriorityStatus = PriorityStatus.LOW

    var reporter: User? = null
    var asset: Asset? = null

    fun submitReport() {
        status = ReportStatus.PENDING
        println("Report submitted.")
    }

    fun updateStatus(status: ReportStatus) {
        this.status = status
    }

    @JvmName("updatePriority")
    fun setPriority(priority: PriorityStatus) {
        this.priority = priority
    }

    fun viewReport() {
        println("Report ID: $reportId")
        println("Reporter: ${reporter?.name}")
        println("Equipment: ${asset?.assetName}")
        println("Description: $description")
        println("Status: $status")
        println("Priority: $priority")
    }
}
