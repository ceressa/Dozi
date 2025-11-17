package com.bardino.dozi.core.utils

import android.content.Context
import android.widget.Toast
import com.bardino.dozi.core.data.repository.FamilyPlanRepository
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * 🧪 Aile Paketi Test Helper
 *
 * Test için bir kullanıcıyı aile paketi sahibi yapma
 *
 * Kullanım:
 * ```kotlin
 * FamilyPlanTestHelper.createTestFamilyPlan(context) { invitationCode ->
 *     // Davet kodu kullanıma hazır: ABC123
 * }
 * ```
 */
object FamilyPlanTestHelper {

    /**
     * Test aile planı oluştur (1 yıl geçerli)
     *
     * @param context Context
     * @param onCodeReady Davet kodu hazır olduğunda callback
     */
    fun createTestFamilyPlan(
        context: Context,
        onCodeReady: (String) -> Unit = {}
    ) {
        val repository = FamilyPlanRepository()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1 yıl sonra bitecek şekilde expiry date
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, 1)
                val expiryDate = Timestamp(calendar.time)

                // Aile planı oluştur
                val result = repository.createFamilyPlan(expiryDate)

                withContext(Dispatchers.Main) {
                    result.onSuccess { familyPlan ->
                        val message = """
                            ✅ Aile Paketi Aktif!

                            📝 Plan ID: ${familyPlan.id}
                            🎟️ Davet Kodu: ${familyPlan.invitationCode}
                            👤 Organizatör: ${familyPlan.organizerName}
                            📧 Email: ${familyPlan.organizerEmail}
                            👥 Max Üye: ${familyPlan.maxMembers}
                            📅 Bitiş: ${familyPlan.expiresAt?.toDate()}

                            Davet kodu ile maksimum 3 kişi katılabilir!
                        """.trimIndent()

                        Toast.makeText(context, "✅ Aile paketi oluşturuldu!", Toast.LENGTH_LONG).show()
                        android.util.Log.d("FamilyPlanTest", message)

                        onCodeReady(familyPlan.invitationCode)
                    }

                    result.onFailure { error ->
                        Toast.makeText(context, "❌ Hata: ${error.message}", Toast.LENGTH_LONG).show()
                        android.util.Log.e("FamilyPlanTest", "❌ Aile planı oluşturulamadı", error)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("FamilyPlanTest", "❌ Exception", e)
                }
            }
        }
    }

    /**
     * Aile planı bilgilerini göster
     */
    fun showFamilyPlanInfo(context: Context) {
        val repository = FamilyPlanRepository()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = repository.getUserFamilyPlan()

                withContext(Dispatchers.Main) {
                    result.onSuccess { familyPlan ->
                        if (familyPlan == null) {
                            Toast.makeText(context, "ℹ️ Aile planınız yok", Toast.LENGTH_SHORT).show()
                            android.util.Log.d("FamilyPlanTest", "ℹ️ Kullanıcının aile planı yok")
                        } else {
                            val message = """
                                📋 Aile Planı Bilgileri:

                                🎟️ Davet Kodu: ${familyPlan.invitationCode}
                                👤 Organizatör: ${familyPlan.organizerName}
                                👥 Üye Sayısı: ${familyPlan.currentMembers.size}/${familyPlan.maxMembers}
                                📅 Durum: ${familyPlan.status}
                                🕒 Bitiş: ${familyPlan.expiresAt?.toDate()}
                            """.trimIndent()

                            Toast.makeText(context, "✅ Aile planı aktif!", Toast.LENGTH_LONG).show()
                            android.util.Log.d("FamilyPlanTest", message)
                        }
                    }

                    result.onFailure { error ->
                        Toast.makeText(context, "❌ Hata: ${error.message}", Toast.LENGTH_SHORT).show()
                        android.util.Log.e("FamilyPlanTest", "❌ Bilgi alınamadı", error)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("FamilyPlanTest", "❌ Exception", e)
                }
            }
        }
    }

    /**
     * Davet kodu ile katıl (test için)
     */
    fun joinWithCode(context: Context, invitationCode: String) {
        val repository = FamilyPlanRepository()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = repository.joinFamilyPlan(invitationCode)

                withContext(Dispatchers.Main) {
                    result.onSuccess { familyPlan ->
                        Toast.makeText(context, "✅ Aile planına katıldınız!", Toast.LENGTH_LONG).show()
                        android.util.Log.d("FamilyPlanTest", "✅ Katılım başarılı: ${familyPlan.id}")
                    }

                    result.onFailure { error ->
                        Toast.makeText(context, "❌ ${error.message}", Toast.LENGTH_LONG).show()
                        android.util.Log.e("FamilyPlanTest", "❌ Katılım hatası", error)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ Hata: ${e.message}", Toast.LENGTH_LONG).show()
                    android.util.Log.e("FamilyPlanTest", "❌ Exception", e)
                }
            }
        }
    }
}
