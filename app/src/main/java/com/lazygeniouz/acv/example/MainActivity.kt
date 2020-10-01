package com.lazygeniouz.acv.example

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import kotlinx.android.synthetic.main.main.*

@SuppressLint("SetTextI18n")
/**
 * Example App to demonstrate AdContainerView library
 */
class MainActivity : AppCompatActivity(R.layout.main) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adContainerView.setAdListener(object : AdListener() {
            override fun onAdLoaded() {
                ad_state.text = "Ad State : Loaded"
            }

            override fun onAdFailedToLoad(error: LoadAdError?) {
                ad_state.text = "Ad State : Error, \nInfo: ${error?.responseInfo}"
            }
        })
    }
}