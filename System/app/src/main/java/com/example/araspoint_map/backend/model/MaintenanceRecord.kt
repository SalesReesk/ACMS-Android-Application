package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.model.enums.PriorityStatus
import com.example.araspoint_map.backend.model.enums.ReportStatus
import java.util.UUID

data class MaintenanceRecord(
    var reportId: String = UUID.randomUUID().toString(),
    var assetId: String = "",
    var title: String = "",
    var description: String = "",
    var location: String = "",
    var dateReported: String = "",
    var dateUpdated: String = "",
    var status: ReportStatus = ReportStatus.PENDING,
    var priority: PriorityStatus = PriorityStatus.MEDIUM,
    var reporterEmail: String = "",
    var assignedTo: String = ""
)
