# Hiện trạng mã nguồn dự án

Tài liệu này ghi lại những gì đang có trong repository tại thời điểm rà soát. Nội dung được tổng hợp từ mã nguồn, cấu hình, OpenAPI, kiểm thử và roadmap; các mục trong roadmap là kế hoạch, không được xem là chức năng đã triển khai.

## Tóm tắt

Repository hiện là **backend nền tảng học lập trình**, viết bằng Java 21 và Spring Boot 4.1.0, dùng PostgreSQL, JDBC và Flyway. Ngoài catalog và identity theo phiên, đã có API nhận bài Python, hàng đợi Redis và worker riêng tích hợp Judge0 qua adapter. Việc chạy/chấm với Judge0 thật chưa được xác minh; không xem kiểm thử stub là nghiệm thu sandbox cách ly.

Chưa có mã frontend trong repository. CMS, roadmap, theo dõi tiến độ và trợ lý AI hiện mới nằm trong lộ trình. Sandbox chỉ hỗ trợ Python; worker cần Judge0 riêng và chưa được nghiệm thu bằng Judge0 thật.

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
| `POST /api/v1/exercises/{exerciseId}/submissions` | Học viên đã đăng nhập nộp Python; kiểm tra giới hạn và trả `202 QUEUED` sau khi ghi DB, enqueue ID qua Redis. |
| `GET /api/v1/submissions/{submissionId}` | Poll trạng thái/kết quả của submission thuộc người dùng hiện tại. |
| `GET /api/v1/exercises/{exerciseId}/submissions` | Lịch sử submission của người dùng hiện tại cho bài tập. |
| `GET /api/v1/submissions/{submissionId}/detail` | Xem lại mã nguồn và kết quả test của submission thuộc người dùng hiện tại. |

Các truy vấn catalog chỉ lấy nội dung đã xuất bản. Khóa nháp, bài nháp, hoặc bài đã xuất bản nhưng thuộc khóa nháp đều không được truy cập qua API công khai và trả `404`. Submission dùng session hiện tại, CSRF cho request ghi, và vai trò học viên trở lên. Polling đã có; SSE chưa có.

Lỗi được chuẩn hóa theo `application/problem+json`, có `code` và `traceId`. Mỗi request có `X-Request-Id`; giá trị này cũng được đưa vào MDC để đối chiếu log. Các route ngoài danh sách cho phép bị từ chối. CORS cho API được cấu hình theo danh sách origin, profile phát triển mặc định cho phép frontend ở cổng 3000.

## Mã nguồn hiện có

### Backend

- `backend/src/main/java/vn/edu/codelearn/platform/CodeLearnApplication.java`: điểm khởi chạy Spring Boot.
- `.../content/CatalogController.java`: ba endpoint đọc khóa học và bài học.
- `.../content/CatalogService.java`: xử lý truy vấn danh mục, phân trang và trường hợp không tìm thấy.
- `.../content/CatalogRepository.java`: truy vấn PostgreSQL bằng `JdbcClient`, lọc nội dung đã xuất bản.
- `.../content/CatalogViews.java`: DTO/record cho danh sách, chi tiết khóa học và bài học.
- `.../sandbox/ExerciseController.java`, `ExerciseService.java`, `ExerciseRepository.java`, `ExerciseViews.java`: API đọc bài tập đã xuất bản; truy vấn test công khai qua view `test_case_public`.
- `.../submission/SubmissionController.java` và các lớp dịch vụ/repository: tiếp nhận, giới hạn, lịch sử, polling và chi tiết; API chỉ enqueue ID, không gọi Judge0.
- `.../sandbox/Judge0Client.java`, `.../submission/SubmissionWorker.java`: adapter Judge0 và worker chỉ bật với profile `worker`; worker xử lý submission, retry/recovery và ghi kết quả.
- `.../common/SecurityConfiguration.java`: cấu hình session, CSRF, quyền submission, whitelist route và CORS.
- `.../common/HealthController.java`: endpoint health của ứng dụng.
- `.../common/RequestIdFilter.java`: sinh request ID, gắn response header và MDC.
- `.../common/ApiExceptionHandler.java`: ánh xạ lỗi HTTP thành payload Problem Detail có mã lỗi ổn định.

### Cơ sở dữ liệu và cấu hình

- `backend/src/main/resources/db/migration/V1__create_catalog.sql`: tạo bảng `course` và `lesson`, ràng buộc slug/vị trí, quan hệ khóa-bài và cột thời điểm xuất bản.
- `backend/src/main/resources/db/demo/R__seed_demo_catalog.sql`: dữ liệu mẫu Python cơ bản gồm “Xin chào, Python” và “Biến và giá trị”, chỉ được nạp trong profile `dev`.
- `application.yml` và `application-dev.yml`: cấu hình datasource, Flyway, cổng máy chủ, health probe và CORS; profile dev đọc `.env` phía trên thư mục backend.
- `infra/docker-compose.dev.yml` và `.env.example`: PostgreSQL 16 và Redis cho môi trường phát triển.
- `backend/pom.xml`, Maven Wrapper: cấu hình build Java 21 và các phụ thuộc Spring Web MVC, Security, Validation, Actuator, JDBC, Flyway, PostgreSQL và Redis.

### Hợp đồng, kiểm thử và CI

- `docs/api/openapi.yaml`: hợp đồng OpenAPI cho health, danh mục, identity và API đọc bài tập.
- `backend/src/test/java/.../CatalogIntegrationTest.java`: integration test cho health/readiness, phân trang, thứ tự bài, ẩn nội dung nháp, lỗi validation, từ chối truy cập và CORS. Nhóm test cần PostgreSQL và được bật khi có `TEST_DB_URL`.
- `backend/src/test/java/.../ExerciseIntegrationTest.java`: kiểm tra quyền đọc công khai, xuất bản, test công khai và không rò rỉ dữ liệu canary.
- `backend/src/test/java/.../SubmissionIntegrationTest.java`: kiểm tra xác thực, giới hạn submission, quyền sở hữu, polling/history/detail và không rò rỉ test ẩn.
- `backend/src/test/java/.../SubmissionQueueIntegrationTest.java`: kiểm tra submission đã commit và queue Redis nhận đúng ID.
- `backend/src/test/java/.../GradingServiceTest.java`: kiểm tra grading chạy qua fake `SandboxClient`, bao gồm test ẩn và dừng khi Compile Error.
- Kiểm tra gần nhất: `./mvnw --batch-mode --no-transfer-progress verify` với PostgreSQL test riêng và Redis: **35 test, 0 failure, 0 error, 0 skipped**; Spotless/Checkstyle đạt. Judge0 thật không được chạy.
- `backend/src/test/java/.../GradingRulesTest.java` và `Judge0ClientTest.java`: kiểm tra quy tắc chấm và giao tiếp Judge0 bằng HTTP stub cục bộ; không chạy mã trong Judge0 thật.
- `backend/src/test/resources/catalog-fixtures.sql`: dữ liệu fixture cho kiểm thử.
- `.github/workflows/backend-ci.yml`: GitHub Actions dùng Java 21 và PostgreSQL 16, chạy `./mvnw verify` khi có thay đổi liên quan.

## Giới hạn và phần chưa nghiệm thu

Judge0 thật, cách ly môi trường/mạng/tài nguyên, chạy N2/N4 và kiểm thử tải chưa được xác minh. Không khởi chạy worker trên môi trường dùng chung để thực thi mã tùy ý. SSE, giao diện frontend; CMS và quy trình duyệt/xuất bản; blog; roadmap/đồ thị lộ trình; lưu tiến độ học; playground; trợ lý AI/LLM; staging/production và giám sát vận hành chưa được triển khai.

Roadmap 16 tuần trong `docs/ROADMAP.md` mô tả các hạng mục dự kiến, tiêu chí nghiệm thu và mục tiêu nội dung. Đây là kế hoạch cho các bước tiếp theo, không phải bằng chứng các tính năng đó đã có trong backend.

## Cách chạy bản phát triển

README hướng dẫn sao chép `.env.example` thành `.env`, khởi động PostgreSQL và Redis bằng Docker Compose, sau đó chạy API từ `backend`:

```bash
bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run
```

Worker chạy thành tiến trình riêng với profile `worker` và cần `JUDGE0_BASE_URL`, `JUDGE0_PYTHON_LANGUAGE_ID` cùng biến kết nối DB/Redis. Các URL và payload cụ thể được mô tả trong `docs/api/openapi.yaml`; hướng dẫn an toàn và trạng thái nghiệm thu sandbox ở `docs/ENVIRONMENT_SETUP.md`.
