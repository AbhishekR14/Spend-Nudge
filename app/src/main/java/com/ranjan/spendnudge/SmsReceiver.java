package com.ranjan.spendnudge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import java.util.LinkedHashMap;
import java.util.Map;

/** Called by Android for each incoming SMS. Parses it and, if it's a transaction, notifies. */
public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;

        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts == null) return;

        // Long SMS arrive in several parts; join them per sender.
        Map<String, StringBuilder> bySender = new LinkedHashMap<>();
        for (SmsMessage p : parts) {
            if (p == null) continue;
            String sender = p.getDisplayOriginatingAddress();
            bySender.computeIfAbsent(sender, k -> new StringBuilder()).append(p.getMessageBody());
        }

        TxnParser.Rules rules = Prefs.rules(context);
        for (Map.Entry<String, StringBuilder> e : bySender.entrySet()) {
            TxnParser.Txn txn = TxnParser.parse(e.getKey(), e.getValue().toString(), rules);
            if (txn == null) continue;
            if (txn.isDebit && !Prefs.notifyDebits(context)) continue;
            if (!txn.isDebit && !Prefs.notifyCredits(context)) continue;
            Notifier.show(context, txn);
        }
    }
}
