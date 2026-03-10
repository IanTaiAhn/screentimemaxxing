# 🧟 BrainRot RPG

> *The more you scroll, the more you become.*

---

## What Is BrainRot RPG?

BrainRot RPG is an Android idle RPG that turns your real phone usage into a game — passively, in the background, without you lifting a finger.

Your avatar evolves based on **what you actually do on your phone**. Sink 40 hours into TikTok and Reels? Your character starts looking like it. Spend your time on Spotify and audiobooks? Your avatar reflects that instead. The game doesn't judge you — it just holds up a mirror.

There are no quests to complete. No timers to start. No habits to manually log. Just use your phone like you normally would, and watch your character become a portrait of your media diet.

---

## How It Works

BrainRot RPG runs silently in the background using Android's built-in usage tracking. Every 15–30 minutes it checks which apps you've been using, logs the time, and updates your character accordingly.

**Screen time = XP.** The more hours you sink in, the faster you level up.

**App mix = Appearance.** What you spend those hours on shapes what your avatar looks like.

| What You Use | What You Become |
|---|---|
| TikTok, Instagram Reels, YouTube Shorts | 🧟 Glazed eyes, slouched posture, phone glued to hand |
| YouTube, Reddit, Twitter/X | 😶 Extremely Online — neutral but deeply chronically online |
| Spotify, Podcasts, Audible | 🎧 The Fake Intellectual — headphones, turtleneck, suspiciously good taste |

Mix your usage and get a hybrid. Go full brainrot and own it. The avatar is yours.

---

## Avatar Classes

Each class unlocks based on your dominant usage category over time:

- **The Sigma Zombie** — 80%+ Brainrot tier. Dead eyes. Trending fits. Phone is a limb.
- **The Extremely Online** — Heavy Mid tier. Knows every meme 6 hours before you do.
- **The Fake Intellectual** — 80%+ Enrichment tier. Has opinions about podcasts.
- **The Hybrid** — No dominant category. A chaotic blend of all three.

Classes evolve visually as your hours accumulate. Level 5 Sigma Zombie looks very different from Level 20.

---

## Why Does This Exist?

Every other screen time app wants you to use your phone less. They guilt you, block you, lock you out.

BrainRot RPG doesn't fight the behavior — it gamifies it. The brainrot is the point. The joke is self-aware. The mirror is the mechanic.

---

## Platform

**Android only.** iOS does not expose the background usage tracking APIs needed to make this work passively. This is a deliberate technical constraint, not a roadmap item.

---

## Tech Stack

- **Language:** Kotlin
- **Background Tracking:** WorkManager + UsageStatsManager
- **Database:** Room (SQLite)
- **UI:** Jetpack Compose
- **Min SDK:** Android 10 (API 29)

---

## Project Status

🚧 MVP in development. See `TASKS.md` for current progress.
