package com.lazygeniouz.acv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.annotation.Keep
import androidx.annotation.NonNull
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.*
import com.lazygeniouz.acv.base.BaseAd

/**
 * A Container over BaseAd to Handle
 * @see BaseAd
 * @see com.google.android.gms.ads.AdView
 *
 * Handles and calls AdView's
 * respective lifecycle methods.
 *
 * We add a @Keep annotation because there are chances
 * when the user only adds this inside the XML Layout,
 * and either Proguard or R8 might remove this class.
 */
@Keep
class AdContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseAd(context, attrs, defStyleAttr) {

    init {
        // It `should` be a FragmentActivity Instance!
        // For Fragment, afaik, the Host Activity's instance will be used by default.
        if (context is FragmentActivity) context.lifecycle.addObserver(HostActivityObserver())
        else throw IllegalArgumentException("The supplied Context is not an instance of FragmentActivity")
    }

    /**
     * [insertAdView] Loads and Adds the `AdView` in the View
     *
     * @param adUnitId The AdUnitId of your banner ad, default is test adUnitId
     * @param adSize The AdSize of the Banner Ad
     * @param adRequest Optional AdRequest if you have customized request.
     *
     **/
    @JvmOverloads
    fun insertAdView(
        @NonNull adUnitId: String = this.adUnitId,
        adSize: AdSize = this.adSize,
        adRequest: AdRequest = this.getAdRequest(),
        showOnCondition: (() -> Boolean)? = null
    ) {
        if (adUnitId == TEST_AD_ID) Log.i(
            tag, "Current adUnitId is a Test Ad Unit, make sure to use your own in Production"
        )

        if (showOnCondition?.invoke() == false) {
            Log.d(tag, showOnConditionMessage)
            listener?.onAdFailedToLoad(
                LoadAdError(
                    -1, showOnConditionMessage,
                    tag, null, null
                )
            )
            return
        }

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

            override fun onAdOpened() {
                listener?.onAdOpened()
            }

            override fun onAdLoaded() {
                isAdLoaded = true
                newAdView!!.visibility = View.VISIBLE
                listener?.onAdLoaded()
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                listener?.onAdFailedToLoad(error)
                isAdLoaded = false
            }
        }

        removeAllViews()
        addView(newAdView)
        newAdView!!.layoutParams.apply { gravity = Gravity.CENTER }
        newAdView!!.loadAd(adRequest)
    }

    /**
     * Removes / Destroys the Ad from the View.
     * Make sure to call [insertAdView()] to load & add the AdView again
     */
    @Suppress("unused")
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
        isAdLoaded = false
        removeAllViews()
    }

    override fun onDetachedFromWindow() {
        destroyAd()
        super.onDetachedFromWindow()
    }

    companion object {
        const val TEST_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    }

    /**
     * Observer to call AdView's respective methods on appropriate Lifecycle event
     */
    private inner class HostActivityObserver : LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        private fun onCreate() {
            if (autoLoad) {
                insertAdView(adUnitId, adSize)
                Log.d(tag, "onCreate()")
            }
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        private fun pause() {
            pauseAd()
            Log.d(tag, "pauseAd()")
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        private fun destroy() {
            destroyAd()
            Log.d(tag, "destroyAd()")
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        private fun resume() {
            resumeAd()
            Log.d(tag, "resumeAd()")
        }
    }
}