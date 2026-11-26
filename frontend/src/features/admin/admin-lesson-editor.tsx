"use client";

import Link from "next/link";
import { ArrowLeft, ArrowUpRight } from "lucide-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { FormEvent } from "react";
import {
  adminApi,
  adminKeys,
  type AdminLesson,
  type LessonUpdate,
} from "./admin-api";
import {
  AdminError,
  MarkdownField,
  PublicationBadge,
  TitleField,
  formValue,
} from "./admin-shared";

export function AdminLessonEditor({
  courseSlug,
  lessonSlug,
}: {
  courseSlug: string;
  lessonSlug: string;
}) {
  const query = useQuery({
    queryKey: adminKeys.lesson(courseSlug, lessonSlug),
    queryFn: () => adminApi.lesson(courseSlug, lessonSlug),
  });
  if (query.isPending) return <p role="status">Đang tải bài học…</p>;
  if (query.isError)
    return (
      <section className="panel">
        <AdminError error={query.error} />
        <button
          className="button button-secondary"
          onClick={() => query.refetch()}
        >
          Tải lại
        </button>
      </section>
    );
  return <LessonEditor courseSlug={courseSlug} lesson={query.data} />;
}

function LessonEditor({
  courseSlug,
  lesson,
}: {
  courseSlug: string;
  lesson: AdminLesson;
}) {
  const queryClient = useQueryClient();
  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: adminKeys.course(courseSlug) }),
      queryClient.invalidateQueries({ queryKey: ["courses"] }),
      queryClient.invalidateQueries({ queryKey: ["course"] }),
      queryClient.invalidateQueries({ queryKey: ["lesson"] }),
      queryClient.invalidateQueries({ queryKey: ["lesson-exercises"] }),
    ]);
  }
  const save = useMutation({
    mutationFn: (body: LessonUpdate) =>
      adminApi.updateLesson(courseSlug, lesson.slug, body),
    onSuccess: refresh,
  });
  const publish = useMutation({
    mutationFn: (published: boolean) =>
      adminApi.publishLesson(courseSlug, lesson.slug, published),
    onSuccess: refresh,
  });
  const isBusy = save.isPending || publish.isPending;

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    save.mutate({
      title: formValue(event.currentTarget, "title"),
      bodyMarkdown: formValue(event.currentTarget, "bodyMarkdown"),
    });
  }

  return (
    <>
      <Link
        className="back-link"
        href={`/admin/courses/${encodeURIComponent(courseSlug)}`}
      >
        <ArrowLeft size={16} aria-hidden="true" /> Về khóa học
      </Link>
      <header className="admin-page-heading">
        <div>
          <PublicationBadge publishedAt={lesson.publishedAt} />
          <h1>{lesson.title}</h1>
          <p>
            Bài {lesson.position} · /{lesson.slug}
          </p>
        </div>
        <Link
          href={`/courses/${encodeURIComponent(courseSlug)}/lessons/${encodeURIComponent(lesson.slug)}`}
          className="button button-secondary"
        >
          Mở bài học <ArrowUpRight size={16} aria-hidden="true" />
        </Link>
      </header>
      <div className="admin-grid admin-lesson-grid">
        <section className="panel">
          <h2>Biên tập bài học</h2>
          {lesson.publishedAt && (
            <p className="notice">
              Thu hồi bài trước khi chỉnh sửa. Backend sẽ kiểm tra điều kiện
              công khai của khóa học.
            </p>
          )}
          <form
            className="admin-form"
            onSubmit={submit}
            key={`${lesson.title}|${lesson.bodyMarkdown}`}
          >
            <fieldset disabled={Boolean(lesson.publishedAt) || isBusy}>
              <TitleField title={lesson.title} />
              <MarkdownField markdown={lesson.bodyMarkdown} />
              <button className="button button-primary" type="submit">
                {save.isPending ? "Đang lưu…" : "Lưu bài học"}
              </button>
            </fieldset>
            <AdminError error={save.error} />
            {save.isSuccess && (
              <p className="success-message" role="status">
                Đã lưu nội dung bài học.
              </p>
            )}
          </form>
        </section>
        <section className="panel admin-publication">
          <h2>Trạng thái bài học</h2>
          <PublicationBadge publishedAt={lesson.publishedAt} />
          <p>
            Khi bài và khóa đều đã xuất bản, học viên có thể đọc nội dung này.
          </p>
          <p>
            Muốn thu hồi bài công khai cuối cùng, hãy thu hồi khóa học trước.
          </p>
          <AdminError error={publish.error} />
          <button
            className={`button ${lesson.publishedAt ? "button-secondary" : "button-primary"}`}
            disabled={isBusy}
            onClick={() => publish.mutate(!lesson.publishedAt)}
          >
            {publish.isPending
              ? "Đang cập nhật…"
              : lesson.publishedAt
                ? "Thu hồi bài học"
                : "Xuất bản bài học"}
          </button>
        </section>
      </div>
    </>
  );
}
