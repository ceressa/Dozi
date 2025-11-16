package com.bardino.dozi.core.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.core.data.local.entity.ProfileEntity

/**
 * Helper function to get display name for profile
 * Shows "Aile Üyesi" instead of "Varsayılan Profil" for better UX
 */
private fun getProfileDisplayName(profile: ProfileEntity): String {
    return when (profile.name) {
        "Varsayılan Profil", "default-profile" -> "Aile Üyesi"
        else -> profile.name
    }
}

/**
 * Dialog for creating a new profile
 */
@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, avatar: String, color: String) -> Unit,
    isPremium: Boolean
) {
    var name by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("👤") }
    var selectedColor by remember { mutableStateOf("#6200EE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Aile Üyesi Ekle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim") },
                    placeholder = { Text("Örn: Anne, Baba, Çocuk") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Avatar selection
                Text("Avatar Seçin", fontWeight = FontWeight.Bold)
                AvatarPicker(
                    selectedAvatar = selectedAvatar,
                    onAvatarSelected = { selectedAvatar = it }
                )

                // Color selection
                Text("Renk Seçin", fontWeight = FontWeight.Bold)
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedAvatar, selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

/**
 * Dialog for editing an existing profile
 */
@Composable
fun EditProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, avatar: String, color: String) -> Unit,
    onSetPin: (() -> Unit)? = null,
    onRemovePin: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedAvatar by remember { mutableStateOf(profile.avatarIcon) }
    var selectedColor by remember { mutableStateOf(profile.color) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aile Üyesini Düzenle") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("İsim") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Avatar selection
                Text("Avatar Seçin", fontWeight = FontWeight.Bold)
                AvatarPicker(
                    selectedAvatar = selectedAvatar,
                    onAvatarSelected = { selectedAvatar = it }
                )

                // Color selection
                Text("Renk Seçin", fontWeight = FontWeight.Bold)
                ColorPicker(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )

                // PIN management
                if (onSetPin != null || onRemovePin != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Güvenlik", fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (onSetPin != null && profile.pinCode == null) {
                            OutlinedButton(
                                onClick = onSetPin,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("PIN Ekle")
                            }
                        }

                        if (onRemovePin != null && profile.pinCode != null) {
                            OutlinedButton(
                                onClick = onRemovePin,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("PIN Kaldır")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, selectedAvatar, selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

/**
 * Dialog for confirming profile deletion
 */
@Composable
fun DeleteProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Aile Üyesini Sil?") },
        text = {
            Text("\"${getProfileDisplayName(profile)}\" aile üyesini silmek istediğinizden emin misiniz? Bu işlem geri alınamaz ve bu aile üyesinin tüm ilaçları silinecektir.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

/**
 * Avatar picker component
 */
@Composable
fun AvatarPicker(
    selectedAvatar: String,
    onAvatarSelected: (String) -> Unit
) {
    val avatars = listOf(
        "👤", "👨", "👩", "👴", "👵", "👶",
        "👦", "👧", "🧒", "👨‍⚕️", "👩‍⚕️", "🧓",
        "😊", "😃", "🥰", "😇", "🤗", "😎",
        "💊", "🏥", "❤️", "💚", "💙", "💛"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.heightIn(max = 200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(avatars) { avatar ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (avatar == selectedAvatar) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { onAvatarSelected(avatar) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = avatar,
                    fontSize = 24.sp
                )
            }
        }
    }
}

/**
 * Color picker component
 */
@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#6200EE", // Purple
        "#03DAC5", // Teal
        "#FF0266", // Pink
        "#FF5722", // Deep Orange
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FFC107", // Amber
        "#9C27B0", // Purple
        "#00BCD4", // Cyan
        "#FF9800", // Orange
        "#795548", // Brown
        "#607D8B"  // Blue Gray
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        modifier = Modifier.heightIn(max = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(colors) { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(color)))
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (color == selectedColor) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Seçili",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
