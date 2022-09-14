package com.lazygeniouz.acv

import android.content.Context
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.annotation.Keep
import androidx.annotation.Nullable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
        if (context is FragmentActivity) {
            if (isMainThread()) {
                // `addObserver` only on MainThread
                context.lifecycle.addObserver(HostActivityObserver())
            } else {
                // Should we switch the thread for adding observer via [Handler.post()]?
                logDebug("Current thread is not main, not adding lifecycle observer. $makeSureToHandleLifecycleMessage")
            }
        } else logDebug("Context is not a FragmentActivity. $makeSureToHandleLifecycleMessage")
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

        if (adUnitId == TEST_AD_ID) {
            logDebug("Current adUnitId is a Test Ad Unit, make sure to use your own in Production")
        }

        if (showOnCondition?.invoke() == false) {
            logDebug(showOnConditionMessage)
            listener?.onAdFailedToLoad(LoadAdError(-1, showOnConditionMessage, TAG, null, null))
            return
        }

        removeAllViews()

        newAdView = AdView(context).also { newAdView ->
            newAdView.visibility = View.GONE
            newAdView.background = transparent
            newAdView.adUnitId = adUnitId
            newAdView.setAdSize(adSize)
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

    private fun isMainThread(): Boolean {
        return Thread.currentThread() == Looper.getMainLooper().thread
    }

    /**
     * Returns [AdView] if certain op. needs to be performed
     * or certain info is required like mediation info of the ad.
     */
    @Nullable
    @Suppress("unused")
    fun getAdView(): AdView? = newAdView

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
        const val TAG = "AdContainerView"
        const val TEST_AD_ID = "ca-app-pub-3940256099942544/6300978111"

        private fun logDebug(message: String) = Log.d(TAG, message)
    }

    /**
     * Observer to call AdView's respective methods on appropriate Lifecycle event
     */
    private inner class HostActivityObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> if (autoLoad) loadAdView(adUnitId, adSize)
                Lifecycle.Event.ON_RESUME -> resumeAd()
                Lifecycle.Event.ON_PAUSE -> pauseAd()
                Lifecycle.Event.ON_DESTROY -> destroyAd()
                else -> { /* ignore other events */
                }
            }
        }
    }
}