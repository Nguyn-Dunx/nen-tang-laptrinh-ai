"use client";

import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Code2,
  List,
  Play,
} from "lucide-react";
import Link from "next/link";
import { EmptyState, ErrorState, LoadingState } from "@/components/feedback";
import { SiteFooter } from "@/components/site-footer";
import { catalogQueries, lessonPath } from "@/lib/catalog";
import { CourseCover } from "./course-catalog";

export function CourseDetail({ courseSlug }: { courseSlug: string }) {
  const course = useQuery(catalogQueries.course(courseSlug));
  if (course.isPending)
    return (
      <main id="main-content" className="page-container content-page">
        <LoadingState />
      </main>
    );
  if (course.isError)
    return (
      <main id="main-content" className="page-container content-page">
        <ErrorState error={course.error} retry={() => void course.refetch()} />
      </main>
    );
  const data = course.data;
  const first = data.lessons[0];

  return (
    <>
      <main id="main-content">
        <section className="course-intro">
          <div className="page-container">
            <Link className="back-link" href="/courses">
              <ArrowLeft size={16} /> Tất cả khóa học
            </Link>
            <div className="course-intro-grid">
              <div>
                <span className="inline-label">
                  <BookOpen size={16} /> {data.lessons.length} bài học
                </span>
                <h1>{data.title}</h1>
                <p>{data.summary}</p>
                {first && (
                  <Link
                    className="button button-primary button-large"
                    href={lessonPath(courseSlug, first.slug)}
                  >
                    Bắt đầu bài đầu tiên <ArrowRight size={18} />
                  </Link>
                )}
              </div>
              <CourseCover course={data} large />
            </div>
          </div>
        </section>
        <section className="page-container course-curriculum">
          <div>
            <div className="section-heading">
              <div>
                <h2>Nội dung khóa học</h2>
                <p>Đi từng bước, xây nền tảng từ những điều đơn giản.</p>
              </div>
              <span className="course-count">
                {data.lessons.length} bài học
              </span>
            </div>
            {data.lessons.length ? (
              <ol className="lesson-list">
                {data.lessons.map((lesson) => (
                  <li key={lesson.slug}>
                    <Link href={lessonPath(courseSlug, lesson.slug)}>
                      <span className="lesson-number">
                        {String(lesson.position).padStart(2, "0")}
                      </span>
                      <div>
                        <h3>{lesson.title}</h3>
                        <span>Bài học</span>
                      </div>
                      <span className="lesson-open">
                        <Play size={15} /> Mở bài học
                      </span>
                      <ArrowRight className="lesson-arrow" size={19} />
                    </Link>
                  </li>
                ))}
              </ol>
            ) : (
              <EmptyState title="Bài học đang được chuẩn bị">
                Nội dung đã xuất bản sẽ được hiển thị tại đây.
              </EmptyState>
            )}
          </div>
          <aside className="course-aside">
            <h3>Một cách học chủ động hơn</h3>
            <div>
              <BookOpen size={21} />
              <p>
                <strong>Hiểu từ nền tảng</strong>
                <span>Đọc giải thích và các ví dụ ngay trong từng bài.</span>
              </p>
            </div>
            <div>
              <Code2 size={21} />
              <p>
                <strong>Luyện tập có phản hồi</strong>
                <span>
                  Mở bài tập liên quan và kiểm tra lời giải bằng test.
                </span>
              </p>
            </div>
            <div>
              <List size={21} />
              <p>
                <strong>Học theo thứ tự</strong>
                <span>Chuyển giữa các bài dễ dàng từ mục lục khóa học.</span>
              </p>
            </div>
          </aside>
        </section>
      </main>
      <SiteFooter />
    </>
  );
}
