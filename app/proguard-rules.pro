# proguard-rules.pro — ProsumerChat
# Cấu hình R8 / ProGuard cho bản release.
# !!! File này dùng chung — sửa thêm rule phải kiểm tra cẩn thận !!!

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Room Database ────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ── AndroidX SQLite Wrapper ──────────────────────────────────
-keep class androidx.sqlite.db.** { *; }
-keep class androidx.sqlite.db.framework.** { *; }

# ── SQLCipher (net.zetetic) ──────────────────────────────────
-keep class net.zetetic.** { *; }
-keepclassmembers class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-keepclassmembers class * extends net.sqlcipher.database.SQLiteOpenHelper { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# ── Google Tink (Crypto / Keystore) ──────────────────────────
-keep class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# ── Cloudinary ───────────────────────────────────────────────
-keep class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# ── Glide ────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.resource.bitmap.DownsampleStrategy$* {
    **[] $VALUES;
    public *;
}
-dontwarn com.bumptech.glide.**

# ── Android Image Cropper ────────────────────────────────────
-keep class com.canhub.cropper.** { *; }
-keep class com.vanniktech.ui.** { *; }
-dontwarn com.canhub.cropper.**

# ── WorkManager & Navigation ─────────────────────────────────
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**
