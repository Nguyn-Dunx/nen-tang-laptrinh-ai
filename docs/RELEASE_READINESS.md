- **Mục đích** Ghi kết quả tự rà soát bộ khung và việc cần chốt trước pilot.
- **Đối tượng đọc** Dev, reviewer và người phụ trách triển khai.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend và Sandbox, AI, DevOps

# Tự rà soát bộ khung backend

## Tính nhất quán

- Một Spring Boot build đóng gói API và worker. Catalog, Identity, Sandbox và Submission đã có code. Judge0 thật và LLM cần nghiệm thu theo mốc tương ứng. [Kiến trúc](ARCHITECTURE.md) · [ADR 0002](adr/0002-sandbox-worker-and-queue.md)
- OpenAPI khớp các route tài khoản, lỗi HTTP, cookie và quy tắc độ dài mật khẩu theo Unicode code point. Roadmap đã cập nhật phần tài khoản hoàn thành. [OpenAPI](api/openapi.yaml) · [Roadmap](ROADMAP.md)
- Workflow theo nhánh đích `master`. Nhánh tính năng đi qua PR để giữ bước review. [Git workflow](GIT_WORKFLOW.md)

## Cách thực thi quy ước

| Kiểm tra | Cơ chế |
| --- | --- |
| Format, import, độ dài method | Spotless và Checkstyle qua `mvn verify`. |
| Hành vi API, SQL và phiên | Unit và integration test. CI cấp PostgreSQL 16. Local cần `TEST_DB_URL` để tránh skip integration. |
| Tiêu đề commit và PR | Hook khi đã bật trong clone. CI kiểm tra tiêu đề PR. |
| Ranh giới module, OpenAPI phải đổi cùng API, quyền sở hữu khi mở route mới | PR review và test theo từng thay đổi. Chưa có architecture test hay contract diff làm required check. |
| Branch protection, coverage, khôi phục backup và chỉ tiêu pilot | Quản trị repo cấu hình protection. Nhóm đo và ghi bằng chứng khi chạy staging/pilot. |

```powershell
# ✅ Từ backend/, đặt TEST_DB_* theo tài liệu môi trường trước khi chạy.
.\mvnw.cmd verify
# ❌ Dùng kết quả verify có integration bị skip để nghiệm thu SQL hoặc session.
```

[Kiểm thử](TESTING_STRATEGY.md) · [Môi trường local](ENVIRONMENT_SETUP.md) · [Deploy](DEPLOYMENT.md)

## Cần xác nhận và rủi ro

- **[CẦN XÁC NHẬN]** Ngày bàn giao, chỉ tiêu nội dung, ngân sách VPS/LLM và origin triển khai frontend. Các mục tiêu tải pilot cần điều kiện đo và người nghiệm thu.
- **[CẦN XÁC NHẬN]** Host riêng cho Judge0, giới hạn chi phí LLM, nơi cảnh báo và thời gian lưu dữ liệu. Kiểm tra cô lập runner và chính sách dữ liệu trước khi bật tính năng tương ứng. [Bảo mật](AUTH_SECURITY.md) · [Observability](OBSERVABILITY.md)
- **Đăng nhập công khai.** Thêm hạn mức trước khi mở endpoint cho người dùng thật. Đăng ký trả `409` cho email trùng, vì vậy cần đánh giá khả năng dò tài khoản cùng chính sách xác minh và khôi phục tài khoản.
- **Dữ liệu và khả dụng.** PostgreSQL giữ dữ liệu học, phiên và trạng thái chấm. Redis hỗ trợ queue và quota. Theo dõi tuổi bài chờ, retry và độ trễ API. Diễn tập restore, Redis restart và worker restart trên staging trước pilot. [Hiện trạng](PROJECT_STATUS.md)

## Checklist

- [ ] API, dữ liệu cũ, image và CI có bằng chứng kiểm tra cho bản bàn giao.
- [ ] Các điều kiện mở pilot và người chịu trách nhiệm đã được xác nhận.
