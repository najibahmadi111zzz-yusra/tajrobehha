package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LanguageManager {

    private static final String PREF_NAME =
            "tajrobehha_settings";

    private static final String LANGUAGE =
            "language_code";

    // ================================
    // ذخیره زبان
    // ================================

    public static void setLanguage(
            Context context,
            String languageCode) {

        context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        ).edit()
                .putString(
                        LANGUAGE,
                        languageCode
                )
                .apply();
    }

    // ================================
    // دریافت زبان
    // ================================

    public static String getLanguage(
            Context context) {

        return context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        ).getString(
                LANGUAGE,
                "fa"
        );
    }

    // ================================
    // اعمال زبان به Context
    // ================================

    public static Context applyLanguage(
            Context context) {

        String language =
                getLanguage(context);

        Locale locale =
                new Locale(language);

        Locale.setDefault(locale);

        Configuration configuration =
                new Configuration(
                        context.getResources()
                                .getConfiguration()
                );

        configuration.setLocale(locale);

        configuration.setLayoutDirection(
                locale
        );

        return context.createConfigurationContext(
                configuration
        );
    }

    // ================================
    // نام زبان
    // ================================

    public static String getLanguageName(
            String code) {

        if ("en".equals(code)) {
            return "English";
        }

        if ("ps".equals(code)) {
            return "پښتو";
        }

        if ("ur".equals(code)) {
            return "اردو";
        }

        if ("hi".equals(code)) {
            return "हिन्दी";
        }

        return "دری";
    }
}
