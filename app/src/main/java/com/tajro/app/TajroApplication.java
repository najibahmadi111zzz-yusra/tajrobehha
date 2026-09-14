package com.tajro.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class TajroApplication extends Application {

    private static final String UNITY_GAME_ID = "864578833";

    // فعلاً برای تست حتماً true باشد
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
        InitializationConfiguration config =
                new InitializationConfiguration.Builder(
                        UNITY_GAME_ID
                )
                        .withTestMode(TEST_MODE)
                        .build();

        InitializationListener listener =
                error -> {

                    if (error == null) {

                        Log.d(
                                "UnityAds",
                                "Unity Ads initialized successfully"
                        );

                    } else {

                        Log.e(
                                "UnityAds",
                                "Unity Ads initialization failed: "
                                        + error.getMessage()
                        );
                    }
                };

        UnityAds.initialize(
                config,
                listener
        );
    }
}
