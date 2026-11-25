# AGENTS.md

Đọc file này ở đầu MỖI phiên, trước khi sửa bất kỳ dòng mã nào.
Không khẳng định điều gì không có trong mã hoặc trong file này. Chỗ chưa rõ nằm ở mục 10.

## 1. Tổng quan và trạng thái

Nền tảng học lập trình trực tuyến tích hợp trợ lý AI (đồ án tích hợp hệ thống, ~16 tuần, nhóm nhỏ).
Repo hiện là bộ khung backend. Chưa có frontend.

- ĐÃ CÓ = nằm trong mã. KẾ HOẠCH = chỉ nằm trong tài liệu, kể cả `docs/ROADMAP.md`.
- Roadmap KHÔNG phải bằng chứng đã triển khai. Không coi tính năng chưa có là đã có.

| Hạng mục | ĐÃ CÓ trong mã | CHƯA CÓ |
| --- | --- | --- |
| Stack | Java 21, Spring Boot 4.1.0, PostgreSQL 16, Redis, `JdbcClient`, Flyway, Maven Wrapper | — |
| Nội dung | Đọc khóa học và bài học đã xuất bản; seed demo Python cơ bản (chỉ profile `dev`) | CMS, quy trình duyệt/xuất bản, blog, roadmap |
| API | Health, catalog, identity theo phiên, Exercise read, submission/polling/history/detail | Lưu tiến độ, playground, trợ lý AI, SSE |
| Chạy và chấm mã | Submission API, rate limit/Redis queue, worker profile riêng, Judge0 adapter, quy tắc chấm | Judge0 thật và isolation chưa nghiệm thu |
| Hạ tầng | Docker Compose PostgreSQL + Redis cho dev, CI backend | Staging/production, giám sát |
| Giao diện | (không có) | Frontend |

API hiện có:

| Endpoint | Ghi chú |
| --- | --- |
| `/api/v1/health` | Trạng thái API |
| `/actuator/health/liveness` | Probe tiến trình còn sống |
| `/actuator/health/readiness` | Probe sẵn sàng, gồm PostgreSQL |
| `/api/v1/courses?page=0&size=20` | Sắp theo slug, size tối đa 50 |
| `/api/v1/courses/{courseSlug}` | Khóa và danh sách bài đã xuất bản |
| `/api/v1/courses/{courseSlug}/lessons/{lessonSlug}` | Bài và nội dung Markdown |
| `/api/v1/exercises/{exerciseId}` | Bài tập đã xuất bản, chỉ kèm test công khai |
| `POST /api/v1/exercises/{exerciseId}/submissions` | Nộp Python, cần session + CSRF và vai trò LEARNER trở lên |
| `GET /api/v1/submissions/{submissionId}` | Poll trạng thái/kết quả của submission thuộc user |
| `GET /api/v1/exercises/{exerciseId}/submissions` | Lịch sử submission của user |
| `GET /api/v1/submissions/{submissionId}/detail` | Mã nguồn và kết quả test của submission thuộc user |

## 2. Cấu trúc repo

```
.
├── AGENTS.md                      # file này
├── README.md                      # hướng dẫn chạy
├── .github/workflows/backend-ci.yml
├── infra/docker-compose.dev.yml   # PostgreSQL 16 + Redis cho dev
├── docs/
│   ├── api/openapi.yaml           # hợp đồng API (nguồn sự thật cho endpoint)
│   ├── ROADMAP.md                 # kế hoạch 16 tuần (KẾ HOẠCH)
│   └── sandbox/                   # tài liệu Coding + Sandbox
└── backend/
    ├── pom.xml, Maven Wrapper
    └── src/
        ├── main/java/vn/edu/codelearn/platform/
        │   ├── CodeLearnApplication.java
        │   ├── content/           # catalog: khóa học, bài học
        │   ├── sandbox/           # Exercise read API + Judge0 adapter
        │   ├── submission/        # API, queue, worker, grading
        │   └── common/            # security, health, request id, xử lý lỗi
        ├── main/resources/
        │   ├── application.yml, application-dev.yml
        │   ├── db/migration/V1__create_catalog.sql
        │   └── db/demo/R__seed_demo_catalog.sql
        └── test/
            ├── java/.../CatalogIntegrationTest.java
            └── resources/catalog-fixtures.sql
```

Vai trò class hiện có:

- `content/`: `CatalogController` (3 endpoint), `CatalogService` (truy vấn, phân trang, không tìm thấy), `CatalogRepository` (`JdbcClient`, lọc đã xuất bản), `CatalogViews` (record DTO).
- `sandbox/`: `ExerciseController`, `ExerciseService`, `ExerciseRepository`, `ExerciseViews`; chỉ trả test công khai qua view `test_case_public`.
- `submission/`: nhận bài, giới hạn, Redis queue, lịch sử/polling/chi tiết và worker chỉ bật ở profile `worker`.
- `sandbox/`: Exercise read API, cấu hình giới hạn và Judge0 adapter (chỉ worker gọi).
- `common/`: `SecurityConfiguration` (session, CSRF, quyền route, CORS), `HealthController`, `RequestIdFilter` (`X-Request-Id`, MDC), `ApiExceptionHandler` (Problem Detail).
- Migration `V1`: bảng `course`, `lesson`, ràng buộc slug/vị trí, cột thời điểm xuất bản.
- Seed `R__seed_demo_catalog.sql`: chỉ nạp ở profile `dev`.
- `application-dev.yml`: profile `dev` đọc `.env` nằm phía trên thư mục `backend`.
- `.env.example`, `PROJECT_STATUS.md`: vị trí chính xác xem mục 10.

Judge0 thật, isolation mạng/tài nguyên và N2/N4 chưa được xác minh. Không chạy worker trên môi trường dùng chung để thực thi mã tùy ý.

## 3. Lệnh thường dùng

Chỉ dùng lệnh dưới đây. Lệnh nào thiếu thì ghi TODO, không tự đoán.

| Việc | Lệnh |
| --- | --- |
| Chạy dev (từ `backend`) | `bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run` |
| Build + test | `./mvnw verify` |
| Khởi động PostgreSQL dev | Docker Compose với `infra/docker-compose.dev.yml` (TODO lệnh chính xác) |
| Chạy một test riêng | TODO(xác nhận) |
| Lint/format | TODO(xác nhận) |

Chuẩn bị môi trường dev:

1. Sao chép `.env.example` thành `.env`.
2. Khởi động PostgreSQL bằng Docker Compose (`infra/docker-compose.dev.yml`).
3. Từ `backend`, chạy lệnh chạy dev ở bảng trên.

Cảnh báo về test:

- Test tích hợp cần PostgreSQL và CHỈ bật khi có biến `TEST_DB_URL`.
- Thiếu `TEST_DB_URL` thì nhóm test này không chạy. Khi đó `./mvnw verify` xanh chưa chứng minh được gì về phần cần DB.
- Phải đặt `TEST_DB_URL` trước khi chạy `./mvnw verify` để kết luận "đã xong".

## 4. Convention bắt buộc

- **Layering**: Controller → Service → Repository. Controller không đụng DB. Repository không chứa logic nghiệp vụ. Mẫu: `CatalogController` → `CatalogService` → `CatalogRepository`.
- **DTO**: là `record`, đặt trong file `*Views` (mẫu `CatalogViews`). Không trả row hay object thô ra response.
- **Truy cập dữ liệu**: dùng `JdbcClient`. KHÔNG dùng JPA/Hibernate.
- **Lỗi**: mọi lỗi HTTP đi qua `ApiExceptionHandler`, trả `application/problem+json` có `code` ổn định và `traceId`. Thêm `code` mới được; không đổi `code` đã có.
- **Request ID**: giữ `X-Request-Id` và MDC do `RequestIdFilter` cấp. Không bỏ qua, không tự sinh ID song song.
- **Chỉ trả nội dung đã xuất bản**: truy vấn công khai luôn lọc đã xuất bản. Khóa nháp, bài nháp, bài đã xuất bản thuộc khóa nháp → `404`.
- **Phân trang**: ví dụ `page=0&size=20`, `size` tối đa 50.
- **Flyway**: chỉ thêm file `V<n>__<mô_tả>.sql` mới (`n` lớn hơn mọi phiên bản hiện có, hiện biết `V1`). Không sửa migration đã có. Seed demo dùng `R__*` trong `db/demo/`, chỉ profile `dev`.
- **Test**: mỗi thay đổi có test. Dữ liệu test nằm trong fixture ở `backend/src/test/resources/` (mẫu `catalog-fixtures.sql`). Test tích hợp theo mẫu `CatalogIntegrationTest`.
- **Contract-first**: sửa `docs/api/openapi.yaml` TRƯỚC khi code, cùng PR với mã. Chỉ thêm. Không phá endpoint hiện có (không đổi path, method, field, status code đang có).
- **Package theo feature**: mã mới thành package riêng cạnh `content/`, không nhét vào `content/` hay `common/`.
- **Cấu hình**: giá trị theo môi trường lấy từ `.env`/biến môi trường và `application*.yml`. Không hard-code.

## 5. Quy tắc cứng: tuyệt đối KHÔNG

- KHÔNG sửa migration đã có (`V1__create_catalog.sql`). Cần đổi schema thì thêm `V<n>` mới.
- KHÔNG chạy mã người dùng trong JVM của ứng dụng.
- KHÔNG nới `SecurityConfiguration` ngoài đúng route được nêu trong task. Hiện mọi route/method ngoài whitelist `GET` đều bị từ chối.
- KHÔNG thêm dependency khi chưa hỏi.
- KHÔNG để secret trong mã hoặc log.
- KHÔNG để lộ test ẩn hay lời giải tham khảo. Không DTO, response hay log nào chứa `expected_output` của test ẩn hoặc lời giải tham khảo.
- KHÔNG dùng `callback_url` của Judge0.
- KHÔNG public Judge0. Chỉ worker được gọi Judge0.
- KHÔNG `@Disabled` test, KHÔNG sửa test cho xanh thay vì sửa lỗi thật.
- KHÔNG tuyên bố tính năng "đã xong" khi chưa chạy `./mvnw verify` (có `TEST_DB_URL`, xem mục 3).
- KHÔNG phá endpoint hiện có trong `docs/api/openapi.yaml`.
- KHÔNG bịa nội dung `docs/sandbox/*` khi file thiếu.

## 6. Tính năng đang làm: Coding và Sandbox

Chấm bài tự động cho bài tập lập trình. Python trước. CHƯA có dòng mã nào của tính năng này.

Phạm vi backend P1:

| Mã | Nội dung |
| --- | --- |
| SB-02 | Bài tập chấm theo test case stdin/stdout qua Judge0 |
| SB-03 | 6 verdict: Accepted, Wrong Answer, Time Limit, Memory Limit, Runtime Error, Compile Error. Điểm = tổng trọng số test đạt / tổng trọng số, nhân điểm tối đa |
| SB-04 | Test case công khai và ẩn |
| SB-05 | Lưu lịch sử nộp bài |
| SB-06 | Giới hạn tần suất nộp, kích thước mã, thời gian, bộ nhớ, số tiến trình, số bài nộp đang chờ của mỗi user |

Ngoài phạm vi: SB-01 (iframe, thuộc frontend), SB-07 đến SB-10.

Quyết định đã chốt:

- **Luồng 3 pha**: tiếp nhận (lưu `Queued`, trả id ngay) → thực thi (worker gọi Judge0 từng test) → tổng hợp (verdict, điểm, phát kết quả).
- **Trả kết quả**: SSE, có polling làm fallback.
- **Lưu trữ**: Redis cho queue và rate limit. PostgreSQL là source of truth. Có job quét lại bài nộp kẹt ở `QUEUED`/`RUNNING`.
- **Judge0**: chạy trên VPS riêng, không có Internet, không truy cập ngược về máy chính. Worker tự poll Judge0 (không `callback_url`). Judge0 không public.
- **Auth**: nộp bài yêu cầu đăng nhập, vai trò Học viên trở lên. 4 vai trò: Khách, Học viên, Tác giả/Biên tập, Quản trị. Auth chưa có, nên dùng một `CurrentUser` stub CHỈ ở profile `dev`. Stub không được chạy ngoài `dev`.
- **Bí mật**: test ẩn và lời giải tham khảo không bao giờ rời server.
- **Giới hạn sandbox**: lưu trong bảng cấu hình (admin chỉnh được), không hard-code.
- **Cho giai đoạn AI sau**: module Submission cung cấp sẵn số lần nộp chưa đạt và cờ đã Accepted theo (user, exercise) cho hint ladder.
- **Security hiện tại**: các route nộp bài mới cần mở trong `SecurityConfiguration`. Chỉ mở đúng route task nêu. Task không nêu rõ thì dừng và hỏi.

File nào quyết định điều gì (vai trò suy ra từ tên file, chưa xác minh nội dung; lệch thì dừng và báo):

| File trong `docs/sandbox/` | Quyết định |
| --- | --- |
| `00-SCOPE.md` | Phạm vi P1, trong/ngoài phạm vi |
| `01-ARCHITECTURE.md` | Kiến trúc, luồng 3 pha, ranh giới thành phần |
| `02-DATA-MODEL.md` | Mô hình dữ liệu, bảng cần có |
| `03-GRADING-SPEC.md` | Quy tắc chấm: verdict, điểm |
| `04-API-CONTRACT.md` | Hợp đồng API của tính năng (nguồn để cập nhật `docs/api/openapi.yaml`) |
| `05-SECURITY.md` | Ràng buộc bảo mật, giới hạn sandbox |
| `06-TEST-PLAN.md` (+ `fixtures/`) | Kế hoạch test, fixture |
| `07-TASKS.md` | Danh sách task, thứ tự, acceptance criteria |

Thứ tự đọc: `00-SCOPE` → `01-ARCHITECTURE` → `02-DATA-MODEL` → `03-GRADING-SPEC` → `04-API-CONTRACT` → `05-SECURITY` → `06-TEST-PLAN` → `07-TASKS`.
Đầu phiên đọc theo thứ tự trên. Các task sau trong cùng phiên chỉ đọc lại file liên quan.
Không chép chi tiết các file này vào mã, comment hay file khác. Trỏ tới file.

## 7. Quy trình làm việc của agent

1. Đọc `AGENTS.md` này.
2. Kiểm tra `docs/sandbox/*` có đủ không. Thiếu file thì DỪNG và báo người. Không tự viết thay.
3. Đọc docs liên quan và chọn đúng MỘT task trong `docs/sandbox/07-TASKS.md`. Mỗi lần chỉ làm một task.
4. Tài liệu mâu thuẫn với mã, hoặc mâu thuẫn với nhau: DỪNG và báo. Không tự quyết.
5. Sửa `docs/api/openapi.yaml` TRƯỚC (chỉ thêm, không phá endpoint hiện có).
6. Viết code theo convention mục 4, kèm test và fixture.
7. Đặt `TEST_DB_URL`, chạy `./mvnw verify`. Sửa lỗi thật nếu đỏ.
8. Khi xong một giai đoạn, cập nhật `PROJECT_STATUS.md`. Chỉ ghi điều đã có trong mã, không ghi kế hoạch như đã làm.
9. Báo người: task đã làm, file đã đổi, kết quả `./mvnw verify`, điểm còn mở.

## 8. Definition of Done

Một task chỉ xong khi đánh dấu được hết:

- [ ] Task có trong `07-TASKS.md` và có acceptance criteria. Mọi criteria đã đáp ứng.
- [ ] `docs/api/openapi.yaml` đã cập nhật trước khi code, chỉ thêm, không phá endpoint cũ.
- [ ] Đúng layering Controller → Service → Repository, DTO là record trong `*Views`, dùng `JdbcClient`.
- [ ] Mọi lỗi mới đi qua `ApiExceptionHandler` với `code` ổn định.
- [ ] Migration mới là `V<n>` mới. Không sửa `V1`.
- [ ] Có test và fixture. Không `@Disabled`, không sửa test cho xanh.
- [ ] `./mvnw verify` xanh khi đã đặt `TEST_DB_URL`.
- [ ] Không DTO, response hay log nào chứa `expected_output` của test ẩn hoặc lời giải tham khảo.
- [ ] Không secret trong mã hoặc log. Không hard-code giới hạn sandbox.
- [ ] `SecurityConfiguration` không bị nới ngoài route task nêu.
- [ ] Không thêm dependency khi chưa được duyệt.
- [ ] `PROJECT_STATUS.md` đã cập nhật nếu xong một giai đoạn.

## 9. Khi nào phải dừng và hỏi người

DỪNG, báo người và chờ trả lời khi gặp một trong các tình huống:

- Thiếu bất kỳ file nào trong `docs/sandbox/*` mà task cần.
- Tài liệu mâu thuẫn với mã, hoặc các tài liệu mâu thuẫn nhau.
- Cần thêm dependency mới (kể cả client Redis hay HTTP).
- Cần đổi `SecurityConfiguration`, kể cả để mở một route mới.
- Cần sửa migration đã có (`V1`).
- Task không có acceptance criteria, hoặc phạm vi task vượt SB-02 đến SB-06.
- Cần sửa hay xóa endpoint hiện có trong `docs/api/openapi.yaml`.
- Test đỏ mà cách duy nhất để xanh là `@Disabled` hoặc sửa test.
- Cần đưa dữ liệu test ẩn, lời giải tham khảo hoặc secret vào response, log hay mã.
- Cần Judge0 gọi ngược về máy chính, hoặc cần mở Judge0 ra ngoài.
- Gặp lệnh, đường dẫn hoặc quy ước chưa có trong file này (xem mục 10).

## 10. Điểm cần xác nhận

- TODO(xác nhận): lệnh chính xác khởi động PostgreSQL bằng Docker Compose từ `infra/docker-compose.dev.yml`.
- TODO(xác nhận): thư mục chạy `./mvnw verify` (giả định `backend`, nơi có Maven Wrapper).
- TODO(xác nhận): giá trị mẫu của `TEST_DB_URL` và cách đặt khi chạy local.
- TODO(xác nhận): CI (`.github/workflows/backend-ci.yml`) có đặt `TEST_DB_URL` để chạy test tích hợp không.
- TODO(xác nhận): lệnh chạy một test hoặc một class test riêng.
- TODO(xác nhận): lệnh lint/format/static analysis, nếu có.
- TODO(xác nhận): vị trí chính xác của `.env.example`, `.env` và `PROJECT_STATUS.md`.
- TODO(xác nhận): cổng và base URL backend khi chạy dev.
- TODO(xác nhận): quy ước commit, tên branch, quy trình PR.
- TODO(xác nhận): cách bật `CurrentUser` stub ở profile `dev` (property hay annotation).
- TODO(xác nhận): Redis cho dev/test (thêm vào `infra/docker-compose.dev.yml` hay không, biến cấu hình).
- TODO(xác nhận): nội dung thực tế của `docs/sandbox/*` có khớp vai trò suy ra từ tên file ở mục 6.
