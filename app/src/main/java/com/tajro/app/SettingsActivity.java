package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends Activity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                "tajrobehha_settings",
                MODE_PRIVATE
        );

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                30, 40, 30, 30
        );

        layout.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        // عنوان
        TextView title = new TextView(this);

        title.setText("⚙️ تنظیمات تجربه‌ها");
        title.setTextSize(28);
        title.setTextColor(
                Color.rgb(8, 65, 90)
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0, 0, 0, 30
        );

        layout.addView(title);

        // اعلان‌ها
        Switch notificationSwitch =
                new Switch(this);

        notificationSwitch.setText(
                "🔔 اعلان‌ها"
        );

        notificationSwitch.setTextSize(18);

        notificationSwitch.setChecked(
                preferences.getBoolean(
                        "notifications",
                        true
                )
        );

        layout.addView(
                notificationSwitch
        );

        notificationSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    preferences.edit()
                            .putBoolean(
                                    "notifications",
                                    isChecked
                            )
                            .apply();

                    if (isChecked) {

                        Toast.makeText(
                                this,
                                "اعلان‌ها فعال شد 🔔",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        Toast.makeText(
                                this,
                                "اعلان‌ها خاموش شد",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // زبان
        Button languageButton =
                new Button(this);

        languageButton.setText(
                "🌐 زبان برنامه: فارسی / دری"
        );

        layout.addView(
                languageButton
        );

        languageButton.setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "زبان فعلی: فارسی / دری",
                        Toast.LENGTH_SHORT
                ).show()
        );

        // حریم خصوصی
        Button privacyButton =
                new Button(this);

        privacyButton.setText(
                "🔒 حریم خصوصی"
        );

        layout.addView(
                privacyButton
        );

        privacyButton.setOnClickListener(v -> {

            Toast.makeText(
                    this,
                    "اطلاعات حساب و تجربه‌های شما " +
                    "باید مطابق قوانین Firebase محافظت شود.",
                    Toast.LENGTH_LONG
            ).show();
        });

        // پاک کردن تنظیمات محلی
        Button clearButton =
                new Button(this);

        clearButton.setText(
                "🧹 پاک کردن تنظیمات این دستگاه"
        );

        layout.addView(
                clearButton
        );

        clearButton.setOnClickListener(v -> {

            preferences.edit()
                    .clear()
                    .apply();

            notificationSwitch.setChecked(
                    true
            );

            Toast.makeText(
                    this,
                    "تنظیمات محلی پاک شد",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // خروج از حساب
        Button logoutButton =
                new Button(this);

        logoutButton.setText(
                "🚪 خروج از حساب"
        );

        layout.addView(
                logoutButton
        );

        logoutButton.setOnClickListener(v -> {

            FirebaseAuth.getInstance()
                    .signOut();

            Toast.makeText(
                    this,
                    "از حساب خارج شدید",
                    Toast.LENGTH_SHORT
            ).show();
        });

        // نسخه برنامه
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
                "\n📱 تجربه‌ها\n" +
                "نسخه " + version
        );

        versionText.setTextSize(16);
        versionText.setTextColor(
                Color.GRAY
        );

        versionText.setGravity(
                Gravity.CENTER
        );

        versionText.setPadding(
                0, 25, 0, 10
        );

        layout.addView(
                versionText
        );

        // نمایش صفحه
        setContentView(layout);
    }
}
