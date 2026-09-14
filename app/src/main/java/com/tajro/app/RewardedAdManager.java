package com.tajro.app;

import android.app.Activity;
import android.util.Log;

import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    // نام جایگاه Rewarded در Unity
    private static final String AD_UNIT_ID = "Rewarded_Android";

    private final Activity activity;

    private boolean adLoaded = false;

    public RewardedAdManager(Activity activity) {
        this.activity = activity;
    }

    // بارگذاری تبلیغ
    public void loadRewardedAd() {

        UnityAds.load(
                AD_UNIT_ID,
                new IUnityAdsLoadListener() {

                    @Override
                    public void onUnityAdsAdLoaded(String placementId) {
                        adLoaded = true;

                        Log.d(TAG, "Rewarded Ad Loaded");
                    }

                    @Override
                    public void onUnityAdsFailedToLoad(
                            String placementId,
                            UnityAds.UnityAdsLoadError error,
                            String message) {

                        adLoaded = false;

                        Log.e(
                                TAG,
                                "Rewarded Ad Load Failed: "
                                        + error + " - " + message
                        );
                    }
                }
        );
    }

    // نمایش تبلیغ
    public void showRewardedAd(final RewardListener rewardListener) {

        if (!adLoaded) {
            Log.d(TAG, "Rewarded Ad is not ready");

            if (rewardListener != null) {
                rewardListener.onAdNotReady();
            }

            loadRewardedAd();
            return;
        }

        adLoaded = false;

        UnityAds.show(
                activity,
                AD_UNIT_ID,
                new UnityAdsShowOptions(),
                new IUnityAdsShowListener() {

                    @Override
                    public void onUnityAdsShowFailure(
                            String placementId,
                            UnityAds.UnityAdsShowError error,
                            String message) {

                        Log.e(
                                TAG,
                                "Rewarded Ad Show Failed: "
                                        + error + " - " + message
                        );

                        if (rewardListener != null) {
                            rewardListener.onAdFailed();
                        }

                        loadRewardedAd();
                    }

                    @Override
                    public void onUnityAdsShowStart(String placementId) {
                        Log.d(TAG, "Rewarded Ad Started");
                    }

                    @Override
                    public void onUnityAdsShowClick(String placementId) {
                        Log.d(TAG, "Rewarded Ad Clicked");
                    }

                    @Override
                    public void onUnityAdsShowComplete(
                            String placementId,
                            UnityAds.UnityAdsShowCompletionState state) {

                        Log.d(TAG, "Rewarded Ad Completed");

                        if (state ==
                                UnityAds.UnityAdsShowCompletionState.COMPLETED) {

                            // فقط در صورت تماشای کامل تبلیغ پاداش بده
                            if (rewardListener != null) {
                                rewardListener.onRewarded();
                            }
                        }

                        // تبلیغ بعدی را آماده کن
                        loadRewardedAd();
                    }
                }
        );
    }

    public boolean isAdLoaded() {
        return adLoaded;
    }

    public interface RewardListener {

        void onRewarded();

        void onAdNotReady();

        void onAdFailed();
    }
}
