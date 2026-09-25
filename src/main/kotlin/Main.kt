import asset.Asset
import asset.Inventory
import asset.AssetCondition
import asset.AssetStatus
import maintenance.PriorityStatus
import maintenance.ReportStatus
import users.Admin
import users.MaintenancePersonnel
import users.Student
import users.Teacher

fun main() {
    val inventory = Inventory()

    val admin = Admin().apply {
        name = "Maria Santos"
        email = "maria.santos@aams.edu"
        password = "admin123"
    }
    val student = Student().apply {
        name = "Juan Dela Cruz"
        email = "juan.delacruz@aams.edu"
        password = "student123"
    }
    val teacher = Teacher().apply {
        name = "Ana Reyes"
        email = "ana.reyes@aams.edu"
        password = "teacher123"
    }
    val technician = MaintenancePersonnel().apply {
        name = "Carlo Garcia"
        email = "carlo.garcia@aams.edu"
        password = "maintenance123"
    }

    val laptop = Asset().apply {
        assetName = "Dell Latitude 5420"
        assetType = "Laptop"
        location = "Computer Laboratory 1"
        latitude = 14.5995
        longitude = 120.9842
        status = AssetStatus.AVAILABLE
        condition = AssetCondition.GOOD
        photo = "laptop-5420.jpg"
        remarks = "Assigned to the computer laboratory."
    }
    val projector = Asset().apply {
        assetName = "Epson EB-X06"
        assetType = "Projector"
        location = "Room 204"
        latitude = 14.5997
        longitude = 120.9844
        status = AssetStatus.UNDER_MAINTENANCE
        condition = AssetCondition.DAMAGED
        photo = "epson-eb-x06.jpg"
        remarks = "Projector lamp does not turn on."
    }
    val printer = Asset().apply {
        assetName = "HP LaserJet Pro"
        assetType = "Printer"
        location = "Registrar's Office"
        latitude = 14.5993
        longitude = 120.9840
        status = AssetStatus.AVAILABLE
        condition = AssetCondition.GOOD
        photo = "hp-laserjet.jpg"
        remarks = "Toner replaced this month."
    }

    admin.login()
    admin.addAsset(inventory, laptop)
    admin.addAsset(inventory, projector)
    admin.addAsset(inventory, printer)

    println("\n--- Student Asset View ---")
    student.login()
    student.viewProfile()
    student.viewMap()
    student.searchAsset()
    student.calculateDistanceToAsset(
        laptop,
        currentLatitude = 14.6000,
        currentLongitude = 120.9850
    )
    student.viewAsset(laptop)

    println("\n--- Teacher Report ---")
    teacher.login()
    teacher.viewAsset(projector)
    val report = teacher.reportAsset(
        projector,
        "The projector will not power on even after checking the cable."
    ).apply {
        reportId = 1
        dateReported = "2026-09-10"
        priority = PriorityStatus.HIGH
        submitReport()
    }

    println("\n--- Maintenance Workflow ---")
    admin.viewReport(report)
    val task = admin.assignMaintenanceTask(report, technician)
    technician.viewAssignedTask(task)
    technician.acceptTask(task)
    technician.updateEquipmentCondition(task, AssetCondition.GOOD)
    technician.addMaintenanceRemarks(task, "Replaced the faulty power adapter.")
    technician.completeTask(task)
    report.updateStatus(ReportStatus.COMPLETED)
    student.checkReportStatus(report)

    println("\n--- Inventory ---")
    admin.viewAllAsset(inventory)

    student.logout()
    teacher.logout()
    admin.logout()
}