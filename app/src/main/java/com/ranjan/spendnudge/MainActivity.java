package com.ranjan.spendnudge;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Setup, preferences and a box for trying the parser on a real SMS. */
public class MainActivity extends Activity {

    private static final String[] PERMS = {
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.POST_NOTIFICATIONS
    };

    private ImageView icSms;
    private ImageView icNotif;
    private ImageView icTracker;
    private TextView hint;
    private TextView screenLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Notifier.ensureChannel(this);

        icSms = findViewById(R.id.icSms);
        icNotif = findViewById(R.id.icNotif);
        icTracker = findViewById(R.id.icTracker);
        hint = findViewById(R.id.hint);
        screenLabel = findViewById(R.id.screenLabel);

        findViewById(R.id.grant).setOnClickListener(v -> requestPermissions(PERMS, 1));
        findViewById(R.id.appInfo).setOnClickListener(v -> openAppInfo());
        findViewById(R.id.pickScreen).setOnClickListener(v -> pickScreen());
        findViewById(R.id.showDetails).setOnClickListener(v -> showDetails());

        findViewById(R.id.messageTypes).setOnClickListener(
                v -> startActivity(new Intent(this, RulesActivity.class)));

        EditText input = findViewById(R.id.testInput);
        TextView result = findViewById(R.id.testResult);
        findViewById(R.id.testBtn).setOnClickListener(v -> runTest(input, result));

        ((TextView) findViewById(R.id.version))
                .setText(getString(R.string.version_format, versionName()));
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

    // --- Status ------------------------------------------------------------

    private void refreshStatus() {
        boolean sms = has(Manifest.permission.RECEIVE_SMS);
        boolean notif = has(Manifest.permission.POST_NOTIFICATIONS);
        boolean tracker = getPackageManager()
                .getLaunchIntentForPackage(TrackerLauncher.PACKAGE) != null;

        mark(icSms, sms);
        mark(icNotif, notif);
        mark(icTracker, tracker);

        hint.setText(sms && notif ? R.string.hint_ready : R.string.hint_grant);
        findViewById(R.id.grant).setEnabled(!(sms && notif));
        screenLabel.setText(getString(R.string.tap_opens, TrackerLauncher.describeTarget(this)));
    }

    private void mark(ImageView icon, boolean ok) {
        icon.setImageResource(ok ? R.drawable.ic_check : R.drawable.ic_cross);
        icon.setImageTintList(ColorStateList.valueOf(getColor(ok ? R.color.good : R.color.bad)));
    }

    private boolean has(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private void openAppInfo() {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null)));
    }

    // --- What a tap opens --------------------------------------------------

    /** Lets you pick what a notification tap opens in Spending Tracker. */
    private void pickScreen() {
        List<TrackerLauncher.Option> options = TrackerLauncher.options(this);
        if (options.isEmpty()) {
            Toast.makeText(this, R.string.nothing_to_open, Toast.LENGTH_LONG).show();
            return;
        }

        String[] labels = new String[options.size() + 1];
        labels[0] = getString(R.string.pick_normally);
        for (int i = 0; i < options.size(); i++) {
            TrackerLauncher.Option o = options.get(i);
            // Say up front which ones Android will not let this app open.
            labels[i + 1] = o.startable(this) == null
                    ? o.label + getString(R.string.cannot_open_suffix)
                    : o.label;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.pick_title)
                .setItems(labels, (d, which) -> {
                    TrackerLauncher.Option chosen = which == 0 ? null : options.get(which - 1);
                    if (!TrackerLauncher.pin(this, chosen)) {
                        Toast.makeText(this, getString(R.string.cannot_open, chosen.label),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    refreshStatus();
                })
                .show();
    }

    /** Shows what Spending Tracker publishes and which parts we may open. */
    private void showDetails() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.details_title)
                .setMessage(TrackerLauncher.report(this))
                .setPositiveButton(R.string.close, null)
                .show();
    }

    // --- Parser tester -----------------------------------------------------

    private void runTest(EditText input, TextView result) {
        result.setVisibility(View.VISIBLE);
        // Uses your wordings, so the tester agrees with what a real SMS would do.
        TxnParser.Txn txn = TxnParser.parse(null, input.getText().toString(), Prefs.rules(this));

        if (txn == null) {
            result.setTextColor(getColor(R.color.bad));
            result.setText(R.string.test_miss);
            return;
        }

        result.setTextColor(getColor(R.color.text));
        result.setText((txn.isDebit ? "Spent " : "Received ") + "₹" + txn.amountDisplay()
                + "\nMerchant: " + (txn.merchant == null ? "-" : txn.merchant)
                + "\nAccount: " + (txn.account == null ? "-" : "••" + txn.account)
                + "\n\nA test notification was sent.");
        Notifier.show(this, txn);
    }
}
