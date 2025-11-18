package com.bardino.dozi.core.error

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.UnknownHostException

/**
 * 🚨 Merkezi Hata Yönetimi Sistemi
 *
 * Uygulamadaki tüm hataları tek bir sealed class hiyerarşisiyle yönetir.
 * Bu sayede:
 * - Tutarlı hata mesajları
 * - Kullanıcı dostu mesajlar
 * - Kolay hata takibi
 * - Test edilebilir error handling
 */
sealed class DoziError {
    abstract val message: String
    abstract val userMessage: String
    abstract fun log()

    /**
     * 🌐 Ağ Hataları
     */
    data class Network(
        override val message: String,
        val cause: Throwable? = null
    ) : DoziError() {
        override val userMessage: String
            get() = "İnternet bağlantınızı kontrol edin"

        override fun log() {
            android.util.Log.e("DoziError.Network", message, cause)
        }
    }

    /**
     * 🔥 Firebase Hataları
     */
    data class Firebase(
        val code: String,
        override val message: String,
        val cause: FirebaseException? = null
    ) : DoziError() {
        override val userMessage: String
            get() = when (code) {
                "permission-denied" -> "Bu işlem için yetkiniz yok"
                "not-found" -> "İstenen veri bulunamadı"
                "already-exists" -> "Bu veri zaten mevcut"
                "unauthenticated" -> "Lütfen giriş yapın"
                "unavailable" -> "Sunucu şu anda kullanılamıyor"
                else -> "Bir hata oluştu. Lütfen tekrar deneyin"
            }

        override fun log() {
            android.util.Log.e("DoziError.Firebase", "[$code] $message", cause)
        }
    }

    /**
     * 🔐 Kimlik Doğrulama Hataları
     */
    data class Authentication(
        val code: String,
        override val message: String,
        val cause: FirebaseAuthException? = null
    ) : DoziError() {
        override val userMessage: String
            get() = when (code) {
                "invalid-email" -> "Geçersiz email adresi"
                "user-disabled" -> "Hesabınız devre dışı bırakılmış"
                "user-not-found" -> "Kullanıcı bulunamadı"
                "wrong-password" -> "Hatalı şifre"
                "email-already-in-use" -> "Bu email zaten kullanımda"
                "weak-password" -> "Şifre çok zayıf"
                "network-request-failed" -> "İnternet bağlantısı yok"
                else -> "Giriş yapılırken hata oluştu"
            }

        override fun log() {
            android.util.Log.e("DoziError.Auth", "[$code] $message", cause)
        }
    }

    /**
     * ✅ Doğrulama Hataları
     */
    data class Validation(
        val field: String,
        val reason: String,
        override val message: String
    ) : DoziError() {
        override val userMessage: String
            get() = when (field) {
                "medicine_name" -> "İlaç adı boş olamaz"
                "dosage" -> "Doz bilgisi gerekli"
                "time" -> "Geçerli bir saat girin"
                "email" -> "Geçerli bir email girin"
                "password" -> "Şifre en az 6 karakter olmalı"
                else -> "Lütfen tüm alanları doldurun"
            }

        override fun log() {
            android.util.Log.w("DoziError.Validation", "[$field] $reason: $message")
        }
    }

    /**
     * 🔒 İzin Hataları
     */
    data class Permission(
        val permission: String,
        override val message: String
    ) : DoziError() {
        override val userMessage: String
            get() = when (permission) {
                "POST_NOTIFICATIONS" -> "Bildirim izni gerekli. Lütfen ayarlardan izin verin"
                "SCHEDULE_EXACT_ALARM" -> "Tam zamanlı alarm izni gerekli"
                "ACCESS_FINE_LOCATION" -> "Konum izni gerekli"
                "CAMERA" -> "Kamera izni gerekli"
                else -> "Bu özellik için izin gerekli"
            }

        override fun log() {
            android.util.Log.w("DoziError.Permission", "[$permission] $message")
        }
    }

    /**
     * 🗄️ Veritabanı Hataları
     */
    data class Database(
        override val message: String,
        val cause: Throwable? = null
    ) : DoziError() {
        override val userMessage: String
            get() = "Veri kaydedilemedi. Lütfen tekrar deneyin"

        override fun log() {
            android.util.Log.e("DoziError.Database", message, cause)
        }
    }

    /**
     * 💳 Billing Hataları
     */
    data class Billing(
        val code: Int,
        override val message: String
    ) : DoziError() {
        override val userMessage: String
            get() = when (code) {
                1 -> "Satın alma iptal edildi"
                2 -> "Hizmet kullanılamıyor"
                3 -> "Billing servisi bağlanamadı"
                4 -> "Ürün mevcut değil"
                5 -> "Geçersiz ürün türü"
                6 -> "Hata: İşlem başarısız"
                7 -> "Ürün zaten sahipsiniz"
                8 -> "Ürün satın alınamıyor"
                else -> "Satın alma hatası"
            }

        override fun log() {
            android.util.Log.e("DoziError.Billing", "[$code] $message")
        }
    }

    /**
     * ❓ Bilinmeyen Hatalar
     */
    data class Unknown(
        override val message: String,
        val cause: Throwable? = null
    ) : DoziError() {
        override val userMessage: String
            get() = "Beklenmeyen bir hata oluştu"

        override fun log() {
            android.util.Log.e("DoziError.Unknown", message, cause)
        }
    }

    companion object {
        /**
         * Exception'dan DoziError oluştur
         */
        fun from(exception: Throwable): DoziError {
            return when (exception) {
                is UnknownHostException, is IOException -> Network(
                    message = exception.message ?: "Network error",
                    cause = exception
                )

                is FirebaseNetworkException -> Network(
                    message = "Firebase network error",
                    cause = exception
                )

                is FirebaseAuthException -> Authentication(
                    code = exception.errorCode,
                    message = exception.message ?: "Auth error",
                    cause = exception
                )

                is FirebaseFirestoreException -> Firebase(
                    code = exception.code.name,
                    message = exception.message ?: "Firestore error",
                    cause = exception
                )

                is FirebaseException -> Firebase(
                    code = "unknown",
                    message = exception.message ?: "Firebase error",
                    cause = exception
                )

                else -> Unknown(
                    message = exception.message ?: "Unknown error",
                    cause = exception
                )
            }
        }
    }
}

/**
 * 📦 Result Wrapper - Başarı veya Hata
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val error: DoziError) : Result<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun errorOrNull(): DoziError? = when (this) {
        is Success -> null
        is Error -> error
    }

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (DoziError) -> Unit): Result<T> {
        if (this is Error) action(error)
        return this
    }
}

/**
 * Extension: Try-catch bloğunu Result'a dönüştür
 */
inline fun <T> resultOf(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        val error = DoziError.from(e)
        error.log()
        Result.Error(error)
    }
}

/**
 * Extension: Suspend fonksiyonlar için try-catch
 */
suspend inline fun <T> suspendResultOf(crossinline block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        val error = DoziError.from(e)
        error.log()
        Result.Error(error)
    }
}
