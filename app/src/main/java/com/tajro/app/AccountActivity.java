package com.tajro.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AccountActivity extends Activity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText nameInput;
    private EditText loginInput;
    private EditText passwordInput;
    private EditText verificationCodeInput;

    private TextView statusText;
    private TextView modeTitle;

    private Button emailModeButton;
    private Button phoneModeButton;
    private Button registerButton;
    private Button loginButton;
    private Button verifyCodeButton;
    private Button saveProfileButton;
    private Button logoutButton;

    private int themeColor;

    private String appliedLanguage;

    // 0 = ورود
    // 1 = ثبت نام
    private int accountMode = 0;

    // false = ایمیل
    // true = شماره تلفن
    private boolean phoneMode = false;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    // 0 = بدون درخواست
    // 1 = ثبت نام با شماره
    // 2 = ورود با شماره
    private int phoneVerificationMode = 0;

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

        themeColor =
                ThemeManager.getThemeColor(this);

        createAccountScreen();

        showCurrentUser();
    }

    // ==========================================
    // ساخت صفحه حساب
    // ==========================================

    private void createAccountScreen() {

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                35,
                45,
                35,
                35
        );

        layout.setBackgroundColor(
                getLightThemeColor()
        );

        TextView title =
                new TextView(this);

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
        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(Gravity.CENTER);

        title.setPadding(
                0,
                0,
                0,
                20
        );

        layout.addView(title);

        // ==========================================
        // انتخاب حالت
        // ==========================================

        LinearLayout modeLayout =
                new LinearLayout(this);

        modeLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        modeLayout.setGravity(
                Gravity.CENTER
        );

        Button loginModeButton =
                new Button(this);

        loginModeButton.setText(
                text(
                        "🔐 ورود",
                        "🔐 Login",
                        "🔐 ننوتل",
                        "🔐 لاگ اِن",
                        "🔐 लॉग इन"
                )
        );

        styleButton(loginModeButton);

        Button registerModeButton =
                new Button(this);

        registerModeButton.setText(
                text(
                        "📝 ثبت‌نام",
                        "📝 Sign Up",
                        "📝 نوم‌لیکنه",
                        "📝 رجسٹریشن",
                        "📝 साइन अप"
                )
        );

        styleButton(registerModeButton);

        modeLayout.addView(
                loginModeButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        modeLayout.addView(
                registerModeButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        layout.addView(modeLayout);

        modeTitle =
                new TextView(this);

        modeTitle.setTextSize(18);
        modeTitle.setTextColor(themeColor);
        modeTitle.setGravity(Gravity.CENTER);
        modeTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        modeTitle.setPadding(
                0,
                20,
                0,
                10
        );

        layout.addView(modeTitle);

        // ==========================================
        // انتخاب ایمیل / شماره
        // ==========================================

        LinearLayout methodLayout =
                new LinearLayout(this);

        methodLayout.setOrientation(
                LinearLayout.HORIZONTAL
        );

        methodLayout.setGravity(
                Gravity.CENTER
        );

        emailModeButton =
                new Button(this);

        emailModeButton.setText(
                text(
                        "📧 ایمیل",
                        "📧 Email",
                        "📧 برېښنالیک",
                        "📧 ای میل",
                        "📧 ईमेल"
                )
        );

        styleButton(emailModeButton);

        phoneModeButton =
                new Button(this);

        phoneModeButton.setText(
                text(
                        "📱 شماره تلفن",
                        "📱 Phone",
                        "📱 د تلیفون شمېره",
                        "📱 فون نمبر",
                        "📱 फोन नंबर"
                )
        );

        styleButton(phoneModeButton);

        methodLayout.addView(
                emailModeButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        methodLayout.addView(
                phoneModeButton,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        layout.addView(methodLayout);

        // ==========================================
        // نام نمایشی
        // ==========================================

        nameInput =
                new EditText(this);

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

        layout.addView(nameInput);

        // ==========================================
        // ایمیل یا شماره تلفن
        // ==========================================

        loginInput =
                new EditText(this);

        loginInput.setHint(
                text(
                        "ایمیل یا شماره تلفن خود را وارد کنید",
                        "Enter your email or phone number",
                        "خپل برېښنالیک یا د تلیفون شمېره دننه کړئ",
                        "اپنا ای میل یا فون نمبر درج کریں",
                        "अपना ईमेल या फोन नंबर दर्ज करें"
                )
        );

        loginInput.setSingleLine(true);

        loginInput.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        layout.addView(loginInput);

        // ==========================================
        // رمز ورود
        // ==========================================

        passwordInput =
                new EditText(this);

        passwordInput.setHint(
                text(
                        "رمز ورود",
                        "Password",
                        "پټ نوم",
                        "پاس ورڈ",
                        "पासवर्ड"
                )
        );

        passwordInput.setSingleLine(true);

        passwordInput.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        passwordInput.setTransformationMethod(
                PasswordTransformationMethod.getInstance()
        );

        layout.addView(passwordInput);

        // ==========================================
        // کد SMS
        // ==========================================

        verificationCodeInput =
                new EditText(this);

        verificationCodeInput.setHint(
                text(
                        "کد تأیید SMS را وارد کنید",
                        "Enter SMS verification code",
                        "د SMS د تایید کوډ دننه کړئ",
                        "SMS تصدیقی کوڈ درج کریں",
                        "SMS सत्यापन कोड दर्ज करें"
                )
        );

        verificationCodeInput.setSingleLine(true);

        verificationCodeInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
        );

        verificationCodeInput.setVisibility(
                View.GONE
        );

        layout.addView(
                verificationCodeInput
        );

        // ==========================================
        // دکمه ثبت نام
        // ==========================================

        registerButton =
                new Button(this);

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

        layout.addView(registerButton);

        // ==========================================
        // دکمه ورود
        // ==========================================

        loginButton =
                new Button(this);

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

        layout.addView(loginButton);

        // ==========================================
        // دکمه تأیید کد
        // ==========================================

        verifyCodeButton =
                new Button(this);

        verifyCodeButton.setText(
                text(
                        "✅ تأیید کد",
                        "✅ Verify Code",
                        "✅ کوډ تایید کړئ",
                        "✅ کوڈ کی تصدیق کریں",
                        "✅ कोड सत्यापित करें"
                )
        );

        styleButton(verifyCodeButton);

        verifyCodeButton.setVisibility(
                View.GONE
        );

        layout.addView(
                verifyCodeButton
        );

        // ==========================================
        // ذخیره پروفایل
        // ==========================================

        saveProfileButton =
                new Button(this);

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

        layout.addView(
                saveProfileButton
        );

        // ==========================================
        // خروج
        // ==========================================

        logoutButton =
                new Button(this);

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

        layout.addView(
                logoutButton
        );

        // ==========================================
        // وضعیت
        // ==========================================

        statusText =
                new TextView(this);

        statusText.setTextSize(17);

        statusText.setGravity(
                Gravity.CENTER
        );

        statusText.setPadding(
                0,
                25,
                0,
                20
        );

        layout.addView(statusText);

        setContentView(layout);

        // حالت اولیه: ورود با ایمیل
        accountMode = 0;
        phoneMode = false;

        updateScreen();

        // ==========================================
        // ورود
        // ==========================================

        loginModeButton.setOnClickListener(v -> {

            accountMode = 0;

            verificationId = null;
            resendToken = null;
            phoneVerificationMode = 0;

            verificationCodeInput.setText("");

            updateScreen();
        });

        // ==========================================
        // ثبت نام
        // ==========================================

        registerModeButton.setOnClickListener(v -> {

            accountMode = 1;

            verificationId = null;
            resendToken = null;
            phoneVerificationMode = 0;

            verificationCodeInput.setText("");

            updateScreen();
        });

        // ==========================================
        // ایمیل
        // ==========================================

        emailModeButton.setOnClickListener(v -> {

            phoneMode = false;

            verificationId = null;
            resendToken = null;
            phoneVerificationMode = 0;

            verificationCodeInput.setText("");

            updateScreen();
        });

        // ==========================================
        // شماره تلفن
        // ==========================================

        phoneModeButton.setOnClickListener(v -> {

            phoneMode = true;

            verificationId = null;
            resendToken = null;
            phoneVerificationMode = 0;

            verificationCodeInput.setText("");

            updateScreen();
        });

        // ==========================================
        // ثبت نام
        // ==========================================

        registerButton.setOnClickListener(v ->
                registerUser()
        );

        // ==========================================
        // ورود
        // ==========================================

        loginButton.setOnClickListener(v ->
                loginUser()
        );

        // ==========================================
        // تأیید کد
        // ==========================================

        verifyCodeButton.setOnClickListener(v ->
                verifyPhoneCode()
        );

        // ==========================================
        // ذخیره پروفایل
        // ==========================================

        saveProfileButton.setOnClickListener(v ->
                saveProfile()
        );

        // ==========================================
        // خروج
        // ==========================================

        logoutButton.setOnClickListener(v -> {

            FirebaseUser user =
                    auth.getCurrentUser();

            if (user != null) {

                setUserOffline(
                        user.getUid()
                );
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

    // ==========================================
    // تنظیم ظاهر صفحه
    // ==========================================

    private void updateScreen() {

        if (accountMode == 0) {

            modeTitle.setText(
                    text(
                            "🔐 ورود به حساب",
                            "🔐 Login to Account",
                            "🔐 حساب ته ننوتل",
                            "🔐 اکاؤنٹ میں لاگ اِن",
                            "🔐 अकाउंट में लॉग इन"
                    )
            );

            registerButton.setVisibility(
                    View.GONE
            );

            loginButton.setVisibility(
                    View.VISIBLE
            );

        } else {

            modeTitle.setText(
                    text(
                            "📝 ساخت حساب جدید",
                            "📝 Create New Account",
                            "📝 نوی حساب جوړ کړئ",
                            "📝 نیا اکاؤنٹ بنائیں",
                            "📝 नया अकाउंट बनाएं"
                    )
            );

            registerButton.setVisibility(
                    View.VISIBLE
            );

            loginButton.setVisibility(
                    View.GONE
            );
        }

        if (phoneMode) {

            loginInput.setInputType(
                    InputType.TYPE_CLASS_PHONE
            );

            loginInput.setHint(
                    text(
                            "شماره تلفن خود را وارد کنید",
                            "Enter your phone number",
                            "خپل د تلیفون شمېره دننه کړئ",
                            "اپنا فون نمبر درج کریں",
                            "अपना फोन नंबर दर्ज करें"
                    )
            );

            passwordInput.setVisibility(
                    View.GONE
            );

        } else {

            loginInput.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            );

            loginInput.setHint(
                    text(
                            "ایمیل خود را وارد کنید",
                            "Enter your email",
                            "خپل برېښنالیک دننه کړئ",
                            "اپنا ای میل درج کریں",
                            "अपना ईमेल दर्ज करें"
                    )
            );

            passwordInput.setVisibility(
                    View.VISIBLE
            );

            verificationCodeInput.setVisibility(
                    View.GONE
            );

            verifyCodeButton.setVisibility(
                    View.GONE
            );
        }

        if (phoneMode) {

            if (verificationId != null) {

                verificationCodeInput.setVisibility(
                        View.VISIBLE
                );

                verifyCodeButton.setVisibility(
                        View.VISIBLE
                );

            } else {

                verificationCodeInput.setVisibility(
                        View.GONE
                );

                verifyCodeButton.setVisibility(
                        View.GONE
                );
            }
        }

        // نام نمایشی فقط برای ثبت نام لازم است
        if (accountMode == 1) {

            nameInput.setVisibility(
                    View.VISIBLE
            );

        } else {

            nameInput.setVisibility(
                    View.GONE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(
                appliedLanguage)) {

            recreate();
            return;
        }
    }

    // ==========================================
    // ثبت نام
    // ==========================================

    private void registerUser() {

        if (phoneMode) {

            registerWithPhone();

        } else {

            registerWithEmail();
        }
    }

    // ==========================================
    // ثبت نام با ایمیل
    // ==========================================

    private void registerWithEmail() {

        String email =
                loginInput.getText()
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
                    text(
                            "ایمیل و رمز ورود را وارد کنید",
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
                            "رمز ورود باید حداقل ۶ حرف باشد",
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

    // ==========================================
    // ثبت نام با شماره
    // ==========================================

    private void registerWithPhone() {

        String phone =
                normalizePhoneNumber(
                        loginInput.getText()
                                .toString()
                                .trim()
                );

        String name =
                nameInput.getText()
                        .toString()
                        .trim();

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "شماره تلفن را وارد کنید",
                            "Enter your phone number",
                            "د تلیفون شمېره دننه کړئ",
                            "فون نمبر درج کریں",
                            "फोन नंबर दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

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

        startPhoneVerification(
                phone,
                1
        );
    }

    // ==========================================
    // ورود
    // ==========================================

    private void loginUser() {

        if (phoneMode) {

            loginWithPhone();

        } else {

            loginWithEmail();
        }
    }

    // ==========================================
    // ورود با ایمیل
    // ==========================================

    private void loginWithEmail() {

        String email =
                loginInput.getText()
                        .toString()
                        .trim();

        String password =
                passwordInput.getText()
                        .toString();

        if (email.isEmpty()
                || password.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "ایمیل و رمز ورود را وارد کنید",
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

    // ==========================================
    // ورود با شماره
    // ==========================================

    private void loginWithPhone() {

        String phone =
                normalizePhoneNumber(
                        loginInput.getText()
                                .toString()
                                .trim()
                );

        if (phone.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "شماره تلفن را وارد کنید",
                            "Enter your phone number",
                            "د تلیفون شمېره دننه کړئ",
                            "فون نمبر درج کریں",
                            "फोन नंबर दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        startPhoneVerification(
                phone,
                2
        );
    }

    // ==========================================
    // ارسال کد SMS
    // mode:
    // 1 = ثبت نام
    // 2 = ورود
    // ==========================================

    private void startPhoneVerification(
            String phone,
            int mode
    ) {

        // حالت درخواست فعلی را قبل از ارسال SMS ذخیره می‌کنیم
        phoneVerificationMode = mode;

        verificationId = null;
        resendToken = null;

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(
                                60L,
                                TimeUnit.SECONDS
                        )
                        .setActivity(this)
                        .setCallbacks(
                                new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                                    @Override
                                    public void onVerificationCompleted(
                                            PhoneAuthCredential credential
                                    ) {

                                        // اگر درخواست دیگر معتبر نیست
                                        // آن را اجرا نمی‌کنیم.
                                        if (phoneVerificationMode != 1
                                                && phoneVerificationMode != 2) {
                                            return;
                                        }

                                        completePhoneLogin(
                                                credential,
                                                phoneVerificationMode
                                        );
                                    }
                                    
                                    @Override
public void onVerificationFailed(FirebaseException e) {

    String errorMessage = e.getMessage();

    if (errorMessage == null || errorMessage.trim().isEmpty()) {
        errorMessage = e.toString();
    }

    String errorClass = e.getClass().getName();

    String fullError =
            "CLASS:\n"
            + errorClass
            + "\n\nMESSAGE:\n"
            + errorMessage;

    new AlertDialog.Builder(AccountActivity.this)
            .setTitle(
                    text(
                            "خطای ورود با شماره",
                            "Phone Login Error",
                            "د تلیفون د ننوتلو تېروتنه",
                            "فون لاگ اِن کی خرابی",
                            "फ़ोन लॉगिन त्रुटि"
                    )
            )
            .setMessage(fullError)
            .setPositiveButton(
                    text(
                            "باشه",
                            "OK",
                            "سمه ده",
                            "ٹھیک ہے",
                            "ठीक है"
                    ),
                    null
            )
            .show();

    verificationId = null;
    resendToken = null;
    updateScreen();
}
                

                                    @Override
                                    public void onCodeSent(
                                            String id,
                                            PhoneAuthProvider.ForceResendingToken token
                                    ) {

                                        if (phoneVerificationMode != 1
                                                && phoneVerificationMode != 2) {
                                            return;
                                        }

                                        verificationId = id;
                                        resendToken = token;

                                        verificationCodeInput.setText("");

                                        updateScreen();

                                        Toast.makeText(
                                                AccountActivity.this,
                                                text(
                                                        "کد تأیید به شماره شما ارسال شد 📱",
                                                        "Verification code sent 📱",
                                                        "د تایید کوډ واستول شو 📱",
                                                        "تصدیقی کوڈ بھیج دیا گیا 📱",
                                                        "सत्यापन कोड भेजा गया 📱"
                                                ),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                }
                        )
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(
                options
        );
    }

    // ==========================================
    // تأیید کد SMS
    // ==========================================

    private void verifyPhoneCode() {

        String code =
                verificationCodeInput
                        .getText()
                        .toString()
                        .trim();

        if (verificationId == null
                || verificationId.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "ابتدا کد SMS را درخواست کنید",
                            "Request the SMS code first",
                            "لومړی د SMS کوډ وغواړئ",
                            "پہلے SMS کوڈ حاصل کریں",
                            "पहले SMS कोड प्राप्त करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (code.isEmpty()) {

            Toast.makeText(
                    this,
                    text(
                            "کد تأیید را وارد کنید",
                            "Enter the verification code",
                            "د تایید کوډ دننه کړئ",
                            "تصدیقی کوڈ درج کریں",
                            "सत्यापन कोड दर्ज करें"
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(
                        verificationId,
                        code
                );

        // از accountMode استفاده نمی‌کنیم.
        // همان حالت واقعی درخواست SMS استفاده می‌شود.
        int mode = phoneVerificationMode;

        if (mode != 1 && mode != 2) {

            Toast.makeText(
                    this,
                    text(
                            "درخواست تأیید شماره معتبر نیست. دوباره شماره را وارد کنید.",
                            "Phone verification request is no longer valid. Please request a new code.",
                            "د تلیفون د تایید غوښتنه نوره معتبره نه ده. بیا کوډ وغواړئ.",
                            "فون کی تصدیق کی درخواست اب معتبر نہیں۔ دوبارہ کوڈ حاصل کریں۔",
                            "फोन सत्यापन अनुरोध अब मान्य नहीं है। नया कोड प्राप्त करें।"
                    ),
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        completePhoneLogin(
                credential,
                mode
        );
    }

    // ==========================================
    // تکمیل ورود/ثبت نام شماره
    // ==========================================

    private void completePhoneLogin(
            PhoneAuthCredential credential,
            int mode
    ) {

        auth.signInWithCredential(
                        credential
                )
                .addOnSuccessListener(authResult -> {

                    FirebaseUser user =
                            auth.getCurrentUser();

                    if (user == null) {
                        return;
                    }

                    String name =
                            nameInput.getText()
                                    .toString()
                                    .trim();

                    // --------------------------------------
                    // ثبت نام با شماره
                    // --------------------------------------

                    if (mode == 1) {

                        if (name.isEmpty()) {

                            name = text(
                                    "کاربر",
                                    "User",
                                    "کارن",
                                    "صارف",
                                    "उपयोगकर्ता"
                            );
                        }

                        saveUserToFirestore(
                                user,
                                name
                        );

                        Toast.makeText(
                                AccountActivity.this,
                                text(
                                        "اکانت با شماره تلفن ساخته شد 🎉",
                                        "Phone account created 🎉",
                                        "د تلیفون اکاونټ جوړ شو 🎉",
                                        "فون اکاؤنٹ بن گیا 🎉",
                                        "फोन अकाउंट बनाया गया 🎉"
                                ),
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        // --------------------------------------
                        // ورود با شماره
                        // --------------------------------------

                        loadUserProfile(user);

                        setUserOnline(
                                user.getUid()
                        );

                        Toast.makeText(
                                AccountActivity.this,
                                text(
                                        "ورود با شماره موفق بود ✅",
                                        "Phone login successful ✅",
                                        "د تلیفون له لارې ننوتل بریالي شو ✅",
                                        "فون سے لاگ اِن کامیاب ہوا ✅",
                                        "फोन से लॉग इन सफल हुआ ✅"
                                ),
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    verificationId = null;
                    resendToken = null;
                    phoneVerificationMode = 0;

                    clearLoginFields();

                    updateScreen();

                    showCurrentUser();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "تأیید کد ناموفق: ",
                                    "Code verification failed: ",
                                    "د کوډ تایید ناکام شو: ",
                                    "کوڈ کی تصدیق ناکام: ",
                                    "कोड सत्यापन विफल: "
                            ) + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // ==========================================
    // تبدیل شماره به فرمت Firebase
    // ==========================================

    private String normalizePhoneNumber(
            String phone
    ) {

        if (phone == null) {
            return "";
        }

        phone =
                phone.trim()
                        .replace(" ", "")
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "");

        // افغانستان:
        // 07xxxxxxxxx
        // تبدیل به:
        // +937xxxxxxxxx

        if (phone.startsWith("07")) {

            phone =
                    "+93" +
                    phone.substring(1);

        } else if (phone.startsWith("0093")) {

            phone =
                    "+" +
                    phone.substring(2);

        } else if (phone.startsWith("937")) {

            phone =
                    "+" +
                    phone;

        }

        return phone;
    }

    // ==========================================
    // پاک کردن اطلاعات ورود
    // ==========================================

    private void clearLoginFields() {

        loginInput.setText("");

        passwordInput.setText("");

        verificationCodeInput.setText("");

        verificationId = null;

        resendToken = null;

        phoneVerificationMode = 0;
    }

    // ==========================================
    // ذخیره پروفایل
    // ==========================================

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

        data.put(
                "name",
                name
        );

        data.put(
                "email",
                user.getEmail()
        );

        data.put(
                "userId",
                user.getUid()
        );

        if (user.getPhoneNumber() != null) {

            data.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );
        }

        data.put(
                "updatedAt",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            AccountActivity.this,
                            text(
                                    "نام شما ذخیره شد ✅",
                                    "Your name was saved ✅",
                                    "ستاسو نوم خوندي شو ✅",
                                    "آپ کا نام محفوظ ہو گیا ہے ✅",
                                    "आपका नाम सेव हो गया है ✅"
                            ),
                            Toast.LENGTH_SHORT
                    );
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
                    );
                });
    }

    // ==========================================
    // ذخیره کاربر در Firestore
    // ==========================================

    private void saveUserToFirestore(
            FirebaseUser user,
            String name
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "name",
                name
        );

        data.put(
                "email",
                user.getEmail()
        );

        data.put(
                "userId",
                user.getUid()
        );

        if (user.getPhoneNumber() != null) {

            data.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );
        }

        data.put(
                "online",
                true
        );

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put(
                "typingTo",
                ""
        );

        db.collection("users")
                .document(user.getUid())
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    // ==========================================
    // آنلاین
    // ==========================================

    private void setUserOnline(
            String uid
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "online",
                true
        );

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        db.collection("users")
                .document(uid)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    // ==========================================
    // آفلاین
    // ==========================================

    private void setUserOffline(
            String uid
    ) {

        Map<String, Object> data =
                new HashMap<>();

        data.put(
                "online",
                false
        );

        data.put(
                "lastSeen",
                FieldValue.serverTimestamp()
        );

        data.put(
                "typingTo",
                ""
        );

        db.collection("users")
                .document(uid)
                .set(
                        data,
                        SetOptions.merge()
                );
    }

    // ==========================================
    // خواندن پروفایل
    // ==========================================

    private void loadUserProfile(
            FirebaseUser user
    ) {

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {

                    if (document.exists()) {

                        String name =
                                document.getString(
                                        "name"
                                );

                        if (name != null
                                && !name.isEmpty()) {

                            nameInput.setText(name);
                        }
                    }
                });
    }

    // ==========================================
    // نمایش وضعیت حساب
    // ==========================================

    private void showCurrentUser() {

        FirebaseUser user =
                auth.getCurrentUser();

        if (user != null) {

            loadUserProfile(user);

            setUserOnline(
                    user.getUid()
            );

            if (AppLockManager.isPersonalInfoHidden(
                    this
            )) {

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

                String phone =
                        user.getPhoneNumber();

                if (email != null
                        && !email.isEmpty()) {

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

                } else if (phone != null
                        && !phone.isEmpty()) {

                    statusText.setText(
                            text(
                                    "✅ وارد شده‌اید\n\n" +
                                    "🟢 آنلاین\n\n" +
                                    "شماره تلفن:\n" +
                                    phone,

                                    "✅ You are logged in\n\n" +
                                    "🟢 Online\n\n" +
                                    "Phone:\n" +
                                    phone,

                                    "✅ تاسو ننوتلي یاست\n\n" +
                                    "🟢 آنلاین\n\n" +
                                    "د تلیفون شمېره:\n" +
                                    phone,

                                    "✅ آپ لاگ اِن ہیں\n\n" +
                                    "🟢 آن لائن\n\n" +
                                    "فون نمبر:\n" +
                                    phone,

                                    "✅ आप लॉग इन हैं\n\n" +
                                    "🟢 ऑनलाइन\n\n" +
                                    "फोन नंबर:\n" +
                                    phone
                            )
                    );
                }
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

    // ==========================================
    // استایل دکمه
    // ==========================================

    private void styleButton(
            Button button
    ) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                themeColor
        );

        background.setCornerRadius(
                24
        );

        button.setBackground(
                background
        );

        button.setTextColor(
                Color.WHITE
        );
    }

    // ==========================================
    // رنگ پس‌زمینه
    // ==========================================

    private int getLightThemeColor() {

        int red =
                Color.red(themeColor);

        int green =
                Color.green(themeColor);

        int blue =
                Color.blue(themeColor);

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
