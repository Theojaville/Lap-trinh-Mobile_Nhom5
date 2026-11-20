package com.prosumerchat.app.data.local.room;

import android.content.Context;

import androidx.room.RoomDatabase;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

/**
 * AppDatabase — Room database dùng SQLCipher để mã hóa toàn bộ DB.
 *
 * !!! File này dễ conflict — ai thêm Entity/DAO phải báo nhóm trước !!!
 *
 * ═══════════════════════════════════════════════════════════════
 * HƯỚNG DẪN HOÀN THIỆN (Dev A làm Tuần 1):
 * ═══════════════════════════════════════════════════════════════
 * Bước 1: Tạo Entity đầu tiên (ví dụ UserEntity.java trong package này).
 * Bước 2: Uncomment annotation @Database ở dưới và điền entities.
 * Bước 3: Khai báo abstract DAO method tương ứng.
 * Bước 4: Khởi tạo getInstance() từ ChatApplication.onCreate().
 *
 * Template annotation cần thêm vào class này:
 * -----------------------------------------------
 * @Database(
 *     entities = { UserEntity.class /*, MessageEntity.class, ... *‌/ },
 *     version  = 1,
 *     exportSchema = false
 * )
 * -----------------------------------------------
 *
 * Phân công Entity theo từng Dev:
 *   Dev A : UserEntity (Tuần 1-2)
 *   Dev B : FriendEntity (Tuần 2)
 *   Dev C : ConversationEntity, MessageEntity (Tuần 2) — dễ conflict nhất
 *   Dev D : MediaCacheEntity (Tuần 3)
 *
 * Mỗi khi thêm Entity mới: tăng version + viết Migration + báo nhóm.
 * ═══════════════════════════════════════════════════════════════
 */
// TODO Dev A (Tuần 1): Thêm @Database annotation ở đây (xem hướng dẫn ở trên).
public abstract class AppDatabase extends RoomDatabase {

    // TODO: Khai báo DAO abstract method của từng Dev ở đây.
    // Ví dụ:
    //   public abstract UserDao userDao();
    //   public abstract MessageDao messageDao();

    // ── Singleton ──────────────────────────────────────────────
    private static volatile AppDatabase INSTANCE;

    /**
     * Lấy instance AppDatabase (singleton, thread-safe).
     * Gọi sau khi đã thêm @Database annotation và ít nhất 1 Entity.
     *
     * @param context    Application context
     * @param passphrase Key mã hóa SQLCipher (lấy từ TinkKeysetManager, KHÔNG hardcode)
     */
    public static AppDatabase getInstance(Context context, byte[] passphrase) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // SQLCipher SupportFactory — mã hóa toàn bộ database file
                    SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);

                    INSTANCE = androidx.room.Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                "prosumerchat.db"
                            )
                            .openHelperFactory(factory)
                            // Thêm Migration khi nâng version:
                            // .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration(false)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
