package com.tajro.app;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.unity3d.ads.InitializationConfiguration;
import com.unity3d.ads.InitializationListener;
import com.unity3d.ads.UnityAds;

import java.util.HashMap;
import java.util.Map;

public class TajroApplication extends Application {

    private static final String TAG = "TajroPresence";
    private static final String UNITY_TAG = "UnityAdsDiagnostic";
    private static final String UNITY_GAME_ID = "800372948";
    private static final boolean TEST_MODE = false;
    private static final String DEVELOPER_EMAIL = "najibahmadi111zzz@gmail.com";

    private static final String RTDB_URL =
            "https://tajrobehha-app-202-default-rtdb.asia-southeast1.firebasedatabase.app/";
    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int startedActivities = 0;
    private boolean appInForeground = false;
    private String activeUid = null;
    private String presenceUid = null;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseReference connectedRef;
    private DatabaseReference statusRef;
    private com.google.firebase.database.ValueEventListener connectionListener;
    private FirebaseAuth.AuthStateListener authListener;

    private final Runnable heartbeat = new Runnable() {
        @Override
        public void run() {
            if (appInForeground && activeUid != null) {
                writeFirestorePresence(activeUid, true);
                handler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initializeFirebase();
        initializeUnityAds();
        registerActivityLifecycle();
        initializePresence();
    }

    private void initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
            appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
            Log.i("Firebase", "FIREBASE_INIT_SUCCESS");
        } catch (Exception e) {
            Log.e("Firebase", "FIREBASE_INIT_FAILED", e);
            developerToast("خطای Firebase: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private void initializePresence() {
        try {
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
            FirebaseDatabase database = FirebaseDatabase.getInstance(RTDB_URL);
            connectedRef = database.getReference(".info/connected");

            authListener = firebaseAuth -> {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                String newUid = user == null ? null : user.getUid();

                if (activeUid != null && !activeUid.equals(newUid)) {
                    stopPresence(activeUid);
                }

                activeUid = newUid;
                if (appInForeground && activeUid != null) {
                    startPresence(activeUid);
                }
            };
            auth.addAuthStateListener(authListener);

            FirebaseUser current = auth.getCurrentUser();
            activeUid = current == null ? null : current.getUid();
            if (appInForeground && activeUid != null) {
                startPresence(activeUid);
            }
        } catch (Exception e) {
            Log.e(TAG, "Presence initialization failed", e);
        }
    }

    private void registerActivityLifecycle() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            @Override public void onActivityDestroyed(Activity activity) {}

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities++;
                if (startedActivities == 1) {
                    appInForeground = true;
                    if (activeUid != null) startPresence(activeUid);
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities = Math.max(0, startedActivities - 1);
                if (startedActivities == 0) {
                    appInForeground = false;
                    if (activeUid != null) stopPresence(activeUid);
                }
            }
        });
    }

    private void startPresence(String uid) {
        if (uid == null || uid.isEmpty()) return;
        if (uid.equals(presenceUid) && connectionListener != null) {
            writeFirestorePresence(uid, true);
            return;
        }
        presenceUid = uid;

        writeFirestorePresence(uid, true);
        handler.removeCallbacks(heartbeat);
        handler.postDelayed(heartbeat, HEARTBEAT_INTERVAL_MS);

        try {
            statusRef = FirebaseDatabase.getInstance(RTDB_URL)
                    .getReference("status").child(uid);

            if (connectionListener != null && connectedRef != null) {
                connectedRef.removeEventListener(connectionListener);
            }

            connectionListener = new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    Boolean connected = snapshot.getValue(Boolean.class);
                    if (Boolean.TRUE.equals(connected) && appInForeground
                            && uid.equals(activeUid)) {
                        Map<String, Object> online = new HashMap<>();
                        online.put("state", "online");
                        online.put("last_changed", ServerValue.TIMESTAMP);

                        Map<String, Object> offline = new HashMap<>();
                        offline.put("state", "offline");
                        offline.put("last_changed", ServerValue.TIMESTAMP);

                        DatabaseReference userStatusRef =
                                FirebaseDatabase.getInstance(RTDB_URL)
                                        .getReference("status").child(uid);
                        userStatusRef.onDisconnect().setValue(offline)
                                .addOnSuccessListener(unused ->
                                        userStatusRef.setValue(online)
                                                .addOnFailureListener(error ->
                                                        Log.w(TAG, "Could not publish RTDB online state", error))
                                )
                                .addOnFailureListener(error ->
                                        Log.w(TAG, "Could not register onDisconnect", error));
                    }
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Log.w(TAG, "RTDB connection listener cancelled: " + error.getMessage());
                }
            };
            connectedRef.addValueEventListener(connectionListener);
        } catch (Exception e) {
            Log.e(TAG, "Could not start RTDB presence", e);
        }
    }

    private void stopPresence(String uid) {
        if (uid == null) return;
        if (uid.equals(presenceUid)) presenceUid = null;
        handler.removeCallbacks(heartbeat);
        writeFirestorePresence(uid, false);

        try {
            if (connectionListener != null && connectedRef != null) {
                connectedRef.removeEventListener(connectionListener);
                connectionListener = null;
            }
            DatabaseReference ref = FirebaseDatabase.getInstance(RTDB_URL)
                    .getReference("status").child(uid);
            Map<String, Object> offline = new HashMap<>();
            offline.put("state", "offline");
            offline.put("last_changed", ServerValue.TIMESTAMP);
            ref.setValue(offline);
        } catch (Exception e) {
            Log.w(TAG, "Could not mark RTDB offline", e);
        }
    }

    private void writeFirestorePresence(String uid, boolean online) {
        if (firestore == null || uid == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("online", online);
        data.put("lastSeen", FieldValue.serverTimestamp());
        if (!online) data.put("typingTo", "");
        firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnFailureListener(error ->
                        Log.w(TAG, "Firestore presence update failed", error));
    }

    private void initializeUnityAds() {
        Log.i(UNITY_TAG, "========== UNITY DIAGNOSTIC START ==========");
        Log.i(UNITY_TAG, "Game ID: " + UNITY_GAME_ID);
        Log.i(UNITY_TAG, "Test mode: " + TEST_MODE);
        try {
            Log.i(UNITY_TAG, "SDK version: " + UnityAds.getVersion());
            Log.i(UNITY_TAG, "Device supported: " + UnityAds.isSupported());
            Log.i(UNITY_TAG, "Initialized before call: " + UnityAds.isInitialized());

            InitializationConfiguration config =
                    new InitializationConfiguration.Builder(UNITY_GAME_ID)
                            .withTestMode(TEST_MODE)
                            .build();

            InitializationListener listener = error -> {
                if (error == null) {
                    Log.i(UNITY_TAG, "UNITY_INIT_SUCCESS");
                    developerToast("Unity آماده شد. نسخه: " + UnityAds.getVersion(),
                            Toast.LENGTH_LONG);
                } else {
                    Log.e(UNITY_TAG, "UNITY_INIT_FAILED: " + error.getMessage());
                    developerToast("خطای راه‌اندازی Unity: " + error.getMessage(),
                            Toast.LENGTH_LONG);
                }
                Log.i(UNITY_TAG, "========== UNITY DIAGNOSTIC END ==========");
            };
            Log.i(UNITY_TAG, "UNITY_INIT_REQUEST");
            UnityAds.initialize(config, listener);
        } catch (Exception e) {
            Log.e(UNITY_TAG, "UNITY_INIT_EXCEPTION", e);
            developerToast("استثنای Unity: " + e.getClass().getSimpleName()
                    + " — " + String.valueOf(e.getMessage()), Toast.LENGTH_LONG);
        }
    }

    private boolean isDeveloper() {
        try {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            return user != null && user.getEmail() != null
                    && DEVELOPER_EMAIL.equalsIgnoreCase(user.getEmail().trim());
        } catch (Exception e) {
            Log.w(UNITY_TAG, "Could not verify developer account", e);
            return false;
        }
    }

    private void developerToast(String message, int duration) {
        if (!isDeveloper()) return;
        handler.post(() -> Toast.makeText(
                getApplicationContext(), message, duration).show());
    }
}
