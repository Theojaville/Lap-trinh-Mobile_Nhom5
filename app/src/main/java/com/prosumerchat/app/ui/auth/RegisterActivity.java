package com.prosumerchat.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.prosumerchat.app.R;
import com.prosumerchat.app.databinding.ActivityRegisterBinding;
import com.prosumerchat.app.util.ValidationUtils;
import com.prosumerchat.app.viewmodel.AuthViewModel;

/**
 * RegisterActivity — Màn hình đăng ký tài khoản.
 *
 * - Validation realtime: lỗi hiển thị ngay dưới TextInputLayout khi người dùng rời khỏi field.
 * - Chống double-click: disable button trong khi loading.
 * - Không log password.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding mBinding;
    private AuthViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupRealTimeValidation();
        setupListeners();
        observeViewModel();
    }

    private void setupRealTimeValidation() {
        // Display name
        mBinding.etDisplayName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateDisplayName();
        });
        // Username
        mBinding.etUsername.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                mBinding.tilUsername.setError(null);
            }
        });
        mBinding.etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateUsername();
        });
        // Email
        mBinding.etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateEmail();
        });
        // Phone (optional)
        mBinding.etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePhone();
        });
        // Password
        mBinding.etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validatePassword();
        });
        // Confirm password
        mBinding.etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) validateConfirmPassword();
        });
        // IME Done → submit
        mBinding.etConfirmPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });
    }

    private void setupListeners() {
        mBinding.btnRegister.setOnClickListener(v -> attemptRegister());
        mBinding.btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        // Validate all fields
        boolean valid = validateDisplayName()
                & validateUsername()
                & validateEmail()
                & validatePhone()
                & validatePassword()
                & validateConfirmPassword();

        if (!valid) return;

        String displayName = text(mBinding.etDisplayName);
        String username    = text(mBinding.etUsername);
        String email       = text(mBinding.etEmail);
        String phone       = text(mBinding.etPhone);
        String password    = mBinding.etPassword.getText() != null
                ? mBinding.etPassword.getText().toString() : "";

        mBinding.tvRegisterError.setVisibility(View.GONE);
        mViewModel.register(displayName, username, email, password, phone);
    }

    private void observeViewModel() {
        mViewModel.getRegisterResult().observe(this, resource -> {
            if (resource == null) return;

            setLoadingState(resource.isLoading());

            if (resource.isSuccess()) {
                // Chuyển sang màn xác minh email
                Intent intent = new Intent(this, VerifyEmailActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else if (resource.isError()) {
                String msg = resource.messageResId != 0
                        ? getString(resource.messageResId)
                        : (resource.message != null ? resource.message : getString(R.string.error_generic));

                // USERNAME_TAKEN → hiển thị lỗi trực tiếp dưới field username
                if ("USERNAME_TAKEN".equals(resource.errorCode)) {
                    mBinding.tilUsername.setError(getString(R.string.error_username_taken));
                } else {
                    mBinding.tvRegisterError.setText(msg);
                    mBinding.tvRegisterError.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // ── Validators ──────────────────────────────────────────────────────────

    private boolean validateDisplayName() {
        String v = text(mBinding.etDisplayName);
        if (!ValidationUtils.isValidDisplayName(v)) {
            mBinding.tilDisplayName.setError(getString(R.string.error_invalid_display_name));
            return false;
        }
        mBinding.tilDisplayName.setError(null);
        return true;
    }

    private boolean validateUsername() {
        String normalized = ValidationUtils.normalizeUsername(text(mBinding.etUsername));
        if (!ValidationUtils.isValidUsername(normalized)) {
            mBinding.tilUsername.setError(getString(R.string.error_invalid_username));
            return false;
        }
        mBinding.tilUsername.setError(null);
        return true;
    }

    private boolean validateEmail() {
        if (!ValidationUtils.isValidEmail(text(mBinding.etEmail))) {
            mBinding.tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        mBinding.tilEmail.setError(null);
        return true;
    }

    private boolean validatePhone() {
        String phone = text(mBinding.etPhone);
        if (TextUtils.isEmpty(phone)) { // optional
            mBinding.tilPhone.setError(null);
            return true;
        }
        try {
            ValidationUtils.normalizePhoneNumber(phone);
            mBinding.tilPhone.setError(null);
            return true;
        } catch (IllegalArgumentException ex) {
            mBinding.tilPhone.setError(getString(R.string.error_invalid_phone));
            return false;
        }
    }

    private boolean validatePassword() {
        String pw = mBinding.etPassword.getText() != null
                ? mBinding.etPassword.getText().toString() : "";
        if (!ValidationUtils.isValidPassword(pw)) {
            mBinding.tilPassword.setError(getString(R.string.error_invalid_password));
            return false;
        }
        mBinding.tilPassword.setError(null);
        return true;
    }

    private boolean validateConfirmPassword() {
        String pw      = mBinding.etPassword.getText() != null
                ? mBinding.etPassword.getText().toString() : "";
        String confirm = mBinding.etConfirmPassword.getText() != null
                ? mBinding.etConfirmPassword.getText().toString() : "";
        if (!pw.equals(confirm)) {
            mBinding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            return false;
        }
        mBinding.tilConfirmPassword.setError(null);
        return true;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private String text(com.google.android.material.textfield.TextInputLayout til) {
        return til.getEditText() != null && til.getEditText().getText() != null
                ? til.getEditText().getText().toString().trim()
                : "";
    }

    private String text(com.google.android.material.textfield.TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void setLoadingState(boolean loading) {
        mBinding.progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        mBinding.btnRegister.setEnabled(!loading);
        mBinding.btnGoLogin.setEnabled(!loading);
        mBinding.etDisplayName.setEnabled(!loading);
        mBinding.etUsername.setEnabled(!loading);
        mBinding.etEmail.setEnabled(!loading);
        mBinding.etPhone.setEnabled(!loading);
        mBinding.etPassword.setEnabled(!loading);
        mBinding.etConfirmPassword.setEnabled(!loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding = null;
    }

    // ── SimpleTextWatcher ────────────────────────────────────────────────────

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }
}
