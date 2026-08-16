package com.lazygeniouz.acv.example

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.example.databinding.MainBinding

/** Demonstrates a lifecycle-aware, large adaptive banner integration. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = MainBinding.inflate(layoutInflater)
        applySystemBarInsets()
        setContentView(binding.root)

        binding.adContainerView.setAdLoadCallback(object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                renderStatus(
                    R.string.ad_status_loaded_title,
                    getString(R.string.ad_status_loaded_detail),
                    loading = false
                )
                binding.reloadButton.isEnabled = true
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                renderStatus(
                    R.string.ad_status_load_failed_title,
                    errorDetail(adError.message),
                    loading = false
                )
                binding.reloadButton.isEnabled = true
            }
        })

        binding.reloadButton.setOnClickListener { loadBanner() }
        renderStatus(
            R.string.ad_status_initializing_title,
            getString(R.string.ad_status_initializing_detail),
            loading = true
        )

        (application as App).adsInitialization.whenComplete { _, error ->
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    if (error == null) {
                        loadBanner()
                    } else {
                        renderStatus(
                            R.string.ad_status_initialization_failed_title,
                            errorDetail(error.cause?.message ?: error.message),
                            loading = false
                        )
                    }
                }
            }
        }
    }

    private fun loadBanner() {
        renderStatus(
            R.string.ad_status_loading_title,
            getString(R.string.ad_status_loading_detail),
            loading = true
        )
        binding.reloadButton.isEnabled = false
        binding.adContainerView.loadAdView()
    }

    private fun renderStatus(
        @StringRes title: Int,
        detail: String,
        loading: Boolean
    ) {
        binding.adStatusTitle.setText(title)
        binding.adStatusDetail.text = detail
        binding.adProgress.isVisible = loading
    }

    private fun errorDetail(message: String?): String =
        message?.takeIf { it.isNotBlank() }
            ?: getString(R.string.ad_status_unknown_error)

    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            windowInsets
        }
    }
}
