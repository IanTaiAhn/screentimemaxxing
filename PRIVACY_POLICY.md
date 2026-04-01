# Privacy Policy for BrainRot RPG

**Last Updated:** March 31, 2026

## Overview

BrainRot RPG is an Android game that tracks your phone usage to create and evolve your in-game avatar. This privacy policy explains what data we collect, how it's used, and how it's stored.

## Data We Collect

BrainRot RPG collects the following data:

- **App usage statistics**: The amount of time you spend in different apps on your device
- **App categories**: Classification of apps into categories (e.g., social media, productivity, entertainment)
- **XP and level progression**: Your in-game progress based on usage time

This data is collected using Android's `PACKAGE_USAGE_STATS` permission, which allows the app to read per-app foreground usage time through the UsageStatsManager API.

## How We Use Your Data

Your usage data is used exclusively for:

- Calculating your in-game experience points (XP)
- Determining your avatar's appearance based on app usage patterns
- Displaying your level and progress within the game

## Data Storage and Security

**All data stays on your device.** We do not:

- Transmit your usage data to any servers
- Share your data with third parties
- Use your data for advertising or tracking
- Store your data in the cloud

All usage statistics are stored locally in a private database on your device using Android's Room (SQLite) framework. No network connections are made to send or receive usage data.

## Data Retention and Deletion

Your data is retained on your device for as long as the app is installed. You can delete all your data at any time by:

- Uninstalling the app (this permanently deletes all locally stored data)
- Clearing the app's storage in Android Settings → Apps → BrainRot RPG → Storage → Clear Data

## Permissions

BrainRot RPG requires the following permissions:

- **Usage Access (`PACKAGE_USAGE_STATS`)**: Required to read per-app screen time. This is the core mechanic of the game.
- **Foreground Service**: Allows background tracking to continue while the app is not actively open.
- **Receive Boot Completed**: Restarts the tracking service after device reboot.

You can revoke the Usage Access permission at any time in Android Settings, though this will prevent the app from functioning.

## Children's Privacy

BrainRot RPG does not knowingly collect data from children under 13. The app is rated for general audiences and does not contain age-restricted content.

## Third-Party Services

BrainRot RPG does not integrate with any third-party analytics, advertising, or tracking services. The app operates entirely offline.

## Changes to This Policy

We may update this privacy policy from time to time. Any changes will be reflected in the "Last Updated" date at the top of this document. Continued use of the app after changes constitutes acceptance of the updated policy.

## Contact

If you have questions about this privacy policy, please contact:

**Email:** [Your email address]
**GitHub:** https://github.com/[your-username]/screentimemaxxing

---

**Summary:** BrainRot RPG tracks your app usage to power an idle game. All data stays on your device, is never shared or transmitted, and can be deleted by uninstalling the app.
