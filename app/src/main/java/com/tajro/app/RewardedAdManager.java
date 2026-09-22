package com.tajro.app;

import android.app.Activity;
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

    public RewardedAdManager(Activity activity) {
        this.activity = activity;
    }

    // ==============================
    // فقط سازنده پیام‌های فنی را می‌بیند
    // ==============================
    private boolean isDeveloper() {

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            return false;
        }

        String email = user.getEmail();

        return email != null
                && DEVELOPER_EMAIL.equalsIgnoreCase(
                        email.trim()
                );
    }

    private void developerToast(
            String message,
            int duration) {

        if (isDeveloper()) {

            Toast.makeText(
                    activity,
                    message,
                    duration
            ).show();
        }
    }

    // ==============================
    // بارگذاری تبلیغ
    // ==============================
    public void loadRewardedAd() {

        if (!UnityAds.isInitialized()) {

            Log.e(
                    TAG,
                    "Unity Ads is not initialized"
            );

            developerToast(
                    "❌ Unity Ads هنوز آماده نشده است.",
                    Toast.LENGTH_SHORT
            );

            return;
        }

        LoadConfiguration loadConfig =
                new LoadConfiguration.Builder(AD_UNIT_ID)
                        .build();

        RewardedAd.load(
                loadConfig,
                (ad, error) -> {

                    if (ad != null) {

                        rewardedAd = ad;

                        Log.d(
                                TAG,
                                "Rewarded Ad Loaded Successfully"
                        );

                        developerToast(
                                "✅ تبلیغ آماده شد.",
                                Toast.LENGTH_SHORT
                        );

                        rewardedAd.setOnAdExpired(
                                expiredAd -> {

                                    Log.d(
                                            TAG,
                                            "Rewarded Ad Expired"
                                    );

                                    rewardedAd = null;

                                    loadRewardedAd();
                                }
                        );

                    } else {

                        rewardedAd = null;

                        String message;

                        if (error != null) {

                            message =
                                    "کد "
                                            + error.getCode()
                                            + ": "
                                            + error.getMessage();

                        } else {

                            message = "Unknown error";
                        }

                        Log.e(
                                TAG,
                                "Rewarded Ad Load Failed: "
                                        + message
                        );

                        developerToast(
                                "❌ خطای تبلیغ: "
                                        + message,
                                Toast.LENGTH_LONG
                        );
                    }
                }
        );
    }

    // ==============================
    // نمایش تبلیغ
    // ==============================
    public void showRewardedAd(
            final RewardListener rewardListener) {

        if (rewardedAd == null) {

            developerToast(
                    "⏳ تبلیغ هنوز آماده نیست، چند لحظه صبر کنید.",
                    Toast.LENGTH_SHORT
            );

            Log.d(
                    TAG,
                    "Rewarded Ad is not ready"
            );

            loadRewardedAd();

            if (rewardListener != null) {

                rewardListener.onAdNotReady();
            }

            return;
        }

        RewardedAd adToShow = rewardedAd;

        rewardedAd = null;

        ShowConfiguration showConfig =
                new ShowConfiguration.Builder()
                        .build();

        adToShow.show(
                activity,
                showConfig,
                new RewardedShowListener() {

                    @Override
                    public void onStarted(
                            RewardedAd ad) {

                        Log.d(
                                TAG,
                                "Rewarded Ad Started"
                        );
                    }

                    @Override
                    public void onClicked(
                            RewardedAd ad) {

                        Log.d(
                                TAG,
                                "Rewarded Ad Clicked"
                        );
                    }

                    @Override
                    public void onRewarded(
                            RewardedAd ad) {

                        Log.d(
                                TAG,
                                "Reward Earned"
                        );

                        if (rewardListener != null) {

                            rewardListener.onRewarded();
                        }
                    }

                    @Override
                    public void onCompleted(
                            RewardedAd ad,
                            ShowFinishState state) {

                        Log.d(
                                TAG,
                                "Rewarded Ad Completed"
                        );

                        loadRewardedAd();
                    }

                    @Override
                    public void onFailed(
                            RewardedAd ad,
                            UnityAdsError error) {

                        String message;

                        if (error != null) {

                            message =
                                    "کد "
                                            + error.getCode()
                                            + ": "
                                            + error.getMessage();

                        } else {

                            message = "Unknown error";
                        }

                        Log.e(
                                TAG,
                                "Rewarded Ad Show Failed: "
                                        + message
                        );

                        developerToast(
                                "❌ خطای نمایش تبلیغ: "
                                        + message,
                                Toast.LENGTH_LONG
                        );

                        if (rewardListener != null) {

                            rewardListener.onAdFailed();
                        }

                        loadRewardedAd();
                    }
                }
        );
    }

    // ==============================
    // بررسی آماده بودن تبلیغ
    // ==============================
    public boolean isAdLoaded() {

        return rewardedAd != null;
    }

    // ==============================
    // نتیجه تبلیغ
    // ==============================
    public interface RewardListener {

        void onRewarded();

        void onAdNotReady();

        void onAdFailed();
    }
}
