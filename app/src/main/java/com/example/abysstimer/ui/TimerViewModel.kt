package com.example.abysstimer.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class StamInfo(val cur: Int, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)
@Immutable
data class OrbInfo(val cur: Int, val remainMs: Long, val nextInMs: Long, val isFull: Boolean, val fullAt: Long = 0)
@Immutable
data class IdleInfo(val elapsed: Long, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)

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
    val children: List<TimerUiState> = emptyList()
)

class TimerViewModel(private val repository: TimerRepository) : ViewModel() {

    // Local state for pending actions
    private val _dbItems = MutableStateFlow<List<ItemEntity>>(emptyList())
    val pendingChunkUseId = MutableStateFlow<String?>(null)
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()
    
    val customColorsFlow: StateFlow<List<CustomColorEntity>> = repository.allCustomColorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Manual refresh pulse for lifecycle onResume
    private val _manualRefreshPulse = MutableStateFlow(0L)

    fun refreshNow() {
        _manualRefreshPulse.value = System.currentTimeMillis()
    }

    // Lifecycle-aware Cold Ticker Flow: stops automatically when UI is in background/not subscribed
    private val tickerFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            val now = System.currentTimeMillis()
            val unit = 60000L
            val delayTime = Math.max(1000L, unit - (now % unit))
            delay(delayTime)
        }
    }

    // Unified Reactive UI State
    val uiItemsFlow: StateFlow<List<TimerUiState>> = combine(
        _dbItems, 
        pendingChunkUseId, 
        tickerFlow,
        _manualRefreshPulse
    ) { dbItems, pendingId, nowTicker, manualTime ->
        val effectiveNow = if (manualTime > nowTicker) manualTime else nowTicker
        calculateUiStates(dbItems, pendingId, effectiveNow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), emptyList())

    init {
        // Fast-path Direct Read for Cold Start (0ms instant UI rendering without waiting for Flow collector)
        viewModelScope.launch {
            val cachedItems = repository.getAllItems()
            if (cachedItems.isNotEmpty() && _dbItems.value.isEmpty()) {
                _dbItems.value = cachedItems
                _isInitialized.value = true
            }
        }

        // Initialize default colors
        viewModelScope.launch {
            repository.initializeDefaultColorsIfEmpty()
        }

        // Collect database items
        viewModelScope.launch {
            repository.allItemsFlow.collect { items ->
                _dbItems.value = items
                _isInitialized.value = true
            }
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

        // Precalculate collapsed counts for header cards in a single O(N) backward pass
        var currentCollapsedCount = 0
        for (i in result.indices.reversed()) {
            val item = result[i]
            if (item.entity.type == "header") {
                if (item.entity.collapsed && !item.entity.foldLock) {
                    result[i].let { } // keep count
                }
            }
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

    private fun calculateSingleItemUi(it: ItemEntity, now: Long, pendingId: String?): TimerUiState {
        val waitChunk = pendingId == it.id

        return when (it.type) {
            "stam" -> {
                val info = calculateStamInfo(it, now)
                val shownCurrent = if (waitChunk) remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = formatHM(fullAt)

                TimerUiState(
                    entity = it,
                    calculatedCurrent = shownCurrent,
                    remainMs = info.remainMs,
                    isFull = info.isFull,
                    fullAtText = fullAtText,
                    isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L),
                    isClaimPreview = waitChunk
                )
            }
            "orb" -> {
                val info = calculateOrbInfo(it, now)
                val shownCurrent = if (waitChunk) remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = formatHM(fullAt)
                val nextInSec = Math.max(0, info.nextInMs)
                val nextCdText = formatCountdown(nextInSec)

                TimerUiState(
                    entity = it,
                    calculatedCurrent = shownCurrent,
                    remainMs = info.remainMs,
                    isFull = info.isFull,
                    fullAtText = fullAtText,
                    orbNextInMs = info.nextInMs,
                    orbNextCdText = nextCdText,
                    isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L),
                    isClaimPreview = waitChunk
                )
            }
            "idle", "exped" -> {
                val info = calculateIdleInfo(it, now)
                val isUp = it.countMode == "up"
                val runningLabel = if (isUp) formatElapsed(info.elapsed) else formatCountdown(info.remainMs)
                val fullText = if (it.type == "exped") "帰還" else "MAX"

                val displayLabel = if (it.state == "claim") {
                    if (it.type == "exped") "再出発" else "受取"
                } else {
                    if (info.isFull) fullText else runningLabel
                }

                val fullAtText = formatHM(it.start + (it.durationMin * 60000L))

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
                    isClaimPreview = it.state == "claim"
                )
            }
            else -> {
                TimerUiState(entity = it)
            }
        }
    }



    // --- Mathematics recovery helpers ---

    private fun calculateStamInfo(it: ItemEntity, now: Long): StamInfo {
        if (it.current >= it.max) {
            return StamInfo(cur = it.max, remainMs = 0L, isFull = true, fullAt = it.start)
        }
        val intervalMs = Math.max(1, it.intervalMin) * 60000L
        val elapsed = Math.max(0L, now - it.start)
        val recovered = (elapsed / intervalMs).toInt()
        val cur = Math.min(it.max, it.current + recovered)
        if (cur >= it.max) {
            val fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs
            return StamInfo(cur = it.max, remainMs = 0L, isFull = true, fullAt = Math.min(now, fullAt))
        }
        val nextIn = intervalMs - (elapsed % intervalMs)
        val need = it.max - cur
        val remainMs = (need - 1) * intervalMs + nextIn
        return StamInfo(cur = cur, remainMs = remainMs, isFull = false)
    }

    private fun calculateOrbInfo(it: ItemEntity, now: Long): OrbInfo {
        if (it.current >= it.max) {
            return OrbInfo(cur = it.max, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = it.start)
        }
        val intervalMs = Math.max(1, it.intervalMin) * 60000L
        val elapsed = Math.max(0L, now - it.start)
        val recovered = (elapsed / intervalMs).toInt()
        val cur = Math.min(it.max, it.current + recovered)
        if (cur >= it.max) {
            val fullAt = it.start + Math.max(0, it.max - it.current) * intervalMs
            return OrbInfo(cur = it.max, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = Math.min(now, fullAt))
        }
        val nextInMs = intervalMs - (elapsed % intervalMs)
        val need = it.max - cur
        val remainMs = (need - 1) * intervalMs + nextInMs
        return OrbInfo(cur = cur, remainMs = remainMs, nextInMs = nextInMs, isFull = false, fullAt = now + remainMs)
    }

    private fun calculateIdleInfo(it: ItemEntity, now: Long): IdleInfo {
        val durMs = Math.max(1, it.durationMin) * 60000L
        val elapsed = Math.max(0L, now - it.start)
        val remainMs = Math.max(0L, durMs - elapsed)
        return IdleInfo(elapsed = elapsed, remainMs = remainMs, isFull = remainMs <= 0, fullAt = it.start + durMs)
    }

    private fun remainingAfterUse(cur: Int, chunk: Int, type: String): Int {
        val c = Math.max(1, chunk)
        if (type == "orb") {
            return Math.max(0, cur - c)
        }
        return cur % c
    }

    // Phase preservation when interval or max is adjusted
    // This ensures "hidden progress" towards the next recovery point is not lost.
    private fun preservePhaseStart(it: ItemEntity, now: Long): Long {
        val intervalMs = Math.max(1, it.intervalMin) * 60000L
        val phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs
        return now - phase
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

    // --- Action Methods called by user ---

    fun onCardShortTap(it: ItemEntity) {
        val now = System.currentTimeMillis()
        if (it.type == "stam" || it.type == "orb") {
            revertOtherClaimStates(it.id)
            if (it.useChunk == null || it.useChunk <= 0) return

            if (pendingChunkUseId.value == it.id) {
                // Confirm deduction
                val info = if (it.type == "stam") {
                    val res = calculateStamInfo(it, now)
                    StamInfo(cur = res.cur, remainMs = res.remainMs, isFull = res.isFull, fullAt = res.fullAt)
                } else {
                    val res = calculateOrbInfo(it, now)
                    StamInfo(cur = res.cur, remainMs = res.remainMs, isFull = res.isFull, fullAt = res.fullAt)
                }

                val nextCur = remainingAfterUse(info.cur, it.useChunk, it.type)
                val preservedStart = preservePhaseStart(it, now)

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

    // --- Quick Inline Adjustments ---

    fun updateCurrentValue(id: String, newCurrent: Int) {
        val now = System.currentTimeMillis()
        val item = _dbItems.value.find { it.id == id } ?: return
        val clamped = Math.max(0, Math.min(item.max, newCurrent))
        val preservedStart = preservePhaseStart(item, now)
        val updated = item.copy(
            current = clamped,
            start = if (clamped >= item.max) now else preservedStart
        )
        updateDbItemInMemoryAndPersist(updated)
    }

    fun updateMaxValue(id: String, newMax: Int) {
        val now = System.currentTimeMillis()
        val item = _dbItems.value.find { it.id == id } ?: return
        val validMax = Math.max(1, newMax)
        val clampedCur = Math.min(item.current, validMax)
        val preservedStart = preservePhaseStart(item, now)
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
                // Should not happen if target exists
                updates.add(source.copy(parentId = targetParentId, position = 0))
            }
        }

        if (updates.isNotEmpty()) {
            updateDbItemsInMemoryAndPersist(updates)
        }
    }

    // --- CRUD and helpers ---

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
            current = max,
            max = max,
            intervalMin = intervalMin,
            start = now,
            useChunk = useChunk,
            orbMode = orbMode,
            durationMin = durationMin,
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
            current = max,
            max = max,
            intervalMin = intervalMin,
            start = now,
            useChunk = useChunk,
            orbMode = orbMode,
            durationMin = durationMin,
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
            // Preserve recovery cycle phase (Hidden progress)
            val preservedStart = preservePhaseStart(updated, now)
            updated = updated.copy(
                intervalMin = intervalMin,
                start = if (updated.current >= (max ?: updated.max)) now else preservedStart
            )
        }

        if (max != null && max != item.max) {
            // Clamp current value to new max and preserve phase
            val preservedStart = preservePhaseStart(updated, now)
            val newCur = Math.min(updated.current, max)
            updated = updated.copy(
                max = max,
                current = newCur,
                start = if (newCur >= max) now else preservedStart
            )
        }

        if (useChunkClear) {
            updated = updated.copy(useChunk = null)
        } else if (useChunk != null) {
            updated = updated.copy(useChunk = useChunk)
        }

        if (durationMin != null) {
            updated = updated.copy(durationMin = durationMin)
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
            val oneOrbMin = Math.max(1, updated.intervalMin)
            val totalMaxMin = updated.max * oneOrbMin
            val inputMin = orbEditHours * 60 + orbEditMinutes

            var nextCur = updated.current
            var nextStart = updated.start

            val currentOrbMode = orbMode ?: updated.orbMode ?: "down"

            if (currentOrbMode == "up") {
                // Cumulative (elapsed) mode
                val elapsedMin = Math.min(totalMaxMin, Math.max(0, inputMin))
                if (elapsedMin >= totalMaxMin) {
                    nextCur = updated.max
                    nextStart = now
                } else {
                    val cur = Math.min(updated.max - 1, Math.max(0, elapsedMin / oneOrbMin))
                    val phaseMin = elapsedMin % oneOrbMin
                    nextCur = cur
                    nextStart = now - (phaseMin * 60000L)
                }
            } else {
                // Remainder (down) mode
                val totalRemainMin = inputMin
                if (totalRemainMin <= 0) {
                    nextCur = updated.max
                    nextStart = now
                } else if (totalRemainMin >= totalMaxMin) {
                    nextCur = 0
                    nextStart = now
                } else {
                    val elapsedMin = totalMaxMin - totalRemainMin
                    val cur = Math.min(updated.max - 1, Math.max(0, elapsedMin / oneOrbMin))
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
            val durMs = Math.max(1, updated.durationMin) * 60000L
            val currentCountMode = countMode ?: updated.countMode ?: "down"
            val inputMs = (idleEditHours * 60 + idleEditMinutes) * 60000L

            val nextStart = if (currentCountMode == "up") {
                val elapsedMs = Math.min(durMs, inputMs)
                now - elapsedMs
            } else {
                val remainMs = Math.min(durMs, inputMs)
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
            val batchInsert = listOf(newGroup) + newChildren
            repository.insertItems(batchInsert)
            if (updatedTopLevels.isNotEmpty()) {
                repository.updateItems(updatedTopLevels)
            }
        }
    }

    /*
    // --- [UNUSED] Up/Down movement functions (Legacy - now use moveItemToTarget for reordering) ---
    fun moveItemUp(id: String) {
        val items = _dbItems.value
        val item = items.find { it.id == id } ?: return
        val parentId = item.parentId

        val siblingList = items.filter { it.parentId == parentId }.sortedBy { it.position }
        val index = siblingList.indexOfFirst { it.id == id }
        if (index <= 0) return // Already at top

        // Swap positions atomically
        val other = siblingList[index - 1]
        val tempPos = item.position
        updateDbItemsInMemoryAndPersist(
            listOf(
                item.copy(position = other.position),
                other.copy(position = tempPos)
            )
        )
    }

    fun moveItemDown(id: String) {
        val items = _dbItems.value
        val item = items.find { it.id == id } ?: return
        val parentId = item.parentId

        val siblingList = items.filter { it.parentId == parentId }.sortedBy { it.position }
        val index = siblingList.indexOfFirst { it.id == id }
        if (index == -1 || index >= siblingList.size - 1) return // Already at bottom

        // Swap positions atomically
        val other = siblingList[index + 1]
        val tempPos = item.position
        updateDbItemsInMemoryAndPersist(
            listOf(
                item.copy(position = other.position),
                other.copy(position = tempPos)
            )
        )
    }
    */

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
     * Imports items and custom colors from a backup JSON string.
     * Returns true if successful, false otherwise.
     */
    suspend fun importBackupJson(jsonString: String): Boolean {
        return try {
            val trimmed = jsonString.trim()
            val root = JSONObject(trimmed)
            val itemsArray = root.optJSONArray("items") ?: return false

            val newItems = mutableListOf<ItemEntity>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val type = obj.optString("type", "stam")
                val name = obj.optString("name", "")
                val current = obj.optInt("current", 0)
                val max = obj.optInt("max", 0)
                val intervalMin = obj.optInt("intervalMin", 0)
                val start = obj.optLong("start", System.currentTimeMillis())
                val useChunk = if (obj.has("useChunk") && !obj.isNull("useChunk")) obj.getInt("useChunk") else null
                val orbMode = if (obj.has("orbMode") && !obj.isNull("orbMode")) obj.getString("orbMode") else null
                val durationMin = obj.optInt("durationMin", 0)
                val countMode = if (obj.has("countMode") && !obj.isNull("countMode")) obj.getString("countMode") else null
                val state = if (obj.has("state") && !obj.isNull("state")) obj.getString("state") else null
                val color = if (obj.has("color") && !obj.isNull("color")) obj.getString("color") else null
                val collapsed = obj.optBoolean("collapsed", false)
                val foldLock = obj.optBoolean("foldLock", false)
                val layout = if (obj.has("layout") && !obj.isNull("layout")) obj.getString("layout") else null
                val parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getString("parentId") else null
                val position = obj.optInt("position", i)

                newItems.add(
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

            // Also restore custom colors if present
            val colorsArray = root.optJSONArray("customColors")
            if (colorsArray != null) {
                val hexList = mutableListOf<String>()
                for (i in 0 until colorsArray.length()) {
                    val obj = colorsArray.getJSONObject(i)
                    val hex = obj.optString("hexColor", "")
                    if (hex.isNotEmpty()) hexList.add(hex)
                }
                if (hexList.isNotEmpty()) {
                    repository.saveCustomColors(hexList)
                }
            }

            repository.replaceAllItems(newItems)
            refreshNow()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun formatHM(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    private fun formatCountdown(ms: Long): String {
        val clampedMs = Math.max(0L, ms)
        val totalMin = Math.ceil(clampedMs / 60000.0).toLong()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m < 10) "$h:0$m" else "$h:$m"
    }

    private fun formatElapsed(ms: Long): String {
        val clampedMs = Math.max(0L, ms)
        val totalMin = Math.floor(clampedMs / 60000.0).toLong()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m < 10) "$h:0$m" else "$h:$m"
    }
}
