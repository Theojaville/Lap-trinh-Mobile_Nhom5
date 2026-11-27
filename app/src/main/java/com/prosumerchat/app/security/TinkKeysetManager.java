package com.prosumerchat.app.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.aead.AeadConfig;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * TinkKeysetManager — Quản lý khóa mật mã bằng Google Tink (AEAD AES-256-GCM)
 * kết hợp Master Key phần cứng trong Android Keystore.
 *
 * Chịu trách nhiệm:
 * - Khởi tạo keyset AES-256-GCM bọc qua Android Keystore (không lưu plain key).
 * - Sinh và bảo vệ raw key 256-bit (32 bytes) cho SQLCipher (NFR-01, FR-07.01).
 * - Cung cấp API mã hóa/giải mã byte[] và String cho tầng lưu trữ cục bộ.
 */
public final class TinkKeysetManager {

    private static final String TAG = "TinkKeysetManager";

    static final String KEYSET_PREFS = "prosumerchat_keyset_prefs";
    static final String SECURE_KEY_PREFS = "prosumerchat_secure_prefs";
    private static final String KEYSET_NAME = "prosumerchat_aead_keyset";
    private static final String MASTER_KEY_URI = "android-keystore://prosumerchat_master_key";
    private static final String KEY_ENCRYPTED_DB_PASS = "encrypted_sqlcipher_key_v1";
    private static final String DB_NAME = "prosumerchat.db";

    private static final byte[] DB_KEY_AAD =
            "prosumerchat_sqlcipher_db_key_v1".getBytes(StandardCharsets.UTF_8);
    private static final int SQLCIPHER_KEY_BYTES = 32; // 256-bit raw key

    private static volatile Aead sAead;
    private static final Object LOCK = new Object();

    private TinkKeysetManager() {
        // Utility class
    }

    /**
     * Khởi tạo Tink AEAD keyset được bảo vệ bởi Android Keystore.
     * An toàn khi gọi nhiều lần (idempotent) và thread-safe.
     */
    public static void initialize(@NonNull Context context) {
        if (sAead != null) {
            return;
        }
        synchronized (LOCK) {
            if (sAead != null) {
                return;
            }
            Context appContext = context.getApplicationContext();
            try {
                sAead = buildAead(appContext);
            } catch (GeneralSecurityException | IOException e) {
                // Nếu keyset hoặc Android Keystore bị corrupt/reset (vd: đổi ROM hoặc restore lỗi),
                // dọn keyset cũ + dữ liệu không thể giải mã để khôi phục an toàn thay vì crash loop.
                Log.e(TAG, "Keyset recovery triggered due to Keystore/keyset error", e);
                resetCorruptedKeyState(appContext);
                try {
                    sAead = buildAead(appContext);
                } catch (GeneralSecurityException | IOException retryEx) {
                    throw new IllegalStateException(
                            "Unable to initialize Tink AEAD keyset", retryEx);
                }
            }
        }
    }

    @NonNull
    private static Aead buildAead(@NonNull Context appContext)
            throws GeneralSecurityException, IOException {
        AeadConfig.register();
        AndroidKeysetManager keysetManager = new AndroidKeysetManager.Builder()
                .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREFS)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build();
        return keysetManager.getKeysetHandle()
                .getPrimitive(RegistryConfiguration.get(), Aead.class);
    }

    private static void resetCorruptedKeyState(@NonNull Context appContext) {
        appContext.getSharedPreferences(KEYSET_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        appContext.getSharedPreferences(SECURE_KEY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit();
        // Database cũ được mã hóa bằng key cũ không thể giải mã nữa nếu keyset bị mất.
        appContext.deleteDatabase(DB_NAME);
    }

    @NonNull
    private static Aead requireAead(@NonNull Context context) {
        if (sAead == null) {
            initialize(context);
        }
        return sAead;
    }

    /**
     * Mã hóa mảng byte bằng Tink AEAD (AES-256-GCM).
     */
    @NonNull
    public static byte[] encrypt(
            @NonNull Context context,
            @NonNull byte[] plaintext,
            @Nullable byte[] associatedData) throws GeneralSecurityException {
        byte[] aad = associatedData != null ? associatedData : new byte[0];
        return requireAead(context).encrypt(plaintext, aad);
    }

    /**
     * Giải mã mảng byte bằng Tink AEAD (AES-256-GCM).
     */
    @NonNull
    public static byte[] decrypt(
            @NonNull Context context,
            @NonNull byte[] ciphertext,
            @Nullable byte[] associatedData) throws GeneralSecurityException {
        byte[] aad = associatedData != null ? associatedData : new byte[0];
        return requireAead(context).decrypt(ciphertext, aad);
    }

    /**
     * Mã hóa chuỗi UTF-8 thành chuỗi Base64 (NO_WRAP).
     */
    @Nullable
    public static String encryptString(
            @NonNull Context context,
            @Nullable String plaintext,
            @Nullable String aadContext) throws GeneralSecurityException {
        if (plaintext == null) {
            return null;
        }
        byte[] aad = aadContext != null
                ? aadContext.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        byte[] encrypted = encrypt(context, plaintext.getBytes(StandardCharsets.UTF_8), aad);
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    /**
     * Giải mã chuỗi Base64 về chuỗi UTF-8 gốc.
     */
    @Nullable
    public static String decryptString(
            @NonNull Context context,
            @Nullable String ciphertextBase64,
            @Nullable String aadContext) throws GeneralSecurityException {
        if (ciphertextBase64 == null) {
            return null;
        }
        byte[] aad = aadContext != null
                ? aadContext.getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        byte[] decoded = Base64.decode(ciphertextBase64, Base64.NO_WRAP);
        byte[] decrypted = decrypt(context, decoded, aad);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Lấy raw key 256-bit (32 bytes) cho SQLCipher.
     * Lần đầu tiên: sinh ngẫu nhiên 32 bytes bằng SecureRandom, mã hóa bọc bằng Tink AEAD
     * và lưu bản mã vào SharedPreferences riêng.
     * Các lần sau: đọc bản mã và giải mã lấy lại đúng 32 bytes gốc (không dùng PBKDF2).
     *
     * Nên gọi trên background thread (diskIO) để đảm bảo Cold Start < 1.5s.
     */
    @NonNull
    public static byte[] getDatabasePassphrase(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        synchronized (LOCK) {
            initialize(appContext);
            SharedPreferences prefs =
                    appContext.getSharedPreferences(SECURE_KEY_PREFS, Context.MODE_PRIVATE);
            String encodedWrappedKey = prefs.getString(KEY_ENCRYPTED_DB_PASS, null);

            if (encodedWrappedKey != null) {
                try {
                    byte[] wrappedKey = Base64.decode(encodedWrappedKey, Base64.NO_WRAP);
                    byte[] rawKey = decrypt(appContext, wrappedKey, DB_KEY_AAD);
                    if (rawKey.length == SQLCIPHER_KEY_BYTES) {
                        return rawKey;
                    }
                    Log.w(TAG, "Stored SQLCipher key had unexpected length; regenerating");
                } catch (GeneralSecurityException | IllegalArgumentException e) {
                    Log.e(TAG, "Failed to decrypt SQLCipher key; resetting DB key state", e);
                }
                // Nếu giải mã khóa DB thất bại, xóa DB cũ không thể mở và tạo khóa mới an toàn
                appContext.deleteDatabase(DB_NAME);
            }

            byte[] newRawKey = new byte[SQLCIPHER_KEY_BYTES];
            new SecureRandom().nextBytes(newRawKey);
            try {
                byte[] wrapped = encrypt(appContext, newRawKey, DB_KEY_AAD);
                String encoded = Base64.encodeToString(wrapped, Base64.NO_WRAP);
                prefs.edit().putString(KEY_ENCRYPTED_DB_PASS, encoded).commit();
                return newRawKey;
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Unable to wrap SQLCipher database key", e);
            }
        }
    }
}
