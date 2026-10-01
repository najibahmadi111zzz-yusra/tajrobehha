package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

/**
 * فقط مسئول تشخیص ارز محلی دستگاه و ذخیره انتخاب دستی کاربر است.
 * هیچ نرخی را تغییر نمی‌دهد و به منطق معاملات/موجودی دست نمی‌زند.
 */
public final class LocalCurrencyManager {

    private static final String PREF_NAME = "tajro_local_currency";
    private static final String KEY_SELECTED_CURRENCY = "selected_currency";

    private LocalCurrencyManager() {
        // Utility class
    }

    /**
     * ارز انتخاب‌شده توسط کاربر را ذخیره می‌کند.
     */
    public static void saveSelectedCurrency(
            Context context,
            String currency
    ) {

        if (context == null || currency == null || currency.trim().isEmpty()) {
            return;
        }

        context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                )
                .edit()
                .putString(
                        KEY_SELECTED_CURRENCY,
                        currency
                )
                .apply();
    }

    /**
     * اگر کاربر قبلاً ارز را انتخاب کرده باشد همان ارز برمی‌گردد؛
     * در غیر این صورت ارز محلی دستگاه تشخیص داده می‌شود.
     */
    public static String getSelectedOrDetectedCurrency(
            Context context
    ) {

        if (context == null) {
            return ExchangeData.AFN;
        }

        SharedPreferences prefs =
                context.getApplicationContext()
                        .getSharedPreferences(
                                PREF_NAME,
                                Context.MODE_PRIVATE
                        );

        String saved =
                prefs.getString(
                        KEY_SELECTED_CURRENCY,
                        ""
                );

        if (isSupportedCurrency(saved)) {
            return saved;
        }

        return detectCurrencyFromDevice();
    }

    /**
     * تشخیص ارز بر اساس منطقه (Region) تنظیمات دستگاه.
     * نیازی به اینترنت یا اجازه موقعیت مکانی ندارد.
     */
    public static String detectCurrencyFromDevice() {

        String country =
                Locale.getDefault()
                        .getCountry();

        if (country == null) {
            return ExchangeData.AFN;
        }

        country = country.toUpperCase(Locale.US);

        switch (country) {

            case "AF":
                return ExchangeData.AFN;

            case "PK":
                return ExchangeData.PKR;

            case "IR":
                return ExchangeData.TOMAN;

            case "US":
            case "CA":
            case "AU":
            case "NZ":
                return ExchangeData.USD;

            case "IN":
                return ExchangeData.INR;

            case "TR":
                return ExchangeData.TRY;

            case "SA":
                return ExchangeData.SAR;

            case "AE":
            case "BH":
            case "OM":
            case "QA":
                return ExchangeData.AED;

            case "IQ":
                return ExchangeData.IQD;

            case "GB":
                return ExchangeData.GBP;

            case "AT":
            case "BE":
            case "CY":
            case "DE":
            case "EE":
            case "ES":
            case "FI":
            case "FR":
            case "GR":
            case "IE":
            case "IT":
            case "LT":
            case "LU":
            case "LV":
            case "MT":
            case "NL":
            case "PT":
            case "SI":
            case "SK":
                return ExchangeData.EUR;

            default:
                return ExchangeData.AFN;
        }
    }

    private static boolean isSupportedCurrency(
            String currency
    ) {

        if (currency == null || currency.trim().isEmpty()) {
            return false;
        }

        String value = currency.trim();

        String[] currencies =
                ExchangeData.getCurrencies();

        for (String item : currencies) {
            if (item.equals(value)) {
                return true;
            }
        }

        return false;
    }
}
