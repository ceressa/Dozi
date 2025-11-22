package com.bardino.dozi.core.ui.components.base

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dozi renk paleti
 */
object DoziColors {
    val Primary = Color(0xFF26C6DA)
    val PrimaryDark = Color(0xFF0095A8)
    val PrimaryLight = Color(0xFFB2EBF2)

    val Secondary = Color(0xFF7C4DFF)
    val SecondaryDark = Color(0xFF3F1DCB)
    val SecondaryLight = Color(0xFFB388FF)

    val Surface = Color(0xFFFAFAFA)
    val SurfaceDark = Color(0xFF121212)
    val Background = Color(0xFFFFFFFF)
    val BackgroundDark = Color(0xFF1E1E1E)

    val Error = Color(0xFFD32F2F)
    val ErrorLight = Color(0xFFFFCDD2)
    val Success = Color(0xFF4CAF50)
    val SuccessLight = Color(0xFFC8E6C9)
    val Warning = Color(0xFFFFA000)
    val WarningLight = Color(0xFFFFECB3)
    val Info = Color(0xFF2196F3)
    val InfoLight = Color(0xFFBBDEFB)

    val OnPrimary = Color.White
    val OnSecondary = Color.White
    val OnSurface = Color(0xFF212121)
    val OnSurfaceDark = Color(0xFFE0E0E0)
    val OnBackground = Color(0xFF212121)
    val OnBackgroundDark = Color(0xFFE0E0E0)

    // Medicine criticality colors
    val Routine = Color(0xFF26C6DA)
    val Important = Color(0xFFFFA000)
    val Critical = Color(0xFFD32F2F)

    // Chart colors
    val ChartPrimary = Color(0xFF26C6DA)
    val ChartSecondary = Color(0xFF7C4DFF)
    val ChartSuccess = Color(0xFF4CAF50)
    val ChartWarning = Color(0xFFFFA000)
    val ChartError = Color(0xFFD32F2F)

    // Gradient colors
    val GradientStart = Color(0xFF26C6DA)
    val GradientEnd = Color(0xFF7C4DFF)
}

/**
 * Dozi tipografi sistemi
 */
object DoziTypography {
    val h1 = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.sp,
        lineHeight = 36.sp
    )

    val h2 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
        lineHeight = 32.sp
    )

    val h3 = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 28.sp
    )

    val subtitle1 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
        lineHeight = 24.sp
    )

    val subtitle2 = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp
    )

    val body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.sp
    )

    val body2 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.sp
    )

    val button = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 1.25.sp,
        lineHeight = 16.sp
    )

    val caption = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 16.sp
    )

    val overline = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        lineHeight = 16.sp
    )
}

/**
 * Dozi spacing sistemi
 */
object DoziSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

/**
 * Dozi köşe yarıçapları
 */
object DoziCorners {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 1000.dp
}

/**
 * Dozi gölge değerleri
 */
object DoziElevation {
    val none = 0.dp
    val xs = 1.dp
    val sm = 2.dp
    val md = 4.dp
    val lg = 8.dp
    val xl = 16.dp
}
