package com.prosumerchat.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * SecurePreferencesManager â€” LÆ°u trá»¯ an toÃ n cáº¥u hÃ¬nh phiÃªn Ä‘Äƒng nháº­p,
 * Ä‘á»‹nh danh thiáº¿t bá»‹, tráº¡ng thÃ¡i khÃ³a sinh tráº¯c há»c vÃ  tÃ¹y chá»n báº£o máº­t.
 *
 * KhÃ´ng sá»­ dá»¥ng EncryptedSharedPreferences (Ä‘Ã£ deprecated).
 * Má»i giÃ¡ trá»‹ Ä‘Æ°á»£c mÃ£ hÃ³a riÃªng láº» báº±ng Tink AEAD (AES-256-GCM) thÃ´ng qua
 * {@link TinkKeysetManager} vá»›i Associated Data (AAD) gáº¯n theo tÃªn khÃ³a Ä‘á»ƒ
 * ngÄƒn cháº·n trÃ¡o Ä‘á»•i ciphertext giá»¯a cÃ¡c key vÃ  khÃ´ng bao giá» lÆ°u plaintext.
 */
public final class SecurePreferencesManager {

    private static final String TAG = "SecurePrefsManager";

    private static final String KEY_CURRENT_SESSION_ID = "current_session_id";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_BIOMETRIC_TIMEOUT_SECONDS = "biometric_timeout_seconds";
    private static final String KEY_LAST_BACKGROUND_TIMESTAMP = "last_background_timestamp";
    private static final String KEY_CLEAR_DATA_ON_LOGOUT = "clear_data_on_logout";
    private static final String KEY_PREFERRED_LANGUAGE = "preferred_language";
    private static final String KEY_FCM_TOKEN = "fcm_token";

    public static final int DEFAULT_BIOMETRIC_TIMEOUT_SECONDS = 60;
    public static final String DEFAULT_LANGUAGE = "vi";

    private static volatile SecurePreferencesManager sInstance;

    private final Context appContext;
    private final SharedPreferences prefs;

    private SecurePreferencesManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        TinkKeysetManager.initialize(this.appContext);
        this.prefs = this.appContext.getSharedPreferences(
                TinkKeysetManager.SECURE_KEY_PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public static SecurePreferencesManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (SecurePreferencesManager.class) {
                if (sInstance == null) {
                    sInstance = new SecurePreferencesManager(context);
                }
            }
        }
        return sInstance;
    }

    // â”€â”€ Session & Device â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Nullable
    public String getCurrentSessionId() {
        return getEncryptedString(KEY_CURRENT_SESSION_ID, null);
    }

    public void setCurrentSessionId(@Nullable String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            removeKey(KEY_CURRENT_SESSION_ID);
        } else {
            putEncryptedString(KEY_CURRENT_SESSION_ID, sessionId.trim());
        }
    }

    public void clearCurrentSessionId() {
        removeKey(KEY_CURRENT_SESSION_ID);
    }

    /**
     * Láº¥y Device ID á»•n Ä‘á»‹nh trÃªn thiáº¿t bá»‹; sinh UUID má»›i á»Ÿ láº§n Ä‘áº§u vÃ  lÆ°u mÃ£ hÃ³a.
     */
    @NonNull
    public synchronized String getOrCreateDeviceId() {
        String existing = getEncryptedString(KEY_DEVICE_ID, null);
        if (existing != null && !existing.isEmpty()) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        putEncryptedString(KEY_DEVICE_ID, generated);
        return generated;
    }

    @Nullable
    public String getFcmToken() {
        return getEncryptedString(KEY_FCM_TOKEN, null);
    }

    public void setFcmToken(@Nullable String fcmToken) {
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            removeKey(KEY_FCM_TOKEN);
        } else {
            putEncryptedString(KEY_FCM_TOKEN, fcmToken.trim());
        }
    }

    // â”€â”€ Biometric Lock â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public boolean isBiometricEnabled() {
        return getEncryptedBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        putEncryptedBoolean(KEY_BIOMETRIC_ENABLED, enabled);
    }

    /**
     * Thá»i gian chá» khÃ³a sinh tráº¯c há»c (giÃ¢y): 0 (ngay láº­p tá»©c), 60 (1 phÃºt), 300 (5 phÃºt).
     */
    public int getBiometricTimeoutSeconds() {
        return getEncryptedInt(KEY_BIOMETRIC_TIMEOUT_SECONDS, DEFAULT_BIOMETRIC_TIMEOUT_SECONDS);
    }

    public void setBiometricTimeoutSeconds(int timeoutSeconds) {
        int normalized = Math.max(0, timeoutSeconds);
        putEncryptedInt(KEY_BIOMETRIC_TIMEOUT_SECONDS, normalized);
    }

    public long getLastBackgroundTimestamp() {
        return getEncryptedLong(KEY_LAST_BACKGROUND_TIMESTAMP, 0L);
    }

    public void setLastBackgroundTimestamp(long timestampMs) {
        putEncryptedLong(KEY_LAST_BACKGROUND_TIMESTAMP, Math.max(0L, timestampMs));
    }

    // â”€â”€ Privacy / Logout / Language â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    public boolean isClearDataOnLogout() {
        return getEncryptedBoolean(KEY_CLEAR_DATA_ON_LOGOUT, false);
    }

    public void setClearDataOnLogout(boolean clearDataOnLogout) {
        putEncryptedBoolean(KEY_CLEAR_DATA_ON_LOGOUT, clearDataOnLogout);
    }

    @NonNull
    public String getPreferredLanguage() {
        String lang = getEncryptedString(KEY_PREFERRED_LANGUAGE, DEFAULT_LANGUAGE);
        return (lang != null && !lang.isEmpty()) ? lang : DEFAULT_LANGUAGE;
    }

    public void setPreferredLanguage(@NonNull String languageTag) {
        putEncryptedString(KEY_PREFERRED_LANGUAGE, languageTag);
    }

    /**
     * XÃ³a thÃ´ng tin phiÃªn hiá»‡n táº¡i khi Ä‘Äƒng xuáº¥t nhÆ°ng váº«n giá»¯ láº¡i deviceId vÃ  ngÃ´n ngá»¯.
     */
    public void clearSessionState() {
        prefs.edit()
                .remove(KEY_CURRENT_SESSION_ID)
                .remove(KEY_LAST_BACKGROUND_TIMESTAMP)
                .remove(KEY_FCM_TOKEN)
                .apply();
    }

    // â”€â”€ Internal Tink AEAD helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void putEncryptedString(@NonNull String key, @NonNull String value) {
        try {
            String encrypted = TinkKeysetManager.encryptString(appContext, value, key);
            if (encrypted != null) {
                prefs.edit().putString(key, encrypted).apply();
            }
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to encrypt preference entry for key: " + key, e);
        }
    }

    @Nullable
    private String getEncryptedString(@NonNull String key, @Nullable String defaultValue) {
        String encrypted = prefs.getString(key, null);
        if (encrypted == null) {
            return defaultValue;
        }
        try {
            String decrypted = TinkKeysetManager.decryptString(appContext, encrypted, key);
            return decrypted != null ? decrypted : defaultValue;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            Log.w(TAG, "Failed to decrypt preference entry for key: " + key + "; clearing corrupt value");
            removeKey(key);
            return defaultValue;
        }
    }

    private void putEncryptedBoolean(@NonNull String key, boolean value) {
        putEncryptedString(key, Boolean.toString(value));
    }

    private boolean getEncryptedBoolean(@NonNull String key, boolean defaultValue) {
        String raw = getEncryptedString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        if ("true".equalsIgnoreCase(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return false;
        }
        return defaultValue;
    }

    private void putEncryptedInt(@NonNull String key, int value) {
        putEncryptedString(key, Integer.toString(value));
    }

    private int getEncryptedInt(@NonNull String key, int defaultValue) {
        String raw = getEncryptedString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void putEncryptedLong(@NonNull String key, long value) {
        putEncryptedString(key, Long.toString(value));
    }

    private long getEncryptedLong(@NonNull String key, long defaultValue) {
        String raw = getEncryptedString(key, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void removeKey(@NonNull String key) {
        prefs.edit().remove(key).apply();
    }

    public String getDeviceId() {
        String id = getEncryptedString("device_id", null);
        if (id == null || id.trim().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
            putEncryptedString("device_id", id);
        }
        return id;
    }

    public boolean getClearDataOnLogout() {
        return getEncryptedBoolean("clear_data_on_logout", true);
    }

    public void clearSession() {
        removeKey("current_session_id");
        removeKey("current_user_id");
    }
}
