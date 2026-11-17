# Spend Nudge

A bank SMS arrives, you get a notification that says "₹450 spent", you tap it
and Spending Tracker opens, ready for you to log it.

- Listens only for incoming SMS (`RECEIVE_SMS`). It never reads your inbox.
- Nothing runs in the background. Android wakes the app only when an SMS lands.
- No internet permission. Nothing leaves the phone.
- No third-party libraries. Plain Java and the Android framework.

## Download

| | |
|---|---|
| **Release APK** | [latest release](https://github.com/AbhishekR14/Spend-Nudge/releases/latest) |
| **Build of every push** | [Build APK runs](https://github.com/AbhishekR14/Spend-Nudge/actions/workflows/build.yml) → open a run → artifact **spend-nudge-apk** |

A release APK downloads without a GitHub login. Run artifacts need one, and are
kept for 90 days. Both are debug-signed, which is all a sideloaded personal app
needs.

Pushing a `v*` tag builds and publishes a release automatically.

## Install on the phone

1. Open the APK and allow installing unknown apps when asked.
2. Open Spend Nudge and tap **Grant permissions**.
3. If the SMS permission is greyed out, tap **Open app info**, then the three
   dot menu, then **Allow restricted settings**, and grant it again. Android
   blocks SMS permissions for sideloaded apps until you do this.
4. Paste one of your real bank SMS into the test box and tap **Test**.

## What a tap opens, and why it is the main screen

Spending Tracker's "Add Expense" shortcut points at
`com.mhriley.spendingtracker.TransactionActivity` carrying
`transaction_type=expense`, and that activity is not exported. Android lets only
Spending Tracker itself start it. No third-party app can open that form, so a
tap opens the app's main screen and the expense form is one tap away on
**+ Expense**.

The app still looks for a way in, because it costs nothing and would work the
moment the tracker exported that screen, or with another tracker that does:

- It reads the tracker's launcher shortcuts out of its own
  `res/xml/shortcuts.xml` and reuses the exact intent the launcher would send,
  extras included. Firing a shortcut directly is not possible;
  `LauncherApps.startShortcut` is reserved for the active home screen launcher.
- A shortcut may declare several intents, so each is tried in turn, nearest the
  target first, skipping any screen that is not exported.
- It also offers every exported activity.

**Choose what a tap opens** lets you pin any of these, and marks the ones
Android will not allow. **What Spending Tracker publishes** prints what was
found, so a wrong guess can be told from a screen that is out of reach.

## Why nothing is copied

Earlier versions copied the amount to the clipboard. Android 13 and up shows its
own copy confirmation at the bottom of the screen, directly over the tracker's
**+ Expense** button, and an app cannot move or suppress it.

So the amount is not copied. Instead a nudge stays in the notification shade
after you tap it, so the amount is there to read while you type it in. Swipe it
away when you are done.

## Tests

`./gradlew test` runs the JVM tests in `app/src/test/java/com/ranjan/spendnudge/`
with no device needed, and CI runs them before building the APK.

- `TxnParserTest` covers the sample bank SMS, the messages that must be ignored,
  sender filtering, and amount formatting.
- `TrackerLauncherTest` covers which names count as an expense screen.

## Your bank's format is not detected?

Edit `app/src/main/java/com/ranjan/spendnudge/TxnParser.java`, the only file that
parses anything, add a case to `TxnParserTest`, and rebuild.

## Build it yourself

Open the folder in Android Studio and use Build → Build APK(s), or run
`./gradlew assembleDebug`. Needs JDK 17, Android SDK 35 and build-tools 35.0.0.
The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.
