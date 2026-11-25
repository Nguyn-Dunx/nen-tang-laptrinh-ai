- **Mục đích** Hướng dẫn gọi các API đang được triển khai.
- **Đối tượng đọc** Frontend, người tích hợp và dev backend.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Hướng dẫn API

Hợp đồng đầy đủ nằm trong [OpenAPI](api/openapi.yaml). Tài liệu này giới thiệu các endpoint hiện có. API đọc bài tập đã xuất bản đang có; API nộp bài, chấm sandbox và AI trong tài liệu kế hoạch chưa được triển khai.

## Địa chỉ và quy ước chung

- Backend local mặc định: `http://localhost:8080`.
- API nghiệp vụ dùng tiền tố `/api/v1`. Các endpoint Actuator dùng `/actuator`.
- Dữ liệu gửi và nhận dùng JSON, trừ lỗi dùng `application/problem+json`.
- Mỗi phản hồi có `X-Request-Id`; khi báo lỗi hãy cung cấp giá trị này.
- Nội dung khóa học và bài học chưa xuất bản được trả như `404`.

## Danh mục khóa học

Các endpoint đọc danh mục công khai, không cần phiên đăng nhập.

### Liệt kê khóa học

```http
GET /api/v1/courses?page=0&size=20
```

`page` bắt đầu từ 0. `size` từ 1 đến 50, mặc định 20. Kết quả sắp theo slug tăng dần; trang ngoài phạm vi trả `items` rỗng.

```json
{
  "items": [
    {
      "slug": "python-co-ban",
      "title": "Python cơ bản",
      "summary": "Làm quen với cú pháp Python qua các ví dụ ngắn."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1
}
```

### Lấy khóa học và mục lục

```http
GET /api/v1/courses/python-co-ban
```

Phản hồi gồm thông tin khóa học và `lessons` đã xuất bản theo thứ tự học. Mỗi mục bài học có `slug`, `title`, `position`.

### Lấy nội dung bài học

```http
GET /api/v1/courses/python-co-ban/lessons/xin-chao-python
```

Phản hồi gồm `courseSlug`, `slug`, `title`, `position` và `bodyMarkdown`. Cần truyền cả slug khóa và slug bài để định danh bài học.

## Bài tập

### Lấy bài tập đã xuất bản

```http
GET /api/v1/exercises/1
```

Endpoint công khai, không cần đăng nhập. Bài tập chỉ đọc được khi cả lesson và course đã xuất bản; bài không tồn tại hoặc thuộc nội dung nháp trả `404` với `code: EXERCISE_NOT_FOUND`. Phản hồi gồm `id`, `lessonId`, `title`, `language`, `difficulty`, `statementMarkdown`, `starterCode`, `maxScore` và `publicTests`. Mỗi test công khai có `position`, `stdin`, `expectedOutput`. Test ẩn, số lượng test ẩn và lời giải tham khảo không được trả.

```json
{
  "id": 1,
  "lessonId": 2,
  "title": "Tính tổng hai số",
  "language": "python",
  "difficulty": "beginner",
  "statementMarkdown": "Đọc hai số nguyên từ stdin và in tổng của chúng.",
  "starterCode": "",
  "maxScore": 100,
  "publicTests": [
    { "position": 1, "stdin": "2 3", "expectedOutput": "5" }
  ]
}
```

## Tài khoản và phiên đăng nhập

Identity dùng cookie phiên `CODELEARN_SESSION` và bảo vệ các yêu cầu thay đổi trạng thái bằng CSRF. Trình duyệt phải giữ cookie giữa các yêu cầu. Client chạy khác origin cần gửi credential (`credentials: 'include'`) và dùng origin được cấu hình cho CORS.

Trước `POST`, gọi `GET /api/v1/auth/csrf`, giữ cookie phiên và gửi token nhận được trong header `X-CSRF-TOKEN`. Lấy CSRF token mới sau đăng ký, đăng nhập hoặc đăng xuất.

### Lấy CSRF token

```http
GET /api/v1/auth/csrf
```

```json
{
  "headerName": "X-CSRF-TOKEN",
  "token": "<csrf-token>"
}
```

### Đăng ký

```http
POST /api/v1/auth/register
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>

{
  "email": "learner@example.test",
  "password": "development-only-password"
}
```

Thành công trả `201 Created`, hồ sơ tài khoản và `Location: /api/v1/auth/me`. Tài khoản mới có vai trò `LEARNER`; email được chuẩn hóa chữ thường. Mật khẩu đăng ký tối thiểu 15 ký tự.

### Đăng nhập

```http
POST /api/v1/auth/login
Content-Type: application/json
X-CSRF-TOKEN: <csrf-token>

{
  "email": "learner@example.test",
  "password": "development-only-password"
}
```

Thành công trả `204 No Content` và đặt cookie phiên. Gọi `GET /api/v1/auth/me` để đọc hồ sơ.

### Đọc hồ sơ phiên hiện tại

```http
GET /api/v1/auth/me
Cookie: CODELEARN_SESSION=<session-id>
```

Trả `200` cùng `id`, `email`, `role`. Thiếu phiên hợp lệ trả `401`.

### Đăng xuất

```http
POST /api/v1/auth/logout
X-CSRF-TOKEN: <csrf-token>
Cookie: CODELEARN_SESSION=<session-id>
```

Thành công trả `204`, hủy phiên phía server và hết hạn cookie.

## Kiểm tra trạng thái dịch vụ

| Endpoint | Ý nghĩa |
|---|---|
| `GET /api/v1/health` | API có phản hồi HTTP; trả `200` khi hoạt động. |
| `GET /actuator/health/liveness` | Tiến trình ứng dụng còn sống. |
| `GET /actuator/health/readiness` | Ứng dụng sẵn sàng, bao gồm kết nối PostgreSQL. |

Probe Actuator trả `200` khi khỏe và `503` khi không sẵn sàng.

## Lỗi

Lỗi có kiểu nội dung `application/problem+json`. Các trường `code` và `traceId` dành cho xử lý phía client và tra cứu log.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Không tìm thấy nội dung.",
  "instance": "/api/v1/courses/khong-co",
  "code": "NOT_FOUND",
  "traceId": "7fc54937-9385-4df5-9976-494338298a97"
}
```

| HTTP | Trường hợp thường gặp | Cách xử lý |
|---|---|---|
| `400` | Dữ liệu đầu vào hoặc phân trang không hợp lệ (`VALIDATION_ERROR`). | Sửa request theo hợp đồng OpenAPI. |
| `401` | Phiên không hợp lệ hoặc thông tin đăng nhập sai (`UNAUTHENTICATED`). | Đăng nhập lại; kiểm tra email và mật khẩu. |
| `403` | CSRF thiếu/sai hoặc thao tác không được phép (`FORBIDDEN`). | Lấy CSRF token mới và gửi cùng cookie phiên. |
| `404` | Khóa học/bài học (`NOT_FOUND`) hoặc bài tập (`EXERCISE_NOT_FOUND`) không tồn tại/chưa xuất bản. | Kiểm tra lại slug/id và trạng thái nội dung. |
| `409` | Email đã được đăng ký (`CONFLICT`). | Dùng tài khoản hiện có hoặc email khác. |
| `500` | Lỗi nội bộ (`INTERNAL_ERROR`). | Thử lại sau và gửi `traceId` cho đội backend. |

## Thử nhanh bằng curl

```bash
curl 'http://localhost:8080/api/v1/courses?page=0&size=20'
curl 'http://localhost:8080/api/v1/courses/python-co-ban/lessons/xin-chao-python'
curl 'http://localhost:8080/api/v1/health'
```

Để thử luồng phiên bằng PowerShell, xem [ví dụ tài khoản](AUTH_SECURITY.md#thử-api-tài-khoản). Quy tắc cập nhật hợp đồng nằm trong [quy ước API](API_GUIDELINES.md).

## Checklist

- [ ] Xem OpenAPI trước khi tích hợp hoặc thay đổi payload.
- [ ] Client phiên giữ cookie và gửi CSRF cho mọi yêu cầu `POST`.
- [ ] Khi báo lỗi, gửi kèm `X-Request-Id` hoặc `traceId`.
