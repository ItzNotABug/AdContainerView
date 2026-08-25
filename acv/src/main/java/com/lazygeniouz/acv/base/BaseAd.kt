@file:Suppress("unused")

package com.lazygeniouz.acv.base

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import androidx.annotation.Keep
import androidx.annotation.MainThread
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

/**
 * Stores banner configuration, callbacks, and observable state for [AdView].
 * Public APIs and protected mutable state must be accessed from the main thread.
 *
 * @param context the view context.
 * @param attrs XML attributes for the view.
 * @param defStyleAttr the default style attribute.
 */
@Keep
open class BaseAd @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    internal val showOnConditionMessage =
        "Ad request skipped because showOnCondition returned false."
    internal val makeSureToHandleLifecycleMessage =
        "No LifecycleOwner found; call destroyAd() when the host is destroyed."

    /** Whether this view requests its configured banner during the host's `ON_CREATE` event. */
    protected var autoLoad = false
    internal var isAdLoaded = false
    internal var isAdLoading = false
    internal var adSize: AdSize = getAdaptiveAdSize()
    internal var adUnitId = AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID

    internal var parentMayHaveAListView = false
    internal val transparent = Color.TRANSPARENT.toDrawable()

    /** The current load callback, replaced by [setAdLoadCallback]. */
    protected var loadCallback: AdLoadCallback<BannerAd>? = null

    /** The current banner event callback, replaced by [setAdEventCallback]. */
    protected var eventCallback: BannerAdEventCallback? = null

    /** The current refresh callback, replaced by [setAdRefreshCallback]. */
    protected var refreshCallback: BannerAdRefreshCallback? = null

    /** The current SDK view, or null before loading and after destruction. */
    protected var newAdView: AdView? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs, R.styleable.AdContainerView, defStyleAttr, 0
        ).apply {
            try {
                val adSizeValue = getInt(
                    R.styleable.AdContainerView_acv_adSize,
                    XML_AD_SIZE_LARGE_ADAPTIVE
                )
                adUnitId = getString(R.styleable.AdContainerView_acv_adUnitId)
                    ?: resolveTestAdUnitId(adSizeValue)
                autoLoad = getBoolean(R.styleable.AdContainerView_acv_autoLoad, false)
                adSize = resolveAdSize(adSizeValue)
            } finally {
                recycle()
            }
        }
    }

    /**
     * Replaces the callback that receives the initial banner load result.
     * Callbacks registered through this view are delivered on the main thread.
     *
     * @param callback the load callback to receive, or null to clear it.
     */
    @MainThread
    fun setAdLoadCallback(callback: AdLoadCallback<BannerAd>?) {
        loadCallback = callback
    }

    /**
     * Returns the attached load callback if set, null otherwise.
     */
    @MainThread
    fun getAdLoadCallback(): AdLoadCallback<BannerAd>? = loadCallback

    /**
     * Replaces the callback that receives click, impression, paid, app, and full-screen banner
     * events.
     * Callbacks registered through this view are delivered on the main thread.
     *
     * @param callback the event callback to receive, or null to clear it.
     */
    @MainThread
    fun setAdEventCallback(callback: BannerAdEventCallback?) {
        eventCallback = callback
    }

    /**
     * Returns the attached banner event callback if set, null otherwise.
     */
    @MainThread
    fun getAdEventCallback(): BannerAdEventCallback? = eventCallback

    /**
     * Replaces the callback that receives automatic banner refresh results.
     * Callbacks registered through this view are delivered on the main thread.
     *
     * @param callback the refresh callback to receive, or null to clear it.
     */
    @MainThread
    fun setAdRefreshCallback(callback: BannerAdRefreshCallback?) {
        refreshCallback = callback
    }

    /**
     * Returns the attached banner refresh callback if set, null otherwise.
     */
    @MainThread
    fun getAdRefreshCallback(): BannerAdRefreshCallback? = refreshCallback

    /** Returns whether the current banner completed its initial load successfully. */
    @MainThread
    fun isAdLoaded(): Boolean = isAdLoaded

    /** Returns whether an initial banner request is in progress. */
    @MainThread
    fun isLoading(): Boolean = isAdLoading

    /** Returns whether the current SDK view has [VISIBLE] visibility. */
    @MainThread
    fun isVisible(): Boolean = newAdView?.visibility == VISIBLE

    /** Returns the configured or most recently requested banner ad unit ID. */
    @MainThread
    fun getAdUnitId(): String = adUnitId

    /** Returns the configured or most recently requested banner size. */
    @MainThread
    fun getAdSize(): AdSize = adSize

    /** Returns whether automatic loading is enabled. */
    @MainThread
    fun isAutoLoad(): Boolean = autoLoad

    /**
     * Builds a request for the supplied ad unit and size.
     *
     * @param adUnitId the banner ad unit ID.
     * @param adSize the requested banner size.
     */
    protected fun getAdRequest(adUnitId: String, adSize: AdSize): BannerAdRequest =
        BannerAdRequest.Builder(adUnitId, adSize).build()

    internal fun resolveConfiguredAdSize(): AdSize = if (adSize.isLargeAnchoredAdaptiveBanner) {
        getAdaptiveAdSize()
    } else {
        adSize
    }

    // Prefer the measured content width, then the parent content width, then the display.
    private fun getAdaptiveAdSize(): AdSize {
        val displayMetrics = resources.displayMetrics
        val horizontalPadding = paddingLeft + paddingRight
        val parentContentWidth = (parent as? View)?.run {
            width - paddingLeft - paddingRight - horizontalPadding
        }?.takeIf { it > 0 }
        val contentWidth = (width - horizontalPadding)
            .takeIf { it > 0 }
            ?: parentContentWidth
            ?: (displayMetrics.widthPixels - horizontalPadding).coerceAtLeast(1)
        val adWidth = (contentWidth / displayMetrics.density)
            .toInt()
            .coerceAtLeast(1)
        return AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    private fun resolveAdSize(value: Int): AdSize = when (value) {
        XML_AD_SIZE_LARGE_ADAPTIVE,
        XML_AD_SIZE_SMART_BANNER -> getAdaptiveAdSize()

        XML_AD_SIZE_BANNER -> AdSize.BANNER
        XML_AD_SIZE_FULL_BANNER -> AdSize.FULL_BANNER
        XML_AD_SIZE_LARGE_BANNER -> AdSize.LARGE_BANNER
        XML_AD_SIZE_LEADERBOARD -> AdSize.LEADERBOARD
        XML_AD_SIZE_MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        else -> getAdaptiveAdSize()
    }

    private fun resolveTestAdUnitId(value: Int): String =
        if (isFixedAdSize(value)) {
            AdContainerView.FIXED_SIZE_TEST_AD_ID
        } else {
            AdContainerView.ADAPTIVE_SIZE_TEST_AD_ID
        }

    private fun isFixedAdSize(value: Int): Boolean = when (value) {
        XML_AD_SIZE_BANNER,
        XML_AD_SIZE_FULL_BANNER,
        XML_AD_SIZE_LARGE_BANNER,
        XML_AD_SIZE_LEADERBOARD,
        XML_AD_SIZE_MEDIUM_RECTANGLE -> true

        else -> false
    }

    private companion object {
        // Keep these values aligned with attrs.xml.
        const val XML_AD_SIZE_LARGE_ADAPTIVE = 0
        const val XML_AD_SIZE_SMART_BANNER = 1
        const val XML_AD_SIZE_BANNER = 2
        const val XML_AD_SIZE_FULL_BANNER = 3
        const val XML_AD_SIZE_LARGE_BANNER = 4
        const val XML_AD_SIZE_LEADERBOARD = 5
        const val XML_AD_SIZE_MEDIUM_RECTANGLE = 6
    }
}