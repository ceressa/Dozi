// 📁 com.bardino.dozi.core.ui.components.DoziTopBar.kt
package com.bardino.dozi.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bardino.dozi.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoziTopBar(
    title: String,
    canNavigateBack: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit)? = null,
    backgroundColor: Color = Color.White
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
            )
        },
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .background(DoziTurquoise.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = DoziTurquoise
                    )
                }
            }
        },
        actions = {
            actions?.invoke(this)
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor,
            titleContentColor = TextPrimaryLight,
            navigationIconContentColor = DoziTurquoise
        )
    )
}
