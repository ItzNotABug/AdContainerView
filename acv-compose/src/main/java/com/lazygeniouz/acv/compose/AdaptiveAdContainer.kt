@file:JvmMultifileClass
@file:JvmName("AdContainerKt")

package com.lazygeniouz.acv.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError

/**
 * Loads and displays a large anchored adaptive banner using the available Compose width.
 * The banner reloads when its calculated size or [adUnitId] changes.
 *
 * @param adUnitId the banner ad unit ID.
 * @param modifier the modifier applied to the full-width banner container.
 * @param onAdLoadStarted called on the main thread immediately before a new request attempt.
 * @param onAdLoaded called with the loaded banner on the main thread.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdaptiveAdContainer(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onAdLoadStarted: () -> Unit = {},
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    AdaptiveAdContainerContent(
        adUnitId = adUnitId,
        state = rememberAdContainerState(),
        modifier = modifier,
        adLoadCallback = object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) = onAdLoaded(ad)

            override fun onAdFailedToLoad(adError: LoadAdError) = onAdFailedToLoad(adError)
        },
        onAdLoadStarted = onAdLoadStarted
    )
}

@Composable
private fun rememberBannerAdRequest(adUnitId: String, adSize: AdSize): BannerAdRequest =
    remember(adUnitId, adSize) {
        BannerAdRequest.Builder(adUnitId, adSize).build()
    }

/**
 * Loads and displays a controllable large anchored adaptive banner using the available Compose
 * width. The banner reloads when its calculated size or [adUnitId] changes.
 *
 * @param adUnitId the banner ad unit ID.
 * @param state the observable state and reload control for this container.
 * @param modifier the modifier applied to the full-width banner container.
 * @param adLoadCallback receives the initial banner load result.
 * @param adEventCallback receives click, impression, paid, app, and full-screen events.
 * @param adRefreshCallback receives automatic banner refresh results.
 */
@Composable
fun AdaptiveAdContainer(
    adUnitId: String,
    state: AdContainerState,
    modifier: Modifier = Modifier,
    adLoadCallback: AdLoadCallback<BannerAd>? = null,
    adEventCallback: BannerAdEventCallback? = null,
    adRefreshCallback: BannerAdRefreshCallback? = null
) {
    AdaptiveAdContainerContent(
        adUnitId = adUnitId,
        state = state,
        modifier = modifier,
        onAdLoadStarted = {},
        adLoadCallback = adLoadCallback,
        adEventCallback = adEventCallback,
        adRefreshCallback = adRefreshCallback
    )
}

@Composable
private fun AdaptiveAdContainerContent(
    adUnitId: String,
    state: AdContainerState,
    modifier: Modifier,
    onAdLoadStarted: () -> Unit,
    adLoadCallback: AdLoadCallback<BannerAd>?,
    adEventCallback: BannerAdEventCallback? = null,
    adRefreshCallback: BannerAdRefreshCallback? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    BoxWithConstraints(modifier = modifier) {
        val windowSize = windowInfo.containerSize
        val windowWidthDp = with(density) {
            windowSize.width.toDp().value.toInt()
        }.coerceAtLeast(1)
        val availableWidthDp = maxWidth.value
            .takeIf { it.isFinite() && it > 0f }
            ?.toInt()
            ?: windowWidthDp
        val adSize = remember(
            context,
            availableWidthDp,
            windowSize.height,
            density.density
        ) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        }

        AdContainerHost(
            adRequest = rememberBannerAdRequest(adUnitId, adSize),
            state = state,
            modifier = Modifier.fillMaxWidth(),
            onAdLoadStarted = onAdLoadStarted,
            adLoadCallback = adLoadCallback,
            adEventCallback = adEventCallback,
            adRefreshCallback = adRefreshCallback
        )
    }
}