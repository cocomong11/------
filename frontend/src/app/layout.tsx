import type { Metadata } from "next";
import "./globals.css";
import { AppShell } from "@/components/app/app-shell";
import { Providers } from "./providers";

export const metadata: Metadata = {
  title: "AI 간편장부 세무 준비",
  description: "소규모 개인사업자를 위한 간편장부 및 신고 준비 도구",
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

