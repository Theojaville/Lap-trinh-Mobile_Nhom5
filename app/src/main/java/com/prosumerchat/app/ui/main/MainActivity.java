package com.prosumerchat.app.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.prosumerchat.app.databinding.ActivityMainBinding;

/**
 * MainActivity — màn hình khung chính dùng SlidingPaneLayout (FR-06.01).
 *
 * Dev C (Tuần 1): Implement 2-pane layout:
 *   - Pane trái  : danh sách chat (ChatListFragment)
 *   - Pane phải  : chi tiết chat  (ChatDetailFragment)
 * Hiện tại chỉ hiển thị layout rỗng để project compile và launch được.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO Dev C (Tuần 1): Wire SlidingPaneLayout, attach ChatListFragment vào pane trái.
        // TODO Dev A (Tuần 1): Kiểm tra trạng thái auth — nếu chưa login, redirect sang LoginActivity.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
