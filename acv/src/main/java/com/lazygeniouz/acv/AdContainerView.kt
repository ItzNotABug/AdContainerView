package com.lazygeniouz.acv

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.gms.ads.*
import com.lazygeniouz.acv.base.BaseAd

/**
 * A Container over BaseAd to Handle [AdView] via [BaseAd]
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
        else Log.d(
            tag,
            "The supplied Context is not an instance of FragmentActivity. " +
                    "Make sure to call the resume, pause, destroy lifecycle methods."
        )
    }

    /**
     * [loadAdView] Loads and Adds the `AdView` in the View
     *
     * @param adUnitId The AdUnitId of your banner ad, default is Test Ad Unit Id
     * @param adSize The AdSize of the Banner Ad
     * @param adRequest Optional AdRequest if you have customized request.
     * @param parentHasListView Prevents Ad detach inside a Scrollable View
     * @param showOnCondition Load Ad only when this lambda returns True
     *
     **/
    fun loadAdView(
        adUnitId: String = this.adUnitId,
        adSize: AdSize = this.adSize,
        adRequest: AdRequest = this.getAdRequest(),
        parentHasListView: Boolean = false,
        showOnCondition: (() -> Boolean)? = null
    ) {

        parentMayHaveAListView = parentHasListView

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

        newAdView = AdView(context).also { newAdView ->
            newAdView.visibility = View.GONE
            newAdView.background = transparent
            newAdView.adUnitId = adUnitId
            newAdView.adSize = adSize
            newAdView.adListener = object : AdListener() {
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
                    newAdView.visibility = View.VISIBLE
                    listener?.onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    listener?.onAdFailedToLoad(error)
                    isAdLoaded = false
                }
            }
        }

        removeAllViews()
        addView(newAdView)
        newAdView?.let { adView ->
            adView.layoutParams.apply { gravity = Gravity.CENTER }
            adView.loadAd(adRequest)
        }
    }

    /**
     * Removes / Destroys the Ad from the View.
     *
     * Make sure to call [loadAdView] to load & add the AdView again
     */
    @Suppress("unused")
    fun removeAd() = destroyAd()

    /**
     * Same as [AdView.resume]
     */
    fun resumeAd() = newAdView?.resume()

    /**
     * Same as [AdView.pause]
     */
    fun pauseAd() = newAdView?.pause()

    /**
     *
     * Avoiding this issue: "**#004 The webview is destroyed. Ignoring action.**"
     * by using `newAdView = null` or `removeAllViews()`.
     *
     * It is found that the above info. is printed in 2 scenarios.
     * 1. when the Ad refreshes,
     * 2. when the Activity is destroyed.
     *
     * Same as [AdView.destroy]
     **/
    fun destroyAd() {
        newAdView?.destroy()
        newAdView = null
        isAdLoaded = false
        removeAllViews()
    }

    /**
     * A boolean check is necessary to check if any ViewGroup in the hierarchy is a Scrollable View
     * like RecyclerView, ListView, GridView, etc.
     *
     * It is better to use a boolean provided by the developer first hand as manually looping over
     * the View hierarchy is **not** memory efficient and also not a good practice.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!parentMayHaveAListView) destroyAd()
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
                loadAdView(adUnitId, adSize)
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