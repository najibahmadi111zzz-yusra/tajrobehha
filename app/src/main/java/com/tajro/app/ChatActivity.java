package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {

    private static final int RECORD_AUDIO_PERMISSION = 1001;

    private LinearLayout messagesLayout;
    private EditText messageInput;
    private ScrollView scrollView;
    private Button voiceButton;

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

        main.setOrientation(
                LinearLayout.VERTICAL
        );

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

        title.setTextColor(
                Color.rgb(8, 65, 90)
        );

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
                        dp(60),
                        dp(55)
                )
        );

        voiceButton = new Button(this);

        voiceButton.setText("🎤");

        voiceButton.setTextSize(20);

        bottom.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        dp(65),
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

            audioFilePath =
                    getExternalCacheDir()
                            .getAbsolutePath()
                            + "/voice_"
                            + System.currentTimeMillis()
                            + ".3gp";

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

        } catch (IOException e) {

            Toast.makeText(
                    this,
                    "شروع ضبط صدا ناموفق بود",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopRecording() {

        try {

            recorder.stop();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "ضبط صدا ناموفق بود",
                    Toast.LENGTH_SHORT
            ).show();
        }

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

        String fileName =
                "voice_"
                + System.currentTimeMillis()
                + ".3gp";

        StorageReference audioRef =
                storage.getReference()
                        .child("voice_messages")
                        .child(fileName);

        audioRef.putFile(
                android.net.Uri.fromFile(
                        new java.io.File(audioFilePath)
                )
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
                                "آپلود صدا ناموفق بود",
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

        if (mine) {

            messageView.setText(
                    "👤 شما:\n" + message
            );

        } else {

            messageView.setText(
                    "👤 کاربر:\n" + message
            );
        }

        messageView.setTextSize(17);

        messageView.setTextColor(
                Color.DKGRAY
        );

        messageView.setGravity(
                Gravity.RIGHT
        );

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

        if (mine) {

            playButton.setText(
                    "👤 شما   ▶️ پیام صوتی"
            );

        } else {

            playButton.setText(
                    "👤 کاربر   ▶️ پیام صوتی"
            );
        }

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
