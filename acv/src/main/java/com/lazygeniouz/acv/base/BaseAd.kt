@file:Suppress("unused", "DEPRECATION")

package com.lazygeniouz.acv.base

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.lazygeniouz.acv.AdContainerView
import com.lazygeniouz.acv.R


/**
 * A Base Container class to Handle [AdView]
 * @see com.google.android.gms.ads.AdView
 */

@Keep
open class BaseAd @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    internal val showOnConditionMessage =
        "showOnCondition lambda returned false, AdContainerView will not load an Ad."
    internal val makeSureToHandleLifecycleMessage =
        "Make sure to call the resume, pause, destroy lifecycle methods."

    protected var autoLoad = false
    internal var isAdLoaded = false
    internal var adSize: AdSize = AdSize.SMART_BANNER
    internal var adUnitId = AdContainerView.TEST_AD_ID

    internal var parentMayHaveAListView: Boolean = false
    internal val transparent = ColorDrawable(Color.TRANSPARENT)

    protected var listener: AdListener? = null
    protected var newAdView: AdView? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AdContainerView, 0, 0
        ).apply {
            try {
                adUnitId = getString(R.styleable.AdContainerView_acv_adUnitId)
                    ?: AdContainerView.TEST_AD_ID
                autoLoad = getBoolean(R.styleable.AdContainerView_acv_autoLoad, false)
                adSize = getAdSize(getInt(R.styleable.AdContainerView_acv_adSize, 0))
            } finally {
                recycle()
            }
        }

        layoutTransition = LayoutTransition()
    }

    /**
     * Listener for Banner Ads
     * There is no need to create another class, add methods & bloat.
     * AdView's own AdListener is fine.
     *
     * @param listener
     * @see AdView.setAdListener
     */
    fun setAdListener(listener: AdListener) {
        this.listener = listener
    }

    /**
     * Return whether the Ad is loaded or not
     */
    fun isAdLoaded() = isAdLoaded

    /**
     * Return the Ad's Loading State.
     */
    fun isLoading() = newAdView?.isLoading ?: false

    /**
     * Returns the Ad's visibility.
     */
    fun isVisible() = newAdView?.visibility == View.VISIBLE

    /**
     * Returns AdView's current AdUnitId
     */
    fun getAdUnitId() = adUnitId

    /**
     * Returns AdView's current AdSize
     */
    fun getAdSize(): AdSize = adSize

    /**
     * Return autoLoad value
     */
    fun isAutoLoad() = autoLoad

    // Get a simple & default AdRequest
    protected fun getAdRequest(): AdRequest = AdRequest.Builder().build()

    // Get the Adaptive AdSize, if possible, AdSize.SMART_BANNER otherwise
    private fun getAdaptiveAdSize(): AdSize {
        return if (context is FragmentActivity) {
            val outMetrics = DisplayMetrics()
            val display =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
                else (context as FragmentActivity).windowManager.defaultDisplay

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display?.getRealMetrics(outMetrics)
            else display?.getMetrics(outMetrics)
            val density = outMetrics.density
            val adWidthPixels = outMetrics.widthPixels.toFloat()

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        } else AdSize.SMART_BANNER
    }

    private fun getAdSize(typedArrayValue: Int): AdSize {
        return when (typedArrayValue) {
            0 -> getAdaptiveAdSize()
            1 -> AdSize.SMART_BANNER
            2 -> AdSize.BANNER
            3 -> AdSize.FULL_BANNER
            4 -> AdSize.LARGE_BANNER
            5 -> AdSize.LEADERBOARD
            6 -> AdSize.MEDIUM_RECTANGLE
            7 -> AdSize.WIDE_SKYSCRAPER
            else -> throw IllegalArgumentException(
                "Currently Supported AdSizes are: " +
                        "ADAPTIVE, " +
                        "SMART_BANNER, " +
                        "BANNER, " +
                        "FULL_BANNER, " +
                        "LARGE_BANNER, " +
                        "LEADERBOARD, " +
                        "MEDIUM_RECTANGLE, " +
                        "WIDE_SKYSCRAPER"
            )
        }
    }
}