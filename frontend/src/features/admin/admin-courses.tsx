"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Plus, Search } from "lucide-react";
import { adminApi, adminKeys } from "./admin-api";
import {
  AdminError,
  PublicationBadge,
  SlugField,
  TitleField,
  formValue,
} from "./admin-shared";

export function AdminCourses() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();
  const courses = useQuery({
    queryKey: adminKeys.list(page),
    queryFn: () => adminApi.list(page),
  });
  const create = useMutation({
    mutationFn: adminApi.createCourse,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKeys.all }),
  });

  async function createCourse(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    try {
      await create.mutateAsync({
        slug: formValue(form, "slug"),
        title: formValue(form, "title"),
        summary: formValue(form, "summary"),
      });
      form.reset();
    } catch {
      // The mutation error is rendered next to this form.
    }
  }

  const visibleCourses = courses.data?.items.filter((course) =>
    `${course.title} ${course.slug}`
      .toLocaleLowerCase("vi")
      .includes(search.toLocaleLowerCase("vi")),
  );

  return (
    <>
      <header className="admin-page-heading">
        <div>
          <h1>Khóa học của bạn</h1>
          <p>Chuẩn bị nội dung, sắp xếp bài học và chọn thời điểm xuất bản.</p>
        </div>
        <Link href="/courses" className="button button-secondary">
          Xem trang học viên <ArrowRight size={16} aria-hidden="true" />
        </Link>
      </header>
      <div className="admin-grid">
        <section
          className="panel admin-course-list"
          aria-labelledby="course-list-title"
        >
          <div className="admin-section-heading">
            <h2 id="course-list-title">Danh sách khóa học</h2>
            <span className="status-badge">
              {courses.data?.totalElements ?? "—"} khóa học
            </span>
          </div>
          <label className="form-field">
            <span>Lọc khóa học trên trang này</span>
            <span className="admin-search">
              <Search size={18} aria-hidden="true" />
              <input
                className="input"
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Tên khóa học hoặc đường dẫn"
              />
            </span>
          </label>
          {courses.isPending && <p role="status">Đang tải khóa học…</p>}
          <AdminError error={courses.error} />
          {courses.isError && (
            <button
              className="button button-secondary"
              onClick={() => courses.refetch()}
            >
              Tải lại
            </button>
          )}
          {visibleCourses?.length === 0 && (
            <p className="empty-state">
              {search
                ? "Chưa tìm thấy khóa học phù hợp trên trang này."
                : "Chưa có khóa học. Tạo bản nháp đầu tiên để bắt đầu."}
            </p>
          )}
          <div className="admin-course-items">
            {visibleCourses?.map((course) => (
              <Link
                className="admin-course-item"
                key={course.slug}
                href={`/admin/courses/${encodeURIComponent(course.slug)}`}
              >
                <div>
                  <PublicationBadge publishedAt={course.publishedAt} />
                  <h3>{course.title}</h3>
                  <p>{course.summary}</p>
                  <small>/{course.slug}</small>
                </div>
                <ArrowRight size={20} aria-hidden="true" />
              </Link>
            ))}
          </div>
          {courses.data && courses.data.totalElements > 12 && (
            <nav className="pagination" aria-label="Phân trang khóa học">
              <button
                className="button button-secondary"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Trang trước
              </button>
              <span>
                Trang {page + 1} / {Math.ceil(courses.data.totalElements / 12)}
              </span>
              <button
                className="button button-secondary"
                disabled={(page + 1) * 12 >= courses.data.totalElements}
                onClick={() => setPage(page + 1)}
              >
                Trang sau
              </button>
            </nav>
          )}
        </section>
        <section className="panel" aria-labelledby="create-course-title">
          <div className="admin-section-heading">
            <Plus size={20} aria-hidden="true" />
            <h2 id="create-course-title">Tạo khóa học</h2>
          </div>
          <p>Khóa mới được lưu dưới dạng bản nháp để bạn chuẩn bị bài học.</p>
          <form className="admin-form" onSubmit={createCourse}>
            <fieldset disabled={create.isPending}>
              <TitleField />
              <SlugField />
              <label className="form-field">
                <span>Mô tả ngắn</span>
                <textarea
                  className="textarea"
                  name="summary"
                  required
                  maxLength={500}
                  rows={4}
                />
              </label>
              <AdminError error={create.error} />
              {create.isSuccess && (
                <p className="success-message" role="status">
                  Đã tạo bản nháp. Bạn có thể mở khóa học từ danh sách.
                </p>
              )}
              <button className="button button-primary" type="submit">
                {create.isPending ? "Đang tạo…" : "Tạo bản nháp"}
              </button>
            </fieldset>
          </form>
        </section>
      </div>
    </>
  );
}
