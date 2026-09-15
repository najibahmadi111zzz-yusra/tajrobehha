package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends Activity {

    private SharedPreferences preferences;

    private LinearLayout mainLayout;

    private int selectedColor =
            Color.rgb(12, 91, 120);

    private boolean nightMode = false;

    private int dp(int value) {

        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ==================================
    // ساخت دکمه تنظیمات
    // ==================================

    private Button createSettingButton(
            String text
    ) {

        Button button =
                new Button(this);

        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);

        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        button.setGravity(
                Gravity.CENTER
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                selectedColor
        );

        background.setCornerRadius(
                dp(18)
        );

        button.setBackground(
                background
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(62)
                );

        params.setMargins(
                0,
                dp(7),
                0,
                dp(7)
        );

        button.setLayoutParams(params);

        return button;
    }

    // ==================================
    // شروع
    // ==================================

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(savedInstanceState);

        preferences =
                getSharedPreferences(
                        "tajrobehha_settings",
                        MODE_PRIVATE
                );

        selectedColor =
                preferences.getInt(
                        "theme_color",
                        Color.rgb(12, 91, 120)
                );

        nightMode =
                preferences.getBoolean(
                        "night_mode",
                        false
                );

        buildSettings();
    }

    // ==================================
    // ساخت صفحه تنظیمات
    // ==================================

    private void buildSettings() {

        ScrollView scrollView =
                new ScrollView(this);

        mainLayout =
                new LinearLayout(this);

        mainLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.setPadding(
                dp(20),
                dp(25),
                dp(20),
                dp(30)
        );

        updateBackground();

        scrollView.addView(
                mainLayout
        );

        // ==================================
        // عنوان
        // ==================================

        TextView title =
                new TextView(this);

        title.setText(
                "⚙️ تنظیمات تجربه‌ها"
        );

        title.setTextSize(28);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                0,
                0,
                dp(20)
        );

        updateTextColor(title);

        mainLayout.addView(title);

        // ==================================
        // طرح و رنگ
        // ==================================

        Button colorButton =
                createSettingButton(
                        "🎨 طرح و رنگ برنامه"
                );

        mainLayout.addView(
                colorButton
        );

        colorButton.setOnClickListener(
                v -> showColorDialog()
        );

        // ==================================
        // حالت شب
        // ==================================

        Switch nightSwitch =
                new Switch(this);

        nightSwitch.setText(
                "🌙 حالت شب"
        );

        nightSwitch.setTextSize(18);

        nightSwitch.setChecked(
                nightMode
        );

        nightSwitch.setPadding(
                dp(5),
                dp(12),
                dp(5),
                dp(12)
        );

        updateTextColor(nightSwitch);

        mainLayout.addView(
                nightSwitch
        );

        nightSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    nightMode =
                            isChecked;

                    preferences.edit()
                            .putBoolean(
                                    "night_mode",
                                    isChecked
                            )
                            .apply();

                    Toast.makeText(
                            this,
                            isChecked
                                    ? "🌙 حالت شب فعال شد"
                                    : "☀️ حالت روز فعال شد",
                            Toast.LENGTH_SHORT
                    ).show();

                    buildSettings();
                }
        );

        // ==================================
        // زبان
        // ==================================

        Button languageButton =
                createSettingButton(
                        "🌐 زبان برنامه"
                );

        mainLayout.addView(
                languageButton
        );

        languageButton.setOnClickListener(
                v -> showLanguageDialog()
        );

        // ==================================
        // اکانت من
        // ==================================

        Button accountButton =
                createSettingButton(
                        "👤 اکانت من"
                );

        mainLayout.addView(
                accountButton
        );

        accountButton.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    SettingsActivity.this,
                                    AccountActivity.class
                            );

                    startActivity(intent);
                }
        );

        // ==================================
        // حریم خصوصی
        // ==================================

        Button privacyButton =
                createSettingButton(
                        "🔒 حریم خصوصی"
                );

        mainLayout.addView(
                privacyButton
        );

        privacyButton.setOnClickListener(
                v -> showPrivacyDialog()
        );

        // ==================================
        // خروج از حساب
        // ==================================

        Button logoutButton =
                createSettingButton(
                        "🚪 خروج از حساب"
                );

        mainLayout.addView(
                logoutButton
        );

        logoutButton.setOnClickListener(
                v -> showLogoutDialog()
        );

        // ==================================
        // درباره برنامه
        // ==================================

        Button aboutButton =
                createSettingButton(
                        "ℹ️ درباره برنامه"
                );

        mainLayout.addView(
                aboutButton
        );

        aboutButton.setOnClickListener(
                v -> showAboutDialog()
        );

        // ==================================
        // نسخه
        // ==================================

        TextView versionText =
                new TextView(this);

        String version = "1.0";

        try {

            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            version =
                    info.versionName;

        } catch (Exception ignored) {
        }

        versionText.setText(
                "\n📱 تجربه‌ها\nنسخه "
                        + version
        );

        versionText.setTextSize(15);

        versionText.setGravity(
                Gravity.CENTER
        );

        versionText.setPadding(
                0,
                dp(20),
                0,
                dp(10)
        );

        updateTextColor(versionText);

        mainLayout.addView(
                versionText
        );

        // ==================================
        // برگشت
        // ==================================

        Button backButton =
                createSettingButton(
                        "↩️ برگشت"
                );

        mainLayout.addView(
                backButton
        );

        backButton.setOnClickListener(
                v -> finish()
        );

        setContentView(scrollView);
    }

    // ==================================
    // پس‌زمینه
    // ==================================

    private void updateBackground() {

        mainLayout.setBackgroundColor(
                ThemeManager.getBackgroundColor(
                        this
                )
        );
    }

    // ==================================
    // رنگ متن
    // ==================================

    private void updateTextColor(
            TextView textView
    ) {

        textView.setTextColor(
                ThemeManager.getTextColor(
                        this
                )
        );
    }

    // ==================================
    // انتخاب رنگ
    // ==================================

    private void showColorDialog() {

        String[] colors = {
                "آبی اصلی",
                "سبز آرام",
                "بنفش",
                "نارنجی",
                "سرمه‌ای"
        };

        int[] colorValues = {
                Color.rgb(12, 91, 120),
                Color.rgb(30, 120, 85),
                Color.rgb(105, 70, 150),
                Color.rgb(190, 105, 30),
                Color.rgb(25, 55, 90)
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        "🎨 انتخاب رنگ"
                )
                .setItems(
                        colors,
                        (dialog, which) -> {

                            selectedColor =
                                    colorValues[which];

                            preferences.edit()
                                    .putInt(
                                            "theme_color",
                                            selectedColor
                                    )
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "✅ رنگ برنامه تغییر کرد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            buildSettings();
                        }
                )
                .show();
    }

    // ==================================
    // انتخاب زبان
    // ==================================

    private void showLanguageDialog() {

        String[] languages = {
                "🇦🇫 دری",
                "🇬🇧 English",
                "🇦🇫 پښتو",
                "🇵🇰 اردو",
                "🇮🇳 हिन्दी"
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        "🌐 زبان برنامه"
                )
                .setItems(
                        languages,
                        (dialog, which) -> {

                            String language;

                            if (which == 0) {

                                language = "دری";

                            } else if (which == 1) {

                                language = "English";

                            } else if (which == 2) {

                                language = "پښتو";

                            } else if (which == 3) {

                                language = "اردو";

                            } else {

                                language = "हिन्दी";
                            }

                            preferences.edit()
                                    .putString(
                                            "language",
                                            language
                                    )
                                    .apply();

                            Toast.makeText(
                                    this,
                                    "زبان انتخاب شد: "
                                            + language,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }

    // ==================================
    // حریم خصوصی
    // ==================================

    private void showPrivacyDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔒 حریم خصوصی"
                )
                .setMessage(
                        "تجربه‌ها برای محافظت از " +
                        "اطلاعات کاربران تلاش می‌کند.\n\n" +
                        "اطلاعات حساب و داده‌های برنامه " +
                        "باید مطابق قوانین و تنظیمات " +
                        "سرویس‌های مورد استفاده محافظت شوند.\n\n" +
                        "هیچ رمز عبور یا کلید محرمانه‌ای " +
                        "نباید در اختیار دیگران قرار گیرد."
                )
                .setPositiveButton(
                        "متوجه شدم",
                        null
                )
                .show();
    }

    // ==================================
    // خروج از حساب
    // ==================================

    private void showLogoutDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "🚪 خروج از حساب"
                )
                .setMessage(
                        "آیا مطمئن هستید که می‌خواهید " +
                        "از حساب خود خارج شوید؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "خروج",
                        (dialog, which) -> {

                            FirebaseAuth
                                    .getInstance()
                                    .signOut();

                            Toast.makeText(
                                    this,
                                    "از حساب خارج شدید",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .show();
    }

    // ==================================
    // درباره برنامه
    // ==================================

    private void showAboutDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "ℹ️ درباره برنامه"
                )
                .setMessage(
                        "تجربه‌ها\n\n" +
                        "محلی برای ثبت، یادگیری و " +
                        "شریک‌کردن تجربه‌ها.\n\n" +
                        "یاد بگیر • تجربه کن • شریک کن\n\n" +
                        "نسخه برنامه: "
                                + getVersionName()
                )
                .setPositiveButton(
                        "بستن",
                        null
                )
                .show();
    }

    // ==================================
    // نسخه برنامه
    // ==================================

    private String getVersionName() {

        try {

            PackageInfo info =
                    getPackageManager()
                            .getPackageInfo(
                                    getPackageName(),
                                    0
                            );

            return info.versionName;

        } catch (Exception e) {

            return "1.0";
        }
    }
}
