"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  ArrowUpRight,
  Plus,
} from "lucide-react";
import type { FormEvent } from "react";
import {
  adminApi,
  adminKeys,
  type AdminCourse,
  type CourseUpdate,
} from "./admin-api";
import {
  AdminError,
  MarkdownField,
  PublicationBadge,
  SlugField,
  TitleField,
  formValue,
} from "./admin-shared";

export function AdminCourseEditor({ courseSlug }: { courseSlug: string }) {
  const query = useQuery({
    queryKey: adminKeys.course(courseSlug),
    queryFn: () => adminApi.course(courseSlug),
  });
  if (query.isPending) return <p role="status">Đang tải khóa học…</p>;
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
  return <CourseEditor course={query.data} />;
}

function CourseEditor({ course }: { course: AdminCourse }) {
  const queryClient = useQueryClient();
  async function refresh() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: adminKeys.all }),
      queryClient.invalidateQueries({ queryKey: ["courses"] }),
      queryClient.invalidateQueries({ queryKey: ["course"] }),
      queryClient.invalidateQueries({ queryKey: ["lesson"] }),
      queryClient.invalidateQueries({ queryKey: ["lesson-exercises"] }),
    ]);
  }
  const update = useMutation({
    mutationFn: (data: CourseUpdate) =>
      adminApi.updateCourse(course.slug, data),
    onSuccess: refresh,
  });
  const publish = useMutation({
    mutationFn: (published: boolean) =>
      adminApi.publishCourse(course.slug, published),
    onSuccess: refresh,
  });
  const reorder = useMutation({
    mutationFn: (slugs: string[]) => adminApi.orderLessons(course.slug, slugs),
    onSuccess: refresh,
  });
  const isBusy = update.isPending || publish.isPending || reorder.isPending;

  function saveCourse(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    update.mutate({
      title: formValue(event.currentTarget, "title"),
      summary: formValue(event.currentTarget, "summary"),
    });
  }

  function moveLesson(index: number, direction: number) {
    const slugs = course.lessons.map((lesson) => lesson.slug);
    const target = index + direction;
    [slugs[index], slugs[target]] = [slugs[target], slugs[index]];
    reorder.mutate(slugs);
  }

  return (
    <>
      <Link className="back-link" href="/admin/courses">
        <ArrowLeft size={16} aria-hidden="true" /> Tất cả khóa học
      </Link>
      <header className="admin-page-heading">
        <div>
          <PublicationBadge publishedAt={course.publishedAt} />
          <h1>{course.title}</h1>
          <p>/{course.slug}</p>
        </div>
        <Link
          className="button button-secondary"
          href={`/courses/${encodeURIComponent(course.slug)}`}
        >
          Mở trang học viên <ArrowUpRight size={16} aria-hidden="true" />
        </Link>
      </header>
      <div className="admin-grid">
        <div className="admin-stack">
          <section className="panel" aria-labelledby="course-details-title">
            <h2 id="course-details-title">Thông tin khóa học</h2>
            {course.publishedAt && (
              <p className="notice">
                Thu hồi khóa học để chỉnh sửa thông tin. Nội dung sẽ được ẩn
                khỏi danh mục công khai.
              </p>
            )}
            <form
              className="admin-form"
              onSubmit={saveCourse}
              key={`${course.title}|${course.summary}`}
            >
              <fieldset disabled={Boolean(course.publishedAt) || isBusy}>
                <TitleField title={course.title} />
                <label className="form-field">
                  <span>Mô tả ngắn</span>
                  <textarea
                    className="textarea"
                    name="summary"
                    defaultValue={course.summary}
                    required
                    rows={4}
                    maxLength={500}
                  />
                </label>
                <button className="button button-primary" type="submit">
                  {update.isPending ? "Đang lưu…" : "Lưu thay đổi"}
                </button>
              </fieldset>
              <AdminError error={update.error} />
              {update.isSuccess && (
                <p className="success-message" role="status">
                  Đã lưu thông tin khóa học.
                </p>
              )}
            </form>
          </section>
          <section className="panel" aria-labelledby="course-lessons-title">
            <div className="admin-section-heading">
              <h2 id="course-lessons-title">Bài học</h2>
              <span className="status-badge">{course.lessons.length} bài</span>
            </div>
            <p>Dùng các nút lên và xuống để cập nhật thứ tự học.</p>
            <AdminError error={reorder.error} />
            {course.lessons.length === 0 && (
              <p className="empty-state">
                Thêm bài học đầu tiên từ biểu mẫu bên cạnh.
              </p>
            )}
            <ol className="admin-lesson-list">
              {course.lessons.map((lesson, index) => (
                <li key={lesson.slug}>
                  <span className="admin-lesson-position">{index + 1}</span>
                  <Link
                    className="admin-lesson-title"
                    href={`/admin/courses/${encodeURIComponent(course.slug)}/lessons/${encodeURIComponent(lesson.slug)}`}
                  >
                    <strong>{lesson.title}</strong>
                    <PublicationBadge publishedAt={lesson.publishedAt} />
                  </Link>
                  <div className="admin-order-actions">
                    <button
                      className="button button-secondary icon-button"
                      disabled={index === 0 || isBusy}
                      title={`Đưa ${lesson.title} lên`}
                      aria-label={`Đưa ${lesson.title} lên`}
                      onClick={() => moveLesson(index, -1)}
                    >
                      <ArrowUp size={16} aria-hidden="true" />
                    </button>
                    <button
                      className="button button-secondary icon-button"
                      disabled={index === course.lessons.length - 1 || isBusy}
                      title={`Đưa ${lesson.title} xuống`}
                      aria-label={`Đưa ${lesson.title} xuống`}
                      onClick={() => moveLesson(index, 1)}
                    >
                      <ArrowDown size={16} aria-hidden="true" />
                    </button>
                  </div>
                </li>
              ))}
            </ol>
          </section>
        </div>
        <div className="admin-stack">
          <section className="panel" aria-labelledby="publication-title">
            <h2 id="publication-title">Xuất bản</h2>
            <p>
              Khóa học cần có ít nhất một bài đã xuất bản để hiển thị cho học
              viên.
            </p>
            <AdminError error={publish.error} />
            <button
              className={`button ${course.publishedAt ? "button-secondary" : "button-primary"}`}
              disabled={isBusy}
              onClick={() => publish.mutate(!course.publishedAt)}
            >
              {publish.isPending
                ? "Đang cập nhật…"
                : course.publishedAt
                  ? "Thu hồi khóa học"
                  : "Xuất bản khóa học"}
            </button>
          </section>
          <NewLesson courseSlug={course.slug} onCreated={refresh} />
        </div>
      </div>
    </>
  );
}

function NewLesson({
  courseSlug,
  onCreated,
}: {
  courseSlug: string;
  onCreated: () => Promise<void>;
}) {
  const create = useMutation({
    mutationFn: (body: Parameters<typeof adminApi.createLesson>[1]) =>
      adminApi.createLesson(courseSlug, body),
    onSuccess: onCreated,
  });
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await create.mutateAsync({
        slug: formValue(form, "slug"),
        title: formValue(form, "title"),
        bodyMarkdown: formValue(form, "bodyMarkdown"),
      });
      form.reset();
    } catch {
      // The mutation error is rendered beside the form.
    }
  }
  return (
    <section className="panel" aria-labelledby="new-lesson-title">
      <div className="admin-section-heading">
        <Plus size={20} aria-hidden="true" />
        <h2 id="new-lesson-title">Thêm bài học</h2>
      </div>
      <p>Bài mới được thêm vào cuối khóa dưới dạng bản nháp.</p>
      <form className="admin-form" onSubmit={submit}>
        <fieldset disabled={create.isPending}>
          <TitleField />
          <SlugField />
          <MarkdownField />
          <AdminError error={create.error} />
          {create.isSuccess && (
            <p role="status" className="success-message">
              Đã thêm bài học mới.
            </p>
          )}
          <button className="button button-primary" type="submit">
            {create.isPending ? "Đang thêm…" : "Thêm bản nháp"}
          </button>
        </fieldset>
      </form>
    </section>
  );
}
