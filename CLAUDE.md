# BrainRot RPG — Project Brief

## Concept Overview

An Android app that runs passively in the background and tracks per-app screen time usage. The more you use your phone, the more experience your in-game avatar earns. The *type* of app you use shapes what your avatar looks like — not just how fast it levels up. Heavy TikTok/Reels usage produces a visually degraded, glazed zombie-like character. Heavy Spotify/Audiobook usage produces a sharper, more "cultured" aesthetic. The avatar is a mirror of your media diet.

The core loop is intentionally guilt-free — it leans into brainrot behavior rather than fighting it, making the user *want* to keep their screen on to progress.

---

## Core Mechanics

### XP & Leveling
- **Total screen time = XP** (the raw fuel for progression)
- Every minute of screen-on time in a tracked app contributes to the overall XP bar
- Levels unlock new avatar customization states and visual evolutions

### Avatar Appearance (App Category System)
- App mix determines *what the avatar looks like* at each level
- Two users at the same level but different app diets should look completely different
- Visual states update dynamically as usage thresholds shift over time

### App Categories & Tiers

| Tier | Apps | Avatar Effect |
|------|------|---------------|
| Brainrot | TikTok, Instagram Reels, YouTube Shorts | Glazed eyes, slouched posture, trending drip, phone permanently in hand |
| Mid | YouTube (long form), Twitter/X, Reddit | Neutral baseline, "extremely online" aesthetic |
| Enrichment | Spotify, Podcasts, Audible, Apple Books | Sharper look, genre-specific accessories (headphones, coffee, turtleneck) |

### Avatar Class Examples
- **The Sigma Zombie** — 80%+ Brainrot tier usage
- **The Fake Intellectual** — 80%+ Enrichment tier usage
- **The Extremely Online** — heavy Mid tier usage
- **The Hybrid** — balanced mix, unique blended aesthetic

---

## Technical Stack

### Language
- **Kotlin** (native Android — no cross-platform framework)

### Background Tracking
- **WorkManager** — persistent background job, survives OS process kills
- Wakes every 15–30 minutes, reads usage delta, writes to local DB, sleeps
- **UsageStatsManager** — Android API for per-app foreground time
  - Requires `PACKAGE_USAGE_STATS` permission (user must enable manually in Settings)
  - Returns cumulative foreground time per package name

### Local Storage
- **Room** (SQLite wrapper) — stores cumulative per-app hours, updated each WorkManager cycle

### UI / Avatar Rendering
- **Jetpack Compose** for MVP — swappable visual states based on usage thresholds
- Migrate to embedded **Unity** if richer animation/game feel is needed post-MVP

### Data Flow
```
UsageStatsManager
      ↓
WorkManager background job (every 15–30 min)
      ↓
Room Database (cumulative per-category hours)
      ↓
XP + Avatar Engine (maps hours → visual state)
      ↓
Jetpack Compose UI (renders character)
```

### Package Name → Category Map (starter list)
```kotlin
val appCategories = mapOf(
    "com.zhiliaoapp.musically"       to Category.BRAINROT,  // TikTok
    "com.instagram.android"          to Category.BRAINROT,  // Instagram
    "com.google.android.youtube"     to Category.MID,       // YouTube
    "com.twitter.android"            to Category.MID,       // Twitter/X
    "com.reddit.frontpage"           to Category.MID,       // Reddit
    "com.spotify.music"              to Category.ENRICHMENT,
    "com.audible.application"        to Category.ENRICHMENT,
    "com.google.android.apps.podcasts" to Category.ENRICHMENT
)
```

---

## Permissions Required

| Permission | Why |
|------------|-----|
| `PACKAGE_USAGE_STATS` | Read per-app foreground time via UsageStatsManager |
| `FOREGROUND_SERVICE` | Keep background tracking alive |
| `RECEIVE_BOOT_COMPLETED` | Restart tracking job after device reboot |

Note: `PACKAGE_USAGE_STATS` cannot be granted via a normal runtime dialog. The user must be directed to **Settings → Apps → Special App Access → Usage Access** and toggle it on manually. The app should detect if this is missing on launch and deep-link the user to the correct settings screen.

---

## MVP Scope

The minimum viable product to validate the core concept:

- [ ] `UsageStatsManager` reading and logging correctly
- [ ] WorkManager background job running persistently
- [ ] Room storing cumulative per-category hours
- [ ] One avatar with 3–4 distinct visual states that swap at usage thresholds
- [ ] Basic XP bar and level display
- [ ] Mock data layer for testing without real usage history

Everything else (more avatar classes, social features, leaderboards, more app categories) is post-MVP.

---

## Testing Without a Physical Android Device

- Use the **Android Emulator** inside **Android Studio** (free)
- Recommended virtual device: **Pixel 6, Android 13 or 14**
- Apple Silicon Macs run the emulator especially well
- Inject fake usage data via a **mock data layer** during development — don't depend on real emulator usage stats for early testing
- For real permission flow testing, a cheap physical Android device ($30–50, no SIM needed) works as a secondary option

---

## Why iOS Is Not Feasible

- No equivalent to `UsageStatsManager` for third-party apps
- Screen Time API (`FamilyControls` framework) is restricted to parental control use cases only
- Background execution is severely limited — no permitted background mode for passive usage tracking
- No cross-app observation permission exists for third-party developers
- App Store Review would likely reject any workaround attempt under guideline 5.1.2

**Android-only is the correct call for this concept.**

---

## Open Design Questions

- Should YouTube be Mid or split by content type (Shorts = Brainrot, long-form = Mid)?
- Does the avatar degrade visually over time if usage patterns improve, or is it a one-way ratchet?
- Is there a social/competitive layer — e.g. comparing avatar states with friends?
- Should the user be able to see a breakdown of what's driving their avatar's appearance, or is it more impactful if it's opaque?
- Monetization: cosmetic unlocks? Premium avatar classes?
