package com.ranjan.spendnudge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends Activity {

    private static final String[] PERMS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private TextView status;
    private TextView screenLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifier.ensureChannel(this);

        status = findViewById(R.id.status);

        Button grant = findViewById(R.id.grant);
        grant.setOnClickListener(v -> requestPermissions(PERMS, 1));

        findViewById(R.id.appInfo).setOnClickListener(v -> openAppInfo());

        screenLabel = findViewById(R.id.screenLabel);
        findViewById(R.id.pickScreen).setOnClickListener(v -> pickScreen());
        findViewById(R.id.showDetails).setOnClickListener(v -> showDetails());

        Switch credits = findViewById(R.id.credits);
        credits.setChecked(Prefs.notifyCredits(this));
        credits.setOnCheckedChangeListener((b, on) -> Prefs.setNotifyCredits(this, on));

        EditText input = findViewById(R.id.testInput);
        TextView result = findViewById(R.id.testResult);
        findViewById(R.id.testBtn).setOnClickListener(v -> {
            TxnParser.Txn t = TxnParser.parse(null, input.getText().toString());
            if (t == null) {
                result.setText("✗ Not detected as a transaction.\nIf this is a real payment SMS, its format needs adding to TxnParser.java.");
            } else {
                result.setText("✓ " + (t.isDebit ? "Spent" : "Received") + " ₹" + t.amountText()
                        + "\nMerchant: " + (t.merchant == null ? "-" : t.merchant)
                        + "\nAccount: " + (t.account == null ? "-" : "••" + t.account)
                        + "\n\nA test notification was sent.");
                Notifier.show(this, t);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        refreshStatus();
    }

    private boolean has(String p) {
        return checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshStatus() {
        boolean sms = has(Manifest.permission.RECEIVE_SMS);
        boolean notif = has(Manifest.permission.POST_NOTIFICATIONS);
        boolean tracker = getPackageManager()
                .getLaunchIntentForPackage(TrackerLauncher.PACKAGE) != null;

        status.setText((sms ? "✓" : "✗") + " Receive SMS\n"
                + (notif ? "✓" : "✗") + " Notifications\n"
                + (tracker ? "✓" : "✗") + " Spending Tracker installed\n\n"
                + (sms && notif ? "All set. You'll be nudged on the next bank SMS."
                                : "Tap Grant permissions. If the SMS option is greyed out, "
                                  + "open app info → ⋮ → Allow restricted settings, then try again."));
        findViewById(R.id.grant).setEnabled(!(sms && notif));

        screenLabel.setText("A notification tap opens: " + TrackerLauncher.describeTarget(this));
    }

    /** Lets you pick what a notification tap opens in Spending Tracker. */
    private void pickScreen() {
        List<TrackerLauncher.Option> options = TrackerLauncher.options(this);
        if (options.isEmpty()) {
            Toast.makeText(this, "Spending Tracker isn't installed, or it publishes "
                    + "nothing another app may open, so only its main screen can be used.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String[] labels = new String[options.size() + 1];
        labels[0] = "Open the app normally";
        for (int i = 0; i < options.size(); i++) {
            labels[i + 1] = options.get(i).label;
        }

        new AlertDialog.Builder(this)
                .setTitle("What should a tap open?")
                .setItems(labels, (d, which) -> {
                    TrackerLauncher.pin(this, which == 0 ? null : options.get(which - 1));
                    refreshStatus();
                })
                .show();
    }

    /** Shows what Spending Tracker publishes and which parts we may open. */
    private void showDetails() {
        new AlertDialog.Builder(this)
                .setTitle("What Spending Tracker publishes")
                .setMessage(TrackerLauncher.report(this))
                .setPositiveButton("Close", null)
                .show();
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)));
    }
}
