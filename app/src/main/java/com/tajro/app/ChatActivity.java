package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ChatActivity extends Activity {

    private static final int MIC = 1001;
    private static final int PICK = 1002;
    private static final int PICK_PROFILE = 1003;

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    /*
     * 🔑 کلید Supabase خودت را اینجا قرار بده.
     */
    private static final String SUPABASE_PUBLISHABLE_KEY =
            "s";

    private static final String MEDIA_BUCKET = "chat_media";
    private static final String VOICE_BUCKET = "voice_messages";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private LinearLayout usersLayout;
    private LinearLayout messagesLayout;
    private ScrollView scroll;

    private EditText input;
    private Button sendButton;
    private Button mediaButton;
    private Button voiceButton;

    private TextView titleText;
    private TextView statusText;

    private ImageView headerAvatar;

    private MediaRecorder recorder;
    private MediaPlayer player;

    private String audioPath;

    private String myId;
    private String receiverId;
    private String receiverName;
    private String currentChatId;

    private boolean insideChat = false;
    private boolean recording = false;
    private boolean typing = false;
    private boolean blocked = false;

    private ListenerRegistration sentMessageListener;
    private ListenerRegistration receivedMessageListener;

    private ListenerRegistration receiverListener;
    private ListenerRegistration typingListener;
    private ListenerRegistration blockListener;

    private final Handler typingHandler =
            new Handler();

    private final Map<String, DocumentSnapshot> messageCache =
            new HashMap<>();

    private int themeColor;

    // =========================================================
    // HELPERS
    // =========================================================

    private int dp(int n) {
        return (int) (
                n *
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void toast(String text) {

        runOnUiThread(
                () ->
                        Toast.makeText(
                                this,
                                text,
                                Toast.LENGTH_LONG
                        ).show()
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        auth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();

        themeColor =
                ThemeManager.getThemeColor(this);

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            auth.signInAnonymously()
                    .addOnSuccessListener(result -> {

                        FirebaseUser u =
                                auth.getCurrentUser();

                        if (u != null) {

                            myId =
                                    u.getUid();

                            createUserProfile();

                            showUsers();
                        }
                    })
                    .addOnFailureListener(
                            e ->
                                    toast(
                                            "خطا در ورود به چت"
                                    )
                    );

        } else {

            myId =
                    user.getUid();

            createUserProfile();

            showUsers();
        }
    }

    // =========================================================
    // PROFILE
    // =========================================================

    private void createUserProfile() {

        if (myId == null)
            return;

        FirebaseUser user =
                auth.getCurrentUser();

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "userId",
                myId
        );

        if (user != null &&
                user.getEmail() != null) {

            data.put(
                    "email",
                    user.getEmail()
            );

        } else {

            data.put(
                    "email",
                    "کاربر"
            );
        }

        data.put(
                "online",
                true
        );

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put(
                "typingTo",
                ""
        );

        db.collection("users")
                .document(myId)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    // =========================================================
    // USERS
    // =========================================================

    private void showUsers() {

        insideChat = false;

        removeListeners();

        messageCache.clear();

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setPadding(
                dp(12),
                dp(18),
                dp(12),
                dp(10)
        );

        root.setBackgroundColor(
                Color.rgb(
                        235,
                        248,
                        250
                )
        );

        TextView title =
                new TextView(this);

        title.setText(
                "💬 پیام‌رسان تجربه‌ها"
        );

        title.setTextSize(25);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setTextColor(
                themeColor
        );

        title.setGravity(
                Gravity.CENTER
        );

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(60)
                )
        );

        TextView info =
                new TextView(this);

        info.setText(
                "👥 یک نفر را انتخاب کنید تا چت خصوصی شروع شود"
        );

        info.setTextSize(15);

        info.setTextColor(
                Color.DKGRAY
        );

        info.setGravity(
                Gravity.RIGHT
        );

        info.setPadding(
                dp(8),
                dp(5),
                dp(8),
                dp(12)
        );

        root.addView(
                info,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        // =====================================================
        // PROFILE PHOTO BUTTON
        // =====================================================

        Button profileButton =
                new Button(this);

        profileButton.setText(
                "👤 عکس پروفایل من"
        );

        profileButton.setTextSize(15);

        profileButton.setTextColor(
                Color.WHITE
        );

        GradientDrawable profileBg =
                new GradientDrawable();

        profileBg.setColor(
                themeColor
        );

        profileBg.setCornerRadius(
                dp(25)
        );

        profileButton.setBackground(
                profileBg
        );

        profileButton.setOnClickListener(
                v ->
                        chooseProfilePhoto()
        );

        LinearLayout.LayoutParams pp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50)
                );

        pp.setMargins(
                dp(5),
                0,
                dp(5),
                dp(10)
        );

        root.addView(
                profileButton,
                pp
        );

        // =====================================================
        // USERS LIST
        // =====================================================

        ScrollView usersScroll =
                new ScrollView(this);

        usersLayout =
                new LinearLayout(this);

        usersLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        usersScroll.addView(
                usersLayout
        );

        root.addView(
                usersScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);

        loadUsers();
    }

    private void loadUsers() {

        usersLayout.removeAllViews();

        db.collection("experiences")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            Set<String> ids =
                                    new LinkedHashSet<>();

                            Map<String, String> emails =
                                    new HashMap<>();

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                String id =
                                        d.getString(
                                                "userId"
                                        );

                                String email =
                                        d.getString(
                                                "authorEmail"
                                        );

                                if (
                                        id == null ||
                                        id.equals(myId)
                                )
                                    continue;

                                ids.add(id);

                                if (email != null)
                                    emails.put(
                                            id,
                                            email
                                    );
                            }

                            if (ids.isEmpty()) {

                                TextView empty =
                                        new TextView(this);

                                empty.setText(
                                        "هنوز کاربر دیگری برای چت پیدا نشد."
                                );

                                empty.setTextSize(17);

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                empty.setPadding(
                                        dp(20),
                                        dp(40),
                                        dp(20),
                                        dp(40)
                                );

                                usersLayout.addView(
                                        empty
                                );

                                return;
                            }

                            for (String id : ids) {

                                String email =
                                        emails.get(id);

                                final String fallback =
                                        email != null &&
                                        !email.trim().isEmpty()
                                                ? email
                                                : "کاربر تجربه‌ها";

                                db.collection("users")
                                        .document(id)
                                        .get()
                                        .addOnSuccessListener(
                                                userDoc -> {

                                                    String name =
                                                            userDoc.getString(
                                                                    "name"
                                                            );

                                                    String photoUrl =
                                                            userDoc.getString(
                                                                    "photoUrl"
                                                            );

                                                    if (
                                                            name == null ||
                                                            name.trim()
                                                                    .isEmpty()
                                                    ) {

                                                        name =
                                                                fallback;
                                                    }

                                                    addUserItem(
                                                            id,
                                                            name,
                                                            photoUrl
                                                    );
                                                }
                                        )
                                        .addOnFailureListener(
                                                e ->
                                                        addUserItem(
                                                                id,
                                                                fallback,
                                                                null
                                                        )
                                        );
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "دریافت کاربران ناموفق بود"
                                )
                );
    }

    private void addUserItem(
            String uid,
            String name,
            String photoUrl) {

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                Color.WHITE
        );

        bg.setCornerRadius(
                dp(22)
        );

        bg.setStroke(
                dp(1),
                Color.rgb(
                        210,
                        230,
                        235
                )
        );

        card.setBackground(bg);

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(78)
                );

        cp.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        card.setLayoutParams(cp);

        // =====================================================
        // USER PROFILE IMAGE
        // =====================================================

        ImageView avatar =
                new ImageView(this);

        avatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable avatarBg =
                new GradientDrawable();

        avatarBg.setColor(
                Color.rgb(
                        235,
                        248,
                        250
                )
        );

        avatarBg.setCornerRadius(
                dp(50)
        );

        avatar.setBackground(
                avatarBg
        );

        avatar.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        card.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(55)
                )
        );

        if (
                photoUrl != null &&
                !photoUrl.trim().isEmpty()
        ) {

            loadImageIntoView(
                    photoUrl,
                    avatar
            );
        }

        LinearLayout texts =
                new LinearLayout(this);

        texts.setOrientation(
                LinearLayout.VERTICAL
        );

        texts.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView nameView =
                new TextView(this);

        nameView.setText(name);

        nameView.setTextSize(17);

        nameView.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        nameView.setTextColor(
                Color.rgb(
                        20,
                        55,
                        65
                )
        );

        TextView status =
                new TextView(this);

        status.setText(
                "برای چت لمس کنید"
        );

        status.setTextSize(13);

        status.setTextColor(
                Color.GRAY
        );

        texts.addView(nameView);
        texts.addView(status);

        card.addView(
                texts,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        card.setOnClickListener(
                v ->
                        openPrivateChat(
                                uid,
                                name
                        )
        );

        usersLayout.addView(card);
    }

    // =========================================================
    // PROFILE PHOTO PICKER
    // =========================================================

    private void chooseProfilePhoto() {

        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        i.setType("image/*");

        startActivityForResult(
                i,
                PICK_PROFILE
        );
    }

    private void uploadProfilePhoto(
            Uri uri) {

        if (myId == null) {

            toast(
                    "کاربر وارد نشده است"
            );

            return;
        }

        toast(
                "👤 در حال ارسال عکس پروفایل..."
        );

        new Thread(() -> {

            try {

                String file =
                        "profile_" +
                        System.currentTimeMillis() +
                        ".jpg";

                String path =
                        "profiles/" +
                        myId +
                        "/" +
                        file;

                String url =
                        SUPABASE_URL +
                        "/storage/v1/object/" +
                        MEDIA_BUCKET +
                        "/" +
                        path;

                HttpURLConnection c =
                        (HttpURLConnection)
                                new URL(url)
                                        .openConnection();

                c.setRequestMethod("POST");

                c.setDoOutput(true);

                c.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Authorization",
                        "Bearer " +
                                SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Content-Type",
                        "image/jpeg"
                );

                c.setRequestProperty(
                        "x-upsert",
                        "true"
                );

                InputStream in =
                        getContentResolver()
                                .openInputStream(uri);

                if (in == null)
                    throw new IOException(
                            "فایل قابل خواندن نیست"
                    );

                OutputStream out =
                        c.getOutputStream();

                byte[] buffer =
                        new byte[8192];

                int n;

                while (
                        (n =
                                in.read(buffer)) != -1
                ) {

                    out.write(
                            buffer,
                            0,
                            n
                    );
                }

                out.flush();

                out.close();

                in.close();

                int code =
                        c.getResponseCode();

                if (
                        code >= 200 &&
                        code < 300
                ) {

                    String publicUrl =
                            SUPABASE_URL +
                            "/storage/v1/object/public/" +
                            MEDIA_BUCKET +
                            "/" +
                            path;

                    db.collection("users")
                            .document(myId)
                            .set(
                                    Collections.singletonMap(
                                            "photoUrl",
                                            publicUrl
                                    ),
                                    SetOptions.merge()
                            )
                            .addOnSuccessListener(
                                    x -> {

                                        toast(
                                                "✅ عکس پروفایل ذخیره شد"
                                        );

                                        showUsers();
                                    }
                            )
                            .addOnFailureListener(
                                    e ->
                                            toast(
                                                    "ذخیره پروفایل ناموفق بود"
                                            )
                            );

                } else {

                    String error =
                            read(
                                    c.getErrorStream()
                            );

                    runOnUiThread(
                            () ->
                                    toast(
                                            "خطای Supabase: " +
                                            code +
                                            "\n" +
                                            error
                                    )
                    );
                }

                c.disconnect();

            } catch (Exception e) {

                runOnUiThread(
                        () ->
                                toast(
                                        "ارسال عکس پروفایل ناموفق بود:\n" +
                                        e.getMessage()
                                )
                );
            }

        }).start();
    }

    // =========================================================
    // IMAGE LOADER
    // =========================================================

    private void loadImageIntoView(
            String url,
            ImageView imageView) {

        new Thread(() -> {

            try {

                HttpURLConnection c =
                        (HttpURLConnection)
                                new URL(url)
                                        .openConnection();

                c.connect();

                InputStream in =
                        c.getInputStream();

                Bitmap bm =
                        BitmapFactory
                                .decodeStream(in);

                in.close();

                c.disconnect();

                runOnUiThread(
                        () -> {

                            if (
                                    bm != null &&
                                    imageView != null
                            ) {

                                imageView.setImageBitmap(
                                        bm
                                );
                            }
                        }
                );

            } catch (Exception ignored) {}
        }).start();
    }

    // =========================================================
    // CHAT ID
    // =========================================================

    private String makeChatId(
            String a,
            String b) {

        if (a == null || b == null)
            return "";

        return a.compareTo(b) < 0
                ? a + "_" + b
                : b + "_" + a;
    }

    // =========================================================
    // OPEN CHAT
    // =========================================================

    private void openPrivateChat(
            String uid,
            String name) {

        if (
                uid == null ||
                uid.equals(myId)
        )
            return;

        receiverId = uid;

        receiverName = name;

        currentChatId =
                makeChatId(
                        myId,
                        receiverId
                );

        insideChat = true;

        messageCache.clear();

        createChatScreen();

        listenMessages();

        listenReceiver();

        listenTyping();

        listenBlockStatus();

        markMessagesAsRead();
    }

    // =========================================================
    // CHAT SCREEN
    // =========================================================

    private void createChatScreen() {

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(
                        235,
                        248,
                        250
                )
        );

        // HEADER
        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        GradientDrawable hb =
                new GradientDrawable();

        hb.setColor(
                themeColor
        );

        hb.setCornerRadius(
                dp(18)
        );

        header.setBackground(hb);

        Button back =
                new Button(this);

        back.setText("‹");

        back.setTextSize(28);

        back.setTextColor(
                Color.WHITE
        );

        back.setBackgroundColor(
                Color.TRANSPARENT
        );

        back.setOnClickListener(
                v -> showUsers()
        );

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(58)
                )
        );

        // =====================================================
        // HEADER PROFILE PHOTO
        // =====================================================

        headerAvatar =
                new ImageView(this);

        headerAvatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        GradientDrawable hab =
                new GradientDrawable();

        hab.setColor(
                Color.WHITE
        );

        hab.setCornerRadius(
                dp(50)
        );

        headerAvatar.setBackground(
                hab
        );

        headerAvatar.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        header.addView(
                headerAvatar,
                new LinearLayout.LayoutParams(
                        dp(50),
                        dp(50)
                )
        );

        LinearLayout headText =
                new LinearLayout(this);

        headText.setOrientation(
                LinearLayout.VERTICAL
        );

        titleText =
                new TextView(this);

        titleText.setText(
                receiverName
        );

        titleText.setTextSize(18);

        titleText.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        titleText.setTextColor(
                Color.WHITE
        );

        statusText =
                new TextView(this);

        statusText.setText(
                "در حال بررسی وضعیت..."
        );

        statusText.setTextSize(12);

        statusText.setTextColor(
                Color.WHITE
        );

        headText.addView(titleText);

        headText.addView(statusText);

        header.addView(
                headText,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        Button menu =
                new Button(this);

        menu.setText("⋮");

        menu.setTextSize(25);

        menu.setTextColor(
                Color.WHITE
        );

        menu.setBackgroundColor(
                Color.TRANSPARENT
        );

        menu.setOnClickListener(
                v -> showChatMenu()
        );

        header.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(55),
                        dp(58)
                )
        );

        root.addView(
                header,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(70)
                )
        );

        // MESSAGES
        scroll =
                new ScrollView(this);

        messagesLayout =
                new LinearLayout(this);

        messagesLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        messagesLayout.setPadding(
                dp(7),
                dp(10),
                dp(7),
                dp(10)
        );

        scroll.addView(messagesLayout);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        // BOTTOM
        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bottom.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        input =
                new EditText(this);

        input.setHint(
                "پیام بنویسید..."
        );

        input.setTextSize(16);

        input.setGravity(
                Gravity.RIGHT |
                Gravity.CENTER_VERTICAL
        );

        input.setPadding(
                dp(15),
                dp(5),
                dp(15),
                dp(5)
        );

        GradientDrawable ib =
                new GradientDrawable();

        ib.setColor(
                Color.WHITE
        );

        ib.setCornerRadius(
                dp(28)
        );

        input.setBackground(ib);

        bottom.addView(
                input,
                new LinearLayout.LayoutParams(
                        0,
                        dp(55),
                        1
                )
        );

        mediaButton =
                makeRoundButton("📷");

        voiceButton =
                makeRoundButton("🎤");

        sendButton =
                makeRoundButton("➤");

        bottom.addView(
                mediaButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        bottom.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        root.addView(bottom);

        setContentView(root);

        sendButton.setOnClickListener(
                v -> sendText()
        );

        mediaButton.setOnClickListener(
                v -> chooseMedia()
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );

        setupTyping();
    }

    private Button makeRoundButton(
            String text) {

        Button b =
                new Button(this);

        b.setText(text);

        b.setTextSize(20);

        b.setTextColor(
                Color.WHITE
        );

        b.setPadding(
                0,
                0,
                0,
                0
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                themeColor
        );

        bg.setCornerRadius(
                dp(50)
        );

        b.setBackground(bg);

        return b;
    }

    // =========================================================
    // SEND TEXT
    // =========================================================

    private void sendText() {

        if (
                !insideChat ||
                myId == null ||
                receiverId == null
        )
            return;

        String text =
                input.getText()
                        .toString()
                        .trim();

        if (text.isEmpty())
            return;

        if (blocked) {

            toast(
                    "این کاربر بلاک شده است"
            );

            return;
        }

        Map<String, Object> m =
                new HashMap<>();

        m.put(
                "chatId",
                currentChatId
        );

        m.put(
                "senderId",
                myId
        );

        m.put(
                "receiverId",
                receiverId
        );

        m.put(
                "type",
                "text"
        );

        m.put(
                "message",
                text
        );

        m.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        m.put(
                "deletedForAll",
                false
        );

        m.put(
                "deletedFor",
                new ArrayList<String>()
        );

        m.put(
                "read",
                false
        );

        db.collection("messages")
                .add(m)
                .addOnSuccessListener(
                        x -> {

                            input.setText("");

                            setTyping(false);

                            scrollBottom();
                        }
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "ارسال پیام ناموفق بود"
                                )
                );
    }

    // =========================================================
    // MESSAGE LISTENERS
    // =========================================================

    private void listenMessages() {

        removeMessageListeners();

        if (
                myId == null ||
                receiverId == null ||
                currentChatId == null
        )
            return;

        sentMessageListener =
                db.collection("messages")
                        .whereEqualTo(
                                "chatId",
                                currentChatId
                        )
                        .whereEqualTo(
                                "senderId",
                                myId
                        )
                        .addSnapshotListener(
                                (snap, error) -> {

                                    if (error != null) {

                                        toast(
                                                "خطا در دریافت پیام‌های ارسالی"
                                        );

                                        return;
                                    }

                                    if (snap == null)
                                        return;

                                    for (
                                            DocumentSnapshot d :
                                            snap.getDocuments()
                                    ) {

                                        messageCache.put(
                                                d.getId(),
                                                d
                                        );
                                    }

                                    renderMessages();
                                }
                        );

        receivedMessageListener =
                db.collection("messages")
                        .whereEqualTo(
                                "chatId",
                                currentChatId
                        )
                        .whereEqualTo(
                                "receiverId",
                                myId
                        )
                        .addSnapshotListener(
                                (snap, error) -> {

                                    if (error != null) {

                                        toast(
                                                "خطا در دریافت پیام‌های دریافتی"
                                        );

                                        return;
                                    }

                                    if (snap == null)
                                        return;

                                    for (
                                            DocumentSnapshot d :
                                            snap.getDocuments()
                                    ) {

                                        messageCache.put(
                                                d.getId(),
                                                d
                                        );
                                    }

                                    renderMessages();

                                    markMessagesAsRead();
                                }
                        );
    }

    private void removeMessageListeners() {

        if (sentMessageListener != null)
            sentMessageListener.remove();

        if (receivedMessageListener != null)
            receivedMessageListener.remove();

        sentMessageListener = null;

        receivedMessageListener = null;
    }

    // =========================================================
    // RENDER
    // =========================================================

    private void renderMessages() {

        if (messagesLayout == null)
            return;

        messagesLayout.removeAllViews();

        List<DocumentSnapshot> list =
                new ArrayList<>(
                        messageCache.values()
                );

        list.sort(
                (a, b) -> {

                    Date da =
                            a.getDate(
                                    "timestamp"
                            );

                    Date dbb =
                            b.getDate(
                                    "timestamp"
                            );

                    if (
                            da == null &&
                            dbb == null
                    )
                        return a.getId()
                                .compareTo(
                                        b.getId()
                                );

                    if (da == null)
                        return 1;

                    if (dbb == null)
                        return -1;

                    return da.compareTo(dbb);
                }
        );

        for (
                DocumentSnapshot d :
                list
        ) {

            if (
                    Boolean.TRUE.equals(
                            d.getBoolean(
                                    "deletedForAll"
                            )
                    )
            )
                continue;

            Object deletedFor =
                    d.get("deletedFor");

            if (
                    deletedFor instanceof List &&
                    ((List<?>) deletedFor)
                            .contains(myId)
            )
                continue;

            String sender =
                    d.getString(
                            "senderId"
                    );

            String type =
                    d.getString(
                            "type"
                    );

            String id =
                    d.getId();

            if (sender == null)
                continue;

            boolean mine =
                    myId.equals(sender);

            if ("audio".equals(type)) {

                String url =
                        d.getString(
                                "audioUrl"
                        );

                if (url != null)
                    addAudio(
                            url,
                            mine,
                            id,
                            sender
                    );

            } else if ("image".equals(type)) {

                String url =
                        d.getString(
                                "mediaUrl"
                        );

                if (url != null)
                    addImage(
                            url,
                            mine,
                            id,
                            sender
                    );

            } else if ("video".equals(type)) {

                String url =
                        d.getString(
                                "mediaUrl"
                        );

                if (url != null)
                    addVideo(
                            url,
                            mine,
                            id,
                            sender
                    );

            } else {

                String message =
                        d.getString(
                                "message"
                        );

                if (message != null)
                    addText(
                            message,
                            mine,
                            id,
                            sender,
                            d
                    );
            }
        }

        scrollBottom();
    }

    // =========================================================
    // TEXT
    // =========================================================

    private void addText(
            String text,
            boolean mine,
            String id,
            String sender,
            DocumentSnapshot d) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.VERTICAL
        );

        /*
         * مهم:
         * کل عرض را می‌گیرد تا راست و چپ واقعاً
         * نسبت به صفحه اعمال شود.
         */
        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        row.setLayoutParams(
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        LinearLayout bubble =
                new LinearLayout(this);

        bubble.setOrientation(
                LinearLayout.VERTICAL
        );

        bubble.setPadding(
                dp(14),
                dp(9),
                dp(14),
                dp(7)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                mine
                        ? Color.rgb(
                                12,
                                91,
                                120
                        )
                        : Color.WHITE
        );

        bg.setCornerRadius(
                dp(20)
        );

        bubble.setBackground(bg);

        TextView tv =
                new TextView(this);

        tv.setText(text);

        tv.setTextSize(17);

        tv.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.rgb(
                                30,
                                45,
                                50
                        )
        );

        tv.setGravity(
                Gravity.RIGHT
        );

        bubble.addView(tv);

        LinearLayout info =
                new LinearLayout(this);

        info.setGravity(
                Gravity.RIGHT |
                Gravity.CENTER_VERTICAL
        );

        TextView time =
                new TextView(this);

        Date date =
                d.getDate(
                        "timestamp"
                );

        time.setText(
                date != null
                        ? formatTime(date)
                        : "..."
        );

        time.setTextSize(10);

        time.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.GRAY
        );

        info.addView(time);

        if (mine) {

            boolean read =
                    Boolean.TRUE.equals(
                            d.getBoolean(
                                    "read"
                            )
                    );

            TextView ticks =
                    new TextView(this);

            ticks.setText(
                    read
                            ? "  ✓✓"
                            : "  ✓"
            );

            ticks.setTextSize(12);

            ticks.setTextColor(
                    read
                            ? Color.rgb(
                                    0,
                                    210,
                                    90
                            )
                            : Color.LTGRAY
            );

            info.addView(ticks);
        }

        bubble.addView(info);

        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(
                        dp(260),
                        -2
                );

        bp.setMargins(
                dp(4),
                dp(3),
                dp(4),
                dp(3)
        );

        row.addView(
                bubble,
                bp
        );

        View.OnLongClickListener listener =
                v -> {

                    showDeleteMenu(
                            id,
                            sender,
                            null,
                            "text"
                    );

                    return true;
                };

        bubble.setOnLongClickListener(
                listener
        );

        tv.setOnLongClickListener(
                listener
        );

        messagesLayout.addView(row);
    }

    // =========================================================
    // READ
    // =========================================================

    private void markMessagesAsRead() {

        if (
                myId == null ||
                currentChatId == null
        )
            return;

        db.collection("messages")
                .whereEqualTo(
                        "chatId",
                        currentChatId
                )
                .whereEqualTo(
                        "receiverId",
                        myId
                )
                .whereEqualTo(
                        "read",
                        false
                )
                .get()
                .addOnSuccessListener(
                        snap -> {

                            for (
                                    DocumentSnapshot d :
                                    snap.getDocuments()
                            ) {

                                db.collection("messages")
                                        .document(
                                                d.getId()
                                        )
                                        .update(
                                                "read",
                                                true
                                        );
                            }
                        }
                );
    }

    // =========================================================
    // MEDIA PICKER
    // =========================================================

    private void chooseMedia() {

        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        i.setType("*/*");

        i.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                }
        );

        startActivityForResult(
                i,
                PICK
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null
        )
            return;

        Uri uri =
                data.getData();

        // =====================================================
        // PROFILE PHOTO
        // =====================================================

        if (requestCode == PICK_PROFILE) {

            uploadProfilePhoto(uri);

            return;
        }

        // =====================================================
        // CHAT MEDIA
        // =====================================================

        if (requestCode != PICK)
            return;

        String mime =
                getContentResolver()
                        .getType(uri);

        if (mime == null) {

            toast(
                    "نوع فایل مشخص نیست"
            );

            return;
        }

        if (mime.startsWith("image/")) {

            uploadMedia(
                    uri,
                    "image",
                    mime
            );

        } else if (
                mime.startsWith("video/")
        ) {

            uploadMedia(
                    uri,
                    "video",
                    mime
            );

        } else {

            toast(
                    "فقط عکس یا ویدیو انتخاب کنید"
            );
        }
    }

    // =========================================================
    // MEDIA UPLOAD
    // =========================================================

    private void uploadMedia(
            Uri uri,
            String type,
            String mime) {

        if (
                myId == null ||
                receiverId == null
        )
            return;

        toast(
                "image".equals(type)
                        ? "📷 در حال ارسال عکس..."
                        : "🎥 در حال ارسال ویدیو..."
        );

        new Thread(() -> {

            try {

                String ext =
                        "image".equals(type)
                                ? ".jpg"
                                : ".mp4";

                String file =
                        type +
                        "_" +
                        System.currentTimeMillis() +
                        ext;

                String path =
                        "chat/" +
                        myId +
                        "/" +
                        file;

                String url =
                        SUPABASE_URL +
                        "/storage/v1/object/" +
                        MEDIA_BUCKET +
                        "/" +
                        path;

                HttpURLConnection c =
                        (HttpURLConnection)
                                new URL(url)
                                        .openConnection();

                c.setRequestMethod("POST");

                c.setDoOutput(true);

                c.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Authorization",
                        "Bearer " +
                                SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Content-Type",
                        mime
                );

                c.setRequestProperty(
                        "x-upsert",
                        "false"
                );

                InputStream in =
                        getContentResolver()
                                .openInputStream(uri);

                if (in == null)
                    throw new IOException(
                            "فایل قابل خواندن نیست"
                    );

                OutputStream out =
                        c.getOutputStream();

                byte[] buffer =
                        new byte[8192];

                int n;

                while (
                        (n =
                                in.read(buffer)) != -1
                ) {

                    out.write(
                            buffer,
                            0,
                            n
                    );
                }

                out.flush();

                out.close();

                in.close();

                int code =
                        c.getResponseCode();

                if (
                        code >= 200 &&
                        code < 300
                ) {

                    String publicUrl =
                            SUPABASE_URL +
                            "/storage/v1/object/public/" +
                            MEDIA_BUCKET +
                            "/" +
                            path;

                    runOnUiThread(
                            () ->
                                    saveMedia(
                                            publicUrl,
                                            type
                                    )
                    );

                } else {

                    String error =
                            read(
                                    c.getErrorStream()
                            );

                    runOnUiThread(
                            () ->
                                    toast(
                                            "خطای Supabase: " +
                                            code +
                                            "\n" +
                                            error
                                    )
                    );
                }

                c.disconnect();

            } catch (Exception e) {

                runOnUiThread(
                        () ->
                                toast(
                                        "آپلود ناموفق بود:\n" +
                                        e.getMessage()
                                )
                );
            }

        }).start();
    }

    private void saveMedia(
            String url,
            String type) {

        Map<String, Object> m =
                new HashMap<>();

        m.put(
                "chatId",
                currentChatId
        );

        m.put(
                "senderId",
                myId
        );

        m.put(
                "receiverId",
                receiverId
        );

        m.put(
                "type",
                type
        );

        m.put(
                "mediaUrl",
                url
        );

        m.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        m.put(
                "deletedForAll",
                false
        );

        m.put(
                "deletedFor",
                new ArrayList<String>()
        );

        m.put(
                "read",
                false
        );

        db.collection("messages")
                .add(m)
                .addOnSuccessListener(
                        x ->
                                toast(
                                        "image".equals(type)
                                                ? "📷 عکس ارسال شد"
                                                : "🎥 ویدیو ارسال شد"
                                )
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "ذخیره رسانه ناموفق بود"
                                )
                );
    }

    // =========================================================
    // AUDIO RECORDING
    // =========================================================

    private void toggleRecording() {

        if (recording)
            stopRecording();
        else
            startRecording();
    }

    private void startRecording() {

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                )
                != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    MIC
            );

            return;
        }

        try {

            File dir =
                    getExternalCacheDir();

            if (dir == null)
                dir = getCacheDir();

            audioPath =
                    new File(
                            dir,
                            "voice_" +
                            System.currentTimeMillis() +
                            ".3gp"
                    ).getAbsolutePath();

            recorder =
                    new MediaRecorder();

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
                    audioPath
            );

            recorder.prepare();

            recorder.start();

            recording = true;

            voiceButton.setText("⏹");

            toast(
                    "🎙️ در حال ضبط..."
            );

        } catch (Exception e) {

            releaseRecorder();

            toast(
                    "شروع ضبط ناموفق بود"
            );
        }
    }

    private void stopRecording() {

        if (recorder == null)
            return;

        try {

            recorder.stop();

            recorder.release();

            recorder = null;

            recording = false;

            voiceButton.setText("🎤");

            toast(
                    "در حال ارسال صدا..."
            );

            uploadAudio();

        } catch (Exception e) {

            releaseRecorder();

            recording = false;

            voiceButton.setText("🎤");

            toast(
                    "ضبط صدا ناموفق بود"
            );
        }
    }

    private void releaseRecorder() {

        try {

            if (recorder != null)
                recorder.release();

        } catch (Exception ignored) {}

        recorder = null;
    }

    // =========================================================
    // AUDIO UPLOAD
    // =========================================================

    private void uploadAudio() {

        if (
                myId == null ||
                receiverId == null
        )
            return;

        File file =
                new File(audioPath);

        if (!file.exists()) {

            toast(
                    "فایل صوتی پیدا نشد"
            );

            return;
        }

        new Thread(() -> {

            try {

                String name =
                        "voice_" +
                        System.currentTimeMillis() +
                        ".3gp";

                String path =
                        "chat/" +
                        myId +
                        "/" +
                        name;

                String url =
                        SUPABASE_URL +
                        "/storage/v1/object/" +
                        VOICE_BUCKET +
                        "/" +
                        path;

                HttpURLConnection c =
                        (HttpURLConnection)
                                new URL(url)
                                        .openConnection();

                c.setRequestMethod("POST");

                c.setDoOutput(true);

                c.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Authorization",
                        "Bearer " +
                                SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Content-Type",
                        "audio/3gpp"
                );

                c.setRequestProperty(
                        "x-upsert",
                        "false"
                );

                InputStream in =
                        new FileInputStream(file);

                OutputStream out =
                        c.getOutputStream();

                byte[] buffer =
                        new byte[8192];

                int n;

                while (
                        (n =
                                in.read(buffer)) != -1
                ) {

                    out.write(
                            buffer,
                            0,
                            n
                    );
                }

                out.flush();

                out.close();

                in.close();

                int code =
                        c.getResponseCode();

                if (
                        code >= 200 &&
                        code < 300
                ) {

                    String publicUrl =
                            SUPABASE_URL +
                            "/storage/v1/object/public/" +
                            VOICE_BUCKET +
                            "/" +
                            path;

                    runOnUiThread(
                            () ->
                                    saveAudio(
                                            publicUrl
                                    )
                    );

                } else {

                    String error =
                            read(
                                    c.getErrorStream()
                            );

                    runOnUiThread(
                            () ->
                                    toast(
                                            "خطای Supabase صدا: " +
                                            code +
                                            "\n" +
                                            error
                                    )
                    );
                }

                c.disconnect();

            } catch (Exception e) {

                runOnUiThread(
                        () ->
                                toast(
                                        "آپلود صدا ناموفق بود:\n" +
                                        e.getMessage()
                                )
                );
            }

        }).start();
    }

    private void saveAudio(
            String url) {

        Map<String, Object> m =
                new HashMap<>();

        m.put(
                "chatId",
                currentChatId
        );

        m.put(
                "senderId",
                myId
        );

        m.put(
                "receiverId",
                receiverId
        );

        m.put(
                "type",
                "audio"
        );

        m.put(
                "audioUrl",
                url
        );

        m.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        m.put(
                "deletedForAll",
                false
        );

        m.put(
                "deletedFor",
                new ArrayList<String>()
        );

        m.put(
                "read",
                false
        );

        db.collection("messages")
                .add(m)
                .addOnSuccessListener(
                        x ->
                                toast(
                                        "🎤 پیام صوتی ارسال شد"
                                )
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "ذخیره صدا ناموفق بود"
                                )
                );
    }

    // =========================================================
    // AUDIO VIEW
    // =========================================================

    private void addAudio(
            String url,
            boolean mine,
            String id,
            String sender) {

        LinearLayout row =
                createMediaRow(mine);

        LinearLayout bubble =
                new LinearLayout(this);

        bubble.setOrientation(
                LinearLayout.HORIZONTAL
        );

        bubble.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bubble.setPadding(
                dp(8),
                dp(5),
                dp(8),
                dp(5)
        );

        GradientDrawable bg =
                new GradientDrawable();

        bg.setColor(
                mine
                        ? Color.rgb(
                                12,
                                91,
                                120
                        )
                        : Color.WHITE
        );

        bg.setCornerRadius(
                dp(22)
        );

        bubble.setBackground(bg);

        TextView label =
                new TextView(this);

        label.setText(
                "پیام صوتی"
        );

        label.setTextSize(15);

        label.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.DKGRAY
        );

        label.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bubble.addView(
                label,
                new LinearLayout.LayoutParams(
                        dp(110),
                        dp(50)
                )
        );

        Button play =
                makeRoundButton("▶");

        play.setOnClickListener(
                v ->
                        playAudio(url)
        );

        bubble.addView(
                play,
                new LinearLayout.LayoutParams(
                        dp(50),
                        dp(50)
                )
        );

        row.addView(
                bubble
        );

        /*
         * حذف فقط با نگه‌داشتن روی پیام.
         */
        View.OnLongClickListener deleteListener =
                v -> {

                    showDeleteMenu(
                            id,
                            sender,
                            url,
                            "audio"
                    );

                    return true;
                };

        row.setOnLongClickListener(
                deleteListener
        );

        bubble.setOnLongClickListener(
                deleteListener
        );

        label.setOnLongClickListener(
                deleteListener
        );

        messagesLayout.addView(row);
    }

    // =========================================================
    // IMAGE
    // =========================================================

    private void addImage(
            String url,
            boolean mine,
            String id,
            String sender) {

        LinearLayout row =
                createMediaRow(mine);

        LinearLayout content =
                new LinearLayout(this);

        content.setOrientation(
                LinearLayout.VERTICAL
        );

        content.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        GradientDrawable contentBg =
                new GradientDrawable();

        contentBg.setColor(
                mine
                        ? Color.rgb(
                                12,
                                91,
                                120
                        )
                        : Color.WHITE
        );

        contentBg.setCornerRadius(
                dp(18)
        );

        content.setBackground(
                contentBg
        );

        content.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        TextView label =
                new TextView(this);

        label.setText(
                "عکس"
        );

        label.setTextSize(14);

        label.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.DKGRAY
        );

        label.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        label.setPadding(
                dp(5),
                dp(3),
                dp(5),
                dp(5)
        );

        content.addView(label);

        ImageView image =
                new ImageView(this);

        image.setAdjustViewBounds(true);

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(250),
                        dp(250)
                )
        );

        /*
         * لمس عادی عکس فقط عکس را باز می‌کند.
         * هیچ سطل زباله‌ای ظاهر نمی‌شود.
         */
        image.setOnClickListener(
                v ->
                        openUrl(url)
        );

        content.addView(image);

        row.addView(content);

        /*
         * حذف فقط با نگه‌داشتن.
         */
        View.OnLongClickListener deleteListener =
                v -> {

                    showDeleteMenu(
                            id,
                            sender,
                            url,
                            "image"
                    );

                    return true;
                };

        row.setOnLongClickListener(
                deleteListener
        );

        content.setOnLongClickListener(
                deleteListener
        );

        label.setOnLongClickListener(
                deleteListener
        );

        image.setOnLongClickListener(
                deleteListener
        );

        messagesLayout.addView(row);

        loadImageIntoView(
                url,
                image
        );
    }

    // =========================================================
    // VIDEO
    // =========================================================

    private void addVideo(
            String url,
            boolean mine,
            String id,
            String sender) {

        LinearLayout row =
                createMediaRow(mine);

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        GradientDrawable boxBg =
                new GradientDrawable();

        boxBg.setColor(
                mine
                        ? Color.rgb(
                                12,
                                91,
                                120
                        )
                        : Color.WHITE
        );

        boxBg.setCornerRadius(
                dp(18)
        );

        box.setBackground(boxBg);

        box.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        TextView label =
                new TextView(this);

        label.setText(
                "ویدیو"
        );

        label.setTextSize(14);

        label.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.DKGRAY
        );

        label.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        box.addView(label);

        VideoView video =
                new VideoView(this);

        video.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(250),
                        dp(220)
                )
        );

        video.setVideoURI(
                Uri.parse(url)
        );

        box.addView(video);

        Button play =
                new Button(this);

        play.setText(
                "▶ پخش ویدیو"
        );

        play.setOnClickListener(
                v ->
                        video.start()
        );

        box.addView(play);

        row.addView(box);

        /*
         * هیچ سطل زباله‌ای کنار ویدیو وجود ندارد.
         * حذف فقط با نگه‌داشتن روی پیام.
         */
        View.OnLongClickListener deleteListener =
                v -> {

                    showDeleteMenu(
                            id,
                            sender,
                            url,
                            "video"
                    );

                    return true;
                };

        row.setOnLongClickListener(
                deleteListener
        );

        box.setOnLongClickListener(
                deleteListener
        );

        label.setOnLongClickListener(
                deleteListener
        );

        video.setOnLongClickListener(
                deleteListener
        );

        messagesLayout.addView(row);
    }

    // =========================================================
    // MEDIA ROW
    // =========================================================

    private LinearLayout createMediaRow(
            boolean mine) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        /*
         * کل عرض صفحه را می‌گیرد.
         * بنابراین mine واقعاً راست و incoming واقعاً چپ
         * قرار می‌گیرد.
         */
        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        row.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        row.setLayoutParams(params);

        return row;
    }

    // =========================================================
    // TYPING
    // =========================================================

    private void setupTyping() {

        input.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {}

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        if (!insideChat)
                            return;

                        setTyping(true);

                        typingHandler
                                .removeCallbacksAndMessages(
                                        null
                                );

                        typingHandler.postDelayed(
                                () ->
                                        setTyping(false),
                                1500
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s) {}
                }
        );
    }

    private void setTyping(
            boolean value) {

        if (
                myId == null ||
                receiverId == null
        )
            return;

        if (typing == value)
            return;

        typing = value;

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "typingTo",
                value
                        ? receiverId
                        : ""
        );

        db.collection("users")
                .document(myId)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void listenTyping() {

        if (typingListener != null)
            typingListener.remove();

        typingListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (snap, error) -> {

                                    if (
                                            error != null ||
                                            snap == null
                                    )
                                        return;

                                    String typingTo =
                                            snap.getString(
                                                    "typingTo"
                                            );

                                    if (
                                            myId.equals(
                                                    typingTo
                                            )
                                    ) {

                                        statusText.setText(
                                                "⌨️ در حال نوشتن..."
                                        );

                                    } else {

                                        updateReceiverStatus(
                                                snap
                                        );
                                    }
                                }
                        );
    }

    // =========================================================
    // ONLINE
    // =========================================================

    private void listenReceiver() {

        if (receiverListener != null)
            receiverListener.remove();

        receiverListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (snap, error) -> {

                                    if (
                                            error != null ||
                                            snap == null
                                    )
                                        return;

                                    updateReceiverStatus(
                                            snap
                                    );
                                }
                        );
    }

    private void updateReceiverStatus(
            DocumentSnapshot snap) {

        Boolean online =
                snap.getBoolean(
                        "online"
                );

        if (
                Boolean.TRUE.equals(
                        online
                )
        ) {

            statusText.setText(
                    "🟢 آنلاین"
            );

        } else {

            Date last =
                    snap.getDate(
                            "lastSeen"
                    );

            statusText.setText(
                    last != null
                            ? "آخرین حضور: " +
                              formatTime(last)
                            : "🔴 آفلاین"
            );
        }

        // =====================================================
        // RECEIVER PROFILE PHOTO
        // =====================================================

        String photoUrl =
                snap.getString(
                        "photoUrl"
                );

        if (
                photoUrl != null &&
                !photoUrl.trim().isEmpty() &&
                headerAvatar != null
        ) {

            loadImageIntoView(
                    photoUrl,
                    headerAvatar
            );
        }
    }

    // =========================================================
    // BLOCK
    // =========================================================

    private void listenBlockStatus() {

        if (blockListener != null)
            blockListener.remove();

        String blockId =
                myId +
                "_" +
                receiverId;

        blockListener =
                db.collection("blocks")
                        .document(blockId)
                        .addSnapshotListener(
                                (snap, error) -> {

                                    blocked =
                                            snap != null &&
                                            snap.exists();
                                }
                        );
    }

    private void blockUser() {

        String blockId =
                myId +
                "_" +
                receiverId;

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "blockerId",
                myId
        );

        data.put(
                "blockedId",
                receiverId
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("blocks")
                .document(blockId)
                .set(data)
                .addOnSuccessListener(
                        x -> {

                            blocked = true;

                            toast(
                                    "🚫 کاربر بلاک شد"
                            );
                        }
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "بلاک کردن ناموفق بود"
                                )
                );
    }

    private void unblockUser() {

        String blockId =
                myId +
                "_" +
                receiverId;

        db.collection("blocks")
                .document(blockId)
                .delete()
                .addOnSuccessListener(
                        x -> {

                            blocked = false;

                            toast(
                                    "کاربر از بلاک خارج شد"
                            );
                        }
                );
    }

    // =========================================================
    // CHAT MENU
    // =========================================================

    private void showChatMenu() {

        String[] options =
                blocked
                        ? new String[]{
                            "✅ رفع بلاک",
                            "🔄 تازه‌سازی چت"
                        }
                        : new String[]{
                            "🚫 بلاک کردن",
                            "🔄 تازه‌سازی چت"
                        };

        new AlertDialog.Builder(this)
                .setTitle(receiverName)
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 1) {

                                listenMessages();

                                markMessagesAsRead();

                                toast(
                                        "🔄 چت تازه شد"
                                );

                                return;
                            }

                            if (blocked) {

                                unblockUser();

                            } else {

                                new AlertDialog.Builder(this)
                                        .setTitle(
                                                "بلاک کردن؟"
                                        )
                                        .setMessage(
                                                "بعد از بلاک، این کاربر نمی‌تواند برای شما پیام بفرستد."
                                        )
                                        .setNegativeButton(
                                                "لغو",
                                                null
                                        )
                                        .setPositiveButton(
                                                "بلاک",
                                                (d, w) ->
                                                        blockUser()
                                        )
                                        .show();
                            }
                        }
                )
                .show();
    }

    // =========================================================
    // DELETE
    // =========================================================

    private void showDeleteMenu(
            String id,
            String sender,
            String url,
            String type) {

        boolean mine =
                myId.equals(sender);

        String[] options =
                mine
                        ? new String[]{
                            "🗑️ حذف برای من",
                            "🗑️ حذف برای همه",
                            "لغو"
                        }
                        : new String[]{
                            "🗑️ حذف برای من",
                            "لغو"
                        };

        new AlertDialog.Builder(this)
                .setTitle(
                        "حذف پیام"
                )
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                deleteForMe(id);

                            } else if (
                                    which == 1 &&
                                    mine
                            ) {

                                new AlertDialog.Builder(this)
                                        .setTitle(
                                                "حذف برای همه؟"
                                        )
                                        .setMessage(
                                                "این پیام از چت هر دو طرف حذف می‌شود."
                                        )
                                        .setNegativeButton(
                                                "لغو",
                                                null
                                        )
                                        .setPositiveButton(
                                                "حذف",
                                                (d, w) ->
                                                        deleteForBoth(
                                                                id,
                                                                url,
                                                                type
                                                        )
                                        )
                                        .show();
                            }
                        }
                )
                .show();
    }

    private void deleteForMe(
            String id) {

        db.collection("messages")
                .document(id)
                .update(
                        "deletedFor",
                        FieldValue.arrayUnion(
                                myId
                        )
                )
                .addOnSuccessListener(
                        x ->
                                toast(
                                        "پیام برای شما حذف شد"
                                )
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "حذف ناموفق بود"
                                )
                );
    }

    private void deleteForBoth(
            String id,
            String url,
            String type) {

        db.collection("messages")
                .document(id)
                .update(
                        "deletedForAll",
                        true
                )
                .addOnSuccessListener(
                        x -> {

                            toast(
                                    "پیام برای هر دو حذف شد"
                            );

                            if (
                                    url != null &&
                                    !"text".equals(type)
                            ) {

                                deleteSupabaseFile(
                                        url,
                                        type
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                toast(
                                        "حذف پیام ناموفق بود"
                                )
                );
    }

    // =========================================================
    // SUPABASE DELETE
    // =========================================================

    private void deleteSupabaseFile(
            String publicUrl,
            String type) {

        new Thread(() -> {

            HttpURLConnection c = null;

            try {

                String bucket =
                        "audio".equals(type)
                                ? VOICE_BUCKET
                                : MEDIA_BUCKET;

                String marker =
                        "/storage/v1/object/public/" +
                        bucket +
                        "/";

                int index =
                        publicUrl.indexOf(marker);

                if (index < 0)
                    return;

                String path =
                        Uri.decode(
                                publicUrl.substring(
                                        index +
                                        marker.length()
                                )
                        );

                String deleteUrl =
                        SUPABASE_URL +
                        "/storage/v1/object/" +
                        bucket +
                        "/" +
                        path;

                c =
                        (HttpURLConnection)
                                new URL(deleteUrl)
                                        .openConnection();

                c.setRequestMethod(
                        "DELETE"
                );

                c.setRequestProperty(
                        "apikey",
                        SUPABASE_PUBLISHABLE_KEY
                );

                c.setRequestProperty(
                        "Authorization",
                        "Bearer " +
                                SUPABASE_PUBLISHABLE_KEY
                );

                c.getResponseCode();

            } catch (Exception ignored) {

            } finally {

                if (c != null)
                    c.disconnect();
            }

        }).start();
    }

    // =========================================================
    // AUDIO PLAYER
    // =========================================================

    private void playAudio(
            String url) {

        try {

            if (player != null) {

                player.release();

                player = null;
            }

            player =
                    new MediaPlayer();

            player.setDataSource(url);

            player.setOnPreparedListener(
                    MediaPlayer::start
            );

            player.setOnCompletionListener(
                    p -> {

                        p.release();

                        player = null;
                    }
            );

            player.prepareAsync();

        } catch (Exception e) {

            toast(
                    "پخش صدا ناموفق بود"
            );
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void openUrl(
            String url) {

        try {

            startActivity(
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    )
            );

        } catch (Exception e) {

            toast(
                    "باز کردن فایل ناموفق بود"
            );
        }
    }

    private String formatTime(
            Date date) {

        Calendar c =
                Calendar.getInstance();

        c.setTime(date);

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY),
                c.get(Calendar.MINUTE)
        );
    }

    private String read(
            InputStream in) {

        if (in == null)
            return "جزئیات خطا موجود نیست";

        try {

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            byte[] b =
                    new byte[1024];

            int n;

            while (
                    (n =
                            in.read(b)) != -1
            ) {

                out.write(
                        b,
                        0,
                        n
                );
            }

            in.close();

            return out.toString(
                    "UTF-8"
            );

        } catch (Exception e) {

            return "خطا";
        }
    }

    private void scrollBottom() {

        if (scroll != null) {

            scroll.post(
                    () ->
                            scroll.fullScroll(
                                    View.FOCUS_DOWN
                            )
            );
        }
    }

    private void removeListeners() {

        removeMessageListeners();

        if (receiverListener != null)
            receiverListener.remove();

        if (typingListener != null)
            typingListener.remove();

        if (blockListener != null)
            blockListener.remove();

        receiverListener = null;

        typingListener = null;

        blockListener = null;
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] results) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                results
        );

        if (requestCode == MIC) {

            if (
                    results.length > 0 &&
                    results[0] ==
                            PackageManager.PERMISSION_GRANTED
            ) {

                startRecording();

            } else {

                toast(
                        "اجازه استفاده از میکروفون داده نشد"
                );
            }
        }
    }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    @Override
    protected void onStart() {

        super.onStart();

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "online",
                    true
            );

            data.put(
                    "lastSeen",
                    FieldValue.serverTimestamp()
            );

            db.collection("users")
                    .document(user.getUid())
                    .set(
                            data,
                            SetOptions.merge()
                    );
        }
    }

    @Override
    protected void onStop() {

        super.onStop();

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "online",
                    false
            );

            data.put(
                    "lastSeen",
                    FieldValue.serverTimestamp()
            );

            data.put(
                    "typingTo",
                    ""
            );

            db.collection("users")
                    .document(user.getUid())
                    .set(
                            data,
                            SetOptions.merge()
                    );
        }
    }

    @Override
    protected void onDestroy() {

        setTyping(false);

        removeListeners();

        releaseRecorder();

        if (player != null) {

            try {

                player.release();

            } catch (Exception ignored) {}

            player = null;
        }

        super.onDestroy();
    }
}
