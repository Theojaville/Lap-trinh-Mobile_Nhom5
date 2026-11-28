package com.prosumerchat.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.prosumerchat.app.R;
import com.prosumerchat.app.databinding.ActivityLoginBinding;
import com.prosumerchat.app.ui.main.MainActivity;
import com.prosumerchat.app.util.Resource;
import com.prosumerchat.app.util.ValidationUtils;
import com.prosumerchat.app.viewmodel.AuthViewModel;

/**
 * LoginActivity — Màn hình đăng nhập.
 *
 * - Hỗ trợ Email hoặc @username làm identifier (NFR-06).
 * - Lỗi xác thực chỉ hiển thị thông báo chung, không tiết lộ tài khoản tồn tại.
 * - Chống double-click: disable button trong khi loading.
 * - Xoay màn hình giữ trạng thái nhờ ViewModel.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding mBinding;
    private AuthViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        // Nhấn Done trên bàn phím → đăng nhập
        mBinding.etPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });

        mBinding.btnLogin.setOnClickListener(v -> attemptLogin());

        mBinding.btnForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        mBinding.btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String identifier = mBinding.etIdentifier.getText() != null
                ? mBinding.etIdentifier.getText().toString().trim() : "";
        String password   = mBinding.etPassword.getText() != null
                ? mBinding.etPassword.getText().toString() : "";

        // Validation cơ bản phía client
        if (TextUtils.isEmpty(identifier)) {
            mBinding.tilIdentifier.setError(getString(R.string.error_field_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            mBinding.tilPassword.setError(getString(R.string.error_field_required));
            return;
        }

        mBinding.tilIdentifier.setError(null);
        mBinding.tilPassword.setError(null);
        mBinding.tvLoginError.setVisibility(View.GONE);

        mViewModel.login(identifier, password);
    }

    private void observeViewModel() {
        mViewModel.getLoginResult().observe(this, resource -> {
            if (resource == null) return;

            setLoadingState(resource.isLoading());

            if (resource.isSuccess() && resource.data != null) {
                boolean emailVerified = resource.data.isEmailVerified();
                if (emailVerified) {
                    navigateToMain();
                } else {
                    navigateToVerifyEmail();
                }
            } else if (resource.isError()) {
                showError(resource);
            }
        });
    }

    private void setLoadingState(boolean loading) {
        mBinding.progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        mBinding.btnLogin.setEnabled(!loading);
        mBinding.btnForgotPassword.setEnabled(!loading);
        mBinding.btnGoRegister.setEnabled(!loading);
        mBinding.etIdentifier.setEnabled(!loading);
        mBinding.etPassword.setEnabled(!loading);
    }

    private void showError(Resource<?> resource) {
        String message;
        if (resource.messageResId != 0) {
            message = getString(resource.messageResId);
        } else if (!TextUtils.isEmpty(resource.message)) {
            message = resource.message;
        } else {
            message = getString(R.string.error_generic);
        }

        // "TOO_MANY_REQUESTS" → show Snackbar thêm thông báo
        if ("TOO_MANY_REQUESTS".equals(resource.errorCode)) {
            Snackbar.make(mBinding.getRoot(),
                    R.string.error_too_many_requests, Snackbar.LENGTH_LONG).show();
            return;
        }

        mBinding.tvLoginError.setText(message);
        mBinding.tvLoginError.setVisibility(View.VISIBLE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToVerifyEmail() {
        Intent intent = new Intent(this, VerifyEmailActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }
}
