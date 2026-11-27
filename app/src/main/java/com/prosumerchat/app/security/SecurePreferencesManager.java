package com.prosumerchat.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.util.UUID;

/**
 * SecurePreferencesManager — Lưu trữ an toàn cấu hình phiên đăng nhập,
 * định danh thiết bị, trạng thái khóa sinh trắc học và tùy chọn bảo mật.
 *
 * Không sử dụng EncryptedSharedPreferences (đã deprecated).
 * Mọi giá trị được mã hóa riêng lẻ bằng Tink AEAD (AES-256-GCM) thông qua
 * {@link TinkKeysetManager} với Associated Data (AAD) gắn theo tên khóa để
 * ngăn chặn tráo đổi ciphertext giữa các key và không bao giờ lưu plaintext.
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

    // ── Session & Device ──────────────────────────────────────────────────────

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
     * Lấy Device ID ổn định trên thiết bị; sinh UUID mới ở lần đầu và lưu mã hóa.
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

    // ── Biometric Lock ────────────────────────────────────────────────────────

    public boolean isBiometricEnabled() {
        return getEncryptedBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        putEncryptedBoolean(KEY_BIOMETRIC_ENABLED, enabled);
    }

    /**
     * Thời gian chờ khóa sinh trắc học (giây): 0 (ngay lập tức), 60 (1 phút), 300 (5 phút).
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

    // ── Privacy / Logout / Language ───────────────────────────────────────────

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
     * Xóa thông tin phiên hiện tại khi đăng xuất nhưng vẫn giữ lại deviceId và ngôn ngữ.
     */
    public void clearSessionState() {
        prefs.edit()
                .remove(KEY_CURRENT_SESSION_ID)
                .remove(KEY_LAST_BACKGROUND_TIMESTAMP)
                .remove(KEY_FCM_TOKEN)
                .apply();
    }

    // ── Internal Tink AEAD helpers ────────────────────────────────────────────

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
}
