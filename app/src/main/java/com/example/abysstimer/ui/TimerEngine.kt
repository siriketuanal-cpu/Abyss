package com.example.abysstimer.ui

import androidx.compose.runtime.Immutable
import com.example.abysstimer.data.ItemEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class StamInfo(val cur: Int, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)

@Immutable
data class OrbInfo(val cur: Int, val remainMs: Long, val nextInMs: Long, val isFull: Boolean, val fullAt: Long = 0)

@Immutable
data class RecoveryInfo(val cur: Int, val remainMs: Long, val nextInMs: Long, val isFull: Boolean, val fullAt: Long = 0)

@Immutable
data class IdleInfo(val elapsed: Long, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)

/**
 * Pure calculation engine corresponding directly to WEB version engine.js.
 * Contains purely mathematical, zero-side-effect, thread-safe calculations.
 */
object TimerEngine {

    fun calculateRecoveryInfo(it: ItemEntity, now: Long): RecoveryInfo {
        val maxSafe = it.max.coerceAtLeast(1)
        if (it.current >= maxSafe) {
            return RecoveryInfo(cur = maxSafe, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = it.start)
        }
        val intervalMs = it.intervalMin.coerceAtLeast(1) * 60000L
        val elapsed = (now - it.start).coerceAtLeast(0L)
        val recovered = (elapsed / intervalMs).toInt()
        val cur = (it.current + recovered).coerceAtMost(maxSafe)
        if (cur >= maxSafe) {
            val fullAt = it.start + (maxSafe - it.current).coerceAtLeast(0) * intervalMs
            return RecoveryInfo(cur = maxSafe, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = Math.min(now, fullAt))
        }
        val nextIn = intervalMs - (elapsed % intervalMs)
        val need = maxSafe - cur
        val remainMs = (need - 1) * intervalMs + nextIn
        val fullAt = now + remainMs
        return RecoveryInfo(cur = cur, remainMs = remainMs, nextInMs = nextIn, isFull = false, fullAt = fullAt)
    }

    fun calculateStamInfo(it: ItemEntity, now: Long): StamInfo {
        val r = calculateRecoveryInfo(it, now)
        return StamInfo(cur = r.cur, remainMs = r.remainMs, isFull = r.isFull, fullAt = r.fullAt)
    }

    fun calculateOrbInfo(it: ItemEntity, now: Long): OrbInfo {
        val r = calculateRecoveryInfo(it, now)
        return OrbInfo(cur = r.cur, remainMs = r.remainMs, nextInMs = r.nextInMs, isFull = r.isFull, fullAt = r.fullAt)
    }

    fun calculateIdleInfo(it: ItemEntity, now: Long): IdleInfo {
        val durMs = it.durationMin.coerceAtLeast(1) * 60000L
        val elapsed = (now - it.start).coerceAtLeast(0L)
        val remainMs = (durMs - elapsed).coerceAtLeast(0L)
        return IdleInfo(elapsed = elapsed, remainMs = remainMs, isFull = remainMs <= 0, fullAt = it.start + durMs)
    }

    fun remainingAfterUse(cur: Int, chunk: Int, type: String): Int {
        val c = chunk.coerceAtLeast(1)
        if (type == "orb") {
            return (cur - c).coerceAtLeast(0)
        }
        return cur % c
    }

    // Phase preservation when interval or max is adjusted
    // This ensures "hidden progress" towards the next recovery point is not lost.
    fun preservePhaseStart(it: ItemEntity, now: Long): Long {
        val intervalMs = it.intervalMin.coerceAtLeast(1) * 60000L
        val phase = ((now - it.start) % intervalMs + intervalMs) % intervalMs
        return now - phase
    }

    private val cachedTimeZone = java.util.TimeZone.getDefault()

    fun formatHM(timestamp: Long): String {
        val offset = cachedTimeZone.getOffset(timestamp)
        val localMillis = timestamp + offset
        val totalMinutes = localMillis / 60000L
        val minuteOfDay = (totalMinutes % 1440L).toInt()
        val h = minuteOfDay / 60
        val m = minuteOfDay % 60
        val hStr = if (h < 10) "0$h" else "$h"
        val mStr = if (m < 10) "0$m" else "$m"
        return "$hStr:$mStr"
    }

    fun formatCountdown(ms: Long): String {
        val clampedMs = ms.coerceAtLeast(0L)
        val totalMin = Math.ceil(clampedMs / 60000.0).toLong()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m < 10) "$h:0$m" else "$h:$m"
    }

    fun formatElapsed(ms: Long): String {
        val clampedMs = ms.coerceAtLeast(0L)
        val totalMin = Math.floor(clampedMs / 60000.0).toLong()
        val h = totalMin / 60
        val m = totalMin % 60
        return if (m < 10) "$h:0$m" else "$h:$m"
    }

    /**
     * Re-indexes top-level items and grouped children so their `position` values
     * are strictly contiguous 0, 1, 2... within their respective parent scopes.
     * Prevents index gaps, out-of-order jumps, duplicate positions, and ensures
     * orphan items are never silently dropped.
     */
    fun normalizeItemPositions(items: List<ItemEntity>): List<ItemEntity> {
        if (items.isEmpty()) return emptyList()

        val topLevels = items.filter { it.parentId == null }.sortedBy { it.position }
        val childrenByParent = items.filter { it.parentId != null }.groupBy { it.parentId }

        val normalized = ArrayList<ItemEntity>(items.size)
        topLevels.forEachIndexed { topIdx, top ->
            val normTop = if (top.position != topIdx) top.copy(position = topIdx) else top
            normalized.add(normTop)
            val children = childrenByParent[top.id]?.sortedBy { it.position }
            if (children != null) {
                children.forEachIndexed { childIdx, child ->
                    val normChild = if (child.position != childIdx) child.copy(position = childIdx) else child
                    normalized.add(normChild)
                }
            }
        }

        // Safety fallback: if there are orphan children whose parent doesn't exist, preserve them
        if (normalized.size < items.size) {
            val includedIds = HashSet<String>(normalized.size).apply {
                for (it in normalized) add(it.id)
            }
            for (it in items) {
                if (!includedIds.contains(it.id)) {
                    normalized.add(it)
                }
            }
        }

        return normalized
    }
}
