# BrainRot RPG — Codebase Overview

## Data Pipeline

The core loop runs on a 15-minute WorkManager schedule (`WorkScheduler` → `UsageTrackingWorker`). Each cycle it:

1. Reads `lastCheckedTimestamp` from the Room database
2. Queries `UsageStatsManager` for per-app foreground time since that timestamp (or uses `MockUsageStatsReader` in debug builds, which hardcodes 3h TikTok + 1h Spotify + 30min YouTube)
3. Maps package names to categories via `AppCategories.kt`:
   - TikTok/Instagram → `BRAINROT`
   - YouTube/Reddit/Twitter → `MID`
   - Spotify/Audible/Podcasts → `ENRICHMENT`
4. Writes `UsageRecord` entries to Room, then updates the singleton `PlayerStats` row
5. Checks for any active `RoomObject` boosts and applies their XP multipliers before writing

---

## XP & Leveling

`XpEngine` is simple: **1 hour = 100 XP**, all categories equal. Level thresholds are a hardcoded list:

| Level | XP Required |
|-------|-------------|
| 1     | 0           |
| 2     | 500         |
| 3     | 1,200       |
| 4     | 2,000       |
| 5     | 3,000       |
| 6     | 4,500       |
| 7     | 6,500       |
| 8     | 9,000       |
| 9     | 12,000      |
| 10    | 16,000      |

Room objects can multiply XP per category — they stack multiplicatively.

---

## Avatar State

`resolveAvatarState()` looks at the ratio of cumulative hours. If any single category exceeds **60%** of total hours, you get that class. Otherwise you're Hybrid.

| Class | Condition |
|-------|-----------|
| `SigmaZombie` | BRAINROT > 60% of total hours |
| `ExtremelyOnline` | MID > 60% of total hours |
| `FakeIntellectual` | ENRICHMENT > 60% of total hours |
| `Hybrid` | No category dominates |

This updates reactively via a `StateFlow` in `AvatarViewModel`.

---

## Two Hour Pools

`PlayerStats` tracks hours in two separate ways:

- **Cumulative hours** — used only for avatar class resolution, never decremented
- **Spendable hours** — a currency that gets deducted when you buy room objects from the shop

---

## Room Scene & Interaction

`RoomScene.kt` is the most complex file — a `Canvas`-based isometric room drawn entirely in code (no sprites). It:

- Draws a floor diamond, two walls, and a couch using `Path` and `drawIsometricBox`
- Populates category items (little colored cubes) at predefined positions based on hour thresholds — more hours = more clutter
- Draws `RoomObject` placements with active/dormant visual states (glow ring vs. grey dot)
- Handles tap input via `screenToWorld` / `worldToScreen` coordinate conversion between screen pixels and normalized (0–1) isometric world space
- Runs a `withFrameMillis` physics loop for character movement — the character walks toward tapped floor positions and has a breathing animation when idle

---

## Shop & Room Objects

`RoomObjectType` is an enum where each entry encodes its own cost, cost category, and XP multipliers. The full catalogue:

| Object | Cost | Effect |
|--------|------|--------|
| 🏋️ Weights | 2h Enrichment | +15% Enrichment XP |
| 📚 Bookshelf | 3h Enrichment | +25% Enrichment XP |
| 🖥️ Second Monitor | 2h Mid | +20% Mid XP |
| 🍕 Pizza Tower | 2h Brainrot | +20% Brainrot XP |
| ⚡ Energy Fridge | 3h Brainrot | +15% Brainrot & Mid XP, -10% Enrichment XP |
| 🎧 Headphone Stand | 2h Enrichment | +20% Enrichment XP |

Players spend their spendable hours in `ShopSheet` (a `ModalBottomSheet`), enter placement mode, tap the floor to place the object, then later tap it in-world to activate a **4-hour boost window**. Tapping an active object shows an info dialog with time remaining and a 50%-refund removal option.

---

## Database

Room with two migrations supported. Three tables:

- **`usage_records`** — raw per-cycle log entries
- **`player_stats`** — singleton row (`id = 1` always)
- **`room_objects`** — placed objects with position and activation state

The `AppDatabase` is a singleton via `DatabaseProvider`.

---

## Build Config

Debug builds set `USE_MOCK_DATA = true`, which swaps in `MockUsageStatsReader` via `UsageStatsReaderProvider`. This means the whole pipeline can be tested without a real device or real usage stats.