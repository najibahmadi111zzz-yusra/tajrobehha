{"variant":"document","id":"73514","title":"AccountActivity.java — نسخه چندزبانه"}
package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AccountActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private TextView statusText;

    private int themeColor;

    private String appliedLanguage;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    private String text(
            String fa,
            String en,
            String ps,
            String ur,
            String hi
    ) {
        String lang = LanguageManager.getLanguage(this);

        if ("en".equals(lang)) {
            return en;
        }

        if ("ps".equals(lang)) {
            return ps;
        }

        if ("ur".equals(lang)) {
            return ur;
        }

        if ("hi".equals(lang)) {
            return hi;
        }

        return fa;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appliedLanguage =
                LanguageManager.getLanguage(this);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        themeColor = ThemeManager.getThemeColor(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);
        layout.setBackgroundColor(getLightThemeColor());

        TextView title = new TextView(this);
        title.setText(
                text(
                        "👤 اکانت من",
                        "👤 My Account",
                        "👤 زما اکاونټ",
                        "👤 میرا اکاؤنٹ",
                        "👤 मेरा अकाउंट"
                )
        );
        title.setTextSize(28);
        title.setTextColor(themeColor);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        // نام نمایشی
        nameInput = new EditText(this);
        nameInput.setHint(
                text(
                        "نام نمایشی شما",
                        "Your display name",
                        "ستاسو ښودل کېدونکی نوم",
                        "آپ کا نمایشی نام",
                        "आपका प्रदर्शन नाम"
                )
        );
        nameInput.setSingleLine(true);

        // ایمیل
        emailInput = new EditText(this);
        emailInput.setHint(
                text(
                        "ایمیل خود را وارد کنید",
                        "Enter your email",
                        "خپل برېښنالیک دننه کړئ",
                        "اپنا ای میل درج کریں",
                        "अपना ईमेल दर्ज करें"
                )
        );
        emailInput.setInputType(33);
        emailInput.setSingleLine(true);

        // رمز عبور
        passwordInput = new EditText(this);
        passwordInput.setHint(
                text(
                        "رمز عبور",
                        "Password",
                        "پټ نوم",
                        "پاس ورڈ",
                        "पासवर्ड"
                )
        );
        passwordInput.setSingleLine(true);
        passwordInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        );
        passwordInput.setTransformationMethod(
                PasswordTransformationMethod.getInstance()
        );

        // دکمه ثبت نام
        Button registerButton = new Button(this);
        registerButton.setText(
                text(
                        "📝 ثبت‌نام",
                        "📝 Sign Up",
                        "📝 نوم‌لیکنه",
                        "📝 رجسٹریشن",
                        "📝 साइन अप"
                )
        );
        styleButton(registerButton);

        // دکمه ورود
        Button loginButton = new Button(this);
        loginButton.setText(
                text(
                        "🔐 ورود",
                        "🔐 Login",
                        "🔐 ننوتل",
                        "🔐 لاگ اِن",
                        "🔐 लॉग इन"
                )
        );
        styleButton(loginButton);

        // دکمه ذخیره پروفایل
        Button saveProfileButton = new Button(this);
        saveProfileButton.setText(
                text(
                        "💾 ذخیره نام",
                        "💾 Save Name",
                        "💾 نوم خوندي کړئ",
                        "💾 نام محفوظ کریں",
                        "💾 नाम सेव करें"
                )
        );
        styleButton(saveProfileButton);

        // دکمه خروج
        Button logoutButton = new Button(this);
        logoutButton.setText(
                text(
                        "🚪 خروج",
                        "🚪 Logout",
                        "🚪 وتل",
                        "🚪 لاگ آؤٹ",
                        "🚪 लॉग आउट"
                )
        );
        styleButton(logoutButton);

        statusText = new TextView(this);
        statusText.setTextSize(17);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 25, 0, 20);

        layout.addView(title);
        layout.addView(nameInput);
        layout.addView(emailInput);
        layout.addView(passwordInput);
        layout.addView(registerButton);
        layout.addView(loginButton);
        layout.addView(saveProfileButton);
        layout.addView(logoutButton);
        layout.addView(statusText);

        setContentView(layout);

        showCurrentUser();

        // ثبت نام
        registerButton.setOnClickListener(v ->
                registerUser()
        );

        // ورود
        loginButton.setOnClickListener(v ->
                loginUser()
        );

        // ذخیره پروفایل
        saveProfileButton.setOnClickListener(v ->
                saveProfile()
        );

        // خروج
        logoutButton.setOnClickListener(v -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                setUserOffline(user.getUid());
            }

            auth.signOut();

            AppLockManager.lockSession(this);

            clearLoginFields();

            showCurrentUser();

            Toast.makeText(
                    AccountActivity.this,
                    text(
                            "از حساب خارج شدید 🔒",
                            "You have logged out 🔒",
                            "تاسو له حساب څخه ووتل 🔒",
                            "آپ اکاؤنٹ سے لاگ آؤٹ ہو گئے ہیں 🔒",
                            "आप लॉग आउट हो गए हैं 🔒"
                    ),
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {

            recreate();
            return;
        }
    }

    // ==================================
    // ثبت نام
    // ==================================

    private void registerUser() {

        String email =
                emailInput.getText()
                        .toString()
                        .trim();

        String password =
                passwordInput.getText()
                        .toString();

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        if (email.isEmpty() || password.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "ایمیل و رمز عبور را وارد کنید",
                            "Enter email and password",
                            "برېښنالیک او پټ نوم دننه کړئ",
                            "ای میل اور پاس ورڈ درج کریں",
                            "ईमेल और पासवर्ड दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    this,
                    text(
                            "رمز عبور باید حداقل ۶ حرف باشد",
                            "Password must be at least 6 characters",
                            "پټ نوم باید لږ تر لږه ۶ توري ولري",
                            "پاس ورڈ کم از کم ۶ حروف کا ہونا چاہیے",
                            "पासवर्ड कम से कम 6 अक्षरों का होना चाहिए"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (name.isEmpty()) {
            name = email.split("@")[0];
        }

        final String finalName = name;

        auth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user != null) {

                        saveUserToFirestore(
                                user,
                                finalName
                        );
                    }

                    // پاک کردن ایمیل و رمز بعد از ثبت نام موفق
                    clearLoginFields();

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "اکانت با موفقیت ساخته شد 🎉",
                                    "Account created successfully 🎉",
                                    "اکاونټ په بریالیتوب جوړ شو 🎉",
                                    "اکاؤنٹ کامیابی سے بن گیا 🎉",
                                    "अकाउंट सफलतापूर्वक बनाया गया 🎉"
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentUser();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "خطا: ",
                                    "Error: ",
                                    "تېروتنه: ",
                                    "خرابی: ",
                                    "त्रुटि: "
                            ) + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ==================================
    // ورود
    // ==================================

    private void loginUser() {

        String email =
                emailInput.getText()
                        .toString()
                        .trim();

        String password =
                passwordInput.getText()
                        .toString();

        if (email.isEmpty() || password.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "ایمیل و رمز عبور را وارد کنید",
                            "Enter email and password",
                            "برېښنالیک او پټ نوم دننه کړئ",
                            "ای میل اور پاس ورڈ درج کریں",
                            "ईमेल और पासवर्ड दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        auth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user != null) {

                        loadUserProfile(user);

                        setUserOnline(
                                user.getUid()
                        );
                    }

                    // مهم:
                    // بعد از ورود موفق، ایمیل و رمز پاک می‌شوند.
                    clearLoginFields();

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "ورود موفق بود ✅",
                                    "Login successful ✅",
                                    "ننوتل بریالی شو ✅",
                                    "لاگ اِن کامیاب ہوا ✅",
                                    "लॉग इन सफल हुआ ✅"
                            ),
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentUser();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "ورود ناموفق: ",
                                    "Login failed: ",
                                    "ننوتل ناکام شو: ",
                                    "لاگ اِن ناکام: ",
                                    "लॉग इन विफल: "
                            ) + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ==================================
    // پاک کردن اطلاعات ورود
    // ==================================

    private void clearLoginFields() {

        emailInput.setText("");
        passwordInput.setText("");
    }

    // ==================================
    // ذخیره پروفایل
    // ==================================

    private void saveProfile() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    text(
                            "ابتدا وارد حساب شوید",
                            "Please log in first",
                            "لومړی خپل حساب ته ننوتل وکړئ",
                            "پہلے اپنے اکاؤنٹ میں لاگ اِن کریں",
                            "पहले अपने अकाउंट में लॉग इन करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        if (name.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "نام نمایشی را وارد کنید",
                            "Enter your display name",
                            "خپل ښودل کېدونکی نوم دننه کړئ",
                            "اپنا نمایشی نام درج کریں",
                            "अपना प्रदर्शन नाम दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("name", name);
        data.put("email", user.getEmail());
        data.put("userId", user.getUid());
        data.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "نام شما ذخیره شد ✅",
                                    "Your name was saved ✅",
                                    "ستاسو نوم خوندي شو ✅",
                                    "آپ کا نام محفوظ ہو گیا ✅",
                                    "आपका नाम सेव हो गया ✅"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "خطا در ذخیره نام: ",
                                    "Error saving name: ",
                                    "د نوم په خوندي کولو کې تېروتنه: ",
                                    "نام محفوظ کرنے میں خرابی: ",
                                    "नाम सेव करने में त्रुटि: "
                            ) + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ==================================
    // ذخیره کاربر در Firestore
    // ==================================

    private void saveUserToFirestore(
            FirebaseUser user,
            String name
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("name", name);
        data.put("email", user.getEmail());
        data.put("userId", user.getUid());
        data.put("online", true);
        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );
        data.put("typingTo", "");

        db.collection("users")
                .document(user.getUid())
                .set(data, SetOptions.merge());
    }

    // ==================================
    // آنلاین
    // ==================================

    private void setUserOnline(String uid) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("online", true);
        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .set(data, SetOptions.merge());
    }

    // ==================================
    // آفلاین
    // ==================================

    private void setUserOffline(String uid) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("online", false);
        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );
        data.put("typingTo", "");

        db.collection("users")
                .document(uid)
                .set(data, SetOptions.merge());
    }

    // ==================================
    // خواندن پروفایل
    // ==================================

    private void loadUserProfile(
            FirebaseUser user
    ) {

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString("name");

                        if (name != null
                                && !name.isEmpty()) {

                            nameInput.setText(name);
                        }
                    }
                });
    }

    // ==================================
    // نمایش وضعیت حساب
    // ==================================

    private void showCurrentUser() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            loadUserProfile(user);

            setUserOnline(user.getUid());

            if (AppLockManager.isPersonalInfoHidden(this)) {

                statusText.setText(
                        text(
                                "✅ وارد شده‌اید\n\n" +
                                "🟢 آنلاین\n\n" +
                                "👁️ اطلاعات شخصی شما مخفی است",

                                "✅ You are logged in\n\n" +
                                "🟢 Online\n\n" +
                                "👁️ Your personal information is hidden",

                                "✅ تاسو ننوتلي یاست\n\n" +
                                "🟢 آنلاین\n\n" +
                                "👁️ ستاسو شخصي معلومات پټ دي",

                                "✅ آپ لاگ اِن ہیں\n\n" +
                                "🟢 آن لائن\n\n" +
                                "👁️ آپ کی ذاتی معلومات چھپی ہوئی ہیں",

                                "✅ आप लॉग इन हैं\n\n" +
                                "🟢 ऑनलाइन\n\n" +
                                "👁️ आपकी निजी जानकारी छिपी हुई है"
                        )
                );

            } else {

                String email =
                        user.getEmail();

                statusText.setText(
                        text(
                                "✅ وارد شده‌اید\n\n" +
                                "🟢 آنلاین\n\n" +
                                "ایمیل:\n" +
                                email,

                                "✅ You are logged in\n\n" +
                                "🟢 Online\n\n" +
                                "Email:\n" +
                                email,

                                "✅ تاسو ننوتلي یاست\n\n" +
                                "🟢 آنلاین\n\n" +
                                "برېښنالیک:\n" +
                                email,

                                "✅ آپ لاگ اِن ہیں\n\n" +
                                "🟢 آن لائن\n\n" +
                                "ای میل:\n" +
                                email,

                                "✅ आप लॉग इन हैं\n\n" +
                                "🟢 ऑनलाइन\n\n" +
                                "ईमेल:\n" +
                                email
                        )
                );
            }

        } else {

            statusText.setText(
                    text(
                            "🔒 وارد حساب نشده‌اید",
                            "🔒 You are not logged in",
                            "🔒 تاسو حساب ته نه یاست ننوتلي",
                            "🔒 آپ اکاؤنٹ میں لاگ اِن نہیں ہیں",
                            "🔒 आप लॉग इन नहीं हैं"
                    )
            );
        }
    }

    // ==================================
    // استایل دکمه
    // ==================================

    private void styleButton(Button button) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);
        background.setCornerRadius(24);

        button.setBackground(background);
        button.setTextColor(Color.WHITE);
    }

    // ==================================
    // رنگ پس‌زمینه
    // ==================================

    private int getLightThemeColor() {

        int red =
                Color.red(themeColor);

        int green =
                Color.green(themeColor);

        int blue =
                Color.blue(themeColor);

        red =
                red + (255 - red) * 92 / 100;

        green =
                green + (255 - green) * 92 / 100;

        blue =
                blue + (255 - blue) * 92 / 100;

        return Color.rgb(
                red,
                green,
                blue
        );
    }
}
