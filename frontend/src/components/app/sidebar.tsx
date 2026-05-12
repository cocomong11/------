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
  Settings,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "대시보드", icon: Home },
  { href: "/onboarding/business", label: "시작 설정", icon: ClipboardList },
  { href: "/businesses", label: "사업자", icon: Building2 },
  { href: "/files", label: "파일 업로드", icon: FolderUp },
  { href: "/transactions", label: "거래 정리", icon: FileSpreadsheet },
  { href: "/ledger", label: "간편장부", icon: FileSpreadsheet },
  { href: "/reports", label: "리포트", icon: BarChart3 },
  { href: "/checklist", label: "체크리스트", icon: CheckSquare },
  { href: "/settings", label: "설정", icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="md:sticky md:top-6 md:h-[calc(100vh-7rem)]">
      <div className="hidden rounded-md border bg-card p-4 shadow-sm md:block">
        <div className="mb-4 flex items-center justify-between gap-2">
          <span className="text-sm font-semibold">업무 메뉴</span>
          <Badge variant="secondary">Beta</Badge>
        </div>
        <nav className="space-y-1">
          {navItems.map((item) => (
            <SidebarLink
              key={item.href}
              active={pathname === item.href || pathname.startsWith(`${item.href}/`)}
              {...item}
            />
          ))}
        </nav>
      </div>
      <nav className="flex gap-2 overflow-x-auto pb-2 md:hidden">
        {navItems.map((item) => (
          <SidebarLink
            key={item.href}
            active={pathname === item.href || pathname.startsWith(`${item.href}/`)}
            compact
            {...item}
          />
        ))}
      </nav>
    </aside>
  );
}

function SidebarLink({
  active,
  compact = false,
  href,
  icon: Icon,
  label,
}: {
  active: boolean;
  compact?: boolean;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}) {
  return (
    <Link
      className={cn(
        "inline-flex h-10 shrink-0 items-center gap-2 rounded-md px-3 text-sm font-medium text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground",
        !compact && "w-full",
        active && "bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground",
      )}
      href={href}
    >
      <Icon className="h-4 w-4" />
      <span>{label}</span>
    </Link>
  );
}
