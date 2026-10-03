@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example.abysstimer.ui

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalFocusTracker = compositionLocalOf<(Boolean) -> Unit> { {} }

val DialogBiasAlignment = androidx.compose.ui.BiasAlignment(0f, -0.2f)
val ToastCardShape = RoundedCornerShape(14.dp)
val ToastRowShape = RoundedCornerShape(6.dp)
val ToastInputShape = RoundedCornerShape(4.dp)
val ToastActionBtnShape = RoundedCornerShape(8.dp)

@Composable
fun FastDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        contentAlignment = DialogBiasAlignment
    ) {
        // Transparent backdrop capturing outside taps instantly on press (0ms response)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerDownTap { currentOnDismiss() }
        )

        // Dialog Content - perfectly isolated, zero gesture lag
        Box {
            content()
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

/**
 * Common color parser with safe fallback.
 */
fun String?.toComposeColor(default: Color = Color(0xFF52617A)): Color {
    if (this == null) return default
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        default
    }
}

/**
 * Unified Toast Dialog Action Button
 */
@Composable
fun ToastActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
    fontSize: TextUnit = 12.5.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    onClick: () -> Unit
) {
    val actionBorder = remember(color) {
        BorderStroke(1.dp, color.copy(alpha = 0.35f))
    }
    val actionBg = remember(color) {
        color.copy(alpha = 0.10f)
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(ToastActionBtnShape)
            .background(actionBg)
            .border(actionBorder, ToastActionBtnShape)
            .pointerDownTap { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}

private val NameEditTextStyle = TextStyle(
    color = Color.White,
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium
)
private val NameEditBoxShape = RoundedCornerShape(8.dp)
private val NameEditBtnShape = RoundedCornerShape(6.dp)
private val NameEditCancelBorder = BorderStroke(1.dp, Color(255, 255, 255, 18))

/**
 * Ultra-lightweight name edit sub-content for Toast Dialogs
 */
@Composable
fun NameEditContent(
    title: String,
    initialName: String,
    accentColor: Color,
    onSave: (String) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember(initialName) { mutableStateOf(initialName) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = NameEditTextStyle,
            singleLine = true,
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onSave(text.trim())
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(NameEditBoxShape)
                .background(Color(0xFF0E0F14))
                .border(1.2.dp, accentColor, NameEditBoxShape)
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "名称を入力...",
                            color = Color(0xFF6E7282),
                            fontSize = 13.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(NameEditBtnShape)
                    .background(Color(0xFF22252D))
                    .border(NameEditCancelBorder, NameEditBtnShape)
                    .pointerDownTap { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text("キャンセル", color = Color(0xFFC5C8D4), fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }

            val confirmBorder = remember(accentColor) { BorderStroke(1.2.dp, accentColor) }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(NameEditBtnShape)
                    .background(accentColor.copy(alpha = 0.25f))
                    .border(confirmBorder, NameEditBtnShape)
                    .pointerDownTap { onSave(text.trim()) },
                contentAlignment = Alignment.Center
            ) {
                Text("保存", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Unified Toast Delete Button with 2-Step Confirmation
 */
@Composable
fun ToastDeleteButton(
    isConfirming: Boolean,
    onToggleConfirm: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isConfirming) Color(0x33EF4444) else Color(0x1AEF4444))
            .border(
                BorderStroke(
                    1.dp,
                    if (isConfirming) Color(0xFFEF4444) else Color(0x59EF4444)
                ),
                RoundedCornerShape(8.dp)
            )
            .pointerDownTap {
                if (isConfirming) {
                    onDelete()
                } else {
                    onToggleConfirm(true)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isConfirming) "本当に削除しますか？" else "削除",
            color = Color(0xFFEF4444),
            fontSize = if (isConfirming) 11.5.sp else 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ToastDialogContainer(
    groupName: String,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val displayName = remember(groupName) {
        if (groupName.isBlank()) "アカウント" else groupName
    }

    val containerBorder = remember(borderColor) {
        BorderStroke(1.2.dp, borderColor)
    }

    Surface(
        modifier = Modifier.width(280.dp),
        shape = ToastCardShape,
        color = Color(0xFF1B1D22),
        border = containerBorder
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 2.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = displayName,
                color = Color(0xFFE8EAEF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
            )
            content()
        }
    }
}

@Composable
fun WebSetupFieldContainer(
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
fun WebSetupInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.width(44.dp),
    placeholder: String = "",
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val borderColor = if (isActive) Color(0xFFFFD166) else Color(255, 255, 255, 46)
    val borderWidth = if (isActive) 1.2.dp else 1.dp
    val textColor = if (isActive) Color(0xFFFFD166) else Color.White

    Box(
        modifier = modifier
            .height(24.dp)
            .background(Color(0x59000000), RoundedCornerShape(5.dp))
            .border(BorderStroke(borderWidth, borderColor), RoundedCornerShape(5.dp))
            .then(
                if (onClick != null) {
                    Modifier.pointerDownTap { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty() && placeholder.isNotEmpty() && !isActive) {
            Text(placeholder, color = Color(0xFF606775), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        } else {
            val displayVal = if (isActive && value.isEmpty()) "_" else value
            Text(
                text = displayVal,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ColorSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    accentColor: Color
) {
    var isEditing by remember { mutableStateOf(false) }
    var editStr by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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

        if (isEditing) {
            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current
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
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        isEditing = false
                    }
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

private val ToastInputTextStyle = TextStyle(
    color = Color.White,
    fontSize = 13.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center
)

private val ToastSmallInputTextStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center
)

@Composable
fun ToastItemRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    allowEmpty: Boolean = false,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor = if (isActive) Color(0xFFFFD166) else Color(255, 255, 255, 18)
    val borderWidth = if (isActive) 1.2.dp else 1.dp

    Row(
        modifier = modifier
            .height(34.dp)
            .background(Color(0xFF22252D), ToastRowShape)
            .border(borderWidth, borderColor, ToastRowShape)
            .then(
                if (onClick != null) {
                    Modifier.pointerDownTap { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color(0xFFC5C8D4), fontWeight = FontWeight.Medium)
        
        if (onClick != null) {
            val textColor = if (isActive) Color(0xFFFFD166) else if (value.isEmpty()) Color(0xFF6E7282) else Color.White
            val displayVal = if (isActive && value.isEmpty()) "_" else if (value.isEmpty() && placeholder.isNotEmpty()) placeholder else value
            Text(
                text = displayVal,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp)
            )
        } else {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    onValueChange(input.filter { it.isDigit() }.take(3))
                },
                textStyle = ToastInputTextStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .width(48.dp)
                    .height(24.dp)
                    .background(Color(0xFF0E0F14), ToastInputShape)
                    .border(1.dp, Color(255, 255, 255, 30), ToastInputShape),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = Color(0xFF6E7282), fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun ToastTimeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    allowEmpty: Boolean = false,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val borderColor = if (isActive) Color(0xFFFFD166) else Color(255, 255, 255, 30)
    val borderWidth = if (isActive) 1.2.dp else 1.dp

    Box(
        modifier = modifier
            .height(24.dp)
            .background(Color(0xFF0E0F14), ToastInputShape)
            .border(borderWidth, borderColor, ToastInputShape)
            .then(
                if (onClick != null) {
                    Modifier.pointerDownTap { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (onClick != null) {
            val textColor = if (isActive) Color(0xFFFFD166) else if (value.isEmpty()) Color(0xFF6E7282) else Color.White
            val displayVal = if (isActive && value.isEmpty()) "_" else if (value.isEmpty() && placeholder.isNotEmpty()) placeholder else value
            Text(
                text = displayVal,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        } else {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }
                    onValueChange(digits)
                },
                textStyle = ToastSmallInputTextStyle,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                ),
                singleLine = true,
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField: @Composable () -> Unit ->
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, color = Color(0xFF6E7282), fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun ClearOnFocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    isDigitOnly: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    if (onClick != null) {
        Box(
            modifier = modifier
                .background(Color(0xFF2C2F3A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(255, 255, 255, 12), RoundedCornerShape(8.dp))
                .pointerDownTap { onClick() }
                .padding(10.dp)
        ) {
            Column {
                Text(label, color = Color(0xFF8A8EA3), fontSize = 11.sp)
                Text(
                    text = if (value.isEmpty()) " " else value,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    } else {
        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                val filtered = if (isDigitOnly) input.filter { it.isDigit() }.take(3) else input
                onValueChange(filtered)
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done,
                autoCorrect = (keyboardType == KeyboardType.Text)
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF52617A)
            ),
            modifier = modifier
        )
    }
}
