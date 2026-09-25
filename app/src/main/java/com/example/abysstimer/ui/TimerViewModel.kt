package com.example.abysstimer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class TimerUiState(
    val entity: ItemEntity,
    val calculatedCurrent: Int = 0,
    val remainMs: Long = 0,
    val isFull: Boolean = false,
    val fullAtText: String = "",
    val orbNextInMs: Long = 0,
    val orbNextCdText: String = "",
    val idleElapsedMs: Long = 0,
    val idleRemainMs: Long = 0,
    val idleDisplayLabel: String = "",
    val isWarn: Boolean = false,
    val isClaimPreview: Boolean = false,
    val collapsedCount: Int = 0,
    val children: List<TimerUiState> = emptyList(),
    val parsedColor: Color = Color.Unspecified
)

class TimerViewModel(
    private val repository: TimerRepository,
    initialItems: List<ItemEntity> = emptyList()
) : ViewModel() {

    // Local in-memory state pre-seeded synchronously (0ms latency, zero black screen)
    private val _dbItems = MutableStateFlow<List<ItemEntity>>(initialItems)
    val pendingChunkUseId = MutableStateFlow<String?>(null)
    
    private val _isInitialized = MutableStateFlow(true)
    val isInitialized = _isInitialized.asStateFlow()
    
    val customColorsFlow: StateFlow<List<CustomColorEntity>> = repository.allCustomColorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Manual refresh pulse for lifecycle onResume (single trigger mechanism)
    private val _refreshTrigger = MutableStateFlow(System.currentTimeMillis())

    fun refreshNow() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // Lifecycle-aware ticker flow: emits only on each exact minute boundary
    private val tickerFlow = flow {
        while (true) {
            val now = System.currentTimeMillis()
            val unit = 60000L
            val delayTime = Math.max(1000L, unit - (now % unit))
            delay(delayTime)
            emit(System.currentTimeMillis())
        }
    }

    // Pre-calculate initial UI items synchronously so First Frame is populated immediately
    private val initialUiList = calculateUiStates(initialItems, null, System.currentTimeMillis())

    // Unified Reactive UI State - lazily kept in memory, initialized with preloaded items
    val uiItemsFlow: StateFlow<List<TimerUiState>> = combine(
        _dbItems, 
        pendingChunkUseId, 
        merge(_refreshTrigger, tickerFlow)
    ) { dbItems, pendingId, now ->
        calculateUiStates(dbItems, pendingId, now)
    }.stateIn(viewModelScope, SharingStarted.Lazily, initialUiList)

    init {
        // Collect Room live changes for background reactive updates
        viewModelScope.launch {
            launch {
                repository.allItemsFlow.collect { items ->
                    if (_dbItems.value != items) {
                        _dbItems.value = items
                    }
                }
            }

            // Low Priority: Lazy initialize colors in background only after main UI is fully ready
            launch {
                delay(1000)
                repository.initializeDefaultColorsIfEmpty()
            }
        }
    }

    // --- Action Dispatcher (MVI / Single Event Bus) ---

    fun onAction(action: TimerAction) {
        when (action) {
            is TimerAction.CardShortTap -> onCardShortTap(action.item)
            is TimerAction.CancelPendingStates -> cancelPendingStates()
            is TimerAction.UpdateCurrentValue -> updateCurrentValue(action.id, action.current)
            is TimerAction.UpdateMaxValue -> updateMaxValue(action.id, action.max)
            is TimerAction.MoveItemToTarget -> moveItemToTarget(action.sourceId, action.targetId)
            is TimerAction.AddItem -> addItem(
                type = action.type,
                name = action.name,
                intervalMin = action.intervalMin,
                max = action.max,
                useChunk = action.useChunk,
                durationMin = action.durationMin,
                countMode = action.countMode,
                orbMode = action.orbMode,
                color = action.color,
                parentId = action.parentId
            )
            is TimerAction.AddGroup -> addGroup()
            is TimerAction.AddChildToGroup -> addChildToGroup(
                groupId = action.groupId,
                type = action.type,
                intervalMin = action.intervalMin,
                max = action.max,
                useChunk = action.useChunk,
                durationMin = action.durationMin,
                countMode = action.countMode,
                orbMode = action.orbMode
            )
            is TimerAction.UpdateItemName -> updateItemName(action.id, action.name)
            is TimerAction.UpdateItemColor -> updateItemColor(action.id, action.color)
            is TimerAction.UpdateItemSettings -> updateItemSettings(
                id = action.id,
                intervalMin = action.intervalMin,
                max = action.max,
                useChunk = action.useChunk,
                useChunkClear = action.useChunkClear,
                durationMin = action.durationMin,
                countMode = action.countMode,
                orbMode = action.orbMode,
                layout = action.layout,
                foldLock = action.foldLock,
                orbEditHours = action.orbEditHours,
                orbEditMinutes = action.orbEditMinutes,
                idleEditHours = action.idleEditHours,
                idleEditMinutes = action.idleEditMinutes
            )
            is TimerAction.DeleteItem -> deleteItem(action.id)
            is TimerAction.CloneGroup -> cloneGroup(action.id)
            is TimerAction.ToggleHeaderCollapsed -> toggleHeaderCollapsed(action.id)
            is TimerAction.SaveCustomColors -> saveCustomColors(action.colors)
            is TimerAction.RefreshNow -> refreshNow()
        }
    }

    private fun calculateUiStates(dbList: List<ItemEntity>, pendingId: String?, now: Long): List<TimerUiState> {
        if (dbList.isEmpty()) return emptyList()

        val topLevels = mutableListOf<ItemEntity>()
        val childrenMap = mutableMapOf<String, MutableList<ItemEntity>>()
        
        for (item in dbList) {
            val pid = item.parentId
            if (pid == null) {
                topLevels.add(item)
            } else {
                childrenMap.getOrPut(pid) { mutableListOf() }.add(item)
            }
        }
        
        topLevels.sortBy { it.position }
        
        val result = topLevels.map { top ->
            val children = childrenMap[top.id]?.sortedBy { it.position }?.map { ch ->
                calculateSingleItemUi(ch, now, pendingId)
            } ?: emptyList()
            calculateSingleItemUi(top, now, pendingId).copy(children = children)
        }

        // Forward single pass to assign exact collapsedCount for each header
        val finalResult = ArrayList<TimerUiState>(result.size)
        var i = 0
        while (i < result.size) {
            val item = result[i]
            if (item.entity.type == "header" && item.entity.collapsed && !item.entity.foldLock) {
                var count = 0
                var j = i + 1
                while (j < result.size && result[j].entity.type != "header") {
                    if (result[j].entity.type != "rule") {
                        count++
                    }
                    j++
                }
                finalResult.add(item.copy(collapsedCount = count))
            } else {
                finalResult.add(item)
            }
            i++
        }

        return finalResult
    }

    private fun parseItemColor(colorHex: String?, type: String): Color {
        if (!colorHex.isNullOrEmpty()) {
            try {
                return Color(android.graphics.Color.parseColor(colorHex))
            } catch (e: Exception) {
                // fallback to type defaults
            }
        }
        return when (type) {
            "stam" -> Color(0xFF5AA9FF)
            "orb" -> Color(0xFFA78BFA)
            "idle" -> Color(0xFFF0A85A)
            "exped" -> Color(0xFF34D399)
            "header" -> Color(0xFF9B8BFF)
            "rule" -> Color(0xFF52617A)
            else -> Color.White
        }
    }

    private fun calculateSingleItemUi(it: ItemEntity, now: Long, pendingId: String?): TimerUiState {
        val waitChunk = pendingId == it.id
        val parsedCol = parseItemColor(it.color, it.type)

        return when (it.type) {
            "stam" -> {
                val info = TimerEngine.calculateStamInfo(it, now)
                val shownCurrent = if (waitChunk) TimerEngine.remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = TimerEngine.formatHM(fullAt)

                TimerUiState(
                    entity = it,
                    calculatedCurrent = shownCurrent,
                    remainMs = info.remainMs,
                    isFull = info.isFull,
                    fullAtText = fullAtText,
                    isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L),
                    isClaimPreview = waitChunk,
                    parsedColor = parsedCol
                )
            }
            "orb" -> {
                val info = TimerEngine.calculateOrbInfo(it, now)
                val shownCurrent = if (waitChunk) TimerEngine.remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = TimerEngine.formatHM(fullAt)
                val nextInSec = Math.max(0, info.nextInMs)
                val nextCdText = TimerEngine.formatCountdown(nextInSec)

                TimerUiState(
                    entity = it,
                    calculatedCurrent = shownCurrent,
                    remainMs = info.remainMs,
                    isFull = info.isFull,
                    fullAtText = fullAtText,
                    orbNextInMs = info.nextInMs,
                    orbNextCdText = nextCdText,
                    isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L),
                    isClaimPreview = waitChunk,
                    parsedColor = parsedCol
                )
            }
            "idle", "exped" -> {
                val info = TimerEngine.calculateIdleInfo(it, now)
                val isUp = it.countMode == "up"
                val runningLabel = if (isUp) TimerEngine.formatElapsed(info.elapsed) else TimerEngine.formatCountdown(info.remainMs)
                val fullText = if (it.type == "exped") "帰還" else "MAX"

                val displayLabel = if (it.state == "claim") {
                    if (it.type == "exped") "再出発" else "受取"
                } else {
                    if (info.isFull) fullText else runningLabel
                }

                val fullAtText = TimerEngine.formatHM(it.start + (it.durationMin.coerceAtLeast(1) * 60000L))

                TimerUiState(
                    entity = it,
                    calculatedCurrent = 0,
                    remainMs = info.remainMs,
                    isFull = info.isFull,
                    fullAtText = fullAtText,
                    idleElapsedMs = info.elapsed,
                    idleRemainMs = info.remainMs,
                    idleDisplayLabel = displayLabel,
                    isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L),
                    isClaimPreview = it.state == "claim",
                    parsedColor = parsedCol
                )
            }
            else -> {
                TimerUiState(entity = it, parsedColor = parsedCol)
            }
        }
    }

    // --- In-Memory Synchronous Update Helpers to prevent flicker and race conditions ---

    private fun updateDbItemInMemoryAndPersist(updated: ItemEntity) {
        val currentList = _dbItems.value
        _dbItems.value = currentList.map { if (it.id == updated.id) updated else it }
        viewModelScope.launch {
            repository.updateItem(updated)
        }
    }

    private fun updateDbItemsInMemoryAndPersist(updatedList: List<ItemEntity>) {
        val updatedMap = updatedList.associateBy { it.id }
        val currentList = _dbItems.value
        _dbItems.value = currentList.map { updatedMap[it.id] ?: it }
        viewModelScope.launch {
            repository.updateItems(updatedList)
        }
    }

    private fun deleteDbItemInMemoryAndPersist(item: ItemEntity) {
        val currentList = _dbItems.value
        _dbItems.value = currentList.filter { it.id != item.id && it.parentId != item.id }
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    private fun insertDbItemInMemoryAndPersist(newItem: ItemEntity) {
        val currentList = _dbItems.value
        _dbItems.value = currentList + newItem
        viewModelScope.launch {
            repository.insertItem(newItem)
        }
    }

    // --- Action Handlers ---

    fun onCardShortTap(it: ItemEntity) {
        val now = System.currentTimeMillis()
        if (it.type == "stam" || it.type == "orb") {
            revertOtherClaimStates(it.id)
            if (it.useChunk == null || it.useChunk <= 0) return

            if (pendingChunkUseId.value == it.id) {
                // Confirm deduction
                val info = if (it.type == "stam") {
                    val res = TimerEngine.calculateStamInfo(it, now)
                    StamInfo(cur = res.cur, remainMs = res.remainMs, isFull = res.isFull, fullAt = res.fullAt)
                } else {
                    val res = TimerEngine.calculateOrbInfo(it, now)
                    StamInfo(cur = res.cur, remainMs = res.remainMs, isFull = res.isFull, fullAt = res.fullAt)
                }

                val nextCur = TimerEngine.remainingAfterUse(info.cur, it.useChunk, it.type)
                val preservedStart = TimerEngine.preservePhaseStart(it, now)

                val updated = it.copy(
                    current = nextCur,
                    start = if (nextCur >= it.max) now else preservedStart
                )
                pendingChunkUseId.value = null
                updateDbItemInMemoryAndPersist(updated)
            } else {
                pendingChunkUseId.value = it.id
            }
        } else if (it.type == "idle" || it.type == "exped") {
            pendingChunkUseId.value = null
            revertOtherClaimStates(it.id)
            if (it.state == "claim") {
                // Restart running timer
                val updated = it.copy(
                    state = "running",
                    start = now
                )
                updateDbItemInMemoryAndPersist(updated)
            } else {
                // Set to claim early
                val updated = it.copy(
                    state = "claim"
                )
                updateDbItemInMemoryAndPersist(updated)
            }
        }
    }

    private fun revertOtherClaimStates(exceptId: String? = null) {
        val items = _dbItems.value
        val revertList = items.filter { item ->
            item.id != exceptId && (item.type == "idle" || item.type == "exped") && item.state == "claim"
        }.map { it.copy(state = "running") }

        if (revertList.isNotEmpty()) {
            updateDbItemsInMemoryAndPersist(revertList)
        }
    }

    fun cancelPendingStates() {
        pendingChunkUseId.value = null
        val items = _dbItems.value
        val revertList = items.filter { item ->
            (item.type == "idle" || item.type == "exped") && item.state == "claim"
        }.map { it.copy(state = "running") }

        if (revertList.isNotEmpty()) {
            updateDbItemsInMemoryAndPersist(revertList)
        }
    }

    fun cancelPendingChunkUse() {
        cancelPendingStates()
    }

    fun updateCurrentValue(id: String, newCurrent: Int) {
        val now = System.currentTimeMillis()
        val item = _dbItems.value.find { it.id == id } ?: return
        val clamped = newCurrent.coerceIn(0, item.max.coerceAtLeast(1))
        val preservedStart = TimerEngine.preservePhaseStart(item, now)
        val updated = item.copy(
            current = clamped,
            start = if (clamped >= item.max) now else preservedStart
        )
        updateDbItemInMemoryAndPersist(updated)
    }

    fun updateMaxValue(id: String, newMax: Int) {
        val now = System.currentTimeMillis()
        val item = _dbItems.value.find { it.id == id } ?: return
        val validMax = newMax.coerceAtLeast(1)
        val clampedCur = item.current.coerceAtMost(validMax)
        val preservedStart = TimerEngine.preservePhaseStart(item, now)
        val updated = item.copy(
            max = validMax,
            current = clampedCur,
            start = if (clampedCur >= validMax) now else preservedStart
        )
        updateDbItemInMemoryAndPersist(updated)
    }

    fun moveItemToTarget(sourceId: String, targetId: String) {
        val items = _dbItems.value
        val source = items.find { it.id == sourceId } ?: return
        val target = items.find { it.id == targetId } ?: return
        if (sourceId == targetId) return

        val sourceParentId = source.parentId
        val targetParentId = target.parentId

        val updates = mutableListOf<ItemEntity>()

        if (sourceParentId == targetParentId) {
            val siblings = items.filter { it.parentId == sourceParentId }.sortedBy { it.position }
            val sourceIdx = siblings.indexOfFirst { it.id == sourceId }
            val targetIdx = siblings.indexOfFirst { it.id == targetId }
            if (sourceIdx == -1 || targetIdx == -1) return

            if (sourceIdx < targetIdx) {
                // Move down: shift items between source and target up
                for (i in (sourceIdx + 1)..targetIdx) {
                    updates.add(siblings[i].copy(position = siblings[i].position - 1))
                }
                updates.add(source.copy(position = siblings[targetIdx].position))
            } else {
                // Move up: shift items between target and source down
                for (i in targetIdx until sourceIdx) {
                    updates.add(siblings[i].copy(position = siblings[i].position + 1))
                }
                updates.add(source.copy(position = siblings[targetIdx].position))
            }
        } else {
            // Move to different parent
            val oldSiblings = items.filter { it.parentId == sourceParentId }.sortedBy { it.position }
            val newSiblings = items.filter { it.parentId == targetParentId }.sortedBy { it.position }
            
            val sourceIdx = oldSiblings.indexOfFirst { it.id == sourceId }
            val targetIdx = newSiblings.indexOfFirst { it.id == targetId }
            
            // Shift items in old parent up
            if (sourceIdx != -1) {
                for (i in (sourceIdx + 1) until oldSiblings.size) {
                    updates.add(oldSiblings[i].copy(position = oldSiblings[i].position - 1))
                }
            }
            
            // Shift items in new parent down
            if (targetIdx != -1) {
                for (i in targetIdx until newSiblings.size) {
                    updates.add(newSiblings[i].copy(position = newSiblings[i].position + 1))
                }
                updates.add(source.copy(parentId = targetParentId, position = newSiblings[targetIdx].position))
            } else {
                updates.add(source.copy(parentId = targetParentId, position = 0))
            }
        }

        if (updates.isNotEmpty()) {
            updateDbItemsInMemoryAndPersist(updates)
        }
    }

    fun addItem(
        type: String,
        name: String = "",
        intervalMin: Int = 5,
        max: Int = 100,
        useChunk: Int? = null,
        durationMin: Int = 0,
        countMode: String? = "down",
        orbMode: String? = "down",
        color: String? = null,
        parentId: String? = null
    ) {
        val now = System.currentTimeMillis()
        val items = _dbItems.value
        val nextPos = if (items.isEmpty()) 0 else items.maxOf { it.position } + 1

        val newEntity = ItemEntity(
            id = "it_${UUID.randomUUID()}",
            type = type,
            name = name,
            current = max.coerceAtLeast(1),
            max = max.coerceAtLeast(1),
            intervalMin = intervalMin.coerceAtLeast(1),
            start = now,
            useChunk = useChunk,
            orbMode = orbMode,
            durationMin = durationMin.coerceAtLeast(0),
            countMode = countMode,
            state = if (type == "idle" || type == "exped") "running" else null,
            color = color,
            parentId = parentId,
            position = nextPos
        )
        insertDbItemInMemoryAndPersist(newEntity)
    }

    fun addGroup() {
        val items = _dbItems.value
        val nextPos = if (items.isEmpty()) 0 else items.maxOf { it.position } + 1

        val newEntity = ItemEntity(
            id = "group_${UUID.randomUUID()}",
            type = "group",
            name = "",
            color = "#52617a",
            position = nextPos
        )
        insertDbItemInMemoryAndPersist(newEntity)
    }

    fun addChildToGroup(
        groupId: String,
        type: String,
        intervalMin: Int = 5,
        max: Int = 100,
        useChunk: Int? = null,
        durationMin: Int = 0,
        countMode: String? = "down",
        orbMode: String? = "down"
    ) {
        val now = System.currentTimeMillis()
        val items = _dbItems.value
        val groupChildren = items.filter { it.parentId == groupId }
        val nextPos = if (groupChildren.isEmpty()) 0 else groupChildren.maxOf { it.position } + 1

        val newEntity = ItemEntity(
            id = "it_${UUID.randomUUID()}",
            type = type,
            name = "",
            current = max.coerceAtLeast(1),
            max = max.coerceAtLeast(1),
            intervalMin = intervalMin.coerceAtLeast(1),
            start = now,
            useChunk = useChunk,
            orbMode = orbMode,
            durationMin = durationMin.coerceAtLeast(0),
            countMode = countMode,
            state = if (type == "idle" || type == "exped") "running" else null,
            parentId = groupId,
            position = nextPos
        )
        insertDbItemInMemoryAndPersist(newEntity)
    }

    fun updateItemName(id: String, newName: String) {
        val item = _dbItems.value.find { it.id == id } ?: return
        updateDbItemInMemoryAndPersist(item.copy(name = newName))
    }

    fun updateItemColor(id: String, hexColor: String) {
        val item = _dbItems.value.find { it.id == id } ?: return
        updateDbItemInMemoryAndPersist(item.copy(color = hexColor))
    }

    fun updateItemSettings(
        id: String,
        intervalMin: Int? = null,
        max: Int? = null,
        useChunk: Int? = null,
        useChunkClear: Boolean = false,
        durationMin: Int? = null,
        countMode: String? = null,
        orbMode: String? = null,
        layout: String? = null,
        foldLock: Boolean? = null,
        orbEditHours: Int? = null,
        orbEditMinutes: Int? = null,
        idleEditHours: Int? = null,
        idleEditMinutes: Int? = null
    ) {
        val now = System.currentTimeMillis()
        val item = _dbItems.value.find { it.id == id } ?: return

        var updated = item

        if (intervalMin != null && intervalMin != item.intervalMin) {
            val safeInt = intervalMin.coerceAtLeast(1)
            val preservedStart = TimerEngine.preservePhaseStart(updated, now)
            updated = updated.copy(
                intervalMin = safeInt,
                start = if (updated.current >= (max ?: updated.max)) now else preservedStart
            )
        }

        if (max != null && max != item.max) {
            val safeMax = max.coerceAtLeast(1)
            val preservedStart = TimerEngine.preservePhaseStart(updated, now)
            val newCur = updated.current.coerceAtMost(safeMax)
            updated = updated.copy(
                max = safeMax,
                current = newCur,
                start = if (newCur >= safeMax) now else preservedStart
            )
        }

        if (useChunkClear) {
            updated = updated.copy(useChunk = null)
        } else if (useChunk != null) {
            updated = updated.copy(useChunk = useChunk.coerceAtLeast(1))
        }

        if (durationMin != null) {
            updated = updated.copy(durationMin = durationMin.coerceAtLeast(0))
        }

        if (countMode != null) {
            updated = updated.copy(countMode = countMode)
        }

        if (orbMode != null) {
            updated = updated.copy(orbMode = orbMode)
        }

        if (layout != null) {
            updated = updated.copy(layout = layout)
        }

        if (foldLock != null) {
            updated = updated.copy(foldLock = foldLock)
        }

        if (orbEditHours != null && orbEditMinutes != null) {
            val oneOrbMin = updated.intervalMin.coerceAtLeast(1)
            val totalMaxMin = updated.max * oneOrbMin
            val inputMin = orbEditHours * 60 + orbEditMinutes

            var nextCur = updated.current
            var nextStart = updated.start

            val currentOrbMode = orbMode ?: updated.orbMode ?: "down"

            if (currentOrbMode == "up") {
                val elapsedMin = inputMin.coerceIn(0, totalMaxMin)
                if (elapsedMin >= totalMaxMin) {
                    nextCur = updated.max
                    nextStart = now
                } else {
                    val cur = (elapsedMin / oneOrbMin).coerceIn(0, updated.max - 1)
                    val phaseMin = elapsedMin % oneOrbMin
                    nextCur = cur
                    nextStart = now - (phaseMin * 60000L)
                }
            } else {
                val totalRemainMin = inputMin
                if (totalRemainMin <= 0) {
                    nextCur = updated.max
                    nextStart = now
                } else if (totalRemainMin >= totalMaxMin) {
                    nextCur = 0
                    nextStart = now
                } else {
                    val elapsedMin = totalMaxMin - totalRemainMin
                    val cur = (elapsedMin / oneOrbMin).coerceIn(0, updated.max - 1)
                    val phaseMin = elapsedMin % oneOrbMin
                    nextCur = cur
                    nextStart = now - (phaseMin * 60000L)
                }
            }

            if (nextCur >= updated.max) {
                nextCur = updated.max
                nextStart = now
            }

            updated = updated.copy(
                current = nextCur,
                start = nextStart
            )
        }

        if (idleEditHours != null && idleEditMinutes != null) {
            val durMs = updated.durationMin.coerceAtLeast(1) * 60000L
            val currentCountMode = countMode ?: updated.countMode ?: "down"
            val inputMs = (idleEditHours * 60 + idleEditMinutes) * 60000L

            val nextStart = if (currentCountMode == "up") {
                val elapsedMs = inputMs.coerceAtMost(durMs)
                now - elapsedMs
            } else {
                val remainMs = inputMs.coerceAtMost(durMs)
                now - (durMs - remainMs)
            }

            updated = updated.copy(
                start = nextStart,
                state = if (updated.state == "claim") "running" else updated.state
            )
        }

        updateDbItemInMemoryAndPersist(updated)
    }

    fun deleteItem(id: String) {
        val item = _dbItems.value.find { it.id == id } ?: return
        deleteDbItemInMemoryAndPersist(item)
    }

    fun cloneGroup(id: String) {
        val dbList = _dbItems.value
        val orig = dbList.find { it.id == id && it.type == "group" } ?: return
        val origChildren = dbList.filter { it.parentId == id }

        val now = System.currentTimeMillis()
        val newGroupId = "group_${UUID.randomUUID()}"
        val newGroupName = if (orig.name.isEmpty()) "アカウント (コピー)" else "${orig.name} (コピー)"

        // Shift positions of top-level items below orig
        val topLevels = dbList.filter { it.parentId == null }.sortedBy { it.position }
        val origIndex = topLevels.indexOfFirst { it.id == id }
        val updatedTopLevels = mutableListOf<ItemEntity>()
        if (origIndex != -1) {
            for (i in (origIndex + 1) until topLevels.size) {
                val itemToShift = topLevels[i]
                updatedTopLevels.add(itemToShift.copy(position = itemToShift.position + 1))
            }
        }

        val newGroupPos = orig.position + 1
        val newGroup = orig.copy(
            id = newGroupId,
            name = newGroupName,
            position = newGroupPos
        )

        val newChildren = origChildren.map { child ->
            child.copy(
                id = "it_${UUID.randomUUID()}",
                parentId = newGroupId,
                start = now,
                state = if (child.state == "claim") "running" else child.state
            )
        }

        val allNew = (dbList.map { item -> updatedTopLevels.find { it.id == item.id } ?: item } + newGroup + newChildren)
        _dbItems.value = allNew
        viewModelScope.launch {
            // Atomic batch update & insert transaction
            repository.batchUpdateAndInsert(
                itemsToUpdate = updatedTopLevels,
                itemsToInsert = listOf(newGroup) + newChildren
            )
        }
    }

    fun toggleHeaderCollapsed(id: String) {
        val header = _dbItems.value.find { it.id == id && it.type == "header" } ?: return
        if (header.foldLock) return
        updateDbItemInMemoryAndPersist(header.copy(collapsed = !header.collapsed))
    }

    fun saveCustomColors(colors: List<String>) {
        viewModelScope.launch {
            repository.saveCustomColors(colors)
        }
    }

    /**
     * Serializes all current database items and custom colors into a lightweight JSON string.
     */
    fun exportBackupJson(): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArray = JSONArray()
        for (item in _dbItems.value) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("type", item.type)
            obj.put("name", item.name)
            obj.put("current", item.current)
            obj.put("max", item.max)
            obj.put("intervalMin", item.intervalMin)
            obj.put("start", item.start)
            item.useChunk?.let { obj.put("useChunk", it) }
            item.orbMode?.let { obj.put("orbMode", it) }
            obj.put("durationMin", item.durationMin)
            item.countMode?.let { obj.put("countMode", it) }
            item.state?.let { obj.put("state", it) }
            item.color?.let { obj.put("color", it) }
            obj.put("collapsed", item.collapsed)
            obj.put("foldLock", item.foldLock)
            item.layout?.let { obj.put("layout", it) }
            item.parentId?.let { obj.put("parentId", it) }
            obj.put("position", item.position)
            itemsArray.put(obj)
        }
        root.put("items", itemsArray)

        val colorsArray = JSONArray()
        for (c in customColorsFlow.value) {
            val obj = JSONObject()
            obj.put("slotIndex", c.slotIndex)
            obj.put("hexColor", c.hexColor)
            colorsArray.put(obj)
        }
        root.put("customColors", colorsArray)

        return root.toString()
    }

    /**
     * Imports items and custom colors from a backup JSON string with strict schema validation
     * and automatic position sequence normalization.
     */
    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return false
            
            val root = JSONObject(trimmed)
            val itemsArray = root.optJSONArray("items") ?: return false

            val parsedItems = mutableListOf<ItemEntity>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifEmpty { UUID.randomUUID().toString() }
                val type = obj.optString("type", "stam").let {
                    if (it in setOf("stam", "orb", "idle", "exped", "group", "header", "rule")) it else "stam"
                }
                val name = obj.optString("name", "")
                val current = obj.optInt("current", 0).coerceAtLeast(0)
                val max = obj.optInt("max", 100).coerceAtLeast(1)
                val intervalMin = obj.optInt("intervalMin", 5).coerceAtLeast(1)
                val start = obj.optLong("start", System.currentTimeMillis())
                val useChunk = if (obj.has("useChunk") && !obj.isNull("useChunk")) obj.optInt("useChunk").coerceAtLeast(1) else null
                val orbMode = if (obj.has("orbMode") && !obj.isNull("orbMode")) obj.optString("orbMode") else null
                val durationMin = obj.optInt("durationMin", 0).coerceAtLeast(0)
                val countMode = if (obj.has("countMode") && !obj.isNull("countMode")) obj.optString("countMode") else null
                val state = if (obj.has("state") && !obj.isNull("state")) obj.optString("state") else null
                val color = if (obj.has("color") && !obj.isNull("color")) obj.optString("color") else null
                val collapsed = obj.optBoolean("collapsed", false)
                val foldLock = obj.optBoolean("foldLock", false)
                val layout = if (obj.has("layout") && !obj.isNull("layout")) obj.optString("layout") else null
                val parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.optString("parentId").ifEmpty { null } else null
                val position = obj.optInt("position", i)

                parsedItems.add(
                    ItemEntity(
                        id = id,
                        type = type,
                        name = name,
                        current = current,
                        max = max,
                        intervalMin = intervalMin,
                        start = start,
                        useChunk = useChunk,
                        orbMode = orbMode,
                        durationMin = durationMin,
                        countMode = countMode,
                        state = state,
                        color = color,
                        collapsed = collapsed,
                        foldLock = foldLock,
                        layout = layout,
                        parentId = parentId,
                        position = position
                    )
                )
            }

            // Normalization: clean and ensure contiguous positions
            val topLevels = parsedItems.filter { it.parentId == null }.sortedBy { it.position }
            val childrenByParent = parsedItems.filter { it.parentId != null }.groupBy { it.parentId }

            val normalizedItems = mutableListOf<ItemEntity>()
            topLevels.forEachIndexed { topIdx, top ->
                normalizedItems.add(top.copy(position = topIdx))
                val children = childrenByParent[top.id]?.sortedBy { it.position } ?: emptyList()
                children.forEachIndexed { childIdx, child ->
                    normalizedItems.add(child.copy(position = childIdx))
                }
            }

            // Restore custom colors if present
            val colorsArray = root.optJSONArray("customColors")
            if (colorsArray != null) {
                val hexList = mutableListOf<String>()
                for (i in 0 until colorsArray.length()) {
                    val obj = colorsArray.optJSONObject(i) ?: continue
                    val hex = obj.optString("hexColor", "")
                    if (hex.isNotEmpty()) hexList.add(hex)
                }
                if (hexList.isNotEmpty()) {
                    repository.saveCustomColors(hexList)
                }
            }

            repository.replaceAllItems(normalizedItems)
            refreshNow()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
