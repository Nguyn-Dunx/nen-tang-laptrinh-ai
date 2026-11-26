import type { Metadata } from "next";
import "@fontsource/be-vietnam-pro/vietnamese-400.css";
import "@fontsource/be-vietnam-pro/vietnamese-500.css";
import "@fontsource/be-vietnam-pro/vietnamese-600.css";
import "@fontsource/be-vietnam-pro/vietnamese-700.css";
import "@fontsource/be-vietnam-pro/latin-400.css";
import "@fontsource/be-vietnam-pro/latin-500.css";
import "@fontsource/be-vietnam-pro/latin-600.css";
import "@fontsource/be-vietnam-pro/latin-700.css";
import "@fontsource/jetbrains-mono/latin-400.css";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "EduSky — Hiểu từng bước, tự viết từng dòng",
    template: "%s | EduSky",
  },
  description:
    "Học lập trình theo khóa học, thực hành Python và nhận kết quả chấm tự động trên EduSky.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="vi">
      <body>
        <Providers>
          <a className="skip-link" href="#main-content">
            Bỏ qua điều hướng
          </a>
          <SiteHeader />
          {children}
        </Providers>
      </body>
    </html>
  );
}
