package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends Activity {

    private RewardedAdManager rewardedAdManager;
    private InterstitialAdManager interstitialAdManager;

    // ایمیل سازنده برنامه
    private static final String DEVELOPER_EMAIL =
            "najibahmadi111zzz@gmail.com";

    // عناصر صفحه
    private LinearLayout mainLayout;
    private TextView title;
    private TextView welcome;
    private TextView footer;

    private Button addButton;
    private Button listButton;
    private Button chatButton;
    private Button aiButton;
    private Button exchangeButton;
    private Button settingsButton;
    private Button rewardedButton;

    // تصویر پس‌زمینه
    private ImageView backgroundImage;

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

    // ==================================
    // بررسی اینکه کاربر سازنده است یا نه
    // ==================================

    private boolean isDeveloper() {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return false;
        }

        String email = user.getEmail();

        return email != null
                && DEVELOPER_EMAIL.equalsIgnoreCase(
                email.trim()
        );
    }

    // ==================================
    // ساخت دکمه
    // ==================================

    private Button createButton(String text) {

        Button button = new Button(this);

        button.setText(text);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);

        button.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        button.setGravity(Gravity.CENTER);

        styleButton(button);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        dp(65),
                        1
                );

        params.setMargins(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
        );

        button.setLayoutParams(params);

        return button;
    }

    // ==================================
    // ظاهر دکمه‌ها
    // ==================================

    private void styleButton(Button button) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                ThemeManager.getThemeColor(this)
        );

        background.setCornerRadius(
                dp(18)
        );

        button.setBackground(background);
        button.setTextColor(Color.WHITE);
    }

    // ==================================
    // ساخت تصویر پس‌زمینه
    // ==================================

    private ImageView createBackgroundImage() {

        ImageView imageView =
                new ImageView(this);

        // عکس موجود در drawable
        imageView.setImageResource(
                R.drawable.tajrobehha_background
        );

        /*
         * عکس کل صفحه را پر می‌کند.
         * CENTER_CROP باعث می‌شود فضای خالی
         * باقی نماند.
         */
        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        /*
         * شفافیت متوسط و ملایم
         * تا نوشته‌ها و دکمه‌ها واضح بمانند.
         */
        imageView.setAlpha(0.22f);

        /*
         * عکس فقط پس‌زمینه است
         * و قابل لمس نیست.
         */
        imageView.setClickable(false);
        imageView.setFocusable(false);

        return imageView;
    }

    // ==================================
    // onCreate
    // ==================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ==================================
        // قفل امنیتی برنامه
        // ==================================

        if (AppLockManager.hasPassword(this)
                && AppLockManager.isLockEnabled(this)
                && !AppLockManager.isSessionUnlocked(this)) {

            Intent lockIntent =
                    new Intent(
                            MainActivity.this,
                            AppLockActivity.class
                    );

            startActivity(lockIntent);
            finish();

            return;
        }

        // ==================================
        // Unity Rewarded Ad
        // ==================================

        rewardedAdManager =
                new RewardedAdManager(this);

        rewardedAdManager.loadRewardedAd();

        // ==================================
        // Unity Interstitial Ad
        // ==================================

        interstitialAdManager =
                new InterstitialAdManager(this);

        interstitialAdManager.loadInterstitialAd();

        // ==================================
        // لایه اصلی
        // ==================================

        FrameLayout rootLayout =
                new FrameLayout(this);

        rootLayout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        /*
         * زمینه اصلی را سفید/رنگی نمی‌کنیم
         * تا عکس پس‌زمینه دیده شود.
         */
        rootLayout.setBackgroundColor(
                Color.TRANSPARENT
        );

        // ==================================
        // عکس پس‌زمینه تجربه‌ها
        // ==================================

        backgroundImage =
                createBackgroundImage();

        FrameLayout.LayoutParams backgroundParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        backgroundParams.gravity =
                Gravity.CENTER;

        rootLayout.addView(
                backgroundImage,
                backgroundParams
        );

        // ==================================
        // ScrollView
        // ==================================

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setFillViewport(true);

        /*
         * اسکرول‌بار مزاحم نباشد
         */
        scrollView.setVerticalScrollBarEnabled(false);

        // ==================================
        // صفحه اصلی
        // ==================================

        mainLayout =
                new LinearLayout(this);

        mainLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        mainLayout.setPadding(
                dp(20),
                dp(35),
                dp(20),
                dp(25)
        );

        /*
         * کاملاً شفاف تا تصویر دیده شود.
         */
        mainLayout.setBackgroundColor(
                Color.TRANSPARENT
        );

        scrollView.addView(
                mainLayout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ==================================
        // لوگوی بالای صفحه
        // ==================================

        TextView logo =
                new TextView(this);

        logo.setText("📖💡");
        logo.setTextSize(52);
        logo.setGravity(Gravity.CENTER);

        // ==================================
        // عنوان
        // ==================================

        title =
                new TextView(this);

        title.setText("تجربه‌ها");
        title.setTextSize(34);

        title.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD
        );

        title.setGravity(
                Gravity.CENTER
        );

        title.setPadding(
                0,
                dp(5),
                0,
                dp(8)
        );

        // ==================================
        // خوش‌آمدگویی
        // ==================================

        welcome =
                new TextView(this);

        welcome.setText(
                "تجربه‌های خود را ثبت کنید و با دیگران شریک شوید"
        );

        welcome.setTextSize(17);
        welcome.setGravity(Gravity.CENTER);

        welcome.setPadding(
                dp(10),
                dp(5),
                dp(10),
                dp(30)
        );

        mainLayout.addView(logo);
        mainLayout.addView(title);
        mainLayout.addView(welcome);

        // ==================================
        // ردیف اول
        // ==================================

        LinearLayout row1 =
                new LinearLayout(this);

        row1.setOrientation(
                LinearLayout.HORIZONTAL
        );

        addButton =
                createButton("✍️ ثبت تجربه");

        listButton =
                createButton("📚 دیدن تجربه‌ها");

        row1.addView(addButton);
        row1.addView(listButton);

        // ==================================
        // ردیف دوم
        // ==================================

        LinearLayout row2 =
                new LinearLayout(this);

        row2.setOrientation(
                LinearLayout.HORIZONTAL
        );

        chatButton =
                createButton("💬 چت");

        aiButton =
                createButton("🤖 دستیار هوشمند");

        row2.addView(chatButton);
        row2.addView(aiButton);

        // ==================================
        // ردیف سوم
        // ==================================

        LinearLayout row3 =
                new LinearLayout(this);

        row3.setOrientation(
                LinearLayout.HORIZONTAL
        );

        exchangeButton =
                createButton("💱 صرافی");

        settingsButton =
                createButton("⚙️ تنظیمات");

        row3.addView(exchangeButton);
        row3.addView(settingsButton);

        // ==================================
        // تبلیغ جایزه‌ای
        // ==================================

        rewardedButton =
                createButton(
                        "🎁 تماشای تبلیغ و دریافت جایزه"
                );

        LinearLayout.LayoutParams rewardedParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(65)
                );

        rewardedParams.setMargins(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
        );

        rewardedButton.setLayoutParams(
                rewardedParams
        );

        // ==================================
        // اضافه کردن بخش‌ها
        // ==================================

        mainLayout.addView(row1);
        mainLayout.addView(row2);
        mainLayout.addView(row3);
        mainLayout.addView(rewardedButton);

        // ==================================
        // پایین صفحه
        // ==================================

        footer =
                new TextView(this);

        footer.setText(
                "تجربه‌ها • یاد بگیر • شریک کن"
        );

        footer.setTextSize(14);
        footer.setGravity(Gravity.CENTER);

        footer.setPadding(
                0,
                dp(30),
                0,
                dp(10)
        );

        mainLayout.addView(footer);

        // ==================================
        // ثبت تجربه
        // ==================================

        addButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AddExperienceActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // دیدن تجربه‌ها
        // ==================================

        listButton.setOnClickListener(v -> {

            interstitialAdManager
                    .showInterstitialIfAllowed();

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ExperienceListActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // چت
        // ==================================

        chatButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ChatActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // دستیار هوشمند
        // ==================================

        aiButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AIActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // صرافی
        // ==================================

        exchangeButton.setOnClickListener(v -> {

            interstitialAdManager
                    .showInterstitialIfAllowed();

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            ExchangeActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // تنظیمات
        // ==================================

        settingsButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            SettingsActivity.class
                    );

            startActivity(intent);
        });

        // ==================================
        // تبلیغ جایزه‌ای
        // ==================================

        rewardedButton.setOnClickListener(v -> {

            rewardedAdManager.showRewardedAd(
                    new RewardedAdManager.RewardListener() {

                        @Override
                        public void onRewarded() {

                            if (isDeveloper()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "🎉 جایزه شما فعال شد!",
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                        @Override
                        public void onAdNotReady() {

                            if (isDeveloper()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "⏳ تبلیغ هنوز آماده نیست، چند لحظه بعد دوباره امتحان کنید.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onAdFailed() {

                            if (isDeveloper()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "❌ نمایش تبلیغ ناموفق بود.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );
        });

        // ==================================
        // قرار دادن ScrollView روی صفحه
        // ==================================

        FrameLayout.LayoutParams contentParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );

        contentParams.gravity =
                Gravity.CENTER;

        rootLayout.addView(
                scrollView,
                contentParams
        );

        // ==================================
        // نمایش صفحه
        // ==================================

        setContentView(rootLayout);

        // ==================================
        // اعمال رنگ و حالت شب
        // ==================================

        applyTheme();
    }

    // ==================================
    // برگشت از تنظیمات
    // ==================================

    @Override
    protected void onResume() {
        super.onResume();

        if (mainLayout != null) {
            applyTheme();
        }
    }

    // ==================================
    // اعمال رنگ و حالت شب
    // ==================================

    private void applyTheme() {

        boolean night =
                ThemeManager.isNightMode(this);

        int themeColor =
                ThemeManager.getThemeColor(this);

        // ==================================
        // زمینه محتوا شفاف
        // ==================================

        mainLayout.setBackgroundColor(
                Color.TRANSPARENT
        );

        // ==================================
        // تنظیم عکس پس‌زمینه
        // ==================================

        if (backgroundImage != null) {

            backgroundImage.setAlpha(
                    0.22f
            );

            backgroundImage.setScaleX(
                    1.0f
            );

            backgroundImage.setScaleY(
                    1.0f
            );
        }

        // ==================================
        // عنوان
        // ==================================

        if (night) {

            title.setTextColor(
                    Color.WHITE
            );

        } else {

            title.setTextColor(
                    themeColor
            );
        }

        // ==================================
        // خوش‌آمدگویی
        // ==================================

        welcome.setTextColor(
                ThemeManager.getNormalTextColor(this)
        );

        // ==================================
        // پایین صفحه
        // ==================================

        if (night) {

            footer.setTextColor(
                    Color.LTGRAY
            );

        } else {

            footer.setTextColor(
                    Color.GRAY
            );
        }

        // ==================================
        // دکمه‌ها
        // ==================================

        styleButton(addButton);
        styleButton(listButton);
        styleButton(chatButton);
        styleButton(aiButton);
        styleButton(exchangeButton);
        styleButton(settingsButton);
        styleButton(rewardedButton);
    }
    }
