- **Mục đích** Chạy Spring Boot và test PostgreSQL trên máy dev.
- **Đối tượng đọc** Dev mới và reviewer cần tái lập.
- **Cập nhật lần cuối** 2026-11-25
- **Owner** Backend

# Thiết lập môi trường local

Cần Git, JDK 21, Docker với Compose v2 và Linux containers. Khi chạy Bash Wrapper trên macOS/Linux, cài `unzip` để Wrapper dùng bản ZIP đã ghim checksum. Windows dùng quick start trong [README](../README.md). Lệnh Maven chạy từ `backend/`, nên profile `dev` tìm được `../.env`.

```bash
# ✅ Từ repo root trên macOS/Linux, giữ .env nếu đã có.
test -f .env || cp .env.example .env
docker compose --env-file .env -f infra/docker-compose.dev.yml up -d --wait
cd backend
bash ./mvnw -Dspring-boot.run.profiles=dev spring-boot:run
# ❌ git add .env
```

| Biến | Cách dùng |
| --- | --- |
| `DB_NAME`, `DB_PORT` | Tên DB local và cổng host của Compose. Mặc định `learn_platform` và `5432`. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Datasource. Ngoài profile `dev`, cung cấp đủ ba biến. |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` | Redis cho rate limit và hàng đợi submission. Compose dev chỉ bind Redis vào loopback. |
| `SERVER_PORT`, `SERVER_ADDRESS` | Cổng mặc định `8080`. Address của profile `dev` mặc định `127.0.0.1`. |
| `FRONTEND_ORIGINS` | Danh sách origin phân cách bằng dấu phẩy. Local mặc định localhost và 127.0.0.1 cổng 3000. |
| `TEST_DB_URL`, `TEST_DB_USER`, `TEST_DB_PASSWORD` | Datasource riêng cho integration test. |

Tạo `codelearn_test` một lần từ repo root khi dùng cấu hình mẫu. Đổi user, password và cổng theo `.env` local nếu khác.

```powershell
docker compose --env-file .env -f infra/docker-compose.dev.yml exec -T postgres createdb -U learn codelearn_test
Set-Location backend
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/codelearn_test'
$env:TEST_DB_USER = 'learn'
$env:TEST_DB_PASSWORD = 'learn_dev_password'
.\mvnw.cmd verify
```

Trên macOS/Linux, export ba biến `TEST_DB_*` tương ứng rồi chạy `bash ./mvnw verify`. Nếu cổng DB bị chiếm, đổi `DB_PORT` và URL test. Nếu readiness lỗi, xem `docker compose --env-file .env -f infra/docker-compose.dev.yml ps` và log Flyway. Dừng API bằng `Ctrl+C`. Từ repo root, chạy `docker compose --env-file .env -f infra/docker-compose.dev.yml down` để giữ volume.

## Sandbox: nộp bài và worker

Compose dev khởi động PostgreSQL và Redis. API trong profile `dev` nhận submission và đưa ID vào Redis; chạy API không tự chạy mã học viên. Worker là tiến trình riêng, profile `worker`, và cần kết nối tới một Judge0 đã được dựng trong môi trường cô lập.

Flyway V7 cấp đủ giới hạn Sandbox ở cả môi trường ngoài `dev` và giữ giá trị đã tùy chỉnh. V8 thêm lease cho worker. Khởi động API và áp xong migration trước khi bật worker.

Không khởi động worker trỏ tới Judge0 dùng chung hoặc production để thử mã tùy ý. Chưa xác minh Judge0 thật, cô lập mạng/tài nguyên, hay các bài nghiệm thu N2/N4; hiện CI và test local chỉ dùng HTTP stub an toàn.

Worker cần các biến `DB_URL`, `DB_USER`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `JUDGE0_BASE_URL` và `JUDGE0_PYTHON_LANGUAGE_ID`. Chỉ đặt `JUDGE0_AUTH_TOKEN` nếu Judge0 yêu cầu. Chạy riêng trong terminal đã được cấu hình các biến môi trường:

```bash
cd backend
bash ./mvnw -Dspring-boot.run.profiles=worker spring-boot:run
```

Submission API, quyền truy cập, mã lỗi và polling được mô tả trong [OpenAPI](api/openapi.yaml). SSE chưa được triển khai.

## Chạy cả backend bằng Compose

Từ repo root có `.env`, bật profile `app` để build và khởi động cả backend. Cách này dùng JDK trong Docker image. Nếu backend chạy bằng Maven Wrapper đã chiếm cổng, dừng tiến trình đó hoặc đổi `SERVER_PORT` trong `.env` trước khi chạy.

```powershell
docker compose --env-file .env -f infra/docker-compose.dev.yml --profile app up -d --build
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
docker compose --env-file .env -f infra/docker-compose.dev.yml --profile app down
```

Chờ readiness báo `UP` sau khi backend khởi động và áp migration. Thay `8080` trong URL nếu đã đổi `SERVER_PORT`.

[Biến mẫu](../.env.example) · [Kiểm thử](TESTING_STRATEGY.md) · [Deploy](DEPLOYMENT.md)

## Checklist

- [ ] Local dùng profile `dev` và readiness báo `UP`.
- [ ] DB test tách DB phát triển và `.env` không vào Git.
