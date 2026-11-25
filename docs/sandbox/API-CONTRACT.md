# 04-API-CONTRACT: Hợp đồng API Coding + Sandbox chấm bài

## 1. Mục đích và phạm vi

- Đây là **contract đề xuất**. Chưa có mã cho Exercise API, nộp bài, SSE hay auth. Chỉ 6 endpoint GET ở mục 3 là ĐÃ CÓ.
- Contract-first: sửa `docs/api/openapi.yaml` → kiểm tra không phá endpoint cũ → code → test → `./mvnw verify`. Chỉ thêm, không phá.
- Phạm vi: SB-02 đến SB-06, UC10, phần backend của AD-03. Ngoài phạm vi (SB-01, SB-07 đến SB-10, ngôn ngữ ngoài Python, frontend, endpoint AI/chat): xem `00-SCOPE.md`.
- Không lặp: cột bảng và vòng đời trạng thái → `02-DATA-MODEL.md`; ánh xạ verdict, tính điểm, retry → `03-GRADING-SPEC.md`; kiến trúc, SSE/Redis bên trong → `01-ARCHITECTURE.md`; checklist bảo mật → `05-SECURITY.md`.

## 2. Quy ước chung

| Quy ước | Nội dung | Trạng thái |
|---|---|---|
| Prefix | Endpoint mới đều ở `/api/v1/...` | ĐÃ CÓ (convention) |
| Lỗi | `application/problem+json`, có `code` ổn định và `traceId`; mọi lỗi mới đi qua `ApiExceptionHandler` | ĐÃ CÓ |
| Request id | Mọi request có `X-Request-Id` | ĐÃ CÓ |
| Phân trang | Tham số `page`, `size`, `size` tối đa 50 | ĐÃ CÓ (catalog) |
| DTO | Record trong file `*Views` (package `submission`, `sandbox`) | ĐÃ CÓ (convention) |
| Nội dung xuất bản | Exercise của lesson nháp hoặc course nháp trả 404, như catalog | ĐÃ CÓ (quy tắc) |
| Thời gian | ISO 8601 UTC, ví dụ `2026-01-01T00:00:00Z` | (đề xuất) |
| Đặt tên trường JSON | camelCase, khớp API/OpenAPI hiện có | ĐÃ CÓ |
| Xác thực | Identity phiên đã có; auth/phân quyền cho submission chưa có. Submission yêu cầu đăng nhập | ĐÃ CÓ một phần |
| Dev | `CurrentUser` stub, chỉ ở profile `dev` | Quyết định đã chốt (`00-SCOPE.md`) |
| Quyền sở hữu | Học viên chỉ xem lịch sử và mã nguồn của chính mình | Quyết định đã chốt |

## 3. Bảng tổng hợp endpoint

| Method | Path | Mô tả | Xác thực | Vai trò tối thiểu | Trạng thái |
|---|---|---|---|---|---|
| GET | `/api/v1/health` | Health ứng dụng | Không | Không | ĐÃ CÓ |
| GET | `/actuator/health/liveness` | Liveness | Không | Không | ĐÃ CÓ |
| GET | `/actuator/health/readiness` | Readiness | Không | Không | ĐÃ CÓ |
| GET | `/api/v1/courses?page=0&size=20` | Danh sách khóa (sắp theo slug, size ≤ 50) | Không | Không | ĐÃ CÓ |
| GET | `/api/v1/courses/{courseSlug}` | Chi tiết khóa | Không | Không | ĐÃ CÓ |
| GET | `/api/v1/courses/{courseSlug}/lessons/{lessonSlug}` | Chi tiết bài học | Không | Không | ĐÃ CÓ |
| GET | `/api/v1/exercises/{exerciseId}` | Xem Exercise + test công khai | Không | Không | ĐÃ CÓ (T2b) |
| POST | `/api/v1/exercises/{exerciseId}/submissions` | Nộp bài | Có | Học viên (đề xuất) | ĐỀ XUẤT |
| GET | `/api/v1/submissions/{submissionId}` | Trạng thái/kết quả một submission (polling) | Có | Chủ submission | ĐỀ XUẤT |
| GET | `/api/v1/submissions/{submissionId}/events` | SSE theo dõi kết quả | Có (`TODO`) | Chủ submission | ĐỀ XUẤT |
| GET | `/api/v1/exercises/{exerciseId}/submissions?page=0&size=20` | Lịch sử nộp theo bài tập | Có | Chủ | ĐỀ XUẤT |
| GET | `/api/v1/submissions/{submissionId}/detail` | Xem lại: mã nguồn + kết quả từng test | Có | Chủ | ĐỀ XUẤT |
| GET/PUT | `/api/v1/admin/sandbox-limits` | Đọc/sửa config giới hạn (AD-03) | Có | Quản trị | ĐỀ XUẤT, `TODO(xác nhận)` có làm không |

- Bảng exercise: `{exerciseId}` là id `bigint` của `exercise` (`02-DATA-MODEL.md`).
- Đường dẫn mới đều là đề xuất. Có thể gộp `/detail` vào `GET /api/v1/submissions/{submissionId}` nếu nhóm muốn (đề xuất, `TODO`).

## 4. Đặc tả endpoint

### 4.1 `GET /api/v1/exercises/{exerciseId}` (ĐÃ CÓ)

- Path: `exerciseId`. Không có query.
- 200:

```json
{
  "id": 1,
  "lessonId": 10,
  "title": "Tính tổng hai số",
  "language": "python",
  "difficulty": "easy",
  "statementMarkdown": "Nhập hai số, in tổng.",
  "starterCode": "a = int(input())\n",
  "maxScore": 100,
  "publicTests": [
    { "position": 1, "stdin": "1 2\n", "expectedOutput": "3\n" }
  ]
}
```

- Chỉ test `is_public = true` có `stdin`, `expectedOutput`. Test ẩn không có trong `publicTests`.
- Endpoint công khai. Không trả số lượng test ẩn, trọng số hoặc `position` của test ẩn.
- Giá trị `difficulty`: theo `02-DATA-MODEL.md`.
- Lỗi: `EXERCISE_NOT_FOUND` (404, kể cả exercise của lesson/course nháp).

### 4.2 `POST /api/v1/exercises/{exerciseId}/submissions`

- Body:

```json
{ "language": "python", "source_code": "print(sum(map(int, input().split())))" }
```

- Thứ tự kiểm tra (đề xuất): xác thực → exercise tồn tại và đã xuất bản → `language` hỗ trợ → kích thước mã → rate limit → số bài đang chờ tối đa.
- Giá trị giới hạn lấy từ bảng config, không điền số (`02-DATA-MODEL.md`).
- **202 Accepted** (non-blocking):

```json
{ "submission_id": 123, "status": "QUEUED", "verdict": null, "created_at": "2026-01-01T00:00:00Z" }
```

- Header `Location: /api/v1/submissions/123` (đề xuất).
- Lỗi: `AUTH_REQUIRED` (401), `FORBIDDEN` (403), `EXERCISE_NOT_FOUND` (404), `LANGUAGE_NOT_SUPPORTED` (400), `SOURCE_CODE_TOO_LARGE` (413), `RATE_LIMITED` (429), `TOO_MANY_PENDING_SUBMISSIONS` (429), `GRADER_UNAVAILABLE` (503).
- **Nộp trùng (double click):** báo cáo chưa nêu idempotency.
  - Đề xuất MVP: không thêm cơ chế riêng, dựa vào giới hạn số bài đang chờ và rate limit.
  - Hệ quả: hai lần bấm có thể ra hai submission.
  - Hướng thay thế: header `Idempotency-Key` (đề xuất, chưa làm). `TODO(xác nhận)`.

### 4.3 `GET /api/v1/submissions/{submissionId}` (polling)

- 200 là `SubmissionView` ở mục 6:

```json
{
  "submission_id": 123,
  "exercise_id": 1,
  "status": "DONE",
  "verdict": "WRONG_ANSWER",
  "score": 50.00,
  "max_score": 100,
  "time_ms": 120,
  "memory_kb": 9000,
  "error_message": null,
  "created_at": "2026-01-01T00:00:00Z",
  "finished_at": "2026-01-01T00:00:03Z",
  "tests": [
    { "position": 1, "public": true, "passed": true, "verdict": "ACCEPTED", "time_ms": 20, "memory_kb": 8800,
      "stdin": "1 2\n", "expected_output": "3\n", "actual_output": "3\n" },
    { "position": 2, "public": false, "passed": false, "verdict": "WRONG_ANSWER", "time_ms": 30, "memory_kb": 9000 }
  ]
}
```

- `tests` có mặt khi `status = DONE`. Khi `QUEUED`/`RUNNING` thì rỗng hoặc vắng (đề xuất).
- Test ẩn chỉ có `position`, `public: false`, `passed`, `verdict`, `time_ms`, `memory_kb` (đề xuất, `TODO` mục 11; 03 mục 9 đang đề xuất hiện time/memory của test ẩn).
- Không có `source_code` ở endpoint này. Mã nguồn chỉ có ở 4.6.
- Lỗi: `AUTH_REQUIRED` (401), `SUBMISSION_NOT_FOUND` (404).

### 4.4 `GET /api/v1/exercises/{exerciseId}/submissions?page=0&size=20`

- Query: `page` (từ 0), `size` (tối đa 50, kẹp ở tầng repository, theo `02-DATA-MODEL.md` mục 6).
- Chỉ trả submission của user hiện tại, mới nhất trước. Không có `source_code`, không có `tests`.
- 200:

```json
{
  "items": [
    { "submission_id": 123, "status": "DONE", "verdict": "ACCEPTED", "score": 100.00,
      "time_ms": 120, "memory_kb": 9000, "created_at": "2026-01-01T00:00:00Z" }
  ],
  "page": 0, "size": 20
}
```

- Dạng phân trang cuối (offset hay cursor): theo catalog hiện có hoặc keyset ở 02. `TODO(xác nhận)`. Nếu keyset thì `page` thay bằng `cursor` (đề xuất).
- Lỗi: `AUTH_REQUIRED` (401), `EXERCISE_NOT_FOUND` (404), `VALIDATION_ERROR` (400, `size` > 50 hoặc `page` < 0; hoặc kẹp, theo catalog).

### 4.5 Quyền sở hữu: submission của người khác

- **Đề xuất: trả 404** (`SUBMISSION_NOT_FOUND`), không phân biệt "không tồn tại" và "không phải của bạn".
- Lý do: không lộ sự tồn tại của submission, không cho dò id tuần tự. Cùng cách với 404 cho nội dung chưa xuất bản.
- Đánh đổi: khó phân biệt lỗi khi debug, dùng `traceId`. `TODO(xác nhận)`: 404 hay 403.

### 4.6 `GET /api/v1/submissions/{submissionId}/detail` (xem lại)

- 200 là `SubmissionView` ở 4.3 cộng thêm `source_code` và `language`. Chỉ chủ submission.

```json
{ "submission_id": 123, "language": "python", "source_code": "print(1)", "...": "như 4.3" }
```

- Lỗi: `AUTH_REQUIRED`, `SUBMISSION_NOT_FOUND`.
- `source_code` không được log, không cache dùng chung (đề xuất, `05-SECURITY.md`).

### 4.7 Admin config (nếu có): `GET/PUT /api/v1/admin/sandbox-limits`

- **Chưa chốt có làm.** Nếu làm: cần quyền Quản trị (403 với vai trò khác), thay đổi theo key trong `sandbox_limit_config` (`02-DATA-MODEL.md`).
- Request/response chi tiết chưa thiết kế. `TODO(xác nhận)`. Nếu không làm: chỉ đọc từ bảng, không endpoint.

## 5. SSE (theo dõi kết quả)

Toàn bộ mục này là đề xuất. Kênh chính: SSE. Fallback: polling `GET /api/v1/submissions/{submissionId}`. Cơ chế bên trong (Redis, phát sự kiện): `01-ARCHITECTURE.md`.

| Hạng mục | Đề xuất |
|---|---|
| Endpoint | `GET /api/v1/submissions/{submissionId}/events`, `Accept: text/event-stream` |
| Sự kiện `status` | Mỗi lần đổi `status` (`QUEUED`, `RUNNING`) |
| Sự kiện `result` | Khi `DONE` hoặc lỗi hệ thống; payload = `SubmissionView` (như 4.3, không `source_code`) |
| Heartbeat | Có, dạng comment SSE. Chu kỳ: `TODO(xác nhận)` |
| Đóng kết nối | Server đóng ngay sau `result` |
| Kết nối lúc submission đã xong | Gửi ngay `result` rồi đóng |
| Reconnect | Client tự nối lại (EventSource). Mỗi lần nối lại server gửi trạng thái hiện tại, nên không cần `Last-Event-ID`. Khoảng chờ: `TODO(xác nhận)` |
| Fallback | Lỗi liên tiếp hoặc không nối được thì client chuyển sang polling. Tần suất polling: `TODO(xác nhận)` |
| Lỗi | Kiểm tra quyền và 404 xảy ra **trước** khi mở stream, trả `problem+json` bình thường |

- Payload sự kiện tuân theo mục 8: không có trường cấm.
- **Vấn đề xác thực của EventSource:** `EventSource` của trình duyệt không gửi được header tùy chỉnh (như `Authorization`).
  - Hướng khả thi: cookie phiên, hoặc thư viện fetch-based SSE, hoặc vé ngắn hạn.
  - Không tự quyết. Phụ thuộc cơ chế auth. `TODO(xác nhận)`.
  - Không đưa token vào query string (lộ trong log), trừ khi nhóm chốt kèm biện pháp (đề xuất).

## 6. Biểu diễn trạng thái và verdict

Hai trường tách biệt. Giá trị trạng thái theo `02-DATA-MODEL.md` mục 5; nếu 02 đổi thì theo 02.

| Trường | Giá trị | Ghi chú |
|---|---|---|
| `status` | `QUEUED`, `RUNNING`, `DONE`, `FAILED` | `FAILED` = lỗi hệ thống (đề xuất tên) |
| `verdict` | `ACCEPTED`, `WRONG_ANSWER`, `TIME_LIMIT`, `MEMORY_LIMIT`, `RUNTIME_ERROR`, `COMPILE_ERROR`, hoặc `null` | Đúng 6 verdict. Dạng chuỗi theo 02 |

| `status` | `verdict` | `score` | Ý nghĩa |
|---|---|---|---|
| `QUEUED`, `RUNNING` | `null` | `null` | Đang xử lý |
| `DONE` | khác `null` | số | Chấm xong |
| `FAILED` | `null` | `null` | Lỗi hệ thống. **Không phải verdict.** Có `error_message` chung |

- Verdict `null` khi và chỉ khi `status` khác `DONE`.
- Lỗi hệ thống: thông báo chung, khuyến khích nộp lại, không lộ chi tiết nội bộ (`03-GRADING-SPEC.md` mục 9).
- `score`: tổng trọng số test đạt / tổng trọng số × `max_score`. Làm tròn, Compile Error: xem 03 mục 6.
- `error_message`: chứa thông báo biên dịch (Compile Error) hoặc traceback test công khai. Quy tắc nội dung: 03 mục 9.
- Không đưa `attempt_count` ra response (đề xuất).

## 7. Bảng mã lỗi

Mọi `code` là **đề xuất**. Định dạng: `application/problem+json` có `code`, `traceId`.

| code | HTTP | Khi nào | Client nên làm |
|---|---|---|---|
| `AUTH_REQUIRED` | 401 | Chưa đăng nhập (kể cả khách) | Chuyển tới đăng nhập |
| `FORBIDDEN` | 403 | Sai vai trò (ví dụ không phải Quản trị) | Hiện thông báo không đủ quyền |
| `EXERCISE_NOT_FOUND` | 404 | Exercise không tồn tại, hoặc thuộc lesson/course nháp | Quay lại bài học |
| `SUBMISSION_NOT_FOUND` | 404 | Không tồn tại hoặc không phải của bạn | Quay lại danh sách |
| `LANGUAGE_NOT_SUPPORTED` | 400 | `language` không hỗ trợ (MVP: chỉ Python) | Sửa `language` |
| `VALIDATION_ERROR` | 400 | Body hỏng, thiếu trường, `page`/`size` sai | Sửa request |
| `SOURCE_CODE_TOO_LARGE` | 413 | Vượt giới hạn kích thước mã (từ config) | Rút gọn mã |
| `RATE_LIMITED` | 429 | Vượt tần suất nộp | Chờ rồi thử lại, theo `Retry-After` nếu có |
| `TOO_MANY_PENDING_SUBMISSIONS` | 429 | Vượt số bài đang chờ tối đa mỗi user | Chờ bài trước chấm xong |
| `GRADER_UNAVAILABLE` | 503 | Không nhận được bài (ví dụ hàng đợi không khả dụng) | Thử lại sau |
| `INTERNAL_ERROR` | 500 | Lỗi không lường trước | Thử lại, báo `traceId` |

- `Retry-After` cho 429: có hay không là `TODO(xác nhận)`. Nếu có thì theo giá trị từ config, không điền số trong tài liệu.
- Hai mã 429 cùng HTTP status, phân biệt bằng `code`.
- Lỗi hệ thống **sau** khi đã nhận bài không phải mã lỗi HTTP. Nó là `status = FAILED` ở mục 6.

## 8. Trường bị cấm xuất hiện trong response

Áp dụng cho mọi response, SSE, DTO và log.

| Dữ liệu | Lý do | Cách đảm bảo ở tầng DTO/truy vấn | Test cần có (`06-TEST-PLAN.md`) |
|---|---|---|---|
| `expected_output` của test ẩn | Lộ đáp án | API đọc test qua view `test_case_public` (`02` mục 4.3); `*Views` không có trường này cho test ẩn | Quét JSON response: không chứa giá trị `expected_output` của test ẩn |
| `stdin` của test ẩn | Suy ra đáp án (đề xuất) | Như trên; kết quả test ẩn chỉ có trường ở 4.3 | Như trên |
| Lời giải tham khảo (`exercise_reference_solution`) | Lộ đáp án | Repository API không `SELECT`/`JOIN` bảng này (`02` mục 7) | Fixture có lời giải với chuỗi đánh dấu; khẳng định không xuất hiện ở mọi endpoint |
| `actual_output` và stderr của test ẩn | Có thể in lại input ẩn | Không ghi `actual_output` cho test ẩn (`02` mục 4.5); `error_message` chỉ từ test công khai | Test ẩn fail kèm stderr chứa chuỗi đánh dấu: không lộ |
| `source_code` của người khác | Riêng tư | Truy vấn luôn có `user_id = :currentUser`; sai chủ trả 404 | Hai user: user B gọi submission của A nhận 404 |
| `source_code` trong danh sách lịch sử và SSE | Giảm lộ, nhẹ response | Truy vấn danh sách không chọn cột này (`02` mục 6) | Khẳng định không có trường `source_code` ở 4.3, 4.4, SSE |
| Thông tin nội bộ sandbox | Lộ hạ tầng | Chuẩn hoá/cắt thông báo lỗi, bỏ đường dẫn tuyệt đối (03 mục 9) | Mã gây lỗi in đường dẫn: response không chứa đường dẫn sandbox |
| Token, địa chỉ Judge0, credential | Lộ hạ tầng | Không có trong `*Views`; không log (`05-SECURITY.md`) | Quét response và log |
| `attempt_count`, `user_id` của người khác | Không cần cho client (đề xuất) | Không đưa vào `*Views` | Khẳng định không có trong response |

- Log: không ghi các dữ liệu trên. Quy tắc log chi tiết: `03-GRADING-SPEC.md` mục 7, `05-SECURITY.md`.

## 9. Contract nội bộ cho hint ladder

- **Không phải endpoint HTTP** trừ khi nhóm chốt. Là giao diện service giữa module `submission` và module AI (giai đoạn sau). Ta không viết logic gợi ý.
- Giao diện (đề xuất):

```java
record AttemptSummary(long failedCount, boolean hasAccepted) {}
interface SubmissionAttemptQuery {
    AttemptSummary summarize(UserId userId, ExerciseId exerciseId);
}
```

- Kiểu `UserId`, `ExerciseId`: `TODO(xác nhận)` (kiểu `user_id` chưa chốt, `02` mục 9).
- Ngữ nghĩa:
  - `failedCount`: số submission `status = DONE` và verdict khác `ACCEPTED`.
  - `hasAccepted`: có ít nhất một submission `ACCEPTED`.
  - Submission `FAILED` (lỗi hệ thống), `QUEUED`, `RUNNING` **không tính** vào `failedCount`.
- Truy vấn SQL và index: `02-DATA-MODEL.md` mục 6. Giao diện này không trả `source_code` hay dữ liệu test.
- Chưa có caller trong phạm vi này. Chỉ cần có sẵn và có test.

## 10. Thay đổi ở SecurityConfiguration và OpenAPI

Hiện tại: stateless, chỉ whitelist GET, route/method khác bị từ chối. Chỉ mở đúng route ở mục 3.

| Route | Mở thêm | Công khai hay xác thực |
|---|---|---|
| `GET /api/v1/exercises/{exerciseId}` | Không | ĐÃ CÓ (công khai) |
| `POST /api/v1/exercises/{exerciseId}/submissions` | Có (POST đầu tiên) | Xác thực |
| `GET /api/v1/submissions/{submissionId}` | Có | Xác thực |
| `GET /api/v1/submissions/{submissionId}/events` | Có | Xác thực (`TODO`, vấn đề EventSource, mục 5) |
| `GET /api/v1/submissions/{submissionId}/detail` | Có | Xác thực |
| `GET /api/v1/exercises/{exerciseId}/submissions` | Có | Xác thực |
| `/api/v1/admin/sandbox-limits` | Chỉ khi nhóm chốt làm | Xác thực + Quản trị |

- Ở profile `dev`, route cần xác thực dùng `CurrentUser` stub. Môi trường thật cần auth thật (`TODO`, `00-SCOPE.md`).
- CORS: giữ như hiện có. Nếu SSE/auth cần credentials thì cần xem lại, `TODO(xác nhận)`.
- OpenAPI: thêm path, schema `ExerciseView`, `SubmissionView`, `SubmissionListView`, `ProblemDetail` mới (mã lỗi mục 7). Không sửa 6 endpoint ĐÃ CÓ. Chi tiết bảo mật: `05-SECURITY.md`.

## 11. Điểm cần xác nhận

- `TODO(xác nhận):` cơ chế xác thực (header/token/cookie) và thời điểm có auth thật.
- `TODO(xác nhận):` xác thực cho SSE/EventSource, giải quyết cùng auth.
- `TODO(xác nhận):` 404 hay 403 cho submission của người khác (đề xuất: 404).
- `GET exercise` công khai đã được xác nhận; test ẩn và số lượng test ẩn không được trả.
- `TODO(xác nhận):` có endpoint admin config không, và chi tiết request/response nếu có.
- `TODO(xác nhận):` `Retry-After` cho 429.
- `TODO(xác nhận):` idempotency khi nộp trùng (đề xuất MVP: dựa vào giới hạn số bài đang chờ).
- `TODO(xác nhận):` có trả `time_ms`/`memory_kb` của test ẩn không (thống nhất với `03-GRADING-SPEC.md` mục 9).
- `TODO(xác nhận):` kiểu id (exercise, submission, user) và dạng phân trang cuối (offset hay keyset).
- `TODO(xác nhận):` chu kỳ heartbeat SSE, khoảng chờ reconnect, tần suất polling.
- `TODO(xác nhận):` tên `code` lỗi, đường dẫn endpoint submission và tên trường JSON cuối cùng; có gộp `/detail` vào endpoint submission không.
