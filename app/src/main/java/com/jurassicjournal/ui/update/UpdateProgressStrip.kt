package com.jurassicjournal.ui.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jurassicjournal.data.update.SyncPhase
import com.jurassicjournal.data.update.SyncProgress

@Composable
fun UpdateProgressStrip(
    progress: SyncProgress,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = progress.isActive,
        enter = slideInVertically { it },
        exit  = slideOutVertically { it },
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = progress.phaseLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = progress.speedLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (progress.phase == SyncPhase.DB_DOWNLOAD) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(
                        progress = { progress.fraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val SyncProgress.phaseLabel: String
    get() = when (phase) {
        SyncPhase.IDLE         -> ""
        SyncPhase.DB_DOWNLOAD  -> "Downloading database…"
        SyncPhase.IMAGE_SYNC   -> "Syncing images  $current / $total files"
        SyncPhase.ABILITY_SYNC -> "Syncing ability icons  $current / $total files"
    }

private val SyncProgress.speedLabel: String
    get() {
        if (speedBytesPerSec <= 0L) return ""
        return if (speedBytesPerSec >= 1_000_000L) {
            "${"%.1f".format(speedBytesPerSec / 1_000_000.0)} MB/s"
        } else {
            "${speedBytesPerSec / 1_000L} KB/s"
        }
    }
