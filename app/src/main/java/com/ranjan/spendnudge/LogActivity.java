package com.ranjan.spendnudge;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/** Copies the amount to the clipboard and opens Spending Tracker. Has no UI. */
public class LogActivity extends Activity {

    static final String TRACKER_PACKAGE = "com.mhriley.spendingtracker";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String amount = getIntent().getStringExtra(Notifier.EXTRA_AMOUNT);
        int notifId = getIntent().getIntExtra(Notifier.EXTRA_NOTIF_ID, -1);
        if (notifId != -1) getSystemService(NotificationManager.class).cancel(notifId);

        if (amount != null) {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            cm.setPrimaryClip(ClipData.newPlainText("amount", amount));
            Toast.makeText(this, "₹" + amount + " copied — paste it in Amount",
                    Toast.LENGTH_SHORT).show();
        }

        Intent launch = getPackageManager().getLaunchIntentForPackage(TRACKER_PACKAGE);
        if (launch == null) {
            launch = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + TRACKER_PACKAGE));
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
        finish();
    }
}
