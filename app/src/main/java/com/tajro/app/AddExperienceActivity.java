package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
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

import java.util.HashMap;
import java.util.Map;

public class AddExperienceActivity extends Activity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private int themeColor;

    private boolean isPublishing = false;

    // ================================
    // اعمال زبان
    // ================================

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    // ================================
    // ترجمه متن‌ها
    // ================================

    private String text(String fa) {

        String lang =
                LanguageManager.getLanguage(this);

        if ("en".equals(lang)) {

            if (fa.equals("✍️ ثبت تجربه جدید"))
                return "✍️ Add New Experience";

            if (fa.equals("عنوان تجربه را بنویسید"))
                return "Write the experience title";

            if (fa.equals("تجربه خود را با دیگران شریک کنید..."))
                return "Share your experience with others...";

            if (fa.equals("🚀 انتشار تجربه"))
                return "🚀 Publish Experience";

            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید"))
                return "Please log in to your account first";

            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید"))
                return "Please enter the experience title and text";

            if (fa.equals("عنوان تجربه خیلی طولانی است"))
                return "The experience title is too long";

            if (fa.equals("متن تجربه خیلی طولانی است"))
                return "The experience text is too long";

            if (fa.equals("⏳ در حال بررسی..."))
                return "⏳ Checking...";

            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️"))
                return "This experience has already been published ❤️";

            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉"))
                return "Experience published successfully! 🎉";

            if (fa.equals("خطا در بررسی تجربه: "))
                return "Error checking experience: ";

            if (fa.equals("خطا در انتشار تجربه: "))
                return "Error publishing experience: ";
        }

        if ("ps".equals(lang)) {

            if (fa.equals("✍️ ثبت تجربه جدید"))
                return "✍️ نوې تجربه ثبت کړئ";

            if (fa.equals("عنوان تجربه را بنویسید"))
                return "د تجربې سرلیک ولیکئ";

            if (fa.equals("تجربه خود را با دیگران شریک کنید..."))
                return "خپله تجربه له نورو سره شریکه کړئ...";

            if (fa.equals("🚀 انتشار تجربه"))
                return "🚀 تجربه خپره کړئ";

            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید"))
                return "مهرباني وکړئ لومړی خپل حساب ته ننوځئ";

            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید"))
                return "مهرباني وکړئ د تجربې سرلیک او متن ولیکئ";

            if (fa.equals("عنوان تجربه خیلی طولانی است"))
                return "د تجربې سرلیک ډېر اوږد دی";

            if (fa.equals("متن تجربه خیلی طولانی است"))
                return "د تجربې متن ډېر اوږد دی";

            if (fa.equals("⏳ در حال بررسی..."))
                return "⏳ د کتلو په حال کې...";

            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️"))
                return "دا تجربه مخکې خپره شوې ده ❤️";

            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉"))
                return "تجربه په بریالیتوب سره خپره شوه! 🎉";

            if (fa.equals("خطا در بررسی تجربه: "))
                return "د تجربې په کتلو کې تېروتنه: ";

            if (fa.equals("خطا در انتشار تجربه: "))
                return "د تجربې په خپرولو کې تېروتنه: ";
        }

        if ("ur".equals(lang)) {

            if (fa.equals("✍️ ثبت تجربه جدید"))
                return "✍️ نیا تجربہ درج کریں";

            if (fa.equals("عنوان تجربه را بنویسید"))
                return "تجربے کا عنوان لکھیں";

            if (fa.equals("تجربه خود را با دیگران شریک کنید..."))
                return "اپنا تجربہ دوسروں کے ساتھ شیئر کریں...";

            if (fa.equals("🚀 انتشار تجربه"))
                return "🚀 تجربہ شائع کریں";

            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید"))
                return "براہِ کرم پہلے اپنے اکاؤنٹ میں لاگ اِن کریں";

            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید"))
                return "براہِ کرم تجربے کا عنوان اور متن درج کریں";

            if (fa.equals("عنوان تجربه خیلی طولانی است"))
                return "تجربے کا عنوان بہت لمبا ہے";

            if (fa.equals("متن تجربه خیلی طولانی است"))
                return "تجربے کا متن بہت لمبا ہے";

            if (fa.equals("⏳ در حال بررسی..."))
                return "⏳ جانچ جاری ہے...";

            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️"))
                return "یہ تجربہ پہلے ہی شائع ہو چکا ہے ❤️";

            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉"))
                return "تجربہ کامیابی سے شائع ہو گیا! 🎉";

            if (fa.equals("خطا در بررسی تجربه: "))
                return "تجربہ چیک کرنے میں خرابی: ";

            if (fa.equals("خطا در انتشار تجربه: "))
                return "تجربہ شائع کرنے میں خرابی: ";
        }

        if ("hi".equals(lang)) {

            if (fa.equals("✍️ ثبت تجربه جدید"))
                return "✍️ नया अनुभव जोड़ें";

            if (fa.equals("عنوان تجربه را بنویسید"))
                return "अनुभव का शीर्षक लिखें";

            if (fa.equals("تجربه خود را با دیگران شریک کنید..."))
                return "अपना अनुभव दूसरों के साथ साझा करें...";

            if (fa.equals("🚀 انتشار تجربه"))
                return "🚀 अनुभव प्रकाशित करें";

            if (fa.equals("لطفاً ابتدا وارد اکانت خود شوید"))
                return "कृपया पहले अपने खाते में लॉग इन करें";

            if (fa.equals("لطفاً عنوان و متن تجربه را وارد کنید"))
                return "कृपया अनुभव का शीर्षक और पाठ दर्ज करें";

            if (fa.equals("عنوان تجربه خیلی طولانی است"))
                return "अनुभव का शीर्षक बहुत लंबा है";

            if (fa.equals("متن تجربه خیلی طولانی است"))
                return "अनुभव का पाठ बहुत लंबा है";

            if (fa.equals("⏳ در حال بررسی..."))
                return "⏳ जाँच हो रही है...";

            if (fa.equals("این تجربه قبلاً منتشر شده است ❤️"))
                return "यह अनुभव पहले ही प्रकाशित हो चुका है ❤️";

            if (fa.equals("تجربه با موفقیت منتشر شد! 🎉"))
                return "अनुभव सफलतापूर्वक प्रकाशित हुआ! 🎉";

            if (fa.equals("خطا در بررسی تجربه: "))
                return "अनुभव जाँचने में त्रुटि: ";

            if (fa.equals("خطا در انتشار تجربه: "))
                return "अनुभव प्रकाशित करने में त्रुटि: ";
        }

        return fa;
    }

    // ================================
    // شروع صفحه
    // ================================

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
        title.setText(
                text("✍️ ثبت تجربه جدید")
        );
        title.setTextSize(25);
        title.setTextColor(themeColor);
        title.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(
                0,
                0,
                0,
                dp(25)
        );

        EditText experienceTitle = new EditText(this);
        experienceTitle.setHint(
                text("عنوان تجربه را بنویسید")
        );
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
                text("تجربه خود را با دیگران شریک کنید...")
        );
        experienceText.setTextSize(17);
        experienceText.setGravity(
                Gravity.TOP | Gravity.RIGHT
        );
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

        experienceText.setLayoutParams(
                textParams
        );

        Button publishButton = new Button(this);

        publishButton.setText(
                text("🚀 انتشار تجربه")
        );

        publishButton.setTextSize(16);
        styleButton(publishButton);

        publishButton.setOnClickListener(v -> {

            if (isPublishing) {
                return;
            }

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user == null) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        text(
                                "لطفاً ابتدا وارد اکانت خود شوید"
                        ),
                        Toast.LENGTH_LONG
                ).show();

                return;
            }

            String titleText =
                    experienceTitle
                            .getText()
                            .toString()
                            .trim();

            String experience =
                    experienceText
                            .getText()
                            .toString()
                            .trim();

            if (titleText.isEmpty()
                    || experience.isEmpty()) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        text(
                                "لطفاً عنوان و متن تجربه را وارد کنید"
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (titleText.length() > 150) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        text(
                                "عنوان تجربه خیلی طولانی است"
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (experience.length() > 10000) {

                Toast.makeText(
                        AddExperienceActivity.this,
                        text(
                                "متن تجربه خیلی طولانی است"
                        ),
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            isPublishing = true;

            publishButton.setEnabled(false);
            publishButton.setAlpha(0.6f);

            publishButton.setText(
                    text("⏳ در حال بررسی...")
            );

            String contentKey =
                    createContentKey(
                            user.getUid(),
                            titleText,
                            experience
                    );

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

                                    publishButton
                                            .setEnabled(true);

                                    publishButton
                                            .setAlpha(1.0f);

                                    publishButton.setText(
                                            text(
                                                    "🚀 انتشار تجربه"
                                            )
                                    );

                                    Toast.makeText(
                                            AddExperienceActivity.this,
                                            text(
                                                    "این تجربه قبلاً منتشر شده است ❤️"
                                            ),
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

                                publishButton
                                        .setEnabled(true);

                                publishButton
                                        .setAlpha(1.0f);

                                publishButton.setText(
                                        text(
                                                "🚀 انتشار تجربه"
                                        )
                                );

                                Toast.makeText(
                                        AddExperienceActivity.this,
                                        text(
                                                "خطا در بررسی تجربه: "
                                        ) + e.getMessage(),
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

        experienceTitle.setLayoutParams(
                titleParams
        );

        layout.addView(experienceTitle);
        layout.addView(experienceText);
        layout.addView(publishButton);

        setContentView(layout);
    }

    // ================================
    // انتشار تجربه
    // ================================

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

        experienceData.put(
                "contentKey",
                contentKey
        );

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
                                    text(
                                            "تجربه با موفقیت منتشر شد! 🎉"
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();

                            titleField.setText("");
                            textField.setText("");

                            isPublishing = false;

                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);

                            publishButton.setText(
                                    text(
                                            "🚀 انتشار تجربه"
                                    )
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            isPublishing = false;

                            publishButton.setEnabled(true);
                            publishButton.setAlpha(1.0f);

                            publishButton.setText(
                                    text(
                                            "🚀 انتشار تجربه"
                                    )
                            );

                            Toast.makeText(
                                    AddExperienceActivity.this,
                                    text(
                                            "خطا در انتشار تجربه: "
                                    ) + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    // ================================
    // ساخت شناسه ثابت
    // ================================

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
                .toLowerCase(
                        java.util.Locale.ROOT
                );
    }

    // ================================
    // اندازه
    // ================================

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ================================
    // استایل دکمه
    // ================================

    private void styleButton(Button button) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);
        background.setCornerRadius(
                dp(22)
        );

        button.setBackground(background);

        button.setTextColor(Color.WHITE);

        button.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD
                )
        );
    }

    // ================================
    // رنگ پس‌زمینه
    // ================================

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
