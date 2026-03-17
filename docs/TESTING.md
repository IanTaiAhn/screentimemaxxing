# BrainRot RPG — Testing Guide

A complete reference for testing every layer of the app, from pure logic to manual UI flows.

---

## Table of Contents

1. [Test Infrastructure Overview](#1-test-infrastructure-overview)
2. [Unit Tests (Local JVM)](#2-unit-tests-local-jvm)
3. [Instrumented Tests (Device / Emulator)](#3-instrumented-tests-device--emulator)
4. [Manual Testing — Lifecycle System](#4-manual-testing--lifecycle-system)
5. [Manual Testing — Avatar & XP Progression](#5-manual-testing--avatar--xp-progression)
6. [Manual Testing — Room Objects & Shop](#6-manual-testing--room-objects--shop)
7. [Manual Testing — Background Worker](#7-manual-testing--background-worker)
8. [Manual Testing — Permission Flow](#8-manual-testing--permission-flow)
9. [Manual Testing — Navigation & Edge Cases](#9-manual-testing--navigation--edge-cases)
10. [What the Tests Do NOT Cover](#10-what-the-tests-do-not-cover)
11. [Recommended Testing Sequence](#11-recommended-testing-sequence)

---

## 1. Test Infrastructure Overview

The project has three testing layers:

| Layer | Location | Runs On | Command |
|---|---|---|---|
| Unit tests | `app/src/test/` | Local JVM (no device needed) | `./gradlew test` |
| Instrumented tests | `app/src/androidTest/` | Device or emulator | `./gradlew connectedAndroidTest` |
| Manual tests | n/a | Emulator or physical device | Launch app from Android Studio |

**Mock data is enabled by default in debug builds.** `BuildConfig.USE_MOCK_DATA = true` means the app uses `MockUsageStatsReader` instead of the real `UsageStatsManager`. This gives a fixed simulated usage of 3h TikTok (Brainrot), 1h Spotify (Enrichment), and 30min YouTube (Mid) per worker cycle — which is ideal for testing without needing real phone usage history.

---

## 2. Unit Tests (Local JVM)

These tests run entirely on the JVM with no Android device required. Run all of them with:

```bash
./gradlew test
```

Or run a single test class in Android Studio by clicking the green play button next to the class or method name.

---

### 2.1 `XpEngineTest`

**File:** `app/src/test/java/com/brainrotrpg/XpEngineTest.kt`

Tests the core XP and levelling math.

| Test | What It Checks |
|---|---|
| `calculateXp returns 0 for all zero hours` | No hours = no XP |
| `calculateXp returns 100 for one hour total` | 1h of any category = 100 XP |
| `calculateXp sums all categories equally` | All categories contribute equally to raw XP |
| `calculateXp handles fractional hours` | 0.5h = 50 XP |
| `calculateLevel returns 1 for 0 XP` | Starting state is Level 1 |
| `calculateLevel returns 2 at 500 XP` | Level 2 threshold is correct |
| `calculateLevel returns max level for very high XP` | Level 10 at 16,000+ XP |
| `calculateLevel returns correct level just below threshold` | Boundary conditions — 1199 XP = Level 2, not Level 3 |

**What to watch for:** If you change the `LEVEL_THRESHOLDS` array in `XpEngine.kt`, these tests will fail and need updating. That's intentional — they act as a guard against accidental threshold changes.

---

### 2.2 `AvatarStateTest`

**File:** `app/src/test/java/com/brainrotrpg/AvatarStateTest.kt`

Tests the avatar classification logic.

| Test | What It Checks |
|---|---|
| SigmaZombie when brainrot exceeds 60% | Dominant brainrot path |
| ExtremelyOnline when mid exceeds 60% | Dominant mid path |
| FakeIntellectual when enrichment exceeds 60% | Dominant enrichment path |
| Hybrid for no dominant category | Balanced usage falls through to Hybrid |
| Hybrid when total hours is zero | Zero-state defaults correctly |
| Hybrid when brainrot is exactly 60% | Boundary: `> 0.6f` not `>= 0.6f` |

**Key boundary to understand:** The threshold is *strictly greater than* 60%. A player sitting at exactly 60% brainrot is still classified as Hybrid. This is tested explicitly in `resolveAvatarState returns Hybrid when brainrot is exactly 60 percent`.

---

### 2.3 `AppCategoriesTest`

**File:** `app/src/test/java/com/brainrotrpg/AppCategoriesTest.kt`

Tests that every tracked package name maps to the correct category.

| Package | Expected Category |
|---|---|
| `com.zhiliaoapp.musically` | BRAINROT |
| `com.instagram.android` | BRAINROT |
| `com.google.android.youtube` | MID |
| `com.twitter.android` | MID |
| `com.reddit.frontpage` | MID |
| `com.spotify.music` | ENRICHMENT |
| `com.audible.application` | ENRICHMENT |
| `com.google.android.apps.podcasts` | ENRICHMENT |
| `com.unknown.app` | UNTRACKED |

If you add new apps to `packageCategoryMap` in `AppCategories.kt`, add a corresponding test here.

---

### 2.4 `UsageStatsReaderTest`

**File:** `app/src/test/java/com/brainrotrpg/UsageStatsReaderTest.kt`

Tests the aggregation logic that converts raw per-package usage into per-category totals.

| Test | What It Checks |
|---|---|
| Empty input → empty output | No crashes on zero data |
| Brainrot apps aggregate correctly | TikTok + Instagram durations sum under BRAINROT |
| Mid apps aggregate correctly | YouTube + Twitter + Reddit sum under MID |
| Enrichment apps aggregate correctly | Spotify + Audible + Podcasts sum under ENRICHMENT |
| Unknown packages go to UNTRACKED | No data is silently dropped |
| Mixed categories aggregate correctly | Multiple categories in one call work simultaneously |

Note: These tests call `UsageStatsReader.aggregateCategorizedUsage()` directly — they do not invoke the Android `UsageStatsManager` API, so no device is needed.

---

### 2.5 `UsagePermissionHelperTest`

**File:** `app/src/test/java/com/brainrotrpg/UsagePermissionHelperTest.kt`

Tests the permission mode interpretation logic.

| Test | What It Checks |
|---|---|
| `MODE_ALLOWED` → `true` | Permission granted is detected correctly |
| `MODE_DEFAULT`, `MODE_IGNORED`, `MODE_ERRORED` → `false` | All non-allowed modes are rejected |

---

### 2.6 `LifecycleEngineTest`

**File:** `app/src/test/java/com/brainrotrpg/LifecycleEngineTest.kt`

The most comprehensive unit test file. Tests lifecycle completion detection, record building, stat resetting, and outcome resolution.

#### Lifecycle Completion

| Test | What It Checks |
|---|---|
| `isLifecycleComplete` false below 16,000 XP | Threshold not triggered early |
| `isLifecycleComplete` true at exactly 16,000 XP | Exact threshold fires |
| `isLifecycleComplete` true above 16,000 XP | Above-threshold fires |

#### Outcome Resolution

| Test | What It Checks |
|---|---|
| GRADUATE when enrichment > 60% | Enrichment dominant path |
| DIE when brainrot > 60% | Brainrot dominant path |
| ASCEND for hybrid usage | No dominant category |
| ASCEND when mid > 60% | Mid has no special outcome — falls to ASCEND |
| Exactly 60% enrichment → ASCEND (not GRADUATE) | Strict boundary check |
| Exactly 60% brainrot → ASCEND (not DIE) | Strict boundary check |
| Enrichment takes precedence when both high | GRADUATE wins over DIE if enrichment checked first |

#### Record Building (`buildRecord`)

| Test | What It Checks |
|---|---|
| All fields populated from PlayerStats | lifecycle number, XP, level, hours, timestamps |
| GRADUATE outcome for enrichment-heavy stats | Correct outcome written to record |
| ASCEND outcome for hybrid stats | Correct outcome written to record |
| totalHours = sum of all category hours | Derived field is correct |

#### Stat Reset (`resetStats`)

| Test | What It Checks |
|---|---|
| All XP and hours zeroed | Clean slate after reset |
| lifecycleNumber incremented | Life counter increments correctly |
| `pendingLifecycleEnd` set to false | Flag cleared on reset |
| `lifecycleStartedAt` updated to `now` | New life timestamp is correct |
| Singleton id preserved | DB row id stays as 1 |

#### Dominant Category (on `LifecycleRecord`)

| Test | What It Checks |
|---|---|
| Zero hours → "None" | Handles zero-state correctly |
| Each category dominant at 70% | All three dominant paths return correct label |
| Hybrid when no category dominates | Falls through correctly |
| Exactly 60% → "Hybrid" | Boundary is consistent with AvatarState logic |

---

## 3. Instrumented Tests (Device / Emulator)

These tests run on a real Android device or emulator. They test Room database operations end-to-end.

**To run:** Connect an emulator or device, then in Android Studio right-click `LifecycleEndToEndTest.kt` and select **Run**. Or run from the terminal:

```bash
./gradlew connectedAndroidTest
```

---

### 3.1 `LifecycleEndToEndTest`

**File:** `app/src/androidTest/java/com/brainrotrpg/LifecycleEndToEndTest.kt`

Uses an in-memory Room database so it does not affect your real app data. Tests the complete lifecycle flow from detection through to archive.

| Step | Test Method | What It Verifies |
|---|---|---|
| 1 | `step1_isLifecycleComplete_triggers_at_threshold` | Engine fires at exactly 16,000 XP |
| 2 | `step2_lifecycleRecord_is_written_to_db_with_correct_outcome` | Record written with correct outcome, hours, lifecycle number |
| 3 | `step3_pendingLifecycleEnd_is_true_after_completion` | PlayerStats flag set correctly |
| 4 | `step4_app_relaunch_detects_pendingLifecycleEnd` | Simulates cold relaunch routing to end screen |
| 5 | `step5_end_screen_record_matches_db_record` | Most recent record available for end screen display |
| 6 | `step6_beginNewLife_resets_stats_and_clears_objects` | Stats zeroed, room objects deleted |
| 7 | `step7_new_life_starts_at_level_1_with_incremented_lifecycle_number` | New life is Level 1, lifecycle number + 1 |
| 8 | `step8_archive_shows_completed_life` | Single completed life appears in archive |
| 9 | `step9_two_lifecycles_appear_in_archive_reverse_chronological` | Two lives ordered newest-first (lifecycle 2 then lifecycle 1) |

---

## 4. Manual Testing — Lifecycle System

Automated tests cover the logic and database layer but not the actual navigation flow. Manual testing is required for the UI transitions.

### Setup: Lower the XP Threshold

Edit `LifecycleEngine.kt` temporarily:

```kotlin
// Change this:
private const val LIFECYCLE_XP_THRESHOLD = 16_000L
// To this:
private const val LIFECYCLE_XP_THRESHOLD = 500L
```

With mock data enabled (debug build), each WorkManager cycle adds roughly 450 XP (4.5 total hours × 100 XP/hour). Two worker cycles will trigger lifecycle completion at a 500 XP threshold.

> **Remember to revert this before building a release.**

### Force a WorkManager Run

WorkManager runs on a 15-minute interval by default. To trigger it immediately during testing, add a temporary debug button in `AvatarScreen.kt`:

```kotlin
// Temporary debug trigger — remove before release
Button(onClick = {
    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequestBuilder<UsageTrackingWorker>().build()
    )
}) {
    Text("Force Sync")
}
```

### Lifecycle Manual Test Checklist

| # | Action | Expected Result |
|---|---|---|
| L1 | Launch app, force two worker syncs | App navigates away from avatar screen to Lifecycle End screen |
| L2 | Lifecycle End screen appears | Shows correct emoji, outcome name, and description matching usage mix |
| L3 | Stats summary card on end screen | Hours for each category are non-zero and add up correctly |
| L4 | Tap "View Archive" | Navigates to Archive screen showing one completed life card |
| L5 | Archive card content | Shows lifecycle number, outcome emoji, dominant category, total hours, final level |
| L6 | Press back from Archive | Returns to Lifecycle End screen (not avatar screen) |
| L7 | Tap "Begin New Life" | Navigates to avatar screen, all stats reset to Level 1 / 0 XP |
| L8 | Force another two worker syncs | Second lifecycle completes |
| L9 | View Archive after second life | Two cards shown, most recent life listed first |
| L10 | Force-kill app while `pendingLifecycleEnd = true`, relaunch | App resumes directly on Lifecycle End screen (not avatar screen) |

---

## 5. Manual Testing — Avatar & XP Progression

### Visual State Testing via Compose Previews

The fastest way to verify avatar visuals without running the full app is to add `@Preview` composables directly in `AvatarScreen.kt`. Android Studio renders these in the preview pane without a device.

Add one preview per avatar class:

```kotlin
@Preview(showBackground = true)
@Composable
private fun SigmaZombiePreview() {
    BrainRotRPGTheme {
        AvatarScreenContent(uiState = AvatarUiState(
            level = 5, totalXp = 3000L, xpToNextLevel = 1500L,
            xpProgressFraction = 0.5f,
            avatarState = AvatarState.SigmaZombie,
            brainrotHours = 80f, midHours = 10f, enrichmentHours = 10f
        ))
    }
}

@Preview(showBackground = true)
@Composable
private fun FakeIntellectualPreview() {
    BrainRotRPGTheme {
        AvatarScreenContent(uiState = AvatarUiState(
            level = 8, totalXp = 9000L, xpToNextLevel = 3000L,
            xpProgressFraction = 0.33f,
            avatarState = AvatarState.FakeIntellectual,
            brainrotHours = 10f, midHours = 10f, enrichmentHours = 80f
        ))
    }
}
```

### XP & Avatar Manual Test Checklist

| # | Action | Expected Result |
|---|---|---|
| A1 | Launch fresh app (clear data first) | Level 1, 0/500 XP, Hybrid class |
| A2 | Force one worker sync with mock data | XP increases, progress bar moves |
| A3 | XP bar fills past Level 2 threshold (500 XP) | Level badge updates to Level 2 |
| A4 | Check stats breakdown section | All three category bars show proportional fill |
| A5 | Brainrot hours dominate (>60%) | Class name reads "Sigma Zombie" |
| A6 | Enrichment hours dominate (>60%) | Class name reads "Fake Intellectual" |
| A7 | No category dominates | Class name reads "Hybrid" |
| A8 | Room scene at low hours | Sparse items in room, few clutter objects |
| A9 | Force many worker syncs | More items appear in room as hours accumulate |
| A10 | Character tap on floor | Character walks to tapped position |

### Note on Avatar Visuals

The four SVG drawable files (`avatar_sigma_zombie.xml`, `avatar_extremely_online.xml`, etc.) are present in `res/drawable/` but are **not currently loaded** in `RoomScene.kt` or `AvatarScreen.kt`. The visual differentiation between avatar classes at MVP is currently the room clutter items and the class label text only. The character sprite itself does not change between classes. This is expected for MVP — verify that the label text and room scene items update correctly rather than looking for distinct character art.

---

## 6. Manual Testing — Room Objects & Shop

### Shop & Placement Manual Test Checklist

| # | Action | Expected Result |
|---|---|---|
| S1 | Tap 🛒 FAB with zero spendable hours | Shop opens, all items show red cost text and disabled Place buttons |
| S2 | Accumulate enrichment hours via worker sync | Enrichment-cost items (Bookshelf, Weights, Headphone Stand) become affordable |
| S3 | Tap Place on an affordable item | Shop closes, placement mode banner appears at top of screen |
| S4 | Tap the floor in placement mode | Object appears at tapped position in the room scene |
| S5 | Spendable hours deducted after purchase | Hours in shop header reduced by item cost |
| S6 | Tap an inactive placed object | Object activates (glow ring appears) |
| S7 | Tap an active placed object | Inspection dialog opens showing time remaining |
| S8 | Tap "Remove (50% refund)" in inspection dialog | Object removed from scene, half cost refunded to spendable hours |
| S9 | Tap outside shop sheet | Sheet dismisses without entering placement mode |
| S10 | Tap floor without placement mode active | Character walks to tapped location |
| S11 | Place multiple objects of different types | All appear in scene simultaneously, each with correct emoji color |

---

## 7. Manual Testing — Background Worker

The `UsageTrackingWorker` is the core data pipeline. It cannot be unit tested meaningfully because WorkManager's scheduler does not run in test environments.

### Worker Manual Test Checklist

| # | Action | Expected Result |
|---|---|---|
| W1 | Force worker sync in debug build | XP and hours update in UI within a few seconds |
| W2 | Check Room DB directly via Database Inspector | `player_stats` row updated with new XP, hours, timestamp |
| W3 | Check `usage_records` table | New rows inserted for each category with correct timestamps |
| W4 | Force worker sync twice back-to-back | XP accumulates (hours are added, not replaced) |
| W5 | Place and activate a room object, force sync | XP multiplier from object is reflected in XP gain |
| W6 | Force-kill app, wait 15+ minutes, relaunch | Worker has run in background, XP has increased |
| W7 | Reboot emulator, wait 15+ minutes, relaunch | `BootReceiver` re-registered work, XP has increased |

**How to use Database Inspector:**
In Android Studio, go to **View → Tool Windows → App Inspection**, select your running app, and open the Database Inspector tab. You can browse and query the `player_stats`, `usage_records`, `room_objects`, and `lifecycle_records` tables in real time.

---

## 8. Manual Testing — Permission Flow

| # | Action | Expected Result |
|---|---|---|
| P1 | Fresh install, launch app | Permission screen shown (not avatar screen) |
| P2 | Tap "Enable Usage Access" | Navigates to Android Settings → Usage Access |
| P3 | Grant permission in Settings, return to app | App automatically detects permission and navigates to avatar screen |
| P4 | Deny permission, return to app | Permission screen still shown |
| P5 | Grant permission, force-kill app, relaunch | Goes directly to avatar screen (permission persists) |
| P6 | Revoke permission in Settings while app is in background, bring app to foreground | Behavior depends on your `ON_RESUME` check — app should re-show permission screen or degrade gracefully |

---

## 9. Manual Testing — Navigation & Edge Cases

| # | Action | Expected Result |
|---|---|---|
| N1 | Tap 📜 FAB from avatar screen | Navigates to Archive screen |
| N2 | Tap back arrow in Archive | Returns to avatar screen |
| N3 | Tap "View Archive" from Lifecycle End screen | Navigates to Archive (back returns to Lifecycle End) |
| N4 | Rotate device on avatar screen | State preserved (hours, level, class name all intact) |
| N5 | Rotate device on lifecycle end screen | Record still displayed correctly |
| N6 | Force-kill app mid-placement mode, relaunch | No crash, placement mode not persisted (expected) |
| N7 | Begin New Life with room objects placed | Objects cleared from scene and database |
| N8 | Begin New Life, force worker sync immediately | New lifecycle's XP accumulates from 0, not from old value |
| N9 | Multiple worker syncs before lifecycle end | `pendingLifecycleEnd` only set once, duplicate lifecycle records not created |

---

## 10. What the Tests Do NOT Cover

Be aware of the following gaps. These require manual verification or future test additions.

| Gap | Reason | How to Verify |
|---|---|---|
| `UsageTrackingWorker` end-to-end | WorkManager doesn't run in unit tests | Manual testing with debug button |
| Avatar sprite rendering per class | No visual regression testing in project | Compose previews + manual inspection |
| Navigation transitions | No Espresso/Compose UI tests written | Manual navigation checklist above |
| `UsageTrackingService` implementation | Service body is empty stub | Not applicable until implemented |
| Real `UsageStatsManager` data | Mock data used in debug; real data only on physical device | Test on physical Android device |
| Object active timer expiry | 4-hour active window hard to reproduce | Set `activeDurationMs` to a small value temporarily for testing |
| Concurrent WorkManager + UI updates | Race conditions not tested | Stress test with rapid force-sync presses |
| XP multipliers with stacked objects | Worker logic tested manually only | Place multiple active objects, verify XP gain increases |

---

## 11. Recommended Testing Sequence

For a complete test pass before a release build, work through this order:

1. **Run unit tests** — `./gradlew test`. All tests should pass with no failures.
2. **Run instrumented tests** — `./gradlew connectedAndroidTest` on an emulator (Pixel 6, API 33+). All 9 steps in `LifecycleEndToEndTest` should pass.
3. **Permission flow** — Fresh install, verify permission gate works (checklist section 8).
4. **XP & avatar progression** — Force worker syncs, verify XP increases and avatar class changes (checklist section 5).
5. **Room shop & objects** — Purchase, place, activate, and remove objects (checklist section 6).
6. **Lifecycle end-to-end** — Lower the threshold, trigger lifecycle completion, navigate through all screens (checklist section 4).
7. **Archive** — Verify two completed lives appear in reverse order.
8. **Navigation edge cases** — Device rotation, back stack, force-kill recovery (checklist section 9).
9. **Restore the XP threshold** — Set `LIFECYCLE_XP_THRESHOLD` back to `16_000L` before building release.
10. **Build release APK** and verify `USE_MOCK_DATA = false` in the release variant before submitting.
