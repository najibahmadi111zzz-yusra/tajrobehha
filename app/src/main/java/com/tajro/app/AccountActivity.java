package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountActivity extends Activity {

    private FirebaseAuth auth;
    private EditText emailInput;
    private EditText passwordInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(35, 50, 35, 35);

        TextView title = new TextView(this);
        title.setText("👤 اکانت من");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 35);

        emailInput = new EditText(this);
        emailInput.setHint("ایمیل خود را وارد کنید");
        emailInput.setInputType(33);

        passwordInput = new EditText(this);
        passwordInput.setHint("رمز عبور");
        passwordInput.setInputType(129);

        Button registerButton = new Button(this);
        registerButton.setText("📝 ثبت‌نام");

        Button loginButton = new Button(this);
        loginButton.setText("🔐 ورود");

        Button logoutButton = new Button(this);
        logoutButton.setText("🚪 خروج");

        statusText = new TextView(this);
        statusText.setTextSize(17);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 30, 0, 20);

        layout.addView(title);
        layout.addView(emailInput);
        layout.addView(passwordInput);
        layout.addView(registerButton);
        layout.addView(loginButton);
        layout.addView(logoutButton);
        layout.addView(statusText);

        setContentView(layout);

        showCurrentUser();

        registerButton.setOnClickListener(v -> registerUser());

        loginButton.setOnClickListener(v -> loginUser());

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            showCurrentUser();

            Toast.makeText(
                    AccountActivity.this,
                    "از حساب خارج شدید",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void registerUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
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

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

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

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                    this,
                    "ایمیل و رمز عبور را وارد کنید",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {

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
                            "ورود ناموفق: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void showCurrentUser() {

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {

            statusText.setText(
                    "✅ وارد شده‌اید\n\n" +
                    "ایمیل:\n" +
                    user.getEmail()
            );

        } else {

            statusText.setText(
                    "🔒 وارد حساب نشده‌اید"
            );
        }
    }
}
