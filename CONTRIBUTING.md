# CONTRIBUTING — ProsumerChat

Quy ước làm việc nhóm để tránh conflict code.

## Phân công package

| Dev | Package chính | Tuần |
|-----|--------------|------|
| **Dev A** | `ui/auth`, `ui/profile`, `ui/settings`, `security/` | 1–2 |
| **Dev B** | `ui/friends`, `ui/search` | 2–3 |
| **Dev C** | `ui/main` (MainActivity), `ui/chatlist`, `ui/chatdetail` | 1–3 |
| **Dev D** | `media/`, `data/remote/cloudinary/` | 3–4 |

> Mỗi người **chỉ sửa package của mình**. Sửa file dùng chung phải báo nhóm trước.

---

## File dễ conflict — báo nhóm trước khi sửa

| File | Lý do |
|------|-------|
| `AndroidManifest.xml` | Mỗi Activity/Service/Permission mới cần thêm vào đây |
| `AppDatabase.java` | Thêm Entity, DAO, nâng version → phải đồng bộ toàn team |
| `gradle/libs.versions.toml` | Thêm dependency → có thể conflict version |
| `app/build.gradle.kts` | Thêm dependency hoặc build config |
| `firestore.rules` | Mọi rule thay đổi ảnh hưởng toàn bộ bảo mật |
| `MessageEntity.java` (khi tạo) | Dev B, C, D cùng đọc/ghi message |

---

## Quy tắc Git

```bash
# Mỗi ngày trước khi code:
git pull origin main

# Commit nhỏ, rõ ràng:
git commit -m "feat(auth): add email validation in RegisterActivity"

# Mỗi feature làm trên branch riêng:
git checkout -b feat/dev-a/login-screen

# Merge vào main chỉ khi build pass:
# → Tạo Pull Request, 1 người review, squash merge.
```

### Quy ước prefix commit message

| Prefix | Dùng khi |
|--------|----------|
| `feat(scope):` | Thêm tính năng mới |
| `fix(scope):` | Sửa bug |
| `refactor(scope):` | Refactor không đổi chức năng |
| `chore(scope):` | Cập nhật build, dependency |
| `docs(scope):` | Chỉ sửa comment/doc |

### Scope gợi ý: `auth`, `chat`, `friends`, `media`, `db`, `security`, `build`

---

## Thêm Entity vào Room (AppDatabase)

1. Tạo file `XxxEntity.java` trong package riêng của bạn.  
2. Mở `AppDatabase.java`, thêm class vào annotation `@Database(entities = {...})`.  
3. **Tăng `version`** (vd: 1 → 2) và viết `Migration`.  
4. **Báo nhóm** trước khi merge — dev khác cũng có thể đang sửa file này.

---

## Lưu ý bảo mật

- **KHÔNG** commit `google-services.json` — file đã bị chặn trong `.gitignore`.
- **KHÔNG** hardcode API key, Cloudinary secret, hay password trong code.
- Mọi secret đọc từ `BuildConfig` (khai báo trong `build.gradle.kts` với giá trị từ `local.properties`).
