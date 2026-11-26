"use client";

import { AlertCircle, ArrowRight, BookOpen, RefreshCw } from "lucide-react";
import Link from "next/link";
import { ApiError, getErrorMessage } from "@/lib/api/client";

export function ErrorState({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  const notFound = error instanceof ApiError && error.status === 404;
  return (
    <div className="feedback-panel" role="alert">
      <span className="feedback-icon">
        <AlertCircle size={25} />
      </span>
      <h2>{notFound ? "Nội dung chưa có sẵn" : "Chưa tải được nội dung"}</h2>
      <p>
        {getErrorMessage(
          error,
          "Kết nối bị gián đoạn. Bạn hãy thử tải lại sau một chút.",
        )}
      </p>
      {error instanceof ApiError && error.traceId && (
        <small>Mã tra cứu {error.traceId}</small>
      )}
      {retry && (
        <button className="button button-secondary" onClick={retry}>
          <RefreshCw size={16} /> Thử lại
        </button>
      )}
      {notFound && (
        <Link className="text-link" href="/courses">
          Về các khóa học <ArrowRight size={16} />
        </Link>
      )}
    </div>
  );
}

export function EmptyState({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="feedback-panel">
      <span className="feedback-icon">
        <BookOpen size={25} />
      </span>
      <h2>{title}</h2>
      <p>{children}</p>
    </div>
  );
}

export function LoadingState({
  label = "Đang tải nội dung…",
}: {
  label?: string;
}) {
  return (
    <div className="loading-state" role="status">
      <span className="spinner" />
      <span>{label}</span>
    </div>
  );
}
