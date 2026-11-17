package com.ranjan.spendnudge;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Decides which screen of Spending Tracker a notification tap should open.
 *
 * Spending Tracker is closed source, so nothing here can be hard coded: the
 * add-expense screen is looked for at run time among the activities the tracker
 * declares. Android only lets one app start another app's activity when that
 * activity is exported, so if the tracker keeps its entry screen private we open
 * the app normally instead, exactly as before. MainActivity lets you pin a
 * different screen by hand when the guess is wrong.
 */
final class TrackerLauncher {

    static final String PACKAGE = "com.mhriley.spendingtracker";

    // A screen counts as an expense form only if its name says both what it is
    // about and that it creates one. "AddIncomeActivity" deliberately misses.
    private static final String[] SUBJECT = {"expense", "transaction", "txn", "spend"};
    private static final String[] VERB = {"add", "new", "create", "entry", "enter", "edit", "input"};

    private TrackerLauncher() {}

    /**
     * True if a class name looks like a screen for entering an expense, for
     * example "com.example.AddExpenseActivity". Only the class name counts, not
     * the package. Pure string work, so it is covered by the JVM unit tests.
     */
    static boolean looksLikeExpenseScreen(String className) {
        if (className == null) return false;
        String name = shortName(className).toLowerCase(Locale.US);
        return containsAny(name, SUBJECT) && containsAny(name, VERB);
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

    /**
     * Every tracker screen this app is allowed to open, likeliest expense form
     * first. Empty when the tracker is missing or exports nothing.
     */
    static List<ActivityInfo> candidates(Context ctx) {
        List<ActivityInfo> out = new ArrayList<>();
        PackageInfo info;
        try {
            info = ctx.getPackageManager().getPackageInfo(PACKAGE, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            return out;
        }
        if (info.activities == null) return out;
        for (ActivityInfo ai : info.activities) {
            // A private activity would throw SecurityException on startActivity.
            if (ai.exported && ai.enabled) out.add(ai);
        }
        out.sort((a, b) -> Boolean.compare(
                looksLikeExpenseScreen(b.name), looksLikeExpenseScreen(a.name)));
        return out;
    }

    /** The screen a tap opens: the one you pinned, else the detected one, else null. */
    static ComponentName target(Context ctx) {
        String pinned = Prefs.trackerScreen(ctx);
        if (pinned != null) {
            ComponentName cn = ComponentName.unflattenFromString(pinned);
            if (cn != null) return cn;
        }
        for (ActivityInfo ai : candidates(ctx)) {
            if (looksLikeExpenseScreen(ai.name)) {
                return new ComponentName(ai.packageName, ai.name);
            }
        }
        return null;
    }

    /** What MainActivity shows you. */
    static String describeTarget(Context ctx) {
        ComponentName cn = target(ctx);
        return cn == null ? "the app's main screen" : shortName(cn.getClassName());
    }

    /** Opens the expense screen if we may, the app's main screen if not. */
    static void open(Context ctx) {
        ComponentName target = target(ctx);
        if (target != null
                && start(ctx, new Intent(Intent.ACTION_MAIN).setComponent(target))) {
            return;
        }

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(PACKAGE);
        if (launch == null) {
            launch = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=" + PACKAGE));
        }
        start(ctx, launch);
    }

    private static boolean start(Context ctx, Intent intent) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false; // Private or missing screen: the caller falls back.
        }
    }
}
