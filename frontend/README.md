# EduSky frontend

Frontend cho các API Khóa học, Tài khoản và Sandbox hiện có. Giao diện dựa trên EduSky v2 trong Figma, dùng font Be Vietnam Pro, JetBrains Mono, nền sáng, màu navy, cam và teal. Các trang học và thực hành có bố cục responsive.

## Chạy local

Cần Node.js 22 trở lên, khuyến nghị Node.js 24 LTS. Chạy backend theo README ở thư mục gốc trước, với profile `dev` để có khóa Python mẫu. Để bài nộp nhận kết quả cuối cùng, cần Redis, worker và Judge0 được cấu hình theo tài liệu backend.

Từ thư mục `frontend` chạy các lệnh sau.

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

Mở [EduSky local](http://127.0.0.1:3000). Biến `BACKEND_URL` mặc định là `http://127.0.0.1:8080`. Khởi động lại Next.js sau khi thay biến này. Trên macOS/Linux thay `Copy-Item` bằng `cp`.

Trình duyệt gọi `/api/v1` cùng origin với FE. Next.js chuyển yêu cầu sang backend và giữ cookie phiên. Backend phát triển đã cho phép origin `http://127.0.0.1:3000`. Khi đổi host hoặc cổng FE, cập nhật `FRONTEND_ORIGINS` của backend cho khớp.

## Các luồng đã nối

| Route | Chức năng |
| --- | --- |
| `/`, `/courses` | Khóa đã xuất bản, phân trang và lọc trong trang hiện tại |
| `/courses/[courseSlug]` | Chi tiết khóa, mục lục và mở bài đầu tiên |
| `/courses/[courseSlug]/lessons/[lessonSlug]` | Markdown, điều hướng bài trước/sau và tìm bài tập liên quan |
| `/practice` | Chọn khóa và mở các bài tập đã xuất bản |
| `/exercises/[exerciseId]` | Soạn Python, nộp bài, theo dõi chấm, test công khai/ẩn, lịch sử và dùng lại mã |
| `/login`, `/register` | Phiên cookie, CSRF và quay về tác vụ đang làm |
| `/admin/courses` | Danh mục quản trị và tạo khóa nháp |
| `/admin/courses/[courseSlug]` | Biên tập, xuất bản/thu hồi, thêm và đổi thứ tự bài |
| `/admin/courses/[courseSlug]/lessons/[lessonSlug]` | Biên tập Markdown và xuất bản/thu hồi bài |

Đăng ký tạo học viên. Để sử dụng quản trị, đăng nhập bằng tài khoản có role `ADMIN` do môi trường backend cấp. Phân quyền và quyền sở hữu được kiểm tra lại trên backend với mỗi yêu cầu.

Sandbox dùng luồng nộp bài bất đồng bộ của backend. FE theo dõi `QUEUED` và `RUNNING`, hiển thị `DONE` cùng verdict, hoặc lỗi hạ tầng `FAILED`. Sau 90 giây, người học có thể chủ động tiếp tục kiểm tra. Bản nháp mã được giữ trong `sessionStorage`, theo từng bài và tài khoản. Khi trình duyệt chặn storage, bản nháp được giữ trong bộ nhớ của tab.

## Nền tảng kỹ thuật

- Next.js App Router và React, TypeScript strict. Route và layout tách khỏi module nghiệp vụ.
- TanStack Query quản lý tải dữ liệu, cache, mutation và polling. Query riêng tư có ID tài khoản trong khóa cache. Đổi phiên sẽ hủy request và reset dữ liệu đã tải.
- `lib/api/client.ts` tập trung cookie, timeout, CSRF mới cho mỗi lần ghi và ánh xạ Problem Detail. Các mutation gửi một lần để tránh nộp trùng khi kết nối lỗi.
- CodeMirror tải phía client, hỗ trợ Python và thao tác bàn phím. Font và editor được đóng gói cùng ứng dụng.
- React Markdown và remark-gfm render bài học. HTML nhúng bị bỏ qua, URL dùng bộ lọc của React Markdown.
- CSS semantic token ở `app/globals.css`, CSS sandbox trong module. Kích thước và tương phản thích ứng cho desktop, tablet và điện thoại, có chế độ giảm chuyển động.

```text
src/
  app/                  Route, layout và metadata
  components/           Header, footer, Markdown, feedback, provider
  features/
    auth/               Tài khoản và vòng đời phiên
    courses/            Danh mục, đọc bài và khám phá bài tập
    sandbox/            Editor, bài nộp, polling và lịch sử
    admin/              Quản trị khóa học và bài học
  lib/
    api/                Client và kiểu sinh từ OpenAPI
    catalog.ts          Query và đường dẫn học tập dùng chung
```

## Kiểu API và kiểm tra

`src/lib/api/schema.d.ts` được sinh từ `../docs/api/openapi.yaml`. Khi backend đổi hợp đồng, cập nhật OpenAPI rồi sinh lại kiểu trong cùng PR.

```powershell
npm run api:generate
npm run check
```

`check` chạy ESLint, TypeScript, các ca kiểm tra client HTTP và production build. Các ca HTTP kiểm tra CSRF sau xoay phiên, phản hồi rỗng, lỗi proxy, hủy request và chống gửi lại mutation. CI chạy độc lập với backend, đồng thời kiểm tra schema đã sinh còn khớp OpenAPI.

Để kiểm tra thủ công, đi qua khóa Python → bài Nhập dữ liệu → bài tập → đăng ký/đăng nhập → nộp Python → kết quả → lịch sử → dùng lại mã. Với tài khoản quản trị, tạo khóa nháp, thêm bài, xuất bản bài rồi khóa, kiểm tra ở giao diện học viên và thử đổi thứ tự bài. Dùng bộ dữ liệu riêng cho thử nghiệm.

## Hạng mục tiếp theo

Nối roadmap cá nhân, tiến độ và trợ lý AI khi các API tương ứng được hoàn thiện. Trước pilot, nghiệm thu Judge0 thực cùng backend, kiểm tra tải chấm bài, hạn mức đăng nhập và môi trường HTTPS. Khi triển khai FE và BE, giữ định tuyến `/api/v1` và cấu hình origin/cookie theo môi trường.
