package com.ranjan.spendnudge;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns an Indian bank SMS into a Txn (amount, debit/credit, merchant, account),
 * or returns null if the SMS is not a completed transaction.
 *
 * Pure Java on purpose: no Android imports, so it can be unit-tested on any JVM.
 * If your bank's format isn't picked up, this is the only file you need to edit.
 */
public final class TxnParser {

    public static final class Txn {
        public final double amount;
        public final boolean isDebit;
        public final String merchant; // may be null
        public final String account;  // last digits, may be null

        Txn(double amount, boolean isDebit, String merchant, String account) {
            this.amount = amount;
            this.isDebit = isDebit;
            this.merchant = merchant;
            this.account = account;
        }

        /** Plain digits, for anything that has to match exactly. */
        public String amountText() {
            if (amount == Math.rint(amount)) return String.format(Locale.US, "%.0f", amount);
            return String.format(Locale.US, "%.2f", amount);
        }

        /** Grouped for reading, Indian style: 85,000 and 1,00,000. */
        public String amountDisplay() {
            boolean whole = amount == Math.rint(amount);
            NumberFormat f = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            f.setMinimumFractionDigits(whole ? 0 : 2);
            f.setMaximumFractionDigits(whole ? 0 : 2);
            return f.format(amount);
        }

        @Override public String toString() {
            return (isDebit ? "DEBIT " : "CREDIT ") + amountText()
                    + " merchant=" + merchant + " acct=" + account;
        }
    }

    private static final int CI = Pattern.CASE_INSENSITIVE;

    // Banks send from alphanumeric DLT headers like "AX-HDFCBK" or "VM-SBIUPI-S".
    // Messages from normal phone numbers are ignored.
    private static final Pattern BANK_SENDER = Pattern.compile("^[A-Z]{2}-[A-Z0-9]{3,9}(-[A-Z])?$", CI);

    // Things that look like transactions but aren't.
    private static final Pattern IGNORE = Pattern.compile(
            "\\bOTP\\b|one[ -]?time password|verification code"
            + "|will be debited|to be debited|is due|due date|due on|min(imum)? amt due|total amt due"
            + "|requested money|has requested|collect request|payment request"
            + "|pre-?approved|\\boffer\\b|\\bloan of\\b|cashback of up to|apply now"
            + "|failed|declined|reversed|\\bauto ?pay (is )?set", CI);

    private static final Pattern DEBIT = Pattern.compile(
            "\\b(debited|spent|paid|sent|withdrawn|purchase|deducted|txn of|transaction of)\\b", CI);
    private static final Pattern CREDIT = Pattern.compile(
            "\\b(credited|received|deposited|refund(ed)?)\\b", CI);

    private static final String NUM = "([0-9][0-9,]*(?:\\.[0-9]{1,2})?)";

    // "Rs.450.00", "Rs 450", "INR 1,234.50", "₹99"
    private static final Pattern AMOUNT_CURRENCY = Pattern.compile("(?:Rs\\.?|INR|₹)\\s*" + NUM, CI);
    // SBI-style with no currency: "debited by 450.0"
    private static final Pattern AMOUNT_BARE = Pattern.compile(
            "(?:debited|credited)\\s+(?:by|for|with)\\s+" + NUM, CI);

    private static final Pattern[] MERCHANT = {
            Pattern.compile("\\bVPA\\s+([A-Za-z0-9.\\-_]+@[A-Za-z0-9]+)", CI),
            Pattern.compile("\\bInfo:\\s*(?:UPI-)?([^-.]+)", CI),
            Pattern.compile("\\bby\\s+UPI-(.+?)(?:\\.\\s|\\s+Avl|\\s+Ref|$)", CI),
            Pattern.compile("\\btrf to\\s+(.+?)(?:\\s+Ref|\\s+on\\b|\\.|$)", CI),
            Pattern.compile("\\bat\\s+(.+?)(?:\\s+on\\b|\\s+Avl|\\.\\s|$)", CI),
            Pattern.compile(";\\s*(.+?)\\s+credited", CI),
            Pattern.compile("\\bto\\s+(?!a/?c|your|you\\b)(.+?)(?:\\s+on\\b|\\s+via\\b|\\s+Ref|\\s+UPI|\\.\\s|$)", CI),
            Pattern.compile("\\bfrom\\s+(?!a/?c|your|HDFC|ICICI|SBI|Axis|Kotak)(.+?)(?:\\s+on\\b|\\s+Ref|\\s+UPI|\\.\\s|$)", CI),
    };

    private static final Pattern ACCOUNT = Pattern.compile(
            "(?:A/?c|Acct|Account|Card)(?:\\s*(?:no\\.?|ending|ending in))?\\s*[:.]?\\s*[X*x]*\\s*(\\d{3,6})", CI);

    // Fallback: bare masked number like "HDFC Bank XX1234".
    private static final Pattern ACCOUNT_MASKED = Pattern.compile("\\b[X*]{2,}(\\d{3,6})\\b", CI);

    private TxnParser() {}

    public static boolean looksLikeBank(String sender) {
        return sender != null && BANK_SENDER.matcher(sender.trim()).matches();
    }

    /** @param sender pass null to skip the sender check (used by the in-app tester). */
    public static Txn parse(String sender, String body) {
        if (body == null) return null;
        if (sender != null && !looksLikeBank(sender)) return null;

        String text = body.replaceAll("\\s+", " ").trim();
        if (IGNORE.matcher(text).find()) return null;

        // Debit or credit: whichever keyword appears first wins
        // (ICICI writes "debited for Rs 450; SWIGGY credited", which is a debit).
        Matcher d = DEBIT.matcher(text);
        Matcher c = CREDIT.matcher(text);
        int dPos = d.find() ? d.start() : Integer.MAX_VALUE;
        int cPos = c.find() ? c.start() : Integer.MAX_VALUE;
        if (dPos == Integer.MAX_VALUE && cPos == Integer.MAX_VALUE) return null;
        boolean isDebit = dPos < cPos;

        Double amount = firstAmount(text);
        if (amount == null || amount <= 0) return null;

        return new Txn(amount, isDebit, findMerchant(text), findAccount(text));
    }

    private static Double firstAmount(String text) {
        // The first amount in the SMS is the transaction; "Avl Bal" comes later.
        Matcher m = AMOUNT_CURRENCY.matcher(text);
        if (m.find()) return toDouble(m.group(1));
        m = AMOUNT_BARE.matcher(text);
        if (m.find()) return toDouble(m.group(1));
        return null;
    }

    private static Double toDouble(String s) {
        try {
            String clean = s.replace(",", "");
            if (clean.endsWith(".")) clean = clean.substring(0, clean.length() - 1);
            return Double.parseDouble(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String findMerchant(String text) {
        for (Pattern p : MERCHANT) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String s = m.group(1).trim();
                if (s.length() > 40) s = s.substring(0, 40).trim() + "…";
                if (!s.isEmpty() && !s.matches("(?i).*\\b(rs|inr)\\b.*")) return s;
            }
        }
        return null;
    }

    private static String findAccount(String text) {
        Matcher m = ACCOUNT.matcher(text);
        if (m.find()) return m.group(1);
        m = ACCOUNT_MASKED.matcher(text);
        return m.find() ? m.group(1) : null;
    }
}
