package com.bardino.dozi.onboarding.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Spotlight için bileşen koordinatlarını yakalar
 */
fun Modifier.spotlightTarget(
    key: String,
    coordinator: SpotlightCoordinator?,
    enabled: Boolean = true
): Modifier {
    if (!enabled || coordinator == null) return this

    return this.onGloballyPositioned { coordinates ->
        val position = coordinates.positionInRoot()
        val size = coordinates.size

        coordinator.updateTarget(
            key = key,
            rect = Rect(
                left = position.x,
                top = position.y,
                right = position.x + size.width,
                bottom = position.y + size.height
            )
        )
    }
}