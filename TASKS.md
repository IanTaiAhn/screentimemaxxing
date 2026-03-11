# BrainRot RPG — MVP Tasks

This file is the source of truth for Claude Code. Work through tasks in order.
Each task is scoped to be completable in one focused session.
Mark tasks `[x]` when complete. Do not skip tasks — later tasks depend on earlier ones.

---

## Phase 1: Project Setup

- [x] **Task 1.1 — Initialize Android Project**
  - Create a new Android project in Android Studio
  - Package name: `com.brainrotrpg`
  - Min SDK: API 29 (Android 10)
  - Language: Kotlin
  - Build system: Gradle (Kotlin DSL)
  - Enable Jetpack Compose in `build.gradle.kts`

- [x] **Task 1.2 — Add Dependencies**
  - Add to `build.gradle.kts`:
    - `androidx.room` (runtime, ktx, compiler)
    - `androidx.work` (WorkManager runtime-ktx)
    - `androidx.lifecycle` (viewmodel-compose, runtime-compose)
    - `kotlinx.coroutines` (android)
  - Sync and confirm build passes with no errors

- [x] **Task 1.3 — Configure Permissions in AndroidManifest**
  - Add `PACKAGE_USAGE_STATS` permission
  - Add `FOREGROUND_SERVICE` permission
  - Add `RECEIVE_BOOT_COMPLETED` permission
  - Register a `BootReceiver` placeholder in the manifest
  - Register a `UsageTrackingService` placeholder in the manifest

---

## Phase 2: Usage Tracking

- [x] **Task 2.1 — Permission Check & Deep Link**
  - Create `UsagePermissionHelper.kt`
  - Function: `hasUsagePermission(context): Boolean` — checks if `PACKAGE_USAGE_STATS` is granted
  - Function: `openUsageAccessSettings(context)` — deep links to Settings > Special App Access > Usage Access
  - Write a unit test confirming the permission check returns false when not granted

- [x] **Task 2.2 — App Category Map**
  - Create `AppCategories.kt`
  - Define a `Category` enum: `BRAINROT`, `MID`, `ENRICHMENT`, `UNTRACKED`
  - Define a `packageCategoryMap: Map<String, Category>` with at least:
    - TikTok, Instagram → BRAINROT
    - YouTube, Reddit, Twitter/X → MID
    - Spotify, Audible, Google Podcasts → ENRICHMENT
  - Write a unit test confirming known packages return the correct category

- [x] **Task 2.3 — UsageStatsManager Reader**
  - Create `UsageStatsReader.kt`
  - Function: `getUsageSince(context, sinceMillis): Map<String, Long>`
    - Returns map of packageName → foregroundTimeMillis for all apps used since `sinceMillis`
  - Function: `getCategorizedUsage(context, sinceMillis): Map<Category, Long>`
    - Calls `getUsageSince`, maps packages through `AppCategories`, sums by category
  - Handle the case where permission is not granted (return empty map, log warning)

- [x] **Task 2.4 — Mock Data Layer**
  - Create `MockUsageStatsReader.kt`
  - Implements the same interface as `UsageStatsReader`
  - Returns hardcoded fake data (e.g. 3 hours TikTok, 1 hour Spotify)
  - Controlled by a `BuildConfig.USE_MOCK_DATA` flag in `build.gradle.kts`
  - All subsequent tasks should work with mock data during development

---

## Phase 3: Local Database (Room)

- [x] **Task 3.1 — Define Room Entities**
  - Create `UsageRecord.kt` — entity with fields: `id`, `timestamp`, `category`, `durationMillis`
  - Create `PlayerStats.kt` — entity with fields: `id` (singleton, always 1), `totalXp`, `level`, `brainrotHours`, `midHours`, `enrichmentHours`, `lastCheckedTimestamp`

- [x] **Task 3.2 — Create DAOs**
  - Create `UsageRecordDao.kt`
    - `insert(record: UsageRecord)`
    - `getRecordsSince(timestamp: Long): List<UsageRecord>`
    - `deleteOlderThan(timestamp: Long)` (keep DB clean)
  - Create `PlayerStatsDao.kt`
    - `getStats(): PlayerStats?`
    - `upsert(stats: PlayerStats)`

- [ ] **Task 3.3 — Build the Database**
  - Create `AppDatabase.kt` — `@Database` class with both entities
  - Create `DatabaseProvider.kt` — singleton that provides the database instance
  - Confirm Room compiles with no errors (annotation processing)

---

## Phase 4: Background Worker

- [x] **Task 4.1 — Create UsageTrackingWorker**
  - Create `UsageTrackingWorker.kt` extending `CoroutineWorker`
  - On each run:
    1. Read `lastCheckedTimestamp` from `PlayerStats`
    2. Call `UsageStatsReader.getCategorizedUsage(since = lastCheckedTimestamp)`
    3. Insert a `UsageRecord` for each category with duration
    4. Update `PlayerStats` — add hours, recalculate XP, update timestamp
  - Return `Result.success()` on completion, `Result.retry()` on failure

- [x] **Task 4.2 — XP & Level Calculation**
  - Create `XpEngine.kt`
  - Function: `calculateXp(brainrotHours, midHours, enrichmentHours): Long`
    - All hours contribute equally to XP (1 hour = 100 XP)
  - Function: `calculateLevel(totalXp): Int`
    - Simple threshold table: Level 1 = 0 XP, Level 2 = 500 XP, Level 3 = 1200 XP, etc.
  - Write unit tests for both functions with known inputs/outputs

- [x] **Task 4.3 — Schedule WorkManager Job**
  - Create `WorkScheduler.kt`
  - Function: `schedulePeriodicTracking(context)`
    - Uses `PeriodicWorkRequestBuilder` with 15-minute interval
    - Sets `ExistingPeriodicWorkPolicy.KEEP` (don't restart if already scheduled)
  - Call `schedulePeriodicTracking` from `Application.onCreate()`
  - Create `Application` subclass and register it in `AndroidManifest.xml`

- [x] **Task 4.4 — Boot Receiver**
  - Create `BootReceiver.kt` extending `BroadcastReceiver`
  - On `BOOT_COMPLETED` intent, call `WorkScheduler.schedulePeriodicTracking(context)`
  - Register in manifest with `RECEIVE_BOOT_COMPLETED` intent filter
  - Confirm tracking resumes after a simulated reboot in the emulator

---

## Phase 5: Avatar System

- [x] **Task 5.1 — Define Avatar States**
  - Create `AvatarState.kt`
  - Define a sealed class or enum with states:
    - `SigmaZombie` — dominant BRAINROT (>60% of hours)
    - `ExtremelyOnline` — dominant MID (>60% of hours)
    - `FakeIntellectual` — dominant ENRICHMENT (>60% of hours)
    - `Hybrid` — no dominant category
  - Function: `resolveAvatarState(brainrotHours, midHours, enrichmentHours): AvatarState`
  - Write unit tests for each state resolution path

- [x] **Task 5.2 — Avatar Visual Assets (Placeholder)**
  - Create 4 placeholder avatar images (simple colored rectangles or basic illustrations)
    - One per avatar state, clearly labeled
  - Place in `res/drawable/`
  - These are replaced with real art post-MVP — placeholders are fine for now

- [x] **Task 5.3 — AvatarViewModel**
  - Create `AvatarViewModel.kt` extending `ViewModel`
  - Expose a `StateFlow<AvatarUiState>` where `AvatarUiState` contains:
    - `level: Int`
    - `totalXp: Long`
    - `xpToNextLevel: Long`
    - `avatarState: AvatarState`
    - `brainrotHours: Float`
    - `midHours: Float`
    - `enrichmentHours: Float`
  - Pulls live data from `PlayerStatsDao` via a coroutine flow
  - Maps `PlayerStats` → `AvatarUiState` using `XpEngine` and `AvatarState`

---

## Phase 6: UI (Jetpack Compose)

- [ ] **Task 6.1 — Permission Gate Screen**
  - Create `PermissionScreen.kt` composable
  - Shows when `hasUsagePermission()` returns false
  - Displays a brief explanation of why the permission is needed
  - Has a single CTA button: "Enable Usage Access" → calls `openUsageAccessSettings()`
  - On return from settings, re-checks permission and navigates to main screen if granted

- [ ] **Task 6.2 — Main Avatar Screen**
  - Create `AvatarScreen.kt` composable
  - Consumes `AvatarViewModel`
  - Layout:
    - Avatar image (swaps based on `AvatarState`)
    - Level badge
    - XP progress bar (current XP / XP to next level)
    - Avatar class name label (e.g. "Sigma Zombie")
  - Use `collectAsStateWithLifecycle()` to observe the `StateFlow`

- [ ] **Task 6.3 — Stats Breakdown Row**
  - Add a stats section below the avatar on `AvatarScreen`
  - Shows three stat bars or labels:
    - 🧟 Brainrot hours
    - 😐 Mid hours
    - 🎧 Enrichment hours
  - Values pulled from `AvatarUiState`
  - Keep it minimal — numbers + simple progress indicators

- [ ] **Task 6.4 — Navigation & Entry Point**
  - Set up `NavHost` in `MainActivity.kt` with two destinations:
    - `"permission"` → `PermissionScreen`
    - `"avatar"` → `AvatarScreen`
  - On app launch, check permission and route accordingly
  - Confirm full flow works: launch → permission screen → grant → avatar screen

---

## Phase 7: Polish & Testing

- [ ] **Task 7.1 — End-to-End Mock Data Test**
  - With `USE_MOCK_DATA = true`, run the full app in the emulator
  - Confirm: mock data flows through Worker → Room → ViewModel → UI
  - Confirm: avatar state resolves correctly for mock data values
  - Confirm: XP bar and level display correctly

- [ ] **Task 7.2 — End-to-End Real Data Test**
  - Switch `USE_MOCK_DATA = false`
  - Install on emulator or physical device
  - Grant `PACKAGE_USAGE_STATS` permission
  - Use a few apps inside the emulator, wait for WorkManager to fire (or trigger manually)
  - Confirm real usage data flows through and updates the UI

- [ ] **Task 7.3 — Edge Cases**
  - Test: app launched with permission already granted (should skip permission screen)
  - Test: permission revoked mid-session (should handle gracefully, not crash)
  - Test: device rebooted (WorkManager should reschedule via BootReceiver)
  - Test: no tracked apps used since last check (should not crash, no-op update)

- [ ] **Task 7.4 — Basic UI Cleanup**
  - Apply a consistent dark theme (brainrot aesthetic — dark background, neon or muted accent)
  - Confirm no hardcoded strings (move to `strings.xml`)
  - Confirm no hardcoded colors (move to `Color.kt` / theme)
  - Remove all debug logs before considering MVP done

---

## MVP Complete Checklist

Before calling this MVP done, confirm all of the following:

- [ ] App installs and launches without crashing
- [ ] Permission screen shows correctly when permission is missing
- [ ] Permission deep link works and app detects grant on return
- [ ] Background Worker runs and updates Room database
- [ ] Avatar state resolves correctly based on usage mix
- [ ] XP and level update based on total hours
- [ ] All four avatar states are reachable
- [ ] App survives device reboot (WorkManager reschedules)
- [ ] Mock data mode works for development/testing
- [ ] No crashes on any edge case listed in Task 7.3

---

## Out of Scope for MVP

Do not build these during MVP — save for v2:

- Social features or leaderboards
- Push notifications
- Avatar animations (static images only for MVP)
- More than 4 avatar states
- Onboarding tutorial
- Settings screen
- App icon (use default for now)
- Play Store submission
