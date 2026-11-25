- **Mục đích** Dùng nhất quán từ nghiệp vụ và tên kỹ thuật giữa code, hợp đồng và tài liệu.
- **Đối tượng đọc** Dev, reviewer và người tích hợp frontend.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Thuật ngữ dự án

| Thuật ngữ | Nghĩa trong dự án |
| --- | --- |
| Roadmap / node | Lộ trình học và một chủ đề trên lộ trình, có thể trỏ đến nội dung học. |
| Course / lesson | Khóa học / bài học. Slug bài duy nhất trong một khóa. |
| Run / submission | Lượt chạy thử / bài nộp được lưu để chấm và tra cứu. |
| Verdict | Kết quả nghiệp vụ chấm, ví dụ `ACCEPTED` hoặc `WRONG_ANSWER`. |
| Test ẩn | Test chấm có dữ liệu giữ ở backend, chỉ công bố kết quả được phép. |
| Hint ladder | Bậc gợi ý AI xác định từ trạng thái học và số bài nộp hợp lệ trước đó. |
| AuthN / AuthZ | Xác định chủ thể / quyết định chủ thể được làm gì trên tài nguyên. |
| DTO / ADR / SSE | Kiểu dữ liệu ở biên / bản ghi quyết định kiến trúc / luồng sự kiện một chiều từ server. |
| Lease | Quyền xử lý tạm thời của worker, hết hạn để công việc có thể được nhận lại. |
| Request ID | Mã tra cứu một request, xuất hiện ở `X-Request-Id` và `traceId` của lỗi API. |

Giữ cùng thuật ngữ trong tên field, OpenAPI và test để frontend đọc đúng ý nghĩa. Ví dụ Java minh họa cho module chấm.

```java
// ✅ Tách trạng thái xử lý khỏi kết quả chấm.
SubmissionStatus status = SubmissionStatus.COMPLETED;
Verdict verdict = Verdict.ACCEPTED;
// ❌ Dùng SubmissionStatus.ACCEPTED làm trạng thái xử lý.
```

[API](API_GUIDELINES.md) · [Kiến trúc](ARCHITECTURE.md) · [Roadmap](ROADMAP.md)

## Checklist

- [ ] Tên mới mang cùng nghĩa trong code, OpenAPI và UI.
- [ ] Trạng thái xử lý và kết quả nghiệp vụ có tên riêng.
