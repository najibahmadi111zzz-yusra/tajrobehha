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

                            // ذخیره زبان با سیستم جدید
                            LanguageManager.setLanguage(
                                    SettingsActivity.this,
                                    languageCode
                            );

                            // پیام موفقیت
                            Toast.makeText(
                                    SettingsActivity.this,
                                    "✅ زبان با موفقیت انتخاب شد",
                                    Toast.LENGTH_SHORT
                            ).show();

                            /*
                             * صفحه فعلی دوباره ساخته می‌شود
                             * تا زبان انتخاب‌شده از ابتدا اعمال شود.
                             */
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
                        ? "فعال"
                        : "غیرفعال";

        String hideStatus =
                AppLockManager.isPersonalInfoHidden(this)
                        ? "فعال"
                        : "غیرفعال";

        String message =
                "🔐 قفل برنامه: " + lockStatus +
                "\n👁️ مخفی‌کردن اطلاعات: " + hideStatus +
                "\n\n" +
                "از گزینه‌های زیر استفاده کنید.";

        new AlertDialog.Builder(this)
                .setTitle("🔒 حریم خصوصی")
                .setMessage(message)
                .setPositiveButton(
                        "🔐 قفل برنامه",
                        (dialog, which) ->
                                showLockSettings()
                )
                .setNeutralButton(
                        "👁️ اطلاعات شخصی",
                        (dialog, which) ->
                                showPersonalInfoSettings()
                )
                .setNegativeButton(
                        "بستن",
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
                    .setTitle("🔐 ساخت رمز قفل")
                    .setMessage(
                            "برای قفل برنامه یک رمز دقیقاً ۶ رقمی بسازید."
                    )
                    .setPositiveButton(
                            "ساخت رمز",
                            (dialog, which) ->
                                    showCreatePasswordDialog()
                    )
                    .setNegativeButton(
                            "انصراف",
                            null
                    )
                    .show();

            return;
        }

        String[] options;

        if (lockEnabled) {

            options = new String[]{
                    "🔑 تغییر رمز",
                    "🔓 غیرفعال کردن قفل"
            };

        } else {

            options = new String[]{
                    "🔐 فعال کردن قفل",
                    "🔑 تغییر رمز"
            };
        }

        new AlertDialog.Builder(this)
                .setTitle("🔐 تنظیمات قفل")
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
                        "رمز جدید: ۶ رقم"
                );

        EditText confirm =
                createPasswordInput(
                        "تکرار رمز: ۶ رقم"
                );

        layout.addView(password);
        layout.addView(confirm);

        new AlertDialog.Builder(this)
                .setTitle("🔐 ساخت رمز قفل")
                .setView(layout)
                .setPositiveButton(
                        "ذخیره",
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
                                        "رمز باید دقیقاً ۶ رقم باشد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!p.equals(c)) {

                                Toast.makeText(
                                        this,
                                        "دو رمز یکسان نیستند",
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
                                        "رمز ۶ رقمی ساخته شد ✅",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        "ساخت رمز ناموفق بود",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "انصراف",
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
                        "رمز فعلی: ۶ رقم"
                );

        EditText newPassword =
                createPasswordInput(
                        "رمز جدید: ۶ رقم"
                );

        EditText confirmPassword =
                createPasswordInput(
                        "تکرار رمز جدید"
                );

        layout.addView(oldPassword);
        layout.addView(newPassword);
        layout.addView(confirmPassword);

        new AlertDialog.Builder(this)
                .setTitle("🔑 تغییر رمز")
                .setView(layout)
                .setPositiveButton(
                        "تغییر",
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
                                        "رمز فعلی باید دقیقاً ۶ رقم باشد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!AppLockManager.isValidPassword(
                                    newPass
                            )) {

                                Toast.makeText(
                                        this,
                                        "رمز جدید باید دقیقاً ۶ رقم باشد",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            if (!newPass.equals(confirm)) {

                                Toast.makeText(
                                        this,
                                        "رمز جدید و تکرار آن یکسان نیست",
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
                                        "رمز با موفقیت تغییر کرد ✅",
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        this,
                                        "رمز فعلی اشتباه است ❌",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "انصراف",
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
                "🔐 قفل برنامه فعال شد",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ==================================
    // غیرفعال کردن قفل
    // ==================================

    private void disableLock() {

        new AlertDialog.Builder(this)
                .setTitle("🔓 غیرفعال کردن قفل")
                .setMessage(
                        "آیا می‌خواهید قفل برنامه غیرفعال شود؟"
                )
                .setNegativeButton(
                        "انصراف",
                        null
                )
                .setPositiveButton(
                        "غیرفعال",
                        (dialog, which) -> {

                            AppLockManager.setLockEnabled(
                                    this,
                                    false
                            );

                            Toast.makeText(
                                    this,
                                    "قفل برنامه غیرفعال شد",
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
                "👁️ نمایش اطلاعات شخصی",
                "🙈 مخفی‌کردن اطلاعات شخصی"
        };

        new AlertDialog.Builder(this)
                .setTitle("👁️ اطلاعات شخصی")
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
                                            ? "اطلاعات شخصی مخفی شد 🙈"
                                            : "اطلاعات شخصی نمایش داده می‌شود 👁️",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        }
                )
                .setNegativeButton(
                        "بستن",
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

                            AppLockManager.lockSession(this);

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
