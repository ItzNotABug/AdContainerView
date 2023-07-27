# AdContainerView

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/685458c0953f4dd0b84956383b491f29)](https://app.codacy.com/gh/ItzNotABug/AdContainerView?utm_source=github.com&utm_medium=referral&utm_content=ItzNotABug/AdContainerView&utm_campaign=Badge_Grade_Settings)

AdContainerView is a simple, lifecycle aware wrapper over Google AdMob's AdView (Banner Ad) for a plug & play use-case.

For the simplest use:\
You just need to add `AdContainerView` in your layout, define `adUnitId`, `adSize` & **that's it!**\
`AdContainerView` hooks to your Activity's lifecycle process & handles AdView's lifecycle (Resume, Pause, Destroy Ad).\
This is most helpful when you just want to add a simple Banner Ad without any boilerplate.

## Adding in your project
`AdContainerView` is now on `mavenCentral()`,\
Make sure to add that to your repositories block.

`val latest_version = 0.3.8`

**Gradle**
```gradle
implementation 'com.lazygeniouz:acv:$latest_version'
```
**Maven**
```xml
<dependency>
    <groupId>com.lazygeniouz</groupId>
    <artifactId>acv</artifactId>
    <version>$latest_version</version>
    <type>aar</type>
</dependency>
```

## Using `AdContainerView`
### The same old XML Way
```xml
<com.lazygeniouz.acv.AdContainerView 
    android:id="@+id/adContainerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:acv_autoLoad="true"
    app:acv_adSize="ADAPTIVE"
    app:acv_adUnitId="@string/test_ad"/>
```
The attributes are as follows:
*   `acv_autoLoad`: Default Value is `false`\
    If `true`, the AdView will be loaded as soon as the Activity is created.

*   `acv_adSize`: Default Value is `ADAPTIVE` which equals to `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize()`.\
    Define an AdSize for the the AdView, check [AdSize](https://developers.google.com/android/reference/com/google/android/gms/ads/AdSize#field-summary).

*   `acv_adUnitId`: Define an `adUnitId` for the Banner AdView from your AdMob dashboard.

### Programmatically
```kotlin
val adContainerView = AdContainerView(this@MainActivity)
adContainerView.loadAdView(adUnitId, adSize, adRequest)
parentLayout.addView(adContainerView)
```
**Loading the Ad:**
```kotlin
fun loadAdView(
    adUnitId: String,
    adSize: AdSize,
    adRequest: AdRequest,
    parentHasListView: Boolean,
    showOnCondition: (() -> Boolean)? = null
)
```
1. The arguments `adSize` & `adRequest` are optional\
2. The default value of parameter `adSize` is `ADAPTIVE`.\
3. Pass your `AdRequest` if you have a customized request.\
4. Pass `true` if AdContainerView is added inside a list (RecyclerView, List / GridView).\
   If you don't do this & the parent view group is a List, it will be **destroyed**.
6. Pass you condition to evaluate whether to show the Ad or not (Default is Null).

---
**All other methods:**

* `@Nullable getAdView()`: Returns the underlying `AdView`, can be `@null` if called before `loadAdView()`.

* `isLoading(): Boolean`: Returns `true` if the Ad is currently loading, `false` otherwise.

* `isAdLoaded(): Boolean`: Returns `true` if the Ad is loaded, `false` otherwise.

* `isVisible(): Boolean`: Returns `true` if the Ad is loaded, not null & visible, `false` otherwise.

* `getAdSize()`: Returns current `adSize`.

* `getAdUnitId()`: Returns current `adUnitId`.

* `setAdListener(listener: AdListener)`: Use the AdView's [`AdListener`](https://developers.google.com/android/reference/com/google/android/gms/ads/AdListener).

* `@Nullable getAdListener()`: Returns the Listener if set, null otherwise.

* `removeAd()`: Removes the AdView. Make sure to call `loadAdView()` to re-add `AdView`.

* `resumeAd()`: Resumes AdView **(Handled automatically)**

* `pauseAd()`: Pauses AdView **(Handled automatically)**

* `destroyAd()`: Destroys AdView **(Handled automatically)**
