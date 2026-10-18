
package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIActivity extends Activity {

    private LinearLayout messagesLayout;
    private EditText questionInput;
    private ScrollView scrollView;
    private Button sendButton;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private boolean isSending = false;

    private final int primary = Color.rgb(12, 91, 120);
    private final int darkBlue = Color.rgb(5, 43, 70);
    private final int background = Color.rgb(235, 248, 250);
    private final int textColor = Color.rgb(28, 48, 62);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(darkBlue);
        getWindow().setNavigationBarColor(background);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(background);

        // سربرگ حرفه‌ای
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(13), dp(16), dp(13));
        header.setBackground(createBackground(
                new int[]{darkBlue, primary}, 0, 0
        ));

        TextView backButton = new TextView(this);
        backButton.setText("‹");
        backButton.setTextSize(38);
        backButton.setTextColor(Color.WHITE);
        backButton.setGravity(Gravity.CENTER);
        header.addView(backButton, new LinearLayout.LayoutParams(
                dp(40), dp(48)
        ));
        backButton.setOnClickListener(v -> finish());

        TextView robotIcon = new TextView(this);
        robotIcon.setText("✦");
        robotIcon.setTextSize(30);
        robotIcon.setTextColor(Color.WHITE);
        robotIcon.setGravity(Gravity.CENTER);
        robotIcon.setBackground(createBackground(
                new int[]{Color.rgb(22, 151, 194),
                        Color.rgb(22, 74, 151)},
                Color.rgb(80, 220, 255), 50
        ));

        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(52), dp(52));
        iconParams.setMargins(dp(8), 0, dp(12), 0);
        header.addView(robotIcon, iconParams);

        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(this);
        title.setText("دستیار هوشمند");
        title.setTextSize(21);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView subtitle = new TextView(this);
        subtitle.setText("همراه دانای شما");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(160, 238, 237));

        titleBox.addView(title);
        titleBox.addView(subtitle);

        header.addView(titleBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));

        TextView menuIcon = new TextView(this);
        menuIcon.setText("⋮");
        menuIcon.setTextSize(28);
        menuIcon.setTextColor(Color.WHITE);
        menuIcon.setGravity(Gravity.CENTER);
        header.addView(menuIcon, new LinearLayout.LayoutParams(
                dp(32), dp(48)
        ));

        main.addView(header);

        // معرفی کوتاه
        TextView welcome = new TextView(this);
        welcome.setText(
                "✨  بپرس، یاد بگیر و تجربه کن\n" +
                "سؤالت را بنویس؛ من برای پاسخ‌گویی آماده‌ام."
        );
        welcome.setTextSize(14);
        welcome.setTextColor(primary);
        welcome.setGravity(Gravity.CENTER);
        welcome.setPadding(dp(12), dp(12), dp(12), dp(12));

        main.addView(welcome);

        // بخش گفت‌وگو
        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);

        messagesLayout = new LinearLayout(this);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        messagesLayout.setPadding(dp(12), dp(8), dp(12), dp(12));

        scrollView.addView(messagesLayout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

        main.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        // نوار نوشتن پیام
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(10), dp(9), dp(10), dp(10));
        bottom.setBackgroundColor(Color.WHITE);

        questionInput = new EditText(this);
        questionInput.setHint("پیام خود را بنویسید...");
        questionInput.setTextSize(16);
        questionInput.setTextColor(textColor);
        questionInput.setHintTextColor(Color.GRAY);
        questionInput.setPadding(dp(15), dp(10), dp(15), dp(10));
        questionInput.setMinHeight(dp(50));
        questionInput.setMaxLines(4);
        questionInput.setSingleLine(false);
        questionInput.setImeOptions(EditorInfo.IME_ACTION_SEND);
        questionInput.setBackground(createBackground(
                new int[]{Color.rgb(248, 252, 255),
                        Color.rgb(248, 252, 255)},
                Color.rgb(205, 231, 239), 24
        ));

        bottom.addView(questionInput, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));

        sendButton = new Button(this);
        sendButton.setText("➤");
        sendButton.setTextSize(23);
        sendButton.setTextColor(Color.WHITE);
        sendButton.setAllCaps(false);
        sendButton.setMinWidth(0);
        sendButton.setMinimumWidth(0);
        sendButton.setPadding(0, 0, 0, 0);
        sendButton.setBackground(createBackground(
                new int[]{primary, darkBlue}, 0, 50
        ));

        LinearLayout.LayoutParams sendParams =
                new LinearLayout.LayoutParams(dp(54), dp(54));
        sendParams.setMargins(dp(8), 0, 0, 0);
        bottom.addView(sendButton, sendParams);

        main.addView(bottom);

        setContentView(main);

        addMessage(false,
                "سلام! 🌟 من دستیار هوشمند تجربه‌ها هستم.\n" +
                "دربارهٔ آموزش، تکنولوژی، زندگی روزمره و موضوعات " +
                "گوناگون سؤال بپرس. تلاش می‌کنم پاسخ روشن و مفیدی بدهم.",
                false);

        sendButton.setOnClickListener(v -> sendQuestion());

        questionInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendQuestion();
                return true;
            }
            return false;
        });
    }

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable createBackground(
            int[] colors, int strokeColor, int radius) {

        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, colors
        );
        drawable.setCornerRadius(dp(radius));

        if (strokeColor != 0) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private void sendQuestion() {
        if (isSending) {
            return;
        }

        String question = questionInput.getText()
                .toString().trim();

        if (question.isEmpty()) {
            Toast.makeText(this, "لطفاً سؤال خود را بنویسید",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        isSending = true;
        sendButton.setEnabled(false);
        sendButton.setAlpha(0.6f);

        addMessage(true, question, false);
        questionInput.setText("");

        ((InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(
                        questionInput.getWindowToken(), 0);

        final TextView loading = addMessage(
                false, "در حال فکر کردن... ✨", false);

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                String apiKey = BuildConfig.OPENROUTER_API_KEY;

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new Exception(
                            "کلید OpenRouter در برنامه تنظیم نشده است."
                    );
                }

                URL url = new URL(
                        "https://openrouter.ai/api/v1/chat/completions"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Authorization", "Bearer " + apiKey);
                connection.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8");

                JSONObject body = new JSONObject();
                body.put("model", "openrouter/free");

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();
                systemMessage.put("role", "system");
                systemMessage.put("content",
                        "تو دستیار هوشمند اپلیکیشن «تجربه‌ها» هستی. " +
                        "به زبان فارسی/دری، روشن، محترمانه و مفید پاسخ بده. " +
                        "خود را به موضوع تجربه‌های ثبت‌شده محدود نکن؛ " +
                        "تا جایی که دانش و توانایی مدل اجازه می‌دهد، " +
                        "به پرسش‌های عمومی، علمی، آموزشی، فناوری، " +
                        "تاریخی و روزمره پاسخ بده. " +
                        "اگر اطلاعات کافی نداری، صادقانه بگو. " +
                        "اطلاعات ساختگی را به عنوان حقیقت ارائه نکن. " +
                        "برای پرسش‌های پزشکی پاسخ عمومی و محتاطانه بده " +
                        "و در وضعیت‌های جدی مراجعه به پزشک را توصیه کن. " +
                        "پاسخ‌ها را منظم و متناسب با سؤال بنویس."
                );
                messages.put(systemMessage);

                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", question);
                messages.put(userMessage);

                body.put("messages", messages);

                byte[] data = body.toString()
                        .getBytes(StandardCharsets.UTF_8);

                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                    output.flush();
                }

                int responseCode = connection.getResponseCode();

                InputStream inputStream = responseCode >= 200
                        && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (inputStream != null) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    inputStream, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                    }
                }

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception("OpenRouter HTTP " +
                            responseCode + "\n" + response);
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray choices = json.getJSONArray("choices");

                if (choices.length() == 0) {
                    throw new Exception("پاسخی از مدل دریافت نشد.");
                }

                JSONObject message = choices.getJSONObject(0)
                        .getJSONObject("message");

                String answer = message.optString("content", "").trim();

                if (answer.isEmpty()) {
                    throw new Exception("پاسخ مدل خالی بود.");
                }

                String finalAnswer = answer;

                runOnUiThread(() -> {
                    removeMessage(loading);
                    addMessage(false, finalAnswer, true);
                    finishSending();
                });

            } catch (Exception e) {
                String error = e.getMessage();
                if (error == null || error.isEmpty()) {
                    error = "خطای نامشخص: " + e;
                }

                String finalError = error;

                runOnUiThread(() -> {
                    removeMessage(loading);
                    addMessage(false,
                            "متأسفم، پاسخ دریافت نشد.\n" + finalError,
                            false);
                    finishSending();
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private void finishSending() {
        isSending = false;
        sendButton.setEnabled(true);
        sendButton.setAlpha(1f);
    }

    private TextView addMessage(
            boolean isUser, String message, boolean allowCopy) {

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setGravity(isUser ? Gravity.END : Gravity.START);

        LinearLayout.LayoutParams wrapperParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        wrapperParams.setMargins(0, dp(5), 0, dp(7));
        messagesLayout.addView(wrapper, wrapperParams);

        LinearLayout bubble = new LinearLayout(this);
        bubble.setOrientation(LinearLayout.VERTICAL);
        bubble.setPadding(dp(14), dp(12), dp(14), dp(10));
        bubble.setBackground(createBackground(
                isUser
                        ? new int[]{Color.rgb(202, 239, 250),
                                    Color.rgb(220, 246, 251)}
                        : new int[]{Color.WHITE, Color.rgb(248, 253, 255)},
                isUser ? Color.rgb(166, 225, 241)
                        : Color.rgb(215, 234, 241),
                20
        ));

        LinearLayout.LayoutParams bubbleParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        bubbleParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        bubbleParams.setMargins(
                isUser ? dp(24) : 0,
                0,
                isUser ? 0 : dp(24),
                0);

        wrapper.addView(bubble, bubbleParams);

        TextView sender = new TextView(this);
        sender.setText(isUser ? "شما" : "✦ دستیار هوشمند");
        sender.setTextSize(13);
        sender.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sender.setTextColor(isUser ? primary : Color.rgb(10, 115, 153));
        sender.setGravity(Gravity.RIGHT);
        bubble.addView(sender);

        TextView text = new TextView(this);
        text.setText(message);
        text.setTextSize(16);
        text.setTextColor(textColor);
        text.setGravity(Gravity.RIGHT);
        text.setTextIsSelectable(true);
        text.setLineSpacing(dp(3), 1.0f);
        text.setPadding(0, dp(8), 0, dp(4));
        bubble.addView(text);

        if (allowCopy) {
            Button copyButton = new Button(this);
            copyButton.setText("▣  کپی پاسخ");
            copyButton.setTextSize(12);
            copyButton.setTextColor(primary);
            copyButton.setAllCaps(false);
            copyButton.setMinHeight(0);
            copyButton.setMinimumHeight(0);
            copyButton.setPadding(dp(10), dp(2), dp(10), dp(2));
            copyButton.setBackground(createBackground(
                    new int[]{Color.WHITE, Color.WHITE},
                    Color.rgb(205, 231, 239), 18
            ));

            LinearLayout.LayoutParams copyParams =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            dp(38));
            copyParams.gravity = Gravity.START;
            copyParams.topMargin = dp(6);
            bubble.addView(copyButton, copyParams);

            copyButton.setOnClickListener(v -> {
                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(
                                CLIPBOARD_SERVICE);

                clipboard.setPrimaryClip(
                        ClipData.newPlainText("پاسخ دستیار", message));

                Toast.makeText(this, "پاسخ کپی شد",
                        Toast.LENGTH_SHORT).show();
            });
        }

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));

        return text;
    }

    private void removeMessage(TextView messageView) {
        if (messageView == null) {
            return;
        }

        ViewParentHelper.remove(messageView);
    }

    private static class ViewParentHelper {
        static void remove(View view) {
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
