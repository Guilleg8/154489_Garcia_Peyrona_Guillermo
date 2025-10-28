package com.example.a154489_garcia_peyrona_guillermo;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationsUtils {

    private static final String CHANNEL_ID = "TEMPERATURE_CHANNEL";
    private static final int NOTIFICATION_ID = 101;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Control de Temperatura";
            String description = "Notificaciones sobre la media de temperatura";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static void sendTemperatureNotification(Context context, float average) {
        String title;
        String message;
        int iconResId;

        if (average < 19.0f) {
            title = "Temperatura Baja";
            message = String.format("Media: %.1f°C. ¡Considera encender la calefacción!", average);
            iconResId = R.drawable.ic_temp_low;
        } else if (average > 23.0f) {
            title = "Temperatura Alta";
            message = String.format("Media: %.1f°C. ¡Considera encender el A/C!", average);
            iconResId = R.drawable.ic_temp_high;
        } else {
            title = "Temperatura Óptima";
            message = String.format("Media: %.1f°C. La temperatura es ideal.", average);
            iconResId = R.drawable.ic_temp_ok;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(MainActivity.ACTION_START_NEW_DAY);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(iconResId)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}