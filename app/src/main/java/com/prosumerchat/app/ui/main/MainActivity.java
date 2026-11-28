package com.prosumerchat.app.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.prosumerchat.app.R;
import com.prosumerchat.app.databinding.ActivityMainBinding;
import com.prosumerchat.app.security.SecurePreferencesManager;
import com.prosumerchat.app.security.SessionManager;
import com.prosumerchat.app.ui.auth.LoginActivity;
import com.prosumerchat.app.ui.auth.VerifyEmailActivity;

/**
 * MainActivity — Màn hình chính (SlidingPaneLayout — Dev C sẽ implement khung chat).
 *
 * Dev A (Giai đoạn 3.4): Auth Gate + SessionManager.
 * - Kiểm tra FirebaseAuth.getCurrentUser() và sessionId hợp lệ trước khi hiển thị nội dung.
 * - Gắn SessionManager.SnapshotListener để tự đăng xuất ≤ 2s khi isRevoked=true.
 * - Hiển thị banner nhắc xác minh email nếu emailVerified = false.
 */
public class MainActivity extends AppCompatActivity implements SessionManager.SessionRevokedCallback {

    public static final String EXTRA_LIMITED_MODE = "extra_limited_mode";

    private ActivityMainBinding mBinding;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ── Auth Gate ──────────────────────────────────────────────────────
        if (!isAuthValid()) {
            redirectToLogin(false);
            return;
        }

        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        // ── SessionManager ─────────────────────────────────────────────────
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            mSessionManager = SessionManager.getInstance(this);
            mSessionManager.startListening(currentUser.getUid(), this);
        }

        // ── Banner: nhắc xác minh email ────────────────────────────────────
        setupEmailVerificationBanner();

        // ── Hiển thị thông báo nếu được chuyển đến từ session revoke ──────
        if (getIntent().getBooleanExtra("EXTRA_SESSION_REVOKED", false)) {
            Snackbar.make(mBinding.getRoot(),
                    R.string.error_session_revoked, Snackbar.LENGTH_LONG).show();
        }

        // TODO Dev C: Wire SlidingPaneLayout, attach ChatListFragment vào pane trái.
    }

    // ── Auth Gate ────────────────────────────────────────────────────────────

    /**
     * Kiểm tra tính hợp lệ của phiên đăng nhập.
     * Trả về false nếu:
     * - Chưa đăng nhập (currentUser == null)
     * - Không có sessionId hợp lệ trong SecurePreferencesManager
     */
    private boolean isAuthValid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;

        String sessionId = SecurePreferencesManager.getInstance(this).getCurrentSessionId();
        return sessionId != null && !sessionId.isEmpty();
    }

    private void setupEmailVerificationBanner() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        boolean isLimitedMode = getIntent().getBooleanExtra(EXTRA_LIMITED_MODE, false);

        // Hiện banner cảnh báo nếu email chưa xác minh
        if (!user.isEmailVerified() && isLimitedMode) {
            // TODO Dev A (Giai đoạn 4): Thêm banner UI trong activity_main.xml
            // Tạm thời hiển thị Snackbar
            if (mBinding != null) {
                Snackbar.make(mBinding.getRoot(),
                        R.string.warning_email_not_verified,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.action_verify_now, v -> {
                            startActivity(new Intent(this, VerifyEmailActivity.class));
                        })
                        .show();
            }
        }
    }

    // ── SessionRevokedCallback ────────────────────────────────────────────────

    /**
     * Callback từ SessionManager khi phiên bị thu hồi từ thiết bị khác.
     * Được gọi trên Main Thread, phải xảy ra trong ≤ 2 giây.
     */
    @Override
    public void onSessionRevoked() {
        // Điều hướng về LoginActivity với flag clear stack
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("EXTRA_SESSION_REVOKED", true);
        startActivity(intent);
        finish();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Kiểm tra lại auth khi resume (ví dụ: user đăng xuất từ tab khác trong cùng thiết bị)
        if (!isAuthValid()) {
            redirectToLogin(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mSessionManager != null) {
            mSessionManager.stopListening();
        }
        super.onDestroy();
        mBinding = null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void redirectToLogin(boolean sessionRevoked) {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        if (sessionRevoked) {
            intent.putExtra("EXTRA_SESSION_REVOKED", true);
        }
        startActivity(intent);
        finish();
    }
}
