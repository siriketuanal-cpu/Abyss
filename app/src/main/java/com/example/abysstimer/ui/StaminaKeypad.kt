package com.example.abysstimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- Keypad Dedicated State Holder (Isolated State Architecture to prevent Recomposition Storms) ---
@Stable
class KeypadEditingState {
    var activeId by mutableStateOf<String?>(null)
        private set
    var fieldTag by mutableStateOf<String?>(null)
        private set
    var text by mutableStateOf("")
        private set
    private var fallbackText: String = ""
    private var maxDigits: Int = 3
    private var onValueChangeCallback: ((String) -> Unit)? = null
    private var onCommitCallback: ((String) -> Unit)? = null

    val isEditing: Boolean get() = activeId != null

    fun isFieldActive(id: String, field: String? = null): Boolean {
        return activeId == id && (field == null || fieldTag == field)
    }

    fun start(
        id: String,
        field: String? = null,
        initialText: String = "",
        onValueChange: ((String) -> Unit)? = null
    ) {
        start(id, field, initialText, 3, onValueChange, onValueChange)
    }

    fun start(
        id: String,
        field: String? = null,
        initialText: String = "",
        maxDigits: Int = 3,
        onValueChange: ((String) -> Unit)? = null
    ) {
        start(id, field, initialText, maxDigits, onValueChange, onValueChange)
    }

    fun start(
        id: String,
        field: String? = null,
        initialText: String = "",
        maxDigits: Int = 3,
        onValueChange: ((String) -> Unit)? = null,
        onCommit: ((String) -> Unit)? = null
    ) {
        activeId = id
        fieldTag = field
        this.maxDigits = maxDigits
        this.fallbackText = initialText
        this.text = "" // フォーカス時は空欄からスタート
        onValueChangeCallback = onValueChange
        onCommitCallback = onCommit
        onValueChangeCallback?.invoke("")
    }

    fun appendDigit(d: String) {
        if (text.length < maxDigits) {
            text += d
            onValueChangeCallback?.invoke(text)
        }
    }

    fun clear() {
        text = ""
        onValueChangeCallback?.invoke("")
    }

    fun commit() {
        val currentText = if (text.isEmpty()) fallbackText else text
        onValueChangeCallback?.invoke(currentText)
        onCommitCallback?.invoke(currentText)
        finish()
    }

    fun finish() {
        activeId = null
        fieldTag = null
        text = ""
        fallbackText = ""
        maxDigits = 3
        onValueChangeCallback = null
        onCommitCallback = null
    }
}

// Keypad layout constants
val KeypadTopShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

private val KeypadRowsData = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("AC", "0", "確定")
)

private val ColorKeyBgConfirm = Color(0xFF3868C8)
private val ColorKeyBgClear = Color(0xFF282028)
private val ColorKeyBgDefault = Color(0xFF1E202E)

private val ColorKeyBorderConfirm = Color(0xFF5B8EF5)
private val ColorKeyBorderClear = Color(0xFF4A2F3D)
private val ColorKeyBorderDefault = Color(0xFF2B2E42)

private val ColorKeyTextConfirm = Color.White
private val ColorKeyTextClear = Color(0xFFFF7B7B)
private val ColorKeyTextDefault = Color(0xFFECEEF2)

@Immutable
data class KeypadKeySpec(
    val key: String,
    val bgColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val fontSize: androidx.compose.ui.unit.TextUnit
)

private val PrecomputedKeypadGrid: List<List<KeypadKeySpec>> = KeypadRowsData.map { row ->
    row.map { key ->
        val isConfirm = key == "確定"
        val isClear = key == "AC"
        val keyBgColor = if (isConfirm) ColorKeyBgConfirm else if (isClear) ColorKeyBgClear else ColorKeyBgDefault
        val keyBorderColor = if (isConfirm) ColorKeyBorderConfirm else if (isClear) ColorKeyBorderClear else ColorKeyBorderDefault
        val keyTextColor = if (isConfirm) ColorKeyTextConfirm else if (isClear) ColorKeyTextClear else ColorKeyTextDefault
        val fontSize = if (isConfirm) 16.sp else if (isClear) 18.sp else 22.sp
        KeypadKeySpec(key, keyBgColor, keyBorderColor, keyTextColor, fontSize)
    }
}

private val CloseIconVector = Icons.Default.Close
private val KeypadTextStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

/**
 * In-app stamina/numeric keypad (zero dead space, seamless touch grid).
 */
@Composable
fun StaminaKeypad(
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDigit by rememberUpdatedState(onDigit)
    val currentOnClear by rememberUpdatedState(onClear)
    val currentOnDone by rememberUpdatedState(onDone)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(KeypadTopShape)
            .background(Color(0xFF13141F))
            .border(1.dp, Color(0xFF2C2F44), KeypadTopShape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
        ) {
            // Ultra-slim close handle bar (24dp height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF222433))
                        .instantPointerTap { currentOnDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CloseIconVector,
                        contentDescription = "閉じる",
                        tint = Color(0xFFA0AFC4),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            // Keypad Grid (3 columns x 4 rows) - 0dp spacing for 100% active touch coverage
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                PrecomputedKeypadGrid.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowKeys.forEach { spec ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .background(spec.bgColor)
                                    .border(0.5.dp, spec.borderColor)
                                    .instantPointerTap {
                                        when (spec.key) {
                                            "AC" -> currentOnClear()
                                            "確定" -> currentOnDone()
                                            else -> currentOnDigit(spec.key)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = spec.key,
                                    fontSize = spec.fontSize,
                                    fontWeight = FontWeight.Bold,
                                    color = spec.textColor,
                                    style = KeypadTextStyle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overlay host for keypad editing modal backdrop and positioning.
 */
@Composable
fun KeypadOverlayHost(
    keypadState: KeypadEditingState,
    onDigit: (String) -> Unit,
    onClear: () -> Unit,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    if (keypadState.isEditing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerDownTap {
                    onDismiss()
                }
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            StaminaKeypad(
                onDigit = onDigit,
                onClear = onClear,
                onDone = onDone,
                onDismiss = onDismiss
            )
        }
    }
}
