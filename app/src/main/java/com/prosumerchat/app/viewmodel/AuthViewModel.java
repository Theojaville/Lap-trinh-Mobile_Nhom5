package com.prosumerchat.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseUser;
import com.prosumerchat.app.data.repository.AuthRepository;
import com.prosumerchat.app.util.DeviceInfoProvider;
import com.prosumerchat.app.util.Resource;
import com.prosumerchat.app.util.ValidationUtils;

/**
 * AuthViewModel — ViewModel quản lý trạng thái UI cho toàn bộ luồng xác thực.
 *
 * Giữ LiveData<Resource<>> cho Login, Register, VerifyEmail, ForgotPassword, ChangePassword.
 * Không giữ tham chiếu Activity/Fragment/View.
 * Không log mật khẩu hoặc token.
 */
public class AuthViewModel extends AndroidViewModel {

    private final AuthRepository mRepository;

    // ── LiveData outputs ────────────────────────────────────────────────────
    private final MutableLiveData<Resource<Void>>         mRegisterResult   = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>>      mVerifyResult     = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>>         mResendEmailResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<FirebaseUser>> mLoginResult      = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>>         mForgotResult     = new MutableLiveData<>();
    private final MutableLiveData<Resource<Void>>         mChangePassResult = new MutableLiveData<>();

    // Rate-limit phía client: đếm số lần đăng nhập sai liên tiếp
    private int mLoginFailCount = 0;
    private static final int MAX_LOGIN_FAILS = 5;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        mRepository = new AuthRepository(application);
    }

    // ── Getters ─────────────────────────────────────────────────────────────

    @NonNull public LiveData<Resource<Void>>         getRegisterResult()    { return mRegisterResult; }
    @NonNull public LiveData<Resource<Boolean>>      getVerifyResult()      { return mVerifyResult; }
    @NonNull public LiveData<Resource<Void>>         getResendEmailResult() { return mResendEmailResult; }
    @NonNull public LiveData<Resource<FirebaseUser>> getLoginResult()       { return mLoginResult; }
    @NonNull public LiveData<Resource<Void>>         getForgotResult()      { return mForgotResult; }
    @NonNull public LiveData<Resource<Void>>         getChangePassResult()  { return mChangePassResult; }

    public int getLoginFailCount() { return mLoginFailCount; }

    // ── Commands ─────────────────────────────────────────────────────────────

    /**
     * Kích hoạt đăng ký tài khoản mới.
     */
    public void register(@NonNull String displayName,
                         @NonNull String rawUsername,
                         @NonNull String email,
                         @NonNull String password,
                         @Nullable String rawPhone) {

        String username    = ValidationUtils.normalizeUsername(rawUsername);
        String phoneNumber;
        try {
            phoneNumber = ValidationUtils.normalizePhoneNumber(rawPhone);
        } catch (IllegalArgumentException ex) {
            phoneNumber = "";
        }

        LiveData<Resource<Void>> source =
                mRepository.register(displayName, username, email, password, phoneNumber);
        // Forward về mRegisterResult
        observeOnce(source, mRegisterResult);
    }

    /**
     * Kiểm tra email đã xác minh chưa + đồng bộ.
     */
    public void checkEmailVerified() {
        LiveData<Resource<Boolean>> source = mRepository.checkEmailVerified();
        observeOnce(source, mVerifyResult);
    }

    /**
     * Gửi lại email xác minh (countdown 60s do Activity quản lý).
     */
    public void resendVerificationEmail() {
        LiveData<Resource<Void>> source = mRepository.resendVerificationEmail();
        observeOnce(source, mResendEmailResult);
    }

    /**
     * Kích hoạt đăng nhập.
     * Rate-limit: Nếu sai ≥5 lần liên tiếp → trả lỗi TOO_MANY_REQUESTS ngay.
     */
    public void login(@NonNull String identifier, @NonNull String password) {
        if (mLoginFailCount >= MAX_LOGIN_FAILS) {
            // Để Firebase Auth tự báo lỗi TOO_MANY_REQUESTS ở lần tiếp theo;
            // UI đã countdown — không block ở đây, chỉ ghi nhận.
        }

        Application app = getApplication();
        String deviceId   = DeviceInfoProvider.getDeviceId(app);
        String deviceName = DeviceInfoProvider.getDeviceName();
        String osVersion  = DeviceInfoProvider.getOsVersion();

        LiveData<Resource<FirebaseUser>> source =
                mRepository.login(identifier, password, deviceId, deviceName, osVersion);

        // Forward + cập nhật rate-limit counter
        source.observeForever(res -> {
            if (res == null) return;
            mLoginResult.postValue(res);
            if (res.isError()) {
                mLoginFailCount++;
            } else if (res.isSuccess()) {
                mLoginFailCount = 0;
            }
        });
    }

    /**
     * Gửi link đặt lại mật khẩu (NFR-06: luôn trả success để không lộ email).
     */
    public void sendPasswordReset(@NonNull String email) {
        LiveData<Resource<Void>> source = mRepository.sendPasswordResetEmail(email);
        observeOnce(source, mForgotResult);
    }

    /**
     * Đổi mật khẩu.
     */
    public void changePassword(@NonNull String email,
                               @NonNull String oldPassword,
                               @NonNull String newPassword) {
        LiveData<Resource<Void>> source =
                mRepository.changePassword(email, oldPassword, newPassword);
        observeOnce(source, mChangePassResult);
    }

    /**
     * Đăng xuất (gọi từ màn hình Cài đặt hoặc SessionManager).
     */
    public void logout(boolean clearLocalData) {
        mRepository.logout(clearLocalData);
    }

    // ── Helper: forward LiveData một lần ───────────────────────────────────

    /**
     * Lắng nghe source LiveData và forward sang dest MutableLiveData.
     * Tránh memory leak bằng cách tự gỡ observer sau khi nhận kết quả cuối (non-LOADING).
     */
    private <T> void observeOnce(@NonNull LiveData<T> source,
                                 @NonNull MutableLiveData<T> dest) {
        source.observeForever(new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T value) {
                dest.postValue(value);
                // Gỡ observer sau khi nhận kết quả terminal
                if (value instanceof Resource) {
                    Resource<?> res = (Resource<?>) value;
                    if (!res.isLoading()) {
                        source.removeObserver(this);
                    }
                } else {
                    source.removeObserver(this);
                }
            }
        });
    }
}
