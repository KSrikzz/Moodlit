package com.lamp.moodlit;

import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class ContactPickerActivity extends AppCompatActivity {

    ListView listView;
    Button btnDone, btnCancel;
    EditText etSearch;

    ArrayList<ContactRow> allContacts = new ArrayList<>();
    ArrayList<ContactRow> filteredContacts = new ArrayList<>();
    ArrayList<String> selectedNumbers = new ArrayList<>();

    ContactAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_picker);

        listView = findViewById(R.id.list_contacts);
        btnDone = findViewById(R.id.btn_done_contacts);
        btnCancel = findViewById(R.id.btn_cancel_contacts);
        etSearch = findViewById(R.id.et_search_contact);

        loadContacts();
        filteredContacts.addAll(allContacts);

        adapter = new ContactAdapter();
        listView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnDone.setOnClickListener(v -> {
            android.content.Intent data = new android.content.Intent();
            data.putStringArrayListExtra("selected_numbers", selectedNumbers);
            setResult(RESULT_OK, data);
            finish();
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    void loadContacts() {
        Cursor cursor = getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                },
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor == null) return;

        ArrayList<String> seen = new ArrayList<>();

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            String number = cursor.getString(1);

            String digits = number.replaceAll("[^0-9]", "");
            if (digits.length() < 10) continue;

            String last10 = digits.substring(digits.length() - 10);

            if (seen.contains(last10)) continue;
            seen.add(last10);

            allContacts.add(new ContactRow(name, last10));
        }

        cursor.close();
    }

    void filterContacts(String query) {
        filteredContacts.clear();

        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            filteredContacts.addAll(allContacts);
        } else {
            for (ContactRow row : allContacts) {
                String name = row.name == null ? "" : row.name.toLowerCase(Locale.ROOT);
                String number = row.number == null ? "" : row.number.toLowerCase(Locale.ROOT);

                if (name.contains(q) || number.contains(q)) {
                    filteredContacts.add(row);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    class ContactAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return filteredContacts.size();
        }

        @Override
        public Object getItem(int position) {
            return filteredContacts.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.row_contact_picker, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tv_contact_name);
            TextView tvNum = convertView.findViewById(R.id.tv_contact_number);
            CheckBox cb = convertView.findViewById(R.id.cb_contact);

            ContactRow row = filteredContacts.get(position);
            tvName.setText(row.name);
            tvNum.setText(row.number);

            cb.setOnCheckedChangeListener(null);
            cb.setChecked(row.selected);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                row.selected = isChecked;

                if (isChecked) {
                    if (!selectedNumbers.contains(row.number)) {
                        selectedNumbers.add(row.number);
                    }
                } else {
                    selectedNumbers.remove(row.number);
                }
            });

            return convertView;
        }
    }

    static class ContactRow {
        String name;
        String number;
        boolean selected = false;

        ContactRow(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }
}
