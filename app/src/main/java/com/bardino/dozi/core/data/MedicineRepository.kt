package com.bardino.dozi.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 💊 Kullanıcının kendi kaydettiği ilaç modeli
data class Medicine(
    val id: String,
    val name: String,
    val dosage: String = "", // Artık opsiyonel, varsayılan boş string
    val stock: Int
)

// 🧠 Uygulama genelinde hem cache hem kullanıcı verisini yöneten repository
object MedicineRepository {
    private const val PREF_NAME = "medicines_prefs"
    private const val KEY_MEDICINES = "medicines_json"
    private val gson = Gson()

    private var cachedIlaclar: List<Ilac>? = null
    private var initialized = false

    // 🔹 Belleğe yüklenmiş listeye salt-okunur erişim
    val ilaclarCache: List<Ilac>?
        get() = cachedIlaclar

    // ✅ Tek seferlik JSON yükleme
    fun initialize(context: Context) {
        if (initialized) return  // zaten yüklendiyse tekrar yükleme
        initialized = true

        try {
            val json = context.assets.open("ilaclar.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val raw = gson.fromJson<List<Map<String, Any>>>(json, type)
            val dataSection = raw.firstOrNull { it["type"] == "table" }?.get("data") as? List<Map<String, Any>>

            cachedIlaclar = dataSection?.map {
                Ilac(
                    ID = it["ID"]?.toString(),
                    barcode = it["barcode"]?.toString(),
                    Active_Ingredient = it["Active_Ingredient"]?.toString(),
                    Product_Name = it["Product_Name"]?.toString(),
                    Category_1 = it["Category_1"]?.toString(),
                    Description = it["Description"]?.toString()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            cachedIlaclar = emptyList()
        }
    }

    fun isInitialized(): Boolean = initialized

    // 🗂 Kullanıcının kendi eklediği ilaçları yükle
    fun loadMedicines(context: Context): List<Medicine> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICINES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Medicine>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveMedicines(context: Context, medicines: List<Medicine>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDICINES, gson.toJson(medicines)).apply()
    }

    fun saveMedicine(context: Context, medicine: Medicine) {
        val medicines = loadMedicines(context).toMutableList()
        val index = medicines.indexOfFirst { it.id == medicine.id }
        if (index >= 0) medicines[index] = medicine else medicines.add(medicine)
        saveMedicines(context, medicines)
    }

    fun deleteMedicine(context: Context, id: String) {
        val newList = loadMedicines(context).filterNot { it.id == id }
        saveMedicines(context, newList)
    }

    fun getMedicine(context: Context, id: String): Medicine? {
        return loadMedicines(context).find { it.id == id }
    }

    fun findByNameOrIngredient(query: String): Ilac? {
        val clean = query.trim().lowercase()
        return cachedIlaclar?.firstOrNull {
            it.Product_Name?.lowercase()?.contains(clean) == true ||
                    it.Active_Ingredient?.lowercase()?.contains(clean) == true
        }
    }
}
