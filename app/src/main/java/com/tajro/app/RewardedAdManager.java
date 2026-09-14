package com.tajro.app;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.RewardedAd;
import com.unity3d.ads.RewardedShowListener;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.ShowFinishState;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    // شناسه دقیق جایگاه تبلیغ Rewarded در Unity
    private static final String AD_UNIT_ID = "BP_Rewarded_Android";

    private final Activity activity;

    private RewardedAd rewardedAd;

    public RewardedAdManager(Activity activity) {
        this.activity = activity;
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

            Toast.makeText(
                    activity,
                    "❌ Unity Ads هنوز آماده نشده است.",
                    Toast.LENGTH_SHORT
            ).show();

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

                        Toast.makeText(
                                activity,
                                "✅ تبلیغ آماده شد.",
                                Toast.LENGTH_SHORT
                        ).show();

                        rewardedAd.setOnAdExpired(
                                expiredAd -> {

                                    Log.d(
                                            TAG,
                                            "Rewarded Ad Expired"
                                    );

                                    rewardedAd = null;

                                    // بارگذاری تبلیغ جدید
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

                        // نمایش خطای واقعی روی صفحه
                        Toast.makeText(
                                activity,
                                "❌ خطای تبلیغ: " + message,
                                Toast.LENGTH_LONG
                        ).show();
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

            Toast.makeText(
                    activity,
                    "⏳ تبلیغ هنوز آماده نیست، چند لحظه صبر کنید.",
                    Toast.LENGTH_SHORT
            ).show();

            Log.d(
                    TAG,
                    "Rewarded Ad is not ready"
            );

            // تلاش دوباره برای بارگذاری
            loadRewardedAd();

            if (rewardListener != null) {

                rewardListener.onAdNotReady();
            }

            return;
        }

        // تبلیغ فعلی را برای نمایش نگه می‌داریم
        RewardedAd adToShow = rewardedAd;

        // جلوگیری از نمایش دوباره همان تبلیغ
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

                        // آماده‌سازی تبلیغ بعدی
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

                        Toast.makeText(
                                activity,
                                "❌ خطای نمایش تبلیغ: "
                                        + message,
                                Toast.LENGTH_LONG
                        ).show();

                        if (rewardListener != null) {

                            rewardListener.onAdFailed();
                        }

                        // تلاش برای بارگذاری تبلیغ بعدی
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
