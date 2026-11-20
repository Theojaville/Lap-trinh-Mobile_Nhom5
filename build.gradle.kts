// Top-level build file — cấu hình chung cho toàn project.
// Thêm plugin ở đây, KHÔNG thêm dependency vào đây.
plugins {
    alias(libs.plugins.android.application)  apply false
    alias(libs.plugins.google.services)      apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}
