WITH pilot (ordinal, slug, title, body) AS (
    VALUES
    (1, 'nhap-va-chuyen-kieu', 'Nhập dữ liệu và chuyển kiểu', $md$# Dữ liệu nhập là chuỗi

`input()` đọc một dòng từ bàn phím. Dùng `int()` khi cần tính toán với số nguyên.

```python
age = int(input())
print(age + 1)
```

Với đầu vào `20`, chương trình in `21`. Hãy thử đọc hai số trên cùng một dòng bằng `map(int, input().split())`, rồi in tổng. Trong bài chấm tự động, chỉ in kết quả yêu cầu.
$md$),
    (2, 'dieu-kien', 'Rẽ nhánh với if và else', $md$# Chọn một nhánh

`if` thực thi khối lệnh khi điều kiện đúng. `else` xử lý trường hợp còn lại. Python dùng thụt lề để xác định khối.

```python
score = int(input())
if score >= 5:
    print("PASS")
else:
    print("RETRY")
```

Thử các đầu vào `4`, `5` và `6`. Giá trị ngay tại ngưỡng giúp kiểm tra bạn đã dùng đúng phép so sánh.
$md$),
    (3, 'vong-lap', 'Vòng lặp và biến tích lũy', $md$# Lặp với range

`range(1, n + 1)` tạo các số từ 1 đến n. Biến `total` giữ tổng sau mỗi lượt lặp.

```python
n = int(input())
total = 0
for value in range(1, n + 1):
    total += value
print(total)
```

Với đầu vào `3`, các giá trị được cộng là 1, 2 và 3. Với `0`, vòng lặp không chạy và tổng bằng 0.
$md$),
    (4, 'ham-co-ban', 'Hàm và giá trị trả về', $md$# Tách phần tính toán thành hàm

Hàm nhận đầu vào qua tham số và trả kết quả bằng `return`. Tách việc tính toán khỏi nhập/xuất giúp bạn thử hàm với nhiều dữ liệu.

```python
def square(number):
    return number * number

print(square(4))
```

Thử `square(0)` và `square(-3)`. Sau đó viết một hàm nhận hai số và trả giá trị lớn hơn.
$md$)
),
target AS (
    SELECT id, COALESCE((SELECT max(position) FROM lesson WHERE course_id = course.id), 0) AS last_position
    FROM course WHERE slug = 'python-co-ban'
),
missing AS (
    SELECT target.id AS course_id, pilot.*, target.last_position
    FROM target CROSS JOIN pilot
    WHERE NOT EXISTS (
        SELECT 1 FROM lesson WHERE lesson.course_id = target.id AND lesson.slug = pilot.slug
    )
)
INSERT INTO lesson (course_id, slug, title, position, body_markdown, published_at)
SELECT course_id, slug, title,
       last_position + row_number() OVER (PARTITION BY course_id ORDER BY ordinal),
       body, CURRENT_TIMESTAMP
FROM missing
ON CONFLICT (course_id, slug) DO NOTHING;
