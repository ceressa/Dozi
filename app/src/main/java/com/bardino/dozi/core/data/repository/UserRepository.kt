package com.bardino.dozi.core.data.repository

import com.bardino.dozi.core.data.model.PremiumPlanType
import com.bardino.dozi.core.data.model.User
import com.bardino.dozi.core.premium.PremiumFields
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

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
     * 📱 DeviceId ile kullanıcı bul
     * Uygulama silinip tekrar yüklendiğinde deviceId ile kullanıcıyı tanımak için
     */
    suspend fun getUserByDeviceId(deviceId: String): User? {
        return try {
            val snapshot = db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .limit(1)
                .get()
                .await()

            if (snapshot.documents.isNotEmpty()) {
                snapshot.documents.first().toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "❌ Error finding user by deviceId: ${e.message}")
            null
        }
    }

    /**
     * 🎁 Yeni kullanıcıya 3 günlük ücretsiz trial başlat
     */
    suspend fun activateTrialForNewUser() {
        val user = auth.currentUser ?: return
        val docRef = db.collection("users").document(user.uid)
        val snapshot = docRef.get().await()
        val userData = snapshot.toObject(User::class.java) ?: return

        // Eğer kullanıcı zaten premium veya trial almışsa, tekrar verme
        if (userData.isPremium || userData.premiumExpiryDate > 0) {
            return
        }

        // 3 günlük trial ver
        val now = System.currentTimeMillis()
        val expiryDate = now + (3 * 24 * 60 * 60 * 1000L) // 3 gün

        docRef.update(PremiumFields.activePlan(PremiumPlanType.TRIAL, now, expiryDate, isTrial = true)).await()
    }

    /**
     * 👨‍👩‍👧‍👦 Kullanıcının premium durumunu kontrol et (Aile planı dahil)
     *
     * Premium durumu iki şekilde olabilir:
     * 1. Bireysel premium (isPremium = true, premiumExpiryDate kontrol)
     * 2. Aile planı üyesi (familyPlanId var, aile planı aktif)
     */
    suspend fun isPremiumUser(): Boolean {
        val userData = getUserData() ?: return false

        // 1. Bireysel premium kontrolü
        if (userData.premiumStatus().isActive) {
            return true
        }

        // 2. Aile planı kontrolü
        if (userData.isInFamilyPlan()) {
            // Aile planının aktif olup olmadığını kontrol et
            try {
                val familyPlanId = userData.familyPlanId ?: return false
                val familyPlanDoc = db.collection("family_plans").document(familyPlanId).get().await()

                if (familyPlanDoc.exists()) {
                    val status = familyPlanDoc.getString("status") ?: ""
                    val expiresAt = familyPlanDoc.getTimestamp("expiresAt")

                    // Plan aktif mi ve süresi dolmamış mı?
                    if (status == "ACTIVE" && expiresAt != null) {
                        val now = System.currentTimeMillis()
                        return now < expiresAt.toDate().time
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserRepository", "❌ Error checking family plan: ${e.message}")
            }
        }

        return false
    }

    /**
     * 🌟 Premium özelliklere erişim kontrolü
     *
     * Premium gerektiren özellikler için kullan
     */
    suspend fun requiresPremium(): Boolean {
        return !isPremiumUser()
    }
}
