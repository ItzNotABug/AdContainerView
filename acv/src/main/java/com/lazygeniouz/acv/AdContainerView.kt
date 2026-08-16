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
import androidx.lifecycle.ViewTreeLifecycleOwner
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

/**
 * A lifecycle-aware container for a Next-Gen banner [AdView].
 *
 * @param context the view context.
 * @param attrs XML attributes for the view.
 * @param defStyleAttr the default style attribute.
 */
@Keep
class AdContainerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : BaseAd(context, attrs, defStyleAttr) {

    private val hostLifecycleObserver = HostLifecycleObserver()
    private var observedLifecycle: Lifecycle? = null
    private var autoLoadPending = false

    /**
     * Loads a banner with the configured ad unit and size.
     * A skipped request reports [LoadAdError.ErrorCode.CANCELLED] and leaves the current banner
     * and request configuration unchanged.
     *
     * @param adUnitId the banner ad unit ID.
     * @param adSize the requested banner size. When available, this view's measured content width
     * is used to recalculate the configured adaptive default.
     * @param parentHasListView disables detach cleanup for temporary list recycling. When true,
     * the caller must invoke [destroyAd] when its lifecycle ends.
     * @param showOnCondition skips the request when it returns false.
     */
    @MainThread
    fun loadAdView(
        adUnitId: String = this.adUnitId,
        adSize: AdSize = resolveConfiguredAdSize(),
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
     * A skipped request reports [LoadAdError.ErrorCode.CANCELLED] and leaves the current banner
     * and request configuration unchanged.
     *
     * @param adRequest the fully configured banner request.
     * @param parentHasListView disables detach cleanup for temporary list recycling. When true,
     * the caller must invoke [destroyAd] when its lifecycle ends.
     * @param showOnCondition skips the request when it returns false.
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
            notifyLoadFailure(LoadAdError(LoadAdError.ErrorCode.CANCELLED, message))
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
        shouldResolveInitialAdaptiveSize = false

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

    /** Returns the current [AdView], or null before loading and after destruction. */
    @MainThread
    fun getAdView(): AdView? = newAdView

    /** Removes and destroys the current banner. Call [loadAdView] to load another. */
    @MainThread
    fun removeAd() = destroyAd()

    /**
     * Destroys and removes the current banner, cancels a pending measured-width auto-load, and
     * resets load state. Registered callbacks and the latest request configuration are retained
     * for reuse; late callbacks from the destroyed banner are ignored.
     */
    @MainThread
    fun destroyAd() {
        autoLoadPending = false
        newAdView?.destroy()
        newAdView = null
        isAdLoading = false
        isAdLoaded = false
        removeAllViews()
    }

    /** Binds cleanup to the nearest view-tree lifecycle owner. */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bindToLifecycle(ViewTreeLifecycleOwner.get(this) ?: context as? LifecycleOwner)
    }

    /** Keeps ads alive across temporary detachments when explicitly used in a scrolling parent. */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearLifecycleObserver()
        if (!parentMayHaveAListView) destroyAd()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (autoLoadPending && w - paddingLeft - paddingRight > 0) {
            autoLoadPending = false
            loadAdView()
        }
    }

    private fun bindToLifecycle(owner: LifecycleOwner?) {
        val lifecycle = owner?.lifecycle
        if (lifecycle === observedLifecycle) return

        clearLifecycleObserver()
        if (lifecycle == null) {
            logDebug(makeSureToHandleLifecycleMessage)
            return
        }

        observedLifecycle = lifecycle
        lifecycle.addObserver(hostLifecycleObserver)
    }

    private fun clearLifecycleObserver() {
        observedLifecycle?.removeObserver(hostLifecycleObserver)
        observedLifecycle = null
    }

    private fun loadConfiguredAdWhenMeasured() {
        if (width - paddingLeft - paddingRight > 0) {
            loadAdView()
        } else {
            autoLoadPending = true
        }
    }

    companion object {
        /** Logcat tag used by this library. */
        const val TAG = "AdContainerView"

        /** Google's fixed-size Android banner test ad unit ID. */
        const val FIXED_SIZE_TEST_AD_ID = "ca-app-pub-3940256099942544/6300978111"

        /** Google's adaptive Android banner test ad unit ID. */
        const val ADAPTIVE_SIZE_TEST_AD_ID = "ca-app-pub-3940256099942544/9214589741"

        private val MAIN_HANDLER = Handler(Looper.getMainLooper())

        private fun logDebug(message: String) = Log.d(TAG, message)
    }

    /** Connects automatic loading and cleanup to the nearest view-tree lifecycle. */
    private inner class HostLifecycleObserver : LifecycleEventObserver {

        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_CREATE -> if (autoLoad && newAdView == null) {
                    loadConfiguredAdWhenMeasured()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    destroyAd()
                    clearLifecycleObserver()
                }
                else -> { /* ignore other events */
                }
            }
        }
    }
}
