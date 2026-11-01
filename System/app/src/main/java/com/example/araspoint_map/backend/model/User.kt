package com.example.araspoint_map.backend.model

import com.example.araspoint_map.backend.model.enums.UserRole

data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val department: String = "",
    val idNumber: String = "",
    val campus: String = "BatStateU ARASOF-Nasugbu",
    val photo: String = ""
)

val User.shortDepartment: String
    get() = department.toShortDepartment()

fun String.toShortDepartment(): String {
    if (this.isBlank()) return "—"
    val trimmed = this.trim()

    if (trimmed.length <= 8 && !trimmed.contains(" ")) return trimmed

    val regex = Regex("""\(([^)]+)\)""")
    val match = regex.find(trimmed)
    if (match != null) {
        val code = match.groupValues[1].trim()
        if (code.length <= 10) return code
    }

    if (trimmed.contains(" - ")) {
        val firstPart = trimmed.substringBefore(" - ").trim().removePrefix("(").removeSuffix(")")
        if (firstPart.length <= 10) return firstPart
    }

    return when {
        trimmed.contains("Informatics", ignoreCase = true) || trimmed.contains("CICS", ignoreCase = true) -> "CICS"
        trimmed.contains("Engineering", ignoreCase = true) || trimmed.contains("CEAFA", ignoreCase = true) -> "CEAFA"
        trimmed.contains("Accountancy", ignoreCase = true) || trimmed.contains("CABEIHM", ignoreCase = true) -> "CABEIHM"
        trimmed.contains("Arts and Sciences", ignoreCase = true) || trimmed.contains("CAS", ignoreCase = true) -> "CAS"
        trimmed.contains("Teacher", ignoreCase = true) || trimmed.contains("CTE", ignoreCase = true) -> "CTE"
        trimmed.contains("Criminal", ignoreCase = true) || trimmed.contains("CCJE", ignoreCase = true) -> "CCJE"
        trimmed.contains("Health", ignoreCase = true) || trimmed.contains("CHS", ignoreCase = true) -> "CHS"
        trimmed.contains("Nursing", ignoreCase = true) || trimmed.contains("CONAHS", ignoreCase = true) -> "CONAHS"
        trimmed.contains("Industrial", ignoreCase = true) || trimmed.contains("CIT", ignoreCase = true) -> "CIT"
        trimmed.contains("Agriculture", ignoreCase = true) || trimmed.contains("CAF", ignoreCase = true) -> "CAF"
        trimmed.contains("Administration", ignoreCase = true) -> "Admin"
        trimmed.contains("Student", ignoreCase = true) -> "Student"
        else -> trimmed
    }
}
