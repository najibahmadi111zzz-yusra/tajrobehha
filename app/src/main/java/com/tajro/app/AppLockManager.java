package com.tajro.app;

import android.content.Context;
import android.content.SharedPreferences;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class AppLockManager {

    private static final String PREF_NAME = "tajrobehha_privacy";
    private static final String KEY_SALT = "password_salt";
    private static final String KEY_HASH = "password_hash";
    private static final String KEY_LOCK_ENABLED = "lock_enabled";
    private static final String KEY_HIDE_INFO = "hide_personal_info";

    private static final String KEY_SESSION_UNLOCKED = "session_unlocked";

    private AppLockManager() {
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // =========================
    // بررسی داشتن رمز
    // =========================

    public static boolean hasPassword(Context context) {

        String hash = getPreferences(context)
                .getString(KEY_HASH, "");

        return hash != null && !hash.isEmpty();
    }

    // =========================
    // فعال بودن قفل
    // =========================

    public static boolean isLockEnabled(Context context) {

        return getPreferences(context)
                .getBoolean(
                        KEY_LOCK_ENABLED,
                        hasPassword(context)
                );
    }

    // =========================
    // فعال / غیرفعال کردن قفل
    // =========================

    public static void setLockEnabled(
            Context context,
            boolean enabled
    ) {

        getPreferences(context)
                .edit()
                .putBoolean(KEY_LOCK_ENABLED, enabled)
                .apply();
    }

    // =========================
    // ساخت رمز جدید
    // =========================

    public static boolean setPassword(
            Context context,
            String password
    ) {

        if (!isValidPassword(password)) {
            return false;
        }

        byte[] salt = new byte[16];

        new SecureRandom().nextBytes(salt);

        String saltText = bytesToHex(salt);
        String hashText = hashPassword(password, saltText);

        if (hashText == null) {
            return false;
        }

        getPreferences(context)
                .edit()
                .putString(KEY_SALT, saltText)
                .putString(KEY_HASH, hashText)
                .putBoolean(KEY_LOCK_ENABLED, true)
                .apply();

        return true;
    }

    // =========================
    // بررسی رمز
    // =========================

    public static boolean verifyPassword(
            Context context,
            String password
    ) {

        if (!isValidPassword(password)) {
            return false;
        }

        SharedPreferences preferences =
                getPreferences(context);

        String salt =
                preferences.getString(KEY_SALT, "");

        String savedHash =
                preferences.getString(KEY_HASH, "");

        if (salt.isEmpty() || savedHash.isEmpty()) {
            return false;
        }

        String enteredHash =
                hashPassword(password, salt);

        return savedHash.equals(enteredHash);
    }

    // =========================
    // تغییر رمز
    // =========================

    public static boolean changePassword(
            Context context,
            String oldPassword,
            String newPassword
    ) {

        if (!verifyPassword(context, oldPassword)) {
            return false;
        }

        return setPassword(context, newPassword);
    }

    // =========================
    // اعتبارسنجی رمز
    // دقیقاً ۶ رقم
    // =========================

    public static boolean isValidPassword(
            String password
    ) {

        if (password == null) {
            return false;
        }

        if (password.length() != 6) {
            return false;
        }

        for (int i = 0; i < password.length(); i++) {

            char c = password.charAt(i);

            if (c < '0' || c > '9') {
                return false;
            }
        }

        return true;
    }

    // =========================
    // مخفی کردن اطلاعات شخصی
    // =========================

    public static boolean isPersonalInfoHidden(
            Context context
    ) {

        return getPreferences(context)
                .getBoolean(KEY_HIDE_INFO, false);
    }

    public static void setPersonalInfoHidden(
            Context context,
            boolean hidden
    ) {

        getPreferences(context)
                .edit()
                .putBoolean(KEY_HIDE_INFO, hidden)
                .apply();
    }

    // =========================
    // وضعیت باز بودن قفل
    // =========================

    public static boolean isSessionUnlocked(
            Context context
    ) {

        return getPreferences(context)
                .getBoolean(KEY_SESSION_UNLOCKED, false);
    }

    public static void setSessionUnlocked(
            Context context,
            boolean unlocked
    ) {

        getPreferences(context)
                .edit()
                .putBoolean(
                        KEY_SESSION_UNLOCKED,
                        unlocked
                )
                .apply();
    }

    // =========================
    // قفل کردن مجدد
    // =========================

    public static void lockSession(
            Context context
    ) {

        setSessionUnlocked(context, false);
    }

    // =========================
    // رمزنگاری SHA-256
    // =========================

    private static String hashPassword(
            String password,
            String salt
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            String combined =
                    salt + ":" + password;

            byte[] hash =
                    digest.digest(
                            combined.getBytes("UTF-8")
                    );

            return bytesToHex(hash);

        } catch (Exception e) {

            return null;
        }
    }

    private static String bytesToHex(
            byte[] bytes
    ) {

        StringBuilder result =
                new StringBuilder();

        for (byte b : bytes) {

            result.append(
                    String.format(
                            "%02x",
                            b & 0xff
                    )
            );
        }

        return result.toString();
    }
}
