package com.tajro.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.unity3d.ads.InterstitialAd;
import com.unity3d.ads.InterstitialShowListener;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class InterstitialAdManager {

    private static final String TAG = "InterstitialAdManager";
    private static final String AD_UNIT_ID = "BP_Interstitial_Android";
    private static final String PREF_NAME = "tajro_interstitial_ads";
    private static final String LAST_SHOWN = "last_shown_time";
    private static final long ONE_DAY = 24L * 60L * 60L * 1000L;
    private static final long RETRY_DELAY_MS = 8000L;
    private static final int MAX_INIT_CHECKS = 20;

    private static final String DEVELOPER_EMAIL =
            "najibahmadi111zzz@gmail.com";

    private final Activity activity;
    private final SharedPreferences preferences;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private InterstitialAd interstitialAd;
    private boolean loading = false;
    private boolean showing = false;
    private boolean showWhenLoaded = false;
    private int initChecks = 0;

    public InterstitialAdManager(Activity activity) {
        this.activity = activity;
        preferences = activity.getSharedPreferences(
                PREF_NAME, Context.MODE_PRIVATE
        );
    }

    private boolean isDeveloper() {
        try {
            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            return user != null
                    && user.getEmail() != null
                    && DEVELOPER_EMAIL.equalsIgnoreCase(
                            user.getEmail().trim()
                    );

        } catch (Exception e) {
            Log.w(TAG, "DEVELOPER_CHECK_FAILED", e);
            return false;
        }
    }

    private void showMessage(String message) {
        if (!isDeveloper()) return;
        if (activity == null || activity.isFinishing()) return;

        handler.post(() -> {
            if (!activity.isFinishing()) {
                Toast.makeText(
                        activity,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // Load the ad, retrying if SDK initialization has not finished yet.
    public void loadInterstitialAd() {
        if (activity == null || activity.isFinishing()) return;

        if (!UnityAds.isInitialized()) {
            initChecks++;
            Log.w(TAG, "Unity Ads not initialized yet; check " + initChecks);

            if (initChecks <= MAX_INIT_CHECKS) {
                handler.postDelayed(this::loadInterstitialAd, 1500L);
            } else {
                Log.e(TAG,
                        "INTERSTITIAL_INIT_TIMEOUT: Unity Ads did not initialize");

                showMessage(
                        "Unity Ads آماده نشد؛ تبلیغ بینابینی بارگذاری نشد."
                );

                initChecks = 0;
            }
            return;
        }

        initChecks = 0;

        if (loading || interstitialAd != null || showing) return;

        loading = true;
        Log.i(TAG, "INTERSTITIAL_LOAD_START: Ad Unit=" + AD_UNIT_ID);

        try {
            LoadConfiguration loadConfig =
                    new LoadConfiguration.Builder(AD_UNIT_ID).build();

            InterstitialAd.load(loadConfig, (ad, error) -> {
                loading = false;

                if (ad != null) {
                    interstitialAd = ad;

                    Log.i(TAG,
                            "INTERSTITIAL_LOAD_SUCCESS: " + AD_UNIT_ID);

                    showMessage("تبلیغ بینابینی آماده است.");

                    ad.setOnAdExpired(expiredAd -> {
                        Log.w(TAG, "INTERSTITIAL_AD_EXPIRED");

                        if (interstitialAd == expiredAd) {
                            interstitialAd = null;
                        }

                        loadInterstitialAd();
                    });

                    if (showWhenLoaded) {
                        showWhenLoaded = false;
                        handler.postDelayed(this::showLoadedAd, 300L);
                    }

                } else {
                    interstitialAd = null;

                    String details = error == null
                            ? "Unity returned no ad and no error"
                            : error.getCode() + " — " + error.getMessage();

                    Log.e(TAG, "INTERSTITIAL_LOAD_FAILED: " + details);

                    showMessage(
                            "بارگذاری تبلیغ بینابینی ناموفق: " + details
                    );

                    handler.postDelayed(
                            this::loadInterstitialAd,
                            RETRY_DELAY_MS
                    );
                }
            });

        } catch (Exception e) {
            loading = false;

            Log.e(TAG, "INTERSTITIAL_LOAD_EXCEPTION", e);

            showMessage(
                    "خطای بارگذاری تبلیغ: "
                            + e.getClass().getSimpleName()
                            + " — " + String.valueOf(e.getMessage())
            );
        }
    }

    // Keep the existing once-per-24-hours limit.
    public void showInterstitialIfAllowed() {
        long now = System.currentTimeMillis();
        long lastShown = preferences.getLong(LAST_SHOWN, 0L);

        if (lastShown > 0L && now - lastShown < ONE_DAY) {
            Log.d(TAG, "INTERSTITIAL_BLOCKED_24H: lastShown=" + lastShown);

            showMessage(
                    "تبلیغ بینابینی به دلیل محدودیت ۲۴ ساعته نمایش داده نمی‌شود."
            );
            return;
        }

        if (showing) {
            Log.d(TAG, "INTERSTITIAL_ALREADY_SHOWING");

            showMessage("یک تبلیغ در حال نمایش است.");
            return;
        }

        if (interstitialAd == null) {
            Log.w(TAG,
                    "INTERSTITIAL_NOT_READY: starting load; will show when loaded");

            showWhenLoaded = true;

            showMessage("تبلیغ آماده نیست؛ در حال بارگذاری است.");

            loadInterstitialAd();
            return;
        }

        showLoadedAd();
    }

    private void showLoadedAd() {
        if (activity == null || activity.isFinishing()) return;
        if (showing) return;

        if (interstitialAd == null) {
            Log.w(TAG, "INTERSTITIAL_SHOW_REQUEST_BUT_NOT_READY");

            showMessage("تبلیغ هنوز آماده نیست؛ لطفاً دوباره امتحان کنید.");
            return;
        }

        InterstitialAd adToShow = interstitialAd;
        interstitialAd = null;
        showing = true;

        Log.i(TAG, "INTERSTITIAL_SHOW_START_REQUEST");

        try {
            ShowConfiguration showConfig =
                    new ShowConfiguration.Builder().build();

            adToShow.show(
                    activity,
                    showConfig,
                    new InterstitialShowListener() {

                        @Override
                        public void onStarted(InterstitialAd ad) {
                            Log.i(TAG, "INTERSTITIAL_SHOW_STARTED");

                            preferences.edit()
                                    .putLong(
                                            LAST_SHOWN,
                                            System.currentTimeMillis()
                                    )
                                    .apply();

                            showMessage("تبلیغ بینابینی شروع شد.");
                        }

                        @Override
                        public void onClicked(InterstitialAd ad) {
                            Log.d(TAG, "INTERSTITIAL_CLICKED");
                        }

                        @Override
                        public void onCompleted(
                                InterstitialAd ad,
                                ShowFinishState state
                        ) {
                            showing = false;

                            Log.i(TAG,
                                    "INTERSTITIAL_SHOW_COMPLETED: " + state);

                            loadInterstitialAd();
                        }

                        @Override
                        public void onFailed(
                                InterstitialAd ad,
                                UnityAdsError error
                        ) {
                            showing = false;

                            String details = error == null
                                    ? "Unity returned no error details"
                                    : error.getCode() + " — " + error.getMessage();

                            Log.e(TAG,
                                    "INTERSTITIAL_SHOW_FAILED: " + details);

                            showMessage(
                                    "نمایش تبلیغ بینابینی ناموفق: " + details
                            );

                            loadInterstitialAd();
                        }
                    }
            );

        } catch (Exception e) {
            showing = false;

            Log.e(TAG, "INTERSTITIAL_SHOW_EXCEPTION", e);

            showMessage(
                    "خطای نمایش تبلیغ: "
                            + e.getClass().getSimpleName()
                            + " — " + String.valueOf(e.getMessage())
            );

            loadInterstitialAd();
        }
    }

    public boolean isAdLoaded() {
        return interstitialAd != null;
    }
}
