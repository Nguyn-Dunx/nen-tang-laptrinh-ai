"use client";

import { useQuery } from "@tanstack/react-query";
import {
  ArrowLeft,
  BookOpen,
  Check,
  Code2,
  FileCode2,
  History,
  LoaderCircle,
  LogIn,
  RefreshCw,
  RotateCcw,
  Send,
  Terminal,
} from "lucide-react";
import dynamic from "next/dynamic";
import Link from "next/link";
import { useState } from "react";
import { Markdown } from "@/components/markdown";
import { useAuth } from "@/features/auth/use-auth";
import { api, getErrorMessage } from "@/lib/api/client";
import type { ExerciseView } from "@/lib/api/types";
import { SubmissionHistory } from "./submission-history";
import { SubmissionResults } from "./submission-results";
import { SubmittedSource } from "./submitted-source";
import { useDraftCode } from "./use-draft-code";
import { useSubmission } from "./use-submission";

const CodeEditor = dynamic(() => import("./code-editor"), {
  ssr: false,
  loading: () => (
    <div className="editor-loading" role="status">
      <LoaderCircle size={22} className="spin" />
      Đang mở trình soạn thảo…
    </div>
  ),
});

type ExerciseWorkspaceProps = {
  exerciseId: number;
  courseSlug?: string;
  lessonSlug?: string;
};

function PublicExamples({ exercise }: { exercise: ExerciseView }) {
  return (
    <section
      className="sandbox-examples"
      aria-labelledby="public-examples-title"
    >
      <h2 id="public-examples-title">Ví dụ công khai</h2>
      {exercise.publicTests.map((test) => (
        <div className="sandbox-example" key={test.position}>
          <h3>Test {test.position}</h3>
          <div className="example-columns">
            <div>
              <span>Đầu vào</span>
              <pre>{test.stdin || "(trống)"}</pre>
            </div>
            <div>
              <span>Đầu ra mong đợi</span>
              <pre>{test.expectedOutput || "(trống)"}</pre>
            </div>
          </div>
        </div>
      ))}
      {exercise.publicTests.length === 0 && (
        <p className="muted">Bài tập này chưa có test công khai.</p>
      )}
    </section>
  );
}

function WorkspaceBody({
  exercise,
  userId,
  loginHref,
}: {
  exercise: ExerciseView;
  userId: number | undefined;
  loginHref: string;
}) {
  const [code, setCode] = useDraftCode(
    exercise.id,
    userId,
    exercise.starterCode,
  );
  const [tab, setTab] = useState<"results" | "history">("results");
  const [confirmReset, setConfirmReset] = useState(false);
  const grading = useSubmission(exercise.id, userId);
  const submitDisabled = !userId || !code.trim() || grading.pending;
  const sourceChanged =
    grading.active?.source !== undefined && grading.active.source !== code;

  async function submit() {
    if (submitDisabled) return;
    setTab("results");
    await grading.submit(code);
  }

  return (
    <div className="workspace-grid">
      <aside className="workspace-statement">
        <div className="workspace-pane-heading">
          <BookOpen size={18} />
          <h2>Đề bài</h2>
        </div>
        <div className="workspace-statement-body">
          <Markdown>{exercise.statementMarkdown}</Markdown>
          <PublicExamples exercise={exercise} />
        </div>
      </aside>

      <section className="workspace-editor" aria-label="Soạn mã và chấm bài">
        <div
          className="editor-shell"
          onKeyDown={(event) => {
            if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
              event.preventDefault();
              void submit();
            }
          }}
        >
          <div className="editor-toolbar">
            <span>
              <FileCode2 size={17} />
              main.py
            </span>
            <span className="editor-language">Python</span>
            <button
              className="button button-editor button-small"
              disabled={code === exercise.starterCode}
              onClick={() => setConfirmReset(true)}
              title="Khôi phục mã khởi đầu"
            >
              <RotateCcw size={15} />
              Đặt lại
            </button>
          </div>
          {confirmReset && (
            <div className="sandbox-confirm editor-confirm">
              <p>
                Khôi phục mã khởi đầu của bài tập? Mã đang soạn sẽ được thay
                thế.
              </p>
              <button
                className="button button-primary button-small"
                onClick={() => {
                  setCode(exercise.starterCode);
                  setConfirmReset(false);
                }}
              >
                Khôi phục
              </button>
              <button
                className="button button-secondary button-small"
                onClick={() => setConfirmReset(false)}
              >
                Hủy
              </button>
            </div>
          )}
          <CodeEditor value={code} onChange={setCode} />
          <div className="editor-footer">
            <span>
              <Check size={13} />
              Bản nháp trong tab này
            </span>
            <span>
              {new TextEncoder().encode(code).length.toLocaleString("vi-VN")}{" "}
              byte · UTF-8
            </span>
          </div>
        </div>

        <div className="sandbox-submit-row">
          <p className="muted">Nộp mã để chấm trên bộ test của bài tập.</p>
          <button
            className="button button-primary"
            disabled={submitDisabled}
            onClick={() => void submit()}
          >
            {grading.pending ? (
              <LoaderCircle size={17} className="spin" />
            ) : (
              <Send size={17} />
            )}
            {grading.create.isPending
              ? "Đang nộp…"
              : grading.pending
                ? "Đang chấm…"
                : "Nộp bài"}
            <kbd>⌘/Ctrl ↵</kbd>
          </button>
        </div>
        {!userId && (
          <div className="sandbox-signin">
            <LogIn size={20} />
            <div>
              <strong>Đăng nhập để nhận kết quả chấm</strong>
              <p>Bản nháp được giữ trong tab này khi bạn đăng nhập.</p>
            </div>
            <Link
              href={loginHref}
              className="button button-secondary button-small"
            >
              Đăng nhập
            </Link>
          </div>
        )}
        {grading.create.isError && (
          <div className="error-message" role="alert">
            <p>
              {getErrorMessage(
                grading.create.error,
                "Chưa xác nhận được bài nộp. Hãy kiểm tra lịch sử trước khi thử lại.",
              )}
            </p>
            <button
              className="button button-ghost button-small"
              onClick={() => setTab("history")}
            >
              Kiểm tra lịch sử bài nộp
            </button>
          </div>
        )}

        <div
          className="sandbox-tabs"
          role="tablist"
          aria-label="Kết quả và lịch sử bài nộp"
          onKeyDown={(event) => {
            if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key))
              return;
            event.preventDefault();
            const nextTab =
              event.key === "Home"
                ? "results"
                : event.key === "End"
                  ? "history"
                  : tab === "results"
                    ? "history"
                    : "results";
            setTab(nextTab);
            event.currentTarget
              .querySelector<HTMLButtonElement>(`#${nextTab}-tab`)
              ?.focus();
          }}
        >
          <button
            id="results-tab"
            role="tab"
            tabIndex={tab === "results" ? 0 : -1}
            aria-selected={tab === "results"}
            aria-controls="results-panel"
            className={tab === "results" ? "is-active" : ""}
            onClick={() => setTab("results")}
          >
            <Terminal size={17} />
            Kết quả
          </button>
          <button
            id="history-tab"
            role="tab"
            tabIndex={tab === "history" ? 0 : -1}
            aria-selected={tab === "history"}
            aria-controls="history-panel"
            className={tab === "history" ? "is-active" : ""}
            onClick={() => setTab("history")}
          >
            <History size={17} />
            Lịch sử nộp
          </button>
        </div>
        <div
          id="results-panel"
          role="tabpanel"
          aria-labelledby="results-tab"
          hidden={tab !== "results"}
          className="sandbox-tab-panel"
        >
          {!grading.active && (
            <div className="sandbox-empty">
              <Code2 size={30} />
              <h2>Sẵn sàng cho bài nộp đầu tiên</h2>
              <p>
                Viết mã, đọc ví dụ và nộp bài. Bạn sẽ thấy điểm cùng kết quả
                từng test tại đây.
              </p>
            </div>
          )}
          {grading.active && grading.submission.isPending && (
            <p className="muted" role="status">
              <LoaderCircle size={17} className="spin" />
              Đang nhận trạng thái bài nộp #{grading.active.id}…
            </p>
          )}
          {grading.submission.isError && (
            <div className="error-message" role="alert">
              <p>
                {getErrorMessage(
                  grading.submission.error,
                  "Chưa cập nhật được kết quả chấm.",
                )}
              </p>
              <button
                className="button button-secondary button-small"
                onClick={grading.resume}
              >
                <RefreshCw size={15} />
                Kiểm tra lại trạng thái
              </button>
            </div>
          )}
          {grading.submission.data && (
            <SubmissionResults
              submission={grading.submission.data}
              sourceChanged={sourceChanged}
            />
          )}
          {grading.watchPaused && (
            <div className="sandbox-notice">
              <p>
                Bài nộp vẫn đang được xử lý. Theo dõi tự động đã tạm dừng sau 90
                giây.
              </p>
              <button
                className="button button-secondary button-small"
                onClick={grading.resume}
              >
                <RefreshCw size={15} />
                Tiếp tục theo dõi
              </button>
            </div>
          )}
          {userId && grading.active && (
            <SubmittedSource
              key={grading.active.id}
              userId={userId}
              submissionId={grading.active.id}
              onRestore={setCode}
            />
          )}
        </div>
        <div
          id="history-panel"
          role="tabpanel"
          aria-labelledby="history-tab"
          hidden={tab !== "history"}
          className="sandbox-tab-panel"
        >
          {userId ? (
            <SubmissionHistory
              exerciseId={exercise.id}
              userId={userId}
              selectedId={grading.active?.id ?? null}
              onSelect={(id) => {
                grading.select(id);
                setTab("results");
              }}
            />
          ) : (
            <p className="muted">Đăng nhập để xem các bài nộp của bạn.</p>
          )}
        </div>
      </section>
    </div>
  );
}

export function ExerciseWorkspace({
  exerciseId,
  courseSlug,
  lessonSlug,
}: ExerciseWorkspaceProps) {
  const auth = useAuth();
  const exercise = useQuery({
    queryKey: ["exercise", exerciseId],
    queryFn: ({ signal }) =>
      api.get<ExerciseView>(`/exercises/${exerciseId}`, { signal }),
  });
  const backHref =
    courseSlug && lessonSlug
      ? `/courses/${encodeURIComponent(courseSlug)}/lessons/${encodeURIComponent(lessonSlug)}`
      : "/courses";
  const search = new URLSearchParams();
  if (courseSlug) search.set("course", courseSlug);
  if (lessonSlug) search.set("lesson", lessonSlug);
  const returnTo = `/exercises/${exerciseId}${search.size ? `?${search}` : ""}`;
  const loginHref = `/login?returnTo=${encodeURIComponent(returnTo)}`;
  const difficulty: Record<string, string> = {
    EASY: "Cơ bản",
    MEDIUM: "Trung bình",
    HARD: "Nâng cao",
    easy: "Cơ bản",
    medium: "Trung bình",
    hard: "Nâng cao",
    beginner: "Cơ bản",
    intermediate: "Trung bình",
    advanced: "Nâng cao",
  };

  return (
    <main id="main-content" className="workspace-page">
      <div className="workspace-header">
        <Link href={backHref} className="back-link">
          <ArrowLeft size={16} />
          {courseSlug && lessonSlug ? "Quay lại bài học" : "Khám phá khóa học"}
        </Link>
        {exercise.data && (
          <div className="workspace-title">
            <div>
              <h1>{exercise.data.title}</h1>
              <p>
                <span className="status-badge status-neutral">
                  {difficulty[exercise.data.difficulty] ||
                    exercise.data.difficulty}
                </span>
                <span>Python</span>
                <span>{exercise.data.maxScore} điểm</span>
              </p>
            </div>
            <span className="workspace-mode">
              <Code2 size={18} />
              Không gian thực hành
            </span>
          </div>
        )}
      </div>
      {(exercise.isPending || auth.isPending) && (
        <div className="workspace-loading" role="status">
          <LoaderCircle size={24} className="spin" />
          <p>Đang chuẩn bị bài tập và trình soạn thảo…</p>
        </div>
      )}
      {exercise.isError && (
        <div className="panel state-panel">
          <Code2 size={36} aria-hidden="true" />
          <h1>Chưa mở được bài tập</h1>
          <p>
            {getErrorMessage(
              exercise.error,
              "Bài tập có thể chưa được xuất bản hoặc kết nối đang gián đoạn.",
            )}
          </p>
          <button
            className="button button-primary"
            onClick={() => exercise.refetch()}
          >
            <RefreshCw size={17} />
            Thử lại
          </button>
        </div>
      )}
      {exercise.data && !auth.isPending && (
        <WorkspaceBody
          key={`${exerciseId}:${auth.user?.id ?? "guest"}`}
          exercise={exercise.data}
          userId={auth.user?.id}
          loginHref={loginHref}
        />
      )}
    </main>
  );
}
