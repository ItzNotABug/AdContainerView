# AdContainerView

AdContainerView is a simple, lifecycle aware wrapper over Google AdMob's AdView (Banner Ad) which handles most of the stuff by itself.

For the simplest use:\
You just need to add `AdContainerView` in your layout, define `adUnitId`, `adSize` & **that's it!**\
`AdContainerView` hooks to your Activity's lifecycle process & handles AdView's lifecycle (Resume, Pause, Destroy Ad).\
This is most helpful when you just want to add a simple Banner Ad without any boilerplate.

## Adding in your project
**Gradle**
```gradle
implementation 'com.lazygeniouz:acv:0.1.2'
```
**Maven**
```xml
<dependency>
  <groupId>com.lazygeniouz</groupId>
  <artifactId>acv</artifactId>
  <version>0.1.1</version>
  <type>pom</type>
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
*   `acv_autoLoad`:\
    Default Value is `false`\
    If `true`, the AdView will be loaded as soon as the Activity is created.

*   `acv_adSize`:\
    Default Value is `ADAPTIVE` which equals to `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize()`.\
    Define an AdSize for the the AdView, check [AdSize](https://developers.google.com/android/reference/com/google/android/gms/ads/AdSize#field-summary).

*   `acv_adUnitId`:\
    Define an `adUnitId` for the Banner AdView from your AdMob dashboard.

### Programmatically
```kotlin
val adContainerView = AdContainerView(this@MainActivity)
adContainerView.insertAdView(adUnitId, adSize, adRequest)
parentLayout.removeAllViews()
parentLayout.addView(adContainerView)
// And thats it!
```
**All Methods:**
*   `insertAdView(@NonNull adUnitId, adSize, adRequest)`:\
    The arguments `adSize` & `adRequest` are optional\
    The default value of parameter `adSize` is `ADAPTIVE`.\
    Pass your `AdRequest` if you have a customized request.
    
*   `isLoading(): Boolean`: Returns `true` if the Ad is currently loading, `false` otherwise.

*   `isVisible(): Boolean`: Returns `true` if the Ad is not null & visible, `false` otherwise.

*   `getAdUnitId()`: Returns current `adUnitId`.

*   `getAdSize()`: Returns current `adSize`.

*   `setAdListener(listener: AdListener)`: You can use the AdView's default [`AdListener`](https://developers.google.com/android/reference/com/google/android/gms/ads/AdListener)\

*   `removeAd()`: Removes the AdView. Make sure to call `insertAdView()` to re-add `AdView`.

*   `resumeAd()`: Resumes AdView **(Handled automatically)**

*   `pauseAd()`: Pauses AdView **(Handled automatically)**

*   `destroyAd()`: Destroys AdView **(Handled automatically)**
