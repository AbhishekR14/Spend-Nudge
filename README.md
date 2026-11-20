# Spend Nudge

Turns an Indian bank SMS into a one tap reminder to log the spend in
[Spending Tracker](https://play.google.com/store/apps/details?id=com.mhriley.spendingtracker).

[![Build APK](https://github.com/AbhishekR14/Spend-Nudge/actions/workflows/build.yml/badge.svg)](https://github.com/AbhishekR14/Spend-Nudge/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/AbhishekR14/Spend-Nudge?label=release)](https://github.com/AbhishekR14/Spend-Nudge/releases/latest)
![minSdk 31](https://img.shields.io/badge/minSdk-31-blue)
![Java 17](https://img.shields.io/badge/Java-17-blue)
![Dependencies: none](https://img.shields.io/badge/runtime%20dependencies-none-success)

A bank SMS arrives, Android wakes the app, and a notification says
**₹2,263.10 spent · To BLINKIT ECO**. Tap it and Spending Tracker opens, with
the amount still on screen in the shade while you type it in.

Nothing else runs. There is no service, no polling and no network access.

---

## Contents

- [Features](#features)
- [Install](#install)
- [How it works](#how-it-works)
- [Messages it understands](#messages-it-understands)
- [Permissions and privacy](#permissions-and-privacy)
- [Known limitation](#known-limitation)
- [Development](#development)
- [Project layout](#project-layout)
- [Releasing](#releasing)

## Features

- **Event driven.** A manifest registered `BroadcastReceiver` is the only entry
  point. Nothing runs between messages, so there is no battery cost.
- **Reads one SMS, never your inbox.** `RECEIVE_SMS` only. No `READ_SMS`.
- **Offline by construction.** No `INTERNET` permission, so nothing can leave
  the phone even by accident.
- **Credits are optional.** Money received can nudge you too, or stay quiet.
- **Editable wordings.** Banks word the same event differently. The
  **Message types** screen lists every wording that marks a message as a spend,
  as income, or as one to skip, and you can add or remove any of them on the
  phone. No rebuild needed.
- **Built in tester.** Paste a real SMS on the main screen and see exactly what
  the parser makes of it before trusting it.
- **No third party code.** Plain Java and the Android framework. JUnit is test
  only and is not packaged into the APK.

## Install

Download the APK from the [latest release](https://github.com/AbhishekR14/Spend-Nudge/releases/latest),
or take the **spend-nudge-apk** artifact from any
[Build APK run](https://github.com/AbhishekR14/Spend-Nudge/actions/workflows/build.yml)
(artifacts need a GitHub login and expire after 90 days).

1. Open the APK on the phone and allow installing unknown apps when asked.
2. Open Spend Nudge and tap **Grant permissions**.
3. If the SMS permission is greyed out, tap **Open app info**, then the three
   dot menu, then **Allow restricted settings**, and grant it again. Android
   blocks SMS permissions for sideloaded apps until this is done.
4. Paste a real bank SMS into the test box and tap **Test** to confirm the
   parser reads your bank's wording.

Builds are debug signed, which is all a sideloaded personal app needs.

## How it works

```
bank SMS ──▶ SmsReceiver ──▶ TxnParser ──▶ Notifier ──▶ notification
                                              │
                                   tap        ▼
                                        LogActivity ──▶ TrackerLauncher ──▶ Spending Tracker
```

| Step | What happens |
|---|---|
| `SmsReceiver` | Woken by Android per message. Joins multipart messages by sender. |
| `TxnParser` | Decides whether the text is a completed transaction, then pulls out amount, direction, merchant and the last digits of the account. Pure Java, no Android imports, so it is unit tested on the JVM. |
| `Notifier` | Posts the nudge. It is not dismissed on tap, so the amount stays readable while you type it in. |
| `LogActivity` | Invisible and translucent. Hands off to `TrackerLauncher` and finishes. |
| `TrackerLauncher` | Works out the best screen to open in the tracker, and falls back to its main screen. |

## Messages it understands

Covered by `TxnParserTest`, which runs on every build.

| Sender | Message | Parsed as |
|---|---|---|
| `AX-HDFCBK` | Sent Rs.450.00 From HDFC Bank A/C *1234 To SWIGGY On 17/11/26 | debit 450, SWIGGY, 1234 |
| `JD-ICICIT-S` | ICICI Bank Acct XX123 debited for Rs 250.00 on 17-Nov-26; ZOMATO LTD credited. | debit 250, ZOMATO LTD, 123 |
| `VM-SBIUPI` | Dear UPI user A/C X4521 debited by 120.0 on date 17Nov26 trf to RAPIDO | debit 120, RAPIDO, 4521 |
| `AD-HDFCBK` | Rs.1,299.00 spent on HDFC Bank Card x9876 at AMAZON PAY INDIA. Avl Lmt: Rs.85,000.00 | debit 1299, AMAZON PAY INDIA, 9876 |
| `BZ-AXISBK` | INR 5,000.00 credited to A/c no. XX7788 by UPI-RAHUL SHARMA. Avl Bal INR 23,456.78 | credit 5000, RAHUL SHARMA, 7788 |
| `JM-KOTAKB` | Received Rs.2000.00 in your Kotak Bank A/c X5566 from priya@okaxis | credit 2000, priya@okaxis, 5566 |
| `AX-HDFCBK` | Update! INR 2,100.00 debited from HDFC Bank XX1234. Info: UPI-BIGBASKET-bigbasket@hdfcbank | debit 2100, BIGBASKET, 1234 |
| `AD-HSBCIN` | HSBC Credit Card xx4805 used at CAS*SWIGGY for INR 228.00 on 21/11/26. Avl limit INR 1002593.18 | debit 228, SWIGGY, 4805 |

Deliberately ignored: OTPs, scheduled debits ("will be debited"), bill reminders
("is due"), payment requests, failed or reversed transactions, and anything from
a plain phone number rather than a bank's sender ID.

An amount later in the message, such as `Avl Bal`, is never mistaken for the
transaction, `debited ... ; MERCHANT credited` is read as a debit, and a card
network prefix is dropped, so `CAS*SWIGGY` is recorded as `SWIGGY`.

**If your bank is missed,** open **Message types and wordings** in the app and
add the wording. See below.

## Message types

A bank SMS is classified by the wordings it contains, and the app shows you the
lists it uses.

| List | Meaning | Defaults include |
|---|---|---|
| Money went out | counts as a spend | `debited`, `spent`, `paid`, `used`, `withdrawn` |
| Money came in | counts as income | `credited`, `received`, `deposited`, `refund` |
| Never a transaction | skipped whatever else it says | `OTP`, `is due`, `will be debited`, `failed` |

Open **Message types and wordings** on the main screen to add a wording, remove
one, or switch spends and income on and off separately. Wordings are matched
whole and without regard to case, so `used` never fires on `user`, and a wording
you type is always matched literally rather than as a pattern. **Reset to
defaults** appears once you have changed anything.

## Permissions and privacy

| Permission | Why |
|---|---|
| `RECEIVE_SMS` | To read an incoming message as it arrives. Stored messages are never read. |
| `POST_NOTIFICATIONS` | To show the nudge. |

Not requested, by design: `INTERNET`, `READ_SMS`, `FOREGROUND_SERVICE`,
`RECEIVE_BOOT_COMPLETED`. Message contents are parsed in memory and only the
parsed amount, merchant and masked account reach the notification. Nothing is
written to disk except two settings, and nothing is transmitted.

## Known limitation

**A tap opens the tracker's main screen, not its expense form.**

Spending Tracker's own "Add Expense" shortcut points at
`com.mhriley.spendingtracker.TransactionActivity`, carrying
`transaction_type=expense`, and that activity is not exported. Android allows
only Spending Tracker itself to start it, and `LauncherApps.startShortcut` is
reserved for the active home screen launcher. No third party app can reach that
form, so the expense form stays one tap away on **+ Expense**.

The app still looks for a way in, since it costs nothing and would work at once
if the tracker exported that screen, or with any other tracker that does:

1. It reads the tracker's launcher shortcuts from its own `res/xml/shortcuts.xml`
   and reuses the exact intent the launcher would send, extras included.
2. A shortcut may declare several intents, so each is tried in turn, nearest the
   target first, skipping any screen that is not exported.
3. Every exported activity is offered as well.

**Choose what a tap opens** pins any of these and marks the ones Android will
refuse. **What Spending Tracker publishes** prints what was found, which tells a
wrong guess apart from a screen that is genuinely out of reach.

**Nothing is copied to the clipboard.** Android 13 and up shows its own copy
confirmation at the bottom of the screen, directly over the tracker's
**+ Expense** button, and an app cannot move or suppress it. The notification
stays in the shade instead, so the amount is there while you type it.

## Development

Requires JDK 17, Android SDK 35 and build-tools 35.0.0. Android Studio offers to
install both SDK pieces on first open.

```bash
./gradlew test           # JVM unit tests, no device needed
./gradlew assembleDebug  # app/build/outputs/apk/debug/app-debug.apk
```

CI runs the tests before assembling, so a red build means a real failure.

**Adding your bank's wording:** `TxnParser.java` is the only file that parses
anything. Add the pattern there, add the message to `TxnParserTest`, and run
`./gradlew test`.

## Project layout

```
app/src/main/java/com/ranjan/spendnudge/
├── TxnParser.java        SMS text to a transaction. Pure Java, unit tested.
├── SmsReceiver.java      Manifest receiver. The app's only entry point.
├── Notifier.java         Builds and posts the nudge.
├── LogActivity.java      Invisible. Opens the tracker and finishes.
├── TrackerLauncher.java  Decides what to open in the tracker.
├── Prefs.java            Two stored settings.
└── MainActivity.java     Setup, preferences and the parser tester.

app/src/test/java/com/ranjan/spendnudge/
├── TxnParserTest.java        Sample messages, ignore cases, amount formatting.
└── TrackerLauncherTest.java  Which names count as an expense screen.
```

## Releasing

Either push a tag:

```bash
git tag v1.0.0 && git push origin v1.0.0
```

or run the **Build APK** workflow from the Actions tab and set **release_tag**
to the version. Both build, test, and publish a release with the APK attached.

Bump `versionCode` and `versionName` in `app/build.gradle.kts` first.
