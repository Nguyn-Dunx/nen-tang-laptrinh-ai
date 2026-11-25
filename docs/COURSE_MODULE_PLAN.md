- **Mục đích** Triển khai module Khóa học trong 6 tuần và nối luồng Sandbox.
- **Đối tượng đọc** Leader, frontend, Sandbox và QA.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Leader · Module `content`

# Kế hoạch triển khai module Khóa học

## Đầu ra cuối tuần 6

Một khóa Python cơ bản với **6–8 bài Markdown và ít nhất 3 bài tập** là mục tiêu đề xuất. Học viên mở khóa, đọc bài đã xuất bản, tìm bài tập từ bài học, đăng nhập, nộp mã và xem verdict bằng polling. Quản trị viên tạo, sửa và xuất bản nội dung trên staging. Dùng OpenAPI làm hợp đồng với frontend. [Hiện trạng](PROJECT_STATUS.md) · [OpenAPI](api/openapi.yaml)

## Kiến trúc và hợp đồng

- Giữ URL đọc hiện tại và `courseSlug` + `lessonSlug`. Tạo API quản trị dưới `/api/v1/admin/courses`, dùng Spring Security role `ADMIN` và CSRF cho ghi. Chốt URL chi tiết và payload trong PR đầu tiên.
- `content` sở hữu `course`, `lesson`, quy tắc xuất bản và DTO đọc/ghi. Đặt validation, transaction trong service và dùng JdbcClient. Đề xuất `published_at` là nguồn hiển thị công khai. Chốt cách sửa nội dung đã xuất bản và phạm vi thu hồi trước khi viết migration.
- Đề xuất `GET /api/v1/courses/{courseSlug}/lessons/{lessonSlug}/exercises` trả ID, tiêu đề và ngôn ngữ từ dữ liệu Sandbox qua hợp đồng đọc. Route phải lọc khóa/bài nháp cùng quy tắc với catalog. Sandbox sở hữu đề, test case và grader. Frontend lấy ID qua API bằng slug.
- Cập nhật schema bằng migration tiếp theo sau V8. Giữ slug và ID đã có. Chốt cơ chế đồng thời khi đổi thứ tự bài và giữ khóa ngoại từ bài tập. Chỉ render Markdown theo cấu hình đã thống nhất với frontend.

## Lịch thực hiện

| Tuần | Leader · Khóa học | Tích hợp và nghiệm thu |
| --- | --- | --- |
| 1 | Chốt phạm vi pilot, ma trận quyền, vòng đời xuất bản, payload và migration. | Frontend, Sandbox cùng review hợp đồng, fixture và danh sách bài. |
| 2 | Làm API tạo, sửa nháp và test validation, CSRF, quyền ADMIN, slug trùng. | Nối màn hình danh mục, mục lục và bài học với API đọc hiện tại. |
| 3 | Thêm xuất bản, ẩn nháp, điều hướng, API tìm bài tập theo bài học. | Demo mở bài, tìm bài tập, nộp và đọc trạng thái qua stub. |
| 4 | Hoàn tất 6–8 bài và duyệt nội dung với Sandbox. | Nối ít nhất 3 bài tập Python, kiểm tra verdict trên runner đã nghiệm thu hoặc ghi demo dùng stub. |
| 5 | Kiểm thử chạy migration trên dữ liệu cũ, hai tài khoản và Markdown. | UAT trên staging. Chốt các lỗi chặn chức năng, bảo mật và vận hành. |
| 6 | Sửa lỗi, chốt nội dung, hướng dẫn cập nhật và bàn giao. | Chạy lại E2E, báo cáo kết quả, demo và phương án khôi phục. |

## Điều kiện nghiệm thu

- [ ] API đọc hiện tại tương thích, bài và khóa nháp không lộ qua URL trực tiếp.
- [ ] Khách hoặc học viên không ghi được nội dung quản trị. Thiếu CSRF trả lỗi.
- [ ] Người học tìm bài tập từ slug, nộp bài và tra lại kết quả của mình sau reload.
- [ ] Nội dung pilot và test công khai đã được ít nhất một người khác duyệt.
- [ ] `mvn verify`, migration và kiểm thử staging có bằng chứng kèm PR.

[Kiến trúc](ARCHITECTURE.md) · [Lộ trình toàn dự án](ROADMAP.md) · [Kiểm thử](TESTING_STRATEGY.md)
