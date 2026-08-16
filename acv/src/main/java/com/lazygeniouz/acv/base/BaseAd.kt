package com.lazygeniouz.acv.base

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.RelativeLayout
import androidx.annotation.Keep
import androidx.core.graphics.drawable.toDrawable
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.lazygeniouz.acv.AdContainerView
import com.lazygeniouz.acv.R
/** Stores banner configuration, callbacks, and observable state for [AdView]. */
@Keep
open class BaseAd @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    internal val showOnConditionMessage =
        "Ad request skipped because showOnCondition returned false."
    internal val makeSureToHandleLifecycleMessage =
        "Automatic cleanup is unavailable; call destroyAd() when the host is destroyed."

    protected var autoLoad = false
    internal var isAdLoaded = false
    internal var isAdLoading = false
    internal var adSize: AdSize = getAdaptiveAdSize()
    internal var adUnitId = AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID

    internal var parentMayHaveAListView = false
    internal val transparent = Color.TRANSPARENT.toDrawable()

    protected var loadCallback: AdLoadCallback<BannerAd>? = null
    protected var eventCallback: BannerAdEventCallback? = null
    protected var refreshCallback: BannerAdRefreshCallback? = null
    protected var newAdView: AdView? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AdContainerView, 0, 0
        ).apply {
            try {
                val adSizeValue = getInt(R.styleable.AdContainerView_acv_adSize, 0)
                adUnitId = getString(R.styleable.AdContainerView_acv_adUnitId)
                    ?: getTestAdUnitId(adSizeValue)
                autoLoad = getBoolean(R.styleable.AdContainerView_acv_autoLoad, false)
                adSize = getAdSize(adSizeValue)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Receives the initial banner load result.
     * Callbacks registered through this view are delivered on the main thread.
     */
    fun setAdLoadCallback(callback: AdLoadCallback<BannerAd>) {
        loadCallback = callback
    }

    /**
     * Returns the attached load callback if set, null otherwise.
     */
    fun getAdLoadCallback(): AdLoadCallback<BannerAd>? = loadCallback

    /**
     * Receives click, impression, paid, and full-screen banner events.
     * Callbacks registered through this view are delivered on the main thread.
     */
    fun setAdEventCallback(callback: BannerAdEventCallback) {
        eventCallback = callback
    }

    /**
     * Returns the attached banner event callback if set, null otherwise.
     */
    fun getAdEventCallback(): BannerAdEventCallback? = eventCallback

    /**
     * Receives automatic banner refresh results.
     * Callbacks registered through this view are delivered on the main thread.
     */
    fun setAdRefreshCallback(callback: BannerAdRefreshCallback) {
        refreshCallback = callback
    }

    /**
     * Returns the attached banner refresh callback if set, null otherwise.
     */
    fun getAdRefreshCallback(): BannerAdRefreshCallback? = refreshCallback

    /**
     * Return whether the Ad is loaded or not
     */
    fun isAdLoaded(): Boolean = isAdLoaded

    /**
     * Return the Ad's Loading State.
     */
    fun isLoading(): Boolean = isAdLoading

    /**
     * Returns the Ad's visibility.
     */
    fun isVisible(): Boolean = newAdView?.visibility == VISIBLE

    /**
     * Returns AdView's current AdUnitId
     */
    fun getAdUnitId(): String = adUnitId

    /**
     * Returns AdView's current AdSize
     */
    fun getAdSize(): AdSize = adSize

    /**
     * Return autoLoad value
     */
    fun isAutoLoad(): Boolean = autoLoad

    // Get a default request for the configured ad unit and size.
    protected fun getAdRequest(adUnitId: String, adSize: AdSize): BannerAdRequest =
        BannerAdRequest.Builder(adUnitId, adSize).build()

    // Get a large anchored adaptive banner sized to the current display width.
    private fun getAdaptiveAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val adWidth = (displayMetrics.widthPixels / displayMetrics.density)
            .toInt()
            .coerceAtLeast(1)
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    private fun getAdSize(typedArrayValue: Int): AdSize {
        return when (typedArrayValue) {
            0, 1 -> getAdaptiveAdSize() // Includes legacy XML aliases.
            2 -> AdSize.BANNER
            3 -> AdSize.FULL_BANNER
            4 -> AdSize.LARGE_BANNER
            5 -> AdSize.LEADERBOARD
            6 -> AdSize.MEDIUM_RECTANGLE
            7 -> AdSize(160, 600)
            else -> throw IllegalArgumentException(
                "Currently Supported AdSizes are: " +
                        "LARGE_ADAPTIVE, " +
                        "BANNER, " +
                        "FULL_BANNER, " +
                        "LARGE_BANNER, " +
                        "LEADERBOARD, " +
                        "MEDIUM_RECTANGLE, " +
                        "WIDE_SKYSCRAPER"
            )
        }
    }

    private fun getTestAdUnitId(typedArrayValue: Int): String =
        if (typedArrayValue == 0 || typedArrayValue == 1) {
            AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID
        } else {
            AdContainerView.FIXED_SIZE_TEST_AD_ID
        }
}
