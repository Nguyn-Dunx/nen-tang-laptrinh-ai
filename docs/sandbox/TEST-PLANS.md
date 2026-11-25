# 06-TEST-PLAN: Kế hoạch kiểm thử Coding + Sandbox chấm bài

## 1. Mục đích và phạm vi

- Đây là kế hoạch kiểm thử và trạng thái bằng chứng. Đã có unit test cho quy tắc chấm, HTTP stub cục bộ cho Judge0 adapter, integration test API/Redis/PostgreSQL; chưa có bằng chứng Judge0 thật, N2/N4 hay tải.
- Phạm vi: kiểm thử SB-02 đến SB-06, UC10, NF-01, NF-02, NF-04, N2, N4 và thiết kế `docs/sandbox/fixtures/`. Ngoài phạm vi (SB-01, SB-07 đến SB-10, ngôn ngữ ngoài Python, kiểm thử frontend/iframe, trợ lý AI, staging/production, giám sát): xem `00-SCOPE.md`.
- Ba tiêu chí nghiệm thu chính:
  - **N2** (M3, tuần 7–10): đúng verdict trên tối thiểu 30 bài nộp mẫu.
  - **N4** (M5, tuần 14–16): 8 kịch bản tấn công (Bảng 5.9) pass.
  - **NF-02** (M5): 30 bài nộp đồng thời (mục tiêu đề xuất trong báo cáo) không mất bài.
- Không lặp: ánh xạ verdict, chuẩn hoá output, điểm → `03-GRADING-SPEC.md`; cột bảng → `02-DATA-MODEL.md`; endpoint, DTO, mã lỗi → `04-API-CONTRACT.md`; 7 lớp, runbook → `05-SECURITY.md`; kiến trúc → `01-ARCHITECTURE.md`.
- Nhãn: "(đề xuất)" là chưa có trong báo cáo hay repo. `TODO(xác nhận)` là chưa chốt.

## 2. Hiện trạng test và nguyên tắc

**ĐÃ CÓ:**
- `backend/src/test/java/.../CatalogIntegrationTest.java`.
- `backend/src/test/resources/catalog-fixtures.sql`.
- Test tích hợp cần PostgreSQL, chỉ bật khi có biến `TEST_DB_URL`.
- Sandbox unit/integration test cho quy tắc chấm, Judge0 HTTP stub, giới hạn submission, quyền sở hữu và không rò rỉ hidden-test values.
- CI: `.github/workflows/backend-ci.yml` (Java 21, PostgreSQL 16, Redis; chạy `./mvnw verify`). CI **không** có Judge0 thật.

**CÒN THIẾU:** test tích hợp đầy đủ từ queue đến worker với stub; Judge0 thật, N2/N4, test tải và bằng chứng isolation.

**Bằng chứng gần nhất (2026-11-25):** `./mvnw --batch-mode --no-transfer-progress verify` trên DB test riêng: 35 test, 0 failure, 0 error, 0 skipped; Spotless và Checkstyle xanh. Compose config hợp lệ. Không chạy Judge0 thật hoặc mã người dùng trên Judge0.

**Nguyên tắc:**
- Không `@Disabled` test. Không sửa test cho xanh thay vì sửa lỗi thật.
- `./mvnw verify` phải xanh trước khi tuyên bố xong.
- CI không phụ thuộc Judge0 thật. Redis được cung cấp trong CI; phần Judge0 thật/tải chỉ chạy trong môi trường cô lập.
- Test không tự quy định lại quy tắc của `03`. Test bám theo `03`. Nếu `03` đổi thì sửa test theo.
- Mỗi mục kiểm thử có: đầu vào, kết quả mong đợi, cách chạy, nơi lưu bằng chứng.
- **Fixture tấn công chỉ chạy trên Judge0 của môi trường test/spike.** Tuyệt đối không chạy trên production hay máy chính.
- Không thêm dependency hay service CI khi chưa hỏi nhóm.

## 3. Ma trận kiểm thử (tất cả đề xuất)

| Tầng | Phạm vi | PG / Redis / Judge0 | Chạy ở đâu |
|---|---|---|---|
| Unit | Logic chấm: ánh xạ status, chuẩn hoá, điểm, ưu tiên verdict | Không cần cả ba | CI |
| Tích hợp PG | Exercise read API, nộp bài, lịch sử, quyền sở hữu | PG thật (`TEST_DB_URL`) | CI |
| Adapter stub | Judge0 request/poll qua HTTP stub cục bộ | Không cần dịch vụ ngoài | CI |
| Worker grading service | Chấm test công khai/ẩn, dừng khi Compile Error | Stub `SandboxClient` | CI |
| Redis | Rate limit và enqueue sau commit qua submission API; worker recovery chưa có test end-to-end | Redis service | CI |
| Judge0 thật | N2 (≥ 30 ca) + N4 kịch bản 1 đến 7 | Judge0 thật (môi trường test/spike) | Thủ công / nightly |
| Rate limit mức tích hợp | N4 kịch bản 8 | Redis hoặc thay thế (`TODO`) | `TODO(xác nhận)` |
| Tải | NF-02, NF-01 | Judge0 thật + Redis + PG | Thủ công trước M5 |
| Rò rỉ bí mật | Quét response/log mọi endpoint sandbox | PG + stub | CI |
| Profile/Auth | Session, CSRF, role và quyền sở hữu submission | Không cần dịch vụ ngoài | CI |

## 4. Unit test logic chấm

Chạy không cần Judge0, Redis, PG. Tên class là đề xuất: `JudgeStatusMapperTest`, `OutputNormalizerTest`, `ScoreCalculatorTest`, `VerdictAggregatorTest`, `GradingFlowTest`. Mọi giá trị mong đợi lấy từ `03`. File này chỉ nêu cần ca nào.

| Quy tắc (`03`) | Ca cần có |
|---|---|
| Ánh xạ status (mục 4) | Mỗi dòng của bảng ánh xạ ít nhất một ca: Accepted, Time Limit Exceeded, Compilation Error, từng status nhóm Runtime Error, Internal Error, Exec Format Error, status lạ, thiếu trường, JSON hỏng, In Queue/Processing |
| Đối chiếu Memory Limit (mục 4) | Runtime Error kèm memory ≥ giới hạn; Runtime Error kèm memory thấp; Accepted kèm memory chạm giới hạn; Time Limit Exceeded kèm memory cao (không đổi verdict); ca đúng ngưỡng |
| Compile Error Python (mục 4) | stderr bắt đầu bằng từng loại lỗi cú pháp mà `03` liệt kê; một lỗi runtime khác không bị xếp nhầm |
| Chuẩn hoá và so khớp (mục 5) | Mọi dòng trong bảng ca biên của `03` mục 5; stdout bị cắt vì vượt giới hạn output |
| Điểm (mục 6) | Trọng số khác nhau; số thập phân dài; tổng trọng số = 0; điểm gần `max_score` không bị làm tròn lên; Compile Error; Accepted |
| Ưu tiên verdict (mục 6) | Mọi cặp trong bảng ưu tiên của `03`; ba loại cùng lúc; một loại duy nhất |
| Tiếp tục / dừng (mục 6) | Test 1 sai, test sau đúng (điểm một phần); Compile Error dừng ngay, test sau không chạy |
| `time_ms`, `memory_kb` mức bài (mục 6) | Lấy theo quy tắc `03`; Compile Error cho NULL |
| Lỗi hệ thống (mục 7) | Mỗi nguyên nhân trong danh sách lỗi hệ thống: không ra verdict, không thành Wrong Answer hay Runtime Error |
| Không gửi `expected_output` sang Judge0 (mục 2) | Request build ra từ adapter không chứa `expected_output` |
| Giới hạn từ config (mục 8) | Thiếu key config cần dùng thì lỗi hệ thống; không có giá trị mặc định ngầm |

- Test kiểm tra đúng **tên 6 verdict** và không có verdict thứ bảy.

## 5. Test tích hợp PostgreSQL

Chạy khi có `TEST_DB_URL`, theo cơ chế của `CatalogIntegrationTest`. Tên class đề xuất: `ExerciseIntegrationTest`, `SubmissionIntegrationTest`, `SubmissionHistoryIntegrationTest`, `AttemptQueryIntegrationTest`.

| Nhóm | Ca cần có | Kỳ vọng |
|---|---|---|
| Exercise read API | Exercise của lesson + course đã xuất bản | 200, chỉ test công khai có `stdin`/`expected_output` |
| Exercise nháp | Exercise của lesson nháp; của course nháp; lesson đã xuất bản thuộc course nháp | 404 (`EXERCISE_NOT_FOUND`) |
| Nộp bài | Hợp lệ; chưa đăng nhập; exercise chưa xuất bản; `language` không hỗ trợ; mã quá lớn | 202 + `submission_id`, `status = QUEUED`; 401; 404; 400; 413 (`04` mục 7) |
| Lịch sử | Nhiều bài nộp của một user; `size` 50; `size` vượt 50; trang cuối; trang trống | Mới nhất trước; không vượt 50 mỗi trang; không có `source_code` |
| Xem lại | Chủ submission; user khác | Chủ: có `source_code` và kết quả từng test; user khác: 404 |
| Quyền sở hữu | Hai user, mỗi user có submission | User B không đọc được lịch sử, chi tiết, trạng thái của A |
| Hint ladder | 0 bài; 1 bài sai; nhiều bài sai rồi Accepted; có bài `FAILED` (lỗi hệ thống); có bài `QUEUED`/`RUNNING` | `failedCount` và `hasAccepted` đúng; bài `FAILED`, `QUEUED`, `RUNNING` không tính (`04` mục 9) |
| Query bài kẹt | Bài `QUEUED`/`RUNNING` quá ngưỡng và chưa quá ngưỡng | Chỉ bài quá ngưỡng được trả (`02` mục 6) |
| Test case cho worker | Exercise có test công khai và ẩn | Trả đủ test, đúng thứ tự `position` |
| Ràng buộc DB | Vi phạm `CHECK` (`status`/`verdict`, `score`, `weight`), `UNIQUE` | Bị DB từ chối (`02` mục 4) |

**Fixture SQL cần có** (đặt ở `backend/src/test/resources/`, theo convention hiện có):
- `sandbox-fixtures.sql` (đề xuất): course/lesson xuất bản và nháp, exercise, test công khai và ẩn, lời giải tham khảo, submission mẫu. Tên tệp cuối: `TODO(xác nhận)`.
- Phụ thuộc tên cột và kiểu khóa của V1: `TODO(xác nhận)` (`02` mục 9). Fixture phải khớp V1 thực tế, không đoán.
- Fixture SQL **không** dùng `db/demo/R__seed_demo_catalog.sql` (chỉ profile `dev`).
- Các giá trị đánh dấu bí mật gieo trong fixture: mục 10.

## 6. Judge0 stub cho CI

CI không có Judge0. Stub mô phỏng giao diện Judge0 mà adapter dùng. Không thiết kế mã stub chi tiết. Cách dựng stub (viết tay bằng JDK hay thư viện mock HTTP): đề xuất, `TODO(xác nhận)`. Không tự thêm dependency.

**Hành vi stub phải mô phỏng được:**
- Mỗi status trong bảng ánh xạ của `03` mục 4, với stdout, stderr, time, memory điều khiển được từ test.
- Chuỗi In Queue → Processing → kết thúc sau N lần poll.
- Judge0 down (connection refused), timeout (không trả lời), HTTP 5xx, HTTP 4xx, JSON hỏng hoặc thiếu trường, status lạ, kẹt In Queue mãi.
- Internal Error.
- Phản hồi chậm (cho kiểm tra hạn poll và phép đo ở mục 9).
- stdout rất dài (cho kiểm tra cắt output).
- Ghi lại mọi request nhận được, để khẳng định request không chứa `expected_output`.

**Kịch bản test luồng 3 pha với stub** (tên class đề xuất: `GradingWorkerIntegrationTest`):

| Kịch bản | Kỳ vọng |
|---|---|
| Bài đúng mọi test | `DONE`, Accepted, điểm tối đa, có hàng kết quả cho mọi test |
| Bài sai một test | Tiếp tục chấm, điểm một phần, Wrong Answer |
| Compile Error ở test đầu | Dừng ngay, điểm 0, không có hàng kết quả cho test chưa chạy |
| Time Limit / Runtime Error / Memory Limit | Verdict đúng, ưu tiên khi trộn đúng `03` |
| Judge0 down, timeout, Internal Error, 5xx | Không verdict, retry có giới hạn, hết lượt thì `FAILED` (tên theo `02`), không tính vào lần nộp chưa đạt |
| Lỗi hệ thống ở test giữa lượt | Hủy lượt, chấm lại từ đầu, không để kết quả dở dang |
| Lượt retry thứ hai thành công | `DONE`, kết quả không trùng lặp |
| Hai worker nhận cùng bài | Chỉ một bên chuyển được `QUEUED` → `RUNNING` (`02` mục 5) |
| Job quét bài kẹt | Bài `RUNNING` quá ngưỡng được đưa lại `QUEUED` hoặc `FAILED` đúng quy tắc `02` mục 5 |
| Bài `QUEUED` bị mất job khỏi Redis (nếu kiểm được) | Job quét nhặt lại, không mất bài |
| Ghi DB thất bại lúc finalize | Lỗi hệ thống, không để trạng thái nửa vời |

- Số lần retry, backoff, ngưỡng kẹt: lấy từ `03`/`02`, hiện là `TODO` ở đó. Test dùng giá trị cấu hình, không hard-code.

## 7. N2: bộ ≥ 30 bài nộp mẫu

**Thư mục `docs/sandbox/fixtures/` (đề xuất):**

```
docs/sandbox/fixtures/
  exercises.json        # bài tập mẫu + test công khai/ẩn + trọng số
  cases.json            # chỉ mục: id, exercise, file mã, verdict, điểm, ghi chú
  code/                 # mỗi ca một tệp .py (đặt tên theo id ca)
  attacks/              # fixture tấn công, tách riêng (mục 8)
```

- **Định dạng (đề xuất):** `cases.json` làm chỉ mục máy đọc được, mã ở tệp `.py` riêng. Lý do: mã dễ đọc và diff, test runner đọc một chỉ mục. Thay thế: một bảng Markdown duy nhất. `TODO(xác nhận)`.
- Bài tập mẫu: dựa trên khóa Python cơ bản. Nội dung bài tập cụ thể và số test mỗi bài: `TODO(xác nhận)`.
- `exercises.json` và `sandbox-fixtures.sql` (mục 5) phải đồng bộ. Cách đảm bảo đồng bộ: `TODO(xác nhận)`.

**Một dòng mẫu trong `cases.json` (đề xuất):**

```json
{ "id": "WA-03", "exercise": "sum-two", "file": "code/WA-03.py",
  "expected_verdict": "WRONG_ANSWER", "expected_score": 50.00,
  "note": "đúng test công khai, sai một test ẩn" }
```

**Phân bổ theo verdict (số lượng đề xuất, tổng ≥ 30 theo báo cáo, mỗi verdict ≥ 1):**

| Loại ca | Số ca (đề xuất) |
|---|---|
| Accepted | 6 |
| Wrong Answer | 5 |
| Time Limit | 3 |
| Memory Limit | 3 |
| Runtime Error | 4 |
| Compile Error | 4 |
| Ca biên chuẩn hoá khoảng trắng (ra Accepted hoặc Wrong Answer, theo `03` mục 5) | 3 |
| Ca nhiều test khác kết quả (kiểm ưu tiên verdict, `03` mục 6) | 3 |
| Ca điểm một phần theo trọng số | 2 |
| **Tổng** | **33** |

- Ca Time Limit, Memory Limit trong N2 là mã tối thiểu chạm giới hạn, theo mô tả hành vi ở mục 8.
- Mỗi quy tắc ở mục 4 có ít nhất một ca N2 tương ứng (`03` mục 10).

**Cách chạy trên Judge0 thật (thủ công/nightly):**
1. Dựng PG, Redis, worker, Judge0 ở môi trường test/spike. Cấu hình: `TODO(xác nhận)`.
2. Nạp `exercises.json` và các bài mẫu vào DB.
3. Nộp từng ca qua đường nộp bài thật bằng một tài khoản/`CurrentUser` stub của môi trường test.
4. Chờ `DONE`, so `verdict` và `score` với `cases.json`.
5. Ghi kết quả từng ca vào tệp kết quả (mục 12).

**Tiêu chí đạt (đề xuất):** 100% ca khớp verdict và điểm mong đợi. `TODO(xác nhận)`: có chấp nhận ngoại lệ nào không, và cách xử lý khi Judge0 báo khác dự kiến.

**Nơi lưu kết quả:** `docs/sandbox/evidence/n2/` (đề xuất, `TODO`; nhất quán với nơi ghi kết quả spike ở `05` mục 8).

## 8. N4: 8 kịch bản tấn công

**Quy tắc an toàn khi chạy:**
- **Chỉ** chạy trên Judge0 của môi trường test/spike. Không chạy trên production, không chạy trên VPS chính. Môi trường test không nối tới hạ tầng thật (`05` mục 3, lớp 2, 4).
- Fixture ở `docs/sandbox/fixtures/attacks/`, có tệp `README` ghi cảnh báo trên (đề xuất).
- Payload là mã Python tối thiểu chỉ để chạm giới hạn rồi bị chặn. Không chứa đường dẫn, địa chỉ hay lệnh nhắm vào hạ tầng thật. Không có kỹ thuật thoát sandbox.
- Theo dõi tài nguyên VPS Sandbox (CPU, RAM, số tiến trình, đĩa) trong lúc chạy. Cách theo dõi: `TODO(xác nhận)`.
- Chạy tuần tự, từng kịch bản, kiểm tra VPS Sandbox về trạng thái bình thường trước kịch bản tiếp theo.
- **Dừng khẩn cấp (đề xuất, `TODO`):** người chạy luôn có quyền tắt/khởi động lại Judge0 hoặc VPS Sandbox. Ngưỡng tài nguyên nào thì dừng và ai quyết định: `TODO(xác nhận)`.

| # | Kịch bản | Hành vi payload tối thiểu (không mã khai thác) | Kỳ vọng | Cần | Bằng chứng cần thu | Lớp (`05`) |
|---|---|---|---|---|---|---|
| 1 | Vòng lặp vô hạn | Chương trình lặp mãi, không I/O | Ngắt đúng hạn, Time Limit | Judge0 thật | Verdict Time Limit; `time_ms` ≈ giới hạn; không còn tiến trình treo | 3 |
| 2 | Cấp phát bộ nhớ vô hạn | Chương trình cấp phát liên tục tới khi vượt giới hạn bộ nhớ | Ngắt, Memory Limit hoặc Runtime Error | Judge0 thật | Verdict; log Judge0 cho thấy tiến trình bị dừng; RAM VPS Sandbox không cạn | 3 |
| 3 | Fork bomb / tạo tiến trình liên tục | Chương trình tạo tiến trình con liên tục tới khi chạm giới hạn tiến trình | Bị chặn, không treo VPS | Judge0 thật | Verdict theo `03` (Runtime Error dự kiến); VPS Sandbox vẫn phản hồi, chạy được bài tiếp | 3, 6 |
| 4 | Mở kết nối mạng ra ngoài hoặc nội bộ | Chương trình thử mở một kết nối tới đích do môi trường test cấp, đích đó không phải hạ tầng thật | Thất bại | Judge0 thật | Kết quả chạy lỗi kết nối; phía đích/firewall xác nhận không có gói tin đến | 2, 4 |
| 5 | Đọc tệp ngoài thư mục làm việc | Chương trình thử mở một tệp canary do môi trường test đặt ngoài thư mục làm việc | Bị từ chối | Judge0 thật | Lỗi từ chối; nội dung canary không xuất hiện trong output | 1, 6 |
| 6 | Ghi tệp dung lượng cực lớn | Chương trình ghi liên tục vào một tệp trong thư mục làm việc | Bị chặn | Judge0 thật | Verdict Runtime Error (theo `03` mục 8); dung lượng đĩa VPS Sandbox không tăng bất thường | 3, 1 |
| 7 | In output cực lớn | Chương trình in liên tục ra stdout | Cắt ngắn ở ngưỡng tối đa | Judge0 thật | Output lưu/trả về không vượt ngưỡng; worker không hết bộ nhớ | 3 |
| 8 | Spam nộp bài từ một tài khoản | Script gọi API nộp bài liên tục từ một tài khoản | Rate limit chặn, người khác không bị ảnh hưởng | Redis/rate limit (không cần Judge0 thật nếu dùng stub) | Chuỗi 429; tài khoản khác nộp song song vẫn được chấm đúng, độ trễ bình thường | 5 |

- Kịch bản 1 đến 7 **không** chạy được bằng stub: stub chỉ kiểm tra worker xử lý đúng status trả về, không kiểm tra giới hạn thực của Judge0.
- Kịch bản 8 kiểm được ở mức tích hợp trong CI nếu có Redis hoặc thay thế (`TODO`, mục 11).
- Nơi lưu bằng chứng: `docs/sandbox/evidence/n4/` (đề xuất, `TODO`). Khung bằng chứng đồng bộ `05` mục 10.

## 9. NF-02 và NF-01

**NF-02: tải 30 bài nộp đồng thời** (con số 30 là mục tiêu đề xuất của báo cáo, `TODO` xác nhận ở `00-SCOPE.md`):
- **Cách tạo tải (đề xuất):** 30 yêu cầu nộp bài bắt đầu cùng lúc, từ nhiều tài khoản test (để không vướng rate limit theo tài khoản). Công cụ tải: `TODO(xác nhận)`, không tự thêm dependency.
- **Điều kiện:** Judge0 thật + Redis + PG + worker ở môi trường test. Bài mẫu "đơn giản" (xem NF-01). Cấu hình giới hạn ghi lại cùng kết quả.
- **Chỉ số cần ghi:**
  - Số bài nhận được (202) so với số bài đạt `DONE`/`FAILED`. Mục tiêu: **không mất bài**, mỗi bài nộp phải kết thúc ở trạng thái cuối.
  - Thời gian từ nộp đến có kết quả cho từng bài (trung vị, lớn nhất).
  - Số lỗi và loại lỗi (429, 5xx, `FAILED`).
  - Tài nguyên VPS Sandbox và VPS chính trong lúc tải.
- **Ngưỡng đạt:** ngoài "không mất bài" và con số 30, ngưỡng thời gian/tỉ lệ lỗi: `TODO(xác nhận)`.

**NF-01 / M3: mã đơn giản phản hồi khoảng 3 giây:**
- **Điều kiện "mã đơn giản" (đề xuất, `TODO`):** chương trình in kết quả tức thì, số test ít, Judge0 không bị tải.
- **Cách đo (đề xuất):** thời gian từ lúc nhận 202 (hoặc lúc gửi POST) đến lúc kết quả có sẵn (SSE `result` hoặc polling). Chạy nhiều lần, ghi phân bố. Điểm bắt đầu và kết thúc đo: `TODO(xác nhận)`.
- Ngưỡng đạt: "khoảng 3 giây" theo báo cáo. Cách diễn giải (trung vị hay mọi lần): `TODO(xác nhận)`.
- Hệ quả thiết kế (chấm tuần tự theo số test, khoảng poll): `03` mục 6.
- Nơi lưu bằng chứng: `docs/sandbox/evidence/nf/` (đề xuất, `TODO`).

## 10. Test không lộ bí mật

**Kỹ thuật gieo giá trị đánh dấu (đề xuất):**
- Trong fixture SQL, gieo chuỗi đánh dấu duy nhất (ví dụ có tiền tố `CANARY_`) vào:
  - `expected_output` và `stdin` của test **ẩn**.
  - `source_code` của lời giải tham khảo.
  - `source_code` của một submission thuộc user khác.
  - stderr/output mà stub trả về cho test ẩn.
- Chạy các luồng thật qua API rồi khẳng định chuỗi đánh dấu **không xuất hiện** trong response, SSE, hay log. Đối chiếu `expected_output`/`stdin` của test **công khai** thì được phép xuất hiện.
- Tên test đề xuất: `SecretLeakTest`.

**Phải quét (CI, stub):**

| Đối tượng | Cách quét |
|---|---|
| Response mọi endpoint sandbox ở `04` mục 3 (xem Exercise, nộp, trạng thái, lịch sử, chi tiết, admin nếu có) | So khớp chuỗi trong toàn bộ body JSON, kể cả `problem+json` |
| Sự kiện SSE (`04` mục 5) | Đọc toàn bộ stream tới khi đóng, quét chuỗi |
| Log ứng dụng khi chạy luồng chấm | Bắt log (appender của test) rồi quét |
| Request gửi tới Judge0 stub | Khẳng định không chứa `expected_output` (`03` mục 2) |
| Thông báo lỗi trả học viên | Không chứa đường dẫn sandbox (stub trả stderr có đường dẫn giả) |
| Repo (secret scanning) | `TODO(xác nhận)`: công cụ trong CI |

**Trường bị cấm:** danh sách đầy đủ ở `04` mục 8. Mỗi dòng của bảng đó có ít nhất một test ở đây. Test không lặp lại danh sách.

**Các test bảo mật khác:**
- **`CurrentUser` stub chỉ ở `dev`** (đề xuất, nối `05` mục 5): chạy ngữ cảnh Spring với profile khác `dev`, khẳng định không có bean stub, request thiếu xác thực nhận 401, và khởi động fail-fast nếu stub bị bật ngoài `dev`.
- **Quyền sở hữu:** hai user; user B gọi trạng thái, chi tiết, lịch sử, SSE của A, kỳ vọng 404 (hoặc 403, theo `TODO` ở `04` mục 11).
- **Kích thước mã:** mã đúng ngưỡng và vượt ngưỡng (giá trị từ config), kỳ vọng 413 trước khi lưu `submission` (`05` mục 5).
- **Rate limit mức tích hợp:** vượt tần suất nộp, vượt số bài chờ tối đa, kỳ vọng 429 với `code` đúng (`04` mục 7). Cần Redis hoặc thay thế (mục 11).
- **Whitelist:** mọi route/method không nằm trong `04` mục 3 vẫn bị từ chối; 6 endpoint GET hiện có vẫn hoạt động như cũ (`CatalogIntegrationTest` tiếp tục xanh).

## 11. Tích hợp CI và quy trình chạy

**ĐÃ CÓ:**
- Lệnh: `./mvnw verify`.
- Test cần PG bật qua `TEST_DB_URL`. CI cấp PostgreSQL 16.

**Đề xuất:**

| Phần | Chạy ở | Ghi chú |
|---|---|---|
| Unit (mục 4) | CI, `./mvnw verify` | Không cần dịch vụ ngoài |
| Tích hợp PG (mục 5, 10) | CI, khi có `TEST_DB_URL` | Theo cơ chế hiện có |
| Tích hợp Judge0 stub (mục 6) | CI | Không cần Judge0 hay Redis thật |
| Rò rỉ bí mật, profile stub (mục 10) | CI | Dùng stub |
| Redis/queue/rate limit (mục 8 kịch bản 8, mục 10) | `TODO(xác nhận)` | CI chưa có Redis; xem bên dưới |
| N2, N4 (1 đến 7) | Thủ công / nightly | Cần Judge0 thật ở môi trường test/spike |
| NF-02, NF-01 | Thủ công trước M5 | Cần toàn bộ hạ tầng |

- **Redis trong CI:** chưa chốt. Lựa chọn (đề xuất, `TODO`): (a) Redis thật qua container service trong workflow; (b) bản thay thế trong bộ nhớ sau một interface. Không tự thêm service hay dependency.
- **Thay đổi ở `.github/workflows/backend-ci.yml` (đề xuất, `TODO`):** có thể cần thêm service Redis nếu chọn (a). Không thêm Judge0. Nightly cho N2: workflow riêng, chạy thủ công, cần secret/địa chỉ Judge0 test. Chưa thực hiện khi chưa hỏi nhóm.
- Test nightly/thủ công không được làm đỏ `./mvnw verify` mặc định. Cách tách (profile Maven, biến môi trường): `TODO(xác nhận)`. Không dùng `@Disabled` để tách.
- Cách chạy cục bộ: `TEST_DB_URL=... ./mvnw verify`. Giá trị `TEST_DB_URL` theo `.env.example` hiện có.

## 12. Mẫu bảng bằng chứng nghiệm thu (chỉ khung, đồng bộ `05` mục 10)

| Hạng mục | Ngày | Người chạy | Môi trường | Kết quả | Tham chiếu bằng chứng |
|---|---|---|---|---|---|
| N2: bộ ≥ 30 bài nộp mẫu (khớp verdict) | | | | | |
| N4-1 Vòng lặp vô hạn | | | | | |
| N4-2 Cấp phát bộ nhớ vô hạn | | | | | |
| N4-3 Fork bomb / tạo tiến trình | | | | | |
| N4-4 Kết nối mạng ra ngoài / nội bộ | | | | | |
| N4-5 Đọc tệp ngoài thư mục làm việc | | | | | |
| N4-6 Ghi tệp dung lượng cực lớn | | | | | |
| N4-7 In output cực lớn | | | | | |
| N4-8 Spam nộp bài từ một tài khoản | | | | | |
| NF-02: 30 bài nộp đồng thời, không mất bài | | | | | |
| NF-01: mã đơn giản khoảng 3 giây | | | | | |
| Không lộ `expected_output`/`stdin` test ẩn | | | | | |
| Không lộ lời giải tham khảo | | | | | |
| Log không chứa mã nguồn, bí mật | | | | | |
| Submission của người khác bị từ chối | | | | | |
| `CurrentUser` stub tắt ở profile khác `dev` | | | | | |
| `./mvnw verify` xanh | | | | | |

## 13. Điểm cần xác nhận

- `TODO(xác nhận):` công cụ test tải cho NF-02 và cách tạo tải.
- `TODO(xác nhận):` cách dựng Judge0 stub (viết tay hay thư viện mock HTTP); không thêm dependency khi chưa hỏi.
- `TODO(xác nhận):` cách test Redis (queue, rate limit) trong CI: Redis thật qua container hay thay thế.
- `TODO(xác nhận):` số ca mỗi verdict trong N2 (bảng mục 7 là đề xuất).
- `TODO(xác nhận):` tiêu chí đạt N2 (đề xuất 100% khớp) và cách xử lý ngoại lệ.
- `TODO(xác nhận):` ngưỡng đạt NF-02 ngoài "không mất bài"; cách đo và điều kiện "mã đơn giản" cho NF-01.
- `TODO(xác nhận):` định dạng fixture (`cases.json` + tệp `.py` hay bảng Markdown).
- `TODO(xác nhận):` bài tập mẫu và số test mỗi bài; tên tệp `sandbox-fixtures.sql`; cách giữ `exercises.json` và fixture SQL đồng bộ.
- `TODO(xác nhận):` quy tắc dừng khẩn cấp khi chạy kịch bản tấn công và cách theo dõi tài nguyên VPS Sandbox.
- `TODO(xác nhận):` môi trường chạy test Judge0 thật (N2, N4, NF) và cách cấp tài khoản/`CurrentUser` stub cho môi trường đó.
- `TODO(xác nhận):` nơi lưu bằng chứng (`docs/sandbox/evidence/` hay phụ lục, nhất quán `05` mục 8).
- `TODO(xác nhận):` công cụ secret scanning trong CI; cách tách test nightly khỏi `./mvnw verify` mặc định; thay đổi cụ thể ở `backend-ci.yml`.
- `TODO(xác nhận):` các giá trị chờ từ `03`/`02`/`04` (retry, ngưỡng kẹt, 404 hay 403, hiện `time_ms`/`memory_kb` của test ẩn): test phải cập nhật theo khi chốt.
