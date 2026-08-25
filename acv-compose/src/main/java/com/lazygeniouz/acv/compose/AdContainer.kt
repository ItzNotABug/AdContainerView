@file:JvmMultifileClass
@file:JvmName("AdContainerKt")

package com.lazygeniouz.acv.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Loads and displays a banner for the supplied ad unit and fixed or precomputed adaptive size.
 * The banner reloads when [adUnitId] or [adSize] changes.
 *
 * @param adUnitId the banner ad unit ID.
 * @param adSize the fixed or precomputed adaptive banner size.
 * @param modifier the modifier applied to the banner container.
 * @param onAdLoadStarted called on the main thread immediately before a new request attempt.
 * @param onAdLoaded called with the loaded banner on the main thread.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdContainer(
    adUnitId: String,
    adSize: AdSize,
    modifier: Modifier = Modifier,
    onAdLoadStarted: () -> Unit = {},
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    AdContainerHost(
        adRequest = rememberBannerAdRequest(adUnitId, adSize),
        state = rememberAdContainerState(),
        modifier = modifier,
        onAdLoadStarted = onAdLoadStarted,
        adLoadCallback = lambdaAdLoadCallback(onAdLoaded, onAdFailedToLoad),
        adEventCallback = null,
        adRefreshCallback = null
    )
}

/**
 * Loads and displays a controllable banner for the supplied ad unit and fixed or precomputed
 * adaptive size. Call [AdContainerState.reload] to submit the same request again.
 *
 * @param adUnitId the banner ad unit ID.
 * @param adSize the fixed or precomputed adaptive banner size.
 * @param state the observable state and reload control for this container.
 * @param modifier the modifier applied to the banner container.
 * @param adLoadCallback receives the initial banner load result.
 * @param adEventCallback receives click, impression, paid, app, and full-screen events.
 * @param adRefreshCallback receives automatic banner refresh results.
 */
@Composable
fun AdContainer(
    adUnitId: String,
    adSize: AdSize,
    state: AdContainerState,
    modifier: Modifier = Modifier,
    adLoadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    adRefreshCallback: BannerAdRefreshCallback? = null
) {
    AdContainerHost(
        adRequest = rememberBannerAdRequest(adUnitId, adSize),
        state = state,
        modifier = modifier,
        onAdLoadStarted = {},
        adLoadCallback = adLoadCallback,
        adEventCallback = adEventCallback,
        adRefreshCallback = adRefreshCallback
    )
}

/**
 * Loads and displays a banner for a customized [BannerAdRequest]. Remember requests that should
 * remain stable across recomposition; a different request instance reloads the banner.
 *
 * @param adRequest the fully configured banner request.
 * @param modifier the modifier applied to the banner container.
 * @param onAdLoadStarted called on the main thread immediately before a new request attempt.
 * @param onAdLoaded called with the loaded banner on the main thread. Configure event and refresh
 * callbacks on the returned banner when needed.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdContainer(
    adRequest: BannerAdRequest,
    modifier: Modifier = Modifier,
    onAdLoadStarted: () -> Unit = {},
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    AdContainerHost(
        adRequest = adRequest,
        state = rememberAdContainerState(),
        modifier = modifier,
        onAdLoadStarted = onAdLoadStarted,
        adLoadCallback = lambdaAdLoadCallback(onAdLoaded, onAdFailedToLoad),
        adEventCallback = null,
        adRefreshCallback = null
    )
}

/**
 * Loads and displays a controllable banner for a customized [BannerAdRequest]. Remember requests
 * that should remain stable across recomposition; a different request instance reloads the banner.
 * Call [AdContainerState.reload] to submit the current request again.
 *
 * @param adRequest the fully configured banner request.
 * @param state the observable state and reload control for this container.
 * @param modifier the modifier applied to the banner container.
 * @param adLoadCallback receives the initial banner load result.
 * @param adEventCallback receives click, impression, paid, app, and full-screen events.
 * @param adRefreshCallback receives automatic banner refresh results.
 */
@Composable
fun AdContainer(
    adRequest: BannerAdRequest,
    state: AdContainerState,
    modifier: Modifier = Modifier,
    adLoadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    adRefreshCallback: BannerAdRefreshCallback? = null
) {
    AdContainerHost(
        adRequest = adRequest,
        state = state,
        modifier = modifier,
        onAdLoadStarted = {},
        adLoadCallback = adLoadCallback,
        adEventCallback = adEventCallback,
        adRefreshCallback = adRefreshCallback
    )
}

@Composable
private fun rememberBannerAdRequest(adUnitId: String, adSize: AdSize): BannerAdRequest =
    remember(adUnitId, adSize) {
        BannerAdRequest.Builder(adUnitId, adSize).build()
    }

private fun lambdaAdLoadCallback(
    onAdLoaded: (BannerAd) -> Unit,
    onAdFailedToLoad: (LoadAdError) -> Unit
): AdLoadCallback<BannerAd> = object : AdLoadCallback<BannerAd> {
    override fun onAdLoaded(ad: BannerAd) = onAdLoaded(ad)

    override fun onAdFailedToLoad(adError: LoadAdError) = onAdFailedToLoad(adError)
}