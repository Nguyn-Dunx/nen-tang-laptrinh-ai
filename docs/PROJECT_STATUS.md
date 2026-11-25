# Hiện trạng mã nguồn dự án

Tài liệu này ghi lại những gì đang có trong repository tại thời điểm rà soát. Nội dung được tổng hợp từ mã nguồn, cấu hình, OpenAPI, kiểm thử và roadmap; các mục trong roadmap là kế hoạch, không được xem là chức năng đã triển khai.

## Tóm tắt

Repository hiện là **bộ khung backend cho nền tảng học lập trình**, viết bằng Java 21 và Spring Boot 4.1.0, dùng PostgreSQL, JDBC và Flyway. Backend có catalog đọc nội dung đã xuất bản, identity theo phiên và API đọc Exercise công khai. Có dữ liệu mẫu khóa Python cơ bản với hai bài học.

Chưa có mã frontend trong repository. Các chức năng tài khoản, CMS, roadmap, theo dõi tiến độ, chạy/chấm mã và trợ lý AI hiện mới nằm trong lộ trình.

## Những gì API hiện làm được

| API | Hiện trạng |
| --- | --- |
| `GET /api/v1/health` | Trả trạng thái API (`UP`). |
| `GET /actuator/health/liveness` | Probe kiểm tra tiến trình còn hoạt động. |
| `GET /actuator/health/readiness` | Probe sẵn sàng, gồm trạng thái PostgreSQL. |
| `GET /api/v1/courses?page=0&size=20` | Liệt kê khóa đã xuất bản, sắp theo slug, phân trang; kích thước trang tối đa 50. |
| `GET /api/v1/courses/{courseSlug}` | Trả thông tin khóa và danh sách bài đã xuất bản theo vị trí. |
| `GET /api/v1/courses/{courseSlug}/lessons/{lessonSlug}` | Trả thông tin bài cùng nội dung Markdown. |
| `GET /api/v1/exercises/{exerciseId}` | Đọc bài tập đã xuất bản cùng test công khai; công khai, không trả test ẩn hoặc số lượng test ẩn. |

Các truy vấn chỉ lấy nội dung đã xuất bản. Khóa nháp, bài nháp, hoặc bài đã xuất bản nhưng thuộc khóa nháp đều không được truy cập qua API công khai và trả `404`.

Lỗi được chuẩn hóa theo `application/problem+json`, có `code` và `traceId`. Mỗi request có `X-Request-Id`; giá trị này cũng được đưa vào MDC để đối chiếu log. Các route ngoài danh sách cho phép và các HTTP method ghi dữ liệu bị từ chối. CORS cho API được cấu hình theo danh sách origin, profile phát triển mặc định cho phép frontend ở cổng 3000.

## Mã nguồn hiện có

### Backend

- `backend/src/main/java/vn/edu/codelearn/platform/CodeLearnApplication.java`: điểm khởi chạy Spring Boot.
- `.../content/CatalogController.java`: ba endpoint đọc khóa học và bài học.
- `.../content/CatalogService.java`: xử lý truy vấn danh mục, phân trang và trường hợp không tìm thấy.
- `.../content/CatalogRepository.java`: truy vấn PostgreSQL bằng `JdbcClient`, lọc nội dung đã xuất bản.
- `.../content/CatalogViews.java`: DTO/record cho danh sách, chi tiết khóa học và bài học.
- `.../sandbox/ExerciseController.java`, `ExerciseService.java`, `ExerciseRepository.java`, `ExerciseViews.java`: API đọc bài tập đã xuất bản; truy vấn test công khai qua view `test_case_public`.
- `.../common/SecurityConfiguration.java`: cấu hình stateless, whitelist GET, từ chối route/method còn lại và CORS.
- `.../common/HealthController.java`: endpoint health của ứng dụng.
- `.../common/RequestIdFilter.java`: sinh request ID, gắn response header và MDC.
- `.../common/ApiExceptionHandler.java`: ánh xạ lỗi HTTP thành payload Problem Detail có mã lỗi ổn định.

### Cơ sở dữ liệu và cấu hình

- `backend/src/main/resources/db/migration/V1__create_catalog.sql`: tạo bảng `course` và `lesson`, ràng buộc slug/vị trí, quan hệ khóa-bài và cột thời điểm xuất bản.
- `backend/src/main/resources/db/demo/R__seed_demo_catalog.sql`: dữ liệu mẫu Python cơ bản gồm “Xin chào, Python” và “Biến và giá trị”, chỉ được nạp trong profile `dev`.
- `application.yml` và `application-dev.yml`: cấu hình datasource, Flyway, cổng máy chủ, health probe và CORS; profile dev đọc `.env` phía trên thư mục backend.
- `infra/docker-compose.dev.yml` và `.env.example`: PostgreSQL 16 cho môi trường phát triển.
- `backend/pom.xml`, Maven Wrapper: cấu hình build Java 21 và các phụ thuộc Spring Web MVC, Security, Validation, Actuator, JDBC, Flyway và PostgreSQL.

### Hợp đồng, kiểm thử và CI

- `docs/api/openapi.yaml`: hợp đồng OpenAPI cho health, danh mục, identity và API đọc bài tập.
- `backend/src/test/java/.../CatalogIntegrationTest.java`: integration test cho health/readiness, phân trang, thứ tự bài, ẩn nội dung nháp, lỗi validation, từ chối truy cập và CORS. Nhóm test cần PostgreSQL và được bật khi có `TEST_DB_URL`.
- `backend/src/test/java/.../ExerciseIntegrationTest.java`: kiểm tra quyền đọc công khai, xuất bản, test công khai và không rò rỉ dữ liệu canary.
- `backend/src/test/resources/catalog-fixtures.sql`: dữ liệu fixture cho kiểm thử.
- `.github/workflows/backend-ci.yml`: GitHub Actions dùng Java 21 và PostgreSQL 16, chạy `./mvnw verify` khi có thay đổi liên quan.

## Chưa có trong mã nguồn hiện tại

Theo đối chiếu các package và API đang có, các hạng mục sau chưa được triển khai: giao diện frontend; CMS và quy trình duyệt/xuất bản; blog; roadmap/đồ thị lộ trình; lưu tiến độ học; playground; nộp bài và chấm sandbox (worker/Judge0); trợ lý AI/LLM; SSE, hàng đợi hoặc worker; triển khai staging/production và giám sát vận hành. Identity có đăng ký/đăng nhập và hồ sơ phiên, nhưng chưa có phân quyền nghiệp vụ cho vai trò.

Roadmap 16 tuần trong `docs/ROADMAP.md` mô tả các hạng mục dự kiến, tiêu chí nghiệm thu và mục tiêu nội dung. Đây là kế hoạch cho các bước tiếp theo, không phải bằng chứng các tính năng đó đã có trong backend.

## Cách chạy bản phát triển

README hướng dẫn sao chép `.env.example` thành `.env`, khởi động PostgreSQL bằng Docker Compose, sau đó chạy từ `backend`:

```bash
bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run
```

Các URL và payload cụ thể được mô tả trong `README.md` và `docs/api/openapi.yaml`.
