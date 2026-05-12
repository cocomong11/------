"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { LogIn, LogOut, ShieldCheck, UserPlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import { authApi } from "@/lib/api/auth";
import { clearAuthTokens, getAccessToken, getRefreshToken } from "@/lib/api/client";

export function Header() {
  const pathname = usePathname();
  const [hasToken, setHasToken] = useState(false);

  useEffect(() => {
    setHasToken(Boolean(getAccessToken() || getRefreshToken()));
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
    <header className="border-b bg-card/95 backdrop-blur">
      <div className="container flex h-16 items-center justify-between gap-4">
        <Link className="flex min-w-0 items-center gap-3" href="/dashboard">
          <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-primary text-primary-foreground">
            <ShieldCheck className="h-5 w-5" />
          </span>
          <span className="truncate text-base font-semibold">세무 준비 도우미</span>
        </Link>
        <nav className="flex items-center gap-2">
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
