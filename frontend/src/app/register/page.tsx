import type { Metadata } from "next";
import { Suspense } from "react";
import { AuthForm } from "@/features/auth/auth-form";

export const metadata: Metadata = { title: "Tạo tài khoản" };

export default function RegisterPage() {
  return (
    <Suspense
      fallback={
        <main className="auth-page" aria-busy="true">
          Đang tải trang tạo tài khoản…
        </main>
      }
    >
      <AuthForm mode="register" />
    </Suspense>
  );
}
