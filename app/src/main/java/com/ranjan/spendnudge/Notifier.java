package com.ranjan.spendnudge;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

public final class Notifier {

    static final String CHANNEL_ID = "transactions";
    static final String EXTRA_AMOUNT = "amount";
    static final String EXTRA_NOTIF_ID = "notif_id";

    private Notifier() {}

    static void ensureChannel(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "Transactions", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Reminds you to log each bank transaction");
        nm.createNotificationChannel(ch);
    }

    public static void show(Context ctx, TxnParser.Txn txn) {
        if (ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        ensureChannel(ctx);

        int id = (int) (System.currentTimeMillis() & 0x7fffffff);
        String amt = "₹" + txn.amountText();

        String title = txn.isDebit
                ? amt + " spent, log it?"
                : amt + " received, log as income?";

        StringBuilder text = new StringBuilder();
        if (txn.merchant != null) text.append(txn.isDebit ? "To " : "From ").append(txn.merchant);
        if (txn.account != null) {
            if (text.length() > 0) text.append(" · ");
            text.append("A/c ••").append(txn.account);
        }
        if (text.length() == 0) text.append("Tap to open Spending Tracker");

        Intent open = new Intent(ctx, LogActivity.class)
                .putExtra(EXTRA_AMOUNT, txn.amountText())
                .putExtra(EXTRA_NOTIF_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(pi)
                .addAction(new Notification.Action.Builder(null, "Log it", pi).build())
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build();

        ctx.getSystemService(NotificationManager.class).notify(id, n);
    }
}
