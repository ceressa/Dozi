package com.bardino.dozi.core.ui.state

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: UiError) : UiState<Nothing>
}

sealed class UiError(open val message: String) {
    data class Network(override val message: String = "İnternet bağlantınızı kontrol edin") : UiError(message)
    data class Validation(override val message: String) : UiError(message)
    data class Unknown(override val message: String = "Bir hata oluştu") : UiError(message)
}