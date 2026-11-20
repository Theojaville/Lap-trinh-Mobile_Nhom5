package com.prosumerchat.app.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.prosumerchat.app.databinding.ActivityLoginBinding;

/**
 * LoginActivity — màn hình đăng nhập.
 *
 * Dev A (Tuần 1-2): Implement Firebase Auth (email/password, Google Sign-In).
 * Hiện tại là stub rỗng — chỉ inflate layout để project build được.
 */
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO Dev A: Implement email/password login với FirebaseAuth.
        // TODO Dev A: Implement Google Sign-In.
        // TODO Dev A: Điều hướng sang RegisterActivity khi tap "Chưa có tài khoản".
        // TODO Dev A: Điều hướng sang MainActivity khi login thành công.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
