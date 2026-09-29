package com.example.abysstimer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TimerRepository(private val itemDao: ItemDao) {

    companion object {
        @Volatile
        var inMemoryCache: List<ItemEntity>? = null
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
        inMemoryCache = null
    }

    suspend fun insertItems(items: List<ItemEntity>) {
        itemDao.insertItems(items)
        inMemoryCache = null
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item)
        inMemoryCache = null
    }

    suspend fun updateItems(items: List<ItemEntity>) {
        itemDao.updateItems(items)
        inMemoryCache = null
    }

    suspend fun batchUpdateAndInsert(
        itemsToUpdate: List<ItemEntity>,
        itemsToInsert: List<ItemEntity> = emptyList()
    ) {
        itemDao.batchUpdateAndInsert(itemsToUpdate, itemsToInsert)
        inMemoryCache = null
    }

    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
        if (item.type == "group") {
            // Also delete all children belonging to this group
            itemDao.deleteChildrenOf(item.id)
        }
        inMemoryCache = null
    }

    suspend fun deleteItemById(id: String) {
        itemDao.deleteItemById(id)
        inMemoryCache = null
    }

    suspend fun initializeDefaultColorsIfEmpty() {
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
    }

    suspend fun saveCustomColors(colors: List<String>) {
        val entities = colors.take(6).mapIndexed { index, hex ->
            CustomColorEntity(index, hex)
        }
        itemDao.insertCustomColors(entities)
    }

    suspend fun replaceAllItems(items: List<ItemEntity>) {
        itemDao.replaceAllItems(items)
        inMemoryCache = null
    }
}
