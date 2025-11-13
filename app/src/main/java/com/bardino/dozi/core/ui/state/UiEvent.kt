package com.bardino.dozi.core.ui.state

sealed interface UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent
    data class ShowToast(val message: String) : UiEvent
    data class Navigate(val route: String) : UiEvent
    data object NavigateBack : UiEvent
}