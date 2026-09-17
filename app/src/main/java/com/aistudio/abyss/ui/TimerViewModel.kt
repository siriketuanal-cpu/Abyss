package com.aistudio.abyss.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.abyss.data.TimerRepository
import com.aistudio.abyss.model.AbyssItem
import com.aistudio.abyss.model.GroupItem
import com.aistudio.abyss.model.HeaderItem
import com.aistudio.abyss.model.IdleItem
import com.aistudio.abyss.model.RuleItem
import com.aistudio.abyss.model.StamItem
import com.aistudio.abyss.model.TimerCardItem
import com.aistudio.abyss.model.TimerEntry
import com.aistudio.abyss.model.abyssMaxFor
import com.aistudio.abyss.model.calculateStamina
import com.aistudio.abyss.model.preserveCycle
import com.aistudio.abyss.model.remainingAfter40
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class DialogState {
    data class AddPanel(val parentGroupId: String? = null) : DialogState()
    data class SetupTimer(val parentGroupId: String?, val type: String) : DialogState()
    data class SetupHeader(val initialColor: String = "#9B8BFF") : DialogState()
    data class SetupRule(val initialColor: String = "#52617A") : DialogState()
    data class EditName(val id: String, val title: String, val currentName: String) : DialogState()
    data class EditNumber(val id: String, val title: String, val currentValue: Int, val min: Int, val max: Int, val onConfirm: (Int) -> Unit) : DialogState()
    data class EditRank(val id: String, val currentRank: Int) : DialogState()
    data class EditHeaderColor(val id: String, val currentColor: String) : DialogState()
    data class ActionMenu(val item: TimerEntry, val parentGroupId: String? = null) : DialogState()
}

class TimerViewModel(private val repository: TimerRepository) : ViewModel() {

    private val _items = MutableStateFlow<List<TimerEntry>>(emptyList())
    val items: StateFlow<List<TimerEntry>> = _items.asStateFlow()

    private val _now = MutableStateFlow(System.currentTimeMillis())
    val now: StateFlow<Long> = _now.asStateFlow()

    private val _pending40Id = MutableStateFlow<String?>(null)
    val pending40Id: StateFlow<String?> = _pending40Id.asStateFlow()

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    init {
        loadData()
        startClock()
    }

    private fun loadData() {
        val loaded = repository.loadItems()
        if (loaded.isNotEmpty()) {
            _items.value = loaded
        }
    }

    private fun saveData() {
        repository.saveItems(_items.value)
    }

    private fun startClock() {
        viewModelScope.launch {
            while (isActive) {
                _now.value = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    fun openAddMenu(parentGroupId: String? = null) {
        _dialogState.value = DialogState.AddPanel(parentGroupId)
    }

    fun openSetupTimer(parentGroupId: String?, type: String) {
        _dialogState.value = DialogState.SetupTimer(parentGroupId, type)
    }

    fun openSetupHeader() {
        _dialogState.value = DialogState.SetupHeader()
    }

    fun openSetupRule() {
        _dialogState.value = DialogState.SetupRule()
    }

    fun openActionMenu(item: TimerEntry, parentGroupId: String? = null) {
        _dialogState.value = DialogState.ActionMenu(item, parentGroupId)
    }

    fun openEditName(id: String, title: String, currentName: String) {
        _dialogState.value = DialogState.EditName(id, title, currentName)
    }

    fun openEditRank(id: String, currentRank: Int) {
        _dialogState.value = DialogState.EditRank(id, currentRank)
    }

    fun openEditNumber(id: String, title: String, currentValue: Int, min: Int, max: Int, onConfirm: (Int) -> Unit) {
        _dialogState.value = DialogState.EditNumber(id, title, currentValue, min, max, onConfirm)
    }

    fun openEditHeaderColor(id: String, currentColor: String) {
        _dialogState.value = DialogState.EditHeaderColor(id, currentColor)
    }

    fun dismissDialog() {
        _dialogState.value = null
    }

    fun cancelPendingActions() {
        if (_pending40Id.value != null) {
            _pending40Id.value = null
        }
        // Cancel claim states on idle items
        var changed = false
        val currentList = _items.value.map { entry ->
            when (entry) {
                is GroupItem -> {
                    val newChildren = entry.children.map { child ->
                        if (child is IdleItem && child.state == "claim") {
                            changed = true
                            child.copy(state = "running")
                        } else child
                    }
                    if (changed) entry.copy(children = newChildren) else entry
                }
                is IdleItem -> {
                    if (entry.state == "claim") {
                        changed = true
                        entry.copy(state = "running")
                    } else entry
                }
                else -> entry
            }
        }
        if (changed) {
            _items.value = currentList
            saveData()
        }
    }

    fun addGroup(name: String = "") {
        _items.value = _items.value + GroupItem(name = name)
        saveData()
        dismissDialog()
    }

    fun addHeader(color: String = "#9B8BFF") {
        _items.value = _items.value + HeaderItem(color = color)
        saveData()
        dismissDialog()
    }

    fun addRule(color: String = "#52617A") {
        _items.value = _items.value + RuleItem(color = color)
        saveData()
        dismissDialog()
    }

    fun addStam(parentGroupId: String?, intervalMin: Int) {
        val item = StamItem(intervalMin = intervalMin, max = 100, current = 0)
        insertChildTimer(parentGroupId, item)
        dismissDialog()
    }

    fun addAbyss(parentGroupId: String?, intervalMin: Int) {
        val rank = 1
        val item = AbyssItem(rank = rank, max = abyssMaxFor(rank), intervalMin = intervalMin, current = 0)
        insertChildTimer(parentGroupId, item)
        dismissDialog()
    }

    fun addIdle(parentGroupId: String?, durationMin: Int) {
        val item = IdleItem(durationMin = durationMin)
        insertChildTimer(parentGroupId, item)
        dismissDialog()
    }

    private fun insertChildTimer(parentGroupId: String?, child: TimerCardItem) {
        if (parentGroupId != null) {
            _items.value = _items.value.map { entry ->
                if (entry is GroupItem && entry.id == parentGroupId && entry.children.size < 2) {
                    entry.copy(children = entry.children + child)
                } else entry
            }
        } else {
            // Create a group automatically if none exists, or append
            val newGroup = GroupItem(children = listOf(child))
            _items.value = _items.value + newGroup
        }
        saveData()
    }

    fun deleteItem(id: String, parentGroupId: String? = null) {
        if (parentGroupId != null) {
            _items.value = _items.value.map { entry ->
                if (entry is GroupItem && entry.id == parentGroupId) {
                    entry.copy(children = entry.children.filter { it.id != id })
                } else entry
            }
        } else {
            _items.value = _items.value.filter { it.id != id }
        }
        if (_pending40Id.value == id) _pending40Id.value = null
        saveData()
        dismissDialog()
    }

    fun updateName(id: String, newName: String) {
        _items.value = _items.value.map { entry ->
            when {
                entry.id == id -> when (entry) {
                    is GroupItem -> entry.copy(name = newName)
                    is HeaderItem -> entry.copy(name = newName)
                    is StamItem -> entry.copy(name = newName)
                    is AbyssItem -> entry.copy(name = newName)
                    is IdleItem -> entry.copy(name = newName)
                    else -> entry
                }
                entry is GroupItem -> {
                    entry.copy(children = entry.children.map { child ->
                        if (child.id == id) {
                            when (child) {
                                is StamItem -> child.copy(name = newName)
                                is AbyssItem -> child.copy(name = newName)
                                is IdleItem -> child.copy(name = newName)
                            }
                        } else child
                    })
                }
                else -> entry
            }
        }
        saveData()
        dismissDialog()
    }

    fun updateHeaderColor(id: String, newColor: String) {
        _items.value = _items.value.map { entry ->
            if (entry is HeaderItem && entry.id == id) {
                entry.copy(color = newColor)
            } else entry
        }
        saveData()
        dismissDialog()
    }

    fun updateStamCurrent(id: String, newCurrent: Int) {
        val currentTime = System.currentTimeMillis()
        updateCardItem(id) { item ->
            if (item is StamItem) {
                val clamped = newCurrent.coerceIn(0, item.max)
                val newStart = preserveCycle(item.intervalMin, item.start, currentTime)
                item.copy(current = clamped, start = newStart)
            } else item
        }
    }

    fun updateStamMax(id: String, newMax: Int) {
        val currentTime = System.currentTimeMillis()
        updateCardItem(id) { item ->
            if (item is StamItem) {
                val validMax = newMax.coerceIn(1, 999)
                val calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, currentTime)
                val newCur = minOf(calc.current, validMax)
                val newStart = preserveCycle(item.intervalMin, item.start, currentTime)
                item.copy(max = validMax, current = newCur, start = newStart)
            } else item
        }
    }

    fun updateAbyssRank(id: String, newRank: Int) {
        val currentTime = System.currentTimeMillis()
        val validRank = newRank.coerceIn(1, 200)
        val newMax = abyssMaxFor(validRank)
        updateCardItem(id) { item ->
            if (item is AbyssItem) {
                val calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, currentTime)
                val newCur = minOf(calc.current, newMax)
                val newStart = preserveCycle(item.intervalMin, item.start, currentTime)
                item.copy(rank = validRank, max = newMax, current = newCur, start = newStart)
            } else item
        }
        _pending40Id.value = null
    }

    fun updateAbyssCurrent(id: String, newCurrent: Int) {
        val currentTime = System.currentTimeMillis()
        updateCardItem(id) { item ->
            if (item is AbyssItem) {
                val clamped = newCurrent.coerceIn(0, item.max)
                val newStart = preserveCycle(item.intervalMin, item.start, currentTime)
                item.copy(current = clamped, start = newStart)
            } else item
        }
        _pending40Id.value = null
    }

    fun onAbyssCardTap(item: AbyssItem) {
        val now = System.currentTimeMillis()
        if (_pending40Id.value == item.id) {
            // Confirm 40 consumption
            val calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, now)
            val newCur = remainingAfter40(calc.current)
            val newStart = preserveCycle(item.intervalMin, item.start, now)
            updateCardItem(item.id) {
                if (it is AbyssItem) it.copy(current = newCur, start = newStart) else it
            }
            _pending40Id.value = null
        } else {
            _pending40Id.value = item.id
        }
    }

    fun onIdleCardTap(item: IdleItem) {
        val now = System.currentTimeMillis()
        if (item.state == "claim") {
            // Confirm claim: reset start time to now and state to running
            updateCardItem(item.id) {
                if (it is IdleItem) it.copy(state = "running", start = now) else it
            }
        } else {
            // Enter claim wait state
            updateCardItem(item.id) {
                if (it is IdleItem) it.copy(state = "claim") else it
            }
        }
    }

    private fun updateCardItem(id: String, transform: (TimerCardItem) -> TimerCardItem) {
        _items.value = _items.value.map { entry ->
            when (entry) {
                is TimerCardItem -> if (entry.id == id) transform(entry) else entry
                is GroupItem -> entry.copy(
                    children = entry.children.map { child ->
                        if (child.id == id) transform(child) else child
                    }
                )
                else -> entry
            }
        }
        saveData()
    }
}
