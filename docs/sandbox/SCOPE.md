# 00-SCOPE: Phạm vi Coding + Sandbox chấm bài

## 1. Mục đích

- File này chốt phạm vi: cái gì được làm, cái gì không, xong khi nào. Không nói làm thế nào.
- Các file `01`–`07` và `AGENTS.md` trỏ về đây để biết ranh giới phạm vi.
- Xung đột với file khác về **ranh giới phạm vi**: file này thắng. Xung đột về **chi tiết kỹ thuật**: dừng và báo người, không tự chọn.

## 2. Bối cảnh ngắn

- Nền tảng học lập trình trực tuyến tích hợp trợ lý AI (đồ án ~16 tuần, nhóm nhỏ; hai khóa pilot: Web cơ bản HTML/CSS/JS và Python cơ bản). Phân hệ Sandbox và chấm bài (SB) là một trong ba trụ cột, cạnh Roadmap tương tác và Trợ lý AI gia sư.
- **ĐÃ CÓ (mã):** backend Java 21, Spring Boot 4.1.0, PostgreSQL 16, JDBC (`JdbcClient`), Flyway, catalog/read Exercise API, session-authenticated submission/polling/history/detail, Redis rate limit/queue, worker profile riêng và Judge0 adapter. CI chạy PostgreSQL + Redis.
- **CHƯA NGHIỆM THU:** Judge0 thật, isolation/mạng/tài nguyên, N2/N4 và tải. SSE, frontend, trợ lý AI, staging/production chưa có. Tài liệu `docs/sandbox/*` vẫn là đặc tả/checklist và không thay thế bằng chứng nghiệm thu thật.

## 3. Trong phạm vi (backend P1)

| Mã yêu cầu | Nội dung | File chi tiết |
|---|---|---|
| SB-02 | Bài tập cuối bài chấm tự động theo test case stdin/stdout qua Judge0 | 01, 03 |
| SB-03 | Trả đủ 6 verdict (Accepted, Wrong Answer, Time Limit, Memory Limit, Runtime Error, Compile Error); điểm = (tổng trọng số test đạt / tổng trọng số tất cả test) × điểm tối đa | 03 |
| SB-04 | Tách test công khai (hiện input/output) và test ẩn (chỉ hiện Passed/Failed) | 02, 03, 04 |
| SB-05 | Lưu lịch sử nộp bài, xem lại mã nguồn và kết quả từng lần | 02, 04 |
| SB-06 | Giới hạn tần suất nộp, kích thước mã, thời gian chạy, bộ nhớ, số tiến trình, số bài đang chờ tối đa mỗi user | 01, 02, 03, 05 |
| UC10 | Luồng nộp bài 3 pha (tiếp nhận, thực thi, tổng hợp) | 01, 03 |
| NF-01 / M3 | Mã đơn giản phản hồi khoảng 3 giây | 03, 06 |
| NF-02 | Mục tiêu đề xuất 30 bài nộp đồng thời không mất bài | 01, 06 |
| NF-04 | 7 lớp bảo vệ sandbox (Bảng 5.8) | 05 |
| N2 | ≥ 30 bài nộp mẫu cho đúng verdict | 06 |
| N4 | 8 kịch bản tấn công (Bảng 5.9) | 05, 06 |
| AD-03 (phần backend) | Bảng cấu hình giới hạn sandbox, không hard-code giới hạn | 02, 03 |

- Submission module phải cung cấp sẵn **số lần nộp chưa đạt** và **cờ đã Accepted** theo (user, exercise) cho hint ladder sau này. Chi tiết: 02, 04.
- Chỉ ngôn ngữ **Python** trong MVP.

## 4. Ngoài phạm vi

| Mã / hạng mục | Lý do | Ai / giai đoạn nào làm |
|---|---|---|
| SB-01 (`<iframe sandbox>` cho HTML/CSS/JS) | Thuộc frontend | Frontend |
| SB-07 (Playground độc lập đa ngôn ngữ) | Mức P2 | P2 |
| SB-08 (Pyodide cho Python trên trình duyệt) | Mức P2 | P2 |
| SB-09 (chấm HTML/CSS bằng DOM) | Mức P2 | P2 |
| SB-10 (unit test JUnit/pytest) | Mức P3 | P3, sau MVP |
| Ngôn ngữ ngoài Python (Java, C/C++, C#...) | Mở rộng sau MVP bằng cấu hình; MVP làm Python trước | Sau MVP |
| Trợ lý AI, hint ladder (AI-05, giai đoạn 3) | Thuộc phân hệ AI; ta chỉ cung cấp contract truy vấn | Giai đoạn 3 |
| Frontend, Monaco Editor | Không thuộc backend | Frontend |
| CMS/duyệt xuất bản, blog, roadmap, lưu tiến độ | Phân hệ khác | Ngoài phân hệ SB |
| Giao diện trang quản trị (AD-03) | Là frontend; chỉ phần backend của AD-03 thuộc phạm vi | Frontend |
| Auth đầy đủ | Chưa chốt có thuộc phạm vi không, xem mục 6 và 10 | `TODO(xác nhận)` |
| Staging/production, giám sát | Chưa thuộc giai đoạn này | Ngoài phạm vi hiện tại |

## 5. Quyết định đã chốt

| Quyết định | File chi tiết |
|---|---|
| Python trước | 03 |
| Luồng 3 pha: tiếp nhận (lưu Queued, trả id ngay), thực thi (worker gọi Judge0 từng test), tổng hợp (verdict, điểm, phát kết quả) | 01, 03 |
| Kết quả qua SSE, có polling fallback | 01 |
| Redis cho queue và rate limit; PostgreSQL là source of truth; có job quét lại bài kẹt QUEUED/RUNNING | 01, 02 |
| Lệch báo cáo/repo: báo cáo có Redis, repo hiện chỉ có PostgreSQL trong compose. Đã chốt theo báo cáo. Nếu sau này bỏ Redis để dùng PG `SKIP LOCKED` thì phải ghi rõ là lệch đặc tả | 01 |
| Judge0 trên VPS riêng, không Internet, không truy cập ngược về máy chính, không public | 01, 05 |
| KHÔNG dùng `callback_url`; chỉ worker được gọi Judge0 | 01, 03, 05 |
| `expected_output` của test ẩn và lời giải tham khảo không bao giờ rời server (không DTO, response, log) | 02, 04, 05 |
| Giới hạn sandbox nằm trong bảng cấu hình, không hard-code | 02, 03 |
| Quy tắc xuất bản hiện có áp dụng cho Exercise: bài thuộc lesson nháp hoặc course nháp trả 404 | 02, 04 |
| Contract-first: sửa `docs/api/openapi.yaml` trước, chỉ thêm, không phá endpoint hiện có | 04 |
| Trong lúc chờ auth: `CurrentUser` stub CHỈ ở profile `dev` | 04, 05 |
| Mã mới ở package `submission` và `sandbox` cạnh `content`; Judge0 client nằm sau một interface (đề xuất, chưa phải đã có) | 01 |

## 6. Ranh giới với các phân hệ khác

| Phía kia | Họ cần gì từ ta | Ta cần gì từ họ |
|---|---|---|
| Frontend | Contract trong `docs/api/openapi.yaml`, kết quả qua SSE + polling fallback | Soạn thảo mã, hiển thị kết quả; không cần gì khác từ ta ngoài contract |
| Auth | Cung cấp danh tính user cho nộp bài và lịch sử | Danh tính user (`user_id`). Hiện chỉ có `CurrentUser` stub ở profile `dev`. Auth thật (JWT + role) là **tiền đề của nộp bài** ở môi trường thật; chưa chốt có làm trong phạm vi này không: `TODO(xác nhận)` |
| Trợ lý AI (hint ladder) | Contract truy vấn: số lần nộp chưa đạt, cờ đã Accepted theo (user, exercise). Ta không viết logic gợi ý | Không cần gì từ họ ở giai đoạn này |
| Admin UI | Dữ liệu cấu hình giới hạn sandbox đọc từ bảng; có endpoint sửa hay không: `TODO(xác nhận)` | Giao diện chỉnh cấu hình (frontend) |

## 7. Tiêu chí nghiệm thu

| Tiêu chí | Cách kiểm chứng | File test chi tiết |
|---|---|---|
| Sáu verdict đúng (SB-03) | Bộ bài nộp mẫu có verdict mong đợi cho từng verdict | 03, 06 |
| N2 | ≥ 30 bài nộp mẫu cho đúng verdict | 06 |
| N4 | Chạy đủ 8 kịch bản tấn công (Bảng 5.9), kết quả đúng kỳ vọng | 05, 06 |
| NF-01 / M3 | Đo thời gian phản hồi mã đơn giản so với mục tiêu khoảng 3 giây | 06 |
| NF-02 | Kiểm thử tải 30 bài nộp song song không mất bài (M5); con số cần xác nhận, xem mục 10 | 06 |
| Không lộ test ẩn / lời giải | Kiểm tra response và log không chứa `expected_output` của test ẩn hay lời giải tham khảo | 05, 06 |
| `./mvnw verify` xanh | CI chạy `./mvnw verify`; test tích hợp chạy khi có `TEST_DB_URL` | 06 |
| OpenAPI cập nhật | `docs/api/openapi.yaml` có endpoint mới, chỉ thêm, không phá endpoint hiện có | 04 |

- Không điền thêm con số nào ngoài những số có trong báo cáo.

## 8. Giả định và rủi ro

| Mã | Nội dung | Ghi chú |
|---|---|---|
| R3 | Sandbox bị lạm dụng/tấn công tài nguyên, tác động Cao | Giảm thiểu bằng NF-04, kiểm chứng bằng N4 (xem 05) |
| R7 | Judge0 khó cài đặt hoặc vận hành thiếu ổn định, tác động Cao | Spike tuần 1–2 (M1) bằng Docker Compose |

- **Phương án dự phòng** (không phải quyết định): dùng bản dịch vụ của Judge0 hoặc chuyển sang Piston nếu Judge0 khó vận hành.
- **Giả định:**
  - Judge0 chạy ổn định trong spike tuần 1–2.
  - Mốc: tuần 3–6 khung backend; tuần 7–10 (M3) tích hợp Judge0, Redis, worker, kiểm thử ≥ 30 bài nộp mẫu; tuần 14–16 (M5) kiểm thử tải và 8 kịch bản tấn công.
  - Chỉ cần Python trong MVP.
  - Có thể thêm Redis vào hạ tầng theo báo cáo dù compose hiện chỉ có PostgreSQL.

## 9. Quy tắc khi yêu cầu nằm ngoài phạm vi

- Yêu cầu **không có trong mục 3**: dừng và hỏi người, không tự mở rộng.
- Yêu cầu nằm ở mục 4: không làm, trừ khi người xác nhận đổi phạm vi và cập nhật file này.
- Không tự dựng auth thật, thêm ngôn ngữ ngoài Python, hay đổi quyết định ở mục 5 khi chưa được xác nhận.
- Không sửa chi tiết kỹ thuật ở `01`–`07` để lách ranh giới phạm vi.
- Gặp xung đột giữa file này và file khác: áp dụng quy tắc ở mục 1.

## 10. Điểm cần xác nhận

- `TODO(xác nhận):` auth thật (JWT + role) có nằm trong phạm vi này không; nếu không thì nộp bài ở môi trường thật dựa vào đâu để có `user_id`.
- `TODO(xác nhận):` có endpoint admin để sửa cấu hình giới hạn sandbox, hay chỉ đọc từ bảng ở giai đoạn này.
- `TODO(xác nhận):` có làm ngôn ngữ ngoài Python trong MVP không (hiện: không).
- `TODO(xác nhận):` con số 30 bài nộp đồng thời (NF-02) là mục tiêu đề xuất trong báo cáo; cần nhóm xác nhận trước khi dùng làm tiêu chí nghiệm thu.
