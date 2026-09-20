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

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    /*
     * اینجا همان Publishable Key واقعی فعلی خودت را نگه دار.
     * Secret Key را هرگز اینجا قرار نده.
     */
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

    private MediaRecorder recorder;
    private MediaPlayer player;
    private String audioPath;

    private final Handler typingHandler = new Handler();

    private final Map<String, DocumentSnapshot> messageCache =
            new HashMap<>();

    private final Set<String> hiddenUserIds =
            new HashSet<>();

    private int themeColor = Color.rgb(12, 91, 120);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "ابتدا وارد اکانت خود شوید",
                    Toast.LENGTH_LONG
            ).show();

            startActivity(
                    new Intent(
                            this,
                            AccountActivity.class
                    )
            );

            finish();
            return;
        }

        myId = user.getUid();

        ensureUserProfile();

        createUsersScreen();

        loadUsers();
    }

    private void ensureUserProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "userId",
                user.getUid()
        );

        if (user.getEmail() != null) {
            data.put(
                    "email",
                    user.getEmail()
            );
        }

        if (user.getPhoneNumber() != null) {
            data.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );
        }

        if (user.getDisplayName() != null &&
                !user.getDisplayName().trim().isEmpty()) {

            data.put(
                    "name",
                    user.getDisplayName()
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
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    private GradientDrawable roundedBackground(
            int color,
            float radius
    ) {

        GradientDrawable drawable =
                new GradientDrawable();

        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        return drawable;
    }

    private TextView makeText(
            String text,
            float size,
            int color
    ) {

        TextView tv =
                new TextView(this);

        tv.setText(text);
        tv.setTextSize(size);
        tv.setTextColor(color);
        tv.setGravity(
                Gravity.CENTER_VERTICAL
        );
        tv.setTypeface(
                Typeface.DEFAULT
        );

        return tv;
    }

    private Button makeButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);

        button.setPadding(
                14,
                6,
                14,
                6
        );

        button.setBackground(
                roundedBackground(
                        themeColor,
                        18
                )
        );

        return button;
    }

    private void createUsersScreen() {

        insideChat = false;

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

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                10,
                12,
                10,
                12
        );

        header.setBackgroundColor(
                themeColor
        );

        Button myProfile =
                makeButton("👤");

        myProfile.setTextSize(22);

        myProfile.setContentDescription(
                "پروفایل من"
        );

        myProfile.setOnClickListener(
                v -> showOwnProfile()
        );

        header.addView(
                myProfile,
                new LinearLayout.LayoutParams(
                        58,
                        55
                )
        );

        TextView title =
                makeText(
                        "💬 کاربران",
                        23,
                        Color.WHITE
                );

        title.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button refresh =
                makeButton("🔄");

        refresh.setTextSize(21);

        refresh.setOnClickListener(
                v -> loadUsers()
        );

        header.addView(
                refresh,
                new LinearLayout.LayoutParams(
                        62,
                        55
                )
        );

        root.addView(header);

        TextView info =
                makeText(
                        "برای گفتگو روی کاربر بزنید؛ برای دیدن پروفایل روی عکس یا نام بزنید.",
                        16,
                        Color.DKGRAY
                );

        info.setPadding(
                18,
                15,
                18,
                10
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
                12,
                5,
                12,
                25
        );

        scroll.addView(
                usersContainer
        );

        root.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        setContentView(root);
    }

    private void loadUsers() {

        if (myId == null ||
                usersContainer == null) {
            return;
        }

        usersContainer.removeAllViews();

        TextView loading =
                makeText(
                        "در حال دریافت کاربران...",
                        18,
                        Color.DKGRAY
                );

        loading.setGravity(
                Gravity.CENTER
        );

        loading.setPadding(
                10,
                30,
                10,
                30
        );

        usersContainer.addView(
                loading
        );

        db.collection("hiddenUsers")
                .whereEqualTo(
                        "ownerId",
                        myId
                )
                .get()
                .addOnSuccessListener(
                        hiddenSnapshot -> {

                            hiddenUserIds.clear();

                            for (DocumentSnapshot doc :
                                    hiddenSnapshot.getDocuments()) {

                                String hiddenId =
                                        doc.getString(
                                                "hiddenUserId"
                                        );

                                if (hiddenId != null) {
                                    hiddenUserIds.add(
                                            hiddenId
                                    );
                                }
                            }

                            loadVisibleUsers();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            hiddenUserIds.clear();

                            loadVisibleUsers();
                        }
                );
    }

    private void loadVisibleUsers() {

        db.collection("users")
                .get()
                .addOnSuccessListener(
                        snapshot -> {

                            usersContainer.removeAllViews();

                            Map<String, DocumentSnapshot> uniqueUsers =
                                    new HashMap<>();

                            for (DocumentSnapshot doc :
                                    snapshot.getDocuments()) {

                                String uid =
                                        doc.getId();

                                String storedUserId =
                                        doc.getString(
                                                "userId"
                                        );

                                if (storedUserId != null &&
                                        !storedUserId.trim().isEmpty()) {

                                    uid = storedUserId;
                                }

                                if (uid.equals(myId)) {
                                    continue;
                                }

                                if (hiddenUserIds.contains(uid)) {
                                    continue;
                                }

                                uniqueUsers.put(
                                        uid,
                                        doc
                                );
                            }

                            List<DocumentSnapshot> users =
                                    new ArrayList<>(
                                            uniqueUsers.values()
                                    );

                            Collections.sort(
                                    users,
                                    new Comparator<DocumentSnapshot>() {

                                        @Override
                                        public int compare(
                                                DocumentSnapshot a,
                                                DocumentSnapshot b
                                        ) {

                                            String nameA =
                                                    a.getString(
                                                            "name"
                                                    );

                                            String nameB =
                                                    b.getString(
                                                            "name"
                                                    );

                                            if (nameA == null) {
                                                nameA = "";
                                            }

                                            if (nameB == null) {
                                                nameB = "";
                                            }

                                            return nameA.compareToIgnoreCase(
                                                    nameB
                                            );
                                        }
                                    }
                            );

                            if (users.isEmpty()) {

                                TextView empty =
                                        makeText(
                                                "هنوز کاربر دیگری برای گفتگو وجود ندارد.",
                                                18,
                                                Color.DKGRAY
                                        );

                                empty.setGravity(
                                        Gravity.CENTER
                                );

                                empty.setPadding(
                                        15,
                                        40,
                                        15,
                                        40
                                );

                                usersContainer.addView(
                                        empty
                                );

                                return;
                            }

                            for (DocumentSnapshot doc :
                                    users) {

                                addUserItem(doc);
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            usersContainer.removeAllViews();

                            TextView error =
                                    makeText(
                                            "خطای دریافت کاربران\n" +
                                                    e.getMessage(),
                                            17,
                                            Color.RED
                                    );

                            error.setPadding(
                                    15,
                                    30,
                                    15,
                                    30
                            );

                            usersContainer.addView(
                                    error
                            );
                        }
                );
    }

    private void addUserItem(
            DocumentSnapshot doc
    ) {

        String uid =
                doc.getString("userId");

        if (uid == null ||
                uid.trim().isEmpty()) {

            uid = doc.getId();
        }

        if (uid.equals(myId)) {
            return;
        }

        String name =
                doc.getString("name");

        if (name == null ||
                name.trim().isEmpty()) {

            name = "کاربر";
        }

        String photoUrl =
                doc.getString("photoUrl");

        Boolean online =
                doc.getBoolean("online");

        boolean isOnline =
                Boolean.TRUE.equals(online);

        LinearLayout card =
                new LinearLayout(this);

        card.setOrientation(
                LinearLayout.HORIZONTAL
        );

        card.setGravity(
                Gravity.CENTER_VERTICAL
        );

        card.setPadding(
                14,
                12,
                14,
                12
        );

        card.setBackground(
                roundedBackground(
                        Color.WHITE,
                        20
                )
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(
                0,
                7,
                0,
                7
        );

        usersContainer.addView(
                card,
                cardParams
        );

        ImageView avatar =
                new ImageView(this);

        avatar.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        avatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        avatar.setBackground(
                roundedBackground(
                        Color.LTGRAY,
                        100
                )
        );

        avatar.setPadding(
                8,
                8,
                8,
                8
        );

        card.addView(
                avatar,
                new LinearLayout.LayoutParams(
                        68,
                        68
                )
        );

        LinearLayout nameBox =
                new LinearLayout(this);

        nameBox.setOrientation(
                LinearLayout.VERTICAL
        );

        nameBox.setPadding(
                15,
                0,
                8,
                0
        );

        TextView nameText =
                makeText(
                        name,
                        19,
                        Color.rgb(
                                25,
                                25,
                                25
                        )
                );

        nameText.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        nameBox.addView(
                nameText
        );

        LinearLayout statusRow =
                new LinearLayout(this);

        statusRow.setOrientation(
                LinearLayout.HORIZONTAL
        );

        statusRow.setGravity(
                Gravity.CENTER_VERTICAL
        );

        View dot =
                new View(this);

        GradientDrawable dotBg =
                new GradientDrawable();

        dotBg.setShape(
                GradientDrawable.OVAL
        );

        dotBg.setColor(
                isOnline
                        ? Color.rgb(
                                20,
                                190,
                                70
                        )
                        : Color.GRAY
        );

        dot.setBackground(
                dotBg
        );

        statusRow.addView(
                dot,
                new LinearLayout.LayoutParams(
                        13,
                        13
                )
        );

        TextView status =
                makeText(
                        isOnline
                                ? "  آنلاین"
                                : "  آفلاین",
                        15,
                        isOnline
                                ? Color.rgb(
                                        15,
                                        150,
                                        55
                                )
                                : Color.GRAY
                );

        statusRow.addView(status);

        nameBox.addView(
                statusRow
        );

        card.addView(
                nameBox,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button menu =
                makeButton("⋮");

        menu.setTextSize(25);
        menu.setTextColor(themeColor);

        menu.setBackground(
                roundedBackground(
                        Color.TRANSPARENT,
                        15
                )
        );

        final String finalName =
                name;

        final String finalPhotoUrl =
                photoUrl;

        final String finalUid =
                uid;

        menu.setOnClickListener(
                v -> showUserManagementMenu(
                        finalUid,
                        finalName
                )
        );

        card.addView(
                menu,
                new LinearLayout.LayoutParams(
                        55,
                        55
                )
        );

        /*
         * عکس یا نام = پروفایل
         */
        avatar.setOnClickListener(
                v -> showUserProfile(
                        finalUid
                )
        );

        nameText.setOnClickListener(
                v -> showUserProfile(
                        finalUid
                )
        );

        /*
         * لمس قسمت خالی کارت = چت
         */
        card.setOnClickListener(
                v -> openPrivateChat(
                        finalUid,
                        finalName,
                        finalPhotoUrl
                )
        );

        card.setOnLongClickListener(
                v -> {

                    showUserManagementMenu(
                            finalUid,
                            finalName
                    );

                    return true;
                }
        );

        if (photoUrl != null &&
                !photoUrl.trim().isEmpty()) {

            loadImage(
                    photoUrl,
                    avatar
            );
        }
    }

    private void showUserManagementMenu(
            String uid,
            String name
    ) {

        String[] options = {
                "👤 دیدن پروفایل",
                "💬 باز کردن چت",
                "🚫 بلاک کردن",
                "🗑️ حذف از فهرست من",
                "لغو"
        };

        new AlertDialog.Builder(this)
                .setTitle(name)
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                showUserProfile(uid);

                            } else if (which == 1) {

                                db.collection("users")
                                        .document(uid)
                                        .get()
                                        .addOnSuccessListener(
                                                doc -> {

                                                    String photo =
                                                            doc.getString(
                                                                    "photoUrl"
                                                            );

                                                    openPrivateChat(
                                                            uid,
                                                            name,
                                                            photo
                                                    );
                                                }
                                        );

                            } else if (which == 2) {

                                blockUserFromList(
                                        uid,
                                        name
                                );

                            } else if (which == 3) {

                                confirmDeleteUser(
                                        uid,
                                        name
                                );
                            }
                        }
                )
                .show();
    }

    private void showUserProfile(
            String uid
    ) {

        if (uid == null ||
                uid.trim().isEmpty()) {
            return;
        }

        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            if (!doc.exists()) {

                                Toast.makeText(
                                        this,
                                        "پروفایل کاربر پیدا نشد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            showProfileDialog(
                                    doc,
                                    false
                            );
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای دریافت پروفایل: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showOwnProfile() {

        if (myId == null) {
            return;
        }

        db.collection("users")
                .document(myId)
                .get()
                .addOnSuccessListener(
                        doc -> {

                            if (doc.exists()) {

                                showProfileDialog(
                                        doc,
                                        true
                                );

                            } else {

                                ensureUserProfile();

                                db.collection("users")
                                        .document(myId)
                                        .get()
                                        .addOnSuccessListener(
                                                newDoc ->
                                                        showProfileDialog(
                                                                newDoc,
                                                                true
                                                        )
                                        );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای دریافت پروفایل: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void showProfileDialog(
            DocumentSnapshot doc,
            boolean ownProfile
    ) {

        String name =
                doc.getString("name");

        if (name == null ||
                name.trim().isEmpty()) {

            name = "کاربر";
        }

        String email =
                doc.getString("email");

        String phone =
                doc.getString("phoneNumber");

        String photoUrl =
                doc.getString("photoUrl");

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        layout.setPadding(
                25,
                20,
                25,
                10
        );

        ImageView profileImage =
                new ImageView(this);

        profileImage.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        profileImage.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        profileImage.setBackground(
                roundedBackground(
                        Color.LTGRAY,
                        300
                )
        );

        profileImage.setPadding(
                5,
                5,
                5,
                5
        );

        layout.addView(
                profileImage,
                new LinearLayout.LayoutParams(
                        180,
                        180
                )
        );

        if (photoUrl != null &&
                !photoUrl.trim().isEmpty()) {

            loadImage(
                    photoUrl,
                    profileImage
            );
        }

        TextView nameText =
                makeText(
                        "👤 " + name,
                        21,
                        Color.rgb(
                                25,
                                25,
                                25
                        )
                );

        nameText.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        nameText.setGravity(
                Gravity.CENTER
        );

        nameText.setPadding(
                5,
                18,
                5,
                8
        );

        layout.addView(
                nameText
        );

        if (email != null &&
                !email.trim().isEmpty()) {

            TextView emailText =
                    makeText(
                            "📧 " + email,
                            17,
                            Color.DKGRAY
                    );

            emailText.setGravity(
                    Gravity.CENTER
            );

            emailText.setPadding(
                    5,
                    5,
                    5,
                    5
            );

            layout.addView(
                    emailText
            );
        }

        if (phone != null &&
                !phone.trim().isEmpty()) {

            TextView phoneText =
                    makeText(
                            "📱 " + phone,
                            17,
                            Color.DKGRAY
                    );

            phoneText.setGravity(
                    Gravity.CENTER
            );

            phoneText.setPadding(
                    5,
                    5,
                    5,
                    5
            );

            layout.addView(
                    phoneText
            );
        }

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(
                                ownProfile
                                        ? "👤 پروفایل من"
                                        : "👤 پروفایل کاربر"
                        )
                        .setView(layout)
                        .setNegativeButton(
                                "بستن",
                                null
                        )
                        .create();

        if (ownProfile) {

            dialog.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    "🖼️ تغییر عکس",
                    (d, which) -> pickProfilePhoto()
            );
        }

        dialog.show();
    }

    private void blockUserFromList(
            String uid,
            String name
    ) {

        String blockId =
                myId + "_" + uid;

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "blockerId",
                myId
        );

        data.put(
                "blockedId",
                uid
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

                            Toast.makeText(
                                    this,
                                    name + " بلاک شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            loadUsers();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای بلاک: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void confirmDeleteUser(
            String uid,
            String name
    ) {

        new AlertDialog.Builder(this)
                .setTitle("حذف کاربر")
                .setMessage(
                        "آیا می‌خواهید «" +
                                name +
                                "» از فهرست کاربران شما حذف شود؟\n\n" +
                                "این کار اکانت طرف مقابل و پیام‌ها را حذف نمی‌کند."
                )
                .setNegativeButton(
                        "لغو",
                        null
                )
                .setPositiveButton(
                        "حذف",
                        (dialog, which) ->
                                deleteUserFromMyList(
                                        uid,
                                        name
                                )
                )
                .show();
    }

    private void deleteUserFromMyList(
            String uid,
            String name
    ) {

        String hiddenId =
                myId + "_" + uid;

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
                "name",
                name
        );

        data.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("hiddenUsers")
                .document(hiddenId)
                .set(data)
                .addOnSuccessListener(
                        v -> {

                            hiddenUserIds.add(uid);

                            Toast.makeText(
                                    this,
                                    "کاربر از فهرست شما حذف شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            if (insideChat &&
                                    uid.equals(receiverId)) {

                                showUsers();

                            } else {

                                loadUsers();
                            }
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای حذف کاربر: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void openPrivateChat(
            String uid,
            String name,
            String photoUrl
    ) {

        if (uid == null ||
                uid.trim().isEmpty()) {
            return;
        }

        receiverId = uid;
        receiverName = name;

        currentChatId =
                makeChatId(
                        myId,
                        receiverId
                );

        messageCache.clear();

        insideChat = true;

        createChatScreen(photoUrl);

        listenMessages();
        listenReceiver();
        listenTyping();
        listenBlockStatus();
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
                Color.rgb(
                        235,
                        248,
                        250
                )
        );

        LinearLayout header =
                new LinearLayout(this);

        header.setOrientation(
                LinearLayout.HORIZONTAL
        );

        header.setGravity(
                Gravity.CENTER_VERTICAL
        );

        header.setPadding(
                8,
                8,
                8,
                8
        );

        header.setBackgroundColor(
                themeColor
        );

        Button back =
                makeButton("‹");

        back.setTextSize(33);

        back.setBackgroundColor(
                Color.TRANSPARENT
        );

        back.setOnClickListener(
                v -> showUsers()
        );

        header.addView(
                back,
                new LinearLayout.LayoutParams(
                        58,
                        58
                )
        );

        headerAvatar =
                new ImageView(this);

        headerAvatar.setImageResource(
                android.R.drawable.ic_menu_myplaces
        );

        headerAvatar.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        headerAvatar.setBackground(
                roundedBackground(
                        Color.WHITE,
                        100
                )
        );

        headerAvatar.setPadding(
                5,
                5,
                5,
                5
        );

        headerAvatar.setOnClickListener(
                v -> showUserProfile(receiverId)
        );

        header.addView(
                headerAvatar,
                new LinearLayout.LayoutParams(
                        55,
                        55
                )
        );

        headerOnlineDot =
                makeText(
                        "●",
                        22,
                        Color.GRAY
                );

        headerOnlineDot.setGravity(
                Gravity.CENTER
        );

        header.addView(
                headerOnlineDot,
                new LinearLayout.LayoutParams(
                        25,
                        55
                )
        );

        LinearLayout titleBox =
                new LinearLayout(this);

        titleBox.setOrientation(
                LinearLayout.VERTICAL
        );

        titleBox.setPadding(
                8,
                0,
                5,
                0
        );

        titleText =
                makeText(
                        receiverName,
                        20,
                        Color.WHITE
                );

        titleText.setTypeface(
                Typeface.DEFAULT_BOLD
        );

        titleText.setOnClickListener(
                v -> showUserProfile(receiverId)
        );

        titleBox.addView(
                titleText
        );

        statusText =
                makeText(
                        "در حال اتصال...",
                        15,
                        Color.WHITE
                );

        titleBox.addView(
                statusText
        );

        header.addView(
                titleBox,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button menu =
                makeButton("⋮");

        menu.setTextSize(28);

        menu.setBackgroundColor(
                Color.TRANSPARENT
        );

        menu.setOnClickListener(
                v -> showChatMenu()
        );

        header.addView(
                menu,
                new LinearLayout.LayoutParams(
                        58,
                        58
                )
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
                10,
                12,
                10,
                18
        );

        messagesScroll.addView(
                messagesContainer
        );

        root.addView(
                messagesScroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout inputBar =
                new LinearLayout(this);

        inputBar.setOrientation(
                LinearLayout.HORIZONTAL
        );

        inputBar.setGravity(
                Gravity.CENTER_VERTICAL
        );

        inputBar.setPadding(
                7,
                8,
                7,
                8
        );

        inputBar.setBackgroundColor(
                Color.WHITE
        );

        mediaButton =
                new ImageButton(this);

        mediaButton.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        mediaButton.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        mediaButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        mediaButton.setPadding(
                8,
                8,
                8,
                8
        );

        mediaButton.setOnClickListener(
                v -> pickMedia()
        );

        inputBar.addView(
                mediaButton,
                new LinearLayout.LayoutParams(
                        62,
                        62
                )
        );

        messageInput =
                new EditText(this);

        messageInput.setHint(
                "پیام خود را بنویسید..."
        );

        messageInput.setTextSize(18);

        messageInput.setSingleLine(false);

        messageInput.setMaxLines(4);

        messageInput.setPadding(
                15,
                10,
                15,
                10
        );

        messageInput.setBackground(
                roundedBackground(
                        Color.rgb(
                                240,
                                244,
                                246
                        ),
                        22
                )
        );

        inputBar.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        voiceButton =
                new ImageButton(this);

        voiceButton.setImageResource(
                android.R.drawable.ic_btn_speak_now
        );

        voiceButton.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        voiceButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        voiceButton.setPadding(
                7,
                7,
                7,
                7
        );

        voiceButton.setOnClickListener(
                v -> toggleRecording()
        );

        inputBar.addView(
                voiceButton,
                new LinearLayout.LayoutParams(
                        64,
                        62
                )
        );

        sendButton =
                new ImageButton(this);

        sendButton.setImageResource(
                android.R.drawable.ic_menu_send
        );

        sendButton.setScaleType(
                ImageView.ScaleType.CENTER_INSIDE
        );

        sendButton.setBackgroundColor(
                Color.TRANSPARENT
        );

        sendButton.setColorFilter(
                themeColor
        );

        sendButton.setPadding(
                7,
                7,
                7,
                7
        );

        sendButton.setOnClickListener(
                v -> sendText()
        );

        inputBar.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        64,
                        62
                )
        );

        root.addView(
                inputBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

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

                        if (!insideChat ||
                                receiverId == null) {
                            return;
                        }

                        if (s.length() > 0) {
                            setTyping(true);
                        } else {
                            setTyping(false);
                        }
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        if (photoUrl != null &&
                !photoUrl.trim().isEmpty()) {

            loadImage(
                    photoUrl,
                    headerAvatar
            );
        }

        setContentView(root);
    }

    private void listenMessages() {

        if (currentChatId == null ||
                myId == null) {
            return;
        }

        if (sentMessageListener != null) {
            sentMessageListener.remove();
        }

        if (receivedMessageListener != null) {
            receivedMessageListener.remove();
        }

        messageCache.clear();

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
                                (snapshot, error) -> {

                                    if (error != null) {

                                        Toast.makeText(
                                                this,
                                                "خطای پیام‌های ارسالی: " +
                                                        error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    applyMessageChanges(
                                            snapshot
                                    );

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
                                (snapshot, error) -> {

                                    if (error != null) {

                                        Toast.makeText(
                                                this,
                                                "خطای پیام‌های دریافتی: " +
                                                        error.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();

                                        return;
                                    }

                                    if (snapshot == null) {
                                        return;
                                    }

                                    applyMessageChanges(
                                            snapshot
                                    );

                                    markUnreadFromSnapshot(
                                            snapshot
                                    );

                                    renderMessages();
                                }
                        );
    }

    private void applyMessageChanges(
            QuerySnapshot snapshot
    ) {

        for (DocumentChange change :
                snapshot.getDocumentChanges()) {

            DocumentSnapshot doc =
                    change.getDocument();

            String id =
                    doc.getId();

            if (change.getType() ==
                    DocumentChange.Type.REMOVED) {

                messageCache.remove(id);

            } else {

                messageCache.put(
                        id,
                        doc
                );
            }
        }
    }

    private void markUnreadFromSnapshot(
            QuerySnapshot snapshot
    ) {

        if (myId == null) {
            return;
        }

        for (DocumentSnapshot doc :
                snapshot.getDocuments()) {

            Boolean read =
                    doc.getBoolean("read");

            if (Boolean.TRUE.equals(read)) {
                continue;
            }

            db.collection("messages")
                    .document(doc.getId())
                    .update(
                            "read",
                            true
                    );
        }
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
                new Comparator<DocumentSnapshot>() {

                    @Override
                    public int compare(
                            DocumentSnapshot a,
                            DocumentSnapshot b
                    ) {

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
                }
        );

        for (DocumentSnapshot doc :
                list) {

            renderOneMessage(doc);
        }

        messagesScroll.post(
                () -> messagesScroll.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    private void renderOneMessage(
            DocumentSnapshot doc
    ) {

        String senderId =
                doc.getString("senderId");

        if (senderId == null) {
            return;
        }

        boolean deletedForAll =
                Boolean.TRUE.equals(
                        doc.getBoolean(
                                "deletedForAll"
                        )
                );

        if (deletedForAll) {

            addMessageBubble(
                    "🚫 این پیام برای همه حذف شده است",
                    senderId.equals(myId),
                    false,
                    null
            );

            return;
        }

        Object deletedObject =
                doc.get("deletedFor");

        if (deletedObject instanceof List) {

            for (Object item :
                    (List<?>) deletedObject) {

                if (item != null &&
                        myId.equals(
                                String.valueOf(item)
                        )) {

                    return;
                }
            }
        }

        String type =
                doc.getString("type");

        if (type == null) {
            type = "text";
        }

        boolean mine =
                senderId.equals(myId);

        boolean read =
                Boolean.TRUE.equals(
                        doc.getBoolean("read")
                );

        if ("text".equals(type)) {

            String text =
                    doc.getString(
                            "message"
                    );

            if (text == null) {
                text = "";
            }

            addMessageBubble(
                    text,
                    mine,
                    true,
                    doc.getId(),
                    read
            );

        } else if ("image".equals(type)) {

            addMediaMessage(
                    doc.getString("mediaUrl"),
                    mine,
                    false,
                    doc.getId(),
                    read
            );

        } else if ("video".equals(type)) {

            addMediaMessage(
                    doc.getString("mediaUrl"),
                    mine,
                    true,
                    doc.getId(),
                    read
            );

        } else if ("audio".equals(type)) {

            addAudioMessage(
                    doc.getString("audioUrl"),
                    mine,
                    doc.getId(),
                    read
            );
        }
    }

    private void addMessageBubble(
            String text,
            boolean mine,
            boolean allowMenu
    ) {

        addMessageBubble(
                text,
                mine,
                allowMenu,
                null,
                false
        );
    }

    private void addMessageBubble(
            String text,
            boolean mine,
            boolean allowMenu,
            String messageId
    ) {

        addMessageBubble(
                text,
                mine,
                allowMenu,
                messageId,
                false
        );
    }

    private void addMessageBubble(
            String text,
            boolean mine,
            boolean allowMenu,
            String messageId,
            boolean read
    ) {

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        LinearLayout bubbleBox =
                new LinearLayout(this);

        bubbleBox.setOrientation(
                LinearLayout.VERTICAL
        );

        bubbleBox.setPadding(
                16,
                11,
                13,
                7
        );

        bubbleBox.setBackground(
                roundedBackground(
                        mine
                                ? themeColor
                                : Color.WHITE,
                        22
                )
        );

        TextView bubble =
                makeText(
                        text,
                        18,
                        mine
                                ? Color.WHITE
                                : Color.rgb(
                                        30,
                                        30,
                                        30
                                )
                );

        bubble.setPadding(
                0,
                0,
                0,
                2
        );

        bubble.setMaxWidth(
                (int) (
                        getResources()
                                .getDisplayMetrics()
                                .widthPixels
                                * 0.78
                )
        );

        bubbleBox.addView(
                bubble
        );

        if (mine &&
                messageId != null) {

            TextView check =
                    makeText(
                            read
                                    ? "✓✓"
                                    : "✓",
                            15,
                            read
                                    ? Color.rgb(
                                            50,
                                            230,
                                            90
                                    )
                                    : Color.WHITE
                    );

            check.setGravity(
                    Gravity.RIGHT
            );

            check.setTypeface(
                    Typeface.DEFAULT_BOLD
            );

            bubbleBox.addView(
                    check
            );
        }

        row.addView(
                bubbleBox
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(
                5,
                5,
                5,
                5
        );

        messagesContainer.addView(
                row,
                params
        );

        if (allowMenu &&
                messageId != null) {

            bubbleBox.setOnLongClickListener(
                    v -> {

                        showDeleteMenu(
                                messageId,
                                mine
                        );

                        return true;
                    }
            );
        }
    }

    private void addMediaMessage(
            String url,
            boolean mine,
            boolean video,
            String messageId,
            boolean read
    ) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.VERTICAL
        );

        ImageView image =
                new ImageView(this);

        image.setImageResource(
                android.R.drawable.ic_menu_gallery
        );

        image.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        image.setBackground(
                roundedBackground(
                        Color.WHITE,
                        18
                )
        );

        box.addView(
                image,
                new LinearLayout.LayoutParams(
                        260,
                        260
                )
        );

        if (mine) {

            TextView check =
                    makeText(
                            read
                                    ? "✓✓"
                                    : "✓",
                            15,
                            read
                                    ? Color.rgb(
                                            50,
                                            220,
                                            90
                                    )
                                    : themeColor
                    );

            check.setGravity(
                    Gravity.RIGHT
            );

            box.addView(
                    check
            );
        }

        row.addView(box);

        messagesContainer.addView(
                row
        );

        loadImage(
                url,
                image
        );

        image.setOnLongClickListener(
                v -> {

                    showDeleteMenu(
                            messageId,
                            mine
                    );

                    return true;
                }
        );
    }

    private void addAudioMessage(
            String url,
            boolean mine,
            String messageId,
            boolean read
    ) {

        if (url == null ||
                url.trim().isEmpty()) {
            return;
        }

        LinearLayout row =
                new LinearLayout(this);

        row.setOrientation(
                LinearLayout.HORIZONTAL
        );

        row.setGravity(
                mine
                        ? Gravity.RIGHT
                        : Gravity.LEFT
        );

        LinearLayout box =
                new LinearLayout(this);

        box.setOrientation(
                LinearLayout.HORIZONTAL
        );

        Button play =
                makeButton(
                        "▶️  پخش پیام صوتی"
                );

        play.setTextSize(17);

        play.setPadding(
                20,
                8,
                20,
                8
        );

        play.setOnClickListener(
                v -> playAudio(url)
        );

        box.addView(
                play
        );

        if (mine) {

            TextView check =
                    makeText(
                            read
                                    ? "✓✓"
                                    : "✓",
                            15,
                            read
                                    ? Color.rgb(
                                            50,
                                            220,
                                            90
                                    )
                                    : themeColor
                    );

            check.setPadding(
                    10,
                    0,
                    5,
                    0
            );

            box.addView(check);
        }

        row.addView(box);

        messagesContainer.addView(
                row
        );

        play.setOnLongClickListener(
                v -> {

                    showDeleteMenu(
                            messageId,
                            mine
                    );

                    return true;
                }
        );
    }

    private void showDeleteMenu(
            String messageId,
            boolean mine
    ) {

        List<String> options =
                new ArrayList<>();

        options.add(
                "🗑️ حذف برای من"
        );

        if (mine) {
            options.add(
                    "🗑️ حذف برای همه"
            );
        }

        options.add("لغو");

        new AlertDialog.Builder(this)
                .setTitle("مدیریت پیام")
                .setItems(
                        options.toArray(
                                new String[0]
                        ),
                        (dialog, which) -> {

                            if (which == 0) {

                                deleteForMe(
                                        messageId
                                );

                            } else if (
                                    mine &&
                                            which == 1
                            ) {

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
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای حذف پیام: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
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
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای حذف پیام: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
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

        if (receiverId == null ||
                currentChatId == null) {
            return;
        }

        String text =
                messageInput
                        .getText()
                        .toString()
                        .trim();

        if (text.isEmpty()) {
            return;
        }

        Map<String, Object> message =
                new HashMap<>();

        message.put(
                "chatId",
                currentChatId
        );

        message.put(
                "senderId",
                myId
        );

        message.put(
                "receiverId",
                receiverId
        );

        message.put(
                "type",
                "text"
        );

        message.put(
                "message",
                text
        );

        message.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        message.put(
                "read",
                false
        );

        message.put(
                "deletedForAll",
                false
        );

        message.put(
                "deletedFor",
                new ArrayList<String>()
        );

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(
                        doc -> {

                            messageInput.setText("");

                            setTyping(false);
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای ارسال پیام: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void listenReceiver() {

        if (receiverId == null) {
            return;
        }

        if (receiverListener != null) {
            receiverListener.remove();
        }

        receiverListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (doc, error) -> {

                                    if (error != null ||
                                            doc == null ||
                                            !doc.exists()) {
                                        return;
                                    }

                                    Boolean online =
                                            doc.getBoolean(
                                                    "online"
                                            );

                                    updateOnlineStatus(
                                            Boolean.TRUE.equals(
                                                    online
                                            )
                                    );

                                    String photo =
                                            doc.getString(
                                                    "photoUrl"
                                            );

                                    if (photo != null &&
                                            !photo.isEmpty() &&
                                            headerAvatar != null) {

                                        loadImage(
                                                photo,
                                                headerAvatar
                                        );
                                    }
                                }
                        );
    }

    private void updateOnlineStatus(
            boolean online
    ) {

        if (statusText == null) {
            return;
        }

        if (online) {

            statusText.setText(
                    "آنلاین"
            );

            statusText.setTextColor(
                    Color.rgb(
                            70,
                            255,
                            100
                    )
            );

            if (headerOnlineDot != null) {

                headerOnlineDot.setText(
                        "●"
                );

                headerOnlineDot.setTextColor(
                        Color.rgb(
                                50,
                                240,
                                80
                        )
                );
            }

        } else {

            statusText.setText(
                    "آفلاین"
            );

            statusText.setTextColor(
                    Color.LTGRAY
            );

            if (headerOnlineDot != null) {

                headerOnlineDot.setText(
                        "●"
                );

                headerOnlineDot.setTextColor(
                        Color.GRAY
                );
            }
        }
    }

    private void listenTyping() {

        if (receiverId == null) {
            return;
        }

        if (typingListener != null) {
            typingListener.remove();
        }

        typingListener =
                db.collection("users")
                        .document(receiverId)
                        .addSnapshotListener(
                                (doc, error) -> {

                                    if (error != null ||
                                            doc == null ||
                                            !doc.exists()) {
                                        return;
                                    }

                                    String typingTo =
                                            doc.getString(
                                                    "typingTo"
                                            );

                                    if (myId != null &&
                                            myId.equals(
                                                    typingTo
                                            )) {

                                        statusText.setText(
                                                "✍️ در حال نوشتن..."
                                        );

                                        statusText.setTextColor(
                                                Color.WHITE
                                        );

                                    } else {

                                        Boolean online =
                                                doc.getBoolean(
                                                        "online"
                                                );

                                        updateOnlineStatus(
                                                Boolean.TRUE.equals(
                                                        online
                                                )
                                        );
                                    }
                                }
                        );
    }

    private void setTyping(
            boolean value
    ) {

        if (myId == null ||
                receiverId == null) {
            return;
        }

        typing = value;

        typingHandler.removeCallbacksAndMessages(
                null
        );

        String valueToSave =
                value
                        ? receiverId
                        : "";

        db.collection("users")
                .document(myId)
                .update(
                        "typingTo",
                        valueToSave
                );

        if (value) {

            typingHandler.postDelayed(
                    () -> {

                        if (typing) {
                            setTyping(false);
                        }

                    },
                    3000
            );
        }
    }

    private void listenBlockStatus() {

        if (receiverId == null) {
            return;
        }

        if (blockListener != null) {
            blockListener.remove();
        }

        String blockId =
                myId + "_" + receiverId;

        blockListener =
                db.collection("blocks")
                        .document(blockId)
                        .addSnapshotListener(
                                (doc, error) -> {

                                    if (error != null) {
                                        return;
                                    }

                                    blocked =
                                            doc != null &&
                                                    doc.exists();

                                    if (blocked) {

                                        statusText.setText(
                                                "🚫 بلاک شده"
                                        );

                                        statusText.setTextColor(
                                                Color.WHITE
                                        );

                                        messageInput.setEnabled(
                                                false
                                        );

                                        sendButton.setEnabled(
                                                false
                                        );

                                        mediaButton.setEnabled(
                                                false
                                        );

                                        voiceButton.setEnabled(
                                                false
                                        );

                                    } else {

                                        messageInput.setEnabled(
                                                true
                                        );

                                        sendButton.setEnabled(
                                                true
                                        );

                                        mediaButton.setEnabled(
                                                true
                                        );

                                        voiceButton.setEnabled(
                                                true
                                        );
                                    }
                                }
                        );
    }

    private void showChatMenu() {

        String blockText =
                blocked
                        ? "✅ رفع بلاک"
                        : "🚫 بلاک کردن";

        String[] options = {
                "👤 دیدن پروفایل",
                blockText,
                "🗑️ حذف کاربر از فهرست من",
                "🔄 تازه‌سازی پیام‌ها",
                "لغو"
        };

        new AlertDialog.Builder(this)
                .setTitle(receiverName)
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                showUserProfile(receiverId);

                            } else if (which == 1) {

                                if (blocked) {
                                    unblockUser();
                                } else {
                                    blockUser();
                                }

                            } else if (which == 2) {

                                confirmDeleteUser(
                                        receiverId,
                                        receiverName
                                );

                            } else if (which == 3) {

                                listenMessages();
                            }
                        }
                )
                .show();
    }

    private void blockUser() {

        if (receiverId == null) {
            return;
        }

        String blockId =
                myId + "_" + receiverId;

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
                        v -> Toast.makeText(
                                this,
                                "کاربر بلاک شد",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای بلاک: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void unblockUser() {

        if (receiverId == null) {
            return;
        }

        String blockId =
                myId + "_" + receiverId;

        db.collection("blocks")
                .document(blockId)
                .delete()
                .addOnSuccessListener(
                        v -> Toast.makeText(
                                this,
                                "بلاک برداشته شد",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای رفع بلاک: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
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

            if ((data.getFlags() &
                    Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {

                getContentResolver()
                        .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );
            }

        } catch (Exception ignored) {
        }

        if (requestCode == PICK_MEDIA) {

            uploadMedia(uri);

        } else if (requestCode == PICK_PROFILE) {

            uploadProfilePhoto(uri);
        }
    }

    private String encodePath(
            String path
    ) {

        try {

            String[] parts =
                    path.split("/");

            StringBuilder result =
                    new StringBuilder();

            for (int i = 0;
                    i < parts.length;
                    i++) {

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

        } catch (Exception e) {

            return path;
        }
    }

    private String getSupabasePublicUrl(
            String bucket,
            String path
    ) {

        return SUPABASE_URL +
                "/storage/v1/object/public/" +
                bucket +
                "/" +
                encodePath(path);
    }

    private String readErrorResponse(
            HttpURLConnection connection
    ) {

        InputStream errorStream = null;

        try {

            errorStream =
                    connection.getErrorStream();

            if (errorStream == null) {
                return "";
            }

            java.io.ByteArrayOutputStream buffer =
                    new java.io.ByteArrayOutputStream();

            byte[] data =
                    new byte[4096];

            int n;

            while (
                    (n = errorStream.read(data)) != -1
            ) {

                buffer.write(
                        data,
                        0,
                        n
                );
            }

            return buffer.toString(
                    "UTF-8"
            );

        } catch (Exception e) {

            return "";

        } finally {

            try {

                if (errorStream != null) {
                    errorStream.close();
                }

            } catch (Exception ignored) {
            }
        }
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

                        /*
                         * POST برای Storage Supabase.
                         */
                        connection.setRequestMethod(
                                "POST"
                        );

                        connection.setDoOutput(
                                true
                        );

                        connection.setDoInput(
                                true
                        );

                        connection.setConnectTimeout(
                                30000
                        );

                        connection.setReadTimeout(
                                60000
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

                        connection.setRequestProperty(
                                "Content-Type",
                                contentType
                        );

                        connection.setRequestProperty(
                                "x-upsert",
                                "true"
                        );

                        /*
                         * برای صدا Uri.fromFile داریم،
                         * بنابراین مستقیم از FileInputStream
                         * استفاده می‌کنیم.
                         */
                        if ("file".equals(
                                uri.getScheme()
                        )) {

                            input =
                                    new FileInputStream(
                                            new File(
                                                    uri.getPath()
                                            )
                                    );

                        } else {

                            input =
                                    getContentResolver()
                                            .openInputStream(uri);
                        }

                        if (input == null) {

                            throw new Exception(
                                    "فایل قابل خواندن نیست"
                            );
                        }

                        output =
                                connection.getOutputStream();

                        byte[] buffer =
                                new byte[8192];

                        int length;

                        while (
                                (length =
                                        input.read(
                                                buffer
                                        )) != -1
                        ) {

                            output.write(
                                    buffer,
                                    0,
                                    length
                            );
                        }

                        output.flush();

                        int responseCode =
                                connection.getResponseCode();

                        if (responseCode >= 200 &&
                                responseCode < 300) {

                            String publicUrl =
                                    getSupabasePublicUrl(
                                            bucket,
                                            objectPath
                                    );

                            runOnUiThread(
                                    () -> callback.onSuccess(
                                            publicUrl
                                    )
                            );

                        } else {

                            String body =
                                    readErrorResponse(
                                            connection
                                    );

                            String errorText =
                                    "HTTP " +
                                            responseCode;

                            if (body != null &&
                                    !body.trim().isEmpty()) {

                                errorText +=
                                        "\n" +
                                                body;
                            }

                            String finalError =
                                    errorText;

                            runOnUiThread(
                                    () -> callback.onError(
                                            finalError
                                    )
                            );
                        }

                    } catch (Exception e) {

                        String message =
                                e.getMessage();

                        if (message == null ||
                                message.trim().isEmpty()) {

                            message =
                                    e.getClass()
                                            .getSimpleName();
                        }

                        String finalMessage =
                                message;

                        runOnUiThread(
                                () -> callback.onError(
                                        finalMessage
                                )
                        );

                    } finally {

                        try {

                            if (input != null) {
                                input.close();
                            }

                        } catch (Exception ignored) {
                        }

                        try {

                            if (output != null) {
                                output.close();
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

    private interface SupabaseUploadCallback {

        void onSuccess(
                String publicUrl
        );

        void onError(
                String error
        );
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

        if (uri == null) {
            return;
        }

        String mime =
                getContentResolver()
                        .getType(uri);

        boolean image =
                mime != null &&
                        mime.startsWith(
                                "image/"
                        );

        boolean video =
                mime != null &&
                        mime.startsWith(
                                "video/"
                        );

        if (!image && !video) {

            Toast.makeText(
                    this,
                    "فقط عکس یا ویدیو انتخاب کنید",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        String type =
                image
                        ? "image"
                        : "video";

        String extension;

        if (mime != null &&
                mime.equals("image/png")) {

            extension = ".png";

        } else if (mime != null &&
                mime.equals("image/webp")) {

            extension = ".webp";

        } else if (image) {

            extension = ".jpg";

        } else if (mime != null &&
                mime.equals("video/3gpp")) {

            extension = ".3gp";

        } else {

            extension = ".mp4";
        }

        String fileName =
                System.currentTimeMillis() +
                        "_" +
                        myId +
                        extension;

        String objectPath =
                "chat/" +
                        fileName;

        Toast.makeText(
                this,
                "در حال ارسال فایل...",
                Toast.LENGTH_SHORT
        ).show();

        String contentType =
                mime != null
                        ? mime
                        : image
                        ? "image/jpeg"
                        : "video/mp4";

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                contentType,
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String publicUrl
                    ) {

                        saveMediaMessage(
                                type,
                                publicUrl
                        );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای ارسال فایل:\n" +
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

        if (currentChatId == null ||
                receiverId == null) {
            return;
        }

        Map<String, Object> message =
                new HashMap<>();

        message.put(
                "chatId",
                currentChatId
        );

        message.put(
                "senderId",
                myId
        );

        message.put(
                "receiverId",
                receiverId
        );

        message.put(
                "type",
                type
        );

        message.put(
                "mediaUrl",
                url
        );

        message.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        message.put(
                "read",
                false
        );

        message.put(
                "deletedForAll",
                false
        );

        message.put(
                "deletedFor",
                new ArrayList<String>()
        );

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(
                        doc -> Toast.makeText(
                                this,
                                "فایل ارسال شد ✅",
                                Toast.LENGTH_SHORT
                        ).show()
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای ذخیره پیام فایل: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void toggleRecording() {

        if (blocked) {

            Toast.makeText(
                    this,
                    "این کاربر بلاک شده است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (recording) {

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
                                    ".m4a"
                    );

            audioPath =
                    file.getAbsolutePath();

            recorder =
                    new MediaRecorder();

            recorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );

            recorder.setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
            );

            recorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
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

            if (recorder != null) {

                try {
                    recorder.release();
                } catch (Exception ignored) {
                }

                recorder = null;
            }

            Toast.makeText(
                    this,
                    "خطای میکروفون: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopRecording() {

        if (recorder == null) {

            recording = false;

            return;
        }

        boolean stopped =
                false;

        try {

            recorder.stop();

            stopped = true;

        } catch (Exception ignored) {
        }

        try {

            recorder.release();

        } catch (Exception ignored) {
        }

        recorder = null;

        recording = false;

        if (voiceButton != null) {

            voiceButton.setImageResource(
                    android.R.drawable.ic_btn_speak_now
            );
        }

        if (!stopped) {

            Toast.makeText(
                    this,
                    "ضبط صدا خیلی کوتاه بود یا کامل نشد",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (audioPath != null) {

            uploadVoice(
                    audioPath
            );
        }
    }

    private void uploadVoice(
            String path
    ) {

        if (blocked) {
            return;
        }

        if (path == null ||
                path.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "مسیر فایل صوتی خالی است",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        File file =
                new File(path);

        if (!file.exists() ||
                file.length() == 0) {

            Toast.makeText(
                    this,
                    "فایل صوتی پیدا نشد یا خالی است",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        Uri uri =
                Uri.fromFile(file);

        String fileName =
                System.currentTimeMillis() +
                        "_" +
                        myId +
                        ".m4a";

        String objectPath =
                "voice/" +
                        fileName;

        Toast.makeText(
                this,
                "در حال ارسال پیام صوتی...",
                Toast.LENGTH_SHORT
        ).show();

        /*
         * برای m4a از octet-stream استفاده می‌کنیم
         * تا خطای 400 مربوط به MIME کمتر شود.
         * خود فایل همچنان m4a است.
         */
        uploadToSupabase(
                uri,
                VOICE_BUCKET,
                objectPath,
                "application/octet-stream",
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String publicUrl
                    ) {

                        saveAudioMessage(
                                publicUrl
                        );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای ارسال صدا:\n" +
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

        if (currentChatId == null ||
                receiverId == null) {
            return;
        }

        Map<String, Object> message =
                new HashMap<>();

        message.put(
                "chatId",
                currentChatId
        );

        message.put(
                "senderId",
                myId
        );

        message.put(
                "receiverId",
                receiverId
        );

        message.put(
                "type",
                "audio"
        );

        message.put(
                "audioUrl",
                url
        );

        message.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        message.put(
                "read",
                false
        );

        message.put(
                "deletedForAll",
                false
        );

        message.put(
                "deletedFor",
                new ArrayList<String>()
        );

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(
                        doc -> {

                            Toast.makeText(
                                    this,
                                    "پیام صوتی ارسال شد ✅",
                                    Toast.LENGTH_SHORT
                            ).show();

                            try {

                                File file =
                                        new File(
                                                audioPath
                                        );

                                if (file.exists()) {
                                    file.delete();
                                }

                            } catch (Exception ignored) {
                            }
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "خطای ذخیره صدا: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
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

            player.setDataSource(
                    url
            );

            player.setOnPreparedListener(
                    MediaPlayer::start
            );

            player.setOnCompletionListener(
                    mp -> {

                        mp.release();

                        if (player == mp) {
                            player = null;
                        }
                    }
            );

            player.prepareAsync();

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطای پخش صدا: " +
                            e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void uploadProfilePhoto(
            Uri uri
    ) {

        if (myId == null ||
                uri == null) {
            return;
        }

        String fileName =
                "profile_" +
                        myId +
                        "_" +
                        System.currentTimeMillis() +
                        ".jpg";

        String objectPath =
                "profiles/" +
                        fileName;

        Toast.makeText(
                this,
                "در حال آپلود عکس...",
                Toast.LENGTH_SHORT
        ).show();

        uploadToSupabase(
                uri,
                CHAT_BUCKET,
                objectPath,
                "image/jpeg",
                new SupabaseUploadCallback() {

                    @Override
                    public void onSuccess(
                            String publicUrl
                    ) {

                        Map<String, Object> update =
                                new HashMap<>();

                        update.put(
                                "photoUrl",
                                publicUrl
                        );

                        db.collection("users")
                                .document(myId)
                                .set(
                                        update,
                                        SetOptions.merge()
                                )
                                .addOnSuccessListener(
                                        v -> {

                                            Toast.makeText(
                                                    ChatActivity.this,
                                                    "عکس پروفایل ذخیره شد ✅",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            showOwnProfile();

                                            loadUsers();
                                        }
                                )
                                .addOnFailureListener(
                                        e ->
                                                Toast.makeText(
                                                        ChatActivity.this,
                                                        "خطای ذخیره عکس پروفایل: " +
                                                                e.getMessage(),
                                                        Toast.LENGTH_LONG
                                                ).show()
                                );
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        Toast.makeText(
                                ChatActivity.this,
                                "خطای آپلود عکس:\n" +
                                        error,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void loadImage(
            String url,
            ImageView imageView
    ) {

        new Thread(
                () -> {

                    HttpURLConnection connection =
                            null;

                    InputStream input =
                            null;

                    try {

                        URL imageUrl =
                                new URL(url);

                        connection =
                                (HttpURLConnection)
                                        imageUrl.openConnection();

                        connection.setConnectTimeout(
                                10000
                        );

                        connection.setReadTimeout(
                                10000
                        );

                        connection.setDoInput(
                                true
                        );

                        connection.connect();

                        input =
                                connection.getInputStream();

                        Bitmap bitmap =
                                BitmapFactory.decodeStream(
                                        input
                                );

                        if (bitmap != null) {

                            runOnUiThread(
                                    () -> {

                                        if (!isFinishing()) {

                                            imageView.setImageBitmap(
                                                    bitmap
                                            );
                                        }
                                    }
                            );
                        }

                    } catch (Exception ignored) {

                    } finally {

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

    private void showUsers() {

        setTyping(false);

        removeListeners();

        insideChat = false;

        receiverId = null;
        receiverName = null;
        currentChatId = null;

        messageCache.clear();

        createUsersScreen();

        loadUsers();
    }

    private void removeListeners() {

        if (sentMessageListener != null) {

            sentMessageListener.remove();

            sentMessageListener = null;
        }

        if (receivedMessageListener != null) {

            receivedMessageListener.remove();

            receivedMessageListener = null;
        }

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
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            myId = user.getUid();

            Map<String, Object> data =
                    new HashMap<>();

            data.put(
                    "userId",
                    myId
            );

            data.put(
                    "online",
                    true
            );

            data.put(
                    "lastSeen",
                    FieldValue.serverTimestamp()
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
    }

    @Override
    protected void onDestroy() {

        setTyping(false);

        removeListeners();

        if (recorder != null) {

            try {
                recorder.stop();
            } catch (Exception ignored) {
            }

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }

        if (player != null) {

            try {
                player.release();
            } catch (Exception ignored) {
            }

            player = null;
        }

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
}
