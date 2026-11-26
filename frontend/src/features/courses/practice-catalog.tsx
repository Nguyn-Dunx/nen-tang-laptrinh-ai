"use client";

import { useQueries, useQuery } from "@tanstack/react-query";
import { ArrowRight, Code2, Terminal } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { EmptyState, ErrorState, LoadingState } from "@/components/feedback";
import { catalogQueries, exercisePath } from "@/lib/catalog";

function CourseExercises({ courseSlug }: { courseSlug: string }) {
  const course = useQuery(catalogQueries.course(courseSlug));
  const exercises = useQueries({
    queries: (course.data?.lessons ?? []).map((lesson) =>
      catalogQueries.exercises(courseSlug, lesson.slug),
    ),
  });
  if (course.isPending) return <LoadingState />;
  if (course.isError)
    return (
      <ErrorState error={course.error} retry={() => void course.refetch()} />
    );
  if (exercises.some((query) => query.isPending))
    return <LoadingState label="Đang tìm bài tập của khóa học…" />;
  const error = exercises.find((query) => query.isError);
  const rows = exercises.flatMap((query, i) =>
    (query.data ?? []).map((exercise) => ({
      ...exercise,
      lesson: course.data.lessons[i],
    })),
  );
  return (
    <>
      {error && (
        <ErrorState
          error={error.error}
          retry={() =>
            void Promise.all(
              exercises.filter((q) => q.isError).map((q) => q.refetch()),
            )
          }
        />
      )}
      {rows.length ? (
        <div className="practice-list">
          {rows.map((exercise) => (
            <Link
              className="practice-row"
              key={exercise.id}
              href={exercisePath(exercise.id, courseSlug, exercise.lesson.slug)}
            >
              <span className="practice-symbol">
                <Code2 size={23} />
              </span>
              <div>
                <span className="practice-lesson">{exercise.lesson.title}</span>
                <h2>{exercise.title}</h2>
              </div>
              <span className="language-badge">{exercise.language}</span>
              <ArrowRight size={21} />
            </Link>
          ))}
        </div>
      ) : (
        !error && (
          <EmptyState title="Chưa có bài tập trong khóa học">
            Hãy đọc bài học trước hoặc chọn một khóa học khác.
          </EmptyState>
        )
      )}
    </>
  );
}

export function PracticeCatalog() {
  const courses = useQuery(catalogQueries.courses());
  const [selection, setSelection] = useState("");
  const selected = selection || courses.data?.items[0]?.slug || "";
  return (
    <main id="main-content" className="page-container practice-page">
      <div className="practice-heading">
        <div>
          <div className="practice-heading-icon">
            <Terminal size={25} />
          </div>
          <h1>Biến kiến thức thành kỹ năng.</h1>
          <p>
            Chọn một bài tập, tự viết lời giải và nhận phản hồi từ bộ chấm tự
            động.
          </p>
        </div>
      </div>
      {courses.isPending ? (
        <LoadingState />
      ) : courses.isError ? (
        <ErrorState
          error={courses.error}
          retry={() => void courses.refetch()}
        />
      ) : courses.data.items.length ? (
        <>
          <div className="practice-toolbar">
            <label htmlFor="practice-course">Bài tập theo khóa học</label>
            <select
              className="input"
              id="practice-course"
              value={selected}
              onChange={(e) => setSelection(e.target.value)}
            >
              {courses.data.items.map((course) => (
                <option key={course.slug} value={course.slug}>
                  {course.title}
                </option>
              ))}
            </select>
          </div>
          <CourseExercises key={selected} courseSlug={selected} />
        </>
      ) : (
        <EmptyState title="Thư viện thực hành đang được chuẩn bị">
          Các bài tập sẽ xuất hiện cùng khóa học đã xuất bản.
        </EmptyState>
      )}
    </main>
  );
}
