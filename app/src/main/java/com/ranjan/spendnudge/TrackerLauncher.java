package com.ranjan.spendnudge;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.net.Uri;
import android.os.Bundle;

import org.xmlpull.v1.XmlPullParser;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Decides what a notification tap opens in Spending Tracker.
 *
 * The tracker is closed source and keeps its add-an-expense screen private, so
 * it cannot be started by name. What it does publish is its launcher shortcuts
 * ("Add Expense", "Add Income"), and a static shortcut declares its intent in
 * the app's own res/xml/shortcuts.xml. Any app may read another app's
 * resources, so we read that file and reuse the exact intent the launcher would
 * send. That is the only way in: Android reserves starting a shortcut itself
 * (LauncherApps.startShortcut) for the active home screen launcher.
 *
 * Two things can still stop it. Shortcuts created in code rather than XML are
 * invisible from here, and an intent aimed at a private activity is refused by
 * the system. Both land on the same safe fallback: open the app normally.
 * Exported activities are offered too, so MainActivity's picker always has
 * something to show, and whatever you pick there wins.
 */
final class TrackerLauncher {

    static final String PACKAGE = "com.mhriley.spendingtracker";

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String SHORTCUT_META = "android.app.shortcuts";

    // A screen counts as an expense form only if its name says both what it is
    // about and that it creates one. "Add Income" deliberately misses.
    private static final String[] SUBJECT = {"expense", "transaction", "txn", "spend"};
    private static final String[] VERB = {"add", "new", "create", "entry", "enter", "edit", "input"};

    private TrackerLauncher() {}

    /**
     * Something a notification tap could open. A shortcut may declare several
     * intents, a parent screen and then the one you actually want, so they are
     * kept in order of preference: the screen the shortcut ends on comes first.
     */
    static final class Option {
        final String label;
        final List<Intent> intents;
        final boolean expenseForm;

        Option(String label, List<Intent> intents, boolean expenseForm) {
            this.label = label;
            this.intents = intents;
            this.expenseForm = expenseForm;
        }

        /** The first of its intents this app is actually allowed to start. */
        Intent startable(Context ctx) {
            for (Intent i : intents) {
                if (canStart(ctx, i)) return i;
            }
            return null;
        }
    }

    /**
     * Whether Android would let us start this intent. An activity that is not
     * exported is refused outright, whoever asks, so there is no point trying.
     */
    static boolean canStart(Context ctx, Intent intent) {
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = intent.getComponent();
        if (cn == null) return intent.resolveActivity(pm) != null;
        try {
            ActivityInfo ai = pm.getActivityInfo(cn, 0);
            return ai.exported && ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    // --- Name matching, pure string work, covered by the unit tests ----------

    /**
     * True if a name looks like a way to enter an expense, whether it is a
     * shortcut label ("Add Expense") or a class ("com.x.AddExpenseActivity").
     */
    static boolean looksLikeExpenseScreen(String name) {
        if (name == null) return false;
        String n = shortName(name).toLowerCase(Locale.US);
        return containsAny(n, SUBJECT) && containsAny(n, VERB);
    }

    /** "com.foo.AddExpenseActivity" becomes "AddExpenseActivity". */
    static String shortName(String className) {
        if (className == null) return "";
        int dot = className.lastIndexOf('.');
        return dot == -1 ? className : className.substring(dot + 1);
    }

    private static boolean containsAny(String name, String[] words) {
        for (String w : words) {
            if (name.contains(w)) return true;
        }
        return false;
    }

    // --- What we can offer --------------------------------------------------

    /** Shortcuts the tracker publishes, then its exported screens, best first. */
    static List<Option> options(Context ctx) {
        List<Option> out = new ArrayList<>();
        addShortcuts(ctx, out);
        addExportedScreens(ctx, out);
        // Stable, so an expense shortcut stays ahead of an expense activity.
        out.sort((a, b) -> Boolean.compare(b.expenseForm, a.expenseForm));
        return out;
    }

    private static void addShortcuts(Context ctx, List<Option> out) {
        PackageManager pm = ctx.getPackageManager();
        PackageInfo info;
        Resources res;
        try {
            info = pm.getPackageInfo(PACKAGE,
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
            res = pm.getResourcesForApplication(PACKAGE);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        if (info.activities == null) return;

        for (ActivityInfo ai : info.activities) {
            Bundle meta = ai.metaData;
            if (meta == null) continue;
            int xml = meta.getInt(SHORTCUT_META, 0);
            if (xml == 0) continue;

            XmlResourceParser parser = null;
            try {
                parser = res.getXml(xml);
                readShortcuts(res, parser, out);
            } catch (Exception e) {
                // Unreadable shortcuts: the exported screens below still apply.
            } finally {
                if (parser != null) parser.close();
            }
        }
    }

    /** Reads res/xml/shortcuts.xml and turns each shortcut back into its intents. */
    private static void readShortcuts(Resources res, XmlResourceParser parser, List<Option> out)
            throws Exception {
        String label = null;
        boolean enabled = true;
        List<Intent> intents = new ArrayList<>();

        for (int event = parser.next(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            String tag = parser.getName();
            if (tag == null) continue;

            if (event == XmlPullParser.START_TAG) {
                if ("shortcut".equals(tag)) {
                    label = shortcutLabel(res, parser);
                    enabled = parser.getAttributeBooleanValue(ANDROID_NS, "enabled", true);
                    intents = new ArrayList<>();
                } else if ("intent".equals(tag)) {
                    intents.add(readIntent(parser));
                } else if ("extra".equals(tag) && !intents.isEmpty()) {
                    readExtra(intents.get(intents.size() - 1), parser);
                }
            } else if (event == XmlPullParser.END_TAG && "shortcut".equals(tag)) {
                if (enabled && label != null && !intents.isEmpty()) {
                    // The last intent is the screen the shortcut ends on, so try it first.
                    Collections.reverse(intents);
                    out.add(new Option(label, intents, looksLikeExpenseScreen(label)));
                }
                label = null;
                intents = new ArrayList<>();
            }
        }
    }

    private static String shortcutLabel(Resources res, XmlResourceParser parser) {
        int labelRes = parser.getAttributeResourceValue(ANDROID_NS, "shortcutShortLabel", 0);
        if (labelRes == 0) {
            labelRes = parser.getAttributeResourceValue(ANDROID_NS, "shortcutLongLabel", 0);
        }
        if (labelRes != 0) {
            try {
                return res.getString(labelRes);
            } catch (Resources.NotFoundException ignored) {
                // Fall through to the id.
            }
        }
        String id = parser.getAttributeValue(ANDROID_NS, "shortcutId");
        return id == null ? "Shortcut" : id;
    }

    private static Intent readIntent(XmlResourceParser parser) {
        String action = parser.getAttributeValue(ANDROID_NS, "action");
        Intent intent = new Intent(action == null ? Intent.ACTION_MAIN : action);

        String pkg = parser.getAttributeValue(ANDROID_NS, "targetPackage");
        String cls = parser.getAttributeValue(ANDROID_NS, "targetClass");
        if (pkg != null && cls != null) {
            intent.setComponent(new ComponentName(pkg, cls));
        } else {
            // Never let a half written shortcut send us to some other app.
            intent.setPackage(PACKAGE);
        }

        String data = parser.getAttributeValue(ANDROID_NS, "data");
        String mime = parser.getAttributeValue(ANDROID_NS, "mimeType");
        if (data != null && mime != null) {
            intent.setDataAndType(Uri.parse(data), mime);
        } else if (data != null) {
            intent.setData(Uri.parse(data));
        } else if (mime != null) {
            intent.setType(mime);
        }
        return intent;
    }

    /** Shortcut extras arrive as text, so put each back under its likely type. */
    private static void readExtra(Intent intent, XmlResourceParser parser) {
        String name = parser.getAttributeValue(ANDROID_NS, "name");
        String value = parser.getAttributeValue(ANDROID_NS, "value");
        if (name == null || value == null) return;

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            intent.putExtra(name, Boolean.parseBoolean(value));
            return;
        }
        try {
            intent.putExtra(name, Integer.parseInt(value));
            return;
        } catch (NumberFormatException notAnInt) {
            intent.putExtra(name, value);
        }
    }

    private static void addExportedScreens(Context ctx, List<Option> out) {
        PackageInfo info;
        try {
            info = ctx.getPackageManager().getPackageInfo(PACKAGE, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }
        if (info.activities == null) return;

        for (ActivityInfo ai : info.activities) {
            // A private screen would throw SecurityException on startActivity.
            if (!ai.exported || !ai.enabled) continue;
            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(ai.packageName, ai.name));
            out.add(new Option(shortName(ai.name),
                    Collections.singletonList(intent), looksLikeExpenseScreen(ai.name)));
        }
    }

    // --- What a tap actually opens ------------------------------------------

    /** The option detected automatically: an expense form we may actually start. */
    private static Option detected(Context ctx) {
        for (Option o : options(ctx)) {
            if (o.expenseForm && o.startable(ctx) != null) return o;
        }
        return null;
    }

    /**
     * What the tracker publishes and what we may do with it. Shown on the main
     * screen so a tap that lands on the wrong page can be explained rather than
     * guessed at.
     */
    static String report(Context ctx) {
        List<Option> options = options(ctx);
        if (options.isEmpty()) {
            return "Spending Tracker isn't installed, or it publishes nothing readable.";
        }

        StringBuilder sb = new StringBuilder();
        for (Option o : options) {
            sb.append(o.label).append(o.expenseForm ? "  (looks like an expense form)" : "")
                    .append('\n');
            for (Intent i : o.intents) {
                ComponentName cn = i.getComponent();
                sb.append("   screen: ")
                        .append(cn == null ? "any in " + i.getPackage() : shortName(cn.getClassName()))
                        .append(canStart(ctx, i) ? "  [can open]" : "  [private, cannot open]")
                        .append("\n   action: ").append(i.getAction()).append('\n');
                Bundle extras = i.getExtras();
                if (extras != null) {
                    for (String key : extras.keySet()) {
                        sb.append("   extra: ").append(key).append(" = ")
                                .append(extras.get(key)).append('\n');
                    }
                }
            }
            sb.append('\n');
        }

        for (Option o : options) {
            if (o.expenseForm && o.startable(ctx) == null) {
                sb.append("\n").append(o.label).append(" points at a screen Spending Tracker ")
                        .append("keeps private. Android lets only that app open it, so a tap ")
                        .append("opens its main screen and the amount is on the clipboard, ")
                        .append("ready to paste.");
                break;
            }
        }
        return sb.toString().trim();
    }

    private static Intent pinnedIntent(Context ctx) {
        String saved = Prefs.trackerScreen(ctx);
        if (saved == null) return null;
        try {
            return Intent.parseUri(saved, Intent.URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            ComponentName cn = ComponentName.unflattenFromString(saved);
            return cn == null ? null : new Intent(Intent.ACTION_MAIN).setComponent(cn);
        }
    }

    /**
     * Remembers your choice from the picker. Pass null to go back to automatic.
     * Returns false for an option Android would never let us open, so the
     * caller can say why instead of pinning something that cannot work.
     */
    static boolean pin(Context ctx, Option option) {
        if (option == null) {
            Prefs.setTrackerScreen(ctx, null, null);
            return true;
        }
        Intent intent = option.startable(ctx);
        if (intent == null) return false;
        Prefs.setTrackerScreen(ctx, intent.toUri(Intent.URI_INTENT_SCHEME), option.label);
        return true;
    }

    /** What MainActivity shows you. */
    static String describeTarget(Context ctx) {
        if (pinnedIntent(ctx) != null) {
            String label = Prefs.trackerScreenLabel(ctx);
            return label == null ? "the screen you pinned" : label;
        }
        Option auto = detected(ctx);
        return auto == null ? "the app's main screen" : auto.label;
    }

    /** Opens the expense screen when we may, the app's main screen when we may not. */
    static void open(Context ctx) {
        Intent target = pinnedIntent(ctx);
        if (target == null) {
            Option auto = detected(ctx);
            if (auto != null) target = auto.startable(ctx);
        }
        // CLEAR_TOP and SINGLE_TOP matter only here: with NEW_TASK alone, a
        // tracker that is already running just comes to the front and never
        // receives the extras that choose the screen.
        if (target != null && start(ctx, target, Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP)) {
            return;
        }

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if (launch == null) {
            launch = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + PACKAGE));
        }
        // No extra flags: this is the plain "open the app" route that tapping
        // its icon takes, and it must not throw away what you were doing in it.
        start(ctx, launch, 0);
    }

    private static boolean start(Context ctx, Intent intent, int extraFlags) {
        try {
            ctx.startActivity(new Intent(intent)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | extraFlags));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false; // Private or missing screen: the caller falls back.
        }
    }
}
