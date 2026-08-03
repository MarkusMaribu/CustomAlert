# CustomAlert

Android app that plays custom notification sounds for any app — either for all of that app’s notifications, or when the notification text matches a rule (for example, contains “Banana”).

## Requirements

- Android 8.0 (API 26)+
- Notification access permission
- Optional but recommended: unrestricted battery usage

## Setup in the app

1. Grant **notification access** to CustomAlert.
2. Allow **post notifications** (Android 13+).
3. Set battery usage to **Unrestricted**.
4. Keep **Monitoring** on (shows a silent persistent notification).

## Smoke-test checklist

1. Enable notification access and monitoring.
2. Set a default sound for an app and trigger a notification from it.
3. Add a rule containing a unique word; confirm only matching notifications use that sound.
4. Turn the screen off, wait 1–2 minutes, trigger another notification — sound should still play.
5. Confirm the monitoring notification is silent (no sound/vibration).
6. Import a custom `.ogg` / `.mp3` from storage and preview it.
7. Toggle **Prefer replace when possible** and compare behavior on a simple notification.

## Build

Open the project in Android Studio and sync Gradle, or:

```bash
./gradlew :app:assembleDebug
```
