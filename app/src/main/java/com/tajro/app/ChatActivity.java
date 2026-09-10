package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private LinearLayout messagesLayout;
    private ScrollView scrollView;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(15, 20, 15, 15);
        main.setBackgroundColor(Color.rgb(235, 248, 250));

        TextView title = new TextView(this);
        title.setText("💬 چت تجربه‌ها");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(8, 65, 90));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        main.addView(title);

        scrollView = new ScrollView(this);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setPadding(10, 10, 10, 10);

        scrollView.addView(
                messagesLayout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        main.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);

        messageInput = new EditText(this);
        messageInput.setHint("پیام خود را بنویسید...");
        messageInput.setTextSize(16);

        bottom.addView(
                messageInput,
                new LinearLayout.LayoutParams(
                        0,
                        60,
                        1
                )
        );

        Button sendButton = new Button(this);
        sendButton.setText("📤 ارسال");

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        100,
                        60
                )
        );

        main.addView(bottom);

        sendButton.setOnClickListener(v -> sendMessage());

        setContentView(main);

// loadMessages();
    private void sendMessage() {

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(
                    this,
                    "لطفاً ابتدا وارد اکانت خود شوید",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String message =
                messageInput.getText().toString().trim();

        if (message.isEmpty()) {
            Toast.makeText(
                    this,
                    "لطفاً پیام خود را بنویسید",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("text", message);
        data.put("senderId", user.getUid());
        data.put("receiverId", "test");
        data.put(
                "timestamp",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("messages")
                .add(data)
                .addOnSuccessListener(documentReference -> {

                    messageInput.setText("");

                    Toast.makeText(
                            this,
                            "پیام ارسال شد ✅",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "خطا در ارسال پیام: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void loadMessages() {

        db.collection("messages")
                .orderBy(
                        "timestamp",
                        Query.Direction.ASCENDING
                )
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(
                                this,
                                "خطا در دریافت پیام‌ها",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    messagesLayout.removeAllViews();

                    if (snapshots == null) {
                        return;
                    }

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String text =
                                document.getString("text");

                        String senderId =
                                document.getString("senderId");

                        if (text == null) {
                            continue;
                        }

                        TextView messageView =
                                new TextView(this);

                        if (auth.getCurrentUser() != null &&
                                auth.getCurrentUser()
                                        .getUid()
                                        .equals(senderId)) {

                            messageView.setText(
                                    "👤 شما:\n" + text
                            );

                        } else {

                            messageView.setText(
                                    "👤 کاربر:\n" + text
                            );
                        }

                        messageView.setTextSize(17);
                        messageView.setPadding(
                                15, 12, 15, 12
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
                });
    }
}
