package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class ChatActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText messageInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(20, 25, 20, 20);
        main.setBackgroundColor(Color.rgb(235, 248, 250));

        TextView title = new TextView(this);
        title.setText("💬 چت تجربه‌ها");
        title.setTextSize(27);
        title.setTextColor(Color.rgb(8, 65, 90));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        main.addView(title);

        ScrollView scrollView = new ScrollView(this);

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
                        65,
                        1
                )
        );

        Button sendButton = new Button(this);
        sendButton.setText("📤 ارسال");

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        110,
                        65
                )
        );

        main.addView(bottom);

        setContentView(main);

        sendButton.setOnClickListener(v -> {

            String message =
                    messageInput.getText().toString().trim();

            if (!message.isEmpty()) {

                TextView messageView = new TextView(this);

                messageView.setText(
                        "👤 شما:\n" + message
                );

                messageView.setTextSize(17);
                messageView.setTextColor(Color.DKGRAY);
                messageView.setPadding(
                        15, 12, 15, 12
                );

                messagesLayout.addView(messageView);

                messageInput.setText("");

                scrollView.post(() ->
                        scrollView.fullScroll(
                                android.view.View.FOCUS_DOWN
                        )
                );
            }
        });
    }
}
