package com.ranjan.spendnudge;

import android.content.Context;

final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_CREDITS = "notify_credits";
    private static final String KEY_SCREEN = "tracker_screen";
    private static final String KEY_SCREEN_LABEL = "tracker_screen_label";

    private Prefs() {}

    /** The intent you pinned, as a URI, or null while detection is automatic. */
    static String trackerScreen(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SCREEN, null);
    }

    /** The name shown for the pinned intent, for the main screen to display. */
    static String trackerScreenLabel(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_SCREEN_LABEL, null);
    }

    /** Pass nulls to go back to detecting the screen automatically. */
    static void setTrackerScreen(Context c, String intentUri, String label) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString(KEY_SCREEN, intentUri)
                .putString(KEY_SCREEN_LABEL, label)
                .apply();
    }

    static boolean notifyCredits(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_CREDITS, true);
    }

    static void setNotifyCredits(Context c, boolean v) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_CREDITS, v).apply();
    }
}
