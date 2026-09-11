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

import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.GenerateContentResponse;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AIActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText questionInput;
    private ScrollView scrollView;

    private final Executor executor =
            Executors.newSingleThreadExecutor();

    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // اتصال به Firebase AI
        GenerativeModel ai =
                FirebaseAI.getInstance(
                        GenerativeBackend.googleAI()
                ).generativeModel("gemini-3.7-flash");

        model = GenerativeModelFutures.from(ai);

        // صفحه اصلی
        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(20, 25, 20, 15);
        main.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        // عنوان
        TextView title = new TextView(this);
        title.setText("🤖 دستیار هوشمند");
        title.setTextSize(27);
        title.setTextColor(
                Color.rgb(8, 65, 90)
        );
        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);

        main.addView(title);

        // قسمت پیام‌ها
        scrollView = new ScrollView(this);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(
                LinearLayout.VERTICAL
        );
        messagesLayout.setPadding(
                10, 10, 10, 10
        );

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

        // قسمت پایین برای نوشتن سؤال
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(
                LinearLayout.HORIZONTAL
        );
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, 5, 0, 5);

        // کادر سؤال
        questionInput = new EditText(this);

        questionInput.setHint(
                "سؤال خود را بنویسید..."
        );

        questionInput.setTextSize(16);

        questionInput.setSingleLine(false);

        questionInput.setMinHeight(60);

        questionInput.setPadding(
                15, 5, 15, 5
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                );

        bottom.addView(
                questionInput,
                inputParams
        );

        // دکمه ارسال
        Button sendButton = new Button(this);
        sendButton.setText("📤 ارسال");
        sendButton.setTextSize(15);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        bottom.addView(
                sendButton,
                buttonParams
        );

        main.addView(bottom);

        // نمایش صفحه
        setContentView(main);

        // پیام خوش‌آمدگویی
        addMessage(
                "🤖 دستیار:",
                "سلام! من دستیار هوشمند تجربه‌ها هستم.\n" +
                "سؤال خود را بنویسید."
        );

        // دکمه ارسال
        sendButton.setOnClickListener(v ->
                sendQuestion()
        );
    }

    private void sendQuestion() {

        String question =
                questionInput.getText()
                        .toString()
                        .trim();

        if (question.isEmpty()) {

            Toast.makeText(
                    this,
                    "لطفاً سؤال خود را بنویسید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // نمایش سؤال کاربر
        addMessage(
                "👤 شما:",
                question
        );

        // پاک کردن کادر
        questionInput.setText("");

        // نمایش وضعیت
        addMessage(
                "🤖 دستیار:",
                "در حال فکر کردن..."
        );

        // متن ارسال‌شده به هوش مصنوعی
        Content prompt = new Content.Builder()
                .addText(
                        "تو دستیار هوشمند اپلیکیشن «تجربه‌ها» هستی. " +
                        "به زبان فارسی/دری، محترمانه و کوتاه پاسخ بده. " +
                        "اگر سؤال پزشکی بود، پاسخ عمومی بده و در موارد جدی " +
                        "کاربر را به پزشک یا مرکز صحی راهنمایی کن.\n\n" +
                        "سؤال کاربر:\n" +
                        question
                )
                .build();

        // ارسال به Gemini
        ListenableFuture<GenerateContentResponse> response =
                model.generateContent(prompt);

        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {

                    @Override
                    public void onSuccess(
                            GenerateContentResponse result) {

                        String answer =
                                result.getText();

                        runOnUiThread(() -> {

                            removeLastMessage();

                            if (answer == null ||
                                    answer.trim().isEmpty()) {

                                addMessage(
                                        "⚠️ دستیار:",
                                        "پاسخ خالی دریافت شد."
                                );

                            } else {

                                addMessage(
                                        "🤖 دستیار:",
                                        answer
                                );
                            }
                        });
                    }

                    @Override
                    public void onFailure(
                            Throwable t) {

                        runOnUiThread(() -> {

                            removeLastMessage();

                            addMessage(
                                    "⚠️ خطا:",
                                    "پاسخ دریافت نشد.\n\n" +
                                    "لطفاً اینترنت و تنظیمات Firebase AI را بررسی کنید."
                            );
                        });
                    }

                },
                executor
        );
    }

    private void addMessage(
            String sender,
            String message) {

        TextView text = new TextView(this);

        text.setText(
                sender + "\n" + message
        );

        text.setTextSize(17);

        text.setTextColor(
                Color.DKGRAY
        );

        text.setPadding(
                15, 12, 15, 12
        );

        messagesLayout.addView(text);

        scrollView.post(() ->
                scrollView.fullScroll(
                        View.FOCUS_DOWN
                )
        );
    }

    private void removeLastMessage() {

        int count =
                messagesLayout.getChildCount();

        if (count > 0) {

            messagesLayout.removeViewAt(
                    count - 1
            );
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (executor instanceof java.util.concurrent.ExecutorService) {

            ((java.util.concurrent.ExecutorService) executor)
                    .shutdown();
        }
    }
}
