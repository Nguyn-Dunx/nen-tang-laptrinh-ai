"use client";

import { ArrowUpRight, BookOpen, Code2, LogOut, Menu, X } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/features/auth/use-auth";

export function SiteHeader() {
  const pathname = usePathname();
  const [expandedAt, setExpandedAt] = useState<string | null>(null);
  const [logoutError, setLogoutError] = useState(false);
  const auth = useAuth();
  const expanded = expandedAt === pathname;
  const links = [
    {
      href: "/courses",
      label: "Khóa học",
      icon: BookOpen,
      active: pathname === "/" || pathname.startsWith("/courses"),
    },
    {
      href: "/practice",
      label: "Thực hành",
      icon: Code2,
      active:
        pathname.startsWith("/practice") || pathname.startsWith("/exercises"),
    },
  ];

  return (
    <header className="site-header">
      <div className="header-inner">
        <Link href="/" className="brand" aria-label="EduSky — Trang chủ">
          <span className="brand-mark">
            <Code2 size={23} strokeWidth={2.4} />
          </span>
          EduSky<span className="brand-dot">.</span>
        </Link>
        <nav className="desktop-nav" aria-label="Điều hướng chính">
          {links.map(({ href, label, active }) => (
            <Link
              key={href}
              href={href}
              className={active ? "nav-link active" : "nav-link"}
              aria-current={active ? "page" : undefined}
            >
              {label}
            </Link>
          ))}
          {auth.user?.role === "ADMIN" && (
            <Link
              href="/admin/courses"
              className={
                pathname.startsWith("/admin") ? "nav-link active" : "nav-link"
              }
            >
              Quản trị
            </Link>
          )}
        </nav>
        <div className="header-actions">
          {auth.isPending ? (
            <span
              className="session-loading"
              aria-label="Đang kiểm tra phiên"
            />
          ) : auth.user ? (
            <>
              <span className="account-email" title={auth.user.email}>
                {auth.user.email}
              </span>
              <button
                className="icon-button"
                aria-label="Đăng xuất"
                disabled={auth.isLoggingOut}
                onClick={async () => {
                  setLogoutError(false);
                  try {
                    await auth.logout();
                  } catch {
                    setLogoutError(true);
                  }
                }}
              >
                <LogOut size={19} />
              </button>
            </>
          ) : (
            <>
              <Link className="login-link" href="/login">
                Đăng nhập
              </Link>
              <Link
                className="button button-primary header-cta"
                href="/register"
              >
                Bắt đầu học <ArrowUpRight size={16} />
              </Link>
            </>
          )}
          <button
            className="icon-button mobile-menu-button"
            aria-label={expanded ? "Đóng điều hướng" : "Mở điều hướng"}
            aria-expanded={expanded}
            aria-controls="mobile-navigation"
            onClick={() => setExpandedAt(expanded ? null : pathname)}
          >
            {expanded ? <X size={21} /> : <Menu size={21} />}
          </button>
        </div>
      </div>
      {logoutError && (
        <p className="header-error" role="alert">
          Đăng xuất chưa thành công. Hãy thử lại.
        </p>
      )}
      {expanded && (
        <nav
          id="mobile-navigation"
          className="mobile-nav"
          aria-label="Điều hướng trên điện thoại"
        >
          {links.map(({ href, label, icon: Icon }) => (
            <Link href={href} key={href} onClick={() => setExpandedAt(null)}>
              <Icon size={18} />
              {label}
            </Link>
          ))}
          {auth.user?.role === "ADMIN" && (
            <Link href="/admin/courses" onClick={() => setExpandedAt(null)}>
              Quản trị khóa học
            </Link>
          )}
        </nav>
      )}
    </header>
  );
}
