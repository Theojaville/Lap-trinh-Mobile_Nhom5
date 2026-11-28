package com.prosumerchat.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * ValidationUtils — Pure Java validation helpers (không dùng android.util.Patterns).
 * Chạy được trực tiếp trên JVM (JUnit 4) không cần Android runtime.
 *
 * Chuẩn hóa từ SRS FR-01.01, NFR-06 và Rubric yêu cầu E.164 / mật khẩu mạnh.
 */
public final class ValidationUtils {

    // ---- Username: ^[a-z0-9_]{4,30}$ ----
    private static final Pattern PATTERN_USERNAME =
            Pattern.compile("^[a-z0-9_]{4,30}$");

    // ---- Email: RFC-5321 simplified (pure Java, no android.util.Patterns) ----
    private static final Pattern PATTERN_EMAIL =
            Pattern.compile(
                    "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
            );

    // ---- Password components ----
    private static final Pattern PATTERN_PW_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern PATTERN_PW_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern PATTERN_PW_DIGIT   = Pattern.compile("[0-9]");
    private static final Pattern PATTERN_PW_SPECIAL = Pattern.compile("[^a-zA-Z0-9\\s]");

    // ---- Phone E.164 (full international) ----
    private static final Pattern PATTERN_E164 =
            Pattern.compile("^\\+[1-9]\\d{7,14}$");

    // ---- VN mobile prefix 10 digits: 0[35789]xxxxxxxx ----
    private static final Pattern PATTERN_VN_LOCAL =
            Pattern.compile("^0([35789]\\d{8})$");

    // ---- Display name ----
    private static final int DISPLAY_NAME_MIN = 2;
    private static final int DISPLAY_NAME_MAX = 50;

    private ValidationUtils() { /* utility class */ }

    // ─────────────────────────── Username ───────────────────────────────────

    /**
     * Chuẩn hóa raw input thành username lowercase, bỏ '@' đầu, trim khoảng trắng.
     * Trả về chuỗi rỗng nếu null/blank.
     */
    @NonNull
    public static String normalizeUsername(@Nullable String raw) {
        if (raw == null) return "";
        String trimmed = raw.trim().toLowerCase();
        if (trimmed.startsWith("@")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    /**
     * Kiểm tra username đã chuẩn hóa hợp lệ: ^[a-z0-9_]{4,30}$
     */
    public static boolean isValidUsername(@Nullable String username) {
        if (username == null || username.isEmpty()) return false;
        return PATTERN_USERNAME.matcher(username).matches();
    }

    // ─────────────────────────── Email ──────────────────────────────────────

    /**
     * Kiểm tra email hợp lệ (pure Java regex, không dùng android.util.Patterns).
     */
    public static boolean isValidEmail(@Nullable String email) {
        if (email == null || email.isEmpty()) return false;
        return PATTERN_EMAIL.matcher(email.trim()).matches();
    }

    // ─────────────────────────── Password ───────────────────────────────────

    /**
     * Kiểm tra mật khẩu mạnh: ≥8 ký tự, có chữ hoa, chữ thường, số, ký tự đặc biệt.
     */
    public static boolean isValidPassword(@Nullable String password) {
        if (password == null || password.length() < 8) return false;
        return PATTERN_PW_UPPER.matcher(password).find()
                && PATTERN_PW_LOWER.matcher(password).find()
                && PATTERN_PW_DIGIT.matcher(password).find()
                && PATTERN_PW_SPECIAL.matcher(password).find();
    }

    // ─────────────────────────── Phone Number ───────────────────────────────

    /**
     * Chuẩn hóa số điện thoại về E.164:
     * - Bỏ khoảng trắng, dấu '-', '(', ')'.
     * - Nếu dạng VN 10 số 0[35789]xxxxxxxx → +84xxxxxxxxx.
     * - Nếu đã có '+' kiểm tra E.164 trực tiếp.
     * - Trả về chuỗi rỗng nếu null/blank (trường SĐT không bắt buộc).
     * - Ném IllegalArgumentException nếu có nội dung nhưng không hợp lệ.
     */
    @NonNull
    public static String normalizePhoneNumber(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String cleaned = raw.trim().replaceAll("[\\s\\-().]+", "");
        // Chuyển số VN 10 chữ số bắt đầu 0 → +84
        java.util.regex.Matcher vnMatcher = PATTERN_VN_LOCAL.matcher(cleaned);
        if (vnMatcher.matches()) {
            return "+84" + vnMatcher.group(1);
        }
        // Kiểm tra E.164
        if (PATTERN_E164.matcher(cleaned).matches()) {
            return cleaned;
        }
        throw new IllegalArgumentException("Invalid phone number: not E.164 format");
    }

    /**
     * Kiểm tra chuỗi đã chuẩn hóa có phải E.164 hợp lệ không.
     * Trả về true nếu rỗng (SĐT tùy chọn).
     */
    public static boolean isValidPhoneNumberOrEmpty(@Nullable String normalized) {
        if (normalized == null || normalized.isEmpty()) return true;
        return PATTERN_E164.matcher(normalized).matches();
    }

    // ─────────────────────────── Display Name ───────────────────────────────

    /**
     * Kiểm tra tên hiển thị hợp lệ: 2–50 ký tự không rỗng.
     */
    public static boolean isValidDisplayName(@Nullable String name) {
        if (name == null) return false;
        String trimmed = name.trim();
        return trimmed.length() >= DISPLAY_NAME_MIN && trimmed.length() <= DISPLAY_NAME_MAX;
    }

    // ─────────────────────────── Identifier helper ──────────────────────────

    /**
     * Xác định identifier nhập vào là email hay username.
     * Trả về true nếu là email (chứa '@' nhưng không bắt đầu bằng '@').
     */
    public static boolean isEmailIdentifier(@Nullable String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return false;
        String trimmed = identifier.trim();
        return !trimmed.startsWith("@") && trimmed.contains("@");
    }
}
