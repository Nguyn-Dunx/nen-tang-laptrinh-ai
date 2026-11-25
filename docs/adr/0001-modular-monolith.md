- **Mục đích** Ghi quyết định về đơn vị deploy và ranh giới module.
- **Đối tượng đọc** Dev, reviewer và vận hành.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Chọn Modular Monolith cho backend

**Trạng thái** Accepted · **Ngày quyết định** 2026-11-25

## Bối cảnh

Nhóm có ba người. Repo đã có một ứng dụng Spring Boot và catalog PostgreSQL. Luồng MVP cần thêm tài khoản, tiến độ, chấm bài bất đồng bộ và trợ lý AI.

## Quyết định

Dùng một Maven build và một tiến trình Spring Boot cho API và worker giai đoạn đầu. Chia package theo nghiệp vụ, với API Java công khai có chủ đích và triển khai nội bộ. PostgreSQL phục vụ các module thông qua bảng có chủ sở hữu. Judge0 chạy ở môi trường cô lập riêng.

## Lựa chọn và hệ quả

- Chia package theo tầng giảm thiết lập ban đầu, nhưng ranh giới dữ liệu giữa nghiệp vụ khó kiểm tra khi luồng học mở rộng.
- Microservices hỗ trợ scale từng dịch vụ, đồng thời tăng pipeline, giao tiếp mạng và ca lỗi vận hành.
- Modular Monolith cho phép tích hợp sớm trên nền repo đang có. Đổi lại, API và worker chia tài nguyên tiến trình. Đo tải trước và xem xét tách worker nếu tải chấm cản trở API hoặc cần scale độc lập.

```java
// ✅ Module learning gọi hợp đồng đọc của module content.
var lesson = lessonContextReader.getPublishedLesson(courseSlug, lessonSlug);
// ❌ Module learning import CatalogRepository để truy cập bảng content.
```

[Kiến trúc](../ARCHITECTURE.md) · [Cấu trúc thư mục](../FOLDER_STRUCTURE.md) · [Mẫu ADR](0000-adr-template.md)

## Checklist

- [ ] Có một build ứng dụng và các module nghiệp vụ có owner.
- [ ] Ranh giới chạy mã được xác minh trước khi nhận bài thật.
