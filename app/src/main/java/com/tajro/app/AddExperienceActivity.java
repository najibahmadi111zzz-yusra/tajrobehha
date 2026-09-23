package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AddExperienceActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int themeColor;

    /*
     * جلوگیری از ثبت چندباره یک تجربه
     */
    private boolean isPublishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        themeColor = ThemeManager.getThemeColor(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);
        layout.setBackgroundColor(getLightThemeColor());

        TextView title = new TextView(this);
        title.setText("✍️ ثبت تجربه جدید");
        title.setTextSize(26);
        title.setTextColor(themeColor);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 35);

        EditText experienceTitle = new EditText(this);
        experienceTitle.setHint("عنوان تجربه را بنویسید");

        EditText experienceText = new EditText(this);
        experienceText.setHint("تجربه خود را با دیگران شریک کنید...");
        experienceText.setGravity(Gravity.TOP);
        experienceText.setMinLines(6);

        Button publishButton = new Button(this);
        publishButton.setText("🚀 انتشار تجربه");
        styleButton(publishButton);

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (isPublishing) {
                    return;
                }

                FirebaseUser user = auth.getCurrentUser();

                if (user == null) {
                    Toast.makeText(
                            AddExperienceActivity.this,
                            "لطفاً ابتدا وارد اکانت خود شوید",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }

                String titleText =
                        experienceTitle.getText().toString().trim();

                String experience =
                        experienceText.getText().toString().trim();

                if (titleText.isEmpty() || experience.isEmpty()) {
                    Toast.makeText(
                            AddExperienceActivity.this,
                            "لطفاً عنوان و متن تجربه را وارد کنید",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                isPublishing = true;
                publishButton.setEnabled(false);
                publishButton.setAlpha(0.6f);
                publishButton.setText("⏳ در حال انتشار...");

                Map<String, Object> experienceData =
                        new HashMap<>();

                experienceData.put("title", titleText);
                experienceData.put("text", experience);
                experienceData.put("userId", user.getUid());
                experienceData.put("authorEmail", user.getEmail());
                experienceData.put(
                        "timestamp",
                        FieldValue.serverTimestamp()
                );

                db.collection("experiences")
                        .add(experienceData)
                        .addOnSuccessListener(documentReference -> {

                            /*
                             * تست تشخیصی:
                             * اطلاعات واقعی Firebase که همین APK
                             * در حال استفاده از آن است.
                             */
                            String projectId = "نامشخص";
                            String appId = "نامشخص";
                            String documentId =
                                    documentReference.getId();

                            try {
                                FirebaseApp firebaseApp =
                                        FirebaseApp.getInstance();

                                if (firebaseApp.getOptions().getProjectId()
                                        != null) {

                                    projectId =
                                            firebaseApp.getOptions()
                                                    .getProjectId();
                                }

                                if (firebaseApp.getOptions()
                                        .getApplicationId() != null) {

                                    appId =
                                            firebaseApp.getOptions()
                                                    .getApplicationId();
                                }

                            } catch (Exception testError) {

                                projectId =
                                        "خطا در خواندن Project ID";

                                appId =
                                        "خطا در خواندن App ID";
                            }

                            /*
                             * پیام اصلی موفقیت
                             */
                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "تجربه با موفقیت منتشر شد! 🎉",
                                    Toast.LENGTH_LONG
                            ).show();

                            /*
                             * نمایش اطلاعات تشخیصی
                             */
                            String diagnosticMessage =
                                    "🔎 اطلاعات واقعی برنامه\n\n"
                                    + "Firebase Project ID:\n"
                                    + projectId
                                    + "\n\n"
                                    + "Firebase App ID:\n"
                                    + appId
                                    + "\n\n"
                                    + "Document ID ساخته‌شده:\n"
                                    + documentId
                                    + "\n\n"
                                    + "Collection:\n"
                                    + "experiences"
                                    + "\n\n"
                                    + "✅ نوشتن در Firestore موفق بود.";

                            new AlertDialog.Builder(
                                    AddExperienceActivity.this
                            )
                                    .setTitle("تست Firebase")
                                    .setMessage(diagnosticMessage)
                                    .setPositiveButton(
                                            "باشه",
                                            null
                                    )
                                    .show();

                            experienceTitle.setText("");
                            experienceText.setText("");

                            isPublishing = false;
                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);
                            publishButton.setText("🚀 انتشار تجربه");

                        })
                        .addOnFailureListener(e -> {

                            isPublishing = false;
                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);
                            publishButton.setText("🚀 انتشار تجربه");

                            String errorMessage =
                                    e.getMessage();

                            if (errorMessage == null) {
                                errorMessage =
                                        "خطای نامشخص";
                            }

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "خطا در انتشار تجربه:\n"
                                            + errorMessage,
                                    Toast.LENGTH_LONG
                            ).show();

                            /*
                             * اگر نوشتن واقعاً شکست بخورد،
                             * خطای کامل را نیز نمایش می‌دهیم.
                             */
                            new AlertDialog.Builder(
                                    AddExperienceActivity.this
                            )
                                    .setTitle("❌ خطای Firestore")
                                    .setMessage(
                                            "نوشتن تجربه در Firebase "
                                            + "موفق نشد.\n\n"
                                            + errorMessage
                                    )
                                    .setPositiveButton(
                                            "باشه",
                                            null
                                    )
                                    .show();
                        });
            }
        });

        layout.addView(title);
        layout.addView(experienceTitle);
        layout.addView(experienceText);
        layout.addView(publishButton);

        setContentView(layout);
    }

    private void styleButton(Button button) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);
        background.setCornerRadius(24);

        button.setBackground(background);
        button.setTextColor(Color.WHITE);
    }

    private int getLightThemeColor() {

        int red = Color.red(themeColor);
        int green = Color.green(themeColor);
        int blue = Color.blue(themeColor);

        red = red + (255 - red) * 92 / 100;
        green = green + (255 - green) * 92 / 100;
        blue = blue + (255 - blue) * 92 / 100;

        return Color.rgb(red, green, blue);
    }
                    }
