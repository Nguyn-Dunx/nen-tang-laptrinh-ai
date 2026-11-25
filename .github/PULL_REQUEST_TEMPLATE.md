- **Mục đích** Ghi hành vi, bằng chứng và ảnh hưởng trước khi merge.
- **Đối tượng đọc** Tác giả PR và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Pull request backend

## Thay đổi và cách kiểm tra

<!-- Ghi issue, điều kiện nghiệm thu, lệnh đã chạy, kết quả và test bị skip. Với mục không áp dụng, ghi N/A kèm lý do. -->

```text
✅ Từ backend/: .\mvnw.cmd verify với DB test. Kết quả: <passed>, <failed>, <skipped>.
❌ Maven OK.
```

## Tác động khi đưa lên môi trường dùng chung

<!-- Ghi route hoặc cấu hình thay đổi, migration, và thao tác rollback nếu có. -->

[Đóng góp](../CONTRIBUTING.md) · [Kiểm thử](../docs/TESTING_STRATEGY.md) · [Deploy](../docs/DEPLOYMENT.md)

## Checklist

- [ ] PR có điều kiện nghiệm thu và kết quả kiểm tra.
- [ ] Đã cập nhật OpenAPI, migration, ADR và changelog ở các mục áp dụng.
- [ ] Đã rà dữ liệu riêng, secret và tác động khi rollback.
