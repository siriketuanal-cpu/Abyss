package com.example.abysstimer.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Extension to trigger action on pointerdown when the event is unconsumed by children.
 * Used for background preview dismissal without blocking child interactions.
 */
fun Modifier.pointerDownTap(
    enabled: Boolean = true,
    pass: PointerEventPass = PointerEventPass.Main,
    onTap: () -> Unit
): Modifier = composed {
    val currentTap by rememberUpdatedState(onTap)
    val currentEnabled by rememberUpdatedState(enabled)
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(pass)
                if (currentEnabled && event.type == PointerEventType.Press) {
                    val isHandled = event.changes.any { it.isConsumed }
                    if (!isHandled) {
                        event.changes.forEach { it.consume() }
                        currentTap()
                    }
                }
            }
        }
    }
}

/**
 * High-performance tap + long press gesture detector.
 * Uses awaitPointerEventScope to trigger onTap at exactly 0ms upon finger lift (PointerUp),
 * completely bypassing Compose's detectTapGestures overhead/delays.
 * If held past viewConfiguration.longPressTimeoutMillis (400ms), triggers onLongPress immediately.
 */
fun Modifier.snappyTapOrLongPress(
    enabled: Boolean = true,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
): Modifier = composed {
    val currentTap by rememberUpdatedState(onTap)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentEnabled by rememberUpdatedState(enabled)

    if (!currentEnabled) return@composed this
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Press && event.changes.none { it.isConsumed }) {
                    // Consume the initial press to claim the gesture and prevent background dismissal interference
                    event.changes.forEach { it.consume() }
                    
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
                    try {
                        val upOrCancel = withTimeout(longPressTimeout) {
                            waitForUpOrCancellation()
                        }
                        if (upOrCancel != null) {
                            upOrCancel.consume()
                            currentTap?.invoke()
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        // 400ms elapsed while held down: trigger long press immediately
                        currentLongPress?.invoke()
                        // Consume remaining events until all fingers are lifted
                        while (true) {
                            val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sequential PointerDown 0ms preview dismissal + exact system 400ms long-press menu modifier.
 * - PointerDown (0ms): If preview is active (isPendingOrEditing), instantly dismisses preview!
 * - Press & Hold (400ms exact timer): Opens edit toast menu while finger is still down.
 * - Short Tap: Executes optional onTap callback on release if released before 400ms.
 */
fun Modifier.instantDismissOrLongPress(
    enabled: Boolean = true,
    isPendingOrEditing: Boolean = false,
    onDismissOutside: () -> Unit = {},
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
): Modifier = composed {
    val currentEnabled by rememberUpdatedState(enabled)
    val currentIsPending by rememberUpdatedState(isPendingOrEditing)
    val currentDismiss by rememberUpdatedState(onDismissOutside)
    val currentTap by rememberUpdatedState(onTap)
    val currentLongPress by rememberUpdatedState(onLongPress)

    if (!currentEnabled) return@composed this
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                if (event.type == PointerEventType.Press && event.changes.none { it.isConsumed }) {
                    val wasPending = currentIsPending
                    if (wasPending) {
                        event.changes.forEach { it.consume() }
                        currentDismiss()
                        while (true) {
                            val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.changes.all { !it.pressed }) break
                        }
                        continue
                    }

                    // Not pending: consume the press to claim the gesture and prevent background dismissal
                    event.changes.forEach { it.consume() }
                    val longPressTimeout = viewConfiguration.longPressTimeoutMillis

                    try {
                        val upOrCancel = withTimeout(longPressTimeout) {
                            waitForUpOrCancellation()
                        }
                        if (upOrCancel != null) {
                            upOrCancel.consume()
                            currentTap?.invoke()
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        // System long-press timer expired at exactly 400ms while finger is still held down!
                        currentLongPress?.invoke()
                        // Consume all remaining pointer events until release
                        while (true) {
                            val nextEvent = awaitPointerEvent(PointerEventPass.Main)
                            nextEvent.changes.forEach { it.consume() }
                            if (nextEvent.changes.all { !it.pressed }) break
                        }
                    }
                }
            }
        }
    }
}


