# AdContainerView

A lifecycle-aware banner wrapper for the
[GMA Next-Gen SDK](https://developers.google.com/admob/android/next-gen). It creates and
releases `AdView`, tracks load state, and forwards banner callbacks on the main thread.

> [!IMPORTANT]
> Version 0.5.0 uses GMA Next-Gen SDK 1.3.1. It requires Android API 24+, `compileSdk` 35+,
> Kotlin 1.9+ for Kotlin apps, and completed SDK initialization before the first ad request.

## Install

Make sure both Google Maven and Maven Central are configured:

```gradle
repositories {
    google()
    mavenCentral()
}

def version = '0.5.0'

dependencies {
    implementation "com.lazygeniouz:acv:$version"
}
```

The Next-Gen SDK is exposed transitively; don't add `play-services-ads`.

Apps using AdMob mediation must exclude the
[legacy SDK modules](https://developers.google.com/admob/android/next-gen/mediation#exclude_comgoogleandroidgms_modules_in_mediation_integrations)
that adapters otherwise pull in:

```gradle
configurations.configureEach {
    exclude group: 'com.google.android.gms', module: 'play-services-ads'
    exclude group: 'com.google.android.gms', module: 'play-services-ads-lite'
}
```

Other mediation platforms are not currently compatible with Next-Gen.

## Quick start

Add the container to your layout:

```xml
<com.lazygeniouz.acv.AdContainerView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/adContainerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:acv_adSize="LARGE_ADAPTIVE"
    app:acv_adUnitId="@string/banner_ad_unit_id" />
```

Initialize Next-Gen in a background coroutine, then load the banner:

```kotlin
val adContainerView = findViewById<AdContainerView>(R.id.adContainerView)

adContainerView.setAdLoadCallback(object : AdLoadCallback<BannerAd> {
    override fun onAdLoaded(ad: BannerAd) {
        // Banner loaded. This callback runs on the main thread.
    }

    override fun onAdFailedToLoad(adError: LoadAdError) {
        // Handle the load failure.
    }
})

val config = InitializationConfig.Builder(ADMOB_APP_ID).build()

CoroutineScope(Dispatchers.IO).launch {
    MobileAds.initialize(applicationContext, config)
    withContext(Dispatchers.Main) {
        adContainerView.loadAdView()
    }
}
```

With mediation, wait for the SDK initialization callback before loading so adapters are ready.

Next-Gen receives the app ID through `InitializationConfig`, not the legacy
`com.google.android.gms.ads.APPLICATION_ID` manifest entry. Apps using UMP must still keep that
manifest entry for UMP.

The legacy `OPTIMIZE_INITIALIZATION` and `OPTIMIZE_AD_LOADING` manifest flags are not part of the
Next-Gen setup. Initialize Next-Gen in a background coroutine instead.

## Configuration

| XML attribute  | Default          | Description                                                           |
|----------------|------------------|-----------------------------------------------------------------------|
| `acv_adUnitId` | Google test unit | Banner ad unit ID; test default matches the selected size             |
| `acv_adSize`   | `LARGE_ADAPTIVE` | Large adaptive or fixed banner size                                   |
| `acv_autoLoad` | `false`          | Loads during `ON_CREATE`; SDK initialization must already be complete |

`SMART_BANNER` has no Next-Gen equivalent. For source compatibility, the legacy
`ADAPTIVE` and `SMART_BANNER` XML values both resolve to `LARGE_ADAPTIVE`; use
`LARGE_ADAPTIVE` in new layouts.

The fixed XML options are `BANNER`, `LARGE_BANNER`, `MEDIUM_RECTANGLE`, `FULL_BANNER`, and
`LEADERBOARD`. `WIDE_SKYSCRAPER` was removed because Next-Gen does not support it as a standard
banner size.

You can also supply the ad unit and size directly:

```kotlin
adContainerView.loadAdView(adUnitId, adSize)
```

For targeting or request extras, pass a customized `BannerAdRequest`:

```kotlin
val request = BannerAdRequest.Builder(adUnitId, adSize)
    .addKeyword("games")
    .build()

adContainerView.loadAdView(request)
```

Both overloads accept `parentHasListView` and `showOnCondition` options. Setting
`parentHasListView=true` disables detach cleanup for recycled list items, so the caller must invoke
`destroyAd()` when its lifecycle ends.

## Callbacks

Next-Gen separates banner callbacks by purpose:

- `setAdLoadCallback()` — initial load success or failure
- `setAdEventCallback()` — click, impression, paid, and full-screen events
- `setAdRefreshCallback()` — automatic refresh success or failure

AdContainerView delivers callbacks registered through these methods on the main thread.

## API

- `getAdView()` returns the current Next-Gen `AdView`.
- `isLoading()`, `isAdLoaded()`, and `isVisible()` expose banner state.
- `getAdSize()` and `getAdUnitId()` expose the active request configuration.
- `removeAd()` and `destroyAd()` release the current banner. Detached views are cleaned up
  automatically unless `parentHasListView=true`.

## Migrating from 0.4.x

| 0.4.x                          | 0.5.0                                      |
|--------------------------------|--------------------------------------------|
| `play-services-ads`            | `ads-mobile-sdk:1.3.1`                     |
| Minimum API 21                 | Minimum API 24                             |
| Manifest app ID                | `InitializationConfig.Builder(appId)`      |
| `AdRequest`                    | `BannerAdRequest` with ad unit ID and size |
| `AdListener`                   | Load, event, and refresh callbacks         |
| Smart/standard adaptive banner | Large anchored adaptive banner             |
| `WIDE_SKYSCRAPER`              | Removed; no standard Next-Gen equivalent   |
| `pauseAd()` / `resumeAd()`     | Removed; Next-Gen has no equivalent        |

See Google's [SDK migration guide](https://developers.google.com/admob/android/next-gen/migration)
and [banner migration guide](https://developers.google.com/admob/android/next-gen/migration/migrate-banner)
for more detail.
