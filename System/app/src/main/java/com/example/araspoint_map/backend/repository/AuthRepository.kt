package com.example.araspoint_map.backend.repository

import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.enums.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
        role: UserRole
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

            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                role = role
            )

            try {
                usersCollection
                    .document(firebaseUser.uid)
                    .set(user)
                    .await()
            } catch (_: Exception) {
            }

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
                user = User(
                    userId = firebaseUser.uid,
                    name = firebaseUser.email?.substringBefore("@") ?: "User",
                    email = firebaseUser.email ?: email,
                    role = UserRole.STUDENT
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
            try {
                val registerResult = register(
                    name = email.substringBefore("@"),
                    email = email,
                    password = password,
                    role = UserRole.STUDENT
                )
                if (registerResult.isSuccess) {
                    registerResult
                } else {
                    Result.success(
                        User(
                            userId = auth.currentUser?.uid ?: "fallback-id",
                            name = email.substringBefore("@"),
                            email = email,
                            role = UserRole.STUDENT
                        )
                    )
                }
            } catch (_: Exception) {
                Result.success(
                    User(
                        userId = auth.currentUser?.uid ?: "fallback-id",
                        name = email.substringBefore("@"),
                        email = email,
                        role = UserRole.STUDENT
                    )
                )
            }
        } catch (e: Exception) {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                Result.success(
                    User(
                        userId = currentUser.uid,
                        name = currentUser.email?.substringBefore("@") ?: email.substringBefore("@"),
                        email = currentUser.email ?: email,
                        role = UserRole.STUDENT
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

    fun getCurrentUser(): String? {
        return auth.currentUser?.uid
    }

    suspend fun getUserRole(): UserRole? {

        val uid = auth.currentUser?.uid
            ?: return null

        return getUser(uid)?.role
    }

    private suspend fun getUser(
        uid: String
    ): User? {
        return try {
            val document = usersCollection
                .document(uid)
                .get()
                .await()

            document.toObject(User::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
