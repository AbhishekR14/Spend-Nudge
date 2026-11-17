package com.ranjan.spendnudge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers the part of TrackerLauncher that is plain string work: deciding whether
 * an activity name looks like an add-an-expense screen. The rest of the class
 * talks to the package manager and needs a device.
 */
public class TrackerLauncherTest {

    @Test
    public void expenseFormsAreRecognised() {
        assertTrue(TrackerLauncher.looksLikeExpenseScreen(
                "com.mhriley.spendingtracker.AddExpenseActivity"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen(
                "com.mhriley.spendingtracker.ui.NewTransactionActivity"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("com.x.ExpenseEntryActivity"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("com.x.EditTxnActivity"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("com.x.CreateSpendActivity"));
    }

    @Test
    public void shortcutLabelsAreRecognised() {
        // Launcher shortcut labels go through the same check as class names.
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("Add Expense"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("add_expense"));
        assertTrue(TrackerLauncher.looksLikeExpenseScreen("New Transaction"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("Add Income"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("add_income"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("Shortcut"));
    }

    @Test
    public void otherScreensAreNotRecognised() {
        assertFalse(TrackerLauncher.looksLikeExpenseScreen(
                "com.mhriley.spendingtracker.MainActivity"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.x.SettingsActivity"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.x.ReportsActivity"));
        // Says what, but not that it creates one.
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.x.ExpenseListActivity"));
        // Says it creates one, but not an expense.
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.x.AddIncomeActivity"));
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.x.AddAccountActivity"));
    }

    @Test
    public void onlyTheClassNameCounts() {
        // "add" and "expense" are in the package here, not the screen's own name.
        assertFalse(TrackerLauncher.looksLikeExpenseScreen("com.add.expense.MainActivity"));
    }

    @Test
    public void nullIsNotAScreen() {
        assertFalse(TrackerLauncher.looksLikeExpenseScreen(null));
    }

    @Test
    public void shortNameDropsThePackage() {
        assertEquals("AddExpenseActivity",
                TrackerLauncher.shortName("com.mhriley.spendingtracker.AddExpenseActivity"));
        assertEquals("AddExpenseActivity", TrackerLauncher.shortName("AddExpenseActivity"));
        assertEquals("", TrackerLauncher.shortName(null));
    }
}
