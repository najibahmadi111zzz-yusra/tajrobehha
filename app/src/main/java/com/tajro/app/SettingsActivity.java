package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
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

    private String appliedLanguage;

    // ==================================
    // اعمال زبان
    // ==================================

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    // ==================================
    // متن چندزبانه
    // ==================================

    private String text(
            String fa,
            String en,
            String ps,
            String ur,
            String hi
    ) {

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

    // ==================================
    // اندازه
    // ==================================

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

        appliedLanguage =
                LanguageManager.getLanguage(this);

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
    // بررسی تغییر زبان
    // ==================================

    @Override
    protected void onResume() {

        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null &&
                !currentLanguage.equals(appliedLanguage)) {

            recreate();
            return;
        }
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
                text(
                        "⚙️ تنظیمات تجربه‌ها",
                        "⚙️ Experiences Settings",
                        "⚙️ د تجربو ترتیبات",
                        "⚙️ تجربات کی ترتیبات",
                        "⚙️ अनुभव सेटिंग्स"
                )
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
                        text(
                                "🎨 طرح و رنگ برنامه",
                                "🎨 App Theme & Color",
                                "🎨 د پروګرام بڼه او رنګ",
                                "🎨 ایپ کی تھیم اور رنگ",
                                "🎨 ऐप थीम और रंग"
                        )
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
                text(
                        "🌙 حالت شب",
                        "🌙 Night Mode",
                        "🌙 د شپې حالت",
                        "🌙 نائٹ موڈ",
                        "🌙 नाइट मोड"
                )
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
                                    ? text(
                                        "🌙 حالت شب فعال شد",
                                        "🌙 Night mode enabled",
                                        "🌙 د شپې حالت فعال شو",
                                        "🌙 نائٹ موڈ فعال ہوگیا",
                                        "🌙 नाइट मोड चालू हो गया"
                                    )
                                    : text(
                                        "☀️ حالت روز فعال شد",
                                        "☀️ Day mode enabled",
                                        "☀️ د ورځې حالت فعال شو",
                                        "☀️ ڈے موڈ فعال ہوگیا",
                                        "☀️ डे मोड चालू हो गया"
                                    ),
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
                        text(
                                "🌐 زبان برنامه",
                                "🌐 App Language",
                                "🌐 د پروګرام ژبه",
                                "🌐 ایپ کی زبان",
                                "🌐 ऐप की भाषा"
                        )
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
                        text(
                                "👤 اکانت من",
                                "👤 My Account",
                                "👤 زما اکاونټ",
                                "👤 میرا اکاؤنٹ",
                                "👤 मेरा अकाउंट"
                        )
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
                        text(
                                "🔒 حریم خصوصی",
                                "🔒 Privacy",
                                "🔒 محرمیت",
                                "🔒 رازداری",
                                "🔒 गोपनीयता"
                        )
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
                        text(
                                "🚪 خروج از حساب",
                                "🚪 Log Out",
                                "🚪 له اکاونټ څخه وتل",
                                "🚪 اکاؤنٹ سے لاگ آؤٹ",
                                "🚪 लॉग आउट"
                        )
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
                        text(
                                "ℹ️ درباره برنامه",
                                "ℹ️ About App",
                                "ℹ️ د پروګرام په اړه",
                                "ℹ️ ایپ کے بارے میں",
                                "ℹ️ ऐप के बारे में"
                        )
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
                "\n📱 " +
                text(
                        "تجربه‌ها",
                        "Experiences",
                        "تجربې",
                        "تجربات",
                        "अनुभव"
                ) +
                "\n" +
                text(
                        "نسخه ",
                        "Version ",
                        "نسخه ",
                        "ورژن ",
                        "संस्करण "
                ) +
                version
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
                        text(
                                "↩️ برگشت",
                                "↩️ Back",
                                "↩️ شاته",
                                "↩️ واپس",
                                "↩️ वापस"
                        )
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
                text(
                        "آبی اصلی",
                        "Main Blue",
                        "اصلي شین",
                        "مرکزی نیلا",
                        "मुख्य नीला"
                ),
                text(
                        "سبز آرام",
                        "Calm Green",
                        "ارام شین",
                        "پرسکون سبز",
                        "शांत हरा"
                ),
                text(
                        "بنفش",
                        "Purple",
                        "ارغواني",
                        "جامنی",
                        "बैंगनी"
                ),
                text(
                        "نارنجی",
                        "Orange",
                        "نارنجي",
                        "نارنجی",
                        "नारंगी"
                ),
                text(
                        "سرمه‌ای",
                        "Navy Blue",
                        "تیاره شین",
                        "گہرا نیلا",
                        "गहरा नीला"
                )
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
                        text(
                                "🎨 انتخاب رنگ",
                                "🎨 Choose Color",
                                "🎨 رنګ وټاکئ",
                                "🎨 رنگ منتخب کریں",
                                "🎨 रंग चुनें"
                        )
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
                                    text(
                                            "✅ رنگ برنامه تغییر کرد",
                                            "✅ App color changed",
                                            "✅ د پروګرام رنګ بدل شو",
                                            "✅ ایپ کا رنگ تبدیل ہوگیا",
                                            "✅ ऐप का रंग बदल गया"
                                    ),
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
                        text(
                                "🌐 زبان برنامه",
                                "🌐 App Language",
                                "🌐 د پروګرام ژبه",
                                "🌐 ایپ کی زبان",
                                "🌐 ऐप की भाषा"
                        )
                )
                .setItems(
                        languages,
                        (dialog, which) -> {

                            String languageCode;

                            if (which == 0) {
                                languageCode = "fa";
                            } else if (which == 1) {
                                languageCode = "en";
                            } else if (which == 2) {
                                languageCode = "ps";
                            } else if (which == 3) {
                                languageCode = "ur";
                            } else {
                                languageCode = "hi";
                            }

                            LanguageManager.setLanguage(
                                    SettingsActivity.this,
                                    languageCode
                            );

                            Toast.makeText(
                                    SettingsActivity.this,
                                    text(
                                            "✅ زبان با موفقیت انتخاب شد",
                                            "✅ Language selected successfully",
                                            "✅ ژبه په بریالیتوب وټاکل شوه",
                                            "✅ زبان کامیابی سے منتخب ہوگئی",
                                            "✅ भाषा सफलतापूर्वक चुनी गई"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();

                            recreate();
                        }
                )
                .show();
    }

    // ==================================
    // حریم خصوصی
    // ==================================

    private void showPrivacyDialog() {

        String lockStatus =
                AppLockManager.hasPassword(this)
                        ? text(
                            "فعال",
                            "Enabled",
                            "فعال",
                            "فعال",
                            "सक्रिय"
                        )
                        : text(
                            "غیرفعال",
                            "Disabled",
                            "غیرفعال",
                            "غیر فعال",
                            "निष्क्रिय"
                        );

        String hideStatus =
                AppLockManager.isPersonalInfoHidden(this)
                        ? text(
                            "فعال",
                            "Enabled",
                            "فعال",
                            "فعال",
                            "सक्रिय"
                        )
                        : text(
                            "غیرفعال",
                            "Disabled",
                            "غیرفعال",
                            "غیر فعال",
                            "निष्क्रिय"
                        );

        String message =
                "🔐 " +
                text(
                        "قفل برنامه: ",
                        "App lock: ",
                        "د پروګرام قفل: ",
                        "ایپ لاک: ",
                        "ऐप लॉक: "
                ) +
                lockStatus +
                "\n👁️ " +
                text(
                        "مخفی‌کردن اطلاعات: ",
                        "Hide personal information: ",
                        "د شخصي معلوماتو پټول: ",
                        "ذاتی معلومات چھپانا: ",
                        "व्यक्तिगत जानकारी छिपाना: "
                ) +
                hideStatus +
                "\n\n" +
                text(
                        "از گزینه‌های زیر استفاده کنید.",
                        "Use the options below.",
                        "لاندې انتخابونه وکاروئ.",
                        "نیچے دیے گئے اختیارات استعمال کریں۔",
                        "नीचे दिए गए विकल्पों का उपयोग करें।"
                );

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔒 " +
                        text(
                                "حریم خصوصی",
                                "Privacy",
                                "محرمیت",
                                "رازداری",
                                "गोपनीयता"
                        )
                )
                .setMessage(message)
                .setPositiveButton(
                        "🔐 " +
                        text(
                                "قفل برنامه",
                                "App Lock",
                                "د پروګرام قفل",
                                "ایپ لاک",
                                "ऐप लॉक"
                        ),
                        (dialog, which) ->
                                showLockSettings()
                )
                .setNeutralButton(
                        "👁️ " +
                        text(
                                "اطلاعات شخصی",
                                "Personal Information",
                                "شخصي معلومات",
                                "ذاتی معلومات",
                                "व्यक्तिगत जानकारी"
                        ),
                        (dialog, which) ->
                                showPersonalInfoSettings()
                )
                .setNegativeButton(
                        text(
                                "بستن",
                                "Close",
                                "بندول",
                                "بند کریں",
                                "बंद करें"
                        ),
                        null
                )
                .show();
    }

    // ==================================
    // تنظیمات قفل
    // ==================================

    private void showLockSettings() {

        boolean hasPassword =
                AppLockManager.hasPassword(this);

        boolean lockEnabled =
                AppLockManager.isLockEnabled(this);

        if (!hasPassword) {

            new AlertDialog.Builder(this)
                    .setTitle(
                            "🔐 " +
                            text(
                                    "ساخت رمز قفل",
                                    "Create Lock Password",
                                    "د قفل رمز جوړول",
                                    "لاک پاس ورڈ بنائیں",
                                    "लॉक पासवर्ड बनाएं"
                            )
                    )
                    .setMessage(
                            text(
                                    "برای قفل برنامه یک رمز دقیقاً ۶ رقمی بسازید.",
                                    "Create an exactly 6-digit password for the app lock.",
                                    "د پروګرام د قفل لپاره دقیقاً ۶ عددي رمز جوړ کړئ.",
                                    "ایپ لاک کے لیے بالکل ۶ ہندسوں کا پاس ورڈ بنائیں۔",
                                    "ऐप लॉक के लिए ठीक 6 अंकों का पासवर्ड बनाएं।"
                            )
                    )
                    .setPositiveButton(
                            text(
                                    "ساخت رمز",
                                    "Create Password",
                                    "رمز جوړول",
                                    "پاس ورڈ بنائیں",
                                    "पासवर्ड बनाएं"
                            ),
                            (dialog, which) ->
                                    showCreatePasswordDialog()
                    )
                    .setNegativeButton(
                            text(
                                    "انصراف",
                                    "Cancel",
                                    "لغوه",
                                    "منسوخ",
                                    "रद्द करें"
                            ),
                            null
                    )
                    .show();

            return;
        }

        String[] options;

        if (lockEnabled) {

            options = new String[]{
                    "🔑 " +
                    text(
                            "تغییر رمز",
                            "Change Password",
                            "رمز بدلول",
                            "پاس ورڈ تبدیل کریں",
                            "पासवर्ड बदलें"
                    ),
                    "🔓 " +
                    text(
                            "غیرفعال کردن قفل",
                            "Disable Lock",
                            "قفل غیر فعالول",
                            "لاک غیر فعال کریں",
                            "लॉक बंद करें"
                    )
            };

        } else {

            options = new String[]{
                    "🔐 " +
                    text(
                            "فعال کردن قفل",
                            "Enable Lock",
                            "قفل فعالول",
                            "لاک فعال کریں",
                            "लॉक चालू करें"
                    ),
                    "🔑 " +
                    text(
                            "تغییر رمز",
                            "Change Password",
                            "رمز بدلول",
                            "پاس ورڈ تبدیل کریں",
                            "पासवर्ड बदलें"
                    )
            };
        }

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔐 " +
                        text(
                                "تنظیمات قفل",
                                "Lock Settings",
                                "د قفل ترتیبات",
                                "لاک کی ترتیبات",
                                "लॉक सेटिंग्स"
                        )
                )
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (lockEnabled) {

                                if (which == 0) {

                                    showChangePasswordDialog();

                                } else {

                                    disableLock();
                                }

                            } else {

                                if (which == 0) {

                                    enableLock();

                                } else {

                                    showChangePasswordDialog();
                                }
                            }
                        }
                )
                .show();
    }

    // ==================================
    // ساخت رمز
    // ==================================

    private void showCreatePasswordDialog() {

        LinearLayout layout =
                createPasswordLayout();

        EditText password =
                createPasswordInput(
                        text(
                                "رمز جدید: ۶ رقم",
                                "New password: 6 digits",
                                "نوی رمز: ۶ عددې",
                                "نیا پاس ورڈ: ۶ ہندسے",
                                "नया पासवर्ड: 6 अंक"
                        )
                );

        EditText confirm =
                createPasswordInput(
                        text(
                                "تکرار رمز: ۶ رقم",
                                "Confirm password: 6 digits",
                                "د رمز تکرار: ۶ عددې",
                                "پاس ورڈ دوبارہ: ۶ ہندسے",
                                "पासवर्ड दोबारा: 6 अंक"
                        )
                );

        layout.addView(password);
        layout.addView(confirm);

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔐 " +
                        text(
                                "ساخت رمز قفل",
                                "Create Lock Password",
                                "د قفل رمز جوړول",
                                "لاک پاس ورڈ بنائیں",
                                "लॉक पासवर्ड बनाएं"
                        )
                )
                .setView(layout)
                .setPositiveButton(
                        text(
                                "ذخیره",
                                "Save",
                                "ساتل",
                                "محفوظ کریں",
                                "सहेजें"
                        ),
                        (dialog, which) -> {

                            String p =
                                    password.getText()
                                            .toString();

                            String c =
                                    confirm.getText()
                                            .toString();

                            if (!AppLockManager.isValidPassword(p)) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز باید دقیقاً ۶ رقم باشد",
                                                "Password must be exactly 6 digits",
                                                "رمز باید دقیقاً ۶ عددې وي",
                                                "پاس ورڈ بالکل ۶ ہندسوں کا ہونا چاہیے",
                                                "पासवर्ड ठीक 6 अंकों का होना चाहिए"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!p.equals(c)) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "دو رمز یکسان نیستند",
                                                "The two passwords do not match",
                                                "دواړه رمزونه یو شان نه دي",
                                                "دونوں پاس ورڈ ایک جیسے نہیں ہیں",
                                                "दोनों पासवर्ड समान नहीं हैं"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (AppLockManager.setPassword(
                                    this,
                                    p
                            )) {

                                AppLockManager.setSessionUnlocked(
                                        this,
                                        true
                                );

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز ۶ رقمی ساخته شد ✅",
                                                "6-digit password created ✅",
                                                "۶ عددي رمز جوړ شو ✅",
                                                "۶ ہندسوں کا پاس ورڈ بن گیا ✅",
                                                "6 अंकों का पासवर्ड बनाया गया ✅"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        text(
                                                "ساخت رمز ناموفق بود",
                                                "Failed to create password",
                                                "د رمز جوړول ناکام شول",
                                                "پاس ورڈ بنانا ناکام ہوگیا",
                                                "पासवर्ड बनाना विफल हुआ"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        text(
                                "انصراف",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .show();
    }

    // ==================================
    // تغییر رمز
    // ==================================

    private void showChangePasswordDialog() {

        LinearLayout layout =
                createPasswordLayout();

        EditText oldPassword =
                createPasswordInput(
                        text(
                                "رمز فعلی: ۶ رقم",
                                "Current password: 6 digits",
                                "اوسنی رمز: ۶ عددې",
                                "موجودہ پاس ورڈ: ۶ ہندسے",
                                "वर्तमान पासवर्ड: 6 अंक"
                        )
                );

        EditText newPassword =
                createPasswordInput(
                        text(
                                "رمز جدید: ۶ رقم",
                                "New password: 6 digits",
                                "نوی رمز: ۶ عددې",
                                "نیا پاس ورڈ: ۶ ہندسے",
                                "नया पासवर्ड: 6 अंक"
                        )
                );

        EditText confirmPassword =
                createPasswordInput(
                        text(
                                "تکرار رمز جدید",
                                "Confirm new password",
                                "د نوي رمز تکرار",
                                "نئے پاس ورڈ کی تصدیق",
                                "नए पासवर्ड की पुष्टि करें"
                        )
                );

        layout.addView(oldPassword);
        layout.addView(newPassword);
        layout.addView(confirmPassword);

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔑 " +
                        text(
                                "تغییر رمز",
                                "Change Password",
                                "رمز بدلول",
                                "پاس ورڈ تبدیل کریں",
                                "पासवर्ड बदलें"
                        )
                )
                .setView(layout)
                .setPositiveButton(
                        text(
                                "تغییر",
                                "Change",
                                "بدلول",
                                "تبدیل کریں",
                                "बदलें"
                        ),
                        (dialog, which) -> {

                            String oldPass =
                                    oldPassword.getText()
                                            .toString();

                            String newPass =
                                    newPassword.getText()
                                            .toString();

                            String confirm =
                                    confirmPassword.getText()
                                            .toString();

                            if (!AppLockManager.isValidPassword(
                                    oldPass
                            )) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز فعلی باید دقیقاً ۶ رقم باشد",
                                                "Current password must be exactly 6 digits",
                                                "اوسنی رمز باید دقیقاً ۶ عددې وي",
                                                "موجودہ پاس ورڈ بالکل ۶ ہندسوں کا ہونا چاہیے",
                                                "वर्तमान पासवर्ड ठीक 6 अंकों का होना चाहिए"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!AppLockManager.isValidPassword(
                                    newPass
                            )) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز جدید باید دقیقاً ۶ رقم باشد",
                                                "New password must be exactly 6 digits",
                                                "نوی رمز باید دقیقاً ۶ عددې وي",
                                                "نیا پاس ورڈ بالکل ۶ ہندسوں کا ہونا چاہیے",
                                                "नया पासवर्ड ठीक 6 अंकों का होना चाहिए"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!newPass.equals(confirm)) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز جدید و تکرار آن یکسان نیست",
                                                "New password and confirmation do not match",
                                                "نوی رمز او تکرار یې یو شان نه دي",
                                                "نیا پاس ورڈ اور تصدیق ایک جیسے نہیں ہیں",
                                                "नया पासवर्ड और पुष्टि समान नहीं हैं"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (AppLockManager.changePassword(
                                    this,
                                    oldPass,
                                    newPass
                            )) {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز با موفقیت تغییر کرد ✅",
                                                "Password changed successfully ✅",
                                                "رمز په بریالیتوب بدل شو ✅",
                                                "پاس ورڈ کامیابی سے تبدیل ہوگیا ✅",
                                                "पासवर्ड सफलतापूर्वक बदल गया ✅"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        text(
                                                "رمز فعلی اشتباه است ❌",
                                                "Current password is incorrect ❌",
                                                "اوسنی رمز ناسم دی ❌",
                                                "موجودہ پاس ورڈ غلط ہے ❌",
                                                "वर्तमान पासवर्ड गलत है ❌"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        text(
                                "انصراف",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .show();
    }

    // ==================================
    // فعال کردن قفل
    // ==================================

    private void enableLock() {

        AppLockManager.setLockEnabled(
                this,
                true
        );

        Toast.makeText(
                this,
                text(
                        "🔐 قفل برنامه فعال شد",
                        "🔐 App lock enabled",
                        "🔐 د پروګرام قفل فعال شو",
                        "🔐 ایپ لاک فعال ہوگیا",
                        "🔐 ऐप लॉक चालू हो गया"
                ),
                Toast.LENGTH_SHORT
        ).show();
    }

    // ==================================
    // غیرفعال کردن قفل
    // ==================================

    private void disableLock() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "🔓 " +
                        text(
                                "غیرفعال کردن قفل",
                                "Disable Lock",
                                "قفل غیر فعالول",
                                "لاک غیر فعال کریں",
                                "लॉक बंद करें"
                        )
                )
                .setMessage(
                        text(
                                "آیا می‌خواهید قفل برنامه غیرفعال شود؟",
                                "Do you want to disable the app lock?",
                                "ایا غواړئ د پروګرام قفل غیر فعال کړئ؟",
                                "کیا آپ ایپ لاک غیر فعال کرنا چاہتے ہیں؟",
                                "क्या आप ऐप लॉक बंद करना चाहते हैं?"
                        )
                )
                .setNegativeButton(
                        text(
                                "انصراف",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .setPositiveButton(
                        text(
                                "غیرفعال",
                                "Disable",
                                "غیر فعال",
                                "غیر فعال کریں",
                                "बंद करें"
                        ),
                        (dialog, which) -> {

                            AppLockManager.setLockEnabled(
                                    this,
                                    false
                            );

                            Toast.makeText(
                                    this,
                                    text(
                                            "قفل برنامه غیرفعال شد",
                                            "App lock disabled",
                                            "د پروګرام قفل غیر فعال شو",
                                            "ایپ لاک غیر فعال ہوگیا",
                                            "ऐप लॉक बंद हो गया"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                )
                .show();
    }

    // ==================================
    // اطلاعات شخصی
    // ==================================

    private void showPersonalInfoSettings() {

        boolean hidden =
                AppLockManager.isPersonalInfoHidden(this);

        String[] options = {
                "👁️ " +
                text(
                        "نمایش اطلاعات شخصی",
                        "Show Personal Information",
                        "شخصي معلومات ښکاره کول",
                        "ذاتی معلومات دکھائیں",
                        "व्यक्तिगत जानकारी दिखाएं"
                ),
                "🙈 " +
                text(
                        "مخفی‌کردن اطلاعات شخصی",
                        "Hide Personal Information",
                        "شخصي معلومات پټول",
                        "ذاتی معلومات چھپائیں",
                        "व्यक्तिगत जानकारी छिपाएं"
                )
        };

        new AlertDialog.Builder(this)
                .setTitle(
                        "👁️ " +
                        text(
                                "اطلاعات شخصی",
                                "Personal Information",
                                "شخصي معلومات",
                                "ذاتی معلومات",
                                "व्यक्तिगत जानकारी"
                        )
                )
                .setSingleChoiceItems(
                        options,
                        hidden ? 1 : 0,
                        (dialog, which) -> {

                            boolean hide =
                                    which == 1;

                            AppLockManager
                                    .setPersonalInfoHidden(
                                            this,
                                            hide
                                    );

                            Toast.makeText(
                                    this,
                                    hide
                                            ? text(
                                                "اطلاعات شخصی مخفی شد 🙈",
                                                "Personal information hidden 🙈",
                                                "شخصي معلومات پټ شول 🙈",
                                                "ذاتی معلومات چھپا دیے گئے 🙈",
                                                "व्यक्तिगत जानकारी छिपाई गई 🙈"
                                            )
                                            : text(
                                                "اطلاعات شخصی نمایش داده می‌شود 👁️",
                                                "Personal information is visible 👁️",
                                                "شخصي معلومات ښکاره کېږي 👁️",
                                                "ذاتی معلومات دکھائے جا رہے ہیں 👁️",
                                                "व्यक्तिगत जानकारी दिखाई जा रही है 👁️"
                                            ),
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        }
                )
                .setNegativeButton(
                        text(
                                "بستن",
                                "Close",
                                "بندول",
                                "بند کریں",
                                "बंद करें"
                        ),
                        null
                )
                .show();
    }

    // ==================================
    // ساخت Layout رمز
    // ==================================

    private LinearLayout createPasswordLayout() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                dp(20),
                dp(5),
                dp(20),
                dp(5)
        );

        return layout;
    }

    // ==================================
    // فیلد رمز
    // ==================================

    private EditText createPasswordInput(
            String hint
    ) {

        EditText input =
                new EditText(this);

        input.setHint(hint);

        input.setTextSize(18);

        input.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        input.setSingleLine(true);

        input.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(58)
                );

        params.setMargins(
                0,
                dp(5),
                0,
                dp(5)
        );

        input.setLayoutParams(params);

        return input;
    }

    // ==================================
    // خروج از حساب
    // ==================================

    private void showLogoutDialog() {

        new AlertDialog.Builder(this)
                .setTitle(
                        "🚪 " +
                        text(
                                "خروج از حساب",
                                "Log Out",
                                "له اکاونټ څخه وتل",
                                "اکاؤنٹ سے لاگ آؤٹ",
                                "लॉग आउट"
                        )
                )
                .setMessage(
                        text(
                                "آیا مطمئن هستید که می‌خواهید " +
                                "از حساب خود خارج شوید؟",
                                "Are you sure you want to log out?",
                                "ایا ډاډه یاست چې غواړئ له خپل اکاونټ څخه ووځئ؟",
                                "کیا آپ واقعی اپنے اکاؤنٹ سے لاگ آؤٹ کرنا چاہتے ہیں؟",
                                "क्या आप वाकई अपने अकाउंट से लॉग आउट करना चाहते हैं?"
                        )
                )
                .setNegativeButton(
                        text(
                                "انصراف",
                                "Cancel",
                                "لغوه",
                                "منسوخ",
                                "रद्द करें"
                        ),
                        null
                )
                .setPositiveButton(
                        text(
                                "خروج",
                                "Log Out",
                                "وتل",
                                "لاگ آؤٹ",
                                "लॉग आउट"
                        ),
                        (dialog, which) -> {

                            FirebaseAuth
                                    .getInstance()
                                    .signOut();

                            AppLockManager.lockSession(this);

                            Toast.makeText(
                                    this,
                                    text(
                                            "از حساب خارج شدید",
                                            "You have logged out",
                                            "له اکاونټ څخه ووتل",
                                            "آپ لاگ آؤٹ ہوگئے ہیں",
                                            "आप लॉग आउट हो गए हैं"
                                    ),
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
                        "ℹ️ " +
                        text(
                                "درباره برنامه",
                                "About App",
                                "د پروګرام په اړه",
                                "ایپ کے بارے میں",
                                "ऐप के बारे में"
                        )
                )
                .setMessage(
                        text(
                                "تجربه‌ها\n\n" +
                                "محلی برای ثبت، یادگیری و " +
                                "شریک‌کردن تجربه‌ها.\n\n" +
                                "یاد بگیر • تجربه کن • شریک کن\n\n" +
                                "نسخه برنامه: ",
                                "Experiences\n\n" +
                                "A place to record, learn from, and " +
                                "share experiences.\n\n" +
                                "Learn • Experience • Share\n\n" +
                                "App version: ",
                                "تجربې\n\n" +
                                "د تجربو د ثبت، زده کړې او شریکولو لپاره یو ځای.\n\n" +
                                "زده کړه • تجربه کړه • شریک یې کړه\n\n" +
                                "د پروګرام نسخه: ",
                                "تجربات\n\n" +
                                "تجربات درج کرنے، سیکھنے اور شیئر کرنے کی جگہ۔\n\n" +
                                "سیکھیں • تجربہ کریں • شیئر کریں\n\n" +
                                "ایپ ورژن: ",
                                "अनुभव\n\n" +
                                "अनुभव दर्ज करने, सीखने और साझा करने की जगह।\n\n" +
                                "सीखें • अनुभव करें • साझा करें\n\n" +
                                "ऐप संस्करण: "
                        )
                        + getVersionName()
                )
                .setPositiveButton(
                        text(
                                "بستن",
                                "Close",
                                "بندول",
                                "بند کریں",
                                "बंद करें"
                        ),
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
