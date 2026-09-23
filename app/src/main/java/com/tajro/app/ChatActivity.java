package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends Activity {

    private static final int REQUEST_MIC = 1001;
    private static final int PICK_MEDIA = 1002;
    private static final int PICK_PROFILE = 1003;
    private static final int REQUEST_SAVE = 1004;

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    private static final String SUPABASE_PUBLISHABLE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";

    private static final String CHAT_BUCKET = "chat_media";
    private static final String VOICE_BUCKET = "voice_messages";

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout root;
    private LinearLayout usersContainer;
    private LinearLayout messagesContainer;
    private ScrollView messagesScroll;

    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton mediaButton;
    private ImageButton voiceButton;

    private TextView titleText;
    private TextView statusText;
    private ImageView headerAvatar;
    private TextView headerOnlineDot;
    private TextView chatMenuButton;

    private String myId;
    private String receiverId;
    private String receiverName;
    private String receiverPhotoUrl;
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

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String audioPath;

    private final android.os.Handler typingHandler =
            new android.os.Handler();

    private final Map<String, DocumentSnapshot> messageCache =
            new HashMap<>();

    private final Set<String> hiddenUserIds =
            new HashSet<>();

    private int themeColor = Color.rgb(12, 91, 120);

    private String pendingSaveUrl;
    private String pendingSaveName = "tajrobehha.jpg";

    private interface SupabaseUploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "لطفاً اول وارد حساب شوید",
                    Toast.LENGTH_LONG
            ).show();

            try {
                startActivity(
                        new Intent(this, AccountActivity.class)
                );
            } catch (Exception ignored) {
            }

            finish();
            return;
        }

        myId = user.getUid();

        ensureUserProfile();
        createUsersScreen();
        loadUsers();
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density +
                        0.5f
        );
    }

    private GradientDrawable bg(
            int color,
            float radius
    ) {

        GradientDrawable d =
                new GradientDrawable();

        d.setColor(color);

        d.setCornerRadius(
                dp((int) radius)
        );

        return d;
    }

    private TextView text(
            String value,
            int size
    ) {

        TextView t =
                new TextView(this);

        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.DKGRAY);

        t.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6)
        );

        return t;
    }

    private ImageView avatarView(int size) {

        ImageView image =
                new ImageView(this);

        image.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(size),
                        dp(size)
                )
        );

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        image.setBackground(
                bg(Color.LTGRAY, 50)
        );

        return image;
    }

    private void ensureUserProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        Map<String, Object> data =
                new HashMap<>();

        data.put("userId", myId);

        data.put(
                "email",
                user.getEmail() == null
                        ? ""
                        : user.getEmail()
        );

        data.put("online", true);

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put("typingTo", "");

        if (user.getPhoneNumber() != null) {

            data.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );
        }

        if (user.getDisplayName() != null &&
                !user.getDisplayName()
                        .trim()
                        .isEmpty()) {

            data.put(
                    "name",
                    user.getDisplayName()
            );
        }

        db.collection("users")
                .document(myId)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private void createUsersScreen() {

        insideChat = false;

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        header.setBackgroundColor(
                themeColor
        );

        ImageButton profile =
                new ImageButton(this);

        profile.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        profile.setBackgroundColor(
                Color.TRANSPARENT
        );

        profile.setOnClickListener(
                v -> showMyProfile()
        );

    header.addView(
        profile,
        new LinearLayout.LayoutParams(
                dp(48),
                dp(48)
        )
);

TextView myProfileMenu =
        text("⋮", 26);

myProfileMenu.setTextColor(
        Color.WHITE
);

myProfileMenu.setGravity(
        Gravity.CENTER
);

myProfileMenu.setOnClickListener(
        v -> showChatPrivacySettings()
);

header.addView(
        myProfileMenu,
        new LinearLayout.LayoutParams(
                dp(40),
                dp(48)
        )
);

titleText =
        text("💬 کاربران", 21);

        titleText.setTextColor(
                Color.WHITE
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                );

        header.addView(
                titleText,
                titleParams
        );

        ImageButton refresh =
                new ImageButton(this);

        refresh.setImageResource(
                android.R.drawable.ic_popup_sync
        );

        refresh.setBackgroundColor(
                Color.TRANSPARENT
        );

        refresh.setOnClickListener(
                v -> loadUsers()
        );

        header.addView(
                refresh,
                new LinearLayout.LayoutParams(
                        dp(48),
                        dp(48)
                )
        );

        root.addView(header);

        TextView info =
                text(
                        "یک کاربر را انتخاب کنید تا چت خصوصی باز شود.",
                        14
                );

        info.setGravity(
                Gravity.CENTER
        );

        root.addView(info);

        ScrollView scroll =
                new ScrollView(this);

        usersContainer =
                new LinearLayout(this);

        usersContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        usersContainer.setPadding(
                dp(8),
                dp(4),
                dp(8),
                dp(12)
        );

        scroll.addView(usersContainer);

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private void loadUsers() {

        if (usersContainer == null) return;

        usersContainer.removeAllViews();

        db.collection("hiddenUsers")
                .whereEqualTo(
                        "ownerId",
                        myId
                )
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            hiddenUserIds.clear();

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                String id =
                                        d.getString(
                                                "hiddenUserId"
                                        );

                                if (id != null) {
                                    hiddenUserIds.add(id);
                                }
                            }

                            loadVisibleUsers();
                        }
                )
                .addOnFailureListener(
                        e -> loadVisibleUsers()
                );
    }

    private void loadVisibleUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            usersContainer
                                    .removeAllViews();

                            List<DocumentSnapshot> users =
                                    new ArrayList<>();

                            Set<String> ids =
                                    new HashSet<>();

                            for (
                                    DocumentSnapshot d :
                                    snapshot.getDocuments()
                            ) {

                                String id =
                                        d.getId();

                                if (id.equals(myId)) {
                                    continue;
                                }

                                if (hiddenUserIds
                                        .contains(id)) {
                                    continue;
                                }

                                if (!ids.add(id)) {
                                    continue;
                                }

                                users.add(d);
                            }

                            Collections.sort(
                                    users,
                                    Comparator.comparing(
                                            d -> {

                                                String n =
                                                        d.getString(
                                                                "name"
                                                        );

                                                return n == null
                                                        ? ""
                                                        : n;
                                            },
                                            String.CASE_INSENSITIVE_ORDER
                                    )
                            );

                            if (users.isEmpty()) {

                                TextView empty =
                                        text(
                                                "هنوز کاربر دیگری پیدا نشد.",
                                                16
                                        );

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                usersContainer
                                        .addView(empty);

                                return;
                            }

                            for (
                                    DocumentSnapshot d :
                                    users
                            ) {

                                addUserItem(d);
                            }
                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "خطا در دریافت کاربران",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void addUserItem(
            DocumentSnapshot d
    ) {

        String uid =
                d.getId();

        String name =
                d.getString("name");

        if (name == null ||
                name.trim().isEmpty()) {

            name =
                    d.getString("email");
        }

        if (name == null ||
                name.trim().isEmpty()) {

            name = "کاربر";
        }

        String photoUrl =
                d.getString("photoUrl");

        Boolean online =
                d.getBoolean("online");

        final String finalUid = uid;
        final String finalName = name;
        final String finalPhotoUrl = photoUrl;

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                dp(10),
                dp(8),
                dp(10),
                dp(8)
        );

        card.setBackground(
                bg(Color.WHITE, 18)
        );

        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        cp.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        card.setLayoutParams(cp);

        ImageView avatar =
                avatarView(76);

        if (finalPhotoUrl != null &&
                !finalPhotoUrl.isEmpty()) {

            loadImage(
                    finalPhotoUrl,
                    avatar
            );
        }

        avatar.setOnClickListener(
                v ->
                        showProfileDialog(
                                finalUid,
                                finalName,
                                finalPhotoUrl
                        )
        );

        card.addView(avatar);

        LinearLayout info =
                new LinearLayout(this);

        info.setOrientation(
                LinearLayout.VERTICAL
        );

        info.setPadding(
                dp(10),
                0,
                dp(5),
                0
        );

        TextView nameText =
                text(
                        finalName,
                        17
                );

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        LinearLayout statusRow =
                new LinearLayout(this);

        statusRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        TextView dot =
                text(
                        online != null &&
                                online
                                ? "●"
                                : "○",
                        14
                );

        dot.setTextColor(
                online != null && online
                        ? Color.rgb(0, 160, 70)
                        : Color.GRAY
        );

        TextView status =
                text(
                        online != null && online
                                ? " آنلاین"
                                : " آفلاین",
                        13
                );

        statusRow.addView(dot);
        statusRow.addView(status);

        info.addView(nameText);
        info.addView(statusRow);

        card.addView(
                info,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        TextView menu =
                text("⋮", 26);

        menu.setGravity(
                Gravity.CENTER
        );

        menu.setOnClickListener(
                v ->
                        showUserMenu(
                                finalUid,
                                finalName
                        )
        );

        card.addView(
                menu,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(60)
                )
        );

        View.OnClickListener open =
                v ->
                        openPrivateChat(
                                finalUid,
                                finalName,
                                finalPhotoUrl
                        );

        card.setOnClickListener(open);
        nameText.setOnClickListener(open);
        info.setOnClickListener(open);

        usersContainer.addView(card);
    }
    
private void showUserMenu(
        String uid,
        String name
) {

    String[] items = {
            "👁 مخفی کردن / رفع مخفی",
            "🚫 بلاک / رفع مسدودیت"
    };

    new AlertDialog.Builder(this)
            .setTitle(name)
            .setItems(
                    items,
                    (d, which) -> {

                        if (which == 0) {

                            // بررسی وضعیت مخفی بودن
                            db.collection("hiddenUsers")
                                    .whereEqualTo("ownerId", myId)
                                    .whereEqualTo("hiddenUserId", uid)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {

                                        if (!snapshot.isEmpty()) {

                                            // رفع مخفی
                                            for (com.google.firebase.firestore.DocumentSnapshot document
                                                    : snapshot.getDocuments()) {

                                                db.collection("hiddenUsers")
                                                        .document(document.getId())
                                                        .delete();
                                            }

                                            Toast.makeText(
                                                    this,
                                                    "مخفی بودن کاربر رفع شد",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();

                                        } else {

                                            // مخفی کردن
                                            Map<String, Object> data =
                                                    new HashMap<>();

                                            data.put(
                                                    "ownerId",
                                                    myId
                                            );

                                            data.put(
                                                    "hiddenUserId",
                                                    uid
                                            );

                                            data.put(
                                                    "hiddenUserName",
                                                    name
                                            );

                                            db.collection("hiddenUsers")
                                                    .add(data)
                                                    .addOnSuccessListener(
                                                            x -> {
                                                                Toast.makeText(
                                                                        this,
                                                                        "کاربر مخفی شد",
                                                                        Toast.LENGTH_SHORT
                                                                ).show();

                                                                loadUsers();
                                                            }
                                                    );
                                        }
                                    });

                        } else {

                            // بررسی وضعیت بلاک
                            db.collection("blocks")
                                    .whereEqualTo("ownerId", myId)
                                    .whereEqualTo("blockedUserId", uid)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {

                                        if (!snapshot.isEmpty()) {

                                            // رفع مسدودیت
                                            for (com.google.firebase.firestore.DocumentSnapshot document
                                                    : snapshot.getDocuments()) {

                                                db.collection("blocks")
                                                        .document(document.getId())
                                                        .delete();
                                            }

                                            Toast.makeText(
                                                    this,
                                                    "مسدودیت کاربر رفع شد",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();

                                        } else {

                                            // بلاک کردن
                                            Map<String, Object> data =
                                                    new HashMap<>();

                                            data.put(
                                                    "ownerId",
                                                    myId
                                            );

                                            data.put(
                                                    "blockedUserId",
                                                    uid
                                            );

                                            data.put(
                                                    "blockedUserName",
                                                    name
                                            );

                                            db.collection("blocks")
                                                    .add(data)
                                                    .addOnSuccessListener(
                                                            x ->
                                                                    Toast.makeText(
                                                                            this,
                                                                            "کاربر بلاک شد",
                                                                            Toast.LENGTH_SHORT
                                                                    ).show()
                                                    );
                                        }
                                    });
                        }
                    }
            )
            .show();
}

    private void showMyProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) return;

        db.collection("users")
                .document(myId)
                .get()
                .addOnSuccessListener(
                        d -> {

                            String name =
                                    d.getString("name");

                            String photo =
                                    d.getString("photoUrl");

                            if (name == null) {
                                name = "کاربر";
                            }

                            showProfileDialog(
                                    myId,
                                    name,
                                    photo
                            );
                        }
                );
    }

    private void showProfileDialog(
            String uid,
            String name,
            String photoUrl
    ) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                Gravity.CENTER
        );

        box.setPadding(
                dp(20),
                dp(15),
                dp(20),
                dp(15)
        );

        ImageView image =
                avatarView(220);

        if (photoUrl != null &&
                !photoUrl.isEmpty()) {

            loadImage(
                    photoUrl,
                    image
            );
        }

        image.setOnClickListener(
                v -> {

                    if (photoUrl != null &&
                            !photoUrl.isEmpty()) {

                        showImageViewer(
                                photoUrl
                        );
                    }
                }
        );

        box.addView(image);

        TextView nameText =
                text(
                        name,
                        20
                );

        nameText.setGravity(
                Gravity.CENTER
        );

        nameText.setTypeface(
                null,
                Typeface.BOLD
        );

        box.addView(nameText);

        Button change =
                new Button(this);

        change.setText(
                "📷 تغییر عکس پروفایل"
        );

        if (!uid.equals(myId)) {

            change.setVisibility(
                    View.GONE
            );
        }

        change.setOnClickListener(
                v -> pickProfilePhoto()
        );

        box.addView(change);
        
if (uid.equals(myId)) {

    Button privacy =
            new Button(this);

    privacy.setText(
            "🔒 تنظیمات حریم خصوصی"
    );

    privacy.setOnClickListener(
            v -> showChatPrivacySettings()
    );

    box.addView(privacy);


    Button blocked =
            new Button(this);

    blocked.setText(
            "🚫 فهرست مسدودشده‌ها"
    );

    blocked.setOnClickListener(
            v -> showBlockedUsers()
    );

    box.addView(blocked);
}
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle("پروفایل")
                        .setView(box)
                        .setPositiveButton(
                                "بستن",
                                null
                        )
                        .create();

        dialog.show();
    }

    private void showBlockedUsers() {

    db.collection("blocks")
            .whereEqualTo(
                    "ownerId",
                    myId
            )
            .get()
            .addOnSuccessListener(
                    snapshot -> {

                        if (snapshot.isEmpty()) {

                            new AlertDialog.Builder(this)
                                    .setTitle(
                                            "🚫 فهرست مسدودشده‌ها"
                                    )
                                    .setMessage(
                                            "هیچ کاربری مسدود نشده است."
                                    )
                                    .setPositiveButton(
                                            "بستن",
                                            null
                                    )
                                    .show();

                            return;
                        }

                        LinearLayout box =
                                new LinearLayout(this);

                        box.setOrientation(
                                LinearLayout.VERTICAL
                        );

                        box.setPadding(
                                dp(20),
                                dp(10),
                                dp(20),
                                dp(10)
                        );

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            String uid =
                                    doc.getString(
                                            "blockedUserId"
                                    );

                            String name =
                                    doc.getString(
                                            "blockedUserName"
                                    );

                            if (uid == null ||
                                    uid.isEmpty()) {
                                continue;
                            }

                            if (name == null ||
                                    name.isEmpty()) {
                                name = "کاربر";
                            }

                            Button unblock =
                                    new Button(this);

                            unblock.setText(
                                    "🚫 " +
                                    name +
                                    "   •   رفع مسدودی"
                            );

                            String finalName = name;

                            unblock.setOnClickListener(
                                    v -> {

                                        new AlertDialog.Builder(
                                                this
                                        )
                                                .setTitle(
                                                        "رفع مسدودی"
                                                )
                                                .setMessage(
                                                        "آیا می‌خواهید «" +
                                                        finalName +
                                                        "» را از مسدودی خارج کنید؟"
                                                )
                                                .setNegativeButton(
                                                        "لغو",
                                                        null
                                                )
                                                .setPositiveButton(
                                                        "رفع مسدودی",
                                                        (d, w) -> {

                                                            db.collection(
                                                                    "blocks"
                                                            )
                                                                    .document(
                                                                            doc.getId()
                                                                    )
                                                                    .delete()
                                                                    .addOnSuccessListener(
                                                                            x -> {

                                                                                Toast.makeText(
                                                                                        this,
                                                                                        "مسدودی برداشته شد",
                                                                                        Toast.LENGTH_SHORT
                                                                                ).show();

                                                                                showBlockedUsers();
                                                                            }
                                                                    );
                                                        }
                                                )
                                                .show();
                                    }
                            );

                            box.addView(
                                    unblock
                            );
                        }

                        new AlertDialog.Builder(this)
                                .setTitle(
                                        "🚫 فهرست مسدودشده‌ها"
                                )
                                .setView(box)
                                .setPositiveButton(
                                        "بستن",
                                        null
                                )
                                .show();
                    }
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "دریافت فهرست مسدودشده‌ها انجام نشد",
                            Toast.LENGTH_SHORT
                    ).show()
            );
}
    private void openPrivateChat(
            String uid,
            String name,
            String photoUrl
    ) {

        receiverId = uid;
        receiverName = name;
receiverPhotoUrl = photoUrl;
        
        currentChatId =
                makeChatId(
                        myId,
                        receiverId
                );

        messageCache.clear();

        insideChat = true;
        blocked = false;

        createChatScreen(photoUrl);

        listenMessages();
        listenReceiver();
        listenTyping();
        listenBlock();
    }

    private String makeChatId(
            String a,
            String b
    ) {

        if (a.compareTo(b) < 0) {
            return a + "_" + b;
        }

        return b + "_" + a;
    }

    private void createChatScreen(
            String photoUrl
    ) {

        root =
                new LinearLayout(this);

        root.setOrientation(
                LinearLayout.VERTICAL
        );

        root.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
        );

        header.setBackgroundColor(
                themeColor
        );

        ImageButton back =
                new ImageButton(this);

        back.setImageResource(
                android.R.drawable.ic_media_previous
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
                        dp(48),
                        dp(48)
                )
        );

        headerAvatar =
                avatarView(62);

        if (photoUrl != null &&
                !photoUrl.isEmpty()) {

            loadImage(
                    photoUrl,
                    headerAvatar
            );
        }

        headerAvatar.setOnClickListener(
                v -> {

                    if (photoUrl != null &&
                            !photoUrl.isEmpty()) {

                        showImageViewer(
                                photoUrl
                        );
                    }
                }
        );

        header.addView(headerAvatar);

        LinearLayout titles =
                new LinearLayout(this);

        titles.setOrientation(
                LinearLayout.VERTICAL
        );

        titles.setPadding(
                dp(8),
                0,
                0,
                0
        );

        titleText =
                text(
                        receiverName,
                        18
                );

        titleText.setTextColor(
                Color.WHITE
        );

        titleText.setTypeface(
                null,
                Typeface.BOLD
        );

        statusText =
                text(
                        "در حال بررسی...",
                        13
                );

        statusText.setTextColor(
                Color.WHITE
        );

        titles.addView(titleText);
        titles.addView(statusText);

        header.addView(
                titles,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        headerOnlineDot =
                text("●", 15);

        headerOnlineDot.setTextColor(
                Color.GREEN
        );
        
        TextView chatMenu = new TextView(this);
chatMenu.setText("⋮");
chatMenu.setTextColor(Color.WHITE);
chatMenu.setTextSize(28);
chatMenu.setGravity(Gravity.CENTER);
chatMenu.setPadding(8, 0, 8, 0);

chatMenu.setOnClickListener(v -> showChatMenu());

header.addView(chatMenu,
        new LinearLayout.LayoutParams(
                48,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        header.addView(
                headerOnlineDot
        );

        root.addView(header);

        messagesScroll =
                new ScrollView(this);

        messagesContainer =
                new LinearLayout(this);

        messagesContainer.setOrientation(
                LinearLayout.VERTICAL
        );

        messagesContainer.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        messagesScroll.addView(
                messagesContainer
        );

        root.addView(
                messagesScroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        LinearLayout bottom =
                new LinearLayout(this);

        bottom.setGravity(
                Gravity.CENTER_VERTICAL
        );

        bottom.setPadding(
                dp(6),
                dp(5),
                dp(6),
                dp(5)
        );

        bottom.setBackgroundColor(
                Color.WHITE
        );

        mediaButton =
                new ImageButton(this);

        mediaButton.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        mediaButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        mediaButton.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        mediaButton.setOnClickListener(
                v -> pickMedia()
        );

        bottom.addView(
                mediaButton,
                new LinearLayout.LayoutParams(
                        dp(56),
                        dp(56)
                )
        );

        messageInput =
                new EditText(this);

        messageInput.setHint(
                "پیام خود را بنویسید..."
        );

        messageInput.setTextSize(15);
        messageInput.setSingleLine(false);

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        -2,
                        1
                )
        );

        voiceButton =
                new ImageButton(this);

        voiceButton.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        voiceButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        voiceButton.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(5)
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );

        bottom.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        dp(58),
                        dp(58)
                )
        );

        sendButton =
                new ImageButton(this);

        sendButton.setImageResource(
                android.R.drawable.ic_menu_send
        );

        sendButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        sendButton.setOnClickListener(
                v -> sendText()
        );

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(52),
                        dp(52)
                )
        );

        root.addView(bottom);

        messageInput.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        updateTyping(
                                !s.toString()
                                        .trim()
                                        .isEmpty()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        setContentView(root);
    }

    private void listenMessages() {

        removeMessageListeners();

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
                                (snapshot, error) ->
                                        handleMessages(
                                                snapshot
                                        )
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
                                (snapshot, error) ->
                                        handleMessages(
                                                snapshot
                                        )
                        );
    }

    private void handleMessages(
            QuerySnapshot snapshot
    ) {

        if (snapshot == null ||
                messagesContainer == null) {

            return;
        }

        for (
                DocumentChange change :
                snapshot.getDocumentChanges()
        ) {

            messageCache.put(
                    change.getDocument().getId(),
                    change.getDocument()
            );
        }

        renderMessages();
    }

    private void renderMessages() {

        if (messagesContainer == null) {
            return;
        }

        messagesContainer.removeAllViews();

        List<DocumentSnapshot> list =
                new ArrayList<>(
                        messageCache.values()
                );

        Collections.sort(
                list,
                (a, b) -> {

                    com.google.firebase.Timestamp ta =
                            a.getTimestamp(
                                    "timestamp"
                            );

                    com.google.firebase.Timestamp tb =
                            b.getTimestamp(
                                    "timestamp"
                            );

                    if (ta == null &&
                            tb == null) {

                        return 0;
                    }

                    if (ta == null) {
                        return -1;
                    }

                    if (tb == null) {
                        return 1;
                    }

                    return ta.compareTo(tb);
                }
        );

        for (
                DocumentSnapshot d :
                list
        ) {

            renderOneMessage(d);
        }

        if (messagesScroll != null) {

            messagesScroll.post(
                    () ->
                            messagesScroll.fullScroll(
                                    View.FOCUS_DOWN
                            )
            );
        }
    }

    private boolean isDeletedForMe(
            DocumentSnapshot d
    ) {

        List<String> deletedFor =
                (List<String>)
                        d.get("deletedFor");

        return deletedFor != null &&
                deletedFor.contains(myId);
    }

    private void renderOneMessage(
            DocumentSnapshot d
    ) {

        if (isDeletedForMe(d)) {
            return;
        }

        Boolean deleted =
                d.getBoolean(
                        "deletedForAll"
                );

        String sender =
                d.getString("senderId");

        if (deleted != null &&
                deleted) {

            addSimpleMessage(
                    "پیام حذف شد",
                    sender,
                    d.getId()
            );

            return;
        }

        String type =
                d.getString("type");

        if (type == null) {
            type = "text";
        }

        if ("text".equals(type)) {

            String message =
                    d.getString("message");

            if (message == null) {

                message =
                        d.getString("text");
            }

            if (message == null) {
                message = "";
            }

            addTextMessage(
                    message,
                    sender,
                    d.getId()
            );

        } else if ("image".equals(type)) {

            String url =
                    d.getString("mediaUrl");

            if (url != null) {

                addMediaMessage(
                        url,
                        false,
                        sender,
                        d.getId()
                );
            }

        } else if ("video".equals(type)) {

            String url =
                    d.getString("mediaUrl");

            if (url != null) {

                addMediaMessage(
                        url,
                        true,
                        sender,
                        d.getId()
                );
            }

        } else if ("audio".equals(type)) {

            String url =
                    d.getString("audioUrl");

            if (url != null) {

                addAudioMessage(
                        url,
                        sender,
                        d.getId()
                );
            }
        }
    }

    private void addSimpleMessage(
            String message,
            String sender,
            String messageId
    ) {

        addTextMessage(
                message,
                sender,
                messageId
        );
    }

    private void addTextMessage(
            String message,
            String sender,
            String messageId
    ) {

        TextView bubble =
                text(
                        message,
                        16
                );

        boolean mine =
                myId.equals(sender);

        bubble.setTextColor(
                mine
                        ? Color.WHITE
                        : Color.DKGRAY
        );

        bubble.setBackground(
                bg(
                        mine
                                ? themeColor
                                : Color.WHITE,
                        16
                )
        );

        bubble.setPadding(
                dp(13),
                dp(9),
                dp(13),
                dp(9)
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.setMargins(
                dp(8),
                dp(4),
                dp(8),
                dp(4)
        );

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        messagesContainer.addView(
                bubble,
                p
        );

        if (messageId != null) {

            bubble.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void addMediaMessage(
            String url,
            boolean video,
            String sender,
            String messageId
    ) {

        ImageView image =
                new ImageView(this);

        image.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dp(300),
                        dp(300)
                )
        );

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setBackground(
                bg(Color.WHITE, 14)
        );

        image.setPadding(
                dp(2),
                dp(2),
                dp(2),
                dp(2)
        );

        loadImage(
                url,
                image
        );

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        dp(300),
                        dp(300)
                );

        boolean mine =
                myId.equals(sender);

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        p.setMargins(
                dp(6),
                dp(5),
                dp(6),
                dp(5)
        );

        messagesContainer.addView(
                image,
                p
        );

        if (!video) {

            image.setOnClickListener(
                    v ->
                            showImageViewer(url)
            );
        }

        if (messageId != null) {

            image.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void addAudioMessage(
            String url,
            String sender,
            String messageId
    ) {

        Button play =
                new Button(this);

        play.setText(
                "▶️ پخش پیام صوتی"
        );

        play.setTextSize(14);

        play.setOnClickListener(
                v -> playAudio(url)
        );

        boolean mine =
                myId.equals(sender);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        p.gravity =
                mine
                        ? Gravity.END
                        : Gravity.START;

        p.setMargins(
                dp(6),
                dp(4),
                dp(6),
                dp(4)
        );

        messagesContainer.addView(
                play,
                p
        );

        if (messageId != null) {

            play.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId
                        );

                        return true;
                    }
            );
        }
    }

    private void showDeleteMenu(
            String messageId
    ) {

        String[] items = {
                "حذف برای من",
                "حذف برای همه"
        };

        new AlertDialog.Builder(this)
                .setItems(
                        items,
                        (d, which) -> {

                            if (which == 0) {

                                deleteForMe(
                                        messageId
                                );

                            } else {

                                deleteForAll(
                                        messageId
                                );
                            }
                        }
                )
                .show();
    }

    private void deleteForMe(
            String messageId
    ) {

        db.collection("messages")
                .document(messageId)
                .update(
                        "deletedFor",
                        FieldValue.arrayUnion(
                                myId
                        )
                )
                .addOnSuccessListener(
                        x -> {

                            messageCache.remove(
                                    messageId
                            );

                            renderMessages();

                        }
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "حذف پیام ناموفق بود",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void deleteForAll(
            String messageId
    ) {

        db.collection("messages")
                .document(messageId)
                .update(
                        "deletedForAll",
                        true
                )
                .addOnSuccessListener(
                        x -> renderMessages()
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "حذف پیام ناموفق بود",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void sendText() {

        if (blocked) {

            Toast.makeText(
                    this,
                    "این کاربر بلاک شده است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (messageInput == null) {
            return;
        }

        String message =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                "text"
        );

        data.put(
                "message",
                message
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        x ->
                                messageInput
                                        .setText("")
                )
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "خطا در ارسال پیام",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void updateTyping(
            boolean value
    ) {

        if (receiverId == null) {
            return;
        }

        typing = value;

        db.collection("users")
                .document(myId)
                .update(
                        "typingTo",
                        value
                                ? receiverId
                                : ""
                );

        typingHandler
                .removeCallbacksAndMessages(
                        null
                );

        if (value) {

            typingHandler.postDelayed(
                    () -> {

                        db.collection("users")
                                .document(myId)
                                .update(
                                        "typingTo",
                                        ""
                                );

                        typing = false;
                    },
                    2500
            );
        }
    }

    private void listenReceiver() {

        if (receiverListener != null) {
            receiverListener.remove();
        }

        receiverListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (d, error) -> {

                                    if (d == null ||
                                            !d.exists()) {
                                        return;
                                    }

                                    Boolean online =
                                            d.getBoolean(
                                                    "online"
                                            );

                                    if (online != null &&
                                            online) {

                                        statusText.setText(
                                                "آنلاین"
                                        );

                                        headerOnlineDot
                                                .setTextColor(
                                                        Color.rgb(
                                                                0,
                                                                180,
                                                                70
                                                        )
                                                );

                                    } else {

                                        statusText.setText(
                                                "آفلاین"
                                        );

                                        headerOnlineDot
                                                .setTextColor(
                                                        Color.GRAY
                                                );
                                    }
                                }
                        );
    }

    private void listenTyping() {

        if (typingListener != null) {
            typingListener.remove();
        }

        typingListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (d, error) -> {

                                    if (d == null ||
                                            !d.exists()) {
                                        return;
                                    }

                                    String typingTo =
                                            d.getString(
                                                    "typingTo"
                                            );

                                    if (receiverId
                                            .equals(myId)) {
                                        return;
                                    }

                                    if (myId.equals(
                                            typingTo
                                    )) {

                                        statusText.setText(
                                                "در حال نوشتن..."
                                        );
                                    }
                                }
                        );
    }

    private void listenBlock() {

        if (blockListener != null) {
            blockListener.remove();
        }

        blockListener =
                db.collection("blocks")
                        .whereEqualTo(
                                "ownerId",
                                myId
                        )
                        .whereEqualTo(
                                "blockedUserId",
                                receiverId
                        )
                        .addSnapshotListener(
                                (snapshot, error) -> {

                                    blocked =
                                            snapshot != null &&
                                            !snapshot.isEmpty();
                                }
                        );
    }

    private void pickMedia() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "image/*",
                        "video/*"
                }
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_MEDIA
        );
    }

    private void pickProfilePhoto() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_PROFILE
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

        if (resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {

            return;
        }

        Uri uri =
                data.getData();

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );

        } catch (Exception ignored) {
        }

        if (requestCode == PICK_MEDIA) {

            uploadMedia(uri);

        } else if (requestCode == PICK_PROFILE) {

            uploadProfilePhoto(uri);

        } else if (requestCode == REQUEST_SAVE) {

            if (pendingSaveUrl != null) {

                saveImageToGallery(
                        pendingSaveUrl,
                        pendingSaveName
                );
            }
        }
    }

    private void uploadMedia(
            Uri uri
    ) {

        if (blocked) {

            Toast.makeText(
                    this,
                    "این کاربر بلاک شده است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String mime =
                getContentResolver()
                        .getType(uri);

        if (mime == null) {

            mime =
                    "application/octet-stream";
        }

        boolean image =
                mime.startsWith("image/");

        boolean video =
                mime.startsWith("video/");

        if (!image && !video) {

            Toast.makeText(
                    this,
                    "این نوع فایل پشتیبانی نمی‌شود",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String ext;

        if (image) {

            if (mime.contains("png")) {

                ext = "png";

            } else if (mime.contains("webp")) {

                ext = "webp";

            } else {

                ext = "jpg";
            }

        } else {

            if (mime.contains("3gp")) {

                ext = "3gp";

            } else {

                ext = "mp4";
            }
        }

        String objectPath =
                "chat/" +
                        System.currentTimeMillis() +
                        "_" +
                        myId +
                        "." +
                        ext;

        String finalMime =
                mime;

        Toast.makeText(
                this,
                "در حال ارسال فایل...",
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                finalMime,
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        saveMediaMessage(
                                image
                                        ? "image"
                                        : "video",
                                url
                        );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای ارسال:\n" +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveMediaMessage(
            String type,
            String url
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                type
        );

        data.put(
                "mediaUrl",
                url
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "خطا در ذخیره پیام فایل",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void toggleRecording() {

        if (recording) {

            stopRecording();

        } else {

            startRecording();
        }
    }

    /*
     * فقط بخش ضبط صدا با VoiceActivity هماهنگ شده:
     * 3GP + AMR_NB
     */
    private void startRecording() {

        if (blocked) {

            Toast.makeText(
                    this,
                    "این کاربر بلاک شده است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    REQUEST_MIC
            );

            return;
        }

        try {

            File file =
                    new File(
                            getCacheDir(),
                            "voice_" +
                                    System.currentTimeMillis() +
                                    ".3gp"
                    );

            audioPath =
                    file.getAbsolutePath();

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

            voiceButton.setImageResource(
                    android.R.drawable.ic_media_pause
            );

            Toast.makeText(
                    this,
                    "🎤 در حال ضبط... دوباره بزنید تا ارسال شود",
                    Toast.LENGTH_SHORT
            ).show();

        } catch (Exception e) {

            recording = false;

            Toast.makeText(
                    this,
                    "خطا در شروع ضبط: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();

            releaseRecorder();
        }
    }

    private void stopRecording() {

        if (recorder == null) {
            return;
        }

        try {

            recorder.stop();

        } catch (Exception e) {

            recording = false;

            releaseRecorder();

            Toast.makeText(
                    this,
                    "ضبط صدا ناموفق بود",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        recording = false;

        releaseRecorder();

        voiceButton.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        uploadVoice(audioPath);
    }

    private void releaseRecorder() {

        try {

            if (recorder != null) {

                recorder.reset();
                recorder.release();
            }

        } catch (Exception ignored) {
        }

        recorder = null;
    }

    /*
     * ارسال صدا نیز دقیقاً با VoiceActivity هماهنگ شده:
     * فایل 3GP
     * MIME = audio/3gpp
     * bucket = voice_messages
     * فایل در ریشه bucket قرار می‌گیرد.
     */
    private void uploadVoice(
            String path
    ) {

        if (path == null) {

            Toast.makeText(
                    this,
                    "فایل صوتی پیدا نشد",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File file =
                new File(path);

        if (!file.exists() ||
                file.length() <= 0) {

            Toast.makeText(
                    this,
                    "فایل صوتی خالی است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String fileName =
                System.currentTimeMillis() +
                        "_" +
                        myId +
                        ".3gp";

        /*
         * مثل VoiceActivity:
         * فایل مستقیم داخل bucket قرار می‌گیرد.
         */
        String objectPath =
                fileName;

        Toast.makeText(
                this,
                "در حال ارسال پیام صوتی...",
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                Uri.fromFile(file),
                VOICE_BUCKET,
                objectPath,
                "audio/3gpp",
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        saveAudioMessage(url);

                        try {
                            file.delete();
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای ارسال پیام صوتی:\n" +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void saveAudioMessage(
            String url
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "chatId",
                currentChatId
        );

        data.put(
                "senderId",
                myId
        );

        data.put(
                "receiverId",
                receiverId
        );

        data.put(
                "type",
                "audio"
        );

        data.put(
                "audioUrl",
                url
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        data.put(
                "read",
                false
        );

        data.put(
                "deletedForAll",
                false
        );

        data.put(
                "deletedFor",
                new ArrayList<>()
        );

        db.collection("messages")
                .add(data)
                .addOnFailureListener(
                        e ->
                                Toast.makeText(
                                        this,
                                        "خطا در ذخیره پیام صوتی",
                                        Toast.LENGTH_SHORT
                                ).show()
                );
    }

    private void playAudio(
            String url
    ) {

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
                    mp -> {

                        mp.release();
                        player = null;
                    }
            );

            player.setOnErrorListener(
                    (mp, what, extra) -> {

                        Toast.makeText(
                                this,
                                "پخش صدا ناموفق بود",
                                Toast.LENGTH_SHORT
                        ).show();

                        return true;
                    }
            );

            player.prepareAsync();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در پخش صدا",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void uploadProfilePhoto(
            Uri uri
    ) {

        String mime =
                getContentResolver()
                        .getType(uri);

        if (mime == null ||
                !mime.startsWith("image/")) {

            mime = "image/jpeg";
        }

        String ext;

        if (mime.contains("png")) {

            ext = "png";

        } else if (mime.contains("webp")) {

            ext = "webp";

        } else {

            ext = "jpg";
        }

        String objectPath =
                "profiles/profile_" +
                        myId +
                        "_" +
                        System.currentTimeMillis() +
                        "." +
                        ext;

        String finalMime =
                mime;

        Toast.makeText(
                this,
                "در حال ارسال عکس پروفایل...",
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                finalMime,
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String url
                    ) {

                        Map<String, Object> data =
                                new HashMap<>();

                        data.put(
                                "photoUrl",
                                url
                        );

                        db.collection("users")
                                .document(myId)
                                .set(
                                        data,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(
                                        x -> {

                                            Toast.makeText(
                                                    ChatActivity.this,
                                                    "عکس پروفایل ذخیره شد",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            loadUsers();
                                        }
                                );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای عکس پروفایل:\n" +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void uploadToSupabase(
            Uri uri,
            String bucket,
            String objectPath,
            String contentType,
            SupabaseUploadCallback callback
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    InputStream input =
                            null;

                    OutputStream output =
                            null;

                    try {

                        String endpoint =
                                SUPABASE_URL +
                                        "/storage/v1/object/" +
                                        bucket +
                                        "/" +
                                        encodePath(
                                                objectPath
                                        );

                        URL url =
                                new URL(endpoint);

                        connection =
                                (HttpURLConnection)
                                        url.openConnection();

                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setDoOutput(true);
                        connection.setDoInput(true);

                        connection.setConnectTimeout(
                                30000
                        );

                        connection.setReadTimeout(
                                60000
                        );

                        /*
                         * فقط apikey.
                         * Authorization Bearer برای
                         * publishable key استفاده نمی‌شود.
                         */
                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Accept",
                                "application/json"
                        );

                        connection.setRequestProperty(
                                "Content-Type",
                                contentType
                        );

                        long size = -1;

                        if ("file".equals(
                                uri.getScheme()
                        )) {

                            File file =
                                    new File(
                                            uri.getPath()
                                    );

                            if (!file.exists()) {

                                throw new Exception(
                                        "فایل وجود ندارد"
                                );
                            }

                            if (file.length() <= 0) {

                                throw new Exception(
                                        "فایل خالی است"
                                );
                            }

                            size =
                                    file.length();

                            input =
                                    new FileInputStream(
                                            file
                                    );

                        } else {

                            input =
                                    getContentResolver()
                                            .openInputStream(
                                                    uri
                                            );

                            if (input == null) {

                                throw new Exception(
                                        "فایل قابل خواندن نیست"
                                );
                            }

                            size =
                                    getUriSize(uri);
                        }

                        if (size > 0) {

                            connection
                                    .setFixedLengthStreamingMode(
                                            size
                                    );
                        }

                        output =
                                connection
                                        .getOutputStream();

                        byte[] buffer =
                                new byte[8192];

                        int count;

                        while (
                                (count =
                                        input.read(
                                                buffer
                                        )) != -1
                        ) {

                            output.write(
                                    buffer,
                                    0,
                                    count
                            );
                        }

                        output.flush();
                        output.close();
                        output = null;

                        int code =
                                connection
                                        .getResponseCode();

                        if (code >= 200 &&
                                code < 300) {

                            String publicUrl =
                                    getSupabasePublicUrl(
                                            bucket,
                                            objectPath
                                    );

                            runOnUiThread(
                                    () ->
                                            callback.onSuccess(
                                                    publicUrl
                                            )
                            );

                        } else {

                            String body =
                                    readErrorResponse(
                                            connection
                                    );

                            String message =
                                    "HTTP " +
                                            code +
                                            " " +
                                            connection
                                                    .getResponseMessage();

                            if (body != null &&
                                    !body.trim()
                                            .isEmpty()) {

                                message +=
                                        "\n" +
                                                body;
                            }

                            String finalMessage =
                                    message;

                            runOnUiThread(
                                    () ->
                                            callback.onError(
                                                    finalMessage
                                            )
                            );
                        }

                    } catch (Exception e) {

                        String message =
                                e.getMessage();

                        if (message == null ||
                                message.trim()
                                        .isEmpty()) {

                            message =
                                    e.getClass()
                                            .getSimpleName();
                        }

                        String finalMessage =
                                message;

                        runOnUiThread(
                                () ->
                                        callback.onError(
                                                finalMessage
                                        )
                        );

                    } finally {

                        try {

                            if (output != null) {
                                output.close();
                            }

                        } catch (Exception ignored) {
                        }

                        try {

                            if (input != null) {
                                input.close();
                            }

                        } catch (Exception ignored) {
                        }

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }

    private long getUriSize(
            Uri uri
    ) {

        try {

            android.database.Cursor cursor =
                    getContentResolver()
                            .query(
                                    uri,
                                    new String[]{
                                            OpenableColumns.SIZE
                                    },
                                    null,
                                    null,
                                    null
                            );

            if (cursor != null) {

                try {

                    if (cursor.moveToFirst()) {

                        int index =
                                cursor.getColumnIndex(
                                        OpenableColumns.SIZE
                                );

                        if (index >= 0 &&
                                !cursor.isNull(index)) {

                            return cursor.getLong(
                                    index
                            );
                        }
                    }

                } finally {

                    cursor.close();
                }
            }

        } catch (Exception ignored) {
        }

        return -1;
    }

    private String readErrorResponse(
            HttpURLConnection connection
    ) {

        InputStream stream =
                null;

        try {

            stream =
                    connection.getErrorStream();

            if (stream == null) {

                stream =
                        connection.getInputStream();
            }

            if (stream == null) {
                return "";
            }

            java.util.Scanner scanner =
                    new java.util.Scanner(
                            stream,
                            "UTF-8"
                    ).useDelimiter("\\A");

            return scanner.hasNext()
                    ? scanner.next()
                    : "";

        } catch (Exception e) {

            return "";

        } finally {

            try {

                if (stream != null) {
                    stream.close();
                }

            } catch (Exception ignored) {
            }
        }
    }

    private String encodePath(
            String path
    ) throws Exception {

        String[] parts =
                path.split("/");

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 0;
                i < parts.length;
                i++
        ) {

            if (i > 0) {
                result.append("/");
            }

            result.append(
                    URLEncoder.encode(
                            parts[i],
                            "UTF-8"
                    ).replace(
                            "+",
                            "%20"
                    )
            );
        }

        return result.toString();
    }

    private String getSupabasePublicUrl(
            String bucket,
            String path
    ) {

        try {

            return SUPABASE_URL +
                    "/storage/v1/object/public/" +
                    bucket +
                    "/" +
                    encodePath(path);

        } catch (Exception e) {

            return SUPABASE_URL +
                    "/storage/v1/object/public/" +
                    bucket +
                    "/" +
                    path;
        }
    }

    private void loadImage(
            String url,
            ImageView imageView
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    try {

                        connection =
                                (HttpURLConnection)
                                        new URL(url)
                                                .openConnection();

                        connection.setConnectTimeout(
                                15000
                        );

                        connection.setReadTimeout(
                                30000
                        );

                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Authorization",
                                "Bearer " +
                                        SUPABASE_PUBLISHABLE_KEY
                        );

                        InputStream input =
                                connection
                                        .getInputStream();

                        Bitmap bitmap =
                                BitmapFactory
                                        .decodeStream(
                                                input
                                        );

                        input.close();

                        if (bitmap != null) {

                            runOnUiThread(
                                    () ->
                                            imageView
                                                    .setImageBitmap(
                                                            bitmap
                                                    )
                            );
                        }

                    } catch (Exception ignored) {

                    } finally {

                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                }
        ).start();
    }

    private void showImageViewer(
            String url
    ) {

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        box.setGravity(
                Gravity.CENTER
        );

        box.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        ImageView image =
                new ImageView(this);

        image.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        box.addView(
                image,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(380)
                )
        );

        Button save =
                new Button(this);

        save.setText(
                "💾 ذخیره عکس در گالری"
        );

        box.addView(
                save,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        loadImage(
                url,
                image
        );

        save.setOnClickListener(
                v -> {

                    pendingSaveUrl =
                            url;

                    pendingSaveName =
                            "tajrobehha_" +
                                    System.currentTimeMillis() +
                                    ".jpg";

                    saveImageToGallery(
                            pendingSaveUrl,
                            pendingSaveName
                    );
                }
        );

        new AlertDialog.Builder(this)
                .setTitle("تصویر")
                .setView(box)
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    private void saveImageToGallery(
            String url,
            String fileName
    ) {

        new Thread(
                () -> {

                    try {

                        HttpURLConnection connection =
                                (HttpURLConnection)
                                        new URL(url)
                                                .openConnection();

                        connection.setConnectTimeout(
                                20000
                        );

                        connection.setReadTimeout(
                                30000
                        );

                        connection.setRequestProperty(
                                "apikey",
                                SUPABASE_PUBLISHABLE_KEY
                        );

                        connection.setRequestProperty(
                                "Authorization",
                                "Bearer " +
                                        SUPABASE_PUBLISHABLE_KEY
                        );

                        InputStream input =
                                connection
                                        .getInputStream();

                        if (Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.Q) {

                            ContentValues values =
                                    new ContentValues();

                            values.put(
                                    MediaStore.Images.Media
                                            .DISPLAY_NAME,
                                    fileName
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .MIME_TYPE,
                                    "image/jpeg"
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .RELATIVE_PATH,
                                    Environment
                                            .DIRECTORY_PICTURES +
                                            "/تجربه‌ها"
                            );

                            values.put(
                                    MediaStore.Images.Media
                                            .IS_PENDING,
                                    1
                            );

                            Uri imageUri =
                                    getContentResolver()
                                            .insert(
                                                    MediaStore.Images.Media
                                                            .getContentUri(
                                                                    MediaStore
                                                                            .VOLUME_EXTERNAL_PRIMARY
                                                            ),
                                                    values
                                            );

                            if (imageUri == null) {

                                throw new Exception(
                                        "گالری قابل دسترسی نیست"
                                );
                            }

                            OutputStream output =
                                    getContentResolver()
                                            .openOutputStream(
                                                    imageUri
                                            );

                            if (output == null) {

                                throw new Exception(
                                        "فضای ذخیره‌سازی باز نشد"
                                );
                            }

                            byte[] buffer =
                                    new byte[8192];

                            int count;

                            while (
                                    (count =
                                            input.read(
                                                    buffer
                                            )) != -1
                            ) {

                                output.write(
                                        buffer,
                                        0,
                                        count
                                );
                            }

                            output.flush();
                            output.close();
                            input.close();

                            ContentValues done =
                                    new ContentValues();

                            done.put(
                                    MediaStore.Images.Media
                                            .IS_PENDING,
                                    0
                            );

                            getContentResolver()
                                    .update(
                                            imageUri,
                                            done,
                                            null,
                                            null
                                    );

                            runOnUiThread(
                                    () ->
                                            Toast.makeText(
                                                    this,
                                                    "✅ عکس در گالری ذخیره شد",
                                                    Toast.LENGTH_SHORT
                                            ).show()
                            );

                        } else {

                            input.close();

                            runOnUiThread(
                                    () ->
                                            Toast.makeText(
                                                    this,
                                                    "ذخیره مستقیم در گالری در این نسخه اندروید پشتیبانی نمی‌شود",
                                                    Toast.LENGTH_LONG
                                            ).show()
                            );
                        }

                        connection.disconnect();

                    } catch (Exception e) {

                        String error =
                                e.getMessage();

                        if (error == null) {

                            error =
                                    "خطا در ذخیره عکس";
                        }

                        String finalError =
                                error;

                        runOnUiThread(
                                () ->
                                        Toast.makeText(
                                                this,
                                                "خطای ذخیره عکس:\n" +
                                                        finalError,
                                                Toast.LENGTH_LONG
                                        ).show()
                        );
                    }

                }
        ).start();
    }

    private void removeMessageListeners() {

        if (sentMessageListener != null) {

            sentMessageListener.remove();
            sentMessageListener = null;
        }

        if (receivedMessageListener != null) {

            receivedMessageListener.remove();
            receivedMessageListener = null;
        }
    }

    private void removeChatListeners() {

        removeMessageListeners();

        if (receiverListener != null) {

            receiverListener.remove();
            receiverListener = null;
        }

        if (typingListener != null) {

            typingListener.remove();
            typingListener = null;
        }

        if (blockListener != null) {

            blockListener.remove();
            blockListener = null;
        }
    }

    private void showUsers() {

        removeChatListeners();

        insideChat = false;
        receiverId = null;
        receiverName = null;
        currentChatId = null;

        messageCache.clear();

        createUsersScreen();
        loadUsers();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (myId != null) {

            db.collection("users")
                    .document(myId)
                    .set(
                            new HashMap<String, Object>() {{
                                put(
                                        "online",
                                        true
                                );

                                put(
                                        "lastSeen",
                                        FieldValue
                                                .serverTimestamp()
                                );
                            }},
                            SetOptions.merge()
                    );
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (myId != null) {

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "online",
                    false
            );

            data.put(
                    "lastSeen",
                    FieldValue
                            .serverTimestamp()
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
    }

    @Override
    protected void onDestroy() {

        removeChatListeners();

        try {

            if (recorder != null) {
                recorder.release();
            }

        } catch (Exception ignored) {
        }

        recorder = null;

        try {

            if (player != null) {
                player.release();
            }

        } catch (Exception ignored) {
        }

        player = null;

        super.onDestroy();
    }

     @Override
public void onBackPressed() {

    if (insideChat) {

        showUsers();

    } else {

        super.onBackPressed();
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

    if (requestCode == REQUEST_MIC) {

        if (grantResults.length > 0 &&
                grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {

            startRecording();

        } else {

            Toast.makeText(
                    this,
                    "اجازه میکروفون داده نشد",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}

private void showChatMenu() {

    String[] options = {
            "🔕 بی‌صدا کردن اعلان‌های این چت",
            "👤 مشاهده پروفایل",
            "🔒 تنظیمات حریم خصوصی",
            "🗑️ حذف کامل چت",
            "🚫 مسدود کردن"
    };

    new AlertDialog.Builder(this)
            .setTitle("تنظیمات چت")
            .setItems(
                    options,
                    (dialog, which) -> {

                        if (which == 0) {

                            Toast.makeText(
                                    ChatActivity.this,
                                    "اعلان‌های این چت بی‌صدا شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else if (which == 1) {

                            showProfileDialog(
                                    receiverId,
                                    receiverName,
                                    receiverPhotoUrl
                            );

                        } else if (which == 2) {

                            showChatPrivacySettings();

                        } else if (which == 3) {

                            new AlertDialog.Builder(
                                    ChatActivity.this
                            )
                                    .setTitle("⚠️ حذف کامل چت")
                                    .setMessage(
                                            "آیا مطمئن هستید که می‌خواهید تمام این گفتگو را حذف کنید؟"
                                    )
                                    .setNegativeButton(
                                            "لغو",
                                            null
                                    )
                                    .setPositiveButton(
                                            "مرحله اول",
                                            (d, w) -> {

                                                new AlertDialog.Builder(
                                                        ChatActivity.this
                                                )
                                                        .setTitle(
                                                                "تأیید نهایی حذف"
                                                        )
                                                        .setMessage(
                                                                "این کار تمام پیام‌های این گفتگو را حذف می‌کند و قابل برگشت نیست. ادامه می‌دهید؟"
                                                        )
                                                        .setNegativeButton(
                                                                "لغو",
                                                                null
                                                        )
                                                        .setPositiveButton(
                                                                "حذف کامل",
                                                                (d2, w2) ->
                                                                        deleteCurrentChatForMe()
                                                        )
                                                        .show();
                                            }
                                    )
                                    .show();

                        } else if (which == 4) {

                            new AlertDialog.Builder(
                                    ChatActivity.this
                            )
                                    .setTitle("مسدود کردن کاربر")
                                    .setMessage(
                                            "آیا می‌خواهید این کاربر را مسدود کنید؟"
                                    )
                                    .setNegativeButton(
                                            "لغو",
                                            null
                                    )
                                    .setPositiveButton(
                                            "مسدود کردن",
                                            (d, w) ->
                                                    blockCurrentUser()
                                    )
                                    .show();
                        }
                    }
            )
            .show();
}

private void showChatPrivacySettings() {

    final String[] items = {
            "نمایش آنلاین بودن",
            "نمایش آخرین بازدید",
            "نمایش «در حال نوشتن…»",
            "نمایش رسید خوانده شدن ✓✓"
    };

    final boolean[] checked = {
            true,
            true,
            true,
            true
    };

    new AlertDialog.Builder(this)
            .setTitle("🔒 تنظیمات حریم خصوصی")
            .setMultiChoiceItems(
                    items,
                    checked,
                    (dialog, which, isChecked) ->
                            checked[which] = isChecked
            )
            .setNegativeButton(
                    "لغو",
                    null
            )
            .setPositiveButton(
                    "ذخیره",
                    (dialog, which) -> {

                        Map<String, Object> privacy =
                                new HashMap<>();

                        privacy.put(
                                "showOnline",
                                checked[0]
                        );

                        privacy.put(
                                "showLastSeen",
                                checked[1]
                        );

                        privacy.put(
                                "showTyping",
                                checked[2]
                        );

                        privacy.put(
                                "readReceipts",
                                checked[3]
                        );

                        db.collection("users")
                                .document(myId)
                                .set(
                                        privacy,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(
                                        v ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        "تنظیمات ذخیره شد",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                )
                                .addOnFailureListener(
                                        e ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        "ذخیره تنظیمات ناموفق بود",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                );
                    }
            )
            .show();
}

private void reportCurrentUser() {

    Map<String, Object> report =
            new HashMap<>();

    report.put(
            "reporterId",
            myId
    );

    report.put(
            "reportedUserId",
            receiverId
    );

    report.put(
            "chatId",
            currentChatId
    );

    report.put(
            "timestamp",
            FieldValue.serverTimestamp()
    );

    db.collection("reports")
            .add(report)
            .addOnSuccessListener(
                    v ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    "گزارش شما ثبت شد",
                                    Toast.LENGTH_SHORT
                            ).show()
            )
            .addOnFailureListener(
                    e ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    "ثبت گزارش ناموفق بود",
                                    Toast.LENGTH_SHORT
                            ).show()
            );
}

private void deleteCurrentChatForMe() {

    if (currentChatId == null ||
            currentChatId.isEmpty()) {

        return;
    }

    db.collection("chats")
            .document(currentChatId)
            .collection("messages")
            .get()
            .addOnSuccessListener(
                    snapshot -> {

                        com.google.firebase.firestore.WriteBatch batch =
                                db.batch();

                        for (
                                DocumentSnapshot doc :
                                snapshot.getDocuments()
                        ) {

                            batch.update(
                                    doc.getReference(),
                                    "deletedFor",
                                    FieldValue.arrayUnion(
                                            myId
                                    )
                            );
                        }

                        batch.commit()
                                .addOnSuccessListener(
                                        v -> {

                                            messageCache.clear();

                                            if (messagesContainer != null) {

                                                messagesContainer
                                                        .removeAllViews();
                                            }

                                            Toast.makeText(
                                                    ChatActivity.this,
                                                    "چت از حساب شما پاک شد",
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        }
                                )
                                .addOnFailureListener(
                                        e ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        "پاک کردن چت ناموفق بود",
                                                        Toast.LENGTH_SHORT
                                                ).show()
                                );
                    }
            )
            .addOnFailureListener(
                    e ->
                            Toast.makeText(
                                    ChatActivity.this,
                                    "دسترسی به پیام‌های چت ناموفق بود",
                                    Toast.LENGTH_SHORT
                            ).show()
            );
}

private void blockCurrentUser() {

    if (receiverId == null ||
            receiverId.isEmpty()) {

        Toast.makeText(
                ChatActivity.this,
                "کاربر انتخاب نشده است",
                Toast.LENGTH_SHORT
        ).show();

        return;
    }

    String blockId =
            myId + "_" + receiverId;

    Map<String, Object> data =
            new HashMap<>();

    data.put(
            "ownerId",
            myId
    );

    data.put(
            "blockedUserId",
            receiverId
    );

    data.put(
            "blockedUserName",
            receiverName == null ||
                    receiverName.trim().isEmpty()
                    ? "کاربر"
                    : receiverName
    );

    data.put(
            "timestamp",
            FieldValue.serverTimestamp()
    );

    db.collection("blocks")
            .document(blockId)
            .set(data)
            .addOnSuccessListener(
                    v -> {

                        blocked = true;

                        Toast.makeText(
                                ChatActivity.this,
                                "کاربر مسدود شد",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
            )
            .addOnFailureListener(
                    e -> {

                        String error =
                                e.getMessage();

                        if (error == null ||
                                error.trim().isEmpty()) {

                            error =
                                    "خطای نامشخص";
                        }

                        Toast.makeText(
                                ChatActivity.this,
                                "مسدود کردن ناموفق بود:\n" +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
            );
}
    
