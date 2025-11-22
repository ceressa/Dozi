package com.bardino.dozi.core.ui.components.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun DoziTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = DoziColors.OnSurface.copy(alpha = 0.5f)) } },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isError) DoziColors.Error else DoziColors.Primary
                )
            }
        },
        trailingIcon = trailingIcon?.let {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (isError) DoziColors.Error else DoziColors.OnSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = DoziColors.Error) }
        } else null,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(DoziCorners.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DoziColors.Primary,
            unfocusedBorderColor = DoziColors.OnSurface.copy(alpha = 0.3f),
            errorBorderColor = DoziColors.Error,
            focusedLabelColor = DoziColors.Primary,
            cursorColor = DoziColors.Primary
        )
    )
}

@Composable
fun DoziSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ara...",
    onSearch: (() -> Unit)? = null
) {
    DoziTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        singleLine = true,
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke() }
        ),
        keyboardOptions = KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        )
    )
}
