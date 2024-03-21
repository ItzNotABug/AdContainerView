package com.lazygeniouz.acv.example

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

/**
 * Let's initialize MobileAds in our Application
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        markTestDevice()
        initializeAdsSdk()
    }

    private fun markTestDevice() {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                arrayListOf("A1310A728BE015FCB189C307B880CBA8")
            ).build()
        )
    }

    private fun initializeAdsSdk() {
        MobileAds.initialize(this)
    }
}