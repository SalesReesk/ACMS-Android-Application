package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus

data class MaintenanceRecord(
    var reportId: Int = 0,
    var description: String = "",
    var dateReported: String = "",
    var status: ReportStatus = ReportStatus.PENDING,
    var priority: PriorityStatus = PriorityStatus.LOW,

    var reporter: User? = null,
    var asset: Asset? = null
)