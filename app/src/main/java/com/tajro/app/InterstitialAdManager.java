package com.tajro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.InterstitialShowListener;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class InterstitialAdManager {

    private static final String TAG = "InterstitialAdManager";

    // شناسه دقیق تبلیغ بینابینی Unity
    private static final String AD_UNIT_ID =
            "BP_Interstitial_Android";

    // ذخیره زمان آخرین نمایش
    private static final String PREF_NAME =
            "tajro_interstitial_ads";

    private static final String LAST_SHOWN =
            "last_shown_time";

    // 24 ساعت
    private static final long ONE_DAY =
            24L * 60L * 60L * 1000L;

    private final Activity activity;

    private final SharedPreferences preferences;

    private InterstitialAd interstitialAd;

    public InterstitialAdManager(Activity activity) {

        this.activity = activity;

        preferences =
                activity.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // ==============================
    // بارگذاری تبلیغ
    // ==============================

    public void loadInterstitialAd() {

        if (!UnityAds.isInitialized()) {

            Log.e(
                    TAG,
                    "Unity Ads is not initialized"
            );

            return;
        }

        LoadConfiguration loadConfig =
                new LoadConfiguration.Builder(
                        AD_UNIT_ID
                ).build();

        InterstitialAd.load(
                loadConfig,
                (ad, error) -> {

                    if (ad != null) {

                        interstitialAd = ad;

                        Log.d(
                                TAG,
                                "Interstitial Ad Loaded"
                        );

                        interstitialAd.setOnAdExpired(
                                expiredAd -> {

                                    Log.d(
                                            TAG,
                                            "Interstitial Ad Expired"
                                    );

                                    interstitialAd = null;

                                    loadInterstitialAd();
                                }
                        );

                    } else {

                        interstitialAd = null;

                        String message =
                                error != null
                                        ? "کد "
                                        + error.getCode()
                                        + ": "
                                        + error.getMessage()
                                        : "Unknown error";

                        Log.e(
                                TAG,
                                "Interstitial Load Failed: "
                                        + message
                        );
                    }
                }
        );
    }

    // ==============================
    // نمایش تبلیغ
    // فقط یک بار در 24 ساعت
    // ==============================

    public void showInterstitialIfAllowed() {

        long now =
                System.currentTimeMillis();

        long lastShown =
                preferences.getLong(
                        LAST_SHOWN,
                        0
                );

        // اگر در 24 ساعت گذشته نمایش داده شده
        if (lastShown > 0
                && now - lastShown < ONE_DAY) {

            Log.d(
                    TAG,
                    "Interstitial blocked: "
                            + "24 hours not passed"
            );

            return;
        }

        // اگر تبلیغ هنوز آماده نیست
        if (interstitialAd == null) {

            Log.d(
                    TAG,
                    "Interstitial Ad is not ready"
            );

            loadInterstitialAd();

            return;
        }

        InterstitialAd adToShow =
                interstitialAd;

        // جلوگیری از نمایش دوباره همان تبلیغ
        interstitialAd = null;

        ShowConfiguration showConfig =
                new ShowConfiguration.Builder()
                        .build();

        adToShow.show(
                activity,
                showConfig,
                new InterstitialShowListener() {

                    @Override
                    public void onStarted(
                            InterstitialAd ad) {

                        Log.d(
                                TAG,
                                "Interstitial Ad Started"
                        );

                        // زمان نمایش موفق ثبت می‌شود
                        preferences.edit()
                                .putLong(
                                        LAST_SHOWN,
                                        System.currentTimeMillis()
                                )
                                .apply();
                    }

                    @Override
                    public void onClicked(
                            InterstitialAd ad) {

                        Log.d(
                                TAG,
                                "Interstitial Ad Clicked"
                        );
                    }

                    @Override
                    public void onCompleted(
                            InterstitialAd ad,
                            ShowFinishState state) {

                        Log.d(
                                TAG,
                                "Interstitial Ad Completed"
                        );

                        // تبلیغ بعدی را آماده کن
                        loadInterstitialAd();
                    }

                    @Override
                    public void onFailed(
                            InterstitialAd ad,
                            UnityAdsError error) {

                        String message =
                                error != null
                                        ? "کد "
                                        + error.getCode()
                                        + ": "
                                        + error.getMessage()
                                        : "Unknown error";

                        Log.e(
                                TAG,
                                "Interstitial Show Failed: "
                                        + message
                        );

                        Toast.makeText(
                                activity,
                                "❌ تبلیغ نمایش داده نشد",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadInterstitialAd();
                    }
                }
        );
    }

    // ==============================
    // بررسی آماده بودن تبلیغ
    // ==============================

    public boolean isAdLoaded() {

        return interstitialAd != null;
    }
}
