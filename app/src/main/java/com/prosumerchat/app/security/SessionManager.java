package com.prosumerchat.app.security;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.prosumerchat.app.data.local.room.AppDatabase;
import com.prosumerchat.app.data.remote.firebase.FirebaseAuthDataSource;
import com.prosumerchat.app.util.AppExecutors;

/**
 * SessionManager — Quản lý phiên thiết bị và đăng xuất từ xa ≤ 2 giây.
 *
 * Cơ chế: Gắn Firestore SnapshotListener vào /users/{uid}/sessions/{currentSessionId}.
 * Khi isRevoked == true hoặc document bị xóa → lập tức kích hoạt signOut + điều hướng
 * về LoginActivity trong ≤ 2 giây (Firestore SDK hỗ trợ realtime ~1s).
 *
 * Không log sessionId, uid hay bất kỳ PII nào.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";

    private static volatile SessionManager sInstance;
    private static final Object LOCK = new Object();

    private final Context mAppContext;
    private final SecurePreferencesManager mPrefs;
    private final AppExecutors mExecutors;

    @Nullable
    private ListenerRegistration mSessionListener;

    @Nullable
    private SessionRevokedCallback mCallback;

    // ── Singleton ────────────────────────────────────────────────────────────

    private SessionManager(@NonNull Context context) {
        mAppContext = context.getApplicationContext();
        mPrefs      = SecurePreferencesManager.getInstance(mAppContext);
        mExecutors  = AppExecutors.getInstance();
    }

    @NonNull
    public static SessionManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new SessionManager(context);
                }
            }
        }
        return sInstance;
    }

    // ── Callback interface ───────────────────────────────────────────────────

    public interface SessionRevokedCallback {
        /**
         * Được gọi trên Main Thread khi phiên bị thu hồi hoặc bị xóa.
         * Activity phải chuyển về LoginActivity ngay lập tức.
         */
        @MainThread
        void onSessionRevoked();
    }

    // ── Start / Stop listening ───────────────────────────────────────────────

    /**
     * Bắt đầu lắng nghe trạng thái phiên hiện tại.
     * Phải gọi sau khi đăng nhập thành công trong MainActivity.onCreate().
     *
     * @param uid      UID của người dùng hiện tại
     * @param callback Callback sẽ được gọi khi phiên bị thu hồi
     */
    @MainThread
    public void startListening(@NonNull String uid, @NonNull SessionRevokedCallback callback) {
        stopListening(); // Dọn dẹp listener cũ trước
        mCallback = callback;

        String sessionId = mPrefs.getCurrentSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            Log.w(TAG, "startListening: no current sessionId — triggering revoke");
            callback.onSessionRevoked();
            return;
        }

        // Cập nhật lastActiveAt
        updateLastActive(uid, sessionId);

        // Gắn SnapshotListener realtime
        mSessionListener = FirebaseFirestore.getInstance()
                .collection(FirebaseAuthDataSource.COL_USERS).document(uid)
                .collection(FirebaseAuthDataSource.COL_SESSIONS).document(sessionId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        // Lỗi mạng tạm thời — không tự đăng xuất, Firestore sẽ retry
                        Log.w(TAG, "Session snapshot error (network?), will retry automatically");
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        // Document bị xóa → phiên đã bị thu hồi
                        Log.i(TAG, "Session document removed — triggering revoke");
                        handleRevoke();
                        return;
                    }

                    Boolean isRevoked = snapshot.getBoolean("isRevoked");
                    if (Boolean.TRUE.equals(isRevoked)) {
                        Log.i(TAG, "Session isRevoked=true — triggering revoke");
                        handleRevoke();
                    }
                });
    }

    /**
     * Dừng lắng nghe phiên (gọi khi onDestroy hoặc trước khi đăng xuất chủ động).
     */
    public void stopListening() {
        if (mSessionListener != null) {
            mSessionListener.remove();
            mSessionListener = null;
        }
        mCallback = null;
    }

    // ── Revoke handler ───────────────────────────────────────────────────────

    /**
     * Xử lý khi phiên bị thu hồi:
     * 1. Gỡ mọi listener.
     * 2. signOut Firebase Auth.
     * 3. Xóa sessionId local.
     * 4. Nếu bật "Xóa dữ liệu khi đăng xuất" → xóa Room.
     * 5. Điều hướng về LoginActivity trên Main Thread.
     */
    private void handleRevoke() {
        stopListening();

        // signOut trên Main Thread (FirebaseAuth.signOut() là an toàn)
        FirebaseAuth.getInstance().signOut();

        // Xóa sessionId local
        boolean clearData = mPrefs.getClearDataOnLogout();
        mPrefs.clearSession();

        if (clearData) {
            mExecutors.diskIO().execute(() ->
                    AppDatabase.getInstance(mAppContext).clearAllTables());
        }

        // Gọi callback trên Main Thread
        mExecutors.mainThread().execute(() -> {
            SessionRevokedCallback cb = mCallback;
            if (cb != null) {
                cb.onSessionRevoked();
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void updateLastActive(@NonNull String uid, @NonNull String sessionId) {
        mExecutors.networkIO().execute(() -> {
            try {
                FirebaseFirestore.getInstance()
                        .collection(FirebaseAuthDataSource.COL_USERS).document(uid)
                        .collection(FirebaseAuthDataSource.COL_SESSIONS).document(sessionId)
                        .update("lastActiveAt",
                                com.google.firebase.firestore.FieldValue.serverTimestamp());
            } catch (Exception e) {
                // Best-effort — không ảnh hưởng luồng chính
            }
        });
    }

    /**
     * Tạo Intent điều hướng về LoginActivity với flag clear stack.
     * Truyền extra thông báo để UI hiển thị snackbar.
     */
    @NonNull
    public static Intent buildLoginIntentAfterRevoke(@NonNull Context context) {
        // Dùng class name để tránh import vòng tròn với ui.auth
        try {
            Class<?> loginClass = Class.forName("com.prosumerchat.app.ui.auth.LoginActivity");
            Intent intent = new Intent(context, loginClass);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("EXTRA_SESSION_REVOKED", true);
            return intent;
        } catch (ClassNotFoundException e) {
            // Fallback: để caller xử lý
            throw new IllegalStateException("LoginActivity not found", e);
        }
    }
}
