- **Mục đích** Chuẩn bị một PR có thể review và tái lập.
- **Đối tượng đọc** Dev và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Đóng góp vào backend

1. Từ một issue, ghi hành vi cần đổi và ít nhất một điều kiện nghiệm thu, để reviewer có cơ sở kiểm tra.
2. Tạo branch theo Git workflow. Làm một thay đổi nghiệp vụ mỗi PR để review và revert theo phạm vi đó.
3. Chạy kiểm tra phù hợp. Cùng PR, cập nhật hợp đồng API, migration, ADR hay changelog khi thay đổi chạm đến nội dung tương ứng.
4. Trong PR, ghi môi trường, lệnh, kết quả và cách thử lỗi liên quan. Đánh dấu `N/A` kèm lý do cho mục checklist không áp dụng.

```powershell
# ✅ Từ backend/, với DB test nếu thay đổi chạm PostgreSQL.
.\mvnw.cmd verify
# ❌ Dùng riêng .\mvnw.cmd compile làm kết quả nghiệm thu hành vi.
```

[Mẫu PR](.github/PULL_REQUEST_TEMPLATE.md) · [Git workflow](docs/GIT_WORKFLOW.md) · [Kiểm thử](docs/TESTING_STRATEGY.md) · [Changelog](CHANGELOG.md)

## Checklist

- [ ] Có issue hoặc điều kiện nghiệm thu trong PR.
- [ ] Có bằng chứng kiểm thử và tài liệu liên quan đã cập nhật.
