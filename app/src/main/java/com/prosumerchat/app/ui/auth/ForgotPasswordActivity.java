package com.prosumerchat.app.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.prosumerchat.app.R;
import com.prosumerchat.app.databinding.ActivityForgotPasswordBinding;
import com.prosumerchat.app.util.ValidationUtils;
import com.prosumerchat.app.viewmodel.AuthViewModel;

/**
 * ForgotPasswordActivity — Màn hình quên mật khẩu.
 *
 * NFR-06: Luôn hiển thị "Đã gửi link đặt lại mật khẩu" dù email tồn tại hay không
 * (Firebase Auth cũng trả success cho cả hai trường hợp).
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding mBinding;
    private AuthViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
        observeViewModel();
    }

    private void setupListeners() {
        mBinding.btnSendReset.setOnClickListener(v -> attemptSendReset());

        mBinding.btnBack.setOnClickListener(v -> finish());

        mBinding.btnBackToLogin.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptSendReset() {
        String email = mBinding.etEmail.getText() != null
                ? mBinding.etEmail.getText().toString().trim() : "";

        mBinding.tilEmail.setError(null);
        mBinding.tvForgotMessage.setVisibility(View.GONE);

        if (!ValidationUtils.isValidEmail(email)) {
            mBinding.tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        mViewModel.sendPasswordReset(email);
    }

    private void observeViewModel() {
        mViewModel.getForgotResult().observe(this, resource -> {
            if (resource == null) return;

            setLoadingState(resource.isLoading());

            if (resource.isSuccess()) {
                // NFR-06: Luôn hiển thị thành công
                mBinding.tvForgotMessage.setText(R.string.desc_reset_email_sent);
                mBinding.tvForgotMessage.setTextColor(
                        getColor(com.google.android.material.R.color.material_dynamic_primary40));
                mBinding.tvForgotMessage.setVisibility(View.VISIBLE);
                mBinding.btnSendReset.setEnabled(false); // Tránh gửi nhiều lần
            } else if (resource.isError()) {
                // Chỉ hiển thị lỗi mạng, không phân biệt email tồn tại hay không
                mBinding.tvForgotMessage.setText(R.string.error_network);
                mBinding.tvForgotMessage.setTextColor(
                        getColor(com.google.android.material.R.color.design_default_color_error));
                mBinding.tvForgotMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void setLoadingState(boolean loading) {
        mBinding.progressForgot.setVisibility(loading ? View.VISIBLE : View.GONE);
        mBinding.btnSendReset.setEnabled(!loading);
        mBinding.etEmail.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }
}
