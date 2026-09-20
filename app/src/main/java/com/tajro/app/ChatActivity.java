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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

/*
 * این دو مقدار را تغییر نده.
 * Secret Key را هرگز داخل برنامه نگذار.
 *
 * اگر Publishable Key فعلی خودت در فایل اصلی متفاوت است،
 * همان Publishable Key درست قبلی خودت را اینجا نگه دار.
 */
private static final String SUPABASE_URL =
        "https://gorbhuqmkjlkrklhasdh.supabase.co";

private static final String SUPABASE_PUBLISHABLE_KEY =
        "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";

private static final String CHAT_BUCKET = "chat_media";
private static final String VOICE_BUCKET = "voice_messages";

private FirebaseAuth auth;
private FirebaseFirestore db;
private FirebaseStorage storage;

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
    storage = FirebaseStorage.getInstance();

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
    FirebaseUser user = auth.getCurrentUser();

    if (user == null) {
        return;
    }

    Map<String, Object> data =
            new HashMap<>();

    data.put(
            "userId",
            user.getUid()
    );

    data.put(
            "email",
            user.getEmail()
    );

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
                    com.google.firebase.firestore.SetOptions
                            .merge()
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
    tv.setGravity(Gravity.CENTER_VERTICAL);
    tv.setTypeface(Typeface.DEFAULT);

    return tv;
}

private Button makeButton(
        String text
) {
    Button button =
            new Button(this);

    button.setText(text);
    button.setTextSize(15);
    button.setTextColor(Color.WHITE);
    button.setAllCaps(false);
    button.setGravity(Gravity.CENTER);
    button.setPadding(
            18,
            8,
            18,
            8
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
            Color.rgb(235, 248, 250)
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
            16,
            16,
            16,
            16
    );

    header.setBackgroundColor(
            themeColor
    );

    TextView title =
            makeText(
                    "💬 کاربران",
                    22,
                    Color.WHITE
            );

    title.setTypeface(
            Typeface.DEFAULT_BOLD
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

    refresh.setTextSize(19);

    refresh.setOnClickListener(
            v -> loadUsers()
    );

    header.addView(
            refresh,
            new LinearLayout.LayoutParams(
                    58,
                    52
            )
    );

    root.addView(header);

    TextView info =
            makeText(
                    "برای گفتگو روی کاربر بزنید؛ برای مدیریت کاربر لمس طولانی کنید.",
                    15,
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
                    17,
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

                        List<DocumentSnapshot> users =
                                new ArrayList<>();

                        for (DocumentSnapshot doc :
                                snapshot.getDocuments()) {

                            String uid =
                                    doc.getId();

                            if (uid.equals(myId)) {
                                continue;
                            }

                            if (hiddenUserIds.contains(uid)) {
                                continue;
                            }

                            users.add(doc);
                        }

                        Collections.sort(
                                users,
                                new Comparator<DocumentSnapshot>() {
                                    @Override
                                    public int compare(
                                            DocumentSnapshot a,
                                            DocumentSnapshot b
                                    ) {

                                        String nameA =
                                                a.getString("name");

                                        String nameB =
                                                b.getString("name");

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
                                            17,
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
                                        16,
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
            doc.getId();

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

    LinearLayout avatarBox =
            new LinearLayout(this);

    avatarBox.setGravity(
            Gravity.CENTER
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

    avatarBox.addView(
            avatar,
            new LinearLayout.LayoutParams(
                    60,
                    60
            )
    );

    if (Boolean.TRUE.equals(online)) {

        View greenDot =
                new View(this);

        greenDot.setBackground(
                roundedBackground(
                        Color.rgb(20, 190, 70),
                        50
                )
        );

        LinearLayout.LayoutParams dotParams =
                new LinearLayout.LayoutParams(
                        15,
                        15
                );

        dotParams.gravity =
                Gravity.BOTTOM |
                        Gravity.RIGHT;

        avatarBox.addView(
                greenDot,
                dotParams
        );
    }

    card.addView(
            avatarBox,
            new LinearLayout.LayoutParams(
                    66,
                    66
            )
    );

    LinearLayout nameBox =
            new LinearLayout(this);

    nameBox.setOrientation(
            LinearLayout.VERTICAL
    );

    nameBox.setPadding(
            14,
            0,
            8,
            0
    );

    TextView nameText =
            makeText(
                    name,
                    18,
                    Color.rgb(25, 25, 25)
            );

    nameText.setTypeface(
            Typeface.DEFAULT_BOLD
    );

    nameBox.addView(
            nameText
    );

    TextView hint =
            makeText(
                    Boolean.TRUE.equals(online)
                            ? "🟢 آنلاین"
                            : "⚪ آفلاین",
                    14,
                    Boolean.TRUE.equals(online)
                            ? Color.rgb(15, 150, 55)
                            : Color.GRAY
            );

    nameBox.addView(
            hint
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

    menu.setTextSize(23);

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

    menu.setOnClickListener(
            v -> showUserManagementMenu(
                    uid,
                    finalName
            )
    );

    card.addView(
            menu,
            new LinearLayout.LayoutParams(
                    52,
                    52
            )
    );

    card.setOnClickListener(
            v -> openPrivateChat(
                    uid,
                    finalName,
                    finalPhotoUrl
            )
    );

    card.setOnLongClickListener(
            v -> {

                showUserManagementMenu(
                        uid,
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

                        } else if (which == 1) {

                            blockUserFromList(
                                    uid,
                                    name
                            );

                        } else if (which == 2) {

                            confirmDeleteUser(
                                    uid,
                                    name
                            );
                        }
                    }
            )
            .show();
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
            Color.rgb(235, 248, 250)
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

    back.setTextSize(31);

    back.setBackgroundColor(
            Color.TRANSPARENT
    );

    back.setOnClickListener(
            v -> showUsers()
    );

    header.addView(
            back,
            new LinearLayout.LayoutParams(
                    55,
                    55
            )
    );

    LinearLayout headerAvatarBox =
            new LinearLayout(this);

    headerAvatarBox.setGravity(
            Gravity.CENTER
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

    headerAvatarBox.addView(
            headerAvatar,
            new LinearLayout.LayoutParams(
                    50,
                    50
            )
    );

    headerOnlineDot =
            makeText(
                    "●",
                    20,
                    Color.GRAY
            );

    headerOnlineDot.setGravity(
            Gravity.CENTER
    );

    headerAvatarBox.addView(
            headerOnlineDot,
            new LinearLayout.LayoutParams(
                    22,
                    50
            )
    );

    header.addView(
            headerAvatarBox,
            new LinearLayout.LayoutParams(
                    72,
                    55
            )
    );

    LinearLayout titleBox =
            new LinearLayout(this);

    titleBox.setOrientation(
            LinearLayout.VERTICAL
    );

    titleBox.setPadding(
            10,
            0,
            5,
            0
    );

    titleText =
            makeText(
                    receiverName,
                    19,
                    Color.WHITE
            );

    titleText.setTypeface(
            Typeface.DEFAULT_BOLD
    );

    titleBox.addView(
            titleText
    );

    statusText =
            makeText(
                    "در حال اتصال...",
                    14,
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

    menu.setTextSize(26);

    menu.setBackgroundColor(
            Color.TRANSPARENT
    );

    menu.setOnClickListener(
            v -> showChatMenu()
    );

    header.addView(
            menu,
            new LinearLayout.LayoutParams(
                    55,
                    55
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
            7,
            7,
            7
    );

    inputBar.setBackgroundColor(
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

    mediaButton.setOnClickListener(
            v -> pickMedia()
    );

    inputBar.addView(
            mediaButton,
            new LinearLayout.LayoutParams(
                    48,
                    52
            )
    );

    messageInput =
            new EditText(this);

    messageInput.setHint(
            "پیام خود را بنویسید..."
    );

    messageInput.setTextSize(17);

    messageInput.setSingleLine(false);

    messageInput.setMaxLines(4);

    messageInput.setPadding(
            15,
            9,
            15,
            9
    );

    messageInput.setBackground(
            roundedBackground(
                    Color.rgb(240, 244, 246),
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

    voiceButton.setBackgroundColor(
            Color.TRANSPARENT
    );

    voiceButton.setOnClickListener(
            v -> toggleRecording()
    );

    inputBar.addView(
            voiceButton,
            new LinearLayout.LayoutParams(
                    50,
                    52
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

    sendButton.setColorFilter(
            themeColor
    );

    sendButton.setOnClickListener(
            v -> sendText()
    );

    inputBar.addView(
            sendButton,
            new LinearLayout.LayoutParams(
                    50,
                    52
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

/*
 * اصلاح اصلی دریافت پیام:
 *
 * Query اول فقط پیام‌هایی را می‌گیرد که خود من فرستاده‌ام.
 * Query دوم فقط پیام‌هایی را می‌گیرد که برای خود من آمده‌اند.
 *
 * این دقیقاً با Firestore Rules فعلی سازگار است.
 */
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

        /*
         * چون این Query فقط receiverId == myId است،
         * طبق Rules فعلی اجازه update داریم.
         */
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
                            a.getTimestamp("timestamp");

                    com.google.firebase.Timestamp tb =
                            b.getTimestamp("timestamp");

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

    Object deletedObject =
            doc.get("deletedFor");

    List<String> deletedFor =
            new ArrayList<>();

    if (deletedObject instanceof List) {

        try {

            for (Object item :
                    (List<?>) deletedObject) {

                if (item != null) {
                    deletedFor.add(
                            String.valueOf(item)
                    );
                }
            }

        } catch (Exception ignored) {
        }
    }

    boolean deletedForAll =
            Boolean.TRUE.equals(
                    doc.getBoolean("deletedForAll")
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

    if (deletedFor.contains(myId)) {
        return;
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
                doc.getString("message");

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

        String url =
                doc.getString("mediaUrl");

        addMediaMessage(
                url,
                mine,
                false,
                doc.getId(),
                read
        );

    } else if ("video".equals(type)) {

        String url =
                doc.getString("mediaUrl");

        addMediaMessage(
                url,
                mine,
                true,
                doc.getId(),
                read
        );

    } else if ("audio".equals(type)) {

        String url =
                doc.getString("audioUrl");

        addAudioMessage(
                url,
                mine,
                doc.getId(),
                read
        );

    } else {

        String text =
                doc.getString("message");

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

/*
 * نمایش وضعیت پیام:
 *
 * ✓  = ارسال شده
 * ✓✓ سبز = خوانده شده
 */
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
            10,
            13,
            8
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
                    17,
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
                        14,
                        read
                                ? Color.rgb(
                                        50,
                                        220,
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
            4,
            5,
            4
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
                    230,
                    230
            )
    );

    if (mine) {

        TextView check =
                makeText(
                        read
                                ? "✓✓"
                                : "✓",
                        14,
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

        box.addView(check);
    }

    row.addView(box);

    messagesContainer.addView(
            row
    );

    loadImage(
            url,
            image
    );

    image.setOnClickListener(
            v -> {

                if (video) {

                    Toast.makeText(
                            this,
                            "ویدیو انتخاب شد",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            }
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
                    "▶️ پخش پیام صوتی"
            );

    play.setTextSize(16);

    play.setOnClickListener(
            v -> playAudio(url)
    );

    box.addView(play);

    if (mine) {

        TextView check =
                makeText(
                        read
                                ? "✓✓"
                                : "✓",
                        14,
                        read
                                ? Color.rgb(
                                        50,
                                        220,
                                        90
                                )
                                : themeColor
                );

        check.setPadding(
                8,
                0,
                4,
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
                    FieldValue.arrayUnion(myId)
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
                                        !photo.isEmpty()) {

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
                "● آنلاین"
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
                "● آفلاین"
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

                            if (blocked) {
                                unblockUser();
                            } else {
                                blockUser();
                            }

                        } else if (which == 1) {

                            confirmDeleteUser(
                                    receiverId,
                                    receiverName
                            );

                        } else if (which == 2) {

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

    if (requestCode == PICK_MEDIA) {

        uploadMedia(uri);

    } else if (requestCode == PICK_PROFILE) {

        uploadProfilePhoto(uri);
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

    if (uri == null) {
        return;
    }

    String mime =
            getContentResolver()
                    .getType(uri);

    boolean image =
            mime != null &&
                    mime.startsWith("image/");

    boolean video =
            mime != null &&
                    mime.startsWith("video/");

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

    String extension =
            image
                    ? ".jpg"
                    : ".mp4";

    String fileName =
            System.currentTimeMillis()
                    + "_" +
                    myId
                    + extension;

    StorageReference ref =
            storage.getReference()
                    .child(
                            "chat_media/" +
                                    fileName
                    );

    Toast.makeText(
            this,
            "در حال ارسال فایل...",
            Toast.LENGTH_SHORT
    ).show();

    ref.putFile(uri)
            .addOnSuccessListener(
                    task -> ref.getDownloadUrl()
                            .addOnSuccessListener(
                                    downloadUri ->
                                            saveMediaMessage(
                                                    type,
                                                    downloadUri
                                                            .toString()
                                            )
                            )
                            .addOnFailureListener(
                                    e -> Toast.makeText(
                                            this,
                                            "فایل آپلود شد ولی لینک دریافت نشد: " +
                                                    e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            )
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "خطای ارسال فایل: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
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

    try {

        recorder.stop();

    } catch (Exception ignored) {
    }

    try {

        recorder.release();

    } catch (Exception ignored) {
    }

    recorder = null;

    recording = false;

    voiceButton.setImageResource(
            android.R.drawable.ic_btn_speak_now
    );

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
            System.currentTimeMillis()
                    + "_" +
                    myId
                    + ".m4a";

    StorageReference ref =
            storage.getReference()
                    .child(
                            "voice_messages/" +
                                    fileName
                    );

    Toast.makeText(
            this,
            "در حال ارسال پیام صوتی...",
            Toast.LENGTH_SHORT
    ).show();

    ref.putFile(uri)
            .addOnSuccessListener(
                    task ->
                            ref.getDownloadUrl()
                                    .addOnSuccessListener(
                                            downloadUri ->
                                                    saveAudioMessage(
                                                            downloadUri
                                                                    .toString()
                                                    )
                                    )
                                    .addOnFailureListener(
                                            e -> Toast.makeText(
                                                    this,
                                                    "صدا آپلود شد ولی لینک دریافت نشد: " +
                                                            e.getMessage(),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    )
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "خطای ارسال صدا: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
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

    if (myId == null) {
        return;
    }

    String fileName =
            "profile_" +
                    myId +
                    "_" +
                    System.currentTimeMillis() +
                    ".jpg";

    StorageReference ref =
            storage.getReference()
                    .child(
                            "profiles/" +
                                    fileName
                    );

    Toast.makeText(
            this,
            "در حال آپلود عکس...",
            Toast.LENGTH_SHORT
    ).show();

    ref.putFile(uri)
            .addOnSuccessListener(
                    task -> ref.getDownloadUrl()
                            .addOnSuccessListener(
                                    downloadUri -> {

                                        db.collection(
                                                "users"
                                        )
                                                .document(
                                                        myId
                                                )
                                                .update(
                                                        "photoUrl",
                                                        downloadUri
                                                                .toString()
                                                )
                                                .addOnSuccessListener(
                                                        v ->
                                                                Toast.makeText(
                                                                        this,
                                                                        "عکس پروفایل ذخیره شد",
                                                                        Toast.LENGTH_SHORT
                                                                ).show()
                                                )
                                                .addOnFailureListener(
                                                        e ->
                                                                Toast.makeText(
                                                                        this,
                                                                        "خطای ذخیره عکس پروفایل: " +
                                                                                e.getMessage(),
                                                                        Toast.LENGTH_LONG
                                                                ).show()
                                                );
                                    }
                            )
            )
            .addOnFailureListener(
                    e -> Toast.makeText(
                            this,
                            "خطای آپلود عکس: " +
                                    e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show()
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
                            8000
                    );

                    connection.setReadTimeout(
                            8000
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
                        com.google.firebase.firestore.SetOptions
                                .merge()
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
                        com.google.firebase.firestore.SetOptions
                                .merge()
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
