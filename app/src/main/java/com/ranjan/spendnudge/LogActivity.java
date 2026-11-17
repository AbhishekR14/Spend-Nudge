package com.ranjan.spendnudge;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.widget.Toast;

/** Copies the amount to the clipboard and opens Spending Tracker. Has no UI. */
public class LogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String amount = getIntent().getStringExtra(Notifier.EXTRA_AMOUNT);
        int notifId = getIntent().getIntExtra(Notifier.EXTRA_NOTIF_ID, -1);
        if (notifId != -1) getSystemService(NotificationManager.class).cancel(notifId);

        if (amount != null) {
            ClipboardManager cm = getSystemService(ClipboardManager.class);
            cm.setPrimaryClip(ClipData.newPlainText("amount", amount));
            Toast.makeText(this, "₹" + amount + " copied, paste it in Amount",
                    Toast.LENGTH_SHORT).show();
        }

        TrackerLauncher.open(this);
        finish();
    }
}
