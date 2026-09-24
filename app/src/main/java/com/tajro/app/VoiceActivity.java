package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class VoiceActivity extends Activity {

    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    private static final String BUCKET_NAME =
            "voice_messages";

    private static final String SUPABASE_KEY =
            "sb_publishable_a02sM3MABB4afGU90ZBdFA_OTYG6gUs";

    private static final int RECORD_AUDIO_REQUEST = 100;

    private MediaRecorder recorder;
    private String audioPath;

    private Button recordButton;
    private Button stopButton;
    private TextView statusText;

    private String appliedLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appliedLanguage =
                LanguageManager.getLanguage(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText(
                text(
                        "🎤 پیام صوتی",
                        "🎤 Voice Message",
                        "🎤 غږیز پیغام",
                        "🎤 صوتی پیغام",
                        "🎤 वॉइस संदेश"
                )
        );
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        recordButton = new Button(this);
        recordButton.setText(
                text(
                        "🎙️ شروع ضبط",
                        "🎙️ Start Recording",
                        "🎙️ ثبت پیل کړئ",
                        "🎙️ ریکارڈنگ شروع کریں",
                        "🎙️ रिकॉर्डिंग शुरू करें"
                )
        );

        stopButton = new Button(this);
        stopButton.setText(
                text(
                        "⏹️ توقف و ارسال",
                        "⏹️ Stop & Upload",
                        "⏹️ درول او اپلوډ",
                        "⏹️ روکیں اور اپ لوڈ کریں",
                        "⏹️ रोकें और अपलोड करें"
                )
        );
        stopButton.setEnabled(false);

        statusText = new TextView(this);
        statusText.setText(
                text(
                        "آماده ضبط صدا",
                        "Ready to record",
                        "د غږ ثبتولو لپاره چمتو",
                        "ریکارڈنگ کے لیے تیار",
                        "रिकॉर्डिंग के लिए तैयार"
                )
        );
        statusText.setTextSize(17);
        statusText.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(recordButton);
        layout.addView(stopButton);
        layout.addView(statusText);

        setContentView(layout);

        recordButton.setOnClickListener(v -> {

            if (checkSelfPermission(
                    Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        RECORD_AUDIO_REQUEST
                );

            } else {
                startRecording();
            }
        });

        stopButton.setOnClickListener(v -> stopRecording());
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {

            recreate();
        }
    }

    private String text(
            String fa,
            String en,
            String ps,
            String ur,
            String hi) {

        String language =
                LanguageManager.getLanguage(this);

        if ("en".equals(language)) {
            return en;
        }

        if ("ps".equals(language)) {
            return ps;
        }

        if ("ur".equals(language)) {
            return ur;
        }

        if ("hi".equals(language)) {
            return hi;
        }

        return fa;
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

        if (requestCode == RECORD_AUDIO_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(
                        this,
                        text(
                                "اجازه میکروفون داده شد",
                                "Microphone permission granted",
                                "د مایکروفون اجازه ورکړل شوه",
                                "مائیکروفون کی اجازت دے دی گئی",
                                "माइक्रोफ़ोन की अनुमति दी गई"
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                startRecording();

            } else {

                statusText.setText(
                        text(
                                "اجازه میکروفون داده نشد",
                                "Microphone permission was denied",
                                "د مایکروفون اجازه ورنه کړل شوه",
                                "مائیکروفون کی اجازت نہیں دی گئی",
                                "माइक्रोफ़ोन की अनुमति नहीं दी गई"
                        )
                );

                Toast.makeText(
                        this,
                        text(
                                "اجازه استفاده از میکروفون داده نشد",
                                "Microphone permission was not granted",
                                "د مایکروفون د کارولو اجازه ورنه کړل شوه",
                                "مائیکروفون استعمال کرنے کی اجازت نہیں دی گئی",
                                "माइक्रोफ़ोन इस्तेमाल करने की अनुमति नहीं दी गई"
                        ),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void startRecording() {

        if (checkSelfPermission(
                Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    text(
                            "اجازه میکروفون وجود ندارد",
                            "Microphone permission is not available",
                            "د مایکروفون اجازه نشته",
                            "مائیکروفون کی اجازت موجود نہیں ہے",
                            "माइक्रोफ़ोन की अनुमति उपलब्ध नहीं है"
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        try {

            File file = new File(
                    getCacheDir(),
                    "voice_" +
                            System.currentTimeMillis() +
                            ".3gp"
            );

            audioPath = file.getAbsolutePath();

            recorder = new MediaRecorder();

            recorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );

            recorder.setOutputFormat(
                    MediaRecorder.OutputFormat.THREE_GPP
            );

            recorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AMR_NB
            );

            recorder.setOutputFile(audioPath);

            recorder.prepare();
            recorder.start();

            recordButton.setEnabled(false);
            stopButton.setEnabled(true);

            statusText.setText(
                    text(
                            "🔴 در حال ضبط...",
                            "🔴 Recording...",
                            "🔴 ثبت روان دی...",
                            "🔴 ریکارڈنگ جاری ہے...",
                            "🔴 रिकॉर्डिंग हो रही है..."
                    )
            );

        } catch (Exception e) {

            statusText.setText(
                    text(
                            "خطا در شروع ضبط",
                            "Error starting recording",
                            "د ثبتولو په پیل کې تېروتنه",
                            "ریکارڈنگ شروع کرنے میں خرابی",
                            "रिकॉर्डिंग शुरू करने में त्रुटि"
                    )
            );

            Toast.makeText(
                    this,
                    text(
                            "خطا: ",
                            "Error: ",
                            "تېروتنه: ",
                            "خرابی: ",
                            "त्रुटि: "
                    ) + getErrorText(e),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void stopRecording() {

        if (recorder == null) {
            return;
        }

        try {

            recorder.stop();
            recorder.release();
            recorder = null;

            recordButton.setEnabled(true);
            stopButton.setEnabled(false);

            statusText.setText(
                    text(
                            "در حال ارسال...",
                            "Uploading...",
                            "اپلوډ روان دی...",
                            "اپ لوڈ ہو رہا ہے...",
                            "अपलोड हो रहा है..."
                    )
            );

            uploadToSupabase();

        } catch (Exception e) {

            if (recorder != null) {

                try {
                    recorder.release();
                } catch (Exception ignored) {
                }

                recorder = null;
            }

            recordButton.setEnabled(true);
            stopButton.setEnabled(false);

            statusText.setText(
                    text(
                            "خطا در توقف ضبط",
                            "Error stopping recording",
                            "د ثبتولو په درولو کې تېروتنه",
                            "ریکارڈنگ روکنے میں خرابی",
                            "रिकॉर्डिंग रोकने में त्रुटि"
                    )
            );

            Toast.makeText(
                    this,
                    text(
                            "خطا: ",
                            "Error: ",
                            "تېروتنه: ",
                            "خرابی: ",
                            "त्रुटि: "
                    ) + getErrorText(e),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private String encodePath(String path) {

        try {

            String[] parts = path.split("/");

            StringBuilder result =
                    new StringBuilder();

            for (int i = 0; i < parts.length; i++) {

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

    private String getErrorText(Exception e) {

        String message = e.getMessage();

        if (message == null ||
                message.trim().isEmpty()) {

            return e.getClass().getSimpleName();
        }

        return message;
    }

    private String readStream(
            InputStream stream
    ) {

        if (stream == null) {
            return "";
        }

        BufferedReader reader = null;

        try {

            reader = new BufferedReader(
                    new InputStreamReader(
                            stream,
                            "UTF-8"
                    )
            );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                if (result.length() > 0) {
                    result.append("\n");
                }

                result.append(line);
            }

            return result.toString();

        } catch (Exception e) {

            return text(
                    "خواندن پاسخ ناموفق بود: ",
                    "Failed to read response: ",
                    "د ځواب لوستل ناکام شول: ",
                    "جواب پڑھنے میں ناکامی: ",
                    "उत्तर पढ़ने में विफल: "
            ) + getErrorText(e);

        } finally {

            if (reader != null) {

                try {
                    reader.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void uploadToSupabase() {

        new Thread(() -> {

            HttpURLConnection connection = null;
            InputStream input = null;
            OutputStream output = null;

            File audioFile =
                    new File(audioPath);

            try {

                if (!audioFile.exists()) {
                    throw new Exception(
                            text(
                                    "فایل صوتی وجود ندارد",
                                    "Audio file does not exist",
                                    "غږیز فایل شتون نه لري",
                                    "آڈیو فائل موجود نہیں ہے",
                                    "ऑडियो फ़ाइल मौजूद नहीं है"
                            )
                    );
                }

                if (audioFile.length() <= 0) {
                    throw new Exception(
                            text(
                                    "فایل صوتی خالی است",
                                    "Audio file is empty",
                                    "غږیز فایل تش دی",
                                    "آڈیو فائل خالی ہے",
                                    "ऑडियो फ़ाइल खाली है"
                            )
                    );
                }

                String fileName =
                        "voice_" +
                                System.currentTimeMillis() +
                                ".3gp";

                String uploadUrl =
                        SUPABASE_URL +
                                "/storage/v1/object/" +
                                BUCKET_NAME +
                                "/" +
                                encodePath(fileName);

                URL url = new URL(uploadUrl);

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setDoOutput(true);
                connection.setDoInput(true);

                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_KEY
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "audio/3gpp"
                );

                connection.setRequestProperty(
                        "x-upsert",
                        "true"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                connection.setFixedLengthStreamingMode(
                        audioFile.length()
                );

                input =
                        new FileInputStream(audioFile);

                output =
                        connection.getOutputStream();

                byte[] buffer = new byte[8192];

                int count;

                while ((count =
                        input.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            count
                    );
                }

                output.flush();

                int responseCode =
                        connection.getResponseCode();

                String responseBody;

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    responseBody =
                            readStream(
                                    connection.getInputStream()
                            );

                } else {

                    responseBody =
                            readStream(
                                    connection.getErrorStream()
                            );

                    if (responseBody == null ||
                            responseBody.trim().isEmpty()) {

                        responseBody =
                                text(
                                        "Supabase پاسخ متنی برنگرداند",
                                        "Supabase returned no text response",
                                        "Supabase متني ځواب ورنه کړ",
                                        "Supabase نے کوئی متنی جواب نہیں دیا",
                                        "Supabase ने कोई टेक्स्ट प्रतिक्रिया नहीं दी"
                                );
                    }
                }

                final String finalBody =
                        responseBody;

                final int finalCode =
                        responseCode;

                runOnUiThread(() -> {

                    if (finalCode >= 200 &&
                            finalCode < 300) {

                        statusText.setText(
                                text(
                                        "✅ آپلود موفق شد\nHTTP ",
                                        "✅ Upload successful\nHTTP ",
                                        "✅ اپلوډ بریالی شو\nHTTP ",
                                        "✅ اپ لوڈ کامیاب ہوا\nHTTP ",
                                        "✅ अपलोड सफल हुआ\nHTTP "
                                ) +
                                        finalCode
                        );

                        Toast.makeText(
                                VoiceActivity.this,
                                text(
                                        "✅ صدا با موفقیت ذخیره شد",
                                        "✅ Voice saved successfully",
                                        "✅ غږ په بریالیتوب سره خوندي شو",
                                        "✅ آواز کامیابی سے محفوظ ہو گئی",
                                        "✅ वॉइस सफलतापूर्वक सहेजी गई"
                                ),
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        statusText.setText(
                                text(
                                        "❌ خطای Supabase\nHTTP ",
                                        "❌ Supabase error\nHTTP ",
                                        "❌ د Supabase تېروتنه\nHTTP ",
                                        "❌ Supabase کی خرابی\nHTTP ",
                                        "❌ Supabase त्रुटि\nHTTP "
                                ) +
                                        finalCode +
                                        "\n" +
                                        finalBody
                        );

                        Toast.makeText(
                                VoiceActivity.this,
                                "HTTP " +
                                        finalCode +
                                        "\n" +
                                        finalBody,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

                if (responseCode >= 200 &&
                        responseCode < 300) {

                    audioFile.delete();
                }

            } catch (Exception e) {

                final String error =
                        getErrorText(e);

                runOnUiThread(() -> {

                    statusText.setText(
                            text(
                                    "❌ خطای اتصال\n",
                                    "❌ Connection error\n",
                                    "❌ د پیوستون تېروتنه\n",
                                    "❌ کنکشن کی خرابی\n",
                                    "❌ कनेक्शन त्रुटि\n"
                            ) +
                                    error
                    );

                    Toast.makeText(
                            VoiceActivity.this,
                            text(
                                    "خطا:\n",
                                    "Error:\n",
                                    "تېروتنه:\n",
                                    "خرابی:\n",
                                    "त्रुटि:\n"
                            ) + error,
                            Toast.LENGTH_LONG
                    ).show();
                });

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

        }).start();
    }

    @Override
    protected void onDestroy() {

        if (recorder != null) {

            try {
                recorder.release();
            } catch (Exception ignored) {
            }

            recorder = null;
        }

        super.onDestroy();
    }
    }
