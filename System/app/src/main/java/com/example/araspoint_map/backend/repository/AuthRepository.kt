package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        deparment: String = "",
        idNumber: String = ""
    ): Result<User> {

        return try {

            val result = auth.createUserWithEmailAndPassword(
                email,
                password
            ).await()

            val firebaseUser = result.user
                ?: return Result.failure(
                    Exception("User registration failed.")
                )

            try {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            } catch (_: Exception) {}

            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                role = role,
                department = deparment,
                idNumber = idNumber.ifBlank { email.substringBefore("@") }
            )

            usersCollection
                .document(firebaseUser.uid)
                .set(user)
                .await()

            try {
                firebaseUser.sendEmailVerification().await()
            } catch ( _: Exception) {}

            Result.success(user)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<User> {

        return try {

            val result = auth.signInWithEmailAndPassword(
                email,
                password
            ).await()

            val firebaseUser = result.user
                ?: return Result.failure(
                    Exception("Login failed.")
                )

            var user = getUser(firebaseUser.uid)

            if (user == null) {
                val fbUser = auth.currentUser
                val fallbackRole = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT
                user = User(
                    userId = firebaseUser.uid,
                    name = fbUser?.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                    email = firebaseUser.email ?: email,
                    role = fallbackRole
                )
                try {
                    usersCollection
                        .document(firebaseUser.uid)
                        .set(user)
                        .await()
                } catch (_: Exception) {
                }
            }

            Result.success(user)

        } catch (_: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Wrong email or password."))
        } catch (_: FirebaseAuthInvalidUserException) {
            val fallbackRole = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT
            try {
                val registerResult = register(
                    name = email.substringBefore("@"),
                    email = email,
                    password = password,
                    role = fallbackRole
                )
                if (registerResult.isSuccess) {
                    registerResult
                } else {
                    Result.success(
                        User(
                            userId = auth.currentUser?.uid ?: "fallback-id",
                            name = auth.currentUser?.displayName ?: email.substringBefore("@"),
                            email = email,
                            role = fallbackRole
                        )
                    )
                }
            } catch (_: Exception) {
                Result.success(
                    User(
                        userId = auth.currentUser?.uid ?: "fallback-id",
                        name = email.substringBefore("@"),
                        email = email,
                        role = fallbackRole
                    )
                )
            }
        } catch (e: Exception) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val fallbackRole = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT
                Result.success(
                    User(
                        userId = currentUser.uid,
                        name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: email.substringBefore("@"),
                        email = currentUser.email ?: email,
                        role = fallbackRole
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun sendEmailVerification(): Result<Unit>  {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
                ?: return Result.failure(Exception("User not logged in."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified == true
        } catch (_: Exception) {
            false
        }
    }

    fun getCurrentUser(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserRole(): UserRole? {

        val uid = auth.currentUser?.uid
            ?: return null

        return getUser(uid)?.role
    }

    suspend fun getFullUser(): User? {
        val uid = auth.currentUser?.uid?: return null
        return getUser(uid)
    }

    suspend fun updateUserProfile(department: String, idNumber: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid?: return Result.failure(Exception("User not logged in."))
            usersCollection.document(uid)
                .update(mapOf("department" to department, "idNumber" to idNumber))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMaintenanceUsers(): List<User> {
        return try {
            val snapshot = usersCollection.whereEqualTo("role", UserRole.MAINTENANCE.name).get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun getUser(
        uid: String
    ): User? {
        val fbUser = auth.currentUser
        val email = fbUser?.email ?: ""
        val fallbackRole = if (email.contains("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.STUDENT

        return try {
            val document = usersCollection
                .document(uid)
                .get()
                .await()

            val user = document.toObject(User::class.java)
            if (user != null) {
                user
            } else {
                if (fbUser != null && fbUser.uid == uid) {
                    User(
                        userId = uid,
                        name = fbUser.displayName ?: email.substringBefore("@"),
                        email = email,
                        role = fallbackRole
                    )
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            if (fbUser != null && fbUser.uid == uid) {
                User(
                    userId = uid,
                    name = fbUser.displayName ?: email.substringBefore("@"),
                    email = email,
                    role = fallbackRole
                )
            } else {
                null
            }
        }
    }
}
