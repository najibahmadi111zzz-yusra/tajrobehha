package com.tajro.app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class TajroFirebaseMessagingService extends FirebaseMessagingService {

    // شناسه جدید تا تنظیمات کانال قدیمیِ بی‌صدا دوباره استفاده نشود.
    private static final String CHANNEL_ID = "tajro_chat_messages_v2";
    private static final String CHANNEL_NAME = "پیام‌های تجربه‌ها";
    private static final String CHANNEL_DESCRIPTION = "اعلان پیام‌های متنی، تصویری، ویدیویی و صوتی";

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
        } else if ("image".equals(messageType)) {
            body = "🖼️ عکس جدید";
        } else if ("video".equals(messageType)) {
            body = "🎬 ویدیوی جدید";
        } else if ("text".equals(messageType)) {
            String message = remoteMessage.getData().get("message");
            body = (message != null && !message.trim().isEmpty())
                    ? message : "💬 پیام متنی جدید";
        } else if (remoteMessage.getNotification() != null) {
            String notificationTitle = remoteMessage.getNotification().getTitle();
            String notificationBody = remoteMessage.getNotification().getBody();

            if (notificationTitle != null && !notificationTitle.trim().isEmpty()) {
                title = notificationTitle;
            }
            if (notificationBody != null && !notificationBody.trim().isEmpty()) {
                body = notificationBody;
            }
        }

        showNotification(title, body);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // اگر توکن در Firestore ذخیره می‌شود، منطق ذخیره‌سازی پروژه را اینجا نگه دارید.
    }

    private void showNotification(String title, String body) {
        createNotificationChannel();

        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 2001, intent, pendingFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 300, 200, 300});

        // در Android 7.1 و پایین‌تر، صدای اعلان را مستقیم روی اعلان می‌گذاریم.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            builder.setSound(soundUri);
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManagerCompat.from(this).notify(
                (int) (System.currentTimeMillis() & 0x7fffffff),
                builder.build()
        );
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) {
                return;
            }

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            channel.setSound(soundUri, audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }
    }
}
