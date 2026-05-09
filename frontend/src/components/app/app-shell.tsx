"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Building2,
  CheckSquare,
  ClipboardList,
  FileSpreadsheet,
  FolderUp,
  Home,
  LogIn,
  Settings,
  UserPlus,
} from "lucide-react";
import { LEGAL_NOTICE } from "@/lib/constants";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "대시보드", icon: Home },
  { href: "/onboarding", label: "시작 설정", icon: ClipboardList },
  { href: "/businesses", label: "사업자", icon: Building2 },
  { href: "/files", label: "파일", icon: FolderUp },
  { href: "/transactions", label: "거래", icon: FileSpreadsheet },
  { href: "/ledger", label: "장부", icon: FileSpreadsheet },
  { href: "/reports", label: "리포트", icon: BarChart3 },
  { href: "/checklist", label: "체크리스트", icon: CheckSquare },
  { href: "/settings", label: "설정", icon: Settings },
];

const accountItems = [
  { href: "/login", label: "로그인", icon: LogIn },
  { href: "/signup", label: "회원가입", icon: UserPlus },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="min-h-screen">
      <header className="border-b bg-card">
        <div className="container flex h-16 items-center justify-between gap-4">
          <Link className="text-base font-semibold" href="/dashboard">
            AI 간편장부 세무 준비
          </Link>
          <nav className="hidden items-center gap-1 md:flex">
            {accountItems.map((item) => (
              <NavLink key={item.href} active={pathname === item.href} {...item} />
            ))}
          </nav>
        </div>
      </header>

      <div className="container grid gap-6 py-6 md:grid-cols-[220px_1fr]">
        <aside className="md:sticky md:top-6 md:h-[calc(100vh-7rem)]">
          <nav className="flex gap-2 overflow-x-auto pb-2 md:flex-col md:overflow-visible md:pb-0">
            {navItems.map((item) => (
              <NavLink key={item.href} active={pathname === item.href} {...item} />
            ))}
          </nav>
        </aside>

        <main className="min-w-0 space-y-6">
          {children}
          <footer className="rounded-md border bg-card p-4 text-sm text-muted-foreground">
            {LEGAL_NOTICE}
          </footer>
        </main>
      </div>
    </div>
  );
}

function NavLink({
  href,
  label,
  icon: Icon,
  active,
}: {
  href: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  active: boolean;
}) {
  return (
    <Link
      className={cn(
        "inline-flex h-10 shrink-0 items-center gap-2 rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground",
        active && "bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground",
      )}
      href={href}
    >
      <Icon className="h-4 w-4" />
      <span>{label}</span>
    </Link>
  );
}
