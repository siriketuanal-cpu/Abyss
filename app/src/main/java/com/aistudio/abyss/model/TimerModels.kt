package com.aistudio.abyss.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ItemType {
    STAM,
    ABYSS,
    IDLE,
    GROUP,
    HEADER,
    RULE
}

sealed class TimerEntry(
    open val id: String,
    open val type: ItemType
) {
    abstract fun toJson(): JSONObject

    companion object {
        fun fromJson(json: JSONObject): TimerEntry? {
            val typeStr = json.optString("type", "")
            val id = json.optString("id", UUID.randomUUID().toString())
            return when (typeStr) {
                "stam" -> StamItem(
                    id = id,
                    name = json.optString("name", ""),
                    current = json.optInt("current", 0),
                    max = json.optInt("max", 100),
                    intervalMin = json.optInt("intervalMin", 5),
                    start = json.optLong("start", System.currentTimeMillis())
                )
                "abyss" -> {
                    val rank = json.optInt("rank", 1).coerceIn(1, 200)
                    val maxVal = abyssMaxFor(rank)
                    val curVal = json.optInt("current", 0).coerceAtMost(maxVal)
                    AbyssItem(
                        id = id,
                        name = json.optString("name", ""),
                        rank = rank,
                        current = curVal,
                        max = maxVal,
                        intervalMin = json.optInt("intervalMin", 3),
                        start = json.optLong("start", System.currentTimeMillis())
                    )
                }
                "idle" -> IdleItem(
                    id = id,
                    name = json.optString("name", ""),
                    durationMin = json.optInt("durationMin", 720),
                    state = json.optString("state", "running"),
                    start = json.optLong("start", System.currentTimeMillis())
                )
                "group" -> {
                    val childrenJson = json.optJSONArray("children") ?: JSONArray()
                    val children = mutableListOf<TimerCardItem>()
                    for (i in 0 until childrenJson.length()) {
                        val childObj = childrenJson.optJSONObject(i) ?: continue
                        val child = fromJson(childObj) as? TimerCardItem
                        if (child != null) children.add(child)
                    }
                    GroupItem(
                        id = id,
                        name = json.optString("name", ""),
                        children = children
                    )
                }
                "header" -> HeaderItem(
                    id = id,
                    name = json.optString("name", ""),
                    color = json.optString("color", "#9B8BFF")
                )
                "rule" -> RuleItem(
                    id = id,
                    color = json.optString("color", "#52617A")
                )
                else -> null
            }
        }
    }
}

sealed class TimerCardItem(
    override val id: String,
    override val type: ItemType
) : TimerEntry(id, type)

data class StamItem(
    override val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val current: Int = 0,
    val max: Int = 100,
    val intervalMin: Int = 5,
    val start: Long = System.currentTimeMillis()
) : TimerCardItem(id, ItemType.STAM) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "stam")
        put("name", name)
        put("current", current)
        put("max", max)
        put("intervalMin", intervalMin)
        put("start", start)
    }
}

data class AbyssItem(
    override val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rank: Int = 1,
    val current: Int = 0,
    val max: Int = abyssMaxFor(rank),
    val intervalMin: Int = 3,
    val start: Long = System.currentTimeMillis()
) : TimerCardItem(id, ItemType.ABYSS) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "abyss")
        put("name", name)
        put("rank", rank)
        put("current", current)
        put("max", max)
        put("intervalMin", intervalMin)
        put("start", start)
    }
}

data class IdleItem(
    override val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val durationMin: Int = 720,
    val state: String = "running", // "running" or "claim"
    val start: Long = System.currentTimeMillis()
) : TimerCardItem(id, ItemType.IDLE) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "idle")
        put("name", name)
        put("durationMin", durationMin)
        put("state", state)
        put("start", start)
    }
}

data class GroupItem(
    override val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val children: List<TimerCardItem> = emptyList()
) : TimerEntry(id, ItemType.GROUP) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "group")
        put("name", name)
        val arr = JSONArray()
        children.forEach { arr.put(it.toJson()) }
        put("children", arr)
    }
}

data class HeaderItem(
    override val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = "#9B8BFF"
) : TimerEntry(id, ItemType.HEADER) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "header")
        put("name", name)
        put("color", color)
    }
}

data class RuleItem(
    override val id: String = UUID.randomUUID().toString(),
    val color: String = "#52617A"
) : TimerEntry(id, ItemType.RULE) {
    override fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", "rule")
        put("color", color)
    }
}

// Business calculation helpers

data class StaminaCalc(
    val current: Int,
    val remainMs: Long,
    val isFull: Boolean,
    val fullAt: Long
)

data class IdleCalc(
    val remainMs: Long,
    val isFull: Boolean,
    val fullAt: Long
)

fun abyssMaxFor(rank: Int): Int {
    val r = rank.coerceIn(1, 200)
    return 240 + (r - 1) * 5
}

fun remainingAfter40(current: Int): Int {
    return current.coerceAtLeast(0) % 40
}

fun calculateStamina(current: Int, max: Int, intervalMin: Int, start: Long, now: Long = System.currentTimeMillis()): StaminaCalc {
    val intervalMs = (intervalMin.coerceAtLeast(1)) * 60_000L
    if (current >= max) {
        return StaminaCalc(current = max, remainMs = 0L, isFull = true, fullAt = start)
    }
    val elapsed = (now - start).coerceAtLeast(0L)
    val recovered = (elapsed / intervalMs).toInt()
    val cur = (current + recovered).coerceAtMost(max)
    if (cur >= max) {
        val fullAt = start + (max - current).coerceAtLeast(0) * intervalMs
        return StaminaCalc(current = max, remainMs = 0L, isFull = true, fullAt = minOf(now, fullAt))
    }
    val nextIn = intervalMs - (elapsed % intervalMs)
    val need = max - cur
    val remainMs = (need - 1) * intervalMs + nextIn
    val fullAt = now + remainMs
    return StaminaCalc(current = cur, remainMs = remainMs, isFull = false, fullAt = fullAt)
}

fun calculateIdle(durationMin: Int, start: Long, now: Long = System.currentTimeMillis()): IdleCalc {
    val durMs = durationMin.coerceAtLeast(1) * 60_000L
    val elapsed = (now - start).coerceAtLeast(0L)
    val remainMs = (durMs - elapsed).coerceAtLeast(0L)
    return IdleCalc(
        remainMs = remainMs,
        isFull = remainMs <= 0L,
        fullAt = start + durMs
    )
}

fun preserveCycle(intervalMin: Int, start: Long, now: Long = System.currentTimeMillis()): Long {
    val intervalMs = intervalMin.coerceAtLeast(1) * 60_000L
    val phase = ((now - start) % intervalMs + intervalMs) % intervalMs
    return now - phase
}

fun formatHM(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun formatCountdown(remainMs: Long): String {
    val safeMs = remainMs.coerceAtLeast(0L)
    val totalMin = (safeMs + 59_999L) / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return String.format(Locale.US, "%d:%02d", h, m)
}

fun isNearFull(remainMs: Long, isFull: Boolean): Boolean {
    return isFull || (remainMs > 0 && remainMs < 7_200_000L) // Under 2 hours
}
