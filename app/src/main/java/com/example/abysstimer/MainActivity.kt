@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.example.abysstimer

import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.abysstimer.data.AppDatabase
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import com.example.abysstimer.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val customColors by viewModel.customColorsFlow.collectAsStateWithLifecycle()
    val pendingChunkId by viewModel.pendingChunkUseId.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var activeSetupType by remember { mutableStateOf<String?>(null) }
    var activeSetupGroupId by remember { mutableStateOf<String?>(null) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Abyss Timer",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0B14)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add timer",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        containerColor = Color(0xFF0B0B14)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable {
                    // Cancel any active previews
                    if (pendingChunkId != null) {
                        viewModel.cancelPendingChunkUse()
                    }
                }
        ) {
            if (uiItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "＋ でタイマーを追加",
                            fontSize = 18.sp,
                            color = Color(0xFF8A8EA3),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "ゲームのスタミナや遠征を管理しましょう",
                            fontSize = 14.sp,
                            color = Color(0xFF555B68)
                        )
                    }
                }
            } else {
                // Group items by headers if collapsed
                val visibleItems = remember(uiItems) {
                    val list = mutableListOf<TimerUiState>()
                    var skipUntilNextHeader = false
                    for (ui in uiItems) {
                        if (ui.entity.type == "header") {
                            skipUntilNextHeader = ui.entity.collapsed && !ui.entity.foldLock
                            list.add(ui)
                        } else {
                            if (!skipUntilNextHeader) {
                                list.add(ui)
                            } else {
                                // We count hidden items for header badge, already calculated in ViewModel
                            }
                        }
                    }
                    list
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(visibleItems, key = { it.entity.id }) { ui ->
                        when (ui.entity.type) {
                            "header" -> {
                                // Count how many subsequent items are collapsed under this header
                                val collapsedCount = remember(ui, uiItems) {
                                    var count = 0
                                    val index = uiItems.indexOfFirst { it.entity.id == ui.entity.id }
                                    if (index != -1 && ui.entity.collapsed && !ui.entity.foldLock) {
                                        for (i in (index + 1) until uiItems.size) {
                                            if (uiItems[i].entity.type == "header") break
                                            if (uiItems[i].entity.type != "rule") {
                                                count++
                                            }
                                        }
                                    }
                                    count
                                }

                                HeaderCard(
                                    ui = ui,
                                    collapsedCount = collapsedCount,
                                    onToggleCollapse = { viewModel.toggleHeaderCollapsed(ui.entity.id) },
                                    onEdit = { itemToEdit = ui.entity }
                                )
                            }
                            "rule" -> {
                                RuleCard(
                                    ui = ui,
                                    onEdit = { itemToEdit = ui.entity }
                                )
                            }
                            "group" -> {
                                GroupCard(
                                    ui = ui,
                                    pendingChunkId = pendingChunkId,
                                    onCardTap = { viewModel.onCardShortTap(it) },
                                    onEditChild = { itemToEdit = it },
                                    onEditGroup = { itemToEdit = ui.entity },
                                    onAddChild = {
                                        activeSetupGroupId = ui.entity.id
                                        showAddDialog = true
                                    }
                                )
                            }
                            else -> {
                                // Regular top-level timer
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    TimerCard(
                                        ui = ui,
                                        isGroupChild = false,
                                        isClaimPreview = ui.isClaimPreview,
                                        onTap = { viewModel.onCardShortTap(ui.entity) },
                                        onEdit = { itemToEdit = ui.entity }
                                    )
                                }
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
            onSaveName = { viewModel.updateItemName(entity.id, it) },
            onSaveColor = { viewModel.updateItemColor(entity.id, it) },
            onSaveCustomColors = { viewModel.saveCustomColors(it) },
            onUpdateSettings = { map ->
                viewModel.updateItemSettings(
                    id = entity.id,
                    intervalMin = map["intervalMin"] as? Int,
                    max = map["max"] as? Int,
                    useChunk = map["useChunk"] as? Int,
                    useChunkClear = map["useChunkClear"] as? Boolean ?: false,
                    durationMin = map["durationMin"] as? Int,
                    countMode = map["countMode"] as? String,
                    orbMode = map["orbMode"] as? String,
                    layout = map["layout"] as? String,
                    foldLock = map["foldLock"] as? Boolean
                )
            },
            onDelete = {
                viewModel.deleteItem(entity.id)
                itemToEdit = null
            },
            onMoveUp = { viewModel.moveItemUp(entity.id) },
            onMoveDown = { viewModel.moveItemDown(entity.id) }
        )
    }
}

// --- Individual Card Composables ---

@Composable
fun HeaderCard(
    ui: TimerUiState,
    collapsedCount: Int,
    onToggleCollapse: () -> Unit,
    onEdit: () -> Unit
) {
    val headerColor = remember(ui.entity.color) {
        ui.entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF9B8BFF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = onToggleCollapse,
                onLongClick = onEdit
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .width(4.dp)
                    .height(20.dp)
                    .background(headerColor, RoundedCornerShape(2.dp))
            )
            Text(
                text = ui.entity.name.ifEmpty { "見出し(ゲーム名など)" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (ui.entity.name.isEmpty()) Color(0xFF8A8EA3) else headerColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (collapsedCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "▸ ${collapsedCount}件",
                    fontSize = 12.sp,
                    color = Color(0xFFFFAB5C),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color(0xFF2A1C14), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            if (ui.entity.foldLock) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Fold Locked",
                    tint = Color(0xFF8A8EA3),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        IconButton(
            onClick = onEdit,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = headerColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun RuleCard(
    ui: TimerUiState,
    onEdit: () -> Unit
) {
    val lineColor = remember(ui.entity.color) {
        ui.entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF52617A)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onEdit
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(lineColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Settings",
            tint = lineColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun GroupCard(
    ui: TimerUiState,
    pendingChunkId: String?,
    onCardTap: (ItemEntity) -> Unit,
    onEditChild: (ItemEntity) -> Unit,
    onEditGroup: () -> Unit,
    onAddChild: () -> Unit
) {
    val groupColor = remember(ui.entity.color) {
        ui.entity.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: Color(0xFF52617A)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, groupColor, RoundedCornerShape(10.dp))
            .background(Color(0xFF15151F), RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onEditGroup,
                onLongClick = onEditGroup
            )
            .padding(8.dp)
    ) {
        // Group header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ui.entity.name.ifEmpty { "アカウント" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (ui.entity.name.isEmpty()) Color(0xFF8A8EA3) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row {
                if (ui.children.size < 4) {
                    IconButton(
                        onClick = onAddChild,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Add child timer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onEditGroup,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Group options",
                        tint = Color(0xFF8A8EA3),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Group body grid/row
        if (ui.children.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(onClick = onAddChild),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "＋ タイマーを追加",
                    color = Color(0xFF8A8EA3),
                    fontSize = 13.sp
                )
            }
        } else {
            val is2x2 = ui.entity.layout == "2x2" && ui.children.size > 2
            if (is2x2) {
                // Render as 2x2 grid
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val chunks = ui.children.chunked(2)
                    chunks.forEach { rowChildren ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowChildren.forEach { childUi ->
                                Box(modifier = Modifier.weight(1f)) {
                                    TimerCard(
                                        ui = childUi,
                                        isGroupChild = true,
                                        isClaimPreview = pendingChunkId == childUi.entity.id,
                                        onTap = { onCardTap(childUi.entity) },
                                        onEdit = { onEditChild(childUi.entity) }
                                    )
                                }
                            }
                            if (rowChildren.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                // Render side-by-side horizontally
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ui.children.forEach { childUi ->
                        Box(modifier = Modifier.weight(1f)) {
                            TimerCard(
                                ui = childUi,
                                isGroupChild = true,
                                isClaimPreview = pendingChunkId == childUi.entity.id,
                                onTap = { onCardTap(childUi.entity) },
                                onEdit = { onEditChild(childUi.entity) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerCard(
    ui: TimerUiState,
    isGroupChild: Boolean,
    isClaimPreview: Boolean,
    onTap: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current

    val themeColor = when (ui.entity.type) {
        "stam" -> Color(0xFF5AA9FF)
        "orb" -> Color(0xFFA78BFA)
        "idle" -> Color(0xFFF0A85A)
        "exped" -> Color(0xFF34D399)
        else -> Color.White
    }

    val containerColor = if (ui.isFull) {
        Color(0xFF221A0F) // Highlighted background when full/complete
    } else {
        Color(0xFF1B1B28)
    }

    val borderStrokeColor = if (isClaimPreview) {
        Color(0xFFFFD166) // Gold claim double-tap preview border
    } else if (ui.isFull) {
        Color(0xFFFFAB5C) // Full indicator border
    } else {
        themeColor.copy(alpha = 0.5f)
    }

    // Trigger vibration haptic feedback on state confirmed or claim preview
    LaunchedEffect(isClaimPreview) {
        if (isClaimPreview) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(30)
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isGroupChild) 52.dp else 56.dp)
            .border(
                width = if (isClaimPreview || ui.isFull) 2.dp else 1.dp,
                color = borderStrokeColor,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onClick = onTap,
                onLongClick = onEdit
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
            // Type specific render
            when (ui.entity.type) {
                "stam", "orb" -> {
                    // Clock displays at the top-right / left
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Value Displays e.g. "80 / 100"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${ui.calculatedCurrent}",
                                fontSize = if (isGroupChild) 18.sp else 20.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = if (ui.isWarn) Color(0xFFFF6B6B) else themeColor
                            )
                            Text(
                                text = "/",
                                fontSize = 12.sp,
                                color = Color(0xFF8A8EA3)
                            )
                            Text(
                                text = "${ui.entity.max}",
                                fontSize = 12.sp,
                                color = Color(0xFF8A8EA3)
                            )
                        }

                        // Time display stack
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (ui.entity.type == "orb" && !ui.isFull) {
                                // "Next" orb cooldown
                                Text(
                                    text = "次 ${ui.orbNextCdText}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8A8EA3),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (ui.isFull) "FULL" else ui.fullAtText,
                                fontSize = if (isGroupChild) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ui.isFull) Color(0xFF5CD68A) else Color.White
                            )
                        }
                    }
                }
                "idle", "exped" -> {
                    // Displays countdown/up or "RESTART"/"CLAIM"
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (ui.entity.type == "exped") "遠征" else "放置",
                                fontSize = 10.sp,
                                color = Color(0xFF8A8EA3),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = ui.fullAtText,
                                fontSize = 11.sp,
                                color = Color(0xFF555B68)
                            )
                        }

                        Text(
                            text = ui.idleDisplayLabel,
                            fontSize = if (isGroupChild) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ui.isFull) Color(0xFF5CD68A) else Color.White,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}
