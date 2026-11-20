package com.prosumerchat.app.ui.auth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.prosumerchat.app.databinding.ActivityRegisterBinding;

/**
 * RegisterActivity — màn hình đăng ký tài khoản mới.
 *
 * Dev A (Tuần 1-2): Implement Firebase Auth (tạo user mới), lưu profile lên Firestore.
 * Hiện tại là stub rỗng — chỉ inflate layout để project build được.
 */
public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO Dev A: Implement form đăng ký (email, password, display name).
        // TODO Dev A: Validate input, tạo user FirebaseAuth, lưu profile Firestore.
        // TODO Dev A: Điều hướng về LoginActivity sau khi đăng ký thành công.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
