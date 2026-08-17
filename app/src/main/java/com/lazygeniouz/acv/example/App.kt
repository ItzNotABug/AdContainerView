package com.lazygeniouz.acv.example

import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.material.color.DynamicColors
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Initializes the sample's theme and the Google Mobile Ads SDK once per process. */
class App : Application() {

    internal val adsInitialization: CompletableFuture<Unit> = CompletableFuture()

    override fun onCreate() {
        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        initializeAdsSdkAsync()
    }

    private fun initializeAdsSdkAsync() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val initializationConfig = InitializationConfig.Builder(SAMPLE_APP_ID)
                    .build()

                MobileAds.initialize(applicationContext, initializationConfig)
            }.onSuccess {
                adsInitialization.complete(Unit)
            }.onFailure(adsInitialization::completeExceptionally)
        }
    }

    private companion object {
        const val SAMPLE_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    }
}