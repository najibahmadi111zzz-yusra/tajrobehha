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
        title.setText("🎮 بازی‌ها");
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
                "🎮 توپ در خانه‌ها",
                "توپ را با ◀️ ⬆️ ▶️ به خانه پایان برسان"
        );

        root.addView(
                ballGame,
                createButtonParams()
        );

        Button colorSortGame = createGameButton(
                "🎨 سورت رنگ‌ها",
                "رنگ‌ها را در شیشه‌ها مرتب کن"
        );

        root.addView(
                colorSortGame,
                createButtonParams()
        );

        TextView info = new TextView(this);
        info.setText(
                "بازی‌ها برای سرگرمی طراحی شده‌اند\n" +
                "و می‌توانیم مراحل بیشتری به آنها اضافه کنیم."
        );
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
