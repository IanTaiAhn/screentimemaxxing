# BrainRot RPG — Google Play Publishing Checklist

## 1. Code & Build

- [ ] Remove all `Log.d` / `Log.w` calls from production code (check `UsageTrackingWorker`, `UsageStatsReader`, `BrainrotAdManager`)
- [ ] Confirm no `UsageTrackingService` stub is registered in `AndroidManifest.xml` that has no implementation (remove or implement)
- [ ] Generate a signed release `.aab` via **Build → Generate Signed Bundle / APK** in Android Studio
- [ ] Create and securely store your keystore file and passwords — you cannot change or recover this later
- [ ] Confirm `isMinifyEnabled = true` in the release build type (already set — just verify the build works)
- [ ] Confirm `USE_MOCK_DATA = false` in the release build (already configured — double-check the final `.aab`)
- [ ] Run a full end-to-end test with a real device and `USE_MOCK_DATA = false` to confirm real `UsageStatsManager` data flows correctly

## 2. Manual Testing (Unchecked in TASKS.md)

- [ ] **Task 7.1** — End-to-end mock data test: mock data → Worker → Room → ViewModel → UI
- [ ] **Task 7.2** — End-to-end real data test: grant permission, use apps, wait for WorkManager, confirm UI updates
- [ ] **Task 7.3** — Edge cases:
  - App launched with permission already granted (skips permission screen)
  - Permission revoked mid-session (no crash)
  - Device rebooted (WorkManager reschedules via BootReceiver)
  - No tracked apps used since last check (no-op, no crash)
- [ ] Lifecycle system end-to-end: trigger completion, navigate end screen, begin new life, view archive
- [ ] Shop & room objects: purchase, place, activate, remove with refund
- [ ] Navigation: back stack, device rotation, force-kill recovery

## 3. Privacy Policy

- [ ] Replace `[Your email address]` placeholder in `PRIVACY_POLICY.md`
- [ ] Replace `[your-username]` GitHub placeholder in `PRIVACY_POLICY.md`
- [ ] Host the privacy policy at a publicly accessible URL. Free options:
  - GitHub Gist (set to public)
  - GitHub Pages
  - Google Sites
- [ ] Paste the hosted URL into **App content → Privacy policy** in Play Console

## 4. Store Assets

None of these exist yet — all are required before submission.

- [ ] **App icon** — 512×512px PNG, no transparency in background (currently the default Android robot)
  - Generate all sizes via Android Studio: right-click `res/` → New → Image Asset
- [ ] **Feature graphic** — 1024×500px JPG or PNG (banner shown at top of store page)
- [ ] **Screenshots** — minimum 2, up to 8; capture from Pixel 6 emulator at 1080×2400
  - Suggested: permission screen, avatar screen, lifecycle end screen, archive screen
- [ ] **Short description** — 80 characters max (draft in `docs/BrainRotRPG_ASO_Guide.md`)
- [ ] **Full description** — up to 4,000 characters (draft in `docs/BrainRotRPG_ASO_Guide.md`)

## 5. Google Play Console Account

- [ ] Register at [play.google.com/console](https://play.google.com/console) — one-time $25 fee
- [ ] Complete identity verification — allow 1–2 days
- [ ] Create the app listing: Game → Role Playing, Free

## 6. Play Console Policy Forms

- [ ] **Content rating** — complete the IARC questionnaire; select "Everyone"
- [ ] **Data Safety form** — answer as follows:
  - Collects data: Yes (app activity / app usage history)
  - Shared with third parties: No
  - Used for tracking/advertising: No
  - Encrypted in transit: N/A (data never leaves device)
  - Users can request deletion: Yes (uninstall deletes all Room data)
- [ ] **Permissions declaration** — write a justification for `PACKAGE_USAGE_STATS`. Suggested text:
  > *"This permission is required to read per-app foreground usage time via UsageStatsManager. The app is an idle RPG where the user's real phone usage determines their avatar's appearance and XP. App usage data is processed locally, never transmitted off the device, and is the core mechanic of the game. Without this permission, the app cannot function."*

## 7. Upload & Submit

- [ ] Go to **Release → Production → Create new release** in Play Console
- [ ] Upload the signed `.aab`
- [ ] Write release notes (e.g. *"Initial release of BrainRot RPG."*)
- [ ] Review Play Console's pre-launch checklist and fix any errors
- [ ] Submit for review — expect **3–7 days**, longer if `PACKAGE_USAGE_STATS` triggers extra manual review

---

## Critical Path (Do These First)

The `PACKAGE_USAGE_STATS` permission triggers manual review by Google, which is your longest wait. Start the Play Console account and permissions declaration early.

1. Fix privacy policy placeholders → host it
2. Register Play Console account → verify identity (1–2 days)
3. Generate keystore → build signed `.aab`
4. Create store assets (icon, screenshots, feature graphic)
5. Complete policy forms including permissions declaration
6. Upload and submit

---

## Things That Are Already Done ✓

- `isMinifyEnabled = true` in release build
- `USE_MOCK_DATA = false` in release build
- `PACKAGE_USAGE_STATS` permission in manifest with correct `tools:ignore`
- `BootReceiver` registered for post-reboot WorkManager scheduling
- Privacy policy content written (just needs placeholders filled and hosting)
- ASO copy drafted in `docs/BrainRotRPG_ASO_Guide.md`
- All unit and instrumented tests written and passing
