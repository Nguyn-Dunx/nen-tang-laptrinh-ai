- **Mục đích** Thực hiện thay đổi đúng phạm vi và có bằng chứng kiểm tra.
- **Đối tượng đọc** AI coding agent và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Hướng dẫn cho AI coding agent

- Đọc `git status --short` trước khi sửa để nhận diện phần việc đang có. Giữ chỉnh sửa của người khác trong diff.
- Đọc README, ADR và tài liệu của module trước khi chỉnh. Dùng package hiện có và helper có cùng trách nhiệm để giữ mã dễ dò.
- Dùng Wrapper từ `backend/`. Khi sửa SQL, repository hoặc security filter, chạy `verify` với DB kiểm thử và kiểm tra kết quả nhóm integration.
- Chạy `spotless:apply` khi sửa Java. `verify` kiểm tra format và Checkstyle.
- Cập nhật OpenAPI cùng thay đổi hợp đồng. Thêm migration mới theo thứ tự nếu schema đổi. Lý do và lệnh kiểm tra đi cùng PR.
- Cấm đưa secret, dữ liệu thật, thư mục `target/` hoặc output sinh tự động ngoài yêu cầu vào commit. Giữ deploy, push và tạo PR trong phạm vi được giao.
- Trong bàn giao, ghi file đổi, bước đã kiểm tra, số test bị skip nếu có, và điểm `[CẦN XÁC NHẬN]` có ảnh hưởng.

```powershell
# ✅ Từ repo root, sau khi cấu hình TEST_DB_* theo tài liệu local.
git status --short
Set-Location backend
.\mvnw.cmd verify
# ❌ Dùng .\mvnw.cmd -DskipTests verify làm bằng chứng đã test.
```

[README](README.md) · [Coding standards](docs/CODING_STANDARDS.md) · [Cài đặt local](docs/ENVIRONMENT_SETUP.md) · [ADR kiến trúc](docs/adr/0001-modular-monolith.md)

## Checklist

- [ ] Diff thuộc phạm vi yêu cầu và giữ các thay đổi có sẵn.
- [ ] Kiểm tra đã chạy hoặc có nêu rõ điều kiện còn thiếu.
