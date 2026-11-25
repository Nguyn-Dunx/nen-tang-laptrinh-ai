- **Mục đích** Giữ hợp đồng backend ổn định cho frontend.
- **Đối tượng đọc** Dev backend, frontend và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Quy ước REST và SSE

- API nghiệp vụ dùng `/api/v1`, tên resource số nhiều và URL phân cấp theo tài nguyên. Cập nhật [OpenAPI](api/openapi.yaml) trong cùng PR nếu đổi path, schema, status hoặc quyền truy cập.
- Trả DTO trực tiếp. Dùng `200` khi đọc, `201` kèm `Location` khi tạo đồng bộ, `202` khi đã lưu yêu cầu xử lý nền và `204` khi thành công không có body. Giữ GraphQL ngoài phạm vi MVP để dùng một hợp đồng.
- Danh mục khóa dùng `page` từ 0, `size` từ 1 đến 50, mặc định 20, sắp theo slug. Response gồm `items`, `page`, `size`, `totalElements`. Link bài học giữ cả slug khóa và slug bài. CORS cho phép `GET` với catalog và `GET`/`POST` có credential với `/api/v1/auth/**`, từ các origin đã cấu hình.
- Lỗi dùng `application/problem+json`, có `code` ổn định và `traceId` khớp `X-Request-Id`. Nội dung chưa xuất bản trả `404`. Hồ sơ thiếu phiên hoặc đăng nhập sai trả `401`. Thiếu CSRF, thao tác trái quyền và route chưa mở trả `403`. [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457.html)
- Thêm trường tương thích vào v1. Đổi ngữ nghĩa, bỏ trường hoặc đổi kiểu cần kế hoạch chuyển version và thống nhất với frontend trước merge.
- Dự kiến `submission` có polling trạng thái trước SSE. Hợp đồng SSE phải nêu event, kết thúc, lỗi sau khi stream mở và cách tải lại trạng thái. Tra cứu hai kênh đều kiểm tra quyền sở hữu.

```http
# ✅ API hiện có, đủ slug để xác định bài học.
GET /api/v1/courses/python-co-ban/lessons/xin-chao-python
# ❌ GET /api/v1/lessons/xin-chao-python
```

[Bảo mật](AUTH_SECURITY.md) · [Observability](OBSERVABILITY.md)

## Checklist

- [ ] OpenAPI khớp response, status và quyền đã kiểm thử.
- [ ] Client hiện tại vẫn đọc được endpoint hoặc đã có kế hoạch chuyển version.
