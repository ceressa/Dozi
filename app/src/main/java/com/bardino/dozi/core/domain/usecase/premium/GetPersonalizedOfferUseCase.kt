package com.bardino.dozi.core.domain.usecase.premium

import com.bardino.dozi.core.data.repository.MedicationLogRepository
import com.bardino.dozi.core.data.repository.MedicineRepository
import javax.inject.Inject

/**
 * Kişiselleştirilmiş premium teklifi üreten UseCase
 */
class GetPersonalizedOfferUseCase @Inject constructor(
    private val getUserSegmentUseCase: GetUserSegmentUseCase,
    private val medicineRepository: MedicineRepository,
    private val medicationLogRepository: MedicationLogRepository
) {
    data class PersonalizedOffer(
        val headline: String,
        val subheadline: String,
        val highlightedFeatures: List<String>,
        val ctaText: String
    )

    suspend operator fun invoke(userId: String): PersonalizedOffer {
        val segment = getUserSegmentUseCase(userId)
        val medicineCount = medicineRepository.getMedicineCount()
        val missedCount = medicationLogRepository.getMissedCountLast7Days(userId)

        return when (segment) {
            GetUserSegmentUseCase.UserSegment.HIGH_FREQUENCY -> PersonalizedOffer(
                headline = "$medicineCount ilacınız için Premium koruma",
                subheadline = "Sınırsız hatırlatma ve gelişmiş takvim",
                highlightedFeatures = listOf(
                    "Sınırsız ilaç ve hatırlatma",
                    "Gelişmiş takvim görünümü",
                    "Çoklu zaman dilimi desteği"
                ),
                ctaText = "Tüm ilaçlarımı yönet"
            )

            GetUserSegmentUseCase.UserSegment.FAMILY_USER -> PersonalizedOffer(
                headline = "Aileniz için Premium",
                subheadline = "Tüm aileyi tek hesaptan yönetin",
                highlightedFeatures = listOf(
                    "Aile planı (5 kişiye kadar)",
                    "Gelişmiş Badi özellikleri",
                    "Paylaşılan ilaç takibi"
                ),
                ctaText = "Aile planını başlat"
            )

            GetUserSegmentUseCase.UserSegment.CHRONIC_USER -> PersonalizedOffer(
                headline = "Uzun vadeli tedaviniz için",
                subheadline = "Aylık raporlar ve trend analizi",
                highlightedFeatures = listOf(
                    "Haftalık sağlık raporu",
                    "Doktor paylaşım özelliği",
                    "Detaylı uyumluluk analizi"
                ),
                ctaText = "Tedavimi optimize et"
            )

            GetUserSegmentUseCase.UserSegment.VITAMIN_USER -> PersonalizedOffer(
                headline = "Vitamin rutininizi güçlendirin",
                subheadline = "Takviye takibi için özel özellikler",
                highlightedFeatures = listOf(
                    "Stok takibi",
                    "Yenileme hatırlatmaları",
                    "Besin etkileşim uyarıları"
                ),
                ctaText = "Vitaminlerimi takip et"
            )

            GetUserSegmentUseCase.UserSegment.NEW_USER -> {
                if (missedCount > 0) {
                    PersonalizedOffer(
                        headline = "Son 7 günde $missedCount doz kaçırdınız",
                        subheadline = "Premium ile uyumluluğunuzu artırın",
                        highlightedFeatures = listOf(
                            "Gelişmiş bildirimler",
                            "Stok takibi",
                            "Detaylı istatistikler"
                        ),
                        ctaText = "Premium'a geç"
                    )
                } else {
                    PersonalizedOffer(
                        headline = "İlaçlarınızı hiç kaçırmayın",
                        subheadline = "Premium ile tam kontrol",
                        highlightedFeatures = listOf(
                            "Sınırsız hatırlatma",
                            "Badi sistemi",
                            "Gelişmiş raporlar"
                        ),
                        ctaText = "Premium'u keşfet"
                    )
                }
            }
        }
    }
}
