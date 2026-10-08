package com.example.abysstimer.ui

import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Result data class for parsed backup content.
 */
data class BackupData(
    val items: List<ItemEntity>,
    val customColors: List<String>
)

/**
 * Pure business logic component for backup export and import parsing.
 * Decoupled from ViewModel for testability and performance.
 */
object TimerBackupManager {

    /**
     * Serializes all current database items and custom colors into a lightweight JSON string.
     */
    fun exportBackupJson(
        items: List<ItemEntity>,
        customColors: List<CustomColorEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val itemsArray = JSONArray()
        for (item in items) {
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
        for (c in customColors) {
            val obj = JSONObject()
            obj.put("slotIndex", c.slotIndex)
            obj.put("hexColor", c.hexColor)
            colorsArray.put(obj)
        }
        root.put("customColors", colorsArray)

        return root.toString()
    }

    /**
     * Parses and validates raw JSON string into BackupData.
     * Applies schema checks and normalizes positions using TimerEngine.
     * Returns null if the JSON is malformed or invalid.
     */
    fun parseBackupJson(jsonString: String): BackupData? {
        return try {
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

            val root = JSONObject(trimmed)
            val itemsArray = root.optJSONArray("items") ?: return null

            val parsedItems = mutableListOf<ItemEntity>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                val id = obj.optString("id").ifEmpty { UUID.randomUUID().toString() }
                val type = obj.optString("type", "stam").let {
                    if (it in setOf("stam", "orb", "idle", "exped", "group", "header", "rule", "space")) it else "stam"
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
                val layout = if (obj.has("layout") && !obj.isNull("layout")) {
                    obj.optString("layout")
                } else if (type == "header") {
                    "5"
                } else {
                    null
                }
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

            // Normalization: clean and ensure contiguous positions via unified TimerEngine logic
            val normalizedItems = TimerEngine.normalizeItemPositions(parsedItems)

            // Restore custom colors if present
            val hexList = mutableListOf<String>()
            val colorsArray = root.optJSONArray("customColors")
            if (colorsArray != null) {
                for (i in 0 until colorsArray.length()) {
                    val obj = colorsArray.optJSONObject(i) ?: continue
                    val hex = obj.optString("hexColor", "")
                    if (hex.isNotEmpty()) hexList.add(hex)
                }
            }

            BackupData(
                items = normalizedItems,
                customColors = hexList
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
