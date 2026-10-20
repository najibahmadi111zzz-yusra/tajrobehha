
package com.tajro.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SplashActivity extends Activity {

    private static final String PREFS = "tajro_splash_prefs";
    private static final String KEY_SHOWN = "intro_shown";

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private boolean opened = false;

    private final Runnable openMain = () -> {
        if (isFinishing() || isDestroyed() || opened) {
            return;
        }

        opened = true;

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SHOWN, true)
                .apply();

        startActivity(new Intent(
                SplashActivity.this,
                MainActivity.class
        ));

        finish();
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(
                LanguageManager.applyLanguage(newBase)
        );
    }

    private int dp(int value) {
        return (int) (value *
                getResources().getDisplayMetrics().density);
    }

    private String text(
            String fa, String en, String ps,
            String ur, String hi) {

        String language = LanguageManager.getLanguage(this);

        if ("en".equals(language)) return en;
        if ("ps".equals(language)) return ps;
        if ("ur".equals(language)) return ur;
        if ("hi".equals(language)) return hi;

        return fa;
    }

    private TextView makeText(
            String value, int size, int color, boolean bold) {

        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setLineSpacing(dp(3), 1.0f);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private void goToMainImmediately() {
        if (opened) return;

        opened = true;

        startActivity(new Intent(
                SplashActivity.this,
                MainActivity.class
        ));

        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs =
                getSharedPreferences(PREFS, MODE_PRIVATE);

        if (prefs.getBoolean(KEY_SHOWN, false)) {
            goToMainImmediately();
            return;
        }

        getWindow().setStatusBarColor(Color.rgb(13, 54, 43));
        getWindow().setNavigationBarColor(Color.rgb(13, 54, 43));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(16), dp(24), dp(16));

        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {
                        Color.rgb(10, 43, 35),
                        Color.rgb(22, 83, 62),
                        Color.rgb(9, 39, 34)
                }
        );

        root.setBackground(background);

        TextView book = makeText(
                "📖", 68, Color.rgb(255, 220, 135), false);

        TextView welcome = makeText(
                text(
                        "به تجربه‌ها خوش آمدید",
                        "Welcome to Experiences",
                        "تجربو ته ښه راغلاست",
                        "تجربات میں خوش آمدید",
                        "अनुभवों में आपका स्वागत है"
                ),
                24, Color.rgb(255, 224, 153), true
        );

        welcome.setPadding(0, dp(8), 0, dp(8));

        TextView slogan = makeText(
                text(
                        "هر تجربه یک درس و هر داستان چراغیست",
                        "Every experience is a lesson, and every story is a light",
                        "هره تجربه یو درس او هره کیسه یو څراغ دی",
                        "ہر تجربہ ایک سبق اور ہر کہانی ایک چراغ ہے",
                        "हर अनुभव एक सीख है और हर कहानी एक दीपक है"
                ),
                17, Color.WHITE, true
        );

        TextView purpose = makeText(
                text(
                        "هدف ما گسترش آگاهی، یادگیری و تبادل تجربه‌های مفید است. با احترام و نیت نیک از این برنامه استفاده کنید و در نشر مطالب آموزنده سهم بگیرید.",
                        "Our goal is to spread awareness, learning, and useful experiences. Use this app respectfully and help share educational content.",
                        "زموږ موخه د پوهاوي، زده کړې او ګټورو تجربو خپرول دي. له اپلېکېشن څخه په درناوي ګټه واخلئ او ښوونیز مطالب خپاره کړئ.",
                        "ہمارا مقصد آگاہی، سیکھنے اور مفید تجربات کا تبادلہ ہے۔ ایپ کو احترام سے استعمال کریں اور مفید مواد پھیلائیں۔",
                        "हमारा उद्देश्य जागरूकता, सीखने और उपयोगी अनुभवों को साझा करना है। ऐप का सम्मानपूर्वक उपयोग करें और ज्ञानवर्धक सामग्री साझा करें।"
                ),
                14, Color.rgb(239, 246, 239), false
        );

        purpose.setPadding(0, dp(10), 0, dp(10));

        TextView responsibility = makeText(
                text(
                        "مسئولیت محتوای منتشرشده و نحوهٔ استفاده از برنامه بر عهدهٔ کاربران است.",
                        "Users are responsible for published content and how they use the app.",
                        "د خپرو شوو مطالبو او د اپلېکېشن د کارونې مسؤلیت د کاروونکو پر غاړه دی.",
                        "شائع کردہ مواد اور ایپ کے استعمال کی ذمہ داری صارفین پر ہے۔",
                        "प्रकाशित सामग्री और ऐप के उपयोग की ज़िम्मेदारी उपयोगकर्ताओं की है।"
                ),
                12, Color.rgb(220, 231, 220), false
        );

        responsibility.setPadding(0, dp(6), 0, dp(6));

        TextView closing = makeText(
                text(
                        "با هم بیاموزیم و دانایی را گسترش دهیم. 🌱",
                        "Let’s learn together and spread knowledge. 🌱",
                        "راځئ په ګډه زده کړه وکړو او پوهه خپره کړو. 🌱",
                        "آئیے مل کر سیکھیں اور علم کو فروغ دیں۔ 🌱",
                        "आइए मिलकर सीखें और ज्ञान फैलाएँ। 🌱"
                ),
                15, Color.rgb(255, 224, 153), true
        );

        closing.setPadding(0, dp(8), 0, 0);

        root.addView(book);
        root.addView(welcome);
        root.addView(slogan);
        root.addView(purpose);
        root.addView(responsibility);
        root.addView(closing);

        setContentView(root);

        handler.postDelayed(openMain, 4000);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openMain);
        super.onDestroy();
    }
}
