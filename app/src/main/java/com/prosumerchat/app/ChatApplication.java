package com.prosumerchat.app;

import android.app.Application;
import android.util.Log;

/**
 * ChatApplication — Application class chính.
 *
 * Dev A: Khởi tạo Firebase, Tink, SQLCipher ở đây khi đã sẵn sàng.
 * Hiện tại chỉ là stub để Manifest compile được.
 */
public class ChatApplication extends Application {

    private static final String TAG = "ChatApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "ChatApplication started");

        // TODO Dev A (Tuần 1): Khởi tạo Firebase
        //   FirebaseApp.initializeApp(this);

        // TODO Dev A (Tuần 1): Khởi tạo Tink key management
        //   TinkKeysetManager.initialize(this);

        // TODO Dev A (Tuần 2): Crashlytics opt-in check theo privacy policy
        //   FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);
    }
}
