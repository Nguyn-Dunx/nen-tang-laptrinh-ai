- **Mục đích** Dùng pattern khi có trách nhiệm rõ trong luồng hiện tại.
- **Đối tượng đọc** Dev và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Design patterns của backend

| Pattern | Khi dùng | Quy tắc |
| --- | --- | --- |
| Service Layer | Use case gọi dữ liệu hoặc cần transaction | Một service điều phối use case, để controller tập trung vào HTTP. |
| Repository | Module đọc hoặc ghi PostgreSQL | Gom SQL theo module và bind giá trị bằng `JdbcClient` để dễ kiểm tra truy vấn. |
| DTO | Hợp đồng HTTP hoặc biên module | Chỉ đưa các trường đã có chủ đích công khai vào record. |
| Port và Adapter | Tích hợp Judge0 hoặc LLM | Module nghiệp vụ sở hữu interface, adapter chứa HTTP/SDK. Test nghiệp vụ dùng fake qua interface. |

Chỉ thêm abstraction khi cần cô lập cơ chế I/O, biểu diễn hợp đồng nghiệp vụ hoặc có ít nhất hai cách triển khai thực tế hay trong test. Với luồng nội bộ đơn giản, gọi trực tiếp service cần dùng để giảm lớp trung gian. Giữ abstraction cần cho các hợp đồng module đã định trong kiến trúc.

```java
// ✅ Hợp đồng thuộc module submission. Kiểu dữ liệu SDK ở adapter.
interface JudgeGateway {
    JudgeResult execute(JudgeRequest request);
}
// ❌ Service chấm import SDK rồi tự dựng client và xử lý HTTP ở mỗi use case.
```

[Kiến trúc](ARCHITECTURE.md) · [Coding standards](CODING_STANDARDS.md) · [Kiểm thử](TESTING_STRATEGY.md)

## Checklist

- [ ] Mỗi wrapper có lý do và điểm sử dụng cụ thể.
- [ ] Test service có thể chạy độc lập với nhà cung cấp bên ngoài.
