package com.example.abysstimer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TimerRepository(private val itemDao: ItemDao) {

    companion object {
        @Volatile
        var inMemoryCache: List<ItemEntity>? = null
            private set

        @Volatile
        private var hasCheckedColors = false

        fun updateCache(items: List<ItemEntity>) {
            inMemoryCache = items
        }

        fun clearCache() {
            inMemoryCache = null
        }
    }

    val allItemsFlow: Flow<List<ItemEntity>> = itemDao.getAllItemsFlow()
    val allCustomColorsFlow: Flow<List<CustomColorEntity>> = itemDao.getAllCustomColorsFlow()

    suspend fun getAllItems(): List<ItemEntity> {
        val items = itemDao.getAllItems()
        inMemoryCache = items
        return items
    }

    suspend fun insertItem(item: ItemEntity) {
        itemDao.insertItem(item)
        inMemoryCache = inMemoryCache?.let { it + item }
    }

    suspend fun insertItems(items: List<ItemEntity>) {
        itemDao.insertItems(items)
        inMemoryCache = inMemoryCache?.let { it + items }
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item)
        inMemoryCache = inMemoryCache?.map { if (it.id == item.id) item else it }
    }

    suspend fun updateItems(items: List<ItemEntity>) {
        itemDao.updateItems(items)
        if (inMemoryCache != null) {
            val map = items.associateBy { it.id }
            inMemoryCache = inMemoryCache?.map { map[it.id] ?: it }
        }
    }

    suspend fun batchUpdateAndInsert(
        itemsToUpdate: List<ItemEntity>,
        itemsToInsert: List<ItemEntity> = emptyList()
    ) {
        itemDao.batchUpdateAndInsert(itemsToUpdate, itemsToInsert)
        if (inMemoryCache != null) {
            val map = itemsToUpdate.associateBy { it.id }
            inMemoryCache = inMemoryCache?.map { map[it.id] ?: it }?.let { it + itemsToInsert }
        }
    }

    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
        if (item.type == "group") {
            // Also delete all children belonging to this group
            itemDao.deleteChildrenOf(item.id)
            inMemoryCache = inMemoryCache?.filter { it.id != item.id && it.parentId != item.id }
        } else {
            inMemoryCache = inMemoryCache?.filter { it.id != item.id }
        }
    }

    suspend fun deleteItemAndReorder(item: ItemEntity, changedItems: List<ItemEntity>) {
        itemDao.deleteItemAndReorder(item, changedItems)
        val remaining = if (item.type == "group") {
            inMemoryCache?.filter { it.id != item.id && it.parentId != item.id }
        } else {
            inMemoryCache?.filter { it.id != item.id }
        }
        if (remaining != null) {
            val map = changedItems.associateBy { it.id }
            inMemoryCache = remaining.map { map[it.id] ?: it }
        }
    }

    suspend fun deleteItemById(id: String) {
        itemDao.deleteItemById(id)
        inMemoryCache = inMemoryCache?.filter { it.id != id }
    }

    suspend fun initializeDefaultColorsIfEmpty() {
        if (hasCheckedColors) return
        val colors = itemDao.getAllCustomColorsFlow().first()
        if (colors.isEmpty()) {
            val defaults = listOf(
                "#38bdf8", "#f472b6", "#4ade80", "#fbbf24", "#a78bfa", "#fb923c"
            )
            val entities = defaults.mapIndexed { index, hex ->
                CustomColorEntity(index, hex)
            }
            itemDao.insertCustomColors(entities)
        }
        hasCheckedColors = true
    }

    suspend fun saveCustomColors(colors: List<String>) {
        val entities = colors.take(6).mapIndexed { index, hex ->
            CustomColorEntity(index, hex)
        }
        itemDao.insertCustomColors(entities)
        hasCheckedColors = true
    }

    suspend fun replaceAllItems(items: List<ItemEntity>) {
        itemDao.replaceAllItems(items)
        inMemoryCache = null
    }
}
