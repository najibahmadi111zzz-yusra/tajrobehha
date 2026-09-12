package com.tajro.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VoiceActivity extends Activity {

    // آدرس پروژه Supabase
    private static final String SUPABASE_URL =
            "https://gorbhuqmkjlkrklhasdh.supabase.co";

    // نام Bucket
    private static final String BUCKET_NAME =
            "voice_messages";

    // Publishable Key خودت را فقط اینجا قرار بده
    private static final String SUPABASE_KEY =
        sb_publishable_xCkd5NsZ3jcQ6IqKKzm_Lg_UtF_ToDD

    private MediaRecorder recorder;
    private String audioPath;

    private Button recordButton;
    private Button stopButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("🎤 پیام صوتی");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        recordButton = new Button(this);
        recordButton.setText("🎙️ شروع ضبط");

        stopButton = new Button(this);
        stopButton.setText("⏹️ توقف و ارسال");
        stopButton.setEnabled(false);

        statusText = new TextView(this);
        statusText.setText("آماده ضبط صدا");
        statusText.setTextSize(18);
        statusText.setGravity(Gravity.CENTER);

        layout.addView(title);
        layout.addView(recordButton);
        layout.addView(stopButton);
        layout.addView(statusText);

        setContentView(layout);

        recordButton.setOnClickListener(v -> startRecording());

        stopButton.setOnClickListener(v -> stopRecording());
    }

    private void startRecording() {

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    100
            );
            return;
        }

        try {

            File file = new File(
                    getCacheDir(),
                    "voice_" + System.currentTimeMillis() + ".m4a"
            );

            audioPath = file.getAbsolutePath();

            recorder = new MediaRecorder();

            recorder.setAudioSource(
                    MediaRecorder.AudioSource.MIC
            );

            recorder.setOutputFormat(
                    MediaRecorder.OutputFormat.MPEG_4
            );

            recorder.setAudioEncoder(
                    MediaRecorder.AudioEncoder.AAC
            );

            recorder.setOutputFile(audioPath);

            recorder.prepare();
            recorder.start();

            recordButton.setEnabled(false);
            stopButton.setEnabled(true);

            statusText.setText("🔴 در حال ضبط...");

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "خطا در شروع ضبط: " + e.getMessage(),
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

            statusText.setText("⏳ در حال ارسال...");

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

            Toast.makeText(
                    this,
                    "خطا در توقف ضبط: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void uploadToSupabase() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                File file = new File(audioPath);

                if (!file.exists()) {
                    throw new Exception("فایل صوتی پیدا نشد");
                }

                String fileName =
                        "voice_" +
                        System.currentTimeMillis() +
                        ".m4a";

                String uploadUrl =
                        SUPABASE_URL +
                        "/storage/v1/object/" +
                        BUCKET_NAME +
                        "/" +
                        fileName;

                URL url = new URL(uploadUrl);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(30000);

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + SUPABASE_KEY
                );

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_KEY
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "audio/mp4"
                );

                connection.setRequestProperty(
                        "x-upsert",
                        "true"
                );

                FileInputStream input =
                        new FileInputStream(file);

                OutputStream output =
                        connection.getOutputStream();

                byte[] buffer = new byte[8192];

                int length;

                while ((length = input.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            length
                    );
                }

                output.flush();

                input.close();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                runOnUiThread(() -> {

                    if (responseCode >= 200 &&
                            responseCode < 300) {

                        statusText.setText(
                                "✅ پیام صوتی ذخیره شد"
                        );

                        Toast.makeText(
                                VoiceActivity.this,
                                "صدا با موفقیت ذخیره شد",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        statusText.setText(
                                "❌ ارسال ناموفق بود"
                        );

                        Toast.makeText(
                                VoiceActivity.this,
                                "خطای Supabase: " + responseCode,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });

                file.delete();

            } catch (Exception e) {

                runOnUiThread(() -> {

                    statusText.setText(
                            "❌ خطا در ارسال"
                    );

                    Toast.makeText(
                            VoiceActivity.this,
                            "خطا: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });

            } finally {

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
