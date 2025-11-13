package com.bardino.dozi.onboarding.components

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect

class SpotlightCoordinator {
    private val _targets = mutableStateMapOf<String, Rect>()
    val targets: Map<String, Rect> = _targets

    fun updateTarget(key: String, rect: Rect) {
        _targets[key] = rect
    }

    fun getTarget(key: String): Rect? = _targets[key]

    fun clear() {
        _targets.clear()
    }
}

@Composable
fun rememberSpotlightCoordinator(): SpotlightCoordinator {
    return remember { SpotlightCoordinator() }
}