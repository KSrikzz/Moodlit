package com.lamp.moodlit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDashboardActivity extends AppCompatActivity {

    SharedPreferences prefs;

    Switch swAnxiety, swDndToxic, swDndWork, swDndFriends, swDndFamily;
    TextView tvCountToxic, tvCountWork, tvCountFriends, tvCountFamily;
    LinearLayout logList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        prefs = getSharedPreferences("MoodlitPrefs", MODE_PRIVATE);

        swAnxiety    = findViewById(R.id.sw_anxiety);
        swDndToxic   = findViewById(R.id.sw_dnd_toxic);
        swDndWork    = findViewById(R.id.sw_dnd_work);
        swDndFriends = findViewById(R.id.sw_dnd_friends);
        swDndFamily  = findViewById(R.id.sw_dnd_family);
        tvCountToxic   = findViewById(R.id.tv_count_toxic);
        tvCountWork    = findViewById(R.id.tv_count_work);
        tvCountFriends = findViewById(R.id.tv_count_friends);
        tvCountFamily  = findViewById(R.id.tv_count_family);
        logList = findViewById(R.id.log_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        swAnxiety.setChecked(prefs.getBoolean("anxiety_free", false));
        swDndToxic.setChecked(prefs.getBoolean("dnd_toxic", false));
        swDndWork.setChecked(prefs.getBoolean("dnd_work", false));
        swDndFriends.setChecked(prefs.getBoolean("dnd_friends", false));
        swDndFamily.setChecked(prefs.getBoolean("dnd_family", false));

        if (swAnxiety.isChecked()) {
            swDndToxic.setChecked(true);
            swDndWork.setChecked(true);
            swDndToxic.setEnabled(false);
            swDndWork.setEnabled(false);
        }

        swAnxiety.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("anxiety_free", checked).apply();

            if (checked) {
                swDndToxic.setChecked(true);
                swDndWork.setChecked(true);
                prefs.edit()
                        .putBoolean("dnd_toxic", true)
                        .putBoolean("dnd_work", true)
                        .apply();
                swDndToxic.setEnabled(false);
                swDndWork.setEnabled(false);
            } else {
                swDndToxic.setEnabled(true);
                swDndWork.setEnabled(true);
                swDndToxic.setChecked(false);
                swDndWork.setChecked(false);
                prefs.edit()
                        .putBoolean("dnd_toxic", false)
                        .putBoolean("dnd_work", false)
                        .apply();
            }
        });

        swDndToxic.setOnCheckedChangeListener((b, c) -> {
            if (swDndToxic.isEnabled())
                prefs.edit().putBoolean("dnd_toxic", c).apply();
        });

        swDndWork.setOnCheckedChangeListener((b, c) -> {
            if (swDndWork.isEnabled())
                prefs.edit().putBoolean("dnd_work", c).apply();
        });

        swDndFriends.setOnCheckedChangeListener((b, c) ->
                prefs.edit().putBoolean("dnd_friends", c).apply());

        swDndFamily.setOnCheckedChangeListener((b, c) ->
                prefs.edit().putBoolean("dnd_family", c).apply());

        findViewById(R.id.btn_refresh_log).setOnClickListener(v -> loadLocalLog());

        loadLocalLog();
    }

    @SuppressLint("SetTextI18n")
    void loadLocalLog() {
        String raw = prefs.getString("call_log", "[]");
        try {
            JSONArray arr = new JSONArray(raw);

            int toxic = 0, work = 0, friends = 0, family = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject entry = arr.getJSONObject(i);
                switch (entry.optString("cat", "")) {
                    case "toxic":   toxic++;   break;
                    case "work":    work++;    break;
                    case "friends": friends++; break;
                    case "family":  family++;  break;
                }
            }

            tvCountToxic.setText(String.valueOf(toxic));
            tvCountWork.setText(String.valueOf(work));
            tvCountFriends.setText(String.valueOf(friends));
            tvCountFamily.setText(String.valueOf(family));

            renderLog(arr);

        } catch (Exception e) {
            tvCountToxic.setText("0");
            tvCountWork.setText("0");
            tvCountFriends.setText("0");
            tvCountFamily.setText("0");
            logList.removeAllViews();
            TextView err = new TextView(this);
            err.setText("⚠️ Could not read local log.");
            err.setTextColor(0xFFe74c3c);
            err.setTextSize(13);
            logList.addView(err);
        }
    }

    public static void logCall(Context context, String cat, String number) {
        SharedPreferences prefs = context.getSharedPreferences("MoodlitPrefs", Context.MODE_PRIVATE);
        try {
            String raw = prefs.getString("call_log", "[]");
            JSONArray arr = new JSONArray(raw);

            JSONObject entry = new JSONObject();
            entry.put("time", new SimpleDateFormat("HH:mm · dd MMM", Locale.getDefault())
                    .format(new Date()));
            entry.put("cat", cat);
            entry.put("number", number);

            JSONArray updated = new JSONArray();
            updated.put(entry);
            for (int i = 0; i < arr.length(); i++) {
                updated.put(arr.getJSONObject(i));
            }

            JSONArray capped = new JSONArray();
            int limit = Math.min(updated.length(), 50);
            for (int i = 0; i < limit; i++) {
                capped.put(updated.getJSONObject(i));
            }

            prefs.edit().putString("call_log", capped.toString()).apply();

        } catch (Exception ignored) {}
    }

    @SuppressLint("SetTextI18n")
    void renderLog(JSONArray arr) {
        logList.removeAllViews();

        if (arr.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No calls logged yet.");
            empty.setTextColor(0xFF6b6a67);
            empty.setTextSize(13);
            logList.addView(empty);
            return;
        }

        int show = Math.min(arr.length(), 15);
        for (int i = 0; i < show; i++) {
            try {
                JSONObject entry = arr.getJSONObject(i);
                String time   = entry.optString("time", "--");
                String cat    = entry.optString("cat", "?");
                String number = entry.optString("number", "");

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 10, 0, 10);

                TextView tvTime = new TextView(this);
                tvTime.setText(time);
                tvTime.setTextColor(0xFF6b6a67);
                tvTime.setTextSize(12);
                tvTime.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvCat = new TextView(this);
                tvCat.setText(getEmoji(cat) + " " + cat);
                tvCat.setTextColor(getCatColor(cat));
                tvCat.setTextSize(13);
                tvCat.setTypeface(null, android.graphics.Typeface.BOLD);

                TextView tvNum = new TextView(this);
                if (number.length() >= 4) {
                    tvNum.setText("···" + number.substring(number.length() - 4));
                }
                tvNum.setTextColor(0xFF6b6a67);
                tvNum.setTextSize(11);
                tvNum.setPadding(16, 0, 0, 0);

                row.addView(tvTime);
                row.addView(tvCat);
                row.addView(tvNum);

                TextView divider = new TextView(this);
                divider.setHeight(1);
                divider.setBackgroundColor(0xFF2e2d2b);

                logList.addView(row);
                logList.addView(divider);

            } catch (Exception ignored) {}
        }
    }

    String getEmoji(String cat) {
        return switch (cat) {
            case "toxic" -> "🚫";
            case "work" -> "💼";
            case "friends" -> "⭐";
            case "family" -> "🏠";
            default -> "📞";
        };
    }

    int getCatColor(String cat) {
        return switch (cat) {
            case "toxic" -> 0xFFe74c3c;
            case "work" -> 0xFF4f98a3;
            case "friends" -> 0xFFf1c40f;
            case "family" -> 0xFF6daa45;
            default -> 0xFFE8E6E2;
        };
    }
}