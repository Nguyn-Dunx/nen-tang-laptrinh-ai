export function submissionLabel(status: string, verdict?: string | null) {
  if (status === "QUEUED") return "Đang chờ chấm";
  if (status === "RUNNING") return "Đang chấm bài";
  if (status === "FAILED") return "Hệ thống chấm gặp sự cố";

  const labels: Record<string, string> = {
    ACCEPTED: "Bài làm chính xác",
    WRONG_ANSWER: "Sai kết quả",
    TIME_LIMIT: "Vượt thời gian",
    MEMORY_LIMIT: "Vượt bộ nhớ",
    RUNTIME_ERROR: "Lỗi khi chạy",
    COMPILE_ERROR: "Lỗi cú pháp hoặc biên dịch",
  };
  return (verdict && labels[verdict]) || "Đã chấm xong";
}

export function submissionTone(status: string, verdict?: string | null) {
  if (status === "QUEUED" || status === "RUNNING") return "pending";
  if (status === "FAILED") return "warning";
  return verdict === "ACCEPTED" ? "success" : "danger";
}

export function isSubmissionPending(status: string) {
  return status === "QUEUED" || status === "RUNNING";
}

export function formatSubmittedAt(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
