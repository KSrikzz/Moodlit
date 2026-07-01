package com.lamp.moodlit;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotifHelper {
    private static final String CHANNEL_ID = "moodlit_channel";
    private static final int NOTIF_ID = 42;

    static void show(Context ctx, String category, String number) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Lamp Alerts", NotificationManager.IMPORTANCE_HIGH));
        }

        if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return;

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getEmoji(category) + " " + capitalize(category) + " is calling")
                .setContentText("Lamp triggered → " + category)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        nm.notify(NOTIF_ID, b.build());
    }

    static void cancel(Context ctx) {
        NotificationManager nm = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
    }

    private static String getEmoji(String cat) {
        switch (cat) {
            case "toxic":   return "🚫";
            case "work":    return "💼";
            case "friends": return "⭐";
            case "family":  return "🏠";
            default:        return "📞";
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}