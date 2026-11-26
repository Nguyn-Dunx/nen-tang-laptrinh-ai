import type { CsrfToken } from "./types";

const API_BASE = "/api/v1";
const REQUEST_TIMEOUT_MS = 15_000;

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId?: string;
  readonly detail?: string;

  constructor(
    status: number,
    code: string,
    options: { detail?: string; traceId?: string } = {},
  ) {
    super(options.detail ?? code);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.detail = options.detail;
    this.traceId = options.traceId;
  }
}

type RequestOptions = { signal?: AbortSignal };
type RequestMethod = "GET" | "POST" | "PUT" | "DELETE";

function readString(value: unknown): string | undefined {
  return typeof value === "string" && value.length > 0 ? value : undefined;
}

async function responseError(response: Response): Promise<ApiError> {
  const data: unknown = await response.json().catch(() => null);
  const problem = data && typeof data === "object" ? data : {};
  const code = "code" in problem ? readString(problem.code) : undefined;
  const detail = "detail" in problem ? readString(problem.detail) : undefined;
  const traceId =
    "traceId" in problem ? readString(problem.traceId) : undefined;

  return new ApiError(response.status, code ?? `HTTP_${response.status}`, {
    detail,
    traceId: traceId ?? response.headers.get("X-Request-Id") ?? undefined,
  });
}

async function request<T>(
  path: string,
  method: RequestMethod,
  body: unknown,
  options: RequestOptions = {},
): Promise<T> {
  if (!path.startsWith("/") || path.startsWith("//")) {
    throw new Error("API paths must be relative to /api/v1.");
  }

  const timeout = AbortSignal.timeout(REQUEST_TIMEOUT_MS);
  const signal = options.signal
    ? AbortSignal.any([options.signal, timeout])
    : timeout;
  const headers = new Headers({ Accept: "application/json" });

  try {
    if (method !== "GET") {
      // Authentication rotates CSRF. Read a fresh token for every write.
      const csrf = await request<CsrfToken>("/auth/csrf", "GET", undefined, {
        signal,
      });
      headers.set(csrf.headerName, csrf.token);
    }
    if (body !== undefined) headers.set("Content-Type", "application/json");

    const response = await fetch(`${API_BASE}${path}`, {
      method,
      credentials: "include",
      cache: "no-store",
      headers,
      signal,
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    });

    if (!response.ok) throw await responseError(response);
    if (response.status === 204) return undefined as T;

    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError || options.signal?.aborted) throw error;
    if (timeout.aborted) throw new ApiError(0, "REQUEST_TIMEOUT");
    throw new ApiError(0, "NETWORK_ERROR");
  }
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, "GET", undefined, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, "POST", body, options),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, "PUT", body, options),
  delete: <T>(path: string, options?: RequestOptions) =>
    request<T>(path, "DELETE", undefined, options),
};

const errorMessages: Record<string, string> = {
  UNAUTHENTICATED: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.",
  FORBIDDEN: "Bạn chưa có quyền thực hiện thao tác này. Hãy thử tải lại trang.",
  VALIDATION_ERROR: "Vui lòng kiểm tra lại thông tin đã nhập.",
  CONFLICT:
    "Thông tin đã được sử dụng hoặc vừa thay đổi. Vui lòng kiểm tra lại.",
  NOT_FOUND: "Nội dung này không tồn tại hoặc chưa được xuất bản.",
  EXERCISE_NOT_FOUND: "Bài tập này không còn khả dụng.",
  LANGUAGE_NOT_SUPPORTED: "Ngôn ngữ này chưa được hỗ trợ cho bài tập.",
  SOURCE_CODE_TOO_LARGE: "Mã nguồn vượt quá kích thước cho phép.",
  RATE_LIMITED: "Bạn thao tác quá nhanh. Vui lòng chờ một lúc rồi thử lại.",
  TOO_MANY_PENDING_SUBMISSIONS:
    "Bạn đang có nhiều bài chờ chấm. Hãy chờ kết quả trước khi nộp tiếp.",
  GRADER_UNAVAILABLE: "Dịch vụ chấm bài đang bận. Vui lòng thử lại sau.",
  COURSE_SLUG_TAKEN: "Đường dẫn khóa học đã được sử dụng.",
  LESSON_SLUG_TAKEN: "Đường dẫn bài học đã được sử dụng trong khóa này.",
  COURSE_IS_PUBLISHED: "Hãy thu hồi xuất bản khóa học trước khi chỉnh sửa.",
  LESSON_IS_PUBLISHED: "Hãy thu hồi xuất bản bài học trước khi chỉnh sửa.",
  COURSE_HAS_NO_PUBLISHED_LESSONS:
    "Khóa học cần ít nhất một bài học đã xuất bản.",
  COURSE_REQUIRES_PUBLISHED_LESSON:
    "Khóa học đang xuất bản cần giữ ít nhất một bài học đã xuất bản.",
  INVALID_LESSON_ORDER: "Thứ tự cần chứa đầy đủ mỗi bài học đúng một lần.",
  REQUEST_TIMEOUT: "Kết nối phản hồi chậm. Vui lòng thử lại.",
  NETWORK_ERROR:
    "Chưa kết nối được đến máy chủ. Vui lòng kiểm tra lại kết nối.",
};

export function getErrorMessage(
  error: unknown,
  fallback = "Đã có lỗi xảy ra. Vui lòng thử lại.",
): string {
  if (!(error instanceof ApiError)) return fallback;
  if (errorMessages[error.code]) return errorMessages[error.code];
  if (error.status === 401) return errorMessages.UNAUTHENTICATED;
  if (error.status === 403) return errorMessages.FORBIDDEN;
  if (error.status === 404) return errorMessages.NOT_FOUND;
  if (error.status === 429) return errorMessages.RATE_LIMITED;
  if (error.status < 500 && error.detail && error.detail.length <= 300) {
    return error.detail;
  }
  return fallback;
}
