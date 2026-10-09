@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.abysstimer.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.abysstimer.data.ItemEntity

@Composable
fun DeleteConfirmDialog(
    itemName: String,
    itemType: String,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF1B1D24),
            border = BorderStroke(1.2.dp, Color(0xFFFF6B6B).copy(alpha = 0.75f)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "削除の確認",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val displayName = when {
                    itemName.isNotEmpty() -> "「$itemName」"
                    itemType == "group" -> "アカウント枠"
                    itemType == "header" -> "見出し"
                    itemType == "rule" -> "仕切り線"
                    itemType == "space" -> "空白スペーサー"
                    itemType == "stam" -> "スタミナタイマー"
                    itemType == "orb" -> "オーブタイマー"
                    itemType == "idle" -> "放置タイマー"
                    itemType == "exped" -> "遠征タイマー"
                    else -> "この項目"
                }

                Text(
                    text = "${displayName}を削除しますか？",
                    fontSize = 13.sp,
                    color = Color(0xFFC5C8D4),
                    textAlign = TextAlign.Center
                )

                if (itemType == "group") {
                    Text(
                        text = "※枠内の全タイマーもまとめて削除されます",
                        fontSize = 11.sp,
                        color = Color(0xFFFFB4AB),
                        textAlign = TextAlign.Center
                    )
                }

                HorizontalDivider(color = Color(255, 255, 255, 25), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 20)), RoundedCornerShape(6.dp))
                            .pointerDownTap { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("キャンセル", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF451818))
                            .border(BorderStroke(1.dp, Color(0xFFFF5252)), RoundedCornerShape(6.dp))
                            .pointerDownTap { onConfirmDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("削除", color = Color(0xFFFF6B6B), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

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
    onAddChildTimer: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var isConfirmingDelete by remember { mutableStateOf(false) }

    val currentLayout = entity.layout ?: "regular"

    BackHandler(enabled = true) {
        when {
            showColorPicker -> showColorPicker = false
            isEditingName -> isEditingName = false
            isConfirmingDelete -> isConfirmingDelete = false
            else -> onDismiss()
        }
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

    val groupColor = remember(entity.color) {
        entity.color.toComposeColor(Color(0xFF9B8BFF))
    }
    val dialogBorder = remember(groupColor) {
        BorderStroke(1.2.dp, groupColor.copy(alpha = 0.70f))
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ToastCardShape,
            color = Color(0xFF1B1D22),
            border = dialogBorder,
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingName) {
                    NameEditContent(
                        title = "アカウント名を変更",
                        initialName = entity.name,
                        accentColor = groupColor,
                        onSave = { newName ->
                            if (newName != entity.name) {
                                onSaveName(newName)
                            }
                            isEditingName = false
                        },
                        onCancel = { isEditingName = false }
                    )
                } else {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    ToastActionButton(
                        text = "タイマー追加",
                        color = Color(0xFF5EEAD4),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            onAddChildTimer()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ToastActionButton(
                            text = "移動",
                            color = Color(0xFFFBBF24),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onDismiss()
                                onStartMove()
                            }
                        )

                        ToastActionButton(
                            text = "枠を複製",
                            color = Color(0xFFC5C8D4),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium,
                            onClick = {
                                onDismiss()
                                onCloneGroup()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderToastDialog(
    entity: ItemEntity,
    isCompact: Boolean = false,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSaveName: (String) -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onToggleFoldLock: (Boolean) -> Unit,
    onUpdateLayout: (String) -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
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

    val headerColor = remember(entity.color) {
        entity.color.toComposeColor(Color(0xFF9B8BFF))
    }
    val dialogBorder = remember(headerColor) {
        BorderStroke(1.2.dp, headerColor.copy(alpha = 0.70f))
    }

    val defaultCols = 5
    val maxCols = 6
    val colOptions = remember { listOf(4, 5, 6) }
    var selectedCols by remember(entity.layout) {
        mutableStateOf(
            entity.layout?.filter { it.isDigit() }?.toIntOrNull()?.coerceIn(4, maxCols) ?: defaultCols
        )
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ToastCardShape,
            color = Color(0xFF1B1D22),
            border = dialogBorder,
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditingName) {
                    NameEditContent(
                        title = "見出し名を変更",
                        initialName = entity.name,
                        accentColor = headerColor,
                        onSave = { newName ->
                            if (newName != entity.name) {
                                onSaveName(newName)
                            }
                            isEditingName = false
                        },
                        onCancel = { isEditingName = false }
                    )
                } else {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

                    // 1行のタイマー設置数（通常表示: 1〜4枠 / コンパクト表示: 1〜6枠セレクター）
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF22252D))
                            .border(BorderStroke(1.dp, Color(255, 255, 255, 12)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "1行の枠設置数",
                                color = Color(0xFFAAAAAA),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${selectedCols}枠",
                                color = headerColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            colOptions.forEach { colNum ->
                                val isSelected = selectedCols == colNum
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(30.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isSelected) headerColor.copy(alpha = 0.35f) else Color(0xFF1B1D22)
                                        )
                                        .border(
                                            BorderStroke(
                                                if (isSelected) 1.5.dp else 1.dp,
                                                if (isSelected) headerColor else Color(255, 255, 255, 15)
                                            ),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .pointerDownTap {
                                            if (selectedCols != colNum) {
                                                selectedCols = colNum
                                                onUpdateLayout("$colNum")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$colNum",
                                        color = if (isSelected) Color.White else Color(0xFF888899),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    ToastActionButton(
                        text = "移動",
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            onStartMove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RuleToastDialog(
    entity: ItemEntity,
    customColors: List<String>,
    onDismiss: () -> Unit,
    onSaveColor: (String) -> Unit,
    onSaveCustomColors: (List<String>) -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

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

    val ruleColor = remember(entity.color) {
        entity.color.toComposeColor(Color(0xFF52617A))
    }
    val dialogBorder = remember(ruleColor) {
        BorderStroke(1.2.dp, ruleColor.copy(alpha = 0.75f))
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ToastCardShape,
            color = Color(0xFF1B1D22),
            border = dialogBorder,
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                                    entity.color.toComposeColor(Color(0xFF52617A))
                                )
                        )
                    }
                }

                ToastActionButton(
                    text = "移動",
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismiss()
                        onStartMove()
                    }
                )
            }
        }
    }
}

@Composable
fun SpaceToastDialog(
    entity: ItemEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onStartMove: () -> Unit
) {
    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ToastCardShape,
            color = Color(0xFF1B1D22),
            border = BorderStroke(1.2.dp, Color(0xFF4A5568)),
            modifier = Modifier.width(280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "空白スペーサー (1行 / 48dp)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                ToastActionButton(
                    text = "移動",
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        onDismiss()
                        onStartMove()
                    }
                )
            }
        }
    }
}

@Composable
private fun StaminaQuickIncrementRow(
    getMax: () -> String,
    onMaxChange: (String) -> Unit,
    entityMax: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val deltas = remember { listOf(1, 5, 10) }
        deltas.forEach { delta ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .border(1.dp, Color(255, 209, 102, 102), RoundedCornerShape(6.dp))
                    .background(Color(255, 209, 102, 31), RoundedCornerShape(6.dp))
                    .pointerDownTap {
                        val curMax = getMax().toIntOrNull() ?: entityMax
                        val nextMax = Math.min(999, curMax + delta)
                        onMaxChange(nextMax.toString())
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
}

@Composable
fun StaminaToastDialog(
    entity: ItemEntity,
    groupName: String = "",
    onDismiss: () -> Unit,
    onUpdateSettings: (TimerSettingsUpdate) -> Unit,
    onDelete: () -> Unit,
    onRequestKeypad: (field: String, initial: String, onCommit: (String) -> Unit) -> Unit = { _, _, _ -> },
    isKeypadActive: (field: String) -> Boolean = { false }
) {
    var intervalStr by remember(entity.id) { mutableStateOf(entity.intervalMin.toString()) }
    var maxStr by remember(entity.id) { mutableStateOf(entity.max.toString()) }
    var chunkStr by remember(entity.id) { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var isDirty by remember(entity.id) { mutableStateOf(false) }

    val commitChanges = {
        if (isDirty) {
            val newInterval = intervalStr.toIntOrNull() ?: entity.intervalMin
            val newMax = maxStr.toIntOrNull() ?: entity.max
            val rawChunk = chunkStr.toIntOrNull()
            val newChunk = if (rawChunk == null || rawChunk <= 0) null else rawChunk

            if (newInterval != entity.intervalMin || newMax != entity.max || newChunk != entity.useChunk) {
                onUpdateSettings(
                    TimerSettingsUpdate(
                        intervalMin = newInterval,
                        max = newMax,
                        useChunk = newChunk,
                        useChunkClear = (newChunk == null)
                    )
                )
            }
        }
    }

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        ToastDialogContainer(
            groupName = groupName,
            borderColor = Color(0xFF90A0DD).copy(alpha = 0.70f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToastItemRow(
                    label = "回復(分)",
                    value = intervalStr,
                    onValueChange = { intervalStr = it; isDirty = true },
                    isActive = isKeypadActive("interval"),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRequestKeypad("interval", intervalStr) { intervalStr = it; isDirty = true }
                    }
                )
                ToastItemRow(
                    label = "最大",
                    value = maxStr,
                    onValueChange = { maxStr = it; isDirty = true },
                    isActive = isKeypadActive("max"),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRequestKeypad("max", maxStr) { maxStr = it; isDirty = true }
                    }
                )
            }

            ToastItemRow(
                label = "使い切り",
                value = chunkStr,
                onValueChange = { chunkStr = it; isDirty = true },
                placeholder = "なし",
                allowEmpty = true,
                isActive = isKeypadActive("chunk"),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onRequestKeypad("chunk", chunkStr) { chunkStr = it; isDirty = true }
                }
            )

            StaminaQuickIncrementRow(
                getMax = { maxStr },
                entityMax = entity.max,
                onMaxChange = { maxStr = it; isDirty = true }
            )
        }
    }
}

@Composable
fun OrbToastDialog(
    entity: ItemEntity,
    groupName: String = "",
    onDismiss: () -> Unit,
    onUpdateSettings: (TimerSettingsUpdate) -> Unit,
    onDelete: () -> Unit = {},
    onRequestKeypad: (field: String, initial: String, onCommit: (String) -> Unit) -> Unit = { _, _, _ -> },
    isKeypadActive: (field: String) -> Boolean = { false }
) {
    var maxStr by remember(entity.id) { mutableStateOf(entity.max.toString()) }
    var hoursInterval by remember(entity.id) { mutableStateOf((entity.intervalMin / 60).toString()) }
    var chunkStr by remember(entity.id) { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var orbMode by remember(entity.id) { mutableStateOf(entity.orbMode ?: "down") }

    val initialOrbMinutes = remember(entity.id, orbMode) {
        val now = System.currentTimeMillis()
        val oneOrbMin = Math.max(1, entity.intervalMin)
        if (entity.current >= entity.max) {
            if (orbMode == "up") entity.max * oneOrbMin else 0
        } else {
            val elapsed = Math.max(0L, now - entity.start)
            val recovered = (elapsed / (oneOrbMin * 60000L)).toInt()
            val cur = Math.min(entity.max, entity.current + recovered)
            if (cur >= entity.max) {
                if (orbMode == "up") entity.max * oneOrbMin else 0
            } else {
                val nextInMs = (oneOrbMin * 60000L) - (elapsed % (oneOrbMin * 60000L))
                val need = entity.max - cur
                val remainMs = (need - 1) * (oneOrbMin * 60000L) + nextInMs
                if (orbMode == "up") {
                    (cur * oneOrbMin + Math.max(0L, (oneOrbMin * 60000L) - nextInMs) / 60000L).toInt()
                } else {
                    Math.ceil(remainMs / 60000.0).toInt()
                }
            }
        }
    }
    val defaultOrbH = "%02d".format(initialOrbMinutes / 60)
    val defaultOrbM = "%02d".format(initialOrbMinutes % 60)
    var orbHoursStr by remember(entity.id, initialOrbMinutes) { mutableStateOf(defaultOrbH) }
    var orbMinutesStr by remember(entity.id, initialOrbMinutes) { mutableStateOf(defaultOrbM) }
    var isDirty by remember(entity.id) { mutableStateOf(false) }

    val commitChanges = {
        if (isDirty) {
            val newMax = maxStr.toIntOrNull() ?: entity.max
            val hInter = hoursInterval.toIntOrNull() ?: 6
            val newInterval = hInter * 60
            val rawChunk = chunkStr.toIntOrNull()
            val newChunk = if (rawChunk == null || rawChunk <= 0) null else rawChunk
            val isTimeEdited = orbHoursStr != defaultOrbH || orbMinutesStr != defaultOrbM
            val hEdit = if (isTimeEdited) orbHoursStr.toIntOrNull() else null
            val mEdit = if (isTimeEdited) orbMinutesStr.toIntOrNull() else null

            val isMaxChanged = newMax != entity.max
            val isIntervalChanged = newInterval != entity.intervalMin
            val isChunkChanged = newChunk != entity.useChunk
            val isOrbModeChanged = orbMode != (entity.orbMode ?: "down")

            if (isMaxChanged || isIntervalChanged || isChunkChanged || isOrbModeChanged || isTimeEdited) {
                onUpdateSettings(
                    TimerSettingsUpdate(
                        max = newMax,
                        intervalMin = newInterval,
                        useChunk = newChunk,
                        useChunkClear = (newChunk == null),
                        orbMode = orbMode,
                        orbEditHours = hEdit,
                        orbEditMinutes = mEdit
                    )
                )
            }
        }
    }

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        ToastDialogContainer(
            groupName = groupName,
            borderColor = Color(0xFFB48CFF).copy(alpha = 0.70f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToastItemRow(
                    label = "最大",
                    value = maxStr,
                    onValueChange = { maxStr = it; isDirty = true },
                    isActive = isKeypadActive("max"),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRequestKeypad("max", maxStr) { maxStr = it; isDirty = true }
                    }
                )
                ToastItemRow(
                    label = "回復(時間)",
                    value = hoursInterval,
                    onValueChange = { hoursInterval = it; isDirty = true },
                    isActive = isKeypadActive("hours"),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRequestKeypad("hours", hoursInterval) { hoursInterval = it; isDirty = true }
                    }
                )
            }

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
                            onValueChange = { orbHoursStr = it; isDirty = true },
                            isActive = isKeypadActive("orbHours"),
                            modifier = Modifier.width(26.dp),
                            onClick = {
                                onRequestKeypad("orbHours", orbHoursStr) { orbHoursStr = it; isDirty = true }
                            }
                        )
                        Text(":", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 1.dp))
                        ToastTimeInput(
                            value = orbMinutesStr,
                            onValueChange = { orbMinutesStr = it; isDirty = true },
                            isActive = isKeypadActive("orbMinutes"),
                            modifier = Modifier.width(26.dp),
                            onClick = {
                                onRequestKeypad("orbMinutes", orbMinutesStr) { orbMinutesStr = it; isDirty = true }
                            }
                        )
                    }
                }

                ToastItemRow(
                    label = "消費数",
                    value = chunkStr,
                    onValueChange = { chunkStr = it; isDirty = true },
                    placeholder = "なし",
                    allowEmpty = true,
                    isActive = isKeypadActive("chunk"),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onRequestKeypad("chunk", chunkStr) { chunkStr = it; isDirty = true }
                    }
                )
            }

            val isOrbDown = orbMode == "down"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isOrbDown) Color(0x1F38BDF8) else Color(0x1FA78BFA))
                    .border(1.dp, if (isOrbDown) Color(0x6638BDF8) else Color(0x66A78BFA), RoundedCornerShape(6.dp))
                    .pointerDownTap { 
                        orbMode = if (isOrbDown) "up" else "down"
                        isDirty = true
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
        }
    }
}

@Composable
fun IdleExpedToastDialog(
    entity: ItemEntity,
    groupName: String = "",
    onDismiss: () -> Unit,
    onUpdateSettings: (TimerSettingsUpdate) -> Unit,
    onDelete: () -> Unit = {},
    onRequestKeypad: (field: String, initial: String, onCommit: (String) -> Unit) -> Unit = { _, _, _ -> },
    isKeypadActive: (field: String) -> Boolean = { false }
) {
    var countMode by remember(entity.id) { mutableStateOf(entity.countMode ?: "down") }
    var setHoursStr by remember(entity.id) { mutableStateOf((entity.durationMin / 60).toString()) }
    var setMinutesStr by remember(entity.id) { mutableStateOf((entity.durationMin % 60).toString()) }

    val initialIdleMinutes = remember(entity.id, countMode) {
        val now = System.currentTimeMillis()
        val durMs = Math.max(1, entity.durationMin) * 60000L
        val elapsed = Math.max(0L, now - entity.start)
        val remainMs = Math.max(0L, durMs - elapsed)
        val curMin = if (countMode == "up") {
            elapsed / 60000L
        } else {
            Math.ceil(remainMs / 60000.0).toLong()
        }
        curMin.toInt()
    }
    val defaultIdleH = (initialIdleMinutes / 60).toString()
    val defaultIdleM = (initialIdleMinutes % 60).toString()
    var curHoursStr by remember(entity.id, initialIdleMinutes) { mutableStateOf(defaultIdleH) }
    var curMinutesStr by remember(entity.id, initialIdleMinutes) { mutableStateOf(defaultIdleM) }
    var isDirty by remember(entity.id) { mutableStateOf(false) }

    val commitChanges = {
        if (isDirty) {
            val hSet = setHoursStr.toIntOrNull() ?: (entity.durationMin / 60)
            val mSet = setMinutesStr.toIntOrNull() ?: (entity.durationMin % 60)
            val totalDurMin = Math.max(1, hSet * 60 + mSet)

            val isTimeEdited = curHoursStr != defaultIdleH || curMinutesStr != defaultIdleM
            val hCur = if (isTimeEdited) curHoursStr.toIntOrNull() else null
            val mCur = if (isTimeEdited) curMinutesStr.toIntOrNull() else null

            if (totalDurMin != entity.durationMin || countMode != (entity.countMode ?: "down") || isTimeEdited) {
                onUpdateSettings(
                    TimerSettingsUpdate(
                        durationMin = totalDurMin,
                        countMode = countMode,
                        idleEditHours = hCur,
                        idleEditMinutes = mCur
                    )
                )
            }
        }
    }

    val typeColor = if (entity.type == "idle") Color(0xFFFF9F68) else Color(0xFF70D6B0)

    FastDialog(onDismissRequest = {
        commitChanges()
        onDismiss()
    }) {
        ToastDialogContainer(
            groupName = groupName,
            borderColor = typeColor.copy(alpha = 0.70f)
        ) {
            val isCountDown = countMode == "down"
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("方式", fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp))
                        .border(1.dp, if (isCountDown) Color(0x6638BDF8) else Color(0x6634D399), RoundedCornerShape(4.dp))
                        .background(if (isCountDown) Color(0x1F38BDF8) else Color(0x1F34D399))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                        .pointerDownTap { 
                            countMode = if (isCountDown) "up" else "down"
                            isDirty = true
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

            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("設定", fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToastTimeInput(
                        value = setHoursStr,
                        onValueChange = { setHoursStr = it; isDirty = true },
                        isActive = isKeypadActive("setHours"),
                        modifier = Modifier.width(32.dp),
                        onClick = {
                            onRequestKeypad("setHours", setHoursStr) { setHoursStr = it; isDirty = true }
                        }
                    )
                    Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                    ToastTimeInput(
                        value = setMinutesStr,
                        onValueChange = { setMinutesStr = it; isDirty = true },
                        isActive = isKeypadActive("setMinutes"),
                        modifier = Modifier.width(32.dp),
                        onClick = {
                            onRequestKeypad("setMinutes", setMinutesStr) { setMinutesStr = it; isDirty = true }
                        }
                    )
                    Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
                }
            }

            val progressLabel = if (countMode == "up") "経過" else "残り"
            Row(
                modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF22252D), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(255, 255, 255, 18), RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(progressLabel, fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToastTimeInput(
                        value = curHoursStr,
                        onValueChange = { curHoursStr = it; isDirty = true },
                        isActive = isKeypadActive("curHours"),
                        modifier = Modifier.width(32.dp),
                        onClick = {
                            onRequestKeypad("curHours", curHoursStr) { curHoursStr = it; isDirty = true }
                        }
                    )
                    Text("h", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(horizontal = 3.dp))
                    ToastTimeInput(
                        value = curMinutesStr,
                        onValueChange = { curMinutesStr = it; isDirty = true },
                        isActive = isKeypadActive("curMinutes"),
                        modifier = Modifier.width(32.dp),
                        onClick = {
                            onRequestKeypad("curMinutes", curMinutesStr) { curMinutesStr = it; isDirty = true }
                        }
                    )
                    Text("m", color = Color(0xFF8E96A5), fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
                }
            }
        }
    }
}

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

    val backupBorder = remember {
        BorderStroke(1.2.dp, Color(0xFF8C7CFF).copy(alpha = 0.70f))
    }

    FastDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ToastCardShape,
            color = Color(0xFF1B1D22),
            border = backupBorder,
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

                Box(
                    modifier = Modifier.fillMaxWidth().height(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (statusMessage != null) {
                        Text(
                            text = statusMessage ?: "",
                            color = if (isError) Color(0xFFFF6B6B) else Color(0xFF5CD68A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!showPasteInput) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E2829)).border(BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.65f)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                onCopyBackup()
                                statusMessage = "クリップボードにコピーしました"
                                isError = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("データをコピー", color = Color(0xFF6EE7B7), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF232238)).border(BorderStroke(1.2.dp, Color(0xFF9B8BFF).copy(alpha = 0.65f)), RoundedCornerShape(8.dp))
                            .pointerDownTap {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clipData = clipboard?.primaryClip
                                val text = if (clipData != null && clipData.itemCount > 0) {
                                    clipData.getItemAt(0).text?.toString() ?: ""
                                } else ""

                                val trimmed = text.trim()
                                if (trimmed.isNotBlank() && (trimmed.startsWith("ABYSS:") || trimmed.startsWith("{"))) {
                                    onPasteBackup(trimmed)
                                } else {
                                    pasteText = text
                                    showPasteInput = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("データを復元 (ペースト)", color = Color(0xFFC4B5FD), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().height(34.dp).background(Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                            .pointerDownTap { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("閉じる", color = Color(0xFF8A8EA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    BasicTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                        }),
                        cursorBrush = SolidColor(Color.White),
                        modifier = Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF15151F)).border(1.dp, Color(255, 255, 255, 20), RoundedCornerShape(6.dp)).padding(8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.weight(1f).height(36.dp).background(Color(0xFF2A2E39), RoundedCornerShape(6.dp))
                                .pointerDownTap { showPasteInput = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("戻る", color = Color(0xFF8A8EA3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Box(
                            modifier = Modifier.weight(1.2f).height(36.dp).background(Color(0xFF8C7CFF), RoundedCornerShape(6.dp))
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
