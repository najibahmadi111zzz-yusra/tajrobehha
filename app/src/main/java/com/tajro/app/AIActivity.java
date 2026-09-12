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

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(20, 25, 20, 20);
        main.setBackgroundColor(Color.rgb(235, 248, 250));

        TextView title = new TextView(this);
        title.setText("🤖 دستیار هوشمند");
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

        questionInput = new EditText(this);
        questionInput.setHint("سؤال خود را بنویسید...");
        questionInput.setTextSize(16);
        questionInput.setSingleLine(false);
        questionInput.setMinHeight(60);
        questionInput.setPadding(15, 5, 15, 5);

        bottom.addView(
                questionInput,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        Button sendButton = new Button(this);
        sendButton.setText("📤 ارسال");

        bottom.addView(
                sendButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        main.addView(bottom);

        setContentView(main);

        addMessage(
                "🤖 دستیار:",
                "سلام! من دستیار هوشمند تجربه‌ها هستم. سؤال خود را بنویسید."
        );

        sendButton.setOnClickListener(v -> sendQuestion());
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

        addMessage("👤 شما:", question);
        questionInput.setText("");
        addMessage("🤖 دستیار:", "در حال بررسی...");

        executor.execute(() -> {

            try {

                String apiKey =
                        BuildConfig.OPENROUTER_API_KEY;

                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new Exception(
                            "کلید OpenRouter در برنامه تنظیم نشده است."
                    );
                }

                URL url = new URL(
                        "https://openrouter.ai/api/v1/chat/completions"
                );

                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + apiKey
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                JSONObject body = new JSONObject();

                body.put(
                        "model",
                        "openrouter/free"
                );

                JSONArray messages = new JSONArray();

                JSONObject systemMessage = new JSONObject();

                systemMessage.put(
                        "role",
                        "system"
                );

                systemMessage.put(
                        "content",
                        "تو دستیار هوشمند اپلیکیشن «تجربه‌ها» هستی. " +
                        "به زبان فارسی/دری، محترمانه و واضح پاسخ بده. " +
                        "اگر سؤال پزشکی بود، پاسخ عمومی بده و در موارد جدی " +
                        "کاربر را به پزشک یا مرکز صحی راهنمایی کن."
                );

                messages.put(systemMessage);

                JSONObject userMessage = new JSONObject();

                userMessage.put(
                        "role",
                        "user"
                );

                userMessage.put(
                        "content",
                        question
                );

                messages.put(userMessage);

                body.put(
                        "messages",
                        messages
                );

                byte[] data =
                        body.toString()
                                .getBytes(StandardCharsets.UTF_8);

                OutputStream outputStream =
                        connection.getOutputStream();

                outputStream.write(data);
                outputStream.flush();
                outputStream.close();

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    inputStream =
                            connection.getInputStream();

                } else {

                    inputStream =
                            connection.getErrorStream();
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        inputStream,
                                        StandardCharsets.UTF_8
                                )
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                connection.disconnect();

                if (responseCode < 200 ||
                        responseCode >= 300) {

                    throw new Exception(
                            "OpenRouter HTTP " +
                            responseCode +
                            "\n" +
                            response.toString()
                    );
                }

                JSONObject json =
                        new JSONObject(
                                response.toString()
                        );

                JSONArray choices =
                        json.getJSONArray("choices");

                JSONObject firstChoice =
                        choices.getJSONObject(0);

                JSONObject message =
                        firstChoice.getJSONObject("message");

                String answer =
                        message.getString("content");

                runOnUiThread(() -> {

                    removeLastMessage();

                    addMessage(
                            "🤖 دستیار:",
                            answer
                    );
                });

            } catch (Exception e) {

                String error =
                        e.getMessage();

                if (error == null ||
                        error.isEmpty()) {

                    error = e.toString();
                }

                String finalError = error;

                runOnUiThread(() -> {

                    removeLastMessage();

                    addMessage(
                            "⚠️ خطای OpenRouter:",
                            finalError
                    );
                });
            }
        });
    }

    private void addMessage(
            String sender,
            String message) {

        runOnUiThread(() -> {

            TextView text =
                    new TextView(this);

            text.setText(
                    sender + "\n" + message
            );

            text.setTextSize(17);
            text.setTextColor(Color.DKGRAY);
            text.setPadding(
                    15, 12, 15, 12
            );

            messagesLayout.addView(text);

            scrollView.post(() ->
                    scrollView.fullScroll(
                            View.FOCUS_DOWN
                    )
            );
        });
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

        executor.shutdown();
    }
}
