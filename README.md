- **Mục đích** Chạy API local và tìm tài liệu theo công việc.
- **Đối tượng đọc** Dev, reviewer, người tích hợp frontend.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Backend nền tảng học lập trình

Nền tảng giúp người mới và sinh viên học theo roadmap, thực hành và hỏi AI theo ngữ cảnh. Backend hiện có catalog đọc, health và module mẫu `identity` với đăng ký email, đăng nhập, hồ sơ và đăng xuất. Profile `dev` có khóa **Python cơ bản** gồm hai bài mẫu. Stack gồm Java 21, Spring Boot, Maven Wrapper, PostgreSQL và Flyway.

## Chạy nhanh trên Windows

Cài JDK 21, mở Docker Desktop ở chế độ Linux containers. Lần đầu chạy Wrapper cần mạng để tải Maven và dependency. Chạy từ thư mục gốc repo.

```powershell
# ✅ Dùng cấu hình local và Maven Wrapper của repo.
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d --wait
Set-Location backend
.\mvnw.cmd '-Dspring-boot.run.profiles=dev' spring-boot:run
# ❌ Gọi mvn spring-boot:run bằng Maven global và bỏ profile dev.
```

Mở terminal khác để kiểm tra.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/health
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/courses?page=0&size=20'
Invoke-RestMethod http://127.0.0.1:8080/api/v1/courses/python-co-ban
Invoke-RestMethod http://127.0.0.1:8080/api/v1/courses/python-co-ban/lessons/xin-chao-python
```

## Tài liệu theo công việc

- Chạy trên macOS/Linux, đổi cổng, dừng DB và chạy test với PostgreSQL. [Cài đặt local](docs/ENVIRONMENT_SETUP.md) · [Kiểm thử](docs/TESTING_STRATEGY.md)
- Chạy cả backend và PostgreSQL bằng Docker Compose hoặc thử luồng tài khoản qua PowerShell. [Compose local](docs/ENVIRONMENT_SETUP.md#chạy-cả-backend-bằng-compose) · [Thử API tài khoản](docs/AUTH_SECURITY.md#thử-api-tài-khoản)
- Tích hợp và thay đổi API. [Hướng dẫn API](docs/API.md) · [OpenAPI](docs/api/openapi.yaml) · [Quy ước API](docs/API_GUIDELINES.md)
- Chạy và kiểm thử nộp bài/chấm sandbox. [Thiết lập local](docs/ENVIRONMENT_SETUP.md#sandbox-nộp-bài-và-worker) · [Trạng thái hiện tại](docs/PROJECT_STATUS.md) · [Đặc tả sandbox](docs/sandbox/)
- Hiểu module, migration và mốc triển khai. [Kiến trúc](docs/ARCHITECTURE.md) · [Cấu trúc thư mục](docs/FOLDER_STRUCTURE.md) · [Dữ liệu](docs/DATABASE.md) · [Lộ trình](docs/ROADMAP.md)
- Bắt đầu một thay đổi. [Onboarding](docs/ONBOARDING.md) · [Đóng góp](CONTRIBUTING.md) · [Hướng dẫn agent](AGENTS.md) · [Changelog](CHANGELOG.md)
- Kiểm tra mức sẵn sàng của bộ khung. [Tự rà soát và rủi ro còn lại](docs/RELEASE_READINESS.md)

## Checklist

- [ ] Readiness trả trạng thái `UP` và API đọc được bài học mẫu.
- [ ] Đã mở OpenAPI trước khi dùng hoặc đổi payload.
