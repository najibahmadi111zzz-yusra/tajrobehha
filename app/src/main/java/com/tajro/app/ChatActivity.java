package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {

    private static final int RECORD_AUDIO_PERMISSION = 1001;
    private static final int PICK_MEDIA_REQUEST = 1002;

    private LinearLayout messagesLayout;
    private EditText messageInput;
    private ScrollView scrollView;
    private Button voiceButton;
    private Button mediaButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    private MediaRecorder recorder;
    private String audioFilePath;
    private boolean isRecording = false;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        createChatScreen();

        if (auth.getCurrentUser() == null) {

            auth.signInAnonymously()
                    .addOnSuccessListener(result -> loadMessages())
                    .addOnFailureListener(e ->
                            Toast.makeText(
                                    this,
                                    "خطا در اتصال به حساب کاربری",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

        } else {
            loadMessages();
        }
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void createChatScreen() {

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(
                dp(12),
                dp(20),
                dp(12),
                dp(10)
        );

        main.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        TextView title = new TextView(this);
        title.setText("💬 چت تجربه‌ها");
        title.setTextSize(25);
        title.setTextColor(Color.rgb(8, 65, 90));
        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(
                0,
                0,
                0,
                dp(15)
        );

        main.addView(title);

        scrollView = new ScrollView(this);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        messagesLayout.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        scrollView.addView(messagesLayout);

        main.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bottom.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bottom.setPadding(
                0,
                dp(8),
                0,
                0
        );

        messageInput = new EditText(this);
        messageInput.setHint(
                "پیام خود را بنویسید..."
        );

        messageInput.setTextSize(17);
        messageInput.setEnabled(true);
        messageInput.setFocusable(true);
        messageInput.setFocusableInTouchMode(true);
        messageInput.setClickable(true);

        messageInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        messageInput.setGravity(
                Gravity.RIGHT |
                Gravity.CENTER_VERTICAL
        );

        messageInput.setPadding(
                dp(12),
                0,
                dp(12),
                0
        );

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        dp(55),
                        1
                )
        );

        Button sendButton = new Button(this);
        sendButton.setText("📤");
        sendButton.setTextSize(18);

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(55)
                )
        );

        mediaButton = new Button(this);
        mediaButton.setText("📷");
        mediaButton.setTextSize(20);

        bottom.addView(
                mediaButton,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(55)
                )
        );

        voiceButton = new Button(this);
        voiceButton.setText("🎤");
        voiceButton.setTextSize(20);

        bottom.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(55)
                )
        );

        main.addView(bottom);

        setContentView(main);

        messageInput.setOnClickListener(v -> {

            messageInput.requestFocus();

            InputMethodManager imm =
                    (InputMethodManager)
                            getSystemService(
                                    Context.INPUT_METHOD_SERVICE
                            );

            if (imm != null) {

                imm.showSoftInput(
                        messageInput,
                        InputMethodManager.SHOW_IMPLICIT
                );
            }
        });

        sendButton.setOnClickListener(
                v -> sendMessage()
        );

        mediaButton.setOnClickListener(
                v -> chooseMedia()
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );
    }

    private void sendMessage() {

        String message =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {

            Toast.makeText(
                    this,
                    "لطفاً پیام بنویسید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "در حال اتصال...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "message",
                message
        );

        data.put(
                "type",
                "text"
        );

        data.put(
                "senderId",
                auth.getCurrentUser().getUid()
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            messageInput.setText("");

                            scrollToBottom();
                        }
                )
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "ارسال پیام ناموفق بود",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void chooseMedia() {

        Intent intent = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                }
        );

        startActivityForResult(
                intent,
                PICK_MEDIA_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != PICK_MEDIA_REQUEST
                || resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {

            return;
        }

        Uri uri = data.getData();

        String mimeType =
                getContentResolver()
                        .getType(uri);

        if (mimeType == null) {

            Toast.makeText(
                    this,
                    "نوع فایل مشخص نیست",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (mimeType.startsWith("image/")) {

            uploadMedia(
                    uri,
                    "image"
            );

        } else if (mimeType.startsWith("video/")) {

            uploadMedia(
                    uri,
                    "video"
            );

        } else {

            Toast.makeText(
                    this,
                    "فقط عکس یا ویدئو انتخاب کنید",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void uploadMedia(
            Uri uri,
            String type
    ) {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "حساب کاربری آماده نیست",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Toast.makeText(
                this,
                type.equals("image")
                        ? "📷 در حال ارسال عکس..."
                        : "🎥 در حال ارسال ویدئو...",
                Toast.LENGTH_SHORT
        ).show();

        String extension =
                type.equals("image")
                        ? ".jpg"
                        : ".mp4";

        String fileName =
                type +
                        "_" +
                        System.currentTimeMillis() +
                        extension;

        StorageReference mediaRef =
                storage.getReference()
                        .child("chat_media")
                        .child(fileName);

        mediaRef.putFile(uri)
                .addOnSuccessListener(
                        taskSnapshot ->

                                mediaRef.getDownloadUrl()
                                        .addOnSuccessListener(
                                                downloadUri ->
                                                        saveMediaMessage(
                                                                downloadUri.toString(),
                                                                type
                                                        )
                                        )
                                        .addOnFailureListener(e ->
                                                Toast.makeText(
                                                        this,
                                                        "گرفتن لینک فایل ناموفق بود",
                                                        Toast.LENGTH_LONG
                                                ).show()
                                        )
                )
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "آپلود فایل ناموفق بود: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void saveMediaMessage(
            String mediaUrl,
            String type
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "type",
                type
        );

        data.put(
                "mediaUrl",
                mediaUrl
        );

        data.put(
                "senderId",
                auth.getCurrentUser().getUid()
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    type.equals("image")
                                            ? "📷 عکس ارسال شد"
                                            : "🎥 ویدئو ارسال شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            scrollToBottom();
                        }
                )
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "ذخیره پیام ناموفق بود",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void toggleRecording() {

        if (isRecording) {

            stopRecording();

        } else {

            startRecording();
        }
    }

    private void startRecording() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    RECORD_AUDIO_PERMISSION
            );

            return;
        }

        try {

            File directory =
                    getExternalCacheDir();

            if (directory == null) {
                directory = getCacheDir();
            }

            audioFilePath =
                    new File(
                            directory,
                            "voice_" +
                                    System.currentTimeMillis() +
                                    ".3gp"
                    ).getAbsolutePath();

            recorder = new MediaRecorder();

            recorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );

            recorder.setOutputFormat(
                    MediaRecorder.OutputFormat.THREE_GPP
            );

            recorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AMR_NB
            );

            recorder.setOutputFile(
                    audioFilePath
            );

            recorder.prepare();
            recorder.start();

            isRecording = true;

            voiceButton.setText("⏹️");

            Toast.makeText(
                    this,
                    "🎙️ در حال ضبط...",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "شروع ضبط صدا ناموفق بود",
                    Toast.LENGTH_LONG
            ).show();

            if (recorder != null) {

                try {
                    recorder.release();
                } catch (Exception ignored) {
                }

                recorder = null;
            }
        }
    }

    private void stopRecording() {

        if (recorder == null) {
            return;
        }

        try {

            recorder.stop();
            recorder.release();
            recorder = null;

            isRecording = false;

            voiceButton.setText("🎤");

            Toast.makeText(
                    this,
                    "📤 در حال ارسال صدا...",
                    Toast.LENGTH_SHORT
            ).show();

            uploadAudio();

        } catch (Exception e) {

            if (recorder != null) {

                try {
                    recorder.release();
                } catch (Exception ignored) {
                }

                recorder = null;
            }

            isRecording = false;

            voiceButton.setText("🎤");

            Toast.makeText(
                    this,
                    "ضبط صدا ناموفق بود",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void uploadAudio() {

        if (auth.getCurrentUser() == null) {

            Toast.makeText(
                    this,
                    "حساب کاربری آماده نیست",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File audioFile =
                new File(audioFilePath);

        if (!audioFile.exists()) {

            Toast.makeText(
                    this,
                    "فایل صوتی پیدا نشد",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String fileName =
                "voice_" +
                        System.currentTimeMillis() +
                        ".3gp";

        StorageReference audioRef =
                storage.getReference()
                        .child("voice_messages")
                        .child(fileName);

        audioRef.putFile(
                Uri.fromFile(audioFile)
        )
                .addOnSuccessListener(
                        taskSnapshot ->

                                audioRef.getDownloadUrl()
                                        .addOnSuccessListener(
                                                uri ->
                                                        saveAudioMessage(
                                                                uri.toString()
                                                        )
                                        )
                )
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "آپلود صدا ناموفق بود: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void saveAudioMessage(
            String audioUrl
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "type",
                "audio"
        );

        data.put(
                "audioUrl",
                audioUrl
        );

        data.put(
                "senderId",
                auth.getCurrentUser().getUid()
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    "🎤 پیام صوتی ارسال شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            audioFile.delete();

                            scrollToBottom();
                        }
                )
                .addOnFailureListener(e ->

                        Toast.makeText(
                                this,
                                "ذخیره پیام صوتی ناموفق بود",
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void loadMessages() {

        db.collection("messages")
                .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                )
                .addSnapshotListener(
                        (snapshots, error) -> {

                            if (error != null) {

                                Toast.makeText(
                                        this,
                                        "خطا در دریافت پیام‌ها",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            messagesLayout.removeAllViews();

                            if (snapshots == null) {
                                return;
                            }

                            String myId =
                                    auth.getCurrentUser() != null
                                            ? auth.getCurrentUser().getUid()
                                            : "";

                            for (
                                    DocumentSnapshot document :
                                    snapshots.getDocuments()
                            ) {

                                String type =
                                        document.getString(
                                                "type"
                                        );

                                String senderId =
                                        document.getString(
                                                "senderId"
                                        );

                                boolean mine =
                                        myId.equals(senderId);

                                if ("audio".equals(type)) {

                                    String audioUrl =
                                            document.getString(
                                                    "audioUrl"
                                            );

                                    if (audioUrl != null) {

                                        addAudioMessage(
                                                audioUrl,
                                                mine
                                        );
                                    }

                                } else if (
                                        "image".equals(type)
                                ) {

                                    String mediaUrl =
                                            document.getString(
                                                    "mediaUrl"
                                            );

                                    if (mediaUrl != null) {

                                        addImageMessage(
                                                mediaUrl,
                                                mine
                                        );
                                    }

                                } else if (
                                        "video".equals(type)
                                ) {

                                    String mediaUrl =
                                            document.getString(
                                                    "mediaUrl"
                                            );

                                    if (mediaUrl != null) {

                                        addVideoMessage(
                                                mediaUrl,
                                                mine
                                        );
                                    }

                                } else {

                                    String message =
                                            document.getString(
                                                    "message"
                                            );

                                    if (message == null) {
                                        continue;
                                    }

                                    addTextMessage(
                                            message,
                                            mine
                                    );
                                }
                            }

                            scrollToBottom();
                        }
                );
    }

    private void addTextMessage(
            String message,
            boolean mine
    ) {

        TextView messageView =
                new TextView(this);

        messageView.setText(
                mine
                        ? "👤 شما:\n" + message
                        : "👤 کاربر:\n" + message
        );

        messageView.setTextSize(17);
        messageView.setTextColor(Color.DKGRAY);
        messageView.setGravity(Gravity.RIGHT);

        messageView.setPadding(
                dp(15),
                dp(12),
                dp(15),
                dp(12)
        );

        messagesLayout.addView(
                messageView
        );
    }

    private void addAudioMessage(
            String audioUrl,
            boolean mine
    ) {

        Button playButton =
                new Button(this);

        playButton.setText(
                mine
                        ? "👤 شما   ▶️ پیام صوتی"
                        : "👤 کاربر   ▶️ پیام صوتی"
        );

        playButton.setTextSize(16);

        playButton.setOnClickListener(
                v -> playAudio(audioUrl)
        );

        messagesLayout.addView(
                playButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(60)
                )
        );
    }

    private void addImageMessage(
            String imageUrl,
            boolean mine
    ) {

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setGravity(
                Gravity.RIGHT
        );

        TextView label =
                new TextView(this);

        label.setText(
                mine
                        ? "👤 شما  📷 عکس"
                        : "👤 کاربر  📷 عکس"
        );

        label.setTextSize(15);

        ImageView imageView =
                new ImageView(this);

        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        imageView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(260),
                        dp(260)
                )
        );

        imageView.setOnClickListener(
                v -> openMediaUrl(imageUrl)
        );

        container.addView(label);
        container.addView(imageView);

        messagesLayout.addView(container);

        new Thread(() -> {

            try {

                java.net.URL url =
                        new java.net.URL(
                                imageUrl
                        );

                java.net.HttpURLConnection connection =
                        (java.net.HttpURLConnection)
                                url.openConnection();

                connection.connect();

                java.io.InputStream input =
                        connection.getInputStream();

                final android.graphics.Bitmap bitmap =
                        android.graphics.BitmapFactory
                                .decodeStream(input);

                input.close();

                runOnUiThread(() -> {

                    if (bitmap != null) {

                        imageView.setImageBitmap(
                                bitmap
                        );
                    }
                });

            } catch (Exception ignored) {
            }
        }).start();
    }

    private void addVideoMessage(
            String videoUrl,
            boolean mine
    ) {

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setGravity(
                Gravity.RIGHT
        );

        TextView label =
                new TextView(this);

        label.setText(
                mine
                        ? "👤 شما  🎥 ویدئو"
                        : "👤 کاربر  🎥 ویدئو"
        );

        label.setTextSize(15);

        VideoView videoView =
                new VideoView(this);

        videoView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(240)
                )
        );

        videoView.setVideoURI(
                Uri.parse(videoUrl)
        );

        Button playButton =
                new Button(this);

        playButton.setText(
                "▶️ پخش ویدئو"
        );

        playButton.setOnClickListener(
                v -> {

                    videoView.start();

                    Toast.makeText(
                            this,
                            "🎥 در حال پخش...",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        );

        container.addView(label);
        container.addView(videoView);
        container.addView(playButton);

        messagesLayout.addView(
                container
        );
    }

    private void openMediaUrl(
            String url
    ) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "باز کردن فایل ناموفق بود",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void playAudio(
            String url
    ) {

        try {

            if (mediaPlayer != null) {

                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer =
                    new MediaPlayer();

            mediaPlayer.setDataSource(
                    url
            );

            mediaPlayer.setOnPreparedListener(
                    mp -> mp.start()
            );

            mediaPlayer.setOnCompletionListener(
                    mp -> {

                        mp.release();
                        mediaPlayer = null;
                    }
            );

            mediaPlayer.prepareAsync();

            Toast.makeText(
                    this,
                    "▶️ در حال پخش...",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "پخش صدا ناموفق بود",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void scrollToBottom() {

        if (scrollView != null) {

            scrollView.post(() ->
                    scrollView.fullScroll(
                            View.FOCUS_DOWN
                    )
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                RECORD_AUDIO_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                startRecording();

            } else {

                Toast.makeText(
                        this,
                        "اجازه استفاده از میکروفون داده نشد",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (recorder != null) {

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }

        if (mediaPlayer != null) {

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }
    }
    }
