package com.ranjan.spendnudge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * JVM unit tests for the SMS parser. No Android framework involved, so these run
 * with ./gradlew test (no device or emulator needed).
 */
public class TxnParserTest {

    // --- Helpers -----------------------------------------------------------

    private static TxnParser.Txn parsed(String sender, String body) {
        TxnParser.Txn t = TxnParser.parse(sender, body);
        assertNotNull("expected a transaction for: " + body, t);
        return t;
    }

    private static void assertTxn(String sender, String body,
                                  boolean debit, String amount,
                                  String merchant, String account) {
        TxnParser.Txn t = parsed(sender, body);
        assertEquals("debit/credit", debit, t.isDebit);
        assertEquals("amount", amount, t.amountText());
        assertEquals("merchant", merchant, t.merchant);
        assertEquals("account", account, t.account);
    }

    // --- Debits ------------------------------------------------------------

    @Test
    public void hdfcUpiSent() {
        assertTxn("AX-HDFCBK",
                "Sent Rs.450.00 From HDFC Bank A/C *1234 To SWIGGY On 17/11/26 Ref 432112345678",
                true, "450", "SWIGGY", "1234");
    }

    @Test
    public void iciciDebitedWithPayeeCredited() {
        // "debited" comes before "credited", so this is a debit, not a credit.
        assertTxn("JD-ICICIT-S",
                "ICICI Bank Acct XX123 debited for Rs 250.00 on 17-Nov-26; ZOMATO LTD credited. UPI:432198765432.",
                true, "250", "ZOMATO LTD", "123");
    }

    @Test
    public void sbiUpiWithNoCurrencySymbol() {
        assertTxn("VM-SBIUPI",
                "Dear UPI user A/C X4521 debited by 120.0 on date 17Nov26 trf to RAPIDO Refno 432155554444. -SBI",
                true, "120", "RAPIDO", "4521");
    }

    @Test
    public void hdfcCardSpendIgnoresAvailableLimit() {
        assertTxn("AD-HDFCBK",
                "Rs.1,299.00 spent on HDFC Bank Card x9876 at AMAZON PAY INDIA on 2026-11-17:10:32:11. Avl Lmt: Rs.85,000.00",
                true, "1299", "AMAZON PAY INDIA", "9876");
    }

    @Test
    public void hdfcUpiInfoLine() {
        assertTxn("AX-HDFCBK",
                "Update! INR 2,100.00 debited from HDFC Bank XX1234 on 17-NOV-26. Info: UPI-BIGBASKET-bigbasket@hdfcbank. Avl bal:INR 10,000.00",
                true, "2100", "BIGBASKET", "1234");
    }

    // --- Credits -----------------------------------------------------------

    @Test
    public void axisCreditIgnoresAvailableBalance() {
        assertTxn("BZ-AXISBK",
                "INR 5,000.00 credited to A/c no. XX7788 on 17-11-26 by UPI-RAHUL SHARMA. Avl Bal INR 23,456.78",
                false, "5000", "RAHUL SHARMA", "7788");
    }

    @Test
    public void kotakReceivedFromVpa() {
        assertTxn("JM-KOTAKB",
                "Received Rs.2000.00 in your Kotak Bank A/c X5566 from priya@okaxis on 17-11-26.UPI Ref:432111112222.",
                false, "2000", "priya@okaxis", "5566");
    }

    // --- Messages that must be ignored -------------------------------------

    @Test
    public void otpIsIgnored() {
        assertNull(TxnParser.parse("AX-HDFCBK",
                "Your OTP for transaction of Rs 499 is 123456. Do not share."));
    }

    @Test
    public void futureAutopayIsIgnored() {
        assertNull(TxnParser.parse("AX-HDFCBK",
                "Rs.799 will be debited from your A/c XX1234 on 20-Nov-26 towards Netflix autopay mandate."));
    }

    @Test
    public void billDueIsIgnored() {
        assertNull(TxnParser.parse("AX-HDFCBK",
                "Your HDFC Bank Credit Card XX9876 bill of Rs.12,345 is due on 25-Nov-26."));
    }

    @Test
    public void personalPhoneNumberIsIgnored() {
        assertNull(TxnParser.parse("+919876543210", "Sent Rs.100 to you"));
    }

    @Test
    public void nullBodyIsIgnored() {
        assertNull(TxnParser.parse("AX-HDFCBK", null));
    }

    @Test
    public void nonTransactionalBankSmsIsIgnored() {
        assertNull(TxnParser.parse("AX-HDFCBK",
                "Dear Customer, your HDFC Bank statement for Oct 2026 is ready."));
    }

    // --- Sender check ------------------------------------------------------

    @Test
    public void bankSendersAreRecognised() {
        assertTrue(TxnParser.looksLikeBank("AX-HDFCBK"));
        assertTrue(TxnParser.looksLikeBank("JD-ICICIT-S"));
        assertTrue(TxnParser.looksLikeBank("VM-SBIUPI"));
    }

    @Test
    public void nonBankSendersAreRejected() {
        assertFalse(TxnParser.looksLikeBank("+919876543210"));
        assertFalse(TxnParser.looksLikeBank("9876543210"));
        assertFalse(TxnParser.looksLikeBank(null));
    }

    @Test
    public void nullSenderSkipsTheSenderCheck() {
        // MainActivity's test box passes null so any pasted SMS is parsed.
        assertTxn(null,
                "Sent Rs.450.00 From HDFC Bank A/C *1234 To SWIGGY On 17/11/26 Ref 432112345678",
                true, "450", "SWIGGY", "1234");
    }

    // --- Amount formatting -------------------------------------------------

    @Test
    public void wholeAmountsDropTheDecimals() {
        assertEquals("450", parsed("AX-HDFCBK", "Rs.450.00 spent at SWIGGY").amountText());
    }

    @Test
    public void paiseAreKept() {
        assertEquals("450.75", parsed("AX-HDFCBK", "Rs.450.75 spent at SWIGGY").amountText());
    }

    @Test
    public void thousandSeparatorsAreStripped() {
        assertEquals("85000", parsed("AX-HDFCBK", "Rs.85,000.00 spent at SWIGGY").amountText());
    }

    @Test
    public void rupeeSymbolIsAccepted() {
        assertEquals("99", parsed("AX-HDFCBK", "₹99 debited at SWIGGY").amountText());
    }

    // --- Amount as shown to you --------------------------------------------

    @Test
    public void displayedAmountsAreGrouped() {
        assertEquals("450", parsed("AX-HDFCBK", "Rs.450.00 spent at SWIGGY").amountDisplay());
        assertEquals("85,000", parsed("AX-HDFCBK", "Rs.85,000 spent at X").amountDisplay());
        assertEquals("2,263.10", parsed("AX-HDFCBK", "INR 2263.10 spent at X").amountDisplay());
    }

    @Test
    public void displayedAndPlainAmountsAgreeOnValue() {
        // amountText stays plain: anything matching on it must not see separators.
        TxnParser.Txn t = parsed("AX-HDFCBK", "Rs.1,299.00 spent at AMAZON");
        assertEquals("1299", t.amountText());
        assertEquals("1,299", t.amountDisplay());
    }
}
