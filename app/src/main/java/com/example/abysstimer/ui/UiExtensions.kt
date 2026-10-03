package com.example.abysstimer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Extension to trigger action instantly on pointerdown and consume event with zero coroutine recreation.
 * Single unified implementation shared across the entire app.
 */
@Composable
fun Modifier.pointerDownTap(
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier {
    val currentTap by rememberUpdatedState(onTap)
    return if (!enabled) this else this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Press && !event.changes.any { it.isConsumed }) {
                    event.changes.forEach { it.consume() }
                    currentTap()
                }
            }
        }
    }
}

/**
 * Low-level pointer tap: intercepts and consumes press immediately at Initial pass,
 * completely bypassing gesture disambiguation delays and preventing parent container interference.
 */
@Composable
fun Modifier.instantPointerTap(
    enabled: Boolean = true,
    onTap: () -> Unit
): Modifier {
    val currentTap by rememberUpdatedState(onTap)
    return if (!enabled) this else this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press && !event.changes.any { it.isConsumed }) {
                    event.changes.forEach { it.consume() }
                    currentTap()
                }
            }
        }
    }
}
