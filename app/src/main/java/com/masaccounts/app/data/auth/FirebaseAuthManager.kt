package com.masaccounts.app.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {
    private const val TAG = "FirebaseAuthManager"

    val auth: FirebaseAuth?
        get() = try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth not available: ${e.message}")
            null
        }

    val currentFirebaseUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Exception) {
            null
        }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.success(null)
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim().lowercase(), password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signIn failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createAccount(email: String, password: String): Result<FirebaseUser?> {
        val firebaseAuth = auth ?: return Result.success(null)
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim().lowercase(), password).await()
            Result.success(result.user)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase createUser failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val firebaseAuth = auth ?: return Result.success(Unit)
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim().lowercase()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sendPasswordResetEmail failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase signOut error: ${e.message}")
        }
    }
}
