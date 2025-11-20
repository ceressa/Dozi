package com.bardino.dozi.core.data

import android.content.Context

// 🔎 JSON'daki resmi ilaç veritabanında arama yapan sınıf
object IlacJsonRepository {

    /**
     * Uygulama açıldığında MedicineLookupRepository.initialize() çağrıldığı için
     * burada ilaclar.json tekrar okunmaz.
     */
    fun search(context: Context, query: String): List<IlacSearchResult> {
        android.util.Log.d("IlacJsonRepository", "Search called with query: '$query'")

        // Eğer cache henüz hazır değilse, güvenlik için initialize et
        if (!MedicineLookupRepository.isInitialized()) {
            android.util.Log.d("IlacJsonRepository", "Cache not initialized, initializing now...")
            MedicineLookupRepository.initialize(context)
        }

        val clean = query.trim().lowercase()
        val ilaclar = MedicineLookupRepository.ilaclarCache ?: emptyList()
        android.util.Log.d("IlacJsonRepository", "Cache size: ${ilaclar.size}, searching for: '$clean'")

        val results = ilaclar
            .filter {
                it.Product_Name?.lowercase()?.contains(clean) == true ||
                        it.Active_Ingredient?.lowercase()?.contains(clean) == true
            }
            .take(50) // 💡 sadece ilk 50 sonucu getir
            .map { IlacSearchResult(it) }

        android.util.Log.d("IlacJsonRepository", "Search results: ${results.size} medicines found")
        return results
    }

    fun searchByBarcode(context: Context, barcode: String): Ilac? {
        // Cache hazır değilse initialize et
        if (!MedicineLookupRepository.isInitialized()) {
            MedicineLookupRepository.initialize(context)
        }

        // Cache'teki ilaç listesini al
        val ilacList = MedicineLookupRepository.ilaclarCache ?: emptyList()
        val cleanBarcode = barcode.trim()

        // Barkoda göre arama yap
        return ilacList.firstOrNull { it.barcode == cleanBarcode }
    }


}

// 🔹 Arama sonucu modeli
data class IlacSearchResult(
    val item: Ilac,
    val dosage: String? = null
)
