package com.prosumerchat.app.data.remote.firebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * FirebaseAuthDataSource — Data source tầng remote bọc các thao tác
 * FirebaseAuth và FirebaseFirestore liên quan đến xác thực và phiên thiết bị.
 *
 * Không log bất kỳ mật khẩu, token, hay email người dùng ra Logcat.
 * Mọi callback đều trả Task<> để Repository/ViewModel xử lý tiếp trên luồng phù hợp.
 */
public class FirebaseAuthDataSource {

    // ── Firestore collection paths ──────────────────────────────────────────
    public static final String COL_USERS     = "users";
    public static final String COL_USERNAMES = "usernames";
    public static final String COL_CHATS     = "chats";
    public static final String COL_SESSIONS  = "sessions";

    private final FirebaseAuth      mAuth;
    private final FirebaseFirestore mFirestore;

    public FirebaseAuthDataSource() {
        mAuth      = FirebaseAuth.getInstance();
        mFirestore = FirebaseFirestore.getInstance();
    }

    // ─────────────────────── Auth getters ──────────────────────────────────

    @Nullable
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void signOut() {
        mAuth.signOut();
    }

    // ─────────────────────── Register flow ─────────────────────────────────

    /**
     * Kiểm tra username đã tồn tại trong /usernames/{username} chưa.
     * Trả Task<Boolean>: true = đã tồn tại (conflict), false = chưa có.
     */
    @NonNull
    public Task<Boolean> checkUsernameExists(@NonNull String username) {
        return mFirestore.collection(COL_USERNAMES)
                .document(username)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) return false;
                    return task.getResult().exists();
                });
    }

    /**
     * Tạo tài khoản Firebase Auth.
     * Trả Task<FirebaseUser>.
     */
    @NonNull
    public Task<FirebaseUser> createUser(@NonNull String email, @NonNull String password) {
        return mAuth.createUserWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("createUser failed");
                    }
                    return task.getResult().getUser();
                });
    }

    /**
     * Ghi atomically 3 documents bằng WriteBatch:
     * - /users/{uid}
     * - /usernames/{username}
     * - /chats/saved_{uid}
     *
     * Phải gọi sau createUser() để có uid hợp lệ.
     */
    @NonNull
    public Task<Void> writeRegistrationBatch(
            @NonNull String uid,
            @NonNull String email,
            @NonNull String username,
            @NonNull String displayName,
            @NonNull String phoneNumber) {

        WriteBatch batch = mFirestore.batch();
        Timestamp now = Timestamp.now();

        // ── /users/{uid} ──────────────────────────────────────────────────
        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("uid", uid);
        userDoc.put("email", email);
        userDoc.put("username", username);
        userDoc.put("displayName", displayName);
        userDoc.put("phoneNumber", phoneNumber);
        userDoc.put("bio", "");
        userDoc.put("birthday", "");
        userDoc.put("avatarUrl", "");
        userDoc.put("emailVerified", false);
        userDoc.put("createdAt", now);
        userDoc.put("updatedAt", now);
        // privacy map defaults
        Map<String, Object> privacyMap = new HashMap<>();
        privacyMap.put("avatarVisibility", "EVERYONE");
        privacyMap.put("lastSeenVisibility", "EVERYONE");
        privacyMap.put("readReceiptsEnabled", true);
        privacyMap.put("allowFindByPhone", true);
        userDoc.put("privacy", privacyMap);

        batch.set(mFirestore.collection(COL_USERS).document(uid), userDoc);

        // ── /usernames/{username} ─────────────────────────────────────────
        Map<String, Object> usernameDoc = new HashMap<>();
        usernameDoc.put("uid", uid);
        usernameDoc.put("email", email);
        usernameDoc.put("createdAt", now);

        batch.set(mFirestore.collection(COL_USERNAMES).document(username), usernameDoc);

        // ── /chats/saved_{uid} (FR-01.04) ────────────────────────────────
        Map<String, Object> savedChatDoc = new HashMap<>();
        savedChatDoc.put("type", "SAVED");
        savedChatDoc.put("memberIds", Arrays.asList(uid));
        savedChatDoc.put("title", "Saved Messages");
        savedChatDoc.put("isPinned", true);
        savedChatDoc.put("lastMessageTime", now);
        savedChatDoc.put("createdAt", now);
        savedChatDoc.put("updatedAt", now);

        batch.set(mFirestore.collection(COL_CHATS).document("saved_" + uid), savedChatDoc);

        return batch.commit();
    }

    /**
     * Rollback: xóa FirebaseAuth user khi WriteBatch thất bại (tránh orphan Auth account).
     */
    @NonNull
    public Task<Void> deleteAuthUser(@NonNull FirebaseUser user) {
        return user.delete();
    }

    /**
     * Gửi email xác minh đến FirebaseUser hiện tại.
     */
    @NonNull
    public Task<Void> sendEmailVerification(@NonNull FirebaseUser user) {
        return user.sendEmailVerification();
    }

    // ─────────────────────── Login flow ────────────────────────────────────

    /**
     * Lấy email từ /usernames/{username} để hỗ trợ đăng nhập bằng @username.
     * Trả Task<String>: email hoặc null nếu không tìm thấy.
     */
    @NonNull
    public Task<String> getEmailByUsername(@NonNull String username) {
        return mFirestore.collection(COL_USERNAMES)
                .document(username)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null
                            || !task.getResult().exists()) {
                        return null;
                    }
                    return task.getResult().getString("email");
                });
    }

    /**
     * Đăng nhập bằng email + password.
     * Trả Task<FirebaseUser>.
     */
    @NonNull
    public Task<FirebaseUser> signInWithEmail(@NonNull String email, @NonNull String password) {
        return mAuth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("signIn failed");
                    }
                    return task.getResult().getUser();
                });
    }

    /**
     * Reload FirebaseUser để lấy trạng thái emailVerified mới nhất.
     */
    @NonNull
    public Task<Void> reloadCurrentUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return Tasks.forException(new Exception("No current user"));
        return user.reload();
    }

    /**
     * Cập nhật trường emailVerified = true trên /users/{uid}.
     */
    @NonNull
    public Task<Void> syncEmailVerified(@NonNull String uid) {
        Map<String, Object> update = new HashMap<>();
        update.put("emailVerified", true);
        update.put("updatedAt", FieldValue.serverTimestamp());
        return mFirestore.collection(COL_USERS).document(uid).update(update);
    }

    // ─────────────────────── Session ───────────────────────────────────────

    /**
     * Ghi document phiên mới vào /users/{uid}/sessions/{sessionId}.
     */
    @NonNull
    public Task<Void> writeSession(
            @NonNull String uid,
            @NonNull String sessionId,
            @NonNull String deviceId,
            @NonNull String deviceName,
            @NonNull String osVersion,
            @NonNull String fcmToken) {

        Map<String, Object> sessionDoc = new HashMap<>();
        sessionDoc.put("sessionId", sessionId);
        sessionDoc.put("deviceId", deviceId);
        sessionDoc.put("deviceName", deviceName);
        sessionDoc.put("osVersion", osVersion);
        sessionDoc.put("fcmToken", fcmToken);
        sessionDoc.put("isRevoked", false);
        Timestamp now = Timestamp.now();
        sessionDoc.put("createdAt", now);
        sessionDoc.put("lastActiveAt", now);

        return mFirestore.collection(COL_USERS).document(uid)
                .collection(COL_SESSIONS).document(sessionId)
                .set(sessionDoc);
    }

    /**
     * Cập nhật lastActiveAt cho phiên hiện tại.
     */
    @NonNull
    public Task<Void> updateSessionLastActive(@NonNull String uid, @NonNull String sessionId) {
        Map<String, Object> update = new HashMap<>();
        update.put("lastActiveAt", FieldValue.serverTimestamp());
        return mFirestore.collection(COL_USERS).document(uid)
                .collection(COL_SESSIONS).document(sessionId)
                .update(update);
    }

    /**
     * Xóa document phiên khi đăng xuất.
     */
    @NonNull
    public Task<Void> deleteSession(@NonNull String uid, @NonNull String sessionId) {
        return mFirestore.collection(COL_USERS).document(uid)
                .collection(COL_SESSIONS).document(sessionId)
                .delete();
    }

    /**
     * Thu hồi tất cả session khác (isRevoked = true) trừ currentSessionId.
     * Dùng sau khi đổi mật khẩu (FR-01.06).
     * Trả Task<Void> kết hợp từ WriteBatch.
     */
    @NonNull
    public Task<Void> revokeOtherSessions(@NonNull String uid, @NonNull String currentSessionId) {
        return mFirestore.collection(COL_USERS).document(uid)
                .collection(COL_SESSIONS)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new Exception("revokeOtherSessions failed"));
                    }
                    WriteBatch batch = mFirestore.batch();
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        if (!doc.getId().equals(currentSessionId)) {
                            batch.update(doc.getReference(), "isRevoked", true);
                        }
                    }
                    return batch.commit();
                });
    }

    // ─────────────────────── Forgot / Change Password ──────────────────────

    /**
     * Gửi email đặt lại mật khẩu.
     */
    @NonNull
    public Task<Void> sendPasswordResetEmail(@NonNull String email) {
        return mAuth.sendPasswordResetEmail(email);
    }

    /**
     * Xác thực lại bằng mật khẩu cũ trước khi đổi mật khẩu.
     */
    @NonNull
    public Task<Void> reauthenticate(@NonNull String email, @NonNull String oldPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return Tasks.forException(new Exception("No current user"));
        AuthCredential credential = EmailAuthProvider.getCredential(email, oldPassword);
        return user.reauthenticate(credential);
    }

    /**
     * Cập nhật mật khẩu mới.
     */
    @NonNull
    public Task<Void> updatePassword(@NonNull String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return Tasks.forException(new Exception("No current user"));
        return user.updatePassword(newPassword);
    }

    // ─────────────────────── Profile read ──────────────────────────────────

    /**
     * Đọc profile từ /users/{uid}.
     */
    @NonNull
    public Task<DocumentSnapshot> getUserDocument(@NonNull String uid) {
        return mFirestore.collection(COL_USERS).document(uid).get();
    }

    // ─────────────────────── Error classification helpers ──────────────────

    /**
     * Ánh xạ Firebase exception sang error code string.
     * Không dùng thông báo gốc (tránh lộ NFR-06).
     */
    @NonNull
    public static String classifyAuthError(@Nullable Exception e) {
        if (e == null) return "UNKNOWN";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "INVALID_CREDENTIALS";
        if (e instanceof FirebaseAuthInvalidUserException)        return "INVALID_USER";
        if (e instanceof FirebaseAuthUserCollisionException)      return "EMAIL_COLLISION";
        String msg = e.getMessage();
        if (msg != null && msg.contains("TOO_MANY_REQUESTS"))    return "TOO_MANY_REQUESTS";
        if (msg != null && msg.contains("NETWORK_ERROR"))         return "NETWORK_ERROR";
        return "UNKNOWN";
    }
}
