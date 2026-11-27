package com.prosumerchat.app.data.local.room.dao;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.prosumerchat.app.data.local.room.entity.UserEntity;

import java.util.List;

/**
 * UserDao — Data Access Object cho bảng {@code users}.
 * Toàn bộ truy vấn tuân thủ Parameterized Queries (SRS mục 5.2).
 * Truy vấn LIKE bắt buộc nhận đầu vào đã qua {@link com.prosumerchat.app.util.SqlSanitizer#sanitizeLikeQuery(String)}.
 */
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(@NonNull UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(@NonNull List<UserEntity> users);

    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    LiveData<UserEntity> observeUserById(@NonNull String userId);

    @Nullable
    @Query("SELECT * FROM users WHERE user_id = :userId LIMIT 1")
    UserEntity getUserByIdSync(@NonNull String userId);

    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    UserEntity getUserByUsernameSync(@NonNull String username);

    /**
     * Tìm kiếm người dùng cục bộ theo username hoặc tên hiển thị.
     * Caller phải chuẩn hóa {@code sanitizedQuery} bằng {@code SqlSanitizer.sanitizeLikeQuery(raw)}.
     */
    @Query("SELECT * FROM users "
            + "WHERE username LIKE '%' || :sanitizedQuery || '%' ESCAPE '\\' "
            + "OR display_name LIKE '%' || :sanitizedQuery || '%' ESCAPE '\\' "
            + "ORDER BY display_name ASC")
    List<UserEntity> searchUsersLocal(@NonNull String sanitizedQuery);

    @Query("UPDATE users SET avatar_url = :avatarUrl, updated_at = :updatedAt WHERE user_id = :userId")
    int updateAvatarUrl(@NonNull String userId, @Nullable String avatarUrl, long updatedAt);

    @Query("UPDATE users SET email_verified = :emailVerified, updated_at = :updatedAt WHERE user_id = :userId")
    int updateEmailVerified(@NonNull String userId, boolean emailVerified, long updatedAt);

    @Query("UPDATE users SET "
            + "avatar_visibility = :avatarVisibility, "
            + "last_seen_visibility = :lastSeenVisibility, "
            + "read_receipts_enabled = :readReceiptsEnabled, "
            + "allow_find_by_phone = :allowFindByPhone, "
            + "updated_at = :updatedAt "
            + "WHERE user_id = :userId")
    int updatePrivacySettings(
            @NonNull String userId,
            @NonNull String avatarVisibility,
            @NonNull String lastSeenVisibility,
            boolean readReceiptsEnabled,
            boolean allowFindByPhone,
            long updatedAt);

    @Query("DELETE FROM users WHERE user_id = :userId")
    int deleteById(@NonNull String userId);

    @Query("DELETE FROM users")
    void clearAll();
}
