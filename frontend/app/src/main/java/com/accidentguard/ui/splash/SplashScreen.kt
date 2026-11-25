package com.accidentguard.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.accidentguard.R
import com.accidentguard.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation States
    var showHero by remember { mutableStateOf(false) }
    var showBranding by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }

    // Sequential Animation Trigger
    LaunchedEffect(Unit) {
        delay(200)
        showHero = true
        delay(600)
        showBranding = true
        delay(400)
        showTagline = true
        delay(300)
        showLoading = true
        delay(2000) // Total visible splash time
        onSplashFinished()
    }

    // Hero Animations
    val heroAlpha by animateFloatAsState(
        targetValue = if (showHero) 1f else 0f,
        animationSpec = tween(1000),
        label = "HeroAlpha"
    )
    val heroScale by animateFloatAsState(
        targetValue = if (showHero) 1f else 0.95f,
        animationSpec = tween(1200, easing = EaseOutCubic),
        label = "HeroScale"
    )

    // Text Animations
    val brandingAlpha by animateFloatAsState(
        targetValue = if (showBranding) 1f else 0f,
        animationSpec = tween(800),
        label = "BrandingAlpha"
    )
    val taglineAlpha by animateFloatAsState(
        targetValue = if (showTagline) 1f else 0f,
        animationSpec = tween(800),
        label = "TaglineAlpha"
    )
    val loadingAlpha by animateFloatAsState(
        targetValue = if (showLoading) 1f else 0f,
        animationSpec = tween(500),
        label = "LoadingAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(M3Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.weight(1.4f))

            // 1. Hero Illustration
            Image(
                painter = painterResource(id = R.drawable.ic_splash_hero),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .alpha(heroAlpha)
                    .scale(heroScale)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 2. Brand Wordmark (ACCIDENT GUARD)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(brandingAlpha)
            ) {
                Text(
                    text = "ACCIDENT",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colorResource(id = R.color.brand_navy),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GUARD",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colorResource(id = R.color.brand_red),
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Tagline
            Text(
                text = "SMART EMERGENCY & SAFETY NETWORK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = M3Primary.copy(alpha = 0.6f),
                letterSpacing = 2.sp,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.weight(1f))

            // 4. Subtle Loading Indicator
            Row(
                modifier = Modifier
                    .padding(bottom = 50.dp)
                    .alpha(loadingAlpha),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) { index ->
                    LoadingDot(delayMillis = index * 200)
                }
            }
        }
    }
}

@Composable
fun LoadingDot(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "DotTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = delayMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DotAlpha"
    )
    
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(M3Primary.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
    )
}
