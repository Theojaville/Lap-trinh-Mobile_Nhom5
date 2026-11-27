package com.prosumerchat.app;

import android.app.Application;
import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.prosumerchat.app.data.local.room.AppDatabase;
import com.prosumerchat.app.security.SecurePreferencesManager;
import com.prosumerchat.app.security.TinkKeysetManager;
import com.prosumerchat.app.util.AppExecutors;

/**
 * ChatApplication — Application class chính của ứng dụng ProsumerChat.
 *
 * Chịu trách nhiệm khởi tạo các thành phần cốt lõi:
 * - FirebaseApp & cấu hình FirebaseCrashlytics (chống rò rỉ dữ liệu / opt-in DEBUG).
 * - TinkKeysetManager & pre-warm AppDatabase trên diskIO (đảm bảo Cold Start < 1.5s).
 * - Khôi phục và áp dụng ngôn ngữ đã lưu qua AppCompatDelegate.
 */
public class ChatApplication extends Application {

    private static final String TAG = "ChatApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ChatApplication starting initialization...");

        // 1. Khởi tạo Firebase App
        FirebaseApp.initializeApp(this);

        // 2. Cấu hình Firebase Crashlytics
        // Tắt thu thập crash tự động trong môi trường DEBUG để tránh nhiễu log phát triển
        boolean isDebug = BuildConfig.DEBUG;
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!isDebug);

        // 3. Khởi tạo Tink AEAD Keyset và pre-warm Room DB trên background thread (diskIO)
        // Tuyệt đối không block Main Thread để đảm bảo Cold Start < 1.5s (NFR-01)
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                TinkKeysetManager.initialize(getApplicationContext());
                // Pre-warm AppDatabase để load SQLCipher native library và mở connection sẵn
                AppDatabase.getInstance(getApplicationContext());
                long elapsed = System.currentTimeMillis() - startTime;
                Log.i(TAG, "Security & AppDatabase pre-warmed successfully in " + elapsed + "ms");
            } catch (Exception e) {
                Log.e(TAG, "Error pre-warming security and database", e);
            }
        });

        // 4. Áp dụng ngôn ngữ ưa thích đã lưu
        applySavedLanguage();
    }

    private void applySavedLanguage() {
        try {
            SecurePreferencesManager prefs = SecurePreferencesManager.getInstance(this);
            String savedLanguage = prefs.getPreferredLanguage();
            LocaleListCompat currentLocales = AppCompatDelegate.getApplicationLocales();
            if (currentLocales.isEmpty() || !savedLanguage.equalsIgnoreCase(currentLocales.toLanguageTags())) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLanguage));
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to apply preferred language during app startup", e);
        }
    }
}
