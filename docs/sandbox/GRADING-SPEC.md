# 03-GRADING-SPEC: Đặc tả quy tắc chấm bài

## 1. Mục đích và phạm vi

- Đây là **đặc tả đề xuất**. Repo chưa có mã chấm bài, worker, Judge0 adapter hay Redis. Mọi thứ dưới đây là thiết kế cần cài đặt.
- Phạm vi: pha 2 (Thực thi) và pha 3 (Tổng hợp) cho SB-02 đến SB-06. MVP: Python, chỉ so khớp stdin/stdout. SB-09 và SB-10 ngoài phạm vi. Xem `00-SCOPE.md`.
- Bảng, cột, vòng đời trạng thái `submission`: `02-DATA-MODEL.md`. Endpoint, DTO: `04-API-CONTRACT.md`. File này không lặp lại.
- Nhãn: "(đề xuất)" là quy tắc không có trong báo cáo. `TODO(xác nhận)` là chưa kiểm chứng, gắn với spike Judge0 tuần 1–2 (rủi ro R7).

## 2. Nguyên tắc chấm

- Hệ thống tự so khớp output, ánh xạ verdict và tính điểm. Judge0 chỉ thực thi: nhận mã + stdin, trả stdout/stderr, status, time, memory.
- Không gửi `expected_output` sang Judge0 (đề xuất). Judge0 chỉ thấy mã và stdin.
- Lỗi hệ thống không đổ cho học viên: không bao giờ thành Wrong Answer hay Runtime Error. Lỗi hệ thống **không phải** verdict thứ bảy.
- Bí mật không rời server: `expected_output` của test ẩn và lời giải tham khảo không xuất hiện trong response, DTO, log.
- Mọi giới hạn lấy từ bảng config (AD-03). Không hard-code trong mã chấm.
- Chỉ worker gọi Judge0. **Không dùng `callback_url`.**
- Đúng 6 verdict: Accepted, Wrong Answer, Time Limit, Memory Limit, Runtime Error, Compile Error.

## 3. Quy trình chấm một submission

Pseudo-code cho pha 2 và 3 (đề xuất). Việc nhận job và đổi trạng thái `submission`: xem `02-DATA-MODEL.md` mục 5.

```
grade(submission):
  limits = đọc sandbox_limit_config          # thiếu key cần dùng -> LỖI HỆ THỐNG
  tests  = đọc test_case của exercise, ORDER BY position
  tests rỗng -> LỖI HỆ THỐNG                 # không chấm, không ra verdict
  results = []
  for t in tests:                            # tuần tự theo position
      run = judge0.execute(code, language, stdin = t.stdin, limits)
      kind, verdict = map_status(run, limits)             # mục 4
      if kind == SYSTEM_ERROR: dừng lượt chấm -> mục 7
      if verdict == COMPILE_ERROR:
          finalize(COMPILE_ERROR, score = 0, results = [kết quả của t])   # DỪNG NGAY
      if verdict == None:                    # Judge0 báo chương trình chạy xong
          truncated = stdout vượt giới hạn output
          ok = (not truncated) and normalize(run.stdout) == normalize(t.expected_output)
          verdict = ok ? ACCEPTED : WRONG_ANSWER
      results.append({t, passed: verdict == ACCEPTED, verdict, time_ms, memory_kb})
  finalize(aggregate_verdict(results), compute_score(results), results)   # mục 6

finalize: ghi MỘT transaction (kết quả theo test, verdict, score, time_ms,
          memory_kb, finished_at), rồi phát sự kiện hoàn tất (Redis, ngoài file này).
```

- Judge0 chỉ nhận mã, stdin và giới hạn (mục 8).
- **Lấy kết quả bằng poll (đề xuất):** gửi `wait=false`, poll theo token tới khi status khác In Queue/Processing.
  - Lý do: tách được "Judge0 không phản hồi" với "đang xử lý". Timeout do worker kiểm soát. Không giữ kết nối HTTP dài. Không phụ thuộc Judge0 có bật `wait`.
  - Đánh đổi: thêm tối đa một khoảng poll mỗi test. `TODO(xác nhận)`: chọn poll hay `wait=true` sau khi đo ở spike.
- Ghi kết quả một lần ở cuối (đề xuất), để retry (mục 7) không để lại kết quả dở dang.
- Đổi đơn vị (đề xuất): `time` của Judge0 (giây) thành `time_ms` số nguyên làm tròn lên. `memory` thành `memory_kb`. `TODO(xác nhận)`: định dạng thực tế của hai trường.
- Compile Error: chỉ ghi kết quả của test vừa chạy. Test chưa chạy không có hàng kết quả (`02-DATA-MODEL.md` mục 4.5).

## 4. Ánh xạ status Judge0 sang verdict

Toàn bộ tên status dưới đây là kiến thức tham khảo, `TODO(xác nhận)` trên phiên bản Judge0 thực tế (kể cả mã số).

| Status Judge0 | Ra gì | Đối chiếu thêm | Ghi chú |
|---|---|---|---|
| In Queue, Processing | Chưa kết thúc | Không | Tiếp tục poll. Quá hạn poll: lỗi hệ thống |
| Accepted | Chưa có verdict: sang so khớp (mục 5) | Đối chiếu bộ nhớ | Nghĩa là "chạy xong, không lỗi" vì không gửi `expected_output` |
| Wrong Answer | **Lỗi hệ thống** | Không | Không gửi `expected_output` nên status này không được xuất hiện. Xuất hiện nghĩa là adapter cấu hình sai |
| Time Limit Exceeded | Time Limit | Không | Giữ nguyên, không đối chiếu bộ nhớ |
| Compilation Error | Compile Error | Không | Dừng chấm. Python thường không có status này (xem dưới) |
| Runtime Error (SIGSEGV, SIGFPE, SIGABRT, NZEC, Other) | Runtime Error | Đối chiếu bộ nhớ; Python có thêm quy tắc SyntaxError | Vượt bộ nhớ có thể bị báo ở dạng này |
| Runtime Error (SIGXFSZ) | Runtime Error | Không | Vượt dung lượng ghi file (mục 8) |
| Internal Error | **Lỗi hệ thống** | Không | Lỗi của Judge0, không phải của học viên |
| Exec Format Error | **Lỗi hệ thống** | Không | Cấu hình ngôn ngữ/runtime sai |
| Status lạ, thiếu trường, JSON hỏng | **Lỗi hệ thống** | Không | Không đoán verdict |

**Đối chiếu Memory Limit (đề xuất):** áp cho status Accepted và nhóm Runtime Error, không áp cho Time Limit Exceeded.
- Nếu `memory_kb >= giới hạn bộ nhớ` đang áp cho lần chạy (lấy từ config) thì verdict là Memory Limit, bất kể tên status.
- Không chỉ dựa vào tên status, vì Judge0 có thể báo vượt bộ nhớ như lỗi thực thi.
- `TODO(xác nhận)`: ngưỡng `>=` hay `>`, độ chính xác của `memory_kb`, và có dùng thêm tín hiệu stderr `MemoryError` không.

**Compile Error cho Python (đề xuất):** Python không có bước biên dịch riêng, nên lỗi cú pháp có thể về dạng Runtime Error (NZEC) thay vì Compilation Error.
- Nếu ngôn ngữ là Python, status thuộc nhóm Runtime Error, và dòng cuối không rỗng của stderr bắt đầu bằng `SyntaxError`, `IndentationError` hoặc `TabError` thì verdict là Compile Error.
- Ca biên chấp nhận: `SyntaxError` ném lúc chạy (ví dụ `eval` chuỗi sai) cũng bị xếp Compile Error.
- `TODO(xác nhận)`: hành vi thực tế của Judge0 với lỗi cú pháp Python.

## 5. So khớp output và chuẩn hoá khoảng trắng

Quy tắc dưới đây là **đề xuất**. Áp cùng một hàm `normalize` cho cả `stdout` và `expected_output`:

1. Decode UTF-8. Byte không hợp lệ thay bằng U+FFFD.
2. Đổi `\r\n` và `\r` đơn lẻ thành `\n`.
3. Xóa space và tab ở cuối mỗi dòng.
4. Xóa mọi `\n` ở cuối văn bản.
5. So sánh bằng nhau từng ký tự, phân biệt hoa thường.

Không làm: xóa khoảng trắng đầu dòng, gộp khoảng trắng giữa dòng, bỏ dòng trống ở giữa, đổi tab thành space, so số thực có sai số (`TODO(xác nhận)`: có cần sau MVP không).

| `stdout` | `expected_output` | Khớp |
|---|---|---|
| `5` | `5\n` | Có |
| `5\n\n\n` | `5` | Có |
| `5 \t\n` | `5\n` | Có |
| `a\r\nb` | `a\nb` | Có |
| (rỗng) | `\n` | Có |
| `a\n\nb` | `a\nb` | Không |
| ` 5` (có space đầu) | `5` | Không |
| `\na` | `a` | Không |
| `a  b` | `a b` | Không |
| `a\tb` | `a b` | Không |
| `A` | `a` | Không |
| `a\u00a0` (NBSP cuối) | `a` | Không |
| (rỗng) | `0` | Không |
| stdout bị cắt vì vượt giới hạn output | bất kỳ | Không (không so khớp) |

## 6. Tính điểm và verdict tổng

```
passedWeight = Σ weight của test có passed = true
totalWeight  = Σ weight của mọi test của bài
score = ROUND_DOWN(max_score × passedWeight / totalWeight, 2)    # BigDecimal, không dùng float
```

- Làm tròn xuống 2 chữ số thập phân (đề xuất): `score = max_score` chỉ khi mọi test đạt, không bị làm tròn lên khi chưa Accepted.
- `totalWeight = 0` (bài không có test): không tính điểm, không chia cho 0, coi là lỗi hệ thống (mục 7).
- Compile Error: `score = 0` (đề xuất). Accepted: `score = max_score`.
- `time_ms`, `memory_kb` mức bài nộp = giá trị lớn nhất trong các test đã chạy (đề xuất). Compile Error: NULL.

**Verdict tổng (đề xuất).** Báo cáo chỉ nêu "Wrong Answer = sai ở ít nhất 1 test", nên cần thứ tự ưu tiên. Chọn verdict có ưu tiên nhỏ nhất trong các test không đạt:

| Ưu tiên | Verdict | Lý do |
|---|---|---|
| (đặc biệt) | Compile Error | Dừng ngay ở test đầu, không cạnh tranh với verdict khác |
| 1 | Runtime Error | Chương trình crash, lỗi chắc chắn |
| 2 | Memory Limit | Vi phạm tài nguyên |
| 3 | Time Limit | Vi phạm tài nguyên |
| 4 | Wrong Answer | Ít thông tin nhất |
| (không có test fail) | Accepted | Chỉ khi mọi test đạt |

- Ví dụ: test 1 Wrong Answer + test 2 Time Limit cho Time Limit. Test 1 Time Limit + test 2 Runtime Error cho Runtime Error.
- Verdict riêng từng test vẫn được lưu nguyên (`02-DATA-MODEL.md` mục 4.5).

**Tiếp tục hay dừng (đề xuất).** Tiếp tục chấm mọi test sau khi một test không đạt. Chỉ Compile Error dừng ngay. Lý do: điểm là tổng trọng số test đạt chia tổng trọng số, dừng sớm làm điểm một phần sai.

**Hệ quả hiệu năng (NF-01, NF-02):**
- Chấm tuần tự nên thời gian chấm ≈ tổng thời gian từng test. Mục tiêu ~3 giây cho mã đơn giản chỉ đạt khi số test ít và khoảng poll ngắn.
- Chương trình lặp vô hạn tốn tối đa (số test × giới hạn thời gian).
- 30 bài nộp đồng thời sinh ≈ 30 × số test lượt gọi Judge0.
- `TODO(xác nhận)`: số test điển hình mỗi bài, thời gian thực đo mỗi lượt gọi, khoảng poll. Cân nhắc batch hoặc chạy song song test trong một bài nếu không đạt NF-01.

## 7. Lỗi hệ thống và retry

**Là lỗi hệ thống:**
- Judge0 không phản hồi (connection refused, timeout) hoặc trả HTTP 5xx.
- HTTP 4xx do adapter gửi sai, JSON hỏng/thiếu trường, status lạ.
- Internal Error, Exec Format Error, Wrong Answer do Judge0 báo (mục 4).
- Poll quá hạn khi vẫn In Queue/Processing. Hạn = giới hạn thời gian + biên (`TODO(xác nhận)`).
- Bài không có test, thiếu key config cần dùng, ghi DB thất bại.

**Không phải lỗi hệ thống:** mọi status ra verdict ở mục 4, kể cả Time Limit.

**Retry (đề xuất, `TODO(xác nhận)` mọi con số):**
- Một test gặp lỗi hệ thống thì hủy cả lượt chấm và chấm lại từ đầu. Idempotent vì kết quả chỉ ghi một lần ở cuối.
- Tối đa 3 lượt, tính cả lượt đầu. Đếm bằng `attempt_count` (`02-DATA-MODEL.md` mục 5).
- Backoff lũy thừa: chờ 2 s trước lượt 2, 4 s trước lượt 3, cộng jitter ±20%. Cơ chế trì hoãn job nằm ở Redis, ngoài file này.
- Hết lượt: trạng thái cuối "lỗi hệ thống" (tên do `02-DATA-MODEL.md` quy định). `verdict` và `score` NULL. Học viên thấy thông báo chung, không có verdict.
- **Không tính** vào số lần nộp chưa đạt của hint ladder. Truy vấn ở `02-DATA-MODEL.md` mục 6 đã loại các bài này.

**Log:**
- Ghi: `submission_id`, `exercise_id`, `attempt_count`, `test_case_id`/`position`, tên và mã status Judge0, HTTP status, thời gian gọi, loại lỗi.
- KHÔNG ghi: `source_code`, `stdin`, `expected_output` (mọi test), stdout/stderr thô, lời giải tham khảo, credential Judge0, body response nguyên văn.

## 8. Giới hạn tài nguyên và hành vi khi vượt

Tên trường Judge0 trong cột 3 là `TODO(xác nhận)`. Không điền giá trị mặc định. `TODO(xác nhận)`: giá trị cho `dev` và môi trường thật.

| Giới hạn | Nguồn | Trường Judge0 (dự kiến) | Khi vượt |
|---|---|---|---|
| Thời gian chạy | Config: thời gian chạy | `cpu_time_limit`, giới hạn wall time | Time Limit |
| Bộ nhớ | Config: bộ nhớ | `memory_limit` | Memory Limit (đối chiếu mục 4) |
| Số tiến trình/luồng | Config: số tiến trình | `max_processes_and_or_threads` | Runtime Error (đề xuất; `TODO(xác nhận)` bằng ca fork bomb) |
| Dung lượng ghi đĩa | Config: dung lượng ghi (đề xuất, cần thêm key) | `max_file_size` | Runtime Error (SIGXFSZ) |
| Độ dài output | Config: độ dài output (đề xuất, cần thêm key) | Không có hoặc `TODO` | Cắt ngắn ở ngưỡng, test không đạt (Wrong Answer) |

- Worker tự cắt stdout/stderr theo ngưỡng output, không chờ Judge0 cắt.
- Tần suất nộp, kích thước mã, số bài chờ tối đa mỗi user thuộc pha 1 (Redis, `02-DATA-MODEL.md`, `04-API-CONTRACT.md`). Worker không xử lý.

## 9. Thông tin lộ ra cho học viên

DTO cụ thể: `04-API-CONTRACT.md`. Theo loại test:

| Thông tin | Test công khai | Test ẩn |
|---|---|---|
| `stdin` | Hiện | Không |
| `expected_output` | Hiện | **Không bao giờ** |
| stdout thực tế | Hiện, cắt theo giới hạn output | Không |
| Kết quả test | Passed/Failed + verdict của test | Chỉ Passed/Failed |
| `time_ms`, `memory_kb` | Hiện | Hiện (đề xuất) |
| stderr/traceback | Hiện, cắt ngắn (đề xuất) | Không (stderr có thể in lại stdin ẩn) |

Theo verdict tổng:

| Verdict | Học viên thấy |
|---|---|
| Accepted, Wrong Answer | Điểm, danh sách Passed/Failed, chi tiết test công khai |
| Time Limit, Memory Limit | Verdict, điểm, `time_ms`/`memory_kb`. Không có stdout của test ẩn |
| Runtime Error | Traceback của test công khai fail đầu tiên. Nếu chỉ lỗi ở test ẩn: chỉ "Runtime Error", không traceback |
| Compile Error | Thông báo lỗi biên dịch/cú pháp (cắt ngắn, bỏ đường dẫn tuyệt đối của sandbox: đề xuất). Test chưa chạy hiện "chưa chạy" |
| Lỗi hệ thống | Thông báo chung, khuyến khích nộp lại, không verdict |

- Thông báo lỗi lưu ở `submission.error_message` (`02-DATA-MODEL.md`). Chỉ chứa stderr của Compile Error hoặc test công khai, không bao giờ của test ẩn.

## 10. Yêu cầu kiểm thử

N2: tối thiểu 30 bài nộp mẫu. Mỗi quy tắc cần ≥ 1 ca. Chỉ nêu loại, không viết ca.

| Quy tắc | Loại ca mẫu cần có |
|---|---|
| Accepted, so khớp output | Đúng hoàn toàn; đúng nhưng thừa dòng trống, CRLF, khoảng trắng cuối dòng |
| Wrong Answer | Sai ở test công khai; sai ở test ẩn; sai chỉ một test |
| Time Limit | Vòng lặp vô hạn; chạy chậm hơn giới hạn |
| Đối chiếu Memory Limit | Cấp phát vô hạn, Judge0 báo Runtime Error kèm memory ≥ giới hạn (ra Memory Limit); Runtime Error kèm memory thấp (ra Runtime Error) |
| Runtime Error | Chia cho 0, exception, exit code khác 0 |
| Compile Error | Thiếu dấu `:`, thụt lề sai. Kiểm tra dừng ngay, điểm 0, test sau không chạy |
| Ưu tiên verdict | Cặp lẫn lộn: Wrong Answer + Time Limit, Time Limit + Runtime Error, ba loại cùng lúc |
| Tiếp tục chấm | Test 1 sai, các test sau đúng, điểm một phần theo trọng số |
| Điểm và làm tròn | Trọng số khác nhau; số thập phân dài; 0 test; điểm gần max không làm tròn lên max |
| Output quá dài | In vô hạn, bị cắt, test không đạt |
| Ghi file lớn, fork bomb | Runtime Error (`TODO(xác nhận)` hành vi thực) |
| Lỗi hệ thống | Judge0 down, timeout, Internal Error, poll quá hạn: không verdict, có retry, hết lượt ra lỗi hệ thống, không tính vào lần nộp chưa đạt |
| Bí mật | Kiểm tra response và log không chứa `expected_output` test ẩn, `stdin` ẩn, lời giải tham khảo, stderr test ẩn |

**Judge0 stub cho CI phải mô phỏng được** (không thiết kế chi tiết ở đây):
- Mỗi status trong bảng mục 4, với stdout, stderr, time, memory điều khiển được.
- Chuỗi In Queue, Processing, kết thúc sau N lần poll.
- Judge0 down (connection refused), timeout (không trả lời), HTTP 5xx, JSON hỏng hoặc thiếu trường, status lạ, kẹt In Queue mãi.
- Stdout rất dài.
- Kiểm tra request gửi tới stub không chứa `expected_output`.

## 11. Điểm cần xác nhận

- `TODO(xác nhận):` phiên bản Judge0, mã và tên status thực tế, tên trường JSON (status, stdout, stderr, compile_output, time, memory, các trường giới hạn), `language_id` của Python. Gắn với spike tuần 1–2 (R7).
- `TODO(xác nhận):` poll hay `wait=true`, khoảng poll, biên cho hạn poll.
- `TODO(xác nhận):` Judge0 báo lỗi cú pháp Python ở status nào, và quy tắc đọc stderr (mục 4) có đủ không.
- `TODO(xác nhận):` cách Judge0 báo vượt bộ nhớ trên cấu hình thực, ngưỡng đối chiếu, tín hiệu bổ sung.
- `TODO(xác nhận):` số lượt retry tối đa, backoff, jitter.
- `TODO(xác nhận):` giá trị mặc định mọi giới hạn ở mục 8.
- `TODO(xác nhận):` quy tắc chuẩn hoá khoảng trắng cuối cùng (kể cả số thực) và thứ tự ưu tiên verdict (mục 5, 6).
- `TODO(xác nhận):` giới hạn thời gian/bộ nhớ theo từng bài hay chung (báo cáo nhắc "giới hạn của bài tập"; `02-DATA-MODEL.md` cũng hỏi).
- `TODO(xác nhận):` `02-DATA-MODEL.md` chưa có key config cho độ dài output và dung lượng ghi đĩa, cần bổ sung.
- `TODO(xác nhận):` `02-DATA-MODEL.md` mục 5 chỉ cho job quét đưa `RUNNING` về `QUEUED`. Cần xác nhận worker có được trả bài về `QUEUED` sau lỗi hệ thống để retry theo backoff không.
- `TODO(xác nhận):` số test điển hình mỗi bài và thời gian thực đo cho NF-01/NF-02; có cần batch hoặc chạy song song test.
- `TODO(xác nhận):` có chấp nhận hiện `time_ms`/`memory_kb` của test ẩn không; cách bỏ đường dẫn sandbox khỏi thông báo lỗi.
