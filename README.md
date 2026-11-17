# Spend Nudge

Bank SMS arrives → notification "₹450 spent — log it?" → tap → amount is copied
and Spending Tracker opens → paste into Amount.

- Only listens for incoming SMS (RECEIVE_SMS). Never reads your inbox.
- Nothing runs in the background; Android wakes the app only when an SMS arrives.
- No internet permission. Nothing leaves your phone.

## Build

**Option A: GitHub (no laptop setup needed)**
1. Create a new private repo on github.com and upload everything in this folder
   (keep the `.github` folder — it contains the build script).
2. Open the repo's **Actions** tab. The "Build APK" run starts automatically (~4 min).
3. Open the finished run → download **spend-nudge-apk** → unzip → `app-debug.apk`.

**Option B: Android Studio**
Open this folder in Android Studio → Build → Build APK(s).

## Install on the Pixel
1. Open `app-debug.apk` on the phone and allow "Install unknown apps" when asked.
2. Open Spend Nudge → **Grant permissions**.
3. If the SMS permission is blocked/greyed out: tap **Open app info** → ⋮ (top right)
   → **Allow restricted settings** → back → Grant permissions again.
4. Paste one of your real bank SMS into the test box and tap **Test**.

## Tests

`./gradlew test` runs the parser tests in
`app/src/test/java/com/ranjan/spendnudge/TxnParserTest.java` on a plain JVM
(no device needed). They also run in CI before the APK is built.

## Your bank's format isn't detected?
Edit `app/src/main/java/com/ranjan/spendnudge/TxnParser.java` (the only parsing file)
and rebuild.
