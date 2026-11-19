# Lap-trinh-Mobile_Nhom5
🎓 ỨNG DỤNG NHẮN TIN DI ĐỘNG PROSUMER (PROSUMER MOBILE CHAT APP)
Môn học: Lập trình di động (MOPR331279)

Nhóm: 5

Trường: Đại học Công nghệ Kỹ thuật TP.HCM (UTE)

👥 Thành viên nhóm & Phân công công việc

| STT | Họ và Tên | Vai trò | Nhánh Git (Branch) | Nhiệm vụ chính |
| 1 | Chế Thiên Nhi | Backend/Network | `feature/thien-nhi-backend-network` | Spring Boot, REST API, WebSocket, Auth/JWT, Chat & Group APIs, bảo mật tầng server |
| 2 | Đinh Phú Sỹ | Data/Offline | `feature/phu-sy-data-offline` | Room DB, SQLCipher, Outbox Pattern đồng bộ ngoại tuyến, Repository/ViewModel, LRU Cache |
| 3 | Phạm Nữ Nguyệt Minh | UI/Multi-screen | `feature/nguyet-minh-ui-multiscreen` | Giao diện XML (Material 3), Adaptive UI (Phone/Tablet/Foldable) với SlidingPaneLayout |
| 4 | Phạm Thị Cẩm Thúy | Chat/Media | `feature/cam-thuy-chat-media` | Hiển thị tin nhắn, Reply/Reaction/Edit/Recall/Forward, nén ảnh, Voice Notes, upload tệp |

🛠️ Công nghệ sử dụng

- **Mobile Client**: Java 11, XML (Material 3), MVVM + Clean Repository
- **Local Storage**: Room Database + SQLCipher (AES-256), Android Keystore
- **Networking**: Retrofit 2, OkHttp 4, WebSocket (STOMP)
- **Backend**: Spring Boot, Spring Data JPA, Spring Security, Spring WebSocket
- **Database**: PostgreSQL
- **Bảo mật**: JWT (Access/Refresh Token), BCrypt/Argon2, TLS 1.3
- **Công cụ**: Android Studio, Git & GitHub

🚀 Hướng dẫn Clone Dự án & Quy trình Làm việc (Git Workflow)

Link Repository chính thức: `https://github.com/Theojaville/Lap-trinh-Mobile_Nhom5.git`

### 1. Dành cho thành viên tải code lần đầu (Clone & Switch Branch)

Mở Terminal trên máy tính (CMD/PowerShell hoặc Terminal trong Android Studio) và thực hiện 3 bước:

```
# Bước 1: Tải toàn bộ dự án về máy
git clone https://github.com/Theojaville/Lap-trinh-Mobile_Nhom5.git

# Bước 2: Chuyển sang đúng nhánh cá nhân của bạn (Chỉ chạy 1 lệnh tương ứng với vai trò mình)
git checkout thien-nhi-backend-network      # Dành cho Backend/Network
git checkout phu-sy-data-offline         # Dành cho Data/Offline
git checkout nguyet-minh-ui-multiscreen       # Dành cho UI/Multi-screen
git checkout cam-thuy-chat-media           # Dành cho Chat/Media
```

### 2. Quy trình làm việc hằng ngày

```
# Trước khi bắt đầu code mỗi ngày: đồng bộ nhánh main mới nhất về nhánh của mình
git checkout <nhánh-của-bạn>
git pull origin main

# Sau khi code xong 1 phần việc: add, commit, push lên đúng nhánh của mình
git add .
git commit -m "feat: mô tả ngắn gọn việc vừa làm"
git push origin <nhánh-của-bạn>

# Khi phần việc đã hoàn chỉnh và test xong: tạo Pull Request từ nhánh của mình vào main
# (thực hiện trên GitHub, không merge trực tiếp để tránh xung đột code)
# Nếu tạo thêm nhánh mới thì ghi theo cú pháp <tên + nhánh mới>, mỗi lần cập nhật nên tạo nhánh mới (khuyến nghị)
```
