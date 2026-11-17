package com.ranjan.spendnudge;

import android.app.Activity;
import android.os.Bundle;

/**
 * Opens Spending Tracker and gets out of the way. Has no UI of its own.
 *
 * It deliberately copies nothing: Android 13 and up puts its own clipboard
 * confirmation at the bottom of the screen, right over the tracker's + Expense
 * button. The amount stays readable in the notification instead, which is why
 * the notification is not dismissed on tap.
 */
public class LogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TrackerLauncher.open(this);
        finish();
    }
}
