package com.ranjan.spendnudge;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Shows what makes a message worth a nudge, and lets you change it.
 *
 * Banks word the same event differently ("debited", "spent", "used at"), so
 * when one of yours is missed you can add the wording here instead of waiting
 * for a new build.
 */
public class RulesActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setTitle(R.string.rules_title);
        }

        Switch debits = findViewById(R.id.notifyDebits);
        debits.setChecked(Prefs.notifyDebits(this));
        debits.setOnCheckedChangeListener((b, on) -> Prefs.setNotifyDebits(this, on));

        Switch credits = findViewById(R.id.notifyCredits);
        credits.setChecked(Prefs.notifyCredits(this));
        credits.setOnCheckedChangeListener((b, on) -> Prefs.setNotifyCredits(this, on));

        findViewById(R.id.addDebit).setOnClickListener(v -> addWord(Prefs.GROUP_DEBIT));
        findViewById(R.id.addCredit).setOnClickListener(v -> addWord(Prefs.GROUP_CREDIT));
        findViewById(R.id.addIgnore).setOnClickListener(v -> addWord(Prefs.GROUP_IGNORE));
        findViewById(R.id.reset).setOnClickListener(v -> confirmReset());

        render();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void render() {
        renderGroup(R.id.debitList, Prefs.GROUP_DEBIT);
        renderGroup(R.id.creditList, Prefs.GROUP_CREDIT);
        renderGroup(R.id.ignoreList, Prefs.GROUP_IGNORE);
        findViewById(R.id.reset).setVisibility(
                Prefs.rulesEdited(this) ? View.VISIBLE : View.GONE);
    }

    private void renderGroup(int containerId, String group) {
        LinearLayout box = findViewById(containerId);
        box.removeAllViews();

        for (String word : Prefs.words(this, group)) {
            View row = getLayoutInflater().inflate(R.layout.row_rule, box, false);
            ((TextView) row.findViewById(R.id.word)).setText(word);

            TextView tag = row.findViewById(R.id.tag);
            tag.setVisibility(Prefs.isBuiltIn(group, word) ? View.GONE : View.VISIBLE);

            View remove = row.findViewById(R.id.remove);
            remove.setContentDescription(getString(R.string.remove_word, word));
            remove.setOnClickListener(v -> {
                Prefs.removeWord(this, group, word);
                render();
            });
            box.addView(row);
        }

        if (box.getChildCount() == 0) {
            TextView empty = new TextView(this);
            empty.setText(R.string.no_words);
            empty.setTextColor(getColor(R.color.bad));
            empty.setTextSize(14f);
            box.addView(empty);
        }
    }

    private void addWord(String group) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(R.string.add_hint);

        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle(titleFor(group))
                .setMessage(R.string.add_blurb)
                .setView(box)
                .setPositiveButton(R.string.add, (d, which) -> {
                    String word = input.getText().toString().trim();
                    if (word.isEmpty()) return;
                    if (word.length() > 40) {
                        Toast.makeText(this, R.string.word_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Prefs.addWord(this, group, word);
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private int titleFor(String group) {
        switch (group) {
            case Prefs.GROUP_CREDIT: return R.string.add_credit;
            case Prefs.GROUP_IGNORE: return R.string.add_ignore;
            default: return R.string.add_debit;
        }
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.reset_title)
                .setMessage(R.string.reset_blurb)
                .setPositiveButton(R.string.reset, (d, which) -> {
                    Prefs.resetWords(this);
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
