- **Mục đích** Bảo vệ dữ liệu, tài khoản và ranh giới Judge0/LLM.
- **Đối tượng đọc** Dev backend, frontend, reviewer và vận hành.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend và Sandbox, AI, DevOps

# Xác thực và bảo mật

`identity` dùng Spring Security và Spring Session JDBC. Phiên hết hạn sau 30 phút không hoạt động. Cookie `CODELEARN_SESSION` có `HttpOnly`, `SameSite=Lax` và `Secure` ngoài profile `dev`. Đăng ký hoặc đăng nhập đổi ID phiên và CSRF token. Đăng xuất hủy phiên hiện tại trong PostgreSQL. [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

| Chủ thể | Quyền nghiệp vụ MVP |
| --- | --- |
| Khách | Đọc nội dung đã xuất bản. |
| Học viên | Thêm tiến độ, nộp bài, xem bài nộp của mình và hỏi AI theo quota. |
| Tác giả | Soạn và gửi duyệt nội dung mình phụ trách. |
| Quản trị | Duyệt, xuất bản, quản lý roadmap và vai trò. |

Vai trò biên tập/quản trị không mặc định có quyền đọc dữ liệu học tập riêng. Mỗi route riêng tư lấy chủ thể từ session, kiểm tra role và ownership trên server, để ID do client gửi không mở quyền.

- Đăng ký tạo role `LEARNER` và mật khẩu được băm bằng Argon2id. Nạp secret qua môi trường chạy và thu hồi khi lộ. Hạn mức đăng nhập, xác minh email, khôi phục mật khẩu và quản lý vai trò thuộc các mốc sau. Hoàn thành hạn mức trước khi mở đăng nhập công khai.
- Frontend tắt HTML nhúng trong Markdown, kiểm soát URL và cô lập vùng Try it. Backend lọc context gửi LLM theo danh sách trường, loại định danh, test ẩn và lời giải đầy đủ. Kiểm tra retention trước khi dùng dữ liệu thật.
- Chỉ worker kết nối Judge0 qua mạng riêng. Máy chạy mã tách máy chính, không có secret hệ thống chính. Tiến trình chạy bài bị giới hạn tài nguyên và không có mạng. Điều kiện bật được ghi ở [Deploy](DEPLOYMENT.md).

```java
// ✅ Tra trong phạm vi chủ sở hữu đã xác thực.
return submissions.findByIdAndOwnerId(submissionId, principal.userId());
// ❌ return submissions.findById(submissionId);
```

[Kiểm thử](TESTING_STRATEGY.md) · [API](API_GUIDELINES.md) · [Observability](OBSERVABILITY.md)

## Thử API tài khoản

Các bước dưới đây chạy với backend local. Thay email mẫu nếu đã dùng. Đăng ký bắt đầu phiên học viên. Sau khi đăng xuất, lấy lại CSRF rồi gửi cùng JSON đến `/login` để dùng tài khoản cũ. Frontend khác origin ở local cần `credentials: 'include'` và dùng cùng tên host cho hai dịch vụ, chẳng hạn `localhost` ở cả hai cổng.

```powershell
$authUrl = 'http://127.0.0.1:8080/api/v1/auth'
$csrf = Invoke-RestMethod "$authUrl/csrf" -SessionVariable accountSession
$body = @{
    email = 'learner@example.test'
    password = 'development-only-password'
} | ConvertTo-Json
Invoke-RestMethod "$authUrl/register" -Method Post -WebSession $accountSession `
    -ContentType 'application/json' -Headers @{'X-CSRF-TOKEN' = $csrf.token} -Body $body
Invoke-RestMethod "$authUrl/me" -WebSession $accountSession
$csrf = Invoke-RestMethod "$authUrl/csrf" -WebSession $accountSession
Invoke-RestMethod "$authUrl/logout" -Method Post -WebSession $accountSession `
    -Headers @{'X-CSRF-TOKEN' = $csrf.token}
```

## Checklist OWASP cho PR

- [ ] Đã kiểm tra truy cập ngang quyền, role, session và CSRF ở route thay đổi.
- [ ] Đã kiểm tra input, XSS khi render, bind SQL và secret trong log.
- [ ] Dữ liệu gửi ra ngoài được giới hạn theo quyền và trường cho phép.
