# Spend Nudge

Bank SMS arrives → notification "₹450 spent, log it?" → tap → amount is copied
and Spending Tracker opens → paste into Amount.

- Only listens for incoming SMS (RECEIVE_SMS). Never reads your inbox.
- Nothing runs in the background; Android wakes the app only when an SMS arrives.
- No internet permission. Nothing leaves your phone.

## Download the APK

Every push builds a debug APK in GitHub Actions and attaches it to the run as an
artifact called **spend-nudge-apk**. Open the run, download that artifact, unzip
it, and you have `app-debug.apk`. (Downloading an artifact needs you to be signed
in to GitHub; artifacts are kept for 90 days.)

| | |
|---|---|
| **Latest build** | [Build APK → newest run](https://github.com/AbhishekR14/Spend-Nudge/actions/workflows/build.yml?query=branch%3Aclaude%2Ftest-update-repo-trvo91) |
| **Last verified build** | [run #6](https://github.com/AbhishekR14/Spend-Nudge/actions/runs/44151976025) · commit `db4441d` · 17 Nov 2026 · [direct artifact link](https://github.com/AbhishekR14/Spend-Nudge/actions/runs/44151976025/artifacts/14358972764) |

"Last verified build" is the run where the 20 parser tests and `assembleDebug`
were both confirmed green.

## Build it yourself

**Option A: GitHub (no laptop setup needed)**
Push any change, or open the **Actions** tab → **Build APK** → **Run workflow**.
A run takes about 2 minutes and ends with the APK attached as an artifact.

**Option B: Android Studio**
Open this folder in Android Studio → Build → Build APK(s). The APK lands in
`app/build/outputs/apk/debug/app-debug.apk`.

Requires Android SDK 35 and build-tools 35.0.0; Android Studio offers to install
both on first open.

## Install on the Pixel
1. Open `app-debug.apk` on the phone and allow "Install unknown apps" when asked.
2. Open Spend Nudge → **Grant permissions**.
3. If the SMS permission is blocked/greyed out: tap **Open app info** → ⋮ (top right)
   → **Allow restricted settings** → back → Grant permissions again.
4. Paste one of your real bank SMS into the test box and tap **Test**.

## Which screen the notification opens

A tap tries to open Spending Tracker's add-an-expense screen directly, then
falls back to the app's main screen.

Spending Tracker is closed source and keeps that screen private, so it cannot be
started by name. What it does publish is its launcher shortcuts, the "Add
Expense" and "Add Income" entries you get by long pressing its icon. A static
shortcut declares its intent in the app's own `res/xml/shortcuts.xml`, and any
app may read another app's resources, so Spend Nudge reads that file and reuses
the exact intent the launcher would send. Firing the shortcut itself is not an
option: `LauncherApps.startShortcut` is reserved for the active home screen
launcher.

Two things can still stop it, and both fall back to opening the app normally:
shortcuts created in code instead of XML leave nothing to read, and an intent
aimed at a private activity is refused by the system.

The main screen shows what a tap will open. **Choose the screen a tap opens**
lists the shortcuts found plus every exported screen, so you can pin a different
one or go back to opening the app normally.

## Tests

`./gradlew test` runs the parser tests in
`app/src/test/java/com/ranjan/spendnudge/TxnParserTest.java` on a plain JVM
(no device needed). They also run in CI before the APK is built.

## Your bank's format isn't detected?
Edit `app/src/main/java/com/ranjan/spendnudge/TxnParser.java` (the only parsing file)
and rebuild.
