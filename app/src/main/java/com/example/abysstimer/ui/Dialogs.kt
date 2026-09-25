package com.example.abysstimer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.activity.compose.BackHandler
import android.view.WindowManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.abysstimer.data.ItemEntity

val LocalFocusTracker = compositionLocalOf<(Boolean) -> Unit> { {} }

fun Modifier.pointerDownTap(
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(onTap) {
    detectTapGestures(
        onPress = {
            onTap()
        }
    )
}

@Composable
fun FastDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let { w ->
                w.setWindowAnimations(0)
                w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                w.setDimAmount(0f)
                w.setBackgroundDrawableResource(android.R.color.transparent)
            }
            onDispose {}
        }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        var isAnyFocusedInDialog by remember { mutableStateOf(false) }

        CompositionLocalProvider(LocalFocusTracker provides { isAnyFocusedInDialog = it }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .pointerInput(isAnyFocusedInDialog) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            if (isAnyFocusedInDialog && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                focusManager.clearFocus()
                                event.changes.forEach { it.consume() }
                            } else if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                // Background tap logic
                                // We can't easily detect if we are tapping the background here in Initial pass without checking coordinates.
                                // But detectTapGestures on background (handled later) will handle dismissal.
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            onDismissRequest()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                        }
                    )
                }
            ) {
                content()
            }
        }
    }
}
}

val SHARED_COLORS = listOf(
    "#9b8bff", "#b48cff", "#6fc7ff", "#70d6b0", "#ffd166", "#ff9f68", "#ff7b9c", "#d7dbe7", "#52617a"
)

val FULL_PRESET_COLORS = listOf(
    "#FFFFFF", "#FF4D4D", "#FF8000", "#FFD700", "#2ECE6D", "#00BFFF", "#4169E1",
    "#9B8BFF", "#FF69B4", "#FF4500", "#00FA9A", "#6495ED", "#8A2BE2", "#52617A"
)

// --- Add Panel Dialog (Web App Spec) ---
@Composable
fun AddPanelDialog(
    isGroupMode: Boolean,
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1E26),
            border = BorderStroke(1.dp, Color(255, 255, 255, 36)),
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
                    // 2x2 Grid (Stamina, Orb / Idle, Exped)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            // スタミナ
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
                            // オーブ
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
                            // 放置報酬
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
                            // 遠征
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
                    // アカウント枠 (1行フル幅)
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

                    // 見出し | 仕切り線 (2列)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // 見出し
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

                        // 仕切り線
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
                    }
                }
            }
        }
    }
}

// --- Web Setup Field Helpers ---
@Composable
private fun WebSetupFieldContainer(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .height(34.dp)
            .background(Color(0xFF242836), RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, Color(255, 255, 255, 31)), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
private fun WebSetupInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.width(44.dp),
    placeholder: String = ""
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    var localText by remember(value) { mutableStateOf(value) }
    var originalValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (!isFocused) {
            localText = value
            originalValue = value
        }
    }

    BasicTextField(
        value = localText,
        onValueChange = { newVal ->
            if (newVal.all { it.isDigit() }) {
                localText = newVal
                onValueChange(newVal)
            }
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        ),
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .height(24.dp)
            .background(Color(0x59000000), RoundedCornerShape(5.dp))
            .border(BorderStroke(1.dp, Color(255, 255, 255, 46)), RoundedCornerShape(5.dp))
            .padding(horizontal = 4.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    if (!isFocused) {
                        isFocused = true
                        originalValue = if (localText.isNotEmpty()) localText else value
                        if (localText.isNotEmpty()) {
                            localText = ""
                        }
                    }
                } else {
                    if (isFocused) {
                        isFocused = false
                        if (localText.isEmpty() && originalValue.isNotEmpty()) {
                            localText = originalValue
                            onValueChange(originalValue)
                        } else if (localText.isNotEmpty()) {
                            onValueChange(localText)
                        }
                    }
                }
            },
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val ph = if (localText.isEmpty()) (if (placeholder.isNotEmpty()) placeholder else originalValue) else ""
                if (ph.isNotEmpty()) {
                    Text(ph, color = Color(0xFF606775), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                innerTextField()
            }
        }
    )
}

// --- Setup Dialog (Matching Web App #setupPanel exactly) ---
@Composable
fun SetupDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
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
    var countMode by remember { mutableStateOf("down") } // "down" or "up"
    var orbMode by remember { mutableStateOf("down") } // "down" or "up"

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1C1E26),
            border = BorderStroke(1.dp, Color(255, 255, 255, 36)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, top = 12.dp, end = 14.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
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

                // Fields area
                when (type) {
                    "stam" -> {
                        // Row 1: 回復 / 最大
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
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text("分", color = Color(0xFF8E96A5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("最大", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = maxStr,
                                    onValueChange = { maxStr = it },
                                    modifier = Modifier.width(44.dp)
                                )
                            }
                        }

                        // Row 2 (wide): 使い切り
                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("使い切り", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            WebSetupInput(
                                value = chunkStr,
                                onValueChange = { chunkStr = it },
                                modifier = Modifier.width(44.dp),
                                placeholder = "なし"
                            )
                        }
                    }

                    "orb" -> {
                        // Row 1: 最大 / 回復(時間)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("最大", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = maxStr,
                                    onValueChange = { maxStr = it },
                                    modifier = Modifier.width(36.dp)
                                )
                            }

                            WebSetupFieldContainer(modifier = Modifier.weight(1f)) {
                                Text("回復(時間)", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                WebSetupInput(
                                    value = hoursStr,
                                    onValueChange = { hoursStr = it },
                                    modifier = Modifier.width(36.dp)
                                )
                            }
                        }

                        // Row 2 (wide): 消費数
                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("消費数", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            WebSetupInput(
                                value = chunkStr,
                                onValueChange = { chunkStr = it },
                                modifier = Modifier.width(44.dp),
                                placeholder = "なし"
                            )
                        }

                        // Row 3 (wide): 方式
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
                        // Row 1 (wide): 方式
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

                        // Row 2 (wide): 設定 (h / m)
                        WebSetupFieldContainer(modifier = Modifier.fillMaxWidth()) {
                            Text("設定", color = Color(0xFFA0A6B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                WebSetupInput(
                                    value = hoursStr,
                                    onValueChange = { hoursStr = it },
                                    modifier = Modifier.width(36.dp)
                                )
                                Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                WebSetupInput(
                                    value = minutesStr,
                                    onValueChange = { minutesStr = it },
                                    modifier = Modifier.width(32.dp)
                                )
                                Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp)
                            }
                        }
                    }

                    "header", "rule" -> {
                        // Color Palette (9 shared colors split into 2 beautiful rows to prevent squeezing/distortion)
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

                // Action Buttons: やめる | 追加 (1fr : 1.2fr matching web app #setupActions)
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
                                val data = mutableMapOf<String, Any>()
                                if (type == "stam") {
                                    data["intervalMin"] = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5
                                    data["max"] = maxStr.toIntOrNull()?.coerceAtLeast(1) ?: 100
                                    chunkStr.toIntOrNull()?.let { data["useChunk"] = it }
                                } else if (type == "orb") {
                                    val hours = hoursStr.toIntOrNull()?.coerceAtLeast(1) ?: 6
                                    data["intervalMin"] = hours * 60
                                    data["max"] = maxStr.toIntOrNull()?.coerceAtIn(1, 99) ?: 4
                                    chunkStr.toIntOrNull()?.let { data["useChunk"] = it }
                                    data["orbMode"] = orbMode
                                } else if (type == "idle" || type == "exped") {
                                    val h = hoursStr.toIntOrNull() ?: 0
                                    val m = minutesStr.toIntOrNull() ?: 0
                                    data["durationMin"] = h * 60 + m
                                    data["countMode"] = countMode
                                } else if (type == "header" || type == "rule") {
                                    data["color"] = selectedColor
                                }
                                onConfirm(data)
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

// Shared Foldable Bottom Action for Toast Dialogs (Ultra-compact collapsed '…' with zero wasted padding, expanding seamlessly)
@Composable
fun FoldableToastFooter(
    onDelete: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    if (isConfirmingDelete) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(3000)
            isConfirmingDelete = false
        }
    }

    if (!isExpanded) {
        // Ultra-compact trigger button: zero vertical margin, height 18dp, fits tightly
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .pointerDownTap { isExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⋯",
                color = Color(0xFF6E7387),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.15.em
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Extra actions if any (like duplicate group)
            extraContent?.invoke()

            // Unified elegant Delete button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(
                        1.dp,
                        if (isConfirmingDelete) Color(0xFFFF5252) else Color(0x33FF5252),
                        RoundedCornerShape(6.dp)
                    )
                    .background(if (isConfirmingDelete) Color(0xFF451818) else Color(0xFF221417))
                    .pointerDownTap {
                        if (isConfirmingDelete) {
                            onDelete()
                        } else {
                            isConfirmingDelete = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isConfirmingDelete) "本当に削除しますか？" else "削除",
                    color = if (isConfirmingDelete) Color(0xFFFF6B6B) else Color(0xFFEF5350),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper int coercion
private fun Int.coerceAtIn(min: Int, max: Int): Int {
    return Math.max(min, Math.min(max, this))
}

// --- Account Group Toast Dialog (Matching Screenshot 2) ---
@Composable
fun GroupToastDialog(
    entity: ItemEntity,
    customColors: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit = {},
    onUpdateLayout: (String) -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit,
    onCloneGroup: () -> Unit,
    onAddChildTimer: () -> Unit,
    onAddGroupBelow: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var nameStr by remember { mutableStateOf(entity.name) }
    var showColorPicker by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val currentLayout = entity.layout ?: "regular"

    BackHandler(enabled = isEditingName || isConfirmingDelete) {
        if (isEditingName) isEditingName = false
        else if (isConfirmingDelete) isConfirmingDelete = false
    }

    if (showColorPicker) {
        FullColorPickerDialog(
            title = "枠の色",
            selectedColor = entity.color ?: "#9B8BFF",
            customColors = customColors,
            onDismiss = { showColorPicker = false },
            onSelectColor = { newHex ->
                onSaveColor(newHex)
                showColorPicker = false
            },
            onSaveCustomColors = onSaveCustomColors
        )
        return
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 15)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingName) {
                    Text("アカウント名を変更", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    ClearOnFocusTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = "アカウント名",
                        keyboardType = KeyboardType.Text,
                        isDigitOnly = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isEditingName = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("キャンセル", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onSaveName(nameStr)
                                isEditingName = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF53459A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Row 1: アカウント名を変更 (Full Width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                            .pointerDownTap { isEditingName = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("アカウント名を変更", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Row 2: 色 ⭕ | 配置
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 色 ⭕
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22252D))
                                .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                                .pointerDownTap { showColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("色 ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF9B8BFF)
                                        )
                                )
                            }
                        }

                        // 配置：三 1行 or ⊞ 2x2
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22252D))
                                .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    val nextLayout = if (currentLayout == "2x2") "regular" else "2x2"
                                    onUpdateLayout(nextLayout)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentLayout == "2x2") "配置：⊞ 2x2" else "配置：三 1行",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Row 3: タイマー追加 (ティール系アクセント) | 枠を追加 (エメラルド系アクセント)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A5EEAD4))
                                .border(BorderStroke(1.dp, Color(0x595EEAD4)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    onDismiss()
                                    onAddChildTimer()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("タイマー追加", color = Color(0xFF5EEAD4), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A34D399))
                                .border(BorderStroke(1.dp, Color(0x5934D399)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    onDismiss()
                                    onAddGroupBelow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("枠を追加", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row 4: 移動 (単独ライン・アンバー系アクセント)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFBBF24))
                            .border(BorderStroke(1.dp, Color(0x59FBBF24)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                onDismiss()
                                onStartMove()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("移動", color = Color(0xFFFBBF24), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Row 5: ⋯ 折りたたみ（枠を複製 ＋ 削除）
                    FoldableToastFooter(
                        onDelete = {
                            onDismiss()
                            onDelete()
                        },
                        extraContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22252D))
                                    .border(BorderStroke(1.dp, Color(255, 255, 255, 16)), RoundedCornerShape(6.dp))
                                    .pointerDownTap {
                                        onDismiss()
                                        onCloneGroup()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("枠を複製", color = Color(0xFFC5C8D4), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- Header Toast Dialog (Matching Screenshot 2) ---
@Composable
fun HeaderToastDialog(
    entity: ItemEntity,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onToggleFoldLock: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit,
    onAddGroupBelow: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var nameStr by remember { mutableStateOf(entity.name) }
    var showColorPicker by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = isEditingName || isConfirmingDelete) {
        if (isEditingName) isEditingName = false
        else if (isConfirmingDelete) isConfirmingDelete = false
    }

    if (showColorPicker) {
        FullColorPickerDialog(
            title = "見出しの色",
            selectedColor = entity.color ?: "#9B8BFF",
            customColors = customColors,
            onDismiss = { showColorPicker = false },
            onSelectColor = { newHex ->
                onSaveColor(newHex)
                showColorPicker = false
            },
            onSaveCustomColors = onSaveCustomColors
        )
        return
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 15)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingName) {
                    Text("見出し名を変更", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    ClearOnFocusTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = "見出し名",
                        keyboardType = KeyboardType.Text,
                        isDigitOnly = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isEditingName = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("キャンセル", color = Color.White, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                onSaveName(nameStr)
                                isEditingName = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF53459A)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("保存", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Row 1: 見出し名を変更 (Full Width)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                            .pointerDownTap { isEditingName = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("見出し名を変更", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Row 2: 色 ⭕ | 折りたたみ：🔒 / 🔓
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 色 ⭕
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22252D))
                                .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                                .pointerDownTap { showColorPicker = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("色 ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF9B8BFF)
                                        )
                                )
                            }
                        }

                        // 折りたたみ : 🔒 / 🔓
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF22252D))
                                .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    onToggleFoldLock(!entity.foldLock)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (entity.foldLock) "折りたたみ：🔒" else "折りたたみ：🔓",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Row 3: 枠を追加 (エメラルド系アクセント) | 移動 (アンバー系アクセント)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A34D399))
                                .border(BorderStroke(1.dp, Color(0x5934D399)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    onDismiss()
                                    onAddGroupBelow()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("枠を追加", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1AFBBF24))
                                .border(BorderStroke(1.dp, Color(0x59FBBF24)), RoundedCornerShape(8.dp))
                                .pointerDownTap {
                                    onDismiss()
                                    onStartMove()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("移動", color = Color(0xFFFBBF24), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row 4: ⋯ 折りたたみアクション（削除）
                    FoldableToastFooter(
                        onDelete = {
                            onDismiss()
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

// --- Full Color Picker Dialog (Matching Screenshot 3) ---
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

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 15)),
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

                // 14 Presets in 2 rows of 7 (Matching Screenshot 3)
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
                                        .pointerDownTap { onSelectColor(colorHex) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(255, 255, 255, 20), thickness = 1.dp)

                // Custom Slot Row Subheader
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

                        // Top Preview + Quick Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Large preview badge with hex string
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

                            // Close / fold RGB mixer button
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

                        // Compact RGB Sliders
                        ColorSliderRow(label = "R", accentColor = Color(0xFFEF4444), value = redVal, onValueChange = { redVal = it })
                        ColorSliderRow(label = "G", accentColor = Color(0xFF22C55E), value = greenVal, onValueChange = { greenVal = it })
                        ColorSliderRow(label = "B", accentColor = Color(0xFF3B82F6), value = blueVal, onValueChange = { blueVal = it })

                        // Slot save title
                        Text(
                            text = "枠をタップしてカスタム枠に保存・適用",
                            fontSize = 10.sp,
                            color = Color(0xFF8A8EA3),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        )

                        // 6 Mini Slot targets inside mixer
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

                        // Apply without saving button
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

                // Custom 6 Slots + Plus Dotted Button
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
                                .pointerDownTap { onSelectColor(slotHex) }
                        )
                    }

                    // Plus Dotted Button
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

                // Close Button
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

// --- Edit Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemDialog(
    entity: ItemEntity,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onUpdateSettings: (Map<String, Any?>) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onStartMove: (() -> Unit)? = null,
    onCloneGroup: (() -> Unit)? = null,
    onAddChildTimer: (() -> Unit)? = null,
    onAddGroupBelow: (() -> Unit)? = null
) {
    if (entity.type == "group") {
        GroupToastDialog(
            entity = entity,
            customColors = customColors,
            onDismiss = onDismiss,
            onSaveName = onSaveName,
            onSaveColor = onSaveColor,
            onSaveCustomColors = onSaveCustomColors,
            onUpdateLayout = { newLayout ->
                onUpdateSettings(mapOf("layout" to newLayout))
            },
            onDelete = onDelete,
            onStartMove = { onStartMove?.invoke() },
            onCloneGroup = { onCloneGroup?.invoke() },
            onAddChildTimer = { onAddChildTimer?.invoke() },
            onAddGroupBelow = { onAddGroupBelow?.invoke() }
        )
        return
    }

    if (entity.type == "header") {
        HeaderToastDialog(
            entity = entity,
            customColors = customColors,
            onDismiss = onDismiss,
            onSaveName = onSaveName,
            onSaveColor = onSaveColor,
            onSaveCustomColors = onSaveCustomColors,
            onToggleFoldLock = { isLocked ->
                onUpdateSettings(mapOf("foldLock" to isLocked))
            },
            onDelete = onDelete,
            onStartMove = { onStartMove?.invoke() },
            onAddGroupBelow = { onAddGroupBelow?.invoke() }
        )
        return
    }

    if (entity.type == "rule") {
        RuleToastDialog(
            entity = entity,
            customColors = customColors,
            onDismiss = onDismiss,
            onSaveColor = onSaveColor,
            onSaveCustomColors = onSaveCustomColors,
            onDelete = onDelete,
            onStartMove = { onStartMove?.invoke() },
            onAddGroupBelow = { onAddGroupBelow?.invoke() }
        )
        return
    }

    if (entity.type == "stam") {
        StaminaToastDialog(
            entity = entity,
            onDismiss = onDismiss,
            onUpdateSettings = onUpdateSettings,
            onDelete = onDelete
        )
        return
    }

    if (entity.type == "orb") {
        OrbToastDialog(
            entity = entity,
            onDismiss = onDismiss,
            onUpdateSettings = onUpdateSettings,
            onDelete = onDelete
        )
        return
    }

    if (entity.type == "idle" || entity.type == "exped") {
        IdleExpedToastDialog(
            entity = entity,
            onDismiss = onDismiss,
            onUpdateSettings = onUpdateSettings,
            onDelete = onDelete
        )
        return
    }

    var nameStr by remember(entity.id) { mutableStateOf(entity.name) }
    var selectedColor by remember(entity.id) { mutableStateOf(entity.color ?: SHARED_COLORS[0]) }

    // Type specific settings
    var intervalStr by remember(entity.id) { mutableStateOf(entity.intervalMin.toString()) }
    var maxStr by remember(entity.id) { mutableStateOf(entity.max.toString()) }
    var chunkStr by remember(entity.id) { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var durationHoursStr by remember(entity.id) { mutableStateOf((entity.durationMin / 60).toString()) }
    var durationMinutesStr by remember(entity.id) { mutableStateOf((entity.durationMin % 60).toString()) }
    var countMode by remember(entity.id) { mutableStateOf(entity.countMode ?: "down") }
    var orbMode by remember(entity.id) { mutableStateOf(entity.orbMode ?: "down") }
    var layoutMode by remember(entity.id) { mutableStateOf(entity.layout ?: "regular") }
    var isFoldLocked by remember(entity.id) { mutableStateOf(entity.foldLock) }

    val initialOrbMinutes = remember(entity.id, orbMode) {
        if (entity.type != "orb") return@remember 0
        val now = System.currentTimeMillis()
        val isUp = orbMode == "up"
        val oneOrbMin = Math.max(1, entity.intervalMin)
        if (entity.current >= entity.max) {
            if (isUp) entity.max * oneOrbMin else 0
        } else {
            val elapsed = Math.max(0L, now - entity.start)
            val recovered = (elapsed / (oneOrbMin * 60000L)).toInt()
            val cur = Math.min(entity.max, entity.current + recovered)
            if (cur >= entity.max) {
                if (isUp) entity.max * oneOrbMin else 0
            } else {
                val nextInMs = (oneOrbMin * 60000L) - (elapsed % (oneOrbMin * 60000L))
                val need = entity.max - cur
                val remainMs = (need - 1) * (oneOrbMin * 60000L) + nextInMs
                if (isUp) {
                    (cur * oneOrbMin + Math.max(0L, (oneOrbMin * 60000L) - nextInMs) / 60000L).toInt()
                } else {
                    Math.ceil(remainMs / 60000.0).toInt()
                }
            }
        }
    }
    var orbHoursStr by remember(entity.id, initialOrbMinutes) { mutableStateOf((initialOrbMinutes / 60).toString()) }
    var orbMinutesStr by remember(entity.id, initialOrbMinutes) { mutableStateOf((initialOrbMinutes % 60).toString()) }

    val initialIdleMinutes = remember(entity.id, countMode) {
        if (entity.type != "idle" && entity.type != "exped") return@remember 0
        val now = System.currentTimeMillis()
        val isUp = countMode == "up"
        val durMs = Math.max(1, entity.durationMin) * 60000L
        val elapsed = Math.max(0L, now - entity.start)
        val remainMs = Math.max(0L, durMs - elapsed)
        val curMin = if (isUp) {
            elapsed / 60000L
        } else {
            Math.ceil(remainMs / 60000.0).toLong()
        }
        curMin.toInt()
    }
    var idleCurHoursStr by remember(entity.id, initialIdleMinutes) { mutableStateOf((initialIdleMinutes / 60).toString()) }
    var idleCurMinutesStr by remember(entity.id, initialIdleMinutes) { mutableStateOf((initialIdleMinutes % 60).toString()) }

    // Color Mixer States
    var showColorMixer by remember { mutableStateOf(false) }
    var redVal by remember { mutableStateOf(128) }
    var greenVal by remember { mutableStateOf(128) }
    var blueVal by remember { mutableStateOf(128) }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF15151F),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "設定を編集",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFFFF6B6B))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete item")
                    }
                }

                // Name edit (for everything except rules)
                if (entity.type != "rule") {
                    ClearOnFocusTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = "名前/見出し",
                        keyboardType = KeyboardType.Text,
                        isDigitOnly = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                /*
                // [UNUSED] Legacy Up/Down buttons - Hidden per user request
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("表示位置の調整", color = Color(0xFF8A8EA3), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (onStartMove != null) {
                            Button(
                                onClick = {
                                    onDismiss()
                                    onStartMove()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("移動先を選択", color = Color(0xFF6FC7FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        IconButton(
                            onClick = onMoveUp,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF2A2A3A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                        }
                        IconButton(
                            onClick = onMoveDown,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFF2A2A3A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                        }
                    }
                }
                */

                // New Position Reordering (Simplified to only "Select Move Target")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("表示位置の調整", color = Color(0xFF8A8EA3), fontSize = 13.sp)
                    if (onStartMove != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onStartMove()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("移動先を選択", color = Color(0xFF6FC7FF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = Color(0xFF2A2A3A), thickness = 1.dp)

                // Type-Specific Fields
                when (entity.type) {
                    "stam" -> {
                        ClearOnFocusTextField(
                            value = intervalStr,
                            onValueChange = { intervalStr = it },
                            label = "回復時間 (分)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        ClearOnFocusTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = "最大スタミナ",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Add Buttons for Max Stamina (+1, +5, +10) matching toast-fields.js
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 5, 10).forEach { delta ->
                                Button(
                                    onClick = {
                                        val currentVal = maxStr.toIntOrNull() ?: entity.max
                                        val newVal = Math.min(999, currentVal + delta)
                                        maxStr = newVal.toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 4.dp)
                                ) {
                                    Text("+$delta", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        ClearOnFocusTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = "使い切り消費数 (任意)",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "orb" -> {
                        ClearOnFocusTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = "最大個数",
                            modifier = Modifier.fillMaxWidth()
                        )

                        var hoursInterval by remember { mutableStateOf((entity.intervalMin / 60).toString()) }
                        ClearOnFocusTextField(
                            value = hoursInterval,
                            onValueChange = {
                                hoursInterval = it
                                val h = it.toIntOrNull() ?: 6
                                intervalStr = (h * 60).toString()
                            },
                            label = "回復間隔 (時間)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        ClearOnFocusTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = "消費数 (任意)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Direct Elapsed / Remaining time edit
                        val orbTimeLabel = if (orbMode == "up") "蓄積時間" else "全回復"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClearOnFocusTextField(
                                value = orbHoursStr,
                                onValueChange = { orbHoursStr = it },
                                label = "$orbTimeLabel (時間)",
                                modifier = Modifier.weight(1f)
                            )

                            ClearOnFocusTextField(
                                value = orbMinutesStr,
                                onValueChange = { orbMinutesStr = it },
                                label = "$orbTimeLabel (分)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("表示方式", color = Color.White, fontSize = 14.sp)
                            Button(
                                onClick = {
                                    orbMode = if (orbMode == "down") "up" else "down"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
                            ) {
                                Text(
                                    text = if (orbMode == "down") "▼ 残り時間(減算)" else "▲ 経過時間(蓄積)",
                                    color = Color.White
                                )
                            }
                        }
                    }
                    "idle", "exped" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("カウント方式", color = Color.White, fontSize = 14.sp)
                            Button(
                                onClick = {
                                    countMode = if (countMode == "down") "up" else "down"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
                            ) {
                                Text(
                                    text = if (countMode == "down") "▼ カウントダウン" else "▲ カウントアップ",
                                    color = Color.White
                                )
                            }
                        }

                        Text("設定時間", color = Color(0xFF8A8EA3), fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClearOnFocusTextField(
                                value = durationHoursStr,
                                onValueChange = { durationHoursStr = it },
                                label = "時間 (h)",
                                modifier = Modifier.weight(1f)
                            )

                            ClearOnFocusTextField(
                                value = durationMinutesStr,
                                onValueChange = { durationMinutesStr = it },
                                label = "分 (m)",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val progressLabel = if (countMode == "up") "経過時間" else "残り時間"
                        Text(progressLabel, color = Color(0xFF8A8EA3), fontSize = 12.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ClearOnFocusTextField(
                                value = idleCurHoursStr,
                                onValueChange = { idleCurHoursStr = it },
                                label = "時間 (h)",
                                modifier = Modifier.weight(1f)
                            )

                            ClearOnFocusTextField(
                                value = idleCurMinutesStr,
                                onValueChange = { idleCurMinutesStr = it },
                                label = "分 (m)",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    "group" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Regular / 2x2 grid toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("レイアウト", color = Color.White, fontSize = 14.sp)
                                Button(
                                    onClick = {
                                        layoutMode = if (layoutMode == "regular") "2x2" else "regular"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A))
                                ) {
                                    Text(
                                        text = if (layoutMode == "regular") "▼ 横一列" else "▲ 2x2グリッド",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Divider(color = Color(0xFF2A2A3A), thickness = 1.dp)

                            // Action buttons matching Web specifications for Group (Account Frame)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (onAddChildTimer != null) {
                                    Button(
                                        onClick = onAddChildTimer,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("タイマー追加", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (onAddGroupBelow != null) {
                                    Button(
                                        onClick = onAddGroupBelow,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Text("枠を追加", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (onCloneGroup != null) {
                                Button(
                                    onClick = onCloneGroup,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text("枠を複製", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    "header" -> {
                        // Toggle Folding Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("折りたたみをロック", color = Color.White, fontSize = 14.sp)
                            Switch(
                                checked = isFoldLocked,
                                onCheckedChange = { isFoldLocked = it }
                            )
                        }
                    }
                }

                // Color selectors for customized boundaries (Group, Header, Rule)
                if (entity.type == "group" || entity.type == "header" || entity.type == "rule") {
                    Divider(color = Color(0xFF2A2A3A), thickness = 1.dp)

                    Text("カラーカスタマイズ", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                    // SHARED Preset Row
                    Text("プリセット", fontSize = 11.sp, color = Color(0xFF8A8EA3))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SHARED_COLORS.forEach { colorHex ->
                            val color = Color(android.graphics.Color.parseColor(colorHex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (selectedColor.lowercase() == colorHex.lowercase()) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .pointerDownTap { selectedColor = colorHex }
                            )
                        }
                    }

                    // Custom 6 Slots Row
                    Text("カスタム保存枠 (6枠)", fontSize = 11.sp, color = Color(0xFF8A8EA3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            customColors.forEachIndexed { index, colorHex ->
                                val color = Color(android.graphics.Color.parseColor(colorHex))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (selectedColor.lowercase() == colorHex.lowercase()) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .pointerDownTap { selectedColor = colorHex }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                // Initialize rgb values
                                try {
                                    val parsed = android.graphics.Color.parseColor(selectedColor)
                                    redVal = android.graphics.Color.red(parsed)
                                    greenVal = android.graphics.Color.green(parsed)
                                    blueVal = android.graphics.Color.blue(parsed)
                                } catch (e: Exception) {
                                    redVal = 128; greenVal = 128; blueVal = 128
                                }
                                showColorMixer = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text("RGB調色", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("やめる", color = Color(0xFF8A8EA3))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (entity.type != "rule") {
                                onSaveName(nameStr)
                            }
                            if (entity.type == "group" || entity.type == "header" || entity.type == "rule") {
                                onSaveColor(selectedColor)
                            }

                            // Build update parameters
                            val params = mutableMapOf<String, Any>()

                            if (entity.type == "stam") {
                                params["intervalMin"] = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5
                                params["max"] = maxStr.toIntOrNull()?.coerceAtLeast(1) ?: 100
                                val chunk = chunkStr.toIntOrNull()
                                if (chunk == null || chunk <= 0) {
                                    params["useChunkClear"] = true
                                } else {
                                    params["useChunk"] = chunk
                                }
                             } else if (entity.type == "orb") {
                                params["intervalMin"] = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 360
                                params["max"] = maxStr.toIntOrNull()?.coerceAtIn(1, 99) ?: 4
                                val chunk = chunkStr.toIntOrNull()
                                if (chunk == null || chunk <= 0) {
                                    params["useChunkClear"] = true
                                } else {
                                    params["useChunk"] = chunk
                                }
                                params["orbMode"] = orbMode
                                params["orbEditHours"] = orbHoursStr.toIntOrNull() ?: 0
                                params["orbEditMinutes"] = orbMinutesStr.toIntOrNull() ?: 0
                            } else if (entity.type == "idle" || entity.type == "exped") {
                                val h = durationHoursStr.toIntOrNull() ?: 0
                                val m = durationMinutesStr.toIntOrNull() ?: 0
                                params["durationMin"] = h * 60 + m
                                params["countMode"] = countMode
                                params["idleEditHours"] = idleCurHoursStr.toIntOrNull() ?: 0
                                params["idleEditMinutes"] = idleCurMinutesStr.toIntOrNull() ?: 0
                            } else if (entity.type == "group") {
                                params["layout"] = layoutMode
                            } else if (entity.type == "header") {
                                params["foldLock"] = isFoldLocked
                            }

                            onUpdateSettings(params)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("保存", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- Sub Dialog: RGB Color Mixer ---
    if (showColorMixer) {
        BackHandler(enabled = true) {
            showColorMixer = false
        }
        FastDialog(onDismissRequest = { showColorMixer = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1B1B28),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showColorMixer = false }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = "RGBで色を作成",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Selected preview box
                    val hexStr = String.format("#%02x%02x%02x", redVal, greenVal, blueVal)
                    val mixedColor = Color(redVal, greenVal, blueVal)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(mixedColor)
                            .border(1.dp, Color(255, 255, 255, 40), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hexStr.uppercase(),
                            color = if ((redVal * 299 + greenVal * 587 + blueVal * 114) / 1000 > 128) Color.Black else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp
                        )
                    }

                    // R, G, B Slider Rows
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14161C), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        ColorSliderRow(label = "R", accentColor = Color(0xFFEF4444), value = redVal, onValueChange = { redVal = it })
                        ColorSliderRow(label = "G", accentColor = Color(0xFF22C55E), value = greenVal, onValueChange = { greenVal = it })
                        ColorSliderRow(label = "B", accentColor = Color(0xFF3B82F6), value = blueVal, onValueChange = { blueVal = it })
                    }

                    HorizontalDivider(color = Color(255, 255, 255, 20), thickness = 1.dp)

                    Text(
                        text = "保存先スロットを選択（タップで保存・確定）",
                        fontSize = 11.sp,
                        color = Color(0xFF8A8EA3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target slot row selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val slots = (0 until 6).map { i -> customColors.getOrElse(i) { "#52617A" } }
                        slots.forEachIndexed { index, slotHex ->
                            val slotColor = Color(android.graphics.Color.parseColor(slotHex))
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(slotColor)
                                    .border(1.5.dp, Color(255, 255, 255, 60), CircleShape)
                                    .pointerDownTap {
                                        // Save mixed color to slot
                                        val mutable = customColors.toMutableList()
                                        while (mutable.size < 6) {
                                            mutable.add("#52617A")
                                        }
                                        mutable[index] = hexStr
                                        onSaveCustomColors(mutable)
                                        selectedColor = hexStr
                                        showColorMixer = false
                                    }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            selectedColor = hexStr
                            showColorMixer = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF343B4E)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("保存せず色だけ適用", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSliderRow(
    label: String,
    accentColor: Color,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editStr by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label with rounded badge
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(accentColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = accentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        // Sleek Slider
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = accentColor,
                inactiveTrackColor = Color(255, 255, 255, 25)
            )
        )

        // Number Value badge (tap to type if desired)
        if (isEditing) {
            BasicTextField(
                value = editStr,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() }.take(3)
                    editStr = filtered
                    val num = filtered.toIntOrNull()
                    if (num != null) {
                        onValueChange(num.coerceIn(0, 255))
                    }
                },
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { isEditing = false }
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .width(36.dp)
                    .background(Color(0xFF2C2F3A), RoundedCornerShape(4.dp))
                    .border(1.dp, accentColor, RoundedCornerShape(4.dp))
                    .padding(vertical = 2.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .background(Color(255, 255, 255, 12), RoundedCornerShape(4.dp))
                    .pointerDownTap { isEditing = true }
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun QuickNumberEditDialog(
    title: String,
    initialValue: Int,
    minValue: Int = 0,
    maxValue: Int = 999,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var textValue by remember { mutableStateOf("") }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF15151F),
            modifier = Modifier.fillMaxWidth().padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        textValue = digits
                    },
                    placeholder = {
                        Text(
                            text = initialValue.toString(),
                            color = Color(0xFF6E7282)
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            val parsed = textValue.toIntOrNull() ?: initialValue
                            val clamped = Math.max(minValue, Math.min(maxValue, parsed))
                            onConfirm(clamped)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF52617A)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル", color = Color(0xFF8A8EA3))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val parsed = textValue.toIntOrNull() ?: initialValue
                            val clamped = Math.max(minValue, Math.min(maxValue, parsed))
                            onConfirm(clamped)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("決定", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- Specialized Timer Toast Dialogs matching Web App Screenshots ---

@Composable
fun ToastItemRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    allowEmpty: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusTracker = LocalFocusTracker.current
    var isFocused by remember { mutableStateOf(false) }
    var localText by remember(value) { mutableStateOf(value) }
    var originalValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (!isFocused) {
            localText = value
            originalValue = value
        }
    }

    Row(
        modifier = modifier
            .height(34.dp)
            .background(Color(0xFF22252D), RoundedCornerShape(6.dp))
            .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
        BasicTextField(
            value = localText,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                localText = digits
                onValueChange(digits)
            },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }
            ),
            singleLine = true,
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .width(48.dp)
                .height(24.dp)
                .background(Color(0xFF0E0F14), RoundedCornerShape(4.dp))
                .border(1.dp, Color(255, 255, 255, 30), RoundedCornerShape(4.dp))
                .onFocusChanged { focusState ->
                    focusTracker(focusState.isFocused)
                    if (focusState.isFocused) {
                        if (!isFocused) {
                            isFocused = true
                            originalValue = if (localText.isNotEmpty()) localText else value
                            localText = ""
                            keyboardController?.show()
                        }
                    } else {
                        if (isFocused) {
                            isFocused = false
                            if (localText.isEmpty()) {
                                if (allowEmpty) {
                                    localText = ""
                                    onValueChange("")
                                } else if (originalValue.isNotEmpty()) {
                                    localText = originalValue
                                    onValueChange(originalValue)
                                }
                            } else {
                                onValueChange(localText)
                            }
                        }
                    }
                },
            decorationBox = { innerTextField: @Composable () -> Unit ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    val ph = if (localText.isEmpty()) (if (placeholder.isNotEmpty()) placeholder else originalValue) else ""
                    if (ph.isNotEmpty()) {
                        Text(ph, color = Color(0xFF6E7282), fontSize = 12.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun ToastTimeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    allowEmpty: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusTracker = LocalFocusTracker.current
    var isFocused by remember { mutableStateOf(false) }
    var localText by remember(value) { mutableStateOf(value) }
    var originalValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (!isFocused) {
            localText = value
            originalValue = value
        }
    }

    BasicTextField(
        value = localText,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }
            localText = digits
            onValueChange(digits)
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        ),
        singleLine = true,
        cursorBrush = SolidColor(Color.Transparent),
        modifier = modifier
            .height(24.dp)
            .background(Color(0xFF0E0F14), RoundedCornerShape(4.dp))
            .border(1.dp, Color(255, 255, 255, 30), RoundedCornerShape(4.dp))
            .onFocusChanged { focusState ->
                focusTracker(focusState.isFocused)
                if (focusState.isFocused) {
                    if (!isFocused) {
                        isFocused = true
                        originalValue = if (localText.isNotEmpty()) localText else value
                        localText = ""
                        keyboardController?.show()
                    }
                } else {
                    if (isFocused) {
                        isFocused = false
                        if (localText.isEmpty()) {
                            if (allowEmpty) {
                                localText = ""
                                onValueChange("")
                            } else if (originalValue.isNotEmpty()) {
                                localText = originalValue
                                onValueChange(originalValue)
                            }
                        } else {
                            onValueChange(localText)
                        }
                    }
                }
            },
        decorationBox = { innerTextField: @Composable () -> Unit ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val ph = if (localText.isEmpty()) (if (placeholder.isNotEmpty()) placeholder else originalValue) else ""
                if (ph.isNotEmpty()) {
                    Text(ph, color = Color(0xFF6E7282), fontSize = 12.sp)
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun ClearOnFocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    isDigitOnly: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    var localText by remember(value) { mutableStateOf(value) }
    var originalValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (!isFocused) {
            localText = value
            originalValue = value
        }
    }

    OutlinedTextField(
        value = localText,
        onValueChange = { input ->
            val filtered = if (isDigitOnly) input.filter { it.isDigit() } else input
            localText = filtered
            onValueChange(filtered)
        },
        label = { Text(label) },
        placeholder = {
            val ph = if (localText.isEmpty()) originalValue else ""
            if (ph.isNotEmpty()) {
                Text(ph, color = Color(0xFF6E7282))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = Color(0xFF52617A)
        ),
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                if (!isFocused) {
                    isFocused = true
                    originalValue = if (localText.isNotEmpty()) localText else value
                    if (localText.isNotEmpty()) {
                        localText = ""
                    }
                }
            } else {
                if (isFocused) {
                    isFocused = false
                    if (localText.isEmpty() && originalValue.isNotEmpty()) {
                        localText = originalValue
                        onValueChange(originalValue)
                    } else if (localText.isNotEmpty()) {
                        onValueChange(localText)
                    }
                }
            }
        }
    )
}

@Composable
fun RuleToastDialog(
    entity: ItemEntity,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit,
    onAddGroupBelow: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    BackHandler(enabled = isConfirmingDelete) {
        isConfirmingDelete = false
    }

    if (showColorPicker) {
        FullColorPickerDialog(
            title = "仕切り線の色",
            selectedColor = entity.color ?: "#52617A",
            customColors = customColors,
            onDismiss = { showColorPicker = false },
            onSelectColor = { newHex ->
                onSaveColor(newHex)
                showColorPicker = false
            },
            onSaveCustomColors = onSaveCustomColors
        )
        return
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 15)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: 色 ⭕
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF22252D))
                        .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                        .pointerDownTap { showColorPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("色 ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF52617A)
                                )
                        )
                    }
                }

                // Row 2: 枠を追加 (エメラルド系アクセント) | 移動 (アンバー系アクセント)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1A34D399))
                            .border(BorderStroke(1.dp, Color(0x5934D399)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                onDismiss()
                                onAddGroupBelow()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("枠を追加", color = Color(0xFF34D399), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFBBF24))
                            .border(BorderStroke(1.dp, Color(0x59FBBF24)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                onDismiss()
                                onStartMove()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("移動", color = Color(0xFFFBBF24), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Row 3: ⋯ 折りたたみアクション（削除）
                FoldableToastFooter(
                    onDelete = {
                        onDismiss()
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
fun StaminaToastDialog(
    entity: ItemEntity,
    onDismiss: () -> Unit,
    onUpdateSettings: (Map<String, Any?>) -> Unit,
    onDelete: () -> Unit
) {
    var intervalStr by remember(entity.id) { mutableStateOf(entity.intervalMin.toString()) }
    var maxStr by remember(entity.id) { mutableStateOf(entity.max.toString()) }
    var chunkStr by remember(entity.id) { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val commitChanges = {
        val newInterval = intervalStr.toIntOrNull() ?: entity.intervalMin
        val newMax = maxStr.toIntOrNull() ?: entity.max
        val newChunk = chunkStr.toIntOrNull()

        onUpdateSettings(
            mapOf(
                "intervalMin" to newInterval,
                "max" to newMax,
                "useChunk" to newChunk,
                "useChunkClear" to (newChunk == null)
            )
        )
    }

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 25)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: 回復 / 最大
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToastItemRow(
                        label = "回復",
                        value = intervalStr,
                        onValueChange = { intervalStr = it },
                        modifier = Modifier.weight(1f)
                    )
                    ToastItemRow(
                        label = "最大",
                        value = maxStr,
                        onValueChange = { maxStr = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: 使い切り
                ToastItemRow(
                    label = "使い切り",
                    value = chunkStr,
                    onValueChange = { chunkStr = it },
                    placeholder = "なし",
                    allowEmpty = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Row 3: +1 +5 +10 (takes up full width, height 28.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 5, 10).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .border(1.dp, Color(255, 209, 102, 102), RoundedCornerShape(6.dp))
                                .background(Color(255, 209, 102, 31), RoundedCornerShape(6.dp))
                                .pointerDownTap {
                                    val curMax = maxStr.toIntOrNull() ?: entity.max
                                    val nextMax = Math.min(999, curMax + delta)
                                    maxStr = nextMax.toString()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$delta",
                                color = Color(0xFFFFD166),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row 4: ⋯ 折りたたみアクション
                FoldableToastFooter(onDelete = onDelete)
            }
        }
    }
}

@Composable
fun OrbToastDialog(
    entity: ItemEntity,
    onDismiss: () -> Unit,
    onUpdateSettings: (Map<String, Any?>) -> Unit,
    onDelete: () -> Unit
) {
    var maxStr by remember(entity.id) { mutableStateOf(entity.max.toString()) }
    var hoursInterval by remember(entity.id) { mutableStateOf((entity.intervalMin / 60).toString()) }
    var chunkStr by remember(entity.id) { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var orbMode by remember(entity.id) { mutableStateOf(entity.orbMode ?: "down") }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val initialOrbMinutes = remember(entity.id, orbMode) {
        val now = System.currentTimeMillis()
        val isUp = orbMode == "up"
        val oneOrbMin = Math.max(1, entity.intervalMin)
        if (entity.current >= entity.max) {
            if (isUp) entity.max * oneOrbMin else 0
        } else {
            val elapsed = Math.max(0L, now - entity.start)
            val recovered = (elapsed / (oneOrbMin * 60000L)).toInt()
            val cur = Math.min(entity.max, entity.current + recovered)
            if (cur >= entity.max) {
                if (isUp) entity.max * oneOrbMin else 0
            } else {
                val nextInMs = (oneOrbMin * 60000L) - (elapsed % (oneOrbMin * 60000L))
                val need = entity.max - cur
                val remainMs = (need - 1) * (oneOrbMin * 60000L) + nextInMs
                if (isUp) {
                    (cur * oneOrbMin + Math.max(0L, (oneOrbMin * 60000L) - nextInMs) / 60000L).toInt()
                } else {
                    Math.ceil(remainMs / 60000.0).toInt()
                }
            }
        }
    }
    var orbHoursStr by remember(entity.id, initialOrbMinutes) { mutableStateOf("%02d".format(initialOrbMinutes / 60)) }
    var orbMinutesStr by remember(entity.id, initialOrbMinutes) { mutableStateOf("%02d".format(initialOrbMinutes % 60)) }

    val commitChanges = {
        val newMax = maxStr.toIntOrNull() ?: entity.max
        val hInter = hoursInterval.toIntOrNull() ?: 6
        val newInterval = hInter * 60
        val newChunk = chunkStr.toIntOrNull()
        val hEdit = orbHoursStr.toIntOrNull()
        val mEdit = orbMinutesStr.toIntOrNull()

        onUpdateSettings(
            mapOf(
                "max" to newMax,
                "intervalMin" to newInterval,
                "useChunk" to newChunk,
                "useChunkClear" to (newChunk == null),
                "orbMode" to orbMode,
                "orbEditHours" to hEdit,
                "orbEditMinutes" to mEdit
            )
        )
    }

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 25)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: 最大 / 回復(時間)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToastItemRow(
                        label = "最大",
                        value = maxStr,
                        onValueChange = { maxStr = it },
                        modifier = Modifier.weight(1f)
                    )
                    ToastItemRow(
                        label = "回復(時間)",
                        value = hoursInterval,
                        onValueChange = { hoursInterval = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: 全回復 / 消費数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("全回復", fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ToastTimeInput(
                                value = orbHoursStr,
                                onValueChange = { orbHoursStr = it },
                                modifier = Modifier.width(26.dp)
                            )
                            Text(":", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 1.dp))
                            ToastTimeInput(
                                value = orbMinutesStr,
                                onValueChange = { orbMinutesStr = it },
                                modifier = Modifier.width(26.dp)
                            )
                        }
                    }

                    ToastItemRow(
                        label = "消費数",
                        value = chunkStr,
                        onValueChange = { chunkStr = it },
                        placeholder = "なし",
                        allowEmpty = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: 方式ボタン (Improved contrast and clear color differentiation)
                val isOrbDown = orbMode == "down"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOrbDown) Color(0x1F38BDF8) else Color(0x1FA78BFA))
                        .border(
                            1.dp,
                            if (isOrbDown) Color(0x6638BDF8) else Color(0x66A78BFA),
                            RoundedCornerShape(6.dp)
                        )
                        .pointerDownTap { 
                            orbMode = if (isOrbDown) "up" else "down"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isOrbDown) "方式：▼ 残り時間 (減算)" else "方式：▲ 経過時間 (蓄積)",
                        color = if (isOrbDown) Color(0xFF38BDF8) else Color(0xFFA78BFA),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Row 4: ⋯ 折りたたみアクション
                FoldableToastFooter(onDelete = onDelete)
            }
        }
    }
}

@Composable
fun IdleExpedToastDialog(
    entity: ItemEntity,
    onDismiss: () -> Unit,
    onUpdateSettings: (Map<String, Any?>) -> Unit,
    onDelete: () -> Unit
) {
    var countMode by remember(entity.id) { mutableStateOf(entity.countMode ?: "down") }
    var setHoursStr by remember(entity.id) { mutableStateOf((entity.durationMin / 60).toString()) }
    var setMinutesStr by remember(entity.id) { mutableStateOf((entity.durationMin % 60).toString()) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val initialIdleMinutes = remember(entity.id, countMode) {
        val now = System.currentTimeMillis()
        val isUp = countMode == "up"
        val durMs = Math.max(1, entity.durationMin) * 60000L
        val elapsed = Math.max(0L, now - entity.start)
        val remainMs = Math.max(0L, durMs - elapsed)
        val curMin = if (isUp) {
            elapsed / 60000L
        } else {
            Math.ceil(remainMs / 60000.0).toLong()
        }
        curMin.toInt()
    }
    var curHoursStr by remember(entity.id, initialIdleMinutes) { mutableStateOf((initialIdleMinutes / 60).toString()) }
    var curMinutesStr by remember(entity.id, initialIdleMinutes) { mutableStateOf((initialIdleMinutes % 60).toString()) }

    val commitChanges = {
        val hSet = setHoursStr.toIntOrNull() ?: (entity.durationMin / 60)
        val mSet = setMinutesStr.toIntOrNull() ?: (entity.durationMin % 60)
        val totalDurMin = Math.max(1, hSet * 60 + mSet)

        val hCur = curHoursStr.toIntOrNull()
        val mCur = curMinutesStr.toIntOrNull()

        onUpdateSettings(
            mapOf(
                "durationMin" to totalDurMin,
                "countMode" to countMode,
                "idleEditHours" to hCur,
                "idleEditMinutes" to mCur
            )
        )
    }

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 25)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: 方式 (height 34.dp with high-contrast color distinguishing down/up)
                val isCountDown = countMode == "down"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("方式", fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                1.dp,
                                if (isCountDown) Color(0x6638BDF8) else Color(0x6634D399),
                                RoundedCornerShape(4.dp)
                            )
                            .background(if (isCountDown) Color(0x1F38BDF8) else Color(0x1F34D399))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                            .pointerDownTap { 
                                countMode = if (isCountDown) "up" else "down"
                            }
                    ) {
                        Text(
                            text = if (isCountDown) "▼ カウントダウン" else "▲ カウントアップ",
                            color = if (isCountDown) Color(0xFF38BDF8) else Color(0xFF34D399),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Row 2: 設定時間 (height 34.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("設定", fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToastTimeInput(
                            value = setHoursStr,
                            onValueChange = { setHoursStr = it },
                            modifier = Modifier.width(32.dp)
                        )
                        Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                        ToastTimeInput(
                            value = setMinutesStr,
                            onValueChange = { setMinutesStr = it },
                            modifier = Modifier.width(32.dp)
                        )
                        Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
                    }
                }

                // Row 3: 残り / 経過 (height 34.dp)
                val progressLabel = if (countMode == "up") "経過" else "残り"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(progressLabel, fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ToastTimeInput(
                            value = curHoursStr,
                            onValueChange = { curHoursStr = it },
                            modifier = Modifier.width(32.dp)
                        )
                        Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                        ToastTimeInput(
                            value = curMinutesStr,
                            onValueChange = { curMinutesStr = it },
                            modifier = Modifier.width(32.dp)
                        )
                        Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
                    }
                }

                // Row 4: ⋯ 折りたたみアクション
                FoldableToastFooter(onDelete = onDelete)
            }
        }
    }
}

/**
 * Lightweight Toast-style Menu Dialog for Backup (Copy / Paste)
 */
@Composable
fun BackupToastDialog(
    onDismiss: () -> Unit,
    onCopyBackup: () -> Unit,
    onPasteBackup: (String) -> Unit
) {
    val context = LocalContext.current
    var showPasteInput by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.dp, Color(255, 255, 255, 15)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "バックアップ",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        color = if (isError) Color(0xFFFF6B6B) else Color(0xFF5CD68A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!showPasteInput) {
                    // Option 1: Copy to clipboard
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                onCopyBackup()
                                statusMessage = "クリップボードにコピーしました"
                                isError = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "データをコピー",
                            color = Color(0xFFECEEF2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Option 2: Paste from clipboard directly or open input
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clipData = clipboard?.primaryClip
                                val text = if (clipData != null && clipData.itemCount > 0) {
                                    clipData.getItemAt(0).text?.toString() ?: ""
                                } else ""

                                if (text.isNotBlank() && text.trim().startsWith("{")) {
                                    onPasteBackup(text)
                                } else {
                                    pasteText = text
                                    showPasteInput = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "データを復元 (ペースト)",
                            color = Color(0xFFECEEF2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .background(Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                            .pointerDownTap { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("閉じる", color = Color(0xFF8A8EA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Manual paste input area
                    BasicTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 12.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF15151F))
                            .border(1.dp, Color(255, 255, 255, 20), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                                .pointerDownTap { showPasteInput = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("戻る", color = Color(0xFF8A8EA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(36.dp)
                                .background(Color(0xFF8C7CFF), RoundedCornerShape(6.dp))
                                .pointerDownTap {
                                    if (pasteText.isNotBlank()) {
                                        onPasteBackup(pasteText)
                                    } else {
                                        statusMessage = "データが空です"
                                        isError = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("復元実行", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
