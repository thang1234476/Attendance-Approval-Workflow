# 📋 Attendance & Leave Management System

Hệ thống quản lý chấm công và nghỉ phép nhân viên, xây dựng bằng Spring Boot + MySQL + JWT Authentication.

## 🛠️ Công nghệ sử dụng

| Layer | Công nghệ |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.2.2 |
| Security | Spring Security + JWT (jjwt 0.11.5) |
| Database | MySQL 8+ |
| ORM | Spring Data JPA / Hibernate |
| Frontend | HTML, CSS, JavaScript (thuần) |
| Build Tool | Maven |

## 📁 Cấu trúc dự án

```
finalproject/
├── src/main/java/com/example/attendance/
│   ├── config/          # Cấu hình Security, Exception Handler
│   ├── controller/      # REST API Controllers
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA Entities
│   ├── repository/      # Spring Data Repositories
│   ├── security/        # JWT Filter, UserDetails Service
│   └── service/         # Business Logic
├── src/main/resources/
│   ├── static/          # Frontend HTML/CSS/JS
│   ├── application.properties.example  # Template cấu hình
│   └── schema.sql       # Script khởi tạo DB
└── pom.xml
```

## ⚙️ Cài đặt & Chạy dự án

### Yêu cầu hệ thống
- Java 17+
- Maven 3.6+
- MySQL 8+

### Bước 1: Clone dự án
```bash
git clone https://github.com/<your-username>/attendance-system.git
cd attendance-system
```

### Bước 2: Cấu hình Database
```bash
# Tạo file application.properties từ template
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Mở `application.properties` và điền thông tin:
```properties
spring.datasource.username=<tên_user_mysql>
spring.datasource.password=<mật_khẩu_mysql>
application.security.jwt.secret-key=<chuỗi_bí_mật_ít_nhất_64_ký_tự>
```

### Bước 3: Khởi tạo Database
```sql
-- Đăng nhập MySQL và chạy:
CREATE DATABASE attendance_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Sau đó chạy file `src/main/resources/schema.sql` để tạo bảng.

### Bước 4: Chạy ứng dụng
```bash
mvn spring-boot:run
```

Ứng dụng sẽ chạy tại: **http://localhost:8080**

## 🔑 Tài khoản mặc định

| Role | Username | Password |
|------|----------|----------|
| Admin | admin | (set khi khởi tạo) |

## 📌 Các tính năng chính

- ✅ Đăng nhập / Xác thực JWT
- ✅ Quản lý người dùng (ADMIN)
- ✅ Chấm công (check-in / check-out)
- ✅ Đăng ký & phê duyệt nghỉ phép
- ✅ Dashboard thống kê
- ✅ Cấu hình hệ thống

## 🔒 Bảo mật

- **Không commit** file `application.properties` — đã được loại trừ trong `.gitignore`
- Sử dụng file `application.properties.example` làm template
- JWT secret key phải là chuỗi ngẫu nhiên, đủ mạnh (ít nhất 64 ký tự)

## 📄 License

MIT License
