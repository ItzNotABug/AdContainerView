package com.lazygeniouz.acv.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.AdContainerView

/**
 * Loads and displays a large anchored adaptive banner using the available Compose width.
 * The banner reloads when the available width or [adUnitId] changes.
 *
 * @param adUnitId the banner ad unit ID.
 * @param modifier the modifier applied to the full-width banner container.
 * @param onAdLoaded called with the loaded banner on the main thread.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdaptiveAdContainer(
    adUnitId: String,
    modifier: Modifier = Modifier,
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current

    BoxWithConstraints(modifier = modifier) {
        val windowWidthDp = with(density) {
            windowInfo.containerSize.width.toDp().value.toInt()
        }.coerceAtLeast(1)
        val availableWidthDp = maxWidth.value
            .takeIf { it.isFinite() && it > 0f }
            ?.toInt()
            ?: windowWidthDp
        val adSize = remember(context, availableWidthDp) {
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
        }

        AdContainer(
            adUnitId = adUnitId,
            adSize = adSize,
            modifier = Modifier.fillMaxWidth(),
            onAdLoaded = onAdLoaded,
            onAdFailedToLoad = onAdFailedToLoad
        )
    }
}

/**
 * Loads and displays a banner for the supplied ad unit and fixed or precomputed adaptive size.
 * The banner reloads when [adUnitId] or [adSize] changes.
 *
 * @param adUnitId the banner ad unit ID.
 * @param adSize the fixed or precomputed adaptive banner size.
 * @param modifier the modifier applied to the banner container.
 * @param onAdLoaded called with the loaded banner on the main thread.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdContainer(
    adUnitId: String,
    adSize: AdSize,
    modifier: Modifier = Modifier,
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val adRequest = remember(adUnitId, adSize) {
        BannerAdRequest.Builder(adUnitId, adSize).build()
    }
    AdContainer(
        adRequest = adRequest,
        modifier = modifier,
        onAdLoaded = onAdLoaded,
        onAdFailedToLoad = onAdFailedToLoad
    )
}

/**
 * Loads and displays a banner for a customized [BannerAdRequest]. Remember requests that should
 * remain stable across recomposition; a different request instance reloads the banner.
 *
 * @param adRequest the fully configured banner request.
 * @param modifier the modifier applied to the banner container.
 * @param onAdLoaded called with the loaded banner on the main thread. Configure event and refresh
 * callbacks on the returned banner when needed.
 * @param onAdFailedToLoad called with the load error on the main thread.
 */
@Composable
fun AdContainer(
    adRequest: BannerAdRequest,
    modifier: Modifier = Modifier,
    onAdLoaded: (BannerAd) -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val currentOnAdLoaded by rememberUpdatedState(onAdLoaded)
    val currentOnAdFailedToLoad by rememberUpdatedState(onAdFailedToLoad)

    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    key(adRequest) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdContainerView(context).apply {
                    setAdLoadCallback(object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            currentOnAdLoaded(ad)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            currentOnAdFailedToLoad(adError)
                        }
                    })
                    loadAdView(adRequest)
                }
            },
            onRelease = AdContainerView::destroyAd
        )
    }
}