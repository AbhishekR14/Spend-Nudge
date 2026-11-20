package com.ranjan.spendnudge;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Everything the app remembers. Two toggles, a pinned screen and the word lists. */
final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_DEBITS = "notify_debits";
    private static final String KEY_CREDITS = "notify_credits";
    private static final String KEY_SCREEN = "tracker_screen";
    private static final String KEY_SCREEN_LABEL = "tracker_screen_label";

    /** The three word lists, used as a key prefix and passed around as an id. */
    static final String GROUP_DEBIT = "debit";
    static final String GROUP_CREDIT = "credit";
    static final String GROUP_IGNORE = "ignore";

    private Prefs() {}

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    // --- What to nudge about -----------------------------------------------

    static boolean notifyDebits(Context c) {
        return prefs(c).getBoolean(KEY_DEBITS, true);
    }

    static void setNotifyDebits(Context c, boolean v) {
        prefs(c).edit().putBoolean(KEY_DEBITS, v).apply();
    }

    static boolean notifyCredits(Context c) {
        return prefs(c).getBoolean(KEY_CREDITS, true);
    }

    static void setNotifyCredits(Context c, boolean v) {
        prefs(c).edit().putBoolean(KEY_CREDITS, v).apply();
    }

    // --- Which tracker screen a tap opens ----------------------------------

    /** The intent you pinned, as a URI, or null while detection is automatic. */
    static String trackerScreen(Context c) {
        return prefs(c).getString(KEY_SCREEN, null);
    }

    /** The name shown for the pinned intent, for the main screen to display. */
    static String trackerScreenLabel(Context c) {
        return prefs(c).getString(KEY_SCREEN_LABEL, null);
    }

    /** Pass nulls to go back to detecting the screen automatically. */
    static void setTrackerScreen(Context c, String intentUri, String label) {
        prefs(c).edit()
                .putString(KEY_SCREEN, intentUri)
                .putString(KEY_SCREEN_LABEL, label)
                .apply();
    }

    // --- The word lists ----------------------------------------------------

    static String[] builtIn(String group) {
        switch (group) {
            case GROUP_CREDIT: return TxnParser.CREDIT_WORDS;
            case GROUP_IGNORE: return TxnParser.IGNORE_WORDS;
            default: return TxnParser.DEBIT_WORDS;
        }
    }

    /** Built-in words you have not switched off, then the ones you added. */
    static List<String> words(Context c, String group) {
        Set<String> off = lower(read(c, "off_" + group));
        List<String> out = new ArrayList<>();
        for (String w : builtIn(group)) {
            if (!off.contains(w.toLowerCase())) out.add(w);
        }
        List<String> added = new ArrayList<>(read(c, "add_" + group));
        Collections.sort(added, String.CASE_INSENSITIVE_ORDER);
        out.addAll(added);
        return out;
    }

    /** True when the word is one of ours rather than one you typed. */
    static boolean isBuiltIn(String group, String word) {
        for (String w : builtIn(group)) {
            if (w.equalsIgnoreCase(word)) return true;
        }
        return false;
    }

    /** Adds a word, or brings back a built-in you had switched off. */
    static void addWord(Context c, String group, String word) {
        String w = word.trim();
        if (w.isEmpty()) return;

        Set<String> off = read(c, "off_" + group);
        off.removeIf(existing -> existing.equalsIgnoreCase(w));
        write(c, "off_" + group, off);

        if (isBuiltIn(group, w)) return;

        Set<String> added = read(c, "add_" + group);
        for (String existing : added) {
            if (existing.equalsIgnoreCase(w)) return;
        }
        added.add(w);
        write(c, "add_" + group, added);
    }

    /** Deletes one you added, or switches off a built-in. */
    static void removeWord(Context c, String group, String word) {
        Set<String> added = read(c, "add_" + group);
        if (added.removeIf(existing -> existing.equalsIgnoreCase(word))) {
            write(c, "add_" + group, added);
            return;
        }
        Set<String> off = read(c, "off_" + group);
        off.add(word.toLowerCase());
        write(c, "off_" + group, off);
    }

    /** True once any list differs from the defaults. */
    static boolean rulesEdited(Context c) {
        for (String group : new String[] {GROUP_DEBIT, GROUP_CREDIT, GROUP_IGNORE}) {
            if (!read(c, "add_" + group).isEmpty()) return true;
            if (!read(c, "off_" + group).isEmpty()) return true;
        }
        return false;
    }

    static void resetWords(Context c) {
        SharedPreferences.Editor e = prefs(c).edit();
        for (String group : new String[] {GROUP_DEBIT, GROUP_CREDIT, GROUP_IGNORE}) {
            e.remove("add_" + group).remove("off_" + group);
        }
        e.apply();
    }

    /** The word lists as the parser wants them. */
    static TxnParser.Rules rules(Context c) {
        return new TxnParser.Rules(
                words(c, GROUP_DEBIT), words(c, GROUP_CREDIT), words(c, GROUP_IGNORE));
    }

    // A stored set must never be handed back to SharedPreferences as it stands,
    // so every read is a copy.
    private static Set<String> read(Context c, String key) {
        return new LinkedHashSet<>(prefs(c).getStringSet(key, Collections.emptySet()));
    }

    private static void write(Context c, String key, Set<String> value) {
        prefs(c).edit().putStringSet(key, new LinkedHashSet<>(value)).apply();
    }

    private static Set<String> lower(Set<String> in) {
        Set<String> out = new HashSet<>();
        for (String s : in) out.add(s.toLowerCase());
        return out;
    }

    static List<String> builtInList(String group) {
        return Arrays.asList(builtIn(group));
    }
}
