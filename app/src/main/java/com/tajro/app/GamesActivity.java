package com.tajro.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GamesActivity extends Activity {

    private int themeColor;
    private boolean nightMode;
    private int appliedThemeColor;
    private boolean appliedNightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeColor = ThemeManager.getThemeColor(this);
        nightMode = ThemeManager.isNightMode(this);
        appliedThemeColor = themeColor;
        appliedNightMode = nightMode;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        root.setBackgroundColor(getLightThemeColor());

        TextView title = new TextView(this);
        title.setText(tr(
                "🎮 بازی‌ها",
                "🎮 Games",
                "🎮 لوبې",
                "🎮 کھیل",
                "🎮 खेल",
                "🎮 الألعاب"
        ));
        title.setTextSize(30);
        title.setTextColor(themeColor);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(4), 0, dp(30));
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout ballGame = createGameCard(
                "⚽",
                tr("توپ در خانه‌ها", "Ball Game", "د توپ لوبه",
                        "گیند کا کھیل", "गेंद का खेल", "لعبة الكرة"),
                tr("توپ را با ◀️ ⬆️ ▶️ به خانه پایان برسان", "Guide the ball to the finish using ◀️ ⬆️ ▶️",
                        "توپ د ◀️ ⬆️ ▶️ په وسیله تر پای پورې ورسوه", "گیند کو ◀️ ⬆️ ▶️ سے اختتام تک پہنچائیں",
                        "◀️ ⬆️ ▶️ से गेंद को अंतिम स्थान तक पहुँचाएँ", "وجّه الكرة إلى نقطة النهاية باستخدام ◀️ ⬆️ ▶️")
        );
        root.addView(ballGame, createCardParams());

        LinearLayout colorSortGame = createGameCard(
                "🎨",
                tr("مرتب‌سازی رنگ‌ها", "Color Sort", "د رنګونو ترتیب",
                        "رنگوں کی ترتیب", "रंगों को सजाएँ", "ترتيب الألوان"),
                tr("رنگ‌ها را در شیشه‌ها مرتب کن", "Sort the colors into the bottles",
                        "رنګونه په بوتلونو کې منظم کړه", "رنگوں کو بوتلوں میں ترتیب دیں",
                        "रंगों को बोतलों में क्रम से सजाएँ", "رتّب الألوان داخل القوارير")
        );
        root.addView(colorSortGame, createCardParams());

        TextView divider = new TextView(this);
        divider.setText("──────────  ★  ──────────");
        divider.setTextColor(themeColor);
        divider.setTextSize(18);
        divider.setGravity(Gravity.CENTER);
        divider.setPadding(0, dp(12), 0, dp(10));
        root.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView info = new TextView(this);
        info.setText(tr(
                "بازی‌ها برای سرگرمی طراحی شده‌اند\nو می‌توانیم مراحل بیشتری به آن‌ها اضافه کنیم.",
                "These games are made for fun.\nMore levels can be added in the future.",
                "دا لوبې د ساتېرۍ لپاره جوړې شوې دي.\nپه راتلونکي کې نور پړاوونه هم ورزیاتېدای شي.",
                "یہ کھیل تفریح کے لیے بنائے گئے ہیں۔\nآئندہ مزید مراحل شامل کیے جا سکتے ہیں۔",
                "ये खेल मनोरंजन के लिए बनाए गए हैं।\nभविष्य में और स्तर जोड़े जा सकते हैं।",
                "صُممت هذه الألعاب للترفيه.\nيمكن إضافة المزيد من المراحل مستقبلًا."
        ));
        info.setTextSize(16);
        info.setTextColor(nightMode
                ? Color.rgb(210, 220, 232)
                : Color.rgb(48, 66, 76));
        info.setGravity(Gravity.CENTER);
        info.setLineSpacing(dp(5), 1.0f);
        info.setPadding(dp(4), dp(8), dp(4), 0);
        root.addView(info, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView gameDecoration = new TextView(this);
        gameDecoration.setText("🏆   🎮   ⭐");
        gameDecoration.setTextSize(42);
        gameDecoration.setGravity(Gravity.CENTER);
        gameDecoration.setPadding(0, dp(55), 0, 0);
        root.addView(gameDecoration, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);

        ballGame.setOnClickListener(v -> {
            Intent intent = new Intent(GamesActivity.this, BallGameActivity.class);
            startActivity(intent);
        });

        colorSortGame.setOnClickListener(v -> {
            Intent intent = new Intent(GamesActivity.this, ColorSortActivity.class);
            startActivity(intent);
        });
    }

    private LinearLayout createGameCard(
            String icon,
            String title,
            String description
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setBackground(createCardBackground());
        card.setElevation(dp(5));
        card.setClickable(true);
        card.setFocusable(true);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(34);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(createIconBackground());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(62), dp(62));
        iconParams.setMargins(0, 0, dp(12), 0);
        card.addView(iconView, iconParams);

        LinearLayout textArea = new LinearLayout(this);
        textArea.setOrientation(LinearLayout.VERTICAL);
        textArea.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(20);
        titleView.setTextColor(nightMode
                ? Color.rgb(245, 248, 252)
                : Color.WHITE);
        titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        textArea.addView(titleView);

        TextView descriptionView = new TextView(this);
        descriptionView.setText(description);
        descriptionView.setTextSize(12);
        descriptionView.setTextColor(nightMode
                ? Color.rgb(205, 216, 230)
                : Color.rgb(225, 246, 250));
        descriptionView.setGravity(Gravity.CENTER_VERTICAL);
        descriptionView.setPadding(0, dp(5), 0, 0);
        textArea.addView(descriptionView);

        card.addView(textArea, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        ));

        TextView arrow = new TextView(this);
        arrow.setText("‹");
        arrow.setTextSize(34);
        arrow.setTextColor(Color.WHITE);
        arrow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(dp(28), dp(48));
        arrowParams.setMargins(dp(8), 0, 0, 0);
        card.addView(arrow, arrowParams);

        return card;
    }

    private GradientDrawable createCardBackground() {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        lighten(themeColor, 18),
                        themeColor,
                        darken(themeColor, 18)
                }
        );
        if (nightMode) {
            background.setColors(new int[]{
                    blend(Color.rgb(35, 46, 62), themeColor, 0.28f),
                    Color.rgb(27, 37, 51),
                    Color.rgb(22, 30, 42)
            });
        }
        background.setCornerRadius(dp(25));
        background.setStroke(dp(2), nightMode
                ? themeColor
                : Color.rgb(104, 218, 235));
        return background;
    }

    private GradientDrawable createIconBackground() {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(nightMode
                ? Color.rgb(39, 52, 69)
                : Color.rgb(235, 250, 252));
        background.setStroke(dp(2), nightMode
                ? lighten(themeColor, 25)
                : Color.rgb(95, 211, 230));
        return background;
    }

    private LinearLayout.LayoutParams createCardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(112)
        );
        params.setMargins(0, 0, 0, dp(20));
        return params;
    }

    private int lighten(int color, int amount) {
        return Color.rgb(
                Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount),
                Math.min(255, Color.blue(color) + amount)
        );
    }

    private int darken(int color, int amount) {
        return Color.rgb(
                Math.max(0, Color.red(color) - amount),
                Math.max(0, Color.green(color) - amount),
                Math.max(0, Color.blue(color) - amount)
        );
    }

    // دریافت متن مطابق زبان انتخاب‌شده در برنامه
    private String tr(
            String fa,
            String en,
            String ps,
            String ur,
            String hi,
            String ar
    ) {
        String language = LanguageManager.getLanguage(this);

        switch (language) {
            case "en": return en;
            case "ps": return ps;
            case "ur": return ur;
            case "hi": return hi;
            case "ar": return ar;
            case "fa":
            default: return fa;
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int getLightThemeColor() {
        if (nightMode) {
            return Color.rgb(18, 25, 35);
        }

        int red = Color.red(themeColor);
        int green = Color.green(themeColor);
        int blue = Color.blue(themeColor);

        red = red + (255 - red) * 92 / 100;
        green = green + (255 - green) * 92 / 100;
        blue = blue + (255 - blue) * 92 / 100;

        return Color.rgb(red, green, blue);
    }

    private int blend(int color1, int color2, float ratio) {
        float inverse = 1.0f - ratio;
        return Color.rgb(
                (int) (Color.red(color1) * inverse + Color.red(color2) * ratio),
                (int) (Color.green(color1) * inverse + Color.green(color2) * ratio),
                (int) (Color.blue(color1) * inverse + Color.blue(color2) * ratio)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        int currentThemeColor = ThemeManager.getThemeColor(this);
        boolean currentNightMode = ThemeManager.isNightMode(this);

        if (appliedThemeColor != currentThemeColor
                || appliedNightMode != currentNightMode) {
            recreate();
        }
    }
}
