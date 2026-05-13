import type { Metadata } from "next";
import "./globals.css";
import { AppShell } from "@/components/app/app-shell";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "장부톡",
  description: "소상공인을 위한 세무자료 정리와 신고 준비 도구",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          <AppShell>{children}</AppShell>
        </Providers>
      </body>
    </html>
  );
}
