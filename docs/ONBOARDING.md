- **Mục đích** Đưa dev mới từ chạy local đến PR đầu tiên có review.
- **Đối tượng đọc** Dev mới và người hướng dẫn.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Onboarding backend trong hai tuần

| Mốc | Việc cần làm | Kết quả |
| --- | --- | --- |
| Ngày 1 | Chạy local, mở OpenAPI, gọi readiness và API bài mẫu. | Gửi cho reviewer status và slug đọc được. |
| Ngày 2–3 | Lần theo `CatalogController → CatalogService → CatalogRepository`. Chạy integration với DB riêng. | Chỉ ra rule ẩn bản nháp, SQL và test liên quan. |
| Ngày 4–5 | Đọc ADR, mở một PR nhỏ từ issue nhóm giao và ghi cách kiểm tra. | Nhận review từ một thành viên khác. |
| Tuần 2 | Cùng reviewer chốt một thay đổi nhỏ trên module thực, đi từ hợp đồng đến test. | Giải thích quyền truy cập, migration nếu có, và ảnh hưởng triển khai. |

```powershell
# ✅ Chạy một bài đọc mẫu từ dữ liệu mà Flyway đã nạp.
Invoke-RestMethod http://127.0.0.1:8080/api/v1/courses/python-co-ban/lessons/xin-chao-python
# ❌ Dùng riêng health response làm bằng chứng đã chạy được luồng bài học.
```

[README](../README.md) · [Môi trường local](ENVIRONMENT_SETUP.md) · [Kiến trúc](ARCHITECTURE.md) · [Đóng góp](../CONTRIBUTING.md)

## Checklist

- [ ] Đã chạy API bài học và integration test với PostgreSQL.
- [ ] Đã có PR đầu tiên với hướng dẫn tái lập và phản hồi review.
