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
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class TajroFirebaseMessagingService extends FirebaseMessagingService {

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
        Map<String, String> data = remoteMessage.getData();
        String title = "پیام جدید";
        String body = "یک پیام جدید دریافت کردید.";
        String messageType = data.get("type");
        String senderName = data.get("senderName");
        String messageId = data.get("messageId");

        if (senderName != null && !senderName.trim().isEmpty()) title = senderName;

        if ("audio".equals(messageType)) {
            body = "🎙️ پیام صوتی جدید";
        } else if ("image".equals(messageType)) {
            body = "🖼️ عکس جدید";
        } else if ("video".equals(messageType)) {
            body = "🎬 ویدیوی جدید";
        } else if ("text".equals(messageType)) {
            String safeBody = data.get("body");
            if (safeBody == null || safeBody.trim().isEmpty()) safeBody = data.get("message");
            body = (safeBody != null && !safeBody.trim().isEmpty())
                    ? safeBody : "💬 پیام متنی جدید";
        } else if (remoteMessage.getNotification() != null) {
            String notificationTitle = remoteMessage.getNotification().getTitle();
            String notificationBody = remoteMessage.getNotification().getBody();
            if (notificationTitle != null && !notificationTitle.trim().isEmpty()) title = notificationTitle;
            if (notificationBody != null && !notificationBody.trim().isEmpty()) body = notificationBody;
        }

        showNotification(title, body, messageId);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        if (token == null || token.isEmpty()) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("FCM", "Token refreshed, but no signed-in user is available.");
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("fcmTokenUpdatedAt", FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> Log.i("FCM", "New FCM token saved successfully"))
                .addOnFailureListener(error -> Log.e("FCM", "Could not save refreshed FCM token", error));
    }

    private void showNotification(String title, String body, String messageId) {
        createNotificationChannel();
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (messageId != null && !messageId.trim().isEmpty()) {
            intent.putExtra("messageId", messageId);
        }

        int requestCode = (messageId == null || messageId.isEmpty())
                ? (int) (System.currentTimeMillis() & 0x7fffffff)
                : messageId.hashCode();
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent, pendingFlags);

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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri == null) soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            builder.setSound(soundUri);
            builder.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        int notificationId = messageId == null || messageId.isEmpty()
                ? (int) (System.currentTimeMillis() & 0x7fffffff)
                : messageId.hashCode();
        NotificationManagerCompat.from(this).notify(notificationId, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (soundUri == null) soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            channel.setSound(soundUri, audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
