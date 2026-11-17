package com.ranjan.spendnudge;

import android.content.Context;

final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_CREDITS = "notify_credits";
    private static final String KEY_SCREEN = "tracker_screen";

    private Prefs() {}

    /** Flattened ComponentName of the tracker screen you pinned, or null for auto. */
    static String trackerScreen(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SCREEN, null);
    }

    /** Pass null to go back to detecting the screen automatically. */
    static void setTrackerScreen(Context c, String flattened) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putString(KEY_SCREEN, flattened).apply();
    }

    static boolean notifyCredits(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_CREDITS, true);
    }

    static void setNotifyCredits(Context c, boolean v) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_CREDITS, v).apply();
    }
}
