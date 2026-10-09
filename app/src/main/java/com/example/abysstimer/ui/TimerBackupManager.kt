package com.example.abysstimer.ui

import android.util.Base64
import com.example.abysstimer.data.CustomColorEntity
import com.example.abysstimer.data.ItemEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Result data class for parsed backup content.
 */
data class BackupData(
    val items: List<ItemEntity>,
    val customColors: List<String>
)

/**
 * Pure business logic component for backup export and import parsing.
 * Optimized compact representation with Gzip + Base64 encoding (Plan B).
 */
object TimerBackupManager {

    private const val PREFIX = "ABYSS:"

    /**
     * Serializes all current database items and custom colors into a compact,
     * Gzip-compressed Base64 string prefixed with "ABYSS:".
     */
    fun exportBackupJson(
        items: List<ItemEntity>,
        customColors: List<CustomColorEntity>
    ): String {
        val root = JSONObject()
        root.put("v", 1)

        // Assign short string tokens ("0", "1", "2", ...) to parent groups to omit UUID bloat
        val groupIdMap = mutableMapOf<String, String>()
        var groupIndex = 0
        for (item in items) {
            if (item.type == "group") {
                groupIdMap[item.id] = (groupIndex++).toString()
            }
        }

        val itemsArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("t", item.type)

            if (item.name.isNotEmpty()) {
                obj.put("n", item.name)
            }
            if (item.color != null) {
                obj.put("c", item.color)
            }

            when (item.type) {
                "group" -> {
                    groupIdMap[item.id]?.let { obj.put("id", it) }
                    if (item.layout != null && item.layout != "regular") {
                        obj.put("l", item.layout)
                    }
                }
                "header" -> {
                    if (item.collapsed) obj.put("col", true)
                    if (item.foldLock) obj.put("fl", true)
                    if (item.layout != null && item.layout != "5") {
                        obj.put("l", item.layout)
                    }
                }
                "rule", "space" -> {
                    // Minimal: only type and optional name/color
                }
                "stam", "orb" -> {
                    if (item.current > 0) obj.put("cur", item.current)
                    if (item.max != 100) obj.put("m", item.max)
                    if (item.intervalMin != 5) obj.put("i", item.intervalMin)
                    obj.put("s", item.start)
                    item.useChunk?.let { obj.put("chk", it) }
                    if (item.orbMode != null && item.orbMode != "down") {
                        obj.put("om", item.orbMode)
                    }
                    item.parentId?.let { pId ->
                        groupIdMap[pId]?.let { obj.put("pid", it) }
                    }
                }
                "idle", "exped" -> {
                    if (item.durationMin > 0) obj.put("d", item.durationMin)
                    obj.put("s", item.start)
                    if (item.countMode != null && item.countMode != "down") {
                        obj.put("cm", item.countMode)
                    }
                    item.parentId?.let { pId ->
                        groupIdMap[pId]?.let { obj.put("pid", it) }
                    }
                }
            }

            itemsArray.put(obj)
        }
        root.put("items", itemsArray)

        if (customColors.isNotEmpty()) {
            val colorsArray = JSONArray()
            for (c in customColors) {
                if (c.hexColor.isNotEmpty()) {
                    colorsArray.put(c.hexColor)
                }
            }
            root.put("colors", colorsArray)
        }

        // Compress JSON with Gzip and encode into compact Base64
        val jsonBytes = root.toString().toByteArray(Charsets.UTF_8)
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(jsonBytes)
        }
        val compressedBytes = bos.toByteArray()
        val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        return PREFIX + base64
    }

    /**
     * Parses and validates raw backup string into BackupData.
     * Decompresses Base64 + Gzip and normalizes positions using TimerEngine.
     * Returns null if the data is malformed or invalid.
     */
    fun parseBackupJson(input: String): BackupData? {
        return try {
            val trimmed = input.trim()
            val jsonString = if (trimmed.startsWith(PREFIX)) {
                val base64Data = trimmed.substring(PREFIX.length).trim()
                val compressedBytes = Base64.decode(base64Data, Base64.NO_WRAP)
                val bis = ByteArrayInputStream(compressedBytes)
                val decompressedBytes = GZIPInputStream(bis).use { gzip ->
                    gzip.readBytes()
                }
                String(decompressedBytes, Charsets.UTF_8)
            } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed
            } else {
                return null
            }

            val root = JSONObject(jsonString)
            val itemsArray = root.optJSONArray("items") ?: return null

            // Map exported short group ids to freshly generated unique UUIDs
            val idMap = mutableMapOf<String, String>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                if (obj.has("id")) {
                    val bId = obj.optString("id")
                    idMap[bId] = UUID.randomUUID().toString()
                }
            }

            val parsedItems = mutableListOf<ItemEntity>()
            for (i in 0 until itemsArray.length()) {
                val obj = itemsArray.optJSONObject(i) ?: continue
                val bId = obj.optString("id", "")
                val newId = if (bId.isNotEmpty() && idMap.containsKey(bId)) {
                    idMap[bId]!!
                } else {
                    UUID.randomUUID().toString()
                }

                val bPid = obj.optString("pid", "")
                val parentId = if (bPid.isNotEmpty() && idMap.containsKey(bPid)) {
                    idMap[bPid]
                } else {
                    null
                }

                val type = obj.optString("t", "stam").let {
                    if (it in setOf("stam", "orb", "idle", "exped", "group", "header", "rule", "space")) it else "stam"
                }
                val name = obj.optString("n", "")
                val color = if (obj.has("c") && !obj.isNull("c")) obj.optString("c") else null
                val current = obj.optInt("cur", 0).coerceAtLeast(0)
                val max = obj.optInt("m", 100).coerceAtLeast(1)
                val intervalMin = obj.optInt("i", 5).coerceAtLeast(1)
                val start = obj.optLong("s", System.currentTimeMillis())
                val durationMin = obj.optInt("d", 0).coerceAtLeast(0)
                val useChunk = if (obj.has("chk") && !obj.isNull("chk")) obj.optInt("chk").coerceAtLeast(1) else null
                val orbMode = if (obj.has("om") && !obj.isNull("om")) obj.optString("om") else null
                val countMode = if (obj.has("cm") && !obj.isNull("cm")) obj.optString("cm") else null
                val collapsed = obj.optBoolean("col", false)
                val foldLock = obj.optBoolean("fl", false)
                val layout = if (obj.has("l") && !obj.isNull("l")) {
                    obj.optString("l")
                } else if (type == "header") {
                    "5"
                } else {
                    null
                }

                parsedItems.add(
                    ItemEntity(
                        id = newId,
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
                        state = null,
                        color = color,
                        collapsed = collapsed,
                        foldLock = foldLock,
                        layout = layout,
                        parentId = parentId,
                        position = i
                    )
                )
            }

            // Normalization: clean and ensure contiguous positions via unified TimerEngine logic
            val normalizedItems = TimerEngine.normalizeItemPositions(parsedItems)

            // Restore custom colors if present
            val hexList = mutableListOf<String>()
            val colorsArray = root.optJSONArray("colors")
            if (colorsArray != null) {
                for (i in 0 until colorsArray.length()) {
                    val hex = colorsArray.optString(i, "")
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
