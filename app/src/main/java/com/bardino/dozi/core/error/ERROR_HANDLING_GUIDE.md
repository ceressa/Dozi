# Error Handling Guide

## Overview

DoziError provides centralized error management for the entire app with user-friendly messages and consistent logging.

## Quick Start

### Basic Usage

```kotlin
// In Repository
suspend fun getMedicine(id: String): Result<Medicine> = suspendResultOf {
    val doc = firestore.collection("medicines").document(id).get().await()
    doc.toObject(Medicine::class.java) ?: throw Exception("Medicine not found")
}

// In ViewModel
viewModelScope.launch {
    medicineRepository.getMedicine(id)
        .onSuccess { medicine ->
            _uiState.value = UiState.Success(medicine)
        }
        .onError { error ->
            _uiState.value = UiState.Error(error.userMessage)
            error.log()
        }
}
```

### Manual Error Creation

```kotlin
// Validation error
if (medicineName.isBlank()) {
    return Result.Error(
        DoziError.Validation(
            field = "medicine_name",
            reason = "empty",
            message = "Medicine name is required"
        )
    )
}

// Permission error
if (!hasNotificationPermission()) {
    return Result.Error(
        DoziError.Permission(
            permission = "POST_NOTIFICATIONS",
            message = "Notification permission not granted"
        )
    )
}
```

## Error Types

### 1. Network Errors
Internet connectivity issues, timeouts, DNS failures

### 2. Firebase Errors
Firestore, Auth, Functions, Storage errors with Firebase error codes

### 3. Authentication Errors
Login, signup, password reset failures

### 4. Validation Errors
User input validation failures

### 5. Permission Errors
Missing Android runtime permissions

### 6. Database Errors
Room database operation failures

### 7. Billing Errors
In-app purchase failures with billing response codes

### 8. Unknown Errors
Unexpected exceptions

## Best Practices

1. **Always use Result wrapper** in repository functions
2. **Log errors** with `error.log()`
3. **Show user messages** with `error.userMessage`
4. **Handle errors in ViewModel**, not in UI
5. **Test error scenarios** for each operation

## Migration Example

### Before:
```kotlin
// ❌ Bad: Generic error handling
try {
    val medicine = getMedicine(id)
    // ...
} catch (e: Exception) {
    Log.e(TAG, "Error", e)
    showError("Something went wrong")
}
```

### After:
```kotlin
// ✅ Good: Structured error handling
getMedicine(id)
    .onSuccess { medicine ->
        // Handle success
    }
    .onError { error ->
        error.log()
        showError(error.userMessage)
    }
```

## UI Integration

```kotlin
// In Composable
when (val state = uiState) {
    is UiState.Error -> {
        ErrorDialog(
            message = state.message,
            onDismiss = { /* ... */ }
        )
    }
}
```
