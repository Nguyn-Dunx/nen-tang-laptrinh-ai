"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Check, Eye, EyeOff, ShieldCheck } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ApiError, api, getErrorMessage } from "@/lib/api/client";
import type { Account, Credentials } from "@/lib/api/types";
import { updateSessionCache } from "./use-auth";

export function safeReturnTo(value: string | null): string {
  if (!value?.startsWith("/") || value.startsWith("//")) return "/courses";
  if (/[\\\u0000-\u001f\u007f]/.test(value)) return "/courses";

  const destination = new URL(value, "https://edusky.local");
  if (destination.origin !== "https://edusky.local") return "/courses";
  if (["/login", "/register"].includes(destination.pathname)) return "/courses";
  return `${destination.pathname}${destination.search}${destination.hash}`;
}

export function AuthForm({ mode }: { mode: "login" | "register" }) {
  const isRegistration = mode === "register";
  const queryClient = useQueryClient();
  const router = useRouter();
  const search = useSearchParams();
  const returnTo = safeReturnTo(search.get("returnTo"));
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [validationMessage, setValidationMessage] = useState("");

  const authenticate = useMutation({
    mutationFn: async (credentials: Credentials) => {
      await api.post<void | Account>(`/auth/${mode}`, credentials);
      const account = await api.get<Account>("/auth/me");
      await updateSessionCache(queryClient, account);
    },
    onSuccess: () => {
      router.replace(returnTo);
      router.refresh();
    },
  });

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const passwordLength = Array.from(password).length;
    if (isRegistration && passwordLength < 15) {
      setValidationMessage("Mật khẩu cần có ít nhất 15 ký tự.");
      return;
    }
    if (passwordLength > 128) {
      setValidationMessage("Mật khẩu tối đa 128 ký tự.");
      return;
    }
    setValidationMessage("");
    authenticate.mutate({ email: email.trim(), password });
  }

  const error =
    validationMessage ||
    (authenticate.error instanceof ApiError && authenticate.error.status === 401
      ? "Email hoặc mật khẩu chưa đúng. Vui lòng kiểm tra lại."
      : authenticate.error instanceof ApiError &&
          authenticate.error.status === 409
        ? "Email này đã có tài khoản. Bạn có thể đăng nhập để tiếp tục."
        : authenticate.error
          ? getErrorMessage(authenticate.error)
          : "");
  const alternateHref = `${isRegistration ? "/login" : "/register"}?returnTo=${encodeURIComponent(returnTo)}`;

  return (
    <main className="auth-page" id="main-content">
      <section className="auth-intro" aria-labelledby="auth-intro-title">
        <h1 id="auth-intro-title">
          Một nơi để học.
          <br />
          Rất nhiều cách để tiến bộ.
        </h1>
        <p>
          Đọc bài học, thử sức với Python và nhận kết quả chấm ngay trong cùng
          một không gian.
        </p>
        <ul className="auth-benefits">
          <li>
            <Check size={18} aria-hidden="true" /> Bài học được sắp xếp theo
            từng bước
          </li>
          <li>
            <Check size={18} aria-hidden="true" /> Thực hành và xem kết quả từng
            test
          </li>
          <li>
            <Check size={18} aria-hidden="true" /> Xem lại mã nguồn và lịch sử
            nộp của bạn
          </li>
        </ul>
      </section>

      <section className="auth-card" aria-labelledby="auth-title">
        <div className="auth-card-heading">
          <h2 id="auth-title">
            {isRegistration
              ? "Bắt đầu hành trình của bạn"
              : "Chào mừng bạn quay lại"}
          </h2>
          <p>
            {isRegistration
              ? "Tạo tài khoản để nộp bài và theo dõi lịch sử thực hành."
              : "Đăng nhập để tiếp tục thực hành và xem bài đã nộp."}
          </p>
        </div>

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-field">
            <label htmlFor="auth-email">Email</label>
            <input
              className="input"
              id="auth-email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="ban@example.com"
              maxLength={254}
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={authenticate.isPending}
              required
            />
          </div>

          <div className="form-field">
            <label htmlFor="auth-password">Mật khẩu</label>
            <div className="password-input-wrap">
              <input
                className="input"
                id="auth-password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete={
                  isRegistration ? "new-password" : "current-password"
                }
                placeholder={
                  isRegistration ? "Ít nhất 15 ký tự" : "Nhập mật khẩu của bạn"
                }
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                aria-describedby={isRegistration ? "password-help" : undefined}
                disabled={authenticate.isPending}
                required
              />
              <button
                className="password-toggle"
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
                aria-pressed={showPassword}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
            {isRegistration && (
              <p className="field-help" id="password-help">
                Từ 15 đến 128 ký tự. Bạn có thể dùng một cụm từ dễ nhớ.
              </p>
            )}
          </div>

          {error && (
            <p className="error-message" role="alert">
              {error}
            </p>
          )}
          {authenticate.error instanceof ApiError &&
            authenticate.error.traceId && (
              <small className="error-reference">
                Mã hỗ trợ {authenticate.error.traceId}
              </small>
            )}

          <button
            className="button button-primary"
            type="submit"
            disabled={authenticate.isPending}
          >
            {authenticate.isPending
              ? "Đang xử lý…"
              : isRegistration
                ? "Tạo tài khoản"
                : "Đăng nhập"}
            {!authenticate.isPending && (
              <ArrowRight size={18} aria-hidden="true" />
            )}
          </button>
        </form>

        <p className="auth-alternate">
          {isRegistration ? "Bạn đã có tài khoản?" : "Bạn mới đến EduSky?"}{" "}
          <Link href={alternateHref}>
            {isRegistration ? "Đăng nhập" : "Tạo tài khoản"}
          </Link>
        </p>
        <p className="auth-security">
          <ShieldCheck size={16} aria-hidden="true" /> Chỉ bạn xem được lịch sử
          và mã nguồn đã nộp.
        </p>
      </section>
    </main>
  );
}
