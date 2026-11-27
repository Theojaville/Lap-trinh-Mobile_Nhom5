package com.prosumerchat.app.data.local.room;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.prosumerchat.app.data.local.room.dao.UserDao;
import com.prosumerchat.app.data.local.room.entity.UserEntity;
import com.prosumerchat.app.security.TinkKeysetManager;
import com.prosumerchat.app.util.AppExecutors;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

/**
 * AppDatabase — Room database dùng SQLCipher để mã hóa toàn bộ DB bằng raw 256-bit key.
 *
 * !!! File này dễ conflict — ai thêm Entity/DAO phải báo nhóm trước !!!
 *
 * Phân công Entity theo từng Dev:
 *   Dev A : UserEntity (v1)
 *   Dev B : FriendEntity (Tuần 2)
 *   Dev C : ChatEntity, MessageEntity, OutboxEntity (Tuần 2)
 *   Dev D : MediaCacheEntity (Tuần 3)
 *
 * Mỗi khi thêm Entity mới: tăng version + viết Migration + báo nhóm.
 */
@Database(
        entities = {
                UserEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "prosumerchat.db";

    private static volatile AppDatabase INSTANCE;
    private static volatile boolean sSqlCipherLoaded = false;
    private static final Object NATIVE_LOAD_LOCK = new Object();

    public abstract UserDao userDao();

    /**
     * Load native library {@code libsqlcipher.so} an toàn một lần duy nhất.
     */
    public static void ensureSqlCipherLoaded() {
        if (sSqlCipherLoaded) {
            return;
        }
        synchronized (NATIVE_LOAD_LOCK) {
            if (!sSqlCipherLoaded) {
                System.loadLibrary("sqlcipher");
                sSqlCipherLoaded = true;
            }
        }
    }

    /**
     * Lấy singleton {@link AppDatabase}, tự động lấy raw 256-bit key từ {@link TinkKeysetManager}.
     * Lần khởi tạo đầu tiên bắt buộc gọi trên background thread (không chạy trên Main Thread).
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        assertBackgroundThreadForInit();
        byte[] passphrase = TinkKeysetManager.getDatabasePassphrase(context);
        return getInstance(context, passphrase);
    }

    /**
     * Lấy singleton {@link AppDatabase} với passphrase cung cấp sẵn (raw 256-bit key).
     * Lần khởi tạo đầu tiên bắt buộc gọi trên background thread.
     */
    @NonNull
    public static AppDatabase getInstance(@NonNull Context context, @NonNull byte[] passphrase) {
        if (INSTANCE == null) {
            assertBackgroundThreadForInit();
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    ensureSqlCipherLoaded();
                    SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .openHelperFactory(factory)
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static void assertBackgroundThreadForInit() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException(
                    "AppDatabase must be initialized on a background thread (use AppExecutors.diskIO)");
        }
    }

    /**
     * Xóa toàn bộ dữ liệu trong tất cả bảng (phục vụ tùy chọn "Xóa dữ liệu cục bộ khi đăng xuất").
     * Phải được gọi trên background thread.
     */
    public void clearAllData() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("clearAllData() must be called on a background thread");
        }
        clearAllTables();
    }

    /**
     * Xóa toàn bộ dữ liệu cục bộ bất đồng bộ trên {@link AppExecutors#diskIO()}.
     */
    public void clearAllDataAsync(@Nullable Runnable onComplete) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            clearAllTables();
            if (onComplete != null) {
                AppExecutors.getInstance().mainThread().execute(onComplete);
            }
        });
    }
}
