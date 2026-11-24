# Lộ trình backend 16 tuần

MVP cho phép học viên chọn một chủ đề trên lộ trình, mở bài học, chạy mã, nộp bài Python, nhận kết quả chấm và hỏi trợ lý AI trong ngữ cảnh của bài. Kế hoạch dùng các mốc của báo cáo sơ bộ và áp dụng cách phân công ba người trong `starter-kit-github.zip`. Tuần được tính từ mốc khởi động nhóm xác nhận.

| Vai trò | Phần việc chính |
| --- | --- |
| Backend và điều phối | ERD, migration, hợp đồng API, tài khoản, phân quyền, nội dung, roadmap và tiến độ |
| Frontend và UX | Tích hợp API, luồng học, trình chỉnh sửa roadmap, vùng chạy thử và giao diện chấm bài, chat |
| Sandbox, AI và DevOps | Judge0, hàng đợi và worker, tích hợp LLM, staging, triển khai và giám sát |

Cả nhóm chia nhau biên soạn nội dung và kiểm thử. Báo cáo giả định 4–5 người và đề xuất 40–60 bài học. Kế hoạch cho ba người trong starter kit đặt mục tiêu 32 bài và ít nhất 5 bài blog trong hai khoá Web cơ bản và Python cơ bản, đóng băng tính năng cuối tuần 15 và bàn giao ở tuần 16. Nhóm cần xác nhận lại chỉ tiêu nội dung với giảng viên ở mốc M1.

## Điểm bắt đầu

Bộ khung có Java 21, Spring Boot theo module nghiệp vụ, PostgreSQL, Flyway, health check, OpenAPI, kiểm thử và cấu hình CI. API công khai đọc dữ liệu mẫu khoá học và bài học để frontend tích hợp sớm. Việc tiếp theo là chốt hợp đồng cho đồ thị roadmap, thiết kế xác thực và quyền sở hữu, rồi nối các màn hình học bằng dữ liệu thật.

## Tuần 1–2 Khởi động và thiết kế

- [ ] Cả nhóm chạy backend trên máy phát triển, kiểm tra health, migration trên PostgreSQL và gọi được API đọc khoá học, bài học.
- [ ] Chốt phạm vi P1 và luồng demo xuyên suốt với frontend. Chốt OpenAPI cho tài khoản, khoá học, bài học, roadmap và tiến độ, gồm quy ước ID, phân trang và lỗi. Cập nhật hợp đồng trong cùng PR khi thay đổi API.
- [ ] Xác nhận cách URL frontend giữ cả slug khoá và slug bài để học viên có thể mở trực tiếp một bài. Rà soát các quyết định nghiệp vụ tạm thời về node bị bỏ qua, luồng trả nội dung về nháp và điều kiện mở bậc gợi ý. [Mục truy vết trong Figma v1](https://www.figma.com/design/uMV1aZX9M0hAbvX1i350Ox/?node-id=2-6)
- [ ] Sau khi chốt payload đồ thị, làm API chỉ đọc cho một roadmap mẫu có node liên kết tới bài học đã xuất bản. Dùng dữ liệu này để frontend thử hiển thị sơ đồ trước khi làm trình chỉnh sửa.
- [ ] Chốt ERD và ma trận quyền. Khách đọc nội dung đã xuất bản, học viên lưu tiến độ và nộp bài, tác giả sửa nội dung mình phụ trách, quản trị duyệt và xuất bản.
- [ ] Chạy thử Judge0 với vài bài Python gồm đúng, sai và vượt thời gian. Ghi lại cách tái lập, giới hạn tài nguyên và phương án vận hành trên máy riêng.

**M1 cuối tuần 2** — Một thành viên khác chạy được dự án theo hướng dẫn. Frontend đọc được dữ liệu mẫu và có hợp đồng để tích hợp. Đã có kết quả thử Judge0 và backlog theo thứ tự phụ thuộc.

## Tuần 3–6 Tài khoản và nội dung học

- [ ] Làm đăng ký, đăng nhập email, băm mật khẩu, làm mới phiên và đăng xuất. Thêm quyền quản lý tài khoản và vai trò cho quản trị viên. Kiểm thử truy cập trái quyền, truy cập dữ liệu của người khác và thu hồi phiên. Nối Google và GitHub theo AC-01 sau khi luồng email ổn định.
- [ ] Hoàn thiện API công khai cho danh mục, mục lục theo thứ tự, bài trước và bài sau. Thêm CMS cho khoá, bài học và blog. Tác giả tạo nháp và gửi duyệt, quản trị có thể yêu cầu sửa hoặc xuất bản. Lưu phiên bản nội dung khi xuất bản và kiểm thử việc truy cập bản nháp bằng URL trực tiếp.
- [ ] Lưu tiến độ bài học theo tài khoản và cung cấp dữ liệu cho mục Tiếp tục học. Thống nhất cách render Markdown an toàn với frontend. Tích hợp Try it HTML, CSS và JavaScript qua iframe có sandbox ở frontend.
- [ ] Chuẩn bị song song đề bài Python, mã khởi đầu, lời giải do tác giả viết, test công khai và test ẩn. Chốt mô hình dữ liệu và hợp đồng bài nộp để frontend dựng trạng thái chấm.

**M2 cuối tuần 6** — Học viên đăng nhập, học hết một khoá mẫu và thấy tiến độ sau khi đăng nhập lại. Nhóm xuất bản được bài học và blog qua CMS. Mục tiêu nội dung tối thiểu là 12 bài đã qua rà soát.

## Tuần 7–10 Roadmap và chấm bài

- [ ] Trả đồ thị roadmap đã xuất bản gồm cấp độ, node, cạnh tiên quyết và tài nguyên liên kết. Giữ ID node ổn định khi đổi vị trí. API quản trị lưu nháp và xuất bản, kiểm tra cạnh trỏ sai, vòng lặp phụ thuộc và liên kết đến nội dung chưa xuất bản.
- [ ] Lưu trạng thái từng node theo học viên và tính phần trăm tiến độ. Thống nhất cách tính với frontend, kể cả khi bỏ qua node hoặc roadmap đổi phiên bản.
- [ ] Lưu bài nộp trước khi phản hồi mã bài nộp. Worker chấm qua Judge0 trên máy riêng. Khôi phục được công việc chấm nếu quá trình chuyển vào hàng đợi bị gián đoạn. Dùng mã bài nộp để tránh ghi trùng kết quả khi hàng đợi giao lại công việc. Chỉ thử lại có giới hạn đối với lỗi hạ tầng.
- [ ] Tính verdict và điểm theo trọng số test. API lịch sử và kết quả kiểm tra quyền sở hữu, chỉ trả chi tiết dữ liệu của test công khai. Làm API polling trước để kiểm tra luồng đầy đủ, sau đó bổ sung SSE và giữ polling làm dự phòng. Tách chạy thử khỏi nộp bài trong hợp đồng và thống nhất cách đếm hạn mức cho từng loại.
- [ ] Giới hạn kích thước mã, tần suất nộp và số bài chờ theo người dùng. Chạy mã với mạng bị tắt và giới hạn thời gian, bộ nhớ, tiến trình, dung lượng ghi và đầu ra. Chạy kiểm thử bảo mật trên máy sandbox trước khi mở cho người dùng.

**M3 cuối tuần 10** — Học viên đi từ roadmap tới bài tập và xem lại verdict sau khi tải lại trang. Kiểm tra ít nhất 30 bài nộp mẫu và thử 30 lượt nộp đồng thời. Mục tiêu nội dung là 22 bài.

## Tuần 11–13 Trợ lý AI theo ngữ cảnh

- [ ] Nối một nhà cung cấp LLM qua adapter và truyền câu trả lời bằng SSE. Lấy ngữ cảnh bài học, đề bài và kết quả chấm từ backend sau khi kiểm tra quyền. Giới hạn kích thước ngữ cảnh và loại thông tin nhận dạng, dữ liệu test ẩn khỏi yêu cầu gửi LLM.
- [ ] Tính bậc gợi ý trên server từ lịch sử nộp của chính học viên. Dùng ngưỡng đề xuất 0, 2 và 4 lần chưa đạt để mở ba bậc, loại lượt chạy thử và lỗi hệ thống khỏi bộ đếm. Cấp lời giải do tác giả viết qua API riêng khi đã Accepted hoặc sau 6 lần chưa đạt. Giữ lời giải đầy đủ trong backend và dùng gợi ý do tác giả soạn ở bậc cao.
- [ ] Áp hạn mức ngày và giới hạn tần suất, hoàn lượt khi nhà cung cấp lỗi. Lưu phản hồi Hữu ích hoặc Chưa hữu ích, số token và lỗi để đánh giá chất lượng và chi phí.
- [ ] Soạn và chạy 50 câu đánh giá gồm 15 câu khái niệm, 15 câu giải thích lỗi, 10 câu kiểm tra bậc gợi ý và 10 câu ngoài phạm vi hoặc cố đưa chỉ dẫn vào dữ liệu đầu vào.

**M4 cuối tuần 13** — Chat hoạt động trong bài học và bài tập, có số liệu từ bộ đánh giá và kiểm tra được từng bậc gợi ý. Mục tiêu nội dung là 30 bài.

## Tuần 14–16 Nghiệm thu và bàn giao

- [ ] Tuần 14 chạy kiểm thử đầu cuối, thử tải chấm bài và chat, rà soát phân quyền, XSS, hạn mức và các kịch bản tấn công sandbox. Mời người học thử trên staging và ghi kết quả theo tiêu chí bên dưới.
- [ ] Tuần 15 chốt phạm vi, sửa lỗi chặn nghiệm thu, triển khai qua HTTPS và kiểm tra luồng SSE qua reverse proxy. Bật cảnh báo lỗi và chi phí. Thử khôi phục bản sao lưu PostgreSQL trước khi đóng băng tính năng.
- [ ] Tuần 16 dành cho lỗi ảnh hưởng nghiệm thu, tài liệu vận hành, số liệu kiểm thử, báo cáo cuối kỳ và diễn tập demo. Ghi lại kết quả thực tế và các hạng mục còn trong backlog.

**M5 cuối tuần 15** — MVP chạy trên môi trường triển khai, có biên bản nghiệm thu và đã thử khôi phục dữ liệu. Tuần 16 hoàn tất hồ sơ bàn giao.

## Tiêu chí nghiệm thu

| Mã | Kết quả cần kiểm tra |
| --- | --- |
| N1 | Học viên đi hết luồng roadmap, bài học, chạy mã, nộp bài, nhận verdict và hỏi AI bằng dữ liệu thật. Tài khoản khác không đọc được tiến độ và bài nộp riêng. |
| N2 | Ít nhất 30 bài mẫu có verdict đúng, gồm bài đúng, sai, lỗi biên dịch hoặc cú pháp, lỗi khi chạy, quá thời gian và quá bộ nhớ. 30 lượt nộp đồng thời không mất bài. |
| N3 | Chạy bộ 50 câu đánh giá AI theo bốn tiêu chí đúng kiến thức, bám bài, đúng bậc và an toàn. Mục tiêu UAT là trên 70% đánh giá Hữu ích. |
| N4 | Các kịch bản vòng lặp vô hạn, cạn bộ nhớ, tạo quá nhiều tiến trình, truy cập mạng, đọc tệp ngoài vùng cho phép, ghi tệp lớn, in quá nhiều và nộp dồn dập đều bị chặn hoặc giới hạn. |
| N5 | Đo trên môi trường triển khai với mục tiêu trang học tải dưới 2 giây và chạy mã đơn giản khoảng 3 giây. Ghi điều kiện đo cùng kết quả. |
| N6 | Hai khoá pilot có tổng cộng ít nhất 32 bài và 5 bài blog đã xuất bản theo kế hoạch nhóm ba người. |

## Việc chạy song song

- Từ tuần 3, mỗi bài có mục tiêu, giải thích, ví dụ đã chạy thử và bài tập hoặc quiz phù hợp. Tác giả tự kiểm tra, thành viên khác rà soát trước khi xuất bản. Nội dung đi vào CMS khi quy trình duyệt sẵn sàng.
- Hai tuần một lần, demo luồng đã tích hợp. Mỗi đầu việc có người phụ trách, điều kiện hoàn thành và mã yêu cầu từ báo cáo. Backend cùng frontend duyệt hợp đồng khi thay đổi payload. Sandbox và AI cùng backend duyệt trường dữ liệu được phép gửi ra ngoài.
- Sau khi luồng P1 qua nghiệm thu nội bộ, cân nhắc P2 như kiểm tra đầu vào, bookmark, ghi chú, playground và cộng đồng cơ bản. RAG, AI agent, checkpoint mở khoá, mock interview và gamification nằm trong backlog sau MVP.
