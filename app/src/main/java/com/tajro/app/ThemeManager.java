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

    private static final int DEFAULT_ACCENT = Color.rgb(36, 86, 166);

    // ==================================
    // رنگ انتخاب‌شده
    // ==================================

    public static int getThemeColor(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return preferences.getInt(
                THEME_COLOR,
                DEFAULT_ACCENT
        );
    }

    // ==================================
    // حالت شب
    // ==================================

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

    // ==================================
    // رنگ پس‌زمینه
    // ==================================

    public static int getBackgroundColor(Context context) {

        if (isNightMode(context)) {

            return Color.rgb(
                    16,
                    24,
                    39
            );

        } else {

            int themeColor =
                    getThemeColor(context);

            int red =
                    Color.red(themeColor);

            int green =
                    Color.green(themeColor);

            int blue =
                    Color.blue(themeColor);

            red =
                    red +
                    (255 - red) * 96 / 100;

            green =
                    green +
                    (255 - green) * 96 / 100;

            blue =
                    blue +
                    (255 - blue) * 96 / 100;

            return Color.rgb(
                    red,
                    green,
                    blue
            );
        }
    }

    // ==================================
    // رنگ متن اصلی
    // ==================================

    public static int getTextColor(Context context) {

        if (isNightMode(context)) {

            return Color.rgb(241, 245, 252);

        } else {

            return Color.rgb(
                    20,
                    36,
                    58
            );
        }
    }

    // ==================================
    // رنگ متن معمولی
    // ==================================

    public static int getNormalTextColor(Context context) {

        if (isNightMode(context)) {

            return Color.rgb(
                    183,
                    196,
                    214
            );

        } else {

            return Color.rgb(83, 99, 120);
        }
    }

    // ==========================================
    // رنگ سطح کارت‌ها و ورودی‌ها
    // ==========================================

    public static int getSurfaceColor(Context context) {
        if (isNightMode(context)) {
            return Color.rgb(32, 45, 64); // #202D40
        }
        return Color.WHITE;
    }

    // ==========================================
    // رنگ حاشیه کارت‌ها
    // ==========================================

    public static int getBorderColor(Context context) {
        if (isNightMode(context)) {
            return Color.rgb(52, 68, 91);
        }
        return Color.rgb(220, 228, 239);
    }

    // ==========================================
    // رنگ متن راهنمای فیلدها
    // ==========================================

    public static int getHintTextColor(Context context) {
        if (isNightMode(context)) {
            return Color.rgb(157, 174, 197);
        }
        return Color.rgb(112, 128, 150);
    }

}
