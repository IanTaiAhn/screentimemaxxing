# BrainRot RPG — Brainrot Ad Intervention Feature

A self-contained feature branch. Complete tasks in order — each step is independently testable before moving to the next.

---

## Overview

**What this feature adds:**

- A "Brainrot Intervention" mechanic that fires an interstitial ad when the player's Brainrot hours crosses a new whole-hour threshold AND Brainrot exceeds both Mid and Enrichment hours
- Maximum one ad per calendar day, regardless of how many thresholds are crossed
- The ad is framed narratively — presented as the algorithm serving you content, not a cold interruption
- Built on **Google AdMob** interstitial ads
- Ad fires at a natural transition point (app open / screen resume) to comply with Google Play ad policy

**Condition summary:**
```
brainrotHours > midHours
AND brainrotHours > enrichmentHours
AND floor(brainrotHours) > floor(previousBrainrotHours)   ← crossed a new whole hour
AND lastAdShownDate != today
```

**Narrative framing:**

The ad is preceded by a brief in-game overlay that reads like a system message from the algorithm — something like *"The feed has noticed you. Enjoy this message from our sponsors."* After the ad completes, the overlay dismisses and the player returns to the game normally. This makes the ad feel like a designed game moment rather than an external interruption.

> ⚠️ **Google Play Policy Note**
> Play policy ([Interstitial ads guidelines](https://support.google.com/admob/answer/6201362)) prohibits ads that appear unexpectedly or interrupt users mid-task. This implementation complies by:
> - Only firing on app open / `onResume`, never mid-session
> - Showing a full-screen in-game overlay *before* the ad so the transition is not abrupt
> - Never blocking navigation or back-press before the ad loads

---

## Tech Stack Addition

- **Google AdMob SDK** — `com.google.android.gms:play-services-ads`
- AdMob account required — register at [admob.google.com](https://admob.google.com)
- Uses **Interstitial Ad** format (full-screen, closes after completion)
- Test ad unit IDs used during development; replace with real IDs before release

---

## Phase 17: Data Layer

### Task 17.1 — Add Ad Tracking Fields to `PlayerStats`

Two new fields are needed: the last brainrot whole-hour threshold that triggered an ad check, and the date the last ad was shown.

Update `PlayerStats.kt`:

```kotlin
@Entity(tableName = "player_stats")
data class PlayerStats(
    // ... existing fields ...
    val lastAdThresholdHour: Int = 0,       // NEW — last whole brainrot hour that was checked
    val lastAdShownDateEpochDay: Long = -1L  // NEW — LocalDate.toEpochDay() of last ad shown
)
```

Add migration columns to a new `MIGRATION_3_4` in `DatabaseProvider.kt`:

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lastAdThresholdHour INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE player_stats ADD COLUMN lastAdShownDateEpochDay INTEGER NOT NULL DEFAULT -1"
        )
    }
}
```

Bump `AppDatabase` version to 4 and register the migration.

**Verification:** App launches without schema crash. Both columns default correctly on existing installs.

---

### Task 17.2 — Create `BrainrotAdEngine`

Create `app/src/main/java/com/brainrotrpg/BrainrotAdEngine.kt`:

```kotlin
package com.brainrotrpg

import java.time.LocalDate

object BrainrotAdEngine {

    /**
     * Returns true if an ad should be shown given the current player state.
     *
     * Conditions (all must be true):
     * 1. Brainrot hours exceeds both Mid and Enrichment hours
     * 2. Brainrot has crossed a new whole-hour threshold since the last check
     * 3. No ad has been shown today (calendar day, device local time)
     */
    fun shouldShowAd(
        brainrotHours: Float,
        midHours: Float,
        enrichmentHours: Float,
        lastAdThresholdHour: Int,
        lastAdShownDateEpochDay: Long,
        today: LocalDate = LocalDate.now()
    ): Boolean {
        // Condition 1: Brainrot must dominate both other categories
        if (brainrotHours <= midHours || brainrotHours <= enrichmentHours) return false

        // Condition 2: Must have crossed a new whole-hour boundary
        val currentThresholdHour = brainrotHours.toInt()
        if (currentThresholdHour <= lastAdThresholdHour) return false

        // Condition 3: Must not have already shown an ad today
        if (lastAdShownDateEpochDay == today.toEpochDay()) return false

        return true
    }

    /**
     * Returns the updated PlayerStats after an ad has been shown.
     * Call this immediately after the ad is displayed.
     */
    fun recordAdShown(
        stats: PlayerStats,
        today: LocalDate = LocalDate.now()
    ): PlayerStats {
        return stats.copy(
            lastAdThresholdHour = stats.brainrotHours.toInt(),
            lastAdShownDateEpochDay = today.toEpochDay()
        )
    }

    /**
     * Returns the updated PlayerStats after a threshold check with no ad shown.
     * Advances the threshold marker so the same hour doesn't re-trigger.
     */
    fun recordThresholdChecked(stats: PlayerStats): PlayerStats {
        return stats.copy(
            lastAdThresholdHour = stats.brainrotHours.toInt()
        )
    }
}
```

**Verification:** Unit test all three conditions independently — dominant category check, threshold crossing check, daily cap check.

---

## Phase 18: AdMob Integration

### Task 18.1 — Add AdMob Dependency

Add to `app/build.gradle.kts`:

```kotlin
implementation("com.google.android.gms:play-services-ads:23.0.0")
```

Add your AdMob App ID to `AndroidManifest.xml` inside `<application>`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
```

> ⚠️ Use your real AdMob App ID here — the app will crash on launch without it.
> During development, use the AdMob test App ID: `ca-app-pub-3940256099942544~3347511713`

Initialize the SDK once in `BrainRotApp.kt`:

```kotlin
import com.google.android.gms.ads.MobileAds

class BrainRotApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        WorkScheduler.schedulePeriodicTracking(this)
    }
}
```

**Verification:** App builds and launches. No AdMob initialization crash.

---

### Task 18.2 — Create `BrainrotAdManager`

Create `app/src/main/java/com/brainrotrpg/BrainrotAdManager.kt`:

This class handles loading and showing the interstitial ad. It is instantiated once and held at the screen level (not in a ViewModel — AdMob ad objects must not outlive the Activity).

```kotlin
package com.brainrotrpg

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class BrainrotAdManager {

    companion object {
        private const val TAG = "BrainrotAdManager"

        // Replace with your real interstitial ad unit ID before release
        // Test ID: ca-app-pub-3940256099942544/1033173712
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    }

    private var interstitialAd: InterstitialAd? = null
    var isAdReady: Boolean = false
        private set

    /** Pre-load the ad so it's ready to show instantly when needed. */
    fun loadAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdReady = true
                    Log.d(TAG, "Interstitial ad loaded.")
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdReady = false
                    Log.w(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Show the interstitial ad.
     * [onAdDismissed] is called when the ad closes (whether completed or failed).
     * If the ad is not ready, [onAdDismissed] is called immediately so the flow continues.
     */
    fun showAd(activity: Activity, onAdDismissed: () -> Unit) {
        val ad = interstitialAd
        if (ad == null) {
            Log.w(TAG, "Ad not ready — skipping.")
            onAdDismissed()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                isAdReady = false
                onAdDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                isAdReady = false
                Log.w(TAG, "Ad failed to show: ${error.message}")
                onAdDismissed()
            }
        }
        ad.show(activity)
    }

    fun destroy() {
        interstitialAd = null
        isAdReady = false
    }
}
```

**Verification:** Ad loads in test mode. Logcat shows "Interstitial ad loaded." on app launch.

---

## Phase 19: UI — Intervention Overlay & Flow

### Task 19.1 — Create the Narrative Intervention Overlay

This full-screen composable is shown *before* the ad fires. It frames the ad as an in-game event rather than an external interruption.

Create `app/src/main/java/com/brainrotrpg/BrainrotInterventionOverlay.kt`:

```kotlin
package com.brainrotrpg

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun BrainrotInterventionOverlay(
    onProceed: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-proceed after 3 seconds — gives the player time to read the message
    // but does not let them skip it (they tap nothing; it advances on its own)
    LaunchedEffect(Unit) {
        delay(3_000L)
        onProceed()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val glitchAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glitchAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "📡",
                style = MaterialTheme.typography.displayLarge
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "INCOMING TRANSMISSION",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF00FF88).copy(alpha = glitchAlpha),
                textAlign = TextAlign.Center,
                letterSpacing = androidx.compose.ui.unit.TextUnit(
                    4f, androidx.compose.ui.unit.TextUnitType.Sp
                )
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "The algorithm has noticed you.\nEnjoy this message from our sponsors.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
```

**Verification:** Overlay renders correctly. Auto-advances after 3 seconds without any user input.

---

### Task 19.2 — Wire the Ad Flow into `AvatarScreen`

The full sequence when an ad is due:

```
AvatarScreen detects pendingAd = true (from ViewModel)
       ↓
Show BrainrotInterventionOverlay (3 second countdown)
       ↓
Overlay auto-dismisses → fire interstitial ad
       ↓
Ad dismissed → ViewModel records ad shown → pendingAd = false
       ↓
Player returns to normal AvatarScreen
```

Add ad state to `AvatarViewModel` (or a thin wrapper). The ViewModel exposes a `pendingAd: Boolean` derived from `PlayerStats`, and an `onAdShown()` function that writes the updated stats.

Add to `AvatarViewModel.kt`:

```kotlin
val pendingAd: StateFlow<Boolean> = playerStatsDao.observeStats()
    .map { stats ->
        stats != null && BrainrotAdEngine.shouldShowAd(
            brainrotHours = stats.brainrotHours,
            midHours = stats.midHours,
            enrichmentHours = stats.enrichmentHours,
            lastAdThresholdHour = stats.lastAdThresholdHour,
            lastAdShownDateEpochDay = stats.lastAdShownDateEpochDay
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

fun onAdShown() {
    viewModelScope.launch {
        val stats = playerStatsDao.getStats() ?: return@launch
        playerStatsDao.upsert(BrainrotAdEngine.recordAdShown(stats))
    }
}
```

Add to `AvatarScreen.kt`:

```kotlin
val pendingAd by avatarViewModel.pendingAd.collectAsStateWithLifecycle()
var showInterventionOverlay by remember { mutableStateOf(false) }
val context = LocalContext.current

// Detect when an ad becomes due
LaunchedEffect(pendingAd) {
    if (pendingAd) showInterventionOverlay = true
}

// Load the ad manager once, scoped to this screen
val adManager = remember { BrainrotAdManager() }
val activity = context as? Activity

LaunchedEffect(Unit) {
    adManager.loadAd(context)
}

DisposableEffect(Unit) {
    onDispose { adManager.destroy() }
}

if (showInterventionOverlay) {
    BrainrotInterventionOverlay(
        onProceed = {
            showInterventionOverlay = false
            activity?.let {
                adManager.showAd(it) {
                    avatarViewModel.onAdShown()
                    // Preload the next ad in the background
                    adManager.loadAd(context)
                }
            }
        }
    )
} else {
    AvatarScreenContent(/* ... existing params ... */)
}
```

**Verification:** With `shouldShowAd` returning true (inject via fake stats), confirm: overlay appears → auto-advances → ad fires → `onAdShown()` is called → `pendingAd` returns false → avatar screen resumes.

---

## Phase 20: Polish & Testing

### Task 20.1 — Add Strings to `strings.xml`

```xml
<string name="intervention_transmission">INCOMING TRANSMISSION</string>
<string name="intervention_body">The algorithm has noticed you.\nEnjoy this message from our sponsors.</string>
```

---

### Task 20.2 — Unit Tests for `BrainrotAdEngine`

Add tests for:

- `shouldShowAd` returns false when Brainrot does not exceed Mid
- `shouldShowAd` returns false when Brainrot does not exceed Enrichment
- `shouldShowAd` returns false when no new whole-hour threshold has been crossed
- `shouldShowAd` returns false when an ad has already been shown today
- `shouldShowAd` returns true when all four conditions are met
- `recordAdShown` correctly sets `lastAdShownDateEpochDay` to today
- `recordAdShown` correctly advances `lastAdThresholdHour`
- `recordThresholdChecked` advances `lastAdThresholdHour` without touching the date

---

### Task 20.3 — End-to-End Test (Test Ads Only)

> Never test with real ad unit IDs. Always use AdMob test IDs during development.

1. Set player stats so Brainrot > Mid and Brainrot > Enrichment
2. Set `lastAdThresholdHour` to 2 and `brainrotHours` to 3.5 (threshold of 3 not yet seen)
3. Open the app — confirm intervention overlay appears
4. Wait 3 seconds — confirm ad fires (test ad banner should appear)
5. Dismiss the ad — confirm avatar screen resumes normally
6. Repeat steps 2–5 on the same calendar day — confirm no second ad fires
7. Set `lastAdShownDateEpochDay` to yesterday — confirm ad fires again

---

### Task 20.4 — Before Release Checklist

- [ ] Replace test AdMob App ID with real App ID in `AndroidManifest.xml`
- [ ] Replace test interstitial ad unit ID with real unit ID in `BrainrotAdManager`
- [ ] Verify AdMob account is active and ad unit is approved
- [ ] Confirm the intervention overlay auto-advances and cannot be dismissed early
- [ ] Confirm the ad cannot be triggered more than once per calendar day
- [ ] Review [AdMob Interstitial Best Practices](https://support.google.com/admob/answer/6201362) before submission

---

## Feature Complete Checklist

Before merging this feature, confirm:

- [ ] AdMob SDK initializes without crash
- [ ] `BrainrotAdEngine.shouldShowAd` correctly evaluates all four conditions
- [ ] Database migration runs without crashing on existing installs
- [ ] Ad fires only when Brainrot exceeds both Mid and Enrichment
- [ ] Ad fires only when a new whole brainrot hour is crossed
- [ ] Ad fires at most once per calendar day
- [ ] Intervention overlay appears before the ad and auto-advances after 3 seconds
- [ ] Player cannot dismiss the overlay early
- [ ] Avatar screen resumes correctly after the ad closes
- [ ] Ad failure (no fill, load error) is handled gracefully — game continues normally
- [ ] `onAdShown()` is called exactly once per ad shown
- [ ] Next ad is preloaded after each showing
- [ ] Test ad IDs are replaced with real IDs before release
- [ ] All unit tests pass
