package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository : BaseRepository(), IAuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val usersCollection = firestore.collection("users")

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        idNumber: String
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

    override suspend fun login(
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
                    role = fallbackRole,
                    idNumber = ""
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

    override fun logout() {
        auth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return getUser(uid)
    }

    override suspend fun sendEmailVerification(): Result<Unit>  {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
                ?: return Result.failure(Exception("User not logged in."))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified == true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getUserRole(): UserRole? {

        val uid = auth.currentUser?.uid
            ?: return null

        return getUser(uid)?.role
    }

    override suspend fun getFullUser(): User? {
        val uid = auth.currentUser?.uid?: return null
        return getUser(uid)
    }

    override suspend fun updateUserProfile(name: String): Result<Unit> {
        return try {
            val firebaseUser = auth.currentUser ?: return Result.failure(Exception("User not logged in."))
            if (name.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                firebaseUser.updateProfile(profileUpdates).await()
            }
            usersCollection.document(firebaseUser.uid)
                .update(mapOf("name" to name))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMaintenanceUsers(): List<User> {
        return try {
            val snapshot = usersCollection.whereEqualTo("role", UserRole.MAINTENANCE.name).get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllUsers(): List<User> {
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.documents.mapNotNull { it.toObject(User::class.java) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return try {
            usersCollection.document(userId).update("role", newRole).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
