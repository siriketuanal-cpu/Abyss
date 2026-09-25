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
data class IdleInfo(val elapsed: Long, val remainMs: Long, val isFull: Boolean, val fullAt: Long = 0)

/**
 * Pure calculation engine corresponding directly to WEB version engine.js.
 * Contains purely mathematical, zero-side-effect, thread-safe calculations.
 */
object TimerEngine {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun calculateStamInfo(it: ItemEntity, now: Long): StamInfo {
        val maxSafe = it.max.coerceAtLeast(1)
        if (it.current >= maxSafe) {
            return StamInfo(cur = maxSafe, remainMs = 0L, isFull = true, fullAt = it.start)
        }
        val intervalMs = it.intervalMin.coerceAtLeast(1) * 60000L
        val elapsed = (now - it.start).coerceAtLeast(0L)
        val recovered = (elapsed / intervalMs).toInt()
        val cur = (it.current + recovered).coerceAtMost(maxSafe)
        if (cur >= maxSafe) {
            val fullAt = it.start + (maxSafe - it.current).coerceAtLeast(0) * intervalMs
            return StamInfo(cur = maxSafe, remainMs = 0L, isFull = true, fullAt = Math.min(now, fullAt))
        }
        val nextIn = intervalMs - (elapsed % intervalMs)
        val need = maxSafe - cur
        val remainMs = (need - 1) * intervalMs + nextIn
        return StamInfo(cur = cur, remainMs = remainMs, isFull = false)
    }

    fun calculateOrbInfo(it: ItemEntity, now: Long): OrbInfo {
        val maxSafe = it.max.coerceAtLeast(1)
        if (it.current >= maxSafe) {
            return OrbInfo(cur = maxSafe, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = it.start)
        }
        val intervalMs = it.intervalMin.coerceAtLeast(1) * 60000L
        val elapsed = (now - it.start).coerceAtLeast(0L)
        val recovered = (elapsed / intervalMs).toInt()
        val cur = (it.current + recovered).coerceAtMost(maxSafe)
        if (cur >= maxSafe) {
            val fullAt = it.start + (maxSafe - it.current).coerceAtLeast(0) * intervalMs
            return OrbInfo(cur = maxSafe, remainMs = 0L, nextInMs = 0L, isFull = true, fullAt = Math.min(now, fullAt))
        }
        val nextInMs = intervalMs - (elapsed % intervalMs)
        val need = maxSafe - cur
        val remainMs = (need - 1) * intervalMs + nextInMs
        return OrbInfo(cur = cur, remainMs = remainMs, nextInMs = nextInMs, isFull = false, fullAt = now + remainMs)
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

    fun formatHM(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
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
}
