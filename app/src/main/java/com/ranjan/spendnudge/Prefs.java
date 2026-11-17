package com.ranjan.spendnudge;

import android.content.Context;

final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_CREDITS = "notify_credits";

    private Prefs() {}

    static boolean notifyCredits(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getBoolean(KEY_CREDITS, true);
    }

    static void setNotifyCredits(Context c, boolean v) {
        c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putBoolean(KEY_CREDITS, v).apply();
    }
}
