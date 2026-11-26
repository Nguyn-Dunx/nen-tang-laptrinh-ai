import Link from "next/link";
import { ArrowLeft } from "lucide-react";

export default function NotFound() {
  return (
    <main id="main-content" className="page-container not-found">
      <span className="not-found-number">404</span>
      <h1>Đường dẫn này chưa có nội dung.</h1>
      <p>Cùng quay lại thư viện và chọn bài học tiếp theo của bạn.</p>
      <Link href="/courses" className="button button-primary">
        <ArrowLeft size={18} /> Về các khóa học
      </Link>
    </main>
  );
}
