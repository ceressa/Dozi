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
        val doc = db.collection("users").document(user.uid).get().await()
        return doc.toObject(User::class.java)
    }

    suspend fun createUserIfNotExists() {
        val user = auth.currentUser ?: return
        val docRef = db.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) {
            val newUser = User(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString() ?: "",
                createdAt = System.currentTimeMillis(),
                timezone = "Europe/Istanbul",
                language = "tr",
                planType = "free",
                vibration = true,
                theme = "light",
                onboardingCompleted = false
            )
            docRef.set(newUser).await()
        }
    }

    suspend fun updateUser(user: User) {
        val currentUser = auth.currentUser ?: return
        val docRef = db.collection("users").document(currentUser.uid)
        docRef.set(user).await()
    }

    suspend fun updateUserField(field: String, value: Any) {
        val currentUser = auth.currentUser ?: return
        val docRef = db.collection("users").document(currentUser.uid)

        // Önce dokümanın var olup olmadığını kontrol et
        val snapshot = docRef.get().await()
        if (snapshot.exists()) {
            // Doküman varsa update et
            docRef.update(field, value).await()
        } else {
            // Doküman yoksa önce oluştur, sonra update et
            createUserIfNotExists()
            docRef.update(field, value).await()
        }
    }

    fun signOut() {
        auth.signOut()
    }

    /**
     * 🎁 Onboarding tamamlandıktan sonra 1 haftalık ücretsiz trial başlat
     */
    suspend fun activateTrialIfOnboarding() {
        val user = auth.currentUser ?: return
        val docRef = db.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        val userData = snapshot.toObject(User::class.java) ?: return

        // Eğer kullanıcı zaten premium veya trial almışsa, tekrar verme
        if (userData.isPremium || userData.premiumExpiryDate > 0) {
            return
        }

        // 1 haftalık trial ver
        val now = System.currentTimeMillis()
        val expiryDate = now + (7 * 24 * 60 * 60 * 1000L) // 7 gün

        val updates = hashMapOf<String, Any>(
            "isPremium" to true,
            "isTrial" to true,
            "planType" to "trial",
            "premiumStartDate" to now,
            "premiumExpiryDate" to expiryDate,
            "onboardingCompleted" to true
        )

        docRef.update(updates).await()
    }
}
