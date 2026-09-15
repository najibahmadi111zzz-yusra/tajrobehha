package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private RewardedAdManager rewardedAdManager;
    private InterstitialAdManager interstitialAdManager;

    // عناصر صفحه برای تغییر فوری رنگ و حالت شب
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

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density);
    }

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
        // صفحه اصلی
        // ==================================

        ScrollView scrollView =
                new ScrollView(this);

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

        scrollView.addView(
                mainLayout,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        // ==================================
        // لوگو
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

                            Toast.makeText(
                                    MainActivity.this,
                                    "🎉 جایزه شما فعال شد!",
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                        @Override
                        public void onAdNotReady() {

                            Toast.makeText(
                                    MainActivity.this,
                                    "⏳ تبلیغ هنوز آماده نیست، چند لحظه بعد دوباره امتحان کنید.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                        @Override
                        public void onAdFailed() {

                            Toast.makeText(
                                    MainActivity.this,
                                    "❌ نمایش تبلیغ ناموفق بود.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        });

        // ==================================
        // نمایش صفحه
        // ==================================

        setContentView(scrollView);

        // اعمال رنگ و حالت شب
        applyTheme();
    }

    // ==================================
    // برگشت از تنظیمات
    // رنگ و حالت شب فوراً اعمال می‌شود
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

        // پس‌زمینه اصلی
        mainLayout.setBackgroundColor(
                ThemeManager.getBackgroundColor(this)
        );

        // عنوان
        if (night) {
            title.setTextColor(Color.WHITE);
        } else {
            title.setTextColor(themeColor);
        }

        // متن خوش‌آمدگویی
        welcome.setTextColor(
                ThemeManager.getNormalTextColor(this)
        );

        // پایین صفحه
        if (night) {
            footer.setTextColor(
                    Color.LTGRAY
            );
        } else {
            footer.setTextColor(
                    Color.GRAY
            );
        }

        // همه دکمه‌ها
        styleButton(addButton);
        styleButton(listButton);
        styleButton(chatButton);
        styleButton(aiButton);
        styleButton(exchangeButton);
        styleButton(settingsButton);
        styleButton(rewardedButton);
    }
            }
