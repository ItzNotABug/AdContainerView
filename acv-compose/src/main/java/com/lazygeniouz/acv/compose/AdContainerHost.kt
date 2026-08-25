package com.lazygeniouz.acv.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.lazygeniouz.acv.AdContainerView

@Composable
internal fun AdContainerHost(
    adRequest: BannerAdRequest,
    state: AdContainerState,
    modifier: Modifier,
    onAdLoadStarted: () -> Unit,
    adLoadCallback: AdLoadCallback<BannerAd>?,
    adEventCallback: BannerAdEventCallback?,
    adRefreshCallback: BannerAdRefreshCallback?
) {
    val currentOnAdLoadStarted = rememberUpdatedState(onAdLoadStarted)
    val currentAdLoadCallback = rememberUpdatedState(adLoadCallback)

    if (LocalInspectionMode.current) {
        Box(modifier = modifier)
        return
    }

    key(state) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdContainerView(context).also { container ->
                    container.setAdLoadCallback(object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            state.onAdLoaded(container)
                            currentAdLoadCallback.value?.onAdLoaded(ad)
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            state.onAdFailedToLoad(container, adError)
                            currentAdLoadCallback.value?.onAdFailedToLoad(adError)
                        }
                    })
                    container.setAdEventCallback(adEventCallback)
                    container.setAdRefreshCallback(adRefreshCallback)
                    state.attach(container, adRequest)
                    currentOnAdLoadStarted.value()
                    container.loadAdView(adRequest)
                }
            },
            update = { container ->
                container.setAdEventCallback(adEventCallback)
                container.setAdRefreshCallback(adRefreshCallback)
                if (state.updateRequest(container, adRequest)) {
                    currentOnAdLoadStarted.value()
                    container.loadAdView(adRequest)
                }
            },
            onRelease = { container ->
                container.destroyAd()
                state.onReleased(container)
            }
        )
    }
}