# 05-SECURITY: Bảo mật Coding + Sandbox chấm bài

## 1. Mục đích và phạm vi

- Đây là đặc tả bảo mật và checklist nghiệm thu. Mã đã có session/CSRF/role cho submission, Redis queue/rate limit, worker riêng và Judge0 adapter. Các lớp cô lập bên Judge0 và kiểm thử N2/N4 thật chưa được xác minh.
- **Nguyên tắc nền: mã người dùng là KHÔNG TIN CẬY.** Không chạy trong JVM của ứng dụng. Mọi thực thi đi qua Judge0 (adapter) trên VPS riêng.
- Phạm vi: backend cho SB-02 đến SB-06, NF-04, N4. Ngoài phạm vi (SB-01 iframe, SB-07 đến SB-10, ngôn ngữ ngoài Python, trợ lý AI, staging/production, giám sát): xem `00-SCOPE.md`.
- Không lặp: kiến trúc và ADR → `01-ARCHITECTURE.md`; cột bảng → `02-DATA-MODEL.md`; ánh xạ verdict, giới hạn, hành vi khi vượt → `03-GRADING-SPEC.md`; endpoint, DTO, mã lỗi → `04-API-CONTRACT.md`; payload và kịch bản test → `06-TEST-PLAN.md`.
- Nhãn: "(đề xuất)" là quy tắc không có trong báo cáo. `TODO(xác nhận)` là giá trị chưa có, điền sau spike tuần 1–2 (M1, R7).

**ĐÃ CÓ trong mã:** session/CSRF và vai trò cho submission; whitelist route; CORS theo danh sách origin (profile `dev` mặc định cho phép frontend cổng 3000); `application/problem+json` có `code` và `traceId`; `X-Request-Id` đưa vào MDC; PostgreSQL và Redis trong compose dev; kiểm tra giới hạn theo cấu hình; worker/Judge0 adapter; không gửi `expected_output` hoặc callback.

**CHƯA ĐƯỢC NGHIỆM THU:** Judge0 thật và isolation, tách VPS/firewall, N2/N4, kiểm thử tải, SSE, staging/production và giám sát.

## 2. Mô hình đe dọa

**Tài sản cần bảo vệ:**
- Máy chủ chính: PostgreSQL, Redis, khóa bí mật.
- Bộ test ẩn và lời giải tham khảo.
- Mã nguồn và lịch sử nộp của từng học viên.
- Tính sẵn sàng của hệ thống chấm cho người dùng khác.

**Kẻ tấn công điển hình:**
- Học viên (hoặc tài khoản giả) gửi mã độc hại hoặc spam nộp bài.
- Người cố đọc test ẩn qua API.
- Người cố xem bài nộp của người khác.

**Rủi ro:**
- **R3** (tác động Cao): sandbox bị lạm dụng/tấn công tài nguyên.
- **R7** (tác động Cao): Judge0 khó cài đặt hoặc vận hành thiếu ổn định. Giảm thiểu: spike tuần 1–2 (M1), chạy thử bằng Docker Compose.
- Phương án dự phòng nếu Judge0 khó vận hành: dùng bản dịch vụ của Judge0 hoặc chuyển sang Piston. Đây là phương án dự phòng, chưa phải quyết định. Nếu kích hoạt thì phải rà lại toàn bộ file này.

**Ranh giới tin cậy** (kiến trúc chi tiết: `01-ARCHITECTURE.md`):

| Vùng | Mức tin cậy | Ai được gọi ai |
|---|---|---|
| Trình duyệt học viên | Không tin cậy | Chỉ gọi Nginx → Backend API |
| Nginx (VPS chính) | Biên, chấm dứt HTTPS | Chuyển tiếp tới Backend API |
| Backend API (VPS chính) | Tin cậy | Gọi PostgreSQL, Redis. **Không gọi Judge0** |
| Worker chấm bài (VPS chính) | Tin cậy | Đọc job từ Redis, đọc PostgreSQL, **duy nhất được gọi Judge0** |
| PostgreSQL, Redis (VPS chính) | Tin cậy, nội bộ | Chỉ Backend API và Worker. Không mở ra Internet |
| Judge0 (VPS 2) | Không tin cậy với dữ liệu: không giữ dữ liệu nghiệp vụ hay khóa bí mật | Chỉ nhận từ Worker. Không ra Internet, không gọi ngược VPS chính |
| Tiến trình chạy mã người dùng (trong Judge0) | Hoàn toàn không tin cậy | Không mạng, không gọi ai |

- Judge0 **không public**. **Không dùng `callback_url`**, vì Judge0 bị chặn gọi ngược về VPS chính.

## 3. Bảng 5.8 thành checklist (7 lớp, NF-04)

Mọi giá trị cụ thể (giới hạn, cổng, dải IP, tham số Judge0) là `TODO(xác nhận)` sau spike. Hành vi khi vượt giới hạn: `03-GRADING-SPEC.md` mục 8.

### Lớp 1. Cô lập phiên chạy
- **Trách nhiệm:** Judge0 (cấu hình, hạ tầng).
- **Yêu cầu:** mỗi lần thực thi tạo môi trường riêng, hủy ngay sau khi xong, không chia sẻ tệp giữa các lần chạy.
- **Cách kiểm chứng:** chạy hai lần liên tiếp. Lần 1 để lại một tệp trong thư mục làm việc. Lần 2 khẳng định tệp không còn.
- **Kịch bản liên quan:** 5, 6.
- [ ] Mỗi lần chạy dùng môi trường riêng.
- [ ] Môi trường bị hủy sau khi xong (kể cả khi Time Limit).
- [ ] Không còn tệp giữa hai lần chạy.
- [ ] Cơ chế cô lập cụ thể và phiên bản Judge0: `TODO(xác nhận)`.

### Lớp 2. Tắt mạng hoàn toàn
- **Trách nhiệm:** Judge0 (cấu hình) + hạ tầng (firewall/security group).
- **Yêu cầu:** chặn mọi kết nối ra Internet và mạng nội bộ từ tiến trình chạy mã. Judge0 cũng bị chặn ra Internet và chặn truy cập ngược vào VPS chính.
- **Cách kiểm chứng:** chạy mã thử kết nối ra Internet, ra địa chỉ nội bộ, và ra VPS chính. Mọi kết nối phải thất bại.
- **Kịch bản liên quan:** 4.
- [ ] Mã người dùng không kết nối được Internet.
- [ ] Mã người dùng không kết nối được mạng nội bộ.
- [ ] Judge0 không kết nối được VPS chính.
- [ ] Cơ chế chặn cụ thể: `TODO(xác nhận)`.

### Lớp 3. Giới hạn tài nguyên
- **Trách nhiệm:** Backend (nạp giới hạn từ bảng cấu hình, `02`), Worker (gửi giới hạn cho Judge0, đối chiếu kết quả, cắt output; `03`), Judge0 (thực thi giới hạn).
- **Yêu cầu:** khống chế thời gian CPU, RAM, số tiến trình, dung lượng ghi đĩa, độ dài output. Giá trị lấy từ bảng cấu hình, không hard-code.
- **Cách kiểm chứng:** 5 kịch bản 1, 2, 3, 6, 7. Kiểm tra mã nguồn: không có hằng số giới hạn hard-code (grep, review).
- [ ] Mỗi giới hạn lấy từ config, thiếu key thì lỗi cấu hình, không dùng giá trị mặc định ngầm (`03` mục 3).
- [ ] Vượt giới hạn cho verdict đúng (`03`).
- [ ] Giá trị mặc định và tham số Judge0: `TODO(xác nhận)`.

### Lớp 4. Tách máy chủ
- **Trách nhiệm:** Kiến trúc/hạ tầng.
- **Yêu cầu:** Judge0 chạy trên VPS độc lập, không lưu dữ liệu nghiệp vụ hay khóa bí mật.
- **Cách kiểm chứng:** rà soát cấu hình VPS 2: không có kết nối DB, không có secret của VPS chính. Đối chiếu bản đồ mạng.
- **Kịch bản liên quan:** 3, 4, 5.
- [ ] VPS 2 chỉ chạy Judge0.
- [ ] VPS 2 không có credential PostgreSQL, Redis hay secret ứng dụng.
- [ ] Treo hoặc sập VPS 2 không làm sập VPS chính (xem lỗi hệ thống, `03` mục 7).

### Lớp 5. Giới hạn theo người dùng
- **Trách nhiệm:** Backend (tiếp nhận) + Redis (đếm). Xem mục 7.
- **Yêu cầu:** rate limit tần suất nộp, giới hạn kích thước mã, số bài đang chờ tối đa mỗi tài khoản. Giá trị từ bảng cấu hình.
- **Cách kiểm chứng:** kịch bản 8; test tích hợp cho từng giới hạn, kỳ vọng mã lỗi ở `04` mục 7.
- **Kịch bản liên quan:** 8.
- [ ] Vượt tần suất: 429 (`RATE_LIMITED`).
- [ ] Vượt số bài chờ: 429 (`TOO_MANY_PENDING_SUBMISSIONS`).
- [ ] Mã quá lớn: 413 (`SOURCE_CODE_TOO_LARGE`), kiểm tra trước khi lưu (mục 5).
- [ ] Spam từ một tài khoản không ảnh hưởng người dùng khác.

### Lớp 6. Đặc quyền tối thiểu
- **Trách nhiệm:** Judge0 (tài khoản chạy mã), hạ tầng (firewall), Backend/Worker (ai được gọi).
- **Yêu cầu:** mã chạy dưới tài khoản hệ thống không có đặc quyền. **Chỉ Worker** được giao tiếp với Judge0. Backend API không gọi Judge0.
- **Cách kiểm chứng:** chạy mã in danh tính tiến trình (kỳ vọng: không phải root/đặc quyền); thử gọi Judge0 từ máy khác ngoài Worker (kỳ vọng: bị chặn); review mã: chỉ có một Judge0 client, nằm trong module worker/`sandbox`.
- **Kịch bản liên quan:** 3, 5.
- [ ] Tiến trình chạy mã không có đặc quyền.
- [ ] Chỉ nguồn Worker gọi được Judge0.
- [ ] Judge0 không public.
- [ ] Xác thực Worker ↔ Judge0 (token hay chỉ firewall): `TODO(xác nhận)`.

### Lớp 7. Cô lập phía client
- **Trách nhiệm:** **Frontend (SB-01). NGOÀI phạm vi backend.** Chỉ nêu để vẽ ranh giới.
- **Yêu cầu:** `<iframe sandbox>` không có quyền truy cập cookie hay dữ liệu trang chính.
- **Cách kiểm chứng:** thuộc nhóm frontend. Backend không kiểm chứng lớp này.
- [ ] Frontend xác nhận lớp này (ngoài phạm vi, ghi nhận).

## 4. Ánh xạ 8 kịch bản tấn công (Bảng 5.9, N4)

Không có payload ở đây. Payload và mã chạy được: `06-TEST-PLAN.md` và `fixtures/`.

| # | Kịch bản | Kỳ vọng | Lớp chặn | Bằng chứng cần thu |
|---|---|---|---|---|
| 1 | Vòng lặp vô hạn | Ngắt đúng hạn, trả Time Limit | 3 | Kết quả submission: verdict Time Limit; `time_ms` ≈ giới hạn; Judge0 không còn tiến trình treo |
| 2 | Cấp phát bộ nhớ vô hạn | Ngắt, trả Memory Limit hoặc Runtime Error | 3 | Verdict đúng; log Judge0 cho thấy tiến trình bị dừng; VPS 2 không cạn RAM |
| 3 | Fork bomb / tạo tiến trình liên tục | Bị chặn bởi giới hạn tiến trình, không treo VPS | 3, 6 | Verdict Runtime Error (hoặc theo `03`); VPS 2 vẫn phản hồi, chạy được bài tiếp theo |
| 4 | Mở kết nối mạng ra ngoài hoặc nội bộ | Thất bại | 2, 4 | Kết quả chạy cho thấy kết nối lỗi; số liệu từ firewall/máy đích khẳng định không có gói tin |
| 5 | Đọc tệp hệ thống hoặc tệp ngoài thư mục làm việc | Bị từ chối | 1, 6 | Kết quả chạy: lỗi từ chối; nội dung tệp nhạy cảm không xuất hiện trong output |
| 6 | Ghi tệp dung lượng cực lớn ra đĩa | Bị chặn | 3, 1 | Verdict Runtime Error (SIGXFSZ, `03` mục 8); dung lượng ổ VPS 2 không tăng bất thường |
| 7 | In output cực lớn | Cắt ngắn ở ngưỡng tối đa | 3 | Output lưu/trả về không vượt ngưỡng; worker không hết bộ nhớ |
| 8 | Spam nộp bài liên tục từ một tài khoản | Bị rate limit chặn, không ảnh hưởng người dùng khác | 5 | Chuỗi phản hồi 429; tài khoản khác nộp song song vẫn được chấm đúng, độ trễ bình thường |

- Cả 8 ca phải pass trước nghiệm thu (N4, M5, tuần 14–16). Ca 1 đến 7 cũng chạy ở spike tuần 1–2 (mục 8).

## 5. Bảo mật tầng ứng dụng (Spring Boot)

Route cụ thể và mã lỗi: `04-API-CONTRACT.md` mục 3, 7, 10.

**`SecurityConfiguration` (đã triển khai cho các endpoint hiện có):**
- Session hiện tại + CSRF; chỉ mở route đã nêu trong `04`. Route/method khác vẫn bị từ chối.
- `POST` nộp bài yêu cầu xác thực và vai trò LEARNER trở lên.
- Route lịch sử, xem lại và trạng thái kiểm tra chủ sở hữu trong truy vấn. SSE chưa triển khai.
- Exercise đọc công khai; CORS vẫn dùng danh sách origin, không dùng `*` kèm credentials.
- API quản trị config chưa có.

**`CurrentUser`:**
- Lấy ID từ tài khoản gắn với authentication trong session; không dùng stub danh tính trong profile `dev`.
- Hành vi của auth/session khi triển khai staging/production vẫn cần được rà soát trước khi deploy.
- Xác thực SSE/EventSource chỉ cần chốt nếu triển khai SSE.

**Quyền sở hữu submission:**
- Truy vấn lịch sử/chi tiết luôn có điều kiện `user_id = :currentUser`. Không lấy rồi mới so sánh ở tầng ứng dụng.
- Sai chủ: không trả dữ liệu. 404 hay 403: `TODO(xác nhận)` (đề xuất 404, `04` mục 4.5).
- Test hai user, user B không đọc được submission của A.

**Kích thước mã:**
- Kiểm tra ở tầng tiếp nhận (backend), **trước** khi lưu `submission` và trước khi đẩy job. Giới hạn từ bảng cấu hình. Mã quá lớn: 413.
- Giới hạn kích thước body ở Nginx/Spring: `TODO(xác nhận)` (lớp bổ trợ, không thay cho kiểm tra ở backend).

**Quyền sửa cấu hình giới hạn:**
- Là quyền nhạy cảm: hạ giới hạn khiến lạm dụng dễ hơn. Chỉ vai trò Quản trị.
- Ghi nhật ký thay đổi (ai, khi nào, giá trị cũ/mới): đề xuất, `TODO(xác nhận)`.
- Có endpoint admin hay không: chưa chốt (`04` mục 4.7).
- Giá trị cấu hình phải qua kiểm tra hợp lệ (`CHECK` ở DB, `02` mục 4.6).

- [ ] Whitelist chỉ có đúng route ở `04`.
- [ ] POST nộp bài cần xác thực.
- [ ] Stub tắt ở mọi profile khác `dev`, có test.
- [ ] Truy vấn submission luôn lọc theo chủ sở hữu.
- [ ] Mã quá lớn bị từ chối trước khi lưu.

## 6. Bảo vệ dữ liệu bí mật

| Dữ liệu | Ai được đọc | Cấm xuất hiện ở | Cách đảm bảo | Test (`06`) |
|---|---|---|---|---|
| `expected_output` của test ẩn | Worker | Response, SSE, DTO, log | API đọc qua view `test_case_public` (`02` mục 4.3); không có trong `*Views` | Quét response, SSE, log bằng chuỗi đánh dấu |
| `stdin` của test ẩn (đề xuất) | Worker | Response, SSE, DTO, log; **không gửi sang nơi khác ngoài Judore** | Như trên | Như trên |
| Lời giải tham khảo | Không ai qua API; chỉ công cụ quản trị nội bộ (đề xuất) | Mọi response, log | Repository API không `SELECT` bảng này; role DB riêng (`02` mục 7) | Fixture có chuỗi đánh dấu, quét mọi endpoint |
| `actual_output`/stderr test ẩn | Không ai ngoài worker | Response, log | Không ghi `actual_output` cho test ẩn; `error_message` chỉ từ test công khai (`03` mục 9) | Test ẩn fail kèm stderr có chuỗi đánh dấu |
| Mã nguồn học viên | Chủ submission; worker | Response của người khác; log | Lọc theo `user_id`; không chọn cột trong danh sách | Hai user; quét log |
| Secret (mật khẩu DB, token Judge0, khóa) | Ứng dụng lúc chạy | Mã nguồn, log, response, VPS 2 (trừ credential gọi Judge0) | Nạp từ môi trường lúc chạy, không commit | Quét repo (secret scanning), quét log |

- Nơi lưu secret: chưa chốt ngoài `.env.example` cho dev. Biến môi trường, `.env`, hay secret manager: `TODO(xác nhận)`. `.env` thật không được commit.
- Worker gửi cho Judge0 chỉ mã, stdin và giới hạn. **Không gửi `expected_output`** (`03` mục 2).

**Quy tắc log (đề xuất mức ghi):**
- **Ghi:** `X-Request-Id`/`traceId`, `submission_id`, `exercise_id`, `user_id` (nội bộ), độ dài mã, trạng thái, mã status Judge0, thời gian gọi, loại lỗi.
- **KHÔNG ghi:** mã nguồn đầy đủ, `expected_output` (mọi test), `stdin` test ẩn, lời giải tham khảo, stdout/stderr thô, secret, body response Judge0 nguyên văn. Chi tiết: `03` mục 7.
- Thông báo lỗi biên dịch/runtime trả cho học viên không chứa đường dẫn hay thông tin nội bộ sandbox (`03` mục 9).
- Không in ra log giá trị tham số SQL nếu là cột bí mật.
- [ ] Có test tự động khẳng định không lộ (trỏ `06` và `04` mục 8).

## 7. Rate limit và chống lạm dụng ở tầng người dùng

- Yêu cầu (lớp 5): giới hạn tần suất nộp, kích thước mã, số bài đang chờ mỗi tài khoản.
- Giá trị lấy từ bảng cấu hình (`02` mục 4.6). **Không điền số ở đây.**
- Hành vi khi vượt: 429 (`RATE_LIMITED`, `TOO_MANY_PENDING_SUBMISSIONS`). `Retry-After`: `TODO(xác nhận)` (`04` mục 7).
- Redis là nơi đếm (rate limit và hàng đợi). Phần thiết kế key: `01`.
- **Bảo mật Redis (đề xuất, `TODO(xác nhận)`):**
  - Redis không được mở ra Internet. Chỉ lắng nghe trên mạng nội bộ của VPS chính.
  - Có yêu cầu mật khẩu/xác thực.
  - Chỉ Backend API và Worker truy cập.
  - Không chứa `source_code` đầy đủ hay `expected_output` trong job (chỉ id, đề xuất).
- Redis mất dữ liệu: PostgreSQL là source of truth, bài kẹt được job quét xử lý (`02` mục 5).
- **Rủi ro tồn dư (không giải quyết ở đây):** tài khoản giả/đăng ký hàng loạt vượt rate limit theo tài khoản. Phụ thuộc thiết kế auth và đăng ký.
- [ ] Kịch bản 8 pass.
- [ ] Giới hạn theo user không ảnh hưởng người dùng khác.

## 8. Runbook Judge0 (spike tuần 1–2, M1, R7)

Mục tiêu: chạy thử Judge0 bằng Docker Compose để đánh giá độ khó vận hành. Phiên bản, yêu cầu hệ điều hành/kernel/cgroup, tham số giới hạn, cách chặn mạng, cổng: chưa có thông tin. **Mọi giá trị là `TODO(xác nhận)` điền sau spike.** Không có lệnh cài hay tên biến môi trường ở đây.

**Khung các bước:**
1. **Điều kiện tiên quyết VPS 2:** hệ điều hành, kernel, cgroup, Docker. Ghi lại: `TODO(xác nhận)`.
2. **Dựng bằng Docker Compose:** ghi lại phiên bản Judge0, các service, tệp cấu hình, mọi vướng mắc. `TODO(xác nhận)`.
3. **Cô lập mạng:**
   - Chặn Internet ra từ Judge0 và tiến trình chạy mã.
   - Chặn Judge0 truy cập ngược vào VPS chính.
   - Chỉ cho Worker từ VPS chính gọi tới Judge0.
   - Cơ chế (firewall/security group) và địa chỉ: `TODO(xác nhận)`.
   - Xác nhận Judge0 không public.
4. **Xác thực Worker ↔ Judge0:** token/khóa hay chỉ dựa vào firewall (đề xuất: ít nhất firewall, thêm token nếu Judge0 hỗ trợ). `TODO(xác nhận)`.
5. **Cấu hình giới hạn:** ghi lại tham số Judge0 tương ứng với từng giới hạn ở `03` mục 8. `TODO(xác nhận)`.
6. **Kiểm tra sau cài đặt bằng 8 kịch bản Bảng 5.9** (mục 4), ghi bằng chứng theo mẫu mục 10.
7. **Kiểm tra hành vi Judge0 cần cho `03`:** mã status thực tế, cách báo vượt bộ nhớ, lỗi cú pháp Python, trường JSON. Xem `03` mục 11.
8. **Đo sơ bộ:** thời gian một lượt gọi (cho NF-01), khả năng chịu tải (cho NF-02).

**Tiêu chí "Judge0 đủ ổn định" (đề xuất, ngưỡng `TODO(xác nhận)`):**
- Cài và vận hành được trong khung thời gian spike.
- 8 kịch bản đều đạt kỳ vọng.
- Cô lập mạng đạt (kịch bản 4 và hai chiều VPS).
- Không treo hoặc sập khi chạy kịch bản 1 đến 3, 6, 7.
- Hành vi các status đủ ổn định để ánh xạ theo `03`.

**Kích hoạt phương án dự phòng** (Judge0 dịch vụ hoặc Piston) khi không đạt tiêu chí trên. Quyết định bởi nhóm, kèm ghi nhận. Nếu kích hoạt: rà lại toàn bộ file này, đặc biệt lớp 2, 4, 6 và việc "không dùng dịch vụ ngoài cho mã người dùng" (dịch vụ ngoài làm đổi mô hình tin cậy). `TODO(xác nhận)`.

**Ghi kết quả spike:** vào `PROJECT_STATUS.md` hoặc một phụ lục (đề xuất). `TODO(xác nhận)`.

**Tồn dư:** giám sát, sao lưu, vá lỗi VPS Sandbox ngoài phạm vi, chỉ ghi nhận là rủi ro (mục 9).

## 9. Rủi ro tồn dư và phương án dự phòng

| Rủi ro | Biện pháp giảm thiểu / xử lý |
|---|---|
| R3. Sandbox bị lạm dụng/tấn công tài nguyên | Giảm thiểu: 7 lớp (mục 3) + 8 kịch bản N4 (mục 4). Tồn dư: lỗ hổng chưa biết trong Judge0. Chấp nhận, ghi nhận |
| R7. Judge0 khó cài/vận hành | Spike tuần 1–2. Phương án dự phòng: Judge0 dịch vụ hoặc Piston (chưa phải quyết định) |
| `CurrentUser` stub lọt vào production | `@Profile("dev")`, fail-fast, test khẳng định (mục 5). Rà soát cấu hình trước khi triển khai |
| Redis bị truy cập trái phép | Mạng nội bộ, xác thực, không chứa dữ liệu nhạy cảm (mục 7). Chi tiết `TODO(xác nhận)` |
| Tài khoản giả/đăng ký hàng loạt | Rate limit theo tài khoản chưa đủ. Chấp nhận, ghi nhận. Phụ thuộc auth |
| VPS Sandbox không giám sát, không vá | Ngoài phạm vi. Chấp nhận, ghi nhận |
| Hạ giới hạn cấu hình làm lạm dụng dễ hơn | Chỉ Quản trị, ràng buộc `CHECK`, nhật ký thay đổi (đề xuất) |
| Lộ qua stderr/traceback | Chỉ trả stderr từ test công khai, chuẩn hoá (`03` mục 9) |
| Judge0 trả dữ liệu độc hại | Không tin tưởng response Judge0: kiểm tra schema, cắt độ dài, không thực thi (`03` mục 4) |
| Secret rò rỉ qua log hoặc repo | Quy tắc log (mục 6), secret scanning, `.env` không commit |

## 10. Mẫu bảng bằng chứng nghiệm thu (chỉ khung)

**N4: 8 kịch bản tấn công**

| Kiểm tra | Ngày | Người chạy | Kết quả | Tham chiếu bằng chứng |
|---|---|---|---|---|
| 1. Vòng lặp vô hạn | | | | |
| 2. Cấp phát bộ nhớ vô hạn | | | | |
| 3. Fork bomb / tạo tiến trình | | | | |
| 4. Kết nối mạng ra ngoài / nội bộ | | | | |
| 5. Đọc tệp hệ thống / ngoài thư mục làm việc | | | | |
| 6. Ghi tệp dung lượng cực lớn | | | | |
| 7. In output cực lớn | | | | |
| 8. Spam nộp bài từ một tài khoản | | | | |

**NF-02: tải đồng thời** (mục tiêu 30 bài nộp, con số đề xuất cần xác nhận, `00-SCOPE.md`)

| Kiểm tra | Ngày | Người chạy | Kết quả | Tham chiếu bằng chứng |
|---|---|---|---|---|
| Số bài nộp đồng thời | | | | |
| Không mất bài | | | | |

**Không lộ bí mật**

| Kiểm tra | Ngày | Người chạy | Kết quả | Tham chiếu bằng chứng |
|---|---|---|---|---|
| Response không chứa `expected_output`/`stdin` test ẩn | | | | |
| Response không chứa lời giải tham khảo | | | | |
| Log không chứa mã nguồn, bí mật | | | | |
| Submission của người khác bị từ chối | | | | |
| Stub tắt ở profile khác `dev` | | | | |

## 11. Điểm cần xác nhận

- `TODO(xác nhận):` phiên bản Judge0, yêu cầu OS/kernel/cgroup, tham số cấu hình giới hạn, cổng. Điền sau spike (M1, R7).
- `TODO(xác nhận):` cơ chế cô lập mạng cụ thể (firewall/security group) cho cả hai chiều VPS và cho tiến trình chạy mã.
- `TODO(xác nhận):` xác thực Worker ↔ Judge0 (token/khóa hay chỉ firewall).
- `TODO(xác nhận):` auth thật hay chỉ stub; cơ chế xác thực (cookie/token) và xác thực cho SSE/EventSource.
- `TODO(xác nhận):` CSRF nếu dùng cookie.
- `TODO(xác nhận):` nơi lưu secret (biến môi trường, `.env`, secret manager).
- `TODO(xác nhận):` bảo mật Redis (xác thực, ràng buộc mạng).
- `TODO(xác nhận):` 404 hay 403 cho submission của người khác (đề xuất 404).
- `TODO(xác nhận):` nhật ký thay đổi cấu hình giới hạn; có endpoint admin hay không.
- `TODO(xác nhận):` nơi ghi kết quả spike (`PROJECT_STATUS.md` hay phụ lục).
- `TODO(xác nhận):` giá trị mặc định các giới hạn (dev và môi trường thật), giới hạn body ở Nginx/Spring.
- `TODO(xác nhận):` cấu hình profile production và cách phát hiện stub lọt vào.
- `TODO(xác nhận):` tiêu chí định lượng "Judge0 đủ ổn định" và ai quyết định kích hoạt phương án dự phòng.
