package com.example.abysstimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Static cached text styles to eliminate per-frame object allocation
val DigitTnumStyle = TextStyle(
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)
val StandardNoPaddingStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * Pixel-perfect colon separator for timer displays with dual circular dots.
 */
@Composable
fun TimerColon(
    color: Color,
    dotSize: Dp,
    dotSpacing: Dp,
    modifier: Modifier = Modifier
) {
    val totalH = dotSize * 2 + dotSpacing
    Spacer(
        modifier = modifier
            .size(width = dotSize, height = totalH)
            .drawBehind {
                val radius = dotSize.toPx() / 2f
                val centerX = size.width / 2f
                val topCenterY = radius
                val bottomCenterY = size.height - radius
                drawCircle(color = color, radius = radius, center = Offset(centerX, topCenterY))
                drawCircle(color = color, radius = radius, center = Offset(centerX, bottomCenterY))
            }
    )
}

/**
 * High-performance, anti-jitter timer text renderer with tabular numerals and aligned colons.
 */
@Composable
fun SpacedTimerText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    colonPadding: Dp = 2.5.dp,
    useCenterColonGrid: Boolean = false,
    colonFontFamily: FontFamily? = null,
    modifier: Modifier = Modifier
) {
    if (text.contains(':')) {
        val parts = text.split(':')
        val isLarge = fontSize.value >= 15f
        val dotSize = if (isLarge) 2.4.dp else 1.6.dp
        val dotSpacing = if (isLarge) 4.2.dp else 2.5.dp

        if (useCenterColonGrid && parts.size == 2) {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = parts[0],
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = DigitTnumStyle
                    )
                }

                TimerColon(
                    color = color,
                    dotSize = dotSize,
                    dotSpacing = dotSpacing,
                    modifier = Modifier.padding(horizontal = colonPadding)
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = parts[1],
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = DigitTnumStyle
                    )
                }
            }
        } else {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                parts.forEachIndexed { index, part ->
                    if (index > 0) {
                        TimerColon(
                            color = color,
                            dotSize = dotSize,
                            dotSpacing = dotSpacing,
                            modifier = Modifier.padding(horizontal = colonPadding)
                        )
                    }
                    Text(
                        text = part,
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        color = color,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = DigitTnumStyle
                    )
                }
            }
        }
    } else {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            modifier = modifier,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            style = StandardNoPaddingStyle
        )
    }
}

/**
 * Compact countdown / full-time label display for timer cards.
 */
@Composable
fun TimerLabels(ui: TimerUiState, isCompact: Boolean = false) {
    if (ui.entity.type == "orb" && !ui.isFull) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "次",
                    fontSize = if (isCompact) 9.5.sp else 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.padding(end = 2.dp),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                SpacedTimerText(
                    text = ui.orbNextCdText,
                    fontSize = if (isCompact) 10.5.sp else 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECEEF2),
                    colonPadding = if (isCompact) 1.dp else 1.5.dp
                )
            }
            SpacedTimerText(
                text = ui.fullAtText,
                fontSize = if (isCompact) 10.5.sp else 12.sp,
                fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFFC4B5FD),
                colonPadding = if (isCompact) 1.2.dp else 1.5.dp
            )
        }
    } else {
        SpacedTimerText(
            text = ui.fullAtText,
            fontSize = if (isCompact) 10.5.sp else 12.sp,
            fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFFC4B5FD),
            colonPadding = if (isCompact) 1.2.dp else 1.5.dp
        )
    }
}
