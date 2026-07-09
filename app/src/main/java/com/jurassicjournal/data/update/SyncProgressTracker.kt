package com.sufficienteffort.jurassicjournal.data.update

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncPhase { IDLE, DB_DOWNLOAD, IMAGE_SYNC, ABILITY_SYNC }

data class SyncProgress(
    val phase: SyncPhase = SyncPhase.IDLE,
    val current: Int = 0,
    val total: Int = 0,
    val speedBytesPerSec: Long = 0L,
) {
    val isActive: Boolean get() = phase != SyncPhase.IDLE
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
}

@Singleton
class SyncProgressTracker @Inject constructor() {

    private val mutex = Mutex()
    private val _progress = MutableStateFlow(SyncProgress())
    val progress: StateFlow<SyncProgress> = _progress.asStateFlow()

    // Rolling 15-second window: (timestampMs, deltaBytes)
    private val speedWindow = ArrayDeque<Pair<Long, Long>>()
    private val WINDOW_MS = 15_000L

    suspend fun beginPhase(phase: SyncPhase, total: Int) = mutex.withLock {
        speedWindow.clear()
        _progress.value = SyncProgress(phase = phase, current = 0, total = total)
    }

    /** Advance the counter by [filesDelta] and record [bytes] toward speed. */
    suspend fun advance(filesDelta: Int = 0, bytes: Long = 0L) = mutex.withLock {
        if (bytes > 0L) {
            val now = System.currentTimeMillis()
            speedWindow.addLast(now to bytes)
            while (speedWindow.isNotEmpty() && now - speedWindow.first().first > WINDOW_MS) {
                speedWindow.removeFirst()
            }
        }
        val cur = _progress.value
        _progress.value = cur.copy(
            current = cur.current + filesDelta,
            speedBytesPerSec = computeSpeed(),
        )
    }

    suspend fun finish() = mutex.withLock {
        speedWindow.clear()
        _progress.value = SyncProgress()
    }

    private fun computeSpeed(): Long {
        if (speedWindow.size < 2) return 0L
        val now = System.currentTimeMillis()
        val elapsedMs = maxOf(now - speedWindow.first().first, 1000L)
        val totalBytes = speedWindow.sumOf { it.second }
        return totalBytes * 1000L / elapsedMs
    }
}
