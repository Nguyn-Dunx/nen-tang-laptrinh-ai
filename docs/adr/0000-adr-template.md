- **Mục đích** Ghi quyết định có lý do, hệ quả và điều kiện xem xét lại.
- **Đối tượng đọc** Tác giả ADR và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Mẫu Architecture Decision Record

Sao chép file thành `NNNN-<slug>.md` với số kế tiếp. Thay metadata ở đầu, ghi ngày quyết định riêng nếu khác ngày sửa, rồi chọn trạng thái `Proposed`, `Accepted`, `Superseded` hoặc `Rejected`. Giữ lịch sử bằng link tới ADR mới khi thay thế.

## Bối cảnh

<Nêu yêu cầu, ràng buộc, dữ liệu đang có và điểm cần xác nhận.>

## Quyết định và lựa chọn

<Nêu quyết định, ít nhất một phương án đã cân nhắc, trade-off và lý do.>

## Hệ quả và điều kiện xem xét lại

<Ghi chi phí, rủi ro, cách kiểm tra và tín hiệu cần đổi quyết định.>

```java
// ✅ Ví dụ bằng chứng để kiểm tra quyết định dùng JdbcClient.
jdbc.sql("SELECT title FROM course WHERE slug = :slug").param("slug", slug);
// ❌ Chỉ ghi "SQL cho dễ" mà thiếu ví dụ và cách kiểm tra.
```

[ADR mẫu đã duyệt](0001-modular-monolith.md) · [Kiến trúc](../ARCHITECTURE.md)

## Checklist

- [ ] Có trạng thái, lý do, phương án thay thế, hệ quả và cách kiểm tra.
- [ ] Quyết định thay thế có liên kết hai chiều.
