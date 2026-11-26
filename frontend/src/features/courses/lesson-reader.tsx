"use client";

import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Check,
  Code2,
  List,
  Terminal,
} from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { ErrorState, LoadingState } from "@/components/feedback";
import { Markdown } from "@/components/markdown";
import {
  catalogQueries,
  coursePath,
  exercisePath,
  lessonPath,
} from "@/lib/catalog";

export function LessonReader({
  courseSlug,
  lessonSlug,
}: {
  courseSlug: string;
  lessonSlug: string;
}) {
  const course = useQuery(catalogQueries.course(courseSlug));
  const lesson = useQuery(catalogQueries.lesson(courseSlug, lessonSlug));
  const exercises = useQuery(catalogQueries.exercises(courseSlug, lessonSlug));
  const [outlineOpen, setOutlineOpen] = useState(false);
  if (lesson.isPending)
    return (
      <main id="main-content" className="page-container content-page">
        <LoadingState label="Đang mở bài học…" />
      </main>
    );
  if (lesson.isError)
    return (
      <main id="main-content" className="page-container content-page">
        <ErrorState error={lesson.error} retry={() => void lesson.refetch()} />
      </main>
    );
  const data = lesson.data;

  return (
    <main id="main-content" className="lesson-layout">
      <button
        className="button button-secondary outline-toggle"
        aria-expanded={outlineOpen}
        aria-controls="course-outline"
        onClick={() => setOutlineOpen(!outlineOpen)}
      >
        <List size={17} />
        {outlineOpen ? "Ẩn mục lục" : "Mục lục khóa học"}
      </button>
      <aside
        id="course-outline"
        className={`lesson-sidebar ${outlineOpen ? "is-open" : ""}`}
      >
        <Link href={coursePath(courseSlug)} className="back-link">
          <ArrowLeft size={16} /> {course.data?.title || "Về khóa học"}
        </Link>
        <div className="outline-heading">
          <BookOpen size={18} />
          <h2>Nội dung khóa học</h2>
        </div>
        {course.isPending ? (
          <LoadingState />
        ) : course.isError ? (
          <ErrorState
            error={course.error}
            retry={() => void course.refetch()}
          />
        ) : (
          <ol className="course-outline">
            {course.data.lessons.map((item) => (
              <li key={item.slug}>
                <Link
                  href={lessonPath(courseSlug, item.slug)}
                  className={item.slug === lessonSlug ? "current" : ""}
                  aria-current={item.slug === lessonSlug ? "page" : undefined}
                  onClick={() => setOutlineOpen(false)}
                >
                  <span>{String(item.position).padStart(2, "0")}</span>
                  <span>{item.title}</span>
                  {item.slug === lessonSlug && (
                    <span className="current-indicator" />
                  )}
                </Link>
              </li>
            ))}
          </ol>
        )}
        <p className="outline-note">
          Kiến thức vững bắt đầu từ từng bài học nhỏ.
        </p>
      </aside>
      <div className="lesson-main">
        <nav className="breadcrumbs" aria-label="Đường dẫn">
          <Link href="/courses">Khóa học</Link>
          <span>/</span>
          <Link href={coursePath(courseSlug)}>
            {course.data?.title || courseSlug}
          </Link>
          <span>/</span>
          <span>Bài {data.position}</span>
        </nav>
        <article className="lesson-article">
          <div className="lesson-title">
            <span className="inline-label">
              <BookOpen size={15} /> Bài {data.position}
            </span>
            <h1>{data.title}</h1>
          </div>
          <Markdown>{data.bodyMarkdown}</Markdown>
        </article>
        <section className="lesson-exercises">
          <div className="section-heading">
            <div>
              <h2>Đến lượt bạn thực hành</h2>
              <p>Đưa kiến thức vừa học vào lời giải của riêng mình.</p>
            </div>
            <Terminal size={22} />
          </div>
          {exercises.isPending ? (
            <LoadingState />
          ) : exercises.isError ? (
            <ErrorState
              error={exercises.error}
              retry={() => void exercises.refetch()}
            />
          ) : exercises.data.length ? (
            <div className="exercise-links">
              {exercises.data.map((exercise) => (
                <Link
                  key={exercise.id}
                  href={exercisePath(exercise.id, courseSlug, lessonSlug)}
                >
                  <span className="exercise-link-icon">
                    <Code2 size={20} />
                  </span>
                  <div>
                    <strong>{exercise.title}</strong>
                    <span>{exercise.language}</span>
                  </div>
                  <ArrowRight size={20} />
                </Link>
              ))}
            </div>
          ) : (
            <p className="lesson-no-exercise">
              <Check size={18} /> Bài này tập trung vào kiến thức nền. Bạn có
              thể chuyển sang bài tiếp theo.
            </p>
          )}
        </section>
        <nav className="lesson-navigation" aria-label="Chuyển bài học">
          {data.previousLessonSlug ? (
            <Link
              href={lessonPath(courseSlug, data.previousLessonSlug)}
              className="button button-secondary"
            >
              <ArrowLeft size={16} /> Bài trước
            </Link>
          ) : (
            <Link
              href={coursePath(courseSlug)}
              className="button button-secondary"
            >
              <ArrowLeft size={16} /> Về khóa học
            </Link>
          )}
          {data.nextLessonSlug ? (
            <Link
              href={lessonPath(courseSlug, data.nextLessonSlug)}
              className="button button-primary"
            >
              Bài tiếp theo <ArrowRight size={16} />
            </Link>
          ) : (
            <Link
              href={coursePath(courseSlug)}
              className="button button-primary"
            >
              Xem lại mục lục <List size={16} />
            </Link>
          )}
        </nav>
      </div>
      <aside className="lesson-companion">
        <span className="companion-icon">
          <Code2 size={26} />
        </span>
        <h2>Học bằng cách làm</h2>
        <p>
          Đọc kỹ ví dụ, thử thay đổi một giá trị và tự dự đoán kết quả trước khi
          mở bài tập.
        </p>
        <hr />
        <p className="small-copy">
          Không cần nhớ mọi cú pháp. Hãy hiểu cách chương trình hoạt động.
        </p>
        <Link href="#main-content" className="text-link">
          Đọc lại từ đầu <ArrowRight size={15} />
        </Link>
      </aside>
    </main>
  );
}
