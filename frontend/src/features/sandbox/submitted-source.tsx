"use client";

import { useQuery } from "@tanstack/react-query";
import { Code2, Copy, LoaderCircle } from "lucide-react";
import { useState } from "react";
import { api, getErrorMessage } from "@/lib/api/client";
import type { SubmissionDetail } from "@/lib/api/types";

type SubmittedSourceProps = {
  submissionId: number;
  userId: number;
  onRestore: (source: string) => void;
};

export function SubmittedSource({
  submissionId,
  userId,
  onRestore,
}: SubmittedSourceProps) {
  const [expanded, setExpanded] = useState(false);
  const [confirmReplace, setConfirmReplace] = useState(false);
  const detail = useQuery({
    queryKey: ["submission-detail", userId, submissionId],
    queryFn: ({ signal }) =>
      api.get<SubmissionDetail>(`/submissions/${submissionId}/detail`, {
        signal,
      }),
    enabled: expanded,
  });

  return (
    <section className="submitted-source">
      <button
        className="button button-ghost button-small"
        aria-expanded={expanded}
        onClick={() => setExpanded((value) => !value)}
      >
        <Code2 size={16} />
        {expanded ? "Ẩn mã đã nộp" : "Xem mã đã nộp"}
      </button>
      {expanded && (
        <div className="submitted-source-content">
          {detail.isPending && (
            <p className="muted">
              <LoaderCircle size={16} className="spin" />
              Đang tải mã…
            </p>
          )}
          {detail.isError && (
            <p className="error-message" role="alert">
              {getErrorMessage(detail.error, "Chưa tải được mã bài nộp.")}
            </p>
          )}
          {detail.data && (
            <>
              <pre className="submitted-source-code">
                <code>{detail.data.source_code}</code>
              </pre>
              {!confirmReplace ? (
                <button
                  className="button button-secondary button-small"
                  onClick={() => setConfirmReplace(true)}
                >
                  <Copy size={15} />
                  Dùng mã này
                </button>
              ) : (
                <div className="sandbox-confirm">
                  <p>
                    Đưa mã bài nộp #{submissionId} vào trình soạn thảo? Mã đang
                    soạn sẽ được thay thế.
                  </p>
                  <button
                    className="button button-primary button-small"
                    onClick={() => {
                      onRestore(detail.data.source_code);
                      setConfirmReplace(false);
                    }}
                  >
                    Thay mã đang soạn
                  </button>
                  <button
                    className="button button-secondary button-small"
                    onClick={() => setConfirmReplace(false)}
                  >
                    Hủy
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      )}
    </section>
  );
}
