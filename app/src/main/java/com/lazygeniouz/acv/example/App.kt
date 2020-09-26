package com.lazygeniouz.acv.example

import android.app.Application
import com.google.android.gms.ads.MobileAds

@Suppress("unused")
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this)
    }

}