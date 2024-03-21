package com.lazygeniouz.acv.example

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import com.lazygeniouz.acv.AdContainerView

/**
 * Example App to demonstrate AdContainerView library.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val adState: TextView = findViewById(R.id.ad_state)

        (findViewById<AdContainerView>(R.id.adContainerView)).apply {
            setAdListener(object : AdListener() {
                override fun onAdLoaded() {
                    adState.text = String.format("Ad State : Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    adState.text = String.format("Ad State : Error, \nInfo: ${error.message}")
                }
            })

            loadAdView()
        }
    }
}