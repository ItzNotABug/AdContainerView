# AdContainerView

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/685458c0953f4dd0b84956383b491f29)](https://app.codacy.com/gh/ItzNotABug/AdContainerView)

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

dependencies {
    implementation 'com.lazygeniouz:acv:0.5.0'
}
```

The Next-Gen SDK is exposed transitively; don't add `play-services-ads`.

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

Initialize Next-Gen on a background thread, then load the banner:

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

Thread {
    MobileAds.initialize(applicationContext, config)
    runOnUiThread { adContainerView.loadAdView() }
}.start()
```

Next-Gen receives the app ID through `InitializationConfig`, not the legacy
`com.google.android.gms.ads.APPLICATION_ID` manifest entry. Apps using UMP must still keep that
manifest entry for UMP.

## Configuration

| XML attribute  | Default          | Description                                                           |
|----------------|------------------|-----------------------------------------------------------------------|
| `acv_adUnitId` | Google test unit | Banner ad unit ID; test default matches the selected size             |
| `acv_adSize`   | `LARGE_ADAPTIVE` | Large adaptive or fixed banner size                                   |
| `acv_autoLoad` | `false`          | Loads during `ON_CREATE`; SDK initialization must already be complete |

`SMART_BANNER` has no Next-Gen equivalent. For source compatibility, the legacy
`ADAPTIVE` and `SMART_BANNER` XML values both resolve to `LARGE_ADAPTIVE`; use
`LARGE_ADAPTIVE` in new layouts.

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

Both overloads accept `parentHasListView` and `showOnCondition` options.

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
- `removeAd()` and `destroyAd()` release the current banner. Lifecycle-aware hosts are handled
  automatically.

## Migrating from 0.4.x

| 0.4.x                          | 0.5.0                                      |
|--------------------------------|--------------------------------------------|
| `play-services-ads`            | `ads-mobile-sdk:1.3.1`                     |
| Minimum API 21                 | Minimum API 24                             |
| Manifest app ID                | `InitializationConfig.Builder(appId)`      |
| `AdRequest`                    | `BannerAdRequest` with ad unit ID and size |
| `AdListener`                   | Load, event, and refresh callbacks         |
| Smart/standard adaptive banner | Large anchored adaptive banner             |
| `pauseAd()` / `resumeAd()`     | Removed; Next-Gen has no equivalent        |

See Google's [SDK migration guide](https://developers.google.com/admob/android/next-gen/migration)
and [banner migration guide](https://developers.google.com/admob/android/next-gen/migration/migrate-banner)
for more detail.
