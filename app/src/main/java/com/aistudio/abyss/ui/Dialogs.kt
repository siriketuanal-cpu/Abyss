package com.aistudio.abyss.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.abyss.model.GroupItem
import com.aistudio.abyss.model.HeaderItem
import com.aistudio.abyss.model.TimerCardItem
import com.aistudio.abyss.model.abyssMaxFor

val HEADER_COLORS = listOf(
    "#9B8BFF",
    "#B48CFF",
    "#6FC7FF",
    "#70D6B0",
    "#FFD166",
    "#FF9F68",
    "#FF7B9C",
    "#D7DBE7"
)

val RULE_COLORS = listOf(
    "#52617A",
    "#9B8BFF",
    "#5AA9FF",
    "#FFAB5C",
    "#5CD68A",
    "#FF6B6B"
)

@Composable
fun AddPanelDialog(
    parentGroupId: String?,
    onSelectGroup: () -> Unit,
    onSelectHeader: () -> Unit,
    onSelectRule: () -> Unit,
    onSelectStam: () -> Unit,
    onSelectAbyss: () -> Unit,
    onSelectIdle: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (parentGroupId != null) "タイマーの種類を選択" else "追加する項目を選択",
                    color = AbyssColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                if (parentGroupId != null) {
                    // Timer options for inside an account group
                    AddOptionButton("スタミナ", AbyssColors.Blue, onSelectStam)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddOptionButton("Abyssスタミナ", AbyssColors.AbyssPurple, onSelectAbyss)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddOptionButton("放置報酬", AbyssColors.Orange, onSelectIdle)
                } else {
                    // Top-level layout options
                    AddOptionButton("アカウント枠", AbyssColors.Accent, onSelectGroup)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddOptionButton("見出し", AbyssColors.Accent, onSelectHeader)
                    Spacer(modifier = Modifier.height(8.dp))
                    AddOptionButton("仕切り線", AbyssColors.Sub, onSelectRule)
                }

                Spacer(modifier = Modifier.height(14.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("閉じる", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
private fun AddOptionButton(label: String, accentColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Card2),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_add_$label")
    ) {
        Text(text = label, color = accentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SetupTimerDialog(
    type: String,
    onConfirmStam: (Int) -> Unit,
    onConfirmAbyss: (Int) -> Unit,
    onConfirmIdle: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var intervalStr by remember { mutableStateOf(if (type == "abyss") "3" else "5") }
    var hoursStr by remember { mutableStateOf("12") }
    var minsStr by remember { mutableStateOf("0") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = when (type) {
                    "abyss" -> "Abyssスタミナ回復間隔"
                    "stam" -> "スタミナ回復間隔"
                    else -> "満タンまでの時間"
                }
                Text(
                    text = title,
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (type == "idle") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = hoursStr,
                            onValueChange = { hoursStr = it.filter { c -> c.isDigit() }.take(3) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AbyssColors.Text,
                                unfocusedTextColor = AbyssColors.Text,
                                focusedBorderColor = AbyssColors.Accent,
                                unfocusedBorderColor = AbyssColors.Line
                            ),
                            modifier = Modifier.width(70.dp),
                            singleLine = true
                        )
                        Text(" h ", color = AbyssColors.Sub, fontSize = 15.sp)
                        OutlinedTextField(
                            value = minsStr,
                            onValueChange = { minsStr = it.filter { c -> c.isDigit() }.take(2) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AbyssColors.Text,
                                unfocusedTextColor = AbyssColors.Text,
                                focusedBorderColor = AbyssColors.Accent,
                                unfocusedBorderColor = AbyssColors.Line
                            ),
                            modifier = Modifier.width(70.dp),
                            singleLine = true
                        )
                        Text(" m", color = AbyssColors.Sub, fontSize = 15.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = intervalStr,
                            onValueChange = { intervalStr = it.filter { c -> c.isDigit() }.take(3) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AbyssColors.Text,
                                unfocusedTextColor = AbyssColors.Text,
                                focusedBorderColor = AbyssColors.Accent,
                                unfocusedBorderColor = AbyssColors.Line
                            ),
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("分で1回復", color = AbyssColors.Sub, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        when (type) {
                            "stam" -> {
                                val interval = intervalStr.toIntOrNull()?.coerceIn(1, 999) ?: 5
                                onConfirmStam(interval)
                            }
                            "abyss" -> {
                                val interval = intervalStr.toIntOrNull()?.coerceIn(1, 999) ?: 3
                                onConfirmAbyss(interval)
                            }
                            "idle" -> {
                                val h = hoursStr.toIntOrNull()?.coerceIn(0, 999) ?: 12
                                val m = minsStr.toIntOrNull()?.coerceIn(0, 59) ?: 0
                                val totalMin = (h * 60 + m).coerceAtLeast(1)
                                onConfirmIdle(totalMin)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("追加", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
fun HeaderColorDialog(
    initialColor: String,
    title: String = "ゲーム名ヘッダーの文字色",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HEADER_COLORS.take(4).forEach { hex ->
                        ColorSwatch(
                            hex = hex,
                            isSelected = selectedColor.equals(hex, ignoreCase = true),
                            onClick = { selectedColor = hex }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HEADER_COLORS.drop(4).forEach { hex ->
                        ColorSwatch(
                            hex = hex,
                            isSelected = selectedColor.equals(hex, ignoreCase = true),
                            onClick = { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onConfirm(selectedColor) },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("決定", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
fun RuleColorDialog(
    initialColor: String = "#52617A",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "仕切り線の色",
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RULE_COLORS.forEach { hex ->
                        ColorSwatch(
                            hex = hex,
                            isSelected = selectedColor.equals(hex, ignoreCase = true),
                            onClick = { selectedColor = hex }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onConfirm(selectedColor) },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("追加", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color.White else AbyssColors.Line,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun EditNameDialog(
    title: String,
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AbyssColors.Text,
                        unfocusedTextColor = AbyssColors.Text,
                        focusedBorderColor = AbyssColors.Accent,
                        unfocusedBorderColor = AbyssColors.Line
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onConfirm(text) },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
fun EditNumberDialog(
    title: String,
    currentValue: Int,
    min: Int,
    max: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentValue.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AbyssColors.Text,
                        unfocusedTextColor = AbyssColors.Text,
                        focusedBorderColor = AbyssColors.Accent,
                        unfocusedBorderColor = AbyssColors.Line
                    ),
                    modifier = Modifier.width(120.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val num = text.toIntOrNull()?.coerceIn(min, max) ?: currentValue
                        onConfirm(num)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
fun EditRankDialog(
    currentRank: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentRank.toString()) }
    val rankVal = text.toIntOrNull()?.coerceIn(1, 200) ?: 1
    val previewMax = abyssMaxFor(rankVal)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ランク編集 (Lv.1〜200)",
                    color = AbyssColors.Sub,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "最大スタミナ: $previewMax",
                    color = AbyssColors.Orange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AbyssColors.Text,
                        unfocusedTextColor = AbyssColors.Text,
                        focusedBorderColor = AbyssColors.Accent,
                        unfocusedBorderColor = AbyssColors.Line
                    ),
                    modifier = Modifier.width(100.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { onConfirm(rankVal) },
                    colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Accent),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存", color = Color(0xFF100C26), fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("キャンセル", color = AbyssColors.Sub)
                }
            }
        }
    }
}

@Composable
fun ActionMenuDialog(
    item: com.aistudio.abyss.model.TimerEntry,
    parentGroupId: String?,
    onAddTimerToGroup: () -> Unit,
    onChangeHeaderColor: () -> Unit,
    onEditName: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var armedDelete by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = AbyssColors.Card,
            border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val title = when (item) {
                    is GroupItem -> "アカウントの操作"
                    is HeaderItem -> "見出しの操作"
                    is TimerCardItem -> "タイマーの操作"
                    else -> "仕切り線の操作"
                }
                Text(
                    text = title,
                    color = AbyssColors.Text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                if (item is GroupItem && item.children.size < 2) {
                    Button(
                        onClick = onAddTimerToGroup,
                        colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Card2),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("タイマー追加", color = AbyssColors.Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (item is HeaderItem) {
                    Button(
                        onClick = onChangeHeaderColor,
                        colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Card2),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("色を変更", color = AbyssColors.Accent, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (item is GroupItem || item is HeaderItem) {
                    Button(
                        onClick = onEditName,
                        colors = ButtonDefaults.buttonColors(containerColor = AbyssColors.Card2),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("名前を変更", color = AbyssColors.Text, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2-step armed delete button
                Button(
                    onClick = {
                        if (!armedDelete) {
                            armedDelete = true
                        } else {
                            onDelete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (armedDelete) AbyssColors.Danger else AbyssColors.Card2
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = if (armedDelete) {
                        androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, AbyssColors.Line)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("btn_delete_confirm")
                ) {
                    Text(
                        text = if (armedDelete) "本当に削除しますか？ (タップで確定)" else "削除",
                        color = if (armedDelete) Color.White else AbyssColors.Danger,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("やめる", color = AbyssColors.Sub)
                }
            }
        }
    }
}
