package com.tajro.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

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
    private TextView supportDescription;
    private String appliedLanguage;
    private ImageView backgroundImage;

    private View experienceCard;
    private View aiCard;
    private View exchangeCard;
    private View settingsCard;
    private View rewardedCard;
    private View gameCard;

    private static final int NAVY = Color.rgb(25, 48, 65);
    private static final int NAVY_LIGHT = Color.rgb(43, 72, 88);
    private static final int TEAL = Color.rgb(133, 207, 190);
    private static final int WHITE = Color.WHITE;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private boolean isDeveloper() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return false;
            String email = user.getEmail();
            return email != null && DEVELOPER_EMAIL.equalsIgnoreCase(email.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private String text(String fa, String en, String ps, String ur, String hi, String ar) {
        String language = LanguageManager.getLanguage(this);
        if ("en".equals(language)) return en;
        if ("ps".equals(language)) return ps;
        if ("ur".equals(language)) return ur;
        if ("hi".equals(language)) return hi;
        if ("ar".equals(language)) return ar;
        return fa;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private ImageView createBackgroundImage() {
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.tajrobehha_background);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setAlpha(0.12f);
        imageView.setClickable(false);
        imageView.setFocusable(false);
        return imageView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appliedLanguage = LanguageManager.getLanguage(this);

        if (AppLockManager.hasPassword(this)
                && AppLockManager.isLockEnabled(this)
                && !AppLockManager.isSessionUnlocked(this)) {
            Intent lockIntent = new Intent(MainActivity.this, AppLockActivity.class);
            startActivity(lockIntent);
            finish();
            return;
        }

        rewardedAdManager = new RewardedAdManager(this);
        rewardedAdManager.loadRewardedAd();

        interstitialAdManager = new InterstitialAdManager(this);
        interstitialAdManager.loadInterstitialAd();

        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(ThemeManager.getBackgroundColor(this));

        backgroundImage = createBackgroundImage();
        rootLayout.addView(backgroundImage, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setClipToPadding(false);

        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(dp(18), dp(18), dp(18), dp(24));
        mainLayout.setBackgroundColor(Color.TRANSPARENT);

        scrollView.addView(mainLayout, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        buildHomeContent();

        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        rootLayout.addView(scrollView, contentParams);
        setContentView(rootLayout);

        applyTheme();
        requestNotificationPermission();
    }

    private void buildHomeContent() {
        // Brand header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(2), dp(2), dp(2), dp(14));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);

        title = makeText("iNtaj", 30, NAVY, true);
        title.setLetterSpacing(0.015f);
        welcome = makeText(
                text("به دنیای تجربه‌ها خوش آمدید",
                        "Welcome to your world of experiences",
                        "د تجربو نړۍ ته ښه راغلاست",
                        "تجربات کی دنیا میں خوش آمدید",
                        "अनुभवों की दुनिया में आपका स्वागत है",
                        "مرحباً بك في عالم التجارب"),
                13, Color.DKGRAY, false);
        welcome.setPadding(0, dp(1), 0, 0);
        brand.addView(title);
        brand.addView(welcome);

        TextView mark = makeText("N", 23, WHITE, true);
        mark.setGravity(Gravity.CENTER);
        GradientDrawable markBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{NAVY_LIGHT, NAVY});
        markBg.setCornerRadius(dp(15));
        mark.setBackground(markBg);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        markParams.setMargins(dp(10), 0, 0, 0);

        header.addView(brand, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(mark, markParams);
        mainLayout.addView(header);

        // Featured Experiences card
        experienceCard = createExperienceCard();
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(148));
        heroParams.setMargins(0, 0, 0, dp(14));
        mainLayout.addView(experienceCard, heroParams);

        // Two-column feature grid
        LinearLayout rowOne = createFeatureRow();
        aiCard = createFeatureCard(
                text("دستیار هوشمند", "AI Assistant", "هوښیار مرستیال",
                        "ذہین معاون", "स्मार्ट सहायक", "المساعد الذكي"),
                text("همراه پاسخ‌گوی شما", "Your helpful companion",
                        "ستاسو هوښیار ملګری", "آپ کا مددگار معاون",
                        "आपका सहायक", "مساعدك الذكي"),
                IconView.TYPE_AI, 0xFFE0EEE9, 0xFF245B52);
        rewardedCard = createFeatureCard(
                text("حمایت از تجربه‌ها", "Support iNtaj", "د تجربو ملاتړ",
                        "تجربات کی حمایت", "iNtaj का समर्थन", "دعم iNtaj"),
                text("همراه توسعه برنامه", "Help the app grow",
                        "د اپ پرمختګ ملاتړ", "ایپ کی ترقی میں مدد",
                        "ऐप के विकास में मदद", "ساهم في تطوير التطبيق"),
                IconView.TYPE_SUPPORT, 0xFFF1E4DA, 0xFF85583F);
        addWeighted(rowOne, aiCard);
        addWeighted(rowOne, rewardedCard);
        addRow(rowOne);

        LinearLayout rowTwo = createFeatureRow();
        exchangeCard = createFeatureCard(
                text("صرافی", "Exchange", "صرافي", "صرافی",
                        "मुद्रा विनिमय", "الصرافة"),
                text("نرخ و تبدیل ارز", "Currency rates & conversion",
                        "د اسعارو بدلون", "زرِ مبادلہ",
                        "मुद्रा दरें", "أسعار وتحويل العملات"),
                IconView.TYPE_EXCHANGE, 0xFFE1EAF5, 0xFF365D89);
        gameCard = createFeatureCard(
                text("بازی‌ها", "Games", "لوبې", "کھیل",
                        "खेल", "الألعاب"),
                text("کمی سرگرمی و شادی", "A little fun",
                        "ساتېري او خوښي", "تفریح اور خوشی",
                        "मनोरंजन", "قليل من المرح"),
                IconView.TYPE_GAMES, 0xFFECE4F3, 0xFF71538D);
        addWeighted(rowTwo, exchangeCard);
        addWeighted(rowTwo, gameCard);
        addRow(rowTwo);

        // Settings is a calm full-width strip
        settingsCard = createSettingsCard();
        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66));
        settingsParams.setMargins(0, dp(2), 0, dp(12));
        mainLayout.addView(settingsCard, settingsParams);

        supportDescription = makeText(
                text("با تماشای یک تبلیغ کوتاه از توسعهٔ برنامه حمایت کنید",
                        "Support app development by watching a short ad.",
                        "د لنډ اعلان په کتلو د اپ له پرمختګ ملاتړ وکړئ.",
                        "مختصر اشتہار دیکھ کر ایپ کی ترقی میں مدد کریں۔",
                        "छोटा विज्ञापन देखकर ऐप के विकास में मदद करें।",
                        "ادعم تطوير التطبيق بمشاهدة إعلان قصير"),
                12, Color.GRAY, false);
        supportDescription.setGravity(Gravity.CENTER);
        supportDescription.setPadding(dp(8), 0, dp(8), dp(8));
        mainLayout.addView(supportDescription);

        footer = makeText(
                text("iNtaj • یاد بگیر • شریک کن",
                        "iNtaj • Learn • Share",
                        "iNtaj • زده کړه • شریک یې کړه",
                        "iNtaj • سیکھیں • شیئر کریں",
                        "iNtaj • सीखें • साझा करें",
                        "iNtaj • تعلّم • شارك"),
                12, Color.GRAY, false);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, dp(8));
        mainLayout.addView(footer);

        // Keep all original navigation and ad behavior
        experienceCard.setOnClickListener(v -> {
            interstitialAdManager.showInterstitialIfAllowed();
            startActivity(new Intent(MainActivity.this, ExperienceListActivity.class));
        });

        aiCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AIActivity.class)));

        exchangeCard.setOnClickListener(v -> {
            interstitialAdManager.showInterstitialIfAllowed();
            startActivity(new Intent(MainActivity.this, ExchangeActivity.class));
        });

        settingsCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class)));

        gameCard.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GamesActivity.class)));

        rewardedCard.setOnClickListener(v -> showSupportAd());
    }

    private View createExperienceCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(rounded(NAVY, 23, 0x335CC9B1));
        card.setElevation(dp(4));
        card.setClickable(true);
        card.setFocusable(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_VERTICAL);

        TextView heading = makeText(
                text("دنیای تجربه‌ها", "Discover Experiences", "د تجربو نړۍ",
                        "تجربات کی دنیا", "अनुभवों की दुनिया", "عالم التجارب"),
                21, WHITE, true);
        TextView description = makeText(
                text("داستان‌ها، ایده‌ها و دیدگاه‌ها",
                        "Stories, ideas and perspectives",
                        "کیسې، نظرونه او لیدلوري",
                        "کہانیاں، خیالات اور نظریات",
                        "कहानियाँ, विचार और अनुभव",
                        "قصص وأفكار ووجهات نظر"),
                12, 0xFFD7E5E8, false);
        description.setPadding(0, dp(5), 0, dp(12));

        TextView action = makeText(
                text("مشاهده تجربه‌ها  ←", "Explore experiences  →",
                        "تجربې وګورئ  ←", "تجربات دیکھیں  ←",
                        "अनुभव देखें  →", "استكشف التجارب  ←"),
                13, NAVY, true);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(14), dp(8), dp(14), dp(8));
        action.setBackground(rounded(TEAL, 12, Color.TRANSPARENT));

        content.addView(heading);
        content.addView(description);
        content.addView(action);

        IconView icon = new IconView(this, IconView.TYPE_EXPERIENCE, TEAL);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        iconParams.setMargins(dp(12), 0, 0, 0);

        card.addView(content, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        card.addView(icon, iconParams);
        return card;
    }

    private LinearLayout createFeatureRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private void addWeighted(LinearLayout row, View child) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, dp(142), 1);
        params.setMargins(dp(4), dp(4), dp(4), dp(9));
        row.addView(child, params);
    }

    private void addRow(LinearLayout row) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mainLayout.addView(row, params);
    }

    private View createFeatureCard(String heading, String description,
                                   int iconType, int backgroundColor, int iconColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(10), dp(11));
        card.setBackground(rounded(backgroundColor, 19, 0x22000000));
        card.setElevation(dp(2));
        card.setClickable(true);
        card.setFocusable(true);

        FrameLayout iconHolder = new FrameLayout(this);
        iconHolder.setBackground(rounded(0x44FFFFFF, 15, 0x22000000));
        IconView icon = new IconView(this, iconType, iconColor);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                dp(48), dp(48), Gravity.CENTER);
        iconHolder.addView(icon, iconParams);

        LinearLayout.LayoutParams holderParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        holderParams.setMargins(0, 0, 0, dp(7));
        card.addView(iconHolder, holderParams);

        TextView titleView = makeText(heading, 14, iconColor, true);
        titleView.setMaxLines(1);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(titleView);

        TextView subtitle = makeText(description, 10, 0xFF53616B, false);
        subtitle.setMaxLines(1);
        subtitle.setSingleLine(true);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subParams.topMargin = dp(2);
        card.addView(subtitle, subParams);
        return card;
    }

    private View createSettingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(15), dp(8), dp(15), dp(8));
        card.setBackground(rounded(0xFFF0F2F3, 17, 0x22000000));
        card.setElevation(dp(1));
        card.setClickable(true);
        card.setFocusable(true);

        IconView icon = new IconView(this, IconView.TYPE_SETTINGS, 0xFF526B78);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        iconParams.setMargins(0, 0, dp(11), 0);
        card.addView(icon, iconParams);

        TextView label = makeText(
                text("تنظیمات برنامه", "App settings", "د اپ ترتیبات",
                        "ایپ کی ترتیبات", "ऐप सेटिंग्स", "إعدادات التطبيق"),
                15, 0xFF344B59, true);
        card.addView(label, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));

        TextView arrow = makeText("‹", 28, 0xFF526B78, false);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(30),
                ViewGroup.LayoutParams.MATCH_PARENT));
        return card;
    }

    private void showSupportAd() {
        rewardedAdManager.showRewardedAd(new RewardedAdManager.RewardListener() {
            @Override
            public void onRewarded() {
                // Viewing the ad supports app development; no reward is promised.
            }

            @Override
            public void onAdNotReady() {
                if (isDeveloper()) {
                    Toast.makeText(MainActivity.this,
                            text("⏳ تبلیغ هنوز آماده نیست، کمی بعد دوباره امتحان کنید.",
                                    "⏳ The ad is not ready yet. Try again shortly.",
                                    "⏳ اعلان لا چمتو نه دی، لږ وروسته هڅه وکړئ.",
                                    "⏳ اشتہار ابھی تیار نہیں، کچھ دیر بعد کوشش کریں۔",
                                    "⏳ विज्ञापन अभी तैयार नहीं है, थोड़ी देर बाद प्रयास करें।",
                                    "⏳ الإعلان غير جاهز، حاول بعد قليل."),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAdFailed() {
                if (isDeveloper()) {
                    Toast.makeText(MainActivity.this,
                            text("❌ نمایش تبلیغ ناموفق بود.",
                                    "❌ Ad failed to display.",
                                    "❌ د اعلان ښودل ناکام شول.",
                                    "❌ اشتہار دکھانے میں ناکامی ہوئی۔",
                                    "❌ विज्ञापन दिखाने में विफल रहा।",
                                    "❌ تعذّر عرض الإعلان."),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mainLayout != null) {
            String currentLanguage = LanguageManager.getLanguage(this);
            if (appliedLanguage != null && !currentLanguage.equals(appliedLanguage)) {
                appliedLanguage = currentLanguage;
                recreate();
                return;
            }
            applyTheme();
        }
    }

    private void applyTheme() {
        boolean night = ThemeManager.isNightMode(this);
        int backgroundColor = ThemeManager.getBackgroundColor(this);
        if (mainLayout == null) return;

        mainLayout.setBackgroundColor(Color.TRANSPARENT);
        if (backgroundImage != null) {
            backgroundImage.setAlpha(night ? 0.07f : 0.12f);
        }

        if (getWindow() != null) {
            getWindow().setStatusBarColor(night ? NAVY : 0xFFE9F0F0);
            getWindow().setNavigationBarColor(night ? NAVY : 0xFFE9F0F0);
            if (Build.VERSION.SDK_INT >= 23) {
                getWindow().getDecorView().setSystemUiVisibility(
                        night ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                                | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
        }

        if (title != null) title.setTextColor(night ? WHITE : NAVY);
        if (welcome != null) welcome.setTextColor(
                night ? 0xFFD3DEE2 : 0xFF53616B);
        if (footer != null) footer.setTextColor(night ? 0xFFB9C5CB : Color.GRAY);
        if (supportDescription != null) supportDescription.setTextColor(
                night ? 0xFFB9C5CB : Color.GRAY);

        if (night) {
            if (settingsCard != null) settingsCard.setBackground(
                    rounded(0xFF34434C, 17, 0x335CC9B1));
            if (settingsCard instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) settingsCard;
                for (int i = 0; i < row.getChildCount(); i++) {
                    View child = row.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(0xFFE8EFF1);
                    }
                }
            }
        } else if (settingsCard != null) {
            settingsCard.setBackground(rounded(0xFFF0F2F3, 17, 0x22000000));
            if (settingsCard instanceof LinearLayout) {
                LinearLayout row = (LinearLayout) settingsCard;
                for (int i = 0; i < row.getChildCount(); i++) {
                    View child = row.getChildAt(i);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(0xFF344B59);
                    }
                }
            }
        }

        if (getWindow() != null) getWindow().getDecorView().setBackgroundColor(backgroundColor);
    }

    /**
     * Small custom vector-like marks drawn in code.
     * No image assets or third-party icon libraries are required.
     */
    private static class IconView extends View {
        static final int TYPE_EXPERIENCE = 1;
        static final int TYPE_AI = 2;
        static final int TYPE_SUPPORT = 3;
        static final int TYPE_EXCHANGE = 4;
        static final int TYPE_GAMES = 5;
        static final int TYPE_SETTINGS = 6;

        private final int type;
        private final int accent;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        IconView(Context context, int type, int accent) {
            super(context);
            this.type = type;
            this.accent = accent;
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private void stroke(int color, float width) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(width);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
        }

        private void fill(int color) {
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth(), getHeight()) / 64f;
            canvas.save();
            canvas.translate((getWidth() - 64 * scale) / 2f,
                    (getHeight() - 64 * scale) / 2f);
            canvas.scale(scale, scale);

            int dark = 0xFF294958;
            int light = accent;
            paint.setShadowLayer(2.2f, 0, 2, 0x33000000);

            switch (type) {
                case TYPE_EXPERIENCE:
                    drawExperience(canvas, dark, light);
                    break;
                case TYPE_AI:
                    drawRobot(canvas, dark, light);
                    break;
                case TYPE_SUPPORT:
                    drawSupport(canvas, dark, light);
                    break;
                case TYPE_EXCHANGE:
                    drawExchange(canvas, dark, light);
                    break;
                case TYPE_GAMES:
                    drawGames(canvas, dark, light);
                    break;
                case TYPE_SETTINGS:
                    drawSettings(canvas, dark, light);
                    break;
            }
            paint.clearShadowLayer();
            canvas.restore();
        }

        private void drawExperience(Canvas c, int d, int a) {
            fill(0xFFBDE4D9);
            c.drawRoundRect(new RectF(10, 14, 48, 43), 8, 8, paint);
            fill(0xFFEFFFFA);
            c.drawRoundRect(new RectF(16, 20, 54, 49), 8, 8, paint);
            stroke(d, 2.5f);
            c.drawRoundRect(new RectF(16, 20, 54, 49), 8, 8, paint);
            path.reset();
            path.moveTo(25, 49); path.lineTo(22, 56); path.lineTo(34, 49);
            stroke(d, 2.5f); c.drawPath(path, paint);
            fill(a);
            path.reset();
            path.moveTo(36, 25); path.lineTo(39, 31); path.lineTo(46, 32);
            path.lineTo(41, 37); path.lineTo(42, 44); path.lineTo(36, 40);
            path.lineTo(30, 44); path.lineTo(31, 37); path.lineTo(26, 32);
            path.lineTo(33, 31); path.close();
            c.drawPath(path, paint);
        }

        private void drawRobot(Canvas c, int d, int a) {
            stroke(d, 3); c.drawLine(32, 8, 32, 15, paint);
            fill(a); c.drawCircle(32, 7, 4, paint);
            fill(0xFFEFFFFA); c.drawRoundRect(new RectF(11, 16, 53, 51), 12, 12, paint);
            stroke(d, 3); c.drawRoundRect(new RectF(11, 16, 53, 51), 12, 12, paint);
            fill(d); c.drawCircle(23, 31, 3.5f, paint); c.drawCircle(41, 31, 3.5f, paint);
            stroke(a, 3); c.drawArc(new RectF(23, 31, 41, 44), 20, 140, false, paint);
            stroke(d, 2.5f); c.drawLine(5, 29, 10, 29, paint); c.drawLine(54, 29, 59, 29, paint);
        }

        private void drawSupport(Canvas c, int d, int a) {
            stroke(d, 3.5f);
            path.reset();
            path.moveTo(8, 27); path.lineTo(18, 19); path.lineTo(28, 27);
            path.lineTo(32, 34); path.lineTo(36, 27); path.lineTo(46, 19);
            path.lineTo(56, 27); path.lineTo(48, 44); path.lineTo(32, 55);
            path.lineTo(16, 44); path.close();
            c.drawPath(path, paint);
            fill(a);
            path.reset();
            path.moveTo(32, 45); path.cubicTo(26, 40, 20, 35, 25, 30);
            path.cubicTo(28, 27, 31, 30, 32, 33);
            path.cubicTo(35, 27, 42, 27, 44, 33);
            path.cubicTo(46, 39, 38, 43, 32, 45);
            path.close(); c.drawPath(path, paint);
        }

        private void drawExchange(Canvas c, int d, int a) {
            fill(0xFFFFD98C); c.drawCircle(32, 32, 16, paint);
            stroke(d, 2.8f); c.drawCircle(32, 32, 16, paint);
            fill(d); paint.setTextSize(22); paint.setTypeface(Typeface.DEFAULT_BOLD);
            c.drawText("$", 25, 40, paint);
            stroke(a, 4);
            path.reset(); path.moveTo(8, 22); path.cubicTo(12, 7, 39, 4, 48, 17);
            c.drawPath(path, paint);
            path.reset(); path.moveTo(40, 10); path.lineTo(49, 17); path.lineTo(38, 20);
            path.close(); fill(a); c.drawPath(path, paint);
            stroke(a, 4);
            path.reset(); path.moveTo(56, 42); path.cubicTo(50, 57, 25, 60, 15, 47);
            c.drawPath(path, paint);
            path.reset(); path.moveTo(23, 54); path.lineTo(14, 47); path.lineTo(25, 44);
            path.close(); fill(a); c.drawPath(path, paint);
        }

        private void drawGames(Canvas c, int d, int a) {
            fill(0xFFE9E0F1); c.drawRoundRect(new RectF(7, 18, 57, 47), 12, 12, paint);
            stroke(d, 3); c.drawRoundRect(new RectF(7, 18, 57, 47), 12, 12, paint);
            stroke(a, 4); c.drawLine(18, 32, 30, 32, paint); c.drawLine(24, 26, 24, 38, paint);
            fill(d); c.drawCircle(42, 29, 3, paint); c.drawCircle(49, 36, 3, paint);
            fill(a); c.drawCircle(15, 14, 3, paint);
        }

        private void drawSettings(Canvas c, int d, int a) {
            stroke(d, 5);
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4.0;
                float x1 = 32 + (float)Math.cos(angle) * 19;
                float y1 = 32 + (float)Math.sin(angle) * 19;
                float x2 = 32 + (float)Math.cos(angle) * 25;
                float y2 = 32 + (float)Math.sin(angle) * 25;
                c.drawLine(x1, y1, x2, y2, paint);
            }
            fill(a); c.drawCircle(32, 32, 17, paint);
            fill(0xFFF0F2F3); c.drawCircle(32, 32, 7, paint);
        }
    }
}
