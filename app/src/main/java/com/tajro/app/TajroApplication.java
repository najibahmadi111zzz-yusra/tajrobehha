package com.tajro.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.UnityAds;

public class TajroApplication extends Application
        implements IUnityAdsInitializationListener {

    private static final String UNITY_GAME_ID = "800372948";

    // فعلاً حتماً true باشد؛ برای تست تبلیغ
    private static final boolean TEST_MODE = true;

    @Override
    public void onCreate() {
        super.onCreate();

        // Firebase
        FirebaseApp.initializeApp(this);

        // Firebase App Check
        FirebaseAppCheck firebaseAppCheck =
                FirebaseAppCheck.getInstance();

        firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
        );

        // Unity Ads
        UnityAds.initialize(
                this,
                UNITY_GAME_ID,
                TEST_MODE,
                this
        );
    }

    @Override
    public void onInitializationComplete() {
        Log.d("UnityAds", "Unity Ads initialized successfully");
    }

    @Override
    public void onInitializationFailed(
            UnityAds.UnityAdsInitializationError error,
            String message) {

        Log.e(
                "UnityAds",
                "Unity Ads initialization failed: "
                        + error + " - " + message
        );
    }
}
