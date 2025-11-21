package com.bardino.dozi.core.data.repository

import android.util.Log
import com.bardino.dozi.core.data.model.FAQ
import com.bardino.dozi.core.data.model.FAQCategory
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FAQRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Tum SSS'leri kategoriye gore gruplu getir
     */
    suspend fun getAllFAQs(): List<FAQ> {
        return try {
            val snapshot = db.collection("faqs")
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(FAQ::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching FAQs: ${e.message}")
            emptyList()
        }
    }

    /**
     * Kategorileri getir
     */
    suspend fun getCategories(): List<FAQCategory> {
        return try {
            val snapshot = db.collection("faq_categories")
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(FAQCategory::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching categories: ${e.message}")
            emptyList()
        }
    }

    /**
     * Belirli bir kategorideki SSS'leri getir
     */
    suspend fun getFAQsByCategory(category: String): List<FAQ> {
        return try {
            val snapshot = db.collection("faqs")
                .whereEqualTo("category", category)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(FAQ::class.java)?.copy(id = it.id) }
                .sortedBy { it.order }
        } catch (e: Exception) {
            Log.e("FAQRepository", "Error fetching FAQs by category: ${e.message}")
            emptyList()
        }
    }

    /**
     * SSS'lerde arama yap
     */
    suspend fun searchFAQs(query: String): List<FAQ> {
        val allFaqs = getAllFAQs()
        val lowerQuery = query.lowercase()

        return allFaqs.filter { faq ->
            faq.question.lowercase().contains(lowerQuery) ||
            faq.answer.lowercase().contains(lowerQuery)
        }
    }
}
