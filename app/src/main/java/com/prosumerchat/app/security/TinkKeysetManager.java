package com.prosumerchat.app.security;

import android.content.Context;

/**
 * TinkKeysetManager — Helper quản lý cryptographic keyset dùng Google Tink + Android Keystore.
 *
 * Dev A (Tuần 1-2): Implement các method dưới đây.
 *
 * Mục đích:
 *   - Tạo/lưu trữ AES-GCM keyset trong Android Keystore (không lưu plain key)
 *   - Cung cấp passphrase cho SQLCipher (AppDatabase.getInstance)
 *   - Mã hóa/giải mã SharedPreferences nhạy cảm
 *
 * Tham khảo:
 *   - https://developers.google.com/tink/generate-plaintext-keyset
 *   - https://developer.android.com/reference/androidx/security/crypto/MasterKey
 */
public class TinkKeysetManager {

    private static final String TAG = "TinkKeysetManager";
    private static final String KEYSET_PREFS = "prosumerchat_keyset_prefs";

    // TODO Dev A: Khởi tạo Tink registry và load/tạo keyset từ Android Keystore.
    public static void initialize(Context context) {
        // TODO Dev A: Implement
        //   TinkConfig.register();
        //   hoặc AeadConfig.register();
        //   Load hoặc generate keyset và lưu vào KeysetHandle qua AndroidKeystore.
        throw new UnsupportedOperationException("TinkKeysetManager chưa được implement — Dev A làm Tuần 1-2");
    }

    /**
     * Lấy passphrase dùng để mở SQLCipher database.
     * KHÔNG gọi method này trước khi initialize().
     *
     * @return byte[] passphrase (derived từ Tink keyset)
     */
    public static byte[] getDatabasePassphrase(Context context) {
        // TODO Dev A: Implement — derive passphrase từ keyset, KHÔNG hardcode.
        throw new UnsupportedOperationException("TinkKeysetManager chưa được implement — Dev A làm Tuần 1-2");
    }
}
