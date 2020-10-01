@file:Suppress("unused")

package com.lazygeniouz.acv.example

import android.app.Application
import com.google.android.gms.ads.MobileAds

/**
 * Let's initialize MobileAds in our Application
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this)
    }

}