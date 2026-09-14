package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

public class ThemeManager {

    private static final String PREF_NAME =
            "tajrobehha_settings";

    private static final String THEME_COLOR =
            "theme_color";

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
}
