package com.bardino.dozi.core.ui.screens.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bardino.dozi.R
import com.bardino.dozi.core.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Dozi'ye Hoş Geldin!",
                description = "İlaç takibini kolaylaştır, sağlığını kontrol altında tut. Hiç ilaç almayı unutma!",
                icon = Icons.Default.MedicalServices,
                backgroundColor = listOf(DoziTurquoise, DoziTurquoiseDark)
            ),
            OnboardingPage(
                title = "Hatırlatmalar",
                description = "İlaçlarını zamanında almak için özel hatırlatmalar oluştur. Günlük, haftalık veya özel takvim.",
                icon = Icons.Default.Notifications,
                backgroundColor = listOf(DoziBlue, Color(0xFF0288D1))
            ),
            OnboardingPage(
                title = "İlaç Takibi",
                description = "İlaç stoklarını takip et, reçete bilgilerini kaydet. Hangi ilacı ne zaman aldığını gör.",
                icon = Icons.Default.EventNote,
                backgroundColor = listOf(DoziCoral, DoziCoralDark)
            ),
            OnboardingPage(
                title = "Tüm Cihazlarda",
                description = "Giriş yap ve verilerini tüm cihazlarında senkronize et. Her yerden erişim sağla.",
                icon = Icons.Default.CloudSync,
                backgroundColor = listOf(SuccessGreen, Color(0xFF388E3C))
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = pages[pagerState.currentPage].backgroundColor
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onSkip,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Daha Sonra",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ViewPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Page Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (isSelected) 32.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                            )
                            .animateContentSize(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button (invisible on first page)
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Geri", fontWeight = FontWeight.Medium)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                // Next/Finish Button
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = pages[pagerState.currentPage].backgroundColor[0]
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 140.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 4.dp
                    )
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Devam" else "Başla",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (pagerState.currentPage < pages.size - 1)
                            Icons.Default.ArrowForward else Icons.Default.Check,
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Icon with Dozi character
        AnimatedContent(
            targetState = page.icon,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                        fadeOut(animationSpec = tween(600))
            },
            label = "icon_animation"
        ) { icon ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Dozi character image
                Surface(
                    modifier = Modifier.size(200.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Image(
                        painter = painterResource(R.drawable.dozi_happy),
                        contentDescription = "Dozi",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    )
                }

                // Feature icon
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White,
                    tonalElevation = 8.dp
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = page.backgroundColor[0],
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title with animation
        AnimatedContent(
            targetState = page.title,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600)) +
                        slideInVertically(animationSpec = tween(600)) { it / 2 }) togetherWith
                        fadeOut(animationSpec = tween(600))
            },
            label = "title_animation"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 32.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description with animation
        AnimatedContent(
            targetState = page.description,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                        slideInVertically(animationSpec = tween(600, delayMillis = 100)) { it / 2 }) togetherWith
                        fadeOut(animationSpec = tween(600))
            },
            label = "description_animation"
        ) { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                lineHeight = 28.sp,
                fontSize = 18.sp
            )
        }
    }
}
