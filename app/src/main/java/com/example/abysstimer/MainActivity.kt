@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.example.abysstimer

import com.example.abysstimer.ui.theme.ColonFontFamily

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Rect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.abysstimer.data.AppDatabase
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import com.example.abysstimer.ui.*

// Zero-allocation static singleton for transparent text handles
private val TransparentTextSelectionColors = TextSelectionColors(
    handleColor = Color.Transparent,
    backgroundColor = Color.Transparent
)

private val GroupCardShape = RoundedCornerShape(8.dp)
private val TimerCardShape = RoundedCornerShape(10.dp)

private val ColorClaimBorder = Color(0xFF5CD68A)
private val ColorPreviewBorder = Color(0xFFFFD166)
private val ColorFullBorder = Color(0xFFFFAB5C)
private val ColorFullText = Color(0xFFFF6B6B)
private val ColorWarnText = Color(0xFFFFAB5C)
private val ColorNormalText = Color(0xFFECEEF2)
private val JapaneseTextOffsetModifier = Modifier.offset(y = (-1).dp)
private val BadgeShape = RoundedCornerShape(8.dp)
private val BadgeBgColor = Color(255, 255, 255, 18)

// Direct OS InputMethodManager hide ensuring soft keyboard is dismissed instantly with zero race conditions
fun hideKeyboardDirectly(context: Context) {
    var ctx: Context? = context
    var activity: Activity? = null
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            activity = ctx
            break
        }
        ctx = ctx.baseContext
    }
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val view = activity?.currentFocus ?: activity?.window?.decorView
    if (view != null && imm != null) {
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
    activity?.currentFocus?.clearFocus()
}

class MainActivity : ComponentActivity() {
    private val viewModel: TimerViewModel by viewModels {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TimerRepository(database.itemDao())
        TimerViewModelFactory(repository, TimerRepository.inMemoryCache ?: emptyList())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0B0B14"))

        setContent {
            AbyssTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0B14)
                ) {
                    AbyssTimerApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForegrounded()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackgrounded()
    }
}

// --- Composable Themes ---

@Composable
fun AbyssTimerTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF9B8BFF),
        secondary = Color(0xFF5AA9FF),
        tertiary = Color(0xFFA78BFA),
        background = Color(0xFF0B0B14),
        surface = Color(0xFF15151F),
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = Color(0xFFECEEF2),
        onSurface = Color(0xFFECEEF2),
        error = Color(0xFFFF6B6B)
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        content = content
    )
}

// --- Main App UI Layout ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbyssTimerApp(viewModel: TimerViewModel) {
    val uiSnapshot by viewModel.uiSnapshotFlow.collectAsStateWithLifecycle()
    val allItems = uiSnapshot.allItems
    val visibleItems = uiSnapshot.visibleItems
    val customColors by viewModel.customColorsFlow.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var activeSetupType by remember { mutableStateOf<String?>(null) }
    var activeSetupGroupId by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var movingItemId by remember { mutableStateOf<String?>(null) }
    var isDeleteMode by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }

    val keypadState = remember { KeypadEditingState() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isImeVisible = WindowInsets.isImeVisible

    // Unified single-path keypad commit & dismissal handler
    val commitAndDismissKeypad = remember(keypadState) {
        {
            keypadState.commit()
        }
    }

    // Optimize BackHandler enabled check to be a stable derived state
    val isBackHandlerEnabled by remember(keypadState, isImeVisible, isDeleteMode, itemToDelete, movingItemId, showAddDialog, showBackupDialog, activeSetupType, itemToEdit) {
        derivedStateOf {
            keypadState.isEditing || isImeVisible || isDeleteMode || itemToDelete != null || movingItemId != null || showAddDialog || showBackupDialog || activeSetupType != null || itemToEdit != null
        }
    }

    // Predictive Back Handling: Dismiss keypad, cancel editing, delete mode, moving mode or close dialogs
    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            keypadState.isEditing -> {
                commitAndDismissKeypad()
            }
            isImeVisible -> {
                hideKeyboardDirectly(context)
                focusManager.clearFocus(force = true)
            }
            itemToDelete != null -> itemToDelete = null
            isDeleteMode -> isDeleteMode = false
            movingItemId != null -> movingItemId = null
            itemToEdit != null -> itemToEdit = null
            activeSetupType != null -> {
                activeSetupType = null
                activeSetupGroupId = null
            }
            showAddDialog -> {
                showAddDialog = false
                activeSetupGroupId = null
            }
            showBackupDialog -> {
                showBackupDialog = false
            }
        }
    }

    val customColorHexes = remember(customColors) { customColors.map { it.hexColor } }

    val openEditDialog: (ItemEntity) -> Unit = remember(viewModel, commitAndDismissKeypad, keypadState) {
        { entity: ItemEntity ->
            if (keypadState.isEditing) {
                commitAndDismissKeypad()
            } else if (viewModel.cancelPendingStates()) {
                // プレビュー表示中だった場合はプレビューを閉じるのみ（ダイアログは開かない）
            } else {
                itemToEdit = entity
            }
        }
    }

    val onItemClickAction: (ItemEntity) -> Unit = remember(viewModel, commitAndDismissKeypad, keypadState) {
        { entity: ItemEntity ->
            if (keypadState.isEditing) {
                // ガード動作: 編集中の場合は値を確定して閉じるのみ（タップしたカードのアクションは実行しない）
                commitAndDismissKeypad()
            } else if (isDeleteMode) {
                itemToDelete = entity
            } else {
                when (entity.type) {
                    "header" -> {
                        if (!viewModel.cancelPendingStates()) {
                            viewModel.toggleHeaderCollapsed(entity.id)
                        }
                    }
                    "rule" -> {
                        viewModel.cancelPendingStates()
                    }
                    else -> {
                        viewModel.onCardShortTap(entity)
                    }
                }
            }
        }
    }

    val onItemEditAction: (ItemEntity) -> Unit = remember(openEditDialog) {
        { entity: ItemEntity ->
            if (isDeleteMode) {
                itemToDelete = entity
            } else {
                openEditDialog(entity)
            }
        }
    }

    val onFocusChangedAction: (Boolean) -> Unit = remember(viewModel) {
        { focused: Boolean ->
            if (focused) viewModel.cancelPendingStates()
        }
    }

    // Zero-overhead stable Keypad callbacks (completely eliminates StaminaKeypad recomposition during keypresses)
    val onKeypadDigit: (String) -> Unit = remember(keypadState) {
        { d: String -> keypadState.appendDigit(d) }
    }
    val onKeypadClear: () -> Unit = remember(keypadState) {
        { keypadState.clear() }
    }
    val onKeypadDone: () -> Unit = commitAndDismissKeypad
    val onKeypadDismiss: () -> Unit = commitAndDismissKeypad

    val onStartEditingAction: (String) -> Unit = remember(viewModel, keypadState, commitAndDismissKeypad) {
        { id: String ->
            if (keypadState.isEditing) {
                // 既にテンキーが開いている場合は閉じるのみ（別の入力へ直接飛ばない）
                commitAndDismissKeypad()
            } else if (viewModel.cancelPendingStates()) {
                // プレビュー表示中（放置受取や使い切り確認）だった場合はプレビューを閉じるのみ（テンキーは開かない）
            } else {
                val initialVal = viewModel.getItemEntity(id)?.let {
                    TimerEngine.calculateStamInfo(it, System.currentTimeMillis()).cur.toString()
                } ?: ""
                keypadState.start(
                    id = id,
                    field = "cur",
                    initialText = initialVal,
                    maxDigits = 3,
                    onValueChange = null,
                    onCommit = { textVal ->
                        val parsed = textVal.toIntOrNull()
                        if (parsed != null) {
                            viewModel.updateCurrentValue(id, parsed)
                        }
                    }
                )
            }
        }
    }

    val onFinishEditingAction: (String) -> Unit = remember(keypadState) {
        { id: String ->
            if (keypadState.activeId == id) {
                keypadState.finish()
            }
        }
    }

    val onAddChildGroupAction: (ItemEntity) -> Unit = remember(viewModel, commitAndDismissKeypad, keypadState) {
        { groupEntity: ItemEntity ->
            if (keypadState.isEditing) {
                commitAndDismissKeypad()
            } else if (viewModel.cancelPendingStates()) {
                // プレビューを閉じるのみ
            } else if (isDeleteMode) {
                itemToDelete = groupEntity
            } else {
                activeSetupGroupId = groupEntity.id
                showAddDialog = true
            }
        }
    }

    LaunchedEffect(showAddDialog, activeSetupType, showBackupDialog, isDeleteMode) {
        if (showAddDialog || activeSetupType != null || showBackupDialog || isDeleteMode) {
            viewModel.cancelPendingStates()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B14))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed Header Area (32dp height + Status bar)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .height(32.dp)
            ) {
                // Invisible 32dp Touch Area on Top-Left for Emergency Backup / Restore
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterStart)
                        .pointerDownTap {
                            if (!isDeleteMode) {
                                viewModel.cancelPendingStates()
                                showBackupDialog = true
                            }
                        }
                )

                // Delete mode center hint
                if (isDeleteMode) {
                    Text(
                        text = "削除する枠をタップ",
                        color = Color(0xFFFF6B6B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // 32dp Circle Add Button / Delete Mode Toggle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.CenterEnd)
                        .border(
                            width = 1.dp,
                            color = if (isDeleteMode) Color(0xFFFF5252) else Color(255, 255, 255, 25),
                            shape = CircleShape
                        )
                        .background(
                            color = if (isDeleteMode) Color(0xFF451818) else Color(255, 255, 255, 12),
                            shape = CircleShape
                        )
                        .pointerInput(isDeleteMode) {
                            detectTapGestures(
                                onTap = {
                                    if (isDeleteMode) {
                                        isDeleteMode = false
                                    } else {
                                        viewModel.cancelPendingStates()
                                        activeSetupGroupId = null
                                        showAddDialog = true
                                    }
                                },
                                onLongPress = {
                                    viewModel.cancelPendingStates()
                                    isDeleteMode = !isDeleteMode
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isDeleteMode) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = if (isDeleteMode) "削除モード解除" else "枠を追加",
                        tint = if (isDeleteMode) Color(0xFFFF6B6B) else Color(0xFFECEEF2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .pointerDownTap {
                        viewModel.cancelPendingStates()
                    }
            ) {
                // List content (Grid) - Stable persistent container renders instantly without node replacement
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(
                        top = 0.dp,
                        bottom = 120.dp
                    )
                ) {
                        items(
                            items = visibleItems,
                            key = { it.entity.id },
                            span = { ui -> GridItemSpan(ui.gridSpan) },
                            contentType = { it.entity.type }
                        ) { ui ->
                        val isMovingSource = movingItemId == ui.entity.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isMovingSource && ui.entity.type != "group") {
                                        Modifier.border(2.dp, Color(0xFF4DA3FF), RoundedCornerShape(10.dp))
                                    } else if (isDeleteMode && ui.entity.type != "group") {
                                        Modifier.border(1.2.dp, Color(0x66FF5252), RoundedCornerShape(8.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            when (ui.entity.type) {
                                "header" -> {
                                    HeaderCard(
                                        ui = ui,
                                        collapsedCount = ui.collapsedCount,
                                        isMoveMode = movingItemId != null,
                                        onToggleCollapse = onItemClickAction,
                                        onEdit = onItemEditAction
                                    )
                                }
                                "rule" -> {
                                    RuleCard(
                                        ui = ui,
                                        isMoveMode = movingItemId != null,
                                        onEdit = onItemEditAction
                                    )
                                }
                                "group" -> {
                                    GroupCard(
                                        ui = ui,
                                        isMovingSource = isMovingSource,
                                        isMoveMode = movingItemId != null,
                                        keypadState = keypadState, // Pass state holder
                                        onStartEditing = onStartEditingAction,
                                        onFinishEditing = onFinishEditingAction,
                                        onCardTap = onItemClickAction,
                                        onEditChild = onItemEditAction,
                                        onEditGroup = onItemEditAction,
                                        onAddChild = onAddChildGroupAction,
                                        onFocusChanged = onFocusChangedAction
                                    )
                                }
                                else -> {
                                    TimerCard(
                                        ui = ui,
                                        isGroupChild = false,
                                        isClaimPreview = ui.isClaimPreview,
                                        keypadState = keypadState, // Pass state holder
                                        onStartEditing = onStartEditingAction,
                                        onFinishEditing = onFinishEditingAction,
                                        onTap = onItemClickAction,
                                        onEdit = onItemEditAction,
                                        onFocusChanged = onFocusChangedAction
                                    )
                                }
                            }

                            // Full-surface overlay for Move mode selection
                            if (movingItemId != null) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .pointerDownTap {
                                            if (movingItemId != ui.entity.id) {
                                                viewModel.moveItemToTarget(movingItemId!!, ui.entity.id)
                                            }
                                            viewModel.cancelPendingStates()
                                            movingItemId = null
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

    // --- Isolated Dialog Layer ---
    // Prevent main app recompositions from trickling down to dialogs unnecessarily
    DialogLayer(
        viewModel = viewModel,
        keypadState = keypadState, // Passed
        showAddDialog = showAddDialog,
        onDismissAddDialog = {
            showAddDialog = false
            activeSetupGroupId = null
        },
        activeSetupGroupId = activeSetupGroupId,
        activeSetupType = activeSetupType,
        onSetSetupType = { activeSetupType = it },
        onResetSetup = {
            activeSetupType = null
            activeSetupGroupId = null
        },
        itemToEdit = itemToEdit,
        onDismissEditDialog = { itemToEdit = null },
        showBackupDialog = showBackupDialog,
        onDismissBackupDialog = { showBackupDialog = false },
        customColorHexes = customColorHexes,
        onStartMove = { id ->
            viewModel.cancelPendingStates()
            movingItemId = id
        },
        onTriggerAddChild = { groupId ->
            viewModel.cancelPendingStates()
            activeSetupGroupId = groupId
            showAddDialog = true
        }
    )

    itemToDelete?.let { entity ->
        DeleteConfirmDialog(
            itemName = entity.name,
            itemType = entity.type,
            onDismiss = {
                itemToDelete = null
                isDeleteMode = false
            },
            onConfirmDelete = {
                viewModel.deleteItem(entity.id)
                itemToDelete = null
                isDeleteMode = false
            }
        )
    }

    // Overlay Keypad (Isolated container so keypad toggles do not recompose root AbyssTimerApp)
    KeypadOverlayHost(
        keypadState = keypadState,
        onDigit = onKeypadDigit,
        onClear = onKeypadClear,
        onDone = onKeypadDone,
        onDismiss = onKeypadDismiss
    )
    }
}

@Composable
private fun DialogLayer(
    viewModel: TimerViewModel,
    keypadState: KeypadEditingState, // Added
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit,
    activeSetupGroupId: String?,
    activeSetupType: String?,
    onSetSetupType: (String?) -> Unit,
    onResetSetup: () -> Unit,
    itemToEdit: ItemEntity?,
    onDismissEditDialog: () -> Unit,
    showBackupDialog: Boolean,
    onDismissBackupDialog: () -> Unit,
    customColorHexes: List<String>,
    onStartMove: (String?) -> Unit,
    onTriggerAddChild: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    if (showAddDialog) {
        AddPanelDialog(
            isGroupMode = activeSetupGroupId != null,
            onDismiss = onDismissAddDialog,
            onSelectType = { type ->
                onDismissAddDialog()
                if (type == "group") {
                    viewModel.addGroup()
                } else {
                    onSetSetupType(type)
                }
            }
        )
    }

    activeSetupType?.let { type ->
        SetupDialog(
            type = type,
            onDismiss = onResetSetup,
            onConfirm = { params ->
                if (activeSetupGroupId != null) {
                    viewModel.addChildToGroup(
                        groupId = activeSetupGroupId,
                        type = type,
                        intervalMin = params.intervalMin,
                        max = params.max,
                        useChunk = params.useChunk,
                        durationMin = params.durationMin,
                        countMode = params.countMode,
                        orbMode = params.orbMode
                    )
                } else {
                    viewModel.addItem(
                        type = type,
                        intervalMin = params.intervalMin,
                        max = params.max,
                        useChunk = params.useChunk,
                        durationMin = params.durationMin,
                        countMode = params.countMode,
                        orbMode = params.orbMode,
                        color = params.color
                    )
                }
                onResetSetup()
            },
            onRequestKeypad = { field, initial, onCommit ->
                keypadState.start("setup", field, initial, onCommit)
            },
            isKeypadActive = { field ->
                keypadState.isFieldActive("setup", field)
            }
        )
    }

    itemToEdit?.let { entity ->
        val parentGroupName = remember(entity.parentId) {
            viewModel.getGroupNameForChild(entity.parentId)
        }
        EditItemDialog(
            entity = entity,
            customColors = customColorHexes,
            groupName = parentGroupName,
            onDismiss = onDismissEditDialog,
            onSaveName = {
                viewModel.updateItemName(entity.id, it)
                onDismissEditDialog()
            },
            onSaveColor = {
                viewModel.updateItemColor(entity.id, it)
                onDismissEditDialog()
            },
            onSaveCustomColors = { viewModel.saveCustomColors(it) },
            onUpdateSettings = { settings ->
                viewModel.updateItemSettings(
                    id = entity.id,
                    settings = settings
                )
                onDismissEditDialog()
            },
            onDelete = {
                viewModel.deleteItem(entity.id)
                onDismissEditDialog()
            },
            onMoveUp = {},
            onMoveDown = {},
            onStartMove = {
                onDismissEditDialog()
                onStartMove(entity.parentId ?: entity.id)
            },
            onCloneGroup = {
                viewModel.cloneGroup(entity.id)
                onDismissEditDialog()
            },
            onAddChildTimer = {
                onDismissEditDialog()
                onTriggerAddChild(entity.id)
            },
            onAddGroupBelow = {
                viewModel.addGroup()
                onDismissEditDialog()
            },
            onRequestKeypad = { field, initial, onCommit ->
                keypadState.start(entity.id, field, initial, onCommit)
            },
            isKeypadActive = { field ->
                keypadState.isFieldActive(entity.id, field)
            }
        )
    }

    if (showBackupDialog) {
        BackupToastDialog(
            onDismiss = onDismissBackupDialog,
            onCopyBackup = {
                val json = viewModel.exportBackupJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("AbyssTimerBackup", json)
                clipboard?.setPrimaryClip(clip)
            },
            onPasteBackup = { jsonStr ->
                coroutineScope.launch {
                    val ok = viewModel.importBackupJson(jsonStr)
                    if (ok) {
                        onDismissBackupDialog()
                    }
                }
            }
        )
    }
}

// --- Individual Card Composables ---

@Composable
fun HeaderCard(
    ui: TimerUiState,
    collapsedCount: Int,
    isMoveMode: Boolean = false,
    onToggleCollapse: (ItemEntity) -> Unit,
    onEdit: (ItemEntity) -> Unit
) {
    val headerColor = ui.parsedColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerDownTap { onToggleCollapse(ui.entity) }
            .padding(top = 2.dp, bottom = 0.dp, start = 2.dp, end = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(bottom = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = ui.entity.name.ifEmpty { "見出し" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (ui.entity.name.isEmpty()) Color(0xFF8A8EA3) else headerColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = StandardNoPaddingStyle
                )
                if (collapsedCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▸ ${collapsedCount}件",
                        fontSize = 9.5.sp,
                        color = Color(0xFF8A8EA3),
                        fontWeight = FontWeight.Bold,
                        style = StandardNoPaddingStyle,
                        modifier = Modifier
                            .background(BadgeBgColor, BadgeShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                if (ui.entity.foldLock) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Fold Locked",
                        tint = Color(0xFF8A8EA3),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Settings button on right of header (refined subtle solid dot)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .pointerDownTap { onEdit(ui.entity) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(headerColor.copy(alpha = 0.78f))
                )
            }
        }

        // Crisp, visible bottom underline with header theme accent color
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.2.dp)
                .background(headerColor.copy(alpha = 0.35f))
        )
    }
}

@Composable
fun RuleCard(
    ui: TimerUiState,
    isMoveMode: Boolean = false,
    onEdit: (ItemEntity) -> Unit
) {
    val lineColor = ui.parsedColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerDownTap { onEdit(ui.entity) }
            .padding(top = 1.dp, bottom = 1.dp, start = 2.dp, end = 2.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Line bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(lineColor)
        )
    }
}

@Composable
fun GroupCard(
    ui: TimerUiState,
    isMovingSource: Boolean = false,
    isMoveMode: Boolean = false,
    keypadState: KeypadEditingState, // Pass the state holder instead of raw values
    onStartEditing: (String) -> Unit = {},
    onFinishEditing: (String) -> Unit = {},
    onCardTap: (ItemEntity) -> Unit,
    onEditChild: (ItemEntity) -> Unit,
    onEditGroup: (ItemEntity) -> Unit,
    onAddChild: (ItemEntity) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val groupColor = ui.parsedColor
    val isEmpty = ui.children.isEmpty()
    val isMultiChild = ui.children.size > 1
    
    // Header colors: increased visibility and rich gradient (minimum 35% opacity to avoid washed out lines)
    val leftLineBrush = remember(groupColor) {
        Brush.horizontalGradient(
            0.0f to groupColor.copy(alpha = 0.35f),
            1.0f to groupColor.copy(alpha = 1.0f)
        )
    }
    val rightLineBrush = remember(groupColor) {
        Brush.horizontalGradient(
            0.0f to groupColor.copy(alpha = 1.0f),
            1.0f to groupColor.copy(alpha = 0.35f)
        )
    }

    val actualBorder = remember(isMovingSource) {
        if (isMovingSource) {
            BorderStroke(2.dp, Color(0xFF4DA3FF))
        } else {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Surface(
            shape = GroupCardShape,
            color = Color.Transparent,
            border = actualBorder,
            modifier = Modifier.fillMaxWidth()
        ) {
            val topPadding = 12.dp
            val bottomPadding = 2.dp
            val horizPadding = 2.5.dp
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding, bottom = bottomPadding, start = horizPadding, end = horizPadding)
            ) {
                if (isEmpty) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF15151F),
                        border = BorderStroke(1.2.dp, groupColor.copy(alpha = 0.70f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .pointerDownTap { onAddChild(ui.entity) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "＋",
                                color = groupColor.copy(alpha = 0.75f),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    val is2x2Layout = ui.entity.layout == "2x2" && ui.children.size > 2
                    val rows = remember(ui.children, is2x2Layout) {
                        if (is2x2Layout) ui.children.chunked(2) else listOf(ui.children)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        rows.forEach { rowSlots ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowSlots.forEach { slotUi ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        TimerCard(
                                            ui = slotUi,
                                            isGroupChild = true,
                                            isClaimPreview = slotUi.isClaimPreview,
                                            keypadState = keypadState, // Pass state holder
                                            onStartEditing = onStartEditing,
                                            onFinishEditing = onFinishEditing,
                                            onTap = onCardTap,
                                            onEdit = onEditChild,
                                            onFocusChanged = onFocusChanged
                                        )
                                    }
                                }
                                if (is2x2Layout && rowSlots.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header Overlay: Flawless symmetrical centering & Straight horizontal baseline gradient lines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                val strokeW = 1.8.dp

                if (isMultiChild) {
                    // Left Straight Baseline Gradient Line
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .drawBehind {
                                val strokePx = strokeW.toPx()
                                val lineY = 13.5.dp.toPx() // Positioned at the bottom baseline of the name tag, close to timers
                                drawLine(
                                    brush = leftLineBrush,
                                    start = Offset(0f, lineY),
                                    end = Offset(size.width, lineY),
                                    strokeWidth = strokePx,
                                    cap = StrokeCap.Round
                                )
                            }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Center Name Tag (Always mathematically 100% centered between the weights)
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0B0B14), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 0.dp)
                        .height(18.dp)
                        .pointerDownTap { onEditGroup(ui.entity) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ui.entity.name.ifEmpty { "アカウント" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.entity.name.isEmpty()) Color(0xFF8A8EA3) else Color(0xFFE8EAEF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                    )
                }

                if (isMultiChild) {
                    // Right Straight Baseline Gradient Line
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .drawBehind {
                                val strokePx = strokeW.toPx()
                                val lineY = 13.5.dp.toPx() // Positioned at the bottom baseline of the name tag, close to timers
                                drawLine(
                                    brush = rightLineBrush,
                                    start = Offset(0f, lineY),
                                    end = Offset(size.width, lineY),
                                    strokeWidth = strokePx,
                                    cap = StrokeCap.Round
                                )
                            }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
    }
}

@Composable
fun TimerCard(
    ui: TimerUiState,
    isGroupChild: Boolean,
    isClaimPreview: Boolean,
    keypadState: KeypadEditingState, // Pass the state holder
    onStartEditing: (String) -> Unit = {},
    onFinishEditing: (String) -> Unit = {},
    onTap: (ItemEntity) -> Unit,
    onEdit: (ItemEntity) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnStartEditing by rememberUpdatedState(onStartEditing)
    val currentEntity by rememberUpdatedState(ui.entity)

    val onCardTapAction = remember { { currentOnTap(currentEntity) } }
    val onEditAction = remember { { currentOnEdit(currentEntity) } }
    val onStartEditAction = remember { { currentOnStartEditing(currentEntity.id) } }

    // Isolate text observation: ONLY this card observes keypadState.text if it is the active one.
    // This prevents the entire grid from recomposing when typing.
    val editingText by remember(ui.entity.id) {
        derivedStateOf {
            if (keypadState.activeId == ui.entity.id && keypadState.fieldTag == "cur") keypadState.text else null
        }
    }
    val isCurEditing = editingText != null

    val typeColor = ui.parsedColor

    val containerColor = Color(0xFF15151F)

    // Compute border attributes strictly aligned with styles.css definitions
    val isIdleExpedClaim = (ui.entity.type == "idle" || ui.entity.type == "exped") && ui.entity.state == "claim"

    val borderStrokeColor = if (isClaimPreview) {
        ColorPreviewBorder
    } else if (isIdleExpedClaim) {
        ColorClaimBorder
    } else if (ui.isFull) {
        ColorFullBorder
    } else {
        typeColor
    }

    val borderWidth = if (isClaimPreview || isIdleExpedClaim) 2.dp else 1.2.dp
    val borderStroke = remember(borderWidth, borderStrokeColor) {
        BorderStroke(borderWidth, borderStrokeColor)
    }

    Surface(
        shape = TimerCardShape,
        color = containerColor,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerDownTap(enabled = true, onTap = onCardTapAction)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Menu Indicator Dot (Refined subtle solid color dot, naturally nestled in top-left corner without intruding on numbers or clock)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(30.dp)
                    .height(20.dp)
                    .instantPointerTap { onEditAction() }
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp, top = 6.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.70f))
                )
            }

            // 2. Type-Specific Layout
            when (ui.entity.type) {
                "stam", "orb" -> {
                    // Absolute positioned Recovery clock stack at Top-End (neatly aligned at top-right corner, height 18.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 6.dp)
                            .height(18.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TimerLabels(ui = ui)
                    }

                    // Absolute positioned Symmetrically aligned numbers (X / Y) at Bottom-Center with zero-delay pure Text rendering
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth()
                            .height(22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current value container (weight 1f, symmetrical right-aligned)
                        val curColor = if (ui.isFull) {
                            Color(0xFFFF6B6B) // Red when full (MAX)
                        } else if (ui.isWarn) {
                            Color(0xFFFFAB5C) // Orange when less than 2 hours remaining
                        } else {
                            Color(0xFFECEEF2) // Normal white
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .instantPointerTap {
                                    onStartEditAction()
                                },
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val displayText = if (isCurEditing) {
                                if (!editingText.isNullOrEmpty()) editingText ?: "_" else "_"
                            } else {
                                "${ui.calculatedCurrent}"
                            }
                            val textColor = if (isCurEditing) {
                                Color(0xFFFFD166) // 編集中はゴールドで光る
                            } else {
                                curColor
                            }
                            Text(
                                text = displayText,
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }

                        // Slash "/" centered with balanced font size & subtle thickness
                        Text(
                            text = "/",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA0AFC4),
                            modifier = Modifier.padding(horizontal = 3.dp),
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )

                        // Max value (symmetrical left-aligned, bright airy mint green with high readability on dark background)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "${ui.entity.max}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA6F4E0),
                                style = androidx.compose.ui.text.TextStyle(
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }
                "idle", "exped" -> {
                    // Clock displays at top right (neatly aligned at top-right corner, height 18.dp)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 1.dp, end = 6.dp)
                            .height(18.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        SpacedTimerText(
                            text = ui.fullAtText,
                            fontSize = 11.5.sp,
                            fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFFC4B5FD),
                            colonPadding = 1.dp
                        )
                    }

                    // Main display label in the bottom center (aligned identically to stamina digits with pure Row centering)
                    val isClaimState = ui.entity.state == "claim"

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth()
                            .height(22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val labelText = ui.idleDisplayLabel

                        val labelColor = if (isClaimState) {
                            ColorClaimBorder
                        } else if (ui.isFull) {
                            ColorFullText
                        } else if (ui.isWarn) {
                            ColorWarnText
                        } else {
                            ColorNormalText
                        }

                        val isJapaneseText = isClaimState || (ui.isFull && ui.entity.type == "exped")
                        val labelSize = if (isJapaneseText) {
                            16.sp
                        } else if (ui.isFull) {
                            17.sp
                        } else {
                            18.sp
                        }

                        val labelModifier = if (isJapaneseText) {
                            JapaneseTextOffsetModifier
                        } else {
                            Modifier
                        }

                        SpacedTimerText(
                            text = labelText,
                            fontSize = labelSize,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            colonPadding = 3.2.dp,
                            useCenterColonGrid = true,
                            modifier = labelModifier
                        )
                    }
                }
            }
        }
    }
}

