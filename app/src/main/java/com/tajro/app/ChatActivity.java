package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText messageInput;
    private ScrollView scrollView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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
        title.setPadding(0, 0, 0, dp(15));

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

        // قسمت نوشتن پیام
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

        // فعال بودن نوشتن
        messageInput.setEnabled(true);
        messageInput.setFocusable(true);
        messageInput.setFocusableInTouchMode(true);
        messageInput.setClickable(true);

        // اجازه نوشتن متن معمولی
        messageInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        // راست‌چین برای فارسی
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
        sendButton.setText("📤 ارسال");
        sendButton.setTextSize(15);

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        dp(105),
                        dp(55)
                )
        );

        main.addView(bottom);

        setContentView(main);

        // وقتی روی کادر می‌زند، کیبورد باز شود
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
    }

    private void sendMessage() {

        String message = messageInput
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
                "senderId",
                auth.getCurrentUser().getUid()
        );

        data.put(
                "timestamp",
                com.google.firebase.firestore.FieldValue
                        .serverTimestamp()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(
                        documentReference -> {

                            messageInput.setText("");

                            scrollView.post(() ->
                                    scrollView.fullScroll(
                                            View.FOCUS_DOWN
                                    )
                            );
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

                                String message =
                                        document.getString(
                                                "message"
                                        );

                                String senderId =
                                        document.getString(
                                                "senderId"
                                        );

                                if (message == null) {
                                    continue;
                                }

                                TextView messageView =
                                        new TextView(this);

                                if (myId.equals(senderId)) {
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

                            scrollView.post(() ->
                                    scrollView.fullScroll(
                                            View.FOCUS_DOWN
                                    )
                            );
                        }
                );
    }
                        }
