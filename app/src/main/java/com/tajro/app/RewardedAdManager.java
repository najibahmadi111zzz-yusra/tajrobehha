package com.tajro.app;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.RewardedAd;
import com.unity3d.ads.RewardedShowListener;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    // ایمیل سازنده برنامه
    private static final String DEVELOPER_EMAIL =
            "najibahmadi111zzz@gmail.com";

    // شناسه دقیق جایگاه تبلیغ Rewarded در Unity
    private static final String AD_UNIT_ID =
            "BP_Rewarded_Android";

    private final Activity activity;

    private RewardedAd rewardedAd;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isLoading = false;
    private boolean showWhenLoaded = false;
    private int initializationRetries = 0;

    // نگهداری شنونده تا تبلیغ پس از بارگذاری نمایش داده شود
    private RewardListener pendingRewardListener;

    public RewardedAdManager(Activity activity) {
        this.activity = activity;
    }

    // فقط سازنده پیام‌های فنی را می‌بیند
    private boolean isDeveloper() {
        try {
            FirebaseUser user =
                    FirebaseAuth.getInstance().getCurrentUser();

            if (user == null || user.getEmail() == null) {
                return false;
            }

            return DEVELOPER_EMAIL.equalsIgnoreCase(
                    user.getEmail().trim()
            );

        } catch (Exception e) {
            Log.w(TAG, "DEVELOPER_CHECK_FAILED", e);
            return false;
        }
    }

    private void developerToast(String message, int duration) {
        if (!isDeveloper()) {
            return;
        }

        if (activity == null || activity.isFinishing()) {
            return;
        }

        handler.post(() -> {
            if (isDeveloper()
                    && !activity.isFinishing()
                    && !activity.isDestroyed()) {

                Toast.makeText(
                        activity,
                        message,
                        duration
                ).show();
            }
        });
    }

    // بارگذاری تبلیغ
    public void loadRewardedAd() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            Log.w(TAG, "Activity is finishing; load cancelled");
            return;
        }

        if (!UnityAds.isInitialized()) {
            Log.e(TAG, "Unity Ads is not initialized yet");

            if (initializationRetries == 0) {
                developerToast(
                        "⏳ Unity هنوز آماده نیست؛ دوباره تلاش می‌شود.",
                        Toast.LENGTH_SHORT
                );
            }

            if (initializationRetries < 20) {
                initializationRetries++;
                handler.postDelayed(this::loadRewardedAd, 1500);
            } else {
                Log.e(TAG, "Unity initialization wait timed out");

                developerToast(
                        "❌ Unity آماده نشد؛ گزارش خطا را بررسی کنید.",
                        Toast.LENGTH_LONG
                );

                initializationRetries = 0;
            }
            return;
        }

        initializationRetries = 0;

        if (isLoading || rewardedAd != null) {
            Log.d(TAG, "Load skipped: already loading or ad is ready");
            return;
        }

        isLoading = true;
        Log.i(TAG, "Starting rewarded load; Ad Unit ID=" + AD_UNIT_ID);

        try {
            LoadConfiguration loadConfig =
                    new LoadConfiguration.Builder(AD_UNIT_ID).build();

            RewardedAd.load(loadConfig, (ad, error) -> {
                isLoading = false;

                if (ad != null) {
                    rewardedAd = ad;

                    Log.i(TAG, "REWARDED_LOAD_SUCCESS: " + AD_UNIT_ID);

                    developerToast(
                            "✅ تبلیغ جایزه آماده شد.",
                            Toast.LENGTH_SHORT
                    );

                    ad.setOnAdExpired(expiredAd -> {
                        Log.w(TAG, "Rewarded Ad Expired");

                        if (rewardedAd == expiredAd) {
                            rewardedAd = null;
                        }

                        loadRewardedAd();
                    });

                    if (showWhenLoaded) {
                        showWhenLoaded = false;

                        RewardListener listenerToUse =
                                pendingRewardListener;

                        pendingRewardListener = null;

                        handler.postDelayed(
                                () -> showLoadedAd(listenerToUse),
                                300
                        );
                    }

                } else {
                    rewardedAd = null;

                    String message = error == null
                            ? "Unknown error (no ad and no error details)"
                            : "کد " + error.getCode()
                                    + ": " + error.getMessage();

                    Log.e(TAG, "REWARDED_LOAD_FAILED: " + message);

                    developerToast(
                            "❌ بارگذاری تبلیغ جایزه: " + message,
                            Toast.LENGTH_LONG
                    );

                    handler.postDelayed(this::loadRewardedAd, 8000);
                }
            });

        } catch (Exception e) {
            isLoading = false;

            Log.e(TAG, "REWARDED_LOAD_EXCEPTION", e);

            developerToast(
                    "❌ خطای بارگذاری: "
                            + e.getClass().getSimpleName()
                            + ": " + e.getMessage(),
                    Toast.LENGTH_LONG
            );
        }
    }

    // نمایش تبلیغ
    public void showRewardedAd(final RewardListener rewardListener) {
        if (rewardedAd == null) {
            Log.w(TAG, "Rewarded ad not ready when user tapped button");

            developerToast(
                    "⏳ تبلیغ آماده نیست؛ بارگذاری می‌شود و پس از آماده‌شدن نمایش می‌یابد.",
                    Toast.LENGTH_LONG
            );

            showWhenLoaded = true;
            pendingRewardListener = rewardListener;

            loadRewardedAd();

            if (rewardListener != null) {
                rewardListener.onAdNotReady();
            }
            return;
        }

        showLoadedAd(rewardListener);
    }

    private void showLoadedAd(final RewardListener rewardListener) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        if (rewardedAd == null) {
            Log.e(TAG, "Cannot show rewarded ad: ad is null");

            developerToast(
                    "❌ تبلیغ هنوز آماده نیست؛ دوباره امتحان کنید.",
                    Toast.LENGTH_SHORT
            );
            return;
        }

        RewardedAd adToShow = rewardedAd;
        rewardedAd = null;

        ShowConfiguration showConfig =
                new ShowConfiguration.Builder().build();

        try {
            adToShow.show(
                    activity,
                    showConfig,
                    new RewardedShowListener() {

                        @Override
                        public void onStarted(RewardedAd ad) {
                            Log.i(TAG, "REWARDED_SHOW_STARTED");
                        }

                        @Override
                        public void onClicked(RewardedAd ad) {
                            Log.d(TAG, "Rewarded Ad Clicked");
                        }

                        @Override
                        public void onRewarded(RewardedAd ad) {
                            Log.d(TAG, "Reward Earned");

                            if (rewardListener != null) {
                                rewardListener.onRewarded();
                            }
                        }

                        @Override
                        public void onCompleted(
                                RewardedAd ad,
                                ShowFinishState state
                        ) {
                            Log.i(
                                    TAG,
                                    "REWARDED_SHOW_COMPLETED: " + state
                            );

                            loadRewardedAd();
                        }

                        @Override
                        public void onFailed(
                                RewardedAd ad,
                                UnityAdsError error
                        ) {
                            String message;

                            if (error != null) {
                                message = "کد "
                                        + error.getCode()
                                        + ": "
                                        + error.getMessage();
                            } else {
                                message = "Unknown error";
                            }

                            Log.e(
                                    TAG,
                                    "Rewarded Ad Show Failed: " + message
                            );

                            developerToast(
                                    "❌ خطای نمایش تبلیغ جایزه: " + message,
                                    Toast.LENGTH_LONG
                            );

                            if (rewardListener != null) {
                                rewardListener.onAdFailed();
                            }

                            loadRewardedAd();
                        }
                    }
            );

        } catch (Exception e) {
            Log.e(TAG, "REWARDED_SHOW_EXCEPTION", e);

            developerToast(
                    "❌ استثنای نمایش تبلیغ: "
                            + e.getClass().getSimpleName()
                            + ": " + e.getMessage(),
                    Toast.LENGTH_LONG
            );

            if (rewardListener != null) {
                rewardListener.onAdFailed();
            }

            loadRewardedAd();
        }
    }

    // بررسی آماده بودن تبلیغ
    public boolean isAdLoaded() {
        return rewardedAd != null;
    }

    // نتیجه تبلیغ
    public interface RewardListener {
        void onRewarded();

        void onAdNotReady();

        void onAdFailed();
    }
}
