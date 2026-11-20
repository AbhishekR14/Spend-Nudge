package com.ranjan.spendnudge;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

    /** Wordings that mean money went out. Editable in the app. */
    public static final String[] DEBIT_WORDS = {
            "debited", "spent", "paid", "sent", "withdrawn", "purchase", "deducted",
            "used", "txn of", "transaction of",
    };

    /** Wordings that mean money came in. Editable in the app. */
    public static final String[] CREDIT_WORDS = {
            "credited", "received", "deposited", "refund", "refunded",
    };

    /** Wordings that look like a transaction but are not one. Editable in the app. */
    public static final String[] IGNORE_WORDS = {
            "OTP", "one time password", "one-time password", "verification code",
            "will be debited", "to be debited", "is due", "due date", "due on",
            "min amt due", "minimum amt due", "total amt due",
            "requested money", "has requested", "collect request", "payment request",
            "pre-approved", "preapproved", "offer", "loan of", "cashback of up to",
            "apply now", "failed", "declined", "reversed",
            "autopay set", "auto pay set", "autopay is set", "auto pay is set",
    };

    /**
     * The wordings in force when a message is read. Built from the defaults
     * above plus whatever you added or removed on the Message types screen.
     * Each word is matched whole and without regard to case.
     */
    public static final class Rules {
        private final List<String> debit;
        private final List<String> credit;
        private final List<String> ignore;
        private final Pattern debitPattern;
        private final Pattern creditPattern;
        private final Pattern ignorePattern;

        public Rules(Collection<String> debit, Collection<String> credit, Collection<String> ignore) {
            this.debit = new ArrayList<>(debit);
            this.credit = new ArrayList<>(credit);
            this.ignore = new ArrayList<>(ignore);
            this.debitPattern = compile(this.debit);
            this.creditPattern = compile(this.credit);
            this.ignorePattern = compile(this.ignore);
        }

        public static Rules defaults() {
            return new Rules(Arrays.asList(DEBIT_WORDS),
                    Arrays.asList(CREDIT_WORDS), Arrays.asList(IGNORE_WORDS));
        }

        public List<String> debitWords() { return Collections.unmodifiableList(debit); }

        public List<String> creditWords() { return Collections.unmodifiableList(credit); }

        public List<String> ignoreWords() { return Collections.unmodifiableList(ignore); }

        /** One alternation of quoted words, so a word can never be read as a pattern. */
        private static Pattern compile(List<String> words) {
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (word == null) continue;
                String trimmed = word.trim();
                if (trimmed.isEmpty()) continue;
                if (sb.length() > 0) sb.append('|');
                sb.append(Pattern.quote(trimmed));
            }
            return sb.length() == 0 ? null : Pattern.compile("\\b(?:" + sb + ")\\b", CI);
        }
    }

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
            // "for Rs" ends the name: card messages read "at MERCHANT for INR 228.00".
            Pattern.compile("\\bat\\s+(.+?)"
                    + "(?:\\s+on\\b|\\s+for\\s+(?:Rs\\.?|INR|₹)|\\s+Avl|\\.\\s|$)", CI),
            Pattern.compile(";\\s*(.+?)\\s+credited", CI),
            Pattern.compile("\\bto\\s+(?!a/?c|your|you\\b|report\\b)(.+?)"
                    + "(?:\\s+on\\b|\\s+via\\b|\\s+Ref|\\s+UPI|\\.\\s|$)", CI),
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
        return parse(sender, body, Rules.defaults());
    }

    /** @param rules the wordings in force, or null for the built-in ones. */
    public static Txn parse(String sender, String body, Rules rules) {
        if (body == null) return null;
        if (sender != null && !looksLikeBank(sender)) return null;
        if (rules == null) rules = Rules.defaults();

        String text = body.replaceAll("\\s+", " ").trim();
        if (rules.ignorePattern != null && rules.ignorePattern.matcher(text).find()) return null;

        // Debit or credit: whichever wording appears first wins
        // (ICICI writes "debited for Rs 450; SWIGGY credited", which is a debit).
        int dPos = firstMatch(rules.debitPattern, text);
        int cPos = firstMatch(rules.creditPattern, text);
        if (dPos == Integer.MAX_VALUE && cPos == Integer.MAX_VALUE) return null;
        boolean isDebit = dPos < cPos;

        Double amount = firstAmount(text);
        if (amount == null || amount <= 0) return null;

        return new Txn(amount, isDebit, findMerchant(text), findAccount(text));
    }

    private static int firstMatch(Pattern pattern, String text) {
        if (pattern == null) return Integer.MAX_VALUE;
        Matcher m = pattern.matcher(text);
        return m.find() ? m.start() : Integer.MAX_VALUE;
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
                // Card networks prefix the name, as in "CAS*SWIGGY" or "PAYU*BOOKMY".
                s = s.replaceFirst("^[A-Za-z0-9]{2,6}\\*", "").trim();
                s = s.replaceAll("[\\s.,;:]+$", "");
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
