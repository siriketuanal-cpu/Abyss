package com.example.abysstimer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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

val ColorCardValueMax = Color(0xFFA6F4E0)
val ColorCardClaimBorder = Color(0xFF5CD68A)
val ColorCardFullText = Color(0xFFFF6B6B)
val ColorCardWarnText = Color(0xFFFFAB5C)
val ColorCardNormalText = Color(0xFFECEEF2)
val ColorCardEditingText = Color(0xFFFFD166)
val ColorCardScheduledNormal = Color(0xFFC4B5FD)
val ColorCardScheduledFull = Color(0xFFFF6B6B)
val ColorCardOrbTsugi = Color(0xFFA78BFA)

/**
 * Ultra-responsive, zero-allocation flat text layout for TimerCard interior.
 * Direct Skia paragraph rendering with tabular figures (tnum) ensures instant 0ms finger release response,
 * while maintaining the clean, lightweight layout structure.
 */
@Composable
fun TimerCardContent(
    ui: TimerUiState,
    isCurEditing: Boolean,
    editingText: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Top Row (12dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(horizontal = 2.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (ui.entity.type) {
                "stam" -> {
                    Text(
                        text = ui.fullAtText,
                        fontSize = 10.5.sp,
                        fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (ui.isFull) ColorCardScheduledFull else ColorCardScheduledNormal,
                        maxLines = 1,
                        softWrap = false,
                        style = DigitTnumStyle
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${ui.entity.max}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCardValueMax,
                        maxLines = 1,
                        softWrap = false,
                        style = DigitTnumStyle
                    )
                }
                "orb" -> {
                    Text(
                        text = if (ui.isFull) "" else ui.orbNextCdText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorCardOrbTsugi,
                        maxLines = 1,
                        softWrap = false,
                        style = DigitTnumStyle
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = ui.fullAtText,
                        fontSize = 10.5.sp,
                        fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (ui.isFull) ColorCardScheduledFull else ColorCardScheduledNormal,
                        maxLines = 1,
                        softWrap = false,
                        style = DigitTnumStyle
                    )
                }
                "idle", "exped" -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ui.fullAtText,
                            fontSize = 10.5.sp,
                            fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (ui.isFull) ColorCardScheduledFull else ColorCardScheduledNormal,
                            maxLines = 1,
                            softWrap = false,
                            style = DigitTnumStyle
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // 2. Bottom Row (26dp)
        when (ui.entity.type) {
            "stam", "orb" -> {
                val displayText = if (isCurEditing) {
                    if (!editingText.isNullOrEmpty()) editingText else "_"
                } else {
                    "${ui.calculatedCurrent}"
                }
                val curColor = if (isCurEditing) {
                    ColorCardEditingText
                } else if (ui.isFull) {
                    ColorCardFullText
                } else if (ui.isWarn) {
                    ColorCardWarnText
                } else {
                    ui.paleColor
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = curColor,
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = DigitTnumStyle
                    )
                    if (ui.entity.type == "orb") {
                        Text(
                            text = "${ui.entity.max}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ColorCardValueMax,
                            maxLines = 1,
                            softWrap = false,
                            style = DigitTnumStyle,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 2.dp, bottom = 0.5.dp)
                        )
                    }
                }
            }
            "idle", "exped" -> {
                val isClaimState = ui.entity.state == "claim"
                val labelColor = if (isClaimState) {
                    ColorCardClaimBorder
                } else if (ui.isFull) {
                    ColorCardFullText
                } else if (ui.isWarn) {
                    ColorCardWarnText
                } else {
                    ui.paleColor
                }

                val isJapaneseText = isClaimState || (ui.isFull && ui.entity.type == "exped")
                val labelSize = if (isJapaneseText) 15.sp else if (ui.isFull) 16.5.sp else 17.5.sp
                val yOffset = if (isJapaneseText) (-1).dp else 0.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .offset(y = yOffset),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.idleDisplayLabel,
                        color = labelColor,
                        fontSize = labelSize,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        style = DigitTnumStyle
                    )
                }
            }
        }
    }
}

@Composable
fun TimerCardCanvas(
    ui: TimerUiState,
    isCurEditing: Boolean,
    editingText: String?,
    modifier: Modifier = Modifier
) {
    TimerCardContent(
        ui = ui,
        isCurEditing = isCurEditing,
        editingText = editingText,
        modifier = modifier
    )
}
