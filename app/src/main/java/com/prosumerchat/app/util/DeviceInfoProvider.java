package com.prosumerchat.app.util;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.prosumerchat.app.security.SecurePreferencesManager;

/**
 * DeviceInfoProvider — Cung cấp thông tin thiết bị để ghi session Firestore.
 *
 * Nguồn deviceId: UUID 128-bit được sinh lần đầu và lưu mã hóa qua SecurePreferencesManager.
 * Không dùng ANDROID_ID (không ổn định, có thể reset).
 */
public final class DeviceInfoProvider {

    private DeviceInfoProvider() { /* utility class */ }

    /**
     * Lấy deviceId duy nhất của thiết bị (UUID sinh 1 lần, lưu trong SecurePreferencesManager).
     */
    @NonNull
    public static String getDeviceId(@NonNull Context context) {
        return SecurePreferencesManager.getInstance(context).getDeviceId();
    }

    /**
     * Tên thiết bị: "Manufacturer Model" (ví dụ: "Samsung Galaxy S22").
     * Không log vì có thể chứa thông tin định danh máy.
     */
    @NonNull
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    /**
     * Phiên bản OS: "Android X.Y" (ví dụ: "Android 14").
     */
    @NonNull
    public static String getOsVersion() {
        return "Android " + Build.VERSION.RELEASE;
    }

    @NonNull
    private static String capitalize(@NonNull String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
