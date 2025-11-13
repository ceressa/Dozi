package com.bardino.dozi.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// TİTCK veritabanından gelen ilaç
data class Ilac(
    val ID: String?,
    val barcode: String?,
    val Active_Ingredient: String?,
    val Product_Name: String?,
    val Category_1: String?,
    val Description: String?,
    val Company_Name: String? = null
)

// TİTCK ilaç arama repository
object IlacRepository {
    private var cachedIlaclar: List<Ilac>? = null

    fun initialize(context: Context) {
        if (cachedIlaclar != null) return // zaten yüklenmiş
        try {
            val json = context.assets.open("ilaclar.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val raw = Gson().fromJson<List<Map<String, Any>>>(json, type)
            val dataSection = raw.firstOrNull { it["type"] == "table" }?.get("data") as? List<Map<String, Any>>
            cachedIlaclar = dataSection?.map {
                Ilac(
                    ID = it["ID"]?.toString(),
                    barcode = it["barcode"]?.toString(),
                    Active_Ingredient = it["Active_Ingredient"]?.toString(),
                    Product_Name = it["Product_Name"]?.toString(),
                    Category_1 = it["Category_1"]?.toString(),
                    Description = null, // devasa açıklamaları yükleme
                    Company_Name = it["Company_Name"]?.toString()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            cachedIlaclar = emptyList()
        }
    }

    fun getAll(): List<Ilac> = cachedIlaclar ?: emptyList()

    fun findByNameOrIngredient(query: String): Ilac? {
        val clean = query.trim().lowercase()
        return cachedIlaclar?.firstOrNull {
            it.Product_Name?.lowercase()?.contains(clean) == true ||
                    it.Active_Ingredient?.lowercase()?.contains(clean) == true
        }
    }
}