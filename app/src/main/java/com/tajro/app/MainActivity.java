package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
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

    private static final String DEVELOPER_EMAIL =
            "najibahmadi111zzz@gmail.com";

    private LinearLayout mainLayout;
    private TextView title;
    private TextView welcome;
    private TextView footer;
    private String appliedLanguage;

    private Button addButton;
    private Button listButton;
    private Button chatButton;
    private Button aiButton;
    private Button exchangeButton;
    private Button settingsButton;
    private Button rewardedButton;

    private Button gameButton;

    private ImageView backgroundImage;

    // ==================================
    // اعمال زبان قبل از ساخت صفحه
    // ==================================

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    private int dp(int value) {
        return (int) (
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    // ==================================
    // بررسی سازنده برنامه
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
    // متن بر اساس زبان
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
    // تصویر پس‌زمینه
    // ==================================

    private ImageView createBackgroundImage() {

        ImageView imageView =
                new ImageView(this);

        imageView.setImageResource(
                R.drawable.tajrobehha_background
        );

        imageView.setScaleType(
                ImageView.ScaleType.CENTER_CROP
        );

        imageView.setAlpha(1.0f);

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
        
        appliedLanguage = LanguageManager.getLanguage(this);

        // ==================================
        // قفل امنیتی
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

        rootLayout.setBackgroundColor(
                Color.TRANSPARENT
        );

        // ==================================
        // عکس پس‌زمینه
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

        title.setText(
                text(
                        "تجربه‌ها",
                        "Experiences",
                        "تجربې",
                        "تجربات",
                        "अनुभव"
                )
        );

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
                text(
                        "تجربه‌های خود را ثبت کنید و با دیگران شریک شوید",
                        "Share your experiences and learn from others",
                        "خپلې تجربې ثبت کړئ او له نورو سره یې شریکې کړئ",
                        "اپنے تجربات درج کریں اور دوسروں کے ساتھ شیئر کریں",
                        "अपने अनुभव दर्ज करें और दूसरों के साथ साझा करें"
                )
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
                createButton(
                        text(
                                "✍️ ثبت تجربه",
                                "✍️ Add Experience",
                                "✍️ تجربه ثبتول",
                                "✍️ تجربہ درج کریں",
                                "✍️ अनुभव दर्ज करें"
                        )
                );

        listButton =
                createButton(
                        text(
                                "📚 دیدن تجربه‌ها",
                                "📚 View Experiences",
                                "📚 تجربې کتل",
                                "📚 تجربات دیکھیں",
                                "📚 अनुभव देखें"
                        )
                );

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
                createButton(
                        text(
                                "💬 چت",
                                "💬 Chat",
                                "💬 خبرې",
                                "💬 چیٹ",
                                "💬 चैट"
                        )
                );

        aiButton =
                createButton(
                        text(
                                "🤖 دستیار هوشمند",
                                "🤖 AI Assistant",
                                "🤖 هوښیار مرستیال",
                                "🤖 ذہین معاون",
                                "🤖 स्मार्ट सहायक"
                        )
                );

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
                createButton(
                        text(
                                "💱 صرافی",
                                "💱 Exchange",
                                "💱 صرافي",
                                "💱 صرافی",
                                "💱 मुद्रा विनिमय"
                        )
                );

        settingsButton =
                createButton(
                        text(
                                "⚙️ تنظیمات",
                                "⚙️ Settings",
                                "⚙️ ترتیبات",
                                "⚙️ ترتیبات",
                                "⚙️ सेटिंग्स"
                        )
                );

        row3.addView(exchangeButton);
        row3.addView(settingsButton);

        // ==================================
        // بازی
        // ==================================

        gameButton =
                createButton(
                        text(
                                "🎮 بازی توپ در خانه‌ها",
                                "🎮 Ball Game",
                                "🎮 د توپ لوبه",
                                "🎮 گیند کا کھیل",
                                "🎮 गेंद का खेल"
                        )
                );

        LinearLayout.LayoutParams gameParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(65)
                );

        gameParams.setMargins(
                dp(6),
                dp(6),
                dp(6),
                dp(6)
        );

        gameButton.setLayoutParams(
                gameParams
        );

        // ==================================
        // تبلیغ جایزه‌ای
        // ==================================

        rewardedButton =
                createButton(
                        text(
                                "🎁 تماشای تبلیغ و دریافت جایزه",
                                "🎁 Watch Ad & Get Reward",
                                "🎁 اعلان وګورئ او جایزه ترلاسه کړئ",
                                "🎁 اشتہار دیکھیں اور انعام حاصل کریں",
                                "🎁 विज्ञापन देखें और पुरस्कार पाएं"
                        )
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
        mainLayout.addView(gameButton);
        mainLayout.addView(rewardedButton);

        // ==================================
        // پایین صفحه
        // ==================================

        footer =
                new TextView(this);

        footer.setText(
                text(
                        "تجربه‌ها • یاد بگیر • شریک کن",
                        "Experiences • Learn • Share",
                        "تجربې • زده کړه • شریک یې کړه",
                        "تجربات • سیکھیں • شیئر کریں",
                        "अनुभव • सीखें • साझा करें"
                )
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
        // بازی
        // ==================================

        gameButton.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            GamesActivity.class
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
                                        text(
                                                "🎉 جایزه شما فعال شد!",
                                                "🎉 Your reward is active!",
                                                "🎉 ستاسو جایزه فعاله شوه!",
                                                "🎉 آپ کا انعام فعال ہوگیا!",
                                                "🎉 आपका पुरस्कार सक्रिय हो गया!"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        }

                        @Override
                        public void onAdNotReady() {

                            if (isDeveloper()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        text(
                                                "⏳ تبلیغ هنوز آماده نیست، چند لحظه بعد دوباره امتحان کنید.",
                                                "⏳ The ad is not ready yet. Try again shortly.",
                                                "⏳ اعلان لا تر اوسه چمتو نه دی، لږ وروسته بیا هڅه وکړئ.",
                                                "⏳ اشتہار ابھی تیار نہیں، کچھ دیر بعد دوبارہ کوشش کریں۔",
                                                "⏳ विज्ञापन अभी तैयार नहीं है, थोड़ी देर बाद फिर कोशिश करें।"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }

                        @Override
                        public void onAdFailed() {

                            if (isDeveloper()) {

                                Toast.makeText(
                                        MainActivity.this,
                                        text(
                                                "❌ نمایش تبلیغ ناموفق بود.",
                                                "❌ Ad failed to display.",
                                                "❌ د اعلان ښودل ناکام شول.",
                                                "❌ اشتہار دکھانے میں ناکامی ہوئی۔",
                                                "❌ विज्ञापन दिखाने में विफल रहा।"
                                        ),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
            );
        });

        // ==================================
        // قرار دادن ScrollView
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

        String currentLanguage =
                LanguageManager.getLanguage(this);

        if (appliedLanguage != null
                && !currentLanguage.equals(appliedLanguage)) {

            appliedLanguage = currentLanguage;

            recreate();
            return;
        }

        applyTheme();
    }
}

    // ==================================
    // زبان فعلی صفحه
    // ==================================

    private String getAppliedLanguage() {

        return getSharedPreferences(
                "tajrobehha_settings",
                MODE_PRIVATE
        ).getString(
                "main_activity_language",
                LanguageManager.getLanguage(this)
        );
    }

    // ==================================
    // اعمال رنگ و حالت شب
    // ==================================

    private void applyTheme() {

        boolean night =
                ThemeManager.isNightMode(this);

        int themeColor =
                ThemeManager.getThemeColor(this);

        mainLayout.setBackgroundColor(
                Color.TRANSPARENT
        );

        if (backgroundImage != null) {

            backgroundImage.setAlpha(
                    0.35f
            );

            backgroundImage.setScaleX(
                    1.0f
            );

            backgroundImage.setScaleY(
                    1.0f
            );
        }

        if (night) {

            title.setTextColor(
                    Color.WHITE
            );

        } else {

            title.setTextColor(
                    themeColor
            );
        }

        welcome.setTextColor(
                ThemeManager.getNormalTextColor(this)
        );

        if (night) {

            footer.setTextColor(
                    Color.LTGRAY
            );

        } else {

            footer.setTextColor(
                    Color.GRAY
            );
        }

        styleButton(addButton);
        styleButton(listButton);
        styleButton(chatButton);
        styleButton(aiButton);
        styleButton(exchangeButton);
        styleButton(settingsButton);
        styleButton(gameButton);
        styleButton(rewardedButton);
    }
            }
