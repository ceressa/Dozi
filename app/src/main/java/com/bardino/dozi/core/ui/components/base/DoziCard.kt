package com.bardino.dozi.core.ui.components.base

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
fun DoziCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    elevation: Dp = DoziElevation.sm,
    containerColor: Color = DoziColors.Surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(DoziCorners.lg),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(DoziSpacing.md),
            content = content
        )
    }
}

@Composable
fun DoziOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = DoziColors.Primary.copy(alpha = 0.3f),
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(DoziCorners.lg),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(borderColor)
        )
    ) {
        Column(
            modifier = Modifier.padding(DoziSpacing.md),
            content = content
        )
    }
}

@Composable
fun DoziInsightCard(
    title: String,
    description: String,
    severity: String,
    recommendation: String? = null,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (severity) {
        "INFO" -> DoziColors.InfoLight
        "WARNING" -> DoziColors.WarningLight
        "CRITICAL" -> DoziColors.ErrorLight
        else -> DoziColors.Surface
    }

    val accentColor = when (severity) {
        "INFO" -> DoziColors.Info
        "WARNING" -> DoziColors.Warning
        "CRITICAL" -> DoziColors.Error
        else -> DoziColors.Primary
    }

    DoziCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = backgroundColor
    ) {
        Column {
            Text(
                text = title,
                style = DoziTypography.subtitle1,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(DoziSpacing.xs))

            Text(
                text = description,
                style = DoziTypography.body2,
                color = DoziColors.OnSurface
            )

            recommendation?.let {
                Spacer(modifier = Modifier.height(DoziSpacing.sm))
                Text(
                    text = "💡 $it",
                    style = DoziTypography.caption,
                    color = DoziColors.Primary
                )
            }
        }
    }
}

@Composable
fun DoziStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    valueColor: Color = DoziColors.Primary
) {
    DoziCard(modifier = modifier) {
        Column {
            Text(
                text = title,
                style = DoziTypography.caption,
                color = DoziColors.OnSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(DoziSpacing.xs))

            Text(
                text = value,
                style = DoziTypography.h2,
                color = valueColor
            )

            subtitle?.let {
                Spacer(modifier = Modifier.height(DoziSpacing.xxs))
                Text(
                    text = it,
                    style = DoziTypography.caption,
                    color = DoziColors.OnSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
