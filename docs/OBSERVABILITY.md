- **Mục đích** Tra được lỗi API và phát hiện sự cố chấm bài, AI hoặc DB.
- **Đối tượng đọc** Dev và vận hành.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Sandbox, AI và DevOps

# Log metrics và cảnh báo

- **Hiện có** `X-Request-Id` do backend tạo. Dùng giá trị đó dò `traceId` trong lỗi API và MDC. Đây là mã tương quan của request. Liveness và readiness có tại `/actuator/health/liveness` và `/actuator/health/readiness`, readiness kiểm tra DB.
- Log lỗi bất ngờ ở biên xử lý một lần, với operation, kết quả và mã tương quan. Loại token, mật khẩu, mã người học, prompt và dữ liệu test ẩn khỏi log để giảm rủi ro lộ dữ liệu.
- **Dự kiến** đo p95 latency, tỷ lệ 5xx, tuổi bài nộp cũ nhất còn chờ, lỗi Judge0, thời gian tới token đầu, lỗi LLM và token sử dụng. Dùng label hữu hạn như route template hoặc outcome. Đưa request ID và submission ID vào log thay vì nhãn metric.
- Trước pilot, ghi ngưỡng, cửa sổ đo, người nhận và cách xử lý cho API lỗi, hàng đợi chậm và token vượt ngân sách. `[CẦN XÁC NHẬN]` Ngưỡng cảnh báo, công cụ thu thập, retention log và nơi nhận. Bổ sung distributed tracing khi có tiến trình độc lập cần truy vết xuyên dịch vụ.

```java
// ✅ Log trạng thái chấm bằng tham số. MDC gắn traceId nếu có.
log.info("Grading finished submissionId={} verdict={}", submissionId, verdict);
// ❌ log.info("Grading payload={}", judgeRequest);
```

[API](API_GUIDELINES.md) · [Bảo mật](AUTH_SECURITY.md) · [Deploy](DEPLOYMENT.md)

## Checklist

- [ ] Dò được request lỗi bằng `X-Request-Id`.
- [ ] Log đã lọc dữ liệu nhạy cảm và cảnh báo có người nhận.
