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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(30, 30, 30, 30);

        TextView title = new TextView(this);
        title.setText("🎤 پیام صوتی");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        recordButton = new Button(this);
        recordButton.setText("🎙️ شروع ضبط");

        stopButton = new Button(this);
        stopButton.setText("⏹️ توقف و ارسال");
        stopButton.setEnabled(false);

        statusText = new TextView(this);
        statusText.setText("آماده ضبط صدا");
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
                        "اجازه میکروفون داده شد",
                        Toast.LENGTH_SHORT
                ).show();

                startRecording();

            } else {

                statusText.setText(
                        "اجازه میکروفون داده نشد"
                );

                Toast.makeText(
                        this,
                        "اجازه استفاده از میکروفون داده نشد",
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
                    "اجازه میکروفون وجود ندارد",
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
                    "🔴 در حال ضبط..."
            );

        } catch (Exception e) {

            statusText.setText(
                    "خطا در شروع ضبط"
            );

            Toast.makeText(
                    this,
                    "خطا: " + getErrorText(e),
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
                    "در حال ارسال..."
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
                    "خطا در توقف ضبط"
            );

            Toast.makeText(
                    this,
                    "خطا: " + getErrorText(e),
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

            return "خواندن پاسخ ناموفق بود: " +
                    getErrorText(e);

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
                            "فایل صوتی وجود ندارد"
                    );
                }

                if (audioFile.length() <= 0) {
                    throw new Exception(
                            "فایل صوتی خالی است"
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
                                "Supabase پاسخ متنی برنگرداند";
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
                                "✅ آپلود موفق شد\n" +
                                        "HTTP " +
                                        finalCode
                        );

                        Toast.makeText(
                                VoiceActivity.this,
                                "✅ صدا با موفقیت ذخیره شد",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        statusText.setText(
                                "❌ خطای Supabase\n" +
                                        "HTTP " +
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
                            "❌ خطای اتصال\n" +
                                    error
                    );

                    Toast.makeText(
                            VoiceActivity.this,
                            "خطا:\n" + error,
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
