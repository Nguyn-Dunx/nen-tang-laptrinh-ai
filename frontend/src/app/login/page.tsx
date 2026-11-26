import type { Metadata } from "next";
import { Suspense } from "react";
import { AuthForm } from "@/features/auth/auth-form";

export const metadata: Metadata = { title: "Đăng nhập" };

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <main className="auth-page" aria-busy="true">
          Đang tải trang đăng nhập…
        </main>
      }
    >
      <AuthForm mode="login" />
    </Suspense>
  );
}
