package com.tajro.app;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class TajroApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // =========================
        // Firebase
        // =========================
        try {

            FirebaseApp.initializeApp(this);

            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance();

            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );

            Log.d(
                    "Firebase",
                    "Firebase initialized successfully"
            );

        } catch (Exception e) {

            Log.e(
                    "Firebase",
                    "Firebase initialization error",
                    e
            );
        }

        // =========================
        // Unity Ads
        // =========================
        // موقتاً برای آزمایش کاملاً غیرفعال شده است.
        //
        // هیچ Unity Ads initialize نمی‌شود.
        // این فقط یک Build تشخیصی است.
        //
        // بعد از مشخص شدن نتیجه، این قسمت را
        // به حالت اصلی برمی‌گردانیم.

        Log.d(
                "UnityAds",
                "Unity Ads TEMPORARILY DISABLED FOR TEST"
        );
    }

    // =========================
    // ترجمه متن
    // =========================
    private String text(
            String fa,
            String en,
            String ps,
            String ur,
            String hi) {

        String language =
                LanguageManager.getLanguage(this);

        if ("en".equals(language)) {
            return en;
        }

        if ("ps".equals(language)) {
            return ps;
        }

        if ("ur".equals(language)) {
            return ur;
        }

        if ("hi".equals(language)) {
            return hi;
        }

        return fa;
    }
}
