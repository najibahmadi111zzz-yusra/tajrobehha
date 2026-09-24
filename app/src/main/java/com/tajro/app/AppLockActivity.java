package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AppLockActivity extends Activity {

    private EditText passwordInput;

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

        // اگر قفل لازم نیست، مستقیم برنامه را باز کن
        if (!AppLockManager.hasPassword(this)
                || !AppLockManager.isLockEnabled(this)
                || AppLockManager.isSessionUnlocked(this)) {

            openMainActivity();
            return;
        }

        buildLockScreen();
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

    private void buildLockScreen() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setGravity(
                Gravity.CENTER
        );

        layout.setPadding(
                45,
                45,
                45,
                45
        );

        layout.setBackgroundColor(
                Color.rgb(235, 248, 250)
        );

        TextView lockIcon = new TextView(this);

        lockIcon.setText("🔐");
        lockIcon.setTextSize(55);
        lockIcon.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);

        title.setText(
                text(
                        "برنامه قفل است",
                        "App is locked",
                        "اپ بند دی",
                        "ایپ لاک ہے",
                        "ऐप लॉक है"
                )
        );

        title.setTextSize(27);

        title.setTextColor(
                ThemeManager.getThemeColor(this)
        );

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0,
                20,
                0,
                15
        );

        TextView message = new TextView(this);

        message.setText(
                text(
                        "برای ورود رمز ۶ رقمی خود را وارد کنید",
                        "Enter your 6-digit password to continue",
                        "د ننوتلو لپاره خپل ۶ عددي رمز دننه کړئ",
                        "داخل ہونے کے لیے اپنا ۶ ہندسوں کا پاس ورڈ درج کریں",
                        "जारी रखने के लिए अपना ६ अंकों का पासवर्ड दर्ज करें"
                )
        );

        message.setTextSize(17);

        message.setTextColor(
                Color.DKGRAY
        );

        message.setGravity(Gravity.CENTER);

        message.setPadding(
                0,
                0,
                0,
                25
        );

        passwordInput = new EditText(this);

        passwordInput.setHint(
                text(
                        "رمز ۶ رقمی",
                        "6-digit password",
                        "۶ عددي رمز",
                        "۶ ہندسوں کا پاس ورڈ",
                        "६ अंकों का पासवर्ड"
                )
        );

        passwordInput.setTextSize(20);

        passwordInput.setGravity(
                Gravity.CENTER
        );

        passwordInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_VARIATION_PASSWORD
        );

        passwordInput.setSingleLine(true);

        Button unlockButton = new Button(this);

        unlockButton.setText(
                text(
                        "🔓 باز کردن قفل",
                        "🔓 Unlock",
                        "🔓 قلف خلاصول",
                        "🔓 لاک کھولیں",
                        "🔓 अनलॉक करें"
                )
        );

        unlockButton.setTextSize(17);

        unlockButton.setTextColor(
                Color.WHITE
        );

        unlockButton.setBackgroundColor(
                ThemeManager.getThemeColor(this)
        );

        layout.addView(
                lockIcon,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        layout.addView(
                message,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        LinearLayout.LayoutParams inputParams =
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                );

        inputParams.setMargins(
                0,
                0,
                0,
                20
        );

        layout.addView(
                passwordInput,
                inputParams
        );

        layout.addView(
                unlockButton,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        setContentView(layout);

        unlockButton.setOnClickListener(
                v -> checkPassword()
        );
    }

    private void checkPassword() {

        String password =
                passwordInput
                        .getText()
                        .toString();

        if (!AppLockManager.isValidPassword(password)) {

            Toast.makeText(
                    this,
                    text(
                            "رمز باید دقیقاً ۶ رقم باشد",
                            "Password must be exactly 6 digits",
                            "رمز باید په دقیق ډول ۶ عددونه وي",
                            "پاس ورڈ بالکل ۶ ہندسوں کا ہونا چاہیے",
                            "पासवर्ड ठीक ६ अंकों का होना चाहिए"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (AppLockManager.verifyPassword(
                this,
                password
        )) {

            AppLockManager.setSessionUnlocked(
                    this,
                    true
            );

            Toast.makeText(
                    this,
                    text(
                            "قفل باز شد ✅",
                            "Unlocked ✅",
                            "قلف خلاص شو ✅",
                            "لاک کھل گیا ✅",
                            "लॉक खुल गया ✅"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            openMainActivity();

        } else {

            passwordInput.setText("");

            Toast.makeText(
                    this,
                    text(
                            "رمز اشتباه است ❌",
                            "Wrong password ❌",
                            "ناسم رمز دی ❌",
                            "پاس ورڈ غلط ہے ❌",
                            "पासवर्ड गलत है ❌"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openMainActivity() {

        android.content.Intent intent =
                new android.content.Intent(
                        this,
                        MainActivity.class
                );

        intent.addFlags(
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        );

        startActivity(intent);

        finish();
    }

    @Override
    public void onBackPressed() {

        // تا وقتی رمز درست وارد نشده،
        // با دکمه برگشت وارد برنامه نشود.
        finishAffinity();
    }
}
