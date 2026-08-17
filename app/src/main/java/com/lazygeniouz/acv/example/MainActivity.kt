package com.lazygeniouz.acv.example

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.AdContainerView
import com.lazygeniouz.acv.example.databinding.MainBinding

/** Demonstrates lifecycle-aware adaptive and fixed banner integrations. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding
    private var selectedSize = BannerSize.LARGE_ADAPTIVE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = MainBinding.inflate(layoutInflater)
        applySystemBarInsets()
        setContentView(binding.root)
        setupSizeSelector()

        binding.adContainerView.setAdLoadCallback(object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                renderStatus(
                    R.string.ad_status_loaded_title,
                    getString(
                        R.string.ad_status_loaded_detail,
                        getString(selectedSize.label)
                    ),
                    loading = false
                )
                setControlsEnabled(true)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                renderStatus(
                    R.string.ad_status_load_failed_title,
                    errorDetail(adError.message),
                    loading = false
                )
                setControlsEnabled(true)
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

    private fun setupSizeSelector() {
        val availableSizes = BannerSize.entries.filter {
            it.minimumWidthDp <= resources.configuration.screenWidthDp
        }
        val labels = availableSizes.map { getString(it.label) }
        binding.adSizeSelector.setSimpleItems(labels.toTypedArray())
        binding.adSizeSelector.setText(labels.first(), false)
        binding.adSizeSelector.setOnItemClickListener { _, _, position, _ ->
            selectedSize = availableSizes[position]
            loadBanner()
        }
    }

    private fun loadBanner() {
        binding.adContainerView.doOnLayout { adContainer ->
            val sizeLabel = getString(selectedSize.label)
            renderStatus(
                R.string.ad_status_loading_title,
                getString(R.string.ad_status_loading_detail, sizeLabel),
                loading = true
            )
            setControlsEnabled(false)

            val testAdUnitId = if (selectedSize == BannerSize.LARGE_ADAPTIVE) {
                AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID
            } else {
                AdContainerView.FIXED_SIZE_TEST_AD_ID
            }
            binding.adContainerView.loadAdView(
                adUnitId = testAdUnitId,
                adSize = resolveAdSize(adContainer.width)
            )
        }
    }

    private fun resolveAdSize(containerWidth: Int): AdSize = when (selectedSize) {
        BannerSize.LARGE_ADAPTIVE -> {
            val widthDp = (containerWidth / resources.displayMetrics.density)
                .toInt()
                .coerceAtLeast(1)
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(this, widthDp)
        }
        BannerSize.BANNER -> AdSize.BANNER
        BannerSize.LARGE_BANNER -> AdSize.LARGE_BANNER
        BannerSize.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        BannerSize.FULL_BANNER -> AdSize.FULL_BANNER
        BannerSize.LEADERBOARD -> AdSize.LEADERBOARD
    }

    private fun setControlsEnabled(enabled: Boolean) {
        binding.adSizeField.isEnabled = enabled
        binding.reloadButton.isEnabled = enabled
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

    private enum class BannerSize(
        @param:StringRes val label: Int,
        val minimumWidthDp: Int
    ) {
        LARGE_ADAPTIVE(R.string.banner_size_large_adaptive, 0),
        BANNER(R.string.banner_size_banner, 320),
        LARGE_BANNER(R.string.banner_size_large_banner, 320),
        MEDIUM_RECTANGLE(R.string.banner_size_medium_rectangle, 300),
        FULL_BANNER(R.string.banner_size_full_banner, 468),
        LEADERBOARD(R.string.banner_size_leaderboard, 728)
    }
}
