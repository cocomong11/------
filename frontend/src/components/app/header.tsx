"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Bell, Check, LogIn, LogOut, UserPlus } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api/auth";
import { clearAuthTokens, getAccessToken, getRefreshToken } from "@/lib/api/client";

const pageTitles: Record<string, string> = {
  "/dashboard": "대시보드",
  "/files": "파일 업로드",
  "/transactions": "거래 내역",
  "/ledger": "간편장부",
  "/reports": "리포트",
  "/checklist": "신고 체크리스트",
  "/businesses": "사업자 정보",
  "/onboarding": "시작 설정",
  "/settings": "환경 설정",
  "/login": "로그인",
  "/signup": "회원가입",
};

export function Header() {
  const pathname = usePathname();
  const [hasToken, setHasToken] = useState(false);

  useEffect(() => {
    setHasToken(Boolean(getAccessToken() || getRefreshToken()));
  }, [pathname]);

  const title = useMemo(() => {
    const matched = Object.entries(pageTitles)
      .filter(([path]) => pathname === path || pathname.startsWith(`${path}/`))
      .sort((a, b) => b[0].length - a[0].length)[0];
    return matched?.[1] ?? "장부톡";
  }, [pathname]);

  async function handleLogout() {
    const refreshToken = getRefreshToken();
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined);
    }
    clearAuthTokens();
    setHasToken(false);
    window.location.href = "/login";
  }

  return (
    <header className="sticky top-0 z-30 h-14 border-b border-border bg-card">
      <div className="flex h-full items-center justify-between gap-3 px-4 lg:px-7">
        <div className="min-w-0">
          <div className="truncate text-[15px] font-semibold text-slate-900">{title}</div>
        </div>

        <nav className="flex shrink-0 items-center gap-2">
          <Badge className="hidden sm:inline-flex" variant="success">
            <Check className="mr-1 h-3 w-3" />
            간편장부 대상
          </Badge>
          <Button aria-label="알림" size="icon" type="button" variant="outline">
            <Bell className="h-4 w-4" />
          </Button>
          {hasToken ? (
            <Button onClick={handleLogout} size="sm" type="button" variant="ghost">
              <LogOut className="h-4 w-4" />
              로그아웃
            </Button>
          ) : (
            <>
              <Button asChild size="sm" variant="ghost">
                <Link href="/login">
                  <LogIn className="h-4 w-4" />
                  로그인
                </Link>
              </Button>
              <Button asChild size="sm">
                <Link href="/signup">
                  <UserPlus className="h-4 w-4" />
                  가입
                </Link>
              </Button>
            </>
          )}
        </nav>
      </div>
    </header>
  );
}
