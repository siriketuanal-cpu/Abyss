package com.example.abysstimer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StamInfo(val cur: Int, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)
data class OrbInfo(val cur: Int, val remainMs: Long, val nextInMs: Long, val isFull: Boolean, val fullAt: Long = 0)
data class IdleInfo(val elapsed: Long, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)

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
    val children: List<TimerUiState> = emptyList()
)

class TimerViewModel(private val repository: TimerRepository) : ViewModel() {

    private val _dbItems = MutableStateFlow<List<ItemEntity>>(emptyList())
    val customColorsFlow: StateFlow<List<CustomColorEntity>> = repository.allCustomColorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Local state for pending actions
    val pendingChunkUseId = MutableStateFlow<String?>(null)
    val claimId = MutableStateFlow<String?>(null)

    // Calculated UI state flow
    private val _uiItemsFlow = MutableStateFlow<List<TimerUiState>>(emptyList())
    val uiItemsFlow: StateFlow<List<TimerUiState>> = _uiItemsFlow.asStateFlow()

    private var tickerJob: Job? = null

    init {
        // Initialize default colors
        viewModelScope.launch {
            repository.initializeDefaultColorsIfEmpty()
        }

        // Collect database items
        viewModelScope.launch {
            repository.allItemsFlow.collect { items ->
                _dbItems.value = items
                calculateRealtimeStates()
            }
        }

        // Start ticking background calculations
        startTicking()
    }

    private fun startTicking() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                calculateRealtimeStates()
                // Check if any item became naturally full and sync to DB to stop redundant calculations
                syncFullItems()
                delay(1000L) // Tick every second
            }
        }
    }

    private fun calculateRealtimeStates() {
        val now = System.currentTimeMillis()
        val dbList = _dbItems.value
        val pendingId = pendingChunkUseId.value

        // Separate top-level items and children
        val topLevels = dbList.filter { it.parentId == null }.sortedBy { it.position }
        val childrenMap = dbList.filter { it.parentId != null }.groupBy { it.parentId }

        val uiList = topLevels.map { top ->
            val topChildren = (childrenMap[top.id] ?: emptyList()).sortedBy { it.position }
            val childrenUi = topChildren.map { ch ->
                calculateSingleItemUi(ch, now, pendingId)
            }
            calculateSingleItemUi(top, now, pendingId).copy(children = childrenUi)
        }

        _uiItemsFlow.value = uiList
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

    private suspend fun syncFullItems() {
        val now = System.currentTimeMillis()
        var changed = false
        val dbList = _dbItems.value

        for (it in dbList) {
            if (it.type == "stam" && it.current < it.max) {
                val info = calculateStamInfo(it, now)
                if (info.isFull) {
                    val updated = it.copy(current = it.max, start = info.fullAt)
                    repository.updateItem(updated)
                    changed = true
                }
            } else if (it.type == "orb" && it.current < it.max) {
                val info = calculateOrbInfo(it, now)
                if (info.isFull) {
                    val updated = it.copy(current = it.max, start = info.fullAt)
                    repository.updateItem(updated)
                    changed = true
                }
            }
        }
        if (changed) {
            // Re-fetch to update DB list state
            calculateRealtimeStates()
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
    private fun preservePhaseStart(it: ItemEntity, now: Long): Long {
        val intervalMs = Math.max(1, it.intervalMin) * 60000L
        val phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs
        return now - phase
    }

    // --- Action Methods called by user ---

    fun onCardShortTap(it: ItemEntity) {
        val now = System.currentTimeMillis()
        if (it.type == "stam" || it.type == "orb") {
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
                viewModelScope.launch {
                    repository.updateItem(updated)
                }
                pendingChunkUseId.value = null
            } else {
                pendingChunkUseId.value = it.id
            }
        } else if (it.type == "idle" || it.type == "exped") {
            if (it.state == "claim") {
                // Restart running timer
                val updated = it.copy(
                    state = "running",
                    start = now
                )
                viewModelScope.launch {
                    repository.updateItem(updated)
                }
            } else {
                // Set to claim early
                val updated = it.copy(
                    state = "claim"
                )
                viewModelScope.launch {
                    repository.updateItem(updated)
                }
            }
        }
    }

    fun cancelPendingChunkUse() {
        pendingChunkUseId.value = null
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
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val items = repository.getAllItems()
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
            repository.insertItem(newEntity)
        }
    }

    fun addGroup() {
        viewModelScope.launch {
            val items = repository.getAllItems()
            val nextPos = if (items.isEmpty()) 0 else items.maxOf { it.position } + 1

            val newEntity = ItemEntity(
                id = "group_${UUID.randomUUID()}",
                type = "group",
                name = "",
                color = "#52617a",
                position = nextPos
            )
            repository.insertItem(newEntity)
        }
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
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val items = repository.getAllItems()
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
            repository.insertItem(newEntity)
        }
    }

    fun updateItemName(id: String, newName: String) {
        viewModelScope.launch {
            val dbList = repository.getAllItems()
            val item = dbList.find { it.id == id } ?: return@launch
            repository.updateItem(item.copy(name = newName))
        }
    }

    fun updateItemColor(id: String, hexColor: String) {
        viewModelScope.launch {
            val dbList = repository.getAllItems()
            val item = dbList.find { it.id == id } ?: return@launch
            repository.updateItem(item.copy(color = hexColor))
        }
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
        foldLock: Boolean? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dbList = repository.getAllItems()
            val item = dbList.find { it.id == id } ?: return@launch

            var updated = item

            if (intervalMin != null && intervalMin != item.intervalMin) {
                // Preserve recovery cycle phase
                val preservedStart = preservePhaseStart(updated, now)
                updated = updated.copy(
                    intervalMin = intervalMin,
                    start = if (updated.current >= (max ?: updated.max)) now else preservedStart
                )
            }

            if (max != null && max != item.max) {
                // Clamp current value to new max
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

            repository.updateItem(updated)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            val dbList = repository.getAllItems()
            val item = dbList.find { it.id == id } ?: return@launch
            repository.deleteItem(item)
        }
    }

    fun moveItemUp(id: String) {
        viewModelScope.launch {
            val items = repository.getAllItems()
            val item = items.find { it.id == id } ?: return@launch
            val parentId = item.parentId

            val siblingList = items.filter { it.parentId == parentId }.sortedBy { it.position }
            val index = siblingList.indexOfFirst { it.id == id }
            if (index <= 0) return@launch // Already at top

            // Swap positions
            val other = siblingList[index - 1]
            val tempPos = item.position
            repository.updateItem(item.copy(position = other.position))
            repository.updateItem(other.copy(position = tempPos))
        }
    }

    fun moveItemDown(id: String) {
        viewModelScope.launch {
            val items = repository.getAllItems()
            val item = items.find { it.id == id } ?: return@launch
            val parentId = item.parentId

            val siblingList = items.filter { it.parentId == parentId }.sortedBy { it.position }
            val index = siblingList.indexOfFirst { it.id == id }
            if (index == -1 || index >= siblingList.size - 1) return@launch // Already at bottom

            // Swap positions
            val other = siblingList[index + 1]
            val tempPos = item.position
            repository.updateItem(item.copy(position = other.position))
            repository.updateItem(other.copy(position = tempPos))
        }
    }

    fun toggleHeaderCollapsed(id: String) {
        viewModelScope.launch {
            val items = repository.getAllItems()
            val header = items.find { it.id == id && it.type == "header" } ?: return@launch
            if (header.foldLock) return@launch
            repository.updateItem(header.copy(collapsed = !header.collapsed))
        }
    }

    fun saveCustomColors(colors: List<String>) {
        viewModelScope.launch {
            repository.saveCustomColors(colors)
        }
    }

    private fun formatHM(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatCountdown(ms: Long): String {
        val totalSec = Math.max(0, ms / 1000)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        // Format to H:MM or just MM if H is 0
        return if (h > 0) {
            String.format(Locale.getDefault(), "%d:%02d", h, m)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", m, s)
        }
    }

    private fun formatElapsed(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return String.format(Locale.getDefault(), "%d:%02d", h, m)
    }
}
