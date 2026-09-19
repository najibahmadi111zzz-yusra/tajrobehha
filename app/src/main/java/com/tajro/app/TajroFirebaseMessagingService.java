package com.tajro.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TajroFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "tajro_chat_messages";
    private static final String CHANNEL_NAME = "پیام‌های تجربه‌ها";
    private static final String CHANNEL_DESCRIPTION = "اعلان پیام‌های متنی و صوتی";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        String title = "پیام جدید";

        String body = "یک پیام جدید دریافت کردید.";

        String messageType = remoteMessage.getData().get("type");

        String senderName = remoteMessage.getData().get("senderName");

        if (senderName != null && !senderName.trim().isEmpty()) {
            title = senderName;
        }

        if ("audio".equals(messageType)) {
            body = "🎙️ پیام صوتی جدید";
        } else if ("text".equals(messageType)) {

            String message = remoteMessage.getData().get("message");

            if (message != null && !message.trim().isEmpty()) {
                body = message;
            } else {
                body = "💬 پیام متنی جدید";
            }

        } else if (remoteMessage.getNotification() != null) {

            String notificationTitle =
                    remoteMessage.getNotification().getTitle();

            String notificationBody =
                    remoteMessage.getNotification().getBody();

            if (notificationTitle != null &&
                    !notificationTitle.trim().isEmpty()) {
                title = notificationTitle;
            }

            if (notificationBody != null &&
                    !notificationBody.trim().isEmpty()) {
                body = notificationBody;
            }
        }

        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);

        /*
         * بعداً این token را داخل users/{uid} در Firestore ذخیره می‌کنیم
         * تا بتوانیم اعلان را دقیقاً برای همان کاربر ارسال کنیم.
         */
    }

    private void showNotification(String title, String body) {

        createNotificationChannel();

        Intent intent = new Intent(this, ChatActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        PendingIntent pendingIntent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            pendingIntent = PendingIntent.getActivity(
                    this,
                    2001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT |
                            PendingIntent.FLAG_IMMUTABLE
            );

        } else {

            pendingIntent = PendingIntent.getActivity(
                    this,
                    2001,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setStyle(
                                new NotificationCompat.BigTextStyle()
                                        .bigText(body)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setVibrate(new long[]{
                                0,
                                300,
                                200,
                                300
                        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        NotificationManagerCompat
                .from(this)
                .notify(
                        (int) System.currentTimeMillis(),
                        builder.build()
                );
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            if (notificationManager == null) {
                return;
            }

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_HIGH
                    );

            channel.setDescription(CHANNEL_DESCRIPTION);

            channel.enableVibration(true);

            channel.setVibrationPattern(new long[]{
                    0,
                    300,
                    200,
                    300
            });

            notificationManager.createNotificationChannel(channel);
        }
    }
}
