@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.lazygeniouz.acv

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.RelativeLayout
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.*


/**
 * A RelativeLayout Container to Handle
 * @see com.google.android.gms.ads.AdView
 *
 * Handles and calls AdView's
 * respective lifecycle methods.
 *
 */
class AdContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val tag = javaClass.simpleName

    private var adUnitId = ""
    private var adSize = AdSize.SMART_BANNER
    private var autoLoad = false
    private var listener: AdListener? = null

    private var newAdView: AdView? = null

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AdContainerView,
            0,
            0
        ).apply {
            try {
                adUnitId = getString(R.styleable.AdContainerView_acv_adUnitId)
                    ?: ""
                autoLoad = getBoolean(R.styleable.AdContainerView_acv_autoLoad, false)
                adSize = getAdSize(getInt(R.styleable.AdContainerView_acv_adSize, 0))
            } finally {
                recycle()
            }
        }

        // It `should` be a FragmentActivity Instance!
        // For Fragment, afaik, the Host Activity's instance will be used by default.
        if (context is FragmentActivity) context.lifecycle.addObserver(HostActivityObserver())
        else throw IllegalArgumentException("The supplied Context is not an instance of FragmentActivity")
        layoutTransition = LayoutTransition()
    }

    /**
     * @insertAdView = Loads and Adds the `AdView` in the View
     *
     * @param adUnitId = The AdUnitId of your banner ad, default is test adUnitId
     * @param adSize = The AdSize of the Banner Ad
     * @param adRequest = Optional AdRequest if you have customized request.
     *
     **/
    fun insertAdView(
        @NonNull adUnitId: String = this.adUnitId,
        adSize: AdSize = this.adSize,
        adRequest: AdRequest = this.getAdRequest()
    ) {
        if (adUnitId.isEmpty()) throw IllegalArgumentException("The adUnitId cannot be blank or null")
        removeAllViews()
        newAdView = AdView(context)
        newAdView!!.visibility = View.GONE
        newAdView!!.background = ColorDrawable(Color.TRANSPARENT)
        newAdView!!.adUnitId = adUnitId
        newAdView!!.adSize = adSize
        newAdView!!.adListener = object : AdListener() {
            override fun onAdClicked() {
                listener?.onAdClicked()
            }

            override fun onAdImpression() {
                listener?.onAdImpression()
            }

            override fun onAdClosed() {
                listener?.onAdClosed()
            }

            override fun onAdLeftApplication() {
                listener?.onAdLeftApplication()
            }

            override fun onAdOpened() {
                listener?.onAdOpened()
            }

            override fun onAdLoaded() {
                newAdView!!.visibility = View.VISIBLE
                listener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                listener?.onAdFailedToLoad(error)

                // Currently removing below coz
                // we don't want to hide the Ad if a 2nd refresh was failed
                //newAdView!!.visibility = View.GONE
            }
        }

        removeAllViews()
        addView(newAdView)
        newAdView!!.layoutParams.apply { gravity = Gravity.CENTER }
        newAdView!!.loadAd(adRequest)
    }

    fun isLoading() = newAdView?.isLoading ?: false

    fun isVisible() = newAdView?.visibility == View.VISIBLE

    fun getAdUnitId() = adUnitId

    fun getAdSize(): AdSize = adSize

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


    //Make sure to call `insertAdView` to load the AdView again
    fun removeAd() = destroyAd()

    /** @see AdView.resume */
    fun resumeAd() = newAdView?.resume()

    /** @see AdView.pause */
    fun pauseAd() = newAdView?.pause()

    /**
     * @see AdView.destroy
     *
     * Avoiding this issue: `#004 The webview is destroyed. Ignoring action.`
     * by using `newAdView = null` or `removeAllViews()`
     * It is found that the above info. is printed in 2 scenarios.
     *
     * 1. when the Ad refreshes,
     * 2. when the Activity is destroyed.
     **/
    fun destroyAd() {
        newAdView?.destroy()
        newAdView = null
        removeAllViews()
    }

    // Get a simple & default AdRequest
    private fun getAdRequest() = AdRequest.Builder().build()

    /**
     * We try to get the Adaptive AdSize
     * @see AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize
     * (https://developers.google.com/admob/android/banner/adaptive)
     *
     * However,
     * due to any reason (say, context isn't a FragmentActivity or anything else),
     * return AdSize.SMART_BANNER
     * @see AdSize.SMART_BANNER
     * (https://developers.google.com/admob/android/banner/smart)
     */
    private fun getAdaptiveAdSize(): AdSize {
        return if (context is FragmentActivity) {
            val display = (context as FragmentActivity).windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()

            display.getMetrics(outMetrics)
            val density = outMetrics.density
            val adWidthPixels = outMetrics.widthPixels.toFloat()

            val adWidth = (adWidthPixels / density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
            Log.d(tag, "AdSize: Adaptive Banner")
            return adSize
        } else AdSize.SMART_BANNER
    }

    /**
     * Not all AdSize work properly as some are considered as Legacy AdSizes.
     * @see AdSize.FLUID
     * @see AdSize.FULL_BANNER
     * @see AdSize.LEADERBOARD
     * @see AdSize.WIDE_SKYSCRAPER
     *
     * The so called 'Legacy AdSizes' are not
     * marked as @deprecated but most of the times do not return an Ad.
     *
     * It is highly recommended to use other active formats
     * which won't affect your revenue because of low fill rate.
     */
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

    override fun onDetachedFromWindow() {
        destroyAd()
        super.onDetachedFromWindow()
    }

    private inner class HostActivityObserver : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onCreate() {
            if (autoLoad) {
                insertAdView(adUnitId, adSize)
                Log.d(tag, "onCreate()")
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun pause() {
            Log.d(tag, "pauseAd()")
            pauseAd()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() {
            Log.d(tag, "destroyAd()")
            destroyAd()
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun resume() {
            Log.d(tag, "resumeAd()")
            resumeAd()
        }
    }
}