package com.lazygeniouz.acv

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.base.BaseAd

/** A lifecycle-aware container for a Next-Gen banner [AdView]. */
@Keep
class AdContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseAd(context, attrs, defStyleAttr) {

    init {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(HostActivityObserver())
        } else {
            logDebug("Context is not a LifecycleOwner. $makeSureToHandleLifecycleMessage")
        }
    }

    /**
     * Loads a banner with the configured ad unit and size.
     *
     * @param parentHasListView keeps the banner alive across temporary list detachments.
     * @param showOnCondition skips the request when it returns false.
     */
    @MainThread
    fun loadAdView(
        adUnitId: String = this.adUnitId,
        adSize: AdSize = this.adSize,
        parentHasListView: Boolean = false,
        showOnCondition: (() -> Boolean)? = null
    ) = loadAdView(
        adRequest = getAdRequest(adUnitId, adSize),
        parentHasListView = parentHasListView,
        showOnCondition = showOnCondition
    )

    /**
     * Loads a customized Next-Gen [BannerAdRequest]. The request's ad unit ID and
     * ad size become this container's current values.
     */
    @JvmOverloads
    @MainThread
    fun loadAdView(
        adRequest: BannerAdRequest,
        parentHasListView: Boolean = false,
        showOnCondition: (() -> Boolean)? = null
    ) {

        if (showOnCondition?.invoke() == false) {
            logDebug(showOnConditionMessage)
            notifyLoadFailure(
                LoadAdError(LoadAdError.ErrorCode.CANCELLED, showOnConditionMessage)
            )
            return
        }

        if (!MobileAds.isInitialized) {
            val message =
                "Ad request skipped because GMA Next-Gen is not initialized. " +
                    "Await MobileAds.initialize() before calling loadAdView()."
            logDebug(message)
            notifyLoadFailure(LoadAdError(LoadAdError.ErrorCode.APP_ID_MISSING, message))
            return
        }

        if (adRequest.adUnitId == FIXED_SIZE_TEST_AD_ID ||
            adRequest.adUnitId == ADAPTIVE_SIZE_TEST_AD_ID
        ) {
            logDebug("Using a Google test ad unit; replace it before publishing.")
        }

        parentMayHaveAListView = parentHasListView
        adUnitId = adRequest.adUnitId
        adSize = adRequest.adSize

        destroyAd()
        isAdLoading = true

        val adView = AdView(context).also {
            it.visibility = View.GONE
            it.background = transparent
        }
        newAdView = adView

        val layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            addRule(CENTER_IN_PARENT)
        }
        addView(adView, layoutParams)
        logDebug("Loading banner ($adSize).")
        adView.loadAd(adRequest, createLoadCallback(adView))
    }

    private fun createLoadCallback(adView: AdView): AdLoadCallback<BannerAd> =
        object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                attachAdCallbacks(adView, ad)
                runOnMainThread {
                    if (newAdView !== adView) {
                        ad.destroy()
                        return@runOnMainThread
                    }
                    isAdLoading = false
                    isAdLoaded = true
                    adView.visibility = View.VISIBLE
                    logDebug("Banner loaded.")
                    loadCallback?.onAdLoaded(ad)
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                runOnMainThread {
                    if (newAdView !== adView) return@runOnMainThread
                    isAdLoading = false
                    isAdLoaded = false
                    adView.visibility = View.GONE
                    logDebug("Banner load failed (${adError.code}): ${adError.message}")
                    loadCallback?.onAdFailedToLoad(adError)
                }
            }
        }

    private fun attachAdCallbacks(adView: AdView, ad: BannerAd) {
        ad.adEventCallback = object : BannerAdEventCallback {
            override fun onAdClicked() = dispatchFor(adView) {
                eventCallback?.onAdClicked()
            }

            override fun onAdImpression() = dispatchFor(adView) {
                eventCallback?.onAdImpression()
            }

            override fun onAdPaid(value: AdValue) = dispatchFor(adView) {
                eventCallback?.onAdPaid(value)
            }

            override fun onAdShowedFullScreenContent() = dispatchFor(adView) {
                eventCallback?.onAdShowedFullScreenContent()
            }

            override fun onAdDismissedFullScreenContent() = dispatchFor(adView) {
                eventCallback?.onAdDismissedFullScreenContent()
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) = dispatchFor(adView) {
                eventCallback?.onAdFailedToShowFullScreenContent(fullScreenContentError)
            }

            override fun onAppEvent(name: String, data: String?) = dispatchFor(adView) {
                eventCallback?.onAppEvent(name, data)
            }
        }

        ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
            override fun onAdRefreshed() = dispatchFor(adView) {
                isAdLoaded = true
                adView.visibility = View.VISIBLE
                logDebug("Banner refreshed.")
                refreshCallback?.onAdRefreshed()
            }

            override fun onAdFailedToRefresh(adError: LoadAdError) = dispatchFor(adView) {
                logDebug("Banner refresh failed (${adError.code}): ${adError.message}")
                refreshCallback?.onAdFailedToRefresh(adError)
            }
        }
    }

    private fun dispatchFor(adView: AdView, action: () -> Unit) {
        runOnMainThread {
            if (newAdView === adView) action()
        }
    }

    private fun notifyLoadFailure(error: LoadAdError) {
        runOnMainThread {
            loadCallback?.onAdFailedToLoad(error)
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (isMainThread()) action() else MAIN_HANDLER.post { action() }
    }

    private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()

    /**
     * Returns [AdView] if certain op. needs to be performed
     * or certain info is required like mediation info of the ad.
     */
    fun getAdView(): AdView? = newAdView

    /**
     * Removes / Destroys the Ad from the View.
     *
     * Make sure to call [loadAdView] to load & add the AdView again
     */
    @MainThread
    fun removeAd() = destroyAd()

    /** Destroys and removes the current banner. */
    @MainThread
    fun destroyAd() {
        newAdView?.destroy()
        newAdView = null
        isAdLoading = false
        isAdLoaded = false
        removeAllViews()
    }

    /** Keeps ads alive across temporary detachments when explicitly used in a scrolling parent. */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (!parentMayHaveAListView) destroyAd()
    }

    companion object {
        const val TAG = "AdContainerView"
        const val FIXED_SIZE_TEST_AD_ID = "ca-app-pub-3940256099942544/6300978111"
        const val ADAPTIVE_SIZE_TEST_AD_ID = "ca-app-pub-3940256099942544/9214589741"

        private val MAIN_HANDLER = Handler(Looper.getMainLooper())

        private fun logDebug(message: String) = Log.d(TAG, message)
    }

    /** Connects automatic loading and cleanup to the host lifecycle. */
    private inner class HostActivityObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> if (autoLoad) loadAdView(adUnitId, adSize)
                Lifecycle.Event.ON_DESTROY -> destroyAd()
                else -> { /* ignore other events */
                }
            }
        }
    }
}
