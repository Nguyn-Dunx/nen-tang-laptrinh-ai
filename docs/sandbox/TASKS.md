# 07-TASKS: Danh sách task triển khai Coding + Sandbox chấm bài

## 1. Mục đích và cách dùng

- Đây là **danh sách task đề xuất**. **Chưa task nào được làm.** Mọi trạng thái ban đầu là `Chưa làm`.
- Mỗi phiên làm **đúng MỘT task**. Task tự đứng được: biết cần đọc gì, được chạm gì, xong khi nào, verify bằng lệnh nào, khi nào phải dừng hỏi người.
- Phạm vi: `00-SCOPE.md`. Quy tắc làm việc chung của repo: `AGENTS.md` ở root. File này không lặp lại cả hai.
- Nhãn: "(đề xuất)" là tên/ranh giới chưa có trong repo, phải khớp `01`, `02`, `04` khi các file đó chốt. `TODO(xác nhận)` là chưa chốt.
- Hiện trạng: repo có catalog chỉ đọc, identity và Exercise read API. **CHƯA CÓ:** Redis, queue/worker, SSE, Judge0, rate limit, nộp bài và chấm sandbox.

## 2. Quy tắc áp dụng cho mọi task

- Không sửa `V1__create_catalog.sql`. Flyway chỉ thêm `V<n>` mới.
- Không chạy mã người dùng trong JVM. Mọi thực thi qua Judge0 adapter trên VPS riêng. **Không dùng `callback_url`.** Không public Judge0. Chỉ worker gọi Judge0.
- Không nới `SecurityConfiguration` ngoài đúng route nêu trong task (`04` mục 10).
- Không thêm dependency (kể cả client Redis, thư viện JWT, thư viện mock, công cụ tải), không thêm service vào compose hay CI khi chưa hỏi người. Task cần thì có cổng **DỪNG VÀ HỎI**.
- Không để secret trong mã hoặc log. Không lộ `expected_output`/`stdin` của test ẩn hay lời giải tham khảo (`04` mục 8).
- Không `@Disabled` test. Không sửa test cho xanh thay vì sửa lỗi thật. Không tuyên bố xong khi chưa chạy `./mvnw verify`.
- Contract-first: sửa `docs/api/openapi.yaml` trước khi code, chỉ thêm, không phá endpoint cũ.
- Gặp tài liệu thiếu hoặc mâu thuẫn giữa `00`–`06`: dừng và hỏi, không tự chọn.

**Quy trình mỗi task:**
1. Đọc các mục docs được chỉ định.
2. Sửa `docs/api/openapi.yaml` (nếu có endpoint).
3. Viết/cập nhật test và fixture.
4. Viết code.
5. Chạy `./mvnw verify` (và lệnh verify riêng của task).
6. Cập nhật trạng thái task trong file này (mục 6).

## 3. Tổng quan và phụ thuộc

| Task | Tên | Phụ thuộc | Cần thật (PG / Redis / Judge0) | Cần người duyệt trước? | Bị chặn bởi cổng (mục 5) | Trạng thái |
|---|---|---|---|---|---|---|
| T0 | Spike Judge0 | Không | Judge0 thật (VPS test) | Có: kết quả quyết định giữ Judge0 | G3, G7 | Chưa làm |
| T1 | Auth / `CurrentUser` stub | Không (độc lập T0) | PG | Có nếu chọn auth thật | G1 | Chưa làm |
| T2a | Migration + fixture SQL | Schema catalog V1 và identity V2 | PG | Không | Không | Xong (2026-11-25, nhánh `feat/sandbox-coding-p1`) |
| T2b | Exercise read API | T2a | PG | Không | Không | Xong (2026-11-25, nhánh `feat/sandbox-coding-p1`; chưa commit) |
| T3a | Nhận bài + giới hạn + hint ladder | T1, T2 | PG | Có: kiểu `user_id`, đường dẫn | G1 | Chưa làm |
| T3b | Rate limit + queue (Redis) | T3a | PG + Redis | **Có: Redis, dependency, CI** | G2 | Chưa làm |
| T4a | Judge0 adapter + stub | T0, T2 | PG (stub, không Judge0 thật) | Không | G3 | Chưa làm |
| T4b | Worker + tổng hợp verdict/điểm | T3b, T4a | PG + Redis | Có: cách đóng gói worker | G2, G4 | Chưa làm |
| T4c | Retry + job quét bài kẹt | T4b | PG + Redis | Không | G4 | Chưa làm |
| T5 | SSE + polling | T3, T4 | PG + Redis | **Có: xác thực SSE** | G5, G1 | Chưa làm |
| T6 | Test bảo mật + tải | T3–T5; Judge0 thật | Cả ba | Có: công cụ tải | G2, G7 | Chưa làm |
| T7 | Hoàn tất | Tất cả | PG | Không | Không | Chưa làm |

**Thứ tự tổng thể:** T0 → T1 → T2 → T3 → T4 → T5 → T6 → T7 (đúng thứ tự phân tích). T1 có thể làm song song T0.

**Lý do tách (đề xuất):**
- **T2a/T2b:** migration (không đụng API) tách khỏi Exercise API (đụng OpenAPI, security, controller) để mỗi phiên chỉ một mối quan tâm.
- **T3a/T3b:** rate limit và queue cần Redis (cần người duyệt), phần còn lại thì không. Tách để T3a không bị chặn.
- **T4a/T4b/T4c:** adapter + stub, worker + tổng hợp, retry + quét bài kẹt, theo gợi ý của phân tích. Mỗi phần đủ lớn cho một phiên.

## 4. Các task

### T0. Spike Judge0 (tuần 1–2, M1, R7)

- **Mục tiêu:** chạy thử Judge0 ở môi trường test/spike, đánh giá độ khó vận hành, ghi lại hành vi thực. Đầu ra là báo cáo, không phải code backend.
- **Mã liên quan:** NF-04, N4, R7, R3.
- **Phải đọc trước:** `05` mục 3, 4, 8, 10; `06` mục 8; `03` mục 4, 11; `01` (ADR về Judge0).
- **File ĐƯỢC chạm:** báo cáo spike (nơi lưu: `TODO(xác nhận)`, đề xuất `PROJECT_STATUS.md` hoặc phụ lục); `docs/sandbox/fixtures/attacks/` (nếu có).
- **KHÔNG chạm:** `backend/src`, `infra/docker-compose.dev.yml`, `V1`.
- **Các bước:**
  - [ ] Dựng Judge0 trên VPS test theo khung runbook `05` mục 8. Ghi phiên bản, vướng mắc.
  - [ ] Cô lập mạng hai chiều, chỉ cho nguồn Worker test gọi tới.
  - [ ] Chạy 8 kịch bản Bảng 5.9 theo `06` mục 8, thu bằng chứng theo `05` mục 10.
  - [ ] Ghi mã status thực tế, cách báo vượt bộ nhớ, lỗi cú pháp Python, tên trường JSON (điền các `TODO` ở `03`).
  - [ ] So sánh poll với `wait=true`, đề xuất chọn một.
  - [ ] Đo sơ bộ thời gian một lượt gọi.
  - [ ] Đánh giá theo tiêu chí "đủ ổn định" (`05` mục 8), đề xuất giữ Judge0 hay kích hoạt dự phòng.
- **Acceptance criteria:**
  - [ ] Có báo cáo spike ghi phiên bản và điều kiện cài đặt.
  - [ ] Đủ 8 kịch bản có bằng chứng và kết quả.
  - [ ] Đã ghi mã status thực tế và cách báo vượt bộ nhớ.
  - [ ] Có đề xuất poll hay `wait=true` kèm số đo.
  - [ ] Có kết luận giữ Judge0 hoặc kích hoạt phương án dự phòng.
- **Lệnh verify:** không có lệnh Maven. Lệnh dựng/chạy Judge0 và kịch bản: `TODO(xác nhận)`, điền sau spike.
- **DỪNG VÀ HỎI nếu:**
  - Judge0 không đạt tiêu chí ổn định, hoặc định kích hoạt Judge0 dịch vụ/Piston (phải rà lại `01`, `03`, `05`).
  - Cần chạy fixture tấn công ngoài môi trường test, hoặc không chắc môi trường có cô lập khỏi hạ tầng thật.
  - Cần mở cổng hay đổi firewall của VPS chính.
- **Trạng thái:** `Chưa làm`

### T1. Auth / `CurrentUser` stub

- **Mục tiêu:** có abstraction lấy người dùng hiện tại. Nhánh mặc định: stub **chỉ** profile `dev`, bị vô hiệu ở mọi profile khác. Chưa mở route nộp bài.
- **Mã liên quan:** SB-02 (tiền đề), NF-04 lớp 6.
- **Phải đọc trước:** `00` mục 6, 10; `05` mục 5 (`CurrentUser` stub); `04` mục 2, 10; `06` mục 10 (test stub); `01` (ADR-F về abstraction).
- **File ĐƯỢC chạm:** `backend/src/main/java/vn/edu/codelearn/platform/common/` hoặc package auth (đề xuất, theo `01`); `SecurityConfiguration` chỉ phần cần thiết; `backend/src/test/...` cho test profile.
- **KHÔNG chạm:** `V1`, route nộp bài, `content/`, `docs/api/openapi.yaml` (task này chưa có endpoint mới).
- **Các bước:**
  - [ ] Xác nhận cổng G1 với người (auth thật hay stub).
  - [ ] Nếu stub: định nghĩa interface lấy người dùng hiện tại (tên: đề xuất, theo `01`) và bean stub chỉ đăng ký ở profile `dev`.
  - [ ] Thêm cơ chế fail-fast: khởi động ở profile khác `dev` mà có stub thì ứng dụng không chạy (`05` mục 5).
  - [ ] Viết test: ở profile khác `dev` không có bean stub, request thiếu xác thực nhận 401.
  - [ ] Không thêm route mới ở whitelist.
- **Acceptance criteria:**
  - [ ] Stub chỉ hoạt động ở profile `dev`.
  - [ ] Có test khẳng định stub KHÔNG hoạt động ở profile khác.
  - [ ] Có test khẳng định fail-fast khi stub bị bật ngoài `dev`.
  - [ ] 6 endpoint GET hiện có vẫn hoạt động, `CatalogIntegrationTest` xanh.
  - [ ] Không thêm dependency.
- **Lệnh verify:** `./mvnw verify` (từ `backend`, với `TEST_DB_URL` khi cần test tích hợp).
- **DỪNG VÀ HỎI nếu:**
  - Nhóm chốt auth thật (JWT + role): task lớn hơn và cần dependency mới, hỏi trước.
  - Cần đổi `SecurityConfiguration` ngoài phần abstraction.
  - Cần quyết định cookie hay token (liên quan CSRF, SSE).
- **Trạng thái:** `Chưa làm`

### T2a. Migration và fixture SQL

- **Mục tiêu:** thêm bảng sandbox bằng migration mới và fixture SQL cho test. Chưa có endpoint.
- **Mã liên quan:** SB-02, SB-04, SB-05, SB-06, AD-03 (bảng cấu hình).
- **Phải đọc trước:** `02` mục 3, 4, 6, 8, 9; `06` mục 5 (fixture SQL), 10 (giá trị đánh dấu).
- **File ĐƯỢC chạm:** `backend/src/main/resources/db/migration/V3__create_exercise.sql`, `V4__create_submission.sql`, `V5__create_sandbox_limit_config.sql` (V2 đã tạo identity); `backend/src/main/resources/db/demo/R__seed_demo_exercises.sql` (chỉ `dev`); `backend/src/test/resources/sandbox-fixtures.sql`.
- **KHÔNG chạm:** `V1__create_catalog.sql`, `R__seed_demo_catalog.sql`, `backend/src/main/java`.
- **Các bước:**
  - [x] Xác nhận từ `V1` thực tế: `lesson.id` là `bigint`; không cần cột xuất bản mới trong migration này.
  - [x] Viết `V3` (exercise, lời giải tham khảo, test case, view test công khai), `V4` (submission, kết quả theo test, index), `V5` (bảng config, không seed giá trị).
  - [x] Dùng `account.id` từ migration V2 hiện có làm kiểu và FK của `submission.user_id`.
  - [x] Viết seed `dev` cho bài Python tính tổng hai số.
  - [x] Viết fixture SQL theo `06` mục 5, gieo giá trị đánh dấu bí mật (`06` mục 10).
  - [x] Viết test tích hợp kiểm tra ràng buộc `CHECK`/`UNIQUE` (`06` mục 5).
- **Acceptance criteria:**
  - [x] Flyway chạy sạch trên PostgreSQL 16 trống.
  - [x] `V1` không đổi.
  - [x] Vi phạm `CHECK`/`UNIQUE` bị DB từ chối, có test.
  - [x] Không có giá trị mặc định giới hạn trong migration.
  - [x] Seed chỉ chạy ở profile `dev`.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`. Chạy dev: `bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run` (từ `backend`, sau khi khởi động PostgreSQL bằng Docker Compose).
- **DỪNG VÀ HỎI nếu:**
  - Không xác định được khóa chính/cột xuất bản của `V1`.
  - `02` mâu thuẫn với `03`/`04` về cột hoặc trạng thái.
  - Cần thay đổi ngoài schema T2a hoặc thêm dependency.
- **Trạng thái:** `Xong (2026-11-25, nhánh feat/sandbox-coding-p1)`

### T2b. Exercise read API

- **Mục tiêu:** `GET` Exercise kèm test công khai, 404 cho nội dung chưa xuất bản.
- **Mã liên quan:** SB-02, SB-04.
- **Phải đọc trước:** `04` mục 3, 4.1, 7, 8, 10; `02` mục 4.3, 7; `06` mục 5, 10.
- **File ĐƯỢC chạm:** `docs/api/openapi.yaml`; package `sandbox` hoặc `submission` (đề xuất, theo `01`): controller, service, repository, `*Views`; `SecurityConfiguration` chỉ để mở đúng route Exercise; test + fixture.
- **KHÔNG chạm:** 6 endpoint hiện có, `content/` (trừ khi `01` yêu cầu), cột/bảng của T2a.
- **Các bước:**
- [x] Sửa `docs/api/openapi.yaml` trước (chỉ thêm).
- [x] Viết test: exercise xuất bản (200), lesson nháp, course nháp, lesson xuất bản thuộc course nháp (404).
- [x] Truy vấn đọc qua view test công khai, join `lesson`/`course` theo điều kiện xuất bản của `V1`. Không `SELECT *`.
- [x] DTO record trong `*Views`, không có trường cấm (`04` mục 8).
- [x] Mở route công khai theo quyết định G6, không hơn.
- [x] Test quét giá trị đánh dấu: không có `expected_output`/`stdin` test ẩn, lời giải.
- **Acceptance criteria:**
- [x] 200 chỉ chứa test công khai.
- [x] 404 (`EXERCISE_NOT_FOUND`) cho cả ba trường hợp nháp.
- [x] Test rò rỉ bằng giá trị đánh dấu xanh.
- [x] OpenAPI khớp code, endpoint cũ không đổi.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`.
- **DỪNG VÀ HỎI nếu:**
  - Cần nới `SecurityConfiguration` ngoài route Exercise.
- **Quyết định áp dụng:** GET Exercise công khai; response không trả số lượng test ẩn.
- **Trạng thái:** `Xong (2026-11-25, nhánh feat/sandbox-coding-p1; chưa commit)`

### T3a. Nhận bài, giới hạn kích thước/đang chờ, hint ladder

- **Mục tiêu:** `POST` nộp bài lưu `QUEUED`, trả `submission_id` ngay; kiểm tra quyền, kích thước mã, số bài chờ; cung cấp contract truy vấn hint ladder. Chưa đẩy job (T3b).
- **Mã liên quan:** SB-02, SB-05, SB-06, UC10 (pha 1).
- **Phải đọc trước:** `04` mục 4.2, 4.4, 4.5, 4.6, 7, 9, 10; `02` mục 4.4, 5, 6; `05` mục 5; `06` mục 5, 10.
- **File ĐƯỢC chạm:** `docs/api/openapi.yaml`; package `submission` (đề xuất): controller, service, repository, `*Views`; `SecurityConfiguration` chỉ mở route POST và các GET submission ở `04` mục 10; test + fixture.
- **KHÔNG chạm:** Exercise API đã làm, `V1`, gọi Judge0.
- **Các bước:**
  - [ ] Sửa `docs/api/openapi.yaml` trước.
  - [ ] Test: nộp hợp lệ (202, `QUEUED`), chưa đăng nhập (401), exercise nháp (404), ngôn ngữ lạ (400), mã quá lớn (413).
  - [ ] Đọc giới hạn từ bảng config; thiếu key thì lỗi cấu hình rõ ràng, không giá trị ngầm.
  - [ ] Kiểm tra kích thước trước khi lưu.
  - [ ] Lưu `submission`, trả 202.
  - [ ] Lịch sử phân trang (≤ 50), xem lại, quyền sở hữu (`user_id = :currentUser` trong truy vấn).
  - [ ] Service hint ladder `failedCount`, `hasAccepted`; test bài `FAILED`/`QUEUED`/`RUNNING` không tính.
- **Acceptance criteria:**
  - [ ] Mọi mã lỗi ở `04` mục 7 thuộc task này đúng `code`, HTTP.
  - [ ] User B không đọc được submission của A (theo quyết định 404/403 đã chốt).
  - [ ] Lịch sử không có `source_code`, `size` không vượt 50.
  - [ ] Hint ladder đúng ngữ nghĩa `04` mục 9, có test.
  - [ ] Không route nào ngoài `04` mục 10 được mở.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`.
- **DỪNG VÀ HỎI nếu:**
  - Chưa chốt 404 hay 403, kiểu `user_id`, hoặc idempotency khi nộp trùng.
  - Cần FK `user_id` khi chưa có bảng người dùng.
  - Cần đổi `SecurityConfiguration` ngoài route đã nêu.
- **Trạng thái:** `Chưa làm`

### T3b. Rate limit và queue (Redis)

- **Mục tiêu:** đẩy job vào hàng đợi, áp rate limit tần suất nộp từ bảng config. Hết hạn mức: 429.
- **Mã liên quan:** SB-06, NF-02, NF-04 lớp 5, N4 kịch bản 8, UC10 (pha 1).
- **Phải đọc trước:** `01` (queue, rate limit, ADR Redis); `05` mục 3 (lớp 5), 7; `04` mục 7; `02` mục 5; `06` mục 10, 11.
- **File ĐƯỢC chạm:** package `submission`/`sandbox` (đề xuất); chỉ **sau khi được duyệt:** `infra/docker-compose.dev.yml`, `.env.example`, `pom.xml`, `.github/workflows/backend-ci.yml`.
- **KHÔNG chạm:** `V1`, logic chấm, Judge0.
- **Các bước:**
  - [ ] **DỪNG VÀ HỎI** trước khi làm bất cứ gì cần Redis (G2).
  - [ ] Sau khi duyệt: thêm Redis vào compose dev và client Redis theo đúng lựa chọn được duyệt.
  - [ ] Rate limit và số bài chờ mỗi user, giá trị từ bảng config.
  - [ ] Đẩy job chỉ gồm id (đề xuất, không mã nguồn, không `expected_output`).
  - [ ] Test: vượt tần suất (429 `RATE_LIMITED`), vượt số bài chờ (429 `TOO_MANY_PENDING_SUBMISSIONS`), người khác không bị ảnh hưởng.
- **Acceptance criteria:**
  - [ ] Hai mã 429 phân biệt bằng `code`.
  - [ ] Không có giá trị giới hạn hard-code.
  - [ ] Mất job Redis thì bài vẫn `QUEUED` trong PG.
  - [ ] Redis không chứa dữ liệu bí mật.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`. Khởi động Redis ở dev/CI: `TODO(xác nhận)`.
- **DỪNG VÀ HỎI nếu:**
  - Cần thêm Redis, dependency hay service CI (luôn là có).
  - Nhóm định bỏ Redis dùng PG `SKIP LOCKED` (lệch đặc tả, phải ghi vào `01`).
  - Chưa chốt cách test Redis trong CI (`06` mục 11).
- **Trạng thái:** `Chưa làm`

### T4a. Judge0 adapter và Judge0 stub

- **Mục tiêu:** Judge0 client sau một interface, adapter theo kết quả T0, stub cho CI mô phỏng đủ hành vi.
- **Mã liên quan:** SB-02, SB-03, NF-04 lớp 6.
- **Phải đọc trước:** `03` mục 2, 3, 4, 8; `06` mục 6; `05` mục 3 (lớp 6), 6; `01` (ADR adapter).
- **File ĐƯỢC chạm:** package `sandbox` (đề xuất): interface, adapter, stub; test.
- **KHÔNG chạm:** `callback_url` (không dùng), worker, `V1`, route HTTP.
- **Các bước:**
  - [ ] Dùng kết quả T0 để điền tên status, trường JSON, poll hay `wait=true` (G3).
  - [ ] Định nghĩa interface Judge0 client; chỉ một implementation thật.
  - [ ] Adapter không gửi `expected_output`, chỉ mã, stdin, giới hạn từ config.
  - [ ] Stub theo `06` mục 6: mỗi status, down, timeout, 5xx, JSON hỏng, status lạ, chậm; ghi lại request.
  - [ ] Test: request không chứa `expected_output`; status lạ thành lỗi hệ thống.
- **Acceptance criteria:**
  - [ ] Mọi status trong bảng ánh xạ `03` mục 4 mô phỏng được.
  - [ ] Test khẳng định request không chứa `expected_output`.
  - [ ] Không dùng `callback_url`, không có URL/token Judge0 trong mã hay log.
  - [ ] Không thêm dependency (hoặc đã được duyệt).
- **Lệnh verify:** `./mvnw verify`.
- **DỪNG VÀ HỎI nếu:**
  - T0 chưa xong hoặc `03` còn `TODO` chặn cách ánh xạ.
  - Cần thư viện HTTP/mock mới.
  - Kết quả T0 dẫn tới phương án dự phòng.
- **Trạng thái:** `Chưa làm`

### T4b. Worker và tổng hợp verdict/điểm

- **Mục tiêu:** worker lấy job, đọc test, chấm tuần tự, tổng hợp verdict/điểm theo `03`, ghi kết quả.
- **Mã liên quan:** SB-02, SB-03, SB-04, UC10 (pha 2, 3), NF-01.
- **Phải đọc trước:** `03` mục 3, 5, 6, 9; `02` mục 4.5, 5, 6; `01` (ADR-E đóng gói worker); `06` mục 4, 6.
- **File ĐƯỢC chạm:** package `sandbox` (đề xuất): worker, chuẩn hoá, tính điểm, tổng hợp; test unit và tích hợp với stub.
- **KHÔNG chạm:** `SecurityConfiguration`, `V1`, `content/`.
- **Các bước:**
  - [ ] Chốt cách đóng gói worker (G4) trước.
  - [ ] Unit test theo bảng `06` mục 4, rồi code `normalize`, điểm, ưu tiên verdict.
  - [ ] Worker chuyển `QUEUED` → `RUNNING` bằng `UPDATE` có điều kiện (`02` mục 5).
  - [ ] Chấm tuần tự, Compile Error dừng ngay, ghi kết quả một transaction.
  - [ ] Không ghi `actual_output` cho test ẩn; chuẩn hoá `error_message` (`03` mục 9).
  - [ ] Log theo `05` mục 6.
- **Acceptance criteria:**
  - [ ] Đúng 6 verdict, không verdict thứ bảy.
  - [ ] Mỗi quy tắc ở `06` mục 4 có ít nhất một ca xanh.
  - [ ] Test luồng 3 pha với stub xanh (`06` mục 6).
  - [ ] Hai worker nhận cùng bài: chỉ một bên chạy.
  - [ ] Quét giá trị đánh dấu: không rò rỉ vào response và log.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`.
- **DỪNG VÀ HỎI nếu:**
  - Chưa chốt đóng gói worker, hoặc `03` mâu thuẫn `02` về trạng thái.
  - Cần dependency mới (scheduler, queue).
  - Phát hiện quy tắc `03` không kiểm chứng được bằng test.
- **Trạng thái:** `Chưa làm`

### T4c. Retry và job quét bài kẹt

- **Mục tiêu:** retry có giới hạn khi lỗi hệ thống, job quét lại bài kẹt `QUEUED`/`RUNNING`.
- **Mã liên quan:** NF-02, UC10, SB-05 (hint ladder không tính lỗi hệ thống).
- **Phải đọc trước:** `03` mục 7; `02` mục 5, 6; `06` mục 6; `04` mục 6, 9.
- **File ĐƯỢC chạm:** package `sandbox` (đề xuất); test.
- **KHÔNG chạm:** `V1`, quy tắc điểm/verdict của T4b.
- **Các bước:**
  - [ ] Retry: số lần, backoff từ cấu hình (giá trị `TODO` ở `03`/`02`).
  - [ ] Lỗi hệ thống: hủy lượt, chấm lại từ đầu; hết lượt thì `FAILED` (tên theo `02`).
  - [ ] Job quét: truy vấn `02` mục 6, ngưỡng từ cấu hình.
  - [ ] Test bằng stub: Judge0 down, timeout, Internal Error, 5xx; bài kẹt; mất job.
- **Acceptance criteria:**
  - [ ] Lỗi hệ thống không ra verdict, `FAILED` có `verdict` và `score` NULL.
  - [ ] Bài `FAILED` không tính vào `failedCount`.
  - [ ] Bài kẹt được nhặt lại, không mất bài.
  - [ ] Không hard-code số retry/ngưỡng.
- **Lệnh verify:** `./mvnw verify` với `TEST_DB_URL`.
- **DỪNG VÀ HỎI nếu:**
  - Số retry/backoff/ngưỡng kẹt chưa được chốt.
  - Cần cho worker trả bài về `QUEUED` (xung đột `02` mục 5; xem `03` mục 11).
  - Cần scheduler/dependency mới.
- **Trạng thái:** `Chưa làm`

### T5. SSE và polling

- **Mục tiêu:** endpoint polling trạng thái, SSE và fallback theo contract.
- **Mã liên quan:** UC10 (pha 3), SB-02, NF-01.
- **Phải đọc trước:** `04` mục 4.3, 5, 6, 10; `01` (cách worker báo hoàn tất); `05` mục 5.
- **File ĐƯỢC chạm:** `docs/api/openapi.yaml`; package `submission` (đề xuất); `SecurityConfiguration` chỉ route status/events; test.
- **KHÔNG chạm:** logic chấm, `V1`.
- **Các bước:**
  - [ ] **DỪNG VÀ HỎI** về xác thực SSE (G5) nếu chưa chốt. Không tự chọn.
  - [ ] Sửa OpenAPI trước.
  - [ ] Polling `GET` trạng thái trước (không phụ thuộc G5).
  - [ ] SSE: kiểm tra quyền và 404 trước khi mở stream; gửi `result` rồi đóng; kết nối khi đã xong thì gửi ngay.
  - [ ] Test quét payload SSE không có trường cấm.
- **Acceptance criteria:**
  - [ ] Polling và SSE cùng trả `SubmissionView`, `verdict` null khi chưa `DONE`.
  - [ ] User khác không theo dõi được submission của người khác.
  - [ ] Payload không có `source_code` hay dữ liệu test ẩn.
  - [ ] Token không nằm trong query string trừ khi nhóm chốt.
- **Lệnh verify:** `./mvnw verify`.
- **DỪNG VÀ HỎI nếu:**
  - Xác thực EventSource chưa chốt.
  - Cần đổi CORS/CSRF.
  - Cần cách phát sự kiện giữa worker và backend khác `01`.
- **Trạng thái:** `Chưa làm`

### T6. Test bảo mật và tải

- **Mục tiêu:** nghiệm thu N2, N4, NF-02, NF-01, không lộ bí mật; điền bảng bằng chứng.
- **Mã liên quan:** N2, N4, NF-01, NF-02, NF-04, M5.
- **Phải đọc trước:** `06` mục 7, 8, 9, 10, 12; `05` mục 4, 10; `03` mục 10; `00` mục 7.
- **File ĐƯỢC chạm:** `docs/sandbox/fixtures/`, `docs/sandbox/evidence/` (đề xuất); test; tài liệu bằng chứng.
- **KHÔNG chạm:** quy tắc chấm ở `03` (nếu sai thì báo, không tự sửa).
- **Các bước:**
  - [ ] Quét rò rỉ mọi endpoint sandbox (`06` mục 10).
  - [ ] Chạy bộ ≥ 30 ca N2 trên Judge0 thật (môi trường test).
  - [ ] Chạy 8 kịch bản N4 theo quy tắc an toàn `06` mục 8.
  - [ ] Tải 30 đồng thời (NF-02), đo NF-01.
  - [ ] Điền bảng bằng chứng `05` mục 10 và `06` mục 12.
- **Acceptance criteria:**
  - [ ] N2: khớp verdict/điểm theo tiêu chí đã chốt (`06` mục 7).
  - [ ] N4: 8/8 đạt kỳ vọng.
  - [ ] NF-02: không mất bài; NF-01 theo cách đo đã chốt.
  - [ ] Không có chuỗi đánh dấu bí mật trong response/log.
- **Lệnh verify:** `./mvnw verify` cho phần CI. Chạy N2/N4 trên Judge0 thật, test tải: `TODO(xác nhận)`.
- **DỪNG VÀ HỎI nếu:**
  - Cần công cụ tải hoặc dependency mới.
  - Môi trường test không cô lập khỏi hạ tầng thật.
  - Kịch bản tấn công làm VPS Sandbox quá tải: dừng khẩn cấp theo `06` mục 8.
  - Kết quả lệch `03` (báo người, không sửa test cho xanh).
- **Trạng thái:** `Chưa làm`

### T7. Hoàn tất

- **Mục tiêu:** rà OpenAPI khớp contract và code, cập nhật `PROJECT_STATUS.md`, chạy `./mvnw verify`.
- **Mã liên quan:** toàn bộ phạm vi `00` mục 3, 7.
- **Phải đọc trước:** `00` mục 7; `04` mục 3, 10; `06` mục 12; Definition of Done ở `AGENTS.md`.
- **File ĐƯỢC chạm:** `docs/api/openapi.yaml`; `PROJECT_STATUS.md` (vị trí: `TODO(xác nhận)`); file này (trạng thái).
- **KHÔNG chạm:** mã nghiệp vụ (nếu lỗi thì báo, tạo task sửa).
- **Các bước:**
  - [ ] Đối chiếu `openapi.yaml` với `04` và controller thực tế; chỉ thêm, không phá 6 endpoint cũ.
  - [ ] Cập nhật `PROJECT_STATUS.md`.
  - [ ] Chạy `./mvnw verify`.
  - [ ] Rà Definition of Done ở `AGENTS.md` và bảng bằng chứng đã đủ.
- **Acceptance criteria:**
  - [ ] OpenAPI khớp code, không phá endpoint cũ.
  - [ ] `./mvnw verify` xanh.
  - [ ] Mọi task T0–T6 đã đánh dấu xong hoặc có ghi chú.
- **Lệnh verify:** `./mvnw verify`.
- **DỪNG VÀ HỎI nếu:**
  - OpenAPI và code lệch mà không rõ bên nào đúng.
  - Không xác định được vị trí `PROJECT_STATUS.md`.
  - Có task chưa xong nhưng bị yêu cầu tuyên bố hoàn tất.
- **Trạng thái:** `Chưa làm`

## 5. Cổng quyết định chưa chốt

| Mã | Quyết định | Task bị ảnh hưởng | Ai quyết | Ghi vào đâu sau khi chốt |
|---|---|---|---|---|
| G1 | Auth thật (JWT + role) hay chỉ `CurrentUser` stub ở `dev`; cookie hay token | T1, T3a, T5 | Nhóm | `00` mục 10; `04` mục 2; `05` mục 5 |
| G2 | Đưa Redis vào compose, CI, client; hoặc bỏ Redis dùng PG `SKIP LOCKED` (lệch đặc tả) | T3b, T4b, T5, T6 | Nhóm duyệt | `01`; `06` mục 11 |
| G3 | Poll hay `wait=true` khi gọi Judge0 | T0, T4a | Nhóm, dựa kết quả T0 | `03` mục 3, 11; `01` |
| G4 | Worker đóng gói: cùng tiến trình hay tách | T4b, T4c | Nhóm | `01` (ADR-E) |
| G5 | Xác thực cho SSE/EventSource | T5 | Nhóm, cùng G1 | `04` mục 5, 11 |
| G6 | Có endpoint admin sửa config giới hạn không; GET Exercise công khai hay xác thực | T2b, T3a | Nhóm | `04` mục 4.7, 10; `00` mục 10 |
| G7 | Giữ Judge0 hay kích hoạt dự phòng (Judge0 dịch vụ / Piston) | T0 và mọi task sau | Nhóm | `05` mục 8; rà lại `01`, `03`, `05` |

- Nếu G6 chưa chốt: chỉ làm bảng cấu hình và đọc từ bảng, không endpoint admin.

## 6. Quy ước cập nhật file này

- Đánh dấu xong (đề xuất): `Trạng thái: Xong (YYYY-MM-DD, PR/commit <tham chiếu>)`, và tick các checkbox acceptance.
- Làm dở: `Đang làm (ghi chú còn thiếu gì)`. Bị chặn: `Chặn bởi G<n>`.
- Chỉ đánh dấu xong khi mọi acceptance criteria đạt và `./mvnw verify` xanh.
- Cập nhật `PROJECT_STATUS.md` khi xong một giai đoạn (T0, T2, T4, T5, T7). Vị trí: `TODO(xác nhận)`.
- Tách hoặc thêm task: **phải hỏi người**. Không thêm task ngoài phạm vi `00`.
- Tên package/class/migration trong file này là đề xuất. Khi chốt ở `01`, `02`, `04`, cập nhật lại đây.
- Task phát sinh việc ngoài phạm vi (SB-01, SB-07 đến SB-10, ngôn ngữ ngoài Python, frontend, trợ lý AI, staging/production): dừng và hỏi.

## 7. Điểm cần xác nhận

- `TODO(xác nhận):` vị trí và nội dung `PROJECT_STATUS.md`.
- `TODO(xác nhận):` lệnh chạy test Judge0 thật (N2, N4) và test tải.
- `TODO(xác nhận):` cách chạy Redis ở dev và CI.
- `TODO(xác nhận):` auth thật hay chỉ stub (G1).
- `TODO(xác nhận):` poll hay `wait=true` (G3).
- `TODO(xác nhận):` worker đóng gói thế nào (G4).
- `TODO(xác nhận):` có endpoint admin config không (G6).
- `TODO(xác nhận):` nơi lưu báo cáo spike và bằng chứng nghiệm thu.
- `TODO(xác nhận):` tên package, class, migration cuối cùng (theo `01`, `02`, `04`).
- `TODO(xác nhận):` có tách thêm task nào không khi thực tế quá lớn cho một phiên.
