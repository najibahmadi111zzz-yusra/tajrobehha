package com.tajro.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GamesActivity extends Activity {

    private int themeColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        themeColor = ThemeManager.getThemeColor(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(30, 45, 30, 35);
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
        title.setPadding(0, 0, 0, 40);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        Button ballGame = createGameButton(
                tr("🎮 توپ در خانه‌ها", "🎮 Ball Game", "🎮 د توپ لوبه",
                        "🎮 گیند کا کھیل", "🎮 गेंद का खेल", "🎮 لعبة الكرة"),
                tr("توپ را با ◀️ ⬆️ ▶️ به خانه پایان برسان",
                        "Guide the ball to the finish using ◀️ ⬆️ ▶️",
                        "توپ د ◀️ ⬆️ ▶️ په وسیله تر پای پورې ورسوه",
                        "گیند کو ◀️ ⬆️ ▶️ سے اختتام تک پہنچائیں",
                        "◀️ ⬆️ ▶️ से गेंद को अंतिम स्थान तक पहुँचाएँ",
                        "وجّه الكرة إلى نقطة النهاية باستخدام ◀️ ⬆️ ▶️")
        );

        root.addView(
                ballGame,
                createButtonParams()
        );

        Button colorSortGame = createGameButton(
                tr("🎨 مرتب‌سازی رنگ‌ها", "🎨 Color Sort", "🎨 د رنګونو ترتیب",
                        "🎨 رنگوں کی ترتیب", "🎨 रंगों को सजाएँ", "🎨 ترتيب الألوان"),
                tr("رنگ‌ها را در شیشه‌ها مرتب کن",
                        "Sort the colors into the bottles",
                        "رنګونه په بوتلونو کې منظم کړه",
                        "رنگوں کو بوتلوں میں ترتیب دیں",
                        "रंगों को बोतलों में क्रम से सजाएँ",
                        "رتّب الألوان داخل القوارير")
        );

        root.addView(
                colorSortGame,
                createButtonParams()
        );

        TextView info = new TextView(this);
        info.setText(tr(
                "بازی‌ها برای سرگرمی طراحی شده‌اند\\nو می‌توانیم مراحل بیشتری به آنها اضافه کنیم.",
                "These games are made for fun.\\nMore levels can be added in the future.",
                "دا لوبې د ساتېرۍ لپاره جوړې شوې دي.\\nپه راتلونکي کې نور پړاوونه هم ورزیاتېدای شي.",
                "یہ کھیل تفریح کے لیے بنائے گئے ہیں۔\\nآئندہ مزید مراحل شامل کیے جا سکتے ہیں۔",
                "ये खेल मनोरंजन के लिए बनाए गए हैं।\\nभविष्य में और स्तर जोड़े जा सकते हैं।",
                "صُممت هذه الألعاب للترفيه.\\nيمكن إضافة المزيد من المراحل مستقبلًا."
        ));
        info.setTextSize(16);
        info.setTextColor(Color.DKGRAY);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 35, 0, 0);

        root.addView(
                info,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);

        ballGame.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            GamesActivity.this,
                            BallGameActivity.class
                    );

            startActivity(intent);
        });

        colorSortGame.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            GamesActivity.this,
                            ColorSortActivity.class
                    );

            startActivity(intent);
        });
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

    private Button createGameButton(
            String title,
            String description
    ) {

        Button button = new Button(this);

        button.setText(
                title +
                        "\n" +
                        description
        );

        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setAllCaps(false);

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(themeColor);
        background.setCornerRadius(28);

        button.setBackground(background);

        return button;
    }

    private LinearLayout.LayoutParams createButtonParams() {

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        120
                );

        params.setMargins(
                0,
                0,
                0,
                25
        );

        return params;
    }

    private int getLightThemeColor() {

        int red = Color.red(themeColor);
        int green = Color.green(themeColor);
        int blue = Color.blue(themeColor);

        red = red + (255 - red) * 92 / 100;
        green = green + (255 - green) * 92 / 100;
        blue = blue + (255 - blue) * 92 / 100;

        return Color.rgb(
                red,
                green,
                blue
        );
    }
}
