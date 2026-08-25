package com.lazygeniouz.acv.compose

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.AdContainerView

/** The current explicit-load state of a Compose ad container. */
sealed interface AdContainerLoadState {

    /** No ad container is currently attached to this state. */
    data object Idle : AdContainerLoadState

    /** A banner request is in progress. */
    data object Loading : AdContainerLoadState

    /** The latest banner request completed successfully. */
    data object Loaded : AdContainerLoadState

    /** The latest banner request failed. */
    data class Failed(val error: LoadAdError) : AdContainerLoadState
}

/**
 * Observable load state and reload control for one Compose ad container.
 *
 * Use [rememberAdContainerState] in composition. One instance can control only one simultaneously
 * composed ad container.
 */
@Stable
class AdContainerState {

    /** The current explicit-load state. Automatic refresh results are reported separately. */
    var loadState: AdContainerLoadState by mutableStateOf(AdContainerLoadState.Idle)
        private set

    private var attachedContainer: AdContainerView? = null
    private var attachedRequest: BannerAdRequest? = null

    /** Requests a fresh banner using the currently composed request. Does nothing when detached. */
    @MainThread
    fun reload() {
        val container = attachedContainer ?: return
        val request = attachedRequest ?: return

        loadState = AdContainerLoadState.Loading
        container.loadAdView(request)
    }

    internal fun attach(container: AdContainerView, request: BannerAdRequest) {
        check(attachedContainer == null || attachedContainer === container) {
            "AdContainerState cannot be shared by multiple ad containers."
        }
        attachedContainer = container
        attachedRequest = request
        loadState = AdContainerLoadState.Loading
    }

    internal fun updateRequest(container: AdContainerView, request: BannerAdRequest): Boolean {
        if (attachedContainer !== container || attachedRequest === request) return false

        attachedRequest = request
        loadState = AdContainerLoadState.Loading
        return true
    }

    internal fun onAdLoaded(container: AdContainerView) {
        if (attachedContainer === container) {
            loadState = if (container.isLoading()) {
                AdContainerLoadState.Loading
            } else {
                AdContainerLoadState.Loaded
            }
        }
    }

    internal fun onAdFailedToLoad(container: AdContainerView, error: LoadAdError) {
        if (attachedContainer === container) {
            loadState = if (container.isLoading()) {
                AdContainerLoadState.Loading
            } else {
                AdContainerLoadState.Failed(error)
            }
        }
    }

    internal fun onReleased(container: AdContainerView) {
        if (attachedContainer === container) {
            attachedContainer = null
            attachedRequest = null
            loadState = AdContainerLoadState.Idle
        }
    }
}

/** Remembers an [AdContainerState] for one Compose ad container. */
@Composable
fun rememberAdContainerState(): AdContainerState = remember { AdContainerState() }