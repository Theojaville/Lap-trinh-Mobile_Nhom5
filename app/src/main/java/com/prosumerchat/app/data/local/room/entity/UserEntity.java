package com.prosumerchat.app.data.local.room.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * UserEntity â€” Báº£ng lÆ°u há»“ sÆ¡ ngÆ°á»i dÃ¹ng cá»¥c bá»™ trong Room DB (Single Source of Truth).
 * BÃ¡m sÃ¡t SRS má»¥c 4.3, schema Firestore /users/{uid} vÃ  yÃªu cáº§u Rubric (phoneNumber + privacy).
 */
@Entity(
        tableName = "users",
        indices = {
                @Index(value = {"username"})
        }
)
public class UserEntity {

    public static final String VISIBILITY_EVERYONE = "EVERYONE";
    public static final String VISIBILITY_FRIENDS = "FRIENDS";
    public static final String VISIBILITY_NOBODY = "NOBODY";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId = "";

    @NonNull
    @ColumnInfo(name = "username")
    public String username = "";

    @NonNull
    @ColumnInfo(name = "display_name")
    public String displayName = "";

    @Nullable
    @ColumnInfo(name = "nickname")
    private String nickname;

    @Nullable
    @ColumnInfo(name = "avatar_url")
    public String avatarUrl;

    @Nullable
    @ColumnInfo(name = "bio")
    public String bio;

    @NonNull
    @ColumnInfo(name = "email")
    public String email = "";

    @Nullable
    @ColumnInfo(name = "phone_number")
    public String phoneNumber;

    @Nullable
    @ColumnInfo(name = "birthday")
    public String birthday;

    @ColumnInfo(name = "is_online")
    private boolean isOnline;

    @ColumnInfo(name = "last_seen")
    private long lastSeen;

    @ColumnInfo(name = "email_verified")
    public boolean emailVerified;

    @NonNull
    @ColumnInfo(name = "avatar_visibility")
    private String avatarVisibility = VISIBILITY_EVERYONE;

    @NonNull
    @ColumnInfo(name = "last_seen_visibility")
    private String lastSeenVisibility = VISIBILITY_EVERYONE;

    @ColumnInfo(name = "read_receipts_enabled")
    private boolean readReceiptsEnabled = true;

    @ColumnInfo(name = "allow_find_by_phone")
    private boolean allowFindByPhone = true;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public UserEntity() {
        // Default constructor required by Room
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(@NonNull String displayName) {
        this.displayName = displayName;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    public void setNickname(@Nullable String nickname) {
        this.nickname = nickname;
    }

    @Nullable
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(@Nullable String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Nullable
    public String getBio() {
        return bio;
    }

    public void setBio(@Nullable String bio) {
        this.bio = bio;
    }

    @NonNull
    public String getEmail() {
        return email;
    }

    public void setEmail(@NonNull String email) {
        this.email = email;
    }

    @Nullable
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(@Nullable String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @Nullable
    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(@Nullable String birthday) {
        this.birthday = birthday;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    @NonNull
    public String getAvatarVisibility() {
        return avatarVisibility;
    }

    public void setAvatarVisibility(@NonNull String avatarVisibility) {
        this.avatarVisibility = avatarVisibility;
    }

    @NonNull
    public String getLastSeenVisibility() {
        return lastSeenVisibility;
    }

    public void setLastSeenVisibility(@NonNull String lastSeenVisibility) {
        this.lastSeenVisibility = lastSeenVisibility;
    }

    public boolean isReadReceiptsEnabled() {
        return readReceiptsEnabled;
    }

    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) {
        this.readReceiptsEnabled = readReceiptsEnabled;
    }

    public boolean isAllowFindByPhone() {
        return allowFindByPhone;
    }

    public void setAllowFindByPhone(boolean allowFindByPhone) {
        this.allowFindByPhone = allowFindByPhone;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

