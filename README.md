# Backend nền tảng học lập trình

Bộ khung dùng Java 21, Spring Boot, Maven Wrapper và PostgreSQL. Bản đầu tiên cung cấp API đọc khoá học và bài học để frontend có thể tích hợp với dữ liệu thật từ PostgreSQL. Dữ liệu mẫu gồm khoá **Python cơ bản** với hai bài học.

## Chạy trên Windows

Cài JDK 21 và mở Docker Desktop với Linux containers. Lần đầu chạy Maven Wrapper cần mạng để tải Maven và các phụ thuộc.

Từ thư mục `project`, sao chép cấu hình cho máy phát triển và khởi động PostgreSQL.

```powershell
Copy-Item .env.example .env
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d --wait
```

Chạy backend từ `project/backend`. Profile `dev` đọc `project/.env`, áp dụng migration Flyway và thêm dữ liệu mẫu. Nếu cổng PostgreSQL đã được sử dụng, đổi `DB_PORT` trong `.env` trước khi chạy Compose.

```powershell
Set-Location backend
.\mvnw.cmd '-Dspring-boot.run.profiles=dev' spring-boot:run
```

Trên macOS hoặc Linux, dùng `cp .env.example .env` và `bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run` cho hai bước tương ứng.

Mở terminal khác để kiểm tra.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/api/v1/health
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/courses?page=0&size=20'
Invoke-RestMethod http://127.0.0.1:8080/api/v1/courses/python-co-ban
Invoke-RestMethod http://127.0.0.1:8080/api/v1/courses/python-co-ban/lessons/xin-chao-python
```

Dừng API bằng `Ctrl+C`. Từ `project`, chạy `docker compose --env-file .env -f infra/docker-compose.dev.yml down` để dừng PostgreSQL và giữ dữ liệu.

## Hợp đồng frontend

| API | Kết quả |
| --- | --- |
| `GET /api/v1/health` | Trạng thái tiến trình API |
| `GET /api/v1/courses` | Danh sách khoá đã xuất bản, sắp theo slug và phân trang từ 0 |
| `GET /api/v1/courses/{courseSlug}` | Thông tin khoá và mục lục bài đã xuất bản theo thứ tự |
| `GET /api/v1/courses/{courseSlug}/lessons/{lessonSlug}` | Nội dung Markdown của bài đã xuất bản trong khoá |
| `GET /actuator/health/readiness` | Mức sẵn sàng phục vụ, gồm kết nối PostgreSQL |

Backend chỉ mở các route đọc đã khai báo. Nội dung nháp trả `404` cả khi truy cập qua URL trực tiếp. Lỗi API dùng `application/problem+json` với `code` và `traceId`. Dùng `X-Request-Id` để đối chiếu với log. CORS của profile `dev` cho phép frontend ở cổng `3000`. Có thể đổi danh sách origin qua `FRONTEND_ORIGINS`.

Slug bài học duy nhất trong phạm vi khoá. Liên kết từ frontend cần giữ cả `courseSlug` và `lessonSlug` để mở trực tiếp đúng bài. Khi render `bodyMarkdown`, frontend cần tắt HTML nhúng và kiểm soát liên kết.

Cập nhật [OpenAPI](docs/api/openapi.yaml) trong cùng PR khi thay đổi đường dẫn hoặc payload.

## Tổ chức mã nguồn

| Vị trí | Trách nhiệm |
| --- | --- |
| `backend/src/main/java/vn/edu/codelearn/platform/content` | API danh mục, điều kiện xuất bản và truy vấn dữ liệu |
| `backend/src/main/java/vn/edu/codelearn/platform/common` | Cấu hình truy cập, health check, lỗi và mã tra cứu log |
| `backend/src/main/resources/db/migration` | Lịch sử thay đổi schema dùng chung |
| `backend/src/main/resources/db/demo` | Dữ liệu mẫu dành cho profile `dev` |
| `infra/docker-compose.dev.yml` | PostgreSQL cho máy phát triển |
| `.github/workflows/backend-ci.yml` | Build và kiểm thử với PostgreSQL khi `project` là thư mục gốc của repo GitHub |

Mã nguồn được tổ chức theo nghiệp vụ trong một ứng dụng Spring Boot. Khi thêm tài khoản, roadmap, chấm bài và AI, đặt mỗi nhóm chức năng trong package riêng. Controller trả DTO. Service chịu trách nhiệm về quy tắc nghiệp vụ và phạm vi transaction. Repository dùng `JdbcClient` với tham số SQL.

Tạo migration `V2__...sql` cho thay đổi schema kế tiếp. Giữ nguyên migration đã áp dụng trên cơ sở dữ liệu dùng chung. Khi chạy ngoài profile `dev`, cung cấp `DB_URL`, `DB_USER`, `DB_PASSWORD` và origin cần thiết qua biến môi trường. Dùng cơ sở dữ liệu riêng cho mỗi môi trường.

## Kiểm thử

CI chạy `verify` với PostgreSQL 16. Bộ kiểm thử tích hợp kiểm tra migration, dữ liệu mẫu, phân trang, thứ tự bài, quy tắc ẩn bản nháp, định dạng lỗi, từ chối truy cập và CORS.

Để chạy bộ kiểm thử trên máy phát triển, dùng cơ sở dữ liệu riêng. Lệnh `createdb` dưới đây chạy một lần với cấu hình `.env.example`. Nếu đã đổi thông số trong `.env`, dùng các giá trị tương ứng.

```powershell
# Từ project
docker compose --env-file .env -f infra/docker-compose.dev.yml exec -T postgres createdb -U learn codelearn_test
Set-Location backend
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/codelearn_test'
$env:TEST_DB_USER = 'learn'
$env:TEST_DB_PASSWORD = 'learn_dev_password'
.\mvnw.cmd verify
```

Khi không có `TEST_DB_URL`, Maven vẫn có thể build và sẽ bỏ qua nhóm kiểm thử cần PostgreSQL.

## Lộ trình

Xem [lộ trình backend 16 tuần](docs/ROADMAP.md) để phân công backlog và nghiệm thu theo từng mốc. Sau khi frontend gọi được API bài học, hai việc kế tiếp là chốt hợp đồng cho roadmap mẫu và chốt ma trận quyền để bắt đầu tài khoản, CMS và lưu tiến độ.
