package com.bardino.dozi.core.ui.components.base

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class DoziButtonVariant {
    Primary,
    Secondary,
    Outline,
    Text,
    Error
}

enum class DoziButtonSize {
    Small,
    Medium,
    Large
}

@Composable
fun DoziButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DoziButtonVariant = DoziButtonVariant.Primary,
    size: DoziButtonSize = DoziButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = when (variant) {
        DoziButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = DoziColors.Primary,
            contentColor = DoziColors.OnPrimary,
            disabledContainerColor = DoziColors.Primary.copy(alpha = 0.5f),
            disabledContentColor = DoziColors.OnPrimary.copy(alpha = 0.5f)
        )
        DoziButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = DoziColors.Secondary,
            contentColor = DoziColors.OnSecondary,
            disabledContainerColor = DoziColors.Secondary.copy(alpha = 0.5f),
            disabledContentColor = DoziColors.OnSecondary.copy(alpha = 0.5f)
        )
        DoziButtonVariant.Error -> ButtonDefaults.buttonColors(
            containerColor = DoziColors.Error,
            contentColor = Color.White,
            disabledContainerColor = DoziColors.Error.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
        DoziButtonVariant.Outline -> ButtonDefaults.outlinedButtonColors(
            contentColor = DoziColors.Primary
        )
        DoziButtonVariant.Text -> ButtonDefaults.textButtonColors(
            contentColor = DoziColors.Primary
        )
    }

    val height = when (size) {
        DoziButtonSize.Small -> 36.dp
        DoziButtonSize.Medium -> 48.dp
        DoziButtonSize.Large -> 56.dp
    }

    val textStyle = when (size) {
        DoziButtonSize.Small -> DoziTypography.caption
        DoziButtonSize.Medium -> DoziTypography.button
        DoziButtonSize.Large -> DoziTypography.subtitle1
    }

    when (variant) {
        DoziButtonVariant.Outline -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors,
                shape = RoundedCornerShape(DoziCorners.md)
            ) {
                ButtonContent(text, textStyle, loading, icon, size)
            }
        }
        DoziButtonVariant.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors
            ) {
                ButtonContent(text, textStyle, loading, icon, size)
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(height),
                enabled = enabled && !loading,
                colors = colors,
                shape = RoundedCornerShape(DoziCorners.md)
            ) {
                ButtonContent(text, textStyle, loading, icon, size)
            }
        }
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    textStyle: androidx.compose.ui.text.TextStyle,
    loading: Boolean,
    icon: ImageVector?,
    size: DoziButtonSize
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(
                when (size) {
                    DoziButtonSize.Small -> 16.dp
                    DoziButtonSize.Medium -> 20.dp
                    DoziButtonSize.Large -> 24.dp
                }
            ),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
    } else {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(
                    when (size) {
                        DoziButtonSize.Small -> 16.dp
                        DoziButtonSize.Medium -> 20.dp
                        DoziButtonSize.Large -> 24.dp
                    }
                )
            )
            Spacer(Modifier.width(DoziSpacing.sm))
        }
        Text(text, style = textStyle)
    }
}

@Composable
fun DoziIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
    tint: Color = DoziColors.Primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.5f)
        )
    }
}
