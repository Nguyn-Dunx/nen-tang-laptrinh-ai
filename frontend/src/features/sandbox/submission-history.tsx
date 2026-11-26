"use client";

import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, ArrowRight, History, RefreshCw } from "lucide-react";
import { useState } from "react";
import { api, getErrorMessage } from "@/lib/api/client";
import type { SubmissionPage } from "@/lib/api/types";
import {
  formatSubmittedAt,
  submissionLabel,
  submissionTone,
} from "./submission-status";

type SubmissionHistoryProps = {
  exerciseId: number;
  userId: number;
  selectedId: number | null;
  onSelect: (id: number) => void;
};

const PAGE_SIZE = 8;

export function SubmissionHistory({
  exerciseId,
  userId,
  selectedId,
  onSelect,
}: SubmissionHistoryProps) {
  const [page, setPage] = useState(0);
  const history = useQuery({
    queryKey: ["submissions", userId, exerciseId, page],
    queryFn: ({ signal }) =>
      api.get<SubmissionPage>(
        `/exercises/${exerciseId}/submissions?page=${page}&size=${PAGE_SIZE}`,
        { signal },
      ),
  });

  return (
    <section
      className="submission-history"
      aria-labelledby="submission-history-title"
    >
      <div className="section-heading compact">
        <h2 id="submission-history-title">
          <History size={19} />
          Lịch sử của bạn
        </h2>
        <button
          className="button button-ghost button-small"
          onClick={() => history.refetch()}
          disabled={history.isFetching}
        >
          <RefreshCw
            size={14}
            className={history.isFetching ? "spin" : undefined}
          />
          Làm mới
        </button>
      </div>
      {history.isPending && (
        <p className="muted" role="status">
          Đang tải lịch sử bài nộp…
        </p>
      )}
      {history.isError && (
        <p className="error-message" role="alert">
          {getErrorMessage(
            history.error,
            "Chưa tải được lịch sử. Hãy thử lại.",
          )}
        </p>
      )}
      {history.data?.items.length === 0 && (
        <div className="sandbox-empty">
          <History size={26} />
          <p>
            {page === 0
              ? "Bạn chưa nộp bài tập này. Bài làm đầu tiên sẽ xuất hiện ở đây."
              : "Trang này chưa có bài nộp."}
          </p>
        </div>
      )}
      {history.data && history.data.items.length > 0 && (
        <div className="history-table-wrap">
          <table className="history-table">
            <caption className="sr-only">
              Các bài nộp của bạn cho bài tập này
            </caption>
            <thead>
              <tr>
                <th>Bài nộp</th>
                <th>Trạng thái</th>
                <th>Điểm</th>
                <th>Thời gian</th>
                <th>
                  <span className="sr-only">Thao tác</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {history.data.items.map((item) => (
                <tr
                  key={item.submission_id}
                  aria-selected={selectedId === item.submission_id}
                >
                  <td>#{item.submission_id}</td>
                  <td>
                    <span
                      className={`status-badge status-${submissionTone(item.status, item.verdict)}`}
                    >
                      {submissionLabel(item.status, item.verdict)}
                    </span>
                  </td>
                  <td>{item.score ?? "—"}</td>
                  <td>
                    <time dateTime={item.created_at}>
                      {formatSubmittedAt(item.created_at)}
                    </time>
                  </td>
                  <td>
                    <button
                      className="button button-ghost button-small"
                      onClick={() => onSelect(item.submission_id)}
                    >
                      Xem
                      <span className="sr-only">
                        {" "}
                        bài nộp #{item.submission_id}
                      </span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <div className="pagination">
        <button
          className="button button-secondary button-small"
          disabled={page === 0 || history.isFetching}
          onClick={() => setPage((value) => value - 1)}
        >
          <ArrowLeft size={15} />
          Trước
        </button>
        <span>Trang {page + 1}</span>
        <button
          className="button button-secondary button-small"
          disabled={
            !history.data ||
            history.data.items.length < PAGE_SIZE ||
            history.isFetching
          }
          onClick={() => setPage((value) => value + 1)}
        >
          Sau
          <ArrowRight size={15} />
        </button>
      </div>
    </section>
  );
}
