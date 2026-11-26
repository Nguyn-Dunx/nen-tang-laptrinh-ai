"use client";

import {
  CheckCircle2,
  CircleAlert,
  Clock3,
  LoaderCircle,
  LockKeyhole,
  XCircle,
} from "lucide-react";
import type { SubmissionTestResult, SubmissionView } from "@/lib/api/types";
import {
  isSubmissionPending,
  submissionLabel,
  submissionTone,
} from "./submission-status";

type SubmissionResultsProps = {
  submission: SubmissionView;
  sourceChanged: boolean;
};

function TestResult({ test }: { test: SubmissionTestResult }) {
  return (
    <details
      className={`test-result ${test.passed ? "test-result-success" : "test-result-danger"}`}
    >
      <summary>
        {test.passed ? <CheckCircle2 size={17} /> : <XCircle size={17} />}
        <span>
          Test {test.position} · {test.public ? "Công khai" : "Ẩn"}
        </span>
        {!test.public && (
          <LockKeyhole size={14} aria-label="Dữ liệu test được bảo vệ" />
        )}
        <span className="test-result-verdict">
          {submissionLabel("DONE", test.verdict)}
        </span>
      </summary>
      {test.public ? (
        <div className="test-io">
          <div>
            <span>Đầu vào</span>
            <pre>{test.stdin || "(trống)"}</pre>
          </div>
          <div>
            <span>Kết quả mong đợi</span>
            <pre>{test.expected_output || "(trống)"}</pre>
          </div>
          <div>
            <span>Kết quả thực tế</span>
            <pre>{test.actual_output || "(trống)"}</pre>
          </div>
        </div>
      ) : (
        <p className="muted test-hidden-note">
          Dữ liệu của test ẩn được bảo vệ. Bạn có thể đối chiếu bài làm với đề
          bài và các ví dụ công khai.
        </p>
      )}
    </details>
  );
}

export function SubmissionResults({
  submission,
  sourceChanged,
}: SubmissionResultsProps) {
  const pending = isSubmissionPending(submission.status);
  const tone = submissionTone(submission.status, submission.verdict);
  const Icon = pending
    ? LoaderCircle
    : submission.status === "FAILED"
      ? CircleAlert
      : submission.verdict === "ACCEPTED"
        ? CheckCircle2
        : XCircle;

  return (
    <section
      className="submission-results"
      aria-labelledby="submission-results-title"
    >
      <div className="section-heading compact">
        <h2 id="submission-results-title">Kết quả chấm</h2>
        <span className="muted">Bài nộp #{submission.submission_id}</span>
      </div>
      <div
        className={`submission-summary submission-summary-${tone}`}
        role="status"
        aria-live="polite"
      >
        <Icon size={21} className={pending ? "spin" : undefined} />
        <div>
          <strong>
            {submissionLabel(submission.status, submission.verdict)}
          </strong>
          {pending && (
            <p>
              Hệ thống đang xử lý bài của bạn. Kết quả sẽ được cập nhật tại đây.
            </p>
          )}
          {submission.status === "DONE" && (
            <p>
              {submission.score ?? 0}/{submission.max_score} điểm · Đạt{" "}
              {submission.tests.filter((test) => test.passed).length}/
              {submission.tests.length} test đã chấm
            </p>
          )}
          {submission.status === "FAILED" && (
            <p>
              Bài nộp đã được lưu. Bạn có thể kiểm tra lại trạng thái hoặc thử
              nộp sau khi dịch vụ hoạt động trở lại.
            </p>
          )}
        </div>
      </div>
      {sourceChanged && (
        <p className="sandbox-notice">
          <CircleAlert size={16} />
          Kết quả này thuộc phiên bản mã đã nộp. Bạn đã chỉnh sửa mã trong trình
          soạn thảo.
        </p>
      )}
      {submission.status === "DONE" &&
        (submission.time_ms != null || submission.memory_kb != null) && (
          <p className="submission-metrics">
            <Clock3 size={15} />
            {submission.time_ms != null && <span>{submission.time_ms} ms</span>}
            {submission.memory_kb != null && (
              <span>{submission.memory_kb.toLocaleString("vi-VN")} KB</span>
            )}
          </p>
        )}
      {submission.error_message && (
        <pre className="sandbox-error-output">{submission.error_message}</pre>
      )}
      {submission.tests.length > 0 && (
        <div className="test-results-list">
          {submission.tests.map((test) => (
            <TestResult key={test.position} test={test} />
          ))}
        </div>
      )}
    </section>
  );
}
