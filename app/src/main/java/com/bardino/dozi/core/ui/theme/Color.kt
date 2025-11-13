package com.bardino.dozi.core.ui.theme

import androidx.compose.ui.graphics.Color

// --- ANA MARKA RENKLERİ (MaterialTheme'e bağlanan) ---
val DoziPrimary = Color(0xFF4DB6AC)       // Ana turkuaz-yeşil
val DoziPrimaryLight = Color(0xFF80CBC4)
val DoziPrimaryDark = Color(0xFF00897B)

val DoziAccent = Color(0xFFFF7043)        // Vurgu turuncu
val DoziAccentLight = Color(0xFFFFA082)
val DoziAccentDark = Color(0xFFE64A19)

// --- ORİJİNAL / GERİ UYUMLU RENKLER ---
val DoziRed = Color(0xFFFF5C5C)
val DoziBlue = Color(0xFF5DD9E2)
val DoziPurple = Color(0xFFEA4D4D)
val DoziTurquoise = Color(0xFF66E6EB)
val DoziTurquoiseLight = Color(0xFF99EFF2)
val DoziTurquoiseDark = Color(0xFF26C6DA)
val DoziCoral = Color(0xFFFF6B6B)
val DoziCoralLight = Color(0xFFFF9999)
val DoziCoralDark = Color(0xFFFF5252)
val DoziPurpleLight = Color(0xFFFF8080)

// --- DURUM RENKLERİ ---
val SuccessGreen = Color(0xFF66BB6A)
val WarningAmber = Color(0xFFFFCA28)
val WarningOrange = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF5350)

// --- NÖTR TONLAR ---
val VeryLightGray = Color(0xFFF3F4F6)
val LightGray = Color(0xFFE5E7EB)
val MediumGray = Color(0xFF9CA3AF)
val DarkGray = Color(0xFF374151)

// --- GRİ SKALASİ (100 → 900) ---
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
val Gray500 = Color(0xFF9E9E9E)
val Gray600 = Color(0xFF757575)
val Gray700 = Color(0xFF616161)
val Gray800 = Color(0xFF424242)
val Gray900 = Color(0xFF212121)

// --- ARKA PLAN ve METİN RENKLERİ ---
val BackgroundLight = Color(0xFF52CBDA)
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)

// --- KARANLIK MOD RENKLERİ ---
val BackgroundDark = Color(0xFF121212)
val SurfaceDark = Color(0xFF1E1E1E)
val TextPrimaryDark = Color(0xFFF9FAFB)
val TextSecondaryDark = Color(0xFFD1D5DB)

// Tema uyumlu alias'lar
val TextPrimaryLight = TextPrimary
val TextSecondaryLight = TextSecondary

// --- GRADİENTLER ---
val GradientPrimary = listOf(DoziPrimaryLight, DoziPrimaryDark)
val GradientAccent = listOf(DoziAccentLight, DoziAccentDark)
val GradientTurquoise = listOf(DoziTurquoiseLight, DoziTurquoiseDark)
val GradientCoral = listOf(DoziCoralLight, DoziCoralDark)
val GradientHero = listOf(DoziTurquoiseLight, DoziTurquoise)

// --- DİĞER TAMAMLAYICI RENKLER ---
val Overlay = Color(0x80000000) // %50 siyah gölge
val InfoBlue = Color(0xFF3B82F6)
val White10 = Color.White.copy(alpha = 0.1f)
val White20 = Color.White.copy(alpha = 0.2f)
val White85 = Color.White.copy(alpha = 0.85f)