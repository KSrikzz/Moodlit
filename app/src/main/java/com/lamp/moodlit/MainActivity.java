package com.lamp.moodlit;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    SharedPreferences prefs;
    EditText etNumber, etServer;
    Spinner spCategory, spinnerCountryCode;
    TextView tvStatus, tvSelectedCount;
    LinearLayout contactList, selectedContactsList;

    final String[] CAT_KEYS   = {"toxic", "work", "friends", "family"};
    final String[] CAT_LABELS = {"🚫 Toxic (Red)", "💼 Work (Blue)", "⭐ Friends (Yellow)", "🏠 Family (Green)"};

    final String[] COUNTRY_CODES  = {"+91", "+1", "+44", "+61", "+971", "+65", "+81"};
    final String[] COUNTRY_LABELS = {"+91 🇮🇳", "+1 🇺🇸", "+44 🇬🇧", "+61 🇦🇺", "+971 🇦🇪", "+65 🇸🇬", "+81 🇯🇵"};

    ArrayList<String> selectedNumbers = new ArrayList<>();

    private ActivityResultLauncher<Intent> contactPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("MoodlitPrefs", MODE_PRIVATE);

        etNumber = findViewById(R.id.et_number);
        etServer = findViewById(R.id.et_server);
        spCategory = findViewById(R.id.sp_category);
        spinnerCountryCode = findViewById(R.id.spinner_country_code);
        tvStatus = findViewById(R.id.tv_status);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        contactList = findViewById(R.id.contact_list);
        selectedContactsList = findViewById(R.id.selected_contacts_list);

        etServer.setText(prefs.getString("server_ip", ""));

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, CAT_LABELS);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(catAdapter);

        ArrayAdapter<String> codeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, COUNTRY_LABELS);
        codeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountryCode.setAdapter(codeAdapter);
        spinnerCountryCode.setSelection(prefs.getInt("country_code_index", 0));
        spinnerCountryCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                prefs.edit().putInt("country_code_index", pos).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> nums = result.getData().getStringArrayListExtra("selected_numbers");
                        if (nums != null) {
                            selectedNumbers.clear();
                            selectedNumbers.addAll(nums);
                            renderSelectedContacts();
                            setStatus("📁 Selected " + nums.size() + " contact(s)");
                        }
                    }
                });

        findViewById(R.id.btn_add).setOnClickListener(v -> addMapping());
        findViewById(R.id.btn_save_ip).setOnClickListener(v -> saveServerIP());
        findViewById(R.id.btn_test).setOnClickListener(v -> testConnection());
        findViewById(R.id.btn_pick_contacts).setOnClickListener(v -> openMultiContactPicker());
        findViewById(R.id.btn_user_dashboard).setOnClickListener(v ->
                startActivity(new Intent(this, UserDashboardActivity.class)));

        requestPermissions();
        startCallMonitorService();
        renderSelectedContacts();
        loadMappings();
    }

    void startCallMonitorService() {
        Intent serviceIntent = new Intent(this, CallMonitorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }
        setStatus("✅ Moodlit service started — listening for calls");
    }

    void openMultiContactPicker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, 2);
            setStatus("⚠️ Contacts permission required");
            return;
        }
        contactPickerLauncher.launch(new Intent(this, ContactPickerActivity.class));
    }

    void addMapping() {
        String cat = CAT_KEYS[spCategory.getSelectedItemPosition()];
        int added = 0;

        String rawNumber = etNumber.getText().toString().trim().replaceAll("\\s+", "");
        if (!rawNumber.isEmpty()) {
            if (rawNumber.length() < 10) {
                etNumber.setError("Enter full 10 digit number");
                setStatus("⚠️ Number must be 10 digits");
                return;
            }
            String code = COUNTRY_CODES[spinnerCountryCode.getSelectedItemPosition()];
            String fullNumber = code + rawNumber;
            prefs.edit().putString("num_" + fullNumber, cat).apply();
            added++;
        }

        for (String fullNumber : selectedNumbers) {
            prefs.edit().putString("num_" + fullNumber, cat).apply();
            added++;
        }

        if (added == 0) {
            setStatus("⚠️ Enter a number or pick contacts first");
            return;
        }

        etNumber.setText("");
        selectedNumbers.clear();
        renderSelectedContacts();
        setStatus("✅ Added " + added + " mapping(s) → " + cat);
        loadMappings();
    }

    void renderSelectedContacts() {
        selectedContactsList.removeAllViews();
        tvSelectedCount.setText("SELECTED CONTACTS (" + selectedNumbers.size() + ")");

        if (selectedNumbers.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No contacts selected.");
            empty.setTextColor(0xFF6b6a67);
            empty.setTextSize(13);
            selectedContactsList.addView(empty);
            return;
        }

        for (String num : selectedNumbers) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView tv = new TextView(this);
            tv.setText(num);
            tv.setTextColor(0xFFE8E6E2);
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button del = new Button(this);
            del.setText("✕");
            del.setTextSize(12);
            del.setBackgroundColor(0x00000000);
            del.setTextColor(0xFFE74C3C);
            del.setOnClickListener(v -> {
                selectedNumbers.remove(num);
                renderSelectedContacts();
            });

            row.addView(tv);
            row.addView(del);
            selectedContactsList.addView(row);
        }
    }

    void saveServerIP() {
        String ip = etServer.getText().toString().trim();
        if (ip.isEmpty()) {
            setStatus("⚠️ Enter ESP IP address");
            return;
        }
        prefs.edit().putString("server_ip", ip).apply();
        setStatus("✅ ESP IP saved: " + ip);
    }

    void testConnection() {
        String ip = etServer.getText().toString().trim();
        if (ip.isEmpty()) {
            setStatus("⚠️ Enter ESP IP first");
            return;
        }
        setStatus("⏳ Pinging ESP at " + ip + "...");
        new Thread(() -> {
            try {
                URL url = new URL("http://" + ip + "/ping");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("GET");
                c.setConnectTimeout(4000);
                c.setReadTimeout(4000);
                int code = c.getResponseCode();
                c.disconnect();
                runOnUiThread(() -> setStatus(code == 200
                        ? "✅ ESP connected!"
                        : "⚠️ ESP responded HTTP " + code));
            } catch (Exception e) {
                runOnUiThread(() -> setStatus("❌ ESP unreachable: " + e.getMessage()));
            }
        }).start();
    }

    void loadMappings() {
        contactList.removeAllViews();
        Map<String, ?> all = prefs.getAll();
        boolean any = false;

        for (Map.Entry<String, ?> e : all.entrySet()) {
            if (!e.getKey().startsWith("num_")) continue;
            any = true;

            String number = e.getKey().substring(4);
            String cat = (String) e.getValue();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);

            TextView tv = new TextView(this);
            tv.setText(number + "   →   " + getCatLabel(cat));
            tv.setTextColor(0xFFE8E6E2);
            tv.setTextSize(14);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button del = new Button(this);
            del.setText("✕");
            del.setTextSize(12);
            del.setBackgroundColor(0x00000000);
            del.setTextColor(0xFFE74C3C);
            del.setOnClickListener(v -> {
                prefs.edit().remove("num_" + number).apply();
                loadMappings();
            });

            android.view.View line = new android.view.View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            line.setLayoutParams(lp);
            line.setBackgroundColor(0xFF2e2d2b);

            row.addView(tv);
            row.addView(del);
            contactList.addView(row);
            contactList.addView(line);
        }

        if (!any) {
            TextView empty = new TextView(this);
            empty.setText("No mappings yet. Add a number above.");
            empty.setTextColor(0xFF6b6a67);
            empty.setTextSize(13);
            empty.setPadding(0, 12, 0, 12);
            contactList.addView(empty);
        }
    }

    String getCatLabel(String cat) {
        for (int i = 0; i < CAT_KEYS.length; i++) {
            if (CAT_KEYS[i].equals(cat)) return CAT_LABELS[i];
        }
        return cat;
    }

    void setStatus(String msg) {
        tvStatus.setText(msg);
    }

    void requestPermissions() {
        List<String> permsList = new ArrayList<>();
        permsList.add(Manifest.permission.READ_PHONE_STATE);
        permsList.add(Manifest.permission.READ_CALL_LOG);
        permsList.add(Manifest.permission.READ_CONTACTS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permsList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean need = false;
        for (String p : permsList) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need = true;
            }
        }

        if (need) {
            ActivityCompat.requestPermissions(this, permsList.toArray(new String[0]), 1);
        } else {
            setStatus("✅ Permissions OK — listening for calls");
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] p, int[] results) {
        super.onRequestPermissionsResult(code, p, results);
        for (int r : results) {
            if (r != PackageManager.PERMISSION_GRANTED) {
                setStatus("❌ Permissions denied — app won't detect calls");
                return;
            }
        }
        setStatus("✅ Permissions granted — listening for calls");
        startCallMonitorService();
    }
}