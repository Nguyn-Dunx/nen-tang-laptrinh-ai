- **Mục đích** Giữ mã nhất quán, dễ review và dễ kiểm tra.
- **Đối tượng đọc** Dev và reviewer.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Quy chuẩn Java và Spring Boot

- Dùng `PascalCase` cho type, `camelCase` cho method/biến và `UPPER_SNAKE_CASE` cho hằng. Dùng suffix `Controller`, `Service`, `Repository` theo vai trò để tìm lớp nhanh.
- Dùng constructor injection và dependency `final`. Dùng DTO `record` cho dữ liệu bất biến, Bean Validation ở đầu vào HTTP, và mapper ở biên cần chuyển đổi.
- Mỗi method tối đa 40 dòng thân hàm tính theo dòng có mã. Nếu một luồng cần vượt, ghi lý do ở PR để reviewer kiểm tra trách nhiệm và cách bao phủ test.
- Controller xử lý HTTP, service giữ quy tắc nghiệp vụ/transaction, repository thực hiện SQL có bind parameter. Dùng handler chung cho lỗi API. Log theo [Observability](OBSERVABILITY.md).
- Dùng 4 spaces và import tường minh. Spotless với Google Java Format AOSP và Checkstyle kiểm tra format, import và độ dài method trong `verify`. Chạy `.\mvnw.cmd spotless:apply` từ `backend/` để format trên Windows, hoặc `bash ./mvnw spotless:apply` trên macOS/Linux.

```java
// ✅ Controller chuyển việc đọc bài sang service.
CatalogViews.LessonDetail getLesson(String courseSlug, String lessonSlug) {
    return catalog.getLesson(courseSlug, lessonSlug);
}
// ❌ Controller tạo JDBC query rồi ghép JSON trả về.
```

[Patterns](DESIGN_PATTERNS.md) · [API](API_GUIDELINES.md) · [Kiểm thử](TESTING_STRATEGY.md)

## Checklist

- [ ] Tên, import và method qua kiểm tra tương ứng.
- [ ] Có test cho nhánh nghiệp vụ hoặc lỗi mới.
