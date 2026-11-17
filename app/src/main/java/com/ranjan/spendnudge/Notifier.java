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
        String amount = "₹" + txn.amountDisplay();
        String title = amount + (txn.isDebit ? " spent" : " received");

        StringBuilder text = new StringBuilder();
        if (txn.merchant != null) text.append(txn.isDebit ? "To " : "From ").append(txn.merchant);
        if (txn.account != null) {
            if (text.length() > 0) text.append(" · ");
            text.append("A/c ••").append(txn.account);
        }
        if (text.length() > 0) text.append("\n");
        text.append("Tap to open Spending Tracker");

        Intent open = new Intent(ctx, LogActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification n = new Notification.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(title)
                .setContentText(text.toString())
                .setStyle(new Notification.BigTextStyle().bigText(text.toString()))
                .setContentIntent(pi)
                .addAction(new Notification.Action.Builder(null, "Log it", pi).build())
                // Nothing is copied any more, so the amount has to stay readable
                // while you type it into the tracker. You dismiss it yourself.
                .setAutoCancel(false)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setShowWhen(true)
                .build();

        ctx.getSystemService(NotificationManager.class).notify(id, n);
    }
}
