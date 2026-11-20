# proguard-rules.pro — ProsumerChat
# Thêm rule ở đây khi cần giữ lại class cho reflection/serialization.

# ── Firebase ─────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# ── Room ─────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# ── SQLCipher ────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── Cloudinary ───────────────────────────────────────────────
-keep class com.cloudinary.** { *; }

# ── Glide ────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModule
-keepclassmembers class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# ── Tink ─────────────────────────────────────────────────────
-keep class com.google.crypto.tink.** { *; }
