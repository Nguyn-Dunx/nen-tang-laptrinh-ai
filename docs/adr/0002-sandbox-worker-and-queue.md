- **Mục đích** Ghi quyết định bố trí worker và hàng đợi ở giai đoạn triển khai Sandbox.
- **Đối tượng đọc** Backend và vận hành.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Sandbox và DevOps

# Tách tiến trình chấm bài trong cùng backend

**Trạng thái** Implemented · **Phạm vi** Sandbox Coding · [ADR nền tảng](0001-modular-monolith.md)

API và worker dùng cùng mã, Maven build và image. Worker bật bằng profile `worker` và không mở cổng HTTP. API chạy Flyway. Worker bắt đầu sau khi migration hoàn tất. Dự kiến deploy Judge0 trên một máy/VM riêng, chỉ worker được kết nối.

PostgreSQL giữ mã nguồn, trạng thái, attempt, lease và verdict. Redis phục vụ rate limit và gửi ID sau commit. Worker đọc PostgreSQL để tiếp tục xử lý khi tín hiệu Redis bị mất. Recovery kiểm tra hạn lease tại thời điểm cập nhật. Chỉ attempt đang sở hữu bài được gia hạn hoặc ghi kết quả.

Phương án chạy worker chung API ít tiến trình hơn. Tách tiến trình giúp kiểm soát tài nguyên và quyền truy cập Judge0 độc lập, đổi lại cần giám sát thêm worker và Redis. Giữ polling đến khi đo đủ nhu cầu SSE. [Kiến trúc](../ARCHITECTURE.md) · [Triển khai](../DEPLOYMENT.md)

## Checklist

- [ ] API và worker dùng chung schema đã migrate.
- [ ] Kiểm tra mất Redis, restart worker và ghi verdict đúng attempt.
- [ ] Xác minh cách ly Judge0 và tải pilot trước khi nhận mã thật.
