package com.geunwoo.jun.mindfulquestion.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GreenPrimary,
    secondary = GreenLight,
    tertiary = GreenDark
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    secondary = GreenLight,
    tertiary = GreenDark,
    background = BackgroundLight,
    surface = CardBackground,
    surfaceVariant = CardBackgroundTint,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    primaryContainer = CardBackground,
    onPrimaryContainer = TextPrimary,
    secondaryContainer = ButtonBackground
)

@Composable
fun MindfulQuestionTheme(
    darkTheme: Boolean = false,  // 항상 라이트 모드 사용
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,  // 다이나믹 컬러 비활성화
    content: @Composable () -> Unit
) {
    // 항상 라이트 테마 사용
    val colorScheme = LightColorScheme

    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
}