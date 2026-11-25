- **Mục đích** Ghi thay đổi đáng chú ý cho người dùng, bên tích hợp và vận hành.
- **Đối tượng đọc** Dev, bên tích hợp và người chuẩn bị release.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Changelog

## [Unreleased]

### Added

- Bộ tài liệu kiến trúc, phát triển và vận hành cho backend.
- Đăng ký và đăng nhập bằng email, xem hồ sơ và đăng xuất bằng phiên lưu trong PostgreSQL.
- Cấu hình format, lint, kiểm tra Conventional Commit và đóng gói backend bằng Docker.
- Sandbox Python gồm API bài tập, nộp bài, polling, lịch sử, Redis và worker Judge0.

### Fixed

- Flyway cấp mặc định Sandbox ngoài profile dev và giữ cấu hình đã tùy chỉnh.
- Worker giới hạn body Judge0 trước khi giải mã, và gia hạn lease theo từng test case để tránh recovery nhầm.

## Quy ước ghi mục

Đưa thay đổi đáng chú ý vào `Unreleased`, chia theo nhóm có nội dung: `Added`, `Changed`, `Deprecated`, `Removed`, `Fixed`, `Security`. Khi phát hành, tạo mục `[version] - YYYY-MM-DD` theo tag thực, phiên bản mới ở trên. Thêm link so sánh khi có đủ tag để truy vết. [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) · [Git workflow](docs/GIT_WORKFLOW.md) · [Deploy](docs/DEPLOYMENT.md)

Ví dụ minh họa, chỉ ghi mục tương ứng khi thay đổi đã có.

```text
✅ - API danh mục trả 400 khi size lớn hơn 50.
   Kiểm tra: Invoke-RestMethod 'http://127.0.0.1:8080/api/v1/courses?size=51'
❌ - Sửa CatalogController.java.
```

## Checklist

- [ ] Mỗi mục nêu hành vi hoặc tác động.
- [ ] Phiên bản và ngày phát hành khớp tag thật.
