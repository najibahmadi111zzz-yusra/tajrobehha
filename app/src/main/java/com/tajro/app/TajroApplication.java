package com.tajro.app;

import android.app.Application;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsError;

public class TajroApplication extends Application {

    private static final String UNITY_GAME_ID = "800372948";

    // در مرحله تست حتماً true باشد
    private static final boolean TEST_MODE = true;

    @Override
    public void onCreate() {
        super.onCreate();

        // =========================
        // Firebase
        // =========================
        try {

            FirebaseApp.initializeApp(this);

            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance();

            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );

            Log.d(
                    "Firebase",
                    "Firebase initialized successfully"
            );

        } catch (Exception e) {

            Log.e(
                    "Firebase",
                    "Firebase initialization error",
                    e
            );
        }

        // =========================
        // Unity Ads
        // =========================

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

                        Toast.makeText(
                                this,
                                "✅ Unity Ads آماده شد.",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else {

                        String message =
                                error.getMessage();

                        Log.e(
                                "UnityAds",
                                "Unity Ads initialization failed: "
                                        + message
                        );

                        Toast.makeText(
                                this,
                                "❌ خطای Unity Ads: "
                                        + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                };

        Log.d(
                "UnityAds",
                "Starting Unity Ads initialization..."
        );

        UnityAds.initialize(
                config,
                listener
        );
    }
}
