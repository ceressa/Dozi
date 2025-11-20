package com.bardino.dozi.core.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// 💊 Kullanıcının kendi kaydettiği ilaç modeli (Local/Onboarding için)
data class LocalMedicine(
    val id: String,
    val name: String,
    val dosage: String = "",
    val stock: Int
)

/**
 * 🧠 İlaç Arama ve Lokal Depolama Repository'si
 *
 * İki ana görevi var:
 * 1. ilaclar.json'dan ilaç veritabanını yükler (autocomplete için)
 * 2. Kullanıcının lokal ilaçlarını SharedPreferences'da saklar (onboarding için)
 *
 * NOT: Firestore'daki ilaçlar için MedicineRepository (repository paketi) kullanılır
 */
object MedicineLookupRepository {
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
            android.util.Log.d("MedicineLookup", "Already initialized, skipping...")
            // Ama cache boşsa, reload et
            if (cachedIlaclar.isNullOrEmpty()) {
                android.util.Log.w("MedicineLookup", "Cache is empty despite initialization, forcing reload...")
                initialized = false
            } else {
                return  // zaten yüklendiyse ve cache doluysa tekrar yükleme
            }
        }

        try {
            android.util.Log.d("MedicineLookup", "Starting to initialize ilaclar.json...")
            val json = context.assets.open("ilaclar.json").bufferedReader().use { it.readText() }
            android.util.Log.d("MedicineLookup", "JSON file read successfully, length: ${json.length}")

            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val raw = gson.fromJson<List<Map<String, Any>>>(json, type)
            android.util.Log.d("MedicineLookup", "JSON parsed, raw size: ${raw?.size}")

            val dataSection = raw.firstOrNull { it["type"] == "table" }?.get("data") as? List<Map<String, Any>>
            android.util.Log.d("MedicineLookup", "Data section extracted, size: ${dataSection?.size}")

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

            // Sadece başarılı yüklemede initialized = true yap
            if (!cachedIlaclar.isNullOrEmpty()) {
                initialized = true
                android.util.Log.d("MedicineLookup", "✅ Initialize successful! Loaded ${cachedIlaclar?.size ?: 0} medicines")
            } else {
                initialized = false
                android.util.Log.e("MedicineLookup", "❌ Initialize failed: cache is empty after loading")
            }

            // Debug: İlk 3 ilacı logla
            cachedIlaclar?.take(3)?.forEachIndexed { index, ilac ->
                android.util.Log.d("MedicineLookup", "Sample[$index]: ${ilac.Product_Name} | ${ilac.Active_Ingredient}")
            }
        } catch (e: Exception) {
            android.util.Log.e("MedicineLookup", "❌ Error initializing ilaclar.json: ${e.message}", e)
            android.util.Log.e("MedicineLookup", "Stack trace:", e)
            cachedIlaclar = emptyList()
            initialized = false
        }
    }

    fun isInitialized(): Boolean = initialized

    // 🗂 Kullanıcının kendi eklediği ilaçları yükle (Local storage)
    fun loadLocalMedicines(context: Context): List<LocalMedicine> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICINES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LocalMedicine>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLocalMedicines(context: Context, medicines: List<LocalMedicine>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDICINES, gson.toJson(medicines)).apply()
    }

    fun saveLocalMedicine(context: Context, medicine: LocalMedicine) {
        val medicines = loadLocalMedicines(context).toMutableList()
        val index = medicines.indexOfFirst { it.id == medicine.id }
        if (index >= 0) medicines[index] = medicine else medicines.add(medicine)
        saveLocalMedicines(context, medicines)
    }

    fun deleteLocalMedicine(context: Context, id: String) {
        val newList = loadLocalMedicines(context).filterNot { it.id == id }
        saveLocalMedicines(context, newList)
    }

    fun getLocalMedicine(context: Context, id: String): LocalMedicine? {
        return loadLocalMedicines(context).find { it.id == id }
    }

    fun findByNameOrIngredient(query: String): Ilac? {
        val clean = query.trim().lowercase()
        return cachedIlaclar?.firstOrNull {
            it.Product_Name?.lowercase()?.contains(clean) == true ||
                    it.Active_Ingredient?.lowercase()?.contains(clean) == true
        }
    }

    // 🔄 Backward compatibility - eski methodlar
    @Deprecated("Use loadLocalMedicines instead", ReplaceWith("loadLocalMedicines(context)"))
    fun loadMedicines(context: Context) = loadLocalMedicines(context)

    @Deprecated("Use saveLocalMedicine instead", ReplaceWith("saveLocalMedicine(context, medicine)"))
    fun saveMedicine(context: Context, medicine: LocalMedicine) = saveLocalMedicine(context, medicine)

    @Deprecated("Use getLocalMedicine instead", ReplaceWith("getLocalMedicine(context, id)"))
    fun getMedicine(context: Context, id: String) = getLocalMedicine(context, id)
}

// Backward compatibility type alias
@Deprecated("Use LocalMedicine instead", ReplaceWith("LocalMedicine"))
typealias Medicine = LocalMedicine
