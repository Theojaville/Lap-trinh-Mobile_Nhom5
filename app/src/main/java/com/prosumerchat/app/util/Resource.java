package com.prosumerchat.app.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

/**
 * Resource<T> — Wrapper trạng thái cho LiveData theo pattern MVI / Repository.
 *
 * Hỗ trợ 3 trạng thái: LOADING, SUCCESS, ERROR.
 * Kèm errorCode (Firebase/HTTP code) và messageResId (R.string.*)
 * để hiển thị lỗi đa ngôn ngữ mà không cần Context trong Repository/ViewModel.
 */
public final class Resource<T> {

    public enum Status {
        LOADING,
        SUCCESS,
        ERROR
    }

    @NonNull
    public final Status status;

    @Nullable
    public final T data;

    /** Mã lỗi kỹ thuật (Firebase error code, HTTP status...). Null khi SUCCESS/LOADING. */
    @Nullable
    public final String errorCode;

    /** String resource ID để hiển thị thông báo đa ngôn ngữ. 0 nếu không có. */
    @StringRes
    public final int messageResId;

    /** Thông báo lỗi fallback (tiếng Anh/không dịch). Null khi SUCCESS/LOADING. */
    @Nullable
    public final String message;

    private Resource(@NonNull Status status,
                     @Nullable T data,
                     @Nullable String errorCode,
                     @StringRes int messageResId,
                     @Nullable String message) {
        this.status = status;
        this.data = data;
        this.errorCode = errorCode;
        this.messageResId = messageResId;
        this.message = message;
    }

    // ─────────────────── Factory methods ───────────────────────────────────

    @NonNull
    public static <T> Resource<T> loading() {
        return new Resource<>(Status.LOADING, null, null, 0, null);
    }

    @NonNull
    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null, 0, null);
    }

    @NonNull
    public static <T> Resource<T> error(@StringRes int messageResId, @Nullable String errorCode) {
        return new Resource<>(Status.ERROR, null, errorCode, messageResId, null);
    }

    @NonNull
    public static <T> Resource<T> error(@Nullable String message, @Nullable String errorCode) {
        return new Resource<>(Status.ERROR, null, errorCode, 0, message);
    }

    @NonNull
    public static <T> Resource<T> error(@StringRes int messageResId,
                                        @Nullable String errorCode,
                                        @Nullable T data) {
        return new Resource<>(Status.ERROR, data, errorCode, messageResId, null);
    }

    // ─────────────────── Helpers ────────────────────────────────────────────

    public boolean isLoading() { return status == Status.LOADING; }
    public boolean isSuccess() { return status == Status.SUCCESS; }
    public boolean isError()   { return status == Status.ERROR;   }

    @Override
    @NonNull
    public String toString() {
        return "Resource{status=" + status
                + (data != null ? ", data=<present>" : "")
                + (errorCode != null ? ", errorCode=" + errorCode : "")
                + (messageResId != 0 ? ", messageResId=" + messageResId : "")
                + "}";
    }
}
