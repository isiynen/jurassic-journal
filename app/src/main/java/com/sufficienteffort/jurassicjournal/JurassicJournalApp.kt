package com.sufficienteffort.jurassicjournal

import android.app.Application
import android.util.Log
import coil3.EventListener
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.sufficienteffort.jurassicjournal.data.update.BundledAbilityIcons
import com.sufficienteffort.jurassicjournal.data.update.BundledDinoImages
import com.sufficienteffort.jurassicjournal.data.update.GameDataUpdater
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JurassicJournalApp : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        // Must run before any Hilt singleton opens the game DB.
        GameDataUpdater.prepareGameDatabase(this)
        BundledDinoImages.init(this)
        BundledAbilityIcons.init(this)
    }

    /**
     * Logs image decode/fetch failures so intermittent load glitches (e.g. a dino
     * portrait momentarily failing to render) leave a diagnosable trail instead of
     * failing silently.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .eventListener(object : EventListener() {
                override fun onError(request: ImageRequest, result: ErrorResult) {
                    Log.w(TAG, "Image load failed for ${request.data}: ${result.throwable}")
                }
            })
            .build()

    private companion object {
        const val TAG = "JurassicJournalApp"
    }
}
