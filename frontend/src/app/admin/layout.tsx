import type { ReactNode } from "react";
import { AdminShell } from "@/features/admin/admin-shared";

export default function AdminLayout({ children }: { children: ReactNode }) {
  return (
    <main id="main-content">
      <AdminShell>{children}</AdminShell>
    </main>
  );
}
