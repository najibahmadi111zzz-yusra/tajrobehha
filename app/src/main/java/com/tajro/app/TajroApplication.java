package com.tajro.app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.UnityAds;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TajroApplication extends Application {

    private static final String TAG = "UnityAdsDiagnostic";

    private static final String UNITY_GAME_ID = "800372948";
    private static final boolean TEST_MODE = false;
    private static final String DEVELOPER_EMAIL =
            "najibahmadi111zzz@gmail.com";

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();

        initializeFirebase();
        initializeUnityAds();
    }

    private void initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this);

            FirebaseAppCheck appCheck =
                    FirebaseAppCheck.getInstance();

            appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );

            Log.i("Firebase", "FIREBASE_INIT_SUCCESS");

        } catch (Exception e) {
            Log.e("Firebase", "FIREBASE_INIT_FAILED", e);
        }
    }

    private void initializeUnityAds() {
        Log.i(TAG, "========== UNITY DIAGNOSTIC START ==========");
        Log.i(TAG, "Game ID: " + UNITY_GAME_ID);
        Log.i(TAG, "Test mode: " + TEST_MODE);

        try {
            Log.i(TAG, "SDK version: " + UnityAds.getVersion());
            Log.i(TAG, "Device supported: " + UnityAds.isSupported());
            Log.i(TAG, "Initialized before call: "
                    + UnityAds.isInitialized());

            InitializationConfiguration config =
                    new InitializationConfiguration.Builder(
                            UNITY_GAME_ID
                    )
                            .withTestMode(TEST_MODE)
                            .build();

            InitializationListener listener = error -> {
                if (error == null) {
                    Log.i(TAG, "UNITY_INIT_SUCCESS");
                    Log.i(TAG, "Initialized after callback: "
                            + UnityAds.isInitialized());

                    developerToast(
                            "✅ Unity آماده شد. نسخه: "
                                    + UnityAds.getVersion(),
                            Toast.LENGTH_LONG
                    );
                } else {
                    String details = error.getMessage();

                    Log.e(TAG,
                            "UNITY_INIT_FAILED: "
                                    + String.valueOf(details));

                    developerToast(
                            "❌ راه‌اندازی Unity ناموفق: "
                                    + String.valueOf(details),
                            Toast.LENGTH_LONG
                    );
                }

                Log.i(TAG, "========== UNITY DIAGNOSTIC END ==========");
            };

            Log.i(TAG, "UNITY_INIT_REQUEST");

            UnityAds.initialize(config, listener);

        } catch (Exception e) {
            Log.e(TAG, "UNITY_INIT_EXCEPTION", e);

            developerToast(
                    "❌ استثنای Unity: "
                            + e.getClass().getSimpleName()
                            + " — " + String.valueOf(e.getMessage()),
                    Toast.LENGTH_LONG
            );
        }
    }

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
            Log.w(TAG, "Could not verify developer account", e);
            return false;
        }
    }

    private void developerToast(String message, int duration) {
        if (!isDeveloper()) {
            return;
        }

        handler.post(() ->
                Toast.makeText(
                        getApplicationContext(),
                        message,
                        duration
                ).show()
        );
    }
}
