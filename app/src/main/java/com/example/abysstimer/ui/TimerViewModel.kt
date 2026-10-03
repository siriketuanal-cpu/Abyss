package com.example.abysstimer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import com.example.abysstimer.data.TimerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    val parsedColor: Color = Color.Unspecified,
    val gridSpan: Int = 1
)

@Immutable
data class UiSnapshot(
    val allItems: List<TimerUiState> = emptyList(),
    val visibleItems: List<TimerUiState> = emptyList()
)

@Immutable
data class TimerCoreState(
    val items: List<ItemEntity> = emptyList(),
    val pendingChunkId: String? = null
)

class TimerViewModel(
    private val repository: TimerRepository,
    initialItems: List<ItemEntity> = emptyList()
) : ViewModel() {

    // --- High-Performance Skippable UI Cache & Parser (Initialized first to prevent NPE) ---
    private val colorCache = java.util.concurrent.ConcurrentHashMap<String, Color>()
    private var lastDbList: List<ItemEntity>? = null
    private var cachedTopLevels: List<ItemEntity> = emptyList()
    private var cachedChildrenMap: Map<String, List<ItemEntity>> = emptyMap()
    private var cachedCollapsedCountMap: Map<String, Int> = emptyMap()
    private val cachedUiStateMap = HashMap<String, TimerUiState>()
    private var lastSnapshot = UiSnapshot(emptyList(), emptyList())

    private fun parseItemColor(colorHex: String?, type: String): Color {
        if (!colorHex.isNullOrEmpty()) {
            val cached = colorCache[colorHex]
            if (cached != null) return cached
            try {
                val parsed = Color(android.graphics.Color.parseColor(colorHex))
                colorCache[colorHex] = parsed
                return parsed
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
            "group" -> Color(0xFF555B68)
            else -> Color.White
        }
    }

    private fun getOrCreateItemUi(
        it: ItemEntity,
        now: Long,
        pendingId: String?,
        collapsedCount: Int,
        children: List<TimerUiState> = emptyList()
    ): TimerUiState {
        val parsedCol = parseItemColor(it.color, it.type)
        val prev = cachedUiStateMap[it.id]

        return when (it.type) {
            "header" -> {
                if (prev != null && prev.entity == it && prev.collapsedCount == collapsedCount && prev.parsedColor == parsedCol && prev.gridSpan == 4) {
                    prev
                } else {
                    val state = TimerUiState(entity = it, collapsedCount = collapsedCount, parsedColor = parsedCol, gridSpan = 4)
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
            "rule" -> {
                if (prev != null && prev.entity == it && prev.parsedColor == parsedCol && prev.gridSpan == 4) {
                    prev
                } else {
                    val state = TimerUiState(entity = it, parsedColor = parsedCol, gridSpan = 4)
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
            "group" -> {
                val span = if (it.layout == "2x2" && children.size > 2) 2 else children.size.coerceIn(1, 4)
                if (prev != null && prev.entity == it && prev.children === children && prev.parsedColor == parsedCol && prev.gridSpan == span) {
                    prev
                } else {
                    val state = TimerUiState(entity = it, children = children, parsedColor = parsedCol, gridSpan = span)
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
            "stam" -> {
                val waitChunk = pendingId == it.id
                val info = TimerEngine.calculateStamInfo(it, now)
                val shownCurrent = if (waitChunk) TimerEngine.remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = TimerEngine.formatHM(fullAt)
                val isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L)

                if (prev != null &&
                    prev.entity == it &&
                    prev.calculatedCurrent == shownCurrent &&
                    prev.isFull == info.isFull &&
                    prev.fullAtText == fullAtText &&
                    prev.isWarn == isWarn &&
                    prev.isClaimPreview == waitChunk &&
                    prev.parsedColor == parsedCol &&
                    prev.gridSpan == 1
                ) {
                    prev
                } else {
                    val state = TimerUiState(
                        entity = it,
                        calculatedCurrent = shownCurrent,
                        remainMs = info.remainMs,
                        isFull = info.isFull,
                        fullAtText = fullAtText,
                        isWarn = isWarn,
                        isClaimPreview = waitChunk,
                        parsedColor = parsedCol,
                        gridSpan = 1
                    )
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
            "orb" -> {
                val waitChunk = pendingId == it.id
                val info = TimerEngine.calculateOrbInfo(it, now)
                val shownCurrent = if (waitChunk) TimerEngine.remainingAfterUse(info.cur, it.useChunk ?: 1, it.type) else info.cur
                val fullAt = if (info.isFull) info.fullAt else now + info.remainMs
                val fullAtText = TimerEngine.formatHM(fullAt)
                val nextInSec = Math.max(0, info.nextInMs)
                val nextCdText = TimerEngine.formatCountdown(nextInSec)
                val isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L)

                if (prev != null &&
                    prev.entity == it &&
                    prev.calculatedCurrent == shownCurrent &&
                    prev.isFull == info.isFull &&
                    prev.fullAtText == fullAtText &&
                    prev.orbNextCdText == nextCdText &&
                    prev.isWarn == isWarn &&
                    prev.isClaimPreview == waitChunk &&
                    prev.parsedColor == parsedCol &&
                    prev.gridSpan == 1
                ) {
                    prev
                } else {
                    val state = TimerUiState(
                        entity = it,
                        calculatedCurrent = shownCurrent,
                        remainMs = info.remainMs,
                        isFull = info.isFull,
                        fullAtText = fullAtText,
                        orbNextInMs = info.nextInMs,
                        orbNextCdText = nextCdText,
                        isWarn = isWarn,
                        isClaimPreview = waitChunk,
                        parsedColor = parsedCol,
                        gridSpan = 1
                    )
                    cachedUiStateMap[it.id] = state
                    state
                }
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
                val isWarn = !info.isFull && (info.remainMs > 0 && info.remainMs < 7200000L)
                val isClaim = it.state == "claim"

                if (prev != null &&
                    prev.entity == it &&
                    prev.isFull == info.isFull &&
                    prev.fullAtText == fullAtText &&
                    prev.idleDisplayLabel == displayLabel &&
                    prev.isWarn == isWarn &&
                    prev.isClaimPreview == isClaim &&
                    prev.parsedColor == parsedCol &&
                    prev.gridSpan == 1
                ) {
                    prev
                } else {
                    val state = TimerUiState(
                        entity = it,
                        calculatedCurrent = 0,
                        remainMs = info.remainMs,
                        isFull = info.isFull,
                        fullAtText = fullAtText,
                        idleElapsedMs = info.elapsed,
                        idleRemainMs = info.remainMs,
                        idleDisplayLabel = displayLabel,
                        isWarn = isWarn,
                        isClaimPreview = isClaim,
                        parsedColor = parsedCol,
                        gridSpan = 1
                    )
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
            else -> {
                if (prev != null && prev.entity == it && prev.parsedColor == parsedCol && prev.gridSpan == 1) {
                    prev
                } else {
                    val state = TimerUiState(entity = it, parsedColor = parsedCol, gridSpan = 1)
                    cachedUiStateMap[it.id] = state
                    state
                }
            }
        }
    }

    private fun calculateUiSnapshot(dbList: List<ItemEntity>, pendingId: String?, now: Long): UiSnapshot {
        lastCalculationTime = now
        if (dbList.isEmpty()) {
            lastSnapshot = UiSnapshot(emptyList(), emptyList())
            return lastSnapshot
        }

        // Cache DB hierarchy, sort, and collapsed counts if dbList has not changed
        if (dbList !== lastDbList) {
            val isStructuralSame = lastDbList != null &&
                lastDbList!!.size == dbList.size &&
                lastDbList!!.indices.all { idx ->
                    val o = lastDbList!![idx]
                    val n = dbList[idx]
                    o.id == n.id && o.parentId == n.parentId && o.position == n.position &&
                    o.type == n.type && o.collapsed == n.collapsed && o.foldLock == n.foldLock
                }

            if (isStructuralSame) {
                // Structural order is identical (e.g. state change or timestamp update on tap).
                // Zero-allocation fast path: replace entities in existing hierarchy without re-sorting or re-bucketing
                val entityMap = HashMap<String, ItemEntity>(dbList.size)
                for (item in dbList) entityMap[item.id] = item
                cachedTopLevels = cachedTopLevels.map { entityMap[it.id] ?: it }
                val newChildrenMap = HashMap<String, List<ItemEntity>>(cachedChildrenMap.size)
                for ((pid, children) in cachedChildrenMap) {
                    newChildrenMap[pid] = children.map { entityMap[it.id] ?: it }
                }
                cachedChildrenMap = newChildrenMap
                lastDbList = dbList
            } else {
                // Structural change (item added, removed, reordered, or header collapsed)
                val topLevels = ArrayList<ItemEntity>()
                val childrenMap = HashMap<String, ArrayList<ItemEntity>>()
                
                for (item in dbList) {
                    val pid = item.parentId
                    if (pid == null) {
                        topLevels.add(item)
                    } else {
                        childrenMap.getOrPut(pid) { ArrayList() }.add(item)
                    }
                }
                
                topLevels.sortBy { it.position }
                val sortedChildren = HashMap<String, List<ItemEntity>>(childrenMap.size)
                for ((k, v) in childrenMap) {
                    v.sortBy { it.position }
                    sortedChildren[k] = v
                }
                
                val collapsedCounts = HashMap<String, Int>()
                var i = 0
                while (i < topLevels.size) {
                    val top = topLevels[i]
                    if (top.type == "header" && top.collapsed && !top.foldLock) {
                        var count = 0
                        var j = i + 1
                        while (j < topLevels.size && topLevels[j].type != "header") {
                            if (topLevels[j].type != "rule") {
                                count++
                            }
                            j++
                        }
                        collapsedCounts[top.id] = count
                    }
                    i++
                }
                
                cachedTopLevels = topLevels
                cachedChildrenMap = sortedChildren
                cachedCollapsedCountMap = collapsedCounts
                lastDbList = dbList

                val currentIds = HashSet<String>(dbList.size)
                for (item in dbList) {
                    currentIds.add(item.id)
                }
                val keyIterator = cachedUiStateMap.keys.iterator()
                while (keyIterator.hasNext()) {
                    if (!currentIds.contains(keyIterator.next())) {
                        keyIterator.remove()
                    }
                }
            }
        }

        val allList = ArrayList<TimerUiState>(cachedTopLevels.size)
        val visibleList = ArrayList<TimerUiState>(cachedTopLevels.size)
        var skipUntilNextHeader = false

        for (top in cachedTopLevels) {
            val childrenEntities = cachedChildrenMap[top.id]
            val childrenUi: List<TimerUiState> = if (childrenEntities.isNullOrEmpty()) {
                emptyList()
            } else {
                val childList = ArrayList<TimerUiState>(childrenEntities.size)
                for (ch in childrenEntities) {
                    childList.add(getOrCreateItemUi(ch, now, pendingId, 0))
                }
                val prevChildren = cachedUiStateMap[top.id]?.children
                if (prevChildren != null && prevChildren.size == childList.size &&
                    prevChildren.indices.all { idx -> prevChildren[idx] === childList[idx] }) {
                    prevChildren
                } else {
                    childList
                }
            }

            val topCollapsedCount = cachedCollapsedCountMap[top.id] ?: 0
            val topUi = getOrCreateItemUi(top, now, pendingId, topCollapsedCount, childrenUi)

            allList.add(topUi)

            if (topUi.entity.type == "header") {
                skipUntilNextHeader = topUi.entity.collapsed && !topUi.entity.foldLock
                visibleList.add(topUi)
            } else if (!skipUntilNextHeader) {
                visibleList.add(topUi)
            }
        }

        val allMatches = lastSnapshot.allItems.size == allList.size &&
                lastSnapshot.allItems.indices.all { idx -> lastSnapshot.allItems[idx] === allList[idx] }
        val visibleMatches = lastSnapshot.visibleItems.size == visibleList.size &&
                lastSnapshot.visibleItems.indices.all { idx -> lastSnapshot.visibleItems[idx] === visibleList[idx] }

        val finalAll = if (allMatches) lastSnapshot.allItems else allList
        val finalVisible = if (visibleMatches) lastSnapshot.visibleItems else visibleList

        val snapshot = if (allMatches && visibleMatches) lastSnapshot else UiSnapshot(finalAll, finalVisible)
        lastSnapshot = snapshot
        return snapshot
    }

    // Unified Core State: items and pending preview managed atomically to avoid race conditions and double-emissions
    private val _coreState = MutableStateFlow(TimerCoreState(initialItems, null))

    val pendingChunkUseId: StateFlow<String?> = _coreState
        .map { it.pendingChunkId }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    val customColorsFlow: StateFlow<List<CustomColorEntity>> = repository.allCustomColorsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @Volatile
    private var lastCalculationTime: Long = System.currentTimeMillis()

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

    // Pre-calculate initial UI snapshot synchronously so First Frame is populated immediately
    private val initialSnapshot = calculateUiSnapshot(initialItems, null, System.currentTimeMillis())

    // Isolate UI Snapshot calculation: Use distinctUntilChanged on inputs to prevent redundant calculations
    // and only use essential core state + minute-aligned ticker as drivers.
    val uiSnapshotFlow: StateFlow<UiSnapshot> = combine(
        _coreState.map { it.items to it.pendingChunkId }.distinctUntilChanged(),
        merge(_refreshTrigger, tickerFlow).distinctUntilChanged()
    ) { (items, pendingId), _ ->
        calculateUiSnapshot(items, pendingId, System.currentTimeMillis())
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.Eagerly, initialSnapshot)

    fun getItemEntity(id: String): ItemEntity? = _coreState.value.items.find { it.id == id }

    init {
        // Collect Room live changes for background reactive updates (single unified path, zero redundant queries)
        viewModelScope.launch {
            launch {
                repository.allItemsFlow.distinctUntilChanged().collect { items ->
                    val currentCore = _coreState.value
                    // Protect transient in-memory UI preview states (e.g. claim wait state on idle/exped)
                    val claimItem = currentCore.items.find { (it.type == "idle" || it.type == "exped") && it.state == "claim" }

                    val merged = items.map { dbItem ->
                        if (claimItem != null && dbItem.id == claimItem.id) {
                            claimItem
                        } else {
                            pendingUpdates[dbItem.id] ?: dbItem
                        }
                    }
                    if (currentCore.items != merged) {
                        _coreState.value = currentCore.copy(items = merged)
                        TimerRepository.updateCache(merged)
                    }
                }
            }

            // Low Priority: Lazy initialize colors in background on IO dispatcher without delay or main thread contention
            launch(Dispatchers.IO) {
                repository.initializeDefaultColorsIfEmpty()
            }
        }
    }

    // --- In-Memory Synchronous Update Helpers with Debounced Disk Persistence ---

    private val pendingUpdates = java.util.concurrent.ConcurrentHashMap<String, ItemEntity>()
    private var persistDebounceJob: Job? = null

    private fun scheduleDebouncedPersist() {
        persistDebounceJob?.cancel()
        persistDebounceJob = viewModelScope.launch {
            delay(300)
            val itemsToSave = pendingUpdates.values.toList()
            pendingUpdates.clear()
            if (itemsToSave.isNotEmpty()) {
                repository.updateItems(itemsToSave)
            }
        }
    }

    /**
     * Immediately flushes any unwritten pending items directly to disk.
     * Prevents data loss during task switching, sleep, or low-memory kills.
     */
    fun flushPendingPersistsImmediately() {
        persistDebounceJob?.cancel()
        val itemsToSave = pendingUpdates.values.toList()
        pendingUpdates.clear()
        if (itemsToSave.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateItems(itemsToSave)
            }
        }
    }

    /**
     * Unified Task Resume & Launch handler:
     * - Resets stale 2-step preview states (prevents accidental tap deduction after long absence)
     * - Refreshes UI snapshot instantly with the exact current timestamp
     */
    fun onAppForegrounded() {
        cancelPendingStates()
        refreshNow()
    }

    /**
     * Unified Background / Freeze preparation handler:
     * - Cancels transient preview states
     * - Immediately flushes all memory changes to Room DB before OS can suspend or kill process
     */
    fun onAppBackgrounded() {
        cancelPendingStates()
        flushPendingPersistsImmediately()
    }

    override fun onCleared() {
        super.onCleared()
        persistDebounceJob?.cancel()
        val itemsToSave = pendingUpdates.values.toList()
        pendingUpdates.clear()
        if (itemsToSave.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateItems(itemsToSave)
            }
        }
    }

    private fun updateDbItemInMemoryAndPersist(updated: ItemEntity) {
        val currentList = _coreState.value.items
        val newItems = currentList.map { if (it.id == updated.id) updated else it }
        _coreState.value = _coreState.value.copy(items = newItems)
        TimerRepository.updateCache(newItems)
        pendingUpdates[updated.id] = updated
        scheduleDebouncedPersist()
    }

    private fun updateDbItemsInMemoryAndPersist(updatedList: List<ItemEntity>) {
        val updatedMap = updatedList.associateBy { it.id }
        val currentList = _coreState.value.items
        val newItems = currentList.map { updatedMap[it.id] ?: it }
        _coreState.value = _coreState.value.copy(items = newItems)
        TimerRepository.updateCache(newItems)
        for (item in updatedList) {
            pendingUpdates[item.id] = item
        }
        scheduleDebouncedPersist()
    }

    private fun deleteDbItemInMemoryAndPersist(item: ItemEntity) {
        pendingUpdates.remove(item.id)
        val currentList = _coreState.value.items
        if (item.type == "group") {
            val childIds = currentList.filter { it.parentId == item.id }.map { it.id }
            childIds.forEach { pendingUpdates.remove(it) }
        }
        val remaining = currentList.filter { it.id != item.id && it.parentId != item.id }

        // Clean re-index contiguous positions via unified TimerEngine logic
        val normalized = TimerEngine.normalizeItemPositions(remaining)

        val newPendingChunkId = if (_coreState.value.pendingChunkId == item.id ||
            (item.type == "group" && currentList.any { it.parentId == item.id && it.id == _coreState.value.pendingChunkId })) {
            null
        } else {
            _coreState.value.pendingChunkId
        }

        _coreState.value = TimerCoreState(items = normalized, pendingChunkId = newPendingChunkId)
        TimerRepository.updateCache(normalized)

        viewModelScope.launch(Dispatchers.IO) {
            val changedItems = normalized.filter { n ->
                val orig = remaining.find { it.id == n.id }
                orig != null && orig.position != n.position
            }
            repository.deleteItemAndReorder(item, changedItems)
        }
    }

    private fun insertDbItemInMemoryAndPersist(newItem: ItemEntity) {
        val currentList = _coreState.value.items
        val newItems = currentList + newItem
        _coreState.value = _coreState.value.copy(items = newItems)
        TimerRepository.updateCache(newItems)
        viewModelScope.launch {
            repository.insertItem(newItem)
        }
    }

    // --- Action Handlers ---

    private var lastConfirmTimestamp = 0L

    fun onCardShortTap(it: ItemEntity) {
        val now = System.currentTimeMillis()
        if (now - lastConfirmTimestamp < 150L) return

        // Always resolve latest entity from state to prevent stale data
        val target = _coreState.value.items.find { item -> item.id == it.id } ?: it
        val currentCore = _coreState.value

        // 1.0.61仕様: いずれかのカードがプレビュー中（使い切り待機または受取待機）の状態で「別のカード」をタップされた場合、
        // プレビュー状態をキャンセルするのみ（タップされた別カードの処理は実行しない）
        val pendingId = currentCore.pendingChunkId
        val claimItem = currentCore.items.find { (it.type == "idle" || it.type == "exped") && it.state == "claim" }

        if (pendingId != null && pendingId != target.id) {
            cancelPendingStates()
            return
        }
        if (claimItem != null && claimItem.id != target.id) {
            cancelPendingStates()
            return
        }

        when (target.type) {
            "stam", "orb" -> {
                if (target.useChunk == null || target.useChunk <= 0) return

                if (pendingId == target.id) {
                    // Step 2: 確定（Confirm deduction: Calculate new current and preserved start）
                    val info = TimerEngine.calculateRecoveryInfo(target, now)
                    val nextCur = TimerEngine.remainingAfterUse(info.cur, target.useChunk, target.type)
                    val preservedStart = TimerEngine.preservePhaseStart(target, now)

                    val updated = target.copy(
                        current = nextCur,
                        start = if (info.isFull || nextCur >= target.max) now else preservedStart
                    )

                    // Atomic single-step update: clear pending preview AND apply deduction simultaneously
                    val newItems = currentCore.items.map { if (it.id == updated.id) updated else it }
                    _coreState.value = TimerCoreState(items = newItems, pendingChunkId = null)
                    TimerRepository.updateCache(newItems)
                    lastConfirmTimestamp = now

                    pendingUpdates[updated.id] = updated
                    scheduleDebouncedPersist()
                } else {
                    // Step 1: プレビュー開始（Enter preview mode）- 0 allocations, 0ms latency
                    _coreState.value = currentCore.copy(pendingChunkId = target.id)
                }
            }
            "idle", "exped" -> {
                if (target.state == "claim") {
                    // Step 2: 確定（再スタート）- 実データをDBへ永続化
                    val updated = target.copy(state = "running", start = now)
                    val newItems = currentCore.items.map { if (it.id == updated.id) updated else it }
                    _coreState.value = TimerCoreState(items = newItems, pendingChunkId = null)
                    TimerRepository.updateCache(newItems)
                    lastConfirmTimestamp = now

                    pendingUpdates[updated.id] = updated
                    scheduleDebouncedPersist()
                } else {
                    // Step 1: 受取プレビュー待機（Enter claim wait）- 一時UI状態のためDB書き込みせずメモリのみで0ms即座に切り替え
                    val updated = target.copy(state = "claim")
                    val newItems = currentCore.items.map { if (it.id == updated.id) updated else it }
                    _coreState.value = TimerCoreState(items = newItems, pendingChunkId = null)
                }
            }
        }
    }

    fun hasPendingStates(): Boolean {
        val currentCore = _coreState.value
        return currentCore.pendingChunkId != null ||
                currentCore.items.any { (it.type == "idle" || it.type == "exped") && it.state == "claim" }
    }

    fun cancelPendingStates(): Boolean {
        val currentCore = _coreState.value
        val hasPendingChunk = currentCore.pendingChunkId != null
        val hasClaimToRevert = currentCore.items.any { (it.type == "idle" || it.type == "exped") && it.state == "claim" }

        // Fast-path: 0 allocations and 0ms return if nothing is pending or waiting for claim revert
        if (!hasPendingChunk && !hasClaimToRevert) return false

        val newItems = if (hasClaimToRevert) {
            currentCore.items.map { item ->
                if ((item.type == "idle" || item.type == "exped") && item.state == "claim") {
                    val reverted = item.copy(state = "running")
                    pendingUpdates[reverted.id] = reverted
                    reverted
                } else {
                    item
                }
            }
        } else {
            currentCore.items
        }

        _coreState.value = TimerCoreState(items = newItems, pendingChunkId = null)
        if (hasClaimToRevert) {
            scheduleDebouncedPersist()
        }
        return true
    }

    fun cancelPendingChunkUse() {
        cancelPendingStates()
    }

    /**
     * Resolves the parent group name for a child timer, or returns empty string if not found.
     */
    fun getGroupNameForChild(parentId: String?): String {
        if (parentId == null) return ""
        return _coreState.value.items.find { it.id == parentId }?.name ?: ""
    }

    fun updateCurrentValue(id: String, newCurrent: Int) {
        val now = System.currentTimeMillis()
        val item = _coreState.value.items.find { it.id == id } ?: return
        val clamped = newCurrent.coerceIn(0, item.max.coerceAtLeast(1))

        val recovery = when (item.type) {
            "stam", "orb" -> TimerEngine.calculateRecoveryInfo(item, now)
            else -> RecoveryInfo(item.current, 0L, 0L, item.current >= item.max, item.start)
        }

        // 値に変更がない場合（フォーカスして何も触らず閉じた場合など）は満タン時刻や位相を一切書き換えずスキップ
        if (clamped == recovery.cur) {
            return
        }

        val wasFull = recovery.isFull
        val isNowFull = clamped >= item.max
        val preservedStart = TimerEngine.preservePhaseStart(item, now)

        val updatedStart = if (isNowFull) {
            // 新たに満タンにした場合：今満タン達成
            now
        } else if (wasFull) {
            // 満タン状態から減らした場合：今減らして回復開始
            now
        } else {
            // 未満タンから未満タンへの変更：端数秒・位相を維持
            preservedStart
        }

        val updated = item.copy(
            current = clamped,
            start = updatedStart
        )
        updateDbItemInMemoryAndPersist(updated)
    }

    fun updateMaxValue(id: String, newMax: Int) {
        val now = System.currentTimeMillis()
        val item = _coreState.value.items.find { it.id == id } ?: return
        val validMax = newMax.coerceAtLeast(1)
        if (validMax == item.max) return

        val recovery = when (item.type) {
            "stam", "orb" -> TimerEngine.calculateRecoveryInfo(item, now)
            else -> RecoveryInfo(item.current, 0L, 0L, item.current >= item.max, item.start)
        }
        val realCurrent = recovery.cur
        val wasFull = recovery.isFull
        val clampedCur = realCurrent.coerceAtMost(validMax)
        val isNowFull = clampedCur >= validMax

        val newStart = if (wasFull && isNowFull) {
            // 以前も満タン、今も満タン（満タン到達時刻をそのまま保持）
            recovery.fullAt
        } else if (isNowFull || wasFull) {
            now
        } else {
            TimerEngine.preservePhaseStart(item, now)
        }

        val updated = item.copy(
            max = validMax,
            current = clampedCur,
            start = newStart
        )
        updateDbItemInMemoryAndPersist(updated)
    }

    fun moveItemToTarget(sourceId: String, targetId: String) {
        val items = _coreState.value.items
        val source = items.find { it.id == sourceId } ?: return
        val target = items.find { it.id == targetId } ?: return
        if (sourceId == targetId) return

        val sourceParentId = source.parentId
        val targetParentId = target.parentId

        if (sourceParentId == null && targetParentId == null) {
            // Both are top-level items
            val topLevels = items.filter { it.parentId == null }.sortedBy { it.position }.toMutableList()
            val sIdx = topLevels.indexOfFirst { it.id == sourceId }
            val tIdx = topLevels.indexOfFirst { it.id == targetId }
            if (sIdx == -1 || tIdx == -1) return

            val isFoldedHeaderSource = source.type == "header" && source.collapsed && !source.foldLock

            if (!isFoldedHeaderSource) {
                // 通常の移動: 歯抜け・重複位置バグを完全に防ぐ厳格なリスト再構成＆連番正規化
                val item = topLevels.removeAt(sIdx)
                topLevels.add(tIdx, item)
                val updatedTopLevels = topLevels.mapIndexed { idx, it -> it.copy(position = idx) }
                updateDbItemsInMemoryAndPersist(updatedTopLevels)
                return
            }

            // 折りたたまれている見出しの場合のみ、配下の要素を一括移動
            var sourceSectionEnd = sIdx + 1
            while (sourceSectionEnd < topLevels.size && topLevels[sourceSectionEnd].type != "header") {
                sourceSectionEnd++
            }

            // 折りたたみ配下の要素自身への移動は無視
            if (tIdx >= sIdx && tIdx < sourceSectionEnd) return

            val sourceSection = topLevels.subList(sIdx, sourceSectionEnd).toList()
            topLevels.subList(sIdx, sourceSectionEnd).clear()

            val newTIdx = topLevels.indexOfFirst { it.id == targetId }
            if (newTIdx == -1) return

            val isFoldedHeaderTarget = target.type == "header" && target.collapsed && !target.foldLock
            val insertIdx = if (sIdx < tIdx) {
                if (isFoldedHeaderTarget) {
                    var endTarget = newTIdx + 1
                    while (endTarget < topLevels.size && topLevels[endTarget].type != "header") {
                        endTarget++
                    }
                    endTarget
                } else {
                    newTIdx + 1
                }
            } else {
                newTIdx
            }

            topLevels.addAll(insertIdx, sourceSection)

            val updatedTopLevels = topLevels.mapIndexed { idx, it ->
                it.copy(position = idx)
            }
            updateDbItemsInMemoryAndPersist(updatedTopLevels)
        } else if (sourceParentId == targetParentId) {
            val siblings = items.filter { it.parentId == sourceParentId }.sortedBy { it.position }.toMutableList()
            val sourceIdx = siblings.indexOfFirst { it.id == sourceId }
            val targetIdx = siblings.indexOfFirst { it.id == targetId }
            if (sourceIdx == -1 || targetIdx == -1) return

            val item = siblings.removeAt(sourceIdx)
            siblings.add(targetIdx, item)
            val updatedSiblings = siblings.mapIndexed { idx, it -> it.copy(position = idx) }
            updateDbItemsInMemoryAndPersist(updatedSiblings)
        } else {
            // Move to different parent (グループ間または階層間移動)
            val oldSiblings = items.filter { it.parentId == sourceParentId && it.id != sourceId }.sortedBy { it.position }
            val newSiblings = items.filter { it.parentId == targetParentId }.sortedBy { it.position }.toMutableList()
            val targetIdx = newSiblings.indexOfFirst { it.id == targetId }
            val insertIdx = if (targetIdx != -1) targetIdx else newSiblings.size
            newSiblings.add(insertIdx, source.copy(parentId = targetParentId))

            val normalizedOld = oldSiblings.mapIndexed { idx, it -> it.copy(position = idx) }
            val normalizedNew = newSiblings.mapIndexed { idx, it -> it.copy(position = idx) }
            updateDbItemsInMemoryAndPersist(normalizedOld + normalizedNew)
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
        val items = _coreState.value.items
        val siblings = if (parentId == null) items.filter { it.parentId == null } else items.filter { it.parentId == parentId }
        val nextPos = if (siblings.isEmpty()) 0 else siblings.maxOf { it.position } + 1

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
        val items = _coreState.value.items
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
        val items = _coreState.value.items
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
        val item = _coreState.value.items.find { it.id == id } ?: return
        updateDbItemInMemoryAndPersist(item.copy(name = newName))
    }

    fun updateItemColor(id: String, hexColor: String) {
        val item = _coreState.value.items.find { it.id == id } ?: return
        updateDbItemInMemoryAndPersist(item.copy(color = hexColor))
    }

    fun updateItemSettings(
        id: String,
        settings: TimerSettingsUpdate
    ) {
        val now = System.currentTimeMillis()
        val item = _coreState.value.items.find { it.id == id } ?: return

        var updated = item
        var needsDbUpdate = false

        val intervalMin = settings.intervalMin
        val max = settings.max
        val useChunk = settings.useChunk
        val useChunkClear = settings.useChunkClear
        val durationMin = settings.durationMin
        val countMode = settings.countMode
        val orbMode = settings.orbMode
        val layout = settings.layout
        val foldLock = settings.foldLock
        val orbEditHours = settings.orbEditHours
        val orbEditMinutes = settings.orbEditMinutes
        val idleEditHours = settings.idleEditHours
        val idleEditMinutes = settings.idleEditMinutes

        if (intervalMin != null && intervalMin != item.intervalMin) {
            val safeInt = intervalMin.coerceAtLeast(1)
            // Compute real-time recovered current before interval change
            val recovery = when (item.type) {
                "stam", "orb" -> TimerEngine.calculateRecoveryInfo(item, now)
                else -> RecoveryInfo(item.current, 0L, 0L, item.current >= (max ?: updated.max))
            }
            val realCurrent = recovery.cur
            // 回復間隔変更時は回復サイクルをリセットし、現在時刻から新しい間隔でスタート
            updated = updated.copy(
                intervalMin = safeInt,
                current = realCurrent,
                start = now
            )
            needsDbUpdate = true
        }

        if (max != null && max != item.max) {
            val safeMax = max.coerceAtLeast(1)
            // Compute real-time recovered current before max change
            val recovery = when (item.type) {
                "stam", "orb" -> TimerEngine.calculateRecoveryInfo(item, now)
                else -> RecoveryInfo(updated.current, 0L, 0L, updated.current >= safeMax, item.start)
            }
            val realCurrent = recovery.cur
            val wasFull = recovery.isFull
            val preservedStart = TimerEngine.preservePhaseStart(item, now)
            val newCur = realCurrent.coerceAtMost(safeMax)
            val isNowFull = newCur >= safeMax

            val newStart = if (wasFull && isNowFull) {
                recovery.fullAt
            } else if (isNowFull || wasFull) {
                now
            } else {
                preservedStart
            }

            updated = updated.copy(
                max = safeMax,
                current = newCur,
                start = newStart
            )
            needsDbUpdate = true
        }

        if (useChunkClear) {
            if (updated.useChunk != null) {
                updated = updated.copy(useChunk = null)
                needsDbUpdate = true
            }
        } else if (useChunk != null) {
            val safeChunk = useChunk.coerceAtLeast(1)
            if (safeChunk != updated.useChunk) {
                updated = updated.copy(useChunk = safeChunk)
                needsDbUpdate = true
            }
        }

        if (durationMin != null) {
            val safeDur = durationMin.coerceAtLeast(0)
            if (safeDur != updated.durationMin) {
                updated = updated.copy(durationMin = safeDur)
                needsDbUpdate = true
            }
        }

        if (countMode != null && countMode != updated.countMode) {
            updated = updated.copy(countMode = countMode)
            needsDbUpdate = true
        }

        if (orbMode != null && orbMode != updated.orbMode) {
            updated = updated.copy(orbMode = orbMode)
            needsDbUpdate = true
        }

        if (layout != null && layout != updated.layout) {
            updated = updated.copy(layout = layout)
            needsDbUpdate = true
        }

        if (foldLock != null && foldLock != updated.foldLock) {
            updated = updated.copy(foldLock = foldLock)
            needsDbUpdate = true
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

            if (nextCur != updated.current || nextStart != updated.start) {
                updated = updated.copy(
                    current = nextCur,
                    start = nextStart
                )
                needsDbUpdate = true
            }
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

            if (nextStart != updated.start || updated.state == "claim") {
                updated = updated.copy(
                    start = nextStart,
                    state = if (updated.state == "claim") "running" else updated.state
                )
                needsDbUpdate = true
            }
        }

        if (needsDbUpdate) {
            updateDbItemInMemoryAndPersist(updated)
        }
    }

    fun deleteItem(id: String) {
        val item = _coreState.value.items.find { it.id == id } ?: return
        deleteDbItemInMemoryAndPersist(item)
    }

    fun cloneGroup(id: String) {
        val dbList = _coreState.value.items
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
        _coreState.value = _coreState.value.copy(items = allNew)
        TimerRepository.updateCache(allNew)
        viewModelScope.launch {
            // Atomic batch update & insert transaction
            repository.batchUpdateAndInsert(
                itemsToUpdate = updatedTopLevels,
                itemsToInsert = listOf(newGroup) + newChildren
            )
        }
    }

    fun toggleHeaderCollapsed(id: String) {
        val header = _coreState.value.items.find { it.id == id && it.type == "header" } ?: return
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
        return TimerBackupManager.exportBackupJson(_coreState.value.items, customColorsFlow.value)
    }

    /**
     * Imports items and custom colors from a backup JSON string with strict schema validation
     * and automatic position sequence normalization.
     */
    suspend fun importBackupJson(jsonString: String): Boolean {
        val backupData = TimerBackupManager.parseBackupJson(jsonString) ?: return false
        if (backupData.customColors.isNotEmpty()) {
            repository.saveCustomColors(backupData.customColors)
        }
        repository.replaceAllItems(backupData.items)
        refreshNow()
        return true
    }
}
