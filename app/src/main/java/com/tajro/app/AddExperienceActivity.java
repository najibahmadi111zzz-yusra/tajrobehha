package com.tajro.app;

import android.app.Activity;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class AddExperienceActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int themeColor;

    private boolean isPublishing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        themeColor = ThemeManager.getThemeColor(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                dp(24),
                dp(30),
                dp(24),
                dp(30)
        );
        layout.setBackgroundColor(getLightThemeColor());

        TextView title = new TextView(this);
        title.setText("✍️ ثبت تجربه جدید");
        title.setTextSize(25);
        title.setTextColor(themeColor);
        title.setTypeface(
                Typeface.create("sans-serif", Typeface.BOLD)
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, dp(25));

        EditText experienceTitle = new EditText(this);
        experienceTitle.setHint("عنوان تجربه را بنویسید");
        experienceTitle.setTextSize(17);
        experienceTitle.setSingleLine(true);
        experienceTitle.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
        );

        EditText experienceText = new EditText(this);
        experienceText.setHint(
                "تجربه خود را با دیگران شریک کنید..."
        );
        experienceText.setTextSize(17);
        experienceText.setGravity(Gravity.TOP | Gravity.RIGHT);
        experienceText.setMinLines(7);
        experienceText.setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
        );

        LinearLayout.LayoutParams textParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(190)
                );

        textParams.setMargins(
                0,
                dp(14),
                0,
                dp(20)
        );

        experienceText.setLayoutParams(textParams);

        Button publishButton = new Button(this);
        publishButton.setText("🚀 انتشار تجربه");
        publishButton.setTextSize(16);
        styleButton(publishButton);

        publishButton.setOnClickListener(v -> {

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
                    experienceTitle.getText()
                            .toString()
                            .trim();

            String experience =
                    experienceText.getText()
                            .toString()
                            .trim();

            if (titleText.isEmpty() ||
                    experience.isEmpty()) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        "لطفاً عنوان و متن تجربه را وارد کنید",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (titleText.length() > 150) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        "عنوان تجربه خیلی طولانی است",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (experience.length() > 10000) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        "متن تجربه خیلی طولانی است",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            isPublishing = true;

            publishButton.setEnabled(false);
            publishButton.setAlpha(0.6f);
            publishButton.setText("⏳ در حال بررسی...");

            /*
             * یک کلید ثابت برای تشخیص تجربه تکراری
             */
            String contentKey =
                    createContentKey(
                            user.getUid(),
                            titleText,
                            experience
                    );

            /*
             * اول بررسی می‌کنیم همین تجربه قبلاً
             * توسط همین کاربر ثبت نشده باشد.
             */
            db.collection("experiences")
                    .whereEqualTo(
                            "contentKey",
                            contentKey
                    )
                    .limit(1)
                    .get()
                    .addOnSuccessListener(
                            querySnapshot -> {

                                if (!querySnapshot.isEmpty()) {

                                    isPublishing = false;

                                    publishButton.setEnabled(true);
                                    publishButton.setAlpha(1.0f);
                                    publishButton.setText(
                                            "🚀 انتشار تجربه"
                                    );

                                    Toast.makeText(
                                            AddExperienceActivity.this,
                                            "این تجربه قبلاً منتشر شده است ❤️",
                                            Toast.LENGTH_LONG
                                    ).show();

                                    return;
                                }

                                publishExperience(
                                        user,
                                        titleText,
                                        experience,
                                        contentKey,
                                        publishButton,
                                        experienceTitle,
                                        experienceText
                                );
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                isPublishing = false;

                                publishButton.setEnabled(true);
                                publishButton.setAlpha(1.0f);
                                publishButton.setText(
                                        "🚀 انتشار تجربه"
                                );

                                Toast.makeText(
                                        AddExperienceActivity.this,
                                        "خطا در بررسی تجربه: " +
                                                e.getMessage(),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                    );
        });

        layout.addView(title);

        LinearLayout.LayoutParams titleParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        experienceTitle.setLayoutParams(titleParams);

        layout.addView(experienceTitle);
        layout.addView(experienceText);
        layout.addView(publishButton);

        setContentView(layout);
    }

    private void publishExperience(
            FirebaseUser user,
            String titleText,
            String experience,
            String contentKey,
            Button publishButton,
            EditText titleField,
            EditText textField
    ) {

        Map<String, Object> experienceData =
                new HashMap<>();

        experienceData.put(
                "title",
                titleText
        );

        experienceData.put(
                "text",
                experience
        );

        experienceData.put(
                "userId",
                user.getUid()
        );

        experienceData.put(
                "authorEmail",
                user.getEmail()
        );

        /*
         * برای جلوگیری از تجربه‌های تکراری
         */
        experienceData.put(
                "contentKey",
                contentKey
        );

        /*
         * تعداد اولیه لایک
         */
        experienceData.put(
                "likesCount",
                0L
        );

        experienceData.put(
                "timestamp",
                FieldValue.serverTimestamp()
        );

        db.collection("experiences")
                .add(experienceData)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "تجربه با موفقیت منتشر شد! 🎉",
                                    Toast.LENGTH_LONG
                            ).show();

                            titleField.setText("");
                            textField.setText("");

                            isPublishing = false;

                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);
                            publishButton.setText(
                                    "🚀 انتشار تجربه"
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            isPublishing = false;

                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);
                            publishButton.setText(
                                    "🚀 انتشار تجربه"
                            );

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    "خطا در انتشار تجربه: " +
                                            e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    /*
     * ساخت شناسه ثابت برای عنوان + متن + کاربر
     */
    private String createContentKey(
            String userId,
            String title,
            String text
    ) {

        String value =
                userId +
                        "|" +
                        normalizeText(title) +
                        "|" +
                        normalizeText(text);

        return Integer.toHexString(
                value.hashCode()
        );
    }

    private String normalizeText(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(java.util.Locale.ROOT);
    }

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    private void styleButton(Button button) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);
        background.setCornerRadius(dp(22));

        button.setBackground(background);
        button.setTextColor(Color.WHITE);
        button.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );
    }

    private int getLightThemeColor() {

        int red = Color.red(themeColor);
        int green = Color.green(themeColor);
        int blue = Color.blue(themeColor);

        red =
                red +
                        (255 - red) * 92 / 100;

        green =
                green +
                        (255 - green) * 92 / 100;

        blue =
                blue +
                        (255 - blue) * 92 / 100;

        return Color.rgb(
                red,
                green,
                blue
        );
    }
    }
