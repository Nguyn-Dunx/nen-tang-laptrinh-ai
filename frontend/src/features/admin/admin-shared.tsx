"use client";

import Link from "next/link";
import { BookOpen, LockKeyhole, ShieldCheck } from "lucide-react";
import type { ReactNode } from "react";
import { getErrorMessage } from "@/lib/api/client";
import { useAuth } from "@/features/auth/use-auth";

export function AdminShell({ children }: { children: ReactNode }) {
  const { user, isPending, error } = useAuth();

  if (isPending) {
    return (
      <div className="admin-page">
        <p role="status">Đang kiểm tra quyền truy cập…</p>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="admin-page">
        <section className="panel admin-access">
          <LockKeyhole aria-hidden="true" size={28} />
          <h1>Đăng nhập để quản trị nội dung</h1>
          <p>
            Phiên quản trị viên cho phép biên tập khóa học và xuất bản bài học.
          </p>
          <AdminError error={error} />
          <Link
            className="button button-primary"
            href="/login?returnTo=%2Fadmin%2Fcourses"
          >
            Đăng nhập
          </Link>
        </section>
      </div>
    );
  }

  if (user.role !== "ADMIN") {
    return (
      <div className="admin-page">
        <section className="panel admin-access">
          <ShieldCheck aria-hidden="true" size={28} />
          <h1>Trang dành cho quản trị viên</h1>
          <p>Tài khoản hiện tại chưa có quyền quản trị nội dung.</p>
          <Link className="button button-primary" href="/courses">
            Về khóa học
          </Link>
        </section>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="admin-shell">
        <nav className="admin-breadcrumb" aria-label="Điều hướng quản trị">
          <Link href="/admin/courses">
            <BookOpen size={16} aria-hidden="true" /> Quản trị khóa học
          </Link>
          <span className="status-badge">Quản trị viên</span>
        </nav>
        {children}
      </div>
    </div>
  );
}

export function AdminError({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <p className="error-message" role="alert">
      {getErrorMessage(error, "Chưa thể lưu nội dung. Vui lòng thử lại.")}
    </p>
  );
}

export function PublicationBadge({
  publishedAt,
}: {
  publishedAt: string | null;
}) {
  return (
    <span
      className={`status-badge ${publishedAt ? "status-published" : "status-draft"}`}
    >
      {publishedAt ? "Đã xuất bản" : "Bản nháp"}
    </span>
  );
}

export function SlugField() {
  return (
    <label className="form-field">
      <span>Đường dẫn</span>
      <input
        className="input"
        name="slug"
        required
        maxLength={100}
        pattern="[a-z0-9]+(?:-[a-z0-9]+)*"
        placeholder="python-co-ban"
        autoCapitalize="none"
        spellCheck={false}
      />
      <small>
        Dùng chữ thường không dấu, số và dấu gạch nối. Đường dẫn được cố định
        sau khi tạo.
      </small>
    </label>
  );
}

export function TitleField({ title = "" }: { title?: string }) {
  return (
    <label className="form-field">
      <span>Tiêu đề</span>
      <input
        className="input"
        name="title"
        defaultValue={title}
        required
        maxLength={200}
      />
    </label>
  );
}

export function MarkdownField({ markdown = "" }: { markdown?: string }) {
  return (
    <label className="form-field">
      <span>Nội dung Markdown</span>
      <textarea
        className="textarea admin-markdown"
        name="bodyMarkdown"
        defaultValue={markdown}
        required
        maxLength={50000}
        rows={16}
        spellCheck={false}
        placeholder={
          "# Mục tiêu bài học\n\nGiải thích khái niệm và thêm ví dụ để học viên thực hành."
        }
      />
      <small>
        Hỗ trợ tiêu đề, danh sách, liên kết và khối mã. Tối đa 50.000 ký tự.
      </small>
    </label>
  );
}

export function formValue(form: HTMLFormElement, field: string) {
  return String(new FormData(form).get(field) ?? "");
}
