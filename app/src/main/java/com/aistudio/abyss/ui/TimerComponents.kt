package com.aistudio.abyss.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.abyss.model.AbyssItem
import com.aistudio.abyss.model.GroupItem
import com.aistudio.abyss.model.HeaderItem
import com.aistudio.abyss.model.IdleItem
import com.aistudio.abyss.model.RuleItem
import com.aistudio.abyss.model.StamItem
import com.aistudio.abyss.model.TimerCardItem
import com.aistudio.abyss.model.calculateIdle
import com.aistudio.abyss.model.calculateStamina
import com.aistudio.abyss.model.formatCountdown
import com.aistudio.abyss.model.formatHM
import com.aistudio.abyss.model.isNearFull
import com.aistudio.abyss.model.remainingAfter40

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StamCard(
    item: StamItem,
    now: Long,
    onLongClick: () -> Unit,
    onEditCurrent: () -> Unit,
    onEditMax: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, now)
    val isNear = isNearFull(calc.remainMs, calc.isFull)
    val borderColor = if (calc.isFull) AbyssColors.Orange else AbyssColors.Blue

    Box(
        modifier = modifier
            .testTag("card_stam_${item.id}")
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AbyssColors.Card)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onEditCurrent,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Clock at top right
        Text(
            text = formatHM(calc.fullAt),
            color = if (calc.isFull) AbyssColors.Danger else AbyssColors.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Center values: current / max
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${calc.current}",
                color = if (isNear) AbyssColors.Danger else AbyssColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .combinedClickable(
                        onClick = onEditCurrent,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 2.dp)
            )
            Text(
                text = "/",
                color = AbyssColors.Sub,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
            Text(
                text = "${item.max}",
                color = AbyssColors.Blue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .combinedClickable(
                        onClick = onEditMax,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AbyssCard(
    item: AbyssItem,
    now: Long,
    isPending40: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onEditRank: () -> Unit,
    onEditCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, now)
    val isNear = isNearFull(calc.remainMs, calc.isFull)
    val displayCur = if (isPending40) remainingAfter40(calc.current) else calc.current

    val strokeColor = when {
        isPending40 -> AbyssColors.Ok
        calc.isFull -> AbyssColors.Orange
        else -> AbyssColors.AbyssPurple
    }

    Box(
        modifier = modifier
            .testTag("card_abyss_${item.id}")
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AbyssColors.Card)
            .then(
                if (isPending40) {
                    Modifier.drawBehind {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        drawRoundRect(
                            color = strokeColor,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                            style = stroke
                        )
                    }
                } else {
                    Modifier.border(1.dp, strokeColor, RoundedCornerShape(10.dp))
                }
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Top Left: Rank
        Text(
            text = "Lv.${item.rank}",
            color = AbyssColors.Orange,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Default,
            modifier = Modifier
                .align(Alignment.TopStart)
                .combinedClickable(
                    onClick = onEditRank,
                    onLongClick = onLongClick
                )
                .padding(2.dp)
        )

        // Top Right: Clock
        Text(
            text = formatHM(calc.fullAt),
            color = if (calc.isFull) AbyssColors.Danger else AbyssColors.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Bottom Center: Values
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$displayCur",
                color = if (isNear) AbyssColors.Danger else AbyssColors.Text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                modifier = Modifier
                    .combinedClickable(
                        onClick = onEditCurrent,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 2.dp)
            )
            Text(
                text = "/",
                color = AbyssColors.Sub,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
            Text(
                text = "${item.max}",
                color = AbyssColors.Blue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IdleCard(
    item: IdleItem,
    now: Long,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calc = calculateIdle(item.durationMin, item.start, now)
    val isClaim = item.state == "claim"
    val isNear = isClaim || isNearFull(calc.remainMs, calc.isFull)

    val strokeColor = when {
        isClaim -> AbyssColors.Ok
        calc.isFull -> AbyssColors.Orange
        else -> Color(0xFFF0A85A)
    }

    val centerText = when {
        isClaim -> "受取"
        calc.isFull -> formatHM(calc.fullAt)
        else -> formatCountdown(calc.remainMs)
    }

    Box(
        modifier = modifier
            .testTag("card_idle_${item.id}")
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AbyssColors.Card)
            .then(
                if (isClaim) {
                    Modifier.drawBehind {
                        val stroke = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        drawRoundRect(
                            color = strokeColor,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                            style = stroke
                        )
                    }
                } else {
                    Modifier.border(1.dp, strokeColor, RoundedCornerShape(10.dp))
                }
            )
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // Top Right: Clock
        Text(
            text = formatHM(calc.fullAt),
            color = if (calc.isFull) AbyssColors.Danger else AbyssColors.Accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // Center: Countdown / Claim text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp)
        ) {
            Text(
                text = centerText,
                color = if (isNear) AbyssColors.Danger else AbyssColors.Text,
                fontSize = if (isClaim) 17.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupCard(
    item: GroupItem,
    now: Long,
    pending40Id: String?,
    onLongClick: () -> Unit,
    onEditGroupName: () -> Unit,
    onCardClick: (TimerCardItem) -> Unit,
    onChildLongClick: (TimerCardItem) -> Unit,
    onEditChildNumber: (TimerCardItem, Boolean) -> Unit,
    onEditChildRank: (AbyssItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("group_${item.id}")
            .clip(RoundedCornerShape(8.dp))
            .background(AbyssColors.Card2.copy(alpha = 0.5f))
            .border(1.dp, AbyssColors.GroupBorder, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Group Header Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onEditGroupName,
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name.ifEmpty { "アカウント" },
                    color = AbyssColors.Sub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Children Grid: side-by-side if 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.children.forEach { child ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (child) {
                            is StamItem -> StamCard(
                                item = child,
                                now = now,
                                onLongClick = { onChildLongClick(child) },
                                onEditCurrent = { onEditChildNumber(child, true) },
                                onEditMax = { onEditChildNumber(child, false) }
                            )
                            is AbyssItem -> AbyssCard(
                                item = child,
                                now = now,
                                isPending40 = pending40Id == child.id,
                                onCardClick = { onCardClick(child) },
                                onLongClick = { onChildLongClick(child) },
                                onEditRank = { onEditChildRank(child) },
                                onEditCurrent = { onEditChildNumber(child, true) }
                            )
                            is IdleItem -> IdleCard(
                                item = child,
                                now = now,
                                onCardClick = { onCardClick(child) },
                                onLongClick = { onChildLongClick(child) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeaderCard(
    item: HeaderItem,
    onEditName: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(item.color))
    } catch (_: Exception) {
        AbyssColors.Accent
    }

    Box(
        modifier = modifier
            .testTag("header_${item.id}")
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEditName,
                onLongClick = onLongClick
            )
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.name.ifEmpty { "見出し(ゲーム名など)" },
                color = parsedColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AbyssColors.Line)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RuleCard(
    item: RuleItem,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(item.color))
    } catch (_: Exception) {
        Color(0xFF52617A)
    }

    Box(
        modifier = modifier
            .testTag("rule_${item.id}")
            .fillMaxWidth()
            .combinedClickable(
                onClick = onLongClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(parsedColor)
        )
    }
}
