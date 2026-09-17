package com.aistudio.abyss.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AbyssColors {
    val Background = Color(0xFF0B0B14)
    val Card = Color(0xFF15151F)
    val Card2 = Color(0xFF1B1B28)
    val Line = Color(0xFF2A2A3A)
    val Text = Color(0xFFECEEF2)
    val Sub = Color(0xFF8A8EA3)
    val Accent = Color(0xFF9B8BFF)
    val Blue = Color(0xFF5AA9FF)
    val AbyssPurple = Color(0xFFB48CFF)
    val Orange = Color(0xFFFFAB5C)
    val Danger = Color(0xFFFF6B6B)
    val Ok = Color(0xFF5CD68A)
    val GroupBorder = Color(0xFF555B68)
}

val AbyssShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp)
)

val AbyssTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
        color = AbyssColors.Text
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = AbyssColors.Text
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = AbyssColors.Accent,
    onPrimary = Color(0xFF100C26),
    secondary = AbyssColors.Blue,
    background = AbyssColors.Background,
    surface = AbyssColors.Card,
    onBackground = AbyssColors.Text,
    onSurface = AbyssColors.Text,
    outline = AbyssColors.Line
)

@Composable
fun AbyssTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AbyssTypography,
        shapes = AbyssShapes,
        content = content
    )
}
