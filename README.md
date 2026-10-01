# Sweep — Photo & Storage Cleaner (Android)

Sweep is a fully offline Android app that finds the junk taking up space on a
phone and lets you delete it in a couple of taps. Everything runs on-device —
no accounts, no uploads, no backend.

It scans for five things:

| Bucket | How it works |
| --- | --- |
| **Duplicates** | Groups files with identical size, then confirms with a SHA-256 content hash. |
| **Similar photos** | A 64-bit perceptual hash (dHash) clusters near-identical shots. |
| **Screenshots** | Matches the `Screenshots` bucket or filename pattern. |
| **Blurry photos** | Variance-of-Laplacian score below a threshold. |
| **Large files** | Any image or video over 5 MB. |

Deleting uses the Android 11+ system delete request
(`MediaStore.createDeleteRequest`), so the OS shows its own confirmation and the
app never needs broad write access. On Android 10 and below it falls back to a
direct delete.

## Tech

- Kotlin, Jetpack Compose (Material 3), single-activity
- `MediaStore` for all media access — no third-party gallery library
- Coil for thumbnail loading
- Coroutines + `StateFlow` (MVVM)
- Google Play Billing 6.x for the subscription
- AdMob (banner + interstitial) for the free tier

## Project layout

```
app/src/main/java/com/mindhex/sweep/
  SweepApp.kt            Application — initialises Ads
  MainActivity.kt        Hosts Compose, permissions, delete flow
  data/
    MediaModels.kt       Category, MediaItem, MediaGroup, CategoryResult
    ImageAnalysis.kt     dHash + blur scoring
    MediaRepository.kt   MediaStore queries and bucketing
    ScanViewModel.kt     UI state + scan orchestration
  billing/BillingManager.kt   Play Billing wrapper
  ads/AdsManager.kt           AdMob wrapper
  ui/
    AppRoot.kt           Tiny state-based navigation
    HomeScreen.kt        Scan + summary
    CategoryScreen.kt    Grid, multi-select, delete
    PaywallScreen.kt     Subscription screen
    theme/Theme.kt       Material 3 colours
    Format.kt            Byte formatting
```

## Build and run

1. Open the project folder in **Android Studio** (Ladybug or newer).
2. Let Gradle sync. If the wrapper JAR is missing, Android Studio will offer to
   generate it — or run `gradle wrapper` once from the project root.
3. Run on a device or emulator with photos on it (the emulator ships with a few
   sample images).
4. To build a release APK/AAB: **Build → Generate Signed App Bundle / APK**.

The app builds against `compileSdk 34`, `minSdk 24`, using AGP 8.6.1 and
Kotlin 2.0.20.

## Wiring up monetization

Everything is stubbed with **Google's official test IDs** so the app runs safely
out of the box. Before you publish, replace these:

**AdMob** — in `ads/AdsManager.kt`:
```kotlin
const val BANNER_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
const val INTERSTITIAL_UNIT_ID = "ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
```
and in `AndroidManifest.xml`, the `com.google.android.gms.ads.APPLICATION_ID`
meta-data value.

**Subscription** — create an auto-renewing subscription in Play Console with the
product ID `sweep_pro_monthly` (or change `SUBSCRIPTION_ID` in
`billing/BillingManager.kt` to match yours), then set the same price in
`PaywallScreen.kt`.

Two things to know before you rely on this for real revenue:

- The current entitlement check is **client-side only**. For production, verify
  purchases and grant entitlements from a backend so they can't be spoofed.
- Play requires a privacy policy and a data-safety declaration. Sweep collects
  nothing, which makes both easy — but you still have to file them.

## Suggested next steps

- Add a "before / after" space-saved summary and a weekly reminder notification.
- Adaptive launcher icon (currently a plain vector drawable).
- On-device thumbnail cache so re-scans feel instant.
- Optional: detect "similar videos" and burst photos.
