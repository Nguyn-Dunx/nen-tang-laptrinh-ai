- **Mục đích** Đặt file theo module và theo trách nhiệm.
- **Đối tượng đọc** Dev và AI coding agent.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Cấu trúc thư mục

| Vị trí | Nội dung |
| --- | --- |
| `backend/src/main/java/vn/edu/codelearn/platform/<module>/` | Controller, service, repository, model và adapter của nghiệp vụ. |
| `backend/src/main/java/vn/edu/codelearn/platform/common/` | Health, security config, filter và lỗi HTTP dùng chung. |
| `backend/src/main/resources/db/migration/` | Flyway migration cho schema dùng chung. |
| `backend/src/main/resources/db/demo/` | Seed chỉ nạp ở profile `dev` và test chỉ định rõ. |
| `backend/src/test/java/` | Test theo package của module. Giữ `CatalogIntegrationTest` hiện có tại package gốc. |
| `backend/src/test/resources/` | Fixture kiểm thử tái lập được. |
| `infra/`, `.github/`, `docs/` | Hạ tầng local, workflow/mẫu PR, tài liệu và hợp đồng. |

Đặt file cùng package nghiệp vụ trước. Chỉ tạo package con cho một nhóm có trách nhiệm rõ và có nhiều file, để người đọc tìm được toàn bộ luồng từ endpoint đến DB. Mặc định để lớp triển khai package-private. Công khai type khi nó là hợp đồng giữa module hoặc framework cần truy cập.

Module mẫu `identity` đi từ `AuthController` qua `AuthService`, `AccountRepository` đến `Account` và `AuthDtos`. Xem test `AuthServiceTest`, `AuthIntegrationTest` và migration `V2__create_identity.sql` khi thêm module tương tự.

```java
// ✅ CatalogService.java đặt cùng controller và repository của catalog.
package vn.edu.codelearn.platform.content;
// ❌ package vn.edu.codelearn.platform.services;
```

[Kiến trúc](ARCHITECTURE.md) · [Coding standards](CODING_STANDARDS.md) · [Dữ liệu](DATABASE.md)

## Checklist

- [ ] Đường dẫn file khớp package và tên type.
- [ ] Test và fixture nằm trong cây `src/test`.
