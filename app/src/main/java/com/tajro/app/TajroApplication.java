package com.tajro.app;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import com.unity3d.ads.UnityAds;

public class TajroApplication extends Application {

    private static final String UNITY_GAME_ID = "800372948";

    // حالت آزمایشی برای جلوگیری از نمایش تبلیغ واقعی هنگام توسعه
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
                state -> {
                    // Unity Ads initialized
                }
        );
    }
}
