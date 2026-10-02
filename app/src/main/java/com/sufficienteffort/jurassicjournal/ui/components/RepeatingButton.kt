package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Button that fires [onClick] once per tap and repeats while held.
 *
 * Gesture-cancellation-safe: the initial click only fires on a genuine tap
 * release ([waitForUpOrCancellation] returning non-null). Earlier per-screen
 * copies fired on [awaitFirstDown], so a press that turned into a vertical
 * scroll still incremented the value.
 */
@Composable
fun RepeatingButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier.pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                if (!currentEnabled) continue
                var repeatStarted = false
                val job = scope.launch {
                    delay(400L)
                    repeatStarted = true
                    while (currentEnabled) {
                        currentOnClick()
                        delay(80L)
                    }
                }
                val upChange = awaitPointerEventScope { waitForUpOrCancellation() }
                job.cancel()
                if (!repeatStarted && upChange != null) currentOnClick()
            }
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}
