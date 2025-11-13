package com.bardino.dozi.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val DoziShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),  // Daha yuvarlak
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),      // Daha belirgin yuvarlaklık
    extraLarge = RoundedCornerShape(32.dp)
)

val ShapeNone = RoundedCornerShape(0.dp)
val ShapePill = RoundedCornerShape(percent = 50)

val ShapeTopOnly = RoundedCornerShape(    // HomeScreen için kullanılan
    topStart = 28.dp, // Daha büyük köşe
    topEnd = 28.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)