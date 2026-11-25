- **Mục đích** Bao phủ hành vi thay đổi và kiểm tra tích hợp trên PostgreSQL.
- **Đối tượng đọc** Dev và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Chiến lược kiểm thử

| Tầng | Khi dùng | Công cụ |
| --- | --- | --- |
| Unit, nhiều nhất | Quy tắc và nhánh lỗi của service | JUnit, fake hoặc mock ở biên I/O |
| MVC và integration | Validation, auth, status, SQL, constraint và migration | MockMvc, Spring Boot Test, PostgreSQL 16 |
| End-to-end, ít nhất | Luồng MVP qua frontend, trạng thái sau reload và gián đoạn | Smoke staging và kịch bản nghiệm thu |

- Đặt lớp `<Subject>Test` hoặc `<Subject>IntegrationTest`. Tên method nêu hành vi và điều kiện để đọc được lỗi từ CI.
- Với mỗi nghiệp vụ thay đổi, kiểm thử đường thành công và các nhánh lỗi/biên bị tác động. Test route riêng tư gồm thiếu phiên, sai role và sai owner. Test migration gồm schema trống và nâng cấp.
- Mock Judge0/LLM ở port trong unit test. Integration test SQL dùng PostgreSQL thật. Kiểm tra kết nối vendor bằng smoke staging riêng để CI ổn định.
- Hiện `verify` có thể skip integration khi thiếu `TEST_DB_URL`. Dùng DB test riêng và đối chiếu số test skip. Coverage đánh giá theo nhánh nghiệp vụ trong PR. Repo chưa có JaCoCo hay ngưỡng phần trăm.
- `AuthServiceTest` kiểm tra quy tắc service. `AuthIntegrationTest` chạy HTTP trên cổng ngẫu nhiên với PostgreSQL để kiểm tra cookie thực, CSRF, lưu phiên và thu hồi phiên. `CatalogIntegrationTest` dùng MockMvc cho hợp đồng catalog.
- Dùng tải pilot 30 lượt nộp đồng thời và bộ 50 câu đánh giá AI khi các module tương ứng sẵn sàng. [Roadmap và tiêu chí nghiệm thu](ROADMAP.md)

```java
// ✅ Kiểm tra hành vi ẩn bản nháp ở biên HTTP.
mvc.perform(get("/api/v1/courses/draft-fixture"))
        .andExpect(status().isNotFound());
// ❌ Chỉ dùng assertNotNull(catalogController) để nghiệm thu quy tắc xuất bản.
```

[Cấu hình DB kiểm thử](ENVIRONMENT_SETUP.md) · [Coding standards](CODING_STANDARDS.md) · [Bảo mật](AUTH_SECURITY.md)

## Checklist

- [ ] Test thất bại khi bỏ hành vi được bảo vệ.
- [ ] Đã ghi số test đạt, lỗi, skip và DB dùng để chạy.
