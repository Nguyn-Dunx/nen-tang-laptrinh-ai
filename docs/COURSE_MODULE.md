- **Mục đích** Hướng dẫn tích hợp và nghiệm thu API Khóa học.
- **Đối tượng đọc** Leader, frontend, QA và vận hành.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Module `content`

# Module Khóa học

## Quản trị

Tất cả route trong bảng có prefix `/api/v1/admin/courses`, cần phiên `ADMIN`. Gửi `X-CSRF-TOKEN` và cookie phiên cho `POST` và `PUT`. Trên máy dev, dùng tài khoản quản trị đã được cấp quyền trong DB local và đăng nhập lại sau khi cấp quyền. [Xác thực và CSRF](AUTH_SECURITY.md#thử-api-tài-khoản)

| Method | Đường dẫn tương đối | Hành vi |
| --- | --- | --- |
| GET, POST | Tại prefix | Liệt kê cả khóa nháp và khóa công khai, hoặc tạo khóa nháp. |
| GET, PUT | `/{courseSlug}` | Đọc chi tiết quản trị hoặc sửa tiêu đề và mô tả khóa nháp. |
| PUT | `/{courseSlug}/publication` | Gửi `{"published":true}` để xuất bản, `false` để thu hồi. |
| POST | `/{courseSlug}/lessons` | Thêm bài nháp vào cuối khóa. |
| GET, PUT | `/{courseSlug}/lessons/{lessonSlug}` | Đọc hoặc sửa tiêu đề, Markdown của bài nháp. |
| PUT | `/{courseSlug}/lessons/{lessonSlug}/publication` | Xuất bản hoặc thu hồi bài. |
| PUT | `/{courseSlug}/lesson-order` | Gửi `lessonSlugs` chứa tất cả slug bài đúng một lần, theo thứ tự mong muốn. |

Slug giữ cố định sau khi tạo. Thu hồi nội dung trước khi sửa, rồi xuất bản lại. Một khóa cần ít nhất một bài đã xuất bản để công khai. Bài có thể được đánh dấu xuất bản trong khóa nháp. Thu hồi khóa sẽ ẩn các bài và bài tập bên trong, đồng thời giữ trạng thái từng bài để có thể mở lại khóa. Thu hồi bài công khai cuối cùng chỉ thực hiện được khi khóa đã được thu hồi.

## Luồng học

Frontend dùng `courseSlug` và `lessonSlug` để gọi API bài học. Response bài học có `previousLessonSlug` và `nextLessonSlug`, chỉ trỏ tới bài đã xuất bản trong cùng khóa. Tìm bài tập mà không cần biết trước ID nội bộ của bài:

~~~http
GET /api/v1/courses/python-co-ban/lessons/dieu-kien/exercises
~~~

Endpoint trả danh sách `id`, `title`, `language`. Dùng `id` để gọi Sandbox. Khóa hay bài chưa công khai trả `404`. Người học vẫn có thể tra bài nộp của mình sau khi nội dung bị thu hồi. Frontend tắt HTML nhúng và kiểm soát URL khi render Markdown. [OpenAPI](api/openapi.yaml)

## Dữ liệu và kiểm tra

Flyway V9 giữ ID, slug và liên kết bài tập đã có. Profile `dev` có 6 bài Python cơ bản và 3 bài tập. Tài khoản quản trị và dữ liệu mẫu cho staging cấp theo quy trình môi trường của nhóm.

- [ ] Admin đọc, tạo và cập nhật bản nháp. Guest hoặc learner không ghi được.
- [ ] Xuất bản rồi thu hồi. Kiểm tra lại cả bài học và bài tập qua URL công khai.
- [ ] Đổi chỗ hai bài trong cùng khóa và xác nhận điều hướng công khai bỏ qua bài nháp.

[Kế hoạch](COURSE_MODULE_PLAN.md) · [Thiết lập local](ENVIRONMENT_SETUP.md) · [Kiến trúc](ARCHITECTURE.md)
