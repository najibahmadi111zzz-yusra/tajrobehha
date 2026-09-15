package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class ThemeManager {

    private static final String PREF_NAME =
            "tajrobehha_settings";

    private static final String THEME_COLOR =
            "theme_color";

    private static final String NIGHT_MODE =
            "night_mode";

    // رنگ انتخاب‌شده توسط کاربر
    public static int getThemeColor(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return preferences.getInt(
                THEME_COLOR,
                Color.rgb(12, 91, 120)
        );
    }

    // بررسی حالت شب
    public static boolean isNightMode(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return preferences.getBoolean(
                NIGHT_MODE,
                false
        );
    }

    // پس‌زمینه مناسب
    public static int getBackgroundColor(Context context) {

        if (isNightMode(context)) {

            return Color.rgb(25, 30, 35);

        } else {

            int themeColor =
                    getThemeColor(context);

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

    // رنگ متن اصلی
    public static int getTextColor(Context context) {

        if (isNightMode(context)) {

            return Color.WHITE;

        } else {

            return Color.rgb(8, 65, 90);
        }
    }

    // رنگ متن معمولی
    public static int getNormalTextColor(Context context) {

        if (isNightMode(context)) {

            return Color.rgb(235, 235, 235);

        } else {

            return Color.DKGRAY;
        }
    }
}
