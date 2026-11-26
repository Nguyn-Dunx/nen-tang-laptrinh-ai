"use client";

import { useQuery } from "@tanstack/react-query";
import {
  ArrowDown,
  ArrowRight,
  BookOpen,
  Code2,
  Search,
  Terminal,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { EmptyState, ErrorState, LoadingState } from "@/components/feedback";
import { SiteFooter } from "@/components/site-footer";
import { catalogQueries, coursePath, type CourseSummary } from "@/lib/catalog";

export function CourseCover({
  course,
  large = false,
}: {
  course: CourseSummary;
  large?: boolean;
}) {
  const python = /python/i.test(course.title);
  return (
    <div
      className={`course-cover ${large ? "cover-large" : ""} ${python ? "cover-python" : "cover-code"}`}
      aria-hidden="true"
    >
      <span className="cover-language">{python ? "PYTHON" : "LẬP TRÌNH"}</span>
      <span className="cover-symbol">
        {python ? "py" : "</>"}
        <span>_</span>
      </span>
      <div className="cover-bottom">
        <Code2 size={20} />
        <span>Học bằng thực hành</span>
        <ArrowRight size={20} />
      </div>
    </div>
  );
}

export function CourseCatalog() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const courses = useQuery(catalogQueries.courses(page));
  const normalized = search.trim().toLocaleLowerCase("vi");
  const filtered =
    courses.data?.items.filter((course) =>
      `${course.title} ${course.summary}`
        .toLocaleLowerCase("vi")
        .includes(normalized),
    ) ?? [];

  return (
    <>
      <main id="main-content">
        <section className="catalog-hero">
          <div className="page-container hero-grid">
            <div className="hero-copy">
              <h1>
                Từng dòng code.
                <br />
                <span>Một bước tiến mới.</span>
              </h1>
              <p>
                Học nền tảng vững, thực hành ngay trong bài học.
                <br className="desktop-break" /> Bắt đầu hành trình lập trình
                theo nhịp của riêng bạn.
              </p>
              <a href="#courses" className="button button-primary button-large">
                Khám phá khóa học <ArrowDown size={18} />
              </a>
              <div className="hero-detail">
                <span>
                  <BookOpen size={17} /> Bài học có hệ thống
                </span>
                <span>
                  <Terminal size={17} /> Bài tập chấm tự động
                </span>
              </div>
            </div>
            <div className="hero-code" aria-label="Ví dụ chương trình Python">
              <div className="code-window-bar">
                <span className="window-dots">
                  <i />
                  <i />
                  <i />
                </span>
                <span>hello_world.py</span>
                <span className="code-language">Python</span>
              </div>
              <pre>
                <code>
                  <span className="code-comment">
                    # Mọi hành trình đều có dòng đầu tiên
                  </span>
                  {"\n\n"}
                  <span className="code-keyword">def</span>
                  {" learn_something_new():\n"}
                  {"    curiosity = "}
                  <span className="code-string">
                    &quot;không ngừng khám phá&quot;
                  </span>
                  {"\n    "}
                  <span className="code-keyword">return</span>
                  {" curiosity\n\n"}
                  <span className="code-function">print</span>
                  {"(learn_something_new())"}
                </code>
              </pre>
              <div className="code-preview-output">
                <Terminal size={15} />
                <span>không ngừng khám phá</span>
              </div>
              <div className="code-caption">
                <span className="caption-dot" /> Từ đọc hiểu đến tự tay viết
                code
              </div>
            </div>
          </div>
        </section>
        <section className="page-container catalog-section" id="courses">
          <div className="section-heading">
            <div>
              <h2>Chọn điểm bắt đầu của bạn</h2>
              <p>Những kiến thức nhỏ, kết nối thành một nền tảng vững chắc.</p>
            </div>
            <label className="search-field">
              <Search size={18} />
              <span className="sr-only">Tìm trong trang khóa học hiện tại</span>
              <input
                type="search"
                placeholder="Tìm khóa học…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </label>
          </div>
          {courses.isPending ? (
            <LoadingState label="Đang mở thư viện khóa học…" />
          ) : courses.isError ? (
            <ErrorState
              error={courses.error}
              retry={() => void courses.refetch()}
            />
          ) : filtered.length ? (
            <div className="course-grid">
              {filtered.map((course) => (
                <Link
                  className="course-card"
                  key={course.slug}
                  href={coursePath(course.slug)}
                >
                  <CourseCover course={course} />
                  <div className="course-card-body">
                    <h3>{course.title}</h3>
                    <p>{course.summary}</p>
                    <span className="course-card-link">
                      Khám phá khóa học <ArrowRight size={18} />
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              title={
                normalized
                  ? "Chưa tìm thấy khóa học phù hợp"
                  : "Thư viện đang được chuẩn bị"
              }
            >
              {normalized
                ? "Thử một từ khóa ngắn hơn hoặc xóa bộ lọc trên trang này."
                : "Các khóa học đã xuất bản sẽ xuất hiện tại đây."}
            </EmptyState>
          )}
          {courses.data && courses.data.totalElements > courses.data.size && (
            <div className="pagination">
              <button
                className="button button-secondary"
                disabled={!page || courses.isFetching}
                onClick={() => setPage(page - 1)}
              >
                Trang trước
              </button>
              <span>
                Trang {page + 1} /{" "}
                {Math.ceil(courses.data.totalElements / courses.data.size)}
              </span>
              <button
                className="button button-secondary"
                disabled={
                  (page + 1) * courses.data.size >=
                    courses.data.totalElements || courses.isFetching
                }
                onClick={() => setPage(page + 1)}
              >
                Trang sau
              </button>
            </div>
          )}
        </section>
        <section className="page-container learning-note">
          <div className="note-icon">
            <Code2 size={28} />
          </div>
          <div>
            <h2>Đọc để hiểu. Viết để nhớ.</h2>
            <p>
              Mỗi bài học kết nối kiến thức với bài tập. Tự viết lời giải, nộp
              bài và dùng kết quả chấm để tiến bộ từng bước.
            </p>
          </div>
          <Link href="/practice" className="text-link">
            Tới không gian thực hành <ArrowRight size={18} />
          </Link>
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
