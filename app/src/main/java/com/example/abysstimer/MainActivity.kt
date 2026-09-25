@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.abysstimer

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

// Extension to trigger action instantly on pointerdown
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

// Low-level pointer tap: intercepts and consumes press immediately at Initial pass
// completely bypassing gesture disambiguation delays and preventing parent container interference
fun Modifier.instantPointerTap(
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(onTap) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press) {
                event.changes.forEach { it.consume() }
                onTap()
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(android.graphics.Color.parseColor("#0B0B14"))

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TimerRepository(database.itemDao())
        val factory = TimerViewModelFactory(repository)
        val viewModel: TimerViewModel by viewModels { factory }

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
}

// --- Composable Themes ---
@Composable
fun TimerLabels(ui: TimerUiState) {
    if (ui.entity.type == "orb" && !ui.isFull) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "次",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFA78BFA),
                    modifier = Modifier.padding(end = 2.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = ui.orbNextCdText,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFECEEF2),
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
            Text(
                text = ui.fullAtText,
                fontSize = 10.5.sp,
                fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFF9B8BFF),
                style = androidx.compose.ui.text.TextStyle(
                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    } else {
        Text(
            text = ui.fullAtText,
            fontSize = 11.5.sp,
            fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFF9B8BFF),
            style = androidx.compose.ui.text.TextStyle(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}
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
    val uiItems by viewModel.uiItemsFlow.collectAsStateWithLifecycle()
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val customColors by viewModel.customColorsFlow.collectAsStateWithLifecycle()
    val pendingChunkId by viewModel.pendingChunkUseId.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var activeSetupType by remember { mutableStateOf<String?>(null) }
    var activeSetupGroupId by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var movingItemId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle instant refresh: Millisecond-precise calculation the instant user returns to app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNow()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Predictive Back Handling: Cancel moving mode or close dialogs
    BackHandler(enabled = movingItemId != null || showAddDialog || showBackupDialog || activeSetupType != null || itemToEdit != null) {
        when {
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

    // Global focus tracking for defensive tap behavior
    var isAnyFocused by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalFocusTracker provides { isAnyFocused = it }) {
        Scaffold(
            modifier = Modifier.pointerInput(isAnyFocused) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (isAnyFocused && event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            focusManager.clearFocus()
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            },
            topBar = {
                // Fixed Header Area (32dp height)
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
                                showBackupDialog = true
                            }
                    )

                    // 32dp Circle Add Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.CenterEnd)
                            .border(1.dp, Color(255, 255, 255, 25), CircleShape)
                            .background(Color(255, 255, 255, 12), CircleShape)
                            .pointerDownTap {
                                activeSetupGroupId = null
                                showAddDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "枠を追加",
                            tint = Color(0xFFECEEF2),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            containerColor = Color(0xFF0B0B14)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                keyboardController?.hide()
                                focusManager.clearFocus(force = true)
                                // Only cancel if we are NOT in move mode
                                if (movingItemId == null) {
                                    viewModel.cancelPendingStates()
                                }
                            }
                        )
                    }
            ) {
                // List content (Grid) - Renders instantly on the very first frame
                if (uiItems.isEmpty()) {
                    // 空の時は余計なプレースホルダーを出さず、通常背景のみ（右上の＋で追加可能）
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    val visibleItems = remember(uiItems) {
                        val list = ArrayList<TimerUiState>(uiItems.size)
                        var skipUntilNextHeader = false
                        for (ui in uiItems) {
                            if (ui.entity.type == "header") {
                                skipUntilNextHeader = ui.entity.collapsed && !ui.entity.foldLock
                                list.add(ui)
                            } else {
                                if (!skipUntilNextHeader) {
                                    list.add(ui)
                                }
                            }
                        }
                        list
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = paddingValues.calculateBottomPadding() + 80.dp
                        )
                    ) {
                        items(
                            items = visibleItems,
                            key = { it.entity.id },
                            span = { ui ->
                                val spanVal = when (ui.entity.type) {
                                    "header", "rule" -> 4
                                    "group" -> {
                                        val count = ui.children.size
                                        if (ui.entity.layout == "2x2" && count > 2) 2 else Math.min(4, Math.max(1, count))
                                    }
                                    else -> 1
                                }
                                GridItemSpan(spanVal)
                            },
                            contentType = { it.entity.type }
                        ) { ui ->
                        val isMovingSource = movingItemId == ui.entity.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isMovingSource && ui.entity.type != "group") {
                                        Modifier.border(2.dp, Color(0xFF4DA3FF), RoundedCornerShape(10.dp))
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
                                        onToggleCollapse = {
                                            viewModel.toggleHeaderCollapsed(ui.entity.id)
                                        },
                                        onEdit = {
                                            itemToEdit = ui.entity
                                        }
                                    )
                                }
                                "rule" -> {
                                    RuleCard(
                                        ui = ui,
                                        isMoveMode = movingItemId != null,
                                        onEdit = {
                                            itemToEdit = ui.entity
                                        }
                                    )
                                }
                                "group" -> {
                                    GroupCard(
                                        ui = ui,
                                        isMovingSource = isMovingSource,
                                        isMoveMode = movingItemId != null,
                                        pendingChunkId = pendingChunkId,
                                        onCardTap = { child ->
                                            viewModel.onCardShortTap(child)
                                        },
                                        onEditChild = { itemToEdit = it },
                                        onEditGroup = {
                                            itemToEdit = ui.entity
                                        },
                                        onQuickEditValue = { entity, field, newVal ->
                                            if (field == "cur") {
                                                viewModel.updateCurrentValue(entity.id, newVal)
                                            } else {
                                                viewModel.updateMaxValue(entity.id, newVal)
                                            }
                                        },
                                        onAddChild = {
                                            activeSetupGroupId = ui.entity.id
                                            showAddDialog = true
                                        },
                                        onFocusChanged = { isAnyFocused = it }
                                    )
                                }
                                else -> {
                                    TimerCard(
                                        ui = ui,
                                        isGroupChild = false,
                                        isClaimPreview = ui.isClaimPreview,
                                        onTap = {
                                            viewModel.onCardShortTap(ui.entity)
                                        },
                                        onEdit = {
                                            itemToEdit = ui.entity
                                        },
                                        onQuickEditValue = { field, newVal ->
                                            if (field == "cur") {
                                                viewModel.updateCurrentValue(ui.entity.id, newVal)
                                            } else {
                                                viewModel.updateMaxValue(ui.entity.id, newVal)
                                            }
                                        },
                                        onFocusChanged = { isAnyFocused = it }
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
                                            movingItemId = null
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs & Panels ---

    if (showAddDialog) {
        AddPanelDialog(
            isGroupMode = activeSetupGroupId != null,
            onDismiss = {
                showAddDialog = false
                activeSetupGroupId = null
            },
            onSelectType = { type ->
                showAddDialog = false
                if (type == "group") {
                    viewModel.addGroup()
                } else {
                    activeSetupType = type
                }
            }
        )
    }

    activeSetupType?.let { type ->
        SetupDialog(
            type = type,
            onDismiss = {
                activeSetupType = null
                activeSetupGroupId = null
            },
            onConfirm = { map ->
                if (activeSetupGroupId != null) {
                    viewModel.addChildToGroup(
                        groupId = activeSetupGroupId!!,
                        type = type,
                        intervalMin = map["intervalMin"] as? Int ?: 5,
                        max = map["max"] as? Int ?: 100,
                        useChunk = map["useChunk"] as? Int,
                        durationMin = map["durationMin"] as? Int ?: 0,
                        countMode = map["countMode"] as? String ?: "down",
                        orbMode = map["orbMode"] as? String ?: "down"
                    )
                } else {
                    viewModel.addItem(
                        type = type,
                        intervalMin = map["intervalMin"] as? Int ?: 5,
                        max = map["max"] as? Int ?: 100,
                        useChunk = map["useChunk"] as? Int,
                        durationMin = map["durationMin"] as? Int ?: 0,
                        countMode = map["countMode"] as? String ?: "down",
                        orbMode = map["orbMode"] as? String ?: "down",
                        color = map["color"] as? String
                    )
                }
                activeSetupType = null
                activeSetupGroupId = null
            }
        )
    }

    itemToEdit?.let { entity ->
        EditItemDialog(
            entity = entity,
            customColors = customColors.map { it.hexColor },
            onDismiss = { itemToEdit = null },
            onSaveName = {
                viewModel.updateItemName(entity.id, it)
                itemToEdit = null
            },
            onSaveColor = {
                viewModel.updateItemColor(entity.id, it)
                itemToEdit = null
            },
            onSaveCustomColors = { viewModel.saveCustomColors(it) },
            onUpdateSettings = { map ->
                viewModel.updateItemSettings(
                    id = entity.id,
                    intervalMin = map["intervalMin"] as? Int,
                    max = map["max"] as? Int,
                    useChunk = map["useChunk"] as? Int,
                    useChunkClear = (map["useChunkClear"] as? Boolean) ?: (map.containsKey("useChunk") && map["useChunk"] == null),
                    durationMin = map["durationMin"] as? Int,
                    countMode = map["countMode"] as? String,
                    orbMode = map["orbMode"] as? String,
                    layout = map["layout"] as? String,
                    foldLock = map["foldLock"] as? Boolean,
                    orbEditHours = map["orbEditHours"] as? Int,
                    orbEditMinutes = map["orbEditMinutes"] as? Int,
                    idleEditHours = map["idleEditHours"] as? Int,
                    idleEditMinutes = map["idleEditMinutes"] as? Int
                )
                itemToEdit = null
            },
            onDelete = {
                viewModel.deleteItem(entity.id)
                itemToEdit = null
            },
            onMoveUp = { /* Unused: viewModel.moveItemUp(entity.parentId ?: entity.id) */ },
            onMoveDown = { /* Unused: viewModel.moveItemDown(entity.parentId ?: entity.id) */ },
            onStartMove = {
                movingItemId = entity.parentId ?: entity.id
            },
            onCloneGroup = {
                viewModel.cloneGroup(entity.id)
                itemToEdit = null
            },
            onAddChildTimer = {
                activeSetupGroupId = entity.id
                showAddDialog = true
                itemToEdit = null
            },
            onAddGroupBelow = {
                viewModel.addGroup()
                itemToEdit = null
            }
        )
    }

    if (showBackupDialog) {
        BackupToastDialog(
            onDismiss = { showBackupDialog = false },
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
                        showBackupDialog = false
                    }
                }
            }
        )
    }
}
}

// --- Individual Card Composables ---

@Composable
fun HeaderCard(
    ui: TimerUiState,
    collapsedCount: Int,
    isMoveMode: Boolean = false,
    onToggleCollapse: () -> Unit,
    onEdit: () -> Unit
) {
    val headerColor = ui.parsedColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerDownTap { onToggleCollapse() }
            .padding(vertical = if (isMoveMode) 8.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
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
                    overflow = TextOverflow.Ellipsis
                )
                if (collapsedCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▸ ${collapsedCount}件",
                        fontSize = 10.sp,
                        color = Color(0xFF8A8EA3),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(255, 255, 255, 18), RoundedCornerShape(10.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
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
                    .pointerDownTap { onEdit() },
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
        // Bottom divider line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF2A2A3A))
        )
    }
}

@Composable
fun RuleCard(
    ui: TimerUiState,
    isMoveMode: Boolean = false,
    onEdit: () -> Unit
) {
    val lineColor = ui.parsedColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerDownTap { onEdit() }
            .padding(vertical = if (isMoveMode) 10.dp else 4.dp, horizontal = 2.dp),
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
    pendingChunkId: String?,
    onCardTap: (ItemEntity) -> Unit,
    onEditChild: (ItemEntity) -> Unit,
    onEditGroup: () -> Unit,
    onQuickEditValue: (ItemEntity, String, Int) -> Unit,
    onAddChild: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val groupColor = remember(ui.entity.color) {
        ui.entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF555B68)
    }

    val borderColor = if (isMovingSource) Color(0xFF4DA3FF) else groupColor
    val borderWidth = if (isMovingSource) 2.dp else 1.dp
    val groupShape = remember { RoundedCornerShape(8.dp) }
    val borderStroke = remember(borderWidth, borderColor) {
        BorderStroke(borderWidth, borderColor)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 11.dp) // Sufficient top headroom for lifted name tag
    ) {
        // Outer Fieldset Box with clean native Surface border (no clipping artifacts)
        Surface(
            shape = groupShape,
            color = Color(0xFF0B0B14),
            border = borderStroke,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 11.dp, bottom = 6.dp, start = 5.dp, end = 5.dp) // Clean breathing space below the name tag
            ) {
                if (ui.children.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF15151F),
                        border = BorderStroke(1.dp, Color(0xFF2A2A3A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .pointerDownTap { onAddChild() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "＋",
                                color = Color(0xFF6E7387),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Render exactly the child timers in the account frame
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
                                            isClaimPreview = pendingChunkId == slotUi.entity.id,
                                            onTap = { onCardTap(slotUi.entity) },
                                            onEdit = { onEditChild(slotUi.entity) },
                                            onQuickEditValue = { field, newVal ->
                                                onQuickEditValue(slotUi.entity, field, newVal)
                                            },
                                            onFocusChanged = onFocusChanged
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top-Center Title Legend (Fieldset style) - Raised smoothly so it cleanly clears internal timers
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-9).dp)
                .background(Color(0xFF0B0B14), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 0.dp)
                .height(18.dp)
                .pointerDownTap { onEditGroup() },
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
    }
}

@Composable
fun TimerCard(
    ui: TimerUiState,
    isGroupChild: Boolean,
    isClaimPreview: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onQuickEditValue: ((String, Int) -> Unit)? = null,
    onFocusChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusTracker = LocalFocusTracker.current

    var isCurFocused by remember(ui.entity.id) { mutableStateOf(false) }
    var localCurText by remember(ui.entity.id) { mutableStateOf("") }

    val typeColor = ui.parsedColor

    val containerColor = Color(0xFF15151F)

    // Compute border attributes strictly aligned with styles.css definitions
    val isIdleExpedClaim = (ui.entity.type == "idle" || ui.entity.type == "exped") && ui.entity.state == "claim"

    val borderStrokeColor = if (isClaimPreview) {
        Color(0xFFFFD166) // Stamina/Orb chunk preview border (#ffd166)
    } else if (isIdleExpedClaim) {
        Color(0xFF5CD68A) // Recieve/Restart green border (var(--ok) -> #5cd68a)
    } else if (ui.isFull) {
        Color(0xFFFFAB5C) // Full indicator orange border (var(--orange) -> #ffab5c)
    } else {
        typeColor
    }

    val borderWidth = if (isClaimPreview || isIdleExpedClaim) 2.dp else 1.dp
    val cardShape = remember { RoundedCornerShape(10.dp) }
    val borderStroke = remember(borderWidth, borderStrokeColor) {
        BorderStroke(borderWidth, borderStrokeColor)
    }

    Surface(
        shape = cardShape,
        color = containerColor,
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerDownTap(enabled = !isCurFocused, onTap = onTap)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Menu Indicator Dot (Refined subtle solid color dot, naturally nestled in top-left corner without intruding on numbers or clock)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp)
                    .instantPointerTap { onEdit() }
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

                    // Absolute positioned Symmetrically aligned numbers (X / Y) at Bottom-Center with zero-delay native BasicTextField
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth()
                            .height(22.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current value (symmetrical right-aligned, tap to edit instantly)
                        BasicTextField(
                            value = if (isCurFocused) localCurText else "${ui.calculatedCurrent}",
                            onValueChange = { newVal ->
                                val filtered = newVal.filter { it.isDigit() }
                                if (filtered.length <= 3) {
                                    localCurText = filtered
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = if (ui.isWarn) Color(0xFFFF6B6B) else Color(0xFFECEEF2),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
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
                                .weight(1f)
                                .height(22.dp)
                                .onFocusChanged { focusState ->
                                    focusTracker(focusState.isFocused)
                                    if (focusState.isFocused) {
                                        if (!isCurFocused) {
                                            isCurFocused = true
                                            localCurText = ""
                                            keyboardController?.show()
                                        }
                                    } else {
                                        if (isCurFocused) {
                                            isCurFocused = false
                                            keyboardController?.hide()
                                            val parsed = localCurText.toIntOrNull()
                                            if (parsed != null) {
                                                onQuickEditValue?.invoke("cur", parsed)
                                            }
                                            localCurText = ""
                                        }
                                    }
                                },
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxSize()) {
                                    innerTextField()
                                }
                            }
                        )

                        // Slash "/" centered
                        Text(
                            text = "/",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7F8DA3),
                            modifier = Modifier.padding(horizontal = 2.dp),
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )

                        // Max value (symmetrical left-aligned, text only)
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
                                color = Color(0xFF4FA3FF),
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
                        Text(
                            text = ui.fullAtText,
                            fontSize = 11.5.sp,
                            fontWeight = if (ui.isFull) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (ui.isFull) Color(0xFFFF6B6B) else Color(0xFF9B8BFF),
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }

                    // Main display label in the bottom center
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp, start = 6.dp, end = 6.dp)
                            .fillMaxWidth()
                            .height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val isClaimState = ui.entity.state == "claim"
                        val labelText = if (isClaimState) {
                            if (ui.entity.type == "exped") "再出発" else "受取"
                        } else if (ui.isFull) {
                            if (ui.entity.type == "exped") "帰還" else "MAX"
                        } else {
                            ui.idleDisplayLabel
                        }

                        val labelColor = if (isClaimState) {
                            Color(0xFF5CD68A)
                        } else if (ui.isFull) {
                            Color(0xFFFF6B6B)
                        } else if (ui.isWarn) {
                            Color(0xFFFFAB5C)
                        } else {
                            Color(0xFFECEEF2)
                        }

                        val labelSize = if (isClaimState) {
                            13.5.sp
                        } else if (ui.isFull) {
                            if (ui.entity.type == "exped") 13.5.sp else 15.5.sp
                        } else {
                            19.sp
                        }

                        val labelTracking = if (isClaimState || ui.isFull) 0.04.em else 0.em

                        Text(
                            text = labelText,
                            fontSize = labelSize,
                            fontWeight = FontWeight.Bold,
                            color = labelColor,
                            letterSpacing = labelTracking,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                    includeFontPadding = false
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}
