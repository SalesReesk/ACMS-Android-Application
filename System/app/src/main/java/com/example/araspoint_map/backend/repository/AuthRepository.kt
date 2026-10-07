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

            usersCollection
                .document(firebaseUser.uid)
                .set(user)
                .await()

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

            val user = getUser(firebaseUser.uid)

            if (user == null) {
                Result.failure(
                    Exception("User profile not found.")
                )
            } else {
                Result.success(user)
            }

        } catch (_: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Wrong email or password."))
        } catch (_: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Account not found. Please check your email."))
        } catch (e: Exception) {
            Result.failure(e)
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

        val document = usersCollection
            .document(uid)
            .get()
            .await()

        return document.toObject(User::class.java)
    }
}