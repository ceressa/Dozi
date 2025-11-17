package com.bardino.dozi.core.ui.screens.family

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.components.DoziTopBar
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

// Aile üyesi data class
data class FamilyMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val role: MemberRole = MemberRole.MEMBER,
    val joinedDate: Long = System.currentTimeMillis()
)

enum class MemberRole {
    ADMIN,
    MEMBER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyManagementScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State'ler
    var inviteCode by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<FamilyMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf<FamilyMember?>(null) }
    var isVisible by remember { mutableStateOf(false) }

    // Animasyon için
    LaunchedEffect(Unit) {
        isVisible = true
        // Mock data - gerçek implementasyonda Firestore'dan çekilecek
        inviteCode = generateInviteCode()
        members = listOf(
            FamilyMember(
                name = "Sen (Admin)",
                email = "user@example.com",
                role = MemberRole.ADMIN
            ),
            FamilyMember(
                name = "Ahmet Yılmaz",
                email = "ahmet@example.com",
                role = MemberRole.MEMBER
            )
        )
    }

    Scaffold(
        topBar = {
            DoziTopBar(
                title = "Aile Yönetimi",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 🎨 Hero Bölümü
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -50 })
                ) {
                    HeroSection()
                }
            }

            // 🔑 Davet Kodu Bölümü
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
                ) {
                    InviteCodeCard(
                        inviteCode = inviteCode,
                        onCopy = {
                            copyToClipboard(context, inviteCode)
                            Toast.makeText(context, "Davet kodu kopyalandı!", Toast.LENGTH_SHORT).show()
                        },
                        onRefresh = {
                            inviteCode = generateInviteCode()
                            Toast.makeText(context, "Yeni davet kodu oluşturuldu!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // 👥 Üye Listesi
            item {
                Text(
                    text = "Aile Üyeleri (${members.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(members, key = { it.id }) { member ->
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 100 })
                ) {
                    MemberCard(
                        member = member,
                        onRemove = {
                            if (member.role != MemberRole.ADMIN) {
                                showRemoveDialog = member
                            }
                        }
                    )
                }
            }

            // Alt boşluk
            item {
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    // ❌ Üye çıkarma dialog'u
    showRemoveDialog?.let { member ->
        RemoveMemberDialog(
            memberName = member.name,
            onConfirm = {
                members = members.filter { it.id != member.id }
                Toast.makeText(context, "${member.name} aileden çıkarıldı", Toast.LENGTH_SHORT).show()
                showRemoveDialog = null
            },
            onDismiss = { showRemoveDialog = null }
        )
    }
}

@Composable
private fun HeroSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(GradientHero),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.dozi_happy2),
                contentDescription = "Aile",
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
            )
            Text(
                text = "Ailecek Sağlıklı Kalın",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Ailenizle ilaçlarınızı paylaşın ve birbirinizi takip edin",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InviteCodeCard(
    inviteCode: String,
    onCopy: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = DoziTurquoise,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Davet Kodu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Yenile",
                        tint = DoziTurquoise
                    )
                }
            }

            // Davet kodu gösterimi
            Surface(
                color = DoziTurquoise.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, DoziTurquoise.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inviteCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DoziTurquoise,
                        letterSpacing = 4.dp.value.sp
                    )
                    IconButton(onClick = onCopy) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = DoziTurquoise
                        )
                    }
                }
            }

            Text(
                text = "Bu kodu aile üyelerinizle paylaşın. Kodunuzu kullanarak ailenize katılabilirler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MemberCard(
    member: FamilyMember,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (member.role == MemberRole.ADMIN)
                DoziTurquoise.copy(alpha = 0.08f)
            else
                Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (member.role == MemberRole.ADMIN)
                DoziTurquoise.copy(alpha = 0.3f)
            else
                Gray200
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (member.role == MemberRole.ADMIN)
                        DoziTurquoise.copy(alpha = 0.2f)
                    else
                        DoziCoral.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (member.role == MemberRole.ADMIN)
                                DoziTurquoise
                            else
                                DoziCoral,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (member.role == MemberRole.ADMIN) {
                            Surface(
                                color = DoziTurquoise,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = member.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Çıkar butonu (sadece admin olmayanlarda)
            if (member.role != MemberRole.ADMIN) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.PersonRemove,
                        contentDescription = "Çıkar",
                        tint = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoveMemberDialog(
    memberName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    color = WarningOrange.copy(alpha = 0.15f),
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Üyeyi Çıkar?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "$memberName adlı kişiyi aileden çıkarmak istediğinize emin misiniz?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, Gray200)
                    ) {
                        Text(
                            "İptal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ErrorRed
                        )
                    ) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Çıkar",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Utility fonksiyonlar
private fun generateInviteCode(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..6)
        .map { chars.random() }
        .joinToString("")
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Davet Kodu", text)
    clipboard.setPrimaryClip(clip)
}
