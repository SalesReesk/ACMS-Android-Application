package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.model.enums.UserRole

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val idNumber: String = "",
    val campus: String = "BatStateU ARASOF-Nasugbu",
    val photo: String = ""
) : Mappable {
    override val identifier: String
        get() = userId
}
