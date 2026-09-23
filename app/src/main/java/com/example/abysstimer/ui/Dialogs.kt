package com.example.abysstimer.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.abysstimer.data.ItemEntity

val SHARED_COLORS = listOf(
    "#9b8bff", "#b48cff", "#6fc7ff", "#70d6b0", "#ffd166", "#ff9f68", "#ff7b9c", "#d7dbe7", "#52617a"
)

// --- Add Panel Dialog ---
@Composable
fun AddPanelDialog(
    isGroupMode: Boolean,
    onDismiss: () -> Unit,
    onSelectType: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF15151F),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isGroupMode) "タイマーを追加" else "枠を追加",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Timer Types grid
                val types = listOf(
                    "stam" to "スタミナ",
                    "orb" to "オーブ",
                    "idle" to "放置報酬",
                    "exped" to "遠征"
                )

                types.forEach { (type, label) ->
                    Button(
                        onClick = { onSelectType(type) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (type) {
                                "stam" -> Color(0xFF1A365D)
                                "orb" -> Color(0xFF3B2E5C)
                                "idle" -> Color(0xFF5F370E)
                                "exped" -> Color(0xFF124E3F)
                                else -> Color(0xFF2A2A3A)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = label, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isGroupMode) {
                    Divider(color = Color(0xFF2A2A3A), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                    val onlyTop = listOf(
                        "group" to "アカウント枠",
                        "header" to "見出し",
                        "rule" to "仕切り線"
                    )

                    onlyTop.forEach { (type, label) ->
                        OutlinedButton(
                            onClick = { onSelectType(type) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFECEEF2)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF52617A)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = label, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("閉じる", color = Color(0xFF8A8EA3))
                }
            }
        }
    }
}

// --- Setup Dialog ---
@Composable
fun SetupDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    // State values depending on type
    var intervalStr by remember { mutableStateOf(if (type == "stam") "5" else "360") }
    var maxStr by remember { mutableStateOf(if (type == "stam") "100" else "4") }
    var chunkStr by remember { mutableStateOf("") }
    var hoursStr by remember { mutableStateOf(if (type == "exped") "4" else "12") }
    var minutesStr by remember { mutableStateOf("0") }
    var countMode by remember { mutableStateOf("down") } // "down" or "up"
    var orbMode by remember { mutableStateOf("down") } // "down" or "up"
    var selectedColor by remember { mutableStateOf(SHARED_COLORS[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF15151F),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = when (type) {
                        "stam" -> "スタミナ設定"
                        "orb" -> "オーブ設定"
                        "idle" -> "放置報酬の設定"
                        "exped" -> "遠征タイマーの設定"
                        "header" -> "見出しの文字色"
                        "rule" -> "仕切り線の色"
                        else -> "追加設定"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                when (type) {
                    "header", "rule" -> {
                        // Color Selector Row
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("カラーパレット", fontSize = 12.sp, color = Color(0xFF8A8EA3))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SHARED_COLORS.forEach { colorHex ->
                                    val color = Color(android.graphics.Color.parseColor(colorHex))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (selectedColor == colorHex) 3.dp else 0.dp,
                                                color = Color.White,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColor = colorHex }
                                    )
                                }
                            }
                        }
                    }
                    "stam" -> {
                        // Recovery rate
                        OutlinedTextField(
                            value = intervalStr,
                            onValueChange = { intervalStr = it },
                            label = { Text("回復時間 (分)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Max stamina
                        OutlinedTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = { Text("最大スタミナ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Usage chunk
                        OutlinedTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = { Text("使い切り消費数 (任意)") },
                            placeholder = { Text("例: 40 (ダブルタップで消費)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "orb" -> {
                        // Max orbs
                        OutlinedTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = { Text("最大個数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Hours recovery interval
                        var hoursInterval by remember { mutableStateOf("6") }
                        OutlinedTextField(
                            value = hoursInterval,
                            onValueChange = { hoursInterval = it },
                            label = { Text("回復間隔 (時間)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Consumption chunk
                        OutlinedTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = { Text("消費数 (任意)") },
                            placeholder = { Text("例: 1 (ダブルタップで消費)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mode toggle
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

                        LaunchedEffect(hoursInterval) {
                            val h = hoursInterval.toIntOrNull() ?: 6
                            intervalStr = (h * 60).toString()
                        }
                    }
                    "idle", "exped" -> {
                        // Mode Selection
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

                        // Duration inputs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = hoursStr,
                                onValueChange = { hoursStr = it },
                                label = { Text("時間 (h)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = minutesStr,
                                onValueChange = { minutesStr = it },
                                label = { Text("分 (m)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
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
                            val data = mutableMapOf<String, Any>()

                            if (type == "stam") {
                                data["intervalMin"] = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 5
                                data["max"] = maxStr.toIntOrNull()?.coerceAtLeast(1) ?: 100
                                chunkStr.toIntOrNull()?.let { data["useChunk"] = it }
                            } else if (type == "orb") {
                                data["intervalMin"] = intervalStr.toIntOrNull()?.coerceAtLeast(1) ?: 360
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("追加", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper int coercion
private fun Int.coerceAtIn(min: Int, max: Int): Int {
    return Math.max(min, Math.min(max, this))
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
    onUpdateSettings: (Map<String, Any>) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var nameStr by remember { mutableStateOf(entity.name) }
    var selectedColor by remember { mutableStateOf(entity.color ?: SHARED_COLORS[0]) }

    // Type specific settings
    var intervalStr by remember { mutableStateOf(entity.intervalMin.toString()) }
    var maxStr by remember { mutableStateOf(entity.max.toString()) }
    var chunkStr by remember { mutableStateOf(entity.useChunk?.toString() ?: "") }
    var durationHoursStr by remember { mutableStateOf((entity.durationMin / 60).toString()) }
    var durationMinutesStr by remember { mutableStateOf((entity.durationMin % 60).toString()) }
    var countMode by remember { mutableStateOf(entity.countMode ?: "down") }
    var orbMode by remember { mutableStateOf(entity.orbMode ?: "down") }
    var layoutMode by remember { mutableStateOf(entity.layout ?: "regular") }
    var isFoldLocked by remember { mutableStateOf(entity.foldLock) }

    // Color Mixer States
    var showColorMixer by remember { mutableStateOf(false) }
    var redVal by remember { mutableStateOf(128) }
    var greenVal by remember { mutableStateOf(128) }
    var blueVal by remember { mutableStateOf(128) }

    Dialog(onDismissRequest = onDismiss) {
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
                    OutlinedTextField(
                        value = nameStr,
                        onValueChange = { nameStr = it },
                        label = { Text("名前/見出し") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Position Reordering
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("表示位置の調整", color = Color(0xFF8A8EA3), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Divider(color = Color(0xFF2A2A3A), thickness = 1.dp)

                // Type-Specific Fields
                when (entity.type) {
                    "stam" -> {
                        OutlinedTextField(
                            value = intervalStr,
                            onValueChange = { intervalStr = it },
                            label = { Text("回復時間 (分)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = { Text("最大スタミナ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = { Text("使い切り消費数 (任意)") },
                            placeholder = { Text("設定しない場合は空欄") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "orb" -> {
                        OutlinedTextField(
                            value = maxStr,
                            onValueChange = { maxStr = it },
                            label = { Text("最大個数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        var hoursInterval by remember { mutableStateOf((entity.intervalMin / 60).toString()) }
                        OutlinedTextField(
                            value = hoursInterval,
                            onValueChange = {
                                hoursInterval = it
                                val h = it.toIntOrNull() ?: 6
                                intervalStr = (h * 60).toString()
                            },
                            label = { Text("回復間隔 (時間)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = chunkStr,
                            onValueChange = { chunkStr = it },
                            label = { Text("消費数 (任意)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = durationHoursStr,
                                onValueChange = { durationHoursStr = it },
                                label = { Text("時間 (h)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = durationMinutesStr,
                                onValueChange = { durationMinutesStr = it },
                                label = { Text("分 (m)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    "group" -> {
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
                                    color = Color.White
                                )
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
                                    .clickable { selectedColor = colorHex }
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
                                        .clickable { selectedColor = colorHex }
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
                            } else if (entity.type == "idle" || entity.type == "exped") {
                                val h = durationHoursStr.toIntOrNull() ?: 0
                                val m = durationMinutesStr.toIntOrNull() ?: 0
                                params["durationMin"] = h * 60 + m
                                params["countMode"] = countMode
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
        Dialog(onDismissRequest = { showColorMixer = false }) {
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
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(mixedColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = hexStr.uppercase(),
                            color = if ((redVal + greenVal + blueVal) / 3 > 128) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // R, G, B Slider Rows
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorSliderRow(label = "R", color = Color.Red, value = redVal, onValueChange = { redVal = it })
                        ColorSliderRow(label = "G", color = Color.Green, value = greenVal, onValueChange = { greenVal = it })
                        ColorSliderRow(label = "B", color = Color.Blue, value = blueVal, onValueChange = { blueVal = it })
                    }

                    Divider(color = Color(0xFF2A2A3A), thickness = 1.dp)

                    Text(
                        text = "保存先スロットを選択（タップで確定）",
                        fontSize = 12.sp,
                        color = Color(0xFF8A8EA3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target slot row selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        customColors.forEachIndexed { index, slotHex ->
                            val slotColor = Color(android.graphics.Color.parseColor(slotHex))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(slotColor)
                                    .border(2.dp, Color(0xFF2A2A3A), CircleShape)
                                    .clickable {
                                        // Save mixed color to slot
                                        val mutable = customColors.toMutableList()
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A3A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存せず色だけ適用", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSliderRow(
    label: String,
    color: Color,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(16.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End
        )
    }
}
