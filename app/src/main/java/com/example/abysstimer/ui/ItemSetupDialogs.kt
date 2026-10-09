@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.abysstimer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abysstimer.data.ItemEntity

@Composable
fun AddPanelDialog(
    isGroupMode: Boolean,
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D24),
            border = BorderStroke(1.2.dp, Color(0xFF90A0DD).copy(alpha = 0.65f)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isGroupMode) "タイマーを追加" else "枠を追加",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )

                if (isGroupMode) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1A60A5FA))
                                    .border(BorderStroke(1.dp, Color(0x6660A5FA)), RoundedCornerShape(6.dp))
                                    .pointerDownTap { onSelectType("stam") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("スタミナ", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1AC084FC))
                                    .border(BorderStroke(1.dp, Color(0x66C084FC)), RoundedCornerShape(6.dp))
                                    .pointerDownTap { onSelectType("orb") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("オーブ", color = Color(0xFFC084FC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1AFB923C))
                                    .border(BorderStroke(1.dp, Color(0x66FB923C)), RoundedCornerShape(6.dp))
                                    .pointerDownTap { onSelectType("idle") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("放置報酬", color = Color(0xFFFB923C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1A34D399))
                                    .border(BorderStroke(1.dp, Color(0x6634D399)), RoundedCornerShape(6.dp))
                                    .pointerDownTap { onSelectType("exped") },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("遠征", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x1A5EEAD4))
                            .border(BorderStroke(1.dp, Color(0x665EEAD4)), RoundedCornerShape(6.dp))
                            .pointerDownTap { onSelectType("group") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "アカウント枠",
                            color = Color(0xFF5EEAD4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1AC4B5FD))
                                .border(BorderStroke(1.dp, Color(0x66C4B5FD)), RoundedCornerShape(6.dp))
                                .pointerDownTap { onSelectType("header") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("見出し", color = Color(0xFFC4B5FD), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1A94A3B8))
                                .border(BorderStroke(1.dp, Color(0x6694A3B8)), RoundedCornerShape(6.dp))
                                .pointerDownTap { onSelectType("rule") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("仕切り線", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x1A64748B))
                                .border(BorderStroke(1.dp, Color(0x6664748B)), RoundedCornerShape(6.dp))
                                .pointerDownTap { onSelectType("space") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("空白", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (TimerCreationParams) -> Unit,
    onRequestKeypad: (field: String, initial: String, onCommit: (String) -> Unit) -> Unit = { _, _, _ -> },
    isKeypadActive: (field: String) -> Boolean = { false }
) {
    var selectedColor by remember { mutableStateOf(if (type == "rule") "#52617a" else SHARED_COLORS[0]) }

    var intervalStr by remember { mutableStateOf(if (type == "stam") "5" else "360") }
    var maxStr by remember { mutableStateOf(if (type == "stam") "100" else "4") }
    var chunkStr by remember { mutableStateOf("") }
    var hoursStr by remember {
        mutableStateOf(
            when (type) {
                "orb" -> "6"
                "exped" -> "4"
                else -> "12"
            }
        )
    }
    var minutesStr by remember { mutableStateOf("0") }
    var countMode by remember { mutableStateOf("down") }
    var orbMode by remember { mutableStateOf("down") }

    val typeColor = remember(type, selectedColor) {
        when (type) {
            "stam" -> Color(0xFF90A0DD)
            "orb" -> Color(0xFFB48CFF)
            "idle" -> Color(0xFFFF9F68)
            "exped" -> Color(0xFF70D6B0)
            else -> try {
                Color(android.graphics.Color.parseColor(selectedColor))
            } catch (e: Exception) {
                Color(0xFF9B8BFF)
            }
        }
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D24),
            border = BorderStroke(1.2.dp, typeColor.copy(alpha = 0.70f)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (type) {
                        "stam" -> "スタミナ設定"
                        "orb" -> "オーブ設定"
                        "idle" -> "放置報酬の設定"
                        "exped" -> "遠征タイマーの設定"
                        "header" -> "見出しの文字色"
                        "rule" -> "仕切り線の色"
                        else -> "設定"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )

                when (type) {
                    "stam" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("回復", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    WebSetupInput(
                                        value = intervalStr,
                                        onValueChange = { intervalStr = it },
                                        modifier = Modifier.width(36.dp),
                                        isActive = isKeypadActive("interval"),
                                        onClick = {
                                            onRequestKeypad("interval", intervalStr) { intervalStr = it }
                                        }
                                    )
                                    Text("分", color = Color(0xFF8E96A5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("最大", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = maxStr,
                                    onValueChange = { maxStr = it },
                                    modifier = Modifier.width(44.dp),
                                    isActive = isKeypadActive("max"),
                                    onClick = {
                                        onRequestKeypad("max", maxStr) { maxStr = it }
                                    }
                                )
                            }
                        }

                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("使い切り", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            WebSetupInput(
                                value = chunkStr,
                                onValueChange = { chunkStr = it },
                                modifier = Modifier.width(44.dp),
                                placeholder = "なし",
                                isActive = isKeypadActive("chunk"),
                                onClick = {
                                    onRequestKeypad("chunk", chunkStr) { chunkStr = it }
                                }
                            )
                        }
                    }

                    "orb" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("最大", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = maxStr,
                                    onValueChange = { maxStr = it },
                                    modifier = Modifier.width(36.dp),
                                    isActive = isKeypadActive("max"),
                                    onClick = {
                                        onRequestKeypad("max", maxStr) { maxStr = it }
                                    }
                                )
                            }

                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("回復(時間)", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = hoursStr,
                                    onValueChange = { hoursStr = it },
                                    modifier = Modifier.width(36.dp),
                                    isActive = isKeypadActive("hours"),
                                    onClick = {
                                        onRequestKeypad("hours", hoursStr) { hoursStr = it }
                                    }
                                )
                            }
                        }

                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("消費数", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            WebSetupInput(
                                value = chunkStr,
                                onValueChange = { chunkStr = it },
                                modifier = Modifier.width(44.dp),
                                placeholder = "なし",
                                isActive = isKeypadActive("chunk"),
                                onClick = {
                                    onRequestKeypad("chunk", chunkStr) { chunkStr = it }
                                }
                            )
                        }

                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("方式", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val isOrbDown = orbMode == "down"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isOrbDown) Color(0x1F38BDF8) else Color(0x1FA78BFA))
                                    .border(
                                        BorderStroke(1.dp, if (isOrbDown) Color(0x6638BDF8) else Color(0x66A78BFA)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .pointerDownTap {
                                        orbMode = if (isOrbDown) "up" else "down"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isOrbDown) "▼ 残り時間 (減算)" else "▲ 経過時間 (蓄積)",
                                    color = if (isOrbDown) Color(0xFF38BDF8) else Color(0xFFA78BFA),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    "exped", "idle" -> {
                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("方式", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val isCountDown = countMode == "down"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCountDown) Color(0x1F38BDF8) else Color(0x1F34D399))
                                    .border(
                                        BorderStroke(1.dp, if (isCountDown) Color(0x6638BDF8) else Color(0x6634D399)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .pointerDownTap {
                                        countMode = if (isCountDown) "up" else "down"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isCountDown) "▼ カウントダウン" else "▲ カウントアップ",
                                    color = if (isCountDown) Color(0xFF38BDF8) else Color(0xFF34D399),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("設定", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                WebSetupInput(
                                    value = hoursStr,
                                    onValueChange = { hoursStr = it },
                                    modifier = Modifier.width(36.dp),
                                    isActive = isKeypadActive("hours"),
                                    onClick = {
                                        onRequestKeypad("hours", hoursStr) { hoursStr = it }
                                    }
                                )
                                Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                WebSetupInput(
                                    value = minutesStr,
                                    onValueChange = { minutesStr = it },
                                    modifier = Modifier.width(32.dp),
                                    isActive = isKeypadActive("minutes"),
                                    onClick = {
                                        onRequestKeypad("minutes", minutesStr) { minutesStr = it }
                                    }
                                )
                                Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp)
                            }
                        }
                    }

                    "header", "rule" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val chunkedColors = remember { SHARED_COLORS.chunked(5) }
                            chunkedColors.forEach { rowColors ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    rowColors.forEach { colorHex ->
                                        val color = Color(android.graphics.Color.parseColor(colorHex))
                                        val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(
                                                    width = if (isSelected) 2.2.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color(255, 255, 255, 45),
                                                    shape = CircleShape
                                                )
                                                .pointerDownTap { selectedColor = colorHex }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = Color(255, 255, 255, 38),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x14FF6B6B))
                            .border(BorderStroke(1.dp, Color(0x66FF6B6B)), RoundedCornerShape(6.dp))
                            .pointerDownTap { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "やめる",
                            color = Color(0xFFFF6B6B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF9B8BFF))
                            .pointerDownTap {
                                val params = when (type) {
                                    "stam" -> TimerCreationParams(
                                        intervalMin = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5,
                                        max = maxStr.toIntOrNull()?.coerceAtLeast(1) ?: 100,
                                        useChunk = chunkStr.toIntOrNull()
                                    )
                                    "orb" -> {
                                        val hours = hoursStr.toIntOrNull()?.coerceAtLeast(1) ?: 6
                                        TimerCreationParams(
                                            intervalMin = hours * 60,
                                            max = maxStr.toIntOrNull()?.coerceIn(1, 99) ?: 4,
                                            useChunk = chunkStr.toIntOrNull(),
                                            orbMode = orbMode
                                        )
                                    }
                                    "idle", "exped" -> {
                                        val h = hoursStr.toIntOrNull() ?: 0
                                        val m = minutesStr.toIntOrNull() ?: 0
                                        TimerCreationParams(
                                            durationMin = h * 60 + m,
                                            countMode = countMode
                                        )
                                    }
                                    "header", "rule" -> TimerCreationParams(
                                        color = selectedColor
                                    )
                                    else -> TimerCreationParams()
                                }
                                onConfirm(params)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "追加",
                            color = Color(0xFF100C26),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullColorPickerDialog(
    title: String = "見出しの色",
    selectedColor: String,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSelectColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit
) {
    var showRgbMixer by remember { mutableStateOf(false) }
    var redVal by remember { mutableStateOf(128) }
    var greenVal by remember { mutableStateOf(128) }
    var blueVal by remember { mutableStateOf(128) }

    BackHandler(enabled = showRgbMixer) {
        showRgbMixer = false
    }

    val activeBorderColor = remember(selectedColor) {
        selectedColor.toComposeColor(Color(0xFF9B8BFF))
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.2.dp, activeBorderColor.copy(alpha = 0.70f)),
            modifier = Modifier.width(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showRgbMixer) "色をタップしてRGBに取り込み" else "プリセットカラー",
                        fontSize = 11.5.sp,
                        fontWeight = if (showRgbMixer) FontWeight.Bold else FontWeight.Normal,
                        color = if (showRgbMixer) Color(0xFFFFD166) else Color(0xFF8A8EA3)
                    )
                    if (showRgbMixer) {
                        Text(
                            text = "編集中",
                            fontSize = 10.5.sp,
                            color = Color(0xFFFFD166),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val presetRows = FULL_PRESET_COLORS.chunked(7)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            row.forEach { colorHex ->
                                val color = Color(android.graphics.Color.parseColor(colorHex))
                                val isSelected = selectedColor.equals(colorHex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .pointerDownTap {
                                            if (showRgbMixer) {
                                                try {
                                                    val parsed = android.graphics.Color.parseColor(colorHex)
                                                    redVal = android.graphics.Color.red(parsed)
                                                    greenVal = android.graphics.Color.green(parsed)
                                                    blueVal = android.graphics.Color.blue(parsed)
                                                } catch (e: Exception) {}
                                            } else {
                                                onSelectColor(colorHex)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(255, 255, 255, 20), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "カスタム保存枠 (6枠)",
                        fontSize = 11.sp,
                        color = Color(0xFF8A8EA3)
                    )
                    Text(
                        text = "＋でRGB作成",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD166),
                        modifier = Modifier.pointerDownTap {
                            try {
                                val parsed = android.graphics.Color.parseColor(selectedColor)
                                redVal = android.graphics.Color.red(parsed)
                                greenVal = android.graphics.Color.green(parsed)
                                blueVal = android.graphics.Color.blue(parsed)
                            } catch (e: Exception) {
                                redVal = 128; greenVal = 128; blueVal = 128
                            }
                            showRgbMixer = !showRgbMixer
                        }
                    )
                }

                if (showRgbMixer) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14161C), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        val hexStr = String.format("#%02x%02x%02x", redVal, greenVal, blueVal)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(redVal, greenVal, blueVal))
                                    .border(1.dp, Color(255, 255, 255, 40), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = hexStr.uppercase(),
                                    color = if ((redVal * 299 + greenVal * 587 + blueVal * 114) / 1000 > 128) Color.Black else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22252D))
                                    .border(1.dp, Color(255, 255, 255, 20), RoundedCornerShape(6.dp))
                                    .pointerDownTap { showRgbMixer = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("▲", color = Color(0xFF8A8EA3), fontSize = 12.sp)
                            }
                        }

                        ColorSliderRow(label = "R", accentColor = Color(0xFFEF4444), value = redVal, onValueChange = { redVal = it })
                        ColorSliderRow(label = "G", accentColor = Color(0xFF22C55E), value = greenVal, onValueChange = { greenVal = it })
                        ColorSliderRow(label = "B", accentColor = Color(0xFF3B82F6), value = blueVal, onValueChange = { blueVal = it })

                        Text(
                            text = "枠をタップしてカスタム枠に保存・適用",
                            fontSize = 10.sp,
                            color = Color(0xFF8A8EA3),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val slots = (0 until 6).map { i -> customColors.getOrElse(i) { "#52617A" } }
                            slots.forEachIndexed { idx, slotHex ->
                                val slotColor = Color(android.graphics.Color.parseColor(slotHex))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(slotColor)
                                        .border(1.5.dp, Color(255, 255, 255, 60), CircleShape)
                                        .pointerDownTap {
                                            val mutable = customColors.toMutableList()
                                            while (mutable.size < 6) {
                                                mutable.add("#52617A")
                                            }
                                            mutable[idx] = hexStr
                                            onSaveCustomColors(mutable)
                                            onSelectColor(hexStr)
                                            showRgbMixer = false
                                        }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSelectColor(hexStr)
                                showRgbMixer = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF343B4E)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().height(32.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("保存せずこの色だけ適用", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val slots = (0 until 6).map { i -> customColors.getOrElse(i) { "#52617A" } }
                    slots.forEachIndexed { idx, slotHex ->
                        val color = Color(android.graphics.Color.parseColor(slotHex))
                        val isSelected = selectedColor.equals(slotHex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .pointerDownTap {
                                    if (showRgbMixer) {
                                        try {
                                            val parsed = android.graphics.Color.parseColor(slotHex)
                                            redVal = android.graphics.Color.red(parsed)
                                            greenVal = android.graphics.Color.green(parsed)
                                            blueVal = android.graphics.Color.blue(parsed)
                                        } catch (e: Exception) {}
                                    } else {
                                        onSelectColor(slotHex)
                                    }
                                }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22252D))
                            .border(1.dp, if (showRgbMixer) Color(0xFFFFD166) else Color(0xFF8A8EA3), CircleShape)
                            .pointerDownTap {
                                if (!showRgbMixer) {
                                    try {
                                        val parsed = android.graphics.Color.parseColor(selectedColor)
                                        redVal = android.graphics.Color.red(parsed)
                                        greenVal = android.graphics.Color.green(parsed)
                                        blueVal = android.graphics.Color.blue(parsed)
                                    } catch (e: Exception) {
                                        redVal = 128; greenVal = 128; blueVal = 128
                                    }
                                }
                                showRgbMixer = !showRgbMixer
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showRgbMixer) "✕" else "＋",
                            color = if (showRgbMixer) Color(0xFFFFD166) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22252D))
                        .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                        .pointerDownTap { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("閉じる", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditItemDialog(
    entity: ItemEntity,
    isCompact: Boolean = false,
    customColors: List<String>,
    groupName: String = "",
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onUpdateSettings: (TimerSettingsUpdate) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onStartMove: (() -> Unit)? = null,
    onCloneGroup: (() -> Unit)? = null,
    onAddChildTimer: (() -> Unit)? = null,
    onRequestKeypad: (field: String, initial: String, onCommit: (String) -> Unit) -> Unit = { _, _, _ -> },
    isKeypadActive: (field: String) -> Boolean = { false }
) {
    when (entity.type) {
        "group" -> {
            GroupToastDialog(
                entity = entity,
                customColors = customColors,
                onDismiss = onDismiss,
                onSaveName = onSaveName,
                onSaveColor = onSaveColor,
                onSaveCustomColors = onSaveCustomColors,
                onUpdateLayout = { newLayout ->
                    onUpdateSettings(TimerSettingsUpdate(layout = newLayout))
                },
                onDelete = onDelete,
                onStartMove = { onStartMove?.invoke() },
                onCloneGroup = { onCloneGroup?.invoke() },
                onAddChildTimer = { onAddChildTimer?.invoke() }
            )
        }
        "header" -> {
            HeaderToastDialog(
                entity = entity,
                isCompact = isCompact,
                customColors = customColors,
                onDismiss = onDismiss,
                onSaveName = onSaveName,
                onSaveColor = onSaveColor,
                onSaveCustomColors = onSaveCustomColors,
                onToggleFoldLock = { isLocked ->
                    onUpdateSettings(TimerSettingsUpdate(foldLock = isLocked))
                },
                onUpdateLayout = { newLayout ->
                    onUpdateSettings(TimerSettingsUpdate(layout = newLayout))
                },
                onDelete = onDelete,
                onStartMove = { onStartMove?.invoke() }
            )
        }
        "rule" -> {
            RuleToastDialog(
                entity = entity,
                customColors = customColors,
                onDismiss = onDismiss,
                onSaveColor = onSaveColor,
                onSaveCustomColors = onSaveCustomColors,
                onDelete = onDelete,
                onStartMove = { onStartMove?.invoke() }
            )
        }
        "space" -> {
            SpaceToastDialog(
                entity = entity,
                onDismiss = onDismiss,
                onDelete = onDelete,
                onStartMove = { onStartMove?.invoke() }
            )
        }
        "stam" -> {
            StaminaToastDialog(
                entity = entity,
                groupName = groupName,
                onDismiss = onDismiss,
                onUpdateSettings = onUpdateSettings,
                onDelete = onDelete,
                onRequestKeypad = onRequestKeypad,
                isKeypadActive = isKeypadActive
            )
        }
        "orb" -> {
            OrbToastDialog(
                entity = entity,
                groupName = groupName,
                onDismiss = onDismiss,
                onUpdateSettings = onUpdateSettings,
                onDelete = onDelete,
                onRequestKeypad = onRequestKeypad,
                isKeypadActive = isKeypadActive
            )
        }
        "idle", "exped" -> {
            IdleExpedToastDialog(
                entity = entity,
                groupName = groupName,
                onDismiss = onDismiss,
                onUpdateSettings = onUpdateSettings,
                onDelete = onDelete,
                onRequestKeypad = onRequestKeypad,
                isKeypadActive = isKeypadActive
            )
        }
    }
}
