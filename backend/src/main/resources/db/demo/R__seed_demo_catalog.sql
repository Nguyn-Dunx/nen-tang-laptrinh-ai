INSERT INTO course (slug, title, summary, published_at)
VALUES ('python-co-ban', 'Python cơ bản', 'Làm quen với cú pháp Python qua các ví dụ ngắn.', CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO lesson (course_id, slug, title, position, body_markdown, published_at)
SELECT id, 'xin-chao-python', 'Xin chào, Python', 1, $markdown$# Chương trình đầu tiên

Hàm `print()` hiển thị giá trị ra màn hình.

```python
print("Xin chào, Python!")
```

Thử thay câu chào bằng tên của bạn.
$markdown$, CURRENT_TIMESTAMP
FROM course
WHERE slug = 'python-co-ban'
ON CONFLICT DO NOTHING;

INSERT INTO lesson (course_id, slug, title, position, body_markdown, published_at)
SELECT id, 'bien-va-gia-tri', 'Biến và giá trị', 2, $markdown$# Đặt tên cho giá trị

Biến cho phép bạn dùng lại một giá trị trong chương trình.

```python
name = "Lan"
print("Xin chào,", name)
```

Thử gán giá trị mới cho `name` rồi chạy lại chương trình.
$markdown$, CURRENT_TIMESTAMP
FROM course
WHERE slug = 'python-co-ban'
ON CONFLICT DO NOTHING;
