package com.example.araspoint_map.backend.model

data class User(
    var userId: String = generateUserId(),
    var name: String = "",
    var email: String = "",
    var password: String = ""
) {

    companion object {
        private var nextId = 1

        private fun generateUserId(): String {
            return "SR-${nextId++.toString().padStart(4, '0')}"
        }
    }
}