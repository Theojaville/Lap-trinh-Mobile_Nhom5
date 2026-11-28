package com.prosumerchat.app.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.prosumerchat.app.R;
import com.prosumerchat.app.data.local.room.AppDatabase;
import com.prosumerchat.app.data.local.room.dao.UserDao;
import com.prosumerchat.app.data.local.room.entity.UserEntity;
import com.prosumerchat.app.data.remote.firebase.FirebaseAuthDataSource;
import com.prosumerchat.app.security.SecurePreferencesManager;
import com.prosumerchat.app.util.AppExecutors;
import com.prosumerchat.app.util.DeviceInfoProvider;
import com.prosumerchat.app.util.Resource;
import com.prosumerchat.app.util.ValidationUtils;

import java.util.UUID;

/**
 * AuthRepository — Tầng repository xử lý toàn bộ luồng xác thực.
 *
 * Tuân thủ:
 * - FR-01.01: Đăng ký WriteBatch 3 doc + rollback + gửi mail xác minh.
 * - FR-01.02: Đăng nhập Email/@username + lưu session + NFR-06 (không lộ user tồn tại).
 * - FR-01.05: Ghi session /users/{uid}/sessions/{sessionId}.
 * - FR-01.06: Quên/đổi mật khẩu + thu hồi session khác.
 * - NFR-06  : Lỗi đăng nhập không tiết lộ email/username có tồn tại không.
 *
 * Không log mật khẩu, token, raw key, hay email.
 */
public class AuthRepository {

    private final FirebaseAuthDataSource mDataSource;
    private final SecurePreferencesManager mPrefs;
    private final UserDao mUserDao;
    private final AppExecutors mExecutors;
    private final Context mContext;

    public AuthRepository(@NonNull Context context) {
        mContext    = context.getApplicationContext();
        mDataSource = new FirebaseAuthDataSource();
        mPrefs      = SecurePreferencesManager.getInstance(mContext);
        mUserDao    = AppDatabase.getInstance(mContext).userDao();
        mExecutors  = AppExecutors.getInstance();
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. ĐĂNG KÝ (FR-01.01)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Đăng ký tài khoản mới.
     * Flow: check username → createUser → WriteBatch → rollback nếu lỗi → sendVerificationEmail.
     *
     * @param displayName  Tên hiển thị (2–50 ký tự)
     * @param username     Username đã chuẩn hóa (4–30, [a-z0-9_])
     * @param email        Email hợp lệ
     * @param password     Mật khẩu mạnh (≥8, hoa/thường/số/đặc biệt)
     * @param phoneNumber  SĐT E.164 hoặc rỗng
     */
    @NonNull
    public LiveData<Resource<Void>> register(
            @NonNull String displayName,
            @NonNull String username,
            @NonNull String email,
            @NonNull String password,
            @NonNull String phoneNumber) {

        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        // B1: Kiểm tra username chưa tồn tại
        mDataSource.checkUsernameExists(username)
                .addOnSuccessListener(exists -> {
                    if (exists) {
                        result.postValue(Resource.error(
                                R.string.error_username_taken, "USERNAME_TAKEN"));
                        return;
                    }
                    // B2: Tạo Auth user
                    mDataSource.createUser(email, password)
                            .addOnSuccessListener(firebaseUser -> {
                                if (firebaseUser == null) {
                                    result.postValue(Resource.error(
                                            R.string.error_register_failed, "NULL_USER"));
                                    return;
                                }
                                String uid = firebaseUser.getUid();
                                // B3: WriteBatch 3 documents
                                mDataSource.writeRegistrationBatch(
                                        uid, email, username, displayName, phoneNumber)
                                        .addOnSuccessListener(aVoid -> {
                                            // B4: Gửi email xác minh
                                            mDataSource.sendEmailVerification(firebaseUser)
                                                    .addOnCompleteListener(emailTask -> {
                                                        // Dù email xác minh thành công hay không
                                                        // vẫn coi đăng ký thành công
                                                        result.postValue(Resource.success(null));
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            // B4-rollback: Xóa Auth user để tránh orphan account
                                            mDataSource.deleteAuthUser(firebaseUser)
                                                    .addOnCompleteListener(ignored ->
                                                            result.postValue(Resource.error(
                                                                    R.string.error_register_failed,
                                                                    "BATCH_FAILED")));
                                        });
                            })
                            .addOnFailureListener(e -> {
                                String code = FirebaseAuthDataSource.classifyAuthError(e);
                                int resId = "EMAIL_COLLISION".equals(code)
                                        ? R.string.error_email_already_in_use
                                        : R.string.error_register_failed;
                                result.postValue(Resource.error(resId, code));
                            });
                })
                .addOnFailureListener(e ->
                        result.postValue(Resource.error(
                                R.string.error_network, "NETWORK_ERROR")));

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. XÁC MINH EMAIL
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Reload FirebaseUser và đồng bộ emailVerified lên Firestore + Room nếu đã xác minh.
     */
    @NonNull
    public LiveData<Resource<Boolean>> checkEmailVerified() {
        MutableLiveData<Resource<Boolean>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        mDataSource.reloadCurrentUser()
                .addOnSuccessListener(aVoid -> {
                    FirebaseUser user = mDataSource.getCurrentUser();
                    if (user == null) {
                        result.postValue(Resource.error(
                                R.string.error_generic, "NO_USER"));
                        return;
                    }
                    boolean verified = user.isEmailVerified();
                    if (verified) {
                        // Đồng bộ lên Firestore
                        mDataSource.syncEmailVerified(user.getUid())
                                .addOnCompleteListener(ignored -> {
                                    // Đồng bộ Room trên diskIO
                                    mExecutors.diskIO().execute(() -> {
                                        UserEntity entity = mUserDao.getUserByIdSync(user.getUid());
                                        if (entity != null) {
                                            entity.emailVerified = true;
                                            mUserDao.insertOrUpdate(entity);
                                        }
                                    });
                                    result.postValue(Resource.success(true));
                                });
                    } else {
                        result.postValue(Resource.success(false));
                    }
                })
                .addOnFailureListener(e ->
                        result.postValue(Resource.error(R.string.error_network, "NETWORK_ERROR")));

        return result;
    }

    /**
     * Gửi lại email xác minh.
     */
    @NonNull
    public LiveData<Resource<Void>> resendVerificationEmail() {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        FirebaseUser user = mDataSource.getCurrentUser();
        if (user == null) {
            result.setValue(Resource.error(R.string.error_generic, "NO_USER"));
            return result;
        }

        mDataSource.sendEmailVerification(user)
                .addOnSuccessListener(aVoid -> result.postValue(Resource.success(null)))
                .addOnFailureListener(e ->
                        result.postValue(Resource.error(R.string.error_network, "NETWORK_ERROR")));
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. ĐĂNG NHẬP (FR-01.02 + NFR-06)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Đăng nhập bằng email hoặc @username + password.
     *
     * NFR-06: Mọi lỗi xác thực đều trả thông báo chung
     * "Email/tên đăng nhập hoặc mật khẩu không đúng" — không lộ username/email tồn tại.
     *
     * @param identifier Email hoặc @username (cả hai dạng được xử lý)
     * @param password   Mật khẩu
     * @param deviceId   ID thiết bị từ SecurePreferencesManager
     * @param deviceName Tên thiết bị
     * @param osVersion  Phiên bản OS
     */
    @NonNull
    public LiveData<Resource<FirebaseUser>> login(
            @NonNull String identifier,
            @NonNull String password,
            @NonNull String deviceId,
            @NonNull String deviceName,
            @NonNull String osVersion) {

        MutableLiveData<Resource<FirebaseUser>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        // Xác định identifier là email hay username
        if (ValidationUtils.isEmailIdentifier(identifier)) {
            // Đăng nhập trực tiếp bằng email
            performSignIn(identifier.trim(), password, deviceId, deviceName, osVersion, result);
        } else {
            // Đăng nhập bằng @username → tra email từ /usernames/{username}
            String username = ValidationUtils.normalizeUsername(identifier);
            mDataSource.getEmailByUsername(username)
                    .addOnSuccessListener(email -> {
                        if (email == null) {
                            // NFR-06: Không lộ "username không tồn tại"
                            result.postValue(Resource.error(
                                    R.string.error_invalid_credentials, "INVALID_CREDENTIALS"));
                            return;
                        }
                        performSignIn(email, password, deviceId, deviceName, osVersion, result);
                    })
                    .addOnFailureListener(e ->
                            result.postValue(Resource.error(
                                    R.string.error_invalid_credentials, "INVALID_CREDENTIALS")));
        }

        return result;
    }

    private void performSignIn(
            @NonNull String email,
            @NonNull String password,
            @NonNull String deviceId,
            @NonNull String deviceName,
            @NonNull String osVersion,
            @NonNull MutableLiveData<Resource<FirebaseUser>> result) {

        mDataSource.signInWithEmail(email, password)
                .addOnSuccessListener(firebaseUser -> {
                    if (firebaseUser == null) {
                        result.postValue(Resource.error(
                                R.string.error_invalid_credentials, "INVALID_CREDENTIALS"));
                        return;
                    }
                    // Ghi session mới
                    String sessionId = UUID.randomUUID().toString();
                    mDataSource.writeSession(
                            firebaseUser.getUid(),
                            sessionId,
                            deviceId,
                            deviceName,
                            osVersion,
                            "" /* fcmToken — FCM sẽ cập nhật sau */)
                            .addOnCompleteListener(sessionTask -> {
                                // Lưu sessionId dù task thành công hay lỗi (tránh lock-out)
                                mPrefs.setCurrentSessionId(sessionId);
                                // Đồng bộ profile từ Firestore vào Room
                                syncUserToRoom(firebaseUser.getUid());
                                result.postValue(Resource.success(firebaseUser));
                            });
                })
                .addOnFailureListener(e -> {
                    String code = FirebaseAuthDataSource.classifyAuthError(e);
                    // NFR-06: Luôn trả lỗi chung cho mọi loại lỗi xác thực
                    result.postValue(Resource.error(
                            R.string.error_invalid_credentials, code));
                });
    }

    /**
     * Đồng bộ profile từ Firestore vào Room (chạy trên networkIO → diskIO).
     */
    private void syncUserToRoom(@NonNull String uid) {
        mDataSource.getUserDocument(uid).addOnSuccessListener(doc -> {
            if (doc == null || !doc.exists()) return;
            mExecutors.diskIO().execute(() -> {
                UserEntity entity = documentToEntity(doc);
                if (entity != null) {
                    mUserDao.insertOrUpdate(entity);
                }
            });
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. QUÊN MẬT KHẨU (FR-01.06)
    // ══════════════════════════════════════════════════════════════════════

    @NonNull
    public LiveData<Resource<Void>> sendPasswordResetEmail(@NonNull String email) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        mDataSource.sendPasswordResetEmail(email)
                .addOnSuccessListener(aVoid -> result.postValue(Resource.success(null)))
                .addOnFailureListener(e -> {
                    // NFR-06: Không phân biệt email tồn tại hay không
                    result.postValue(Resource.success(null));
                });
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. ĐỔI MẬT KHẨU + THU HỒI SESSION (FR-01.06)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Đổi mật khẩu: reauthenticate → updatePassword → revoke other sessions.
     */
    @NonNull
    public LiveData<Resource<Void>> changePassword(
            @NonNull String email,
            @NonNull String oldPassword,
            @NonNull String newPassword) {

        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());

        mDataSource.reauthenticate(email, oldPassword)
                .addOnSuccessListener(aVoid ->
                        mDataSource.updatePassword(newPassword)
                                .addOnSuccessListener(v -> {
                                    FirebaseUser user = mDataSource.getCurrentUser();
                                    if (user != null) {
                                        String currentSessionId = mPrefs.getCurrentSessionId();
                                        mDataSource.revokeOtherSessions(
                                                user.getUid(), currentSessionId)
                                                .addOnCompleteListener(ignored ->
                                                        result.postValue(Resource.success(null)));
                                    } else {
                                        result.postValue(Resource.success(null));
                                    }
                                })
                                .addOnFailureListener(e ->
                                        result.postValue(Resource.error(
                                                R.string.error_change_password_failed,
                                                "UPDATE_PASSWORD_FAILED"))))
                .addOnFailureListener(e ->
                        result.postValue(Resource.error(
                                R.string.error_invalid_credentials, "REAUTH_FAILED")));

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. ĐĂNG XUẤT
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Đăng xuất: xóa session trên Firestore, signOut Auth, xóa sessionId local.
     *
     * @param clearLocalData true → xóa toàn bộ bảng Room (tính năng "Xóa dữ liệu khi đăng xuất")
     */
    public void logout(boolean clearLocalData) {
        FirebaseUser user = mDataSource.getCurrentUser();
        String sessionId  = mPrefs.getCurrentSessionId();

        if (user != null && sessionId != null && !sessionId.isEmpty()) {
            mDataSource.deleteSession(user.getUid(), sessionId);
        }

        mDataSource.signOut();
        mPrefs.clearSession();

        if (clearLocalData) {
            mExecutors.diskIO().execute(() -> {
                AppDatabase db = AppDatabase.getInstance(mContext);
                db.clearAllTables();
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Helper: DocumentSnapshot → UserEntity
    // ══════════════════════════════════════════════════════════════════════

    private UserEntity documentToEntity(@NonNull DocumentSnapshot doc) {
        try {
            UserEntity e = new UserEntity();
            e.userId        = doc.getId();
            e.username      = doc.getString("username");
            e.displayName   = doc.getString("displayName");
            e.email         = doc.getString("email");
            e.phoneNumber   = doc.getString("phoneNumber");
            e.bio           = doc.getString("bio");
            e.birthday      = doc.getString("birthday");
            e.avatarUrl     = doc.getString("avatarUrl");
            e.emailVerified = Boolean.TRUE.equals(doc.getBoolean("emailVerified"));
            e.updatedAt     = doc.getTimestamp("updatedAt") != null
                    ? doc.getTimestamp("updatedAt").toDate().getTime()
                    : 0L;
            return e;
        } catch (Exception ex) {
            return null;
        }
    }
}
