package com.prosumerchat.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.prosumerchat.app.R;
import com.prosumerchat.app.databinding.ActivityVerifyEmailBinding;
import com.prosumerchat.app.ui.main.MainActivity;
import com.prosumerchat.app.viewmodel.AuthViewModel;

/**
 * VerifyEmailActivity — Màn hình xác minh email sau đăng ký.
 *
 * Chức năng:
 * - Hiển thị thông báo đã gửi email xác minh.
 * - Nút "Tôi đã xác minh" → reload FirebaseUser và kiểm tra isEmailVerified().
 * - Nút "Gửi lại email xác minh" với countdown 60s (tránh spam).
 * - Nút "Tiếp tục vào ứng dụng (chế độ hạn chế)" theo FR-01.01.
 * - Nút "Đăng xuất".
 */
public class VerifyEmailActivity extends AppCompatActivity {

    private static final long RESEND_COOLDOWN_MS = 60_000L; // 60 giây
    private static final long COUNTDOWN_INTERVAL_MS = 1_000L;

    private ActivityVerifyEmailBinding mBinding;
    private AuthViewModel mViewModel;
    private CountDownTimer mCountDownTimer;
    private boolean mCountdownRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();

        // Bắt đầu countdown ngay khi vào màn (email vừa được gửi khi đăng ký)
        startResendCountdown();
    }

    private void setupListeners() {
        mBinding.btnCheckVerified.setOnClickListener(v -> {
            showProgress(true);
            mViewModel.checkEmailVerified();
        });

        mBinding.btnResendEmail.setOnClickListener(v -> {
            if (!mCountdownRunning) {
                mViewModel.resendVerificationEmail();
                startResendCountdown();
            }
        });

        // Chế độ hạn chế: vào ứng dụng nhưng khóa tìm bạn/tạo nhóm
        mBinding.btnContinueLimited.setOnClickListener(v -> navigateToMainLimited());

        mBinding.btnLogout.setOnClickListener(v -> {
            mViewModel.logout(false);
            navigateToLogin();
        });
    }

    private void observeViewModel() {
        // Kết quả kiểm tra email đã xác minh
        mViewModel.getVerifyResult().observe(this, resource -> {
            if (resource == null) return;
            showProgress(resource.isLoading());

            if (resource.isSuccess() && resource.data != null) {
                if (Boolean.TRUE.equals(resource.data)) {
                    // Email đã xác minh → vào app chính
                    navigateToMain();
                } else {
                    // Chưa xác minh
                    mBinding.tvVerifyStatus.setText(R.string.status_email_not_yet_verified);
                    mBinding.tvVerifyStatus.setTextColor(
                            getColor(com.google.android.material.R.color.design_default_color_error));
                    mBinding.tvVerifyStatus.setVisibility(View.VISIBLE);
                }
            } else if (resource.isError()) {
                mBinding.tvVerifyStatus.setText(R.string.error_network);
                mBinding.tvVerifyStatus.setTextColor(
                        getColor(com.google.android.material.R.color.design_default_color_error));
                mBinding.tvVerifyStatus.setVisibility(View.VISIBLE);
            }
        });

        // Kết quả gửi lại email
        mViewModel.getResendEmailResult().observe(this, resource -> {
            if (resource == null) return;
            if (resource.isSuccess()) {
                mBinding.tvVerifyStatus.setText(R.string.status_verification_email_resent);
                mBinding.tvVerifyStatus.setVisibility(View.VISIBLE);
            }
        });
    }

    // ── Countdown ─────────────────────────────────────────────────────────────

    private void startResendCountdown() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mCountdownRunning = true;
        mBinding.btnResendEmail.setEnabled(false);
        mBinding.tvCountdown.setVisibility(View.VISIBLE);

        mCountDownTimer = new CountDownTimer(RESEND_COOLDOWN_MS, COUNTDOWN_INTERVAL_MS) {
            @Override
            public void onTick(long millisUntilFinished) {
                long secs = millisUntilFinished / 1000;
                mBinding.tvCountdown.setText(
                        getString(R.string.countdown_resend_format, secs));
            }

            @Override
            public void onFinish() {
                mCountdownRunning = false;
                mBinding.btnResendEmail.setEnabled(true);
                mBinding.tvCountdown.setVisibility(View.GONE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        super.onDestroy();
        mBinding = null;
    }

    // ── Navigation helpers ────────────────────────────────────────────────────

    private void showProgress(boolean show) {
        mBinding.progressVerify.setVisibility(show ? View.VISIBLE : View.GONE);
        mBinding.btnCheckVerified.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Chế độ hạn chế: vào MainActivity nhưng email chưa xác minh */
    private void navigateToMainLimited() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(MainActivity.EXTRA_LIMITED_MODE, true);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
