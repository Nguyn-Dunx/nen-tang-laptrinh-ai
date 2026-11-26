import { Code2 } from "lucide-react";
import Link from "next/link";

export function SiteFooter() {
  return (
    <footer className="site-footer">
      <div className="page-container footer-inner">
        <div>
          <Link href="/" className="brand">
            <Code2 size={21} />
            EduSky<span className="brand-dot">.</span>
          </Link>
          <p>Hiểu từng bước. Tự viết từng dòng.</p>
        </div>
        <nav aria-label="Điều hướng cuối trang">
          <Link href="/courses">Khám phá khóa học</Link>
          <Link href="/practice">Luyện tập lập trình</Link>
        </nav>
        <span>Không gian học lập trình của bạn.</span>
      </div>
    </footer>
  );
}
