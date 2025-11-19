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
    fun initialize(context: Context, forceReload: Boolean = false) {
        if (initialized && !forceReload) {
            android.util.Log.d("MedicineRepository", "Already initialized, skipping...")
            // Ama cache boşsa, reload et
            if (cachedIlaclar.isNullOrEmpty()) {
                android.util.Log.w("MedicineRepository", "Cache is empty despite initialization, forcing reload...")
                initialized = false
            } else {
                return  // zaten yüklendiyse ve cache doluysa tekrar yükleme
            }
        }
        initialized = true

        try {
            android.util.Log.d("MedicineRepository", "Starting to initialize ilaclar.json...")
            val json = context.assets.open("ilaclar.json").bufferedReader().use { it.readText() }
            android.util.Log.d("MedicineRepository", "JSON file read successfully, length: ${json.length}")

            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val raw = gson.fromJson<List<Map<String, Any>>>(json, type)
            android.util.Log.d("MedicineRepository", "JSON parsed, raw size: ${raw?.size}")

            val dataSection = raw.firstOrNull { it["type"] == "table" }?.get("data") as? List<Map<String, Any>>
            android.util.Log.d("MedicineRepository", "Data section extracted, size: ${dataSection?.size}")

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

            android.util.Log.d("MedicineRepository", "✅ Initialize successful! Loaded ${cachedIlaclar?.size ?: 0} medicines")
        } catch (e: Exception) {
            android.util.Log.e("MedicineRepository", "❌ Error initializing ilaclar.json: ${e.message}", e)
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
