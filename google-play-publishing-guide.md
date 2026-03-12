# 🧟 BrainRot RPG — Google Play Store Publishing Guide

A step-by-step checklist for getting the app live. Work through these in order.

---

## Step 1 — Prepare Your App for Release

- [ ] **Generate a signed release build**
  - In Android Studio: **Build → Generate Signed Bundle / APK**
  - Choose **Android App Bundle (.aab)** — this is what Play Store requires
  - Create a new keystore file when prompted
  - ⚠️ Save your keystore file and passwords somewhere safe forever — you can never change it once published, and losing it means you can never update the app

- [ ] **Enable minification in your release build**
  - In `app/build.gradle.kts`, set `isMinifyEnabled = true` in the `release` block
  - Rebuild and make sure the app still runs correctly

- [ ] **Confirm `USE_MOCK_DATA` is `false` in the release build**
  - Already set correctly in your `build.gradle.kts` — just double-check before submitting

- [ ] **Clean up the stub `UsageTrackingService`**
  - The service in `UsageTrackingService.kt` is empty — either implement it or remove it and its manifest entry to avoid confusion during review

- [ ] **Remove all debug logs**
  - Search for `Log.d` calls across the codebase and remove them before release

- [ ] **Create an app icon**
  - Play Store requires a 512×512px PNG icon
  - In Android Studio: right-click `res/` → **New → Image Asset** to generate all sizes
  - The launcher icon is currently the default Android robot

---

## Step 2 — Create Your Google Play Developer Account

- [ ] Go to [play.google.com/console](https://play.google.com/console) and sign in with a Google account
- [ ] Pay the **one-time $25 registration fee**
- [ ] Fill out your developer profile (name, email, website optional)
- [ ] Verify your identity (Google will ask for ID verification — takes 1–2 days)

---

## Step 3 — Create the App Listing in Play Console

- [ ] Click **"Create app"** in the Play Console dashboard
- [ ] Set:
  - App name: `BrainRot RPG`
  - Default language: English
  - App or game: **Game**
  - Free or paid: **Free**
- [ ] Agree to the Play Developer Distribution Agreement

---

## Step 4 — Prepare Your Store Listing Assets

You'll need all of these before you can publish:

- [ ] **Short description** — 80 characters max. Example: *"Your phone usage shapes your avatar. The brainrot is the point."*
- [ ] **Full description** — up to 4,000 characters. Explain the concept, the avatar classes, how it works. You can pull from the README.
- [ ] **App icon** — 512×512px PNG (no alpha/transparency in the background)
- [ ] **Feature graphic** — 1024×500px JPG or PNG. This is the banner shown at the top of your store page.
- [ ] **Screenshots** — minimum 2, up to 8. Capture the permission screen and the avatar screen from the emulator.
  - In Android Studio emulator: use the camera icon in the emulator toolbar to take screenshots

---

## Step 5 — Fill Out the Content Rating Questionnaire

- [ ] In Play Console, go to **Policy → App content → Content rating**
- [ ] Complete the IARC questionnaire
- [ ] For BrainRot RPG, select **Everyone** — there's no violence, mature content, or user communication
- [ ] Submit to receive your rating

---

## Step 6 — Complete the Data Safety Form ⚠️ (Most Important Step)

This is required because you use `PACKAGE_USAGE_STATS`. Be accurate — Google reviews this.

- [ ] Go to **Policy → App content → Data safety**
- [ ] Answer the questions as follows:
  - **Does your app collect or share user data?** → Yes
  - **Data type collected:** App activity (app usage history)
  - **Is it shared with third parties?** → No
  - **Is it used for tracking/advertising?** → No
  - **Is it encrypted in transit?** → N/A (data never leaves the device)
  - **Can users request deletion?** → Yes (uninstalling the app deletes all Room data)
- [ ] Submit the form

---

## Step 7 — Submit the Permissions Declaration

Because `PACKAGE_USAGE_STATS` is a restricted permission, Google requires a manual declaration explaining why you need it.

- [ ] Go to **Policy → App content → Permissions**
- [ ] Find `PACKAGE_USAGE_STATS` in the list
- [ ] Write a clear justification. Example:

  > *"This permission is required to read per-app foreground usage time via UsageStatsManager. The app is an idle RPG where the user's real phone usage determines their avatar's appearance and XP. App usage data is read locally, never transmitted off the device, and is the core mechanic of the game. Without this permission, the app cannot function."*

- [ ] Submit for review — this may take a few extra days compared to a standard app

---

## Step 8 — Write a Privacy Policy

Required because you access usage stats. It doesn't need to be fancy.

- [ ] Write a simple privacy policy that states:
  - What data is collected (app usage time, per-category)
  - Where it's stored (locally on device only)
  - That it is never transmitted or shared
  - How users can delete it (uninstall the app)
- [ ] Host it somewhere publicly accessible. Free options:
  - A GitHub Gist (set it to public)
  - A free Google Sites page
  - A simple GitHub Pages site
- [ ] Paste the URL into **App content → Privacy policy** in Play Console

---

## Step 9 — Upload Your App Bundle

- [ ] In Play Console, go to **Release → Production → Create new release**
- [ ] Upload the `.aab` file generated in Step 1
- [ ] Write release notes (what's new) — for a first release, something like: *"Initial release of BrainRot RPG."*
- [ ] Click **Save** then **Review release**

---

## Step 10 — Submit for Review

- [ ] Review the pre-launch checklist Play Console shows you — fix any warnings flagged as errors
- [ ] Click **Start rollout to Production**
- [ ] Wait for Google's review — typically **3–7 days** for a first submission
- [ ] If rejected, Play Console will tell you exactly why — address the feedback and resubmit

---

## After Publishing

- Your app will appear in search results within a few hours of approval
- You can update the app at any time by uploading a new `.aab` with an incremented `versionCode` in `build.gradle.kts`
- Monitor for crashes in **Android Vitals** inside Play Console — it shows real crash reports from users

---

## Quick Reference — Things You Cannot Change After Publishing

| Thing | Why it matters |
|---|---|
| Your keystore file | Required to sign every future update |
| Package name (`com.brainrotrpg`) | Permanently tied to your Play listing |
| Whether the app is free or paid | Free apps cannot be switched to paid |

---

*Total mandatory cost: $25 one-time. No ongoing fees for a free app with no backend.*
