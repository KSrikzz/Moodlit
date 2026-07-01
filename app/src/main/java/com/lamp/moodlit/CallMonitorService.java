package com.lamp.moodlit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.net.HttpURLConnection;
import java.net.URL;

public class CallMonitorService extends Service {

    private static final String TAG = "MoodlitService";
    private static final String CHANNEL_ID = "moodlit_call_monitor";
    private static final int NOTIF_ID = 1001;

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;

    private String lastState = TelephonyManager.EXTRA_STATE_IDLE;
    private String lastNumber = "";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                handleCallState(state, incomingNumber);
            }
        };

        try {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
            Log.d(TAG, "PhoneStateListener registered");
        } catch (SecurityException e) {
            Log.e(TAG, "Missing phone permission: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Listener registration failed: " + e.getMessage());
        }
    }

    private void handleCallState(int state, String incomingNumber) {
        SharedPreferences prefs = getSharedPreferences("MoodlitPrefs", MODE_PRIVATE);
        String espIP = prefs.getString("server_ip", "");

        String stateText = stateToText(state);
        Log.d(TAG, "Call state=" + stateText + " | Number=" + incomingNumber);

        if (state == TelephonyManager.CALL_STATE_RINGING) {
            if (incomingNumber != null && !incomingNumber.isEmpty()) {
                lastNumber = incomingNumber;
            }
            lastState = TelephonyManager.EXTRA_STATE_RINGING;

            String category = resolveCategory(prefs, lastNumber);
            Log.d(TAG, "Category resolved: " + category);

            if (!category.equals("unknown")) {
                if (isDndBlocked(prefs, category)) {
                    Log.d(TAG, "DND blocked: " + category);
                    return;
                }

                triggerLamp(espIP, category);
                UserDashboardActivity.logCall(this, category, lastNumber);
                NotifHelper.show(this, category, lastNumber);
            }

        } else if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            lastState = TelephonyManager.EXTRA_STATE_OFFHOOK;

        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (!lastState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                setIdle(espIP);
                NotifHelper.cancel(this);
            }
            lastState = TelephonyManager.EXTRA_STATE_IDLE;
            lastNumber = "";
        }
    }

    private String stateToText(int state) {
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                return "RINGING";
            case TelephonyManager.CALL_STATE_OFFHOOK:
                return "OFFHOOK";
            default:
                return "IDLE";
        }
    }

    private boolean isDndBlocked(SharedPreferences prefs, String category) {
        boolean anxietyFree = prefs.getBoolean("anxiety_free", false);
        if (anxietyFree && (category.equals("toxic") || category.equals("work"))) {
            return true;
        }
        return prefs.getBoolean("dnd_" + category, false);
    }

    private String resolveCategory(SharedPreferences prefs, String number) {
        if (number == null || number.isEmpty()) return "unknown";

        String cleaned = number.replaceAll("[\\s\\-().]+", "");

        String cat = prefs.getString("num_" + cleaned, null);
        if (cat != null) return cat;

        String digits = cleaned.replaceAll("^\\+", "");
        String last10 = digits.length() >= 10
                ? digits.substring(digits.length() - 10)
                : digits;

        cat = prefs.getString("num_" + last10, null);
        if (cat != null) return cat;

        String[] codes = {"+91", "+1", "+44", "+61", "+971", "+65", "+81"};
        for (String code : codes) {
            cat = prefs.getString("num_" + code + last10, null);
            if (cat != null) return cat;
        }

        Log.d(TAG, "No mapping found for: " + cleaned + " (last10=" + last10 + ")");
        return "unknown";
    }

    private void triggerLamp(String ip, String cat) {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + ip + "/trigger?cat=" + cat);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int code = conn.getResponseCode();
                Log.d(TAG, "Trigger HTTP " + code + " → ESP /trigger?cat=" + cat);
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Trigger failed: " + e.getMessage());
            }
        }).start();
    }

    private void setIdle(String ip) {
        new Thread(() -> {
            try {
                URL url = new URL("http://" + ip + "/idle");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

                int code = conn.getResponseCode();
                Log.d(TAG, "Idle HTTP " + code + " → ESP /idle");
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Idle failed: " + e.getMessage());
            }
        }).start();
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Moodlit active")
                .setContentText("Monitoring incoming calls for lamp triggers")
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Moodlit Call Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps Moodlit listening for incoming calls");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallMonitorService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (telephonyManager != null && phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        Log.d(TAG, "CallMonitorService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}