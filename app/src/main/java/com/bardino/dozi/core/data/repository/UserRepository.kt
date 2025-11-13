package com.bardino.dozi.core.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.bardino.dozi.core.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getUserData(): User? {
        val user = auth.currentUser ?: return null
        val doc = db.collection("users").document(user.email!!).get().await()
        return doc.toObject(User::class.java)
    }

    suspend fun createUserIfNotExists() {
        val user = auth.currentUser ?: return
        val docRef = db.collection("users").document(user.email!!)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val newUser = User(
                id = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                timezone = "Europe/Istanbul"
            )
            docRef.set(newUser).await()
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
