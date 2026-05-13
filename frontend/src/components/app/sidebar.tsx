"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  BarChart3,
  Building2,
  CheckSquare,
  ChevronRight,
  FileSpreadsheet,
  FolderUp,
  Home,
  ListChecks,
  Settings,
  UserRound,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type NavSection = {
  title: string;
  items: Array<{
    href: string;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    badge?: string;
  }>;
};

const navSections: NavSection[] = [
  {
    title: "메인",
    items: [
      { href: "/dashboard", label: "대시보드", icon: Home },
      { href: "/files", label: "파일 업로드", icon: FolderUp },
    ],
  },
  {
    title: "장부 관리",
    items: [
      { href: "/transactions", label: "거래 내역", icon: ListChecks, badge: "확인" },
      { href: "/ledger", label: "간편장부", icon: FileSpreadsheet },
    ],
  },
  {
    title: "신고 준비",
    items: [
      { href: "/reports", label: "리포트", icon: BarChart3 },
      { href: "/checklist", label: "신고 체크리스트", icon: CheckSquare },
    ],
  },
  {
    title: "설정",
    items: [
      { href: "/businesses", label: "사업자 정보", icon: Building2 },
      { href: "/onboarding/business", label: "시작 설정", icon: UserRound },
      { href: "/settings", label: "환경 설정", icon: Settings },
    ],
  },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <>
      <aside className="hidden h-screen w-[220px] shrink-0 flex-col border-r border-border bg-card lg:sticky lg:top-0 lg:flex">
        <Link className="border-b border-secondary px-5 py-6" href="/dashboard">
          <div className="text-lg font-bold tracking-normal text-primary">장부톡</div>
          <div className="mt-1 text-[11px] text-muted-foreground">AI 간편장부 · 세무신고 준비</div>
        </Link>

        <div className="mx-3 my-3 rounded-lg bg-primary/10 px-3 py-2.5">
          <div className="truncate text-[13px] font-semibold text-slate-900">내 사업자</div>
          <div className="mt-0.5 truncate text-[11px] font-medium text-primary">간편장부 준비 중</div>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 pb-3">
          {navSections.map((section) => (
            <div key={section.title} className="pt-2">
              <div className="px-2 py-2 text-[10px] font-semibold uppercase tracking-[0.08em] text-slate-400">
                {section.title}
              </div>
              <div className="space-y-0.5">
                {section.items.map((item) => (
                  <SidebarLink
                    key={item.href}
                    active={isActive(pathname, item.href)}
                    badge={item.badge}
                    href={item.href}
                    icon={item.icon}
                    label={item.label}
                  />
                ))}
              </div>
            </div>
          ))}
        </nav>

        <div className="border-t border-secondary p-3">
          <div className="flex items-center gap-2 rounded-lg px-2 py-2 transition-colors hover:bg-secondary">
            <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-bold text-primary-foreground">
              사
            </span>
            <div className="min-w-0 flex-1">
              <div className="truncate text-[13px] font-semibold text-slate-900">사용자</div>
              <div className="truncate text-[11px] text-slate-400">로그인 계정</div>
            </div>
            <ChevronRight className="h-3.5 w-3.5 text-slate-400" />
          </div>
        </div>
      </aside>

      <nav className="flex gap-2 overflow-x-auto border-b border-border bg-card px-4 py-3 lg:hidden">
        {navSections.flatMap((section) => section.items).map((item) => (
          <MobileLink
            key={item.href}
            active={isActive(pathname, item.href)}
            href={item.href}
            icon={item.icon}
            label={item.label}
          />
        ))}
      </nav>
    </>
  );
}

function SidebarLink({
  active,
  badge,
  href,
  icon: Icon,
  label,
}: {
  active: boolean;
  badge?: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}) {
  return (
    <Link
      className={cn(
        "flex h-9 items-center gap-2 rounded-lg px-2.5 text-[13px] font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-slate-900",
        active && "bg-primary/10 text-primary hover:bg-primary/10 hover:text-primary",
      )}
      href={href}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span className="truncate">{label}</span>
      {badge ? (
        <Badge className="ml-auto px-1.5 py-0 text-[10px]" variant="destructive">
          {badge}
        </Badge>
      ) : null}
    </Link>
  );
}

function MobileLink({
  active,
  href,
  icon: Icon,
  label,
}: {
  active: boolean;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}) {
  return (
    <Link
      className={cn(
        "inline-flex h-9 shrink-0 items-center gap-2 rounded-lg border border-border bg-card px-3 text-[13px] font-medium text-muted-foreground",
        active && "border-primary/20 bg-primary/10 text-primary",
      )}
      href={href}
    >
      <Icon className="h-4 w-4" />
      {label}
    </Link>
  );
}

function isActive(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}
