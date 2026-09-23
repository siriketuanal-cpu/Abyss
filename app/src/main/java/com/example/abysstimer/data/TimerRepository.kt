package com.example.abysstimer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TimerRepository(private val itemDao: ItemDao) {

    val allItemsFlow: Flow<List<ItemEntity>> = itemDao.getAllItemsFlow()
    val allCustomColorsFlow: Flow<List<CustomColorEntity>> = itemDao.getAllCustomColorsFlow()

    suspend fun getAllItems(): List<ItemEntity> {
        return itemDao.getAllItems()
    }

    suspend fun insertItem(item: ItemEntity) {
        itemDao.insertItem(item)
    }

    suspend fun insertItems(items: List<ItemEntity>) {
        itemDao.insertItems(items)
    }

    suspend fun updateItem(item: ItemEntity) {
        itemDao.updateItem(item)
    }

    suspend fun deleteItem(item: ItemEntity) {
        itemDao.deleteItem(item)
        if (item.type == "group") {
            // Also delete all children belonging to this group
            itemDao.deleteChildrenOf(item.id)
        }
    }

    suspend fun deleteItemById(id: String) {
        itemDao.deleteItemById(id)
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
}
