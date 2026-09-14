package com.tajro.app;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;
import com.unity3d.ads.ShowConfiguration;
import com.unity3d.ads.LoadConfiguration;
import com.unity3d.ads.RewardedAd;
import com.unity3d.ads.RewardedShowListener;
import com.unity3d.ads.RewardedLoadListener;
import com.unity3d.ads.ShowFinishState;

public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    // شناسه دقیق جایگاه Rewarded از Unity
    private static final String AD_UNIT_ID = "BP_Rewarded_Android";

    private final Activity activity;

    private RewardedAd rewardedAd;

    public RewardedAdManager(Activity activity) {
        this.activity = activity;
    }

    // بارگذاری تبلیغ
    public void loadRewardedAd() {

        if (!UnityAds.isInitialized()) {
            Log.e(TAG, "Unity Ads is not initialized");
            return;
        }

        LoadConfiguration loadConfig =
                new LoadConfiguration.Builder(AD_UNIT_ID)
                        .build();

        RewardedAd.load(
                loadConfig,
                new RewardedLoadListener() {

                    @Override
                    public void onRewardedLoaded(
                            RewardedAd ad,
                            UnityAdsError error) {

                        if (ad != null) {

                            rewardedAd = ad;

                            Log.d(
                                    TAG,
                                    "Rewarded Ad Loaded Successfully"
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

                            String message =
                                    error != null
                                            ? error.getMessage()
                                            : "Unknown error";

                            Log.e(
                                    TAG,
                                    "Rewarded Ad Load Failed: "
                                            + message
                            );
                        }
                    }
                }
        );
    }

    // نمایش تبلیغ
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

            loadRewardedAd();

            if (rewardListener != null) {
                rewardListener.onAdNotReady();
            }

            return;
        }

        RewardedAd adToShow = rewardedAd;

        // برای جلوگیری از نمایش دوباره همان تبلیغ
        rewardedAd = null;

        ShowConfiguration showConfig =
                new ShowConfiguration.Builder()
                        .build();

        adToShow.show(
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

                        // تبلیغ بعدی را آماده می‌کنیم
                        loadRewardedAd();
                    }

                    @Override
                    public void onFailed(
                            RewardedAd ad,
                            UnityAdsError error) {

                        String message =
                                error != null
                                        ? error.getMessage()
                                        : "Unknown error";

                        Log.e(
                                TAG,
                                "Rewarded Ad Show Failed: "
                                        + message
                        );

                        if (rewardListener != null) {
                            rewardListener.onAdFailed();
                        }

                        // دوباره تبلیغ را بارگذاری کن
                        loadRewardedAd();
                    }
                }
        );
    }

    public boolean isAdLoaded() {
        return rewardedAd != null;
    }

    public interface RewardListener {

        void onRewarded();

        void onAdNotReady();

        void onAdFailed();
    }
}
