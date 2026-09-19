package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        themeColor = ThemeManager.getThemeColor(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);
        layout.setBackgroundColor(getLightThemeColor());

        TextView title = new TextView(this);
        title.setText("👤 اکانت من");
        title.setTextSize(28);
        title.setTextColor(themeColor);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);

        // ==============================
        // نام نمایشی
        // ==============================

        nameInput = new EditText(this);
        nameInput.setHint("نام نمایشی شما");
        nameInput.setSingleLine(true);

        // ==============================
        // ایمیل
        // ==============================

        emailInput = new EditText(this);
        emailInput.setHint("ایمیل خود را وارد کنید");
        emailInput.setInputType(33);
        emailInput.setSingleLine(true);

        // ==============================
        // رمز
        // ==============================

passwordInput = new EditText(this);
passwordInput.setHint("رمز عبور");
passwordInput.setSingleLine(true);
passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
passwordInput.setTransformationMethod(
        PasswordTransformationMethod.getInstance()
);

        // ==============================
        // دکمه‌ها
        // ==============================

        Button registerButton = new Button(this);
        registerButton.setText("📝 ثبت‌نام");
        styleButton(registerButton);

        Button loginButton = new Button(this);
        loginButton.setText("🔐 ورود");
        styleButton(loginButton);

        Button saveProfileButton = new Button(this);
        saveProfileButton.setText("💾 ذخیره نام");
        styleButton(saveProfileButton);

        Button logoutButton = new Button(this);
        logoutButton.setText("🚪 خروج");
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

        // ==============================
        // ثبت نام
        // ==============================

        registerButton.setOnClickListener(v ->
                registerUser()
        );

        // ==============================
        // ورود
        // ==============================

        loginButton.setOnClickListener(v ->
                loginUser()
        );

        // ==============================
        // ذخیره پروفایل
        // ==============================

        saveProfileButton.setOnClickListener(v ->
                saveProfile()
        );

        // ==============================
        // خروج
        // ==============================

        logoutButton.setOnClickListener(v -> {

            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                setUserOffline(user.getUid());
            }

            auth.signOut();

            // قفل دوباره برنامه بعد از خروج
            AppLockManager.lockSession(this);

            showCurrentUser();

            Toast.makeText(
                    AccountActivity.this,
                    "از حساب خارج شدید 🔒",
                    Toast.LENGTH_SHORT
            ).show();
        });
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

        if (email.isEmpty()
                || password.isEmpty()) {

            Toast.makeText(
                    this,
                    "ایمیل و رمز عبور را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (password.length() < 6) {

            Toast.makeText(
                    this,
                    "رمز عبور باید حداقل ۶ حرف باشد",
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

                    Toast.makeText(
                            AccountActivity.this,
                            "اکانت با موفقیت ساخته شد 🎉",
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentUser();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            "خطا: " + e.getMessage(),
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

        if (email.isEmpty()
                || password.isEmpty()) {

            Toast.makeText(
                    this,
                    "ایمیل و رمز عبور را وارد کنید",
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

                    Toast.makeText(
                            AccountActivity.this,
                            "ورود موفق بود ✅",
                            Toast.LENGTH_LONG
                    ).show();

                    showCurrentUser();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            "ورود ناموفق: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
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
                    "ابتدا وارد حساب شوید",
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
                    "نام نمایشی را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Map<String, Object> data =
                new HashMap<>();

        data.put("name", name);
        data.put("email", user.getEmail());
        data.put("userId", user.getUid());
        data.put("updatedAt",
                FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            AccountActivity.this,
                            "نام شما ذخیره شد ✅",
                            Toast.LENGTH_SHORT
                    ).show();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            "خطا در ذخیره نام: "
                                    + e.getMessage(),
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
        data.put("lastSeen",
                FieldValue.serverTimestamp());
        data.put("typingTo", "");

        db.collection("users")
                .document(user.getUid())
                .set(data);
    }

    // ==================================
    // آنلاین
    // ==================================

    private void setUserOnline(String uid) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("online", true);
        data.put("lastSeen",
                FieldValue.serverTimestamp());

        db.collection("users")
                .document(uid)
                .set(
                        data,
                        com.google.firebase.firestore.SetOptions.merge()
                );
    }

    // ==================================
    // آفلاین
    // ==================================

    private void setUserOffline(String uid) {

        Map<String, Object> data =
                new HashMap<>();

        data.put("online", false);
        data.put("lastSeen",
                FieldValue.serverTimestamp());
        data.put("typingTo", "");

        db.collection("users")
                .document(uid)
                .set(
                        data,
                        com.google.firebase.firestore.SetOptions.merge()
                );
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
                        "✅ وارد شده‌اید\n\n" +
                        "🟢 آنلاین\n\n" +
                        "👁️ اطلاعات شخصی شما مخفی است"
                );

            } else {

                String email =
                        user.getEmail();

                statusText.setText(
                        "✅ وارد شده‌اید\n\n" +
                        "🟢 آنلاین\n\n" +
                        "ایمیل:\n" +
                        email
                );
            }

        } else {

            statusText.setText(
                    "🔒 وارد حساب نشده‌اید"
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
