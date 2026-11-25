- **Mục đích** Giữ lịch sử dễ review và phục hồi.
- **Đối tượng đọc** Dev, reviewer và quản trị repo.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Git workflow

- Branch đích hiện là `master`. Tạo `feat/<slug>`, `fix/<slug>` hoặc `docs/<slug>` từ bản mới nhất để liên kết scope với issue.
- Dùng Conventional Commits cho commit mới và tiêu đề squash. Chọn `feat`, `fix`, `docs`, `refactor`, `test`, `build`, `ci` hoặc `chore` theo ý định. [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)
- Merge qua PR sau ít nhất một người khác duyệt và các check áp dụng đã đạt. Dùng squash merge để mỗi PR có một điểm revert. Với PR chỉ sửa tài liệu, ghi kết quả rà link và nội dung.
- Workflow chạy `push` vào `master` và cho PR. Hook `commit-msg` và job kiểm tra tiêu đề PR dùng cùng quy tắc. [Workflow](../.github/workflows/backend-ci.yml) · [Hook](../.githooks/commit-msg)
- Quản trị repo xác nhận branch protection và required checks trước khi đánh dấu các điều kiện merge đã được GitHub thực thi.

```bash
# Bật hook từ repo root.
git config --local core.hooksPath .githooks
# ✅ Branch và commit có scope rõ.
git switch -c docs/backend-architecture
git commit -m "docs(backend): document architecture decisions"
# ❌ git commit -m "update"
```

[Đóng góp](../CONTRIBUTING.md) · [Mẫu PR](../.github/PULL_REQUEST_TEMPLATE.md) · [Changelog](../CHANGELOG.md)

## Checklist

- [ ] Tiêu đề mô tả thay đổi và đúng quy ước.
- [ ] Review của người thứ hai và check áp dụng đã đạt.
