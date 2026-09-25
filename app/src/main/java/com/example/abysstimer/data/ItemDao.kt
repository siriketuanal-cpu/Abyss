package com.example.abysstimer.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY position ASC")
    fun getAllItemsFlow(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY position ASC")
    suspend fun getAllItems(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ItemEntity>)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Update
    suspend fun updateItems(items: List<ItemEntity>)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM items WHERE parentId = :parentId")
    suspend fun deleteChildrenOf(parentId: String)

    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    @Transaction
    suspend fun replaceAllItems(items: List<ItemEntity>) {
        clearAllItems()
        insertItems(items)
    }

    @Transaction
    suspend fun batchUpdateAndInsert(
        itemsToUpdate: List<ItemEntity>,
        itemsToInsert: List<ItemEntity> = emptyList()
    ) {
        if (itemsToUpdate.isNotEmpty()) {
            updateItems(itemsToUpdate)
        }
        if (itemsToInsert.isNotEmpty()) {
            insertItems(itemsToInsert)
        }
    }

    @Query("SELECT * FROM custom_colors ORDER BY slotIndex ASC")
    fun getAllCustomColorsFlow(): Flow<List<CustomColorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomColors(colors: List<CustomColorEntity>)
}
