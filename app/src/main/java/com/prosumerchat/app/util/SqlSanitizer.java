package com.prosumerchat.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * SqlSanitizer — Helper làm sạch chuỗi tìm kiếm cho truy vấn SQLite LIKE
 * (SRS mục 5.2: chống wildcard injection & SQL injection).
 *
 * Pure Java — chạy độc lập được trên JVM Unit Test.
 * Luôn sử dụng kết hợp với Parameterized Query (:param) và mệnh đề {@code ESCAPE '\'}.
 */
public final class SqlSanitizer {

    private SqlSanitizer() {
        // Utility class
    }

    /**
     * Escape các ký tự đặc biệt trong SQLite LIKE: '\', '%', '_'.
     * Lưu ý phải escape '\' trước tiên để tránh double-escape.
     *
     * @param rawQuery Từ khóa tìm kiếm thô từ người dùng
     * @return Chuỗi đã trim và escape an toàn, không bao giờ null
     */
    @NonNull
    public static String sanitizeLikeQuery(@Nullable String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String trimmed = rawQuery.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Tạo pattern dạng {@code %sanitized%} để truyền trực tiếp vào {@code LIKE :pattern ESCAPE '\'}.
     */
    @NonNull
    public static String buildContainsLikePattern(@Nullable String rawQuery) {
        return "%" + sanitizeLikeQuery(rawQuery) + "%";
    }
}
